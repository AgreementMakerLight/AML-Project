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
* Filterer that uses (simulated) user interaction (through the Interaction    *
* Manager) to help perform alignment selection.                               *
*                                                                             *
* @author Daniel Faria, Aynaz Taheri                                          *
******************************************************************************/
package aml.filter;

import aml.AML;
import aml.match.Alignment;
import aml.match.Mapping;
import aml.settings.MappingStatus;
import aml.settings.SizeCategory;
import aml.util.InteractionManager;

public class InteractiveFilterer implements Filterer
{
	
//Attributes

	private AML aml;
	private InteractionManager im;
	private Alignment a;
	private QualityFlagger qf;
	//Selection thresholds
	private final double HIGH_THRESH = 0.7;
	private final double AVERAGE_THRESH = 0.2;
	private double lowThresh = 0.45;
	//Auxiliary variables
	private SizeCategory size;
	
//Constructors
	
	public InteractiveFilterer()
	{
		aml = AML.getInstance();
		im = aml.getInteractionManager();
		size = aml.getSizeCategory();
		a = aml.getAlignment();
		qf = aml.buildQualityFlagger();
	}
	
//Public Methods
	
	@Override
	public void filter()
	{
		long time = System.currentTimeMillis()/1000;
		System.out.println("Performing Interactive Selection");
		
		//Setup:
		//1) Sort the input alignment
		a.sortDescending();
		//2) Initialize the final alignment
		Alignment selected = new Alignment();
		//3) Start the consecutive negative count and set the limit
		int consecutiveNegativeCount = 0;
		boolean updated = false;
		int consecutiveNegativeLimit;
		if(size.equals(SizeCategory.SMALL) && size.equals(SizeCategory.MEDIUM))
			consecutiveNegativeLimit = 5;
		else
			consecutiveNegativeLimit = 10;

		//Select - for each mapping:
		for(Mapping m : a)
		{
			//Get the ids
			int sourceId = m.getSourceId();
			int targetId = m.getTargetId();
			double finalSim = m.getSimilarity();
			//Compute the auxiliary parameters
			double maxSim = qf.getMaxSimilarity(sourceId, targetId);
			double average = qf.getAverageSimilarity(sourceId, targetId);
			int support = qf.getSupport(sourceId, targetId);
			
			if(finalSim >= HIGH_THRESH)
			{
				if(support < 2 || selected.containsConflict(m))
				{
					if(im.isInteractive())
						im.classify(m);
					else
						m.setStatus(MappingStatus.INCORRECT);
				}
			}
			else if(finalSim >= lowThresh || maxSim >= lowThresh)
			{
				if(im.isInteractive())
				{
					if(support > 1 && average > AVERAGE_THRESH &&
							!selected.containsConflict(m))
					{
						im.classify(m);
						//Update the consecutive negative query count
						if(m.getStatus().equals(MappingStatus.INCORRECT))
							consecutiveNegativeCount++;
						else
							consecutiveNegativeCount = 0;
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
			if(!m.getStatus().equals(MappingStatus.INCORRECT))
				selected.add(sourceId, targetId, maxSim);
		}
		time = System.currentTimeMillis()/1000 - time;
		System.out.println("Finished in " + time + " seconds");
		aml.setAlignment(selected);
	}
}