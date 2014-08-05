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
 * @date 01-08-2014                                                            *
 * @version 2.0                                                                *
 ******************************************************************************/
package aml.filter;

import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import aml.AML;
import aml.match.Alignment;
import aml.match.Mapping;
import aml.ontology.RelationshipMap;
import aml.ontology.URIMap;
import aml.util.Table3;

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
	private Table3<Integer,Integer,Path> ancestorMap;
	//The length of ancestral paths (to facilitate transitive closure)
	private Table3<Integer,Integer,Integer> pathLengths;
	//The list of conflict sets
	private Vector<Path> conflictSets;
	
//Constructors
	
	public RepairMap(Alignment maps)
	{
		rels = AML.getInstance().getRelationshipMap();
		a = maps;
		long time = System.currentTimeMillis()/1000;
		init();
		System.out.println("Computed CheckList and DescendantMap in " + (System.currentTimeMillis()/1000-time) + " seconds");
		System.out.println("CheckList size: " + checkList.size());
		System.out.println("AncestorMap size: " + ancestorMap.size());
		transitiveClosure();
		System.out.println("Extended AncestorMap in " + (System.currentTimeMillis()/1000-time) + " seconds");
		System.out.println("AncestorMap size: " + ancestorMap.size());
		buildConflictSets();
		System.out.println("Built Conflict Sets in " + (System.currentTimeMillis()/1000-time) + " seconds");
		System.out.println("Conflict Sets: " + conflictSets.size());
		URIMap uris = AML.getInstance().getURIMap();
		try
		{
			PrintWriter pw = new PrintWriter(new FileOutputStream("store/temp2.txt"));
			for(Path p : conflictSets)
			{
				pw.println("Conflict Set");
				for(Integer i : p)
					pw.println(uris.getURI(a.get(i).getSourceId()) + " --- " +
							uris.getURI(a.get(i).getTargetId()));
			}
			pw.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
//Public Methods
	
	public Vector<Path> getConflictSets()
	{
		return conflictSets;
	}
	
	public int getIndex(Mapping m)
	{
		return a.getIndex(m.getSourceId(), m.getTargetId());
	}
	
	public int getIndex(int source, int target)
	{
		return a.getIndex(source, target);
	}

	public Mapping getMapping(int index)
	{
		return a.get(index);
	}
	
//Private Methods
	
	private void addConflict(Path p, Path q)
	{
		//First combine the two paths into a single one
		Path r = new Path(p);
		for(Integer i : q)
		{
			//Excluding overlapping mappings to ensure
			//the combined path is minimal
			if(p.contains(i))
				r.remove(i);
			else
				r.add(i);
		}
		for(int i = 0; i < conflictSets.size(); i++)
		{
			Path s = conflictSets.get(i);
			if(r.contains(s))
				return;
			if(s.contains(r))
			{
				conflictSets.remove(i);
				i--;
			}
		}
		conflictSets.add(r);
	}
	
	private void addRelation(int child, int parent)
	{
		Path p = new Path();
		addRelation(child,parent,p);		
	}
	
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
	}
	
	//Builds the checkList and descendantMap
	private void init()
	{
		classList = new HashSet<Integer>();
		checkList = new HashSet<Integer>();
		ancestorMap = new Table3<Integer,Integer,Path>();
		pathLengths = new Table3<Integer,Integer,Integer>();

		//First build the classList, starting with the classes
		//involved in disjoint clauses
		classList.addAll(rels.getDisjoint());
		//If there aren't any, there is nothing to do
		if(classList.size() == 0)
			return;
		
		//Then add all classes involved in mappings
		classList.addAll(a.getSources());
		classList.addAll(a.getTargets());

		//Then build the checkList, starting with the descendants
		//of classList classes that have at least 2 parents, both
		//of which have a classList class in their ancestral line
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
					for(Integer l : rels.getSuperClasses(k, false))
					{
						if(classList.contains(l))
						{
							count++;
							break;
						}
						if(count > 1)
							break;
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
		HashSet<Integer> toRemove = new HashSet<Integer>();
		for(Integer i : descList)
			for(Integer j : rels.getSubClasses(i, false))
				if(descList.contains(j))
					toRemove.add(i);
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
			//Check if there is no descendant in the checkList
			boolean isRedundant = false;
			int source = m.getSourceId();
			int target = m.getTargetId();
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
				if(sourceCount >= targetCount && sourceCount > 1)
					mapList.add(source);
				else if(targetCount > 1)
					mapList.add(target);
			}
		}
		//Filter out mapped classes that have a descendant in the mapList
		toRemove = new HashSet<Integer>();
		for(Integer i : mapList)
			for(Integer j : rels.getSubClasses(i, false))
				if(mapList.contains(j))
					toRemove.add(i);
		mapList.removeAll(toRemove);
		//Filter out checkList classes that have a descendant in the mapList
		toRemove = new HashSet<Integer>();
		for(Integer i : checkList)
			for(Integer j : rels.getSubClasses(i, false))
				if(mapList.contains(j))
					toRemove.add(i);
		checkList.removeAll(toRemove);
		//Finally, add the mapList to the checkList
		checkList.addAll(mapList);
		//Now that we have the checkList, we can build the acestorMap
		//with all relations between classes in the checkList and
		//classes in the classList
		for(Integer i : checkList)
		{
			Set<Integer> ancs = rels.getSuperClasses(i,false);
			for(Integer j : ancs)
				if(classList.contains(j))
					addRelation(i, j);
		}
	}
	
	//Fills in the ancestorMap by adding paths that pass through
	//mappings doing a breadth first search
	private void transitiveClosure()
	{
		//First, add paths through the direct mappings of checkList classes
		for(Integer i : checkList)
		{
			//Get the mappings
			Set<Integer> maps = a.getMappingsBidirectional(i);
			//And for each mapping
			for(Integer j : maps)
			{
				//Get its index
				int index = a.getIndexBidirectional(i, j);
				//Get its ancestors
				HashSet<Integer> newAncestors = new HashSet<Integer>(rels.getSuperClasses(j,false));
				//Plus the mapping itself
				newAncestors.add(j);
				//And add them, if they are on the checkList
				for(Integer m : newAncestors)
					if(classList.contains(m))
						addRelation(i,m,new Path(index));
			}
		}
		//Then get paths iteratively by extending paths with new
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
	
	private void buildConflictSets()
	{
		conflictSets = new Vector<Path>();
		for(Integer i : checkList)
		{
			Vector<Integer> disj = new Vector<Integer>();
			for(Integer j : ancestorMap.keySet(i))
				if(rels.hasDisjoint(j))
					disj.add(j);
			for(int j = 0; j < disj.size()-1; j++)
			{
				for(int k = j+1; k < disj.size(); k++)
				{
					if(rels.areDisjoint(disj.get(j), disj.get(k)))
					{
						for(Path p : ancestorMap.get(i, disj.get(j)))
							for(Path q : ancestorMap.get(i, disj.get(k)))
								addConflict(p,q);							
					}
				}
			}
		}
	}
}