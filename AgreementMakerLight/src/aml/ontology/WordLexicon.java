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
* The lexicon of words in an Ontology, which is derived from its Lexicon and  *
* used by the WordMatcher.                                                    *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 11-08-2014                                                            *
* @version 2.0                                                                *
******************************************************************************/
package aml.ontology;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import aml.util.StopList;
import aml.util.StringParser;
import aml.util.Table2;
import aml.util.Table2Plus;

public class WordLexicon
{

//Attributes

	//A link to the original Lexicon
	private Lexicon lex;
	//The list of stop words to ignore when building this WordLexicon
	private Set<String> stopList;
	//The language to use when building this WordLexicon
	private String language;
	//The map of classes to names
	private Table2Plus<Integer,String,Double> classWords;
	//The map of classes to names
	private Table2<String,String> nameWords;
	//The map of word evidence contents
	private HashMap<String,Double> wordECs;
	//The map of word evidence contents
	private HashMap<Integer,Double> classECs;
	//The map of word evidence contents
	private HashMap<String,Double> nameECs;
	//Auxiliary count of words entered into the WordLexicon
	private int total;
	
//Constructors

	/**
	 * Constructs a new WordLexicon from the given Lexicon
	 * @param l: the Lexicon from which the WordLexicon is derived
	 */
	public WordLexicon(Lexicon l)
	{
		lex = l;
		language = "";
		init();
	}
	
	/**
	 * Constructs a new WordLexicon from the given Lexicon
	 * @param l: the Lexicon from which the WordLexicon is derived
	 * @param lang: the language to use for this WordLexicon
	 */
	public WordLexicon(Lexicon l, String lang)
	{
		lex = l;
		language = lang;
		init();
	}
	
//Public Methods

	/**
	 * @param classId: the class to search in the lexicon
	 * @return the EC of the given class
	 */
	public double getClassEC(int classId)
	{
		if(classECs.containsKey(classId))
			return classECs.get(classId);
		return -1.0;
	}
	
	/**
	 * @return the set of classes in the lexicon
	 */
	public Set<Integer> getClasses()
	{
		return classWords.keySet();
	}
	
	/**
	 * @param n: the name to search in the lexicon
	 * @return the EC of the given name
	 */
	public double getNameEC(String n)
	{
		if(nameECs.containsKey(n))
			return nameECs.get(n);
		return -1.0;
	}
	
	/**
	 * @return the set of names in the lexicon
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
	 * @param name: the name to search in the lexicon
	 * @param classId: the class to search in the lexicon
	 * @return the weight of the name for the class in the lexicon
	 */
	public double getNameWeight(String name, int classId)
	{
		return lex.getCorrectedWeight(name,classId);
	}
	
	/**
	 * @param w: the word to search in the lexicon
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
		if(!classWords.contains(classId))
			return new HashSet<String>();
		return classWords.keySet(classId);
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
	 * @return the table of words associated with a given set of classes
	 */
	public Table2<String,Integer> getWordTable(Set<Integer> classes)
	{
		Table2<String,Integer> wordTable = new Table2<String,Integer>();
		for(Integer i : classes)
			if(classWords.contains(i))
				for(String w : classWords.keySet(i))
					wordTable.add(w,i);
		return wordTable;
	}
	
	/**
	 * @param word: the word to search in the lexicon
	 * @param classId: the class to search in the lexicon
	 * @return the weight of the word for the class in the lexicon
	 */
	public double getWordWeight(String word, int classId)
	{
		if(!classWords.contains(classId, word))
			return -1.0;
		return classWords.get(classId, word);
	}
	
//Private methods
	
	//Builds the WordLexicon from the original Lexicon
	private void init()
	{
		//Initialize the data structures
		stopList = StopList.read();
		classWords = new Table2Plus<Integer,String,Double>();
		nameWords = new Table2<String,String>();
		wordECs = new HashMap<String,Double>();
		classECs = new HashMap<Integer,Double>();
		nameECs = new HashMap<String,Double>();
		total = 0;
		//Get the classes from the Lexicon
		Set<Integer> classes = lex.getClasses();
		//For each class
		for(Integer c: classes)
		{
			//Get all names 
			Set<String> names;
			if(language.equals(""))
				names = lex.getNames(c);
			else
				names = lex.getNamesWithLanguage(c, language);
			if(names == null)
				continue;
			//And add the words for each name 
			for(String n: names)
				if(!StringParser.isFormula(n))
					addWords(n, c);
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
		for(Integer i : classWords.keySet())
		{
			double ec = 0.0;
			for(String w : classWords.keySet(i))
				ec += wordECs.get(w) * getWordWeight(w, i);
			classECs.put(i, ec);
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
			if(stopList.contains(word) || word.length() < 2 || !word.matches(".*[a-zA-Z].*"))
				continue;
			//Add the name-word pair to the WordLexicon
			nameWords.add(name, word);
			//Update the current weight of the word for the classId
			Double weight = classWords.get(classId,word);
			if(weight == null)
				weight = lex.getCorrectedWeight(name, classId);
			else
				weight += lex.getCorrectedWeight(name, classId);
			//And add the class-word-weight trio
			classWords.add(classId, word, weight);
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