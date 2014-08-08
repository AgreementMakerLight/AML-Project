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

import aml.util.StopList;
import aml.util.StringParser;
import aml.util.Table2;

public class NewWordLexicon
{

//Attributes

	//A link to the original Lexicon
	private Lexicon lex;
	//The list of stop words to ignore when building this WordLexicon
	private Set<String> stopList;
	//The language to use when building this WordLexicon
	private String language;
	//The map of classes to names
	private Table2<Integer,String> classNames;
	//The map of classes to names
	private Table2<String,String> nameWords;
	//The map of word evidence contents
	private HashMap<String,Double> wordECs;
	//Auxiliary count of words entered into the WordLexicon
	private int total;
	
//Constructors

	/**
	 * Constructs a new WordLexicon from the given Lexicon
	 * @param l: the Lexicon from which the WordLexicon is derived
	 */
	public NewWordLexicon(Lexicon l)
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
	public NewWordLexicon(Lexicon l, String lang)
	{
		lex = l;
		language = lang;
		init();
	}
	
//Public Methods

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
	 * @param name: the name to search in the lexicon
	 * @param classId: the class to search in the lexicon
	 * @return the weight of the name for the class in the lexicon
	 */
	public double getWeight(String name, int classId)
	{
		if(classNames.contains(classId,name))
			return lex.getCorrectedWeight(name,classId);
		return -1.0;
	}
	
	/**
	 * @return the set of classes in the lexicon
	 */
	public Set<Integer> getClasses()
	{
		return classNames.keySet();
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
		if(!classNames.contains(classId))
			return new HashSet<String>();
		return new HashSet<String>(classNames.get(classId));
	}
	
	/**
	 * @return the set of words for the given classId
	 */
	public Set<String> getWords(int classId)
	{
		HashSet<String> words = new HashSet<String>();
		if(classNames.contains(classId))
			for(String name : classNames.get(classId))
				if(nameWords.contains(name))
					words.addAll(nameWords.get(name));
		return words;
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
	
//Private methods
	
	//Builds the WordLexicon from the original Lexicon
	private void init()
	{
		//Initialize the data structures
		stopList = StopList.read();
		wordECs = new HashMap<String,Double>();
		classNames = new Table2<Integer,String>();
		nameWords = new Table2<String,String>();
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
		//For each word in the WordLexicon
		Set<String> words = wordECs.keySet();
		for(String w : words)
		{
			//Compute and store the normalized EC
			double ec = 1 - (Math.log(wordECs.get(w)) / max);
			wordECs.put(w, ec);
		}
	}
			
	//Adds all words for a given name and classId
	private void addWords(String name, int classId)
	{
		classNames.add(classId, name);
		String[] words = name.split(" ");
		//if(words.length == 1)
			//return;
		for(String w : words)
		{
			String word = w.replaceAll("[()]", "");
			if(stopList.contains(word) || word.length() < 2 || !word.matches(".*[a-zA-Z].*"))
				continue;
			//Add the name-word pair to the WordLexicon
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