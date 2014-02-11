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
* Matches two Ontologies by measuring the maximum String similarity between   *
* their terms, using one of the four available String similarity measures.    *
* NOTE: This matching algorithm takes O(N^2) time, and thus should be used    *
* only for Alignment extension whenever running time is an issue.             *
*                                                                             *
* @authors Daniel Faria, Cosmin Stroe                                         *
* @date 22-10-2013                                                            *
******************************************************************************/
package aml.match;

import java.util.Set;
import java.util.Vector;

import uk.ac.shef.wit.simmetrics.similaritymetrics.JaroWinkler;
import uk.ac.shef.wit.simmetrics.similaritymetrics.Levenshtein;
import uk.ac.shef.wit.simmetrics.similaritymetrics.QGramsDistance;

import aml.ontology.Lexicon;
import aml.ontology.Ontology;
import aml.ontology.RelationshipMap;
import aml.util.ISub;
import aml.util.StringParser;

public class ParametricStringMatcher implements Matcher
{

//Attributes

	//Links to the source and target Lexicons
	private Lexicon sLex;
	private Lexicon tLex;
	private String measure = "ISub";
	private final double CORRECTION = 0.8;

//Constructors
	
	/**
	 * Constructs a new ParametricStringMatcher with default
	 * String similarity measure (ISub)
	 */
	public ParametricStringMatcher(){}

	/**
	 * Constructs a new ParametricStringMatcher with the given String similarity measure
	 * @args m: the String similarity measure {
	 * 		ISub - the ISub measure from Falcon-AO
	 * 		Edit - the Levenshtein Edit Distance measure
	 *		JW - the Jaro Winkler measure
	 *		QGram - the Q-Gram measure
	 * }
	 */
	public ParametricStringMatcher(String m)
	{
		measure = m;
	}

//Public Methods
	
	@Override
	public Alignment extendAlignment(Alignment a, double thresh)
	{	
		Ontology source = a.getSource();
		Ontology target = a.getTarget();
		sLex = source.getLexicon();
		tLex = target.getLexicon();
		Alignment ext = extendChildrenAndParents(a,thresh);
		Alignment aux = extendChildrenAndParents(ext,thresh);
		int size = 0;
		for(int i = 0; i < 10 && ext.size() > size; i++)
		{
			size = ext.size();
			for(Mapping m : aux)
				if(!a.containsConflict(m))
					ext.add(m);
			aux = extendChildrenAndParents(aux,thresh);
		}
		size = Math.max(sLex.size(), tLex.size());
		if(size <= 30000)
			ext.addAll(extendSiblings(a,thresh));
		return ext;
	}
	
	@Override
	public Alignment match(Ontology source, Ontology target, double thresh)
	{
		sLex = source.getLexicon();
		tLex = target.getLexicon();
		Alignment a = new Alignment(source, target);
		for(int i = 0; i < source.termCount(); i++)
		{
			for(int j = 0; j < source.termCount(); j++)
			{
				Mapping m = mapTwoTerms(i,j);
				if(m.getSimilarity() >= thresh)
					a.add(m);
			}
		}
		return a;
	}
	
	public Alignment rematch(Alignment a, double thresh)
	{
		Ontology source = a.getSource();
		Ontology target = a.getTarget();
		sLex = source.getLexicon();
		tLex = target.getLexicon();
		Alignment maps = new Alignment(source, target);
		for(Mapping m : a)
		{
			Mapping newMap = mapTwoTerms(m.getSourceId(),m.getTargetId());
			if(m.getSimilarity() >= thresh)
				maps.add(newMap);
		}
		return a;
	}
	
//Private Methods
	
