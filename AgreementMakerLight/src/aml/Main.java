/******************************************************************************
* Copyright 2013-2016 LASIGE                                                  *
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
* Runs AgreementMakerLight in either GUI mode (if no command line arguments   *
* are given) or in CLI mode (otherwise).                                      *
* Example 1 - Use AML in automatic mach mode and save the output alignment:   *
* java AMLCommandLine -s store/anatomy/mouse.owl -t store/anatomy/human.owl   *
* -o store/anatomy/alignment.rdf -a                                           *
* Example 2 - Use AML in manual match mode and evaluate the alignment:        *
* java AMLCommandLine -s store/anatomy/mouse.owl -t store/anatomy/human.owl   *
* -i store/anatomy/reference.rdf -m                                           *
* Example 3 - Use AML in repair mode and save the repaired alignment:         *
* java AMLCommandLine -s store/anatomy/mouse.owl -t store/anatomy/human.owl   *
* -i store/anatomy/toRepair.rdf -r -o store/anatomy/repaired.rdf              *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Vector;

import aml.settings.MatchStep;
import aml.settings.NeighborSimilarityStrategy;
import aml.settings.SelectionType;
import aml.settings.StringSimMeasure;
import aml.settings.WordMatchStrategy;

public class Main
{
	
//Attributes
	
	//Path to the config file
	private static final String CONFIG = "store/config.ini";
	//Path to the background knowledge directory
	private static final String BK_PATH = "store/knowledge/";
	//Path where AML is running
	private static String dir;
	//The AML instance
	private static AML aml;
    
//Main Method
	
	/**
	 * Runs AgreementMakerLight in GUI or CLI mode
	 * depending on whether arguments are given
	 */
	public static void main(String[] args)
	{
		aml = AML.getInstance();
		dir = aml.getPath();
		//If no arguments are given, open the GUI
		if(args.length == 0)
		{
			aml.startGUI();
		}
		//Otherwise
		else
		{
			//Setup the parameters
			//Path to input ontology files
			String sourcePath = "";
			String targetPath = "";
			//Path to input alignment (for evaluation / repair)
			String inputPath = "";
			//Path to output alignment file (if left blank, alignment is not saved)
			String outputPath = "";
			//AgreementMakerLight settings
			String mode = "";
		
			//Read the arguments
			for(int i = 0; i < args.length; i++)
			{
				if(args[i].equalsIgnoreCase("-h") || args[i].equalsIgnoreCase("--help"))
				{
					printHelpMessage();
					System.exit(0);
				}
				else if((args[i].equalsIgnoreCase("-s") || args[i].equalsIgnoreCase("--source")) && i+1 < args.length)
					sourcePath = args[++i];
				else if((args[i].equalsIgnoreCase("-t") || args[i].equalsIgnoreCase("--target")) && i+1 < args.length)
					targetPath = args[++i];
				else if((args[i].equalsIgnoreCase("-i") || args[i].equalsIgnoreCase("--input")) && i+1 < args.length)
					inputPath = args[++i];
				else if((args[i].equalsIgnoreCase("-o") || args[i].equalsIgnoreCase("--output")) && i+1 < args.length)
					outputPath = args[++i];
				else if(args[i].equalsIgnoreCase("-a") || args[i].equalsIgnoreCase("--auto"))
					mode = "auto";
				else if(args[i].equalsIgnoreCase("-m") || args[i].equalsIgnoreCase("--manual"))
				{
					if(mode.isEmpty())
						mode = "manual";
					else
					{
						System.out.println("ERROR: You must specify a single mode for running AgreementMakerLight");
						System.out.println("Use -h or --help for instructions on how to run AgreementMakerLight");
						System.exit(1);						
					}
				}
				else if(args[i].equalsIgnoreCase("-r") || args[i].equalsIgnoreCase("--repair"))
				{
					if(mode.isEmpty())
						mode = "repair";
					else
					{
						System.out.println("ERROR: You must specify a single mode for running AgreementMakerLight");
						System.out.println("Use -h or --help for instructions on how to run AgreementMakerLight");
						System.exit(1);						
					}
				}
			}
			//Check that the necessary arguments are given and that the files can be found
			if(mode.isEmpty())
			{
				System.out.println("ERROR: You must specify a mode for running AgreementMakerLight");
				System.out.println("Use -h or --help for instructions on how to run AgreementMakerLight");
				System.exit(1);
			}
			//Input ontologies are necessary in all modes
			if(sourcePath.equals("") || targetPath.equals(""))
			{
				System.out.println("ERROR: You must specify a source ontology and a target ontology");
				System.out.println("Use -h or --help for instructions on how to run AgreementMakerLight");
				System.exit(1);
			}
			else
			{
				File s = new File(sourcePath);
				if(!s.canRead())
				{
					System.out.println("ERROR: Source ontology file not found");
					System.exit(1);
				}
				File t = new File(targetPath);
				if(!t.canRead())
				{
					System.out.println("ERROR: Target ontology file not found");
					System.exit(1);
				}
			}
			//An input alignment is necessary in repair mode
			if(mode.equals("repair"))
			{
				if(inputPath.equals(""))
				{
					System.out.println("ERROR: You must specify an input alignment file in repair mode");
					System.out.println("Use -h or --help for instructions on how to run AgreementMakerLight");
					System.exit(1);
				}
				else
				{
		
					File r = new File(inputPath);
					if(!r.canRead())
					{
						System.out.println("Error: Input alignment file not found");
						System.exit(1);
					}
				}
			}
			
			//Open the ontologies
			try
			{
				aml.openOntologies(sourcePath, targetPath);
			}
			catch(Exception e)
			{
				System.out.println("Error: Could not open ontologies");				
				e.printStackTrace();
				System.exit(1);
			}
			//Proceed according to the run mode
			if(mode.equals("repair"))
			{
				try
				{
					aml.openAlignment(inputPath);
				}
				catch(Exception e)
				{
					System.out.println("Error: Could not open input alignment");	
					e.printStackTrace();
					System.exit(1);
				}
				aml.repair();
			}
			else
			{
				if(mode.equals("manual"))
				{
					aml.readConfigFile();
					aml.matchManual();
				}
				else
					aml.matchAuto();
				
				if(!inputPath.equals(""))
				{
					try
					{
						aml.openReferenceAlignment(inputPath);
					}
					catch(Exception e)
					{
						System.out.println("Error: Could not open input alignment");	
						e.printStackTrace();
						System.exit(1);
					}
					aml.evaluate();
					System.out.println(aml.getEvaluation());
				}
			}
			if(!outputPath.equals(""))
			{
				try
				{
					aml.saveAlignmentRDF(outputPath);
				}
				catch(Exception e)
				{
					System.out.println("Error: Could not save alignment");	
					e.printStackTrace();
					System.exit(0);
				}
			}
		}
	}

