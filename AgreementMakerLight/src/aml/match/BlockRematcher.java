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
import aml.alignment.Alignment;
import aml.alignment.SimpleMapping;
import aml.ontology.EntityType;
import aml.ontology.EntityMap;

public class BlockRematcher implements Rematcher
{
	
//Attributes
	
	private static final String DESCRIPTION = "Rematches classes by computing the fraction\n" +
											  "of mappings that fall within the blocks of the\n" +
											  "ontologies (i.e., have the same high-level\n" +
											  "classes.";
	private static final String NAME = "Block Rematcher";
	private static final EntityType[] SUPPORT = {EntityType.CLASS};

//Constructors
	
	public BlockRematcher(){}
	
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
	public Alignment rematch(Alignment a, EntityType e) throws UnsupportedEntityTypeException
	{
		checkEntityType(e);
		System.out.println("Computing High-Level Structure Overlap");
		long time = System.currentTimeMillis()/1000;
		AML aml = AML.getInstance();
		Alignment maps = new Alignment();
		Alignment high = a.getHighLevelAlignment();
		EntityMap rMap = aml.getEntityMap();
		for(SimpleMapping m : a)
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
}
