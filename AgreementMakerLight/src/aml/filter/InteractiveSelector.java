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
* Selector that uses (simulated) user interaction (through the Interaction    *
* Manager) to help perform alignment selection.                               *
*                                                                             *
* @author Daniel Faria, Aynaz Taheri                                          *
******************************************************************************/
package aml.filter;

import java.util.Vector;

import aml.AML;
import aml.match.Alignment;
import aml.match.BlockRematcher;
import aml.match.Mapping;
import aml.match.NeighborSimilarityMatcher;
import aml.match.Rematcher;
import aml.match.StringMatcher;
import aml.match.WordMatcher;
import aml.settings.NeighborSimilarityStrategy;
import aml.settings.SizeCategory;
import aml.settings.WordMatchStrategy;
import aml.util.InteractionManager;

public class InteractiveSelector implements Selector
{
	
//Attributes
	
	//Interaction manager
	private InteractionManager im;
	//Selection thresholds
	private final double HIGH_THRESH = 0.7;
	private final double AVERAGE_THRESH = 0.2;
	private double lowThresh = 0.45;
	//Auxiliary variables
	private SizeCategory size;
	private Vector<Rematcher> auxMatchers;
	private Vector<Alignment> auxAlignments;
	
//Constructors
	
	public InteractiveSelector()
	{
		AML aml = AML.getInstance();
		im = aml.getInteractionManager();
		size = aml.getSizeCategory();
		//Construct the list of auxiliary (re)matchers, which will be used
		//to compute auxiliary alignments used for interactive selection
		auxMatchers = new Vector<Rematcher>();
		auxMatchers.add(new WordMatcher(WordMatchStrategy.AVERAGE));
		auxMatchers.add(new StringMatcher());
		auxMatchers.add(new NeighborSimilarityMatcher(
				NeighborSimilarityStrategy.DESCENDANTS,
				!size.equals(SizeCategory.SMALL)));
		auxMatchers.add(new NeighborSimilarityMatcher(
				NeighborSimilarityStrategy.ANCESTORS,
				!size.equals(SizeCategory.SMALL)));
		if(size.equals(SizeCategory.HUGE))
			auxMatchers.add(new BlockRematcher());
	}
	
//Public Methods
	
	@Override
	public Alignment select(Alignment a, double thresh)
	{
		long time = System.currentTimeMillis()/1000;
		System.out.println("Performing Interactive Selection");
		
		//Setup:
		//1) Sort the input alignment
		a.sortDescending();
		//2) Initialize the final alignment
		Alignment selected = new Alignment();
		//3) Set the query limit (which is a share of the global limit)
		int queryLimit;
		if(size.equals(SizeCategory.SMALL))
			//For small ontologies, most of the limit is used in selection
			//(as there is not much need for repair)
			queryLimit = (int)Math.round(im.getLimit()*0.9);
		else
			//For larger ontologies, we reserve a bit more of the limit for repair
			queryLimit = (int)Math.round(im.getLimit()*0.75);
		//4) Start the consecutive negative count and set the limit
		int consecutiveNegativeCount = 0;
		boolean updated = false;
		int consecutiveNegativeLimit;
		if(size.equals(SizeCategory.SMALL) && size.equals(SizeCategory.MEDIUM))
			consecutiveNegativeLimit = 5;
		else
			consecutiveNegativeLimit = 10;
		//5) Compute auxiliary alignments
		auxAlignments = new Vector<Alignment>();
		for(Rematcher r : auxMatchers)
			auxAlignments.add(r.rematch(a));

		//Select - for each mapping:
		for(Mapping m : a)
		{
			//Get the ids and URIs
			int sourceId = m.getSourceId();
			int targetId = m.getTargetId();
			double finalSim = m.getSimilarity();
			//Compute the auxiliary parameters
			double maxSim = 0.0;
			double average = finalSim;
			int support = 0;
			for(int i = 0; i < auxAlignments.size(); i++)
			{
				double sim = auxAlignments.get(i).getSimilarity(sourceId, targetId);
				//All auxiliary alignments count for the max
				if(sim > maxSim)
					maxSim = sim;
				//Block rematcher doesn't count towards the support
				if(i != 4 && sim > 0)
					support++;
				//NSM Ancestors doesn't count towards the average
				if(i != 3)
					average += sim;
			}
			average /= 4;
			
			boolean check = false;
			if(finalSim >= HIGH_THRESH)
			{
				check = true;
				if(support < 2 || selected.containsConflict(m))
				{
					if(im.queryCount() < queryLimit)
						check = im.check(m);
					else
						check = false;
				}
			}
			else if(finalSim >= lowThresh || maxSim >= lowThresh)
			{
				check = false;
				if(im.queryCount() < queryLimit)
				{
					if(support > 1 && average > AVERAGE_THRESH &&
							!selected.containsConflict(m))
					{
						check = im.check(m);
						//Update the consecutive negative query count
						if(check)
							consecutiveNegativeCount = 0;
						else
							consecutiveNegativeCount++;
						//If it exceeds the limit, raise the lowThresh to the current
						//similarity value
						if(!updated && consecutiveNegativeCount > consecutiveNegativeLimit)
						{
							if(finalSim > lowThresh)
								lowThresh = finalSim;
							updated = true;
						}
					}
				}
				else
					break;
			}
			if(check)
				selected.add(sourceId, targetId, maxSim);
		}
		time = System.currentTimeMillis()/1000 - time;
		System.out.println("Finished in " + time + " seconds");
		return selected;
	}
}