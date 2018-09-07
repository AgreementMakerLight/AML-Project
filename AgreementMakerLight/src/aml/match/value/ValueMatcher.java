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
 * Matches Individuals by finding literal matches between the values of their  *
 * Annotation and Data Properties, as stored in the ValueMap.                  *
 *                                                                             *
 * @author Daniel Faria                                                        *
 ******************************************************************************/
package aml.match.value;

import java.util.Set;

import aml.AML;
import aml.alignment.SimpleAlignment;
import aml.match.AbstractHashMatcher;
import aml.ontology.EntityType;
import aml.ontology.Ontology;
import aml.ontology.ValueMap;
import aml.settings.InstanceMatchingCategory;

public class ValueMatcher extends AbstractHashMatcher
{

//Attributes

	protected String description = "Matches individuals that have equal values for\n" +
											  "the same Annotation or Data Property, or for\n" +
											  "for matching properties (in secondary mode)";
	protected String name = "Value Matcher";
	protected EntityType[] support = {EntityType.INDIVIDUAL};

//Constructors

	public ValueMatcher(){}
	
//Protected Methods

	@Override
	protected SimpleAlignment hashMatch(Ontology o1, Ontology o2, EntityType e, double thresh)
	{
		SimpleAlignment maps = new SimpleAlignment();
		ValueMap sVal = o1.getValueMap();
		ValueMap tVal = o2.getValueMap();
		AML aml = AML.getInstance();
		
		for(String i : sVal.getProperties())
		{
			if(!tVal.getProperties().contains(i))
				continue;

			for(String s : sVal.getValues(i))
			{
				if(!tVal.getValues(i).contains(s))
					continue;
				Set<String> sourceIndexes = sVal.getIndividuals(i,s);
				sourceIndexes.retainAll(aml.getSourceIndividualsToMatch());
				Set<String> targetIndexes = tVal.getIndividuals(i,s);
				targetIndexes.retainAll(aml.getTargetIndividualsToMatch());
				int count = Math.min(sourceIndexes.size(), targetIndexes.size());
				for(String j : sourceIndexes)
				{
					for(String k : targetIndexes)
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
		return maps;
	}
}