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
* A filtering algorithm based on cardinality that uses an auxiliary alignment *
* to rank mappings.                                                           *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.filter;

import aml.alignment.Alignment;
import aml.alignment.SimpleAlignment;
import aml.alignment.mapping.Mapping;
import aml.alignment.mapping.MappingStatus;

public class CoSelector implements Filterer
{
	
//Attributes
	
	private double thresh;
	private SelectionType type;
	private SimpleAlignment aux;
	
//Constructors
	
	/**
	 * Constructs a CoSelector with the given similarity threshold
	 * and automatic SelectionType, and using the given auxiliary
	 * Alignment as the basis for selection
	 * @param thresh: the similarity threshold
	 * @param aux: the auxiliary Alignment
	 */
	public CoSelector(double thresh, SimpleAlignment aux)
	{
		this.thresh = thresh;
		this.type = SelectionType.getSelectionType();
		this.aux = aux;
	}

	/**
	 * Constructs a CoSelector with the given similarity threshold
	 * and SelectionType, and using the given auxiliary Alignment
	 * as the basis for selection
	 * @param thresh: the similarity threshold
	 * @param type: the SelectionType
	 * @param aux: the auxiliary Alignment
	 */
	public CoSelector(double thresh, SelectionType type, SimpleAlignment aux)
	{
		this.thresh = thresh;
		this.type = type;
		this.aux = aux;
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
		SimpleAlignment out = new SimpleAlignment(in.getSourceOntology(),in.getTargetOntology());
		aux.sortDescending();
		//Then perform selection based on it
		for(Mapping<String> m : aux)
		{
			Mapping<String> n = in.get(m.getEntity1(), m.getEntity2());
			if(n == null)
				continue;
			if(n.getStatus().equals(MappingStatus.CORRECT))
				out.add(n);
			else if(n.getSimilarity() < thresh || n.getStatus().equals(MappingStatus.INCORRECT))
				continue;
			if((type.equals(SelectionType.STRICT) && !out.containsConflict(n)) ||
					(type.equals(SelectionType.PERMISSIVE) && !aux.containsBetterMapping(m)) ||
					(type.equals(SelectionType.HYBRID) && ((n.getSimilarity() > 0.75 && 
					out.cardinality(n.getEntity1()) < 2 && out.cardinality(n.getEntity2()) < 2) ||
					!out.containsBetterMapping(n))))
				out.add(n);
		}
		if(out.size() < a.size())
		{
			for(Mapping<String> m : out)
				if(m.getStatus().equals(MappingStatus.FLAGGED))
					m.setStatus(MappingStatus.UNKNOWN);
		}
		System.out.println("Finished in " +	(System.currentTimeMillis()/1000-time) + " seconds");
		return out;
	}
}