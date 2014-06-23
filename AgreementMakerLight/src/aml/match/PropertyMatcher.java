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
* @date 23-06-2014                                                            *
* @version 2.0                                                                *
******************************************************************************/
package aml.match;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import aml.AML;
import aml.ontology.Property;
import aml.ontology.URIMap;

public class PropertyMatcher
{
	
//Attributes
	
	private Alignment maps;
	private HashMap<Integer,Property> sourceProps;
	private HashMap<Integer,Property> targetProps;
	private HashMap<String,Double> wordEC;
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
		buildWordMap();
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
	
	//Builds the map of word evidence contents from the property names
	private void buildWordMap()
	{
		wordEC = new HashMap<String,Double>();
		int total = 0;
		Set<Integer> sourceKeys = sourceProps.keySet();
		for(Integer i : sourceKeys)
			total += addNames(sourceProps.get(i));
		Set<Integer> targetKeys = targetProps.keySet();
		for(Integer i : targetKeys)
			total += addNames(targetProps.get(i));
		Set<String> words = wordEC.keySet();
		double max = Math.log(total);
		for(String w : words)
		{
			double ec = 1 - (Math.log(wordEC.get(w)) / max);
			wordEC.put(w, ec);
		}
	}
	
	//Parses the name of a property into words and adds them to the wordEC map
	private int addNames(Property p)
	{
		int total = 0;
		String name = p.getName();
		String[] words = name.split(" ");
		for(String w : words)
		{
			Double ec = wordEC.get(w);
			if(ec == null)
				ec = 1.0;
			else
				ec++;
			wordEC.put(w, ec);
			total++;
		}
		return total;
	}

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
			Vector<String> sDomain = s.getDomain();
			Vector<String> tDomain = t.getDomain();
			if(!urisMatch(sDomain,tDomain))
				return sim;
		}
		
		//We should only match datatype properties that have matching ranges
		if(sType.equals("datatype"))
		{
			//And matching ranges
			Vector<String> sRange = s.getRange();
			Vector<String> tRange = t.getRange();
			if(!valuesMatch(sRange,tRange))
				return sim;
		}
		
		//We should only match object properties that have matching
		//ranges, if we have a class alignment to check 
		if(sType.equals("object") && maps != null)
		{
			Vector<String> sRange = s.getRange();
			Vector<String> tRange = t.getRange();
			if(!urisMatch(sRange,tRange))
				return sim;
		}
		
		//Finally, we compute the name similarity between the properties
		String sName = s.getName();
		String tName = t.getName();
		return nameSimilarity(sName,tName);
	}

	//The Jaccard similarity between two Collections of Strings
	private double jaccard(Collection<String> s, Collection<String> t)
	{
		if(s.size() == 0 && t.size() == 0)
			return 0.0;
		double intersection = 0.0;
		double union = 0.0;
		for(String st : s)
		{
			if(t.contains(st))
				intersection++;
			else
				union++;
		}
		union += t.size();
		return intersection/union;
	}
	
	//Checks if two lists of uris match (i.e., have Jaccard similarity above 50%)
	private boolean urisMatch(Vector<String> sURIs, Vector<String> tURIs)
	{
		if(sURIs.size() == 0 && tURIs.size() == 0)
			return true;
		if(sURIs.size() == 0 || tURIs.size() == 0)
			return false;
		if(sURIs.size() == 1 && tURIs.size() == 1)
			return urisMatch(sURIs.get(0),tURIs.get(0));
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

	//Checks if two lists of values match (i.e., have similarity above 50%)
	private boolean valuesMatch(Vector<String> sRange, Vector<String> tRange)
	{
		if(sRange.size() == 0 && tRange.size() == 0)
			return true;
		if(sRange.size() == 0 || tRange.size() == 0)
			return false;
		if(sRange.size() == 1 && tRange.size() == 1)
			return sRange.get(0).equals(tRange.get(0));
		double matches = 0.0;
		for(String s : sRange)
		{
			for(String t : tRange)
			{
				if(s.equals(t))
				{
					matches++;
					break;
				}
			}
		}
		matches /= sRange.size()+tRange.size()-matches;
		return (matches > 0.5);
	}
	
	//Measures the name similarity between two properties using a 
	//weighted Jaccard index between their words
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
		//Compute the weighted Jaccard similarity between
		//the words of each property name
		double intersection = 0.0;
		double union = 0.0;
		for(String w : sWords)
		{
			if(tWords.contains(w))
				intersection += wordEC.get(w);
			else
				union += wordEC.get(w);
		}
		for(String w : tWords)
			union += wordEC.get(w);
		intersection /= union;
		//If we're using WordNet
		if(wn != null)
		{
			//Check if the WordNet similarity
			double sim = jaccard(sSyns,tSyns) * 0.9;
			//Is greater than the name similarity
			if(sim > intersection)
				//And if so, return it
				return sim;
		}
		//Otherwise return the name similarity
		return intersection;
	}
}