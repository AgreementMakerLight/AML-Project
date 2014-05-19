/******************************************************************************
* Copyright 2013-2014 LASIGE                                                  *
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
* An Ontology object, loaded from an OWL or RDFS file using the Jena API and  *
* including links to the various structures that store the Ontology data.     *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 28-03-2014                                                            *
******************************************************************************/
package aml.ontology;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import aml.util.StringParser;

import com.hp.hpl.jena.ontology.EnumeratedClass;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.IntersectionClass;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.ontology.Restriction;
import com.hp.hpl.jena.ontology.UnionClass;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFList;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;


public class Ontology
{

//Attributes
	
	//The URI of the ontology
	protected String uri;
	//The local prefix of the ontology
	protected String localPrefix;
	//The list of URIs of its terms and properties
	protected Vector<String> uris;
	//The list of term and property local names
	protected Vector<String> names;
	//The index that separates terms from properties
	protected int separator;
	//Its list of properties
	protected PropertyList pr;
	//Its lexicon
	protected Lexicon lex;
	//Its map of relationships
	protected RelationshipMap rels;
	//Its map of cross-references
	protected ReferenceMap refs;
	//Whether only local namespace terms are loaded
	protected boolean localOnly = true;
	
	//Auxiliary map of synonym properties to lexicon types
	protected HashMap<OntProperty,String> synProps;
	
//Constructors

	/**
	 * Constructs an Ontology from an OWL or RDFS file  
	 * @param path: the URI of the input Ontology
	 * @param isInput: whether the ontology is an input ontology or
	 * an external ontology
	 */
	public Ontology(URI path, boolean isInput, boolean owl)
	{
		String uriString = path.toString();
		OntModel o = null;
		if(owl)
			o = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
		else
			o = ModelFactory.createOntologyModel(OntModelSpec.RDFS_MEM);
		o.read(uriString,"RDF/XML");
		
		init(o,isInput);
		
		//Close the OntModel
		o.close();
	}


//Public Methods

	/**
	 * Erases the Ontology data structures
	 */
	public void close()
	{
		uri = null;
		uris = null;
		pr = null;
		lex = null;
		rels = null;
		refs = null;
		synProps = null;
	}
	
	/**
	 * @return the Lexicon of the Ontology
	 */
	public Lexicon getLexicon()
	{
		return lex;
	}
	
	/**
	 * @param uri: the URI of the term/property to get the index
	 * @return the index of the term/property with the given URI
	 */
	public int getIndex(String uri)
	{
		return uris.indexOf(uri);
	}
	
	/**
	 * @param index: the index of the term/property to get the URI
	 * @return the localName of the term/property with the given index
	 */
	public String getLocalName(int index)
	{
		return names.get(index);
	}
	
	/**
	 * @return the list of term/property localNames of the Ontology
	 */
	public Vector<String> getLocalNames()
	{
		return names;
	}
	
	/**
	 * @param index: the index of the term/property to get the name
	 * @return the primary name of the term/property with the given index
	 */
	public String getName(int index)
	{
		if(index < separator)
			return getLexicon().getBestName(index);
		else if(index < uris.size())
			return getPropertyList().getName(index);
		return "";
	}

	/**
	 * @return the list of properties of the Ontology
	 */
	public PropertyList getPropertyList()
	{
		return pr;
	}
	
	/**
	 * @return the ReferenceMap of the Ontology
	 */
	public ReferenceMap getReferenceMap()
	{
		return refs;
	}
	
	/**
	 * @return the RelationshipMap of the Ontology
	 */
	public RelationshipMap getRelationshipMap()
	{
		return rels;
	}
	
	/**
	 * @return the list of term/property URIs of the Ontology
	 */
	public Vector<String> getURIs()
	{
		return uris;
	}
	
	/**
	 * @param index: the index of the term/property to get the URI
	 * @return the URI of the term/property with the given index
	 */
	public String getURI(int index)
	{
		return uris.get(index);
	}
	
	/**
	 * @return the URI of the Ontology
	 */
	public String getURI()
	{
		return uri;
	}
	
	/**
	 * @param u: the URI of the term/property to check
	 * @return whether the URI is local to this Ontology 
	 */
	public boolean isLocal(String u)
	{
		return u.startsWith(localPrefix);
	}
	
