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
* The Lexicon of an Ontology, mapping each term (class) to its names and      *
* synonyms. Lexical entries are weighted according to their provenance.       *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 22-10-2013                                                            *
******************************************************************************/
package aml.ontology;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import aml.util.StopList;
import aml.util.Table2;
import aml.util.Table2Plus;
import aml.util.StringParser;


public class Lexicon
{

//Attributes
	
	//The table of names
	private Table2Plus<String,Integer,Provenance> names;
	//The table of terms
	private Table2Plus<Integer,String,Provenance> terms;
	//The table of languages
	private Table2<String,String> languages;
	
//Constructors

	/**
	 * Creates a new empty Lexicon, initializing the multimaps
	 * and the list of provenances
	 */
	public Lexicon()
	{
		names = new Table2Plus<String,Integer,Provenance>();
		terms = new Table2Plus<Integer,String,Provenance>();
		languages = new Table2<String,String>();
	}
	
	/**
	 * Creates a new Lexicon that is a copy of the given Lexicon
	 * @param l: the Lexicon to copy
	 */
	public Lexicon(Lexicon l)
	{
		names = new Table2Plus<String,Integer,Provenance>(l.names);
		terms = new Table2Plus<Integer,String,Provenance>(l.terms);
		languages = new Table2<String,String>(l.languages);
	}

//Public Methods

	/**
	 * Adds a new entry to the Lexicon
	 * @param term: the term to which the name belongs
	 * @param name: the name to add to the Lexicon
	 * @param type: the type of lexical entry (localName, label, etc)
	 * @param source: the source of the lexical entry (ontology URI, etc)
	 */
	public void add(int term, String name, String type, String source, double weight)
	{
		//First ensure that the name contains letters
		if(name == null || !name.matches(".*[a-zA-Z].*"))
			return;

		String s, lang;
		Provenance p;

		//Then check if it is a formula
		if(StringParser.isFormula(name))
		{
			s = StringParser.normalizeFormula(name);
			lang = "Formula";
			p = new Provenance("Formula", source, weight);
		}
		//Or a normal name
		else
		{
			s = StringParser.normalizeName(name);
			lang = "en";
			p = new Provenance(type, source, weight);
		}
		//Then update the tables
		names.addUpgrade(s,term,p);
		terms.addUpgrade(term,s,p);
		languages.add(name,lang);
	}
	
	/**
	 * Adds a new entry to the Lexicon
	 * @param term: the term to which the name belongs
	 * @param name: the name to add to the Lexicon
	 * @param language: the language of the name
	 * @param type: the type of lexical entry (localName, label, etc)
	 * @param source: the source of the lexical entry (ontology URI, etc)
	 */
	public void add(int term, String name, String language, String type, String source, double weight)
	{
		//First ensure that the name contains letters
		if(name == null || !name.matches(".*[a-zA-Z].*"))
			return;

		String s, lang;
		Provenance p;

		//Then check if it is a formula
		if(StringParser.isFormula(name))
		{
			s = StringParser.normalizeFormula(name);
			lang = "Formula";
			p = new Provenance("Formula", source, weight);
		}
		//Or a normal name
		else
		{
			s = StringParser.normalizeName(name);
			lang = language;
			p = new Provenance(type, source, weight);
		}
		//Then update the tables
		names.addUpgrade(s,term,p);
		terms.addUpgrade(term,s,p);
		languages.add(name,lang);
	}
	
	/**
	 * @param name: the name to check in the Lexicon
	 * @return whether the Lexicon contains the name
	 */
	public boolean contains(String name)
	{
		return names.contains(name);
	}
	
	/**
	 * @param term: the term to check in the Lexicon
	 * @param name: the name to check in the Lexicon
	 * @return whether the Lexicon contains the name for the term
	 */
	public boolean contains(int term, String name)
	{
		return terms.contains(term) && terms.get(term).containsKey(name);
	}

