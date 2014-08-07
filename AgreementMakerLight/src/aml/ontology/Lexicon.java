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
* The Lexicon of an Ontology, mapping each class to its names and synonyms.   *
* Lexical entries are weighted according to their provenance.                 *
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

import aml.AML;
import aml.util.StopList;
import aml.util.StringParser;
import aml.util.Table3;


public class Lexicon
{

//Attributes
	
	//The table of names
	private Table3<String,Integer,Provenance> names;
	//The table of classes
	private Table3<Integer,String,Provenance> classes;
	//The language counts
	private HashMap<String,Integer> langCount;
	
//Constructors

	/**
	 * Creates a new empty Lexicon, initializing the multimaps
	 * and the list of provenances
	 */
	public Lexicon()
	{
		names = new Table3<String,Integer,Provenance>();
		classes = new Table3<Integer,String,Provenance>();
		langCount = new HashMap<String,Integer>();
	}
	
	/**
	 * Creates a new Lexicon that is a copy of the given Lexicon
	 * @param l: the Lexicon to copy
	 */
	public Lexicon(Lexicon l)
	{
		names = new Table3<String,Integer,Provenance>(l.names);
		classes = new Table3<Integer,String,Provenance>(l.classes);
		langCount = new HashMap<String,Integer>(l.langCount);
	}

//Public Methods

	/**
	 * Adds a new entry to the Lexicon
	 * @param classId: the class to which the name belongs
	 * @param name: the name to add to the Lexicon
	 * @param type: the type of lexical entry (localName, label, etc)
	 * @param source: the source of the lexical entry (ontology URI, etc)
	 */
	public void add(int classId, String name, String type, String source, double weight)
	{
		//First ensure that the name is not null or empty, and (since we're assuming that
		//the language is English by default, ensure that it contains Latin characters)
		if(name == null || name.equals("") || !name.matches(".*[a-zA-Z].*"))
			return;

		String s, lang;
		Provenance p;

		//If it is a formula, parse it and label it as such
		if(StringParser.isFormula(name))
		{
			s = StringParser.normalizeFormula(name);
			lang = "Formula";
			p = new Provenance("Formula", source, lang, weight);
		}
		//Otherwise, parse it normally
		else
		{
			s = StringParser.normalizeName(name);
			lang = "en";
			p = new Provenance(type, source, lang, weight);
		}
		//Then update the tables
		names.add(s,classId,p);
		classes.add(classId,s,p);
		Integer i = langCount.get(lang);
		if(i == null)
			langCount.put(lang, 1);
		else
			langCount.put(lang, i+1);
	}
	
	/**
	 * Adds a new entry to the Lexicon
	 * @param classId: the class to which the name belongs
	 * @param name: the name to add to the Lexicon
	 * @param language: the language of the name
	 * @param type: the type of lexical entry (localName, label, etc)
	 * @param source: the source of the lexical entry (ontology URI, etc)
	 */
	public void add(int classId, String name, String language, String type, String source, double weight)
	{
		//First ensure that the name is not null or empty
		if(name == null || name.equals(""))
			return;

		String s, lang;
		Provenance p;

		//If the name is in a language that doesn't use a Latin character set
		//we parse it as a formula (i.e., replace only '_' with ' ')
		if(AML.getInstance().isNonLatin(language))
		{
			s = StringParser.normalizeFormula(name);
			lang = language;
			p = new Provenance(type, source, lang, weight);
		}
		//Otherwise
		else
		{
			//If it doesn't contain Latin characters, don't add it
			if(!name.matches(".*[a-zA-Z].*"))
				return;
			//If it is a formula, parse it and label it as such
			else if(StringParser.isFormula(name))
			{
				s = StringParser.normalizeFormula(name);
				lang = "Formula";
				p = new Provenance("Formula", source, lang, weight);
			}
			//Otherwise, parse it normally
			else
			{
				s = StringParser.normalizeName(name);
				lang = language;
				p = new Provenance(type, source, lang, weight);
			}
		}
		//Then update the tables
		names.add(s,classId,p);
		classes.add(classId,s,p);
		Integer i = langCount.get(lang);
		if(i == null)
			langCount.put(lang, 1);
		else
			langCount.put(lang, i+1);
	}
		