	/**
	 * @param index: the index of the uri in the ontology
	 * @return whether the index corresponds to a property (true)
	 * or a term (false)
	 */
	public boolean isProperty(int index)
	{
		return index >= separator;
	}

	/**
	 * @return the number of properties in the Ontology
	 */
	public int propertyCount()
	{
		return uris.size()-separator;
	}

	/**
	 * @return the number of terms in the Ontology
	 */
	public int termCount()
	{
		return separator;
	}

//Private Methods	

	public void init(OntModel o, boolean isInput)
	{
		//Initialize the data structures
		uris = new Vector<String>(0,1);
		names = new Vector<String>(0,1);
		pr = new PropertyList();
		lex = new Lexicon();
		rels = new RelationshipMap();
		refs = null;
		synProps = new HashMap<OntProperty,String>();
		
		//Get the local namespace
		uri = o.listOntologies().next().getURI();
		String obo = "http://purl.obolibrary.org/obo/";
		//If the ontology has an OBO prefix in its URI
		if(uri.startsWith(obo))
			//The local prefix is the OBO prefix (since the guys at OBO like doing things differently)
			localPrefix = obo;
		//Otherwise, the localPrefix is the uri (OWL default)
		else
			localPrefix = uri;
		
		//List the term and property uris and local names
		getTerms(o);
		separator = uris.size();
		getProperties(o);
		//Build the Lexicon
		getLabels(o);
		getSynonyms(o);
		//Extend it
		lex.generateStopWordSynonyms();
		lex.generateBracketSynonyms();
		//Build the relationship map
		if(isInput)
		{
			getRelationships(o);
			getDisjoint(o);
			getIntersections(o);
			rels.transitiveClosure();
		}
		//Or the reference map
		else if(hasReferences(o))
		{
			refs = new ReferenceMap();
			getReferences(o);
		}
	}
	
	//Gets the term uris from the OntModel (and indexes them)
	protected void getTerms(OntModel o)
	{
		//Get an iterator over the ontology classes
		ExtendedIterator<OntClass> classes = o.listClasses();
		//Then get the URI for each class
		while(classes.hasNext())
		{
			OntClass term = classes.next();
			String termUri = term.getURI();
			//If the URI is local
			if(termUri == null)
				continue;
			if(localOnly && !termUri.startsWith(localPrefix))
				continue;
			//Add it to the list of termURIs
			uris.add(termUri);
			//Get the local name from the URI
			int index = termUri.indexOf("#") + 1;
			if(index == 0)
				index = termUri.lastIndexOf("/") + 1;
			String name = termUri.substring(index);
			//Add it to the list of termNames
			names.add(name);
			//If the local name is not alphanumeric, add it to the lexicon
			if(!StringParser.isNumericId(name))
			{
				index = uris.size() - 1;
				String type = "localName";
				double weight = pr.getWeight(type);
				lex.add(index, name, type, weight);
			}
		}
	}
	
	//Gets the properties from the OntModel
	protected void getProperties(OntModel o)
	{
		//Get an iterator over the ontology properties
		ExtendedIterator<OntProperty> p = o.listAllOntProperties();
		//For each property
    	while(p.hasNext())
    	{
    		OntProperty op = p.next();
    		//Get its URI
    		String propUri = op.getURI();
    		if(propUri == null)
    			continue;
    		//And localName
    		String name = op.getLocalName();
    		//Convert it to a lexical type
    		String type = pr.getDefaultLexicalType(name);
    		//If it converts, it is a synonym property
    		if(!type.equals(""))
    			//So we add it to the synProps map
    			synProps.put(op, type);

			//If the URI isn't local proceed to next property
			if(!propUri.startsWith(localPrefix))
				continue;
			//Otherwise, add it to the uris and names lists
			uris.add(propUri);
			names.add(name);
			int index = uris.size() - 1;
			//Initialize the property
			Property prop = new Property(index,name);
			//Get its type(s)
			if(op.isAnnotationProperty())
				prop.addType("annotation");
			if(op.isDatatypeProperty())
				prop.addType("datatype");
			if(op.isObjectProperty())
				prop.addType("object");
			if(op.isFunctionalProperty())
				prop.addType("functional");
			if(op.isInverseFunctionalProperty())
				prop.addType("inverse");
			//Get its domain(s)
			OntResource or = op.getDomain();
			if(or != null && or.isClass())
			{
				OntClass oc = op.getDomain().asClass();
				if(oc.isURIResource())
					prop.addDomain(oc.getURI());
				else if(oc.isUnionClass())
				{
					UnionClass un = oc.asUnionClass();
					ExtendedIterator<? extends OntClass> union = un.listOperands();
					while(union.hasNext())
					{
						OntClass uClass = (OntClass)union.next();
						prop.addDomain(uClass.getURI());
					}
				}
			}
			//And finally its range(s)
			or = op.getRange();
			if(or != null && or.isClass())
			{
				OntClass oc = op.getRange().asClass();
				if(oc.isURIResource())
					prop.addRange(oc.getURI());
				else if(oc.isEnumeratedClass())
				{
					EnumeratedClass en = oc.asEnumeratedClass();
					RDFList enList = en.getOneOf();
					ExtendedIterator<RDFNode> enumer = enList.iterator();
					while(enumer.hasNext())
					{
						RDFNode r = enumer.next();
						if(r.isLiteral())
							prop.addRange(r.asLiteral().getString());
						else if(r.isURIResource())
							prop.addRange(r.asResource().getURI());
					}
				}
			}
			pr.add(prop);
    	}
	}
	
