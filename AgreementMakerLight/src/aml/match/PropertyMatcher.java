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
* Matching algorithm that maps the properties of two Ontologies by comparing  *
* their names, types, domains and ranges. Requires an Alignment between the   *
* terms (classes) of the Ontologies as input, in order to check for domain    *
* and range matches. Can use WordNet to boost the name similarity.            *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 22-10-2013                                                            *
******************************************************************************/
package aml.match;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import aml.ontology.Ontology;
import aml.ontology.Property;
import aml.ontology.PropertyList;

public class PropertyMatcher
{
	
//Attributes
	
	private Alignment maps;
	private Ontology source;
	private Ontology target;
	private HashMap<String,Double> wordEC;
	private WordNetMatcher wn = null;

//Constructors
	
	public PropertyMatcher(boolean useWordNet)
	{
		if(useWordNet)
			wn = new WordNetMatcher();
	}
	
//Public Methods
	
	/**
	 * Maps the properties of two Ontologies given their term Alignment
	 * @param a: the term Alignment between the Ontologies
	 * @param threshold: the similarity threshold for the property Mappings
	 * @return the list of Mappings between the properties of the Ontologies
	 */
	public Vector<Mapping> matchProperties(Alignment a, double threshold)
	{
		maps = a;
		source = a.getSource();
		target = a.getTarget();
		buildWordMap();
		Vector<Mapping> propMaps = new Vector<Mapping>(0,1);
		PropertyList sourceProps = source.getPropertyList();
		PropertyList targetProps = target.getPropertyList();
		for(Property s : sourceProps)
		{
			for(Property t : targetProps)
			{
				Mapping m = matchProperties(s,t);
				if(m.getSimilarity() >= threshold)
					propMaps.add(m);
			}
		}
		maps = null;
		source = null;
		target = null;
		return propMaps;
	}

//Private Methods
	
	//Builds the map of word evidence contents from the property names
	private void buildWordMap()
	{
		wordEC = new HashMap<String,Double>();
		int total = 0;
		PropertyList sourceProps = source.getPropertyList();
		for(Property s : sourceProps)
			total += addNames(s);
		PropertyList targetProps = target.getPropertyList();
		for(Property t : targetProps)
			total += addNames(t);
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
	private Mapping matchProperties(Property s, Property t)
	{
		Mapping m = new Mapping(s.getIndex(),t.getIndex(),0.0);

		//We should only match properties that share a type
		Vector<String> sType = s.getType();
		Vector<String> tType = t.getType();
		double typeSim = jaccard(sType,tType);
		if(typeSim < 0.5)
			return m;
	
		//We should only match properties that have matching domains
		Vector<String> sDomain = s.getDomain();
		Vector<String> tDomain = t.getDomain();
		if(!domainsMatch(sDomain,tDomain))
			return m;
		//And matching ranges
		Vector<String> sRange = s.getRange();
		Vector<String> tRange = t.getRange();
		if(!rangesMatch(sRange,tRange))
			return m;
		
		//Finally, we compute the name similarity between the properties
		String sName = s.getName();
		String tName = t.getName();
		double nameSim = nameSimilarity(sName,tName);
		//store it
		m.setSimilarity(nameSim);
		//and return the Mapping
		return m;
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
	
	//Checks if two lists of property domains match (i.e., have similarity above 50%)
	private boolean domainsMatch(Vector<String> sDomain, Vector<String> tDomain)
	{
		if(sDomain.size() == 0 && tDomain.size() == 0)
			return true;
		if(sDomain.size() == 0 || tDomain.size() == 0)
			return false;
		if(sDomain.size() == 1 && tDomain.size() == 1)
			return domainsMatch(sDomain.get(0),tDomain.get(0));
		double matches = 0.0;
		for(String s : sDomain)
		{
			for(String t : tDomain)
			{
				if(domainsMatch(s,t))
				{
					matches++;
					break;
				}
			}
		}
		matches /= sDomain.size()+tDomain.size()-matches;
		return (matches > 0.5);
	}
	
	//Checks if two domains match:
	//->If the domains are local terms, check if they are matched in the input Alignment
	//or if the Alignment contains a parent Mapping
	//->Otherwise, check if they are equal
	private boolean domainsMatch(String sUri, String tUri)
	{
		int sIndex = -1;
		if(source.isLocal(sUri))
			sIndex = source.getTermIndex(sUri);
		int tIndex = -1;
		if(target.isLocal(tUri))
			tIndex = target.getTermIndex(tUri);
		if(sIndex > -1 && tIndex > -1)
			return (maps.containsMapping(sIndex, tIndex) ||
					maps.containsParentMapping(sIndex, tIndex));
		return sUri.equals(tUri);
	}

	//Checks if two lists of property ranges match (i.e., have similarity above 50%)
	private boolean rangesMatch(Vector<String> sRange, Vector<String> tRange)
	{
		if(sRange.size() == 0 && tRange.size() == 0)
			return true;
		if(sRange.size() == 0 || tRange.size() == 0)
			return false;
		if(sRange.size() == 1 && tRange.size() == 1)
			return rangesMatch(sRange.get(0),tRange.get(0));
		double matches = 0.0;
		for(String s : sRange)
		{
			for(String t : tRange)
			{
				if(rangesMatch(s,t))
				{
					matches++;
					break;
				}
			}
		}
		matches /= sRange.size()+tRange.size()-matches;
		return (matches > 0.5);
	}
	
	//Checks if two ranges match:
	//->If the ranges are local terms, check if they are matched in the input Alignment
	//(but don't check for parent Mappings as ranges should be equal, not contained)
	//->Otherwise, check if they are equal
	private boolean rangesMatch(String sUri, String tUri)
	{
		int sIndex = -1;
		if(source.isLocal(sUri))
			sIndex = source.getTermIndex(sUri);
		int tIndex = -1;
		if(target.isLocal(tUri))
			tIndex = target.getTermIndex(tUri);
		if(sIndex > -1 && tIndex > -1)
			return (maps.containsMapping(sIndex, tIndex));
		return sUri.equals(tUri);
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