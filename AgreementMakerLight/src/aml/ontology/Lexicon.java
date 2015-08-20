/******************************************************************************
* Copyright 2013-2015 LASIGE                                                  *
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
* The Lexicon of an Ontology, mapping each class and property to its names    *
* and synonyms. Lexical entries are weighted according to their provenance.   *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 26-05-2015                                                            *
******************************************************************************/
package aml.ontology;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import aml.AML;
import aml.util.Table3List;
import aml.settings.LexicalType;
import aml.util.StopList;
import aml.util.StringParser;


public class Lexicon
{

//Attributes
	
	//The map of names (String) to class indexes (Integer)
	private Table3List<String,Integer,Provenance> classNames;
	//The map of class indexes (Integer) to names (String)
	private Table3List<Integer,String,Provenance> nameClasses;
	//The map of property indexes (Integer) to names (String)
	private Table3List<Integer,String,Provenance> nameProperties;
	//The language counts
	private HashMap<String,Integer> langCount;
	
//Constructors

	/**
	 * Creates a new empty Lexicon, initializing the multimaps
	 * and the list of provenances
	 */
	public Lexicon()
	{
		classNames = new Table3List<String,Integer,Provenance>();
		nameClasses = new Table3List<Integer,String,Provenance>();
		nameProperties = new Table3List<Integer,String,Provenance>();
		langCount = new HashMap<String,Integer>();
	}
	
	/**
	 * Creates a new Lexicon that is a copy of the given Lexicon
	 * @param l: the Lexicon to copy
	 */
	public Lexicon(Lexicon l)
	{
		classNames = new Table3List<String,Integer,Provenance>(l.classNames);
		nameClasses = new Table3List<Integer,String,Provenance>(l.nameClasses);
		nameProperties = new Table3List<Integer,String,Provenance>(l.nameProperties);
		langCount = new HashMap<String,Integer>(l.langCount);
	}
	
	/**
	 * Reads a Lexicon from a given Lexicon file
	 * @param file: the Lexicon file
	 */
	public Lexicon(String file) throws Exception
	{
		this();
		BufferedReader inStream = new BufferedReader(new FileReader(file));
		String line;
		while((line = inStream.readLine()) != null)
		{
			String[] lex = line.split("\t");
			int id = Integer.parseInt(lex[0]);
			String name = lex[1];
			LexicalType type = LexicalType.parseLexicalType(lex[2]);
			double weight = type.getDefaultWeight();
			addClass(id,name,type,"",weight);
		}
		inStream.close();
	}

//Public Methods

	/**
	 * Adds a new entry to the Lexicon
	 * @param classId: the class to which the name belongs
	 * @param name: the name to add to the Lexicon
	 * @param type: the type of lexical entry (localName, label, etc)
	 * @param source: the source of the lexical entry (ontology URI, etc)
	 */
	public void addClass(int classId, String name, LexicalType type, String source, double weight)
	{
		//First ensure that the name is not null or empty, and (since we're assuming that
		//the language is English by default, ensure that it contains Latin characters)
		if(name == null || name.equals("") || !name.matches(".*[a-zA-Z].*"))
			return;

		String s;
		String lang = "en";
		Provenance p;

		//If it is a formula, parse it and label it as such
		if(StringParser.isFormula(name))
		{
			s = StringParser.normalizeFormula(name);
			p = new Provenance(LexicalType.FORMULA, source, lang, weight);
		}
		//Otherwise, parse it normally
		else
		{
			s = StringParser.normalizeName(name);
			p = new Provenance(type, source, lang, weight);
		}
		//Then update the tables
		classNames.add(s,classId,p);
		nameClasses.add(classId,s,p);
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
	public void addClass(int classId, String name, String language, LexicalType type, String source, double weight)
	{
		//First ensure that the name is not null or empty
		if(name == null || name.equals(""))
			return;

		String s;
		Provenance p;

		//If the name is not in english we parse it as a formula
		if(!language.equals("en"))
		{
			s = StringParser.normalizeFormula(name);
			p = new Provenance(type, source, language, weight);
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
				p = new Provenance(LexicalType.FORMULA, source, language, weight);
			}
			//Otherwise, parse it normally
			else
			{
				s = StringParser.normalizeName(name);
				p = new Provenance(type, source, language, weight);
			}
		}
		//Then update the tables
		classNames.add(s,classId,p);
		nameClasses.add(classId,s,p);
		Integer i = langCount.get(language);
		if(i == null)
			langCount.put(language, 1);
		else
			langCount.put(language, i+1);
	}
	
