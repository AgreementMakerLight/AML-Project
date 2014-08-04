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
* @author Emanuel Santos                                                      *
* @date 23-06-2014                                                            *
* @version 2.0                                                                *
******************************************************************************/
package aml.filter;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;

import aml.AML;
import aml.match.Alignment;
import aml.match.Mapping;
import aml.ontology.URIMap;

public class RepairerOld
{
	
//Attributes
	
	private RepairMapOld repairMap;
	private boolean computeDisjointSets = false;
	private int searchDeepLenght = 0;
	private int maxBlockSize = 100000; //number of checklist terms repaired each time.
	private Double marginOfError = -1.0;
	private int initialThrhold = 0;

//Constructors

	/**
	 * Constructs a new Repairer Object
	 */
	public RepairerOld(){}
	
	/**
	 * Constructs a new Repairer Object
	 * @param blocksize - maximum number of terms from checklist to be considered each loop of the repair.
	 */
	public RepairerOld(int blocksize)
	{
		maxBlockSize = blocksize;
	}
	
//Public Methods
	
	/**
	 * Sets the value of margin of error
	 * @param error -  value of the margin of error
	 * @requires error > 0 && error <= 1
	 */
	public void setMarginError(Double error)
	{
		this.marginOfError = error;
		this.initialThrhold = 10; //(int) Math.max(repairMap.getGlobalListConflicts().size()*0.01, 10);
	}
	
	
	/**
	 * Returns a global repair of an alignment with a coherence check at the end.
	 */
	public Alignment repair(Alignment a)
	{
		//Verify if there are disjoint clauses in the RelationshipMap
		if(AML.getInstance().getRelationshipMap().disjointCount() == 0)
		{
			System.out.println("Nothing to repair - ontologies have no disjoint clauses");
			return a;
		}
		System.out.println("Repairing...");
		//creates repairMap for this repair (builds maps)
		repairMap = new RepairMapOld(a);
		
		//number of possible non optimal mappings deletions
		//int ceilingNonOptimalDeletions = 0;
		
		//gets checklist of terms
		Vector<Integer> checkList = repairMap.getChecklist();
		System.out.println("Classes to check: " + checkList.size());
		
		//setting block variables
		int blockNumber = 1, indexStart = 0, indexEnd = Math.min(checkList.size()-1,maxBlockSize*blockNumber-1);
		Vector<Integer> checkListBlock;
		
		//conflict sets of mappings to resolve
		Vector<Vector<Mapping>> conflictSets;

		//setting the first block of the checklist
		if(indexStart<=indexEnd)
		{
			checkListBlock =  new Vector<Integer>(checkList.subList(indexStart, indexEnd+1));
			System.out.println("Classes to check now: " + checkListBlock.size());
			Vector<Integer> cancelledTerms = new Vector<Integer> ();
			//builts conflicts sets given the terms in checkListBlock
			cancelledTerms = repairMap.builtConflictingSets(checkListBlock);
			//puts cancelled terms in the end of the checkList to be considered at the end again.
			checkList.addAll(cancelledTerms); 
		}
		else{
			checkListBlock = new Vector<Integer>();
		}

		//loop until every term in checklist is verified
		while(!checkListBlock.isEmpty())
		{
			conflictSets = repairMap.getConflictingSets();
			
			saveConflictSetToFile(conflictSets);
						
			//resolve conflict if they exist in this block
			if(!conflictSets.isEmpty()){
				//System.out.println("conflictSets: " + conflictSets.size());
				//applies main repair heuristics
				System.out.println("Resolving " + conflictSets.size() + " minimal conflicting sets of mappings...");
				applyRepairHeuristics(conflictSets,initialThrhold);
			}else{
				System.out.println("No conflicts to resolve");
			}
			
			//updates block variables
			blockNumber++;
			indexStart = indexEnd+1;
			indexEnd = Math.min(checkList.size()-1,maxBlockSize*blockNumber-1);
			
			if(indexStart<=indexEnd)
			{
				checkListBlock =  new Vector<Integer>(checkList.subList(indexStart, indexEnd+1));
				System.out.println("Classes to check now: " + checkListBlock.size());
				Vector<Integer> cancelledTerms = repairMap.builtConflictingSets(checkListBlock);
				checkList.addAll(cancelledTerms);
			}
			else
				break;
		}//end while

		//System.out.println("Possible non-optimal deletions: " +  nr_non_optimal);

		//returns the repair alignment
		Alignment repaired = repairMap.getAlignment();
		System.out.println("Repair finished.");
/*
		System.out.println("Checking coherence");
		if(repairMap.isCoherent())
		{
			System.out.println("Alignment is coherent");
		}
		else
		{
			System.out.println("Alignment is incoherent");
			System.out.println("Incoherent classes:" + repairMap.listIncoherentClasses().size());
			for(Integer aa: repairMap.listIncoherentClasses()){
				System.out.println(AML.getInstance().getURIMap().getURI(aa));
			}
		}
*/		
		return repaired;
	}
	
	
	
//Private Methods

