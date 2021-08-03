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
* Utility class with methods for reading Ontology Alignment files.            *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.lang.StringEscapeUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import aml.AML;
import aml.alignment.Alignment;
import aml.alignment.EDOALAlignment;
import aml.alignment.SimpleAlignment;
import aml.alignment.mapping.*;
import aml.alignment.rdf.*;

public class AlignmentReader
{
	//TODO: check parsing of literals in AttributeOccurrenceRestrictions
	
//Attributes
	
	//The Alignment variable
	@SuppressWarnings("rawtypes")
	private static Alignment a;
	private static boolean isEDOAL;
	
//Public Methods
	
	/**
	 * Reads an alignment file in either RDF (simple or EDOAL) or TSV (AML format)
	 * @param file: the path to the file to read
	 * @param active: whether the alignment is between the active ontologies
	 * @return the Alignment
	 * @throws Exception if unable to read the alignment file
	 */
	@SuppressWarnings("rawtypes")
	public static Alignment read(String file, boolean active) throws Exception
	{
		//Read the first few lines of the file (until a non-empty line is reached)
		//to figure out how to parse it
		BufferedReader inStream = new BufferedReader(new FileReader(file));
		String line = inStream.readLine().trim();
		while((line.isEmpty()))
			line = inStream.readLine().trim();
		inStream.close();
		//AgreementMakerLight TSV format
		if(line.equals("#AgreementMakerLight Alignment File"))
			a = readTSV(file, active);
		//RDF Alignment format
		else if(line.contains("<?xml"))
			a = readRDF(file, active);
		//Unknow format
		else
			throw new Exception("Unable to read: " + file + " - unknown alignment format!");
		return a;
	}

	/**
	 * Reads an RDF alignment file (simple or EDOAL)
	 * @param file: the path to the file to read
	 * @param active: whether the alignment is between the active ontologies
	 * @return the Alignment
	 * @throws Exception if unable to read the alignment file
	 */
	@SuppressWarnings("rawtypes")
	public static Alignment readRDF(String file, boolean active) throws DocumentException
	{
		//Open the Alignment file using SAXReader
		SAXReader reader = new SAXReader();
		File f = new File(file);
		Document doc = reader.read(f);
		readDoc(doc,active);
		return a;
	}
	
	/**
	 * Reads an RDF alignment from a URL (simple or EDOAL)
	 * @param url: the URL of the alignment to read
	 * @param active: whether the alignment is between the active ontologies
	 * @return the Alignment
	 * @throws Exception if unable to read the alignment file
	 */
	@SuppressWarnings("rawtypes")
	public static Alignment readRDF(URL url, boolean active) throws DocumentException
	{
		//Open the Alignment file using SAXReader
		SAXReader reader = new SAXReader();
		Document doc = reader.read(url);
		readDoc(doc,active);
		return a;
	}

	public static SimpleAlignment readTSV(String file, boolean active) throws Exception
	{
		SimpleAlignment a = new SimpleAlignment();
		BufferedReader inStream = new BufferedReader(new FileReader(file));
		//First line contains the reference to AML Alignment Format
		inStream.readLine();
		//Second line contains the source ontology
		String onto1 = inStream.readLine();
		//Third line contains the target ontology
		String onto2 = inStream.readLine();
		boolean reverse = false;
		if(active && onto1.equals(AML.getInstance().getTarget().getURI()) &&
				onto2.equals(AML.getInstance().getSource().getURI()))
			reverse = true;
		else if(active && !onto1.equals(AML.getInstance().getSource().getURI()) &&
				onto2.equals(AML.getInstance().getTarget().getURI()))
		{
			inStream.close();
			throw new Exception("Unable to read: " + file + " - ontology mismatch!");
		}	
		//Fourth line contains the headers
		inStream.readLine();
		//And from the fifth line forward we have mappings
		String line;
		while((line = inStream.readLine()) != null)
		{
			String[] col = line.split("\t");
			//First column contains the entity1 uri
			String sourceURI = col[0];
			//Third contains the entity2 uri
			String targetURI = col[2];
			//Fifth contains the similarity
			String measure = col[4];
			//Parse it, assuming 1 if a valid measure is not found
			double similarity = 1;
			if(measure != null)
			{
				try
				{
					similarity = Double.parseDouble(measure);
		            if(similarity < 0 || similarity > 1)
		            	similarity = 1;
				}
            	catch(Exception ex){/*Do nothing - use the default value*/};
            }
			//The sixth column contains the type of relation
			MappingRelation rel;
			if(col.length > 5)
				rel = MappingRelation.parseRelation(col[5]);
			//For compatibility with previous tsv format without listed relation
			else
				rel = MappingRelation.EQUIVALENCE;
			//The seventh column, if it exists, contains the status of the Mapping
			MappingStatus st;
			if(col.length > 6)
				st = MappingStatus.parseStatus(col[6]);
			//For compatibility with previous tsv format without listed relation
			else
				st = MappingStatus.UNKNOWN;
			if(reverse)
				a.add(targetURI, sourceURI, similarity, rel.inverse(), st);
			else
				a.add(sourceURI, targetURI, similarity, rel, st);
		}
		inStream.close();
		return a;
	}
	
//Private Methods
	
