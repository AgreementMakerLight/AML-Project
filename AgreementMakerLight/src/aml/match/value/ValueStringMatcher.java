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
* Matching algorithm that maps individuals by comparing their ValueMap        *
* entries through the ISub string similarity metric.                          *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.match.value;


import aml.match.AbstractParallelMatcher;
import aml.ontology.EntityType;
import aml.util.similarity.Similarity;

public class ValueStringMatcher extends AbstractParallelMatcher
{
	
//Attributes
	
	protected static final String DESCRIPTION = "Matches individuals by comparing their ValueMap\n" +
											  "entries using the ISub string similarity metric";
	protected static final String NAME = "Value String Matcher";
	protected static final EntityType[] SUPPORT = {EntityType.INDIVIDUAL};
	
//Constructors
	
	public ValueStringMatcher()
	{
		super();
		description = DESCRIPTION;
		name = NAME;
		support = SUPPORT;
	}
	
//Protected Methods
		
	@Override
	protected double mapTwoEntities(String sId, String tId)
	{
		double dataSim = 0.0;
		for(String sd : sVal.getProperties(sId))
		{
			for(String sv : sVal.getValues(sId,sd))
			{
				for(String td : tVal.getProperties(tId))
				{
					double weight = (td==sd ? 1.0 : 0.9);
					for(String tv : tVal.getValues(tId,td))
						dataSim = Math.max(Similarity.nameSimilarity(sv, tv, false) * weight, dataSim);
				}
			}
		}
		return dataSim;
	}
}