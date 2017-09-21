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
* Map of extended relationships of classes involved in disjoint clauses with  *
* mappings from a given Alignment, which supports repair of that Alignment.   *
*                                                                             *
* @authors Daniel Faria & Emanuel Santos                                      *
******************************************************************************/
package aml.filter;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import aml.AML;
import aml.util.Table2Set;
import aml.util.Table3List;
import aml.util.Table3Set;
import aml.match.Alignment;
import aml.match.Mapping;
import aml.ontology.RelationshipMap;
import aml.settings.MappingStatus;

public class RepairMap implements Iterable<Integer>
{
	
//Attributes
	
	private AML aml;
	private RelationshipMap rels;
	private Alignment a;
	//The list of classes that are relevant for coherence checking
	private HashSet<Integer> classList;
	//The list of classes that must be checked for coherence
	private HashSet<Integer> checkList;
	//The minimal map of ancestor relations of checkList classes
	//(checkList class Id, classList class Id, Path)
	private Table3List<Integer,Integer,Path> ancestorMap;
	//The length of ancestral paths to facilitate transitive closure
	//(checklist class Id, Path length, classList class Id)
	private Table3Set<Integer,Integer,Integer> pathLengths;
	//The number of paths to disjoint classes
	private int pathCount;
	//The list of conflict sets
	private Vector<Path> conflictSets;
	//The table of conflicts per mapping
	private Table2Set<Integer,Integer> conflictMappings;
	private Table2Set<Integer,Integer> mappingConflicts;
	//The available CPU threads
	private int threads;
	
//Constructors
	
	/**
	 * Constructs a new RepairMap
	 */
	public RepairMap()
	{
		aml = AML.getInstance();
		rels = aml.getRelationshipMap();
		//We use a clone of the alignment to avoid problems if the
		//order of the original alignment is altered
		a = new Alignment(aml.getAlignment());
		//Remove the FLAGGED status from all mappings that have it
		for(Mapping m : a)
			if(m.getStatus().equals(MappingStatus.FLAGGED))
				m.setStatus(MappingStatus.UNKNOWN);
		threads = Runtime.getRuntime().availableProcessors();
		init();
	}
	
//Public Methods
	
	/**
	 * @param index: the index of the Mapping to get
	 * @return the conflict sets that contain the given Mapping index
	 */
	public Set<Integer> getConflicts(int index)
	{
		return mappingConflicts.get(index);
	}
	
	/**
	 * @param m: the Mapping to get
	 * @return the list of Mappings in conflict with this Mapping
	 */
	public Vector<Mapping> getConflictMappings(Mapping m)
	{
		int index = a.getIndex(m.getSourceId(), m.getTargetId());
		Vector<Mapping> confs = new Vector<Mapping>();
		if(!mappingConflicts.contains(index))
			return confs;
		for(Integer i : mappingConflicts.get(index))
		{
			for(Integer j : conflictMappings.get(i))
			{
				if(j == index)
					continue;
				Mapping n = a.get(j);
				//Get the Mapping from the original alignment
				n = aml.getAlignment().get(n.getSourceId(), n.getTargetId());
				if(!confs.contains(n))
					confs.add(n);
			}
		}
		return confs;
	}
	
	/**
	 * @return the list of conflict sets of mappings
	 * in the form of indexes (as per the alignment
	 * to repair)
	 */
	public Vector<Path> getConflictSets()
	{
		return conflictSets;
	}
	
	/**
	 * @param m: the Mapping to search in the RepairMap
	 * @return the index of the Mapping in the RepairMap
	 */
	public int getIndex(Mapping m)
	{
		return a.getIndex(m.getSourceId(), m.getTargetId());
	}
	
	/**
	 * @param source: the id of the source class to search in the RepairMap
	 * @param target: the id of the target class to search in the RepairMap
	 * @return the index of the Mapping between source and target in
	 * the RepairMap
	 */
	public int getIndex(int source, int target)
	{
		return a.getIndex(source, target);
	}

	/**
	 * @param index: the index of the Mapping to get
	 * @return the Mapping at the given index
	 */
	public Mapping getMapping(int index)
	{
		Mapping m = a.get(index);
		return aml.getAlignment().get(m.getSourceId(), m.getTargetId());
	}
	
