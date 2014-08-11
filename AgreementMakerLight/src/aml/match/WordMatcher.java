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
* Matches Ontologies by measuring the word similarity between their classes,  *
* using a (weighted) Jaccard index.                                           *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 11-08-2014                                                            *
* @version 2.0                                                                *
******************************************************************************/
package aml.match;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import aml.AML;
import aml.AML.WordMatchStrategy;
import aml.ontology.WordLexicon;
import aml.ontology.Ontology;
import aml.util.Similarity;
import aml.util.Table2;

public class WordMatcher implements PrimaryMatcher, Rematcher
{

//Attributes
	
	private final int MAX_BLOCK_SIZE = 10000;
	private WordLexicon sourceLex;
	private WordLexicon targetLex;
	private boolean useWeighting;
	private WordMatchStrategy strategy;
	
//Constructors
	
	/**
	 * Constructs a new WordMatcher with default options
	 */
	public WordMatcher()
	{
		AML aml = AML.getInstance();
		sourceLex = new WordLexicon(aml.getSource().getLexicon());
		targetLex = new WordLexicon(aml.getTarget().getLexicon());
		useWeighting = true;
		strategy = WordMatchStrategy.AVERAGE;
	}
	
	/**
	 * Constructs a new WordMatcher with the given options
	 * @param w: whether to use Evidence Content weights
	 * or perform a simple Jaccard index
	 * @param s: the WordMatchStrategy to use
	 */
	public WordMatcher(boolean w, WordMatchStrategy s)
	{
		AML aml = AML.getInstance();
		sourceLex = new WordLexicon(aml.getSource().getLexicon());
		targetLex = new WordLexicon(aml.getTarget().getLexicon());
		useWeighting = w;
		strategy = s;
	}
	
//Public Methods
	
