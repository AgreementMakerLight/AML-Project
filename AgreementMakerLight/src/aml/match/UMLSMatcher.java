/******************************************************************************
* Copyright 2013-2014 LASIGE                                                  *
*                                                                             *
* Licensed under the Apache License, Version 2.0 (the "License"); you may     *
* not use this file except in compliance with the License. You may obtain a   *
* copy of the License at http://www.apache.org/licenses/LICENSE-2.0           *
*                                                                             *
* Unless required by applicable law or agreed to in writing, software         *
* distributed under the License is distributed on an "AS IS" BASIS,           *
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.    *
* See the License for the specific language governing permissions and         *
* limitations under the License.                                              *
*                                                                             *
*******************************************************************************
* Matches two Ontologies by finding literal full-name matches between their   *
* Lexicons and the UMLS table, after identifying the most suitable UMLS data  *
* source or using the whole table if no suitable source is identified.        *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 22-10-2013                                                            *
******************************************************************************/
package aml.match;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import aml.ontology.Lexicon;
import aml.ontology.Ontology;
import aml.util.Table3;

public class UMLSMatcher implements Matcher
{

//Attributes
	
	//The path to the UMLS table
	private final String PATH = "store/knowledge/UMLS.lexicon";
	//The UMLS table data structure
	private Table3<String,String,Integer> table;
	//The set of UMLS ids
	private HashSet<Integer> ids;
	//Links to the intermediate alignments
	private Alignment src;
	private Alignment tgt;
	
//Constructors
    
