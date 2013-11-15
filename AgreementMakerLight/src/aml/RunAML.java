/******************************************************************************
* Copyright 2013-2013 LASIGE                                                  *
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
* Runs AgreementMakerLight with options input from the command line.          *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 22-10-2013                                                            *
******************************************************************************/
package aml;

import java.io.File;
import java.net.URI;

import org.apache.log4j.PropertyConfigurator;

import aml.match.Alignment;

public class RunAML
{

//Main Method
	
	/**
	 * Runs AgreementMakerLight in OAEI mode
	 * @param args:
	 * -s path_to_source_ontology
	 * -t path_to_target_ontology
	 * [-o ouput_path]
	 * [-r reference_path]
	 * [-bk] -> use all available background knowledge
	 * [-bu] -> use all background knowledge except UMLS
	 * [-r] -> repair final alignment
	 * [-i] -> simulated interactive selection (requires reference alignment)
	 */
	public static void main(String[] args)
	{
		try
		{
			//Configure log4j (writes to store/error.log)
			PropertyConfigurator.configure("log4j.properties");
		}
		catch(Exception e)
		{
			System.out.println("Warning: Could not configure log4j.properties");
			e.printStackTrace();
		}
		//Path to input ontology files
		String sourcePath = "";
		String targetPath = "";
		//Path to reference alignment (if left blank, alignment is not evaluated)
		String referencePath = "";
		//Path to output alignment file (if left blank, alignment is not saved)
		String alignPath = "";
		//AgreementMakerLight settings
		boolean background = false;
		boolean ignoreUMLS = false;
		boolean repair = false;
		boolean interactive = false;
		
		for(int i = 0; i < args.length; i++)
		{
			if(args[i].equalsIgnoreCase("-s"))
				sourcePath = args[++i];
			else if(args[i].equalsIgnoreCase("-t"))
				targetPath = args[++i];
			else if(args[i].equalsIgnoreCase("-o"))
				alignPath = args[++i];
			else if(args[i].equalsIgnoreCase("-r"))
				referencePath = args[++i];
			else if(args[i].equalsIgnoreCase("-bk"))
				background = true;
			else if(args[i].equalsIgnoreCase("-bu"))
			{
				background = true;
				ignoreUMLS = true;
			}
			else if(args[i].equalsIgnoreCase("-r"))
				repair = true;
			else if(args[i].equalsIgnoreCase("-i"))
				interactive = true;			
		}
		
		if(sourcePath.equals("") || targetPath.equals(""))
		{
			System.out.println("Error: You must specify a source ontology and a target ontology");
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

		URI source = s.toURI();
		URI target = t.toURI();
		
		AML aml = new AML(background,ignoreUMLS,repair);
		
		Alignment a;
		if(interactive)
			a = aml.match(source,target,referencePath);
		else
			a = aml.match(source,target);
		
		if(!referencePath.equals(""))
			System.out.println(aml.evaluate(referencePath));
		
		if(!alignPath.equals(""))
			a.save(alignPath);
	}
}