	@Override
	public Alignment match(double thresh)
	{
		System.out.println("Running WordMatcher");
		long time = System.currentTimeMillis()/1000;
		Alignment a = new Alignment();
		//Get the Ontologies
		AML aml = AML.getInstance();
		Ontology source = aml.getSource();
		Ontology target = aml.getTarget();
		//Break them into chunks according to MAX_BLACK_SIZE
		Iterator<Integer> sourceClasses = source.getClasses().iterator();
		Iterator<Integer> targetClasses = target.getClasses().iterator();
		Table2<Integer,Integer> sourcesToMap = new Table2<Integer,Integer>();
		for(int i = 0; sourceClasses.hasNext(); i++)
			sourcesToMap.add(i/MAX_BLOCK_SIZE, sourceClasses.next());
		Table2<Integer,Integer> targetsToMap = new Table2<Integer,Integer>();
		for(int i = 0; targetClasses.hasNext(); i++)
			targetsToMap.add(i/MAX_BLOCK_SIZE, targetClasses.next());
		//Match all chunks
		System.out.println("Blocks to match: " + sourcesToMap.keyCount()*targetsToMap.keyCount());
		int count = 0;
		for(Integer i : sourcesToMap.keySet())
		{
			HashSet<Integer> sources = new HashSet<Integer>(sourcesToMap.get(i));
			Table2<String,Integer> sWLex = sourceLex.getWordTable(sources);
			for(Integer j : targetsToMap.keySet())
			{
				HashSet<Integer> targets = new HashSet<Integer>(targetsToMap.get(j));
				Table2<String,Integer> tWLex = targetLex.getWordTable(targets);
				a.addAll(matchWordLexicons(sWLex,tWLex,thresh));
				count++;
				if(count%10 == 0)
					System.out.println(count*10 + "%");
			}
		}
		time = System.currentTimeMillis()/1000 - time;
		System.out.println("Finished in " + time + " seconds");
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
	
	//Maps two classes according to the set word match strategy
	private Mapping mapTwoClasses(int sourceId, int targetId)
	{
		//If the strategy is not by name, compute the class similarity
		double classSim = 0.0;
		if(!strategy.equals(WordMatchStrategy.BY_NAME))
		{
			classSim = classSimilarity(sourceId,targetId);
			//If the class similarity is very low, return the mapping
			//so as not to waste time computing name similarity
			if(classSim < 0.25)
				return new Mapping(sourceId,targetId,classSim);
		}
		//If the strategy is not by class, compute the name similarity
		double nameSim = 0.0;
		if(!strategy.equals(WordMatchStrategy.BY_CLASS))
		{
			//Which the maximum similarity between all names of both classes
			double sim, weight;
			Set<String> sourceNames = sourceLex.getNames(sourceId);
			Set<String> targetNames = targetLex.getNames(targetId);
			for(String s : sourceNames)
			{
				weight = sourceLex.getNameWeight(s,sourceId);
				for(String t : targetNames)
				{
					sim = weight * targetLex.getNameWeight(t, targetId);
					sim *= nameSimilarity(s,t);
					if(sim > nameSim)
						nameSim = sim;
				}
			}
		}
		//Combine the similarities according to the strategy
		double sim = 0.0;
		if(strategy.equals(WordMatchStrategy.BY_NAME))
			sim = nameSim;
		else if(strategy.equals(WordMatchStrategy.BY_CLASS))
			sim = classSim;
		else if(strategy.equals(WordMatchStrategy.AVERAGE))
			sim = Math.sqrt(nameSim * classSim);
		else if(strategy.equals(WordMatchStrategy.MAXIMUM))
			sim = Math.max(nameSim,classSim);
		else if(strategy.equals(WordMatchStrategy.MINIMUM))
			sim = Math.min(nameSim,classSim);
		//Return the mapping with the combined similarity
		return new Mapping(sourceId,targetId,sim);
	}

	//Computes the word-based (bag-of-words) similarity between two classes
	private double classSimilarity(int sourceId, int targetId)
	{
		Set<String> sourceWords = sourceLex.getWords(sourceId);
		Set<String> targetWords = targetLex.getWords(targetId);
		if(!useWeighting)
			return Similarity.jaccard(sourceWords, targetWords);
		double intersection = 0.0;
		double union = sourceLex.getClassEC(sourceId) + 
				targetLex.getClassEC(targetId);
		for(String w : sourceWords)
		{
			double weight = sourceLex.getWordEC(w) * sourceLex.getWordWeight(w,sourceId);
			if(targetWords.contains(w))
				intersection += Math.sqrt(weight * targetLex.getWordEC(w) *
						targetLex.getWordWeight(w,targetId));
		}			
		union -= intersection;
		return intersection / union;
	}
	
	//Computes the maximum word-based (bag-of-words) similarity between two names
	private double nameSimilarity(String s, String t)
	{
		Set<String> sourceWords = sourceLex.getWords(s);
		Set<String> targetWords = targetLex.getWords(t);
		if(!useWeighting)
			return Similarity.jaccard(sourceWords, targetWords);
		double intersection = 0.0;
		double union = sourceLex.getNameEC(s) + targetLex.getNameEC(t);
		for(String w : sourceWords)
			if(targetWords.contains(w))
				intersection += Math.sqrt(sourceLex.getWordEC(w) * targetLex.getWordEC(w));
		union -= intersection;
		return intersection/union;
	}
	
	//Matches two word tables finding all class pairs that share a word
	//then measuring their word similarity
	private Alignment matchWordLexicons(Table2<String,Integer> sWLex,
			Table2<String,Integer> tWLex, double thresh)
	{
		//The alignment to return
		Alignment maps = new Alignment();
		//The list of computed mappings, to avoid repeated computations
		Table2<Integer,Integer> computedMappings = new Table2<Integer,Integer>();
		//To minimize iterations, we want to iterate through the smallest table
		Table2<String,Integer> larger, smaller;
		boolean sourceIsSmaller = (sWLex.keyCount() <= tWLex.keyCount());
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
		//Get the smaller table's words
		Set<String> words = smaller.keySet();
		for(String s : words)
		{
			//Get all classes with the word in both tables
			Vector<Integer> largerIndexes = larger.get(s);
			Vector<Integer> smallerIndexes = smaller.get(s);
			if(largerIndexes == null)
				continue;
			//Then match all classes
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
					//Unless they have already been matched
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