	/**
	 * @return whether the alignment is coherent
	 */
	public boolean isCoherent()
	{
		return conflictSets == null || conflictSets.size() == 0;
	}
	
	@Override
	public Iterator<Integer> iterator()
	{
		return mappingConflicts.keySet().iterator();
	}
	
	/**
	 * Sets a Mapping as incorrect and removes its conflicts from
	 * the RepairMap (but does not actually remove the Mapping from
	 * the Alignment)
	 * @param index: the index of the Mapping to remove
	 */
	public void remove(int index)
	{
		HashSet<Integer> conflicts = new HashSet<Integer>(mappingConflicts.get(index));
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
		mappingConflicts.remove(index);
		Mapping m = a.get(index);
		m.setStatus(MappingStatus.INCORRECT);
		aml.getAlignment().get(m.getSourceId(), m.getTargetId()).setStatus(MappingStatus.INCORRECT);
	}
	
	/**
	 * Saves the list of minimal conflict sets to a text file
	 * @param file: the path to the file where to save
	 * @throws FileNotFoundException if unable to create/open file
	 */
	public void saveConflictSets(String file) throws FileNotFoundException
	{
		PrintWriter outStream = new PrintWriter(new FileOutputStream(file));
		int id = 1;
		for(Path p : conflictSets)
		{
			outStream.println("Conflict Set " + id++ + ":");
			for(Integer i : p)
				outStream.println(a.get(i).toString());
		}
		outStream.close();
	}
	
//Private Methods
	
	//Builds the RepairMap
	private void init()
	{
		System.out.println("Building Repair Map");
		long globalTime = System.currentTimeMillis()/1000;
		//Initialize the data structures
		classList = new HashSet<Integer>();
		checkList = new HashSet<Integer>();
		ancestorMap = new Table3List<Integer,Integer,Path>();
		pathLengths = new Table3Set<Integer,Integer,Integer>();
		conflictSets = new Vector<Path>();
		
		//Build the classList, starting with the classes
		//involved in disjoint clauses
		classList.addAll(rels.getDisjoint());
		//If there aren't any, there is nothing else to do
		if(classList.size() == 0)
		{
			System.out.println("Nothing to repair!");
			return;
		}
		//Otherwise, add all classes involved in mappings
		for(Integer i : a.getSources())
			if(aml.getURIMap().isClass(i))
				classList.add(i);
		for(Integer i : a.getTargets())
			if(aml.getURIMap().isClass(i))
				classList.add(i);
		
		//Then build the checkList
		long localTime = System.currentTimeMillis()/1000;
		buildCheckList();
		System.out.println("Computed check list in " + 
				(System.currentTimeMillis()/1000-localTime) + " seconds");
		HashSet<Integer> t = new HashSet<Integer>(classList);
		t.addAll(checkList);
		System.out.println("Core fragments: " + t.size() + " classes");
		t.clear();
		System.out.println("Check list: " + checkList.size() + " classes to check");
		
		//Build the ancestorMap with transitive closure
		localTime = System.currentTimeMillis()/1000;
		buildAncestorMap();
		System.out.println("Computed ancestral paths in " + 
				(System.currentTimeMillis()/1000-localTime) + " seconds");
		System.out.println("Paths to process: " + pathCount);
		
		//And finally, get the list of conflict sets
		localTime = System.currentTimeMillis()/1000;
		buildConflictSets();
		System.out.println("Computed minimal conflict sets in " + 
				(System.currentTimeMillis()/1000-localTime) + " seconds");
		System.out.println("Sets of conflicting mappings: " + conflictSets.size());
		System.out.println("Repair Map finished in " +
				(System.currentTimeMillis()/1000-globalTime) + " seconds");
	}
	
