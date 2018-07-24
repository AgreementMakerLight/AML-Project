/******************************************************************************
* Copyright 2013-2018 LASIGE                                                  *
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
package aml.match.lexical;

import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import aml.AML;
import aml.alignment.AbstractMapping;
import aml.alignment.SimpleAlignment;
import aml.alignment.SimpleMapping;
import aml.match.AbstractParallelMatcher;
import aml.ontology.EntityType;
import aml.ontology.Ontology;
import aml.ontology.lexicon.WordLexicon;
import aml.settings.InstanceMatchingCategory;
import aml.util.data.Map2MapComparable;
import aml.util.data.Map2Set;

public class WordMatcher extends AbstractParallelMatcher
{

//Attributes
	
	protected static final String DESCRIPTION = "Matches entities by checking for words\n" +
			  								  "they share in their Lexicon entries.\n" +
			  								  "Computes word similarity by entity, by\n" +
			  								  "by entry, or combined";
	protected static final String NAME = "Word Matcher";
	protected static final EntityType[] SUPPORT = {EntityType.CLASS,EntityType.INDIVIDUAL,EntityType.DATA_PROP,EntityType.OBJECT_PROP};
	private WordLexicon sourceLex;
	private WordLexicon targetLex;
	private WordMatchStrategy strategy = WordMatchStrategy.AVERAGE;
	private String language;

//Constructors
	
	/**
	 * Constructs a new WordMatcher for the given language
	 * @param lang: the language on which to match Ontologies
	 */
	public WordMatcher(String lang)
	{
		language = lang;
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
	public SimpleAlignment extendAlignment(Ontology o1, Ontology o2, SimpleAlignment a, EntityType e, double thresh)
	{
		SimpleAlignment b = match(o1,o2,e,thresh);
		HashSet<AbstractMapping> toRemove = new HashSet<AbstractMapping>();
		for(AbstractMapping m : b)
			if(a.containsConflict(m))
				toRemove.add(m);
		b.removeAll(toRemove);
		return b;				
	}
	
	@Override
	public SimpleAlignment match(Ontology o1, Ontology o2, EntityType e, double thresh)
	{
		SimpleAlignment a = new SimpleAlignment();
		if(!checkEntityType(e))
			return a;
		AML aml = AML.getInstance();
		System.out.println("Building Word Lexicons");
		long time = System.currentTimeMillis()/1000;
		System.out.println("Language: " + language);
		sourceLex = aml.getSource().getWordLexicon(e,language);
		targetLex = aml.getTarget().getWordLexicon(e,language);

		System.out.println("Running Word Matcher");
		
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
			//The word table (words->String, class indexes->String) for the current block
			Map2Set<String,String> sWLex = sourceLex.getWordTable(i);
			for(int j = 0; j < targetLex.blockCount(); j++)
			{
				//The word table (words->String, class indexes->String) for the current block
				Map2Set<String,String> tWLex = targetLex.getWordTable(j);
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
						double nameSim = classNameSimilarity((String)m.getEntity1(),(String)m.getEntity2());
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
							a.add((String)m.getEntity1(),(String)m.getEntity2(),sim);
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
	public SimpleAlignment rematch(Ontology o1, Ontology o2, SimpleAlignment a, EntityType e)
	{
		System.out.println("Building Word Lexicons");
		long time = System.currentTimeMillis()/1000;
		System.out.println("Language: " + language);
		sourceLex = o1.getWordLexicon(e,language);
		targetLex = o2.getWordLexicon(e,language);
		System.out.println("Computing Word Similarity");
		SimpleAlignment maps = super.rematch(o1, o2, a, e);
		time = System.currentTimeMillis()/1000 - time;
		System.out.println("Finished in " + time + " seconds");
		return maps;
	}
	
//Protected Methods	

	//Maps two classes according to the selected strategy.
	protected double mapTwoEntities(String sourceId, String targetId)
	{
		//If the strategy is not by name, compute the class similarity
		double classSim = 0.0;
		if(!strategy.equals(WordMatchStrategy.BY_NAME))
		{
			classSim = classSimilarity(sourceId,targetId);
			//If the class similarity is very low, return the mapping
			//so as not to waste time computing name similarity
			if(classSim < 0.25)
				return classSim;
		}
		//If the strategy is not by class, compute the name similarity
		double nameSim = 0.0;
		if(!strategy.equals(WordMatchStrategy.BY_CLASS))
			nameSim = classNameSimilarity(sourceId,targetId);
		
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
		return sim;
	}
	
//Private Methods
	
	//Computes the word-based (bag-of-words) similarity between two
	//classes, for use by rematch()
	private double classSimilarity(String sourceId, String targetId)
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
	
	//Computes the maximum word-based (bag-of-words) similarity between
	//two classes' names, for use by both match() and rematch()
	private double classNameSimilarity(String sourceId, String targetId)
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
	
	//Matches two WordLexicon blocks by class.
	//Used by match() method either to compute the final BY_CLASS alignment
	//or to compute a preliminary alignment which is then refined according
	//to the WordMatchStrategy.
	private Vector<SimpleMapping> matchBlocks(Map2Set<String,String> sWLex,
			Map2Set<String,String> tWLex, EntityType e, double thresh)
	{
		AML aml = AML.getInstance();
		Map2MapComparable<String,String,Double> maps = new Map2MapComparable<String,String,Double>();
		//To minimize iterations, we want to iterate through the smallest Lexicon
		boolean sourceIsSmaller = (sWLex.keyCount() <= tWLex.keyCount());
		Set<String> words;
		if(sourceIsSmaller)
			words = sWLex.keySet();
		else
			words = tWLex.keySet();
		
		for(String s : words)
		{
			Set<String> sourceIndexes = sWLex.get(s);
			Set<String> targetIndexes = tWLex.get(s);
			if(sourceIndexes == null || targetIndexes == null)
				continue;
			double ec = sourceLex.getWordEC(s) * targetLex.getWordEC(s);
			for(String i : sourceIndexes)
			{
				if(e.equals(EntityType.INDIVIDUAL) && !aml.isToMatchSource(i))
					continue;
				double sim = ec * sourceLex.getWordWeight(s,i);
				for(String j : targetIndexes)
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
		Set<String> sources = maps.keySet();
		Vector<SimpleMapping> a = new Vector<SimpleMapping>();
		for(String i : sources)
		{
			Set<String> targets = maps.keySet(i);
			for(String j : targets)
			{
				double sim = maps.get(i,j);
				sim /= sourceLex.getEntityEC(i) + targetLex.getEntityEC(j) - sim;
				if(sim >= thresh)
					a.add(new SimpleMapping(i, j, sim));
			}
		}
		return a;
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