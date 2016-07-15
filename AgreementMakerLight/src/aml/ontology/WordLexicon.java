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
* The lexicon of words in an Ontology, which is derived from its Lexicon and  *
* used by the WordMatcher.                                                    *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.ontology;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import aml.settings.EntityType;
import aml.settings.LexicalType;
import aml.util.StopList;
import aml.util.Table2Set;
import aml.util.Table2Map;

public class WordLexicon
{

//Attributes

	//The maximum size of class blocks
	private final int MAX_BLOCK_SIZE = 10000;
	//A link to the original Lexicon
	private Lexicon lex;
	//The list of stop words to ignore when building this WordLexicon
	private Set<String> stopSet;
	//The EntityType and language to use when building this WordLexicon
	private EntityType type;
	private String language;
	//The map of words (String) to entities (Integer) divided in blocks
	private Table2Set<String,Integer>[] wordEntities;
	//The map of entities (Integer) to words (String) with weights (Double)
	private Table2Map<Integer,String,Double> entityWords;
	//The map of names (String) to words (String)
	private Table2Set<String,String> nameWords;
	//The map of word (String) evidence contents (Double)
	private HashMap<String,Double> wordECs;
	//The map of entities (Integer) to total evidence contents (Double), which are
	//the sum of evidence contents of all their words (multiplied by frequency)
	private HashMap<Integer,Double> entitiesECs;
	//The map of name (String) evidence contents (Double), which is the sum
	//of evidence contents of all its words (multiplied by frequency)
	private HashMap<String,Double> nameECs;
	//Auxiliary count of words entered into the WordLexicon
	private int total;
	
//Constructors

	/**
	 * Constructs a new WordLexicon from the given Lexicon
	 * @param l: the Lexicon from which the WordLexicon is derived
	 * @param e: the EntityType for which to construct the WordLexicon
	 */
	public WordLexicon(Lexicon l, EntityType e)
	{
		lex = l;
		type = e;
		language = "";
		init();
	}
	
	/**
	 * Constructs a new WordLexicon from the given Lexicon
	 * @param l: the Lexicon from which the WordLexicon is derived
	 * @param e: the EntityType for which to construct the WordLexicon
	 * @param lang: the language to use for this WordLexicon
	 */
	public WordLexicon(Lexicon l, EntityType e, String lang)
	{
		lex = l;
		type = e;
		language = lang;
		init();
	}
	
//Public Methods

	/**
	 * @return the number of blocks in the WordLexicon
	 */
	public int blockCount()
	{
		return wordEntities.length;
	}
	
	/**
	 * @param index: the index of the entity to search in the WordLexicon
	 * @return the EC of the given entity
	 */
	public double getEntityEC(int index)
	{
		if(entitiesECs.containsKey(index))
			return entitiesECs.get(index);
		return -1.0;
	}
	
	/**
	 * @return the set of entities in the WordLexicon
	 */
	public Set<Integer> getEntities()
	{
		return entityWords.keySet();
	}
	
	/**
	 * @return the language used to build this WordLexicon
	 */
	public String getLanguage()
	{
		return language;
	}
	
	/**
	 * @param n: the name to search in the WordLexicon
	 * @return the EC of the given name
	 */
	public double getNameEC(String n)
	{
		if(nameECs.containsKey(n))
			return nameECs.get(n);
		return -1.0;
	}
	
	/**
	 * @return the set of names in the WordLexicon
	 */
	public Set<String> getNames()
	{
		return nameWords.keySet();
	}

	/**
	 * @return the set of names for the given classId
	 */
	public Set<String> getNames(int classId)
	{
		HashSet<String> names = new HashSet<String>();
		for(String n : lex.getNames(classId))
			if(nameWords.contains(n))
				names.add(n);
		return names;
	}
	
	/**
	 * @param name: the name to search in the WordLexicon
	 * @param classId: the class to search in the WordLexicon
	 * @return the weight of the name for the class in the WordLexicon
	 */
	public double getNameWeight(String name, int classId)
	{
		return lex.getCorrectedWeight(name,classId);
	}
	
	/**
	 * @return the EntityType for which this WordLexicon was built
	 */
	public EntityType getType()
	{
		return type;
	}
	