	//Computes the list of classes that must be checked for coherence
	private void buildCheckList()
	{
		//Start with the descendants of classList classes that have
		//either 2+ parents with a classList class in their ancestral
		//line or are involved in a disjoint class and have 1+ parents
		HashSet<Integer> descList = new HashSet<Integer>();
		for(Integer i: classList)
		{
			//Get the subClasses of classList classes
			for(Integer j : rels.getSubClasses(i,false))
			{
				//Count their parents
				Set<Integer> pars = rels.getSuperClasses(j, true);
				//Check if they have a disjoint clause
				int hasDisjoint = 0;
				if(rels.hasDisjoint(j))
					hasDisjoint = 1;
				//Exclude those that don't have at least two parents
				//or a parent and a disjoint clause
				if(pars.size() + hasDisjoint < 2)
					continue;
				//Count the classList classes in the ancestral
				//line of each parent (or until two parents with
				//classList ancestors are found)
				int count = hasDisjoint;
				for(Integer k : pars)
				{
					if(classList.contains(k))
						count++;
					else
					{
						for(Integer l : rels.getSuperClasses(k, false))
						{
							if(classList.contains(l))
							{
								count++;
								break;
							}
						}
					}
					if(count > 1)
						break;
				}
				//Add those that have at least 2 classList
				//classes in their ancestral line
				if(count > 1)
					descList.add(j);
			}
		}
		//Filter out classes that have a descendant in the descList
		//or a mapped descendant
		HashSet<Integer> toRemove = new HashSet<Integer>();
		for(Integer i : descList)
		{
			for(Integer j : rels.getSubClasses(i, false))
			{
				if(descList.contains(j) || a.containsClass(j))
				{
					toRemove.add(i);
					break;
				}
			}
		}
		descList.removeAll(toRemove);
		//And those that have the same set or a subset of
		//classList classes in their ancestor line
		toRemove = new HashSet<Integer>();
		Vector<Integer> desc = new Vector<Integer>();
		Vector<Path> paths = new Vector<Path>();
		for(Integer i : descList)
		{
			//Put the classList ancestors in a path
			Path p = new Path();
			for(Integer j : rels.getSuperClasses(i,false))
				if(classList.contains(j))
					p.add(j);
			//Put the class itself in the path if it
			//is also in classList
			if(classList.contains(i))
				p.add(i);			
			
			boolean add = true;
			//Check if any of the selected classes
			for(int j = 0; j < desc.size() && add; j++)
			{
				//subsumes this class (if so, skip it)
				if(paths.get(j).contains(p))
					add = false;
				//is subsumed by this class (if so,
				//remove the latter and proceed)
				else if(p.contains(paths.get(j)))
				{
					desc.remove(j);
					paths.remove(j);
					j--;
				}
			}
			//If no redundancy was found, add the class
			//to the list of selected classes
			if(add)
			{
				desc.add(i);
				paths.add(p);
			}
		}
		//Add all selected classes to the checkList
		checkList.addAll(desc);
		//Now get the list of all mapped classes that are
		//involved in two mappings or have an ancestral
		//path to a mapped class, from only one side
		HashSet<Integer> mapList = new HashSet<Integer>();
		for(Mapping m : a)
		{
			int source = m.getSourceId();
			int target = m.getTargetId();
			if(!aml.getURIMap().isClass(source) || !aml.getURIMap().isClass(target))
				continue;
			//Check if there is no descendant in the checkList
			boolean isRedundant = false;
			HashSet<Integer> descendants = new HashSet<Integer>(rels.getSubClasses(source, false));
			descendants.addAll(rels.getSubClasses(target, false));
			for(Integer i : descendants)
			{
				if(checkList.contains(i))
				{
					isRedundant = true;
					break;
				}
			}
			if(isRedundant)
				continue;
			//Count the mappings of both source and target classes
			int sourceCount = a.getSourceMappings(source).size();
			int targetCount = a.getTargetMappings(target).size();
			//If the target class has more mappings than the source
			//class (which implies it has at least 2 mappings) add it
			if(targetCount > sourceCount)
				mapList.add(target);
			//If the opposite is true, add the target
			else if(sourceCount > targetCount || sourceCount > 1)
				mapList.add(source);
			//Otherwise, check for mapped ancestors on both sides
			else
			{
				for(Integer j : rels.getSuperClasses(source, false))
					if(a.containsSource(j))
						sourceCount++;
				for(Integer j : rels.getSuperClasses(target, false))
					if(a.containsTarget(j))
						targetCount++;
				if(sourceCount > 1 && targetCount < sourceCount)
					mapList.add(source);
				else if(targetCount > 1)
					mapList.add(target);
			}
		}
		toRemove = new HashSet<Integer>();
		for(Integer i : mapList)
		{
			for(Integer j : rels.getSubClasses(i, false))
			{
				if(mapList.contains(j))
				{
					toRemove.add(i);
					break;
				}
			}
		}
		mapList.removeAll(toRemove);
		//Finally, add the mapList to the checkList
		checkList.addAll(mapList);
	}

