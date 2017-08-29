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
* Matching algorithm that tests all available background knowledge sources,   *
* using either the MediatingMatcher, the WordNetMatcher, or the XRefMatcher,  *
* as appropriate. It combines the alignment obtained with the suitable        *
* background knowledge sources with the direct Lexical alignment.             *
* NOTE: Running this matcher makes running the LexicalMatcher or any of the   *
* Matchers mentioned above redundant.                                         *      
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.match;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Set;
import java.util.Vector;

import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import aml.AML;
import aml.knowledge.MediatorLexicon;
import aml.knowledge.MediatorOntology;
import aml.settings.EntityType;
import aml.settings.SelectionType;
import aml.util.MapSorter;

public class BackgroundKnowledgeMatcher implements PrimaryMatcher
{
	
//Attributes

	private static final String DESCRIPTION = "Matches classes by testing all available\n" +
											  "sources of background knowledge, and using\n" +
											  "those that have a significant mapping gain\n" +
											  "(with Cross-Reference Matcher, Mediating\n" +
											  "Matcher, and/or WordNet Matcher).";
	private static final String NAME = "Background Knowledge Matcher";
	private static final EntityType[] SUPPORT = {EntityType.CLASS};
	//The path to the background knowledge sources
	private final String BK_PATH = "store/knowledge/";
	private String path;
	private AML aml;
	//The minimum gain threshold
	private final double GAIN_THRESH = 0.02;
	//Whether to use 1-to-1 gain or global gain
	private boolean oneToOne;
	//The list of ontologies available as background knowledge
	private Vector<String> sources;
	
//Constructor
	
	public BackgroundKnowledgeMatcher()
	{
		aml = AML.getInstance();
		path = aml.getPath() + BK_PATH;
		sources = aml.getSelectedBKSources();
		oneToOne = !aml.getSelectionType().equals(SelectionType.HYBRID);
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
		System.out.println("Running Background Knowledge Matcher");
		long time = System.currentTimeMillis()/1000;
		LexicalMatcher lm = new LexicalMatcher();
		//The baseline alignment
		Alignment base = lm.match(e,thresh);
		//The alignment to return
		//(note that if no background knowledge sources are selected
		//this matcher will return the baseline Lexical alignment)
		Alignment a = new Alignment(base);
		//The map of pre-selected lexical alignments and their gains
		HashMap<Alignment,Double> selected = new HashMap<Alignment,Double>();
		//Auxiliary variables
		Alignment temp;
		Double gain;
		
		//First go through the listed sources
		for(String s : sources)
		{
			System.out.println("Testing " + s);
			//Lexicon files
			if(s.endsWith(".lexicon"))
			{
				try
				{
					MediatorLexicon ml = new MediatorLexicon(path + s);
					MediatingMatcher mm = new MediatingMatcher(ml, (new File(path + s)).toURI().toString());
					temp = mm.match(e,thresh);
				}
				catch(IOException io)
				{
					System.out.println("Could not open lexicon file: " + path + s);
					io.printStackTrace();
					continue;
				}				
			}
			//WordNet
			else if(s.equals("WordNet"))
			{
				WordNetMatcher wn = new WordNetMatcher();
				temp = wn.match(e,thresh);
			}
			//Ontologies
			else
			{
				try
				{
					long ontoTime = System.currentTimeMillis()/1000;
					MediatorOntology mo = new MediatorOntology(path + s);
					ontoTime = System.currentTimeMillis()/1000 - time;
					System.out.println(mo.getURI() + " loaded in " + ontoTime + " seconds");
					MediatingXRefMatcher x = new MediatingXRefMatcher(mo);
					temp = x.match(e,thresh);
				}
				catch(OWLOntologyCreationException o)
				{
					System.out.println("WARNING: Could not open ontology " + s);
					o.printStackTrace();
					continue;
				}
			}
			
			if(oneToOne)
				gain = temp.gainOneToOne(base);
			else
				gain = temp.gain(base);
			if(gain >= GAIN_THRESH)
				selected.put(new Alignment(temp),gain);
		}
		System.out.println("Sorting and selecting background knowledge sources");
		//Get the set of background knowledge alignments sorted by gain
		Set<Alignment> orderedSelection = MapSorter.sortDescending(selected).keySet();
		//And reevaluate them
		for(Alignment s : orderedSelection)
		{
			if(oneToOne)
				gain = s.gainOneToOne(a);
			else
				gain = s.gain(a);
			if(gain >= GAIN_THRESH)
				a.addAll(s);
		}
		time = System.currentTimeMillis()/1000 - time;
		System.out.println("Finished in " + time + " seconds");
		return a;
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
}