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
* Matches Ontologies by using their logical definitions.                      *
*                                                                             *
* @author Daniel Faria, Catarina Martins                                      *
******************************************************************************/
package aml.match;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import aml.AML;
import aml.ontology.Ontology;
import aml.settings.EntityType;
import aml.util.Table2Set;

public class LogicalDefMatcher implements PrimaryMatcher
{
	
//Attributes

	private static final String DESCRIPTION = "Matches entities that have matching logical\n" +
											  "definitions.";
	private static final String NAME = "Logical Definition Matcher";
	private static final EntityType[] SUPPORT = {EntityType.CLASS};
	private static final String DEF_FILE = "store/knowledge/logical_definitions.txt";
	private static final String UBERON = "store/knowledge/uberon.owl";
	private Table2Set<Integer,String> classDefs;
	private Table2Set<String,Integer> defClasses;
	private Table2Set<String,String> refs;
	//The weight used for matching
	private final double WEIGHT = 0.95;
	
//Constructors

	/**
	 * Constructs a LogicalDefMatcher
	 */
	public LogicalDefMatcher()
	{
		readLogicalDefs();
		if(classDefs.size() > 0)
			readUberonXRefs();
	}

//Public Methods

	@Override
	public String getDescription()
	{
		return DESCRIPTION;
	}

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public EntityType[] getSupportedEntityTypes()
	{
		return SUPPORT;
	}
	
	@Override
	public Alignment match(EntityType e, double thresh) throws UnsupportedEntityTypeException
	{
		checkEntityType(e);
		System.out.println("Running Logical Definition Matcher");
		long time = System.currentTimeMillis()/1000;
		Alignment maps = new Alignment();
		Set<Integer> sClasses = AML.getInstance().getSource().getEntities(e);
		for(Integer s : sClasses)
		{
			if(!classDefs.contains(s))
				continue;
			for(String d : classDefs.get(s))
			{
				String[] def = d.split("\\+");
				if(!refs.contains(def[0]))
					continue;
				Set<String> ref = refs.get(def[0]);
				for(String r : ref)
				{
					String d2 = r + "+" + def[1];
					if(!defClasses.contains(d2))
						continue;
					for(Integer t : defClasses.get(d2))
						maps.add(s,t,WEIGHT);
				}
			}
		}
		time = System.currentTimeMillis()/1000 - time;
		System.out.println("Finished in " + time + " seconds");
		return maps;
	}
	
//Private Methods
	
	private void checkEntityType(EntityType e) throws UnsupportedEntityTypeException
	{
		boolean check = false;
		for(EntityType t : SUPPORT)
		{
			if(t.equals(e))
			{
				check = true;
				break;
			}
		}
		if(!check)
			throw new UnsupportedEntityTypeException(e.toString());
	}
	
	private void readLogicalDefs()
	{
		System.out.println("Reading Logical Definitions File");
		classDefs = new Table2Set<Integer,String>();
		defClasses = new Table2Set<String,Integer>();
		AML aml = AML.getInstance();
		Ontology source = aml.getSource();
		Ontology target = aml.getTarget();
		try
		{
			BufferedReader in = new BufferedReader(new FileReader(DEF_FILE));
			String line;
			while((line = in.readLine()) != null)
			{
				String[] defs = line.split("=");
				String c = defs[0].replace(':','_');
				int id;
				if(source.getLocalNames().contains(c))
					id = source.getIndex(c);
				else if(target.getLocalNames().contains(c))
					id = target.getIndex(c);
				else
					continue;
				String d = defs[1].replace(':','_');
				classDefs.add(id, d);
				defClasses.add(d, id);
			}
			in.close();
		}
		catch(IOException e)
		{
			System.out.println("Could not read " + DEF_FILE);
			e.printStackTrace();
		}
		System.out.println("Read " + classDefs.size() + " definitions");
	}
	
	private void readUberonXRefs()
	{
		System.out.println("Reading UBERON cross-references");
        refs = new Table2Set<String,String>();
        //Increase the entity expansion limit to allow large ontologies
        System.setProperty("entityExpansionLimit", "1000000");
        //Get an Ontology Manager and Data Factory
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        File f = new File(UBERON);
        OWLOntology o;
		try
		{
			o = manager.loadOntologyFromOntologyDocument(f);
		}
		catch(OWLOntologyCreationException e)
		{
			System.out.println("Could not open " + UBERON);
			e.printStackTrace();
			return;
		}
		//Get an iterator over the ontology classes
		Set<OWLClass> owlClasses = o.getClassesInSignature(true);
		//Then get the URI for each class
		for(OWLClass c : owlClasses)
		{
			String classUri = c.getIRI().toString();
			if(classUri == null || classUri.endsWith("owl#Thing") || classUri.endsWith("owl:Thing"))
				continue;
			String name = classUri.substring(classUri.lastIndexOf('/')+1);
			Set<OWLAnnotation> annots = c.getAnnotations(o);
			for(OWLOntology ont : o.getImports())
				annots.addAll(c.getAnnotations(ont));
            for(OWLAnnotation annotation : annots)
            {
            	String propUri = annotation.getProperty().getIRI().toString();
            	if(propUri.endsWith("hasDbXref") && annotation.getValue() instanceof OWLLiteral)
            	{
            		OWLLiteral val = (OWLLiteral) annotation.getValue();
					String xRef = val.getLiteral();
					if(!xRef.startsWith("http"))
					{
						xRef = xRef.replace(':','_');
						refs.add(name,xRef);
						refs.add(xRef, name);
					}
            	}
	        }
		}
		//Close the OntModel
        manager.removeOntology(o);
        //Reset the entity expansion limit
        System.clearProperty("entityExpansionLimit");
		System.out.println("Read " + refs.size() + " cross-references");
	}
}