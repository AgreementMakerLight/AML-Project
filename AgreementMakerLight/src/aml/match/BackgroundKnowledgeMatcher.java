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
* Automatic background knowledge matcher which builds an alignment by         *
* combining the best available background knowledge sources.                  *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 12-08-2014                                                            *
* @version 2.0                                                                *
******************************************************************************/
package aml.match;

import java.io.File;
import java.util.HashMap;
import java.util.Set;
import java.util.Vector;

import aml.ontology.Ontology;
import aml.util.MapSorter;

public class BackgroundKnowledgeMatcher implements PrimaryMatcher
{
	
//Attributes

	//The path to the background knowledge sources
	private final String BK_PATH = "store/knowledge/";
	//The minimum gain threshold
	private final double GAIN_THRESH = 0.02;
	//Whether to do selection or not
	private boolean oneToOne;
	//The list of ontologies available as background knowledge
	private Vector<String> sources;
	
//Constructor
	
	public BackgroundKnowledgeMatcher(Vector<String> s, boolean one)
	{
		sources = s;
		oneToOne = one;
	}

//Public Methods
	
	@Override
	public Alignment match(double thresh)
	{
		LexicalMatcher lm = new LexicalMatcher();
		Alignment base = lm.match(thresh);
		return extendBaseline(base,thresh);
	}
	
//Private Methods
	
	private Alignment extendBaseline(Alignment base, double thresh)
	{
		//The extension alignment to return
		Alignment ext = new Alignment();
		//The map of pre-selected lexical alignments and their gains
		HashMap<Alignment,Double> selected = new HashMap<Alignment,Double>();
		//The baseline alignment (which will be extended during this method)
		Alignment tempBase = new Alignment(base);
		//Auxiliary variables
		Alignment temp;
		Double gain, refGain;
		//First go through the listed sources
		for(String s : sources)
		{
			//Lexicon files
			if(s.endsWith(".lexicon"))
			{
				MediatingMatcher mm = new MediatingMatcher(s);
				temp = mm.match(thresh);
				if(oneToOne)
					gain = temp.gainOneToOne(tempBase);
				else
					gain = temp.gain(tempBase);
				if(gain >= GAIN_THRESH)
					selected.put(new Alignment(temp),gain);
			}
			//WordNet
			else if(s.equals("WordNet"))
			{
				WordNetMatcher wn = new WordNetMatcher();
				temp = wn.match(thresh);
				if(oneToOne)
					gain = temp.gainOneToOne(tempBase);
				else
					gain = temp.gain(tempBase);
				if(gain > GAIN_THRESH)
					selected.put(new Alignment(temp),gain);
			}
			//Ontologies
			else
			{
				//Load the mediator ontology
				String path = BK_PATH + s;
				Ontology mediator = new Ontology(path,false);
				String refs = path.replace(".owl",".xrefs");
				File f = new File(refs);
				if(f.exists())
					mediator.getReferenceMap().extend(refs);
				
				//If there are cross-references, process them
				refGain = 0.0;
				if(mediator.getReferenceMap().size() > 0)
				{
					XRefMatcher x = new XRefMatcher(mediator);
					temp = x.match(thresh);
					if(oneToOne)
						refGain = temp.gainOneToOne(tempBase);
					else
						refGain = temp.gain(tempBase);
					//We always add cross-reference matches, regardless of gain
					ext.addAll(temp);
				}
				//Then do a cross-lexical match
				MediatingMatcher mm = new MediatingMatcher(mediator);
				temp = mm.match(thresh);
				if(oneToOne)
					gain = temp.gainOneToOne(tempBase);
				else
					gain = temp.gain(tempBase);
				//We select this alignment if the gain is above threshold
				//and at least double that of the cross-reference match
				if(gain >= GAIN_THRESH && gain >= 2 * refGain)
					selected.put(new Alignment(temp), gain);
				//And finally close the mediator ontology
				mediator.close();
			}
		}
		//Update the baseline by adding the cross-reference matches
		tempBase.addAll(ext);
		//Get the set of background knowledge alignments sorted by gain
		Set<Alignment> orderedSelection = MapSorter.sortDescending(selected).keySet();
		//And reevaluate them
		for(Alignment a : orderedSelection)
		{
			if(oneToOne)
				gain = a.gainOneToOne(tempBase);
			else
				gain = a.gain(tempBase);
			if(gain > GAIN_THRESH)
			{
				//Adding those selected to the extension alignment
				ext.addAll(a);
				//And updating the baseline accordingly
				tempBase.addAll(a);
			}
		}
		return ext;
	}
}