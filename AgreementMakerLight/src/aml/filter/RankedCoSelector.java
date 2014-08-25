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
* Selection algorithm that reduces an Alignment to the desired cardinality by *
* performing ranked selection according to the ranking in a given auxiliary   *
* Alignment.                                                                  *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 20-08-2014                                                            *
* @version 2.0                                                                *
******************************************************************************/
package aml.filter;

import aml.match.Alignment;
import aml.match.Mapping;
import aml.settings.SelectionType;

public class RankedCoSelector implements Selector
{
	
//Attributes
	
	private Alignment aux;
	private SelectionType type;
	
//Constructors
	
	public RankedCoSelector(Alignment aux)
	{
		this.aux = aux;
		type = SelectionType.getSelectionType(aux);
	}
	
	public RankedCoSelector(Alignment aux, SelectionType s)
	{
		this.aux = aux;
		type = s;
	}

//Public Methods
	
	@Override
	public Alignment select(Alignment a, double thresh)
	{
		aux.sort();
		Alignment selected = new Alignment();
		for(Mapping m : aux)
		{
			double sim = a.getSimilarity(m.getSourceId(), m.getTargetId());
			if(sim < thresh)
				continue;
			Mapping n = new Mapping(m.getSourceId(), m.getTargetId(), sim);
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
				if((sourceCard < 2 && targetCard < 2 && n.getSimilarity() > 0.75) ||
						!selected.containsBetterMapping(n))
					selected.add(new Mapping(n));
			}
		}
		return selected;
	}
}