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
* Rematches Ontologies by computing the neighbor structural similarity        *
* between their classes.                                                      *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 07-07-2014                                                            *
* @version 2.0                                                                *
******************************************************************************/
package aml.match;

import java.util.Set;

import aml.AML;
import aml.ontology.Ontology;
import aml.ontology.RelationshipMap;

public class NeighborStructuralRematcher implements Rematcher
{

	@Override
	public Alignment rematch(Alignment a)
	{
		AML aml = AML.getInstance();
		Alignment maps = new Alignment();
		RelationshipMap rMap = aml.getRelationshipMap();
		Ontology source = aml.getSource();
		Ontology target = aml.getTarget();
		for(Mapping m : a)
		{
			int sId = m.getSourceId();
			int tId = m.getTargetId();
			if(!source.isClass(sId) || ! target.isClass(tId))
				continue;
			Set<Integer> sourceParents = rMap.getSuperClasses(sId,true);
			Set<Integer> targetParents = rMap.getSuperClasses(tId,true);
			Set<Integer> sourceChildren = rMap.getSubClasses(sId,true);
			Set<Integer> targetChildren = rMap.getSubClasses(tId,true);
			double total = Math.min(sourceParents.size(), targetParents.size()) +
					Math.min(sourceChildren.size(), targetChildren.size());
			double sim = 0.0;
			for(Integer i : sourceParents)
			{
				for(Integer j : targetParents)
				{
					if(a.containsMapping(i,j))
					{
						sim++;
						break;
					}
				}
			}
			for(Integer i : sourceChildren)
			{
				for(Integer j : targetChildren)
				{
					if(a.containsMapping(i,j))
					{
						sim++;
						break;
					}
				}
			}
			maps.add(sId,tId,sim/total);
		}
		return maps;
	}

}
