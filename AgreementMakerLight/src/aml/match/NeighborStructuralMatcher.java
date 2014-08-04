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
* @date 31-07-2014                                                            *
* @version 2.0                                                                *
******************************************************************************/
package aml.match;

import java.util.Set;

import aml.AML;
import aml.ontology.Ontology;
import aml.ontology.RelationshipMap;
import aml.util.StringParser;

public class NeighborStructuralMatcher implements SecondaryMatcher, Rematcher
{
	
//Attributes
	
	//Links to ontology data structures
	private RelationshipMap rels;
	private Ontology source;
	private Ontology target;
	private Alignment input;
	
//Constructors
	
	public NeighborStructuralMatcher()
	{
		AML aml = AML.getInstance();
		rels = aml.getRelationshipMap();
		source = aml.getSource();
		target = aml.getTarget();
	}
	
//Public Methods
	
	@Override
	public Alignment extendAlignment(Alignment a, double thresh)
	{
		input = a;
		Alignment maps = new Alignment();
		for(int i = 0; i < input.size(); i++)
		{
			Mapping m = input.get(i);
/*			Set<Integer> sourceSubClasses = rels.getSubClasses(m.getSourceId(),true);
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
*/			Set<Integer> sourceSuperClasses = rels.getSuperClasses(m.getSourceId(),true);
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
		return maps;
	}

	@Override
	public Alignment rematch(Alignment a)
	{
		Alignment maps = new Alignment();
		for(Mapping m : a)
		{
			int sId = m.getSourceId();
			int tId = m.getTargetId();
			maps.add(sId,tId,mapTwoTerms(sId,tId));
		}
		return maps;
	}
	
	//Computes the neighbor structural similarity between two terms by
	//checking for mappings between all their ancestors and descendants
	private double mapTwoTerms(int sId, int tId)
	{
		if(!source.isClass(sId) || ! target.isClass(tId))
			return 0.0;
		Set<Integer> sourceParents = rels.getSuperClasses(sId,false);
		Set<Integer> targetParents = rels.getSuperClasses(tId,false);
		//Set<Integer> sourceChildren = rels.getSubClasses(sId,false);
		//Set<Integer> targetChildren = rels.getSubClasses(tId,false);
		double union = 0.0;
		double sim = 0.0;
		for(Integer i : sourceParents)
		{
			for(Integer j : targetParents)
			{
				if(input.containsMapping(i,j))
					sim += 2 / (rels.getDistance(sId,i) + rels.getDistance(tId, j));
				else
					union += 2 / (rels.getDistance(sId,i) + rels.getDistance(tId, j));
			}
		}
/*		for(Integer i : sourceChildren)
		{
			for(Integer j : targetChildren)
			{
				if(input.containsMapping(i,j))
					sim += 2 / (rels.getDistance(i,sId) + rels.getDistance(j,tId));
				else
					union += 2 / (rels.getDistance(i,sId) + rels.getDistance(j,tId));
			}
		}*/
		return sim/(sim+union);
	}
}
