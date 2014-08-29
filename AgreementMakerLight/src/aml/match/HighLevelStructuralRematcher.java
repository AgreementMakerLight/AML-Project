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
* Rematches Ontologies by computing the high-level structural similarity      *
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

public class HighLevelStructuralRematcher implements Rematcher
{

	@Override
	public Alignment rematch(Alignment a)
	{
		System.out.println("Computing High-Level Structure Overlap");
		long time = System.currentTimeMillis()/1000;
		AML aml = AML.getInstance();
		Alignment maps = new Alignment();
		Alignment high = a.getHighLevelAlignment();
		RelationshipMap rMap = aml.getRelationshipMap();
		Ontology source = aml.getSource();
		Ontology target = aml.getTarget();
		for(Mapping m : a)
		{
			int sId = m.getSourceId();
			int tId = m.getTargetId();
			if(!source.isClass(sId) || ! target.isClass(tId))
				continue;
			Set<Integer> sourceAncestors = rMap.getHighLevelAncestors(sId);
			Set<Integer> targetAncestors = rMap.getHighLevelAncestors(tId);
			double maxSim = 0;
			for(Integer i : sourceAncestors)
			{
				for(Integer j : targetAncestors)
				{
					double sim = high.getSimilarity(i, j);
					if(sim > maxSim)
						maxSim = sim;
				}
			}
			maps.add(sId,tId,maxSim);
		}
		time = System.currentTimeMillis()/1000 - time;
		System.out.println("Finished in " + time + " seconds");
		return maps;
	}
}
