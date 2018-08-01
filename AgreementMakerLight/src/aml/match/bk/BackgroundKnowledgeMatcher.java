/******************************************************************************
* Copyright 2013-2018 LASIGE                                                  *
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
package aml.match.bk;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Set;
import java.util.Vector;

import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import aml.AML;
import aml.alignment.SimpleAlignment;
import aml.match.Matcher;
import aml.match.PrimaryMatcher;
import aml.match.lexical.LexicalMatcher;
import aml.ontology.EntityType;
import aml.ontology.MediatorOntology;
import aml.ontology.Ontology;
import aml.ontology.io.OntologyParser;
import aml.ontology.lexicon.ExternalLexicon;
import aml.settings.SelectionType;
import aml.util.data.MapSorter;

public class BackgroundKnowledgeMatcher extends Matcher implements PrimaryMatcher
{
	
//Attributes

	protected static final String DESCRIPTION = "Matches classes by testing all available\n" +
											  "sources of background knowledge, and using\n" +
											  "those that have a significant mapping gain\n" +
											  "(with Cross-Reference Matcher, Mediating\n" +
											  "Matcher, and/or WordNet Matcher).";
	protected static final String NAME = "Background Knowledge Matcher";
	protected static final EntityType[] SUPPORT = {EntityType.CLASS};
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
	public SimpleAlignment match(Ontology o1, Ontology o2, EntityType e, double thresh)
	{
		if(!checkEntityType(e))
			return new SimpleAlignment(o1,o2);
		System.out.println("Running " + NAME);
		long time = System.currentTimeMillis()/1000;
		LexicalMatcher lm = new LexicalMatcher();
		//The baseline alignment
		SimpleAlignment base = lm.match(o1, o2, e,thresh);
		//The alignment to return
		//(note that if no background knowledge sources are selected
		//this matcher will return the baseline Lexical alignment)
		SimpleAlignment a = new SimpleAlignment(o1,o2);
		a.addAll(base);
		//The map of pre-selected lexical alignments and their gains
		HashMap<SimpleAlignment,Double> selected = new HashMap<SimpleAlignment,Double>();
		//Auxiliary variables
		SimpleAlignment temp;
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
					ExternalLexicon ml = new ExternalLexicon(path + s);
					MediatingMatcher mm = new MediatingMatcher(ml, (new File(path + s)).toURI().toString());
					temp = mm.match(o1,o2,e,thresh);
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
				temp = wn.match(o1,o2,e,thresh);
			}
			//Ontologies
			else
			{
				try
				{
					long ontoTime = System.currentTimeMillis()/1000;
					MediatorOntology mo = OntologyParser.parseMediator(path + s);
					ontoTime = System.currentTimeMillis()/1000 - time;
					System.out.println(mo.getURI() + " loaded in " + ontoTime + " seconds");
					MediatingXRefMatcher x = new MediatingXRefMatcher(mo);
					temp = x.match(o1,o2,e,thresh);
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
				selected.put(temp,gain);
		}
		System.out.println("Sorting and selecting background knowledge sources");
		//Get the set of background knowledge alignments sorted by gain
		Set<SimpleAlignment> orderedSelection = MapSorter.sortDescending(selected).keySet();
		//And reevaluate them
		for(SimpleAlignment s : orderedSelection)
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
}