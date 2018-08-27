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
* A filtering algorithm based on cardinality.                                 *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.filter;

import aml.alignment.Alignment;
import aml.alignment.SimpleAlignment;
import aml.alignment.mapping.Mapping;
import aml.alignment.mapping.MappingStatus;

public class CardinalitySelector implements Filterer, Flagger
{

//Attributes

	private double thresh;
	private SelectionType type;
	private int card;

//Constructors

	/**
	 * Constructs a Selector with the given similarity threshold
	 * and automatic SelectionType
	 * @param thresh: the similarity threshold
	 */
	public CardinalitySelector(double thresh, int c)
	{
		this.thresh = thresh;
		type = SelectionType.getSelectionType();
		card = c;
	}

	/**
	 * Constructs a Selector with the given similarity threshold
	 * and SelectionType
	 * @param thresh: the similarity threshold
	 * @param type: the SelectionType
	 */
	public CardinalitySelector(double thresh, int c, SelectionType type)
	{
		this.thresh = thresh;
		this.type = type;
		card = c;
	}

//Public Methods

	@Override
	@SuppressWarnings("rawtypes")
	public Alignment filter(Alignment a)
	{
		if(!(a instanceof SimpleAlignment))
		{
			System.out.println("Warning: cannot filter non-simple alignment!");
			return a;
		}
		System.out.println("Performing Selection");
		long time = System.currentTimeMillis()/1000;
		SimpleAlignment in = (SimpleAlignment)a;
		SimpleAlignment out = new SimpleAlignment();
		in.sortDescending();
		for(Mapping<String> m : in)
		{
			if(m.getStatus().equals(MappingStatus.CORRECT))
				out.add(m);
			else if(m.getSimilarity() >= thresh && !m.getStatus().equals(MappingStatus.INCORRECT))
			{
				int sourceCard = out.getSourceMappings(m.getEntity1()).size();
				int targetCard = out.getTargetMappings(m.getEntity2()).size();
				if((sourceCard < card && targetCard < card) ||
						(!type.equals(SelectionType.STRICT) && !out.containsBetterMapping(m)) ||
						(type.equals(SelectionType.HYBRID) && m.getSimilarity() > 0.75 && sourceCard <= card && targetCard <= card))
					out.add(m);
			}
		}
		System.out.println("Finished in " +	(System.currentTimeMillis()/1000-time) + " seconds");
		return out;
	}

	@Override
	@SuppressWarnings("rawtypes")
	public void flag(Alignment a)
	{
		if(!(a instanceof SimpleAlignment))
		{
			System.out.println("Warning: cannot filter non-simple alignment!");
			return;
		}
		System.out.println("Running Cardinality Flagger");
		long time = System.currentTimeMillis()/1000;
		SimpleAlignment in = (SimpleAlignment)a;
		for(Mapping<String> m : in)
		{
			if(m.getStatus().equals(MappingStatus.UNKNOWN))
			{		
				if(in.getSourceMappings(m.getEntity1()).size() > card ||
						in.getTargetMappings(m.getEntity2()).size() > card)
					m.setStatus(MappingStatus.FLAGGED);
			}
		}
		System.out.println("Finished in " +	(System.currentTimeMillis()/1000-time) + " seconds");
	}
}