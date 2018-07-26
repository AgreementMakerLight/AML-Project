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
* Matches Ontologies by computing the neighbor structural similarity between  *
* their classes.                                                              *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.match.structural;

import java.util.Set;

import aml.AML;
import aml.alignment.Mapping;
import aml.alignment.SimpleAlignment;
import aml.match.AbstractParallelMatcher;
import aml.ontology.EntityType;
import aml.ontology.Ontology;
import aml.ontology.semantics.EntityMap;
import aml.util.data.Map2Set;

public class NeighborSimilarityMatcher extends AbstractParallelMatcher
{
	
//Attributes
	
	protected static final String DESCRIPTION = "Matches classes that have matching neighbor\n" +
											  "classes (ancestors and/or descendants) by\n" +
											  "propagating neighbor similarity.";
	protected static final String NAME = "Neighbor Similarity Matcher";
	protected static final EntityType[] SUPPORT = {EntityType.CLASS};
	//Links to ontology data structures
	private AML aml;
	private EntityMap rels;
	private SimpleAlignment input;
	private NeighborSimilarityStrategy strat;
	private boolean direct;
	
//Constructors
	
	public NeighborSimilarityMatcher()
	{
		aml = AML.getInstance();
		rels = aml.getEntityMap();
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
	public SimpleAlignment extendAlignment(Ontology o1, Ontology o2, SimpleAlignment a, EntityType e, double thresh)
	{
		if(!checkEntityType(e))
			return new SimpleAlignment();
		System.out.println("Extending Alignment with Neighbor Similarity Matcher");
		long time = System.currentTimeMillis()/1000;
		input = a;
		Map2Set<String,String> toMap = new Map2Set<String,String>();
		for(int i = 0; i < input.size(); i++)
		{
			Mapping m = input.get(i);
			if(!aml.getEntityMap().isClass((String)m.getEntity1()))
				continue;
			Set<String> sourceSubClasses = rels.getSubclasses((String)m.getEntity1(),1);
			Set<String> targetSubClasses = rels.getSubclasses((String)m.getEntity2(),1);
			for(String s : sourceSubClasses)
			{
				if(input.containsSource(s))
					continue;
				for(String t : targetSubClasses)
				{
					if(input.containsTarget(t))
						continue;
					toMap.add(s, t);
				}
			}
			Set<String> sourceSuperClasses = rels.getSuperclasses((String)m.getEntity1(),1);
			Set<String> targetSuperClasses = rels.getSuperclasses((String)m.getEntity2(),1);
			for(String s : sourceSuperClasses)
			{
				if(input.containsSource(s))
					continue;
				for(String t : targetSuperClasses)
				{
					if(input.containsTarget(t))
						continue;
					toMap.add(s, t);
				}				
			}
		}
		SimpleAlignment maps = mapInParallel(toMap,thresh);
		time = System.currentTimeMillis()/1000 - time;
		System.out.println("Finished in " + time + " seconds");
		return maps;
	}
	
	@Override
	/**
	 * WARNING: NeighborSimilarityMatcher does not compute direct alignments. It implements
	 * this method as it is convenient to extend the AbstractParallelMatcher
	 * @return null
	 */
	public SimpleAlignment match(Ontology o1, Ontology o2, EntityType e, double thresh)
	{
		return null;
	}

	@Override
	public SimpleAlignment rematch(Ontology o1, Ontology o2, SimpleAlignment a, EntityType e)
	{
		input = a;
		return super.rematch(o1, o2, a, e);
	}
	
//Protected Methods
	
	//Computes the neighbor structural similarity between two terms by
	//checking for mappings between all their ancestors and descendants
	protected double mapTwoEntities(String sId, String tId)
	{
		double parentSim = 0.0;
		double childrenSim = 0.0;
		if(!strat.equals(NeighborSimilarityStrategy.DESCENDANTS))
		{
			double parentTotal = 0.0;
			Set<String> sourceParents, targetParents;
			if(direct)
			{
				sourceParents = rels.getSuperclasses(sId,1);
				targetParents = rels.getSuperclasses(tId,1);
			}
			else
			{
				sourceParents = rels.getSuperclasses(sId);
				targetParents = rels.getSuperclasses(tId);
			}
			for(String i : sourceParents)
			{
				parentTotal += 0.5 / rels.getDistance(sId,i);
				for(String j : targetParents)
					parentSim += input.getSimilarity(i,j) /
						Math.sqrt(rels.getDistance(sId,i) * rels.getDistance(tId, j));
			}
			for(String i : targetParents)
				parentTotal += 0.5 / rels.getDistance(tId,i);
			parentSim /= parentTotal;
		}
		if(!strat.equals(NeighborSimilarityStrategy.ANCESTORS))
		{
			double childrenTotal = 0.0;
			Set<String> sourceChildren, targetChildren;
			if(direct)
			{
				sourceChildren = rels.getSuperclasses(sId,1);
				targetChildren = rels.getSuperclasses(tId,1);
			}
			else
			{
				sourceChildren = rels.getSuperclasses(sId);
				targetChildren = rels.getSuperclasses(tId);
			}			
			for(String i : sourceChildren)
			{
				childrenTotal += 0.5 / rels.getDistance(i,sId);
				for(String j : targetChildren)
					childrenSim += input.getSimilarity(i,j) /
						Math.sqrt(rels.getDistance(i,sId) * rels.getDistance(j,tId));
			}
			for(String i : targetChildren)
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
}