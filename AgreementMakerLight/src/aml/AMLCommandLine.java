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
* Runs the OAEI2013 matcher with input options from the command line.         *
* Example 1 - Use AML in match mode and save the output alignment:            *
* java AMLCommandLine -s store/anatomy/mouse.owl -t store/anatomy/human.owl   *
* -o store/anatomy/alignment.rdf -m [-r -b -u]                                *
* Example 2 - Use AML in match mode and evaluate the alignment:               *
* java AMLCommandLine -s store/anatomy/mouse.owl -t store/anatomy/human.owl   *
* -i store/anatomy/reference.rdf -m [-r -b -u]                                *
* Example 3 - Use AML in repair mode and save the repaired alignment:         *
* java AMLCommandLine -s store/anatomy/mouse.owl -t store/anatomy/human.owl   *
* -i store/anatomy/toRepair.rdf -r -o store/anatomy/repaired.rdf              *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 23-06-2014                                                            *
* @version 2.0                                                                *
******************************************************************************/
package aml;

import java.io.File;

import aml.settings.MatchingAlgorithm;

public class AMLCommandLine
{
    
//Main Method
	
	/**
	 * Runs AgreementMakerLight in OAEI mode
	 * @param args:
	 * -s path_to_source_ontology
	 * -t path_to_target_ontology
	 * [-m 'match' mode and/or -r 'repair' mode]
	 * [-i input_alignment_path]
	 * [-o ouput_alignment_path]
	 * [-b] -> use background knowledge
	 * [-u] -> exclude UMLS
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception
	{
		//Path to input ontology files
		String sourcePath = "";
		String targetPath = "";
		//Path to input alignment (for evaluation / repair)
		String inputPath = "";
		//Path to output alignment file (if left blank, alignment is not saved)
		String outputPath = "";
		//AgreementMakerLight settings
		boolean match = false;
		boolean background = false;
		boolean ignoreUMLS = false;
		boolean repair = false;
		
		for(int i = 0; i < args.length; i++)
		{
			if(args[i].equalsIgnoreCase("-s"))
				sourcePath = args[++i];
			else if(args[i].equalsIgnoreCase("-t"))
				targetPath = args[++i];
			else if(args[i].equalsIgnoreCase("-m"))
				match = true;
			else if(args[i].equalsIgnoreCase("-r"))
				repair = true;
			else if(args[i].equalsIgnoreCase("-i"))
				inputPath = args[++i];
			else if(args[i].equalsIgnoreCase("-o"))
				outputPath = args[++i];
			else if(args[i].equalsIgnoreCase("-b"))
				background = true;
			else if(args[i].equalsIgnoreCase("-u"))
				ignoreUMLS = true;
		}
		
		if(sourcePath.equals("") || targetPath.equals(""))
		{
			System.out.println("Error: You must specify a source ontology and a target ontology");
			System.out.println("See README.txt file for details on how to run AgreementMakerLight");
			System.exit(0);
		}

		if(repair && !match && inputPath.equals(""))
		{
			System.out.println("Error: You must specify an input alignment file in repair mode");
			System.out.println("See README.txt file for details on how to run AgreementMakerLight");
			System.exit(0);
		}
		
		File s = new File(sourcePath);
		if(!s.exists())
		{
			System.out.println("Error: Source ontology file not found");
			System.exit(0);
		}

		File t = new File(targetPath);
		if(!t.exists())
		{
			System.out.println("Error: Target ontology file not found");
			System.exit(0);
		}

		AML aml = AML.getInstance();
		aml.openOntologies(sourcePath, targetPath);
		
		if(match)
		{
			aml.setMatcher(MatchingAlgorithm.OAEI);
			aml.setMatchOptions(background, ignoreUMLS, repair);
			aml.match();
			if(!inputPath.equals(""))
			{
				aml.openReferenceAlignment(inputPath);
				aml.evaluate();
				System.out.println(aml.getEvaluation());
			}
		}
		else if(repair)
		{
			aml.openAlignment(inputPath);
			aml.repair();
		}
		if(!outputPath.equals(""))
		{
			aml.saveAlignmentRDF(outputPath);
		}
	}
}