	/**
	 * Adds a new entry to the Lexicon
	 * @param propId: the property to which the name belongs
	 * @param name: the name to add to the Lexicon
	 * @param type: the type of lexical entry (localName, label, etc)
	 * @param source: the source of the lexical entry (ontology URI, etc)
	 */
	public void addProperty(int propId, String name, LexicalType type, String source, double weight)
	{
		//First ensure that the name is not null or empty, and (since we're assuming that
		//the language is English by default, ensure that it contains Latin characters)
		if(name == null || name.equals("") || !name.matches(".*[a-zA-Z].*"))
			return;

		String s = StringParser.normalizeProperty(name);
		String lang = "en";
		Provenance p = new Provenance(type, source, lang, weight);
		
		//Then update the tables
		nameProperties.add(propId,s,p);
		Integer i = langCount.get(lang);
		if(i == null)
			langCount.put(lang, 1);
		else
			langCount.put(lang, i+1);
	}
	
	/**
	 * Adds a new entry to the Lexicon
	 * @param propId: the property to which the name belongs
	 * @param name: the name to add to the Lexicon
	 * @param language: the language of the name
	 * @param type: the type of lexical entry (localName, label, etc)
	 * @param source: the source of the lexical entry (ontology URI, etc)
	 */
	public void addProperty(int propId, String name, String language, LexicalType type, String source, double weight)
	{
		//First ensure that the name is not null or empty
		if(name == null || name.equals(""))
			return;

		String s;
		Provenance p;

		//If the name is not in english we parse it as a formula
		if(!language.equals("en"))
		{
			s = StringParser.normalizeFormula(name);
			p = new Provenance(type, source, language, weight);
		}
		//Otherwise
		else
		{
			s = StringParser.normalizeProperty(name);
			p = new Provenance(type, source, language, weight);
		}
		//Then update the tables
		nameProperties.add(propId,s,p);
		Integer i = langCount.get(language);
		if(i == null)
			langCount.put(language, 1);
		else
			langCount.put(language, i+1);
	}
		
	/**
	 * @return the number of classes in the Lexicon
	 */
	public int classCount()
	{
		return nameClasses.keyCount();
	}
	
	/**
	 * @param name: the name to search in the Lexicon
	 * @return the number of classes associated with the name
	 */
	public int classCount(String name)
	{
		return classNames.entryCount(name);
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
		return classNames.entryCount(name, p);
	}
	
	/**
	 * @param name: the name to check in the Lexicon
	 * @return whether a class in the Lexicon contains the name
	 */
	public boolean contains(String name)
	{
		return classNames.contains(name);
	}
	
	/**
	 * @param entityId: the entity to check in the Lexicon
	 * @param name: the name to check in the Lexicon
	 * @return whether the Lexicon contains the name for the entity
	 * (class or property)
	 */
	public boolean contains(int entityId, String name)
	{
		return (nameClasses.contains(entityId) && nameClasses.get(entityId).contains(name)) ||
				(nameProperties.contains(entityId) && nameProperties.get(entityId).contains(name));
	}
	
	/**
	 * @param lang the language to search in the Lexicon
	 * @return whether the Lexicon contains names with the given language
	 */
	public boolean containsLanguage(String lang)
	{
		return langCount.containsKey(lang);
	}
	
	/**
	 * @param classId: the class to check in the Lexicon
	 * @return whether the Lexicon contains a name for the class
	 * other than a small formula (i.e., < 10 characters)
	 */
	public boolean containsNonSmallFormula(int classId)
	{
		if(!nameClasses.contains(classId))
			return false;
		for(String n : nameClasses.keySet(classId))
		{
			if(n.length() >= 10)
				return true;
			for(Provenance p : nameClasses.get(classId,n))
				if(!p.getType().equals(LexicalType.FORMULA))
					return true;
		}
		return false;
	}
	
