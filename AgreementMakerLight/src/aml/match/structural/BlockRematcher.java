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
* Rematches Ontologies by computing the high-level structural similarity      *
* between their classes.                                                      *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.match.structural;

import java.util.Set;

import aml.AML;
import aml.alignment.SimpleAlignment;
import aml.alignment.mapping.Mapping;
import aml.match.Matcher;
import aml.match.Rematcher;
import aml.ontology.EntityType;
import aml.ontology.Ontology;
import aml.ontology.semantics.EntityMap;

public class BlockRematcher extends Matcher implements Rematcher
{
	
//Attributes
	
	protected static final String DESCRIPTION = "Rematches classes by computing the fraction\n" +
											  "of mappings that fall within the blocks of the\n" +
											  "ontologies (i.e., have the same high-level\n" +
											  "classes.";
	protected static final String NAME = "Block Rematcher";
	protected static final EntityType[] SUPPORT = {EntityType.CLASS};

//Constructors
	
	public BlockRematcher(){}
	
//Public Methods
	
	@Override
	public SimpleAlignment rematch(Ontology o1, Ontology o2, SimpleAlignment a, EntityType e)
	{
		SimpleAlignment maps = new SimpleAlignment();
		if(!checkEntityType(e))
			return maps;
		System.out.println("Computing High-Level Structure Overlap");
		long time = System.currentTimeMillis()/1000;
		AML aml = AML.getInstance();
		
		SimpleAlignment high = a.getHighLevelAlignment();
		EntityMap rMap = aml.getEntityMap();
		for(Mapping<String> m : a)
		{
			String sId = m.getEntity1();
			String tId = m.getEntity2();
			if(!aml.getEntityMap().isClass(sId))
			{
				maps.add(m);
				continue;
			}
			Set<String> sourceAncestors = rMap.getHighLevelAncestors(sId);
			Set<String> targetAncestors = rMap.getHighLevelAncestors(tId);
			double maxSim = 0;
			for(String i : sourceAncestors)
			{
				for(String j : targetAncestors)
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