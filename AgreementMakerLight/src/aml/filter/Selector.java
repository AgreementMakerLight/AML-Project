/******************************************************************************
* Copyright 2013-2016 LASIGE                                                  *
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

import aml.AML;
import aml.match.Alignment;
import aml.match.Mapping;
import aml.ontology.RelationshipMap;
import aml.settings.MappingStatus;
import aml.settings.SelectionType;
import aml.util.InteractionManager;

public class Selector implements Filterer, Flagger
{
	
//Attributes
	
	private AML aml;
	private double thresh;
	private SelectionType type;
	private Alignment a;
	private Alignment aux;
	private InteractionManager im;
	
//Constructors
	
	/**
	 * Constructs a Selector with the given similarity threshold
	 * and automatic SelectionType
	 * @param thresh: the similarity threshold
	 */
	public Selector(double thresh)
	{
		aml = AML.getInstance();
		this.thresh = thresh;
		type = SelectionType.getSelectionType();
		a = aml.getAlignment();
		aux = null;
		im = aml.getInteractionManager();
	}
	
	/**
	 * Constructs a Selector with the given similarity threshold
	 * and SelectionType
	 * @param thresh: the similarity threshold
	 * @param type: the SelectionType
	 */
	public Selector(double thresh, SelectionType type)
	{
		this(thresh);
		this.type = type;
	}

	/**
	 * Constructs a Selector with the given similarity threshold
	 * and automatic SelectionType, and using the given auxiliary
	 * Alignment as the basis for selection
	 * @param thresh: the similarity threshold
	 * @param aux: the auxiliary Alignment
	 */
	public Selector(double thresh, Alignment aux)
	{
		this(thresh);
		this.aux = aux;
	}

	/**
	 * Constructs a Selector with the given similarity threshold
	 * and SelectionType, and using the given auxiliary Alignment
	 * as the basis for selection
	 * @param thresh: the similarity threshold
	 * @param type: the SelectionType
	 * @param aux: the auxiliary Alignment
	 */
	public Selector(double thresh, SelectionType type, Alignment aux)
	{
		this(thresh, type);
		this.aux = aux;
	}

//Public Methods
	
	@Override
	public void filter()
	{
		System.out.println("Performing Selection");
		long time = System.currentTimeMillis()/1000;
		Alignment selected;
		if(!type.equals(SelectionType.HYBRID))
			selected = parentFilter(a);
		//In normal selection mode
		if(aux == null)
			selected = filterNormal();
		//In co-selection mode
		else
			selected = filterWithAux();
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
		Alignment selected = new Alignment();
		a.sortDescending();
		for(Mapping m : a)
		{
			if(m.getStatus().equals(MappingStatus.CORRECT))
				selected.add(m);
			else if(m.getSimilarity() < thresh || m.getStatus().equals(MappingStatus.INCORRECT))
				continue;
			else if((type.equals(SelectionType.STRICT) && !selected.containsConflict(m)) ||
					(type.equals(SelectionType.PERMISSIVE) && !selected.containsBetterMapping(m)))
				selected.add(new Mapping(m));
			else if(type.equals(SelectionType.HYBRID))
			{
				int sourceCard = selected.getSourceMappings(m.getSourceId()).size();
				int targetCard = selected.getTargetMappings(m.getTargetId()).size();
				if((sourceCard < 2 && targetCard < 2 && m.getSimilarity() > 0.75) ||
						!selected.containsBetterMapping(m))
					selected.add(m);
			}
		}
		return selected;
	}
	
	@Override
	public void flag()
	{
		System.out.println("Running Cardinality Flagger");
		long time = System.currentTimeMillis()/1000;
		for(Mapping m : a)
			if(a.containsConflict(m) && m.getStatus().equals(MappingStatus.UNKNOWN))
				m.setStatus(MappingStatus.FLAGGED);
		System.out.println("Finished in " +	(System.currentTimeMillis()/1000-time) + " seconds");
	}
	
	private Alignment filterNormal()
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
			else if((type.equals(SelectionType.STRICT) && !selected.containsConflict(m)) ||
					//In PERMISSIVE selection only conflicts of equal similarity are allowed
					(type.equals(SelectionType.PERMISSIVE) && !selected.containsBetterMapping(m)) ||
					//And in HYBRID selection a cardinality of 2 is allowed above 0.75 similarity
					(type.equals(SelectionType.HYBRID) && ((m.getSimilarity() > 0.75 &&
					selected.cardinality(m.getSourceId()) < 2 && selected.cardinality(m.getTargetId()) < 2) ||
					//And PERMISSIVE selection is employed below this limit
					!selected.containsBetterMapping(m))))
				selected.add(m);
			//Finally, if the task is interactive, check if the mapping is correct
			else if(im.isInteractive())
			{
				im.classify(m);
				if(m.getStatus().equals(MappingStatus.CORRECT))
					selected.add(m);
			}
		}
		return selected;
	}
	
	private Alignment filterWithAux()
	{
		//The alignment to store selected mappings
		Alignment selected = new Alignment();
		//Sort the auxiliary alignment
		aux.sortDescending();
		//Then perform selection based on it
		for(Mapping m : aux)
		{
			Mapping n = a.get(m.getSourceId(), m.getTargetId());
			if(n.getStatus().equals(MappingStatus.CORRECT))
				selected.add(n);
			else if(n.getSimilarity() < thresh || n.getStatus().equals(MappingStatus.INCORRECT))
				continue;
			if((type.equals(SelectionType.STRICT) && !selected.containsConflict(n)) ||
					(type.equals(SelectionType.PERMISSIVE) && !selected.containsBetterMapping(n)) ||
					(type.equals(SelectionType.HYBRID) && ((n.getSimilarity() > 0.75 && 
					selected.cardinality(n.getSourceId()) < 2 && selected.cardinality(n.getTargetId()) < 2) ||
					!selected.containsBetterMapping(n))))
				selected.add(n);
			else if(im.isInteractive())
			{
				im.classify(m);
				if(m.getStatus().equals(MappingStatus.CORRECT))
					selected.add(m);
			}
		}
		return selected;
	}
	
	private Alignment parentFilter(Alignment in)
	{
		RelationshipMap r = aml.getRelationshipMap();
		Alignment out = new Alignment();
		for(Mapping m : in)
		{
			int src = m.getSourceId();
			int tgt = m.getTargetId();
			boolean add = true;
			for(Integer t : in.getSourceMappings(src))
			{
				if(r.isSubclass(t,tgt) &&
						in.getSimilarity(src, t) >= in.getSimilarity(src, tgt))
				{
					add = false;
					break;
				}
			}
			if(!add)
				continue;
			for(Integer s : in.getTargetMappings(tgt))
			{
				if(r.isSubclass(s,src) &&
						in.getSimilarity(s, tgt) >= in.getSimilarity(src, tgt))
				{
					add = false;
					break;
				}
			}
			if(add)
				out.add(m);
		}
		return out;
	}
}