	//Builds the map of ancestral relations between all classes
	//in the checkList and all classes in the classList, with
	//(breadth first) transitive closure
	private void buildAncestorMap()
	{
		//First get the "direct" relations between checkList
		//and classList classes, which are present in the
		//RelationshipMap, plus the relations through direct
		//mappings of checkList classes
		for(Integer i : checkList)
		{
			//Direct relations
			Set<Integer> ancs = rels.getSuperClasses(i,false);
			for(Integer j : ancs)
				if(classList.contains(j))
					addRelation(i, j, new Path());
			//Mappings
			Set<Integer> maps = a.getMappingsBidirectional(i);
			for(Integer j : maps)
			{
				//Get both the mapping and its ancestors
				int index = a.getIndexBidirectional(i, j);
				HashSet<Integer> newAncestors = new HashSet<Integer>(rels.getSuperClasses(j,false));
				newAncestors.add(j);
				//And add them
				for(Integer m : newAncestors)
					if(classList.contains(m))
						addRelation(i,m,new Path(index));
			}
		}
		//Then add paths iteratively by extending paths with new
		//mappings, stopping when the ancestorMap stops growing
		int size = 0;
		for(int i = 0; size < ancestorMap.size(); i++)
		{
			size = ancestorMap.size();
			//For each class in the checkList
			for(Integer j : checkList)
			{
				//If it has ancestors through paths with i mappings
				if(!pathLengths.contains(j, i))
					continue;
				//We get those ancestors
				HashSet<Integer> ancestors = new HashSet<Integer>(pathLengths.get(j,i));
				//For each such ancestor
				for(Integer k : ancestors)
				{
					//Cycle check 1 (make sure ancestor != self)
					if(k == j)
						continue;
					//Get the paths between the class and its ancestor
					HashSet<Path> paths = new HashSet<Path>();
					for(Path p : ancestorMap.get(j, k))
						if(p.size() == i)
							paths.add(p);
					//Get the ancestor's mappings
					Set<Integer> maps = a.getMappingsBidirectional(k);
					//And for each mapping
					for(Integer l : maps)
					{
						//Cycle check 2 (make sure mapping != self)
						if(l == j)
							continue;
						//We get its ancestors
						int index = a.getIndexBidirectional(k, l);
						HashSet<Integer> newAncestors = new HashSet<Integer>(rels.getSuperClasses(l,false));
						//Plus the mapping itself
						newAncestors.add(l);
						//Now we must increment all paths between j and k
						for(Path p : paths)
						{
							//Cycle check 3 (make sure we don't go through the
							//same mapping twice)
							if(p.contains(index))
								continue;
							//We increment the path by adding the new mapping
							Path q = new Path(p);
							q.add(index);
							//And add a relationship between j and each descendant of
							//the new mapping (including the mapping itself) that is
							//on the checkList
							for(Integer m : newAncestors)
								//Cycle check 4 (make sure mapping descendant != self)
								if(classList.contains(m) && m != j)
									addRelation(j,m,q);
						}
					}
				}
			}
		}
		//Finally add relations between checkList classes and
		//themselves when they are involved in disjoint clauses
		//(to support the buildClassConflicts method)
		for(Integer i : checkList)
			if(rels.hasDisjoint(i))
				ancestorMap.add(i, i, new Path());
	}
	