	/**
	 * @return the number of classes in the Lexicon
	 */
	public int classCount()
	{
		return classes.keyCount();
	}
	
	/**
	 * @param name: the name to search in the Lexicon
	 * @return the number of classes associated with the name
	 */
	public int classCount(String name)
	{
		return names.entryCount(name);
	}
	
	/**
	 * @param name: the name to search in the Lexicon
	 * @param type: the type to restrict the search
	 * @return the number of classes associated with the name with the given type
	 */
	public int classCount(String name, String type)
	{
		return getClasses(name,type).size();
	}
	
	/**
	 * @param name: the name to search in the Lexicon
	 * @param p: the provenance of the names to get from the Lexicon
	 * @return the number of names with the same language as the
	 * given provenance that are associated with the class
	 */
	public int classCount(String name, Provenance p)
	{
		return names.entryCount(name, p);
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
	 * @param classId: the class to check in the Lexicon
	 * @param name: the name to check in the Lexicon
	 * @return whether the Lexicon contains the name for the class
	 */
	public boolean contains(int classId, String name)
	{
		return classes.contains(classId) && classes.get(classId).contains(name);
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
			String newName;
			if(n.matches("\\([^()]+\\)") || n.contains(") or ("))
				newName = n.replaceAll("[()]", "");
			else if(n.contains(")("))
				continue;
			else
			{
				newName = "";
				char[] chars = n.toCharArray();
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
				newName = newName.trim();
			}
			//Get the classes with the name
			Vector<Integer> tr = new Vector<Integer>(getInternalClasses(n));
			for(Integer i : tr)
			{
				for(Provenance p : names.get(n, i))
				{
					double weight = p.getWeight() * 0.9;
					add(i, newName, p.getLanguage(), "internalExtension", p.getSource(), weight);
				}
			}
		}
	}
	
