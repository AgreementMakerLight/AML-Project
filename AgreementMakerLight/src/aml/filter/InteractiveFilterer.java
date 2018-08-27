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
* Filterer that uses (simulated) user interaction (through the Interaction    *
* Manager) to help perform alignment selection.                               *
*                                                                             *
* @author Daniel Faria, Aynaz Taheri                                          *
******************************************************************************/
package aml.filter;

import aml.AML;
import aml.alignment.Alignment;
import aml.alignment.SimpleAlignment;
import aml.alignment.mapping.Mapping;
import aml.alignment.mapping.MappingStatus;
import aml.alignment.mapping.SimpleMapping;
import aml.settings.SizeCategory;
import aml.util.interactive.InteractionManager;

public class InteractiveFilterer implements Filterer
{
	
//Attributes

	//Selection thresholds
	private final double HIGH_THRESH = 0.7;
	private final double AVERAGE_THRESH = 0.2;
	private double lowThresh = 0.45;
	
//Constructors
	
	public InteractiveFilterer(){}
	
//Public Methods
	
	@Override
	@SuppressWarnings("rawtypes")
	public Alignment filter(Alignment a)
	{
		if(!(a instanceof SimpleAlignment))
		{
			System.out.println("Warning: cannot filter non-simple alignment!");
			return a;
		}
		System.out.println("Performing Interactive Selection");
		long time = System.currentTimeMillis()/1000;

		AML aml = AML.getInstance();
		InteractionManager im = aml.getInteractionManager();
		SizeCategory size = aml.getSizeCategory();
		SimpleAlignment in = (SimpleAlignment)a;
		//Setup:
		//1) Sort the input alignment
		in.sortDescending();
		//2) Initialize the final alignment
		SimpleAlignment out = new SimpleAlignment();
		//3) Start the consecutive negative count and set the limit
		int consecutiveNegativeCount = 0;
		boolean updated = false;
		int consecutiveNegativeLimit;
		if(size.equals(SizeCategory.SMALL) && size.equals(SizeCategory.MEDIUM))
			consecutiveNegativeLimit = 5;
		else
			consecutiveNegativeLimit = 10;
		//4) Run the QualityFlagger
		QualityFlagger qf = new QualityFlagger();
		qf.flag(in);
		
		//Select - for each mapping:
		for(Mapping<String> m : in)
		{
			//Get the ids
			String source = m.getEntity1();
			String target = m.getEntity2();
			double finalSim = m.getSimilarity();
			//Compute the auxiliary parameters
			double maxSim = qf.getMaxSimilarity(source, target);
			double average = qf.getAverageSimilarity(source, target);
			int support = qf.getSupport(source, target);
			
			if(finalSim >= HIGH_THRESH)
			{
				if(support < 2 || out.containsConflict(m))
				{
					if(im.isInteractive())
						im.classify((SimpleMapping)m);
					else if(out.containsBetterMapping(m))
						m.setStatus(MappingStatus.INCORRECT);
					else
						m.setStatus(MappingStatus.CORRECT);
				}
			}
			else if(finalSim >= lowThresh || maxSim >= lowThresh)
			{
				if(im.isInteractive())
				{
					if(support > 1 && average > AVERAGE_THRESH &&
							!out.containsConflict(m))
					{
						im.classify((SimpleMapping)m);
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
					else
						m.setStatus(MappingStatus.INCORRECT);
				}
				else
					break;
			}
			else
				break;
			if(!m.getStatus().equals(MappingStatus.INCORRECT))
				out.add(source, target, maxSim);
		}
		time = System.currentTimeMillis()/1000 - time;
		System.out.println("Finished in " + time + " seconds");
		return out;
	}
}