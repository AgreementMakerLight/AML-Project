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
* @date 23-06-2014                                                            *
* @version 2.0                                                                *
******************************************************************************/
package aml;

import aml.filter.RepairerOld;
import aml.match.Alignment;
import aml.match.LexicalMatcher;

public class AMLRepairEclipse
{

//Main Method
	
	public static void main(String[] args) throws Exception
	{
		//Path to input ontology files (edit manually)
		String sourcePath = "store/anatomy/mouse.owl";
		String targetPath = "store/anatomy/human.owl";
		//Path to input alignment file (edit manually)
		//String alignPath = "store/anatomy/reference.rdf";
		//Path to output repaired alignment (edit manually)
		String repairPath = "store/anatomy/repair.rdf";
		
		//Open the ontologies
		AML aml = AML.getInstance();
		aml.openOntologies(sourcePath, targetPath);
		//Open the input alignment
		LexicalMatcher lm = new LexicalMatcher();
		Alignment a = lm.match(0.6);
		//Repair the alignment
		RepairerOld r = new RepairerOld();
		Alignment b = r.repair(a);
		//And save it
		b.saveRDF(repairPath);
	}
}