/******************************************************************************
* Copyright 2013-2018 LASIGE												  *
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
* An Ontology file parser based on the OWL API.								  *
*																			  *
* @author Daniel Faria, Catia Pesquita                                        *
******************************************************************************/
package aml.io;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.ClassExpressionType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyExpression;
import org.semanticweb.owlapi.model.OWLDataRange;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

import uk.ac.manchester.cs.owl.owlapi.OWLDataAllValuesFromImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLDataCardinalityRestrictionImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLDataHasValueImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLDataSomeValuesFromImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectCardinalityRestrictionImpl;
import aml.AML;
import aml.ontology.EntityType;
import aml.ontology.MediatorOntology;
import aml.ontology.Ontology;
import aml.ontology.ReferenceMap;
import aml.ontology.SKOS;
import aml.ontology.ValueMap;
import aml.ontology.lexicon.ExternalLexicon;
import aml.ontology.lexicon.LexicalMetadata;
import aml.ontology.lexicon.LexicalType;
import aml.ontology.lexicon.Lexicon;
import aml.ontology.lexicon.StringParser;
import aml.ontology.lexicon.WordLexicon;
import aml.ontology.semantics.EntityMap;
import aml.util.data.Map2MapComparable;
import aml.util.data.Map2Set;
import aml.util.data.MapSorter;

public class OntologyParser
{

//Attributes
	
	//The OWL Ontology Manager and Data Factory
	private static OWLOntologyManager manager;
	private static OWLDataFactory factory;
	//The entity expansion limit property
	private static final String LIMIT = "entityExpansionLimit";
	
//Public Methods

	/**
	 * Creates an AML Ontology object from an OWL ontology or SKOS thesaurus local file
	 * by using the OWL API to interpret it
	 * @param path: the path to the file to parse
	 * @param isBK: whether the ontology is to be used strictly as background knowledge
	 * or is to be matched (BK ontologies are not registered in the global EntityMap or
	 * RelationshipMap)
	 * @return the AML Ontology encoding the ontology/thesaurus in the file
	 * @throws OWLOntologyCreationException
	 */
	public static Ontology parse(String path) throws OWLOntologyCreationException
	{
		//Increase the entity expansion limit to allow large ontologies
		System.setProperty(LIMIT, "1000000");
		//Get an Ontology Manager and Data Factory
		manager = OWLManager.createOWLOntologyManager();
		factory = manager.getOWLDataFactory();
		//Load the local ontology
		File f = new File(path);
		OWLOntology o = manager.loadOntologyFromOntologyDocument(f);
		Ontology l = new Ontology();
		l.setURI(f.toURI().toString());
		parse(o,l);
		//Close the OntModel
		manager.removeOntology(o);
		//Reset the entity expansion limit
		System.clearProperty(LIMIT);
		//Return the ontology
		return l;
	}

	/**
	 * Creates an AML Ontology object from an OWL ontology or SKOS thesaurus specified
	 * via an URI by using the OWL API to interpret it
	 * @param uri: the URI to the ontology/thesaurus to parse
	 * @param isBK: whether the ontology is to be used strictly as background knowledge
	 * or is to be matched (BK ontologies are not registered in the global EntityMap or
	 * RelationshipMap)
	 * @return the AML Ontology encoding the ontology/thesaurus in the file
	 * @throws OWLOntologyCreationException
	 */
	public static Ontology parse(URI uri) throws OWLOntologyCreationException
	{
		//Increase the entity expansion limit to allow large ontologies
		System.setProperty(LIMIT, "1000000");
		//Get an Ontology Manager and Data Factory
		manager = OWLManager.createOWLOntologyManager();
		factory = manager.getOWLDataFactory();
		OWLOntology o;
		//Check if the URI is local
		if(uri.toString().startsWith("file:"))
		{
			File f = new File(uri);
			o = manager.loadOntologyFromOntologyDocument(f);
		}
		else
		{
			IRI i = IRI.create(uri);
			o = manager.loadOntology(i);
		}
		Ontology l = new Ontology();
		l.setURI(uri.toString());
		parse(o,l);
		//Close the OntModel
		manager.removeOntology(o);
		//Reset the entity expansion limit
		System.clearProperty(LIMIT);
		//Return the ontology
		return l;
	}
	
	public static MediatorOntology parseMediator(String path) throws OWLOntologyCreationException
	{
		//Increase the entity expansion limit to allow large ontologies
		System.setProperty(LIMIT, "1000000");
		//Get an Ontology Manager and Data Factory
		manager = OWLManager.createOWLOntologyManager();
		factory = manager.getOWLDataFactory();
		//Load the local ontology
		File f = new File(path);
		OWLOntology o = manager.loadOntologyFromOntologyDocument(f);
		MediatorOntology m = new MediatorOntology();
		//Update the URI of the ontology (if it lists one)
		if(o.getOntologyID().getOntologyIRI() != null)
			m.setURI(o.getOntologyID().getOntologyIRI().toString());
		else
			m.setURI((new File(path)).toURI().toString());
		
		parse(o,m);
		//Close the OntModel
        manager.removeOntology(o);
        //Reset the entity expansion limit
        System.clearProperty(LIMIT);
        
        //Check if a xrefs file with the same name as the ontology exists 
		//And if so, use it to extend the ReferenceMap
		String refName = path.substring(0,path.lastIndexOf(".")) + ".xrefs";
		File f2 = new File(refName);
		if(f2.exists())
			m.getReferenceMap().extend(refName);
		
		return m;
	}

//Private Methods	

	//Builds the ontology data structures
	private static void parse(OWLOntology o, Ontology l)
	{
		//Check if the ontology is in SKOS format
		if(o.containsClassInSignature(SKOS.CONCEPT_SCHEME.toIRI()) &&
				o.containsClassInSignature(SKOS.CONCEPT.toIRI()))
		{
			//Update the URI of the ontology
			OWLClass scheme = getClass(o,SKOS.CONCEPT_SCHEME.toIRI());
			Set<OWLIndividual> indivs = scheme.getIndividuals(o);
			String schemeURIs = "";
			for(OWLIndividual i : indivs)
				if(i.isNamed())
					schemeURIs += i.asOWLNamedIndividual().getIRI().toString() + " | ";
			if(schemeURIs.length() > 0)
				l.setURI(schemeURIs.substring(0, schemeURIs.length() - 3));
			parseSKOS(o,l);
		}
		else
		{
			//Update the URI of the ontology (if it lists one)
			if(o.getOntologyID().getOntologyIRI() != null)
				l.setURI(o.getOntologyID().getOntologyIRI().toString());
			parseOWL(o,l);
		}
	}

