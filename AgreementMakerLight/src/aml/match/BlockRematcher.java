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
* Rematches Ontologies by computing the high-level structural similarity      *
* between their classes.                                                      *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.match;

import java.util.Set;

import aml.AML;
import aml.ontology.RelationshipMap;

public class BlockRematcher implements Rematcher
{
	
//Constructors
	
	public BlockRematcher(){}
	
//Public Methods
	
	public static String description()
	{
		return "This rematching algorithm infers a block alignment\n" +
			   "by looking at the overlap between the descendants\n" +
			   "of high-level classes in the input alignment. It\n" +
			   "then defines similarity between two classes as\n" +
			   "the highest overlap between their blocks.";
	}
	
	@Override
	public Alignment rematch(Alignment a)
	{
		System.out.println("Computing High-Level Structure Overlap");
		long time = System.currentTimeMillis()/1000;
		AML aml = AML.getInstance();
		Alignment maps = new Alignment();
		Alignment high = a.getHighLevelAlignment();
		RelationshipMap rMap = aml.getRelationshipMap();
		for(Mapping m : a)
		{
			int sId = m.getSourceId();
			int tId = m.getTargetId();
			if(!aml.getURIMap().isClass(sId))
			{
				maps.add(m);
				continue;
			}
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
