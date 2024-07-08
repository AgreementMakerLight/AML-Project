package aml.applications.skills;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.HashSet;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.FileDocumentSource;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.MissingImportHandlingStrategy;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

import aml.util.Table2Set;



public class SkillsOntology
{
	private static OWLOntologyManager manager;
	private static OWLDataFactory factory;
	private static OWLOntologyLoaderConfiguration conf;
	private static final String NAMESPACE = "http://purl.org/nemo/competence#";
	private static final IRI PERSON = IRI.create("http://purl.org/nemo/competence#Person");
	private static final IRI SKILL_TYPE = IRI.create("http://purl.org/nemo/competence#SkillType");
	private static final IRI TASK = IRI.create("http://purl.org/nemo/competence#Task");
	private static final IRI SKILL = IRI.create("http://purl.org/nemo/competence#Skill");
	private static final IRI HAS_SKILL = IRI.create("http://purl.org/nemo/competence#hasSkill");
	private static final IRI REQUIRES_SKILL = IRI.create("http://purl.org/nemo/competence#requiresSkill");
	private static String file;
	private static String ontology;
	private static String out;
	private static boolean tasks;
	private static boolean groundTruth;
	private static Table2Set<String,String> keySkill;
	private static HashSet<String> skills;
	
	public static void main(String[] args) throws Exception
	{
		processArgs(args);
		readFile();
		if(tasks)
			createTaskOntology();
		else
			createAuthorOntology();
	}
	
	private static void processArgs(String[] args)
	{
		tasks = false;
		groundTruth = false;
		//Read the arguments
		for(int i = 0; i < args.length; i++)
		{
			if(args[i].equalsIgnoreCase("-h") || args[i].equalsIgnoreCase("--help"))
			{
				printHelpMessage();
				System.exit(0);
			}
			else if((args[i].equals("-O") || args[i].equalsIgnoreCase("--ontology")) && i+1 < args.length)
				ontology = args[++i];
			else if((args[i].equals("-i") || args[i].equalsIgnoreCase("--input")) && i+1 < args.length)
				file = args[++i];
			else if((args[i].equals("-o") || args[i].equalsIgnoreCase("--output")) && i+1 < args.length)
				out = args[++i];
			else if(args[i].equals("-t") || args[i].equalsIgnoreCase("--tasks"))
				tasks = true;
			else if(args[i].equals("-gt") || args[i].equalsIgnoreCase("--ground_truth"))
				groundTruth = true;
		}
		//Input ontologies are necessary in all modes
		if(ontology == null || file == null || out == null)
		{
			System.out.println("ERROR: You must specify a base ontology, an input file with instance data, and an output file name");
			System.out.println("Use -h or --help for instructions on how to run AgreementMakerLight");
			System.exit(1);
		}
	}

	private static void printHelpMessage()
	{
		System.out.println(" ______________________________________________________________");
		System.out.println("/                                                              \\");
		System.out.println("|                 AML (AgreementMakerLight)                    |");
		System.out.println("|                 Skills Ontology Populator                    |");
		System.out.println("|                Copyright 2013-2024 LASIGE                    |");
		System.out.println("|                                                              |");
		System.out.println("|                       PARAMETERS:                            |");
		System.out.println("|  -O (--ontology) 'path_to_ontology'                          |");
		System.out.println("|  -i (--input)	'path_to_input_file'                           |");
		System.out.println("|  -o (--output) 'path_to_ouput_populated_ontology'            |");
		System.out.println("|               (if you want to save the resulting alignment)  |");
		System.out.println("|  -t (--tasks) -> populate with tasks (otherwise authors)     |");
		System.out.println("|  -g (--ground_truth) -> use ground truth data (otherwise     |");
		System.out.println("|                   random forest data)                        |");
		System.out.println("\\______________________________________________________________/");
	}
	
	private static void readFile() throws Exception
	{
		System.out.println("Reading file: " + file);
		//Process the options
		String key = "Author";
		if(tasks)
			key = "prNumber";
		String suffix = "_x";
		if(groundTruth)
			suffix = "_y";
		
		//Set up the data structures
		keySkill = new Table2Set<String,String>();
		skills = new HashSet<String>();
		//Auxiliary data structure to read the header
		HashMap<Integer, String> header = new HashMap<Integer, String>(); //header legend
		int index = 0; //index of key column
		
		BufferedReader in = new BufferedReader(new FileReader(file));
		//Process the header
		String line = in.readLine();
		String[] h = line.split(",");
		for(int i = 0; i < h.length; i++)
		{
			//Check for key column
			if(h[i].equals(key))
				index = i;
			//Check for the desired suffix
			else if(h[i].contains(suffix))
			{
				//Parse the name
				String col = h[i].replaceAll(suffix, "").replaceAll(" ", "_");
				//Store the position and name in the header
				header.put(i, col);
				//Store the name in the skills
				skills.add(col);
			}
		}
		//Process the data
		while((line = in.readLine()) != null)
		{
			String[] cols = line.split(",");
			String k = cols[index].replaceAll("[^a-zA-Z0-9]", "_");
			for(Integer i : header.keySet())
				if(Double.parseDouble(cols[i]) > 0.0)
					keySkill.add(k, header.get(i));
		}
		in.close();
	}
	