	private static void parseSKOS(OWLOntology o, Ontology l)
	{
	//1 - SKOS Concepts (which are technically OWL individuals, but will be treated as Classes)
		//The Lexical type and weight
		EntityMap rm = AML.getInstance().getEntityMap();
		Lexicon lex = l.getLexicon();
		LexicalType type;
		double weight;
		//SKOS concepts are instances of class "concept"
		//Thus, we start by retrieving this class
		OWLClass concept = getClass(o,SKOS.CONCEPT.toIRI());
		if(concept == null)
			return;
		//Then retrieve its instances, as well as those of its subclasses
		Set<OWLIndividual> indivs = concept.getIndividuals(o);
		for(OWLClassExpression c : concept.getSubClasses(o))
			if(c instanceof OWLClass)
				indivs.addAll(c.asOWLClass().getIndividuals(o));
		//And process them as if they were OWL classes
		for(OWLIndividual i : indivs)
		{
			if(!i.isNamed())
				continue;
			OWLNamedIndividual ind = i.asOWLNamedIndividual();
			String indivUri = ind.getIRI().toString();
			//If this is not a BK ontology, add the URI to the global list of URIs (as a class)
			rm.addURI(indivUri, EntityType.CLASS);
			//Add it to the ontology
			l.add(indivUri, EntityType.CLASS);
			//Get the local name from the URI
			String name = rm.getLocalName(indivUri);
			//If the local name is not an alphanumeric code, add it to the lexicon
			if(!StringParser.isNumericId(name))
			{
				type = LexicalType.LOCAL_NAME;
				weight = type.getDefaultWeight();
				lex.add(indivUri, name, "en", type, "", weight);
			}

			//Now get the class's annotations (including imports)
			Set<OWLAnnotation> annots = ind.getAnnotations(o);
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
						lex.add(indivUri, name, lang, type, "", weight);
					}
					else if(annotation.getValue() instanceof IRI)
					{
						OWLNamedIndividual ni = factory.getOWLNamedIndividual((IRI) annotation.getValue());
						for(OWLAnnotation a : ni.getAnnotations(o))
						{
							if(a.getValue() instanceof OWLLiteral)
							{
								OWLLiteral val = (OWLLiteral) a.getValue();
								name = val.getLiteral();
								String lang = val.getLang();
								if(lang.equals(""))
									lang = "en";
								lex.add(indivUri, name, lang, type, "", weight);
							}
						}
					}
				}
			}
		}

	//2 - SKOS relationships
		//For simplicity, we convert "broader", "broader_transitive", "narrower"
		//and "narrower_transitive" to subclass relationships
		//We treat "related" as a someValues restriction on property "related"
		//and also as a disjointness, by SKOS definitions
		//Start by adding the "related" property
		String related = SKOS.RELATED.toIRI().toString();
		rm.addURI(related, EntityType.OBJECT_PROP);
		l.getLexicon().add(related, SKOS.RELATED.toString(), "en", LexicalType.LOCAL_NAME,
				"", LexicalType.LOCAL_NAME.getDefaultWeight());
		l.add(related,EntityType.OBJECT_PROP);
		//Related is symmetric by definition
		rm.addSymmetric(related);
		//And irreflexive (since it implies disjointness)
		rm.addIrreflexive(related);
		//Check that the thesaurus explicitly defines the SKOS object properties
		//(for convenience, we test only the "skos:broader") 
		boolean hasObject = o.containsObjectPropertyInSignature(SKOS.BROADER.toIRI());
		for(OWLIndividual i : indivs)
		{
			if(!i.isNamed())
				continue;
			OWLNamedIndividual ind = i.asOWLNamedIndividual();
			String child = ind.getIRI().toString();
			if(!rm.contains(child))
				continue;
			//If the thesaurus has the SKOS Object Properties properly defined
			if(hasObject)
			{
				//We can retrieve the Object Properties of each concept
				Map<OWLObjectPropertyExpression, Set<OWLIndividual>> iProps = i.getObjectPropertyValues(o);
				for(OWLObjectPropertyExpression prop : iProps.keySet())
				{
					if(prop.isAnonymous())
						continue;
					//Get each property's IRI
					IRI rel = prop.asOWLObjectProperty().getIRI();
					//Check that the Object Property is one of the SKOS relations
					if(!rel.equals(SKOS.BROADER.toIRI()) && !rel.equals(SKOS.BROADER_TRANS.toIRI()) &&
							!rel.equals(SKOS.NARROWER.toIRI()) && !rel.equals(SKOS.BROADER_TRANS.toIRI()) &&
							!rel.equals(SKOS.RELATED.toIRI()))
						continue;
					//And if so, get the related concepts
					for(OWLIndividual p : iProps.get(prop))
					{
						if(!p.isNamed())
							continue;
						String parent = p.asOWLNamedIndividual().getIRI().toString();
						if(!rm.contains(parent))
							continue;
						//And add the relation according to the Object Property
						if(rel.equals(SKOS.BROADER.toIRI()) || rel.equals(SKOS.BROADER_TRANS.toIRI()))
							rm.addSubclass(child, parent);
						else if(rel.equals(SKOS.NARROWER.toIRI()) || rel.equals(SKOS.BROADER_TRANS.toIRI()))
							rm.addSubclass(parent, child);
						else if(rel.equals(SKOS.RELATED.toIRI()))
						{
							String exp = SKOS.RELATED.toString() + " min 1 " + rm.getLocalName(parent);
							rm.addMinCardinality(exp, related, parent, 1);
							rm.addDisjoint(child, parent);
						}
					}
				}
			}
			//Otherwise, they will likely register as Annotation Properties
			else
			{
				//So we have to get them from the annotations
				for(OWLAnnotation p : ind.getAnnotations(o))
				{
					IRI rel = p.getProperty().getIRI();
					if(!rel.equals(SKOS.BROADER.toIRI()) && !rel.equals(SKOS.BROADER_TRANS.toIRI()) &&
							!rel.equals(SKOS.NARROWER.toIRI()) && !rel.equals(SKOS.BROADER_TRANS.toIRI()) &&
							!rel.equals(SKOS.RELATED.toIRI()))
						continue;
					OWLAnnotationValue v = p.getValue();
					if(!(v instanceof IRI))
						continue;
					String parent = v.toString();
					if(!rm.contains(parent))
						continue;
					//And add the relation according to the Object Property
					if(rel.equals(SKOS.BROADER.toIRI()) || rel.equals(SKOS.BROADER_TRANS.toIRI()))
						rm.addSubclass(child, parent);
					else if(rel.equals(SKOS.NARROWER.toIRI()) || rel.equals(SKOS.BROADER_TRANS.toIRI()))
						rm.addSubclass(parent, child);
					else if(rel.equals(SKOS.RELATED.toIRI()))
					{
						String exp = SKOS.RELATED.toString() + " min 1 " + rm.getLocalName(parent);
						rm.addMinCardinality(exp, related, parent, 1);
						rm.addDisjoint(child, parent);
					}
				}
			}
		}
	}

	//Parses an OWLOntology as an Ontology
	//WARNING: Read source code at your own peril
	private static void parseOWL(OWLOntology o, Ontology l)
	{
	//1 - OWL Classes
		EntityMap rm = AML.getInstance().getEntityMap();
		Lexicon lex = l.getLexicon();
		ReferenceMap refs = l.getReferenceMap();
		LexicalType type;
		double weight;
		//The label property
		OWLAnnotationProperty label = factory.getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_LABEL.getIRI());
		//Get an iterator over the ontology classes
		Set<OWLClass> classes = o.getClassesInSignature(true);
		//Then get the URI for each class
		for(OWLClass c : classes)
		{
			String classUri = c.getIRI().toString();
			if(classUri == null || classUri.endsWith("owl#Thing") || classUri.endsWith("owl:Thing"))
				continue;
			//If the ontology is not BK, add the URI it to the global list of URIs
			rm.addURI(classUri,EntityType.CLASS);
			//Add it to the Ontology
			l.add(classUri,EntityType.CLASS);

			//Get the local name from the URI
			String name = rm.getLocalName(classUri);
			//If the local name is not an alphanumeric code, add it to the lexicon
			if(!StringParser.isNumericId(name))
			{
				type = LexicalType.LOCAL_NAME;
				weight = type.getDefaultWeight();
				lex.add(classUri, name, "en", type, "", weight);
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
						lex.add(classUri, name, lang, type, "", weight);
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
								lex.add(classUri, name, lang, type, "", weight);
							}
						}
					}
				}
				//xRefs go to the cross-reference table
				else if(propUri.endsWith("hasDbXref") &&
						annotation.getValue() instanceof OWLLiteral)
				{
					OWLLiteral val = (OWLLiteral) annotation.getValue();
					String xRef = val.getLiteral();
					if(!xRef.startsWith("http"))
						refs.add(classUri,xRef.replace(':','_'));
				}
				//Deprecated classes are flagged as obsolete
				else if(propUri.endsWith("deprecated") &&
						annotation.getValue() instanceof OWLLiteral)
				{
					OWLLiteral val = (OWLLiteral) annotation.getValue();
					if(val.isBoolean())
					{
						boolean deprecated = val.parseBoolean();
						if(deprecated)
							l.setObsolete(classUri);
					}
				}
			}
			//Look for logical definition, which are encoded as equivalent
			//class expressions with someValuesFrom restrictions onProperty
			//"part_of" over an intersection of classes
			for(OWLClassExpression e : c.getEquivalentClasses(o))
			{
				//Skip expressions that aren't someValuesFrom
				if(!e.getClassExpressionType().equals(ClassExpressionType.OBJECT_SOME_VALUES_FROM))
					continue;
				//And those with a nested expression count smaller than 4
				Set<OWLClassExpression> nested = e.getNestedClassExpressions();
				nested.remove(e);
				if(nested.size() < 4)
					continue;
				//Get the largest nested expression
				int maxLength = 0;
				OWLClassExpression largest = null;
				//and check if all classes are classes of different ontologies
				boolean classCheck = true;
				for(OWLClassExpression n : nested)
				{
					if(n.getClassExpressionType().equals(ClassExpressionType.OWL_CLASS))
					{
						String namespace = n.asOWLClass().getIRI().toString();
						int index = Math.max(namespace.lastIndexOf('_'), namespace.lastIndexOf('#'));
						if(index < 0)
							index = namespace.lastIndexOf('/');
						namespace = namespace.substring(0,index);
						if(classUri.startsWith(namespace))
						{
							classCheck = false;
							break;
						}
					}
					else
					{
						int length = n.toString().length();
						if(length > maxLength)
						{
							maxLength = length;
							largest = n;
						}
					}
				}
				if(classCheck && largest.getClassExpressionType().equals(ClassExpressionType.OBJECT_INTERSECTION_OF))
					refs.add(classUri, e.toString());
			}
		}

	//2 - Data Properties
		Set<OWLDataProperty> dProps = o.getDataPropertiesInSignature(true);
		for(OWLDataProperty dp : dProps)
		{
			String propUri = dp.getIRI().toString();
			if(propUri == null)
				continue;
			//Add it to the global list of URIs
			rm.addURI(propUri,EntityType.DATA_PROP);
			//And to the ontology
			l.add(propUri,EntityType.DATA_PROP);
			
			//Get the local name from the URI
			String localName = rm.getLocalName(propUri);
			//Get the label(s)
			String lang = "";
			HashSet<String> labelLanguages = new HashSet<String>();
			for(OWLAnnotation a : dp.getAnnotations(o,label))
			{
				if(a.getValue() instanceof OWLLiteral)
				{
					OWLLiteral val = (OWLLiteral) a.getValue();
					String name = val.getLiteral();
					lang = val.getLang();
					if(lang.equals(""))
						lang = "en";
					type = LexicalType.LABEL;
					weight = type.getDefaultWeight();
					lex.add(propUri, name, lang, type, "", weight);
					labelLanguages.add(lang);
				}
			}
			//If the local name is not an alphanumeric code, add it to the lexicon
			//(assume it is in the same language as the label(s), if only one label
			//language is declared; otherwise assume it is English)
			if(!StringParser.isNumericId(localName))
			{
				type = LexicalType.LOCAL_NAME;
				weight = type.getDefaultWeight();
				if(labelLanguages.size() != 1)
					lang = "en";
				lex.add(propUri, localName, lang, type, "", weight);
			}
			//If the property is functional, add it to the RelationshipMap
			if(dp.isFunctional(o))
				rm.addFunctional(propUri);
			//Add its domain(s) to the RelationshipMap
			Set<OWLClassExpression> domains = dp.getDomains(o);
			for(OWLClassExpression ce : domains)
			{
				Set<OWLClassExpression> union = ce.asDisjunctSet();
				for(OWLClassExpression cf : union)
				{
					if(cf instanceof OWLClass)
						rm.addDomain(propUri, cf.asOWLClass().getIRI().toString());
				}
			}
			//And finally its range(s)
			Set<OWLDataRange> ranges = dp.getRanges(o);
			for(OWLDataRange dr : ranges)
			{
				if(dr.isDatatype())
					rm.addRange(propUri, dr.asOWLDatatype().toStringID());
			}
		}
		
	//3 - Object Properties
		Set<OWLObjectProperty> oProps = o.getObjectPropertiesInSignature(true);
		for(OWLObjectProperty op : oProps)
		{
			String propUri = op.getIRI().toString();
			if(propUri == null)
				continue;
			//Add it to the global list of URIs
			rm.addURI(propUri,EntityType.OBJECT_PROP);
			//And to the ontology
			l.add(propUri,EntityType.OBJECT_PROP);

			//Get the local name from the URI
			String localName = rm.getLocalName(propUri);
			//Get the label(s)
			String lang = "";
			HashSet<String> labelLanguages = new HashSet<String>();
			for(OWLAnnotation a : op.getAnnotations(o,label))
			{
				if(a.getValue() instanceof OWLLiteral)
				{
					OWLLiteral val = (OWLLiteral) a.getValue();
					String name = val.getLiteral();
					lang = val.getLang();
					if(lang.equals(""))
						lang = "en";
					type = LexicalType.LABEL;
					weight = type.getDefaultWeight();
					lex.add(propUri, name, lang, type, "", weight);
					labelLanguages.add(lang);
				}
			}
			//If the local name is not an alphanumeric code, add it to the lexicon
			//(assume it is in the same language as the label(s), if only one label
			//language is declared; otherwise assume it is English)
			if(!StringParser.isNumericId(localName))
			{
				type = LexicalType.LOCAL_NAME;
				weight = type.getDefaultWeight();
				if(labelLanguages.size() != 1)
					lang = "en";
				lex.add(propUri, localName, lang, type, "", weight);
			}
			//Add the properties of the property to the EntityMap
			if(op.isFunctional(o))
				rm.addFunctional(propUri);
			if(op.isTransitive(o))
				rm.addTransitive(propUri);
			if(op.isReflexive(o))
				rm.addReflexive(propUri);
			if(op.isIrreflexive(o))
				rm.addIrreflexive(propUri);
			if(op.isSymmetric(o))
				rm.addSymmetric(propUri);
			if(op.isAsymmetric(o))
				rm.addAsymmetric(propUri);

			//Add its domain(s) to the RelationshipMap
			Set<OWLClassExpression> domains = op.getDomains(o);
			for(OWLClassExpression ce : domains)
			{
				Set<OWLClassExpression> union = ce.asDisjunctSet();
				for(OWLClassExpression cf : union)
				{
					if(cf instanceof OWLClass)
						rm.addDomain(propUri, cf.asOWLClass().getIRI().toString());
				}
			}
			//And finally its range(s)
			Set<OWLClassExpression> ranges = op.getRanges(o);
			for(OWLClassExpression ce : ranges)
			{
				Set<OWLClassExpression> union = ce.asDisjunctSet();
				for(OWLClassExpression cf : union)
				{
					if(cf instanceof OWLClass)
						rm.addRange(propUri, cf.asOWLClass().getIRI().toString());
				}
			}
		}
		//Process property chains (including "transitive over" relations)
		//This requires that all properties be indexed, and thus has to be done in a 2nd pass
		for(OWLObjectProperty op : oProps)
		{
			//In OWL, the OBO transitive_over relation is encoded as a sub-property chain of
			//the form: "SubObjectPropertyOf(ObjectPropertyChain( <p1> <p2> ) <p1> )"
			for(OWLAxiom e : op.getReferencingAxioms(o))
			{
				if(!e.isOfType(AxiomType.SUB_PROPERTY_CHAIN_OF))
					continue;
				//Unfortunately, there isn't much support for property chains in the OWL API,
				//so the only way to get the referenced properties while preserving their
				//order is to parse the String representation of the sub-property chain
				//(getObjectPropertiesInSignature() does NOT preserve the order)
				String[] propChain = e.toString().split("[\\(\\)]");
				//Make sure the structure of the sub-property chain is in the expected format
				if(!propChain[0].equals("SubObjectPropertyOf") || !propChain[1].equals("ObjectPropertyChain"))
					continue;
				//The chain can be split into properties by the tags 
				String[] ch = propChain[2].split("[<>]");
				Vector<String> chain = new Vector<String>(ch.length);
				boolean check = true;
				for(String s : ch)
				{
					String propUri = s.trim();
					//Check that every property is in the EntityMap as an ObjectProperty
					if(!rm.isObjectProperty(propUri))
					{
						check = false;
						break;
					}
					chain.add(propUri);
				}
				String propUri = propChain[3].replaceAll("[<>]", "").trim();
				if(!check || !rm.isObjectProperty(propUri))
					continue;
				//If we have a chain of exactly two properties that is equivalent to the first property,
				//then the first is transitiveOver the second
				if(chain.size() == 2 && chain.get(0).equals(propUri))
					rm.addTransitiveOver(chain.get(0), chain.get(1));
				//Otherwise, we add the chain as is
				else
					rm.addPropertyChain(propUri, chain);
			}
		}

	//4 - Named Individuals and their data values
		ValueMap vMap = l.getValueMap();
		Map<String,Integer> langCounts = new LinkedHashMap<String,Integer>();
		//Get an iterator over the ontology individuals
		Set<OWLNamedIndividual> indivs = o.getIndividualsInSignature();
		//Then get the URI for each class
		for(OWLNamedIndividual i : indivs)
		{
			String indivUri = i.getIRI().toString();
			//Note that this should never happen as a named individual must
			//have an URI by definition, but better safe than sorry
			if(indivUri == null)
				continue;
			//Add it to the global list of URIs
			rm.addURI(indivUri, EntityType.INDIVIDUAL);
			//Add it to the Ontology
			l.add(indivUri, EntityType.INDIVIDUAL);
			//Get the local name from the URI
			String localName = rm.getLocalName(indivUri);
			//Get the label(s)
			String lang = "";
			for(OWLAnnotation a : i.getAnnotations(o,label))
			{
				if(a.getValue() instanceof OWLLiteral)
				{
					OWLLiteral val = (OWLLiteral) a.getValue();
					String name = val.getLiteral();
					if(name.equals(localName) && StringParser.isNumericId(name))
						continue;
					lang = val.getLang();
					if(lang.equals(""))
					{
						if(langCounts.isEmpty())
							lang = "en";
						else
						{
							langCounts = MapSorter.sortDescending(langCounts);
							lang = langCounts.keySet().iterator().next();
						}
					}
					else
					{
						int count = 1;
						if(langCounts.containsKey(lang))
							count += langCounts.get(lang);
						langCounts.put(lang, count);
					}
					type = LexicalType.LABEL;
					weight = type.getDefaultWeight();
					lex.add(indivUri, name, lang, type, "", weight);
				}
			}
			//If the local name is not an alphanumeric code, add it to the lexicon
			//(assume it is in the most common label language)
			if(!StringParser.isNumericId(localName))
			{
				type = LexicalType.LOCAL_NAME;
				weight = type.getDefaultWeight();
				if(lang.equals(""))
				{
					if(langCounts.isEmpty())
						lang = "en";
					else
					{
						langCounts = MapSorter.sortDescending(langCounts);
						lang = langCounts.keySet().iterator().next();
					}
				}
				lex.add(indivUri, localName, lang, type, "", weight);
			}
			//Get the annotations of the Individual
			for(OWLAnnotation annotation : i.getAnnotations(o))
			{
				//Skip rdf:labels which were already processed
				if(annotation.getProperty().equals(label))
					continue;
				String propUri = annotation.getProperty().getIRI().toString();
				//Annotations with a LexicalType go to the Lexicon
				type = LexicalType.getLexicalType(propUri);
				if(type != null)
				{
					weight = type.getDefaultWeight();
					if(annotation.getValue() instanceof OWLLiteral)
					{
						OWLLiteral val = (OWLLiteral) annotation.getValue();
						String name = val.getLiteral();
						String localLang = val.getLang();
						if(localLang.equals(""))
							localLang = lang;
						lex.add(indivUri, name, localLang, type, "", weight);
					}
					else if(annotation.getValue() instanceof IRI)
					{
						String iri = ((IRI)annotation.getValue()).toString();
						String name = rm.getLocalName(iri);
						if(!StringParser.isNumericId(name))
							lex.add(indivUri, name, lang, type, "", weight);
						OWLNamedIndividual ni = factory.getOWLNamedIndividual((IRI) annotation.getValue());
						for(OWLAnnotation a : ni.getAnnotations(o,label))
						{
							if(a.getValue() instanceof OWLLiteral)
							{
								OWLLiteral val = (OWLLiteral) a.getValue();
								name = val.getLiteral();
								String localLang = val.getLang();
								if(localLang.equals(""))
									localLang = lang;
								lex.add(indivUri, name, localLang, type, "", weight);
							}
						}
					}
				}
				//Otherwise, literal annotations go to the ValueMap
				else if(annotation.getValue() instanceof OWLLiteral)
				{
					OWLLiteral val = (OWLLiteral) annotation.getValue();
					String v = val.getLiteral();
					//We must first add the annotation property to the EntityMap and Ontology
					//(if it was already added, nothing happens)
					rm.addURI(propUri, EntityType.ANNOTATION_PROP);
					l.add(propUri, EntityType.ANNOTATION_PROP);
					//Then add the value to the ValueMap
					vMap.add(indivUri, propUri, v);
				}
			}
			//Get the data properties associated with the individual and their values
			Map<OWLDataPropertyExpression,Set<OWLLiteral>> dataPropValues = i.getDataPropertyValues(o);
			for(OWLDataPropertyExpression prop : dataPropValues.keySet())
			{
				//Check that the data property expression is a named data property
				if(prop.isAnonymous())
					continue;
				//And if so, process its URI
				String propUri = prop.asOWLDataProperty().getIRI().toString();
				if(!rm.isDataProperty(propUri))
					continue;

				//Data Properties with a LexicalType go to the Lexicon
				type = LexicalType.getLexicalType(propUri);
				if(type != null)
				{
					weight = type.getDefaultWeight();
					for(OWLLiteral val : dataPropValues.get(prop))
						lex.add(indivUri, val.getLiteral(), "en", type, "", weight);
				}
				//Otherwise, they go to the ValueMap
				else
				{
					//Then get its values for the individual
					for(OWLLiteral val : dataPropValues.get(prop))
						vMap.add(indivUri, propUri, val.getLiteral());
				}
				//FIX: Filling in missing types of individuals from data property restrictions
				//(Sometimes ontologies fail to declare individual types)
				if(rm.getIndividualClasses(indivUri).isEmpty() && rm.getDomains(propUri).size() == 1)
					rm.addInstance(indivUri, rm.getDomains(propUri).iterator().next());
			}	
		}

	//5 - Class relationships and Class-Individual relationships
		//For each class index
		for(OWLClass c : classes)
		{
			//Get its identifier
			String child = c.getIRI().toString();
			if(!rm.isClass(child))
				continue;
			//Get the subclass expressions to capture and add relationships
			Set<OWLClassExpression> superClasses = c.getSuperClasses(o);
			for(OWLOntology ont : o.getDirectImports())
				superClasses.addAll(c.getSuperClasses(ont));
			for(OWLClassExpression e : superClasses)
			{
				if(e.getClassExpressionType().equals(ClassExpressionType.OWL_CLASS))
				{
					String parent = e.asOWLClass().getIRI().toString();
					if(!rm.isClass(parent))
						continue;
					rm.addSubclass(child, parent);
				}
				else
				{
					//TODO: handle expressions
				}
			}		
			//Get the equivalence expressions to capture and add relationships
			Set<OWLClassExpression> equivClasses = c.getEquivalentClasses(o);
			for(OWLOntology ont : o.getDirectImports())
				equivClasses.addAll(c.getEquivalentClasses(ont));
			for(OWLClassExpression e : equivClasses)
			{
				if(e.getClassExpressionType().equals(ClassExpressionType.OWL_CLASS))
				{
					String parent = e.asOWLClass().getIRI().toString();
					if(!rm.isClass(parent))
						continue;
					rm.addEquivalentClasses(child, parent);
				}
				else
				{
					//TODO: handle expressions
				}
			}		
			//Get the individuals that belong to the class
			Set<OWLIndividual> ind = c.getIndividuals(o);
			for(OWLIndividual i : ind)
			{
				if(i.isNamed())
				{
					String indivUri = i.asOWLNamedIndividual().getIRI().toString();
					if(rm.isIndividual(indivUri))
						rm.addInstance(indivUri, child);
				}
			}
			//Get the syntactic disjoints
			Set<OWLClassExpression> disjClasses = c.getDisjointClasses(o);
			for(OWLOntology ont : o.getDirectImports())
				disjClasses.addAll(c.getDisjointClasses(ont));
			//For each expression
			for(OWLClassExpression dClass : disjClasses)
			{
				//If it is a class, add it as disjoint
				if(dClass.getClassExpressionType().equals(ClassExpressionType.OWL_CLASS))
				{
					String parent = dClass.asOWLClass().getIRI().toString();
					if(rm.isClass(parent))
						rm.addDisjoint(child, parent);
				}
				//If it is a union, add all classes in the union as disjoint
				else
				{
					//TODO: Handle disjointness with expressions
				}
			}
		}

	//6 - Individual Relationships
		for(OWLNamedIndividual i : indivs)
		{
			//Get the numeric id for each individual
			String indivUri = i.getIRI().toString();
			if(!rm.isIndividual(indivUri))
				continue;

			Map<OWLObjectPropertyExpression, Set<OWLIndividual>> iProps = i.getObjectPropertyValues(o);
			for(OWLObjectPropertyExpression prop : iProps.keySet())
			{
				if(prop.isAnonymous())
					continue;

				String propUri = prop.asOWLObjectProperty().getIRI().toString();
				if(!rm.isObjectProperty(propUri))
					continue;

				//FIX: Filling in missing types of individuals from object property restrictions
				//(Sometimes ontologies fail to declare individual types)
				if(rm.getIndividualClasses(indivUri).isEmpty() && rm.getDomains(propUri).size() == 1)
					rm.addInstance(indivUri, rm.getDomains(propUri).iterator().next());

				for(OWLIndividual rI : iProps.get(prop))
				{
					if(rI.isNamed())
					{
						String relIndivUri = rI.asOWLNamedIndividual().getIRI().toString();
						if(!rm.isIndividual(relIndivUri))
							continue;
						rm.addIndividualRelationship(indivUri, relIndivUri, propUri);
						//If the individual is an alias of the related individual
						//use the individual's lexical entries to extend those of
						//the related individual
						if(propUri.endsWith("isAliasOf"))
						{
							for(String name : lex.getNames(indivUri))
							{
								for(LexicalMetadata p : lex.get(name, indivUri))
								{
									LexicalType t = p.getType();
									if(t.equals(LexicalType.LABEL) || t.equals(LexicalType.LOCAL_NAME))
										t = LexicalType.EXACT_SYNONYM;
									lex.add(relIndivUri,name,p.getLanguage(),t,"",t.getDefaultWeight());
								}
							}
						}
						//FIX: Filling in missing types of individuals from object property restrictions
						//(Sometimes ontologies fail to declare individual types)
						if(rm.getIndividualClasses(relIndivUri).isEmpty() && rm.getRanges(propUri).size() == 1)
							rm.addInstance(relIndivUri, rm.getRanges(propUri).iterator().next());
					}
				}
			}
			//FIX: Relationships between individuals encoded by AnnotationProperties
			//(Sometimes ontologies fail to declare object properties)
			for(OWLAnnotation annotation : i.getAnnotations(o))
			{
				String propUri = annotation.getProperty().getIRI().toString();
				//If the annotation doesn't have a LexicalType and is
				//pointing to a URI rather than a literal, treat it as
				//an object property
				LexicalType t = LexicalType.getLexicalType(propUri);
				if(t == null && annotation.getValue() instanceof IRI)
				{
					OWLNamedIndividual ni = factory.getOWLNamedIndividual((IRI) annotation.getValue());
					//Check that the named individual is in the EntityMap
					String relIndivUri = ni.getIRI().toString();
					if(!rm.isIndividual(relIndivUri))
						continue;
					//Add the property to the EntityMap and Ontology as an object property
					rm.addURI(propUri, EntityType.OBJECT_PROP);
					l.add(propUri, EntityType.OBJECT_PROP);
					//Add its name to the Lexicon
					lex.add(propUri, rm.getLocalName(propUri), "en", LexicalType.LOCAL_NAME,
							"", LexicalType.LOCAL_NAME.getDefaultWeight());
					//Add the relation to the RelationshipMap
					rm.addIndividualRelationship(indivUri, relIndivUri, propUri);
				}
			}
		}

	//7 - Relationships between Data Properties
		for(OWLDataProperty dp : dProps)
		{
			String propUri = dp.getIRI().toString();
			if(!rm.isDataProperty(propUri))
				continue;
			Set<OWLDataPropertyExpression> sProps = dp.getSuperProperties(o);
			for(OWLDataPropertyExpression de : sProps)
			{
				OWLDataProperty sProp = de.asOWLDataProperty();
				String sPropUri = sProp.getIRI().toString();
				if(rm.isDataProperty(sPropUri))
					rm.addSubproperty(propUri,sPropUri);	
			}
		}
		
	//8 - Relationships between Object Properties
		for(OWLObjectProperty op : oProps)
		{
			String propUri = op.getIRI().toString();
			if(!rm.isObjectProperty(propUri))
				continue;
			Set<OWLObjectPropertyExpression> sProps = op.getSuperProperties(o);
			for(OWLObjectPropertyExpression oe : sProps)
			{
				OWLObjectProperty sProp = oe.asOWLObjectProperty();
				String sPropUri = sProp.getIRI().toString();
				if(rm.isObjectProperty(sPropUri))
					rm.addSubproperty(propUri,sPropUri);
			}
			Set<OWLObjectPropertyExpression> iProps = op.getInverses(o);
			for(OWLObjectPropertyExpression oe : iProps)
			{
				OWLObjectProperty iProp = oe.asOWLObjectProperty();
				String iPropUri = iProp.getIRI().toString();
				if(rm.isObjectProperty(iPropUri))
					rm.addInverseProp(propUri,iPropUri);	
			}
		}
	}

	//Parses an OWLOntology as a MediatorOntology
	private static void parse(OWLOntology o, MediatorOntology m)
	{
		EntityMap rm = AML.getInstance().getEntityMap();
		ExternalLexicon lex = m.getExternalLexicon();
		ReferenceMap refs = m.getReferenceMap();
		
		//Get the classes and their lexical and cross-reference information
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
			String name = rm.getLocalName(classUri);
			//If the local name is not an alphanumeric code, add it to the lexicon
			if(!StringParser.isNumericId(name))
			{
				type = LexicalType.LOCAL_NAME;
				weight = type.getDefaultWeight();
				lex.add(classUri, name, weight);
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
	            		lex.add(classUri, name, weight);
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
    		            		lex.add(classUri, name, weight);
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
						refs.add(classUri,xRef.replace(':','_'));
            	}
	        }
		}
	}
	
