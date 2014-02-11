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
* @author Emanuel Santos & Daniel Faria                                       *
* @date 31-01-2014                                                            *
******************************************************************************/
package aml.repair;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;

import aml.match.Alignment;
import aml.match.Mapping;
import aml.ontology.Ontology;

public class Repairer
{
	
//Attributes
	
	private RepairMap repairMap;
	private boolean computeIntersection = false;
	private int searchDeepLenght = 0;
	private int maxchecklist = 10000;

//Constructors

	/**
	 * Constructs a new GlobalRepair Object
	 */
	public Repairer(){}
	
//Public Methods
	
	/**
	 * Returns a global repair of an alignment with a coherence check at the end.
	 */
	public Alignment repair(Alignment a)
	{
		//If the cardinality of the alignment is not 1-to-1,
		//reduce the checklist size limit
		double cardinality = a.cardinality();
		if(cardinality > 1.4)
			maxchecklist = 4000;
		else if(cardinality > 1)
			maxchecklist = 8000;
		
		Ontology source = a.getSource();
		Ontology target = a.getTarget();
		if(source.getRelationshipMap().disjointCount() == 0 &&
				target.getRelationshipMap().disjointCount() == 0)
		{
			System.out.println("Nothing to repair - ontologies have no disjoint clauses");
			return a;
		}
		repairMap = new RepairMap(a);
		
		//number of possible non_optimal mappings deletions
		int nr_non_optimal = 0;
		Vector<String> checklistall = new Vector<String>(repairMap.getChecklist());
		System.out.println("Classes to check: " + checklistall.size());
		int part = 1;
		int index1 = 0;
		int index2 = Math.min(checklistall.size()-1,maxchecklist*part-1);
		Vector<Vector<Mapping>> globalconflictList;
		Vector<String> checklistlocal;

		if(index1<=index2)
		{
			checklistlocal =  new Vector<String>(checklistall.subList(index1, index2));
			repairMap.builtConflictSetsOfMappings(checklistlocal);
		}
		else
			checklistlocal = new Vector<String>();

		
		while(!checklistlocal.isEmpty() && !(globalconflictList = repairMap.getGlobalListConflicts()).isEmpty())
		{
			if(!globalconflictList.isEmpty())
			{
				Vector<Vector<Mapping>> conflictList = globalconflictList;
				//Determine the minimal set of mappings
				if(!conflictList.isEmpty())
				{
					Vector<Vector<Vector<Mapping>>> conflictListsList;
					if(computeIntersection)
						conflictListsList = intersectionMappings(conflictList);
					else
					{
						conflictListsList = new Vector<Vector<Vector<Mapping>>>();
						conflictListsList.add(conflictList);
					}
					//only for computeIntersection==true
					int nr_total_groups = conflictListsList.size();
					for(int ii = 0; ii<conflictListsList.size();ii++)
					{
						Vector<Vector<Mapping>> cc = conflictListsList.get(ii);
						Vector<Vector<Mapping>> new_conflictslist = intersectionStrategy(cc,searchDeepLenght);
						if(!new_conflictslist.isEmpty())
						{
							//if it is not empty do a intersection again to check for redundant lists.
							if(ii>=nr_total_groups)
								//a group that was already "divided" more than 2 times, i.e., it can be non-optimal
								nr_non_optimal++;
	
							if(computeIntersection)
							{
								Vector<Vector<Vector<Mapping>>> ii_new = intersectionMappings(new_conflictslist);
								conflictListsList.addAll(ii_new);
							}
							else
								conflictListsList.add(new_conflictslist);
						}
					}
				}
			}
			part++;
			index1 = index2+1;
			index2 = Math.min(checklistall.size()-1,maxchecklist*part-1);
			if(index1<=index2)
			{
				checklistlocal =  new Vector<String>(checklistall.subList(index1, index2));
				repairMap.builtConflictSetsOfMappings(checklistlocal);
			}
			else
				checklistlocal = new Vector<String>();
		}//end while

		System.out.println("Possible non-optimal deletions: " +  nr_non_optimal);

		//returns the repair alignment
		Alignment repaired = repairMap.getAlignment();

		return repaired;
	}

//Private Methods
	