	private static void readDoc(Document doc, boolean active)
	{		
		//Read the root, then go to the "Alignment" element
		Element root = doc.getRootElement();
		Element align = root.element(RDFElement.ALIGNMENT_.toString());
		//Read the alignment level
		String level = align.elementText(RDFElement.LEVEL.toString());
		isEDOAL = level.equals(EDOALAlignment.LEVEL);
		//Initialize the Alignment
		if(active)
		{
			if(isEDOAL)
				a = new EDOALAlignment(AML.getInstance().getSource(),AML.getInstance().getTarget());
			else
				a = new SimpleAlignment(AML.getInstance().getSource(),AML.getInstance().getTarget());
		}
		else
		{
			if(isEDOAL)
				a = new EDOALAlignment();
			else
				a = new SimpleAlignment();			
		}
		//Try to read the ontologies
		parseOntology(align.element(RDFElement.ONTO1.toString()),true);
		parseOntology(align.element(RDFElement.ONTO2.toString()),false);
		
		//Get an iterator over the mappings
		Iterator<?> map = align.elementIterator(RDFElement.MAP.toString());
		while(map.hasNext())
		{
			//Get the "Cell" in each mapping
			Element cell = ((Element)map.next()).element(RDFElement.CELL_.toString());
			if(cell == null)
				continue;
			parseCell(cell,active);
		}
	}
	
