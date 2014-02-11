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
* Tests AgreementMakerLight in Eclipse, with manually configured options.     *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 31-01-2014                                                            *
******************************************************************************/
package aml;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import org.apache.log4j.PropertyConfigurator;
import org.dom4j.DocumentException;

import aml.match.Alignment;
import aml.match.OAEI2013Matcher;
import aml.ontology.Lexicon;
import aml.ontology.Ontology;

public class AMLTestEclipse
{

//Main Method
	
	public static void main(String[] args) throws IOException, DocumentException
	{
		//Configure log4j (writes to store/error.log)
		PropertyConfigurator.configure("log4j.properties");

		//Path to input ontology files (edit manually)
		String sourcePath = "store/3Top.rdf.xml";
		String targetPath = "store/foursquare.rdf.xml";
		//Path to reference alignment (edit manually, or leave blank for no evaluation)
		String referencePath = "";
		//Path to output alignment file (edit manually, or leave left blank to not save alignment)
		String alignPath = "";
		//The ontologies
		Ontology source = loadOntology(new File(sourcePath).toURI());
		Ontology target = loadOntology(new File(targetPath).toURI());
		//The OAEI2013 Matcher (edit manually to use other matcher(s))
		OAEI2013Matcher matcher = new OAEI2013Matcher(true,true,true);
		//The alignment
		Alignment a = matcher.match(source,target);
		//Evaluate the alignment
		if(!referencePath.equals(""))
			System.out.println(evaluate(a,referencePath));
		//And save it
		if(!alignPath.equals(""))
			a.save(alignPath);
	}
	
	private static Ontology loadOntology(URI u)
	{
		long startTime = System.currentTimeMillis()/1000;
		String uriString = u.toString();
		boolean isOWL = !uriString.endsWith(".rdfs");
		Ontology o = new Ontology(u,true,isOWL);
		Lexicon l = o.getLexicon();
		l.generateStopWordSynonyms();
		l.generateBracketSynonyms();
		long elapsedTime = System.currentTimeMillis()/1000 - startTime;
		System.out.println(o.getURI() + " loaded in " + elapsedTime + " seconds");
		System.out.println("Classes: " + o.termCount());		
		System.out.println("Properties: " + o.propertyCount());
		return o;
	}
	
	private static String evaluate(Alignment a, String referencePath)
	{
		Alignment ref = null;
		try
		{
			ref = new Alignment(a.getSource(), a.getTarget(), referencePath);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		int found = a.size();		
		int correct = a.evaluate(ref);
		int total = ref.size();

		double precision = 1.0*correct/found;
		String prc = Math.round(precision*1000)/10.0 + "%";
		double recall = 1.0*correct/total;
		String rec = Math.round(recall*1000)/10.0 + "%";
		double fmeasure = 2*precision*recall/(precision+recall);
		String fms = Math.round(fmeasure*1000)/10.0 + "%";
		
		return "Precision\tRecall\tF-measure\tFound\tCorrect\tReference\n" + prc +
			"\t" + rec + "\t" + fms + "\t" + found + "\t" + correct + "\t" + total;
	}
}