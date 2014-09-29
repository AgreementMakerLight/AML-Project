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
* @date 29-09-2014                                                            *
* @version 2.1                                                                *
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
				if(args[i].equalsIgnoreCase("-s"))
					sourcePath = args[++i];
				else if(args[i].equalsIgnoreCase("-t"))
					targetPath = args[++i];
				else if(args[i].equalsIgnoreCase("-i"))
					inputPath = args[++i];
				else if(args[i].equalsIgnoreCase("-o"))
					outputPath = args[++i];
				else if(args[i].equalsIgnoreCase("-a"))
					mode = "auto";
				else if(args[i].equalsIgnoreCase("-m"))
				{
					if(mode.isEmpty())
						mode = "manual";
				}
				else if(args[i].equalsIgnoreCase("-r"))
				{
					if(mode.isEmpty())
						mode = "repair";
				}
			}
		
			//Check that the necessary arguments are given and that the files can be found
			if(mode.isEmpty())
			{
				System.out.println("ERROR: You must specify a mode for running AgreementMakerLight");
				System.out.println("See README.txt file for details");
				System.exit(0);
			}
			//Input ontologies are necessary in all modes
			if(sourcePath.equals("") || targetPath.equals(""))
			{
				System.out.println("ERROR: You must specify a source ontology and a target ontology");
				System.out.println("See README.txt file for details");
				System.exit(0);
			}
			else
			{
				File s = new File(sourcePath);
				if(!s.canRead())
				{
					System.out.println("ERROR: Source ontology file not found");
					System.exit(0);
				}
				File t = new File(targetPath);
				if(!t.canRead())
				{
					System.out.println("ERROR: Target ontology file not found");
					System.exit(0);
				}
			}
			//An input alignment is necessary in repair mode
			if(mode.equals("repair"))
			{
				if(inputPath.equals(""))
				{
					System.out.println("ERROR: You must specify an input alignment file in repair mode");
					System.out.println("See README.txt file for details");
					System.exit(0);
				}
				else
				{
		
					File r = new File(inputPath);
					if(!r.canRead())
					{
						System.out.println("Error: Input alignment file not found");
						System.exit(0);
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
				System.exit(0);
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
					System.exit(0);
				}
				aml.repair();
			}
			else
			{
				if(mode.equals("manual"))
				{
					readConfigFile();
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
						System.exit(0);
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
	
	private static void readConfigFile()
	{
		File conf = new File(CONFIG);
		if(!conf.canRead())
		{
			System.out.println("Warning: Config file not found");
			System.out.println("Matching will proceed with default configuration");
		}
		try
		{
			Vector<MatchStep> selection = new Vector<MatchStep>();
			BufferedReader in = new BufferedReader(new FileReader(conf));
			String line;
			while((line=in.readLine()) != null)
			{
				if(line.startsWith("#"))
					continue;
				String[] option = line.split("=");
				option[0] = option[0].trim();
				option[1] = option[1].trim();
				if(option[0].equals("use_translator"))
				{
					if(option[1].equalsIgnoreCase("true") ||
							(option[1].equalsIgnoreCase("auto") &&
							aml.getSelectedSteps().contains(MatchStep.TRANSLATE)))
						selection.add(MatchStep.TRANSLATE);
				}
				else if(option[0].equals("bk_sources"))
				{
					if(option[1].equalsIgnoreCase("none"))
						continue;
					selection.add(MatchStep.BK);
					if(!option[1].equalsIgnoreCase("all"))
					{
						Vector<String> sources = new Vector<String>();
						for(String s : option[1].split(","))
						{
							String source = s.trim();
							File bk = new File(BK_PATH + source);
							if(bk.canRead())
								sources.add(source);
						}							
						aml.setSelectedSources(sources);
					}
				}
				else if(option[0].equals("word_matcher"))
				{
					if(option[1].equalsIgnoreCase("none") ||
							(option[1].equalsIgnoreCase("auto") &&
							!aml.getSelectedSteps().contains(MatchStep.WORD)))
						continue;
					selection.add(MatchStep.WORD);
					if(!option[1].equalsIgnoreCase("auto"))
						aml.setWordMatchStrategy(WordMatchStrategy.parseStrategy(option[1]));
				}
				else if(option[0].equals("string_matcher"))
				{
					if(option[1].equalsIgnoreCase("none"))
						continue;
					selection.add(MatchStep.STRING);
					boolean primary = aml.primaryStringMatcher();
					if(option[1].equalsIgnoreCase("global"))
						primary = true;
					else if(option[1].equalsIgnoreCase("local"))
						primary = false;
					aml.setPrimaryStringMatcher(primary);
				}
				else if(option[0].equals("string_measure"))
				{
					StringSimMeasure sm = StringSimMeasure.parseMeasure(option[1]);
					aml.setStringSimMeasure(sm);
				}
				else if(option[0].equals("struct_matcher"))
				{
					if(option[1].equalsIgnoreCase("none") ||
							(option[1].equalsIgnoreCase("auto") &&
							!aml.getSelectedSteps().contains(MatchStep.STRUCT)))
						continue;
					selection.add(MatchStep.STRUCT);
					if(!option[1].equalsIgnoreCase("auto"))
						aml.setNeighborSimilarityStrategy(NeighborSimilarityStrategy.parseStrategy(option[1]));
				}
				else if(option[0].equals("match_properties"))
				{
					if(option[1].equalsIgnoreCase("true") ||
							(option[1].equalsIgnoreCase("auto") &&
							aml.getSelectedSteps().contains(MatchStep.PROPERTY)))
						selection.add(MatchStep.PROPERTY);
				}
				else if(option[0].equals("selection_type"))
				{
					if(option[1].equalsIgnoreCase("none"))
						continue;
					selection.add(MatchStep.PROPERTY);
					if(!option[1].equalsIgnoreCase("auto"))
						aml.setSelectionType(SelectionType.parseSelector(option[1]));
				}
				else if(option[0].equals("repair_alignment"))
				{
					if(option[1].equalsIgnoreCase("true"))
						selection.add(MatchStep.REPAIR);
				}
			}
			in.close();
		}
		catch(Exception e)
		{
			System.out.println("Error: Could not read config file");
			System.out.println(e.getMessage());
			System.out.println("Matching will proceed with default configuration");
			aml.defaultConfig();
		}
	}
}