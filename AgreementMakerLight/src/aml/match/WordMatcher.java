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
* Matches Ontologies by measuring the word similarity between their classes,  *
* using a weighted Jaccard index.                                             *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.match;

import java.util.Set;
import java.util.Vector;

import aml.AML;
import aml.alignment.Alignment;
import aml.alignment.SimpleMapping;
import aml.ontology.EntityType;
import aml.ontology.lexicon.WordLexicon;
import aml.settings.InstanceMatchingCategory;
import aml.settings.WordMatchStrategy;
import aml.util.data.Map2MapComparable;
import aml.util.data.Map2Set;

public class WordMatcher implements PrimaryMatcher, Rematcher
{

//Attributes
	
	private static final String DESCRIPTION = "Matches entities by checking for words\n" +
			  								  "they share in their Lexicon entries.\n" +
			  								  "Computes word similarity by entity, by\n" +
			  								  "by entry, or combined";
	private static final String NAME = "Word Matcher";
	private static final EntityType[] SUPPORT = {EntityType.CLASS,EntityType.INDIVIDUAL,EntityType.DATA_PROP,EntityType.OBJECT_PROP};
	private WordLexicon sourceLex;
	private WordLexicon targetLex;
	private WordMatchStrategy strategy = WordMatchStrategy.AVERAGE;
	private String language;

//Constructors
	
	/**
	 * Constructs a new WordMatcher with default options
	 */
	public WordMatcher()
	{
		language = "";
	}
	
	/**
	 * Constructs a new WordMatcher for the given language
	 * @param lang: the language on which to match Ontologies
	 */
	public WordMatcher(String lang)
	{
		language = lang;
	}
	
	/**
	 * Constructs a new WordMatcher with the given strategy
	 * @param s: the WordMatchStrategy to use
	 */
	public WordMatcher(WordMatchStrategy s)
	{
		this();
		strategy = s;
	}
	
	/**
	 * Constructs a new WordMatcher for the given language
	 * @param lang: the language on which to match Ontologies
	 * @param s: the WordMatchStrategy to use
	 */
	public WordMatcher(String lang, WordMatchStrategy s)
	{
		this(lang);
		strategy = s;
	}
	
//Public Methods
	
	@Override
	public String getDescription()
	{
		return DESCRIPTION;
	}

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public EntityType[] getSupportedEntityTypes()
	{
		return SUPPORT;
	}
	
	@Override
	public Alignment match(EntityType e, double thresh) throws UnsupportedEntityTypeException
	{
		checkEntityType(e);
		AML aml = AML.getInstance();
		System.out.println("Building Word Lexicons");
		long time = System.currentTimeMillis()/1000;
		if(!language.isEmpty())
		{
			System.out.println("Language: " + language);
			sourceLex = aml.getSource().getWordLexicon(e,language);
			targetLex = aml.getTarget().getWordLexicon(e,language);
		}
		else
		{
			sourceLex = aml.getSource().getWordLexicon(e);
			targetLex = aml.getTarget().getWordLexicon(e);
		}
		System.out.println("Running Word Matcher");
		Alignment a = new Alignment();
		//If the strategy is BY_CLASS, the alignment can be computed
		//globally. Otherwise we need to compute a preliminary
		//alignment and then rematch according to the strategy.
		double t;
		if(strategy.equals(WordMatchStrategy.BY_CLASS))
			t = thresh;
		else
			t = thresh * 0.5;
		//Global matching is done by chunks so as not to overload the memory
		System.out.println("Blocks to match: " + sourceLex.blockCount() +
				"x" + targetLex.blockCount());
		//Match each chunk of both WordLexicons
		for(int i = 0; i < sourceLex.blockCount(); i++)
		{
			//The word table (words->String, class indexes->Integer) for the current block
			Map2Set<String,Integer> sWLex = sourceLex.getWordTable(i);
			for(int j = 0; j < targetLex.blockCount(); j++)
			{
				//The word table (words->String, class indexes->Integer) for the current block
				Map2Set<String,Integer> tWLex = targetLex.getWordTable(j);
				Vector<SimpleMapping> temp = matchBlocks(sWLex,tWLex,e,t);
				//If the strategy is BY_CLASS, just add the alignment
				if(strategy.equals(WordMatchStrategy.BY_CLASS))
					a.addAll(temp);
				//Otherwise, update the similarity according to the strategy
				else
				{
					for(SimpleMapping m : temp)
					{
						//First compute the name similarity
						double nameSim = nameSimilarity(m.getSourceId(),m.getTargetId());
						//Then update the final similarity according to the strategy
						double sim = m.getSimilarity();
						if(strategy.equals(WordMatchStrategy.BY_NAME))
							sim = nameSim;
						else if(strategy.equals(WordMatchStrategy.AVERAGE))
							sim = Math.sqrt(nameSim * sim);
						else if(strategy.equals(WordMatchStrategy.MAXIMUM))
							sim = Math.max(nameSim,sim);
						else if(strategy.equals(WordMatchStrategy.MINIMUM))
							sim = Math.min(nameSim,sim);
						if(sim >= thresh)
							a.add(m.getSourceId(),m.getTargetId(),sim);
					}
				}
				System.out.print(".");
			}
			System.out.println();
		}
		time = System.currentTimeMillis()/1000 - time;
		System.out.println("Finished in " + time + " seconds");
		return a;
	}
	