	private Alignment extendChildrenAndParents(Alignment a, double thresh)
	{
		Ontology source = a.getSource();
		Ontology target = a.getTarget();
		RelationshipMap sMap = source.getRelationshipMap();
		RelationshipMap tMap = target.getRelationshipMap();
		
		Alignment maps = new Alignment(source,target);
		for(int i = 0; i < a.size(); i++)
		{
			Mapping input = a.get(i);
			Vector<Integer> sourceChildren = sMap.getChildren(input.getSourceId());
			Vector<Integer> targetChildren = tMap.getChildren(input.getTargetId());
			if(sourceChildren == null || targetChildren == null)
				continue;
			for(Integer s : sourceChildren)
			{
				if(a.containsSource(s))
					continue;
				for(Integer t : targetChildren)
				{
					if(a.containsTarget(t))
						continue;
					Mapping map = mapTwoTerms(s, t);
					if(map.getSimilarity() >= thresh)
						maps.add(map);
				}
			}
			Vector<Integer> sourceParents = sMap.getParents(input.getSourceId());
			Vector<Integer> targetParents = tMap.getParents(input.getTargetId());
			if(sourceParents == null || targetParents == null)
				continue;
			for(Integer s : sourceParents)
			{
				if(a.containsSource(s))
					continue;
				for(Integer t : targetParents)
				{
					if(a.containsTarget(t))
						continue;
					Mapping map = mapTwoTerms(s, t);
					if(map.getSimilarity() >= thresh)
						maps.add(map);
				}
			}
		}
		return maps;
	}
	
	private Alignment extendSiblings(Alignment a, double thresh)
	{		
		Ontology source = a.getSource();
		Ontology target = a.getTarget();
		RelationshipMap sMap = source.getRelationshipMap();
		RelationshipMap tMap = target.getRelationshipMap();
		
		Alignment maps = new Alignment(source,target);
		for(int i = 0; i < a.size(); i++)
		{
			Mapping input = a.get(i);
			Vector<Integer> sourceSiblings = sMap.getSiblings(input.getSourceId());
			Vector<Integer> targetSiblings = tMap.getSiblings(input.getTargetId());
			if(sourceSiblings == null || targetSiblings == null)
				continue;
			for(Integer s : sourceSiblings)
			{
				if(a.containsSource(s))
					continue;
				for(Integer t : targetSiblings)
				{
					if(a.containsTarget(t))
						continue;
					Mapping map = mapTwoTerms(s, t);
					if(map.getSimilarity() >= thresh)
						maps.add(map);
				}
			}
		}
		return maps;
	}
	
	//Computes the maximum String similarity between two terms by doing a
	//pairwise comparison of all their names
	private Mapping mapTwoTerms(int sId, int tId)
	{
		//Initialize the mapping between the terms with similarity 0
		Mapping m = new Mapping(sId,tId,0.0);
		//Get the source and target names
		Set<String> sourceNames = sLex.getNames(sId);
		Set<String> targetNames = tLex.getNames(tId);
		double weight, similarity;
		
		for(String s : sourceNames)
		{
			if(StringParser.isFormula(s))
				continue;
			weight = sLex.getCorrectedWeight(s, sId);
			for(String t : targetNames)
			{
				if(StringParser.isFormula(t))
					continue;
				similarity = weight * tLex.getCorrectedWeight(t, tId);
				similarity *= stringSimilarity(s,t);
				if(similarity > m.getSimilarity())
					m.setSimilarity(similarity);
			}
		}
		return m;
	}
	
	//Gets the similarity between two Strings
	private double stringSimilarity(String s, String t)
	{
		double sim = 0.0;
		if(measure.equals("ISub"))
			sim = ISub.stringSimilarity(s,t);
		else if(measure.equals("Edit"))
		{
			Levenshtein lv = new Levenshtein();
			sim = lv.getSimilarity(s, t);
		}
		else if(measure.equals("JW"))
		{
			JaroWinkler jv = new JaroWinkler();
			sim = jv.getSimilarity(s, t);
		}
		else if(measure.equals("QGram"))
		{
			QGramsDistance q = new QGramsDistance();
			sim = q.getSimilarity(s, t);
		}
		sim *= CORRECTION;
		return sim;
	}
}