	//Reads the term localNames & labels from the OntModel into the Lexicon
	protected void getLabels(OntModel o)
	{
		String type = "label";
		double weight = pr.getWeight(type);
		//For each term index
		for(int i = 0; i < separator; i++)
		{
			//Get the class from the OntModel through the term uri
			OntClass term = o.getOntClass(uris.get(i));
			//Then get an iterator over the labels
			ExtendedIterator<RDFNode> labs = term.listLabels(null);
			//And add each one to the labels lexicon
			while(labs.hasNext())
			{
				String name = labs.next().asLiteral().getString();
				lex.add(i,name,type,weight);
			}
		}
	}

	//Reads all term synonyms from the OntModel into the Lexicon
	protected void getSynonyms(OntModel o)
	{
		Set<OntProperty> ops = synProps.keySet();
		
    	//For each term index (from 'termURIs' list)
		for(int i = 0; i < separator; i++)
		{
			//Get the class from the OntModel through the term uri
			OntClass term = o.getOntClass(uris.get(i));
			//For each synonym property
			for(OntProperty prop : ops)
			{
				//Get an iterator over the instances of that property for the term
				StmtIterator syns = term.listProperties(prop);
				//For each such instance, process the statement
				while(syns.hasNext())
				{
					Statement s = syns.nextStatement();
					RDFNode syn = s.getObject();
					String synName;
					//If the synonym property was an annotation property, the statement
					//points to a literal object which corresponds to the synonym
					if(syn.isLiteral())
					{
						Literal synLiteral = syn.asLiteral();
						synName = synLiteral.getString();
					}
					//If the synonym property was a datatype property, the statement points
					//to an individual (URI) in which case the synonym should be its label
					else if(syn.canAs(Individual.class))
					{
						Individual synIndividual = syn.as(Individual.class);
						synName = synIndividual.getLabel(null);
					}
					//Otherwise, we don't know how to read the synonym and skip it
					else
						continue;
					//Otherwise, add it to the lexicon
					String type = synProps.get(prop);
					lex.add(i,synName,type,pr.getWeight(type));
				}
			}
		}
	}
	
	//Reads all 'is_a' & 'part_of' relationships from the OntModel
	//into the RelationshipMap
	protected void getRelationships(OntModel o)
	{
		//For each term index (from 'termURIs' list)
		for(int i = 0; i < separator; i++)
		{
			try
			{
				//Get the class from the OntModel through the term uri
				OntClass term = o.getOntClass(uris.get(i));
				//Then get an iterator over the labels
				ExtendedIterator<OntClass> parents = term.listSuperClasses();
				//And add each one to the labels lexicon
				while(parents.hasNext())
				{
					term = parents.next();
					if(term.isRestriction())
					{
						Restriction r = term.asRestriction();
						if(r.isSomeValuesFromRestriction())
						{
							String rName = "";
							try
							{
								rName = r.getOnProperty().getLocalName();
							}
							catch(Exception e)
							{
								//Do nothing
								System.out.println(e.getMessage());
							}
							if(rName.contains("part_of"))
							{
								String uri = r.asSomeValuesFromRestriction().getSomeValuesFrom().getURI();
								int index = uris.indexOf(uri);
								if(index > -1)
									rels.addPartOfEdge(i,index);
							}
						}
					}
					else
					{
						int index = uris.indexOf(term.getURI());
						if(index > -1)
							rels.addIsAEdge(i,index);
					}
				}
			}
			catch(Exception e)
			{
				System.out.println(e.getMessage());
				continue;
			}
		}
	}
	
