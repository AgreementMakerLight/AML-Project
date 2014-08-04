/******************************************************************************
 * Copyright 2013-2013 LASIGE                                                  *
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
 * Unified map of relationships, disjoint clauses and mappings from two        *
 * Ontologies plus their Alignment, which supports the Repairer.               *
 * NOTE: To ensure identifiers are unique, source term indexes are prepended   *
 * by "S" and target term indexes are prepended by "T".                        *
 *                                                                             *
 * @authors Emanuel Santos & Daniel Faria                                      *
 * @date 23-06-2014                                                            *
 * @version 2.0                                                                *
 ******************************************************************************/
package aml.filter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import aml.AML;
import aml.match.Alignment;
import aml.match.Mapping;
import aml.AML.MappingRelation;
import aml.ontology.RelationshipMap;
import aml.util.Table2Plus;

public class RepairMapOld implements Iterable<Integer>
{

	//Attributes

	//A copy of the Alignment to repair 
	private Alignment align;
	//Link to the RelationshipMap
	private RelationshipMap rels;
	//List of classes that need to be checked
	private Vector<Integer> checklist;
	//Unified map of ascending relationships of the source and target Ontologies
	private Table2Plus<Integer,Integer,Boolean> ancestorMap;

	//maximum size of conflicting sets
	private int maxConflictSetSize = 10000000;
	//Maximum number of Threads
	private int maxNumberThreads = Runtime.getRuntime().availableProcessors();
	//Time out settings for threads for conflict sets search
	private int timeOut = 500;
	private TimeUnit timeUnit = TimeUnit.SECONDS;

	// HashMap that stores all terms of checklist that were already computed
	public ConcurrentHashMap<Integer,HashMap<Integer,Vector<Vector<Mapping>>>> conflictsMapComputed = new ConcurrentHashMap<Integer,HashMap<Integer,Vector<Vector<Mapping>>>>();
	// The current set of conflicting sets of mappings (created during built)
	private Vector<Vector<Mapping>> currentConflictSets = new Vector<Vector<Mapping>>();


	//Constructors

	/**
	 * Constructs a new RepairMap from the input Alignment
	 * @param a: the Alignment to build the RepairMap
	 */
	public RepairMapOld(Alignment a)
	{
		align = new Alignment(a); //a copy of the alignment
		rels = AML.getInstance().getRelationshipMap();
		checklist = new Vector<Integer>(0,1);
		ancestorMap = new Table2Plus<Integer,Integer,Boolean>();
		buildMaps();
	}


	//Public Methods

	public Vector<Integer> getChecklist()
	{
		return new Vector<Integer>(checklist);
	}

	/**
	 * @param classId: the id of the input class
	 * @return whether the class is inconsistent
	 */
	public boolean isInconsistent(int classId)
	{
		Vector<Integer> ancestors = getExtendedSuperClasses(classId);
		for(int i = 0; i < ancestors.size()-1; i++)
		{
			Set<Integer> disj = rels.getDisjoint(ancestors.get(i));
			if(disj == null)
				continue;
			for(int j = i + 1; j < ancestors.size(); j++)
				if(disj.contains(ancestors.get(j)))
					return true;
		}
		return false;
	}


	/**
	 * @return an iterator over the checklist
	 */
	public Iterator<Integer> iterator()
	{
		return checklist.iterator();
	}


	/**
	 * @return the list of terms in the checklist that are inconsistent
	 */
	public Vector<Integer> listIncoherentClasses()
	{
		Vector<Integer> classes = new Vector<Integer>(0,1);
		for(Integer i : checklist)
			if(isInconsistent(i))
				classes.add(i);
		return classes;
	}

	/**
	 * @return true if the alignment is coherent
	 */
	public boolean isCoherent()
	{
		for(Integer s : checklist)
			if(isInconsistent(s))
				return false;
		return true;
	}

	/**
	 * @param term: the global identifier of the input term
	 * @return the set of disjoint for which the term is inconsistent
	 */
	public Vector<Vector<Integer>> getConflictDisjoints(Integer term)
	{
		Vector<Integer> ancestors = getExtendedSuperClasses(term);
		Vector<Vector<Integer>> disjoints = new Vector<Vector<Integer>>(1,1);

		for(int i = 0; i < ancestors.size()-1; i++)
		{
			Set<Integer> disj = rels.getDisjoint(ancestors.get(i));
			if(disj == null)
				continue;
			for(int j = i + 1; j < ancestors.size(); j++)
			{
				if(disj.contains(ancestors.get(j)))
				{
					Vector<Integer> cdisj = new Vector<Integer>();
					cdisj.add(ancestors.get(i));
					cdisj.add(ancestors.get(j));
					disjoints.add(cdisj);
				}
			}
		}
		return disjoints;
	}



	/**
	 * Adds a mapping to the alignment.
	 * @param map - the mapping to be add from the alignment
	 * @note this method does NOT update the ancestorMap. Only the respective alignment.
	 */
	public void add(Mapping map)
	{
		align.add(map);		
	}


	/**
	 * Adds a list of mappings to the alignment.
	 * @param maps - list of mappings to be removed from the alignment
	 * @note this method does NOT update the ancestorMap. Only the respective alignment.
	 */
	public void add(Vector<Mapping> maps)
	{
		align.addAll(maps);	
	}


	/**
	 * removes a mapping from the alignment.
	 * @param map - the mapping to be removed from the alignment
	 * @note this method does NOT update the ancestorMap. Only the respective alignment.
	 */
	public void remove(Mapping map)
	{
		align.remove(map);		
	}


