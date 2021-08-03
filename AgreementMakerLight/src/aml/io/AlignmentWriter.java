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
package aml.io;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;

import aml.alignment.Alignment;
import aml.alignment.SimpleAlignment;
import aml.alignment.mapping.Mapping;
import aml.alignment.mapping.SimpleMapping;

public class AlignmentWriter
{
	//TODO: check xmlns:edoal='http://ns.inria.org/edoal/1.0/' declaration, in geolink it showed xmlns:xsd='http://ns.inria.org/edoal/1.0/'>
	
	/**
	 * Saves an Alignment into a text file as a list of douples
	 * @param a: the Alignment to save
	 * @param file: the output file
	 */
	public static void saveDoubles(SimpleAlignment a, String file) throws FileNotFoundException
	{
		PrintWriter outStream = new PrintWriter(new FileOutputStream(file));
		for(Mapping<String> m : a)
			outStream.println("<" + m.getEntity1() + "> <" + m.getEntity2() + ">");
		outStream.close();
	}

	/**
	 * Saves the Alignment into an .rdf file in OAEI format
	 * @param a: the Alignment to save
	 * @param file: the output file
	 */
	@SuppressWarnings("rawtypes")
	public static void saveRDF(Alignment a, String file) throws FileNotFoundException
	{
		PrintWriter outStream = new PrintWriter(new FileOutputStream(file));
		String s = a.toRDF();
		String[] line = s.split("\n");
		int index = 0;
		for(String l : line)
		{
			if(l.startsWith("</"))
				index--;
			for(int i = 0; i < index; i++)
				outStream.print("\t");
			outStream.println(l);
			if(l.startsWith("<") && !l.startsWith("</") && !l.startsWith("<?") &&
					!l.endsWith("/>") && !l.contains("</"))
				index++;
		}
		outStream.close();
	}
	
	/**
	 * Saves the Alignment into a .tsv file in AML format
	 * @param a: the Alignment to save
	 * @param file: the output file
	 */
	public static void saveTSV(SimpleAlignment a, String file) throws FileNotFoundException
	{
		PrintWriter outStream = new PrintWriter(new FileOutputStream(file));
		outStream.println("#AgreementMakerLight Alignment File");
		outStream.println("#entity1 ontology:\t" + a.getSourceURI());
		outStream.println("#entity2 ontology:\t" + a.getTargetURI());
		outStream.println("entity1 URI\tSource Label\tTarget URI\tTarget Label\tSimilarity\tRelationship\tStatus");
		for(Mapping<String> m : a)
			outStream.println(((SimpleMapping)m).toTSV());
		outStream.close();
	}
}