	/**
	 * Generates synonyms by removing within-brackets sections of names
	 */
	public void generateBracketSynonyms()
	{
		Vector<String> nm = new Vector<String>(names.keySet());
		for(String n: nm)
		{
			if(StringParser.isFormula(n) || !n.contains("(") || !n.contains(")"))
				continue;
			char[] chars = n.toCharArray();
			String newName = "";
			boolean copy = true;
			for(char c : chars)
			{
				if(c == '(')
					copy = false;
				if(copy)
					newName += c;
				if(c == ')')
					copy = true;					
			}
			//Get the terms with the name
			Vector<Integer> tr = new Vector<Integer>(getInternalTerms(n));
			//For each term
			for(Integer i: tr)
			{
				//Add an entry to the Lexicon with the synonym
				double weight = getWeight(n, i) * 0.9;
				add(i, newName, "internalExtension", "", weight);
			}			
				
		}
	}
	
	/**
	 * Generates synonyms by removing leading and trailing stop words from names
	 */
	public void generateStopWordSynonyms()
	{
		Vector<String> stopList = StopList.read();
		Vector<String> nm = new Vector<String>(names.keySet());
		for(String n: nm)
		{
			if(StringParser.isFormula(n))
				continue;
			//Build a synonym by removing all leading and trailing stopWords
			String[] nameWords = n.split(" ");
			//First find the first word in the name that is not a stopWord
			int start = 0;
			for(int i = 0; i < nameWords.length; i++)
			{
				if(!stopList.contains(nameWords[i]))
				{
					start = i;
					break;
				}
			}
			//Then find the last word in the name that is not a stopWord
			int end = nameWords.length;
			for(int i = nameWords.length - 1; i > 0; i--)
			{
				if(!stopList.contains(nameWords[i]))
				{
					end = i+1;
					break;
				}
			}
			//If the name contains no leading or trailing stopWords proceed to next name
			if(start == 0 && end == nameWords.length)
				continue;
			//Otherwise build the synonym
			String newName = "";
			for(int i = start; i < end; i++)
				newName += nameWords[i] + " ";
			newName = newName.trim();
			//Get the terms with the name
			Vector<Integer> tr = new Vector<Integer>(getInternalTerms(n));
			//For each term
			for(Integer i: tr)
			{
				//Add an entry to the Lexicon with the synonym
				double weight = getWeight(n, i) * 0.9;
				add(i, newName, "internalExtension", "", weight);
			}
		}
	}
	
	/**
	 * @param term: the term to search in the Lexicon
	 * @return the map of names and provenances for that term
	 */
	public HashMap<String,Provenance> get(int term)
	{
		return terms.get(term);
	}

	/**
	 * @param name: the name to search in the Lexicon
	 * @return the map of terms and provenances for that name
	 */
	public HashMap<Integer,Provenance> get(String name)
	{
		return names.get(name);
	}

	/**
	 * @param term: the term to search in the Lexicon
	 * @return the name associated with the term that has the highest
	 * provenance weight
	 */
	public String getBestName(int term)
	{
		return terms.getKeyMaximum(term);
	}
	
	/**
	 * @param name: the term name to search in the Lexicon
	 * @param internal: whether to restrict the search to internal Lexicon entries
	 * or consider extension entries
	 * @return the term associated with the name that has the highest
	 * provenance weight, or -1 if either no term or two or more such
	 * terms are found
	 */
	public int getBestTerm(String name, boolean internal)
	{
		Set<Integer> hits;
		if(internal)
			hits = getInternalTerms(name);
		else
			hits = getTerms(name);
		if(hits == null)
			return -1;
		
		Vector<Integer> bestTerms = new Vector<Integer>(1,1);
		double weight;
		double maxWeight = 0.0;
		
		for(Integer i : hits)
		{
			weight = getWeight(name,i);
			if(weight > maxWeight)
			{
				maxWeight = weight;
				bestTerms = new Vector<Integer>(1,1);
				bestTerms.add(i);
			}
			else if(weight == maxWeight)
			{
				bestTerms.add(i);
			}
		}
		if(bestTerms.size() != 1)
			return -1;
		return bestTerms.get(0);
	}
	