	/**
	 * removes a list of mappings from the alignment.
	 * @param maps - list of mappings to be removed from the alignment
	 * @note this method does NOT update the ancestorMap. Only the respective alignment.
	 */
	public void remove(Vector<Mapping> maps)
	{
		align.removeAll(maps);		
	}


	/**
	 * @returns the current alignment
	 */
	public Alignment getAlignment()
	{
		return align;		
	}

	/**
	 * @returns current set of conflicting sets of mappings
	 */
	public Vector<Vector<Mapping>> getConflictingSets()
	{
		return currentConflictSets;		
	}


	/**
	 * builds the (minimal) set of conflicting sets of mappings given the checklist of terms.
	 * Uses parallel method.
	 * @param checklist: a set of terms to search for conflicting sets.
	 * @returns a set of terms that were not considered in this build because of timeout.
	 * @ensures getConflictingSets() return value is updated
	 */
	public Vector<Integer> builtConflictingSets(Vector<Integer> checklist)
	{
		conflictsMapComputed.clear();
		Vector<Integer> cancelledTerms = builtConflictingSetsInParallel(checklist); //parallel version
		checklist.removeAll(cancelledTerms);
		builtMinimalConflictMapsParallel(checklist);
		
		return cancelledTerms;
	}


	//Private Methods

	//Builds the internal checklist
	private void buildMaps()
	{		
		//Build a list of mapped classes
		Set<Integer> sourceList = align.getSources();
		Set<Integer> targetList = align.getTargets();

		HashSet<Integer> classList = new HashSet<Integer>(sourceList);
		classList.addAll(targetList);
		//Plus each of its descendants that has more than one parent or is in a disjoint
		Vector<Integer> descList = new Vector<Integer>();

		for(int i : classList)
		{
			Set<Integer> desc = rels.getSubClasses(i, false);
			for(Integer j : desc)
			{		
				if(!classList.contains(j) && !descList.contains(j) && 
						(rels.superClassCount(j, true) > 1 || rels.getDisjoint(j).size() > 0))
					descList.add(j);
			}
		}
		
		//Remove the descendants that have a subclass with more than one parent
		//or a subclass that is in a Mapping
		for(int i = 0; i < descList.size(); i++)
		{
			int cls = descList.get(i);
			Set<Integer> desc = rels.getSubClasses(cls,false);
			for(Integer j : desc)
			{
				if(descList.contains(j) || classList.contains(j))
				{
					descList.remove(i--);
					break;
				}
			}
		}
		
		//Add descList to the list of classes that need to be checked
		checklist.addAll(descList);

		//Go through the class list
		Set<Integer> mappedList = sourceList;
		//We only need to add the source OR the target. We choose the smallest one.
		if(targetList.size() < sourceList.size()){
			mappedList = targetList;
		}

		for(Integer i : mappedList)
		{	
			//Exclude mapped classes that are already on the check list or that have a mapped subclass
			Set<Integer> desc = rels.getSubClasses(i,false);
			boolean checkcls = true;
			for(Integer j : desc)
			{
				if((checklist.contains(j) || classList.contains(j)) &&
						!rels.getSubClasses(j,false).contains(i))
				{
					//it is a superclass
					checkcls = false;
					break;
				}
			}
			//Add all those that don't to the check list
			if(checkcls)
				checklist.add(i);
		}
		//Add all descendants to the classList
		classList.addAll(descList);
		//Check that all target and source classes may lead to incoherence
		//Remove classes that are not subclasses of at least 2 mappings or 1 mapping and 1 disjunction.

		//Build a list of classes consisting of each mapped term
		HashSet<Integer> originalList = new HashSet<Integer>(sourceList);
		originalList.addAll(targetList);

		HashSet<Integer> finalList = new HashSet<Integer>();
		Vector<Integer> checklistRemove = new Vector<Integer>();

		//Check classes that have a ancestor mapping and a ancestor disjoint or mapping
		for(Integer i : classList)
		{

			if(originalList.contains(i))
			{
				finalList.add(i);
				continue;
			}
			Set<Integer> ancs = rels.getSuperClasses(i,false);
			int aDisjoints = 0;
			int aMappings = 0;

			for(Integer a: ancs)
			{
				if(align.containsSource(a) || align.containsTarget(a))
					aMappings ++;
				if(rels.hasDisjoint(a))
					aDisjoints++;
			}
			if(aMappings + aDisjoints > 1)
				finalList.add(i);
			else
				checklistRemove.add(i);
		}

		classList = finalList;
		checklist.removeAll(checklistRemove);

		//remove redundant terms to check (classes with same ancestors)
		//Build the ancestorMap

		//Start by adding all relationships between classList terms
		for(Integer i : classList)
		{
			//And their ancestors
			Set<Integer> ancs = rels.getSuperClasses(i,false);
			for(Integer j : ancs)
				//That are either in the sourceList themselves or in a disjoint clause
				if(classList.contains(j) || rels.hasDisjoint(j))
					ancestorMap.add(i, j, null);
		}

		ArrayList<Integer> ancestorTermsSet = new ArrayList<Integer>(ancestorMap.keySet());
		ArrayList<Integer> toRemove = new ArrayList<Integer>();

		//remove redundant terms to check	
		for(int i = 0; i < ancestorTermsSet.size(); i++)
		{
			Integer t = ancestorTermsSet.get(i);
			Set<Integer> s = ancestorMap.get(t).keySet();

			if(s == null)
				continue;

			for(int j = i + 1; j < ancestorTermsSet.size(); j++)
			{	
				Integer t2 = ancestorTermsSet.get(j);				
				Set<Integer> s2 = ancestorMap.get(t2).keySet();

				if(s2 == null)
					continue;

				if(s.size() <= s2.size() && s2.containsAll(s))
				{
					toRemove.add(t);
					break;
				}
			}	
		}

		//check any redundant term is a mapping or disjoint term
		toRemove.removeAll(originalList);
		Set<Integer> disjSet = rels.getDisjoint();
		toRemove.removeAll(disjSet);

		//remove redundant
		checklist.removeAll(toRemove);
		classList.removeAll(toRemove);
		for(Integer r : toRemove)
		{
			ancestorMap.remove(r);			
			for(Integer a: ancestorMap.keySet())
			{
				ancestorMap.get(a).remove(r);
			}	
		}

		//count checklist and ancestormap		
		System.out.println("AncestorMap:" + ancestorMap.keySet().size());
		System.out.println("Checklist Size:" + (checklist.size()));

	}

