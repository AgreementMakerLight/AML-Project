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
* @date 23-06-2014                                                            *
* @version 2.0                                                                *
******************************************************************************/
package aml;

import aml.settings.MatchingAlgorithm;

public class AMLTestEclipse
{

//Main Method
	
	public static void main(String[] args) throws Exception
	{
		//Path to input ontology files (edit manually)
		String sourcePath = "store/anatomy/mouse.owl";
		String targetPath = "store/anatomy/human.owl";
		//Path to reference alignment (edit manually, or leave blank for no evaluation)
		String referencePath = "store/anatomy/reference.rdf";
		//Path to save output alignment (edit manually, or leave blank for no evaluation)
		String outputPath = "";
		
		
		AML aml = AML.getInstance();
		aml.openOntologies(sourcePath, targetPath);
		
		//Set the matching algorithm
		aml.setMatcher(MatchingAlgorithm.OAEI);
		
		aml.match();
		
		if(!referencePath.equals(""))
		{
			aml.openReferenceAlignment(referencePath);
			aml.evaluate();
			System.out.println(aml.getEvaluation());
		}
		if(!outputPath.equals(""))
			aml.saveAlignmentRDF(outputPath);
	}
}