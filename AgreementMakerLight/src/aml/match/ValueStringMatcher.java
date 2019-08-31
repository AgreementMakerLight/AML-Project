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
* Matching algorithm that maps individuals by comparing their ValueMap        *
* entries through the ISub string similarity metric.                          *
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
import aml.match.Alignment;
import aml.match.Mapping;
import aml.ontology.Ontology;
import aml.ontology.RelationshipMap;
import aml.ontology.ValueMap;
import aml.settings.EntityType;
import aml.settings.InstanceMatchingCategory;
import aml.settings.MappingRelation;
import aml.util.NameSimilarity;
import aml.util.Table2Set;

public class ValueStringMatcher implements PrimaryMatcher, SecondaryMatcher, Rematcher
{
	
//Attributes
	
	private static final String DESCRIPTION = "Matches individuals by comparing their ValueMap\n" +
											  "entries using the ISub string similarity metric";
	private static final String NAME = "Value String Matcher";
	private static final EntityType[] SUPPORT = {EntityType.INDIVIDUAL};
	private AML aml;
	private Ontology source;
	private Ontology target;
	private ValueMap sVal;
	private ValueMap tVal;
	private NameSimilarity ns = null;
	//The available CPU threads
	private int threads;
	
//Constructors
	
	public ValueStringMatcher()
	{
		ns = new NameSimilarity(false);
		aml = AML.getInstance();
		source = aml.getSource();
		target = aml.getTarget();
		sVal = source.getValueMap();
		tVal = target.getValueMap();
		threads = Runtime.getRuntime().availableProcessors();
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
	public Alignment extendAlignment(Alignment a, EntityType e, double thresh) throws UnsupportedEntityTypeException
	{
		checkEntityType(e);
		System.out.println("Running Value String Matcher");
		long time = System.currentTimeMillis()/1000;
		RelationshipMap rels = aml.getRelationshipMap();
		Table2Set<Integer,Integer> toMap = new Table2Set<Integer,Integer>();
		for(int i = 0; i < a.size(); i++)
		{
			Mapping input = a.get(i);
			if(!aml.getURIMap().isIndividual(input.getSourceId()))
				continue;
			Set<Integer> sourceChildren = rels.getIndividualActiveRelations(input.getSourceId());
			Set<Integer> targetChildren = rels.getIndividualActiveRelations(input.getTargetId());
			for(Integer s : sourceChildren)
			{
				if(a.containsSource(s) || !aml.getURIMap().isIndividual(s))
					continue;
				for(Integer t : targetChildren)
				{
					if(a.containsTarget(t))
						continue;
					boolean checkRels = false;
					for(Integer r1 : rels.getIndividualProperties(input.getSourceId(), s))
					{
						if(checkRels)
							break;
						for(Integer r2 : rels.getIndividualProperties(input.getTargetId(), t))
						{
							if(r1 == r2 || a.contains(r1, r2, MappingRelation.EQUIVALENCE))
							{
								checkRels = true;
								break;
							}
						}
					}
					if(checkRels)
						toMap.add(s,t);
				}
			}
			Set<Integer> sourceParents = rels.getIndividualPassiveRelations(input.getSourceId());
			Set<Integer> targetParents = rels.getIndividualPassiveRelations(input.getTargetId());
			for(Integer s : sourceParents)
			{
				if(a.containsSource(s))
					continue;
				for(Integer t : targetParents)
				{
					if(a.containsTarget(t))
						continue;
					boolean checkRels = false;
					for(Integer r1 : rels.getIndividualProperties(s, input.getSourceId()))
					{
						if(checkRels)
							break;
						for(Integer r2 : rels.getIndividualProperties(t, input.getTargetId()))
						{
							if(r1 == r2 || a.contains(r1, r2, MappingRelation.EQUIVALENCE))
							{
								checkRels = true;
								break;
							}
						}
					}
					if(checkRels)
						toMap.add(s, t);
				}
			}
		}
		Alignment ext = mapInParallel(toMap,thresh);
		time = System.currentTimeMillis()/1000 - time;
		System.out.println("Finished in " + time + " seconds");
		return ext;		
	}
	
	@Override
	public Alignment match(EntityType e, double thresh) throws UnsupportedEntityTypeException
	{
		checkEntityType(e);
		System.out.println("Running Value String Matcher");
		long time = System.currentTimeMillis()/1000;
		Set<Integer> sources = sVal.getIndividuals();
		sources.retainAll(aml.getSourceIndividualsToMatch());
		Set<Integer> targets = tVal.getIndividuals();
		targets.retainAll(aml.getTargetIndividualsToMatch());
		Alignment a = new Alignment();
		for(Integer i : sources)
		{
			Table2Set<Integer,Integer> toMap = new Table2Set<Integer,Integer>();
			for(Integer j : targets)
			{
				if(aml.getInstanceMatchingCategory().equals(InstanceMatchingCategory.SAME_CLASSES) &&
						!aml.getRelationshipMap().shareClass(i,j))
					continue;
				toMap.add(i,j);
			}
			a.addAll(mapInParallel(toMap,thresh));
		}
		time = System.currentTimeMillis()/1000 - time;
		System.out.println("Finished in " + time + " seconds");
		return a;
	}
		
	@Override
	public Alignment rematch(Alignment a, EntityType e) throws UnsupportedEntityTypeException
	{
		checkEntityType(e);
		System.out.println("Computing Value String Similarity");
		long time = System.currentTimeMillis()/1000;
		Alignment maps = new Alignment();
		Table2Set<Integer,Integer> toMap = new Table2Set<Integer,Integer>();
		for(Mapping m : a)
		{
			if(aml.getURIMap().getType(m.getSourceId()).equals(e))
				toMap.add(m.getSourceId(),m.getTargetId());
		}
		maps.addAll(mapInParallel(toMap,0.0));
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
				tasks.add(new MappingTask(i,j,thresh));
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
	
	//Computes the maximum String similarity between two Classes by doing a
	//pairwise comparison of all their names
	private double mapTwoEntities(int sId, int tId, double thresh)
	{
		double dataSim = 0.0;
		for(Integer sd : sVal.getProperties(sId))
		{
			for(String sv : sVal.getValues(sId,sd))
			{
				for(Integer td : tVal.getProperties(tId))
				{
					double weight = (td==sd ? 1.0 : 0.9);
					for(String tv : tVal.getValues(tId,td))
						dataSim = Math.max(ns.nameSimilarity(sv, tv, thresh) * weight, dataSim);
				}
			}
		}
		return dataSim;
	}
	
	//Callable class for mapping two classes
	private class MappingTask implements Callable<Mapping>
	{
		private int source;
		private int target;
		private double threshold;
		
		MappingTask(int s, int t, double thresh)
	    {
			source = s;
	        target = t;
	        threshold = thresh;
	    }
	        
	    @Override
	    public Mapping call()
	    {
       		return new Mapping(source,target,mapTwoEntities(source,target,threshold));
        }
	}
}