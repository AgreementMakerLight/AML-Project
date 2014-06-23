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
* Collection of selection algorithm that reduces an Alignment to the desired  *
* cardinality and/or exclude potentially erroneous Mappings.                  *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 23-06-2014                                                            *
* @version 2.0                                                                *
******************************************************************************/
package aml.filter;

import aml.match.Alignment;
import aml.match.Mapping;
import aml.AML.SelectionType;

public class Selector
{
	
//Attributes
	
	private Alignment maps;
	private SelectionType type;
	
//Constructors
	
	public Selector(Alignment a)
	{
		maps = a;
		setSelectionType();
	}
	
	public Selector(Alignment a, SelectionType s)
	{
		maps = a;
		if(s == null || s.equals(SelectionType.AUTO))
			setSelectionType();
		else
			type = s;
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
	 * @param a: the Alignment to select
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
			//Otherwise, add it if it obeys the rules for the chosen SelectionType
			if(type.equals(SelectionType.MANY) ||
					(type.equals(SelectionType.STRICT) && !selected.containsConflict(m)) ||
					(type.equals(SelectionType.PERMISSIVE) && !selected.containsBetterMapping(m)))
				selected.add(new Mapping(m));
		}
		return selected;
	}
	
	/**
	 * Selects matches greedily in descending order of similarity, excluding
	 * those that exceed the desired cardinality
	 * @param a: the Alignment to select
	 * @param thresh: the minimum similarity threshold
	 * @param cardinality: the maximum cardinality of the selected Alignment
	 * @return the selected Alignment
	 */
	public Alignment select(double thresh, int cardinality)
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
			int sourceId = m.getSourceId();
			int sourceCard = selected.getSourceMappings(sourceId).size();
			int targetId = m.getTargetId();
			int targetCard = selected.getTargetMappings(targetId).size();
			if(sourceCard < cardinality && targetCard < cardinality)
				selected.add(new Mapping(m));
		}
		return selected;
	}
	
	/**
	 * Selects matches that are subsumed by the high level Alignment
	 * inferred from this Alignment
	 * @param a: the Alignment to select
	 * @return the selected Alignment
	 */
	public static Alignment selectHighLevel(Alignment a)
	{
		Alignment highLevel = a.getHighLevelAlignment(0.05);
		Alignment selected = new Alignment();
		for(Mapping m : a)
			if(highLevel.containsAncestralMapping(m.getSourceId(), m.getTargetId()))
				selected.add(new Mapping(m));
		return selected;
	}
	
//Private Methods
	
	private void setSelectionType()
	{
		double cardinality = maps.cardinality();
		if(cardinality > 1.4)
			type = SelectionType.MANY;
		else if(cardinality > 1.02)
			type = SelectionType.PERMISSIVE;
		else
			type = SelectionType.STRICT;
	}
}