	private static void parseOntology(Element e, boolean isSource)
	{
		if(e != null)
		{
			String uri;
			String location = null;
			String formalismName = null;
			String formalismURI = null;
			if(e.isTextOnly())
				uri = e.getText();
			else
			{
				Element ont = e.element(RDFElement.ONTOLOGY_.toString());
				if(ont == null)
					return;
				uri = ont.attributeValue(RDFElement.RDF_ABOUT.toString());
				Element loc = ont.element(RDFElement.LOCATION.toString());
				if(loc != null)
					location = loc.getText();
				Element form = ont.element(RDFElement.FORMALISM.toString());
				if(form != null)
				{
					form = form.element(RDFElement.FORMALISM_.toString());
					formalismName = form.attributeValue(RDFElement.NAME.toString());
					formalismURI = form.attributeValue(RDFElement.URI.toString());
				}
			}
			if(isSource)
			{
				a.setSourceURI(uri);
				a.setSourceLocation(location);
				a.setSourceFormalismName(formalismName);
				a.setSourceFormalismURI(formalismURI);
			}
			else
			{
				a.setTargetURI(uri);
				a.setTargetLocation(location);
				a.setTargetFormalismName(formalismName);
				a.setTargetFormalismURI(formalismURI);
			}
		}
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static void parseCell(Element e, boolean active)
	{
		//Declare the mapping
		Mapping m = null;
		//Get the entities
		Element entity1 = e.element(RDFElement.ENTITY1.toString());
		Element entity2 = e.element(RDFElement.ENTITY2.toString());
		//Get the similarity measure
		String measure = e.elementText(RDFElement.MEASURE.toString());
		//Parse it, assuming 1 if a valid measure is not found
		double similarity = 1;
		if(measure != null)
		{
			try{ similarity = Double.parseDouble(measure); }
        	catch(Exception ex){/*Do nothing - use the default value*/};
        }
        if(similarity < 0 || similarity > 1)
        	similarity = 1;
        //Get the relation
        String r = e.elementText(RDFElement.RULE_RELATION.toString());
        if(r == null)
        	r = "=";
        MappingRelation rel = MappingRelation.parseRelation(StringEscapeUtils.unescapeXml(r));
        //Get the status (NOTE: this an extension to the Alignment format used by AML)
        String s = e.elementText(RDFElement.STATUS.toString());
        if(s == null)
        	s = "?";
        MappingStatus st = MappingStatus.parseStatus(s);
		if(!isEDOAL)
		{
			String sourceURI = decode(entity1.attributeValue(RDFElement.RDF_RESOURCE.toString()));
			String targetURI = decode(entity2.attributeValue(RDFElement.RDF_RESOURCE.toString()));
			if(sourceURI == null || targetURI == null)
			{
				System.err.println("WARNING: Skipping mapping - missing alignment entity!\n" + e.asXML());
				return;
			}			
			if(!active || (AML.getInstance().getSource().contains(sourceURI) && AML.getInstance().getTarget().contains(targetURI)))
				m = new SimpleMapping(sourceURI, targetURI, similarity, rel);
			else if(AML.getInstance().getSource().contains(targetURI) && AML.getInstance().getTarget().contains(sourceURI))
				m = new SimpleMapping(targetURI, sourceURI, similarity, rel.inverse());
			else
			{
				System.err.println("WARNING: Skipping mapping - alignment entity not found in input ontology!\n" + e.asXML());
				return;
			}					
		}
		else
		{
			AbstractExpression source = null;
			AbstractExpression target = null;
			//Check if we have a simple entity1 (even though this is an EDOAL alignment)
			List<Element> list = entity1.elements();
			if(list.isEmpty())
			{
				//If so, treat it as an id expression of the appropriate type
				String sourceURI = decode(entity1.attributeValue(RDFElement.RDF_RESOURCE.toString()));
				if(AML.getInstance().getEntityMap().isClass(sourceURI))
					source = new ClassId(sourceURI);
				else if(AML.getInstance().getEntityMap().isObjectProperty(sourceURI))
					source = new RelationId(sourceURI);
				else if(AML.getInstance().getEntityMap().isDataProperty(sourceURI))
					source = new PropertyId(sourceURI,null);
				else if(AML.getInstance().getEntityMap().isIndividual(sourceURI))
					source = new IndividualId(sourceURI);
			}
			//If not, parse it normally
			//(An alignment entity must have a single element,
			//although it may have several sub-elements)
			else if(list.size() == 1)
				source = parseEDOALEntity(list.get(0));
			if(source == null)
			{
				System.err.println("WARNING: Skipping mapping - unable to parse entity1!\n" + entity1.asXML());
				return;
			}
			//Repeat for entity2
			list = entity2.elements();
			if(list.isEmpty())
			{
				String targetURI = decode(entity2.attributeValue(RDFElement.RDF_RESOURCE.toString()));
				if(AML.getInstance().getEntityMap().isClass(targetURI))
					target = new ClassId(targetURI);
				else if(AML.getInstance().getEntityMap().isObjectProperty(targetURI))
					target = new RelationId(targetURI);
				else if(AML.getInstance().getEntityMap().isDataProperty(targetURI))
					target = new PropertyId(targetURI,null);
				else if(AML.getInstance().getEntityMap().isIndividual(targetURI))
					target = new IndividualId(targetURI);
			}
			else if(list.size() == 1)
				target = parseEDOALEntity(list.get(0));
			if(target == null)
			{
				System.err.println("WARNING: Skipping mapping - unable to parse entity2!\n" + entity2.asXML());
				return;
			}
			//If the alignment is active, check if the entities can be found in the open ontologies
			//(and if they are in the right order or reversed)
			boolean reverse = false;
			if(active)
			{
				if(AML.getInstance().getSource().containsAll(source.getElements()) &&
						AML.getInstance().getTarget().containsAll(target.getElements()))
					reverse = false;
				else if(AML.getInstance().getSource().containsAll(target.getElements()) &&
						AML.getInstance().getTarget().containsAll(source.getElements()))
				{
					reverse = true;
					//If they are reversed, invert the elements
					AbstractExpression temp = source;
					source = target;
					target = temp;
					rel = rel.inverse();
					
				}
				else
				{
					System.err.println("WARNING: Skipping mapping - alignment entity not found in input ontology!\n" + e.asXML());
					return;					
				}
			}
			
			//Check for transformations (of which there can be any number) and linkkeys
			List<Element> transform = e.elements(RDFElement.TRANSFORMATION.toString());
			Element key = e.element(RDFElement.LINKKEY.toString());
			//If there are transformations, parse them
			if(transform != null && !transform.isEmpty())
			{
				Set<Transformation> t = new HashSet<Transformation>();
				for(Element f : transform)
				{
					Transformation tf = parseTransformation(f,reverse);
					if(tf != null)
						t.add(tf);
					else
					{
						System.err.println("WARNING: Skipping mapping - unable to parse transformation!\n" + f.asXML());
						return;
					}
				}
				//Check that the entities are class expressions
				if(!(source instanceof ClassExpression && target instanceof ClassExpression))
				{
					System.err.println("WARNING: Skipping mapping - entities must be class expressions in transformation mapping!\n" + e.asXML());
					return;
				}
				m = new TransformationMapping((ClassExpression)source,(ClassExpression)target,similarity,rel,t);
			}
			//Otherwise, if there are linkkeys, parse them (there shouldn't be both transformations and linkkeys)
			else if(key != null)
			{
				LinkKey l = parseLinkKey(key,active);
				//Check that the entities are class expressions
				if(!(source instanceof ClassExpression && target instanceof ClassExpression))
				{
					System.err.println("WARNING: Skipping mapping - entities must be class expressions in linkkey mapping!\n" + e.asXML());
					return;
				}
				m = new LinkKeyMapping((ClassExpression)source,(ClassExpression)target,similarity,rel,l);
			}				
			//Otherwise initialize the mapping as a normal EDOALMapping
			else
				m = new EDOALMapping(source,target,similarity,rel);
		}
		if(m != null)
		{
			m.setStatus(st);
			a.add(m);
		}
	}

	private static AbstractExpression parseEDOALEntity(Element e)
	{
		AbstractExpression a = null;
		//Parse it according to its name
		if(e.getName().equals(RDFElement.CLASS_.toString()))
			a = parseClass(e);
		else if(e.getName().equals(RDFElement.PROPERTY_.toString()))
			a = parseProperty(e);
		else if(e.getName().equals(RDFElement.RELATION_.toString()))
			a = parseRelation(e);
		else if(e.getName().equals(RDFElement.INSTANCE_.toString()))
			a = parseInstance(e);
		else if(e.getName().equals(RDFElement.ATTR_DOMAIN_REST_.toString()))
			a = parseADR(e);
		else if(e.getName().equals(RDFElement.ATTR_OCCURRENCE_REST_.toString()))
			a = parseAOR(e);
		else if(e.getName().equals(RDFElement.ATTR_TYPE_REST_.toString()))
			a = parseATR(e);
		else if(e.getName().equals(RDFElement.ATTR_VALUE_REST_.toString()))
			a = parseAVR(e);
		else if(e.getName().equals(RDFElement.PROPERTY_DOMAIN_REST_.toString()))
			a = parsePDR(e);
		else if(e.getName().equals(RDFElement.PROPERTY_TYPE_REST_.toString()))
			a = parsePTR(e);
		else if(e.getName().equals(RDFElement.PROPERTY_VALUE_REST_.toString()))
			a = parsePVR(e);
		else if(e.getName().equals(RDFElement.RELATION_CODOMAIN_REST_.toString()))
			a = parseRCR(e);
		else if(e.getName().equals(RDFElement.RELATION_DOMAIN_REST_.toString()))
			a = parseRDR(e);
		else if(e.getName().equals(RDFElement.APPLY_.toString()))
			a = parseApply(e);
		else if(e.getName().equals(RDFElement.AGGREGATE_.toString()))
			a = parseAggregate(e);
		return a;
	}
	
	@SuppressWarnings("unchecked")
	private static ClassExpression parseClass(Element e)
	{
		//<Class> nodes are either class ids (with no subnodes)
		List<Element> list = e.elements();
		if(list.isEmpty())
		{
			String uri = decode(e.attributeValue(RDFElement.RDF_ABOUT.toString()));
			if(uri != null)
				return new ClassId(uri);
		}
		//Or compositions (and, or, not)
		else if(list.size() == 1)
		{
			Element f = list.get(0);
			//Start by checking if it is a negation, which is simpler
			if(f.getName().equals(RDFElement.NOT.toString()))
			{
				List<Element> l = f.elements();
				if(l.size() != 1)
					return null;
				AbstractExpression a = parseEDOALEntity(l.get(0));
				if(a instanceof ClassExpression)
					return new ClassNegation((ClassExpression)a);
				else
					return null;
			}
			//Then intersection
			else if(f.getName().equals(RDFElement.AND.toString()))
			{
				List<Element> l = f.elements();
				if(l.isEmpty())
					return null;
				HashSet<ClassExpression> composition = new HashSet<ClassExpression>();
				for(Element g : l)
				{
					AbstractExpression a = parseEDOALEntity(g);
					if(a instanceof ClassExpression)
						composition.add((ClassExpression)a);
					else
						return null;
				}
				if(!composition.isEmpty())
					return new ClassIntersection(composition);
			}
			//Then union
			else if(f.getName().equals(RDFElement.OR.toString()))
			{
				List<Element> l = f.elements();
				if(l.isEmpty())
					return null;
				HashSet<ClassExpression> composition = new HashSet<ClassExpression>();
				for(Element g : l)
				{
					AbstractExpression a = parseEDOALEntity(g);
					if(a instanceof ClassExpression)
						composition.add((ClassExpression)a);
					else
						return null;
				}
				if(!composition.isEmpty())
					return new ClassUnion(composition);
			}
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	private static PropertyExpression parseProperty(Element e)
	{
		//<Property> nodes are either property ids (with no subnodes)
		List<Element> list = e.elements();
		if(list.isEmpty())
		{
			String uri = decode(e.attributeValue(RDFElement.RDF_ABOUT.toString()));
			String lang = e.attributeValue(RDFElement.LANG.toString());
			if(uri != null)
				return new PropertyId(uri,lang);
		}
		else if(list.size() == 1)
		{
			Element f = list.get(0);
			//Start by checking if it is a negation, which is simpler
			if(f.getName().equals(RDFElement.NOT.toString()))
			{
				List<Element> l = f.elements();
				if(l.size() != 1)
					return null;
				AbstractExpression a = parseEDOALEntity(l.get(0));
				if(a instanceof PropertyExpression)
					return new PropertyNegation((PropertyExpression)a);
			}
			//If it is not a negation, it should be a composition, intersection or union
			//Check for intersection first
			else if(f.getName().equals(RDFElement.AND.toString()))
			{
				List<Element> l = f.elements();
				if(l.isEmpty())
					return null;
				HashSet<PropertyExpression> composition = new HashSet<PropertyExpression>();
				for(Element g : l)
				{
					AbstractExpression a = parseEDOALEntity(g);
					if(a instanceof PropertyExpression)
						composition.add((PropertyExpression)a);
					else
						return null;
				}
				if(!composition.isEmpty())
					return new PropertyIntersection(composition);
			}
			//Then union
			else if(f.getName().equals(RDFElement.OR.toString()))
			{
				List<Element> l = f.elements();
				if(l.isEmpty())
					return null;
				HashSet<PropertyExpression> composition = new HashSet<PropertyExpression>();
				for(Element g : l)
				{
					AbstractExpression a = parseEDOALEntity(g);
					if(a instanceof PropertyExpression)
						composition.add((PropertyExpression)a);
					else
						return null;
				}
				if(!composition.isEmpty())
					return new PropertyUnion(composition);
			}
			//Then compose
			else if(f.getName().equals(RDFElement.COMPOSE.toString()))
			{
				List<Element> l = f.elements();
				if(l.size() < 2)
					return null;
				Vector<RelationExpression> composition = new Vector<RelationExpression>();
				for(int i = 0; i < l.size()-1; i++)
				{
					AbstractExpression a = parseEDOALEntity(l.get(i));
					if(a instanceof RelationExpression)
						composition.add((RelationExpression)a);
					else
						return null;
				}
				AbstractExpression end = parseEDOALEntity(l.get(l.size()-1));
				if(!composition.isEmpty() && end instanceof PropertyExpression)
					return new PropertyComposition(composition, (PropertyExpression)end);
			}
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	private static RelationExpression parseRelation(Element e)
	{
		//<Relation> nodes are either property ids (with no subnodes)
		List<Element> list = e.elements();
		if(list.isEmpty())
		{
			String uri = decode(e.attributeValue(RDFElement.RDF_ABOUT.toString()));
			if(uri != null)
				return new RelationId(uri);
		}
		//Or compositions (compose, and, or, not, inverse, reflexive, symmetric, transitive)
		else if(list.size() == 1)
		{
			Element f = list.get(0);
			//Start by checking the cases not involving a collection
			if(f.getName().equals(RDFElement.NOT.toString()))
			{
				List<Element> l = f.elements();
				if(l.size() != 1)
					return null;
				AbstractExpression a = parseEDOALEntity(l.get(0));
				if(a instanceof RelationExpression)
					return new RelationNegation((RelationExpression)a);
			}
			else if(f.getName().equals(RDFElement.INVERSE.toString()))
			{
				List<Element> l = f.elements();
				if(l.size() != 1)
					return null;
				AbstractExpression a = parseEDOALEntity(l.get(0));
				if(a instanceof RelationExpression)
					return new InverseRelation((RelationExpression)a);
			}
			else if(f.getName().equals(RDFElement.REFLEXIVE.toString()))
			{
				List<Element> l = f.elements();
				if(l.size() != 1)
					return null;
				AbstractExpression a = parseEDOALEntity(l.get(0));
				if(a instanceof RelationExpression)
					return new ReflexiveRelation((RelationExpression)a);
			}
			else if(f.getName().equals(RDFElement.SYMMETRIC.toString()))
			{
				List<Element> l = f.elements();
				if(l.size() != 1)
					return null;
				AbstractExpression a = parseEDOALEntity(l.get(0));
				if(a instanceof RelationExpression)
					return new SymmetricRelation((RelationExpression)a);
			}
			else if(f.getName().equals(RDFElement.TRANSITIVE.toString()))
			{
				List<Element> l = f.elements();
				if(l.size() != 1)
					return null;
				AbstractExpression a = parseEDOALEntity(l.get(0));
				if(a instanceof RelationExpression)
					return new TransitiveRelation((RelationExpression)a);
			}
			//If it is not a negation, it should be a composition, intersection or union
			//Check for intersection first
			else if(f.getName().equals(RDFElement.AND.toString()))
			{
				List<Element> l = f.elements();
				if(l.isEmpty())
					return null;
				HashSet<RelationExpression> composition = new HashSet<RelationExpression>();
				for(Element g : l)
				{
					AbstractExpression a = parseEDOALEntity(g);
					if(a instanceof RelationExpression)
						composition.add((RelationExpression)a);
					else
						return null;
				}
				if(!composition.isEmpty())
					return new RelationIntersection(composition);
			}
			else if(f.getName().equals(RDFElement.OR.toString()))
			{
				List<Element> l = f.elements();
				if(l.isEmpty())
					return null;
				HashSet<RelationExpression> composition = new HashSet<RelationExpression>();
				for(Element g : l)
				{
					AbstractExpression a = parseEDOALEntity(g);
					if(a instanceof RelationExpression)
						composition.add((RelationExpression)a);
					else
						return null;
				}
				if(!composition.isEmpty())
					return new RelationIntersection(composition);
			}
			else if(f.getName().equals(RDFElement.COMPOSE.toString()))
			{
				List<Element> l = f.elements();
				if(l.isEmpty())
					return null;
				HashSet<RelationExpression> composition = new HashSet<RelationExpression>();
				for(Element g : l)
				{
					AbstractExpression a = parseEDOALEntity(g);
					if(a instanceof RelationExpression)
						composition.add((RelationExpression)a);
					else
						return null;
				}
				if(!composition.isEmpty())
					return new RelationIntersection(composition);
			}
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	private static AttributeDomainRestriction parseADR(Element e)
	{
		//ADRs should have exactly 2 elements: an onAttribute statement and a class restriction,
		//each of which should consist of a single element
		List<Element> list = e.elements();
		if(list.size() == 2 && list.get(0).getName().equals(RDFElement.ON_ATTRIBUTE.toString()) &&
				list.get(0).elements().size() == 1 &&
				list.get(1).elements().size() == 1)
		{
			AbstractExpression a = parseEDOALEntity((Element)list.get(0).elements().get(0));
			AbstractExpression c = parseEDOALEntity((Element)list.get(1).elements().get(0));
			RestrictionElement r = RestrictionElement.parse(list.get(1).getName());
			if(a instanceof RelationExpression && c instanceof ClassExpression && r != null)
				return new AttributeDomainRestriction((RelationExpression)a,(ClassExpression)c, r);
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	private static AttributeOccurrenceRestriction parseAOR(Element e)
	{
		//AORs should have exactly 3 elements: an onAttribute statement (with 1 sub-element),
		//a comparator (with no sub-elements), and a value expression (with 1 sub-element or 1
		//value that must be a positive integer literal)
		List<Element> list = e.elements();
		if(list.size() == 3 && list.get(0).getName().equals(RDFElement.ON_ATTRIBUTE.toString()) &&
				list.get(0).elements().size() == 1 && 
				list.get(1).getName().equals(RDFElement.COMPARATOR.toString()) &&
				list.get(1).nodeCount() == 0 &&
				list.get(2).getName().equals(RDFElement.VALUE.toString()))
		{
			AbstractExpression a = parseEDOALEntity((Element)list.get(0).elements().get(0));
			Comparator c = parseComparator(list.get(1));
			ValueExpression v = parseValue(list.get(2));
			if(a instanceof AttributeExpression && c != null && v instanceof NonNegativeInteger)
				return new AttributeOccurrenceRestriction((AttributeExpression)a, c, (NonNegativeInteger)v);
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	private static AttributeTypeRestriction parseATR(Element e)
	{
		//ATRs should have exactly 2 elements: an onAttribute statement (with 1 sub-element),
		//and a type restriction (with exactly 1 datatype as sub-element)
		List<Element> list = e.elements();
		if(list.size() == 2 && list.get(0).getName().equals(RDFElement.ON_ATTRIBUTE.toString()) &&
				list.get(0).elements().size() == 1 && 
				list.get(1).getName().equals(RDFElement.DATATYPE.toString()) &&
				list.get(0).elements().size() == 1)
		{
			AbstractExpression a = parseEDOALEntity((Element)list.get(0).elements().get(0));
			Datatype d = parseDatatype((Element)list.get(1).elements().get(0));
			if(d != null && a instanceof PropertyExpression)
				return new AttributeTypeRestriction((PropertyExpression)a, d);
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	private static AttributeValueRestriction parseAVR(Element e)
	{
		//AVRs should have exactly 3 elements: an onAttribute statement (with 1 sub-element),
		//a comparator (with no sub-elements), and a value expression (with 1 sub-element or 1
		//value)
		List<Element> list = e.elements();
		if(list.size() == 3 && list.get(0).getName().equals(RDFElement.ON_ATTRIBUTE.toString()) &&
				list.get(0).elements().size() == 1 && 
				list.get(1).getName().equals(RDFElement.COMPARATOR.toString()) &&
				list.get(1).nodeCount() == 0 &&
				list.get(2).getName().equals(RDFElement.VALUE.toString()))
		{
			AbstractExpression a = parseEDOALEntity((Element)list.get(0).elements().get(0));
			Comparator c = parseComparator(list.get(1));
			ValueExpression v = parseValue(list.get(2));
			if(a instanceof AttributeExpression && c != null && v != null)
				return new AttributeValueRestriction((AttributeExpression)a, c, v);
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	private static PropertyDomainRestriction parsePDR(Element e)
	{
		//PDRs should have a single element that is a class restriction, consisting also of a single class expression
		List<Element> list = e.elements();
		if(list.size() == 1 && list.get(0).getName().equals(RDFElement.CLASS.toString()) &&
				list.get(0).elements().size() == 1)
		{
			AbstractExpression c = parseEDOALEntity((Element)list.get(0).elements().get(0));
			if(c instanceof ClassExpression)
				return new PropertyDomainRestriction((ClassExpression)c);
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	private static PropertyTypeRestriction parsePTR(Element e)
	{
		//PTRs should have a single element that is a datatype restriction, consisting of a datatype
		List<Element> list = e.elements();
		if(list.size() == 1 && list.get(0).getName().equals(RDFElement.DATATYPE.toString()) &&
				list.get(0).elements().size() == 1)
		{
			Datatype d = parseDatatype((Element)list.get(0).elements().get(0));
			if(d != null)
				return new PropertyTypeRestriction(d);
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	private static PropertyValueRestriction parsePVR(Element e)
	{
		//PVRs should have exactly 2 elements: a comparator (with no sub-elements),
		//and a value expression (with 1 sub-element or 1 value)
		List<Element> list = e.elements();
		if(list.size() == 2 && list.get(0).getName().equals(RDFElement.COMPARATOR.toString()) &&
				list.get(1).nodeCount() == 0 && list.get(1).getName().equals(RDFElement.VALUE.toString()))
		{
			Comparator c = parseComparator(list.get(0));
			ValueExpression v = parseValue(list.get(1));
			if(c != null && v != null)
				return new PropertyValueRestriction(c, v);
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	private static RelationCoDomainRestriction parseRCR(Element e)
	{
		//PDRs should have a single element that is a class restriction, consisting also of a single class expression
		List<Element> list = e.elements();
		if(list.size() == 1 && list.get(0).getName().equals(RDFElement.CLASS.toString()) &&
				list.get(0).elements().size() == 1)
		{
			AbstractExpression c = parseEDOALEntity((Element)list.get(0).elements().get(0));
			if(c instanceof ClassExpression)
				return new RelationCoDomainRestriction((ClassExpression)c);
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	private static RelationDomainRestriction parseRDR(Element e)
	{
		//PDRs should have a single element that is a class restriction, consisting also of a single class expression
		List<Element> list = e.elements();
		if(list.size() == 1 && list.get(0).getName().equals(RDFElement.CLASS.toString()) &&
				list.get(0).elements().size() == 1)
		{
			AbstractExpression c = parseEDOALEntity((Element)list.get(0).elements().get(0));
			if(c instanceof ClassExpression)
				return new RelationDomainRestriction((ClassExpression)c);
		}
		return null;
	}
	
	private static IndividualId parseInstance(Element e)
	{
		//Individual expressions currently encompass only individual ids
		if(e.nodeCount() == 0)
		{
			String uri = e.attributeValue(RDFElement.RDF_ABOUT.toString());
			if(uri != null)
				return new IndividualId(uri);
		}
		return null;
	}
	
	private static Comparator parseComparator(Element e)
	{
		if(e.nodeCount() == 0)
		{
			String uri = e.attributeValue(RDFElement.RDF_RESOURCE.toString());
			if(uri != null)
				return new Comparator(uri);
		}
		return null;
	}
	
	private static Datatype parseDatatype(Element e)
	{
		if(e.nodeCount() == 0)
		{
			String uri = e.attributeValue(RDFElement.RDF_ABOUT.toString());
			if(uri != null)
				return new Datatype(uri);
		}
		return null;
	}
	
	private static ValueExpression parseValue(Element e)
	{
		//If the value is provided as text, it must be a literal
		if(e.isTextOnly())
		{
			String value = e.getText();
			//Check if it is a NonNegativeInteger
			try
			{
				int v = Integer.parseInt(value);
				if(v > -1)
					return new NonNegativeInteger(v);
			}
			catch(NumberFormatException n){	/*Do nothing*/ }
			return new Literal(value, null, null);
		}
		//Otherwise
		else if(e.elements().size() == 1)
		{
			Element f = (Element)e.elements().get(0);
			//It may be a literal
			if(f.getName().equals(RDFElement.LITERAL_.toString()))
			{
				String value = f.attributeValue(RDFElement.STRING.toString());
				String type = f.attributeValue(RDFElement.TYPE.toString());
				String lang = f.attributeValue(RDFElement.LANG.toString());
				if(value == null)
					return null;
				if((type == null && lang == null) || type.contains("int"))
				{
					try
					{
						int v = Integer.parseInt(value);
						if(v > -1)
							return new NonNegativeInteger(v);
					}
					catch(NumberFormatException n){	/*Do nothing*/ }
				}
				return new Literal(value, type, lang);
			}
			//But also one of several expressions
			AbstractExpression a = parseEDOALEntity(f);
			if(a instanceof ValueExpression)
				return (ValueExpression)a;
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private static Apply parseApply(Element e)
	{
		//The operator should be an attribute of the Apply element itself
		String operator = e.attributeValue(RDFElement.OPERATOR.toString());
		//Apply should have a single sub-element "arguments" which is a collection of value expressions
		List<Element> list = e.elements();
		if(operator != null && list.size() == 1 && list.get(0).getName().equals(RDFElement.ARGUMENTS.toString()))
		{
			Vector<ValueExpression> attributes = new Vector<ValueExpression>();
			list = list.get(0).elements();
			for(Element f : list)
			{
				ValueExpression x = parseValue(f);
				if(x == null)
					return null;
				attributes.add(x);
			}
			if(!attributes.isEmpty())
				return new Apply(operator, attributes);
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	private static Aggregate parseAggregate(Element e)
	{
		//The operator should be an attribute of the Aggregate element itself
		String operator = e.attributeValue(RDFElement.OPERATOR.toString());
		//Aggregate should have a single sub-element "arguments" which is a collection of value expressions
		List<Element> list = e.elements();
		if(operator != null && list.size() == 1 && list.get(0).getName().equals(RDFElement.ARGUMENTS.toString()))
		{
			Vector<ValueExpression> attributes = new Vector<ValueExpression>();
			list = list.get(0).elements();
			for(Element f : list)
			{
				ValueExpression x = parseValue(f);
				if(x == null)
					return null;
				attributes.add(x);
			}
			if(!attributes.isEmpty())
				return new Aggregate(operator, attributes);
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	private static LinkKey parseLinkKey(Element key, boolean reverse)
	{
		//The first element under <linkkey> should be <Linkkey>
		Element e = key.element(RDFElement.LINKKEY_.toString());
		if(e == null)
			return null;
		//<Linkkey> may have a <lk:type> and should have one or two <binding>
		List<Element> list = e.elements();
		String type = null;
		HashMap<AttributeExpression,AttributeExpression> equals = new HashMap<AttributeExpression,AttributeExpression>();
		HashMap<AttributeExpression,AttributeExpression> intersects = new HashMap<AttributeExpression,AttributeExpression>();
		for(Element f : list)
		{
			if(f.getName().equals("lk:type"))
				type = f.getText();
			else if(f.getName().equals(RDFElement.BINDING.toString()))
			{
				List<Element> binding = f.elements();
				if(binding == null || binding.size() != 1)
					return null;
				String name = binding.get(0).getName();
				boolean eq = name.equals(RDFElement.EQUALS_.toString());
				if(!eq && !name.equals(RDFElement.INTERSECTS_.toString()))
					return null;
				if(binding.get(0).elements().size() != 2)
					return null;
				Element prop1 = binding.get(0).element(RDFElement.PROPERTY1.toString());
				Element prop2 = binding.get(0).element(RDFElement.PROPERTY2.toString());
				if(prop1 == null || prop2 == null ||
						prop1.elements().size() != 1 ||
						prop2.elements().size() != 1)
					return null;
				prop1 = (Element)prop1.elements().get(0);
				prop2 = (Element)prop2.elements().get(0);
				AttributeExpression p1 = null, p2 = null;
				if(prop1.getName().equals(RDFElement.RELATION_.toString()))
					p1 = parseRelation(prop1);
				else if(prop1.getName().equals(RDFElement.PROPERTY_.toString()))
					p1 = parseProperty(prop1);
				else
					return null;
				if(prop2.getName().equals(RDFElement.RELATION_.toString()))
					p2 = parseRelation(prop2);
				else if(prop2.getName().equals(RDFElement.PROPERTY_.toString()))
					p2 = parseProperty(prop2);
				else
					return null;
				if(eq)
					equals.put(p1, p2);
				else
					intersects.put(p1, p2);
			}
		}
		if(equals.size() + intersects.size() == 0)
			return null;
		else
			return new LinkKey(type, equals, intersects);
	}

	private static Transformation parseTransformation(Element e, boolean reverse)
	{
		//Set e to the <Transformation> element, as the entities will be listed therein 
		Element f = e.element(RDFElement.TRANSFORMATION_.toString());
		//Record the direction attribute
		String direction = f.attributeValue(RDFElement.DIRECTION.toString());
		//Parse the entities
		Element entity1 = f.element(RDFElement.ENTITY1.toString());
		Element entity2 = f.element(RDFElement.ENTITY2.toString());
		if(entity1 == null || entity2 == null)
			return null;
		AbstractExpression source, target;
		if(reverse)
		{
			source = parseEDOALEntity(entity2);
			target = parseEDOALEntity(entity1);
		}
		else
		{
			source = parseEDOALEntity(entity1);
			target = parseEDOALEntity(entity2);
		}
		//The entities in a transformation should value expressions and one of them should
		//be an apply or aggregate; all their elements must belong to the respective ontology
		if(!(source instanceof ValueExpression && target instanceof ValueExpression) ||
				!(source instanceof Apply || source instanceof Aggregate || 
				target instanceof Apply || target instanceof Aggregate) ||
				!AML.getInstance().getSource().containsAll(source.getElements()) ||
				AML.getInstance().getTarget().containsAll(target.getElements()))
			return null;
		return new Transformation(direction,(ValueExpression)source,(ValueExpression)target);
	}

	private static String decode(String uri)
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
		return newUri;
	}
}