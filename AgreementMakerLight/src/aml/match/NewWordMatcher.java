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
* Matches Ontologies by measuring the global word similarity between their    *
* classes, using a weighted Jaccard index.                                    *
* NOTE: This matching algorithm requires O(N^2) memory in the worst case and  *
* thus should not be used with very large Ontologies unless adequate memory   *
* is available.                                                               *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 31-07-2014                                                            *
* @version 2.0                                                                *
******************************************************************************/
package aml.match;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import aml.AML;
import aml.AML.SizeCategory;
import aml.ontology.AuxiliaryWordLexicon;
import aml.ontology.NewWordLexicon;
import aml.ontology.Ontology;
import aml.util.Table2;

public class NewWordMatcher implements PrimaryMatcher, Rematcher
{

//Attributes
	
	private final int MAX_BLOCK_SIZE = 10000;
	private NewWordLexicon sourceLex;
	private NewWordLexicon targetLex;
	
//Constructors
	
	/**
	 * Constructs a new WordMatcher
	 */
	public NewWordMatcher()
	{
		AML aml = AML.getInstance();
		sourceLex = new NewWordLexicon(aml.getSource().getLexicon());
		targetLex = new NewWordLexicon(aml.getTarget().getLexicon());
	}
	
//Public Methods
	
	@Override
	public Alignment match(double thresh)
	{
		Alignment a = new Alignment();
		AML aml = AML.getInstance();
		Ontology source = aml.getSource();
		Ontology target = aml.getTarget();
		if(aml.getSizeCategory().equals(SizeCategory.LARGE))
		{
			Iterator<Integer> sourceClasses = source.getClasses().iterator();
			Iterator<Integer> targetClasses = target.getClasses().iterator();
			
			Table2<Integer,Integer> sourcesToMap = new Table2<Integer,Integer>();
			for(int i = 0; sourceClasses.hasNext(); i++)
				sourcesToMap.add(i/MAX_BLOCK_SIZE, sourceClasses.next());
			
			Table2<Integer,Integer> targetsToMap = new Table2<Integer,Integer>();
			for(int i = 0; targetClasses.hasNext(); i++)
				targetsToMap.add(i/MAX_BLOCK_SIZE, targetClasses.next());
			
			System.out.println(sourcesToMap.keyCount()*targetsToMap.keyCount());
			int count = 0;
			for(Integer i : sourcesToMap.keySet())
			{
				HashSet<Integer> sources = new HashSet<Integer>(sourcesToMap.get(i));
				AuxiliaryWordLexicon sWLex = new AuxiliaryWordLexicon(source.getLexicon(),sources);
				for(Integer j : targetsToMap.keySet())
				{
					HashSet<Integer> targets = new HashSet<Integer>(targetsToMap.get(j));
					AuxiliaryWordLexicon tWLex = new AuxiliaryWordLexicon(target.getLexicon(),targets);
					a.addAll(matchWordLexicons(sWLex,tWLex,thresh));
					System.out.println(++count);
				}
			}
		}
		else
		{
			AuxiliaryWordLexicon sWLex = new AuxiliaryWordLexicon(source.getLexicon());
			AuxiliaryWordLexicon tWLex = new AuxiliaryWordLexicon(target.getLexicon());
			a.addAll(matchWordLexicons(sWLex,tWLex,thresh));
		}
		return a;
	}
	
	@Override
	public Alignment rematch(Alignment a)
	{
		Alignment maps = new Alignment();
		for(Mapping m : a)
			maps.add(mapTwoClasses(m.getSourceId(),m.getTargetId()));
		return maps;
	}
	
//Private
	
	private Mapping mapTwoClasses(int sourceId, int targetId)
	{
		double maxSim = 0.0;
		double sim, weight;
		Set<String> sourceNames = sourceLex.getNames(sourceId);
		Set<String> targetNames = targetLex.getNames(targetId);
		for(String s : sourceNames)
		{
			weight = sourceLex.getWeight(s,sourceId);
			for(String t : targetNames)
			{
				sim = weight * targetLex.getWeight(t, targetId);
				sim *= wordSimilarityFlat(s,t);
				if(sim > maxSim)
					maxSim = sim;
			}
		}
		maxSim = Math.max(maxSim,0.85*classSimilarityFlat(sourceId,targetId));
		return new Mapping(sourceId,targetId,maxSim);
	}

