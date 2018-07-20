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
* An Ontology object, loaded using the OWL API.                               *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.knowledge;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

import aml.ontology.ReferenceMap;
import aml.settings.LexicalType;
import aml.util.StringParser;

public class MediatorOntology
{

//Attributes
	
	//The entity expansion limit property
    private final String LIMIT = "entityExpansionLimit";
	//The URI of the ontology
	private String uri;
	//Its lexicon
	private MediatorLexicon lex;
	//Its map of cross-references
	private ReferenceMap refs;
	
	
//Constructors

	/**
	 * Constructs an empty ontology
	 */
	private MediatorOntology()
	{
		lex = new MediatorLexicon();
		refs = new ReferenceMap();
	}
	
	/**
	 * Constructs an Ontology from file 
	 * @param path: the path to the input Ontology file
	 * @throws OWLOntologyCreationException 
	 */
	public MediatorOntology(String path) throws OWLOntologyCreationException
	{
		this();
        //Load the local ontology
		init(path);
        //Check if a xrefs file with the same name as the ontology exists 
		//And if so, use it to extend the ReferenceMap
		String refName = path.substring(0,path.lastIndexOf(".")) + ".xrefs";
		File f = new File(refName);
		if(f.exists())
			refs.extend(refName);
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
	 * @return the MediatorLexicon of the Ontology
	 */
	public MediatorLexicon getMediatorLexicon()
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
		
//Private Methods	

	//Builds the ontology data structures
	private void init(String path) throws OWLOntologyCreationException
	{
        //Increase the entity expansion limit to allow large ontologies
        System.setProperty(LIMIT, "1000000");
        //Get an Ontology Manager and Data Factory
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLDataFactory factory = manager.getOWLDataFactory();
        File f = new File(path);
		uri = f.getAbsolutePath();
        OWLOntology o = manager.loadOntologyFromOntologyDocument(f);

		//Update the URI of the ontology (if it lists one)
		if(o.getOntologyID().getOntologyIRI() != null)
			uri = o.getOntologyID().getOntologyIRI().toString();
		
		//Get the classes and their lexical and cross-reference information
		//The internal index of the class
		int id = 0;
		//The Lexical type and weight
		LexicalType type;
		double weight;
		
		//The label property
		OWLAnnotationProperty label = factory.getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_LABEL.getIRI());
		//Get an iterator over the ontology classes
		Set<OWLClass> owlClasses = o.getClassesInSignature(true);
		//Then get the URI for each class
		for(OWLClass c : owlClasses)
		{
			String classUri = c.getIRI().toString();
			if(classUri == null || classUri.endsWith("owl#Thing") || classUri.endsWith("owl:Thing"))
				continue;
			//Get the local name from the URI
			String name = getLocalName(classUri);
			
			//If the local name is not an alphanumeric code, add it to the lexicon
			if(!StringParser.isNumericId(name))
			{
				type = LexicalType.LOCAL_NAME;
				weight = type.getDefaultWeight();
				lex.add(id, name, weight);
			}

			//Now get the class's annotations (including imports)
			Set<OWLAnnotation> annots = c.getAnnotations(o);
			for(OWLOntology ont : o.getImports())
				annots.addAll(c.getAnnotations(ont));
            for(OWLAnnotation annotation : annots)
            {
            	//Labels and synonyms go to the Lexicon
            	String propUri = annotation.getProperty().getIRI().toString();
            	type = LexicalType.getLexicalType(propUri);
            	if(type != null)
            	{
	            	weight = type.getDefaultWeight();
	            	if(annotation.getValue() instanceof OWLLiteral)
	            	{
	            		OWLLiteral val = (OWLLiteral) annotation.getValue();
	            		name = val.getLiteral();
	            		String lang = val.getLang();
	            		if(lang.equals(""))
	            			lang = "en";
	            		lex.add(id, name, weight);
		            }
	            	else if(annotation.getValue() instanceof IRI)
	            	{
	            		OWLNamedIndividual ni = factory.getOWLNamedIndividual((IRI) annotation.getValue());
	                    for(OWLAnnotation a : ni.getAnnotations(o,label))
	                    {
	                       	if(a.getValue() instanceof OWLLiteral)
	                       	{
	                       		OWLLiteral val = (OWLLiteral) a.getValue();
	                       		name = val.getLiteral();
    		            		lex.add(id, name, weight);
	                       	}
	            		}
	            	}
            	}
            	//xRefs go to the ReferenceMap
            	else if(propUri.endsWith("hasDbXref") &&
            			annotation.getValue() instanceof OWLLiteral)
            	{
            		OWLLiteral val = (OWLLiteral) annotation.getValue();
					String xRef = val.getLiteral();
					if(!xRef.startsWith("http"))
						refs.add(id,xRef.replace(':','_'));
            	}
	        }
            //Increment the index
            id++;
		}
		//Close the OntModel
        manager.removeOntology(o);
        //Reset the entity expansion limit
        System.clearProperty(LIMIT);
	}
	
	//Get the local name of an entity from its URI
	private String getLocalName(String uri)
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