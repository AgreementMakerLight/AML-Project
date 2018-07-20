/******************************************************************************
* Copyright 2013-2016 LASIGE												  *
*																			  *
* Licensed under the Apache License, Version 2.0 (the "License"); you may	  *
* not use this file except in compliance with the License. You may obtain a	  *
* copy of the License at http://www.apache.org/licenses/LICENSE-2.0			  *
*																			  *
* Unless required by applicable law or agreed to in writing, software		  *
* distributed under the License is distributed on an "AS IS" BASIS,			  *
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.	  *
* See the License for the specific language governing permissions and		  *
* limitations under the License.											  *
*																			  *
*******************************************************************************
* An Ontology object.                                                         *
*																			  *
* @author Daniel Faria														  *
******************************************************************************/
package aml.ontology;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import aml.AML;
import aml.ontology.lexicon.Lexicon;
import aml.ontology.lexicon.WordLexicon;
import aml.util.Table2Set;

public class Ontology
{

//Attributes
	
	//The URI of the ontology
	protected String uri;
	//The set of entities in the ontology 
	protected HashSet<String> entities;
	//The table of entities by type in the ontology 
	protected Table2Set<EntityType,String> entityTypes;
	//Its lexicon
	protected Lexicon lex;
	//Its value map
	protected ValueMap vMap;
	//Its word lexicon
	protected WordLexicon wLex;
	//Its reference map
	protected ReferenceMap refs;
	//The map of class names -> uris in the ontology
	//which is necessary for the cross-reference matching
	protected HashMap<String,String> classNames;
	//Its set of obsolete classes
	protected HashSet<String> obsolete;
	
	//Global variables & data structures
	protected AML aml;
	protected URIMap uris;
	protected RelationshipMap rm;
	
//Constructors

	/**
	 * Constructs an empty ontology
	 */
	public Ontology()
	{
		//Initialize the data structures
		entities = new HashSet<String>();
		entityTypes = new Table2Set<EntityType,String>();
		lex = new Lexicon();
		vMap = new ValueMap();
		refs = new ReferenceMap();
		classNames = new HashMap<String,String>();
		obsolete = new HashSet<String>();
		wLex = null;
		aml = AML.getInstance();
		uris = aml.getURIMap();
		rm = aml.getRelationshipMap();
	}
	
//Public Methods

	/**
	 * Adds an entity to the Ontology
	 * @param index: the index of the entity to add
	 * @param e: the type of the entity to add
	 */
	public void add(String uri, EntityType e)
	{
		entities.add(uri);
		entityTypes.add(e, uri);
		if(e.equals(EntityType.CLASS))
		{
			String name = uris.getLocalName(uri);
			classNames.put(name, uri);
		}
	}

	/**
	 * @param uri: the uri of the entity to search in the Ontology
	 * @return whether the Ontology contains the entity with the given index
	 */
	public boolean contains(String uri)
	{
		return entities.contains(uri);
	}

	/**
	 * @param e: the EntityType to check in the Ontology
	 * @return the number of entities of EntityType e in the Ontology
	 */
	public int count(EntityType e)
	{
		return entityTypes.entryCount(e);
	}

	/**
	 * @param e: the EntityType to search in the Ontology
	 * @return the set of entities of the given type in the Ontology
	 */
	public Set<String> getEntities(EntityType e)
	{
		if(entityTypes.contains(e))
			return entityTypes.get(e);
		return new HashSet<String>();
	}

	/**
	 * @param index: the index of the term/property to get the name
	 * @return the primary name of the term/property with the given index
	 */
	public String getName(String uri)
	{
		return getLexicon().getBestName(uri);
	}

	/**
	 * @return the Ontology's Lexicon
	 */
	public Lexicon getLexicon()
	{
		return lex;
	}

	/**
	 * @return the set of class local names in the Ontology
	 */
	public Set<String> getLocalNames()
	{
		return classNames.keySet();
	}

	/**
	 * @return the Ontology's ReferenceMap
	 */
	public ReferenceMap getReferenceMap()
	{
		return refs;
	}

	/**
	 * @return the Ontology's URI
	 */
	public String getURI()
	{
		return uri;
	}

	/**
	 * @param name: the localName of the class to get from the Ontology
	 * @return the index of the corresponding name in the Ontology
	 */
	public String getURI(String name)
	{
		if(classNames.containsKey(name))
			return classNames.get(name);
		return null;
	}	
	
	/**
	 * @return this Ontology's ValueMap
	 */
	public ValueMap getValueMap()
	{
		return vMap;
	}

	/**
	 * Build a new WordLexicon of the given EntityType and without
	 * language restrictions for this Ontology, or returns the
	 * current WordLexicon if it matches these specifications.
	 * @param e: the EntityType for which to build the WordLexicon
	 * @return the WordLexicon of this Ontology
	 */
	public WordLexicon getWordLexicon(EntityType e)
	{
		if(wLex == null || !wLex.getType().equals(e) || !wLex.getLanguage().equals(""))
			wLex = new WordLexicon(lex,e);
		return wLex;
	}

	/**
	 * Build a new WordLexicon of the given EntityType and language
	 * for this Ontology, or returns the current WordLexicon if it
	 * matches these specifications.
	 * @param e: the EntityType for which to build the WordLexicon
	 * @param lang: the language of the WordLexicon
	 * @return the WordLexicon of this Ontology
	 */
	public WordLexicon getWordLexicon(EntityType e, String lang)
	{
		if(wLex == null || !wLex.getLanguage().equals(lang))
			wLex = new WordLexicon(lex,e,lang);
		return wLex;
	}

	/**
	 * @param uri: the URI of the entity in the ontology
	 * @return whether the index corresponds to an obsolete class
	 */
	public boolean isObsoleteClass(String uri)
	{
		return obsolete.contains(uri);
	}

	/**
	 * @return the number of Entities in the Ontology
	 */
	public int size()
	{
		return entities.size();
	}
	
	/**
	 * Adds an uri to the list of obsolete entities
	 * @param uri: the uri of the entity to set as obsolete
	 */
	public void setObsolete(String uri)
	{
		obsolete.add(uri);
	}
	
	/**
	 * Sets the URI of the ontology
	 * @param uri: the uri to set
	 */
	public void setURI(String uri)
	{
		this.uri = uri;
	}
}