	/**
	 * Generates synonyms by removing within-parenthesis sections of names
	 */
	public void generateParenthesisSynonyms()
	{
		Vector<String> nm = new Vector<String>(classNames.keySet());
		for(String n: nm)
		{
			if(StringParser.isFormula(n) || !n.contains("(") || !n.contains(")"))
				continue;
			String newName;
			double weight = 0.0;
			if(n.matches("\\([^()]+\\)") || n.contains(") or ("))
			{
				newName = n.replaceAll("[()]", "");
				weight = 1.0;
			}
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
				weight = Math.sqrt(newName.length() * 1.0 / n.length());
			}
			if(newName.equals(""))
				continue;
			//Get the classes with the name
			Vector<Integer> tr = new Vector<Integer>(getInternalClasses(n));
			for(Integer i : tr)
				for(Provenance p : classNames.get(n, i))
					addClass(i, newName, p.getLanguage(), LexicalType.INTERNAL_SYNONYM,
							p.getSource(), weight*p.getWeight());
		}
	}
	
	/**
	 * Generates synonyms by removing leading and trailing stop words from class names
	 */
	public void generateStopWordSynonyms()
	{
		//Read the stop list
		Set<String> stopList = StopList.read();
		//Process classes
		Vector<String> nm = new Vector<String>(classNames.keySet());
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
				for(Provenance p : classNames.get(n, i))
				{
					double weight = p.getWeight() * 0.9;
					addClass(i, newName, p.getLanguage(), LexicalType.INTERNAL_SYNONYM, p.getSource(), weight);
				}
			}
		}
		//Process properties
		for(Integer i : nameProperties.keySet())
		{
			nm = new Vector<String>(nameProperties.keySet(i));
			for(String n: nm)
			{	
				//Build a synonym by removing all stopWords
				String[] nameWords = n.split(" ");
				String newName = "";
				for(String w : nameWords)
					if(!stopList.contains(w))
						newName += w + " ";
				newName = newName.trim();

				for(Provenance p : nameProperties.get(i, n))
				{
					double weight = p.getWeight() * 0.9;
					addProperty(i, newName, p.getLanguage(), LexicalType.INTERNAL_SYNONYM, p.getSource(), weight);
				}
			}
		}
	}
	
	/**
	 * @param name: the name to search in the Lexicon
	 * @param entityId: the entity to search in the Lexicon
	 * @return the provenances associated with the name,entity pair
	 */	
	public Vector<Provenance> get(String name, int entityId)
	{
		if(classNames.contains(name, entityId))
			return classNames.get(name, entityId);
		if(nameProperties.contains(entityId,name))
			return nameProperties.get(entityId, name);
		return new Vector<Provenance>();
	}
	
	/**
	 * @param entityId: the entity (class or property) to search in the Lexicon
	 * @return the name associated with the class that has the highest
	 * provenance weight
	 */
	public String getBestName(int entityId)
	{
		Set<String> hits = getNamesWithLanguage(entityId, AML.getInstance().getLabelLanguage());
		if(hits.size() == 0)
			hits = getInternalNames(entityId);
		String bestName = "";
		double weight;
		double maxWeight = 0.0;
		
		for(String n : hits)
		{
			weight = getWeight(n,entityId);
			if(weight > maxWeight)
			{
				maxWeight = weight;
				bestName = n;
			}
		}
		return bestName;
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
		return nameClasses.keySet();
	}
	
	/**
	 * @param name: the class name to search in the Lexicon
	 * @return the list of classes associated with the name
	 */
	public Set<Integer> getClasses(String name)
	{
		return classNames.keySet(name);
	}
	
	/**
	 * @param name: the class name to search in the Lexicon
	 * @param type: the type to filter the search
	 * @return the list of classes associated with the name with the given type
	 */
	public Set<Integer> getClasses(String name, String type)
	{
		Set<Integer> hits = classNames.keySet(name);
		HashSet<Integer> classesType = new HashSet<Integer>();
		if(hits == null)
			return classesType;
		for(Integer i : hits)
			for(Provenance p : classNames.get(name,i))
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
		Set<Integer> hits = classNames.keySet(name);
		HashSet<Integer> classesLang = new HashSet<Integer>();
		if(hits == null)
			return classesLang;
		for(Integer i : hits)
			for(Provenance p : classNames.get(name,i))
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
		Set<Integer> ts = nameClasses.keySet();
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
		if(!classNames.contains(name, classId))
			return 0.0;
		double weight = 0.0;
		double correction = 0.0;
		for(Provenance p : classNames.get(name, classId))
		{
			if(p.getWeight() > weight)
			{
				weight = p.getWeight();
				correction = nameCount(classId,p.getType())/100.0;
			}
		}
		return weight - correction;
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
		Vector<Provenance> provs = classNames.get(name, classId);
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
		Set<Integer> ts = nameClasses.keySet();
		for(Integer i : ts)
			if(hasExternalName(i))
				extendedClasses.add(i);
		return extendedClasses;
	}
	
	/**
	 * @param entityId: the entity (class or property) to search in the Lexicon
	 * @return the list of local names associated with the class
	 */
	public Set<String> getInternalNames(int entityId)
	{
		HashSet<String> localHits = new HashSet<String>();
		if(nameClasses.contains(entityId))
		{
			Set<String> hits = nameClasses.keySet(entityId);
			for(String s : hits)
				if(!isExternal(s,entityId))
					localHits.add(s);
		}
		else if(nameProperties.contains(entityId))
		{
			Set<String> hits = nameProperties.keySet(entityId);
			for(String s : hits)
				if(!isExternal(s,entityId))
					localHits.add(s);
		}
		return localHits;
	}
	
	/**
	 * @param name: the class name to search in the Lexicon
	 * @return the list of classes associated with the name from
	 * a local source
	 */
	public Set<Integer> getInternalClasses(String name)
	{
		Set<Integer> hits = classNames.keySet(name);
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
		Set<Integer> hits = classNames.keySet(name);
		HashSet<String> langs = new HashSet<String>();
		if(hits == null)
			return langs;
		for(Integer i : hits)
			for(Provenance p : classNames.get(name,i))
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
		Vector<Provenance> hits = classNames.get(name,classId);
		HashSet<String> langs = new HashSet<String>();
		if(hits == null)
			return langs;
		for(Provenance p : hits)
			langs.add(p.getLanguage());
		return langs;
	}
	
	/**
	 * @return the set of class names in the Lexicon
	 */
	public Set<String> getNames()
	{
		return classNames.keySet();
	}

	/**
	 * @param entityId: the entity (class or property) to search in the Lexicon
	 * @return the list of names associated with the entity
	 */
	public Set<String> getNames(int entityId)
	{
		if(nameClasses.contains(entityId))
			return nameClasses.keySet(entityId);
		if(nameProperties.contains(entityId))
			return nameProperties.keySet(entityId);
		return new HashSet<String>();
	}
	
	/**
	 * @param entityId: the entity (class or property) to search in the Lexicon
	 * @param type: the type to restrict the search
	 * @return the list of names of the given type associated with the entity
	 */
	public Set<String> getNames(int entityId, LexicalType type)
	{
		HashSet<String> namesType = new HashSet<String>();
		if(nameClasses.contains(entityId))
		{
			for(String n : nameClasses.keySet(entityId))
				for(Provenance p : nameClasses.get(entityId,n))
					if(p.getType().equals(type))
						namesType.add(n);
		}
		else if(nameProperties.contains(entityId))
		{
			for(String n : nameProperties.keySet(entityId))
				for(Provenance p : nameProperties.get(entityId,n))
					if(p.getType().equals(type))
						namesType.add(n);

		}
		return namesType;
	}
	
	/**
	 * @param entityId: the entity (class or property) to search in the Lexicon
	 * @param lang: the lang of the names to get from the Lexicon
	 * @return the names with the given language associated with the entity
	 */
	public Set<String> getNamesWithLanguage(int entityId, String lang)
	{
		HashSet<String> namesLang = new HashSet<String>();
		if(nameClasses.contains(entityId))
		{
			for(String n : nameClasses.keySet(entityId))
				for(Provenance p : nameClasses.get(entityId,n))
					if(p.getLanguage().equals(lang))
						namesLang.add(n);
		}
		else if(nameProperties.contains(entityId))
		{
			for(String n : nameProperties.keySet(entityId))
				for(Provenance p : nameProperties.get(entityId,n))
					if(p.getLanguage().equals(lang))
						namesLang.add(n);

		}
		return namesLang;
	}
	
	/**
	 * @return the set of properties in this Lexicon
	 */
	public Set<Integer> getProperties()
	{
		return nameProperties.keySet();
	}
	
	/**
	 * @param name: the name to search in the Lexicon
	 * @param classId: the class to search in the Lexicon
	 * @return the sources of the name for that class
	 */
	public Set<String> getSources(String name, int classId)
	{
		Vector<Provenance> provs = classNames.get(name, classId);
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
	 * @return the best type of the name for that class
	 */
	public LexicalType getType(String name, int classId)
	{
		LexicalType type = null;
		double weight = 0.0;
		for(Provenance p : classNames.get(name, classId))
		{
			if(p.getWeight() > weight)
			{
				weight = p.getWeight();
				type = p.getType();
			}
		}
		return type;
	}
	
	/**
	 * @param name: the name to search in the Lexicon
	 * @param classId: the class to search in the Lexicon
	 * @return the types of the name for that class
	 */
	public Set<LexicalType> getTypes(String name, int classId)
	{
		Vector<Provenance> provs = classNames.get(name, classId);
		HashSet<LexicalType> types = new HashSet<LexicalType>();
		if(provs == null)
			return types;
		for(Provenance p : provs)
			types.add(p.getType());
		return types;
	}
	
	/**
	 * @param name: the name to search in the Lexicon
	 * @param entityId: the entity to search in the Lexicon
	 * @return the best weight of the name for that entity
	 */
	public double getWeight(String name, int entityId)
	{
		double weight = 0.0;
		if(nameClasses.contains(entityId,name))
		{
			for(Provenance p : nameClasses.get(entityId,name))
				if(p.getWeight() > weight)
					weight = p.getWeight();
		}
		else if(nameProperties.contains(entityId,name))
		{
			for(Provenance p : nameProperties.get(entityId,name))
				if(p.getWeight() > weight)
					weight = p.getWeight();
		}
		return weight;
	}
	
	/**
	 * @param name: the name to search in the Lexicon
	 * @param classId: the class to search in the Lexicon
	 * @param lang: the language to search in the Lexicon
	 * @return the weight corresponding to the provenance of the name for that class
	 */
	public double getWeight(String name, int classId, String lang)
	{
		if(!classNames.contains(name, classId))
			return 0.0;
		Vector<Provenance> provs = classNames.get(name, classId);
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
	 * @param entityId: the entity to search in the Lexicon
	 * @return whether the type of the name for the entity
	 * is external
	 */
	public boolean isExternal(String name, int entityId)
	{
		if(nameClasses.contains(entityId,name))
		{
			Vector<Provenance> provs = nameClasses.get(entityId, name);
			for(Provenance p : provs)
				if(!p.isExternal())
					return false;
			return true;
		}
		else if(nameProperties.contains(entityId,name))
		{
			Vector<Provenance> provs = nameProperties.get(entityId, name);
			for(Provenance p : provs)
				if(!p.isExternal())
					return false;
			return true;
		}
		return false;
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
		if(!classNames.contains(name,classId))
			return false;
		Vector<Provenance> provs = classNames.get(name, classId);
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
		return classNames.keyCount();
	}
	
	/**
	 * @param classId: the class to search in the Lexicon
	 * @return the number of names associated with the class
	 */
	public int nameCount(int classId)
	{
		return nameClasses.entryCount(classId);
	}
	
	/**
	 * @param classId: the class to search in the Lexicon
	 * @param type: the type to restrict the search
	 * @return the number of names of the given type associated with the class
	 */
	public int nameCount(int classId, LexicalType type)
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
	public int nameCount(int classId, LexicalType type, String lang)
	{
		Set<String> hits = nameClasses.keySet(classId);
		int count = 0;
		if(hits == null)
			return count;
		for(String n : hits)
			for(Provenance p : nameClasses.get(classId,n))
				if(p.getLanguage().equals(lang) && p.getType().equals(type))
					count++;
		return count;
	}
	
	/**
	 * @return the number of properties in the Lexicon
	 */
	public int propertyCount()
	{
		return nameProperties.keyCount();
	}
	
	/**
	 * Saves this Lexicon to the specified file
	 * @param file: the file on which to save the Lexicon
	 */
	public void save(String file) throws Exception
	{
		PrintWriter outStream = new PrintWriter(new FileOutputStream(file));
		for(Integer i : nameClasses.keySet())
			for(String n : nameClasses.keySet(i))
				outStream.println(i + "\t" + n + "\t" + getType(n,i));
		outStream.close();
	}

	/**
	 * @return the number of class name entries in the Lexicon
	 */
	public int size()
	{
		return classNames.size();
	}
}