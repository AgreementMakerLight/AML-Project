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

import java.util.Set;
import java.util.Vector;

import aml.util.StopList;
import aml.util.StringParser;
import aml.util.Table2List;

public class AuxiliaryWordLexicon
{

//Attributes

	//A link to the original Lexicon
	private Lexicon lex;
	//The language to use when building this WordLexicon
	private String language;
	//The list of classes to use when building this WordLexicon
	private Set<Integer> classList;
	//The list of stop words to ignore when building this WordLexicon
	private Set<String> stopList;
	//The map of words and the classes they occur in
	private Table2List<String,Integer> wordClasses;
	
//Constructors

	/**
	 * Constructs a new WordLexicon from the given Lexicon
	 * @param l: the Lexicon from which the WordLexicon is derived
	 */
	public AuxiliaryWordLexicon(Lexicon l)
	{
		lex = l;
		classList = null;
		language = "";
		buildWordLexicon();
	}
	
	/**
	 * Constructs a new WordLexicon from the given Lexicon
	 * @param l: the Lexicon from which the WordLexicon is derived
	 * @param lang: the language to use for this WordLexicon
	 */
	public AuxiliaryWordLexicon(Lexicon l, String lang)
	{
		lex = l;
		classList = null;
		language = lang;
		buildWordLexicon();
	}
	
	/**
	 * Constructs a new WordLexicon from the given Lexicon and list of classes
	 * @param l: the Lexicon from which the WordLexicon is derived
	 * @param i: the list of classes to exclude from this WordLexicon
	 */
	public AuxiliaryWordLexicon(Lexicon l, Set<Integer> i)
	{
		lex = l;
		classList = i;
		language = "";
		buildWordLexicon();
	}
	
	/**
	 * Constructs a new WordLexicon from the given Lexicon and list of classes
	 * @param l: the Lexicon from which the WordLexicon is derived
	 * @param i: the list of classes to exclude from this WordLexicon
	 * @param lang: the language to use for this WordLexicon
	 */
	public AuxiliaryWordLexicon(Lexicon l, Set<Integer> i, String lang)
	{
		lex = l;
		classList = i;
		language = lang;
		buildWordLexicon();
	}

//Public Methods

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
	
	public int wordCount()
	{
		return wordClasses.keySet().size();
	}
	
//Private methods
	
	//Builds the WordLexicon from the original Lexicon
	private void buildWordLexicon()
	{
		//Initialize the data structures
		stopList = StopList.read();
		wordClasses = new Table2List<String,Integer>();
		//Get the classes from the Lexicon
		Set<Integer> classes = lex.getClasses();
		//For each class
		for(Integer c: classes)
		{
			if(classList != null && !classList.contains(c))
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
			//Add the word-classId pair to the WordLexicon
			wordClasses.add(word, classId);
			//Update the word frequency
		}
	}
}