	//Adds a relation to the ancestorMap (and pathLengths)
	private void addRelation(int child, int parent, Path p)
	{
		if(ancestorMap.contains(child,parent))
		{
			Vector<Path> paths = ancestorMap.get(child,parent);
			for(Path q : paths)
				if(p.contains(q))
					return;
		}
		ancestorMap.add(child,parent,p);
		pathLengths.add(child, p.size(), parent);
		if(rels.hasDisjoint(parent))
			pathCount++;
	}
	
	//Builds the global minimal conflict sets for all checkList classes
	private void buildConflictSets()
	{
		//If there is only one CPU thread available, then process in series
		if(threads == 1)
		{
			//For each checkList class
			for(Integer i : checkList)
			{
				//Get its minimized conflicts
				Vector<Path> classConflicts = buildClassConflicts(i);
				//And add them to the conflictSets, minimizing upon addition
				for(Path p : classConflicts)
					addConflict(p,conflictSets);
			}
		}
		//Otherwise process in parallel
		else
		{
			//Create a task for each checkList class
			ArrayList<ClassConflicts> tasks = new ArrayList<ClassConflicts>();
			for(Integer i : checkList)
				tasks.add(new ClassConflicts(i));
			//Then execute all tasks using the available threads
	        List<Future<Vector<Path>>> results;
			ExecutorService exec = Executors.newFixedThreadPool(threads);
			try
			{
				results = exec.invokeAll(tasks);
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
		        results = new ArrayList<Future<Vector<Path>>>();
			}
			exec.shutdown();
			//Finally, combine all minimal class conflict sets
			Vector<Path> allConflicts = new Vector<Path>();
			for(Future<Vector<Path>> conf : results)
			{
				try
				{
					for(Path p : conf.get())
						allConflicts.add(p);
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
			//Sort them
			Collections.sort(allConflicts);
			//And turn them into the final minimal list of conflict sets
			for(Path p : allConflicts)
				addConflict(p,conflictSets);
		}
		//Now go through the conflict sets and link them to the mappings
		conflictMappings = new Table2Set<Integer,Integer>();
		mappingConflicts = new Table2Set<Integer,Integer>();
		for(int i = 0; i < conflictSets.size(); i++)
		{
			for(Integer j : conflictSets.get(i))
			{
				conflictMappings.add(i,j);
				mappingConflicts.add(j,i);
			}
		}
	}
	
	//Builds the minimal conflict sets for a given checkList class
	private Vector<Path> buildClassConflicts(int classId)
	{
		//First get all ancestors involved in disjoint clauses
		HashSet<Integer> disj = new HashSet<Integer>();
		for(Integer i : ancestorMap.keySet(classId))
			if(rels.hasDisjoint(i))
				disj.add(i);
		
		//Plus the class itself, if it has a disjoint clause
		if(rels.hasDisjoint(classId))
			disj.add(classId);	
		
		//Then test each pair of ancestors for disjointness
		Vector<Path> classConflicts = new Vector<Path>();
		for(Integer i : disj)
		{
			for(Integer j : rels.getDisjoint(i))
			{
				if(i > j || !disj.contains(j))
					continue;
				
				for(Path p : ancestorMap.get(classId, i))
				{
					for(Path q : ancestorMap.get(classId, j))
					{
						Path merged = new Path(p);
						merged.merge(q);
						//Adding the merged path to the list of classConflicts
						classConflicts.add(merged);
					}
				}
			}
		}
		//Then sort that list
		Collections.sort(classConflicts);
		//And turn it into a minimal list
		Vector<Path> minimalConflicts = new Vector<Path>();
		for(Path p : classConflicts)
			addConflict(p, minimalConflicts);
		return minimalConflicts;
	}
	
	//Adds a path to a list of conflict sets if it is a minimal path
	//(this only results in a minimal list of paths if paths are added
	//in order, after sorting)
	private void addConflict(Path p, Vector<Path> paths)
	{
		for(Path q : paths)
			if(p.contains(q))
				return;
		paths.add(p);
	}
	
	//Callable class for computing minimal conflict sets
	private class ClassConflicts implements Callable<Vector<Path>>
	{
		private int term;
		
		ClassConflicts(int t)
	    {
	        term = t;
	    }
	        
	    @Override
	    public Vector<Path> call()
	    {
       		return buildClassConflicts(term);
        }
	}
}