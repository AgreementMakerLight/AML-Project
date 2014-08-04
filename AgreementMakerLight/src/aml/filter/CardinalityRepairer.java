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
* Heuristic repair algorithm that approximates the global minimum number of   *
* removed mappings.                                                           *
*                                                                             *
* @author Daniel Faria & Emanuel Santos                                       *
* @date 01-08-2014                                                            *
* @version 2.0                                                                *
******************************************************************************/
package aml.filter;

import java.util.HashSet;
import java.util.Vector;

import aml.match.Alignment;
import aml.util.Table2;

public class CardinalityRepairer implements Repairer
{
	
//Attributes
	
	private Alignment toRepair;
	private Vector<Path> conflictSets;
	private Table2<Integer,Integer> conflictMappings;
	private Table2<Integer,Integer> mappingConflicts;
	
//Constructors

	/**
	 * Constructs a new CardinalityRepairer
	 */
	public CardinalityRepairer(Alignment a)
	{
		toRepair = a;
		RepairMap rMap = new RepairMap(a);
		conflictSets = rMap.getConflictSets();
		init();
	}
	
//Public Methods
	
	@Override
	public Alignment repair()
	{
		HashSet<Integer> toRemove = new HashSet<Integer>();
		while(conflictMappings.size() > 0)
		{
			int worstMapping = getWorstMapping();
			toRemove.add(worstMapping);
			remove(worstMapping);
		}
		Alignment repaired = new Alignment();
		for(int i = 0; i < toRepair.size(); i++)
		{
			if(!toRemove.contains(i))
				repaired.add(toRepair.get(i));
		}
		return repaired;
	}
	
//Private Methods
	
	private int getWorstMapping()
	{
		int worstMapping = -1;
		int maxCard = 0;
		
		for(Integer i : mappingConflicts.keySet())
		{
			int card = mappingConflicts.get(i).size();
			if(card > maxCard || (card == maxCard &&
					toRepair.get(i).getSimilarity() <
					toRepair.get(worstMapping).getSimilarity()))
			{
				maxCard = card;
				worstMapping = i;
			}
		}
		return worstMapping;
	}
	
	private void init()
	{
		conflictMappings = new Table2<Integer,Integer>();
		mappingConflicts = new Table2<Integer,Integer>();
		
		for(int i = 0; i < conflictSets.size(); i++)
		{
			for(Integer j : conflictSets.get(i))
			{
				conflictMappings.add(i,j);
				mappingConflicts.add(j,i);
			}
		}
	}
	
	private void remove(int mapping)
	{
		HashSet<Integer> conflicts = new HashSet<Integer>(mappingConflicts.get(mapping));
		for(Integer i : conflicts)
		{
			for(Integer j : conflictMappings.get(i))
			{
				if(mappingConflicts.get(j).size() == 1)
					mappingConflicts.remove(j);
				else
					mappingConflicts.remove(j,i);
			}
			conflictMappings.remove(i);
		}
		mappingConflicts.remove(mapping);
	}
}