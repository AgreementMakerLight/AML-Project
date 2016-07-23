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
* The Lexicon of an Ontology, mapping each class and property to its names    *
* and synonyms. Lexical entries are weighted according to their provenance.   *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.ontology;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import aml.AML;
import aml.util.Table3List;
import aml.settings.EntityType;
import aml.settings.LexicalType;
import aml.util.MapSorter;
import aml.util.StopList;
import aml.util.StringParser;


public class Lexicon
{

//Attributes
	
	//The table of entity names (String) to indexes (Integer) organized by EntityType
	private Table3List<String,Integer,Provenance>[] entityNames;
	//The table of entity indexes (Integer) to names (String) organized by EntityType
	private Table3List<Integer,String,Provenance>[] nameEntities;
	//The language counts
	private HashMap<String,Integer> langCount;
	//The URIMap
	private URIMap uris;
	
//Constructors

	/**
	 * Creates a new empty Lexicon, initializing the multimaps
	 * and the list of provenances
	 */
	@SuppressWarnings("unchecked")
	public Lexicon()
	{
		uris = AML.getInstance().getURIMap();
		entityNames = new Table3List[EntityType.values().length];
		for(int i = 0; i < entityNames.length; i++)
			entityNames[i] = new Table3List<String,Integer,Provenance>();
		nameEntities = new Table3List[EntityType.values().length];
		for(int i = 0; i < nameEntities.length; i++)
			nameEntities[i] = new Table3List<Integer,String,Provenance>();
		langCount = new HashMap<String,Integer>();
	}
	
	/**
	 * Creates a new Lexicon that is a copy of the given Lexicon
	 * @param l: the Lexicon to copy
	 */
	@SuppressWarnings("unchecked")
	public Lexicon(Lexicon l)
	{
		uris = AML.getInstance().getURIMap();
		entityNames = new Table3List[EntityType.values().length];
		for(int i = 0; i < entityNames.length; i++)
			entityNames[i] = new Table3List<String,Integer,Provenance>(l.entityNames[i]);
		nameEntities = new Table3List[EntityType.values().length];
		for(int i = 0; i < nameEntities.length; i++)
			nameEntities[i] = new Table3List<Integer,String,Provenance>(l.nameEntities[i]);
		langCount = new HashMap<String,Integer>(l.langCount);
	}
	
//Public Methods