	private static Vector<Vector<Vector<Mapping>>> intersectionMappings(Vector<Vector<Mapping>> vvMappings)
	{		
		int total = vvMappings.size();
		int joined = 0;		
		HashMap<Mapping, HashSet<Integer>> hashVSet = new HashMap<Mapping, HashSet<Integer>>();
		Vector<Vector<Vector<Mapping>>> result = new Vector<Vector<Vector<Mapping>>>();
		
		//create HashMap will all intersections
		for(int i=0;i<vvMappings.size();i++)
		{
			Vector<Mapping> v = vvMappings.get(i);
			for(Mapping m: v)
			{
				if(!hashVSet.containsKey(m))
				{
					HashSet<Integer> hs = new HashSet<Integer>();
					hs.add(i);
					hashVSet.put(m, hs);
				}
				else
					hashVSet.get(m).add(i);
			}
		}
		//create transitivity
		Vector<Mapping> mappings = new Vector<Mapping>(hashVSet.keySet());
		for(int i=0;i<mappings.size();i++)
		{
			Mapping m = mappings.get(i);
			if(!hashVSet.containsKey(m))
				continue;
			HashSet<Vector<Mapping>> newSet= new HashSet<Vector<Mapping>>();
			HashSet<Mapping> toCheck =  new HashSet<Mapping>();
			toCheck.add(m);
			while(!toCheck.isEmpty())
			{
				Mapping current = toCheck.iterator().next();
				HashSet<Integer> vi = hashVSet.get(current);
				hashVSet.remove(current);//done, remove it
				toCheck.remove(current);	//done, remove it
				for(Integer vmi: vi)
				{
					Vector<Mapping> vm = vvMappings.get(vmi);
					if(newSet.add(vm))
					{
						//not contain
						for(Mapping mp: vm)
						{
							if(hashVSet.containsKey(mp))
									toCheck.add(mp); //adds if it doesnt belong yet
						}
					}	
				}
				if(joined + newSet.size()==total)
					break;
			}
			//add the result
			result.add(new Vector<Vector<Mapping>>(newSet));
			joined += newSet.size();
			if(joined==total)
				break;
		}
		return result;
	}
	
