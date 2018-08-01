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
package aml.alignment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.lang.StringEscapeUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import aml.AML;
import aml.alignment.mapping.*;
import aml.alignment.rdf.*;
import aml.ontology.EntityType;
import aml.settings.Namespace;

public class AlignmentReader
{
	
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
		if(line.contains("#AgreementMakerLight"))
			a = parseTSV(file, active);
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
		return a;
	}

	@SuppressWarnings("rawtypes")
	public static Alignment parseTSV(String file, boolean active) throws Exception
	{
		SimpleAlignment a = new SimpleAlignment();
		BufferedReader inStream = new BufferedReader(new FileReader(file));
		//First line contains the reference to AML
		inStream.readLine();
		//Second line contains the entity1 ontology
		inStream.readLine();
		//Third line contains the entity2 ontology
		inStream.readLine();
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
			a.add(sourceURI, targetURI, similarity, rel, st);
		}
		inStream.close();
		return a;
	}
	
//Private Methods
	
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
		//If this is EDOAL, first check whether it is a transformation mapping
		//as that precedes the entities
		if(isEDOAL)
		{
			Element transform = e.element(RDFElement.TRANSFORMATION.toString());
			//If so, parse it independently
			if(transform != null)
			{
				parseTransformation(transform, active);
				return;
			}
		}
		//Otherwise, declare the mapping
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
        	r = "?";
        MappingRelation rel = MappingRelation.parseRelation(StringEscapeUtils.unescapeXml(r));
        //Get the status (NOTE: this an extension to the Alignment format used by AML)
        String s = e.elementText(RDFElement.STATUS.toString());
        if(s == null)
        	s = "?";
        MappingStatus st = MappingStatus.parseStatus(s);
		if(!isEDOAL)
		{
			String sourceURI = entity1.attributeValue(RDFElement.RDF_RESOURCE.toString());
			String targetURI = entity2.attributeValue(RDFElement.RDF_RESOURCE.toString());
			
			if(!active || (AML.getInstance().getSource().contains(sourceURI) && AML.getInstance().getTarget().contains(targetURI)))
				m = new SimpleMapping(sourceURI, targetURI, similarity, rel);
			else if(AML.getInstance().getSource().contains(targetURI) && AML.getInstance().getTarget().contains(sourceURI))
				m = new SimpleMapping(targetURI, sourceURI, similarity, rel);
			if(m != null)
			{
				m.setStatus(st);
				a.add(m);
			}	
		}
		else
		{
			AbstractExpression source = null;
			AbstractExpression target = null;
			//Check if we have a simple entity1 (even though this is an EDOAL alignment)
			if(entity1.nodeCount() == 0)
			{
				String sourceURI = entity1.attributeValue(RDFElement.RDF_RESOURCE.toString());
				Set<EntityType> t = AML.getInstance().getEntityMap().getTypes(sourceURI);
				if(t.isEmpty())
					return;
				if(t.contains(EntityType.CLASS))
					source = new ClassId(sourceURI);
				else if(t.contains(EntityType.OBJECT_PROP))
					source = new RelationId(sourceURI);
				else if(t.contains(EntityType.DATA_PROP))
					source = new PropertyId(sourceURI);
				else if(t.contains(EntityType.INDIVIDUAL))
					source = new IndividualId(sourceURI);
			}
			//If not, parse it normally
			else
				source = parseEDOALEntity(entity1);
			//Check if we have a simple entity2 (even though this is an EDOAL alignment)
			if(entity2.nodeCount() == 0)
			{
				String targetURI = entity2.attributeValue(RDFElement.RDF_RESOURCE.toString());
				Set<EntityType> t = AML.getInstance().getEntityMap().getTypes(targetURI);
				if(t.contains(EntityType.CLASS))
					target = new ClassId(targetURI);
				else if(t.contains(EntityType.OBJECT_PROP))
					target = new RelationId(targetURI);
				else if(t.contains(EntityType.DATA_PROP))
					target = new PropertyId(targetURI);
				else if(t.contains(EntityType.INDIVIDUAL))
					target = new IndividualId(targetURI);
			}
			//If not, parse it normally
			else
				target = parseEDOALEntity(entity2);
			//Check for linkkeys
			Element key = e.element(RDFElement.LINKKEY.toString());
			if(key != null)
			{
				LinkKey l = parseLinkKey(key);
				if(source instanceof ClassExpression && target instanceof ClassExpression)
				{
					if(!active || (AML.getInstance().getSource().containsAll(source.getElements()) &&
							AML.getInstance().getTarget().containsAll(target.getElements())))
						m = new LinkKeyMapping((ClassExpression)source,(ClassExpression)target,l);
					else if(AML.getInstance().getSource().containsAll(target.getElements()) &&
							AML.getInstance().getTarget().containsAll(source.getElements()))
						m = new LinkKeyMapping((ClassExpression)target,(ClassExpression)source,l);
					if(m != null)
					{
						m.setStatus(st);
						a.add(m);
					}
				}
				
				
			}
		}
	}

	private static void parseTransformation(Element transform, boolean active)
	{
		// TODO Auto-generated method stub
		
	}
	
	private static AbstractExpression parseEDOALEntity(Element e)
	{
		Element f = e.element(RDFElement.CLASS_.toString());
		if(f != null)
		{
			System.out.println("class");
			return null;
		}
		f = e.element(RDFElement.PROPERTY_.toString());
		if(f != null)
		{
			System.out.println("property");
			return null;
		}
		f = e.element(RDFElement.RELATION_.toString());
		if(f != null)
		{
			System.out.println("relation");
			return null;
		}
		return null;
	}
	
	private static LinkKey parseLinkKey(Element key)
	{
		// TODO Auto-generated method stub
		return null;
	}

}