	/**
	 * Adds a new entry to the Lexicon
	 * @param id: the numerical index of the entry to add
	 * @param name: the name of the entry to add
	 * @param type: the LexicalType of the entry to add (localName, label, etc)
	 * @param source: the source of the entry (ontology URI, etc)
	 * @param weight: the numeric weight of the entry, in [0.0,1.0]
	 */
	public void add(int id, String name, String language, LexicalType type, String source, double weight)
	{
		//First ensure that the name is not null or empty
		if(name == null || name.equals(""))
			return;

		String s;
		Provenance p;

		//Get the type of the entity
		EntityType e = uris.getType(id);
		int index = getIndex(e);

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
			//If it is a property, parse it as such
			else if(e.equals(EntityType.DATA) || e.equals(EntityType.OBJECT))
			{
				s = StringParser.normalizeProperty(name);
				p = new Provenance(type, source, language, weight);
			}
			//Otherwise, parse it normally
			else
			{
				s = StringParser.normalizeName(name);
				p = new Provenance(type, source, language, weight);
			}
		}
		//Then update the tables
		entityNames[index].add(s,id,p);
		nameEntities[index].add(id,s,p);
		Integer i = langCount.get(language);
		if(i == null)
			langCount.put(language, 1);
		else
			langCount.put(language, i+1);
	}
	
	/**
	 * @param e: the EntityType to check in the Lexicon
	 * @param name: the name to check in the Lexicon
	 * @return whether an entity of the given EntityType in the Lexicon contains the name
	 */
	public boolean contains(EntityType e, String name)
	{
		return entityNames[getIndex(e)].contains(name);
	}
	
	/**
	 * @param id: the index of the entity to check in the Lexicon
	 * @param name: the name to check in the Lexicon
	 * @return whether the Lexicon contains the name for the entity
	 */
	public boolean contains(int id, String name)
	{
		return nameEntities[getIndex(uris.getType(id))].contains(id, name);
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
	 * @param id: the index of the entity to check in the Lexicon
	 * @return whether the Lexicon contains a name for the entity
	 * other than a small formula (i.e., < 10 characters)
	 */
	public boolean containsNonSmallFormula(int id)
	{
		EntityType e = uris.getType(id);
		int index = getIndex(e);
		if(!nameEntities[index].contains(id))
			return false;
		for(String n : nameEntities[index].keySet(id))
		{
			if(n.length() >= 10)
				return true;
			for(Provenance p : nameEntities[index].get(id,n))
				if(!p.getType().equals(LexicalType.FORMULA))
					return true;
		}
		return false;
	}
	
	/**
	 * @param e: the EntityType to count in the Lexicon
	 * @return the number of entities of a given EntityType in the Lexicon
	 */
	public int entityCount(EntityType e)
	{
		return nameEntities[getIndex(e)].keyCount();
	}
	
	/**
	 * @param e: the EntityType to search in the Lexicon
	 * @param name: the name to search in the Lexicon
	 * @return the number of entities of the given EntityType associated with the name
	 */
	public int entityCount(EntityType e, String name)
	{
		return entityNames[getIndex(e)].entryCount(name);
	}
	
	/**
	 * @param e: the EntityType to search in the Lexicon
	 * @param name: the name to search in the Lexicon
	 * @param type: the type to restrict the search
	 * @return the number of entities of the given EntityType associated with the name with the given type
	 */
	public int entityCount(EntityType e, String name, String type)
	{
		return getEntities(e,name,type).size();
	}
	
	/**
	 * @param e: the EntityType to search in the Lexicon
	 * @param name: the name to search in the Lexicon
	 * @param p: the provenance of the names to get from the Lexicon
	 * @return the number of names with the same language as the
	 * given provenance that are associated with the class
	 */
	public int entityCount(EntityType e, String name, Provenance p)
	{
		return entityNames[getIndex(e)].entryCount(name, p);
	}
	
	/**
	 * Generates synonyms by removing within-parenthesis sections of names
	 */
	public void generateParenthesisSynonyms()
	{
		for(int i = 0; i < entityNames.length; i++)
		{
			Vector<String> nm = new Vector<String>(entityNames[i].keySet());
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
				Vector<Integer> tr = new Vector<Integer>(getInternalEntities(EntityType.values()[i], n));
				for(Integer j : tr)
					for(Provenance p : entityNames[i].get(n, j))
						add(j, newName, p.getLanguage(),
								LexicalType.INTERNAL_SYNONYM, p.getSource(), weight*p.getWeight());
			}
		}
	}
	
	/**
	 * Generates synonyms by removing leading and trailing stop words
	 * from class names, and all stop words from individual and
	 * property names
	 */
	public void generateStopWordSynonyms()
	{
		//Read the stop list
		Set<String> stopList = StopList.read();
		//Process Classes (remove only leading and trailing stop words)
		Vector<String> nm = new Vector<String>(entityNames[0].keySet());
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

			//Get the entities with the name
			Vector<Integer> tr = new Vector<Integer>(getInternalEntities(EntityType.values()[0], n));
			for(Integer i : tr)
			{
				for(Provenance p : entityNames[0].get(n, i))
				{
					double weight = p.getWeight() * 0.9;
					add(i, newName, p.getLanguage(),
							LexicalType.INTERNAL_SYNONYM, p.getSource(), weight);
				}
			}
		}
		//Process Individuals and Properties (remove all stop words)
		for(int h = 1; h < entityNames.length; h++)
		{
			nm = new Vector<String>(entityNames[h].keySet());
			for(String n: nm)
			{
				if(StringParser.isFormula(n))
					continue;
				//Build a synonym by removing all leading and trailing stopWords
				String[] nameWords = n.split(" ");
				String newName = "";
				for(int i = 0; i < nameWords.length; i++)
					if(!stopList.contains(nameWords[i]))
						newName += nameWords[i] + " ";
				newName = newName.trim();
	
				//Get the entities with the name
				Vector<Integer> tr = new Vector<Integer>(getInternalEntities(EntityType.values()[h], n));
				for(Integer i : tr)
				{
					for(Provenance p : entityNames[h].get(n, i))
					{
						double weight = p.getWeight() * 0.9;
						add(i, newName, p.getLanguage(),
								LexicalType.INTERNAL_SYNONYM, p.getSource(), weight);
					}
				}
			}
		}
	}
	
	/**
	 * @param name: the name to search in the Lexicon
	 * @param id: the index of the entity to search in the Lexicon
	 * @return the provenances associated with the name,entity pair
	 */	
	public Vector<Provenance> get(String name, int id)
	{
		int index = getIndex(uris.getType(id));
		if(entityNames[index].contains(name, id))
			return entityNames[index].get(name, id);
		return new Vector<Provenance>();
	}
	
	/**
	 * @param e: the EntityType to search in the Lexicon
	 * @param name: the name to search in the Lexicon
	 * @param internal: whether to restrict the search to internal Lexicon entries
	 * or consider extension entries
	 * @return the entity of the given EntityType associated with the name that has the highest
	 * provenance weight, or -1 if either no class or two or more such classes are found
	 */
	public int getBestEntity(EntityType e, String name, boolean internal)
	{
		Set<Integer> hits;
		if(internal)
			hits = getInternalEntities(e,name);
		else
			hits = getEntities(e,name);
		if(hits == null)
			return -1;
		
		Vector<Integer> bestEntities = new Vector<Integer>(1,1);
		double weight;
		double maxWeight = 0.0;
		
		for(Integer i : hits)
		{
			weight = getWeight(name,i);
			if(weight > maxWeight)
			{
				maxWeight = weight;
				bestEntities = new Vector<Integer>(1,1);
				bestEntities.add(i);
			}
			else if(weight == maxWeight)
			{
				bestEntities.add(i);
			}
		}
		if(bestEntities.size() != 1)
			return -1;
		return bestEntities.get(0);
	}
	
	/**
	 * @param id: the index of the entity to search in the Lexicon
	 * @return the name associated with the entity that has the best provenance
	 */
	public String getBestName(int id)
	{
		int index = getIndex(uris.getType(id));
		String lang = AML.getInstance().getLabelLanguage();
		Map<String,Provenance> results = new HashMap<String,Provenance>();
		if(!nameEntities[index].contains(id))
			return "";

		for(String n : nameEntities[index].keySet(id))
		{
			for(Provenance p : nameEntities[index].get(id, n))
			{
				if(p.getLanguage().equals(lang))
				{
					results.put(n,p);
					break;
				}
			}
		}
		if(results.size() == 0)
		{
			for(String n : nameEntities[index].keySet(id))
				results.put(n,nameEntities[index].get(id, n).iterator().next());
		}
		results = MapSorter.sortDescending(results);
		return results.keySet().iterator().next();
	}
	
	/**
	 * @param name: the name to search in the Lexicon
	 * @param id: the index of the entity to search in the Lexicon
	 * @return the weight corresponding to the provenance of the name for that class with a
	 * correction factor depending on how many names of that provenance the the class has
	 */
	public double getCorrectedWeight(String name, int id)
	{
		EntityType e = uris.getType(id);
		int index = getIndex(e);
		if(!entityNames[index].contains(name, id))
			return 0.0;
		double weight = 0.0;
		double correction = 0.0;
		for(Provenance p : entityNames[index].get(name, id))
		{
			if(p.getWeight() > weight)
			{
				weight = p.getWeight();
				correction = nameCount(id,p.getType())/100.0;
			}
		}
		return weight - correction;
	}
	
	/**
	 * @param name: the name to search in the Lexicon
	 * @param id: the index of the entity to search in the Lexicon
	 * @param lang: the language to search in the Lexicon
	 * @return the weight corresponding to the provenance of the name for that class
	 * with a correction factor depending on how many names of that provenance the
	 * the class has
	 */
	public double getCorrectedWeight(String name, int id, String lang)
	{
		EntityType e = uris.getType(id);
		int index = getIndex(e);
		Vector<Provenance> provs = entityNames[index].get(name, id);
		if(provs == null)
			return 0.0;
		for(Provenance p : provs)
		{
			if(p.getLanguage().equals(lang))
			{
				double correction = nameCount(id,p.getType(),p.getLanguage())/100.0;
				return p.getWeight() - correction;
			}
		}
		return 0.0;
	}
	
	/**
	 * @param e: the EntityType to get from the Lexicon
	 * @return the set of entities of the given EntityType in the Lexicon
	 */
	public Set<Integer> getEntities(EntityType e)
	{
		return nameEntities[getIndex(e)].keySet();
	}
	
	/**
	 * @param e: the EntityType to get from the Lexicon
	 * @param name: the class name to search in the Lexicon
	 * @return the list of classes associated with the name
	 */
	public Set<Integer> getEntities(EntityType e, String name)
	{
		return entityNames[getIndex(e)].keySet(name);
	}
	
	/**
	 * @param e: the EntityType to get from the Lexicon
	 * @param name: the class name to search in the Lexicon
	 * @param type: the type to filter the search
	 * @return the list of classes associated with the name with the given type
	 */
	public Set<Integer> getEntities(EntityType e, String name, String type)
	{
		int index = getIndex(e);
		Set<Integer> hits = entityNames[index].keySet(name);
		HashSet<Integer> entitiesType = new HashSet<Integer>();
		if(hits == null)
			return entitiesType;
		for(Integer i : hits)
			for(Provenance p : entityNames[index].get(name,i))
				if(p.getType().equals(type))
					entitiesType.add(i);
		return entitiesType;
	}
	
	/**
	 * @param e: the EntityType to get from the Lexicon
	 * @param name: the class name to search in the Lexicon
	 * @param lang: the language of the names to get from the Lexicon
	 * @return the list of classes associated with the name with the
	 * given language
	 */
	public Set<Integer> getEntitiesWithLanguage(EntityType e, String name, String lang)
	{
		int index = getIndex(e);
		Set<Integer> hits = entityNames[index].keySet(name);
		HashSet<Integer> classesLang = new HashSet<Integer>();
		if(hits == null)
			return classesLang;
		for(Integer i : hits)
			for(Provenance p : entityNames[index].get(name,i))
				if(p.getLanguage().equals(lang))
					classesLang.add(i);
		return classesLang;
	}
	
	/**
	 * @param e: the EntityType to get from the Lexicon
	 * @param source: the source to search in the Lexicon
	 * @return the list of entities of the given EntityType that have names from the given source
	 */
	public Vector<Integer> getEntitiesWithSource(EntityType e, String source)
	{
		Vector<Integer> entitiesWithSource = new Vector<Integer>(0,1);
		Set<Integer> ts = nameEntities[getIndex(e)].keySet();
		for(Integer i : ts)
			if(hasNameFromSource(i,source) && !entitiesWithSource.contains(i))
				entitiesWithSource.add(i);
		return entitiesWithSource;
	}
	
	/**
	 * @param e: the EntityType to get from the Lexicon
	 * @return the list of entities of the given EntityType that have a name from an external source
	 */
	public Set<Integer> getExtendedEntities(EntityType e)
	{
		HashSet<Integer> extendedEntities = new HashSet<Integer>(0,1);
		Set<Integer> ts = nameEntities[getIndex(e)].keySet();
		for(Integer i : ts)
			if(hasExternalName(i))
				extendedEntities.add(i);
		return extendedEntities;
	}
	
	/**
	 * @param id: the index of the entity to search in the Lexicon
	 * @return the list of local names associated with the class
	 */
	public Set<String> getInternalNames(int id)
	{
		int index = getIndex(uris.getType(id));
		HashSet<String> localHits = new HashSet<String>();
		if(nameEntities[index].contains(id))
		{
			Set<String> hits = nameEntities[index].keySet(id);
			for(String s : hits)
				if(!isExternal(s,id))
					localHits.add(s);
		}
		return localHits;
	}
	
	/**
	 * @param e: the EntityType to get from the Lexicon
	 * @param name: the class name to search in the Lexicon
	 * @return the list of classes associated with the name from
	 * a local source
	 */
	public Set<Integer> getInternalEntities(EntityType e, String name)
	{
		Set<Integer> hits = entityNames[getIndex(e)].keySet(name);
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
	 * @param e: the EntityType to get from the Lexicon
	 * @param name: the class name to search in the Lexicon
	 * @return the list of languages declared for the name
	 */
	public Set<String> getLanguages(EntityType e, String name)
	{
		int index = getIndex(e);
		Set<Integer> hits = entityNames[index].keySet(name);
		HashSet<String> langs = new HashSet<String>();
		if(hits == null)
			return langs;
		for(Integer i : hits)
			for(Provenance p : entityNames[index].get(name,i))
				langs.add(p.getLanguage());
		return langs;
	}
	
	/**
	 * @param name: the name to search in the Lexicon
	 * @param id: the index of the entity to search in the Lexicon
	 * @return the list of languages declared for the name,entity pair
	 */
	public Set<String> getLanguages(String name, int id)
	{
		int index = getIndex(uris.getType(id));
		Vector<Provenance> hits = entityNames[index].get(name,id);
		HashSet<String> langs = new HashSet<String>();
		if(hits == null)
			return langs;
		for(Provenance p : hits)
			langs.add(p.getLanguage());
		return langs;
	}
	
	/**
	 * @param e: the EntityType to get from the Lexicon
	 * @return the set of names of the given EntityType in the Lexicon
	 */
	public Set<String> getNames(EntityType e)
	{
		return entityNames[getIndex(e)].keySet();
	}

	/**
	 * @param id: the index of the entity to search in the Lexicon
	 * @return the list of names associated with the entity
	 */
	public Set<String> getNames(int id)
	{
		int index = getIndex(uris.getType(id));
		if(nameEntities[index].contains(id))
			return nameEntities[index].keySet(id);
		return new HashSet<String>();
	}
	
	/**
	 * @param id: the index of the entity to search in the Lexicon
	 * @param type: the type to restrict the search
	 * @return the list of names of the given type associated with the entity
	 */
	public Set<String> getNames(int id, LexicalType type)
	{
		int index = getIndex(uris.getType(id));
		HashSet<String> namesType = new HashSet<String>();
		if(nameEntities[index].contains(id))
		{
			for(String n : nameEntities[index].keySet(id))
				for(Provenance p : nameEntities[index].get(id,n))
					if(p.getType().equals(type))
						namesType.add(n);
		}
		return namesType;
	}
	
	/**
	 * @param id: the index of the entity to search in the Lexicon
	 * @param lang: the lang of the names to get from the Lexicon
	 * @return the names with the given language associated with the entity
	 */
	public Set<String> getNamesWithLanguage(int id, String lang)
	{
		int index = getIndex(uris.getType(id));
		HashSet<String> namesLang = new HashSet<String>();
		if(nameEntities[index].contains(id))
		{
			for(String n : nameEntities[index].keySet(id))
				for(Provenance p : nameEntities[index].get(id,n))
					if(p.getLanguage().equals(lang))
						namesLang.add(n);
		}
		return namesLang;
	}
	
	/**
	 * @param name: the name to search in the Lexicon
	 * @param id: the index of the entity to search in the Lexicon
	 * @return the sources of the name for that entity
	 */
	public Set<String> getSources(String name, int id)
	{
		int index = getIndex(uris.getType(id));
		Vector<Provenance> provs = entityNames[index].get(name, id);
		HashSet<String> sources = new HashSet<String>();
		if(provs == null)
			return sources;
		for(Provenance p : provs)
			sources.add(p.getSource());
		return sources;
	}
	
	/**
	 * @param name: the name to search in the Lexicon
	 * @param id: the index of the entity to search in the Lexicon
	 * @return the best type of the name for that class
	 */
	public LexicalType getType(String name, int id)
	{
		int index = getIndex(uris.getType(id));
		LexicalType type = null;
		double weight = 0.0;
		for(Provenance p : entityNames[index].get(name, id))
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
	 * @param id: the index of the entity to search in the Lexicon
	 * @return the types of the name for that class
	 */
	public Set<LexicalType> getTypes(String name, int id)
	{
		int index = getIndex(uris.getType(id));
		Vector<Provenance> provs = entityNames[index].get(name, id);
		HashSet<LexicalType> types = new HashSet<LexicalType>();
		if(provs == null)
			return types;
		for(Provenance p : provs)
			types.add(p.getType());
		return types;
	}
	
	/**
	 * @param name: the name to search in the Lexicon
	 * @param id: the index of the entity to search in the Lexicon
	 * @return the best weight of the name for that entity
	 */
	public double getWeight(String name, int id)
	{
		int index = getIndex(uris.getType(id));
		double weight = 0.0;
		if(nameEntities[index].contains(id,name))
		{
			for(Provenance p : nameEntities[index].get(id,name))
				if(p.getWeight() > weight)
					weight = p.getWeight();
		}
		return weight;
	}
	
	/**
	 * @param name: the name to search in the Lexicon
	 * @param id: the index of the entity to search in the Lexicon
	 * @param lang: the language to search in the Lexicon
	 * @return the weight corresponding to the provenance of the name for that class
	 */
	public double getWeight(String name, int id, String lang)
	{
		int index = getIndex(uris.getType(id));
		if(!entityNames[index].contains(name, id))
			return 0.0;
		Vector<Provenance> provs = entityNames[index].get(name, id);
		for(Provenance p : provs)
			if(p.getLanguage().equals(lang))
				return p.getWeight();
		return 0.0;
	}

	/**
	 * @param id: the class to search in the Lexicon
	 * @return whether the class has an external name
	 */
	public boolean hasExternalName(int id)
	{
		Set<String> entityNames = getNames(id);
		if(entityNames == null)
			return false;
		for(String n : entityNames)
			if(isExternal(n,id))
				return true;
		return false;
	}

	/**
	 * @param id: the index of the entity to search in the Lexicon
	 * @param source: the source to search in the Lexicon
	 * @return whether the class has an external name
	 */
	public boolean hasNameFromSource(int id, String source)
	{
		Set<String> entityNames = getNames(id);
		if(entityNames == null)
			return false;
		for(String n : entityNames)
			if(getSources(n,id).contains(source))
				return true;
		return false;
	}

	/**
	 * @param name: the name to search in the Lexicon
	 * @param id: the index of the entity to search in the Lexicon
	 * @return whether the type of the name for the entity
	 * is external
	 */
	public boolean isExternal(String name, int id)
	{
		int index = getIndex(uris.getType(id));
		if(nameEntities[index].contains(id,name))
		{
			Vector<Provenance> provs = nameEntities[index].get(id, name);
			for(Provenance p : provs)
				if(!p.isExternal())
					return false;
			return true;
		}
		return false;
	}
	
	/**
	 * @param name: the name to search in the Lexicon
	 * @param id: the index of the entity to search in the Lexicon
	 * @param lang: the language to search in the Lexicon
	 * @return whether the type of the name for the class
	 * is external
	 */
	public boolean isExternal(String name, int id, String lang)
	{
		int index = getIndex(uris.getType(id));
		if(!entityNames[index].contains(name,id))
			return false;
		Vector<Provenance> provs = entityNames[index].get(name, id);
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
	 * @param e: the EntityType to get from the Lexicon
	 * @return the number of names in the Lexicon
	 */
	public int nameCount(EntityType e)
	{
		return entityNames[getIndex(e)].keyCount();
	}
	
	/**
	 * @param id: the index of the entity to search in the Lexicon
	 * @return the number of names associated with the entity
	 */
	public int nameCount(int id)
	{
		int index = getIndex(uris.getType(id));
		return nameEntities[index].entryCount(id);
	}
	
	/**
	 * @param id: the index of the entity to search in the Lexicon
	 * @param type: the type to restrict the search
	 * @return the number of names of the given type associated with the entity
	 */
	public int nameCount(int id, LexicalType type)
	{
		return getNames(id,type).size();
	}
	
	/**
	 * @param id: the index of the entity to search in the Lexicon
	 * @param type: the type to restrict the search
	 * @param lang: the language to restrict the search
	 * @return the number of names with the given type and language
	 * that are associated with the class
	 */
	public int nameCount(int id, LexicalType type, String lang)
	{
		int index = getIndex(uris.getType(id));
		Set<String> hits = nameEntities[index].keySet(id);
		int count = 0;
		if(hits == null)
			return count;
		for(String n : hits)
			for(Provenance p : nameEntities[index].get(id,n))
				if(p.getLanguage().equals(lang) && p.getType().equals(type))
					count++;
		return count;
	}
	
	/**
	 * @return the number of class name entries in the Lexicon
	 */
	public int size()
	{
		int size = 0;
		for(Table3List<String,Integer,Provenance> t : entityNames)
			size += t.size();
		return size;
	}
	
	
//Private Methods
	
	private int getIndex(EntityType e)
	{
		EntityType[] types = EntityType.values();
		for(int i = 0; i < types.length; i++)
			if(types[i].equals(e))
				return i;
		return -1;
	}
}