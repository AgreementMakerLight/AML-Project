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
* @author Daniel Faria                                                        *
* @date 13-08-2014                                                            *
* @version 2.0                                                                *
******************************************************************************/
package aml.match;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import aml.AML;
import aml.ontology.Property;
import aml.ontology.URIMap;
import aml.util.ISub;
import aml.util.Similarity;

public class PropertyMatcher
{
	
//Attributes
	
	private Alignment maps;
	private HashMap<Integer,Property> sourceProps;
	private HashMap<Integer,Property> targetProps;
	private WordNetMatcher wn = null;

//Constructors
	
	public PropertyMatcher(boolean useWordNet)
	{
		if(useWordNet)
			wn = new WordNetMatcher();
		AML aml = AML.getInstance();
		sourceProps = aml.getSource().getPropertyMap();
		targetProps = aml.getTarget().getPropertyMap();
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
		double sim = 0.0;

		//We should only match properties of the same type
		String sType = s.getType();
		String tType = t.getType();
		if(!sType.equals(tType))
			return sim;
	
		//We should only match datatype and object properties that have
		//matching domains, if we have a class alignment to check 
		if((sType.equals("datatype") || sType.equals("object")) && maps != null)
		{
			Set<String> sDomain = s.getDomain();
			Set<String> tDomain = t.getDomain();
			if(!urisMatch(sDomain,tDomain))
				return sim;
		}
		
		//We should only match datatype properties that have matching ranges
		if(sType.equals("datatype"))
		{
			//And matching ranges
			Set<String> sRange = s.getRange();
			Set<String> tRange = t.getRange();
			if(!valuesMatch(sRange,tRange))
				return sim;
		}
		
		//We should only match object properties that have matching
		//ranges, if we have a class alignment to check 
		if(sType.equals("object") && maps != null)
		{
			Set<String> sRange = s.getRange();
			Set<String> tRange = t.getRange();
			if(!urisMatch(sRange,tRange))
				return sim;
		}
		
		//Finally, we compute the name similarity between the properties
		String sName = s.getName();
		String tName = t.getName();
		return nameSimilarity(sName,tName);
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
	
	//Checks if two URIs match (i.e., are either equal or aligned)
	private boolean urisMatch(String sUri, String tUri)
	{
		AML aml = AML.getInstance();
		URIMap uris = aml.getURIMap();
		int sIndex = uris.getIndex(sUri);
		int tIndex = uris.getIndex(tUri);
		return (sIndex == tIndex || maps.containsMapping(sIndex, tIndex));
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
	private double nameSimilarity(String s, String t)
	{
		//If the names are exactly equal, the similarity is 1
		if(s.equals(t))
			return 1.0;
		//Split the source name into words
		String[] sW = s.split(" ");
		HashSet<String> sWords = new HashSet<String>();
		HashSet<String> sSyns = new HashSet<String>();
		for(String w : sW)
		{
			sWords.add(w);
			sSyns.add(w);
			//And compute the WordNet synonyms of each word
			if(wn != null && w.length() > 3)
				sSyns.addAll(wn.getAllWordForms(w));
		}
		//Split the target name into words
		String[] tW = t.split(" ");
		HashSet<String> tWords = new HashSet<String>();
		HashSet<String> tSyns = new HashSet<String>();
		for(String w : tW)
		{
			tWords.add(w);
			//And compute the WordNet synonyms of each word
			if(wn != null && w.length() > 3)
				tSyns.addAll(wn.getAllWordForms(w));
		}
		//Compute the Jaccard word similarity between the properties
		double wordSim = Similarity.jaccard(sWords,tWords)*0.9;
		//Compute their String similarity
		double stringSim = ISub.stringSimilarity(s,t)*0.9;
		//Combine the two
		double sim = 1 - ((1-wordSim) * (1-stringSim));
		//If we're using WordNet
		if(wn != null)
		{
			//Check if the WordNet similarity
			double wordNetSim = Similarity.jaccard(sSyns,tSyns)*0.9;
			//Is greater than the name similarity
			if(wordNetSim > sim)
				//And if so, return it
				sim = wordNetSim;
		}
		//Otherwise return the name similarity
		return sim;
	}
}