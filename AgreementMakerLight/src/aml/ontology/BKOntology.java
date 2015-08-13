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
* An Ontology object, loaded using the OWL API.                               *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 13-08-2015                                                            *
******************************************************************************/
package aml.ontology;

import java.io.File;
import java.util.Set;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

import aml.settings.LexicalType;
import aml.util.StringParser;

public class BKOntology extends Ontology
{

//Attributes
	
	//Its map of cross-references
	protected ReferenceMap refs;
	
	
//Constructors

	/**
	 * Constructs an empty ontology
	 */
	public BKOntology()
	{
		super();
		refs = new ReferenceMap();
	}
	
	/**
	 * Constructs an Ontology from file 
	 * @param path: the path to the input Ontology file
	 * @throws OWLOntologyCreationException 
	 */
	public BKOntology(String path) throws OWLOntologyCreationException
	{
		this();
        //Load the local ontology
        File f = new File(path);
        OWLOntology o;
		o = manager.loadOntologyFromOntologyDocument(f);
		uri = f.getAbsolutePath();
		init(o);
		//Close the OntModel
        manager.removeOntology(o);
        //Reset the entity expansion limit
        System.clearProperty(LIMIT);
        //Check if a xrefs file with the same name as the ontology exists 
		String refName = path.substring(0,path.lastIndexOf(".")) + ".xrefs";
		f = new File(refName);
		if(f.exists())
			//And if so, use it to extend the ReferenceMap
			refs.extend(refName);
	}

//Public Methods

	@Override
	public void close()
	{
		super.close();
		refs = null;
	}
	
	/**
	 * @return the ReferenceMap of the Ontology
	 */
	public ReferenceMap getReferenceMap()
	{
		return refs;
	}
		
//Private Methods	

	//Builds the ontology data structures
	private void init(OWLOntology o)
	{
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
			//Update the index and add it to the list of classes
			classes.add(++id);
			//Get the local name from the URI
			String name = getLocalName(classUri);
			
			//If the local name is not an alphanumeric code, add it to the lexicon
			if(!StringParser.isNumericId(name))
			{
				type = LexicalType.LOCAL_NAME;
				weight = type.getDefaultWeight();
				lex.addClass(id, name, "en", type, "", weight);
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
	            		lex.addClass(id, name, lang, type, "", weight);
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
	                       		String lang = val.getLang();
	    	            		if(lang.equals(""))
	    	            			lang = "en";
    		            		lex.addClass(id, name, lang, type, "", weight);
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
		}
	}
	
	//Get the local name of an entity from its URI
	private String getLocalName(String uri)
	{
		int index = uri.indexOf("#") + 1;
		if(index == 0)
			index = uri.lastIndexOf("/") + 1;
		return uri.substring(index);
	}
}