	//Reads all disjoint clauses from the OntModel into the RelationshipMap
	protected void getDisjoint(OntModel o)
	{
		//For each term index (from 'termURIs' list)
		for(int i = 0; i < separator; i++)
		{
			//Get the class from the OntModel through the term uri
			OntClass term = o.getOntClass(uris.get(i));
			//Then get an iterator over the labels
			ExtendedIterator<OntClass> disjoint = term.listDisjointWith();
			//And add each one to the labels lexicon
			while(disjoint.hasNext())
			{
				term = disjoint.next();
				int index = uris.indexOf(term.getURI());
				if(index > -1)
					rels.addDisjoint(i,index);
			}
		}
	}
	
	//Reads all intersection-equivalent clauses from the OntModel into the RelationshipMap
	protected void getIntersections(OntModel o)
	{
		//For each term index (from 'terms' list)
		for(int i = 0; i < separator; i++)
		{
			try
			{
				//Get the class from the OntModel through the term uri
				OntClass term = o.getOntClass(uris.get(i));
				//Then get an iterator over the labels
				ExtendedIterator<OntClass> equiv = term.listEquivalentClasses();
				//For each equivalentClass
				while(equiv.hasNext())
				{
					
					OntClass equivTerm = equiv.next();
					//Check if it is an intersection
					if(!equivTerm.isIntersectionClass())
						continue;
					//If so, read all classes in the intersection
					IntersectionClass equivInter = equivTerm.asIntersectionClass();
					ExtendedIterator<? extends OntClass> inter = equivInter.listOperands();
					HashSet<Integer> interList = new HashSet<Integer>();
					boolean complete = true;
					//Each class in the intersection					
					while(inter.hasNext())
					{
						OntClass interClass = (OntClass)inter.next();
						//Should be either a restriction
						//(in which case we can't infer an is_a relationship)
						if(interClass.isRestriction())
						{
							complete = false;
							continue;
						}
						//Or a direct reference to a named class
						//(in which case we can, but only if the class is listed)
						int index = uris.indexOf(interClass.getURI());
						if(index > -1)
						{
							rels.addIsAEdge(i, index);
					    	interList.add(index);
						}
						else
						    complete = false;
					}
					//If all terms in the intersection are listed classes
					//then we can add the equivalence to the RelationshipMap
					if(complete)
						rels.addEquivalence(interList, i);
				}
			}
			catch(Exception e)
			{
				System.out.println(e.getMessage());
				continue;
			}
		}
	}
	
	protected void getReferences(OntModel o)
	{
		//Get the hasDbXRef property
		OntProperty p = o.getOntProperty("http://www.geneontology.org/formats/oboInOwl#hasDbXref");
		//For each term index (from 'termURIs' list)
		for(int i = 0; i < separator; i++)
		{
			//Get the class from the OntModel through the term uri
			OntClass term = o.getOntClass(uris.get(i));
			//Get an iterator over the instances of that property for the term
			StmtIterator syns = term.listProperties(p);
			//For each such instance, process the statement
			while(syns.hasNext())
			{
				Statement s = syns.nextStatement();
				RDFNode syn = s.getObject();
				String xRef;
				if(syn.isLiteral())
				{
					Literal synLiteral = syn.asLiteral();
					xRef = synLiteral.getString();
					if(!xRef.startsWith("http"))
						refs.add(i,xRef.replace(':','_'));
				}
			}
		}
	}
	
	//Checks if the ontology lists the hasDbXRef property
	protected boolean hasReferences(OntModel o)
	{
		//Get an iterator over the ontology properties
		OntProperty p = o.getOntProperty("http://www.geneontology.org/formats/oboInOwl#hasDbXref");
    	return p != null;
	}
}
