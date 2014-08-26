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
* Matching algorithm that maps the properties of the Ontologies by comparing  *
* their names, types, domains and ranges. Can use an input class Alignment    *
* to check for domain and range matches. Can use WordNet to boost the name    *
* similarity.                                                                 *
*                                                                             *
* @author Daniel Faria, Catarina Martins                                      *
* @date 20-08-2014                                                            *
* @version 2.0                                                                *
******************************************************************************/
package aml.match;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import aml.AML;
import aml.ontology.Property;
import aml.ontology.RelationshipMap;
import aml.ontology.URIMap;
import aml.util.ISub;
import aml.util.Similarity;
import aml.util.StopList;

public class PropertyMatcher
{
	
//Attributes
	
	private Alignment maps;
	private AML aml;
	private HashMap<Integer,Property> sourceProps;
	private HashMap<Integer,Property> targetProps;
	private WordNetMatcher wn = null;
	private Set<String> stopSet;

//Constructors
	
	public PropertyMatcher(boolean useWordNet)
	{
		if(useWordNet)
			wn = new WordNetMatcher();
		aml = AML.getInstance();
		sourceProps = aml.getSource().getPropertyMap();
		targetProps = aml.getTarget().getPropertyMap();
		stopSet = StopList.read();
	}
	
//Public Methods
	
	/**
	 * Maps the properties of two Ontologies given their term Alignment
	 * @param a: the term Alignment between the Ontologies
	 * @param threshold: the similarity threshold for the property Mappings
	 * @return the list of Mappings between the properties of the Ontologies
	 */
	public Alignment matchProperties(Alignment a, double threshold)
	{
		maps = a;
		Alignment propMaps = new Alignment();
		Set<Integer> sourceKeys = sourceProps.keySet();
		Set<Integer> targetKeys = targetProps.keySet();
		for(Integer i : sourceKeys)
		{
			for(Integer j : targetKeys)
			{
				double sim = matchProperties(sourceProps.get(i),targetProps.get(j));
				if(sim >= threshold)
					propMaps.add(new Mapping(i,j,sim));
			}
		}
		return propMaps;
	}

//Private Methods
	
	//Matches two properties
	private double matchProperties(Property s, Property t)
	{
		double sim = 0.0d;

		//We should only match properties of the same type
		String sType = s.getType();
		String tType = t.getType();
		if(!sType.equals(tType))
			return sim;
	
		//We should only match datatype properties that have matching domains and ranges
		if(sType.equals("datatype"))
		{
			Set<String> sDomain = s.getDomain();
			Set<String> tDomain = t.getDomain();
			if(!urisMatch(sDomain,tDomain))
				return sim;
			Set<String> sRange = s.getRange();
			Set<String> tRange = t.getRange();
			if(!valuesMatch(sRange,tRange))
				return sim;
		}
		//We should only match object properties that have matching domains and ranges
		else if(sType.equals("object"))
		{
			Set<String> sDomain = s.getDomain();
			Set<String> tDomain = t.getDomain();
			if(!urisMatch(sDomain,tDomain))
				return sim;
			Set<String> sRange = s.getRange();
			Set<String> tRange = t.getRange();
			if(!urisMatch(sRange,tRange))
				return sim;
		}
		//If they do, we can compute the name similarity between the properties
		return nameSimilarity(s,t);
	}

	//Checks if two lists of uris match (i.e., have Jaccard similarity above 50%)
	private boolean urisMatch(Set<String> sURIs, Set<String> tURIs)
	{
		if(sURIs.size() == 0 && tURIs.size() == 0)
			return true;
		if(sURIs.size() == 0 || tURIs.size() == 0)
			return false;
		double matches = 0.0;
		for(String s : sURIs)
		{
			for(String t : tURIs)
			{
				if(urisMatch(s,t))
				{
					matches++;
					break;
				}
			}
		}
		matches /= sURIs.size()+tURIs.size()-matches;
		return (matches > 0.5);
	}
	
	//Checks if two URIs match (i.e., are either equal, aligned
	//or one is aligned to the parent of the other)
	private boolean urisMatch(String sUri, String tUri)
	{
		URIMap uris = aml.getURIMap();
		RelationshipMap rm = aml.getRelationshipMap();
		int sIndex = uris.getIndex(sUri);
		int tIndex = uris.getIndex(tUri);

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
	
	//Measures the name similarity between two properties using a 
	//Jaccard index between their words
	//When using WordNet, the WordNet similarity is given by
	//the Jaccard index between all WordNet synonyms, and is
	//returned instead of the name similarity if it is higher
	private double nameSimilarity(Property s, Property t)
	{
		String sourceName = s.getName();
		String targetName = t.getName();
		
		String newSourceName = removeStopWords(sourceName);
		if(!newSourceName.isEmpty())
			sourceName = newSourceName;
		String newTargetName = removeStopWords(targetName);
		if(!newTargetName.isEmpty())
			targetName = newTargetName;
		
		String sourceTrans = s.getTranslation();
		String targetTrans = t.getTranslation();
		
		if(sourceName.equals(targetName) ||
				(!sourceTrans.isEmpty() && sourceTrans.equals(targetName)) ||
				(!targetTrans.isEmpty() && sourceName.equals(targetTrans)))
			return 1.0;
		
		double sim1 = nameSimilarity(sourceName,targetName, wn != null);
		
		double sim2 = 0.0;
		if(!sourceTrans.isEmpty())
			sim2 = nameSimilarity(sourceTrans,targetName, wn != null);
		if(!targetTrans.isEmpty())
			sim2 = Math.max(sim2,nameSimilarity(sourceName,targetTrans, wn != null));
		
		return Math.max(sim1,sim2);
	}
				
	private double nameSimilarity(String n1, String n2, boolean useWordNet)
	{
		//Split the source name into words
		String[] sW = n1.split(" ");
		HashSet<String> sWords = new HashSet<String>();
		HashSet<String> sSyns = new HashSet<String>();
		for(String w : sW)
		{
			sWords.add(w);
			sSyns.add(w);
			//And compute the WordNet synonyms of each word
			if(useWordNet && w.length() > 3)
				sSyns.addAll(wn.getAllWordForms(w));
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
		double simString =  ISub.stringSimilarity(n1,n2)*0.9;
		//Combine the two
		double sim = 1 - ((1-wordSim) * (1-simString));
		if(useWordNet)
		{
			//Check if the WordNet similarity
			double wordNetSim = Similarity.jaccard(sSyns,tSyns)*0.9;
			//Is greater than the name similarity
			if(wordNetSim > sim)
				//And if so, return it
				sim = wordNetSim;
		}
		return sim;
	}
	
	private String removeStopWords(String name)
	{
		String newName = "";
		String[] words = name.split(" ");
		for(String w : words)
			if(!stopSet.contains(w))
				newName += w + " ";
		return newName.trim();
	}
}