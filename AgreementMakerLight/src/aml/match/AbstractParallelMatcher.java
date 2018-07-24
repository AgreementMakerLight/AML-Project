/******************************************************************************
* Copyright 2013-2018 LASIGE                                                  *
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
* Abstract Matcher with parallel execution of match and rematch methods.      *
*                                                                             *
* @authors Daniel Faria                                                       *
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
import aml.alignment.SimpleAlignment;
import aml.alignment.AbstractMapping;
import aml.alignment.SimpleMapping;
import aml.match.PrimaryMatcher;
import aml.match.Rematcher;
import aml.match.UnsupportedEntityTypeException;
import aml.ontology.EntityType;
import aml.ontology.Ontology;
import aml.settings.InstanceMatchingCategory;
import aml.util.data.Map2Set;

public abstract class AbstractParallelMatcher implements PrimaryMatcher, Rematcher, SecondaryMatcher
{

//Attributes

	//The available CPU threads
	protected int threads;

//Constructors
	
	/**
	 * Constructs a new AbstractParallelMatcher
	 */
	public AbstractParallelMatcher()
	{
		threads = Runtime.getRuntime().availableProcessors();
	}

//Public Methods
	
	public abstract String getDescription();

	public abstract String getName();

	public abstract EntityType[] getSupportedEntityTypes();
	
	@Override
	public SimpleAlignment extendAlignment(Ontology o1, Ontology o2, SimpleAlignment maps, EntityType e, double thresh)
	{
		AML aml = AML.getInstance();
		Set<String> sources = o1.getEntities(e);
		Set<String> targets = o2.getEntities(e);
		sources.removeAll(maps.getSources());
		targets.removeAll(maps.getTargets());
		if(e.equals(EntityType.INDIVIDUAL))
		{
			sources.retainAll(aml.getSourceIndividualsToMatch());
			targets.retainAll(aml.getTargetIndividualsToMatch());
		}
		SimpleAlignment a = new SimpleAlignment();
		for(String i : sources)
		{
			Map2Set<String,String> toMap = new Map2Set<String,String>();
			for(String j : targets)
			{
				if(aml.getInstanceMatchingCategory().equals(InstanceMatchingCategory.SAME_CLASSES) &&
						!aml.getEntityMap().shareClass(i,j))
					continue;
				toMap.add(i,j);
			}
			a.addAll(mapInParallel(toMap,thresh));
		}
		return a;
	}
	
	@Override
	public SimpleAlignment match(Ontology o1, Ontology o2, EntityType e, double thresh)
	{
		AML aml = AML.getInstance();
		Set<String> sources = o1.getEntities(e);
		Set<String> targets = o2.getEntities(e);
		if(e.equals(EntityType.INDIVIDUAL))
		{
			sources.retainAll(aml.getSourceIndividualsToMatch());
			targets.retainAll(aml.getTargetIndividualsToMatch());
		}
		SimpleAlignment a = new SimpleAlignment();
		for(String i : sources)
		{
			Map2Set<String,String> toMap = new Map2Set<String,String>();
			for(String j : targets)
			{
				if(aml.getInstanceMatchingCategory().equals(InstanceMatchingCategory.SAME_CLASSES) &&
						!aml.getEntityMap().shareClass(i,j))
					continue;
				toMap.add(i,j);
			}
			a.addAll(mapInParallel(toMap,thresh));
		}
		return a;
	}
		
	@Override
	public SimpleAlignment rematch(Ontology o1, Ontology o2, SimpleAlignment a, EntityType e)
	{
		AML aml = AML.getInstance();
		SimpleAlignment maps = new SimpleAlignment();
		Map2Set<String,String> toMap = new Map2Set<String,String>();
		for(AbstractMapping m : a)
		{
			if(m instanceof SimpleMapping)
			{
				String source = (String)m.getEntity1();
				String target = (String)m.getEntity2();
				if(!aml.getEntityMap().getTypes(source).contains(e))
					continue;
				if(o1.contains(source) && o2.contains(target))
					toMap.add(source,target);
				else if(o1.contains(target) && o2.contains(source))
					toMap.add(target,source);
			}
		}
		maps.addAll(mapInParallel(toMap,0.0));
		return maps;
	}
	
//Protected Methods
	
	//Maps a table of classes in parallel, using all available threads
	protected SimpleAlignment mapInParallel(Map2Set<String, String> toMap, double thresh)
	{
		SimpleAlignment maps = new SimpleAlignment();
		ArrayList<MappingTask> tasks = new ArrayList<MappingTask>();
		for(String i : toMap.keySet())
			for(String j : toMap.get(i))
				tasks.add(new MappingTask(i,j));
        List<Future<SimpleMapping>> results;
		ExecutorService exec = Executors.newFixedThreadPool(threads);
		try
		{
			results = exec.invokeAll(tasks);
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
	        results = new ArrayList<Future<SimpleMapping>>();
		}
		exec.shutdown();
		for(Future<SimpleMapping> fm : results)
		{
			try
			{
				SimpleMapping m = fm.get();
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
	
	protected abstract double mapTwoEntities(String sId, String tId);
	
	//Callable class for mapping two classes
	protected class MappingTask implements Callable<SimpleMapping>
	{
		private String source;
		private String target;
		
		MappingTask(String s, String t)
	    {
			source = s;
	        target = t;
	    }
	        
	    @Override
	    public SimpleMapping call()
	    {
       		return new SimpleMapping(source,target,mapTwoEntities(source,target));
        }
	}
}