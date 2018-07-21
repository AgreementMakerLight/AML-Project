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
* The lexicon of words in an Ontology, which is derived from its Lexicon and  *
* used by the WordMatcher.                                                    *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.ontology.lexicon;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import aml.ontology.EntityType;
import aml.ontology.Ontology;
import aml.util.Table2Set;
import aml.util.Table2Map;

public class WordLexicon
{

//Attributes

	//The maximum size of class blocks
	private final int MAX_BLOCK_SIZE = 10000;
	//A link to the original Ontology
	private Ontology o;
	//The list of stop words to ignore when building this WordLexicon
	private Set<String> stopSet;
	//The EntityType and language to use when building this WordLexicon
	private EntityType type;
	private String language;
	//The map of words to entities divided in blocks
	private Table2Set<String,String>[] wordEntities;
	//The map of entities to words with weights
	private Table2Map<String,String,Double> entityWords;
	//The map of names to words
	private Table2Set<String,String> nameWords;
	//The map of word evidence contents
	private HashMap<String,Double> wordECs;
	//The map of entities to total evidence contents, which are the sum
	//of evidence contents of all their words (multiplied by frequency)
	private HashMap<String,Double> entityECs;
	//The map of name evidence contents, which is the sum of evidence
	//contents of all its words (multiplied by frequency)
	private HashMap<String,Double> nameECs;
	//Auxiliary count of words entered into the WordLexicon
	private int total;
	
//Constructors

	/**
	 * Constructs a new WordLexicon from the given Ontology
	 * @param o: the Ontology from which the WordLexicon is derived
	 * @param e: the EntityType for which to construct the WordLexicon
	 * @param lang: the language to use for this WordLexicon
	 */
	@SuppressWarnings("unchecked")
	public WordLexicon(Ontology o, EntityType e, String lang)
	{
		this.o = o;
		type = e;
		language = lang;
		//Initialize the data structures
		stopSet = StopList.read();
		int size = (int)Math.ceil(1.0*o.count(type)/MAX_BLOCK_SIZE);
		wordEntities = new Table2Set[size];
		for(int i = 0; i < wordEntities.length; i++)
			wordEntities[i] = new Table2Set<String,String>();
		entityWords = new Table2Map<String,String,Double>();
		nameWords = new Table2Set<String,String>();
		wordECs = new HashMap<String,Double>();
		entityECs = new HashMap<String,Double>();
		nameECs = new HashMap<String,Double>();
		total = 0;
		
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
	 * @param uri: the uri of the entity to search in the WordLexicon
	 * @return the EC of the given entity
	 */
	public double getEntityEC(String uri)
	{
		if(entityECs.containsKey(uri))
			return entityECs.get(uri);
		return -1.0;
	}
	
	/**
	 * @return the set of entities in the WordLexicon
	 */
	public Set<String> getEntities()
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
	 * @param uri: the uri of the entity to search in the WordLexicon
	 * @return the set of names for the given entity
	 */
	public Set<String> getNames(String uri)
	{
		HashSet<String> names = new HashSet<String>();
		for(String n : o.getLexicon().getNames(uri))
			if(nameWords.contains(n))
				names.add(n);
		return names;
	}
	
	/**
	 * @param name: the name to search in the WordLexicon
	 * @param uri: the uri of the entity to search in the WordLexicon
	 * @return the weight of the name for the class in the WordLexicon
	 */
	public double getNameWeight(String name, String uri)
	{
		return o.getLexicon().getCorrectedWeight(name,uri);
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
	 * @param uri: the uri of the entity to search in the WordLexicon
	 * @return the set of words for the given entity
	 */
	public Set<String> getWordsByEntity(String uri)
	{
		if(!entityWords.contains(uri))
			return new HashSet<String>();
		return entityWords.keySet(uri);
	}
	
	/**
	 * @param name: the name to search in the WordLexicon
	 * @return the set of words for the given classId
	 */
	public Set<String> getWordsByName(String name)
	{
		if(!nameWords.contains(name))
			return new HashSet<String>();
		return new HashSet<String>(nameWords.get(name));
	}
	
	/**
	 * @return the table of words for a given block of classes
	 */
	public Table2Set<String,String> getWordTable(int block)
	{
		return wordEntities[block];
	}
	
	/**
	 * @param word: the word to search in the WordLexicon
	 * @param uri: the uri of the entity to search in the WordLexicon
	 * @return the weight of the word for the class in the WordLexicon
	 */
	public double getWordWeight(String word, String uri)
	{
		if(!entityWords.contains(uri, word))
			return -1.0;
		return entityWords.get(uri, word);
	}
	
//Private methods
	
	//Builds the WordLexicon from the Ontology and its Lexicon
	private void init()
	{
		//Get the entities from the Ontology
		Set<String> entities = o.getEntities(type);
		//For each entity
		for(String e: entities)
		{
			//Get all names 
			Set<String> names = o.getLexicon().getNamesWithLanguage(e, language);
			if(names == null || names.isEmpty())
				continue;
			//And add the words for each name 
			for(String n: names)
				if(!o.getLexicon().getTypes(n,e).contains(LexicalType.FORMULA))
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
		for(String i : entityWords.keySet())
		{
			double ec = 0.0;
			for(String w : entityWords.keySet(i))
				ec += wordECs.get(w) * getWordWeight(w, i);
			entityECs.put(i, ec);
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
			
	//Adds all words for a given name and uri
	private void addWords(String name, String uri)
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
			wordEntities[block].add(word,uri);
			//Update the current weight of the word for the classId
			Double weight = entityWords.get(uri,word);
			if(weight == null)
				weight = o.getLexicon().getCorrectedWeight(name, uri);
			else
				weight += o.getLexicon().getCorrectedWeight(name, uri);
			//Add the class-word-weight triple
			entityWords.add(uri, word, weight);
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