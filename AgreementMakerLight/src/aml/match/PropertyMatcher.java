/******************************************************************************
* Copyright 2013-2016 LASIGE                                                  *
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
* Matching algorithm that maps Ontology properties by comparing their names,  *
* types, domains and ranges. Can use an input class Alignment to check for    *
* domain and range matches. Can use WordNet to boost the name similarity.     *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.match;

import java.util.HashSet;
import java.util.Set;

import aml.AML;
import aml.match.Alignment;
import aml.match.Mapping;
import aml.match.SecondaryMatcher;
import aml.ontology.DataProperty;
import aml.ontology.ObjectProperty;
import aml.ontology.Ontology2Match;
import aml.ontology.RelationshipMap;
import aml.settings.EntityType;
import aml.util.ISub;
import aml.util.Similarity;
import aml.util.WordNet;

public class PropertyMatcher implements SecondaryMatcher
{
	
//Attributes
	
	private Alignment maps;
	private AML aml;
	private Ontology2Match source;
	private Ontology2Match target;
	private WordNet wn = null;
	
//Constructors
	
	public PropertyMatcher(boolean useWordNet)
	{
		if(useWordNet)
			wn = new WordNet();
		aml = AML.getInstance();
		source = aml.getSource();
		target = aml.getTarget();
	}
	
//Public Methods
	
	@Override
	public Alignment extendAlignment(Alignment a, double threshold)
	{
		System.out.println("Extending Alignment with Property Matcher");
		long time = System.currentTimeMillis()/1000;
		maps = a;
		Alignment propMaps = new Alignment();
		//Map data properties
		Set<Integer> sourceKeys = source.getDataProperties();
		Set<Integer> targetKeys = target.getDataProperties();
		for(Integer i : sourceKeys)
		{
			for(Integer j : targetKeys)
			{
				double sim = matchProperties(i,j,EntityType.DATA);
				if(sim >= threshold)
					propMaps.add(new Mapping(i,j,sim));
			}
		}
		//Map object properties
		sourceKeys = source.getObjectProperties();
		targetKeys = target.getObjectProperties();
		for(Integer i : sourceKeys)
		{
			for(Integer j : targetKeys)
			{
				double sim = matchProperties(i,j,EntityType.OBJECT);
				if(sim >= threshold)
					propMaps.add(new Mapping(i,j,sim));
			}
		}
		time = System.currentTimeMillis()/1000 - time;
		System.out.println("Finished in " + time + " seconds");
		return propMaps;
	}

//Private Methods
	
	//Checks if two lists of ids match (i.e., have Jaccard similarity above 50%)
	private boolean idsMatch(Set<Integer> sIds, Set<Integer> tIds)
	{
		if(sIds.size() == 0 && tIds.size() == 0)
			return true;
		if(sIds.size() == 0 || tIds.size() == 0)
			return false;
		double matches = 0.0;
		for(Integer i : sIds)
		{
			for(Integer j : tIds)
			{
				if(idsMatch(i,j))
				{
					matches++;
					break;
				}
			}
		}
		matches /= sIds.size()+tIds.size()-matches;
		return (matches > 0.5);
	}
	
	//Checks if two ids match (i.e., are either equal, aligned
	//or one is aligned to the parent of the other)
	private boolean idsMatch(int sIndex, int tIndex)
	{
		RelationshipMap rm = aml.getRelationshipMap();

		if(sIndex == tIndex || maps.containsMapping(sIndex, tIndex))
	    	return true;
		
		Set<Integer> sParent= rm.getParents(sIndex);
		if(sParent.size()==1)
		{
			int spId = sParent.iterator().next();
			if(maps.containsMapping(spId, tIndex))
				return true;
		}
		Set<Integer> tParent= rm.getParents(tIndex);
		if(tParent.size()==1)
		{
			int tpId=tParent.iterator().next();
			if(maps.containsMapping(sIndex, tpId))
				return true;
		}
		return false;
	}
	
	//Matches two annotation properties
	private double matchProperties(int sourceId, int targetId, EntityType e)
	{
		double sim = 0.0d;

		//We should only match datatype properties that have matching domains and ranges
		if(e.equals(EntityType.DATA))
		{
			DataProperty sProp = source.getDataProperty(sourceId);
			DataProperty tProp = target.getDataProperty(targetId);
			Set<Integer> sDomain = sProp.getDomain();
			Set<Integer> tDomain = tProp.getDomain();
			if(!idsMatch(sDomain,tDomain))
				return sim;
			Set<String> sRange = sProp.getRange();
			Set<String> tRange = tProp.getRange();
			if(!valuesMatch(sRange,tRange))
				return sim;
		}
		//We should only match object properties that have matching domains and ranges
		else if(e.equals(EntityType.OBJECT))
		{
			ObjectProperty sProp = source.getObjectProperty(sourceId);
			ObjectProperty tProp = target.getObjectProperty(targetId);
			Set<Integer> sDomain = sProp.getDomain();
			Set<Integer> tDomain = tProp.getDomain();
			if(!idsMatch(sDomain,tDomain))
				return sim;
			Set<Integer> sRange = sProp.getRange();
			Set<Integer> tRange = tProp.getRange();
			if(!idsMatch(sRange,tRange))
				return sim;
		}
		//If they do, we can compute the name similarity between the properties
		return nameSimilarity(sourceId,targetId);
	}

	//Measures the name similarity between two properties, given by the
	//maximum similarity between their names
	private double nameSimilarity(int s, int t)
	{
		Set<String> sourceNames = source.getLexicon().getNames(s);
		Set<String> targetNames = target.getLexicon().getNames(t);
		for(String sName : sourceNames)
			for(String tName : targetNames)
				if(sName.equals(tName))
					return 1.0;

		double sim = 0.0;
		for(String sName : sourceNames)
		{
			for(String tName : targetNames)
			{
				double newSim = nameSimilarity(sName,tName,wn != null);
				if(newSim > sim)
					sim = newSim;
			}
		}
		return sim;
	}
			
	//Computes the similarity between two property names using a Jaccard
	//index between their words. When using WordNet, the WordNet similarity
	//is given by the Jaccard index between all WordNet synonyms, and is
	//returned instead of the name similarity if it is higher
	private double nameSimilarity(String n1, String n2, boolean useWordNet)
	{
		//Check if the names are equal
		if(n1.equals(n2))
			return 1.0;
		
		//Split the source name into words
		String[] sW = n1.split(" ");
		HashSet<String> sWords = new HashSet<String>();
		HashSet<String> sSyns = new HashSet<String>();
		for(String w : sW)
		{
			sWords.add(w);
			sSyns.add(w);
			//And compute the WordNet synonyms of each word
			if(useWordNet && w.length() > 2)
				sSyns.addAll(wn.getAllNounWordForms(w));
		}
		
		//Split the target name into words
		String[] tW = n2.split(" ");
		HashSet<String> tWords = new HashSet<String>();
		HashSet<String> tSyns = new HashSet<String>();		
		for(String w : tW)
		{
			tWords.add(w);
			tSyns.add(w);
			//And compute the WordNet synonyms of each word
			if(useWordNet && w.length() > 3)
				tSyns.addAll(wn.getAllWordForms(w));
		}
		
		//Compute the Jaccard word similarity between the properties
		double wordSim = Similarity.jaccard(sWords,tWords)*0.9;
		//and the String similarity
		double simString = ISub.stringSimilarity(n1,n2)*0.9;
		//Combine the two
		double sim = 1 - ((1-wordSim) * (1-simString));
		if(useWordNet)
		{
			//Check if the WordNet similarity
			double wordNetSim = Similarity.jaccard(sSyns,tSyns);
			//Is greater than the name similarity
			if(wordNetSim > sim)
				//And if so, return it
				sim = wordNetSim;
		}
		return sim;
	}

	//Checks if two lists of values match (i.e., have Jaccard similarity above 50%)
	private boolean valuesMatch(Set<String> sRange, Set<String> tRange)
	{
		if(sRange.size() == 0 && tRange.size() == 0)
			return true;
		if(sRange.size() == 0 || tRange.size() == 0)
			return false;
		double sim = Similarity.jaccard(sRange,tRange);
		return (sim > 0.5);
	}
}