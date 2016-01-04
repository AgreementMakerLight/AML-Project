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
* Matches Ontologies by computing the neighbor structural similarity between  *
* their classes.                                                              *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.match;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import aml.AML;
import aml.ontology.RelationshipMap;
import aml.settings.NeighborSimilarityStrategy;
import aml.util.Table2Set;

public class NeighborSimilarityMatcher implements SecondaryMatcher, Rematcher
{
	
//Attributes
	
	//Links to ontology data structures
	private AML aml;
	private RelationshipMap rels;
	private Alignment input;
	private NeighborSimilarityStrategy strat;
	private boolean direct;
	//The available CPU threads
	private int threads;
	
//Constructors
	
	public NeighborSimilarityMatcher()
	{
		aml = AML.getInstance();
		rels = aml.getRelationshipMap();
		strat = NeighborSimilarityStrategy.MINIMUM;
		direct = aml.directNeighbors();
		threads = Runtime.getRuntime().availableProcessors();
	}
	
	public NeighborSimilarityMatcher(NeighborSimilarityStrategy s, boolean direct)
	{
		this();
		strat = s;
		this.direct = direct;
	}
	
//Public Methods
	
	@Override
	public Alignment extendAlignment(Alignment a, double thresh)
	{
		System.out.println("Extending Alignment with Neighbor Similarity Matcher");
		long time = System.currentTimeMillis()/1000;
		input = a;
		Table2Set<Integer,Integer> toMap = new Table2Set<Integer,Integer>();
		for(int i = 0; i < input.size(); i++)
		{
			Mapping m = input.get(i);
			if(!aml.getURIMap().isClass(m.getSourceId()))
				continue;
			Set<Integer> sourceSubClasses = rels.getSubClasses(m.getSourceId(),true);
			Set<Integer> targetSubClasses = rels.getSubClasses(m.getTargetId(),true);
			for(Integer s : sourceSubClasses)
			{
				if(input.containsSource(s))
					continue;
				for(Integer t : targetSubClasses)
				{
					if(input.containsTarget(t))
						continue;
					toMap.add(s, t);
				}
			}
			Set<Integer> sourceSuperClasses = rels.getSuperClasses(m.getSourceId(),true);
			Set<Integer> targetSuperClasses = rels.getSuperClasses(m.getTargetId(),true);
			for(Integer s : sourceSuperClasses)
			{
				if(input.containsSource(s))
					continue;
				for(Integer t : targetSuperClasses)
				{
					if(input.containsTarget(t))
						continue;
					toMap.add(s, t);
				}				
			}
		}
		Alignment maps = mapInParallel(toMap,thresh);
		time = System.currentTimeMillis()/1000 - time;
		System.out.println("Finished in " + time + " seconds");
		return maps;
	}

	@Override
	public Alignment rematch(Alignment a)
	{
		System.out.println("Computing Neighbor Similarity");
		long time = System.currentTimeMillis()/1000;
		input = a;
		Alignment maps = new Alignment();
		Table2Set<Integer,Integer> toMap = new Table2Set<Integer,Integer>();
		for(Mapping m : a)
		{
			int sId = m.getSourceId();
			int tId = m.getTargetId();
			//If the mapping is not a class mapping, we can't compute
			//its similarity, so we add it as is
			if(!aml.getURIMap().isClass(sId))
				maps.add(m);
			else
				toMap.add(sId, tId);
		}
		maps.addAll(mapInParallel(toMap,0.0));
		time = System.currentTimeMillis()/1000 - time;
		System.out.println("Finished in " + time + " seconds");
		return maps;
	}
	
//Private Methods
	
	//Maps a table of classes in parallel, using all available threads
	private Alignment mapInParallel(Table2Set<Integer,Integer> toMap, double thresh)
	{
		Alignment maps = new Alignment();
		ArrayList<MappingTask> tasks = new ArrayList<MappingTask>();
		for(Integer i : toMap.keySet())
			for(Integer j : toMap.get(i))
				tasks.add(new MappingTask(i,j));
        List<Future<Mapping>> results;
		ExecutorService exec = Executors.newFixedThreadPool(threads);
		try
		{
			results = exec.invokeAll(tasks);
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
	        results = new ArrayList<Future<Mapping>>();
		}
		exec.shutdown();
		for(Future<Mapping> fm : results)
		{
			try
			{
				Mapping m = fm.get();
				if(m.getSimilarity() >= thresh)
					maps.add(m);
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
		return maps;
	}
	
	//Computes the neighbor structural similarity between two terms by
	//checking for mappings between all their ancestors and descendants
	private double mapTwoTerms(int sId, int tId)
	{
		double parentSim = 0.0;
		double childrenSim = 0.0;
		if(!strat.equals(NeighborSimilarityStrategy.DESCENDANTS))
		{
			double parentTotal = 0.0;
			Set<Integer> sourceParents = rels.getSuperClasses(sId,direct);
			Set<Integer> targetParents = rels.getSuperClasses(tId,direct);
			for(Integer i : sourceParents)
			{
				parentTotal += 0.5 / rels.getDistance(sId,i);
				for(Integer j : targetParents)
					parentSim += input.getSimilarity(i,j) /
						Math.sqrt(rels.getDistance(sId,i) * rels.getDistance(tId, j));
			}
			for(Integer i : targetParents)
				parentTotal += 0.5 / rels.getDistance(tId,i);
			parentSim /= parentTotal;
		}
		if(!strat.equals(NeighborSimilarityStrategy.ANCESTORS))
		{
			double childrenTotal = 0.0;
			Set<Integer> sourceChildren = rels.getSubClasses(sId,direct);
			Set<Integer> targetChildren = rels.getSubClasses(tId,direct);
			for(Integer i : sourceChildren)
			{
				childrenTotal += 0.5 / rels.getDistance(i,sId);
				for(Integer j : targetChildren)
					childrenSim += input.getSimilarity(i,j) /
						Math.sqrt(rels.getDistance(i,sId) * rels.getDistance(j,tId));
			}
			for(Integer i : targetChildren)
				childrenTotal += 0.5 / rels.getDistance(i,tId);
			childrenSim /= childrenTotal;
		}
		if(strat.equals(NeighborSimilarityStrategy.ANCESTORS))
			return parentSim;
		else if(strat.equals(NeighborSimilarityStrategy.DESCENDANTS))
			return childrenSim;
		else if(strat.equals(NeighborSimilarityStrategy.MINIMUM))
			return Math.min(parentSim,childrenSim);
		else if(strat.equals(NeighborSimilarityStrategy.MAXIMUM))
			return Math.max(parentSim,childrenSim);
		else
			return (parentSim + childrenSim)*0.5;
	}
	
	//Callable class for mapping two classes
	private class MappingTask implements Callable<Mapping>
	{
		private int source;
		private int target;
		
		MappingTask(int s, int t)
	    {
			source = s;
	        target = t;
	    }
	        
	    @Override
	    public Mapping call()
	    {
       		return new Mapping(source,target,mapTwoTerms(source,target));
        }
	}
}