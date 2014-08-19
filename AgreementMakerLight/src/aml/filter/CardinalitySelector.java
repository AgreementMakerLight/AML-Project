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
* Selector that reduces an Alignment to the specified cardinality.            *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 07-07-2014                                                            *
* @version 2.0                                                                *
******************************************************************************/
package aml.filter;

import aml.match.Alignment;
import aml.match.Mapping;

public class CardinalitySelector implements Selector
{
	
//Attributes
	
	private int cardinality;
	
//Constructors
		
	public CardinalitySelector(int card)
	{
		cardinality = card;
	}

//Public Methods
	
	@Override
	public Alignment select(Alignment a, double thresh)
	{
		//Initialize Alignment to return
		Alignment selected = new Alignment();
		//Then sort the alignment
		a.sort();
		//Then select Mappings in ranking order (by similarity)
		for(Mapping m : a)
		{
			//If a Mapping has similarity below the threshold, end the loop
			if(m.getSimilarity() < thresh)
				break;
			//Otherwise, add it if it is within the desired cardinality
			int sourceId = m.getSourceId();
			int sourceCard = selected.getSourceMappings(sourceId).size();
			int targetId = m.getTargetId();
			int targetCard = selected.getTargetMappings(targetId).size();
			if(sourceCard < cardinality && targetCard < cardinality)
				selected.add(new Mapping(m));
		}
		return selected;
	}
}