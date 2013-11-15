/******************************************************************************
* Copyright 2013-2013 LASIGE                                                  *
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
* @date 22-10-2013                                                            *
******************************************************************************/
package aml.match;

public class Selector
{
	
//Constructors
	
	private Selector(){}

//Public Methods
	
	/**
	 * Selects matches greedily in descending order of similarity, excluding
	 * those that excede the desired cardinality
	 * @param a: the Alignment to select
	 * @param thresh: the minimum similarity threshold
	 * @param cardinality: the maximum cardinality of the selected Alignment
	 * @return the selected Alignment
	 */
	public static Alignment selectCardinality(Alignment a, double thresh, double cardinality)
	{
		//Initialize Alignment to return
		Alignment selected = new Alignment(a.getSource(),a.getTarget());
		//Start by boosting the reciprocal bestMatches by 10%
		a.boostBestMatches(0.1);
		//Then sort the alignment
		a.sort();
		//Then select Mappings in ranking order (by similarity)
		for(Mapping m : a)
		{
			//If a Mapping has similarity below the threshold, end the loop
			if(m.getSimilarity() < thresh)
				break;
			int sourceId = m.getSourceId();
			int sourceCard = selected.getSourceMappings(sourceId).size();
			int targetId = m.getTargetId();
			int targetCard = selected.getTargetMappings(targetId).size();
			if(sourceCard < cardinality && targetCard < cardinality)
				selected.add(m);
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
		Alignment selected = new Alignment(a.getSource(),a.getTarget());
		for(Mapping m : a)
			if(highLevel.containsAncestralMapping(m.getSourceId(), m.getTargetId()))
				selected.add(m);
		return selected;
	}
	
	/**
	 * Selects matches greedily in descending order of similarity to obtain
	 * a one-to-one maximal alignment or a near one-to-one alignment
	 * @param a: the Alignment to select
	 * @param thresh: the minimum similarity threshold
	 * @param strict: whether the alignment is strictly one-to-one or ties are permitted
	 * @return the selected Alignment
	 */
	public static Alignment selectOneToOne(Alignment a, double thresh, boolean strict)
	{
		//Initialize Alignment to return
		Alignment selected = new Alignment(a.getSource(),a.getTarget());
		//Start by boosting the reciprocal bestMatches by 10%
		a.boostBestMatches(0.1);
		//Then sort the alignment
		a.sort();
		//Then select Mappings in ranking order (by similarity)
		for(Mapping m : a)
		{
			//If a Mapping has similarity below the threshold, end the loop
			if(m.getSimilarity() < thresh)
				break;
			//Otherwise, add it only if it doesn't conflict with higher similarity Mappings
			if((strict && !selected.containsConflict(m)) || (!strict && !selected.containsBetterMapping(m)))
				selected.add(m);
		}
		return selected;
	}
}