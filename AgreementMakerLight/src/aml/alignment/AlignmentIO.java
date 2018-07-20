package aml.alignment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Vector;

import org.apache.commons.lang.StringEscapeUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

public class AlignmentIO
{

	public static Alignment parseRDF(String file) throws DocumentException
	{
		Alignment a = new Alignment();
		//Open the Alignment file using SAXReader
		SAXReader reader = new SAXReader();
		File f = new File(file);
		Document doc = reader.read(f);
		//Read the root, then go to the "Alignment" element
		Element root = doc.getRootElement();
		Element align = root.element(RDFElement.ALIGNMENT_.toString());
		
		//Read the alignment level
		String level = align.elementText(RDFElement.LEVEL.toString());
		boolean edoal = level.contains("EDOAL");

		//Try to read the ontologies
		String source, target; 
		Element onto1 = align.element(RDFElement.ONTO1.toString());
		if(onto1 != null)
			source = onto1.element(RDFElement.ONTOLOGY_.toString()).attributeValue(RDFElement.RDF_ABOUT.toString());
		Element onto2 = align.element(RDFElement.ONTO2.toString());
		if(onto2 != null)
			target = onto1.element(RDFElement.ONTOLOGY_.toString()).attributeValue(RDFElement.RDF_ABOUT.toString());
		
		//Get an iterator over the mappings
		Iterator<?> map = align.elementIterator(RDFElement.MAP.toString());
		while(map.hasNext())
		{
			//Get the "Cell" in each mapping
			Element e = ((Element)map.next()).element(RDFElement.CELL_.toString());
			if(e == null)
				continue;
			
			Vector<String> sources = new Vector<String>();
			Vector<String> targets = new Vector<String>();
			//SimpleMapping m = new SimpleMapping();
			//Get the entities
			Element entity1 = e.element(RDFElement.ENTITY1.toString());
			Element entity2 = e.element(RDFElement.ENTITY2.toString());
			//If there are no sub-nodes (non-edoal alignment), get the entities directly from the entity tag
			if(entity1.nodeCount() == 0 && entity2.nodeCount() == 0)
			{
				sources.add(entity1.attributeValue(RDFElement.RDF_RESOURCE.toString()));
				targets.add(entity2.attributeValue(RDFElement.RDF_RESOURCE.toString()));
			}
			else
			{
				System.out.println(entity1.asXML());
				System.out.println(entity2.asXML());
			}	
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
			//a.add(sourceURI, targetURI, similarity, rel, st);
		}
		return a;
	}
	
	private void loadMappingsTSV(String file) throws Exception
	{
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
			add(sourceURI, targetURI, similarity, rel, st);
		}
		inStream.close();
	}

	
	/**
	 * Saves the Alignment into a text file as a list of douples
	 * @param file: the output file
	 */
	public void saveDoubles(String file) throws FileNotFoundException
	{
		PrintWriter outStream = new PrintWriter(new FileOutputStream(file));
		for(Mapping m : maps)
			outStream.println("<" + m.getEntity1() + "> <" + m.getEntity2() + ">");
		outStream.close();
	}

	/**
	 * Saves the Alignment into an .rdf file in OAEI format
	 * @param file: the output file
	 */
	public void saveRDF(String file) throws FileNotFoundException
	{
		String sourceURI = aml.getSource().getURI();
		String targetURI = aml.getTarget().getURI();

		PrintWriter outStream = new PrintWriter(new FileOutputStream(file));
		outStream.println("<?xml version='1.0' encoding='utf-8'?>");
		outStream.println("<rdf:RDF xmlns='http://knowledgeweb.semanticweb.org/heterogeneity/alignment'"); 
		outStream.println("\t xmlns:rdf='http://www.w3.org/1999/02/22-rdf-syntax-ns#' "); 
		outStream.println("\t xmlns:xsd='http://www.w3.org/2001/XMLSchema#' ");
		outStream.println("\t alignmentSource='AgreementMakerLight'>\n");
		outStream.println("<Alignment>");
		outStream.println("\t<xml>yes</xml>");
		outStream.println("\t<level>0</level>");
		double card = cardinality();
		if(card < 1.02)
			outStream.println("\t<type>11</type>");
		else
			outStream.println("\t<type>??</type>");
		outStream.println("\t<onto1>" + sourceURI + "</onto1>");
		outStream.println("\t<onto2>" + targetURI + "</onto2>");
		outStream.println("\t<uri1>" + sourceURI + "</uri1>");
		outStream.println("\t<uri2>" + targetURI + "</uri2>");
		for(Mapping m : maps)
			outStream.println(m.toRDF());
		outStream.println("</Alignment>");
		outStream.println("</rdf:RDF>");		
		outStream.close();
	}
	
	/**
	 * Saves the Alignment into a .tsv file in AML format
	 * @param file: the output file
	 */
	public void saveTSV(String file) throws FileNotFoundException
	{
		PrintWriter outStream = new PrintWriter(new FileOutputStream(file));
		outStream.println("#AgreementMakerLight Alignment File");
		outStream.println("#entity1 ontology:\t" + entity1.getURI());
		outStream.println("#entity2 ontology:\t" + entity2.getURI());
		outStream.println("entity1 URI\tSource Label\tTarget URI\tTarget Label\tSimilarity\tRelationship\tStatus");
		for(Mapping m : maps)
			outStream.println(m.toString());
		outStream.close();
	}
}
