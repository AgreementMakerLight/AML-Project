/******************************************************************************
* Copyright 2013-2023 LASIGE                                                  *
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
* their instances.                                                            *
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
import aml.settings.EntityType;
import aml.util.Table2Set;

public class InstanceNeighborSimilarityMatcher implements PrimaryMatcher
{
	
//Attributes
	
	private static final String DESCRIPTION = "Matches individuals that have matching neighbors.";
	private static final String NAME = "Individual Neighbor Similarity Matcher";
	private static final EntityType[] SUPPORT = {EntityType.INDIVIDUAL};
	//Links to ontology data structures
	private AML aml;
	private RelationshipMap rels;
	
//Constructors
	
	public InstanceNeighborSimilarityMatcher()
	{
		aml = AML.getInstance();
		rels = aml.getRelationshipMap();
	}
	
//Public Methods
	
	@Override
	public String getDescription()
	{
		return DESCRIPTION;
	}

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public EntityType[] getSupportedEntityTypes()
	{
		return SUPPORT;
	}

	@Override
	public Alignment match(EntityType e, double thresh) throws UnsupportedEntityTypeException
	{
		checkEntityType(e);
		System.out.println("Matching Ontologies with Individual Neighbor Similarity Matcher");
		long time = System.currentTimeMillis()/1000;
		Table2Set<Integer,Integer> toMap = new Table2Set<Integer,Integer>();
		for(Integer s : aml.getSourceIndividualsToMatch())
			for(Integer t : aml.getTargetIndividualsToMatch())
				toMap.add(s, t);
		Alignment maps = mapInParallel(toMap, thresh);
		time = System.currentTimeMillis()/1000 - time;
		System.out.println("Finished in " + time + " seconds");
		return maps;
	}
	
//Private Methods
	
	private void checkEntityType(EntityType e) throws UnsupportedEntityTypeException
	{
		boolean check = false;
		for(EntityType t : SUPPORT)
		{
			if(t.equals(e))
			{
				check = true;
				break;
			}
		}
		if(!check)
			throw new UnsupportedEntityTypeException(e.toString());
	}
	
	//Maps a table of classes in parallel, using all available threads
	private Alignment mapInParallel(Table2Set<Integer,Integer> toMap, double thresh)
	{
		Alignment maps = new Alignment();
		ArrayList<MappingTask> tasks = new ArrayList<MappingTask>();
		for(Integer i : toMap.keySet())
			for(Integer j : toMap.get(i))
				tasks.add(new MappingTask(i,j));
        List<Future<Mapping>> results;
		ExecutorService exec = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
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
		 Set<Integer> sources = rels.getIndividualActiveRelations(sId);
		 Set<Integer> targets = rels.getIndividualActiveRelations(tId);
		 double sim = 0.0;
		 double union = 0.0;
		 for(Integer i : sources)
		 {
			 if(targets.contains(i))
				 sim++;
			 else
				 union++;
		 }
		 union += targets.size();
		 sim /= union;
		 return sim;
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