	@Override
	public Alignment rematch(Alignment a, EntityType e) throws UnsupportedEntityTypeException
	{
		checkEntityType(e);
		AML aml = AML.getInstance();
		System.out.println("Building Word Lexicons");
		long time = System.currentTimeMillis()/1000;
		if(!language.isEmpty())
		{
			System.out.println("Language: " + language);
			sourceLex = aml.getSource().getWordLexicon(e,language);
			targetLex = aml.getTarget().getWordLexicon(e,language);
		}
		else
		{
			sourceLex = aml.getSource().getWordLexicon(e);
			targetLex = aml.getTarget().getWordLexicon(e);
		}		
		System.out.println("Computing Word Similarity");
		Alignment maps = new Alignment();
		for(SimpleMapping m : a)
		{
			if(aml.getURIMap().getTypes(m.getSourceId()).equals(e))
				maps.add(mapTwoClasses(m.getSourceId(),m.getTargetId()));
		}
		time = System.currentTimeMillis()/1000 - time;
		System.out.println("Finished in " + time + " seconds");
		return maps;
	}
	
//Private Methods
	
	private void checkEntityType(EntityType e) throws UnsupportedEntityTypeException
	{
		boolean check = false;
		for(EntityType t : SUPPORT)
		{
			if(t.equals(e))
			{
				check = true;
				break;
			}
		}
		if(!check)
			throw new UnsupportedEntityTypeException(e.toString());
	}
	
	//Computes the word-based (bag-of-words) similarity between two
	//classes, for use by rematch()
	private double classSimilarity(int sourceId, int targetId)
	{
		Set<String> sourceWords = sourceLex.getWordsByEntity(sourceId);
		Set<String> targetWords = targetLex.getWordsByEntity(targetId);
		double intersection = 0.0;
		double union = sourceLex.getEntityEC(sourceId) + 
				targetLex.getEntityEC(targetId);
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
	
	//Matches two WordLexicon blocks by class.
	//Used by match() method either to compute the final BY_CLASS alignment
	//or to compute a preliminary alignment which is then refined according
	//to the WordMatchStrategy.
	private Vector<SimpleMapping> matchBlocks(Map2Set<String,Integer> sWLex,
			Map2Set<String,Integer> tWLex, EntityType e, double thresh)
	{
		AML aml = AML.getInstance();
		Map2MapComparable<Integer,Integer,Double> maps = new Map2MapComparable<Integer,Integer,Double>();
		//To minimize iterations, we want to iterate through the smallest Lexicon
		boolean sourceIsSmaller = (sWLex.keyCount() <= tWLex.keyCount());
		Set<String> words;
		if(sourceIsSmaller)
			words = sWLex.keySet();
		else
			words = tWLex.keySet();
		
		for(String s : words)
		{
			Set<Integer> sourceIndexes = sWLex.get(s);
			Set<Integer> targetIndexes = tWLex.get(s);
			if(sourceIndexes == null || targetIndexes == null)
				continue;
			double ec = sourceLex.getWordEC(s) * targetLex.getWordEC(s);
			for(Integer i : sourceIndexes)
			{
				if(e.equals(EntityType.INDIVIDUAL) && !aml.isToMatchSource(i))
					continue;
				double sim = ec * sourceLex.getWordWeight(s,i);
				for(Integer j : targetIndexes)
				{
					if(e.equals(EntityType.INDIVIDUAL) && (!aml.isToMatchTarget(j) ||
							(aml.getInstanceMatchingCategory().equals(InstanceMatchingCategory.SAME_CLASSES) &&
							!aml.getEntityMap().shareClass(i,j))))
						continue;
					double finalSim = Math.sqrt(sim * targetLex.getWordWeight(s,j));
					Double previousSim = maps.get(i,j);
					if(previousSim == null)
						previousSim = 0.0;
					finalSim += previousSim;
					maps.add(i,j,finalSim);
				}
			}
		}
		Set<Integer> sources = maps.keySet();
		Vector<SimpleMapping> a = new Vector<SimpleMapping>();
		for(Integer i : sources)
		{
			Set<Integer> targets = maps.keySet(i);
			for(Integer j : targets)
			{
				double sim = maps.get(i,j);
				sim /= sourceLex.getEntityEC(i) + targetLex.getEntityEC(j) - sim;
				if(sim >= thresh)
					a.add(new SimpleMapping(i, j, sim));
			}
		}
		return a;
	}
	
	//Maps two classes according to the selected strategy.
	//Used by rematch() only.
	private SimpleMapping mapTwoClasses(int sourceId, int targetId)
	{
		//If the strategy is not by name, compute the class similarity
		double classSim = 0.0;
		if(!strategy.equals(WordMatchStrategy.BY_NAME))
		{
			classSim = classSimilarity(sourceId,targetId);
			//If the class similarity is very low, return the mapping
			//so as not to waste time computing name similarity
			if(classSim < 0.25)
				return new SimpleMapping(sourceId,targetId,classSim);
		}
		//If the strategy is not by class, compute the name similarity
		double nameSim = 0.0;
		if(!strategy.equals(WordMatchStrategy.BY_CLASS))
			nameSim = nameSimilarity(sourceId,targetId);
		
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
		return new SimpleMapping(sourceId,targetId,sim);
	}

	//Computes the maximum word-based (bag-of-words) similarity between
	//two classes' names, for use by both match() and rematch()
	private double nameSimilarity(int sourceId, int targetId)
	{
		double nameSim = 0;
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
		return nameSim;
	}
	
	//Computes the word-based (bag-of-words) similarity between two names
	private double nameSimilarity(String s, String t)
	{
		Set<String> sourceWords = sourceLex.getWordsByName(s);
		Set<String> targetWords = targetLex.getWordsByName(t);
		double intersection = 0.0;
		double union = sourceLex.getNameEC(s) + targetLex.getNameEC(t);
		for(String w : sourceWords)
			if(targetWords.contains(w))
				intersection += Math.sqrt(sourceLex.getWordEC(w) * targetLex.getWordEC(w));
		union -= intersection;
		return intersection/union;
	}
}