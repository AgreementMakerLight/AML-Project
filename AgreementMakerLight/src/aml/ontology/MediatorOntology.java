/******************************************************************************
* Copyright 2013-2018 LASIGE                                                  *
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
* An Ontology object, loaded using the OWL API.                               *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.ontology;

import aml.ontology.ReferenceMap;
import aml.ontology.lexicon.ExternalLexicon;


public class MediatorOntology
{

//Attributes
	
	//The URI of the ontology
	private String uri;
	//Its lexicon
	private ExternalLexicon lex;
	//Its map of cross-references
	private ReferenceMap refs;
	
	
//Constructors

	/**
	 * Constructs an empty ontology
	 */
	public MediatorOntology()
	{
		lex = new ExternalLexicon();
		refs = new ReferenceMap();
	}
	
//Public Methods

	/**
	 * Closes the Ontology 
	 */
	public void close()
	{
		uri = null;
		lex = null;
		refs = null;
	}
	
	/**
	 * @return the ExternalLexicon of the Ontology
	 */
	public ExternalLexicon getExternalLexicon()
	{
		return lex;
	}
	
	/**
	 * @return the ReferenceMap of the Ontology
	 */
	public ReferenceMap getReferenceMap()
	{
		return refs;
	}
	
	/**
	 * @return the URI of the Ontology
	 */
	public String getURI()
	{
		return uri;
	}
	
	public void setURI(String uri)
	{
		this.uri = uri;
	}
}