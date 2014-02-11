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
* @date 22-10-2013                                                            *
******************************************************************************/
package aml.ontology;

import java.util.HashMap;
import java.util.Set;
import java.util.Vector;

import aml.util.StopList;
import aml.util.Table2;
import aml.util.Table2Plus;

public class WordLexicon
{

//Attributes

	//A link to the original Lexicon
	private Lexicon lex;
	//The list of terms to ignore when building this WordLexicon
	private Vector<Integer> ignoreList;
	//The list of stop words to ignore when building this WordLexicon
	private Vector<String> stopList;
	//The map of words and the terms they occur in
	private Table2<String,Integer> wordTerms;
	//The map of word evidence contents
	private HashMap<String,Double> wordECs;
	//The map of terms and the words they contain
	private Table2Plus<Integer,String,Double> termWords;
	//The map of term evidence contents
	private HashMap<Integer,Double> termECs;

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
		ignoreList = new Vector<Integer>(0,1);
		stopList = StopList.read();
		wordTerms = new Table2<String,Integer>();
		wordECs = new HashMap<String,Double>();
		termWords = new Table2Plus<Integer,String,Double>();
		termECs = new HashMap<Integer,Double>();
		total = 0;
		buildWordLexicon();
	}
	
	/**
	 * Constructs a new WordLexicon from the given Lexicon and list of terms
	 * @param l: the Lexicon from which the WordLexicon is derived
	 * @param i: the list of terms to exclude from this WordLexicon
	 */
	public WordLexicon(Lexicon l, Vector<Integer> i)
	{
		lex = l;
		ignoreList = i;
		stopList = StopList.read();
		wordTerms = new Table2<String,Integer>();
		wordECs = new HashMap<String,Double>();
		termWords = new Table2Plus<Integer,String,Double>();
		termECs = new HashMap<Integer,Double>();
		total = 0;
		buildWordLexicon();
	}

	/**
	 * Constructs a new WordLexicon from the given Lexicon and list of terms
	 * @param l: the Lexicon from which the WordLexicon is derived
	 * @param i: the list of terms to exclude from this WordLexicon
	 */
	public WordLexicon(Lexicon l, Set<Integer> t, Vector<Integer> i)
	{
		lex = l;
		ignoreList = i;
		stopList = StopList.read();
		wordTerms = new Table2<String,Integer>();
		wordECs = new HashMap<String,Double>();
		termWords = new Table2Plus<Integer,String,Double>();
		termECs = new HashMap<Integer,Double>();
		total = 0;
		buildWordLexicon();
	}

//Public Methods

	/**
	 * @param term: the term to search in the lexicon
	 * @return the EC of the given term
	 */
	public double getEC(int term)
	{
		if(termECs.containsKey(term))
			return termECs.get(term);
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
	 * @param term: the term to search in the lexicon
	 * @return the maximum weight of the word for the term
	 */
	public double getWeight(String w, int term)
	{
		if(termWords.contains(term,w))
			return termWords.get(term,w);
		return -1.0;
	}
	
	/**
	 * @return the set of terms in the lexicon
	 */
	public Set<Integer> getTerms()
	{
		return termWords.keySet();
	}

	/**
	 * @param w: the word to search in the lexicon
	 * @return the list of terms with the word
	 */
	public Vector<Integer> getTerms(String w)
	{
		return wordTerms.get(w);
	}

	/**
	 * @return the set of words in the lexicon
	 */
	public Set<String> getWords()
	{
		return wordTerms.keySet();
	}
	
	/**
	 * @return the set of words for the given term
	 */
	public Set<String> getWords(int term)
	{
		return termWords.keySet(term);
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
		return wordTerms.keyCount();
	}
	
//Private methods
	
	//Builds the WordLexicon from the original Lexicon
	private void buildWordLexicon()
	{
		Set<Integer> terms = lex.getTerms();
		//For each term
		for(Integer t: terms)
		{
			if(ignoreList.contains(t))
				continue;
			//Get all names 
			Set<String> names = lex.getNames(t);
			if(names == null)
				continue;
			//And add the words for each name 
			for(String n: names)
				if(!lex.getType(n, t).equals("formula"))
					addWords(n, t);
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
			//Then add it to the total EC of each term with the word
			Vector<Integer> wTerms = getTerms(w);
			for(Integer i : wTerms)
			{
				Double totalEC = termECs.get(i);
				if(totalEC == null)
					totalEC = ec * getWeight(w,i);
				else
					totalEC += ec * getWeight(w,i);
				termECs.put(i, totalEC);
			}
		}
	}
			
	//Adds all words for a given name and term
	private void addWords(String name, int term)
	{
		String[] words = name.split(" ");
		for(String w : words)
		{
			String word = w.replaceAll("[()]", "");
			if(stopList.contains(word) || word.length() < 2 || !word.matches(".*[a-zA-Z].*"))
				continue;
			//Update the current weight of the word for the term
			Double weight = termWords.get(term,word);
			if(weight == null)
				weight = lex.getCorrectedWeight(name, term);
			else
				weight += lex.getCorrectedWeight(name, term);
			//Add the word-term pair to the WordLexicon
			wordTerms.add(word, term);
			termWords.add(term, word, weight);
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
