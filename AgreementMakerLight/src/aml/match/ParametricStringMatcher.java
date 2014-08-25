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
* Matches Ontologies by measuring the maximum String similarity between their *
* classes, using one of the four available String similarity measures.        *
* NOTE: This matching algorithm takes O(N^2) time, and thus should be used    *
* only for Alignment extension whenever running time is an issue.             *
*                                                                             *
* @authors Daniel Faria, Cosmin Stroe                                         *
* @date 31-07-2014                                                            *
* @version 2.0                                                                *
******************************************************************************/
package aml.match;

import java.util.HashSet;
import java.util.Set;

import uk.ac.shef.wit.simmetrics.similaritymetrics.JaroWinkler;
import uk.ac.shef.wit.simmetrics.similaritymetrics.Levenshtein;
import uk.ac.shef.wit.simmetrics.similaritymetrics.QGramsDistance;
import aml.AML;
import aml.ontology.Lexicon;
import aml.ontology.RelationshipMap;
import aml.settings.LanguageSetting;
import aml.util.ISub;
import aml.util.StringParser;

public class ParametricStringMatcher implements SecondaryMatcher, PrimaryMatcher, Rematcher
{

//Attributes

	//Links to the source and target Lexicons
	private Lexicon sLex;
	private Lexicon tLex;
	private LanguageSetting lSet;
	private HashSet<String> languages;
	private String measure = "ISub";
	private final double CORRECTION = 0.80;

//Constructors
	
	/**
	 * Constructs a new ParametricStringMatcher with default
	 * String similarity measure (ISub)
	 */
	public ParametricStringMatcher()
	{
		AML aml = AML.getInstance();
		sLex = aml.getSource().getLexicon();
		tLex = aml.getTarget().getLexicon();
		lSet = AML.getInstance().getLanguageSetting();
		languages = new HashSet<String>();
		if(lSet.equals(LanguageSetting.MULTI))
			for(String l : sLex.getLanguages())
				if(tLex.getLanguages().contains(l))
					languages.add(l);
	}

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
		this();
		measure = m;
	}

//Public Methods
	
	@Override
	public Alignment extendAlignment(Alignment a, double thresh)
	{	
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
		ext.addAll(extendSiblings(a,thresh));
		return ext;
	}
	
	@Override
	public Alignment match(double thresh)
	{
		Alignment a = new Alignment();
		Set<Integer> sources = sLex.getClasses();
		Set<Integer> targets = tLex.getClasses();
		for(Integer i : sources)
		{
			for(Integer j : targets)
			{
				double sim = mapTwoClasses(i,j);
				if(sim >= thresh)
					a.add(i,j,sim);
			}
		}
		return a;
	}
	
	@Override
	public Alignment rematch(Alignment a)
	{
		Alignment maps = new Alignment();
		for(Mapping m : a)
		{
			double sim = mapTwoClasses(m.getSourceId(),m.getTargetId());
			maps.add(m.getSourceId(),m.getTargetId(),sim);
		}
		return maps;
	}
	
//Private Methods
	
	private Alignment extendChildrenAndParents(Alignment a, double thresh)
	{
		AML aml = AML.getInstance();
		RelationshipMap rels = aml.getRelationshipMap();
		
		Alignment maps = new Alignment();
		for(int i = 0; i < a.size(); i++)
		{
			Mapping input = a.get(i);
			Set<Integer> sourceChildren = rels.getChildren(input.getSourceId());
			Set<Integer> targetChildren = rels.getChildren(input.getTargetId());
			for(Integer s : sourceChildren)
			{
				if(a.containsSource(s))
					continue;
				for(Integer t : targetChildren)
				{
					if(a.containsTarget(t))
						continue;
					double sim = mapTwoClasses(s, t);
					if(sim >= thresh)
						maps.add(s,t,sim);
				}
			}
			Set<Integer> sourceParents = rels.getParents(input.getSourceId());
			Set<Integer> targetParents = rels.getParents(input.getTargetId());
			for(Integer s : sourceParents)
			{
				if(a.containsSource(s))
					continue;
				for(Integer t : targetParents)
				{
					if(a.containsTarget(t))
						continue;
					double sim = mapTwoClasses(s, t);
					if(sim >= thresh)
						maps.add(s,t,sim);
				}
			}
		}

		return maps;
	}
	
	private Alignment extendSiblings(Alignment a, double thresh)
	{		
		AML aml = AML.getInstance();
		RelationshipMap rels = aml.getRelationshipMap();
		Alignment maps = new Alignment();
		for(int i = 0; i < a.size(); i++)
		{
			Mapping input = a.get(i);
			Set<Integer> sourceSiblings = rels.getAllSiblings(input.getSourceId());
			Set<Integer> targetSiblings = rels.getAllSiblings(input.getTargetId());
			if(sourceSiblings.size() > 200 || targetSiblings.size() > 200)
				continue;
			for(Integer s : sourceSiblings)
			{
				if(a.containsSource(s))
					continue;
				for(Integer t : targetSiblings)
				{
					if(a.containsTarget(t))
						continue;
					double sim = mapTwoClasses(s, t);
					if(sim >= thresh)
						maps.add(s,t,sim);
				}
			}
		}
		return maps;
	}
	
	//Computes the maximum String similarity between two Classes by doing a
	//pairwise comparison of all their names
	private double mapTwoClasses(int sId, int tId)
	{
		double maxSim = 0.0;
		double sim, weight;
		
		if(lSet.equals(LanguageSetting.MULTI))
		{
			for(String l : languages)
			{
				Set<String> sourceNames = sLex.getNamesWithLanguage(sId,l);
				Set<String> targetNames = tLex.getNamesWithLanguage(tId,l);
				if(sourceNames == null || targetNames == null)
					continue;
			
				for(String s : sourceNames)
				{
					if(StringParser.isFormula(s))
						continue;
					weight = sLex.getCorrectedWeight(s, sId, l);
					
					for(String t : targetNames)
					{
						if(StringParser.isFormula(t))
							continue;
						sim = weight * tLex.getCorrectedWeight(t, tId, l);
						sim *= stringSimilarity(s,t);
						if(sim > maxSim)
							maxSim = sim;
					}
				}
			}
		}
		else
		{
			Set<String> sourceNames = sLex.getNames(sId);
			Set<String> targetNames = tLex.getNames(tId);
			if(sourceNames == null || targetNames == null)
				return maxSim;
			for(String s : sourceNames)
			{
				if(StringParser.isFormula(s))
					continue;
				weight = sLex.getCorrectedWeight(s, sId);
				
				for(String t : targetNames)
				{
					if(StringParser.isFormula(t))
						continue;
					sim = weight * tLex.getCorrectedWeight(t, tId);
					sim *= stringSimilarity(s,t);
					if(sim > maxSim)
						maxSim = sim;
				}
			}
		}
		return maxSim;
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