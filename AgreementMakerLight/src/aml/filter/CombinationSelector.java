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
* Selection algorithm that reduces an Alignment to the desired cardinality    *
* making use of an auxiliary Alignment to weigh in its similarities.          *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 07-07-2014                                                            *
* @version 2.0                                                                *
******************************************************************************/
package aml.filter;


import aml.AML;
import aml.match.Alignment;
import aml.match.Mapping;
import aml.AML.SelectionType;

public class CombinationSelector
{
	
//Attributes
	
	private Alignment maps;
	private Alignment aux;
	private SelectionType type;
	private double weight;
	
//Constructors
	
	public CombinationSelector(Alignment maps, Alignment aux, double w)
	{
		this.maps = maps;
		this.aux = aux;
		AML aml = AML.getInstance();
		type = aml.getSelectionType();
		weight = w;
	}
	
	public CombinationSelector(Alignment maps, Alignment aux, SelectionType s, double w)
	{
		this.maps = maps;
		this.aux = aux;
		type = s;
		weight = w;
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
	 * Selects mappings based on the combined similarity of
	 * the maps Alignment plus the aux Alignment
	 * @param thresh: the minimum similarity threshold
	 * @return the selected Alignment
	 */
	public Alignment select(double thresh)
	{
		Alignment combined = new Alignment();
		for(Mapping m : maps)
		{
			int sId = m.getSourceId();
			int tId = m.getTargetId();
			double sim1 = m.getSimilarity();
			double sim2 = aux.getSimilarity(sId, tId);
			combined.add(sId,tId,(sim1*weight)+(sim2*(1-weight)));
		}
		combined.sort();
		Alignment selected = new Alignment();
		for(Mapping m : combined)
		{
			Mapping n = maps.get(m.getSourceId(), m.getTargetId());
			if(n.getSimilarity() < thresh)
				continue;
			if(type.equals(SelectionType.MANY) ||
					(type.equals(SelectionType.STRICT) && !selected.containsConflict(n)) ||
					(type.equals(SelectionType.PERMISSIVE) && !selected.containsBetterMapping(n)))
				selected.add(new Mapping(n));
			else if(type.equals(SelectionType.HYBRID))
			{
				int sourceId = n.getSourceId();
				int sourceCard = selected.getSourceMappings(sourceId).size();
				int targetId = n.getTargetId();
				int targetCard = selected.getTargetMappings(targetId).size();
				if((sourceCard < 2 && targetCard < 2 && n.getSimilarity() > 0.72) ||
						!selected.containsBetterMapping(m))
					selected.add(new Mapping(n));
			}
		}
		return selected;
	}
}