	/**
	 * @param w: the word to search in the WordLexicon
	 * @return the EC of the given word
	 */
	public double getWordEC(String w)
	{
		if(wordECs.containsKey(w))
			return wordECs.get(w);
		return -1.0;
	}

	/**
	 * @return the set of words for the given classId
	 */
	public Set<String> getWords(int classId)
	{
		if(!entityWords.contains(classId))
			return new HashSet<String>();
		return entityWords.keySet(classId);
	}
	
	/**
	 * @return the set of words for the given classId
	 */
	public Set<String> getWords(String name)
	{
		if(!nameWords.contains(name))
			return new HashSet<String>();
		return new HashSet<String>(nameWords.get(name));
	}
	
	/**
	 * @return the table of words for a given block of classes
	 */
	public Table2Set<String,Integer> getWordTable(int block)
	{
		return wordEntities[block];
	}
	
	/**
	 * @param word: the word to search in the WordLexicon
	 * @param classId: the class to search in the WordLexicon
	 * @return the weight of the word for the class in the WordLexicon
	 */
	public double getWordWeight(String word, int classId)
	{
		if(!entityWords.contains(classId, word))
			return -1.0;
		return entityWords.get(classId, word);
	}
	
//Private methods
	
	//Builds the WordLexicon from the original Lexicon
	@SuppressWarnings("unchecked")
	private void init()
	{
		//Initialize the data structures
		stopSet = StopList.read();
		int size = (int)Math.ceil(1.0*lex.entityCount(type)/MAX_BLOCK_SIZE);
		wordEntities = new Table2Set[size];
		for(int i = 0; i < wordEntities.length; i++)
			wordEntities[i] = new Table2Set<String,Integer>();
		entityWords = new Table2Map<Integer,String,Double>();
		nameWords = new Table2Set<String,String>();
		wordECs = new HashMap<String,Double>();
		entitiesECs = new HashMap<Integer,Double>();
		nameECs = new HashMap<String,Double>();
		total = 0;
		//Get the classes from the Lexicon
		Set<Integer> entities = lex.getEntities(type);
		//For each entity
		for(Integer e: entities)
		{
			//Get all names 
			Set<String> names;
			if(language.equals(""))
				names = lex.getNames(e);
			else
				names = lex.getNamesWithLanguage(e, language);
			if(names == null)
				continue;
			//And add the words for each name 
			for(String n: names)
				if(!lex.getTypes(n,e).contains(LexicalType.FORMULA))
					addWords(n, e);
		}
		//Compute the maximum EC
		double max = Math.log(total);
		//Compute and store the normalized EC for
		//each word in the WordLexicon
		for(String w : wordECs.keySet())
		{
			double ec = 1 - (Math.log(wordECs.get(w)) / max);
			wordECs.put(w, ec);
		}
		//The total EC for each class
		for(Integer i : entityWords.keySet())
		{
			double ec = 0.0;
			for(String w : entityWords.keySet(i))
				ec += wordECs.get(w) * getWordWeight(w, i);
			entitiesECs.put(i, ec);
		}
		//And the total EC for each name
		for(String n : nameWords.keySet())
		{
			double ec = 0.0;
			for(String w : nameWords.get(n))
				ec += wordECs.get(w);
			nameECs.put(n, ec);
		}
	}
			
	//Adds all words for a given name and classId
	private void addWords(String name, int classId)
	{
		String[] words = name.split(" ");
		for(String w : words)
		{
			String word = w.replaceAll("[()]", "");
			if(stopSet.contains(word) || word.length() < 2 || !word.matches(".*[a-zA-Z].*"))
				continue;
			//Get the current block number (as determined by the number of classes already loaded)
			int block = entityWords.keySet().size()/MAX_BLOCK_SIZE;
			//Add the block-word-class triple
			wordEntities[block].add(word,classId);
			//Update the current weight of the word for the classId
			Double weight = entityWords.get(classId,word);
			if(weight == null)
				weight = lex.getCorrectedWeight(name, classId);
			else
				weight += lex.getCorrectedWeight(name, classId);
			//Add the class-word-weight triple
			entityWords.add(classId, word, weight);
			//Add the name-word pair
			nameWords.add(name, word);
			//Update the word frequency
			Double freq = wordECs.get(word);
			if(freq == null)
				freq = 1.0;
			else
				freq++;
			wordECs.put(word,freq);
			//Update the total;
			total++;
		}
	}
}