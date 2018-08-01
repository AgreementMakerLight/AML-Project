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
* Utility class with methods for saving Ontology Alignments into files.       *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.alignment;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;

import aml.alignment.mapping.Mapping;
import aml.settings.Namespace;

public class AlignmentWriter
{
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
		outStream.println("<" + RDFElement.ALIGNMENT_ + ">");
		outStream.println("\t<" + RDFElement.XML + ">yes</" + RDFElement.XML +">");
		outStream.println("\t<" + RDFElement.LEVEL + ">0</" + RDFElement.LEVEL + ">");
		double card = a.cardinality();
		if(card < 1.02)
			outStream.println("\t<" + RDFElement.TYPE + ">11</" + RDFElement.TYPE + ">");
		else
			outStream.println("\t<" + RDFElement.TYPE + ">??</" + RDFElement.TYPE + ">");
		outStream.println("\t<" + RDFElement.ONTO1 + ">" + a.getSourceURI() + "</" + RDFElement.ONTO1 + ">");
		outStream.println("\t<" + RDFElement.ONTO2 + ">" + a.getTargetURI() + "</" + RDFElement.ONTO2 + ">");
		for(Mapping m : a)
			outStream.println(m.toRDF());
		outStream.println("</" + RDFElement.ALIGNMENT_ + ">");
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
