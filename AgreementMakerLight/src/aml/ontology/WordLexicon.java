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
* @date 23-06-2014                                                            *
* @version 2.0                                                                *
******************************************************************************/
package aml.ontology;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import aml.util.StopList;
import aml.util.StringParser;
import aml.util.Table2;
import aml.util.Table2Plus;

public class WordLexicon
{

//Attributes

	//A link to the original Lexicon
	private Lexicon lex;
	//The list of classes to ignore when building this WordLexicon
	private Set<Integer> ignoreList;
	//The list of stop words to ignore when building this WordLexicon
	private Vector<String> stopList;
	//The language to use when building this WordLexicon
	private String language;
	//The map of words and the classes they occur in
	private Table2<String,Integer> wordClasses;
	//The map of word evidence contents
	private HashMap<String,Double> wordECs;
	//The map of classes and the words they contain
	private Table2Plus<Integer,String,Double> classWords;
	//The map of classId evidence contents
	private HashMap<Integer,Double> classECs;
	
	//Auxiliary variable
	private int total;
	
//Constructors

	/**
	 * Constructs a new WordLexicon from the given Lexicon
	 * @param l: the Lexicon from which the WordLexicon is derived
	 */
	public WordLexicon(Lexicon l)
	{
		lex = l;
		ignoreList = new HashSet<Integer>();
		language = "";
		buildWordLexicon();
	}
	
	/**
	 * Constructs a new WordLexicon from the given Lexicon
	 * @param l: the Lexicon from which the WordLexicon is derived
	 * @param lang: the language to use for this WordLexicon
	 */
	public WordLexicon(Lexicon l, String lang)
	{
		lex = l;
		ignoreList = new HashSet<Integer>();
		language = lang;
		buildWordLexicon();
	}
	
	/**
	 * Constructs a new WordLexicon from the given Lexicon and list of classes
	 * @param l: the Lexicon from which the WordLexicon is derived
	 * @param i: the list of classes to exclude from this WordLexicon
	 */
	public WordLexicon(Lexicon l, Set<Integer> i)
	{
		lex = l;
		ignoreList = i;
		language = "";
		buildWordLexicon();
	}
	
	/**
	 * Constructs a new WordLexicon from the given Lexicon and list of classes
	 * @param l: the Lexicon from which the WordLexicon is derived
	 * @param i: the list of classes to exclude from this WordLexicon
	 * @param lang: the language to use for this WordLexicon
	 */
	public WordLexicon(Lexicon l, Set<Integer> i, String lang)
	{
		lex = l;
		ignoreList = i;
		language = lang;
		buildWordLexicon();
	}

//Public Methods

	/**
	 * @param classId: the class to search in the lexicon
	 * @return the EC of the given class
	 */
	public double getEC(int classId)
	{
		if(classECs.containsKey(classId))
			return classECs.get(classId);
		return -1.0;
	}

	/**
	 * @param w: the word to search in the lexicon
	 * @return the EC of the given word
	 */
	public double getEC(String w)
	{
		if(wordECs.containsKey(w))
			return wordECs.get(w);
		return -1.0;
	}

	/**
	 * @param w: the word to search in the lexicon
	 * @param classId: the class to search in the lexicon
	 * @return the maximum weight of the word for the class
	 */
	public double getWeight(String w, int classId)
	{
		if(classWords.contains(classId,w))
			return classWords.get(classId,w);
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
	 * @param w: the word to search in the lexicon
	 * @return the list of classes with the word
	 */
	public Vector<Integer> getClasses(String w)
	{
		return wordClasses.get(w);
	}
	
	/**
	 * @return the set of words in the lexicon
	 */
	public Set<String> getWords()
	{
		return wordClasses.keySet();
	}
	
	/**
	 * @return the set of words for the given classId
	 */
	public Set<String> getWords(int classId)
	{
		return classWords.keySet(classId);
	}
	
	/**
	 * @return whether the lexicon has an ignore list
	 */
	public boolean hasIgnoreList()
	{
		return ignoreList.size() > 0;
	}
	
	/**
	 * @param il: the ignore list to check in the lexicon
	 * @return whether the lexicon has an ignore list
	 * equal to the given list
	 */
	public boolean hasIgnoreList(Vector<Integer> il)
	{
		return ignoreList.equals(il);
	}
	
	/**
	 * @return the number of words in the lexicon
	 */
	public int wordCount()
	{
		return wordClasses.keyCount();
	}
	
//Private methods
	
	//Builds the WordLexicon from the original Lexicon
	private void buildWordLexicon()
	{
		//Initialize the data structures
		stopList = StopList.read();
		wordClasses = new Table2<String,Integer>();
		wordECs = new HashMap<String,Double>();
		classWords = new Table2Plus<Integer,String,Double>();
		classECs = new HashMap<Integer,Double>();
		total = 0;
		//Get the classes from the Lexicon
		Set<Integer> classes = lex.getClasses();
		//For each class
		for(Integer c: classes)
		{
			if(ignoreList.contains(c))
				continue;
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
		//For each word in the WordLexicon
		Set<String> words = wordECs.keySet();
		for(String w : words)
		{
			//Compute and store the normalized EC
			double ec = 1 - (Math.log(wordECs.get(w)) / max);
			wordECs.put(w, ec);
			//Then add it to the total EC of each classId with the word
			Vector<Integer> wClasses = getClasses(w);
			for(Integer i : wClasses)
			{
				Double totalEC = classECs.get(i);
				if(totalEC == null)
					totalEC = ec * getWeight(w,i);
				else
					totalEC += ec * getWeight(w,i);
				classECs.put(i, totalEC);
			}
		}
	}
			
	//Adds all words for a given name and classId
	private void addWords(String name, int classId)
	{
		String[] words = name.split(" ");
		//if(words.length == 1)
			//return;
		for(String w : words)
		{
			String word = w.replaceAll("[()]", "");
			if(stopList.contains(word) || word.length() < 2 || !word.matches(".*[a-zA-Z].*"))
				continue;
			//Update the current weight of the word for the classId
			Double weight = classWords.get(classId,word);
			if(weight == null)
				weight = lex.getCorrectedWeight(name, classId);
			else
				weight += lex.getCorrectedWeight(name, classId);
			//Add the word-classId pair to the WordLexicon
			wordClasses.add(word, classId);
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