	/**
	 * Constructs a UMLS object
	 */
	public UMLSMatcher()
	{
		table = new Table3<String,String,Integer>();
		ids = new HashSet<Integer>();
		try
		{
			BufferedReader inStream = new BufferedReader(new FileReader(PATH));
			String line;
			while((line = inStream.readLine()) != null)
			{
				String[] words = line.split("\t");
				int id = Integer.parseInt(words[0]);
				table.add(words[2], words[1], id);
				ids.add(id);
			}
			inStream.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

//Public Methods

	/**
	 * Erases the UMLS table
	 */
	public void close()
	{
		table = null;
	}
	
	@Override
	public Alignment extendAlignment(Alignment a, double thresh)
	{
		Ontology source = a.getSource();
		Ontology target = a.getTarget();
		src = match(source);
		tgt = match(target);
		Alignment maps = new Alignment(a);
		for(Mapping m : src)
		{
			int sourceId = m.getSourceId();
			if(a.containsSource(sourceId))
				continue;
			int medId = m.getTargetId();
			Vector<Integer> matches = tgt.getTargetMappings(medId);
			for(Integer j : matches)
			{
				if(a.containsTarget(j))
					continue;
				double similarity = Math.min(m.getSimilarity(),
						tgt.getSimilarity(j, medId));
				maps.add(new Mapping(sourceId,j,similarity));
			}
		}
		return maps;
	}

	/**
	 * @return the similarity between the mediating and source ontologies
	 * or -1.0 if MediatingMatcher has not been used to match or extendAlignment
	 */
	public double getSourceSimilarity()
	{
		if(src == null)
			return -1.0;
		double inter = Math.min(src.sourceCount(),src.targetCount());
		double union = src.getSource().termCount() + ids.size() - inter;
		return inter/union;
	}
	
	/**
	 * @return the similarity between the mediating and target ontologies
	 * or -1.0 if MediatingMatcher has not been used to match or extendAlignment
	 */
	public double getTargetSimilarity()
	{
		if(tgt == null)
			return -1.0;
		double inter = Math.min(tgt.sourceCount(),tgt.targetCount());
		double union = tgt.getSource().termCount() + ids.size() - inter;
		return inter/union;
	}
	
	@Override
	public Alignment match(Ontology source, Ontology target, double thresh)
	{
		src = match(source);
		tgt = match(target);
		Alignment maps = new Alignment(source,target);
		for(Mapping m : src)
		{
			int sourceId = m.getSourceId();
			int medId = m.getTargetId();
			Vector<Integer> matches = tgt.getTargetMappings(medId);
			for(Integer j : matches)
			{
				double similarity = Math.min(m.getSimilarity(),
						tgt.getSimilarity(j, medId));
				maps.add(new Mapping(sourceId,j,similarity));
			}
		}
		return maps;
	}
	
	/**
	 * @return the number of entries in the UMLS table
	 */
	public int size()
	{
		return table.size();
	}

//Private Methods
	
	private String getBestSource(Set<String> names)
	{
		HashMap<String,Integer> sourceCount = new HashMap<String,Integer>();
		int total = names.size();
		for(String s : names)
		{
			//Get the sources for the name in UMLS
			Set<String> sources = getSources(s);
			//And update the sourceCount of each source accordingly
			for(String src : sources)
			{
				Integer srcCount = sourceCount.get(src);
				if(srcCount == null)
					srcCount = 1;
				else
					srcCount++;
				sourceCount.put(src,srcCount);
			}
		}
		String bestSource = "";
		int max = 0;
		int second = 0;
		Set<String> sr = sourceCount.keySet();
		for(String s : sr)
		{
			int count = sourceCount.get(s);
			if(count > max)
			{
				second = max;
				max = count;				
				bestSource = s;
			}
		}
		double m = max * 1.0 / total;
		double n = second * 1.0 / total;
		if(m > n + 0.2)
			return bestSource;
		else
			return "";
	}
	
	private Set<Integer> getHits(String name)
	{
		HashSet<Integer> hits = new HashSet<Integer>();
		if(!table.contains(name))
			return hits;
		Set<String> sources = table.keySet(name);
		for(String s : sources)
			hits.addAll(table.get(name, s));
		return hits;
	}
	
	private Set<Integer> getHits(String name, String source)
	{
		if(!table.contains(name,source))
			return new HashSet<Integer>();
		return new HashSet<Integer>(table.get(name,source));
	}
	
	private Set<String> getSources(String name)
	{
		if(!table.contains(name))
			return new HashSet<String>();
		return table.keySet(name);
	}
	
	private Alignment match(Ontology o)
	{
		boolean conservative = (o.termCount() > 30000);
		Lexicon l = o.getLexicon();
		Set<String> names = l.getNames();
		String bestSource = getBestSource(names);
		Alignment maps = new Alignment(o,null);
		//If there is no primary UMLS source
		if(bestSource.equals(""))
		{
			//Iterate through all the names
			for(String s : names)
			{
				Set<Integer> hits = getHits(s);
				if(hits.size() == 0)
					continue;
				Set<Integer> terms = l.getTerms(s);
				for(Integer i : terms)
				{
					for(Integer j : hits)
					{
						double sim = 0.85 * l.getCorrectedWeight(s, i);
						maps.add(i, j, sim);
					}
				}
			}
		}
		//If there is a primary UMLS source
		else
		{
			//Iterate through the terms 
			int size = o.termCount();
			for(int i = 0; i < size; i++)
			{
				HashMap<Integer,Double> hitMap = new HashMap<Integer,Double>();
				Vector<String> termNames = l.getNames(i, "localName");
				if(termNames.size() == 1)
				{
					Set<Integer> hits = getHits(termNames.get(0),bestSource);
					double weight = l.getCorrectedWeight(termNames.get(0), i);
					for(Integer j : hits)
						hitMap.put(j,weight);
				}
				if((conservative && termNames.size() != 1) ||
						(!conservative && hitMap.size() == 0))
				{
					termNames = l.getNames(i, "label");
					for(String s : termNames)
					{
						Set<Integer> hits = getHits(s);
						double weight = l.getCorrectedWeight(s, i);
						for(Integer j : hits)
							hitMap.put(j,weight);
					}
				}
				Set<Integer> hits = hitMap.keySet();
				for(Integer j : hits)
					maps.add(i, j, 0.95 * hitMap.get(j));
			}
		}
		return maps;
	}
}