	/**
	 * Main repair algorithm: applies main repair heuristic and filter.
	 * @param current - the set os conflicts mappings to resolve
	 * @param thrhold - value for each filter should be applied.
	 * TODO: parallel? change recursive part.
	 */
	private void applyRepairHeuristics(Vector<Vector<Mapping>> conflictsets, int thrhold)
	{
		//if set empty then nothing to do
		if(conflictsets.isEmpty())
			return;
		//
		if(computeDisjointSets)
		{
			//compute disjoint sets
			Vector<Vector<Vector<Mapping>>> current = disjointConflictSets(conflictsets);
			//loop each joint set of conflict sets
			for(Vector<Vector<Mapping>> cs: current)
			{	
				int iSize = cs.size();
				Vector<Vector<Mapping>> result = intersectionStrategy(cs, searchDeepLenght, thrhold);
				//if it didn't remove any mapping then threshold must be changed
				if(iSize == result.size())
				{
					thrhold = 0;
					//verify if filter is applied
					if(marginOfError>=0.0){
						result = filterMarginErrorMappingLists(result, marginOfError);
					}
				}
				//currently done in a recursive way
				applyRepairHeuristics(result, thrhold);
			}
		}
		else
		{	
			//apply heuristics until there is no conflict set to resolve.
			while(!conflictsets.isEmpty())
			{
				int iSize = conflictsets.size();
				conflictsets = intersectionStrategy(conflictsets, searchDeepLenght, thrhold);
				//if it didn't remove any mapping then thrhold must be changed
				if(iSize == conflictsets.size())
				{
					thrhold = 0;
					//verify if filter is applied
					if(marginOfError>=0.0)
					{
						conflictsets = filterMarginErrorMappingLists(conflictsets, marginOfError);
					}
				}
			}
		}
	}
	
	
	/**
	 * Computes the disjoint set of conflicts sets of mappings (no mapping in common)
	 * @param conflictSets : a set of conflict sets of mappings
	 * @returns A set of disjoint sets of conflict sets (no mapping in common)
	 */
	private static Vector<Vector<Vector<Mapping>>> disjointConflictSets(Vector<Vector<Mapping>> conflictSets)
	{		
		int total = conflictSets.size();
		int joined = 0;		
		HashMap<Mapping, HashSet<Integer>> hashVSet = new HashMap<Mapping, HashSet<Integer>>();
		Vector<Vector<Vector<Mapping>>> result = new Vector<Vector<Vector<Mapping>>>();
		
		//create HashMap will all intersections
		for(int i=0;i<conflictSets.size();i++)
		{
			Vector<Mapping> v = conflictSets.get(i);
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
					Vector<Mapping> vm = conflictSets.get(vmi);
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
	
	
	/**
	 * Main repair heuristics - Selects and deletes the mapping that belongs the highest number of conflict sets.
	 * Deletes the conflicts sets that are resolved (i.e. that include the selected/deleted mapping)
	 * @param conflictsets - set of conflicts sets of mappings
	 * @param depth - extra search depth value in case of tie
	 * @param threshold - minimum number of conflicts sets that the selected mapping must belong to.
	 * @returns A set of conflict sets of mappings that are still unresolved.
	 */
	private Vector<Vector<Mapping>> intersectionStrategy(Vector<Vector<Mapping>> conflictsets, int depth, int threshold)
	{
		if(conflictsets.isEmpty())
			return conflictsets;
		
		//a copy of our input list
		Vector<Vector<Mapping>> vmaps = new Vector<Vector<Mapping>>(conflictsets);
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
		
		//System.out.println("Total keys:" + count_v.keySet().size());
		
		/*
		HashMap<Integer,Integer> count_maps = new HashMap<Integer,Integer>();
		System.out.println("Total keys:" + count_v.keySet().size());
		for(Mapping m: count_v.keySet())
		{	
			int nr = count_v.get(m);
			
			if(count_maps.containsKey(nr)){
				count_maps.put(nr, count_maps.get(nr)+1);
			}else{
				count_maps.put(nr, 1);
			}
		}
		HashSet<Integer> aaa = new HashSet<Integer>(count_v.values());
		ArrayList<Integer> bbb = new ArrayList<Integer>(aaa);
		Collections.sort(bbb);
		
		for(Integer nr: bbb){
			System.out.println(nr + "---" + count_maps.get(nr));
		}
		System.out.println("---------------");
		*/
		
		if(max<=threshold){
			//it stops to remove if the max is on equal or less conflict sets than loweThreshold
			return vmaps;
		}
			
		/*System.out.println("Total keys:" + count_v.keySet().size());
		HashSet<Integer> aaa = new HashSet<Integer>(count_v.values());
		for(Integer ii: aaa){
			System.out.print("Number o conflitct sets:" + ii + " -- ");
			int nr = 0;
			
			for(Mapping m: count_v.keySet()){
				if(count_v.get(m) == ii){
					nr++;
				}
			}
			System.out.println(nr);
		}
		System.out.println("------------");
		*/
		
		//if the list of 'worst' is empty then there is nothing to do
		if(worst.isEmpty())
				return vmaps;
		
		////select the mapping to deleted
		Mapping delete = null;
		int maxListsResolved = -1; //number of mapping lists resolved
		if(worst.size() == 1 || max == vmaps.size() || depth == 0)
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
				int numberListAffected = intersectionSearch(0,depth,vmaps);
				
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
	

	//comparator that sorts the conflicts sets to filter
    private static Comparator<Vector<Mapping>> filteringComparator = new Comparator<Vector<Mapping>>() {

		public int compare(Vector<Mapping> o1,Vector<Mapping> o2) {
			
			Collections.sort(o1);
			Collections.sort(o2);
			
			Double leasto1 = o1.get(0).getSimilarity();
			Double leasto2 = o2.get(0).getSimilarity();
			
			Double diff = leasto1 - leasto2;
			
			if(Math.abs(diff)<0.00001){
				return 0;
			}else if(diff>0){
				return -1;
			}else{
				return 1;
			}
		}
	};
	
	
	/**
	 * Filtering heuristics - Removes all the conflict sets of mappings where its lowest confidence value mapping is lower 
	 * than the second lowest within a confidence interval
	 * @param conflictsets - A set of conflict sets of mappings to filter
	 * @param confidenceinterval - value of the confidence interval.
	 * @returns A set of filtered conflict sets. 
	 */
	private Vector<Vector<Mapping>> filterMarginErrorMappingLists(Vector<Vector<Mapping>> mappings, Double confidenceinterval)
	{
		//sorts conflict sets from the one that has the highest lowest-confidence mapping to the one that has 
		//lowest-confidence least mapping
		Collections.sort(mappings, filteringComparator);
		Vector<Vector<Mapping>> filtered = new Vector<Vector<Mapping>>(1,1);
		
		for(Vector<Mapping> vm : mappings)
		{
			Collections.sort(vm);
			Mapping lowest = vm.get(0);  //lowest-confidence mapping
			Mapping lowest2 = vm.get(1); //second lowest-confidence mapping
		
			if(withinMarginError(lowest,lowest2,confidenceinterval))
			{
				//the lowest-confidence value is within the confidence interval
				filtered.add(vm);
			}else{
				repairMap.remove(lowest);
			}				
		}
		return filtered;
	}
	
	
	/**
	 * Checks if the confidence values of the mappings are within the confidence interval
	 * @param m1 - a mapping
	 * @param m2 - a mapping
	 * @param confidenceinterval : A value for the margin of error
	 * @result true iff the similarity values of m1 and m2 are within the margin of error
	 */
	private boolean withinMarginError(Mapping mapping1, Mapping mapping2, Double confidenceinterval)
	{
		Double value1 = mapping1.getSimilarity();
		Double value2 = mapping2.getSimilarity();
		
		if(value1<=value2)
		{
			return value1 + confidenceinterval >= value2 - confidenceinterval;
		}else
		{
			return value2 + confidenceinterval >= value1 - confidenceinterval;
		}
	}
	
	
	private void saveConflictSetToFile(Vector<Vector<Mapping>> cs)
	{
		URIMap uris = AML.getInstance().getURIMap();
		try
		{
			PrintWriter outF = new PrintWriter(new FileOutputStream("store/temp.txt"));
			outF.println("-----CONFLICT SET SIZE=" + cs.size() + "-------");
			for(int i = 0; i < cs.size(); i++)
			{
				
				Vector<Mapping> maps = cs.get(i);
				if(maps.size() == 0){
					System.out.println("ERRO - Conflicting Set = 0");
					System.exit(0);
				}
					
				
				outF.println("-------------CS-"+ i +"-----------------------");

				for(int j = 0; j < maps.size(); j++)
				{
					Mapping m = maps.get(j);
					outF.println(uris.getURI(m.getSourceId()) + "  ---  " + uris.getURI(m.getTargetId()));
				}
				outF.println("----------------------------------------");
		
			}
			outF.close();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
}