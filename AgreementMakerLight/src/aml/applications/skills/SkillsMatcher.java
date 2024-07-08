package aml.applications.skills;

import java.io.File;
import java.util.HashSet;
import aml.AML;
import aml.filter.DirectionalSelector;
import aml.match.Alignment;
import aml.match.InstanceNeighborSimilarityMatcher;
import aml.settings.EntityType;
import aml.settings.InstanceMatchingCategory;
import aml.settings.SelectionType;



public class SkillsMatcher
{
	public static void main(String[] args) throws Exception
	{
		//Setup the parameters
		//Path to input ontology files
		String sourcePath = "";
		String targetPath = "";
		//Path to output alignment file
		String outputPath = "";
		//Path to reference alignment file (for evaluation)
		String referencePath = "";
	
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
			else if((args[i].equalsIgnoreCase("-o") || args[i].equalsIgnoreCase("--output")) && i+1 < args.length)
				outputPath = args[++i];
			else if((args[i].equalsIgnoreCase("-r") || args[i].equalsIgnoreCase("--reference")) && i+1 < args.length)
				referencePath = args[++i];
		}
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
		if(outputPath.equals(""))
		{
			System.out.println("ERROR: You must specify an output alignment file");
			System.out.println("Use -h or --help for instructions on how to run AgreementMakerLight");
			System.exit(1);
		}
		//Open the ontologies
		AML aml = AML.getInstance();
		aml.openOntologies(sourcePath, targetPath);
		aml.matchClasses(false);
		aml.matchProperties(false);
		aml.matchIndividuals(true);
		aml.setInstanceMatchingCategory(InstanceMatchingCategory.SAME_ONTOLOGY);
		HashSet<String> sourcesToMatch = new HashSet<String>(1);
		sourcesToMatch.add("Task");
		aml.setSourceClassesToMatch(sourcesToMatch);
		System.out.println("Sources to match: " + aml.getSourceIndividualsToMatch().size());
		HashSet<String> targetsToMatch = new HashSet<String>(1);
		targetsToMatch.add("Person");
		aml.setTargetClassesToMatch(targetsToMatch);
		System.out.println("Targets to match: " + aml.getTargetIndividualsToMatch().size());
		
		InstanceNeighborSimilarityMatcher insm = new InstanceNeighborSimilarityMatcher();
		Alignment a = insm.match(EntityType.INDIVIDUAL, 0.1);
		aml.setAlignment(a);
		DirectionalSelector ds = new DirectionalSelector(0.1,false,SelectionType.PERMISSIVE);
		ds.filter();
		System.out.println(aml.getAlignment().size());
		aml.saveAlignmentRDF(outputPath);
		
		if(!referencePath.equals(""))
		{
			aml.openReferenceAlignment(referencePath);
			aml.evaluate();
			System.out.println(aml.getEvaluation());
		}
	}
	
	private static void printHelpMessage()
	{
		System.out.println(" ______________________________________________________________");
		System.out.println("/                                                              \\");
		System.out.println("|                 AML (AgreementMakerLight)                    |");
		System.out.println("|                       Skill Matcher                          |");
		System.out.println("|                Copyright 2013-2024 LASIGE                    |");
		System.out.println("|                                                              |");
		System.out.println("|                        PARAMETERS:                           |");
		System.out.println("|  -s (--source) 'path_to_source_ontology'                     |");
		System.out.println("|  -t (--target) 'path_to_target_ontology'                     |");
		System.out.println("|  -o (--output) 'path_to_ouput_alignment'                     |");
		System.out.println("|  -r (--reference)	'path_to_reference_alignment' (optional)   |");
		System.out.println("|                                                              |");
		System.out.println("\\______________________________________________________________/");
	}
}