	/**
	 * @param name: the name to search in the Lexicon
	 * @param term: the term to search in the Lexicon
	 * @return the weight corresponding to the provenance of the name for that term
	 * with a correction factor depending on how many names of that provenance the
	 * the term has
	 */
	public double getCorrectedWeight(String name, int term)
	{
		Provenance p = names.get(name, term);
		if(p == null)
			return 0.0;
		double correction = nameCount(term,p)/100.0;
		return p.getWeight() - correction;
	}
	
	/**
	 * @return the list of terms that have a name from an external source
	 */
	public Vector<Integer> getExtendedTerms()
	{
		Vector<Integer> extendedTerms = new Vector<Integer>(0,1);
		Set<Integer> ts = terms.keySet();
		for(Integer i : ts)
			if(hasExternalName(i))
				extendedTerms.add(i);
		return extendedTerms;
	}
	
	/**
	 * @param term: the term to search in the Lexicon
	 * @return the list of local names associated with the term
	 */
	public Set<String> getInternalNames(int term)
	{
		Set<String> hits = terms.keySet(term);
		HashSet<String> localHits = new HashSet<String>();
		if(hits == null)
			return localHits;
		for(String s : hits)
			if(!isExternal(s,term))
				localHits.add(s);
		return localHits;
	}
	
	/**
	 * @param name: the term name to search in the Lexicon
	 * @return the list of terms associated with the name from
	 * a local source
	 */
	public Set<Integer> getInternalTerms(String name)
	{
		Set<Integer> hits = names.keySet(name);
		HashSet<Integer> localHits = new HashSet<Integer>();
		if(hits == null)
			return localHits;
		for(Integer i : hits)
			if(!isExternal(name,i))
				localHits.add(i);
		return localHits;
	}
	
	/**
	 * @param name: the term name to search in the Lexicon
	 * @return the list of languages declared for the name
	 */
	public Vector<String> getLanguages(String name)
	{
		return languages.get(name);
	}
	
	/**
	 * @return the set of names in the Lexicon
	 */
	public Set<String> getNames()
	{
		return names.keySet();
	}

	/**
	 * @param term: the term to search in the Lexicon
	 * @return the list of names associated with the term
	 */
	public Set<String> getNames(int term)
	{
		return terms.keySet(term);
	}

	/**
	 * @param term: the term to search in the Lexicon
	 * @param type: the type to restrict the search
	 * @return the list of names of the given type associated with the term
	 */
	public Vector<String> getNames(int term, String type)
	{
		Provenance p = new Provenance(type, "", 1);
		return terms.getMatchingKeys(term, p);
	}
	
	/**
	 * @param name: the name to search in the Lexicon
	 * @param term: the term to search in the Lexicon
	 * @return the source of the name for that term
	 */
	public String getSource(String name, int term)
	{
		Provenance p = names.get(name, term);
		if(p == null)
			return null;
		return p.getSource();
	}
	
	/**
	 * @return the set of terms in the Lexicon
	 */
	public Set<Integer> getTerms()
	{
		return terms.keySet();
	}
	
	/**
	 * @param name: the term name to search in the Lexicon
	 * @return the list of terms associated with the name
	 */
	public Set<Integer> getTerms(String name)
	{
		return names.keySet(name);
	}
	
	/**
	 * @param name: the term name to search in the Lexicon
	 * @param type: the type to filter the search
	 * @return the list of terms associated with the name with the given type
	 */
	public Vector<Integer> getTerms(String name, String type)
	{
		Provenance p = new Provenance(type, "", 1);
		return names.getMatchingKeys(name, p);		
	}
	
