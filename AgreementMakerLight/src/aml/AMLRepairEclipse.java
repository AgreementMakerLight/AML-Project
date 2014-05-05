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
* Tests AgreementMakerLight in Eclipse, in Alignment Repair mode.             *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 05-05-2014                                                            *
******************************************************************************/
package aml;

import java.io.File;
import java.net.URI;

import org.apache.log4j.PropertyConfigurator;

import aml.match.Alignment;
import aml.ontology.Ontology;
import aml.repair.Repairer;

public class AMLRepairEclipse
{

//Main Method
	
	public static void main(String[] args) throws Exception
	{
		//Configure log4j (writes to store/error.log)
		PropertyConfigurator.configure("log4j.properties");

		//Path to input ontology files (edit manually)
		String sourcePath = "store/anatomy/mouse.owl";
		String targetPath = "store/anatomy/human.owl";
		//Path to input alignment file (edit manually)
		String alignPath = "store/anatomy/reference.rdf";
		//Path to output repaired alignment (edit manually)
		String repairPath = "store/anatomy/repair.rdf";
		
		//Open the ontologies
		Ontology source = loadOntology(new File(sourcePath).toURI());
		Ontology target = loadOntology(new File(targetPath).toURI());
		//Open the input alignment
		Alignment a = new Alignment(source,target,alignPath);
		//Repair the alignment
		Repairer r = new Repairer();
		Alignment b = r.repair(a);
		//And save it
		b.saveRDF(repairPath);
	}
	
	private static Ontology loadOntology(URI u)
	{
		long startTime = System.currentTimeMillis()/1000;
		String uriString = u.toString();
		boolean isOWL = !uriString.endsWith(".rdfs");
		Ontology o = new Ontology(u,true,isOWL);
		long elapsedTime = System.currentTimeMillis()/1000 - startTime;
		System.out.println(o.getURI() + " loaded in " + elapsedTime + " seconds");
		System.out.println("Classes: " + o.termCount());		
		System.out.println("Properties: " + o.propertyCount());
		return o;
	}
}