//Private Methods
	
	private static void printHelpMessage()
	{
		System.out.println(" ______________________________________________________________");
		System.out.println("/                                                              \\");
		System.out.println("|                 AML (AgreementMakerLight)                    |");
		System.out.println("|                  Demo GUI / CLI version                      |");
		System.out.println("|                Copyright 2013-2015 LASIGE                    |");
		System.out.println("|                                                              |");
		System.out.println("|                      AML GUI USAGE:                          |");
		System.out.println("| Double-clicking the AgreementMakerLight.jar file or calling  |");
		System.out.println("| it without any arguments (java -jar AgreementMakerLight.jar) |");
		System.out.println("| will start the GUI.                                          |");
		System.out.println("|                                                              |");
		System.out.println("|                      AML CLI USAGE:                          |");
		System.out.println("| java -jar AgreementMakerLight.jar OPTIONS                    |");
		System.out.println("|                                                              |");
		System.out.println("| The options are:                                             |");
		System.out.println("|  -s (--source) 'path_to_source_ontology'                     |");
		System.out.println("|  -t (--target) 'path_to_target_ontology'                     |");
		System.out.println("|  -i (--input)	'path_to_input_alignment'                      |");
		System.out.println("|               (mandatory in repair mode, where it will be    |");
		System.out.println("|               the alignment to repair; optional in match     |");
		System.out.println("|               mode, where it will be used as the reference   |");
		System.out.println("|               alignment, to evaluate the match result)       |");
		System.out.println("|  -o (--output) 'path_to_ouput_alignment'                     |");
		System.out.println("|               (if you want to save the resulting alignment)  |");
		System.out.println("|  -a (--auto) -> automatic match mode                         |");
		System.out.println("|   OR                                                         |");
		System.out.println("|  -m (--manual) -> manual match mode (you can configure the   |");
		System.out.println("|                   matcher in the store/config.ini file)      |");
		System.out.println("|   OR                                                         |");
		System.out.println("|  -r (--repair) -> repair alignment mode                      |");
		System.out.println("|                                                              |");
		System.out.println("\\______________________________________________________________/");
	}
}