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
* An Ontology object built using the OWL API.                                 *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.ontology;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import aml.settings.EntityType;
import aml.util.Table2Set;

public abstract class Ontology
{
	
//Attributes
	
	//The OWL Ontology Manager and Data Factory
	protected OWLOntologyManager manager;
	protected OWLDataFactory factory;
	//The entity expansion limit property
    protected final String LIMIT = "entityExpansionLimit"; 
	//The URI of the ontology
	protected String uri;
	//The set of entities in the ontology 
	protected HashSet<Integer> entities;
	//The table of entities by type in the ontology 
	protected Table2Set<EntityType,Integer> entityTypes;
	//Its lexicon
	protected Lexicon lex;

//Constructors
	
	public Ontology()
	{
        //Increase the entity expansion limit to allow large ontologies
        System.setProperty(LIMIT, "1000000");
        //Get an Ontology Manager and Data Factory
        manager = OWLManager.createOWLOntologyManager();
        factory = manager.getOWLDataFactory();
        //Initialize the data structures
        entities = new HashSet<Integer>();
        entityTypes = new Table2Set<EntityType,Integer>();
        lex = new Lexicon();
	}

//Public Methods
	
	/**
	 * Erases the Ontology data structures
	 */
	public void close()
	{
		manager = null;
		factory = null;
		entities = null;
		lex = null;
		uri = null;
	}
	
	/**
	 * @return the number of Classes in the Ontology
	 */
	public int classCount()
	{
		return entities.size();
	}
	
	/**
	 * @param index: the index of the entity to search in the Ontology
	 * @return whether the Ontology contains the entity with the given index
	 */
	public boolean contains(int index)
	{
		return entities.contains(index);
	}
	
	/**
	 * @param e: the EntityType to search in the Ontology
	 * @return the set of entities of the given type in the Ontology
	 */
	public Set<Integer> getEntities(EntityType e)
	{
		return entityTypes.get(e);
	}
	
	/**
	 * @param index: the index of the term/property to get the name
	 * @return the primary name of the term/property with the given index
	 */
	public String getName(int index)
	{
		return getLexicon().getBestName(index);
	}

	/**
	 * @return the Ontology's Lexicon
	 */
	public Lexicon getLexicon()
	{
		return lex;
	}
	
	/**
	 * @return the Ontology's URI
	 */
	public String getURI()
	{
		return uri;
	}
	
	//Get the local name of an entity from its URI
	protected String getLocalName(String uri)
	{
		String newUri = uri;
		if(newUri.contains("%") || newUri.contains("&"))
		{
			try
			{
				newUri = URLDecoder.decode(newUri,"UTF-8");
			}
			catch(UnsupportedEncodingException e)
			{
				//Do nothing
			}
		}
		int index = newUri.indexOf("#") + 1;
		if(index == 0)
			index = newUri.lastIndexOf("/") + 1;
		return newUri.substring(index);
	}
}