	// This strategy finds the mapping that belongs to the highest number of lists and lowest similarity.
	// If there is more than one, then it performs a deep search of maximum lenght 'deep' to check
	// which one (2)...(deep) best alternatives for each tie mapping belongs to highest number of lists.
	// If there still more than one, it selects the first of the list. 
	private Vector<Vector<Mapping>> intersectionStrategy(Vector<Vector<Mapping>> vvMappings, int deep)
	{
		if(vvMappings.isEmpty())
			return vvMappings;
		
		//a copy of our input list
		Vector<Vector<Mapping>> vmaps = new Vector<Vector<Mapping>>(vvMappings);
		//the map that maps a mapping to the number of sets that it belongs to
		HashMap<Mapping,Integer> count_v = new HashMap<Mapping,Integer>();
		int max = 0; // maximum current number
		Double minsim = 2.0; // the maximum similarity of the current maximum
		Vector<Mapping> worst = new Vector<Mapping>(); // the list of the current worst mappings
	
		//loops for each list of mappings
		for(int idx = 0; idx<vmaps.size(); idx++)
		{
			Vector<Mapping> mp = vmaps.get(idx);
			
			//checks if the current repairMap constains all the mappings of the list
			if(repairMap.getAlignment().containsMappings(mp))
			{			
				//for each mapping of the list 
				for(Mapping m: mp)
				{
					//gets the number of ocorrencies of this mapping and updates it 
					Integer ic = count_v.get(m);
					if(ic == null)
					{
						ic=1;
						count_v.put(m, ic);//first time
					}
					else
					{
						ic++;
						count_v.put(m, ic);//add
					}
					//checks if it is a new 'worst'
					if(max<ic || (max==ic && m.getSimilarity()<minsim))
					{	
						//new max & worst
						max = ic;
						minsim = m.getSimilarity();
						worst.clear(); 
						worst.add(m);						
					}
					else if(max == ic && minsim==m.getSimilarity())
						worst.add(m); //add new worst
				}//end for
			}
			else
			{
				//this list doesnt anymore. it is removed from the current list.
				vmaps.remove(idx);
				idx--; //because it was removed
			}
		}//end for
		
		//if the list of 'worst' is empty then there is nothing to do
		if(worst.isEmpty())
				return vmaps;
		
		////select the mapping to deleted
		Mapping delete = null;
		int maxListsResolved = -1; //number of mapping lists resolved
		if(worst.size() == 1 || max == vmaps.size() || deep == 0)
		{
			//there is only one, there is no tie
			delete = worst.firstElement();
			
		}
		else if(worst.size()>1)
		{
			//there more than one alternative mapping to delete
			for(Mapping wm: worst)
			{
				//guess wm is to delete
				repairMap.remove(wm);
				//get the maximum number of lists of mappings resolved ofter this remocal
				int numberListAffected = intersectionSearch(0,deep,vmaps);
				
				//is it a new maximum?
				if(maxListsResolved<numberListAffected)
				{
					delete = wm;
					maxListsResolved = numberListAffected;
				}
				//guess wm is not to delete
				repairMap.add(wm);
			}
		}
		//delete the mapping selected as worst 
		if(delete != null)
			repairMap.remove(delete);
		//removes the list that include the worst element
		for(int idx = 0; idx<vmaps.size(); idx++)
		{
			Vector<Mapping> mp = vmaps.get(idx);
			
			if(mp.contains(delete))
			{
				vmaps.remove(idx);
				idx--; //it was removed
			}
		}
		return vmaps;
	}
	
	// This strategy finds the mapping that belongs to the highest number of lists and lowest similarity.
	private int intersectionSearch(int deep, int maxDeep, Vector<Vector<Mapping>> vvMappings)
	{
		if(vvMappings.isEmpty() || deep>maxDeep)
			return 0;
		
		Vector<Vector<Mapping>> vmaps = new Vector<Vector<Mapping>>(vvMappings);
		HashMap<Mapping,Integer> count_v = new HashMap<Mapping,Integer>();
		int max = 0;
		Double maxsim = 0.0;
		Vector<Mapping> worst = new Vector<Mapping>();
		
		for(int idx = 0; idx<vmaps.size(); idx++)
		{
			Vector<Mapping> mp = vmaps.get(idx);
				
			if(repairMap.getAlignment().containsMappings(mp))
			{			
				for(Mapping m: mp)
				{
					Integer ic = count_v.get(m);
					if(ic == null)
					{
						ic=1;
						count_v.put(m, ic);//first time
					}
					else
					{
						ic++;
						count_v.put(m, ic); //add
					}
					if(max<ic || (max==ic && m.getSimilarity()>maxsim))
					{//new max & worst	
						max = ic;
						maxsim = m.getSimilarity();
						worst.clear(); 
						worst.add(m);						
					}
					else if(max == ic && maxsim==m.getSimilarity())
						worst.add(m); //new worst
				}
			}
			else
			{
				vmaps.remove(idx);
				idx--; //it was removed
			}
		}
		count_v.clear();
		if(worst.isEmpty())
			return 0;
		int maxdelete = -1;
		for(Mapping ww: worst)
		{
			//remove
			repairMap.remove(ww);
			int na = intersectionSearch(deep+1,maxDeep,vmaps);
			if(na>maxdelete)
				maxdelete = na;
			//not remove
			repairMap.add(ww);
		}
		return maxdelete + max;
	}
}