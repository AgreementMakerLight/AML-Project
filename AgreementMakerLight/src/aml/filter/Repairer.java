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
* A filtering algorithm based on logical coherence.                           *
*                                                                             *
* @author Daniel Faria & Emanuel Santos                                       *
******************************************************************************/
package aml.filter;

import aml.AML;
import aml.match.Mapping;
import aml.settings.MappingStatus;
import aml.util.InteractionManager;

public class Repairer implements Filterer, Flagger
{
	
//Attributes
	
	private AML aml;
	private RepairMap rMap;
	private InteractionManager im;
	
//Constructors
	
	/**
	 * Constructs a Repairer for automatic repair
	 */
	public Repairer()
	{
		aml = AML.getInstance();
		rMap = aml.getRepairMap();
		if(rMap == null)
			rMap = aml.buildRepairMap();
		im = aml.getInteractionManager();
	}

//Public Methods
	
	@Override
	public void filter()
	{
		if(rMap.isCoherent())
		{
			System.out.println("Alignment is coherent");
			return;
		}
		System.out.println("Repairing Alignment");
		long time = System.currentTimeMillis()/1000;
		int repairCount = 0;
		//Loop until no more mappings can be removed
		while(true)
		{
			int worstMapping = getWorstMapping();
			if(worstMapping != -1)
			{
				if(im.isInteractive())
				{	
					Mapping m = rMap.getMapping(worstMapping);
					im.classify(m);
					if(m.getStatus().equals(MappingStatus.CORRECT))
						continue;
				}
				rMap.remove(worstMapping);
				repairCount++;
			}
			else
				break;
		}
		aml.removeIncorrect();
		System.out.println("Finished Repair in " + 
				(System.currentTimeMillis()/1000-time) + " seconds");
		System.out.println("Removed " + repairCount + " mappings");
	}
	
	@Override
	public void flag()
	{
		System.out.println("Running Coherence Flagger");
		long time = System.currentTimeMillis()/1000;
		for(Integer i : rMap)
			if(rMap.getMapping(i).getStatus().equals(MappingStatus.UNKNOWN))
				rMap.getMapping(i).setStatus(MappingStatus.FLAGGED);
		System.out.println("Finished in " +	(System.currentTimeMillis()/1000-time) + " seconds");
	}
	
//Private Methods
	
	private int getWorstMapping()
	{
		int worstMapping = -1;
		int maxCard = 0;
		
		for(Integer i : rMap)
		{
			int card = rMap.getConflicts(i).size();
			Mapping m = rMap.getMapping(i);
			if(card > maxCard || (card == maxCard &&
				m.getSimilarity() < rMap.getMapping(worstMapping).getSimilarity()) &&
				!m.getStatus().equals(MappingStatus.CORRECT))
			{
				maxCard = card;
				worstMapping = i;
			}
		}
		return worstMapping;
	}
}