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
 * Matches Individuals by finding literal matches between the values of their  *
 * Annotation and Data Properties, as stored in the ValueMap.                  *
 *                                                                             *
 * @author Daniel Faria                                                        *
 ******************************************************************************/
package aml.match.value;

import java.util.Set;

import aml.AML;
import aml.alignment.Alignment;
import aml.alignment.SimpleMapping;
import aml.match.PrimaryMatcher;
import aml.match.SecondaryMatcher;
import aml.match.UnsupportedEntityTypeException;
import aml.ontology.EntityType;
import aml.ontology.ValueMap;
import aml.settings.InstanceMatchingCategory;

public class ValueMatcher implements PrimaryMatcher, SecondaryMatcher
{

//Attributes

	private static final String DESCRIPTION = "Matches individuals that have equal values for\n" +
											  "the same Annotation or Data Property, or for\n" +
											  "for matching properties (in secondary mode)";
	private static final String NAME = "Value Matcher";
	private static final EntityType[] SUPPORT = {EntityType.INDIVIDUAL};

//Constructors

	public ValueMatcher(){}

//Public Methods

	@Override
	public Alignment extendAlignment(Alignment a, EntityType e, double thresh) throws UnsupportedEntityTypeException
	{
		checkEntityType(e);
		System.out.println("Running Value Matcher");
		long time = System.currentTimeMillis()/1000;
		AML aml = AML.getInstance();
		ValueMap sVal = aml.getSource().getValueMap();
		ValueMap tVal = aml.getTarget().getValueMap();
		Alignment maps = new Alignment();

		for(Integer i : sVal.getProperties())
		{
			if(!a.containsSource(i))
				continue;

			for(Integer h : a.getSourceMappings(i))
			{
				for(String s : sVal.getValues(i))
				{
					if(!tVal.getValues(h).contains(s))
						continue;
					Set<Integer> sourceIndexes = sVal.getIndividuals(i,s);
					Set<Integer> targetIndexes = tVal.getIndividuals(i,s);
					for(Integer j : sourceIndexes)
					{
						for(Integer k : targetIndexes)
						{
							double similarity = maps.getSimilarity(j, k);
							similarity += a.getSimilarity(i, h) / Math.min(sVal.getValues(j, i).size(), tVal.getValues(k, i).size());			
							maps.add(i, j, similarity);
						}
					}
				}
			}
		}
		for(SimpleMapping m : maps)
		{
			double similarity = m.getSimilarity() / Math.min(sVal.getProperties(m.getSourceId()).size(),
					tVal.getProperties(m.getTargetId()).size());
			if(similarity >= thresh)
				a.add(m.getSourceId(),m.getTargetId(),similarity);
		}
		time = System.currentTimeMillis()/1000 - time;
		System.out.println("Finished in " + time + " seconds");
		return a;
	}

	@Override
	public String getDescription()
	{
		return DESCRIPTION;
	}

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public EntityType[] getSupportedEntityTypes()
	{
		return SUPPORT;
	}

	@Override
	public Alignment match(EntityType e, double thresh) throws UnsupportedEntityTypeException
	{
		checkEntityType(e);
		System.out.println("Running Value Matcher");
		long time = System.currentTimeMillis()/1000;
		AML aml = AML.getInstance();
		ValueMap sVal = aml.getSource().getValueMap();
		ValueMap tVal = aml.getTarget().getValueMap();
		Alignment maps = new Alignment();

		for(Integer i : sVal.getProperties())
		{
			if(!tVal.getProperties().contains(i))
				continue;

			for(String s : sVal.getValues(i))
			{
				if(!tVal.getValues(i).contains(s))
					continue;
				Set<Integer> sourceIndexes = sVal.getIndividuals(i,s);
				sourceIndexes.retainAll(aml.getSourceIndividualsToMatch());
				Set<Integer> targetIndexes = tVal.getIndividuals(i,s);
				targetIndexes.retainAll(aml.getTargetIndividualsToMatch());
				int count = Math.min(sourceIndexes.size(), targetIndexes.size());
				for(Integer j : sourceIndexes)
				{
					for(Integer k : targetIndexes)
					{
						if(aml.getInstanceMatchingCategory().equals(InstanceMatchingCategory.SAME_CLASSES) &&
								!aml.getEntityMap().shareClass(j,k))
							continue;
						double similarity = maps.getSimilarity(j, k);
						similarity = Math.max(similarity, 1.0/count);

						if(similarity >= thresh)
							maps.add(j, k, similarity);
					}
				}
			}
		}
		time = System.currentTimeMillis()/1000 - time;
		System.out.println("Finished in " + time + " seconds");
		return maps;
	}
	
//Private Methods

	private void checkEntityType(EntityType e) throws UnsupportedEntityTypeException
	{
		boolean check = false;
		for(EntityType t : SUPPORT)
		{
			if(t.equals(e))
			{
				check = true;
				break;
			}
		}
		if(!check)
			throw new UnsupportedEntityTypeException(e.toString());
	}
}