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

import org.apache.commons.lang.StringEscapeUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import aml.AML;
import aml.alignment.mapping.Mapping;
import aml.alignment.mapping.MappingRelation;
import aml.alignment.mapping.MappingStatus;
import aml.settings.Namespace;

public class AlignmentReader
{
	/**
	 * Reads an alignment file in either RDF (simple or EDOAL) or TSV (AML format)
	 * @param file: the path to the file to read
	 * @param active: whether the alignment is between the active ontologies
	 * @return the Alignment
	 * @throws Exception if unable to read the alignment file
	 */
	public static Alignment read(String file, boolean active) throws Exception
	{
		Alignment a = null;
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
		boolean edoal = level.equals(EDOALAlignment.LEVEL);

		if(edoal)
		{
			return null;
		}
		else
		{
			//Try to read the ontologies
			String source = null;
			Element onto1 = align.element(RDFElement.ONTO1.toString());
			if(onto1 != null)
			{
				if(onto1.isTextOnly())
					source = onto1.getText();
				else
				{
					Element ont = onto1.element(RDFElement.ONTOLOGY_.toString());
					if(ont != null)
						source = ont.attributeValue(RDFElement.RDF_ABOUT.toString());
				}
			}
			if(source == null)
				source = "";
			String target = null;
			Element onto2 = align.element(RDFElement.ONTO1.toString());
			if(onto2 != null)
			{
				if(onto2.isTextOnly())
					target = onto2.getText();
				else
				{
					Element ont = onto2.element(RDFElement.ONTOLOGY_.toString());
					if(ont != null)
						target = ont.attributeValue(RDFElement.RDF_ABOUT.toString());
				}
			}
			if(target == null)
				target = "";
			SimpleAlignment a;
			if(!active)
				a = new SimpleAlignment(source,target);
			else
				a = new SimpleAlignment(AML.getInstance().getSource().getURI(),AML.getInstance().getTarget().getURI());
			
			//Get an iterator over the mappings
			Iterator<?> map = align.elementIterator(RDFElement.MAP.toString());
			while(map.hasNext())
			{
				//Get the "Cell" in each mapping
				Element e = ((Element)map.next()).element(RDFElement.CELL_.toString());
				if(e == null)
					continue;
				
				//Get the entities
				String sourceURI = e.element(RDFElement.ENTITY1.toString()).attributeValue(RDFElement.RDF_RESOURCE.toString());
				String targetURI = e.element(RDFElement.ENTITY2.toString()).attributeValue(RDFElement.RDF_RESOURCE.toString());
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
	            String r = e.elementText(RDFElement.MEASURE.toString());
	            if(r == null)
	            	r = "?";
	            MappingRelation rel = MappingRelation.parseRelation(StringEscapeUtils.unescapeXml(r));
	            //Get the status
	            String s = e.elementText(RDFElement.STATUS.toString());
	            if(s == null)
	            	s = "?";
	            MappingStatus st = MappingStatus.parseStatus(s);
	            if(!active || (AML.getInstance().getSource().contains(sourceURI) && AML.getInstance().getTarget().contains(targetURI)))
	            	a.add(sourceURI, targetURI, similarity, rel, st);
	            else if(AML.getInstance().getSource().contains(targetURI) && AML.getInstance().getTarget().contains(sourceURI))
					a.add(targetURI, sourceURI, similarity, rel, st);
			}
			return a;
		}
	}
	
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
}