//Auxiliary Methods

	//Gets a named class from the given OWLOntology 
	private static OWLClass getClass(OWLOntology o, IRI classIRI)
	{
		OWLClass cl = null;
		for(OWLClass c : o.getClassesInSignature())
		{
			if(c.getIRI().equals(classIRI))
			{
				cl = c;
				break;
			}
		}
		return cl;
	}

	//Add a relationship between two classes to the RelationshipMap
	private static void addRelationship(OWLOntology o, OWLClass c, OWLClassExpression e, boolean sub, boolean inverse)
	{
//		int child = rm.getIndex(c.getIRI().toString());
//		int parent;
//		ClassExpressionType type = e.getClassExpressionType();
//		//If it is a class, and we didn't use the reasoner, process it here
//		if(type.equals(ClassExpressionType.OWL_CLASS))
//		{
//			parent = rm.getIndex(e.asOWLClass().getIRI().toString());
//			if(parent < 0)
//				return;
//			if(sub)
//			{
//				if(inverse)
//					rm.addSubclass(parent, child);
//				else
//					rm.addSubclass(child, parent);
//				String name = getName(parent);
//				if(name.contains("Obsolete") || name.contains("obsolete") ||
//						name.contains("Retired") || name.contains ("retired") ||
//						name.contains("Deprecated") || name.contains("deprecated"))
//					obsolete.add(child);
//			}
//			else
//				rm.addEquivalentClass(child, parent);
//		}
//		//If it is a 'some values' object property restriction, process it
//		else if(type.equals(ClassExpressionType.OBJECT_SOME_VALUES_FROM))
//		{
//			//TODO: parse someValuesFrom intersections and unions
//			Set<OWLObjectProperty> props = e.getObjectPropertiesInSignature();
//			if(props == null || props.size() != 1)
//				return;
//			OWLObjectProperty p = props.iterator().next();
//			int property = rm.getIndex(p.getIRI().toString());
//			Set<OWLClass> sup = e.getClassesInSignature();
//			if(sup == null || sup.size() != 1)
//				return;					
//			OWLClass cls = sup.iterator().next();
//			parent = rm.getIndex(cls.getIRI().toString());
//			if(parent == -1 || property == -1)
//				return;
//			if(sub)
//			{
//				if(inverse)
//					rm.addClassRelationship(parent, child, property, false);
//				else
//					rm.addClassRelationship(child, parent, property, false);
//			}
//			else
//				rm.addEquivalence(child, parent, property, false);
//			objectSomeValues.add(property, child, parent);
//		}
//		//If it is a 'all values' object property restriction, process it
//		else if(type.equals(ClassExpressionType.OBJECT_ALL_VALUES_FROM))
//		{
//			Set<OWLObjectProperty> props = e.getObjectPropertiesInSignature();
//			if(props == null || props.size() != 1)
//				return;
//			OWLObjectProperty p = props.iterator().next();
//			int property = rm.getIndex(p.getIRI().toString());
//			Set<OWLClass> sup = e.getClassesInSignature();
//			if(sup == null || sup.size() != 1)
//				return;					
//			OWLClass cls = sup.iterator().next();
//			parent = rm.getIndex(cls.getIRI().toString());
//			if(parent == -1 || property == -1)
//				return;
//			if(sub)
//			{
//				if(inverse)
//					rm.addClassRelationship(parent, child, property, false);
//				else
//					rm.addClassRelationship(child, parent, property, false);
//			}
//			else
//				rm.addEquivalence(child, parent, property, false);
//
//			objectAllValues.add(property, child, parent);
//		}
//		//If it is an intersection of classes, capture the implied subclass relationships
//		else if(type.equals(ClassExpressionType.OBJECT_INTERSECTION_OF))
//		{
//			//TODO: control nesting when revising this method
//			Set<OWLClassExpression> inter = e.asConjunctSet();
//			for(OWLClassExpression cls : inter)
//				addRelationship(o,c,cls,true,false);
//		}
//		//If it is a union of classes, capture the implied subclass relationships
//		else if(type.equals(ClassExpressionType.OBJECT_UNION_OF))
//		{
//			Set<OWLClassExpression> union = e.asDisjunctSet();
//			for(OWLClassExpression cls : union)
//				addRelationship(o,c,cls,true,true);
//		}
//		//Otherwise, we're only interested in properties that may lead to disjointness
//		else if(type.equals(ClassExpressionType.OBJECT_EXACT_CARDINALITY))
//		{
//			Set<OWLObjectProperty> props = e.getObjectPropertiesInSignature();
//			if(props == null || props.size() != 1)
//				return;
//			OWLObjectProperty p = props.iterator().next();
//			int property = rm.getIndex(p.getIRI().toString());
//			if(property == -1)
//				return;
//			int cardinality = ((OWLObjectCardinalityRestrictionImpl)e).getCardinality();
//			card.add(property, child, cardinality);					
//		}
//		else if(type.equals(ClassExpressionType.OBJECT_MAX_CARDINALITY))
//		{
//			Set<OWLObjectProperty> props = e.getObjectPropertiesInSignature();
//			if(props == null || props.size() != 1)
//				return;
//			OWLObjectProperty p = props.iterator().next();
//			int property = rm.getIndex(p.getIRI().toString());
//			if(property == -1)
//				return;
//			int cardinality = ((OWLObjectCardinalityRestrictionImpl)e).getCardinality();
//			maxCard.add(property, child, cardinality);					
//		}
//		else if(type.equals(ClassExpressionType.OBJECT_MIN_CARDINALITY))
//		{
//			Set<OWLObjectProperty> props = e.getObjectPropertiesInSignature();
//			if(props == null || props.size() != 1)
//				return;
//			OWLObjectProperty p = props.iterator().next();
//			int property = rm.getIndex(p.getIRI().toString());
//			if(property == -1)
//				return;
//			int cardinality = ((OWLObjectCardinalityRestrictionImpl)e).getCardinality();
//			minCard.add(property, child, cardinality);					
//		}
//		else if(type.equals(ClassExpressionType.DATA_ALL_VALUES_FROM))
//		{
//			OWLDataAllValuesFromImpl av = (OWLDataAllValuesFromImpl)e;
//			Set<OWLDataProperty> props = av.getDataPropertiesInSignature();
//			if(props == null || props.size() != 1)
//				return;
//			OWLDataProperty p = props.iterator().next();
//			int property = rm.getIndex(p.getIRI().toString());
//			if(property == -1)
//				return;
//			Set<OWLDatatype> dt = av.getDatatypesInSignature();
//			String value = "";
//			for(OWLDatatype d : dt)
//				value += d.toString() + " ";
//			value.trim();
//			dataAllValues.add(property, child, value);
//		}
//		else if(type.equals(ClassExpressionType.DATA_SOME_VALUES_FROM))
//		{
//			OWLDataSomeValuesFromImpl av = (OWLDataSomeValuesFromImpl)e;
//			Set<OWLDataProperty> props = av.getDataPropertiesInSignature();
//			if(props == null || props.size() != 1)
//				return;
//			OWLDataProperty p = props.iterator().next();
//			int property = rm.getIndex(p.getIRI().toString());
//			if(property == -1)
//				return;
//			Set<OWLDatatype> dt = av.getDatatypesInSignature();
//			String value = "";
//			for(OWLDatatype d : dt)
//				value += d.toString() + " ";
//			value.trim();
//			dataSomeValues.add(property, child, value);
//		}
//		else if(type.equals(ClassExpressionType.DATA_HAS_VALUE))
//		{
//			OWLDataHasValueImpl hv = (OWLDataHasValueImpl)e; 
//			Set<OWLDataProperty> props = hv.getDataPropertiesInSignature();
//			if(props == null || props.size() != 1)
//				return;
//			OWLDataProperty p = props.iterator().next();
//			if(!p.isFunctional(o))
//				return;
//			int property = rm.getIndex(p.getIRI().toString());
//			if(property == -1)
//				return;
//			String value = hv.getValue().toString();
//			if(p.isFunctional(o))
//				dataHasValue.add(property, child, value);
//		}
//		else if(type.equals(ClassExpressionType.DATA_EXACT_CARDINALITY))
//		{
//			Set<OWLDataProperty> props = e.getDataPropertiesInSignature();
//			if(props == null || props.size() != 1)
//				return;
//			OWLDataProperty p = props.iterator().next();
//			int property = rm.getIndex(p.getIRI().toString());
//			if(property == -1)
//				return;
//			int cardinality = ((OWLDataCardinalityRestrictionImpl)e).getCardinality();
//			card.add(property, child, cardinality);					
//		}
//		else if(type.equals(ClassExpressionType.DATA_MAX_CARDINALITY))
//		{
//			Set<OWLDataProperty> props = e.getDataPropertiesInSignature();
//			if(props == null || props.size() != 1)
//				return;
//			OWLDataProperty p = props.iterator().next();
//			int property = rm.getIndex(p.getIRI().toString());
//			if(property == -1)
//				return;
//			int cardinality = ((OWLDataCardinalityRestrictionImpl)e).getCardinality();
//			maxCard.add(property, child, cardinality);					
//		}
//		else if(type.equals(ClassExpressionType.DATA_MIN_CARDINALITY))
//		{
//			Set<OWLDataProperty> props = e.getDataPropertiesInSignature();
//			if(props == null || props.size() != 1)
//				return;
//			OWLDataProperty p = props.iterator().next();
//			int property = rm.getIndex(p.getIRI().toString());
//			if(property == -1)
//				return;
//			int cardinality = ((OWLDataCardinalityRestrictionImpl)e).getCardinality();
//			minCard.add(property, child, cardinality);					
//		}
	}
}