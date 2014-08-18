/*******************************************************************************
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
 * Map of extended relationships of classes involved in disjoint clauses with  *
 * mappings from a given Alignment, which supports repair of that Alignment.   *
 *                                                                             *
 * @authors Daniel Faria & Emanuel Santos                                      *
 * @date 12-08-2014                                                            *
 * @version 2.0                                                                *
 ******************************************************************************/
package aml.filter;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import aml.AML;
import aml.match.Alignment;
import aml.match.Mapping;
import aml.ontology.RelationshipMap;
import aml.util.Table3List;
import aml.util.Table3Set;

public class RepairMap
{
	
//Attributes
	
	//Link to the global relationship map
	RelationshipMap rels;
	//Link to the alignment to repair
	private Alignment a;
	//The list of classes that are relevant for coherence checking
	private HashSet<Integer> classList;
	//The list of classes that must be checked for coherence
	private HashSet<Integer> checkList;
	//The minimal map of ancestor relations of checkList classes
	private Table3List<Integer,Integer,Path> ancestorMap;
	//The length of ancestral paths (to facilitate transitive closure)
	private Table3Set<Integer,Integer,Integer> pathLengths;
	//The number of paths to disjoint classes
	private int pathCount;
	//The list of conflict sets
	private Vector<Path> conflictSets;
	
//Constructors
	
	/**
	 * Constructs a new RepairMap based on the RelationshipMap
	 * and the given Alignment to repair
	 * @param maps: the Alignment to repair
	 */
	public RepairMap(Alignment maps)
	{
		rels = AML.getInstance().getRelationshipMap();
		a = maps;
		init();
	}
	
//Public Methods
	
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
		return a.get(index);
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
		long time = System.currentTimeMillis()/1000;
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
		classList.addAll(a.getSources());
		classList.addAll(a.getTargets());
		//Then build the checkList
		buildCheckList();
		System.out.println("Computed check list: " + checkList.size()
				+ " classes to check");
		//Build the ancestorMap with transitive closure
		buildAncestorMap();
		System.out.println("Computed ancestral paths: " + pathCount +
				" paths to process");
		//And finally, get the list of conflict sets
		buildConflictSets();
		System.out.println("Computed minimal conflict sets: " +
				conflictSets.size() + " sets of mappings");
		System.out.println("Repair Map finished in " +
				(System.currentTimeMillis()/1000-time) + " seconds");
	}
	
	//Computes the list of classes that must be checked for coherence
	private void buildCheckList()
	{
		//Start with the descendants of classList classes that have
		//at least 2 parents, both of which have a classList class
		//in their ancestral line
		HashSet<Integer> descList = new HashSet<Integer>();
		for(Integer i: classList)
		{
			//Get the subClasses of classList classes
			for(Integer j : rels.getSubClasses(i,false))
			{
				//Exclude those that have less than two parents
				Set<Integer> pars = rels.getSuperClasses(j, true);
				if(pars.size() < 2)
					continue;
				//Count the classList classes in the ancestral
				//line of each parent (or until two parents with
				//classList ancestors are found)
				int count = 0;
				for(Integer k : pars)
				{
					if(classList.contains(k))
						count++;
					for(Integer l : rels.getSuperClasses(k, false))
					{
						if(classList.contains(l))
						{
							count++;
							break;
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
		for(Integer i : checkList)
		{
			Vector<Path> classConflicts = buildClassConflicts(i);
			for(Path p : classConflicts)
				addConflict(p,conflictSets);
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
						addConflict(merged, classConflicts);
					}
				}
			}
		}
		return classConflicts;
	}
	
	//Adds a path to a list of conflict sets if it is a
	//minimal path, removing redundant paths if they exist
	private void addConflict(Path p, Vector<Path> paths)
	{
		for(int i = 0; i < paths.size(); i++)
		{
			Path q = paths.get(i);
			if(p.size() >= q.size())
			{
				if(p.contains(q))
					return;
			}
			else if(q.contains(p))
			{
				paths.remove(i);
				i--;
			}
		}
		paths.add(p);
	}
}