	/**
	 * Generates synonyms by removing leading and trailing stop words from names
	 */
	public void generateStopWordSynonyms()
	{
		Set<String> stopList = StopList.read();
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

			//Get the classes with the name
			Vector<Integer> tr = new Vector<Integer>(getInternalClasses(n));
			for(Integer i : tr)
			{
				for(Provenance p : names.get(n, i))
				{
					double weight = p.getWeight() * 0.9;
					add(i, newName, p.getLanguage(), "internalExtension", p.getSource(), weight);
				}
			}
		}
	}
	
	/**
	 * @param name: the name to search in the Lexicon
	 * @param classId: the class to search in the Lexicon
	 * @return the provenances associated with the name,class pair
	 */	
	public Vector<Provenance> get(String name, int classId)
	{
		if(names.contains(name, classId))
			return names.get(name, classId);
		return new Vector<Provenance>();
	}
	
	/**
	 * @param classId: the class to search in the Lexicon
	 * @return the name associated with the class that has the highest
	 * provenance weight
	 */
	public String getBestName(int classId)
	{
		return classes.getKeyMaximum(classId);
	}
	
	/**
	 * @param name: the class name to search in the Lexicon
	 * @param internal: whether to restrict the search to internal Lexicon entries
	 * or consider extension entries
	 * @return the class associated with the name that has the highest
	 * provenance weight, or -1 if either no class or two or more such
	 * classes are found
	 */
	public int getBestClass(String name, boolean internal)
	{
		Set<Integer> hits;
		if(internal)
			hits = getInternalClasses(name);
		else
			hits = getClasses(name);
		if(hits == null)
			return -1;
		
		Vector<Integer> bestClasses = new Vector<Integer>(1,1);
		double weight;
		double maxWeight = 0.0;
		
		for(Integer i : hits)
		{
			weight = getWeight(name,i);
			if(weight > maxWeight)
			{
				maxWeight = weight;
				bestClasses = new Vector<Integer>(1,1);
				bestClasses.add(i);
			}
			else if(weight == maxWeight)
			{
				bestClasses.add(i);
			}
		}
		if(bestClasses.size() != 1)
			return -1;
		return bestClasses.get(0);
	}
	
	
	/**
	 * @return the set of classes in the Lexicon
	 */
	public Set<Integer> getClasses()
	{
		return classes.keySet();
	}
	
	/**
	 * @param name: the class name to search in the Lexicon
	 * @return the list of classes associated with the name
	 */
	public Set<Integer> getClasses(String name)
	{
		return names.keySet(name);
	}
	
	/**
	 * @param name: the class name to search in the Lexicon
	 * @param type: the type to filter the search
	 * @return the list of classes associated with the name with the given type
	 */
	public Set<Integer> getClasses(String name, String type)
	{
		Set<Integer> hits = names.keySet(name);
		HashSet<Integer> classesType = new HashSet<Integer>();
		if(hits == null)
			return classesType;
		for(Integer i : hits)
			for(Provenance p : names.get(name,i))
				if(p.getType().equals(type))
					classesType.add(i);
		return classesType;
	}
	
	/**
	 * @param name: the class name to search in the Lexicon
	 * @param lang: the language of the names to get from the Lexicon
	 * @return the list of classes associated with the name with the
	 * given language
	 */
	public Set<Integer> getClassesWithLanguage(String name, String lang)
	{
		Set<Integer> hits = names.keySet(name);
		HashSet<Integer> classesLang = new HashSet<Integer>();
		if(hits == null)
			return classesLang;
		for(Integer i : hits)
			for(Provenance p : names.get(name,i))
				if(p.getLanguage().equals(lang))
					classesLang.add(i);
		return classesLang;
	}
	
	/**
	 * @param source: the source to search in the Lexicon
	 * @return the list of classes that have names from the given source
	 */
	public Vector<Integer> getClassesWithSource(String source)
	{
		Vector<Integer> classesWithSource = new Vector<Integer>(0,1);
		Set<Integer> ts = classes.keySet();
		for(Integer i : ts)
			if(hasNameFromSource(i,source) && !classesWithSource.contains(i))
				classesWithSource.add(i);
		return classesWithSource;
	}
		
	/**
	 * @param name: the name to search in the Lexicon
	 * @param classId: the class to search in the Lexicon
	 * @return the weight corresponding to the provenance of the name for that class
	 * with a correction factor depending on how many names of that provenance the
	 * the class has
	 */
	public double getCorrectedWeight(String name, int classId)
	{
		if(!names.contains(name, classId))
			return 0.0;
		Provenance p = names.get(name, classId).get(0);
		double correction = nameCount(classId,p.getType())/100.0;
		return p.getWeight() - correction;
	}
	
	/**
	 * @param name: the name to search in the Lexicon
	 * @param classId: the class to search in the Lexicon
	 * @param lang: the language to search in the Lexicon
	 * @return the weight corresponding to the provenance of the name for that class
	 * with a correction factor depending on how many names of that provenance the
	 * the class has
	 */
	public double getCorrectedWeight(String name, int classId, String lang)
	{
		Vector<Provenance> provs = names.get(name, classId);
		if(provs == null)
			return 0.0;
		for(Provenance p : provs)
		{
			if(p.getLanguage().equals(lang))
			{
				double correction = nameCount(classId,p.getType(),p.getLanguage())/100.0;
				return p.getWeight() - correction;
			}
		}
		return 0.0;
	}
	
	/**
	 * @return the list of classes that have a name from an external source
	 */
	public Set<Integer> getExtendedClasses()
	{
		HashSet<Integer> extendedClasses = new HashSet<Integer>(0,1);
		Set<Integer> ts = classes.keySet();
		for(Integer i : ts)
			if(hasExternalName(i))
				extendedClasses.add(i);
		return extendedClasses;
	}
	
	/**
	 * @param classId: the class to search in the Lexicon
	 * @return the list of local names associated with the class
	 */
	public Set<String> getInternalNames(int classId)
	{
		Set<String> hits = classes.keySet(classId);
		HashSet<String> localHits = new HashSet<String>();
		if(hits == null)
			return localHits;
		for(String s : hits)
			if(!isExternal(s,classId))
				localHits.add(s);
		return localHits;
	}
	
	/**
	 * @param name: the class name to search in the Lexicon
	 * @return the list of classes associated with the name from
	 * a local source
	 */
	public Set<Integer> getInternalClasses(String name)
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
	 * @param lang: the language code to search in the Lexicon
	 * @return the number of Lexical entries with that language
	 */
	public Integer getLanguageCount(String lang)
	{
		return langCount.get(lang);
	}
	
	/**
	 * @return the set of languages in the Lexicon
	 */
	public Set<String> getLanguages()
	{
		return langCount.keySet();
	}
	
	/**
	 * @param name: the class name to search in the Lexicon
	 * @return the list of languages declared for the name
	 */
	public Set<String> getLanguages(String name)
	{
		Set<Integer> hits = names.keySet(name);
		HashSet<String> langs = new HashSet<String>();
		if(hits == null)
			return langs;
		for(Integer i : hits)
			for(Provenance p : names.get(name,i))
				langs.add(p.getLanguage());
		return langs;
	}
	
	/**
	 * @param name: the name to search in the Lexicon
	 * @param classId: the class to search in the Lexicon
	 * @return the list of languages declared for the name,class pair
	 */
	public Set<String> getLanguages(String name, int classId)
	{
		Vector<Provenance> hits = names.get(name,classId);
		HashSet<String> langs = new HashSet<String>();
		if(hits == null)
			return langs;
		for(Provenance p : hits)
			langs.add(p.getLanguage());
		return langs;
	}
	
	/**
	 * @return the set of names in the Lexicon
	 */
	public Set<String> getNames()
	{
		return names.keySet();
	}

	/**
	 * @param classId: the class to search in the Lexicon
	 * @return the list of names associated with the class
	 */
	public Set<String> getNames(int classId)
	{
		return classes.keySet(classId);
	}
	
	/**
	 * @param classId: the class to search in the Lexicon
	 * @param type: the type to restrict the search
	 * @return the list of names of the given type associated with the class
	 */
	public Set<String> getNames(int classId, String type)
	{
		Set<String> hits = classes.keySet(classId);
		HashSet<String> namesType = new HashSet<String>();
		if(hits == null)
			return namesType;
		for(String n : hits)
			for(Provenance p : classes.get(classId,n))
				if(p.getType().equals(type))
					namesType.add(n);
		return namesType;
	}
	
	/**
	 * @param classId: the class to search in the Lexicon
	 * @param lang: the lang of the names to get from the Lexicon
	 * @return the names with the given language associated with the class
	 */
	public Set<String> getNamesWithLanguage(int classId, String lang)
	{
		Set<String> hits = classes.keySet(classId);
		HashSet<String> namesLang = new HashSet<String>();
		if(hits == null)
			return namesLang;
		for(String n : hits)
			for(Provenance p : classes.get(classId,n))
				if(p.getLanguage().equals(lang))
					namesLang.add(n);
		return namesLang;
	}
	
	/**
	 * @param name: the name to search in the Lexicon
	 * @param classId: the class to search in the Lexicon
	 * @return the sources of the name for that class
	 */
	public Set<String> getSources(String name, int classId)
	{
		Vector<Provenance> provs = names.get(name, classId);
		HashSet<String> sources = new HashSet<String>();
		if(provs == null)
			return sources;
		for(Provenance p : provs)
			sources.add(p.getSource());
		return sources;
	}
	/**
	 * @param name: the name to search in the Lexicon
	 * @param classId: the class to search in the Lexicon
	 * @return the types of the name for that class
	 */
	public Set<String> getTypes(String name, int classId)
	{
		Vector<Provenance> provs = names.get(name, classId);
		HashSet<String> types = new HashSet<String>();
		if(provs == null)
			return types;
		for(Provenance p : provs)
			types.add(p.getType());
		return types;
	}
	
	/**
	 * @param name: the name to search in the Lexicon
	 * @param classId: the class to search in the Lexicon
	 * @return the weight corresponding to the provenance of the name for that class
	 */
	public double getWeight(String name, int classId)
	{
		if(!names.contains(name, classId))
			return 0.0;
		Provenance p = names.get(name, classId).get(0);
		return p.getWeight();
	}
	
	/**
	 * @param name: the name to search in the Lexicon
	 * @param classId: the class to search in the Lexicon
	 * @param lang: the language to search in the Lexicon
	 * @return the weight corresponding to the provenance of the name for that class
	 */
	public double getWeight(String name, int classId, String lang)
	{
		if(!names.contains(name, classId))
			return 0.0;
		Vector<Provenance> provs = names.get(name, classId);
		for(Provenance p : provs)
			if(p.getLanguage().equals(lang))
				return p.getWeight();
		return 0.0;
	}

	/**
	 * @param classId: the class to search in the Lexicon
	 * @return whether the class has an external name
	 */
	public boolean hasExternalName(int classId)
	{
		Set<String> classNames = getNames(classId);
		if(classNames == null)
			return false;
		for(String n : classNames)
			if(isExternal(n,classId))
				return true;
		return false;
	}

	/**
	 * @param classId: the class to search in the Lexicon
	 * @param source: the source to search in the Lexicon
	 * @return whether the class has an external name
	 */
	public boolean hasNameFromSource(int classId, String source)
	{
		Set<String> classNames = getNames(classId);
		if(classNames == null)
			return false;
		for(String n : classNames)
			if(getSources(n,classId).contains(source))
				return true;
		return false;
	}

	/**
	 * @param name: the name to search in the Lexicon
	 * @param classId: the class to search in the Lexicon
	 * @return whether the type of the name for the class
	 * is external
	 */
	public boolean isExternal(String name, int classId)
	{
		if(!names.contains(name,classId))
			return false;
		Vector<Provenance> provs = names.get(name, classId);
		for(Provenance p : provs)
			if(!p.isExternal())
				return false;
		return true;
	}
	
	/**
	 * @param name: the name to search in the Lexicon
	 * @param classId: the class to search in the Lexicon
	 * @param lang: the language to search in the Lexicon
	 * @return whether the type of the name for the class
	 * is external
	 */
	public boolean isExternal(String name, int classId, String lang)
	{
		if(!names.contains(name,classId))
			return false;
		Vector<Provenance> provs = names.get(name, classId);
		for(Provenance p : provs)
			if(p.getLanguage().equals(lang) && p.isExternal())
				return true;
		return false;
	}
	
	/**
	 * @param name: the name to search in the Lexicon
	 * @return whether the name is a formula
	 */
	public boolean isFormula(String name)
	{
		return StringParser.isFormula(name);
	}
	
	/**
	 * @return the number of names in the Lexicon
	 */
	public int nameCount()
	{
		return names.keyCount();
	}
	
	/**
	 * @param classId: the class to search in the Lexicon
	 * @return the number of names associated with the class
	 */
	public int nameCount(int classId)
	{
		return classes.entryCount(classId);
	}
	
	/**
	 * @param classId: the class to search in the Lexicon
	 * @param type: the type to restrict the search
	 * @return the number of names of the given type associated with the class
	 */
	public int nameCount(int classId, String type)
	{
		return getNames(classId,type).size();
	}
	
	/**
	 * @param classId: the class to search in the Lexicon
	 * @param type: the type to restrict the search
	 * @param lang: the language to restrict the search
	 * @return the number of names with the given type and language
	 * that are associated with the class
	 */
	public int nameCount(int classId, String type, String lang)
	{
		Set<String> hits = classes.keySet(classId);
		int count = 0;
		if(hits == null)
			return count;
		for(String n : hits)
			for(Provenance p : classes.get(classId,n))
				if(p.getLanguage().equals(lang) && p.getType().equals(type))
					count++;
		return count;
	}

	/**
	 * @return the number of entries in the Lexicon
	 */
	public int size()
	{
		return names.size();
	}
}