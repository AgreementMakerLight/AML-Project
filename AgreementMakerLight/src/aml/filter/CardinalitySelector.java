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
import aml.AML.SelectionType;

public class CardinalitySelector implements Selector
{
	
//Attributes
	
	private Alignment maps;
	private SelectionType type;
	private int cardinality;
	
//Constructors
		
	public CardinalitySelector(Alignment a, int card)
	{
		maps = a;
		cardinality = card;
		if(card == 1)
			type = SelectionType.STRICT;
		else
			type = SelectionType.MANY;
	}

//Public Methods
	
	/**
	 * @return the selection type of this Selector
	 */
	public SelectionType getSelectionType()
	{
		return type;
	}
	
	/**
	 * Selects matches greedily in descending order of similarity to obtain
	 * a one-to-one maximal alignment or a near one-to-one alignment
	 * @param thresh: the minimum similarity threshold
	 * @param s: the type of selection to carry out
	 * @return the selected Alignment
	 */
	public Alignment select(double thresh)
	{
		//Initialize Alignment to return
		Alignment selected = new Alignment();
		//Then sort the alignment
		maps.sort();
		//Then select Mappings in ranking order (by similarity)
		for(Mapping m : maps)
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