	private double classSimilarity(int sourceId, int targetId)
	{
		Set<String> sourceWords = sourceLex.getWords(sourceId);
		Set<String> targetWords = targetLex.getWords(targetId);
		double intersection = 0.0;
		double union = 0.0;
		for(String w : sourceWords)
		{
			union += sourceLex.getEC(w);
			if(targetWords.contains(w))
				intersection += Math.sqrt(sourceLex.getEC(w) * targetLex.getEC(w));
		}			
		for(String w : targetWords)
			union += targetLex.getEC(w);
		union *= 0.5;
		union -= intersection;
		return intersection / union;
	}
	
	private double classSimilarityFlat(int sourceId, int targetId)
	{
		Set<String> sourceWords = sourceLex.getWords(sourceId);
		Set<String> targetWords = targetLex.getWords(targetId);
		double intersection = 0.0;
		double union = sourceWords.size() + targetWords.size();
		for(String w : sourceWords)
			if(targetWords.contains(w))
				intersection++;
		union -= intersection;
		return intersection / union;
	}
	
	private double wordSimilarity(String s, String t)
	{
		Set<String> sourceWords = sourceLex.getWords(s);
		Set<String> targetWords = targetLex.getWords(t);
		double intersection = 0.0;
		double union = 0.0;
		for(String w : sourceWords)
		{
			union += sourceLex.getEC(w);
			if(targetWords.contains(w))
				intersection += Math.sqrt(sourceLex.getEC(w) * targetLex.getEC(w));
		}			
		for(String w : targetWords)
			union += targetLex.getEC(w);
		union *= 0.5;
		union -= intersection;
		return intersection/union;
	}
	
	private double wordSimilarityFlat(String s, String t)
	{
		Set<String> sourceWords = sourceLex.getWords(s);
		Set<String> targetWords = targetLex.getWords(t);
		double intersection = 0.0;
		double union = sourceWords.size() + targetWords.size();
		for(String w : sourceWords)
			if(targetWords.contains(w))
				intersection++;
		union -= intersection;
		return intersection/union;
	}

	//Matches two auxiliary word lexicons finding all class pairs that share a word
	private Alignment matchWordLexicons(AuxiliaryWordLexicon sWLex, AuxiliaryWordLexicon tWLex, double thresh)
	{
		Alignment maps = new Alignment();
		Table2<Integer,Integer> computedMappings = new Table2<Integer,Integer>();
		AuxiliaryWordLexicon larger, smaller;
		//To minimize iterations, we want to iterate through the smallest Lexicon
		boolean sourceIsSmaller = (sWLex.wordCount() <= tWLex.wordCount());
		if(sourceIsSmaller)
		{
			smaller = sWLex;
			larger = tWLex;
		}
		else
		{
			smaller = tWLex;
			larger = sWLex;
		}
		//Get the smaller ontology words
		Set<String> words = smaller.getWords();
		for(String s : words)
		{
			//Get all term indexes for the name in both ontologies
			Vector<Integer> largerIndexes = larger.getClasses(s);
			Vector<Integer> smallerIndexes = smaller.getClasses(s);
			if(largerIndexes == null)
				continue;
			//Then match all indexes
			for(Integer i : smallerIndexes)
			{
				for(Integer j : largerIndexes)
				{
					int sourceId, targetId;
					if(sourceIsSmaller)
					{
						sourceId = i;
						targetId = j;
					}
					else
					{
						sourceId = j;
						targetId = i;
					}
					if(computedMappings.contains(sourceId, targetId))
						continue;
					Mapping m = mapTwoClasses(sourceId,targetId);
					computedMappings.add(sourceId,targetId);
					if(m.getSimilarity()>=thresh)
						maps.add(m);
				}
			}
		}
		return maps;
	}
}