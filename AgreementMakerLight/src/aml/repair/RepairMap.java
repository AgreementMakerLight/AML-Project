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
* @date 22-10-2013                                                            *
******************************************************************************/
package aml.repair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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

import aml.match.Alignment;
import aml.match.Mapping;
import aml.ontology.Ontology;
import aml.ontology.RelationshipMap;
import aml.util.Table2;
import aml.util.Table2Plus;

public class RepairMap implements Iterable<String>
{
	
//Attributes

	//Links to the Alignment & Ontologies 
	private Alignment align; //copy of
	private Ontology source;
	private Ontology target;
	
	//Unified list of terms that need to be checked in source and target Ontologies
	private Vector<String> checklist;
	//Unified map of ascending relationships of the source and target Ontologies
	private Table2Plus<String,String,Boolean> ancestorMap;
	//Unified map of ascending direct relationships of the source and target Ontologies
	private Table2<String,String> ancestorDirectMap;
	//Unified map of disjoint clauses of the source and target Ontologies
	public Table2<String,String> disjointMap;
	//maximum number of 
	private int maxNumberThreads = Runtime.getRuntime().availableProcessors();
	
//Constructors

	/**
	 * Constructs a new RepairMap from the input Alignment
	 * @param a: the Alignment to build the RepairMap
	 */
	public RepairMap(Alignment a)
	{
		//Initialize the internal data structures
		checklist = new Vector<String>(0,1);
		ancestorMap = new Table2Plus<String,String,Boolean>();
		ancestorDirectMap = new Table2<String,String>();
		disjointMap = new Table2<String,String>();
		
		//Link the Alignment & Ontologies
		align = new Alignment(a); //a copy of the alignment
		source = a.getSource();
		target = a.getTarget(); 
		
		//Now build the internal data structures
		buildMaps();
		
	}
	
//Public Methods

	public Vector<String> getChecklist()
	{
		return checklist;
	}
	
	//Returns the URI for the given term identifier
	public String getURI(String s)
	{
		int id = Integer.parseInt(s.substring(1));
		String uri;
		if(s.startsWith("S"))
			uri = source.getTermURI(id);
		else
			uri = target.getTermURI(id);
		return uri;
	}
	
	//Returns the URI for the given term identifier
	public Vector<String> getURI(Vector<String> v)
	{
		Vector<String> r = new Vector<String>(v.size());
		for(String s: v)
		{
			r.add(getURI(s));
		}
		
		return r;
	}
	
