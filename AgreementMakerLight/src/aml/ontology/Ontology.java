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
* An Ontology object built using the OWL API.                                 *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 13-08-2015                                                            *
******************************************************************************/
package aml.ontology;

import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntologyManager;

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
	//The set of classes in the ontology 
	protected HashSet<Integer> classes;
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
        classes = new HashSet<Integer>();
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
		classes = null;
		lex = null;
		uri = null;
	}
	
	/**
	 * @return the number of Classes in the Ontology
	 */
	public int classCount()
	{
		return classes.size();
	}
	
	/**
	 * @param index: the index of the Class to search in the Ontology
	 * @return whether the Ontology contains the Class with the given index
	 */
	public boolean containsClass(int index)
	{
		return classes.contains(index);
	}
	
	/**
	 * @return the set of Classes in the Ontology
	 */
	public Set<Integer> getClasses()
	{
		return classes;
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
}