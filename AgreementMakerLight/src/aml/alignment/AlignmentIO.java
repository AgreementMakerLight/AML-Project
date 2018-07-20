package aml.alignment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.Iterator;

import org.apache.commons.lang.StringEscapeUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import aml.AML;
import aml.settings.Namespace;

public class AlignmentIO
{

	public static Alignment parseRDF(String file, boolean active) throws DocumentException
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
		boolean edoal = level.contains("EDOAL");

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
			Alignment a;
			if(!active)
				a = new Alignment(source,target);
			else
				a = new Alignment(AML.getInstance().getSource().getURI(),AML.getInstance().getTarget().getURI());
			
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
	            if(active && AML.getInstance().getSource().contains(targetURI) && AML.getInstance().getTarget().contains(sourceURI))
					a.add(targetURI, sourceURI, similarity, rel, st);
	            else	
	            	a.add(sourceURI, targetURI, similarity, rel, st);
			}
			return a;
		}
	}
	
	public static Alignment parseTSV(String file) throws Exception
	{
		Alignment a = new Alignment();
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

	
	/**
	 * Saves an Alignment into a text file as a list of douples
	 * @param a: the Alignment to save
	 * @param file: the output file
	 */
	public static void saveDoubles(Alignment a, String file) throws FileNotFoundException
	{
		PrintWriter outStream = new PrintWriter(new FileOutputStream(file));
		for(Mapping m : a)
			outStream.println("<" + m.getEntity1() + "> <" + m.getEntity2() + ">");
		outStream.close();
	}

	/**
	 * Saves the Alignment into an .rdf file in OAEI format
	 * @param a: the Alignment to save
	 * @param file: the output file
	 */
	public static void saveRDF(Alignment a, String file) throws FileNotFoundException
	{
		PrintWriter outStream = new PrintWriter(new FileOutputStream(file));
		outStream.println("<?xml version='1.0' encoding='utf-8'?>");
		outStream.println("<rdf:RDF xmlns='" + Namespace.ALIGNMENT.uri + "'"); 
		outStream.println("\t xmlns:rdf='" + Namespace.RDF.uri + "' "); 
		outStream.println("\t xmlns:xsd='" + Namespace.XSD.uri + "' ");
		outStream.println("<Alignment>");
		outStream.println("\t<xml>yes</xml>");
		outStream.println("\t<level>0</level>");
		double card = a.cardinality();
		if(card < 1.02)
			outStream.println("\t<type>11</type>");
		else
			outStream.println("\t<type>??</type>");
		outStream.println("\t<onto1>" + a.getSourceURI() + "</onto1>");
		outStream.println("\t<onto2>" + a.getTargetURI() + "</onto2>");
		for(Mapping m : a)
			outStream.println(m.toRDF());
		outStream.println("</Alignment>");
		outStream.println("</rdf:RDF>");		
		outStream.close();
	}
	
	/**
	 * Saves the Alignment into a .tsv file in AML format
	 * @param a: the Alignment to save
	 * @param file: the output file
	 */
	public static void saveTSV(Alignment a, String file) throws FileNotFoundException
	{
		PrintWriter outStream = new PrintWriter(new FileOutputStream(file));
		outStream.println("#AgreementMakerLight Alignment File");
		outStream.println("#entity1 ontology:\t" + a.getSourceURI());
		outStream.println("#entity2 ontology:\t" + a.getTargetURI());
		outStream.println("entity1 URI\tSource Label\tTarget URI\tTarget Label\tSimilarity\tRelationship\tStatus");
		for(Mapping m : a)
			outStream.println(m.toString());
		outStream.close();
	}
}
