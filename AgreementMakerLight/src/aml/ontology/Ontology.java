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
* An Ontology object, loaded using the OWL API.								  *
*																			  *
* @author Daniel Faria														  *
******************************************************************************/
package aml.ontology;

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

import org.semanticweb.elk.owlapi.ElkReasonerFactory;
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
import aml.settings.LexicalType;
import aml.settings.SKOS;
import aml.settings.EntityType;
import aml.util.MapSorter;
import aml.util.StringParser;
import aml.util.Table2Map;
import aml.util.Table2Set;

public class Ontology
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
	//Its value map
	private ValueMap vMap;
	//Its word lexicon
	private WordLexicon wLex;
	//Its reference map
	private ReferenceMap refs;
	//The map of class names (String) -> indexes (Integer) in the ontology
	//which is necessary for the cross-reference matching
	private HashMap<String,Integer> classNames;
	//Its set of obsolete classes
	private HashSet<Integer> obsolete;
	
	//Global variables & data structures
	private AML aml;
	private boolean useReasoner;
	private boolean isSKOS;
	private URIMap uris;
	private RelationshipMap rm;
	
	//Auxiliary data structures to capture semantic disjointness
	private Table2Map<Integer,Integer,Integer> maxCard, minCard, card;
	private Table2Map<Integer,Integer,String> dataAllValues, dataHasValue, dataSomeValues;
	private Table2Map<Integer,Integer,Integer> objectAllValues, objectSomeValues;
	
