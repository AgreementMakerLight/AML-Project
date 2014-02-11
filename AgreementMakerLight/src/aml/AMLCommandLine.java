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
* Runs the OAEI2013 matcher with options input from the command line.         *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 31-01-2014                                                            *
******************************************************************************/
package aml;

import java.io.File;
import java.net.URI;

import org.apache.log4j.PropertyConfigurator;

import aml.match.Alignment;
import aml.match.OAEI2013Matcher;
import aml.ontology.Lexicon;
import aml.ontology.Ontology;

public class AMLCommandLine
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

		URI src = s.toURI();
		URI tgt = t.toURI();
		Ontology source = loadOntology(src);
		Ontology target = loadOntology(tgt);

		OAEI2013Matcher aml;
		
		if(interactive)
			aml = new OAEI2013Matcher(background,ignoreUMLS,repair,referencePath);
		else
			aml = new OAEI2013Matcher(background,ignoreUMLS,repair);
		
		Alignment a = aml.match(source,target);
		
		if(!referencePath.equals(""))
			System.out.println(evaluate(a,referencePath));
		
		if(!alignPath.equals(""))
		{
			try{a.save(alignPath);}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
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