	/**
	 * @param term: the global identifier of the input term
	 * @return whether the term is inconsistent
	 */
	public boolean isInconsistent(String term)
	{
		Vector<String> ancestors = getExtendedAncestors(term);
		for(int i = 0; i < ancestors.size()-1; i++)
		{
			Vector<String> disj = disjointMap.get(ancestors.get(i));
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
	public Iterator<String> iterator()
	{
		return checklist.iterator();
	}
	
	/**
	 * @return the list of terms in the checklist that are inconsistent
	 */
	public Vector<String> listIncoherentClasses()
	{
		Vector<String> classes = new Vector<String>(0,1);
		for(String s : checklist)
			if(isInconsistent(s))
				classes.add(s);
		return classes;
	}
	
	/**
	 * @return true if the alignment is coherent
	 */
	public boolean isCoherent()
	{
		for(String s : checklist)
			if(isInconsistent(s))
				return false;
		return true;
	}
	
	/**
	 * @param term: the global identifier of the input term
	 * @return the set of disjoint for which the term is inconsistent
	 */
	public Vector<Vector<String>> getConflictDisjoints(String term)
	{
		Vector<String> ancestors = getExtendedAncestors(term);
		Vector<Vector<String>> disjoints = new Vector<Vector<String>>(1,1);
		
		for(int i = 0; i < ancestors.size()-1; i++)
		{
			Vector<String> disj = disjointMap.get(ancestors.get(i));
			if(disj == null)
				continue;
			for(int j = i + 1; j < ancestors.size(); j++)
			{
				if(disj.contains(ancestors.get(j)))
				{
					Vector<String> cdisj = new Vector<String>();
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
	 * @returns global list of conflicts
	 */
	public Vector<Vector<Mapping>> getGlobalListConflicts()
	{
		return myglobalconflictList;		
	}
	
	/**
	 * @param vvMappers - A list of lists of mappings
	 * @result The list of minimal lists of mappings, i.e., there is no list that is a sublist of another list.
	 */
	public Vector<Vector<Mapping>> minimalListMappings(Vector<Vector<Mapping>> vhMappings)
	{
		Vector<Vector<Mapping>> minimal = new Vector<Vector<Mapping>>();
		HashSet<Integer> notMinimal = new HashSet<Integer>();
		
		for(int i=0; i<vhMappings.size();i++)
		{
			if(notMinimal.contains(i))
				continue;//it is not minimal already
			
			boolean isMinimal = true;
			Vector<Mapping> a = vhMappings.get(i);
			int asize = a.size();
			
			for(int j = i+1; j<vhMappings.size();j++)
			{
				if(notMinimal.contains(j))
					continue;//don't check to non-minimals or the same
				
				Vector<Mapping> b = vhMappings.get(j);
				int bsize= b.size();
				boolean acontainsAllb = asize>=bsize && a.containsAll(b);
				boolean bcontainsAlla = bsize>=asize && b.containsAll(a);
				
				if(!acontainsAllb && !bcontainsAlla)
				{
					continue;
				}
				
				if(acontainsAllb && bcontainsAlla)
				{
					isMinimal = false; // to remove 'duplicates'
					break;
				}
				
				if(acontainsAllb && !bcontainsAlla)
				{		//list i is not minimal
						isMinimal = false;
						break;
				}
				
				if(!acontainsAllb && bcontainsAlla)
				{		//list j is not minimal
						notMinimal.add(j);
				}
			}//end for	
			
			if(isMinimal)
			{
				minimal.add(a);
			}else{
				notMinimal.add(i);
			}
		}
		
		return minimal;
	}
	
//Private Methods
	
	//Builds the internal data structures (checklist, ancestorMap and disjointMap)
	private void buildMaps()
	{
		//Get the RelationshipMaps
		RelationshipMap sourceMap = source.getRelationshipMap();
		RelationshipMap targetMap = target.getRelationshipMap();
		
		//Build a list of source terms consisting of each mapped term
		HashSet<Integer> sourceList = new HashSet<Integer>(align.getSources());
		//Build a list of target terms consisting of each mapped term
		HashSet<Integer> targetList = new HashSet<Integer>(align.getTargets());
		//begin with source
		int size = sourceList.size();
		//Plus each of its Descendants that has more than one parent that doesnt have any subclass
		//with more than one parent
		Vector<Integer> sourceListDesc = new Vector<Integer>();
		for(int i = 0; i < size; i++)
		{
			Vector<Integer> desc = sourceMap.getDescendants(i);
			for(Integer j : desc)
				if(sourceMap.countIsAParents(j) > 1 && !sourceList.contains(j) && !sourceListDesc.contains(j))
					sourceListDesc.add(j);
		}
		
		
		//Remove the Descendants that have a subclass with more than one parent
		size = sourceListDesc.size();
		for(int i = 0; i < sourceListDesc.size(); i++)
		{
			int cls = sourceListDesc.get(i);
			Vector<Integer> desc = sourceMap.getDescendants(cls);
			
			for(Integer j : desc)
			{
				if(sourceListDesc.contains(j) || sourceList.contains(j)){
					//it could be a superclass
					//lets check if there is a cycle
					if(!sourceMap.getDescendants(j).contains(i))
					{
						//not a cycle -- it is a superclass
						sourceListDesc.remove(i);
						i--;
						break;
					}
				}
			}
		}
		//Add classes that need to be checked
		//terms that have more than one direct parent and doesn't have any subclass with more than one parent.
		
		//add sourceListDesc is already checked	
		for(Integer i : sourceListDesc)
			checklist.add("S" + i);
		
		//add sourceList with the same property
		//We only need to add the source OR the target. we choose the smallest one.
		if(targetList.size()>=sourceList.size())
		{
			for(Integer i : sourceList)
			{	
				Vector<Integer> desc = sourceMap.getDescendants(i);
				boolean checkcls = true;
				for(Integer j : desc)
				{
					if((checklist.contains("S" + j) || sourceList.contains(j)) && !sourceMap.getDescendants(j).contains(i)){
						//it is a superclass
						checkcls = false;
						break;
					}
				}
				
				if(checkcls)
				{
					checklist.add("S" + i);
				}			
			}
		}
		//add all Descendants to the sourceList
		sourceList.addAll(sourceListDesc);
		
		//Add all disjoint clauses to the disjointMap
		Set<Integer> sourceDisj = sourceMap.getDisjoint();
		for(Integer i : sourceDisj)
		{
			Vector<Integer> v = sourceMap.getDisjoint(i);
			for(Integer j : v)
				disjointMap.add("S" + i, "S" + j);
		}
		
		//Then repeat the process for the target Ontology
		size = targetList.size(); //BUG
		//Plus each of its Descendants that has more than one parent that doesnt have any subclass
		//with more than one parent
		Vector<Integer> targetListDesc = new Vector<Integer>();
		for(int i = 0; i < size; i++)
		{
			Vector<Integer> desc = targetMap.getDescendants(i);
			for(Integer j : desc)
				if(targetMap.countIsAParents(j) > 1 && !targetList.contains(j) && !targetListDesc.contains(j))
					targetListDesc.add(j);
		}
		
		for(int i = 0; i < targetListDesc.size(); i++)
		{
			int cls = targetListDesc.get(i);
			Vector<Integer> desc = targetMap.getDescendants(cls);
			
			for(Integer j : desc)
			{
				if(targetListDesc.contains(j) || targetList.contains(j)){
					//it could be a superclass
					//lets check if there is a cycle
					if(!targetMap.getDescendants(j).contains(i))
					{
						//not a cycle -- it is a superclass
						targetListDesc.remove(i);
						i--;
						break;
					}
				}
			}
		}
		for(Integer i : targetListDesc)
			checklist.add("T" + i);
		
		//add targetList with the same property
		//We only need to add the source OR the target. we choose the smallest one.
		if(targetList.size()<sourceList.size())
		{
			for(Integer i : targetList)
			{
				
				Vector<Integer> desc = targetMap.getDescendants(i);
				boolean checkcls = true;
				for(Integer j : desc)
				{
					if((checklist.contains("T" + j) || targetList.contains(j)) && !targetMap.getDescendants(j).contains(i)){
						//it is a superclass
						checkcls = false;
						break;
					}
				}
				
				if(checkcls)
				{
					checklist.add("T" + i);
				}			
			}
		}
		//add all Descendants to the targetList
		targetList.addAll(targetListDesc);
		
		Set<Integer> targetDisj = targetMap.getDisjoint();
		for(Integer i : targetDisj)
		{
			Vector<Integer> v = targetMap.getDisjoint(i);
			for(Integer j : v)
				disjointMap.add("T" + i, "T" + j);
		}

		//Build the ancestorMap
		//Start by adding all relationships between sourceList terms
		for(Integer i : sourceList)
		{
			//And their ancestors
			Vector<Integer> ancs = sourceMap.getAncestors(i,true); //true so is_a
			for(Integer j : ancs)
				//That are either in the sourceList themselves or in a disjoint clause
				if(sourceList.contains(j) || sourceDisj.contains(j))
					ancestorMap.add("S" + i, "S" + j, null);
		}
		//Then do the same for the targetList
		for(Integer i : targetList)
		{
			Vector<Integer> ancs = targetMap.getAncestors(i,true); //true so is_a
			for(Integer j : ancs)
				if(targetList.contains(j) || targetDisj.contains(j))
					ancestorMap.add("T" + i, "T" + j, null);
		}
		
		//Include the direct/indirect ancestor information (source and target)
		for(String si : ancestorMap.keySet())
		{	
			HashMap<String, Boolean> ancs = ancestorMap.get(si);
			//Given an ancestor
			for(String anc1 : ancs.keySet())
			{			
				boolean directAncestor = true;
				//looks for an ancestor of si that is also its ancestor
				for(String anc2 : ancs.keySet())
				{	
					if(!anc1.equals(anc2) && ancestorMap.get(anc2, anc1)!= null){
						//anc1 is an ancestor of anc2 -> anc1 is not direct ancestor of si
						ancestorMap.add(si, anc1, false);
						directAncestor = false;
					}
				}
				//anc1 is a direct ancestor of si
				if(directAncestor)
				{
					ancestorMap.add(si, anc1, true);
					ancestorDirectMap.add(si, anc1);
				}
			}
		}
	}
	
	//building minimal conflictsMaps sets using Parallel method
	public void builtConflictSetsOfMappings(Vector<String> checklocallist)
	{
		conflictsMapComputed.clear();
		builtConflictsMapsPPP(checklocallist); //parallel version
		builtMinimalConflictMapsParallel(checklocallist);
	}
	
	
	//vai determinar os conjuntos minimos de forma paralela
	private void builtMinimalConflictMapsParallel(Vector<String> checklocallist)
	{
		try
		{
			ArrayList<createGlobalConflictListParallel> listsConflictList = new ArrayList<createGlobalConflictListParallel>();
			for(String m: checklocallist)
				listsConflictList.add(new createGlobalConflictListParallel(m));
			
	        List<Future<Vector<Vector<Mapping>>>> results;
			ExecutorService executor1 = Executors.newFixedThreadPool(maxNumberThreads);
			results = executor1.invokeAll(listsConflictList);
			executor1.shutdown();
	        ArrayList<createConflictListParallel> listsConflictList2 = new ArrayList<createConflictListParallel>();
			ExecutorService executor2;
			int blocks = (int) Math.min(results.size()/2,maxNumberThreads);
			while(blocks>1)
			{	
				listsConflictList2.clear();
				listsConflictList2 = new ArrayList<createConflictListParallel>();
				for(int i=0;i<blocks;i++)
					listsConflictList2.add(new createConflictListParallel(results,i,blocks));
				executor2 = Executors.newFixedThreadPool(maxNumberThreads);
				results = executor2.invokeAll(listsConflictList2);
				executor2.shutdown();
				blocks = (int) Math.min(results.size()/2,maxNumberThreads);							
			}
			listsConflictList2.clear();
			listsConflictList2 = new ArrayList<createConflictListParallel>();
			for(int i=0;i<blocks;i++)
				listsConflictList2.add(new createConflictListParallel(results,i,blocks));
			ExecutorService executor3 = Executors.newFixedThreadPool(maxNumberThreads);
			results = executor3.invokeAll(listsConflictList2);
			executor3.shutdown();
			myglobalconflictList = results.get(0).get();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private class createConflictListParallel implements Callable<Vector<Vector<Mapping>>> {
		List<Future<Vector<Vector<Mapping>>>> results;
		int part;
		int total;
        
		createConflictListParallel(List<Future<Vector<Vector<Mapping>>>> results, int part, int total) {
            this.results = results;
    		this.part = part;
    		this.total = total;            
        }
        
        @Override
        public Vector<Vector<Mapping>> call() {
        	try {
				return createMyglobalconflictList(results,  part,  total);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	
        	return null;
        }
	}
      
	
	
	
	private Vector<Vector<Mapping>> createMyglobalconflictList(List<Future<Vector<Vector<Mapping>>>> results, int part, int total) throws InterruptedException, ExecutionException
	{
		
		int size = results.size();
		Double slot = Math.max(Math.ceil(size/total),1);
		Double start = part*slot;
		Double stop = Math.min((part+1)*slot, size);
		Vector<Vector<Mapping>> myresult = results.get(start.intValue()).get();
		HashMap<Mapping, HashSet<Integer>> help = new HashMap<Mapping, HashSet<Integer>>();
		
		for(int i= start.intValue()+1;i<stop;i++)
		{
			myresult = minimalListsMappingsBETTER(results.get(i).get(), myresult,help); 
			//myresult = minimalListMappings(results.get(i).get(), myresult);
			/*eh mais rapido o better para todos*/
			
		}
		
		return removeNulls(myresult);
		//return myresult;
	}
	
	
	
	
	
	
	int count_check_list = 0;
	
	
	private  synchronized  void add_count_check_list()
	{
		count_check_list++;
	}
	
	
	
	/**
	 * TODO - Mudar para a vers�o java 1.7
	 * class - to implement the parallel version of minimalListMappings
	 */
	private class createGlobalConflictListParallel implements Callable<Vector<Vector<Mapping>>>
	{
		private String term;
		createGlobalConflictListParallel(String t)
	    {
	        this.term = t;
	    }
	        
	    @Override
	    public Vector<Vector<Mapping>> call()
	    {
	     	Vector<Vector<Mapping>> aaa = null;
	       	try{
	       		aaa = createGlobalConflictList(term);
	        	add_count_check_list();
	        }catch(Exception e){
	        	e.printStackTrace();
	        }
	        return aaa;
	    }                
	 }
	
	/**
	 * @param checkterm - a class
	 * @return returns a list of minimal conflicting lists of mappings wrt checkterm
	 */
	private Vector<Vector<Mapping>> createGlobalConflictList(String checkterm)
	{
		HashMap<String, Vector<Vector<Mapping>>> aaa =null;
		Set<String> bbb = null;
		
		aaa = conflictsMapComputed.get(checkterm);
		
		if(aaa==null)
		{
			aaa = new HashMap<String, Vector<Vector<Mapping>>>();
		}	
		bbb = aaa.keySet();
		Vector<String> ss  = new Vector<String>(bbb);
		Vector<Vector<Mapping>> gConflictList = new Vector<Vector<Mapping>>();
		for(String d: ss)
		{
			for(String d2: disjointMap.get(d))
			{
				if(ss.contains(d2) && ss.indexOf(d2)>ss.indexOf(d))
				{
					Vector<Vector<Mapping>> joint = joinListMappings(aaa.get(d), aaa.get(d2));
					Vector<Vector<Mapping>> minimal_joint = minimalListMappings(joint);
					minimalListMappings(minimal_joint,gConflictList);
				}
			}
		}
		
		return removeNulls(gConflictList);
	}
	
	/**
	 * @param nonMinimal - A list of lists of mappings
	 * @param minimal - A list of minimal lists of mappings
	 * @result The join list of minimal lists of mappings
	 * @requires minimal - must be a list of minimal lists of mappings
	 */
	private static Vector<Vector<Mapping>> minimalListMappings(Vector<Vector<Mapping>> nonMinimal, Vector<Vector<Mapping>> minimal)
	{
		for(int i=0; i<nonMinimal.size();i++)
		{
			Vector<Mapping> a = nonMinimal.get(i);
			boolean isMinimal = true;
			boolean added = false;
			int asize = a.size();
			
			for(int j = 0; j<minimal.size();j++)
			{
				Vector<Mapping> b = minimal.get(j);
				int bsize = b.size();
				boolean acontainsAllb = asize>=bsize && a.containsAll(b);
				boolean bcontainsAlla = bsize>=asize && b.containsAll(a);
				
				if(!acontainsAllb && !bcontainsAlla)
				{
					continue;
				}
				
				if(acontainsAllb && bcontainsAlla)
				{
					isMinimal = false;
					break;
				}
				
				if(acontainsAllb && !bcontainsAlla)
				{		
					isMinimal = false;
					break;
				}
				
				if(!acontainsAllb && bcontainsAlla)
				{	//it is a new minimal
					if(!added)
					{
						minimal.add(a);
						added = true;
					}
					
					minimal.remove(j);
					j--;
				}
			}//end for	
			
			if(isMinimal)
			{
				minimal.add(a);
			}
		}
		
		return minimal;
	}
	
	private static void addminimalListAUX(HashMap<Mapping, HashSet<Integer>> help, Vector<Mapping> vmaps, int i)
	{
		for(Mapping m: vmaps)
		{
			if(!help.containsKey(m)){
				HashSet<Integer> n = new HashSet<Integer>();
				n.add(i);
				help.put(m, n);
			}
			else
				help.get(m).add(i);
		}
    }
	
    private Vector<Vector<Mapping>> removeNulls(Vector<Vector<Mapping>> vvmaps)
    {
		for(int j = 0; j<vvmaps.size();j++)
		{
			if(vvmaps.get(j)== null){
				vvmaps.remove(j);
				j--;
			}
		}
		return vvmaps;
    }
	    
	//ambas as listas sao minimas
	private static Vector<Vector<Mapping>> minimalListsMappingsBETTER(Vector<Vector<Mapping>> Minimal0, Vector<Vector<Mapping>> minimal,HashMap<Mapping, HashSet<Integer>> help)
	{
		HashMap<Mapping, HashSet<Integer>> ajuda = help;
		if(ajuda.size()==0)
			for(int i=0; i<minimal.size();i++)
				addminimalListAUX(ajuda, minimal.get(i), i);
		for(int i=0; i<Minimal0.size();i++)
		{
			Vector<Mapping> a = Minimal0.get(i);
			boolean isMinimal = true;
			boolean added = false;
			HashSet<Integer> check = new HashSet<Integer>();
			HashSet<Integer> checkback = new HashSet<Integer>();
			CheckMappings:
			for(Mapping m: a)
			{
				if(ajuda.containsKey(m))
				{
					check = new HashSet<Integer>(ajuda.get(m));
					check.removeAll(checkback);
					checkback = check;
					int asize = a.size();
					for(int j: check)
					{
						if(j==i)
							continue;
						Vector<Mapping> b = minimal.get(j);
						if(b==null)
							continue;
						int bsize = b.size();
						boolean acontainsAllb = asize>=bsize && a.containsAll(b);
						boolean bcontainsAlla = bsize>=asize && b.containsAll(a);
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
								minimal.add(a);
								addminimalListAUX(ajuda,a,minimal.size()-1);
								added = true;
							}
							minimal.set(j, null);
						}
					}//end for	
				}//end if
			}// end for mapping a
				
			if(isMinimal)
			{
				minimal.add(a);
				addminimalListAUX(ajuda,a,minimal.size()-1);
			}
		}
		return minimal;
	}
	
	/**
	 * TODO Not efficient -- it doesn't check for all duplicates.
	 * @param vvmappings - a list of lists of mappings
	 * @param vvmappings2 - a list of lists of mappings
	 * @return the join list of the input lists without (some) duplicates
	 */
	private Vector<Vector<Mapping>> joinListMappings(Vector<Vector<Mapping>> vvmappings, Vector<Vector<Mapping>> vvmappings2)
	{
		Vector<Vector<Mapping>> new_list = new Vector<Vector<Mapping>>();
		
		for(Vector<Mapping> vm: vvmappings)
		{
			for(Vector<Mapping> vm2: vvmappings2)
			{
				HashSet<Mapping> addh = new HashSet<Mapping>(vm);
				// removes duplicate mappings
				addh.addAll(vm2);
				Vector<Mapping> newvm = new Vector<Mapping>(addh);
				new_list.add(newvm);			
			}
		}
		
		return new_list;
	}

	
	/**
	 * @param term - a class
	 * @return number of ancestors (including mappings) of the term in the ancestorMap
	 */
	private int countAncestorsH(String term)
	{
		int nr_ancestors = 0;
		if(ancestorMap.contains(term)){
			nr_ancestors = ancestorMap.get(term).size();
		}
		
		for(String m: getMappings(term))
		{
			if(ancestorMap.contains(m))					
				nr_ancestors += ancestorMap.get(m).size();
		}
		
		return nr_ancestors;
	}
	
	
	/**
	 * Comparator of terms. To order terms wrt to their height in the ancestorMap true.
	 */
	private  Comparator<String> TermComparator 
    = new Comparator<String>() {
				public int compare(String a, String b) {
					
					int uri1 = Integer.parseInt(a.substring(1));
					int uri2 = Integer.parseInt(b.substring(1));
					
					if(uri1 == uri2)
						return 0;
					
					int result = countAncestorsH(a) - countAncestorsH(b);
					
					if(result!=0)
						return result;
					else
						return uri1-uri2;
						
				}
	};
	
	
	//auxiliary maps and lists for builtConflictsMaps
	//private HashMap<String,HashMap<String,Vector<Vector<Mapping>>>> conflictsMap = new HashMap<String,HashMap<String,Vector<Vector<Mapping>>>>();
	//private ArrayList<String> conflictsMapCurrent = new ArrayList<String>(); //true if it is done
	//private HashSet<String> conflictsMapFinish = new HashSet<String>(); //true if it is done
	public Vector<Vector<Mapping>> myglobalconflictList = new Vector<Vector<Mapping>>();
	//public HashSet<String> Cycles = new HashSet<String>();
	private Vector<String> clsCheck;
	
	
	
	/*Conflicts Mappings that are already computed completely*/
	private ConcurrentHashMap<String,HashMap<String,Vector<Vector<Mapping>>>> conflictsMapComputed = new ConcurrentHashMap<String,HashMap<String,Vector<Vector<Mapping>>>>();
	
	/*
	 * returns null... it doesnt exist.
	 * 
	 */
	private synchronized HashMap<String,Vector<Vector<Mapping>>> getConflictMapComputed(String term)
	{
		return conflictsMapComputed.get(term);
	} 
	
	
	/*
	 * puts if it doesnt exist
	 * 
	 */
	private synchronized void putConflictMapComputed(String term, HashMap<String,Vector<Vector<Mapping>>> confmap)
	{
		if(getConflictMapComputed(term) == null){
			conflictsMapComputed.put(term, confmap);
		}
	} 
	
	/**
	 * Builds the conflictsMaps for every term in checklist. PARALLEL
	 */
	private void builtConflictsMapsPPP(Vector<String> checklocallist)
	{
		//sort classes by their height in anscestor Tree
		clsCheck = new Vector<String>(checklocallist);
		Collections.sort(clsCheck, TermComparator);
		
		//building minimal conflictsMaps sets using Parallel method
		ArrayList<createBuiltConflictsMapsParallel> listsBuiltConflictList = new ArrayList<createBuiltConflictsMapsParallel>();
		
		for(String cls: clsCheck)
		{
			listsBuiltConflictList.add(new createBuiltConflictsMapsParallel(cls));
		}
		
		
		ExecutorService executor = Executors.newFixedThreadPool(maxNumberThreads);
		try {
			executor.invokeAll(listsBuiltConflictList);
			executor.shutdown();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} 
		
	}
	
	/**
	 * class - to implement the parallel version of minimalListMappings
	 */
	 private class createBuiltConflictsMapsParallel implements Callable<Object> {
	        private String term;
	        
	        createBuiltConflictsMapsParallel(String t) {
	            this.term = t;
	        }
	        
	        @SuppressWarnings("rawtypes")
			@Override
	        public Object call() {
	        	
	        	builtConflictsMapsParallel(term);
	        	
	        	return new Vector();
	        }                
	  }
	
	
	
	@SuppressWarnings("rawtypes")
	private Object builtConflictsMapsParallel(String term)
	{		
		if(getConflictMapComputed(term)!=null)
			return new Vector();
		
		HashMap<String,HashMap<String,Vector<Vector<Mapping>>>> conflictsMap = new HashMap<String,HashMap<String,Vector<Vector<Mapping>>>>();
		ArrayList<String> conflictsMapCurrent = new ArrayList<String>();
		HashSet<String> Cycles = new HashSet<String>();
		
		builtConflictsMaps(term, conflictsMap, conflictsMapCurrent, Cycles);
		
		//in the case there is a cycle
		while(!Cycles.isEmpty())
		{
			conflictsMap.clear();
			conflictsMapCurrent.clear();
			String cycleItem = Cycles.iterator().next();
			Cycles.remove(cycleItem);
			builtConflictsMaps(cycleItem, conflictsMap, conflictsMapCurrent, Cycles);
		}
		
		
		conflictsMap = null;
		conflictsMapCurrent = null;
		Cycles = null;
		
		
		return new Vector();
	}
	
	/**
	 * Auxiliary function to build conflict Maps for a term
	 * @param term - a class
	 * @return A set of terms that need to be checked afterwards
	 */
	private  HashSet<String> builtConflictsMaps(String term, HashMap<String,HashMap<String,Vector<Vector<Mapping>>>> conflictsMap, ArrayList<String> conflictsMapCurrent, HashSet<String> Cycles)
	{
		HashSet<String> result = new HashSet<String>();
		
		if(getConflictMapComputed(term) != null)
		{
			//the conflict mappings were already been computed.
			Cycles.remove(term);
			return result;
		}
		else if(conflictsMapCurrent.contains(term))
		{			
			//it is a cycle;
			Cycles.add(term);
			result.add(term);
			return result;	
		}else{
			//first
		}
		
		Vector<String> ancestors = null;
		//it adds to the current search stack
		
		conflictsMapCurrent.add(term);
		
		if(!conflictsMap.containsKey(term))
		{
			//built a new if it new
			conflictsMap.put(term, new HashMap<String,Vector<Vector<Mapping>>>());
		}
		
		
		
		//get the conflicts maps from ancestors
		//attention term can't be remove from the set because of threads
		//the set is the same as the one in Ancestors.!!!!!
		ancestors = new Vector<String>(getDirectAncestors(term));
		ancestors.remove(term);
		
		
		
		for(String a: ancestors)
		{
				
			HashSet<String> r = builtConflictsMaps(a, conflictsMap, conflictsMapCurrent, Cycles);
			
			r.remove(term);
			result.addAll(r);
			
			if(getConflictMapComputed(a) == null){
				Cycles.add(a);
			}
			
			
		}
		
		//get the conflicts maps from mappings
			//attention term can't be remove from the set because of threads
		Vector<String> maps = new Vector<String>(getMappings(term));
		maps.remove(term);
		
		for(String m: maps)
		{
			HashSet<String> r = builtConflictsMaps(m, conflictsMap, conflictsMapCurrent, Cycles);
			r.remove(term);
			result.addAll(r);
			
			if(getConflictMapComputed(m) == null)
			{
				Cycles.add(m);
			}
		}

		
		//check if the term or its ancestors are part of any disjoint
		
		Set<String> tt = null;
		synchronized(this){
			tt = disjointMap.keySet();
		}
		
		HashMap<String, Vector<Vector<Mapping>>> confMapTerm = conflictsMap.get(term);
		for(String d: tt)
		{	
			if(d.compareTo(term) == 0)
			{
				Vector<Mapping> vm = new Vector<Mapping>();
				Vector<Vector<Mapping>> vvm = new Vector<Vector<Mapping>>();
				vvm.add(vm);

				confMapTerm.put(term, vvm);

				continue;
			}

			//check path of mappings from the ancestors of a to d
			for(String a: ancestors)
			{				
				
				HashMap<String,Vector<Vector<Mapping>>> pathal = getConflictMapComputed(a);
				Vector<Vector<Mapping>> path;
				
				if(pathal!=null){
					path = pathal.get(d); //path of mappings from term to d (finished)
				}else{
					path = conflictsMap.get(a).get(d); //path of mappings from term to d
				}

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
			}
			//if it founds any path then calculates its minimal subset
			if(confMapTerm.containsKey(d))
			{
				Vector<Vector<Mapping>> mlm = null;
				synchronized(this){
				
				mlm =  minimalListMappings(confMapTerm.get(d));
				
				}
				
				confMapTerm.put(d, mlm);
			}
			
			//check path of mappings from the mappings of a to d
			//it needs to add the current mapping to the path
			
			
			HashMap<Integer,Vector<Vector<Mapping>>> newPathMap = null;
			
			
			
			newPathMap = new HashMap<Integer,Vector<Vector<Mapping>>>();
			
			for(int i=0;i<maps.size(); i++)
			{
				String m = maps.get(i);
								
				HashMap<String,Vector<Vector<Mapping>>> pathal = getConflictMapComputed(m);
				Vector<Vector<Mapping>> path;
				
				if(pathal!=null){
					path = pathal.get(d); //path of mappings from m to d (finished)
				}else{
					path = conflictsMap.get(m).get(d); //path of mappings from m to d
				}
				
				if(path != null)
				{
					Mapping map = null;
					
					synchronized(this){
						
						map = align.get(getMapIndex(term, m));
					}
					
					
					Vector<Vector<Mapping>> newPath = new Vector<Vector<Mapping>>();
					
					for(Vector<Mapping> vm: path)
					{
						Vector<Mapping> vmn = new Vector<Mapping>(vm);
						if(!vmn.contains(map))
							vmn.add(map);
						newPath.add(vmn);
					}
					
					
					if(newPathMap.get(i) == null)
					{
						newPathMap.put(i, newPath);//adds this mapping to the new path
					}else
					{
						synchronized(this){
						
						newPath.addAll(newPathMap.get(i));
						newPathMap.put(i, minimalListMappings(newPath));//adds this mapping to the new path
						}
						
						}
					
				}
			}
			
			
			//add/updates the current paths of term to conflictsMap
			for(int i=0;i<maps.size(); i++)
			{
				Vector<Vector<Mapping>> path = newPathMap.get(i);
				
				if(path != null)
				{					
					if(confMapTerm.get(d) == null)
					{
						confMapTerm.put(d, path);
					}else{
						
						
						path.addAll(confMapTerm.get(d));
						
						Vector<Vector<Mapping>> ttt = null;
						
						synchronized(this){
						ttt = minimalListMappings(path);
						}
						
							confMapTerm.put(d, ttt);
						
					
					
					}
				}
				
				path=null;
				
				
			}
		}//ends for each disjoint term
		
		//check if it was computed all the conflict list for this term
		if(conflictsMapCurrent.get(0).compareTo(term) == 0 || result.isEmpty()){
			
			putConflictMapComputed(term, confMapTerm);
			
			synchronized(this){
			
			clsCheck.remove(term);
			
			}
			
			Cycles.remove(term);
		}else{
		}
				
		conflictsMapCurrent.remove(term);
		return result;

	}	

	//Returns all 'is_a' ancestors of a given term considering
	//only paths within its Ontology
	private Vector<String> getAncestors(String term)
	{
		Vector<String> ancestors = new Vector<String>(1,1);
		ancestors.add(term);
		if(ancestorMap.contains(term))
			ancestors.addAll(ancestorMap.get(term).keySet());
		return ancestors;
	}
	
	
	//Returns all 'is_a' ancestors of a given term considering
	//only paths within its Ontology
	private synchronized Vector<String> getDirectAncestors(String term)
	{
		Vector<String> result = ancestorDirectMap.get(term);
		if(result != null)
			return result;
		else return new Vector<String>();

	}	
	
	//Returns all 'is_a' ancestors of a given term in both Ontologies
	//considering all Mappings between the Ontologies
	private Vector<String> getExtendedAncestors(String term)
	{
		Vector<String> ancestors = getAncestors(term);
		int index;
		int size = 0;
		while(size != ancestors.size())
		{
			index = size;
			size = ancestors.size();
			for(int i = index; i < size; i++)
			{
				String a = ancestors.get(i);
				Vector<String> maps = getMappings(a);
				for(String m: maps)
				{
					Vector<String> newAncs = getAncestors(m);
					for(String s : newAncs)
						if(!ancestors.contains(s))
							ancestors.add(s);
				}
			}
		}
		return ancestors;
	}
	
	//Returns all terms mapped to the given term
	public synchronized Vector<String> getMappings(String term)
	{
		Vector<String> mappings = new Vector<String>(0,1);
		int id = Integer.parseInt(term.substring(1));
		if(term.startsWith("S"))
		{
			Vector<Integer> maps = align.getSourceMappings(id);
			for(Integer i : maps)
				mappings.add("T" + i);
		}
		else
		{
			Vector<Integer> maps = align.getTargetMappings(id);
			for(Integer i : maps)
				mappings.add("S" + i);
		}
		return mappings;
	}
	
	//Returns the index of the mapping between two given terms
	private int getMapIndex(String s, String t)
	{
		int source, target;
		if(s.startsWith("S"))
		{
			source = Integer.parseInt(s.substring(1));
			target = Integer.parseInt(t.substring(1));
		}
		else
		{
			source = Integer.parseInt(t.substring(1));
			target = Integer.parseInt(s.substring(1));
		}
		return align.getIndex(source, target);
	}
}