	private static void createAuthorOntology() throws Exception	
	{	
		File ont = new File(ontology);
		FileDocumentSource src = new FileDocumentSource(ont);
		manager = OWLManager.createOWLOntologyManager();
		factory = manager.getOWLDataFactory();
		conf = new OWLOntologyLoaderConfiguration().setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT);
		OWLOntology o = manager.loadOntologyFromOntologyDocument(src,conf);
		OWLClass person = factory.getOWLClass(PERSON);
		OWLClass skillType = factory.getOWLClass(SKILL_TYPE);
		OWLClass skill = factory.getOWLClass(SKILL);
		OWLObjectProperty hasSkill = factory.getOWLObjectProperty(HAS_SKILL);
		for(String s : skills)
		{
			IRI newSkill = IRI.create(NAMESPACE + s);
			OWLNamedIndividual sI = factory.getOWLNamedIndividual(newSkill);
			OWLClassAssertionAxiom typeAxiom = factory.getOWLClassAssertionAxiom(skillType, sI); // class, instance
			manager.addAxiom(o, typeAxiom);
			OWLClass sC = factory.getOWLClass(newSkill);
			OWLSubClassOfAxiom subClassAxiom = factory.getOWLSubClassOfAxiom(sC, skill); // subclass, superclass
			manager.addAxiom(o,subClassAxiom);			
		}
		
		for(String author : keySkill.keySet())
		{
			OWLNamedIndividual a = factory.getOWLNamedIndividual(IRI.create(NAMESPACE + author));
			OWLClassAssertionAxiom typeAxiom = factory.getOWLClassAssertionAxiom(person, a);
			manager.addAxiom(o, typeAxiom);
			for(String s : keySkill.get(author))
			{
				OWLNamedIndividual i = factory.getOWLNamedIndividual(IRI.create(NAMESPACE + s));
				OWLObjectPropertyAssertionAxiom objAxiom = factory.getOWLObjectPropertyAssertionAxiom(hasSkill, a, i);
				manager.addAxiom(o,objAxiom);
			}
		}
		manager.saveOntology(o, IRI.create(new File(out)));
	}
	
	private static void createTaskOntology() throws Exception	
	{
		File ont = new File(ontology);
		FileDocumentSource src = new FileDocumentSource(ont);
		manager = OWLManager.createOWLOntologyManager();
		factory = manager.getOWLDataFactory();
		conf = new OWLOntologyLoaderConfiguration().setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT);
		OWLOntology o = manager.loadOntologyFromOntologyDocument(src,conf);
		OWLClass skillType = factory.getOWLClass(SKILL_TYPE);
		OWLClass task = factory.getOWLClass(TASK);
		OWLClass skill = factory.getOWLClass(SKILL);
		OWLObjectProperty requiresSkill = factory.getOWLObjectProperty(REQUIRES_SKILL);

		for(String s : skills)
		{
			IRI newSkill = IRI.create(NAMESPACE + s);
			OWLNamedIndividual sI = factory.getOWLNamedIndividual(newSkill);
			OWLClassAssertionAxiom typeAxiom = factory.getOWLClassAssertionAxiom(skillType, sI); // class, instance
			manager.addAxiom(o, typeAxiom);
			OWLClass sC = factory.getOWLClass(newSkill);
			OWLSubClassOfAxiom subClassAxiom = factory.getOWLSubClassOfAxiom(sC, skill); // subclass, superclass
			manager.addAxiom(o,subClassAxiom);			
		}

		for(String pr : keySkill.keySet())
		{
			OWLNamedIndividual t = factory.getOWLNamedIndividual(IRI.create(NAMESPACE + "task_" + pr));
			OWLClassAssertionAxiom typeAxiom = factory.getOWLClassAssertionAxiom(task, t);
			manager.addAxiom(o,typeAxiom);
			for(String sk : keySkill.get(pr))
			{
				OWLNamedIndividual s = factory.getOWLNamedIndividual(IRI.create(NAMESPACE + sk));
				OWLObjectPropertyAssertionAxiom objAxiom = factory.getOWLObjectPropertyAssertionAxiom(requiresSkill, t, s);
				manager.addAxiom(o,objAxiom);
			}
		}
		manager.saveOntology(o, IRI.create(new File(out)));
	}
}