	//Methods for building all conflicting sets of mappings
	/**
	 * builds the (non-minimal) set of conflicting sets of mappings given the checklist of terms.
	 * Uses parallel method.
	 * @param checklist: a set of terms to search for conflicting sets.
	 * @returns a set of terms that were not considered in this build because of timeout.
	 * TODO: check isCancelled
	 */
	private Vector<Integer> builtConflictingSetsInParallel(Vector<Integer> checklist)
	{
		//terms of which conflicting sets were already computed
		Vector<Integer> doneTerms = new Vector<Integer>();
		//terms of which the computation was cancelled due to timeout
		Vector<Integer> cancelledTerms = new Vector<Integer>();		
		//building array of parallel tasks for each class of the checklist
		ArrayList<BuiltConflictingSetsTerm> tasks = new ArrayList<BuiltConflictingSetsTerm>();
		for(Integer cls: checklist)
		{
			tasks.add(new BuiltConflictingSetsTerm(cls));
		}

		//setting thread pool and execute tasks in parallel
		ExecutorService executor = Executors.newFixedThreadPool(maxNumberThreads);
		try {
			List<Future<Integer>> future = executor.invokeAll(tasks, timeOut, timeUnit);
			executor.shutdown();

			for(Future<Integer> f: future){

				/*
					try {
						f.get(10, TimeUnit.SECONDS);
					} catch (TimeoutException e) {
						System.out.println("CANCELLED!");
					}
				 */

				if(!f.isCancelled())
				{
					Integer fs = (Integer) f.get();
					doneTerms.add(fs);
				}else{
					//System.out.println("CANCELLED!");
				}

			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		} 

		//checking which terms were cancelled (could be done be calling isCancelled?)
		for(Integer term: checklist){		
			if(!doneTerms.contains(term)){
				cancelledTerms.add(term);
			}
		}

		return cancelledTerms;	
	}	


	int termsDone = 0;
	int lastPerc = -1;
	private synchronized void printStatus(){
		termsDone ++;
		int perc = (termsDone*100/checklist.size());
		if(perc != lastPerc && perc%10==0)
		{
			System.out.print(perc + "% ");
			lastPerc = perc;
			if(perc == 100)
				System.out.println("");
		}

	}


	/**
	 * Implements a callable version of builtConflictingSetsTerm for a given term.
	 */
	private class BuiltConflictingSetsTerm implements Callable<Integer> {
		private Integer term;

		BuiltConflictingSetsTerm(Integer t)
		{
			this.term = t;
		}

		@Override
		public Integer call()
		{	
			builtConflictingSets(term);
			printStatus();
			return term;
		}                
	}


	/**
	 * builds the set of all conflicting sets of mappings for a given term.
	 * Performs a depth first search. It takes care of cycles.
	 * @param term: a term
	 * @ensures updates getConflictMapComputed(term)
	 */
	private void builtConflictingSets(Integer term)
	{		
		//check if the term was already computed
		if(getConflictMapComputed(term)!=null)
			return;

		//current map of conflicting sets for each term
		HashMap<Integer,HashMap<Integer,Vector<Vector<Mapping>>>> conflictsMap 
		= new HashMap<Integer,HashMap<Integer,Vector<Vector<Mapping>>>>();
		//current set of terms in the search branch
		ArrayList<Integer> conflictsMapCurrent = new ArrayList<Integer>();
		//current set of terms in a cycle
		HashSet<Integer> cyclesTerms = new HashSet<Integer>();

		//perform search
		builtConflictsMaps(term, conflictsMap, conflictsMapCurrent, cyclesTerms,0);

		//in case there are cycles
		while(!cyclesTerms.isEmpty())
		{
			conflictsMap.clear();
			conflictsMapCurrent.clear();
			//get one term
			Integer cycleTerm = cyclesTerms.iterator().next();
			cyclesTerms.remove(cycleTerm);
			//perform search for the term in cycle
			builtConflictsMaps(cycleTerm, conflictsMap, conflictsMapCurrent, cyclesTerms,0);
		}
	}


	/**
	 * Performs a depth first search in the structure starting in a given term
	 * It considers the existence of cycles. (main method) 
	 * @param term - a term
	 * @param conflictsmap - HashMap that stores the conflicting sets of mappings already computed for a term.
	 * @param currentconflictsmap - List of terms in the current search stack.
	 * @param cycles - a list of terms that are in a cycle
	 * �
	 * @return A set of terms that need to be checked afterwards
	 */
	private  HashSet<Integer> builtConflictsMaps(Integer term, 
			HashMap<Integer,HashMap<Integer,Vector<Vector<Mapping>>>> conflictsmap, 
			ArrayList<Integer> currentconflictsmap, HashSet<Integer> Cycles, int depth)
			{
		HashSet<Integer> result = new HashSet<Integer>();

		//check if the term was already computed.
		if(getConflictMapComputed(term) != null)
		{
			Cycles.remove(term);
			return result;
		}
		//check if the term was considered in the current search stack => it is a cycle.
		else if(currentconflictsmap.contains(term))
		{			
			Cycles.add(term);
			//it is a cycle, it needs to be checked afterwards.
			result.add(term);
			return result;	
		}

		//check if the term was already partly checked.
		if(!conflictsmap.containsKey(term))
		{
			//it needs to be added to conflictmap.
			HashMap<Integer,Vector<Vector<Mapping>>> c = new HashMap<Integer,Vector<Vector<Mapping>>>();
			//lets add the disjoints terms that are ancestors of this term.
			for(Integer a: getDisjointAncestors(term)){
				Vector<Vector<Mapping>> b = new Vector<Vector<Mapping>>();
				b.add(new Vector<Mapping>());
				c.put(a, b);
			}
			conflictsmap.put(term, c);
		}


		if(depth > maxConflictSetSize)
			return result;

		//term is added the current search stack
		currentconflictsmap.add(term);


		//get the conflicts maps from ancestors
		//term can't be remove from the set because of threads
		Vector<Integer> mappedAncestors = getMappedAncestors(term);

		//search in all ancestors of the term		
		for(Integer a: mappedAncestors)
		{	
			HashSet<Integer> r = builtConflictsMaps(a, conflictsmap, currentconflictsmap, Cycles, depth);
			//add all terms not computed to result
			r.remove(term);
			result.addAll(r);			
			//if the ancestor was not computed then it is because it belongs to a cycle.
			if(getConflictMapComputed(a) == null)
			{
				Cycles.add(a);
			}
		}

		//get the conflicts maps from mappings
		//attention term can't be remove from the set because of threads
		Vector<Integer> mappedTerms = new Vector<Integer>(getMappedTerms(term));
		mappedTerms.remove(term);

		//search in all mapped terms of the term	
		for(Integer m: mappedTerms)
		{
			HashSet<Integer> r = builtConflictsMaps(m, conflictsmap, currentconflictsmap, Cycles, depth+1);
			//add all mapped terms not computed to result
			r.remove(term);
			result.addAll(r);
			//if the mapped term was not computed then it is because it belongs to a cycle.
			if(getConflictMapComputed(m) == null)
			{
				Cycles.add(m);
			}
		}



		Set<Integer> ds = null;
		synchronized(this){
			ds = rels.getDisjoint();
		}


		//get the current map of conflicts sets for this term
		HashMap<Integer, Vector<Vector<Mapping>>> confMapTerm = conflictsmap.get(term);

		//check if the term or its ancestors are part of any disjoint => add them to the current conflict map
		//loop every term of disjoint set.
		for(Integer d: ds)
		{	
			//the current term part of disjoint
			if(d == term)
			{
				Vector<Mapping> vm = new Vector<Mapping>();
				Vector<Vector<Mapping>> vvm = new Vector<Vector<Mapping>>();
				vvm.add(vm);
				//add a empty list of mappings to conflict map for this term
				confMapTerm.put(term, vvm);
				continue;
			}

			//check path of mappings from the ancestors of term to d
			for(Integer a: mappedAncestors)
			{				
				//get the computed conflicting maps generated from a 
				HashMap<Integer,Vector<Vector<Mapping>>> pathcomputed = getConflictMapComputed(a);
				Vector<Vector<Mapping>> path;

				if(pathcomputed!=null){
					path = pathcomputed.get(d); //path of mappings from a to d (computed)
				}else{
					path = conflictsmap.get(a).get(d); //path of mappings from a to d (current)
				}

				//if there are paths from a to d then add them to the current map for term
				if(path != null)
				{
					if(confMapTerm.containsKey(d))
					{
						//add new path from term to d
						confMapTerm.get(d).addAll(path);
					}else
					{
						//add first path from term to d
						confMapTerm.put(d, new Vector<Vector<Mapping>>(path));
					}	
				}
			}//end for ancestors

			//if it was found any path from term to d then calculates its minimal subset and sets is
			//not necessary
			if(confMapTerm.containsKey(d))
			{
				Vector<Vector<Mapping>> minimal = null;
				synchronized(this){
					minimal = minimalConflictSets(confMapTerm.get(d));
				}
				confMapTerm.put(d, minimal);
			}

			HashMap<Integer,Vector<Vector<Mapping>>> newPathMap = new HashMap<Integer,Vector<Vector<Mapping>>>();

			//check path of mappings from the mappings of term to d (same as above)
			//it needs to add the current mapping to the path
			for(int i = 0; i < mappedTerms.size(); i++)
			{
				Integer m = mappedTerms.get(i);

				HashMap<Integer,Vector<Vector<Mapping>>> pathcomputed = getConflictMapComputed(m);
				Vector<Vector<Mapping>> path;

				if(pathcomputed != null)
				{
					path = pathcomputed.get(d); //path of mappings from m to d (finished)
				}else{
					path = conflictsmap.get(m).get(d); //path of mappings from m to d (current)
				}

				if(path != null)
				{
					//get mapping object from term to m
					Mapping map = null;
					synchronized(this){
						map = align.get(getMapIndex(term, m));
					}

					//create new path of mappings
					Vector<Vector<Mapping>> newPath = new Vector<Vector<Mapping>>();

					//add map to each path 
					for(Vector<Mapping> vm: path)
					{
						Vector<Mapping> vmn = new Vector<Mapping>(vm);
						//add map to the path
						if(!vmn.contains(map))
							vmn.add(map);
						newPath.add(vmn);
					}

					//adds to newpathmap
					if(newPathMap.get(i) == null)
					{
						newPathMap.put(i, newPath);//adds this mapping to the new path
					}
					else
					{
						synchronized(this){
							newPath.addAll(newPathMap.get(i));
							newPathMap.put(i, minimalConflictSets(newPath));//adds this mapping to the new path
						}

					}

				}
			}//end for mappedTerms

			//adds &  updates the current (minimal) paths of term to d to conflictsMap
			for(int i=0;i<mappedTerms.size(); i++)
			{
				Vector<Vector<Mapping>> path = newPathMap.get(i);

				if(path != null)
				{					
					if(confMapTerm.get(d) == null)
					{
						confMapTerm.put(d, path);
					}else
					{
						path.addAll(confMapTerm.get(d));	
						Vector<Vector<Mapping>> minimal = null;
						synchronized(this){
							minimal = minimalConflictSets(path);
						}
						confMapTerm.put(d, minimal);
					}
				}				
			}//end for mappedTerms (minimal)
		}//end for disjointSet

		//check if all search paths were computed for this term:
		// 1) if term is the first in search stack; or,
		// 2) the result is empty (no other terms need to be computed)
		if(currentconflictsmap.get(0).compareTo(term) == 0 || result.isEmpty()){
			//put term in computed maps
			putConflictMapComputed(term, confMapTerm);
			Cycles.remove(term);
		}

		//term was already searched. it is removed from stack.
		currentconflictsmap.remove(term);
		return result;
			}


	/**
	 * synchronized method to get terms already searched for and its mappings paths to a disjoint term. 
	 * @param term - a term
	 * @return the hashmap with all mapping paths from term to a disjoint term 
	 */
	private synchronized HashMap<Integer,Vector<Vector<Mapping>>> getConflictMapComputed(Integer term)
	{
		return conflictsMapComputed.get(term);
	} 


	/**
	 * synchronized method to puts terms already searched for and its mappings paths to a disjoint term. 
	 * @param term - a term
	 */
	private synchronized void putConflictMapComputed(Integer term, HashMap<Integer,Vector<Vector<Mapping>>> confmap)
	{
		if(getConflictMapComputed(term) == null){
			conflictsMapComputed.put(term, confmap);
		}
	} 


	// Methods for building conflicting sets and ensuring minimality

	/**
	 * Computes (paralled mode) minimal conflicting sets of mappings from each term in input
	 * @param checklist: a list of term
	 * @ensures: currentConflictSets is updated with the current conflicting sets of mappings wrt input
	 */
	private void builtMinimalConflictMapsParallel(Vector<Integer> checklist)
	{
		try
		{
			//1) Computes all (minimal) conflicting sets of mappings for each term of of checklist
			ArrayList<computeConflictSetTermTask> tasks = new ArrayList<computeConflictSetTermTask>();	
			for(Integer m: checklist)
				tasks.add(new computeConflictSetTermTask(m));
			List<Future<Vector<Vector<Mapping>>>> results;
			ExecutorService executor1 = Executors.newFixedThreadPool(maxNumberThreads);
			//result of this task => a list of minimal conflicts sets lists 
			results = executor1.invokeAll(tasks);
			executor1.shutdown();

			//2) Computes the global (all terms) minimal set of conflicting set of mappings.
			//it applies divide to conquer technique (blocks) given the number of cores avaliable
			ArrayList<computeMinimalConflictSetBlockTask> tasks2 = new ArrayList<computeMinimalConflictSetBlockTask>();
			ExecutorService executor2;
			//number of blocks (minimum 2 blocks are created)

			int blocks = (int) Math.min(results.size()/2,maxNumberThreads);
			//while we have more than 1 block... we use all cores avaliable. 
			while(blocks>1)
			{	
				//computes the minimal set of each block
				tasks2.clear();
				tasks2 = new ArrayList<computeMinimalConflictSetBlockTask>();
				for(int i=0;i<blocks;i++)
					tasks2.add(new computeMinimalConflictSetBlockTask(results,i,blocks));
				executor2 = Executors.newFixedThreadPool(maxNumberThreads);
				results = executor2.invokeAll(tasks2);
				executor2.shutdown();
				//computes number of blocks again.
				blocks = (int) Math.min(results.size()/2,maxNumberThreads);							
			}

			//final block
			tasks2.clear();
			tasks2 = new ArrayList<computeMinimalConflictSetBlockTask>();
			for(int i=0;i<blocks;i++)
				tasks2.add(new computeMinimalConflictSetBlockTask(results,i,blocks));
			ExecutorService executor3 = Executors.newFixedThreadPool(maxNumberThreads);
			results = executor3.invokeAll(tasks2);
			executor3.shutdown();
			//updates the current set of conflicting sets of mappings
			currentConflictSets = results.get(0).get();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}


	/**
	 * Callable class that computes the conflict sets of mappings for a given term
	 * TODO - Mudar para a versao java 1.7
	 */
	private class computeConflictSetTermTask implements Callable<Vector<Vector<Mapping>>>
	{
		private Integer term;
		computeConflictSetTermTask(Integer t)
		{
			this.term = t;
		}

		@Override
		public Vector<Vector<Mapping>> call()
		{
			Vector<Vector<Mapping>> cm = null;
			try{
				cm = computeMinimalConflictSetTerm(term);
			}catch(Exception e){
				e.printStackTrace();
			}
			return cm;
		}                
	}


	/**
	 * builds all (minimal) the conflicts sets for a term
	 * @param checkterm - a class
	 * @return returns a list of minimal conflicting lists of mappings wrt checkterm
	 * TODO: multiple disjoints
	 */
	private Vector<Vector<Mapping>> computeMinimalConflictSetTerm(Integer term)
	{
		//all conflict sets for this term
		HashMap<Integer, Vector<Vector<Mapping>>> conflictsMap = conflictsMapComputed.get(term);
		Vector<Vector<Mapping>> result = new Vector<Vector<Mapping>>();

		if(conflictsMap==null)
		{
			return result;
		}	

		//get all disjoint term that is term reached
		Vector<Integer> disjointTerms = new Vector<Integer>(conflictsMap.keySet());

		//loops every disjoint term
		for(Integer d: disjointTerms)
		{
			for(Integer d2: rels.getDisjoint(d))
			{
				//check if d and d2 are disjoint -- checks its index for redundancy
				if(disjointTerms.contains(d2) && d > d2)
				{
					//if it is a disjoint, joins their mapping paths and computes minimal sets.
					Vector<Vector<Mapping>> joint = joinMappingPaths(conflictsMap.get(d), conflictsMap.get(d2));
					Vector<Vector<Mapping>> minimal_joint = minimalConflictSets(joint);					
					result = minimalConflictSets(minimal_joint,result);
				}
			}
		}

		//removes empty sets
		result = removeNulls(result);
		
		/*
		for(Vector<Mapping> v : result)
			if(v.size() == 0){
				System.out.println("-empty-->" + term);
				System.exit(0);
			}
		*/
		
		return removeNulls(result);
	}


	/**
	 * Joins two to list of list of mappings (context: it builds conflicting sets)
	 * @param path1 - mapping path
	 * @param path2 - mapping path
	 * @result a list of lists of mappings
	 * TODO Not efficient -- it doesn't check for all duplicates.
	 */
	private Vector<Vector<Mapping>> joinMappingPaths(Vector<Vector<Mapping>> path1, Vector<Vector<Mapping>> path2)
	{
		Vector<Vector<Mapping>> result = new Vector<Vector<Mapping>>();

		for(Vector<Mapping> vm: path1)
		{
			for(Vector<Mapping> vm2: path2)
			{
				HashSet<Mapping> addh = new HashSet<Mapping>(vm);
				// removes duplicate mappings
				addh.addAll(vm2);
				// builds a new conflicting set
				Vector<Mapping> newvm = new Vector<Mapping>(addh);
				result.add(newvm);			
			}
		}

		return result;
	}


	/**
	 * Callable class that computes the minimal conflict sets of a block of results
	 * TODO - Mudar para a versao java 1.7
	 */
	private class computeMinimalConflictSetBlockTask implements Callable<Vector<Vector<Mapping>>> {
		List<Future<Vector<Vector<Mapping>>>> results;
		int part;
		int total;

		computeMinimalConflictSetBlockTask(List<Future<Vector<Vector<Mapping>>>> results, int part, int total) {
			this.results = results;
			this.part = part;
			this.total = total;            
		}

		@Override
		public Vector<Vector<Mapping>> call() {
			try {
				return computeMinimalConflictSetBlock(results,  part,  total);
			} catch (InterruptedException e) 
			{
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
			return null;
		}
	}


	/**
	 * Computes the minimal set of conflicting sets of mappings for a given block of results
	 * @param results - input list of conflicting sets of mappings
	 * @param part - a part to compute minimization
	 * @param total - number of blocks
	 * @return A minimal set of conflicting set of mappings
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	private Vector<Vector<Mapping>> computeMinimalConflictSetBlock(List<Future<Vector<Vector<Mapping>>>> results, int part, int total) throws InterruptedException, ExecutionException
	{
		//initialize variable -- getting the right block of results
		int size = results.size();
		Double slot = Math.max(Math.ceil(size/(total*1.0)),1);
		Double start = part*slot;
		Double stop = Math.min((part+1)*slot, size);
		//result is initialize with the first set of block
		Vector<Vector<Mapping>> result = results.get(start.intValue()).get();
		//hashmap that stores which sets does each mapping belongs to. 
		HashMap<Mapping, HashSet<Integer>> help = new HashMap<Mapping, HashSet<Integer>>();

		//because result is initialized with first set of block
		for(int i= start.intValue()+1; i<stop; i++)
		{
			result = joinMinimalConflictSets(results.get(i).get(), result, help); 
		}

		//removes empty sets
		return removeNulls(result);
	}



	//ambas as listas sao minimas


	/***
	 * Computes the minimal set of conflict sets (no supersets), i.e.
	 * there is no set that is a subset of another set.
	 * @param tojoinminimal - a minimal set of conflicting sets of mappings to join
	 * @param minimal - a minimal set of conflicting sets of mappings
	 * @param helpmap - hashmap that stores info about mappings
	 * @return a minimal joint set of the input sets of conflict sets
	 * @TODO - check helpmap - does return minimal again. change return type.
	 */
	private static Vector<Vector<Mapping>> joinMinimalConflictSets(Vector<Vector<Mapping>> tojoinminimal, Vector<Vector<Mapping>> minimal,HashMap<Mapping, HashSet<Integer>> helpmap)
	{

		//adds mapping set to hash (check it, it seems not right, result should be already in helpmap)
		if(helpmap.size()==0)
			for(int i=0; i<minimal.size();i++)
				addMappingHelp(helpmap, minimal.get(i), i);

		//checks if a set is minimal
		for(int i=0; i<tojoinminimal.size();i++)
		{
			Vector<Mapping> current = tojoinminimal.get(i);
			boolean isMinimal = true;
			boolean added = false;
			HashSet<Integer> check = new HashSet<Integer>();
			HashSet<Integer> checkback = new HashSet<Integer>();

			CheckMappings:
				for(Mapping m: current)
				{
					//use helpmap to check if it is minimal
					if(helpmap.containsKey(m))
					{
						check = new HashSet<Integer>(helpmap.get(m));
						check.removeAll(checkback);
						checkback = check;
						int asize = current.size();
						for(int j: check)
						{
							if(j==i)
								continue;
							Vector<Mapping> compare = minimal.get(j);
							if(compare==null)
								continue;
							int bsize = compare.size();
							boolean acontainsAllb = asize >= bsize && current.containsAll(compare);
							boolean bcontainsAlla = bsize >= asize && compare.containsAll(current);
							if(!acontainsAllb && !bcontainsAlla)
								continue;
							if(acontainsAllb && bcontainsAlla)
							{
								isMinimal = false;
								break CheckMappings;
							}
							if(acontainsAllb && !bcontainsAlla)
							{		
								isMinimal = false;
								break CheckMappings;
							}
							if(!acontainsAllb && bcontainsAlla)
							{	//it is a new minimal
								if(!added)
								{					
									minimal.add(current);
									addMappingHelp(helpmap, current, minimal.size()-1);
									added = true;
								}
								minimal.set(j, null);
							}
						}//end for	
					}//end if
				}// end for mapping a

			if(isMinimal)
			{
				//adds map to help map
				minimal.add(current);
				addMappingHelp(helpmap, current, minimal.size()-1);
			}
		}
		return minimal;
	}

	/**
	 * (auxiliary method) Adds a set of mappings to the hashmap (mappings,set of sets that belong to)
	 * @param helpmap - hashmap
	 * @param mappings - list of mappings
	 * @param i - number of the set
	 */
	private static void addMappingHelp(HashMap<Mapping, HashSet<Integer>> helpmap, Vector<Mapping> mappings, int setnumber)
	{
		for(Mapping m: mappings)
		{
			if(!helpmap.containsKey(m)){
				HashSet<Integer> n = new HashSet<Integer>();
				n.add(setnumber);
				helpmap.put(m, n);
			}
			else
				helpmap.get(m).add(setnumber);
		}
	}


	/**
	 * Computes the minimal set of conflict sets (no supersets), i.e.
	 * there is no set that is a subset of another set.
	 * @param notminimal - A set of conflicting sets of mappings
	 * @param minimal - A set of conflicting sets of mappings (minimal)
	 * @result The set of minimal conflicting sets of mappings
	 * @requires minimalset is a minimal set of conflicting sets of mappings 
	 * @TODO not efficient enougth - o factor de ser not minimal n�o aparece ajudar.
	 */
	private static Vector<Vector<Mapping>> minimalConflictSets(Vector<Vector<Mapping>> notminimal, Vector<Vector<Mapping>> minimal)
	{
		Vector<Vector<Mapping>> result = new Vector<Vector<Mapping>>(minimal);

		for(int i=0; i<notminimal.size();i++)
		{
			Vector<Mapping> current = notminimal.get(i);
			boolean isMinimal = true;
			boolean added = false;
			int currentSize = current.size();

			for(int j = 0; j<result.size();j++)
			{
				Vector<Mapping> compare = result.get(j);
				int compareSize = compare.size();
				boolean currentContains = currentSize>=compareSize && current.containsAll(compare);
				boolean compareContains = compareSize>=currentSize && compare.containsAll(current);

				if(!currentContains && !compareContains)
				{
					continue;
				}

				if(currentContains && compareContains)
				{
					isMinimal = false;
					break;
				}

				if(currentContains && !compareContains)
				{		
					isMinimal = false;
					break;
				}

				if(!currentContains && compareContains)
				{	//it is a new minimal
					if(!added)
					{
						result.add(current);
						added = true;
					}

					result.remove(j);
					j--;
				}
			}//end for	

			if(isMinimal)
			{
				result.add(current);
			}
		}

		return result;
	}


	/**
	 * Computes the minimal set of conflict sets (no supersets), i.e.
	 * there is no set that is a subset of another set.
	 * @param conflictsets - A set of conflicting sets of mappings
	 * @result The set of minimal conflicting sets of mappings
	 */
	private static Vector<Vector<Mapping>> minimalConflictSets(Vector<Vector<Mapping>> conflictsets)
	{
		//set of minimal conflicting sets (already checked)
		Vector<Vector<Mapping>> minimalSet = new Vector<Vector<Mapping>>();
		//set of non-minimal conflicting sets (already checked)
		HashSet<Integer> notMinimal = new HashSet<Integer>();

		//loops every conflicting set
		for(int i=0; i<conflictsets.size(); i++)
		{
			//checks if it is already non-minimal
			if(notMinimal.contains(i))
				continue;

			boolean isMinimal = true;
			Vector<Mapping> current = conflictsets.get(i);
			int currentSize = current.size();

			for(int j = i+1; j<conflictsets.size(); j++)
			{
				//checks if it is already non-minimal
				if(notMinimal.contains(j))
					continue;

				Vector<Mapping> compare = conflictsets.get(j);
				int compareSize= compare.size();
				boolean currentContains = currentSize>=compareSize && current.containsAll(compare);
				boolean compareContains = compareSize>=currentSize && compare.containsAll(current);

				if(!currentContains && !compareContains)
				{
					continue;
				}
				else if(currentContains && compareContains)
				{
					// removes 'duplicates'
					isMinimal = false; 
					break;
				}
				else if(currentContains && !compareContains)
				{		
					//current is not minimal
					isMinimal = false;
					break;
				}
				else if(!currentContains && compareContains)
				{	
					//compare is not minimal
					notMinimal.add(j);
				}
			}//end for	

			if(isMinimal)
			{
				minimalSet.add(current);
			}
			else
			{
				notMinimal.add(i);
			}
		}
		return minimalSet;
	}	

	// other methods

	/**
	 * Removes nulls values of the list
	 * @param vector the list
	 * @result the list without null values.
	 */
	private Vector<Vector<Mapping>> removeNulls(Vector<Vector<Mapping>> vector)
	{
		for(int j = 0; j<vector.size();j++)
		{
			if(vector.get(j)== null){
				vector.remove(j);
				j--;
			}
		}
		return vector;
	}


	//Returns all 'is_a' disjoint ancestors of a given term considering
	//only paths within its Ontology
	private Vector<Integer> getMappedAncestors(Integer term)
	{
		Vector<Integer> mappedAncestors = new Vector<Integer>(1,1);
		Vector<Integer> ancestors = getSuperClasses(term);
		ancestors.remove(term);

		//TODO: melhorar
		Set<Integer> aSources = align.getSources();
		Set<Integer> aTargets = align.getTargets();
		for(Integer v: ancestors)
		{
			if(aSources.contains(v) || aTargets.contains(v)){
				mappedAncestors.add(v);
			}
		}

		return mappedAncestors;
	}


	//Returns all 'is_a' disjoint ancestors of a given term considering
	//only paths within its Ontology
	private Vector<Integer> getDisjointAncestors(Integer term)
	{
		Vector<Integer> disjointAncestors = new Vector<Integer>(1,1);
		for(Integer a: getSuperClasses(term))
			if(rels.hasDisjoint(a))
				disjointAncestors.add(a);
		return disjointAncestors;
	}



	//Returns all 'is_a' ancestors of a given term considering
	//only paths within its Ontology
	private Vector<Integer> getSuperClasses(Integer term)
	{
		Vector<Integer> ancestors = new Vector<Integer>(1,1);
		ancestors.add(term);
		if(ancestorMap.contains(term))
			ancestors.addAll(ancestorMap.get(term).keySet());
		return ancestors;
	}
	

	//Returns all 'is_a' ancestors of a given term in both Ontologies
	//considering all Mappings between the Ontologies
	private Vector<Integer> getExtendedSuperClasses(Integer term)
	{
		Vector<Integer> ancestors = getSuperClasses(term);
		int index;
		int size = 0;
		while(size != ancestors.size())
		{
			index = size;
			size = ancestors.size();
			for(int i = index; i < size; i++)
			{
				Integer a = ancestors.get(i);
				Vector<Integer> maps = getMappedTerms(a);
				for(Integer m: maps)
				{
					Vector<Integer> newAncs = getSuperClasses(m);
					for(Integer s : newAncs)
						if(!ancestors.contains(s))
							ancestors.add(s);
				}
			}
		}
		return ancestors;
	}

	//Returns all terms mapped to the given term (equivalence or subclass)
	/**
	 * consideres equivalence and subclasse mappings (not superclass)
	 */
	public synchronized Vector<Integer> getMappedTerms(Integer term)
	{
		Vector<Integer> mappings = new Vector<Integer>(0,1);
		int id = term;
		//TODO melhorar -- n é preciso testar se está contido.
		if(align.getSources().contains(term))
		{
			Set<Integer> maps = align.getSourceMappings(id);
			for(Integer i : maps)
			{
				MappingRelation relation = align.getRelationship(id, i);
				if(relation.equals(MappingRelation.EQUIVALENCE) || relation.equals(MappingRelation.SUBCLASS))
					mappings.add(i);
			}
		}
		
		if(align.getTargets().contains(term))
		{
			Set<Integer> maps = align.getTargetMappings(id);
			for(Integer i : maps)
			{
				MappingRelation relation = align.getRelationship(i, id);
				if(relation.equals(MappingRelation.EQUIVALENCE) || relation.equals(MappingRelation.SUPERCLASS))
					mappings.add(i);
			}
		}
		return mappings;
	}

	//Returns the index of the mapping between two given terms
	private int getMapIndex(Integer s, Integer t)
	{
		int source = s, target = t;
		//TODO algo que diga se é source ou target
		if(align.getIndex(s, t) == -1)
		{
			source = t;
			target = s;
		}
		
		return align.getIndex(source, target);
	}

}