//Constructors

	/**
	 * Constructs an empty ontology
	 */
	private Ontology()
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
		vMap = new ValueMap();
		refs = new ReferenceMap();
		classNames = new HashMap<String,Integer>();
		obsolete = new HashSet<Integer>();
		wLex = null;
		aml = AML.getInstance();
		useReasoner = aml.useReasoner();
		uris = aml.getURIMap();
		rm = aml.getRelationshipMap();
	}
	
	/**
	 * Constructs an Ontology from file 
	 * @param path: the path to the input Ontology file
	 * @throws OWLOntologyCreationException 
	 */
	public Ontology(String path) throws OWLOntologyCreationException
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
		System.out.println(refs.size());
	}

	/**
	 * Constructs an Ontology from an URI  
	 * @param uri: the URI of the input Ontology
	 * @throws OWLOntologyCreationException 
	 */
	public Ontology(URI uri) throws OWLOntologyCreationException
	{
		this();
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
		this.uri = uri.toString(); 
		init(o);
		//Close the OntModel
		manager.removeOntology(o);
		//Reset the entity expansion limit
		System.clearProperty(LIMIT);
	}

	/**
	 * Constructs an Ontology from an OWLOntology
	 * @param o: the OWLOntology to use
	 */
	public Ontology(OWLOntology o)
	{
		this();
		init(o);
		//Reset the entity expansion limit
		System.clearProperty(LIMIT);
	}

	//Public Methods

	/**
	 * Adds an entity to the Ontology
	 * @param index: the index of the entity to add
	 */
	public void add(int index)
	{
		entities.add(index);
		entityTypes.add(uris.getType(index), index);
	}

	/**
	 * Closes the Ontology data structures
	 */
	public void close()
	{
		manager = null;
		factory = null;
		entities = null;
		lex = null;
		uri = null;
		classNames = null;
		wLex = null;
		obsolete = null;
		aml = null;
		uris = null;
		rm = null;
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
	 * @return the number of Entities in the Ontology
	 */
	public int count()
	{
		return entities.size();
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
	public Set<Integer> getEntities(EntityType e)
	{
		if(entityTypes.contains(e))
			return entityTypes.get(e);
		return new HashSet<Integer>();
	}

	/**
	 * @param name: the localName of the class to get from the Ontology
	 * @return the index of the corresponding name in the Ontology
	 */
	public int getIndex(String name)
	{
		if(classNames.containsKey(name))
			return classNames.get(name);
		return -1;
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
	 * @param index: the index of the URI in the ontology
	 * @return whether the index corresponds to an obsolete class
	 */
	public boolean isObsoleteClass(int index)
	{
		return obsolete.contains(index);
	}

	/**
	 * @return whether this ontology is SKOS or OWL/OBO
	 */
	public boolean isSKOS()
	{
		return isSKOS;
	}
	
//Private Methods	

	//Builds the ontology data structures
	private void init(OWLOntology o)
	{
		//Check if the ontology is in SKOS format
		if(o.containsClassInSignature(SKOS.CONCEPT_SCHEME.toIRI()) &&
				o.containsClassInSignature(SKOS.CONCEPT.toIRI()))
		{
			isSKOS = true;
			//Update the URI of the ontology
			OWLClass scheme = getClass(o,SKOS.CONCEPT_SCHEME.toIRI());
			Set<OWLIndividual> indivs = scheme.getIndividuals(o);
			String schemeURIs = "";
			for(OWLIndividual i : indivs)
				if(i.isNamed())
					schemeURIs += i.asOWLNamedIndividual().getIRI().toString() + " | ";
			if(schemeURIs.length() > 0)
				uri = schemeURIs.substring(0, schemeURIs.length() - 3);

			getSKOSConcepts(o);
			getSKOSRelationships(o);
		}
		else
		{
			isSKOS = false;
			//Update the URI of the ontology (if it lists one)
			if(o.getOntologyID().getOntologyIRI() != null)
				uri = o.getOntologyID().getOntologyIRI().toString();
			//Get the classes and their names and synonyms
			getOWLClasses(o);
			//Get the properties
			getOWLProperties(o);
			//Get the individuals
			getOWLNamedIndividuals(o);
			//Build the relationship map
			getOWLRelationships(o);
		}
	}

	//SKOS Thesauri

	//Processes the classes and their lexical information
	private void getSKOSConcepts(OWLOntology o)
	{
		//The Lexical type and weight
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
			//Add it to the global list of URIs (as a class)
			int id = uris.addURI(indivUri, EntityType.CLASS);
			//Add it to the class map
			entities.add(id);			
			//Get the local name from the URI
			String name = uris.getLocalName(id);
			//Add the name to the classNames map
			classNames.put(name, id);
			//If the local name is not an alphanumeric code, add it to the lexicon
			if(!StringParser.isNumericId(name))
			{
				type = LexicalType.LOCAL_NAME;
				weight = type.getDefaultWeight();
				lex.add(id, name, "en", type, "", weight);
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
						lex.add(id, name, lang, type, "", weight);
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
								lex.add(id, name, lang, type, "", weight);
							}
						}
					}
				}
			}
		}
	}

	//Reads all class relationships
	private void getSKOSRelationships(OWLOntology o)
	{
		//For simplicity, we convert "broader", "broader_transitive", "narrower"
		//and "narrower_transitive" to subclass relationships
		//We treat "related" as a form of equivalence with the "related" property

		//Start by adding the "related" property
		int related = uris.addURI(SKOS.RELATED.toIRI().toString(),EntityType.OBJECT);
		lex.add(related, SKOS.RELATED.toString(), "en", LexicalType.LOCAL_NAME,
				"", LexicalType.LOCAL_NAME.getDefaultWeight());
		entities.add(related);
		entityTypes.add(EntityType.OBJECT, related);
		rm.addSymmetric(related);
		//Create a temporary map of disjoints from "related" concepts
		Table2Set<Integer,Integer> disj = new Table2Set<Integer,Integer>();
		//Check that the thesaurus explicitly defines the SKOS object properties
		//(for convenience, we test only the "skos:broader") 
		boolean hasObject = o.containsObjectPropertyInSignature(SKOS.BROADER.toIRI());
		//Retrieving the "concept" class
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
			int child = uris.getIndex(ind.getIRI().toString());
			if(child == -1)
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
						int parent = uris.getIndex(p.asOWLNamedIndividual().getIRI().toString());
						if(parent == -1)
							continue;
						//And add the relation according to the Object Property
						if(rel.equals(SKOS.BROADER.toIRI()) || rel.equals(SKOS.BROADER_TRANS.toIRI()))
							rm.addClassRelationship(child, parent, -1, false);
						else if(rel.equals(SKOS.NARROWER.toIRI()) || rel.equals(SKOS.BROADER_TRANS.toIRI()))
							rm.addClassRelationship(parent, child, -1, false);
						else if(rel.equals(SKOS.RELATED.toIRI()))
						{
							rm.addEquivalence(child, parent, related, false);
							disj.add(child, parent);
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
					int parent = uris.getIndex(v.toString());
					if(parent == -1)
						continue;
					//And add the relation according to the Object Property
					if(rel.equals(SKOS.BROADER.toIRI()) || rel.equals(SKOS.BROADER_TRANS.toIRI()))
						rm.addClassRelationship(child, parent, -1, false);
					else if(rel.equals(SKOS.NARROWER.toIRI()) || rel.equals(SKOS.BROADER_TRANS.toIRI()))
						rm.addClassRelationship(parent, child, -1, false);
					else if(rel.equals(SKOS.RELATED.toIRI()))
					{
						rm.addEquivalence(child, parent, related, false);
						disj.add(child, parent);
					}
				}
			}
		}
		for(Integer i : disj.keySet())
		{
			Set<Integer> top1 = getTopParents(i);
			for(Integer j : disj.get(i))
			{
				Set<Integer> top2 = getTopParents(j);
				for(Integer k : top1)
					for(Integer l : top2)
						rm.addDisjoint(k, l);
			}
		}
	}

	//OWL Ontologies

	//Processes the classes and their lexical information
	private void getOWLClasses(OWLOntology o)
	{
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
			//Add it to the global list of URIs
			int id = uris.addURI(classUri,EntityType.CLASS);
			//Add it to the Ontology
			entities.add(id);
			entityTypes.add(EntityType.CLASS, id);

			//Get the local name from the URI
			String name = uris.getLocalName(id);
			//Add the name to the classNames map
			classNames.put(name, id);

			//If the local name is not an alphanumeric code, add it to the lexicon
			if(!StringParser.isNumericId(name))
			{
				type = LexicalType.LOCAL_NAME;
				weight = type.getDefaultWeight();
				lex.add(id, name, "en", type, "", weight);
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
						lex.add(id, name, lang, type, "", weight);
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
								lex.add(id, name, lang, type, "", weight);
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
						refs.add(id,xRef.replace(':','_'));
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
							obsolete.add(id);
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
					refs.add(id, e.toString());
			}
		}
	}

	//Reads the properties
	private void getOWLProperties(OWLOntology o)
	{
		LexicalType type;
		double weight;
		//The label property
		OWLAnnotationProperty label = factory
				.getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_LABEL.getIRI());

		//Get the Data Properties
		Set<OWLDataProperty> dProps = o.getDataPropertiesInSignature(true);
		for(OWLDataProperty dp : dProps)
		{
			String propUri = dp.getIRI().toString();
			if(propUri == null)
				continue;
			//Add it to the global list of URIs
			int id = uris.addURI(propUri,EntityType.DATA);
			//And to the ontology
			entities.add(id);
			entityTypes.add(EntityType.DATA, id);

			//Get the local name from the URI
			String localName = getLocalName(propUri);
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
					lex.add(id, name, lang, type, "", weight);
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
				lex.add(id, localName, lang, type, "", weight);
			}
			//If the property is functional, add it to the RelationshipMap
			if(dp.isFunctional(o))
				rm.addFunctional(id);
			//Add its domain(s) to the RelationshipMap
			Set<OWLClassExpression> domains = dp.getDomains(o);
			for(OWLClassExpression ce : domains)
			{
				Set<OWLClassExpression> union = ce.asDisjunctSet();
				for(OWLClassExpression cf : union)
				{
					if(cf instanceof OWLClass)
						rm.addDomain(id, uris.getIndex(cf.asOWLClass().getIRI().toString()));
				}
			}
			//And finally its range(s)
			Set<OWLDataRange> ranges = dp.getRanges(o);
			for(OWLDataRange dr : ranges)
			{
				if(dr.isDatatype())
					rm.addRange(id, dr.asOWLDatatype().toStringID());
			}
		}
		//Get the Object Properties
		Set<OWLObjectProperty> oProps = o.getObjectPropertiesInSignature(true);
		for(OWLObjectProperty op : oProps)
		{
			String propUri = op.getIRI().toString();
			if(propUri == null)
				continue;
			//Add it to the global list of URIs
			int id = uris.addURI(propUri,EntityType.OBJECT);
			//And to the ontology
			entities.add(id);
			entityTypes.add(EntityType.OBJECT, id);

			//Get the local name from the URI
			String localName = getLocalName(propUri);
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
					lex.add(id, name, lang, type, "", weight);
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
				lex.add(id, localName, lang, type, "", weight);
			}
			//If the property is transitive and/or symmetric and/or functional
			//add it to the RelationshipMap
			if(op.isTransitive(o))
				rm.addTransitive(id);
			if(op.isSymmetric(o))
				rm.addSymmetric(id);
			if(op.isFunctional(o))
				rm.addFunctional(id);

			//Add its domain(s) to the RelationshipMap
			Set<OWLClassExpression> domains = op.getDomains(o);
			for(OWLClassExpression ce : domains)
			{
				Set<OWLClassExpression> union = ce.asDisjunctSet();
				for(OWLClassExpression cf : union)
				{
					if(cf instanceof OWLClass)
						rm.addDomain(id, uris.getIndex(cf.asOWLClass().getIRI().toString()));
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
						rm.addRange(id, uris.getIndex(cf.asOWLClass().getIRI().toString()));
				}
			}
		}
		//Process "transitive over" relations
		//(This requires that all properties be indexed, and thus has to be done in a 2nd pass)
		for(OWLObjectProperty op : oProps)
		{
			//In OWL, the OBO transitive_over relation is encoded as a sub-property chain of
			//the form: "SubObjectPropertyOf(ObjectPropertyChain( <p1> <p2> ) <this_p> )"
			//in which 'this_p' is usually 'p1' but can also be 'p2' (in case you want to
			//define that another property is transitive over this one, which may happen when
			//the other property is imported and this property occurs only in this ontology)
			for(OWLAxiom e : op.getReferencingAxioms(o))
			{
				if(!e.isOfType(AxiomType.SUB_PROPERTY_CHAIN_OF))
					continue;
				//Unfortunately, there isn't much support for "ObjectPropertyChain"s in the OWL
				//API, so the only way to get the referenced properties while preserving their
				//order is to parse the String representation of the sub-property chain
				//(getObjectPropertiesInSignature() does NOT preserve the order)
				String[] chain = e.toString().split("[\\(\\)]");
				//Make sure the structure of the sub-property chain is in the expected format
				if(!chain[0].equals("SubObjectPropertyOf") || !chain[1].equals("ObjectPropertyChain"))
					continue;
				//Get the indexes of the tags surrounding the URIs
				int index1 = chain[2].indexOf("<")+1;
				int index2 = chain[2].indexOf(">");
				int index3 = chain[2].lastIndexOf("<")+1;
				int index4 = chain[2].lastIndexOf(">");
				//Make sure the indexes check up
				if(index1 < 0 || index2 <= index1 || index3 <= index2 || index4 <= index3)
					continue;
				String uri1 = chain[2].substring(index1,index2);
				String uri2 = chain[2].substring(index3,index4);
				int id1 = uris.getIndex(uri1);
				int id2 = uris.getIndex(uri2);
				//Make sure the URIs are listed object properties
				if(!entityTypes.contains(EntityType.OBJECT, id1) ||
						!entityTypes.contains(EntityType.OBJECT, id2))
					continue;
				//If everything checks up, add the relation to the transitiveOver map
				rm.addTransitiveOver(id1, id2);
			}
		}
	}

	//Processes the named individuals and their data property values
	//@author Catia Pesquita
	private void getOWLNamedIndividuals(OWLOntology o)
	{
		Map<String,Integer> langCounts = new LinkedHashMap<String,Integer>();
		LexicalType type;
		double weight;
		//The label property
		OWLAnnotationProperty label = factory.getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_LABEL.getIRI());
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
			int id = uris.addURI(indivUri, EntityType.INDIVIDUAL);
			//Add it to the Ontology
			entities.add(id);
			entityTypes.add(EntityType.INDIVIDUAL, id);
			//Get the local name from the URI
			String localName = getLocalName(indivUri);
			//Get the label(s)
			String lang = "";
			for(OWLAnnotation a : i.getAnnotations(o,label))
			{
				if(a.getValue() instanceof OWLLiteral)
				{
					OWLLiteral val = (OWLLiteral) a.getValue();
					String name = val.getLiteral();
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
					lex.add(id, name, lang, type, "", weight);
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
				lex.add(id, localName, lang, type, "", weight);
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
						lex.add(id, name, localLang, type, "", weight);
					}
					else if(annotation.getValue() instanceof IRI)
					{
						String iri = ((IRI)annotation.getValue()).toString();
						String name = getLocalName(iri);
						if(!StringParser.isNumericId(name))
							lex.add(id, name, lang, type, "", weight);
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
								lex.add(id, name, localLang, type, "", weight);
							}
						}
					}
				}
				//Otherwise, literal annotations go to the ValueMap
				else if(annotation.getValue() instanceof OWLLiteral)
				{
					OWLLiteral val = (OWLLiteral) annotation.getValue();
					String v = val.getLiteral();
					//We must first add the annotation property to the URIMap and Ontology
					//(if it was already added, nothing happens)
					int propId = uris.addURI(propUri, EntityType.ANNOTATION);
					entities.add(propId);
					entityTypes.add(EntityType.ANNOTATION, propId);
					//Then add the value to the ValueMap
					vMap.add(id, propId, v);
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
				int propIndex = uris.getIndex(propUri);
				if(propIndex == -1)
					continue;

				//Data Properties with a LexicalType go to the Lexicon
				type = LexicalType.getLexicalType(propUri);
				if(type != null)
				{
					weight = type.getDefaultWeight();
					for(OWLLiteral val : dataPropValues.get(prop))
						lex.add(id, val.getLiteral(), "en", type, "", weight);
				}
				//Otherwise, they go to the ValueMap
				else
				{
					//Then get its values for the individual
					for(OWLLiteral val : dataPropValues.get(prop))
						vMap.add(id, propIndex, val.getLiteral());
				}
				//FIX: Filling in missing types of individuals from data property restrictions
				//(Sometimes ontologies fail to declare individual types)
				if(rm.getIndividualClasses(id).isEmpty() && rm.getDomains(propIndex).size() == 1)
					rm.addInstance(id, rm.getObjectRanges(propIndex).iterator().next());
			}	
		}
	}

	//Reads all class relationships
	private void getOWLRelationships(OWLOntology o)
	{
		OWLReasoner reasoner = null;		
		if(useReasoner)
		{
			//Create an ELK reasoner
			OWLReasonerFactory reasonerFactory = new ElkReasonerFactory();
			reasoner = reasonerFactory.createReasoner(o);
		}

		//Auxiliary data structures to capture semantic disjointness
		//Two classes are disjoint if they have:
		//1) Incompatible cardinality restrictions for the same property
		maxCard = new Table2Map<Integer,Integer,Integer>();
		minCard = new Table2Map<Integer,Integer,Integer>();
		card = new Table2Map<Integer,Integer,Integer>();
		//2) Different values for the same functional data property or incompatible value
		//restrictions on the same non-functional data property
		dataAllValues = new Table2Map<Integer,Integer,String>();
		dataHasValue = new Table2Map<Integer,Integer,String>();
		dataSomeValues = new Table2Map<Integer,Integer,String>();
		//3) Disjoint classes for the same functional object property or incompatible value
		//restrictions on disjoint classes for the same non-functional object property
		objectAllValues = new Table2Map<Integer,Integer,Integer>();
		objectSomeValues = new Table2Map<Integer,Integer,Integer>();

		//I - Relationships involving classes
		//Get an iterator over the ontology classes
		Set<OWLClass> classes = o.getClassesInSignature(true);
		//For each class index
		for(OWLClass c : classes)
		{
			//Get its identifier
			int child = uris.getIndex(c.getIRI().toString());
			if(child == -1)
				continue;

			if(useReasoner)
			{
				//Get its superclasses using the reasoner
				Set<OWLClass> parents = reasoner.getSuperClasses(c, true).getFlattened();
				for(OWLClass par : parents)
				{
					int parent = uris.getIndex(par.getIRI().toString());
					if(parent > -1)
					{
						rm.addDirectSubclass(child, parent);
						String name = uris.getLocalName(parent);
						if(name.contains("Obsolete") || name.contains("obsolete") ||
								name.contains("Retired") || name.contains ("retired") ||
								name.contains("Deprecated") || name.contains("deprecated"))
							obsolete.add(child);
					}
				}
				//Get its equivalent classes using the reasoner
				Node<OWLClass> equivs = reasoner.getEquivalentClasses(c);
				for(OWLClass eq : equivs)
				{
					int parent = uris.getIndex(eq.getIRI().toString());
					if(parent > -1 && parent != child)
						rm.addEquivalentClass(child, parent);
				}
			}

			//Get the subclass expressions to capture and add relationships
			Set<OWLClassExpression> superClasses = c.getSuperClasses(o);
			for(OWLOntology ont : o.getDirectImports())
				superClasses.addAll(c.getSuperClasses(ont));
			for(OWLClassExpression e : superClasses)
				addRelationship(o,c,e,true,false);

			//Get the equivalence expressions to capture and add relationships
			Set<OWLClassExpression> equivClasses = c.getEquivalentClasses(o);
			for(OWLOntology ont : o.getDirectImports())
				equivClasses.addAll(c.getEquivalentClasses(ont));
			for(OWLClassExpression e : equivClasses)
				addRelationship(o,c,e,false,false);

			//Get the individuals that belong to the class
			//@author: Catia Pesquita
			Set<OWLIndividual> indivs = c.getIndividuals(o);
			for(OWLIndividual i : indivs)
			{
				if(i.isNamed())
				{
					int ind = uris.getIndex(((OWLNamedIndividual)i).getIRI().toString());
					rm.addInstance(ind, child);
				}
			}

			//Get the syntactic disjoints
			Set<OWLClassExpression> disjClasses = c.getDisjointClasses(o);
			for(OWLOntology ont : o.getDirectImports())
				disjClasses.addAll(c.getDisjointClasses(ont));
			//For each expression
			for(OWLClassExpression dClass : disjClasses)
			{
				ClassExpressionType type = dClass.getClassExpressionType();
				//If it is a class, process it
				if(type.equals(ClassExpressionType.OWL_CLASS))
				{
					int parent = uris.getIndex(dClass.asOWLClass().getIRI().toString());
					if(parent > -1)
						rm.addDisjoint(child, parent);
				}
				//If it is a union, process it
				else if(type.equals(ClassExpressionType.OBJECT_UNION_OF))
				{
					Set<OWLClass> dc = dClass.getClassesInSignature();
					for(OWLClass ce : dc)
					{
						int parent = uris.getIndex(ce.getIRI().toString());
						if(parent > -1)
							rm.addDisjoint(child, parent);
					}
				}
				//If it is an intersection, check for common descendants
				else if(type.equals(ClassExpressionType.OBJECT_INTERSECTION_OF))
				{
					Set<OWLClass> dc = dClass.getClassesInSignature();
					HashSet<Integer> intersect = new HashSet<Integer>();
					for(OWLClass ce : dc)
					{
						int parent = uris.getIndex(ce.getIRI().toString());
						if(parent > -1)
							intersect.add(parent);
					}
					Set<Integer> subclasses = rm.getCommonSubClasses(intersect);
					for(Integer i : subclasses)
						rm.addDisjoint(child,i);
				}
			}
		}

		//Finally process the semantically disjoint classes
		//Classes that have incompatible cardinalities on the same property
		//First exact cardinalities vs exact, min and max cardinalities
		for(Integer prop : card.keySet())
		{
			Vector<Integer> exact = new Vector<Integer>(card.keySet(prop));
			for(int i = 0; i < exact.size()-1; i++)
				for(int j = i+1; j < exact.size(); j++)
					if(card.get(prop, exact.get(i)) != card.get(prop, exact.get(j)))
						rm.addDisjoint(exact.get(i), exact.get(j));
			Set<Integer> max = maxCard.keySet(prop);
			if(max != null)
				for(int i = 0; i < exact.size(); i++)
					for(Integer j : max)
						if(card.get(prop, exact.get(i)) > maxCard.get(prop, j))
							rm.addDisjoint(exact.get(i), j);
			Set<Integer> min = minCard.keySet(prop);
			if(min != null)
				for(int i = 0; i < exact.size(); i++)
					for(Integer j : min)
						if(card.get(prop, exact.get(i)) > minCard.get(prop, j))
							rm.addDisjoint(exact.get(i), j);				
		}
		//Then min vs max cardinalities
		for(Integer prop : minCard.keySet())
		{
			Set<Integer> min = minCard.keySet(prop);
			Set<Integer> max = maxCard.keySet(prop);
			if(max == null)
				continue;
			for(Integer i : min)
				for(Integer j : max)
					if(minCard.get(prop, i) > maxCard.get(prop, j))
						rm.addDisjoint(i, j);
		}
		//Data properties with incompatible values
		//First hasValue restrictions on functional data properties
		for(Integer prop : dataHasValue.keySet())
		{
			Vector<Integer> cl = new Vector<Integer>(dataHasValue.keySet(prop));
			for(int i = 0; i < cl.size()-1; i++)
				for(int j = i+1; j < cl.size(); j++)
					if(!dataHasValue.get(prop, cl.get(i)).equals(dataHasValue.get(prop, cl.get(j))))
						rm.addDisjoint(cl.get(i), cl.get(j));
		}
		//Then incompatible someValues restrictions on functional data properties
		for(Integer prop : dataSomeValues.keySet())
		{
			Vector<Integer> cl = new Vector<Integer>(dataSomeValues.keySet(prop));
			for(int i = 0; i < cl.size()-1; i++)
			{
				for(int j = i+1; j < cl.size(); j++)
				{
					String[] datatypes = dataSomeValues.get(prop, cl.get(j)).split(" ");
					for(String d: datatypes)
					{
						if(!dataSomeValues.get(prop, cl.get(i)).contains(d))
						{
							rm.addDisjoint(cl.get(i), cl.get(j));
							break;
						}
					}
				}
			}
		}
		//Then incompatible allValues restrictions on all data properties
		//(allValues vs allValues and allValues vs someValues)
		for(Integer prop : dataAllValues.keySet())
		{
			Vector<Integer> cl = new Vector<Integer>(dataAllValues.keySet(prop));
			for(int i = 0; i < cl.size()-1; i++)
			{
				for(int j = i+1; j < cl.size(); j++)
				{
					String[] datatypes = dataAllValues.get(prop, cl.get(j)).split(" ");
					for(String d: datatypes)
					{
						if(!dataAllValues.get(prop, cl.get(i)).contains(d))
						{
							rm.addDisjoint(cl.get(i), cl.get(j));
							break;
						}
					}
				}
			}
			Set<Integer> sv = dataSomeValues.keySet(prop);
			if(sv == null)
				continue;
			for(Integer i : cl)
			{
				for(Integer j : sv)
				{
					String[] datatypes = dataSomeValues.get(prop, j).split(" ");
					for(String d: datatypes)
					{
						if(!dataAllValues.get(prop, i).contains(d))
						{
							rm.addDisjoint(i, j);
							break;
						}
					}
				}
			}
		}
		//Classes that incompatible value restrictions for the same object property
		//(i.e., the restrictions point to disjoint classes)
		//First allValues restrictions
		for(Integer prop : objectAllValues.keySet())
		{
			Vector<Integer> cl = new Vector<Integer>(objectAllValues.keySet(prop));
			for(int i = 0; i < cl.size() - 1; i++)
			{
				int c1 = objectAllValues.get(prop, cl.get(i));
				for(int j = i + 1; j < cl.size(); j++)
				{
					int c2 = objectAllValues.get(prop, cl.get(j));
					if(c1 != c2 && rm.areDisjoint(c1, c2))
						rm.addDisjoint(cl.get(i), cl.get(j));
				}
			}

			Set<Integer> sv = objectSomeValues.keySet(prop);
			if(sv == null)
				continue;
			for(Integer i : cl)
			{
				int c1 = objectAllValues.get(prop, i);
				for(Integer j : sv)
				{
					int c2 = objectSomeValues.get(prop, j);
					if(c1 != c2 && rm.areDisjoint(c1, c2))
						rm.addDisjoint(i, j);
				}
			}
		}
		//Finally someValues restrictions on functional properties
		for(Integer prop : objectAllValues.keySet())
		{
			if(!rm.isFunctional(prop))
				continue;
			Set<Integer> sv = objectSomeValues.keySet(prop);
			if(sv == null)
				continue;
			Vector<Integer> cl = new Vector<Integer>(sv);

			for(int i = 0; i < cl.size() - 1; i++)
			{
				int c1 = objectSomeValues.get(prop, cl.get(i));
				for(int j = i + 1; j < cl.size(); j++)
				{
					int c2 = objectAllValues.get(prop, cl.get(j));
					if(c1 != c2 && rm.areDisjoint(c1, c2))
						rm.addDisjoint(cl.get(i), cl.get(j));
				}
			}
		}

		//Clean auxiliary data structures
		maxCard = null;
		minCard = null;
		card = null;
		dataAllValues = null;
		dataHasValue = null;
		dataSomeValues = null;
		objectAllValues = null;
		objectSomeValues = null;

		//II - Relationships between named individuals
		//@author: Catia Pesquita
		//Get an iterator over the named individuals
		Set<OWLNamedIndividual> individuals = o.getIndividualsInSignature();
		for(OWLNamedIndividual i : individuals)
		{
			//Get the numeric id for each individual
			int namedIndivId = uris.getIndex(i.getIRI().toString());
			if(namedIndivId == -1)
				continue;

			Map<OWLObjectPropertyExpression, Set<OWLIndividual>> iProps = i.getObjectPropertyValues(o);
			for(OWLObjectPropertyExpression prop : iProps.keySet())
			{
				if(prop.isAnonymous())
					continue;

				String propURI = prop.asOWLObjectProperty().getIRI().toString();
				int propIndex = uris.getIndex(propURI);
				if(propIndex == -1)
					continue;

				//FIX: Filling in missing types of individuals from object property restrictions
				//(Sometimes ontologies fail to declare individual types)
				if(rm.getIndividualClasses(namedIndivId).isEmpty() && rm.getDomains(propIndex).size() == 1)
					rm.addInstance(namedIndivId, rm.getDomains(propIndex).iterator().next());

				for(OWLIndividual rI : iProps.get(prop))
				{
					if(rI.isNamed())
					{
						int namedRelIndivId = uris.getIndex(rI.asOWLNamedIndividual().getIRI().toString());
						rm.addIndividualRelationship(namedIndivId, namedRelIndivId, propIndex);
						//If the individual is an alias of the related individual
						//use the individual's lexical entries to extend those of
						//the related individual
						if(propURI.endsWith("isAliasOf"))
						{
							for(String name : lex.getNames(namedIndivId))
							{
								for(Provenance p : lex.get(name, namedIndivId))
								{
									LexicalType t = p.getType();
									if(t.equals(LexicalType.LABEL) || t.equals(LexicalType.LOCAL_NAME))
										t = LexicalType.EXACT_SYNONYM;
									double weight = t.getDefaultWeight();
									lex.add(namedRelIndivId,name,p.getLanguage(),t,"",weight);
								}
							}
						}
						//FIX: Filling in missing types of individuals from object property restrictions
						//(Sometimes ontologies fail to declare individual types)
						if(rm.getIndividualClasses(namedRelIndivId).isEmpty() && rm.getObjectRanges(propIndex).size() == 1)
							rm.addInstance(namedRelIndivId, rm.getObjectRanges(propIndex).iterator().next());
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
				LexicalType type = LexicalType.getLexicalType(propUri);
				if(type == null && annotation.getValue() instanceof IRI)
				{
					OWLNamedIndividual ni = factory.getOWLNamedIndividual((IRI) annotation.getValue());
					//Check that the named individual is in the URIMap
					int namedRelIndivId = uris.getIndex(ni.getIRI().toString());
					if(namedRelIndivId == -1)
						continue;
					//Add the property to the URIMap and Ontology as an object property
					int propId = uris.addURI(propUri, EntityType.OBJECT);
					entities.add(propId);
					entityTypes.add(EntityType.OBJECT, propId);
					//Add its name to the Lexicon
					lex.add(propId, getLocalName(propUri), "en", LexicalType.LOCAL_NAME,
							"", LexicalType.LOCAL_NAME.getDefaultWeight());
					//Add the relation to the RelationshipMap
					rm.addIndividualRelationship(namedIndivId, namedRelIndivId, propId);
				}
			}
		}


		//III - Relationships between properties
		//Data Properties
		Set<OWLDataProperty> dProps = o.getDataPropertiesInSignature(true);
		for(OWLDataProperty dp : dProps)
		{
			int propId = uris.getIndex(dp.getIRI().toString());
			if(propId == -1)
				continue;
			Set<OWLDataPropertyExpression> sProps = dp.getSuperProperties(o);
			for(OWLDataPropertyExpression de : sProps)
			{
				OWLDataProperty sProp = de.asOWLDataProperty();
				int sId = uris.getIndex(sProp.getIRI().toString());
				if(sId != -1)
					rm.addSubProperty(propId,sId);	
			}
		}
		//Object Properties
		Set<OWLObjectProperty> oProps = o.getObjectPropertiesInSignature(true);
		for(OWLObjectProperty op : oProps)
		{
			int propId = uris.getIndex(op.getIRI().toString());
			if(propId == -1)
				continue;
			Set<OWLObjectPropertyExpression> sProps = op.getSuperProperties(o);
			for(OWLObjectPropertyExpression oe : sProps)
			{
				OWLObjectProperty sProp = oe.asOWLObjectProperty();
				int sId = uris.getIndex(sProp.getIRI().toString());
				if(sId != -1)
					rm.addSubProperty(propId,sId);	
			}
			Set<OWLObjectPropertyExpression> iProps = op.getInverses(o);
			for(OWLObjectPropertyExpression oe : iProps)
			{
				OWLObjectProperty iProp = oe.asOWLObjectProperty();
				int iId = uris.getIndex(iProp.getIRI().toString());
				if(iId != -1)
					rm.addInverseProp(propId,iId);	
			}
		}
	}

	//Auxiliary Methods

	//Gets a named class from the given OWLOntology 
	private OWLClass getClass(OWLOntology o, IRI classIRI)
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
	private void addRelationship(OWLOntology o, OWLClass c, OWLClassExpression e, boolean sub, boolean inverse)
	{
		int child = uris.getIndex(c.getIRI().toString());
		int parent;
		ClassExpressionType type = e.getClassExpressionType();
		//If it is a class, and we didn't use the reasoner, process it here
		if(type.equals(ClassExpressionType.OWL_CLASS))
		{
			parent = uris.getIndex(e.asOWLClass().getIRI().toString());
			if(parent < 0)
				return;
			if(sub)
			{
				if(inverse)
					rm.addDirectSubclass(parent, child);
				else
					rm.addDirectSubclass(child, parent);
				String name = getName(parent);
				if(name.contains("Obsolete") || name.contains("obsolete") ||
						name.contains("Retired") || name.contains ("retired") ||
						name.contains("Deprecated") || name.contains("deprecated"))
					obsolete.add(child);
			}
			else
				rm.addEquivalentClass(child, parent);
		}
		//If it is a 'some values' object property restriction, process it
		else if(type.equals(ClassExpressionType.OBJECT_SOME_VALUES_FROM))
		{
			//TODO: parse someValuesFrom intersections and unions
			Set<OWLObjectProperty> props = e.getObjectPropertiesInSignature();
			if(props == null || props.size() != 1)
				return;
			OWLObjectProperty p = props.iterator().next();
			int property = uris.getIndex(p.getIRI().toString());
			Set<OWLClass> sup = e.getClassesInSignature();
			if(sup == null || sup.size() != 1)
				return;					
			OWLClass cls = sup.iterator().next();
			parent = uris.getIndex(cls.getIRI().toString());
			if(parent == -1 || property == -1)
				return;
			if(sub)
			{
				if(inverse)
					rm.addClassRelationship(parent, child, property, false);
				else
					rm.addClassRelationship(child, parent, property, false);
			}
			else
				rm.addEquivalence(child, parent, property, false);
			objectSomeValues.add(property, child, parent);
		}
		//If it is a 'all values' object property restriction, process it
		else if(type.equals(ClassExpressionType.OBJECT_ALL_VALUES_FROM))
		{
			Set<OWLObjectProperty> props = e.getObjectPropertiesInSignature();
			if(props == null || props.size() != 1)
				return;
			OWLObjectProperty p = props.iterator().next();
			int property = uris.getIndex(p.getIRI().toString());
			Set<OWLClass> sup = e.getClassesInSignature();
			if(sup == null || sup.size() != 1)
				return;					
			OWLClass cls = sup.iterator().next();
			parent = uris.getIndex(cls.getIRI().toString());
			if(parent == -1 || property == -1)
				return;
			if(sub)
			{
				if(inverse)
					rm.addClassRelationship(parent, child, property, false);
				else
					rm.addClassRelationship(child, parent, property, false);
			}
			else
				rm.addEquivalence(child, parent, property, false);

			objectAllValues.add(property, child, parent);
		}
		//If it is an intersection of classes, capture the implied subclass relationships
		else if(type.equals(ClassExpressionType.OBJECT_INTERSECTION_OF))
		{
			//TODO: control nesting when revising this method
			Set<OWLClassExpression> inter = e.asConjunctSet();
			for(OWLClassExpression cls : inter)
				addRelationship(o,c,cls,true,false);
		}
		//If it is a union of classes, capture the implied subclass relationships
		else if(type.equals(ClassExpressionType.OBJECT_UNION_OF))
		{
			Set<OWLClassExpression> union = e.asDisjunctSet();
			for(OWLClassExpression cls : union)
				addRelationship(o,c,cls,true,true);
		}
		//Otherwise, we're only interested in properties that may lead to disjointness
		else if(type.equals(ClassExpressionType.OBJECT_EXACT_CARDINALITY))
		{
			Set<OWLObjectProperty> props = e.getObjectPropertiesInSignature();
			if(props == null || props.size() != 1)
				return;
			OWLObjectProperty p = props.iterator().next();
			int property = uris.getIndex(p.getIRI().toString());
			if(property == -1)
				return;
			int cardinality = ((OWLObjectCardinalityRestrictionImpl)e).getCardinality();
			card.add(property, child, cardinality);					
		}
		else if(type.equals(ClassExpressionType.OBJECT_MAX_CARDINALITY))
		{
			Set<OWLObjectProperty> props = e.getObjectPropertiesInSignature();
			if(props == null || props.size() != 1)
				return;
			OWLObjectProperty p = props.iterator().next();
			int property = uris.getIndex(p.getIRI().toString());
			if(property == -1)
				return;
			int cardinality = ((OWLObjectCardinalityRestrictionImpl)e).getCardinality();
			maxCard.add(property, child, cardinality);					
		}
		else if(type.equals(ClassExpressionType.OBJECT_MIN_CARDINALITY))
		{
			Set<OWLObjectProperty> props = e.getObjectPropertiesInSignature();
			if(props == null || props.size() != 1)
				return;
			OWLObjectProperty p = props.iterator().next();
			int property = uris.getIndex(p.getIRI().toString());
			if(property == -1)
				return;
			int cardinality = ((OWLObjectCardinalityRestrictionImpl)e).getCardinality();
			minCard.add(property, child, cardinality);					
		}
		else if(type.equals(ClassExpressionType.DATA_ALL_VALUES_FROM))
		{
			OWLDataAllValuesFromImpl av = (OWLDataAllValuesFromImpl)e;
			Set<OWLDataProperty> props = av.getDataPropertiesInSignature();
			if(props == null || props.size() != 1)
				return;
			OWLDataProperty p = props.iterator().next();
			int property = uris.getIndex(p.getIRI().toString());
			if(property == -1)
				return;
			Set<OWLDatatype> dt = av.getDatatypesInSignature();
			String value = "";
			for(OWLDatatype d : dt)
				value += d.toString() + " ";
			value.trim();
			dataAllValues.add(property, child, value);
		}
		else if(type.equals(ClassExpressionType.DATA_SOME_VALUES_FROM))
		{
			OWLDataSomeValuesFromImpl av = (OWLDataSomeValuesFromImpl)e;
			Set<OWLDataProperty> props = av.getDataPropertiesInSignature();
			if(props == null || props.size() != 1)
				return;
			OWLDataProperty p = props.iterator().next();
			int property = uris.getIndex(p.getIRI().toString());
			if(property == -1)
				return;
			Set<OWLDatatype> dt = av.getDatatypesInSignature();
			String value = "";
			for(OWLDatatype d : dt)
				value += d.toString() + " ";
			value.trim();
			dataSomeValues.add(property, child, value);
		}
		else if(type.equals(ClassExpressionType.DATA_HAS_VALUE))
		{
			OWLDataHasValueImpl hv = (OWLDataHasValueImpl)e; 
			Set<OWLDataProperty> props = hv.getDataPropertiesInSignature();
			if(props == null || props.size() != 1)
				return;
			OWLDataProperty p = props.iterator().next();
			if(!p.isFunctional(o))
				return;
			int property = uris.getIndex(p.getIRI().toString());
			if(property == -1)
				return;
			String value = hv.getValue().toString();
			if(p.isFunctional(o))
				dataHasValue.add(property, child, value);
		}
		else if(type.equals(ClassExpressionType.DATA_EXACT_CARDINALITY))
		{
			Set<OWLDataProperty> props = e.getDataPropertiesInSignature();
			if(props == null || props.size() != 1)
				return;
			OWLDataProperty p = props.iterator().next();
			int property = uris.getIndex(p.getIRI().toString());
			if(property == -1)
				return;
			int cardinality = ((OWLDataCardinalityRestrictionImpl)e).getCardinality();
			card.add(property, child, cardinality);					
		}
		else if(type.equals(ClassExpressionType.DATA_MAX_CARDINALITY))
		{
			Set<OWLDataProperty> props = e.getDataPropertiesInSignature();
			if(props == null || props.size() != 1)
				return;
			OWLDataProperty p = props.iterator().next();
			int property = uris.getIndex(p.getIRI().toString());
			if(property == -1)
				return;
			int cardinality = ((OWLDataCardinalityRestrictionImpl)e).getCardinality();
			maxCard.add(property, child, cardinality);					
		}
		else if(type.equals(ClassExpressionType.DATA_MIN_CARDINALITY))
		{
			Set<OWLDataProperty> props = e.getDataPropertiesInSignature();
			if(props == null || props.size() != 1)
				return;
			OWLDataProperty p = props.iterator().next();
			int property = uris.getIndex(p.getIRI().toString());
			if(property == -1)
				return;
			int cardinality = ((OWLDataCardinalityRestrictionImpl)e).getCardinality();
			minCard.add(property, child, cardinality);					
		}
	}

	//Gets the top level parents of a class (recursively)
	private Set<Integer> getTopParents(int classId)
	{
		return getTopParents(rm.getSuperClasses(classId, true));
	}

	//Gets the top level parents of a class (recursively)
	private Set<Integer> getTopParents(Set<Integer> classes)
	{
		Set<Integer> parents = new HashSet<Integer>();
		for(int i : classes)
		{
			boolean check = parents.addAll(rm.getSuperClasses(i, true));
			if(!check)
				parents.add(i);
		}
		if(parents.equals(classes))
			return classes;
		else
			return getTopParents(parents);
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