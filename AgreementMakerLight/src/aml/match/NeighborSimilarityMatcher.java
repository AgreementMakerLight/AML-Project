/******************************************************************************
* Copyright 2013-2014 LASIGE                                                  *
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
* @date 10-09-2014                                                            *
* @version 2.1                                                                *
******************************************************************************/
package aml.match;

import java.util.Set;

import aml.AML;
import aml.ontology.Ontology;
import aml.ontology.RelationshipMap;
import aml.settings.NeighborSimilarityStrategy;

public class NeighborSimilarityMatcher implements SecondaryMatcher, Rematcher
{
	
//Attributes
	
	//Links to ontology data structures
	private RelationshipMap rels;
	private Ontology source;
	private Ontology target;
	private Alignment input;
	private NeighborSimilarityStrategy strat;
	private boolean direct;
	
//Constructors
	
	public NeighborSimilarityMatcher()
	{
		AML aml = AML.getInstance();
		rels = aml.getRelationshipMap();
		source = aml.getSource();
		target = aml.getTarget();
		strat = NeighborSimilarityStrategy.MINIMUM;
		direct = true; //TODO: Update this from class AML
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
		Alignment maps = new Alignment();
		for(int i = 0; i < input.size(); i++)
		{
			Mapping m = input.get(i);
			Set<Integer> sourceSubClasses = rels.getSubClasses(m.getSourceId(),true);
			Set<Integer> targetSubClasses = rels.getSubClasses(m.getTargetId(),true);
			for(Integer s : sourceSubClasses)
			{
				if(input.containsSource(s) || maps.containsSource(s))
					continue;
				for(Integer t : targetSubClasses)
				{
					if(input.containsTarget(t) || maps.containsTarget(t))
						continue;
					double sim = mapTwoTerms(s, t);
					if(sim >= thresh)
						maps.add(s,t,sim);
				}
			}
			Set<Integer> sourceSuperClasses = rels.getSuperClasses(m.getSourceId(),true);
			Set<Integer> targetSuperClasses = rels.getSuperClasses(m.getTargetId(),true);
			for(Integer s : sourceSuperClasses)
			{
				if(input.containsSource(s) || maps.containsSource(s))
					continue;
				for(Integer t : targetSuperClasses)
				{
					if(input.containsTarget(t) || maps.containsTarget(t))
						continue;
					double sim = mapTwoTerms(s, t);
					if(sim >= thresh)
						maps.add(s,t,sim);
				}
			}
		}
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
		for(Mapping m : a)
		{
			int sId = m.getSourceId();
			int tId = m.getTargetId();
			maps.add(sId,tId,mapTwoTerms(sId,tId));
		}
		time = System.currentTimeMillis()/1000 - time;
		System.out.println("Finished in " + time + " seconds");
		return maps;
	}
	
//Private Methods
	
	//Computes the neighbor structural similarity between two terms by
	//checking for mappings between all their ancestors and descendants
	private double mapTwoTerms(int sId, int tId)
	{
		if(!source.isClass(sId) || ! target.isClass(tId))
			return 0.0;
		Set<Integer> sourceParents = rels.getSuperClasses(sId,direct);
		Set<Integer> targetParents = rels.getSuperClasses(tId,direct);
		Set<Integer> sourceChildren = rels.getSubClasses(sId,direct);
		Set<Integer> targetChildren = rels.getSubClasses(tId,direct);
		double parentSim = 0.0;
		double parentTotal = 0.0;
		double childrenSim = 0.0;
		double childrenTotal = 0.0;
		if(!strat.equals(NeighborSimilarityStrategy.DESCENDANTS))
		{
			for(Integer i : sourceParents)
			{
				parentTotal += 0.5 / Math.sqrt(rels.getDistance(sId,i));
				for(Integer j : targetParents)
					parentTotal += input.getSimilarity(i,j) /
						Math.sqrt((rels.getDistance(sId,i) + rels.getDistance(tId, j))*0.5);
			}
			for(Integer i : targetParents)
				parentTotal += 0.5 / Math.sqrt(rels.getDistance(tId,i));
			parentSim /= parentTotal;
		}
		if(!strat.equals(NeighborSimilarityStrategy.ANCESTORS))
		{
			for(Integer i : sourceChildren)
			{
				childrenTotal += 0.5 / Math.sqrt(rels.getDistance(i,sId));
				for(Integer j : targetChildren)
					childrenSim += input.getSimilarity(i,j) /
						Math.sqrt((rels.getDistance(i,sId) + rels.getDistance(j,tId))*0.5);
			}
			for(Integer i : targetChildren)
				childrenTotal += 0.5 / Math.sqrt(rels.getDistance(i,tId));
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