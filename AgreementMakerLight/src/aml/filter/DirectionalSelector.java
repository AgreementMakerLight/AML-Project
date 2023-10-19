/******************************************************************************
* Copyright 2013-2023 LASIGE                                                  *
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
* A filtering algorithm based on directional cardinality on either the source *
* or target ontology.                                                         *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.filter;

import aml.AML;
import aml.match.Alignment;
import aml.match.Mapping;
import aml.settings.MappingStatus;
import aml.settings.SelectionType;

public class DirectionalSelector implements Filterer, Flagger
{
	
//Attributes
	
	private AML aml;
	private double thresh;
	private SelectionType type;
	private Alignment a;
	private boolean reverse;
	
//Constructors
	
	/**
	 * Constructs a Selector with the given similarity threshold
	 * and automatic SelectionType
	 * @param thresh: the similarity threshold
	 */
	public DirectionalSelector(double thresh, boolean reverse)
	{
		aml = AML.getInstance();
		this.thresh = thresh;
		this.reverse = reverse;
		type = SelectionType.getSelectionType();
	}
	
	/**
	 * Constructs a Selector with the given similarity threshold
	 * and SelectionType
	 * @param thresh: the similarity threshold
	 * @param type: the SelectionType
	 */
	public DirectionalSelector(double thresh, boolean reverse , SelectionType type)
	{
		this(thresh,reverse);
		this.type = type;
	}


//Public Methods
	
	@Override
	public void filter()
	{
		System.out.println("Performing Selection");
		long time = System.currentTimeMillis()/1000;
		Alignment selected;
		a = aml.getAlignment();
		selected = filter(a);
		if(selected.size() < a.size())
		{
			for(Mapping m : selected)
				if(m.getStatus().equals(MappingStatus.FLAGGED))
					m.setStatus(MappingStatus.UNKNOWN);
			aml.setAlignment(selected);
		}
		System.out.println("Finished in " +	(System.currentTimeMillis()/1000-time) + " seconds");
	}
	
	/**
	 * Selects a given Alignment
	 * @param a: the Alignment to select
	 * @return: the selected Alignment
	 */
	public Alignment filter(Alignment a)
	{
		//The alignment to store selected mappings
		Alignment selected = new Alignment();
		//Sort the active alignment
		a.sortDescending();
		//Then select Mappings in ranking order (by similarity)
		for(Mapping m : a)
		{
			//If the Mapping is CORRECT, select it, regardless of anything else
			if(m.getStatus().equals(MappingStatus.CORRECT))
				selected.add(m);
			//If it is INCORRECT or below the similarity threshold, discard it
			else if(m.getSimilarity() < thresh || m.getStatus().equals(MappingStatus.INCORRECT))
				continue;
			//Otherwise, add it if it obeys the rules for the chosen SelectionType:
					//In STRICT selection no conflicts are allowed
			else if(reverse)
			{
				
				if(type.equals(SelectionType.STRICT))
				{
					if(!selected.containsTarget(m.getTargetId()))
						selected.add(m);
				}
				else
				{
					if(!selected.containsBetterTargetMapping(m))
						selected.add(m);
				}
			}
			else
			{
				
				if(type.equals(SelectionType.STRICT))
				{
					if(!selected.containsSource(m.getSourceId()))
						selected.add(m);
				}
				else
				{
					if(!selected.containsBetterSourceMapping(m))
						selected.add(m);
				}
			}
		}
		return selected;
	}
	
	@Override
	public void flag()
	{
		System.out.println("Running Cardinality Flagger");
		long time = System.currentTimeMillis()/1000;
		a = aml.getAlignment();
		for(Mapping m : a)
			if(m.getStatus().equals(MappingStatus.UNKNOWN) && ((reverse && a.getTargetMappings(m.getTargetId()).size()>1)
					|| (!reverse && a.getSourceMappings(m.getSourceId()).size()>1)))
				m.setStatus(MappingStatus.FLAGGED);
		System.out.println("Finished in " +	(System.currentTimeMillis()/1000-time) + " seconds");
	}
}