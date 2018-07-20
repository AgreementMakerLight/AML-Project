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
import aml.alignment.Alignment;
import aml.alignment.MappingStatus;
import aml.alignment.SimpleMapping;
import aml.ontology.EntityType;
import aml.ontology.RelationshipMap;
import aml.settings.SelectionType;
import aml.util.InteractionManager;

public class CardinalitySelector implements Filterer, Flagger
{

//Attributes

	private AML aml;
	private double thresh;
	private SelectionType type;
	private Alignment a;
	private Alignment aux;
	private InteractionManager im;
	private int card;

//Constructors

	/**
	 * Constructs a Selector with the given similarity threshold
	 * and automatic SelectionType
	 * @param thresh: the similarity threshold
	 */
	public CardinalitySelector(double thresh, int c)
	{
		aml = AML.getInstance();
		this.thresh = thresh;
		type = SelectionType.getSelectionType();
		aux = null;
		im = aml.getInteractionManager();
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
		this(thresh,c);
		this.type = type;
	}

	/**
	 * Constructs a Selector with the given similarity threshold
	 * and automatic SelectionType, and using the given auxiliary
	 * Alignment as the basis for selection
	 * @param thresh: the similarity threshold
	 * @param aux: the auxiliary Alignment
	 */
	public CardinalitySelector(double thresh, int c, Alignment aux)
	{
		this(thresh,c);
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
	public CardinalitySelector(double thresh, int c, SelectionType type, Alignment aux)
	{
		this(thresh, c, type);
		this.aux = aux;
	}

//Public Methods

	@Override
	public void filter()
	{
		System.out.println("Performing Selection");
		long time = System.currentTimeMillis()/1000;
		Alignment selected;
		a = aml.getAlignment();
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
			for(SimpleMapping m : selected)
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
		for(SimpleMapping m : a)
		{
			boolean toAdd = false;
			if(m.getStatus().equals(MappingStatus.CORRECT))
				toAdd = true;
			else if(m.getSimilarity() >= thresh && !m.getStatus().equals(MappingStatus.INCORRECT))
			{
				int sourceCard = selected.getSourceMappings(m.getSourceId()).size();
				int targetCard = selected.getTargetMappings(m.getTargetId()).size();
				if((sourceCard < card && targetCard < card) ||
						(!type.equals(SelectionType.STRICT) && !selected.containsBetterMapping(m)) ||
						(type.equals(SelectionType.HYBRID) && m.getSimilarity() > 0.75 && sourceCard <= card && targetCard <= card))
					toAdd = true;
			}
			if(toAdd)
				selected.add(new SimpleMapping(m));
		}
		return selected;
	}

	@Override
	public void flag()
	{
		System.out.println("Running Cardinality Flagger");
		long time = System.currentTimeMillis()/1000;
		a = aml.getAlignment();
		for(SimpleMapping m : a)
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
		for(SimpleMapping m : a)
		{
			boolean toAdd = false;
			if(m.getStatus().equals(MappingStatus.CORRECT))
				toAdd = true;
			else if(m.getSimilarity() >= thresh && !m.getStatus().equals(MappingStatus.INCORRECT))
			{
				int sourceCard = selected.getSourceMappings(m.getSourceId()).size();
				int targetCard = selected.getTargetMappings(m.getTargetId()).size();
				if((sourceCard < card && targetCard < card) ||
						(!type.equals(SelectionType.STRICT) && !selected.containsBetterMapping(m)) ||
						(type.equals(SelectionType.HYBRID) && m.getSimilarity() > 0.75 && sourceCard <= card && targetCard <= card))
					toAdd = true;
				else if(im.isInteractive())
				{
					im.classify(m);
					if(m.getStatus().equals(MappingStatus.CORRECT))
						toAdd = true;
				}
			}
			if(toAdd)
				selected.add(m);
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
		for(SimpleMapping n : aux)
		{
			SimpleMapping m = a.get(n.getSourceId(), n.getTargetId());
			if(m == null)
				continue;
			boolean toAdd = false;
			if(m.getStatus().equals(MappingStatus.CORRECT))
				toAdd = true;
			else if(m.getSimilarity() >= thresh && !m.getStatus().equals(MappingStatus.INCORRECT))
			{
				int sourceCard = selected.getSourceMappings(m.getSourceId()).size();
				int targetCard = selected.getTargetMappings(m.getTargetId()).size();
				if((sourceCard < card && targetCard < card) ||
						(!type.equals(SelectionType.STRICT) && !selected.containsBetterMapping(m)) ||
						(type.equals(SelectionType.HYBRID) && m.getSimilarity() > 0.75 && sourceCard <= card && targetCard <= card))
					toAdd = true;
				else if(im.isInteractive())
				{
					im.classify(m);
					if(m.getStatus().equals(MappingStatus.CORRECT))
						toAdd = true;
				}
			}
			if(toAdd)
				selected.add(m);
		}
		return selected;
	}

	private Alignment parentFilter(Alignment in)
	{
		RelationshipMap r = aml.getRelationshipMap();
		Alignment out = new Alignment();
		for(SimpleMapping m : in)
		{
			int src = m.getSourceId();
			int tgt = m.getTargetId();
			if(!aml.getURIMap().getTypes(src).equals(EntityType.CLASS))
				continue;
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