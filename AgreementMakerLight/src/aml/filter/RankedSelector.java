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
* Selector that reduces an Alignment to strict, permissive or hybrid 1-to-1   *
* cardinality.                                                                *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 07-07-2014                                                            *
* @version 2.0                                                                *
******************************************************************************/
package aml.filter;

import aml.match.Alignment;
import aml.match.Mapping;
import aml.AML;
import aml.AML.SelectionType;

public class RankedSelector implements Selector
{
	
//Attributes
	
	private SelectionType type;
	
//Constructors
	
	public RankedSelector()
	{
		AML aml = AML.getInstance();
		type = aml.getSelectionType();
	}
	
	public RankedSelector(SelectionType s)
	{
		type = s;
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
			//Otherwise, add it if it obeys the rules for the chosen SelectionType
			if(type.equals(SelectionType.MANY) ||
					(type.equals(SelectionType.STRICT) && !selected.containsConflict(m)) ||
					(type.equals(SelectionType.PERMISSIVE) && !selected.containsBetterMapping(m)))
				selected.add(new Mapping(m));
			else if(type.equals(SelectionType.HYBRID))
			{
				int sourceId = m.getSourceId();
				int sourceCard = selected.getSourceMappings(sourceId).size();
				int targetId = m.getTargetId();
				int targetCard = selected.getTargetMappings(targetId).size();
				if((sourceCard < 2 && targetCard < 2 && m.getSimilarity() > 0.75) ||
						!selected.containsBetterMapping(m))
					selected.add(new Mapping(m));
			}
		}
		return selected;
	}
}