	/**
	 * @param source: the source to search in the Lexicon
	 * @return the list of terms that have names from the given source
	 */
	public Vector<Integer> getTermsWithSource(String source)
	{
		Vector<Integer> termsWithSource = new Vector<Integer>(0,1);
		Set<Integer> ts = terms.keySet();
		for(Integer i : ts)
			if(hasNameFromSource(i,source) && !termsWithSource.contains(i))
				termsWithSource.add(i);
		return termsWithSource;
	}
		
	/**
	 * @param name: the name to search in the Lexicon
	 * @param term: the term to search in the Lexicon
	 * @return the type of the name for that term
	 */
	public String getType(String name, int term)
	{
		Provenance p = names.get(name, term);
		if(p == null)
			return null;
		return p.getType();
	}
	
	/**
	 * @param name: the name to search in the Lexicon
	 * @param term: the term to search in the Lexicon
	 * @return the weight corresponding to the provenance of the name for that term
	 */
	public double getWeight(String name, int term)
	{
		Provenance p = names.get(name, term);
		if(p == null)
			return 0.0;
		return p.getWeight();
	}

	/**
	 * @param term: the term to search in the Lexicon
	 * @return whether the term has an external name
	 */
	public boolean hasExternalName(int term)
	{
		Set<String> termNames = getNames(term);
		if(termNames == null)
			return false;
		for(String n : termNames)
			if(isExternal(n,term))
				return true;
		return false;
	}

	/**
	 * @param term: the term to search in the Lexicon
	 * @param source: the source to search in the Lexicon
	 * @return whether the term has an external name
	 */
	public boolean hasNameFromSource(int term, String source)
	{
		Set<String> termNames = getNames(term);
		if(termNames == null)
			return false;
		for(String n : termNames)
			if(getSource(n,term).equals(source))
				return true;
		return false;
	}

	/**
	 * @param name: the name to search in the Lexicon
	 * @param term: the term to search in the Lexicon
	 * @return whether the type of the name for the term
	 * is external
	 */
	public boolean isExternal(String name, int term)
	{
		Provenance p = names.get(name, term);
		if(p == null)
			return false;
		return p.isExternal();
	}
	
	/**
	 * @param name: the name to search in the Lexicon
	 * @return whether the name is a formula
	 */
	public boolean isFormula(String name)
	{
		return languages.get(name).equals("Formula");
	}
	
	/**
	 * @return the number of names in the Lexicon
	 */
	public int nameCount()
	{
		return names.keyCount();
	}
	
	/**
	 * @param term: the term to search in the Lexicon
	 * @return the number of names associated with the term
	 */
	public int nameCount(int term)
	{
		return terms.entryCount(term);
	}
	
	/**
	 * @param term: the term to search in the Lexicon
	 * @param type: the type to restrict the search
	 * @return the number of names of the given type associated with the term
	 */
	public int nameCount(int term, String type)
	{
		Provenance p = new Provenance(type, "", 1);
		return terms.entryCount(term, p);
	}
	
	/**
	 * @param term: the term to search in the Lexicon
	 * @param p: the provenance of the names to get from the Lexicon
	 * @return the number of names of the given provenance associated with the term
	 */
	public int nameCount(int term, Provenance p)
	{
		return terms.entryCount(term, p);
	}

	/**
	 * @return the number of entries in the Lexicon
	 */
	public int size()
	{
		return names.size();
	}
	
	/**
	 * @return the number of terms in the Lexicon
	 */
	public int termCount()
	{
		return terms.keyCount();
	}
	
	/**
	 * @param name: the name to search in the Lexicon
	 * @return the number of terms associated with the name
	 */
	public int termCount(String name)
	{
		return names.entryCount(name);
	}
	
	/**
	 * @param name: the name to search in the Lexicon
	 * @param type: the type to restrict the search
	 * @return the number of terms associated with the name with the given type
	 */
	public int termCount(String name, String type)
	{
		Provenance p = new Provenance(type, "", 1);
		return names.entryCount(name, p);
	}
}