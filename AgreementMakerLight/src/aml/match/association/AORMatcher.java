/******************************************************************************
 * Copyright 2013-2020 LASIGE                                                  *
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
 * Matcher based on association rules. Finds complex (1:n) relationships       *
 * between classes and AttributeOccurenceRestrictions                          *
 * @authors Beatriz Lima, Daniel Faria                                         *
 ******************************************************************************/

package aml.match.association;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import aml.AML;
import aml.alignment.rdf.AbstractExpression;
import aml.alignment.rdf.AttributeDomainRestriction;
import aml.alignment.rdf.AttributeOccurrenceRestriction;
import aml.alignment.rdf.ClassId;
import aml.alignment.rdf.Comparator;
import aml.alignment.rdf.NonNegativeInteger;
import aml.alignment.rdf.RelationId;
import aml.ontology.Ontology;
import aml.ontology.semantics.EntityMap;
import aml.util.data.Map2Set;

public class AORMatcher extends aml.match.association.AbstractAssociationRuleMatcher
{
	// Attributes

	//Constructor
	public AORMatcher() 
	{
		super();
	}

	//Protected methods
	/*
	 * Populates EntitySupport and MappingSupport tables
	 * @param o1: source ontology
	 * @param o2: target ontology
	 */
	protected void computeSupport(Ontology o1, Ontology o2) 
	{
		System.out.println("Initialising Attribute Occurence Matcher");
		// Get entity map of relations
		Set<String> sharedInstances = new HashSet<String>(getSharedInstances(o1,o2));
		EntityMap rels = AML.getInstance().getEntityMap();
		Map2Set<String, String> activeRelations = null;

		for(String si : sharedInstances) 
		{
			// Find all classes associated to that instance
			List<String> i1Classes = new ArrayList<String>(rels.getIndividualClassesTransitive(si));
			// If empty set of classes, move on to next instance
			int len = i1Classes.size();
			if(len < 1)
				continue;
			// If map does not have any other triples involving this instance, move on to next instance
			if (rels.getIndividualActiveRelations().get(si) == null)
				continue;

			Set<AttributeOccurrenceRestriction> foundAORsInstance = new HashSet<AttributeOccurrenceRestriction>(); //AORs found for this instance si
			// If map contains any other triples involving that instance, get them
			activeRelations = new Map2Set<String, String>(rels.getIndividualActiveRelations().get(si));
			for (String c1URI: i1Classes) 
			{
				// Transform string uri into correspondent AbstractExpression
				ClassId c1 = new ClassId(c1URI);
				incrementEntitySupport(c1);
				Set<AttributeOccurrenceRestriction> foundAORsClass = new HashSet<AttributeOccurrenceRestriction>(); //AORs found for this class c1

				// Only proceed to populate mappingSupport if there are any other triples involving that instance
				// besides class assignment
				for(String i2: activeRelations.keySet()) 
				{
					//Iterate relations between si and i2 and save the corresponding attribute
					for (String attributeURI: activeRelations.get(i2)) 
					{
						// Filter out cases where relation is from the same ontology as c1
						if (o1.contains(c1URI) && o1.contains(attributeURI)) {continue;}
						else if(o2.contains(c1URI) && o2.contains(attributeURI)){continue;}

						AttributeOccurrenceRestriction aor = new AttributeOccurrenceRestriction(
								new RelationId(attributeURI),
								new Comparator("http://ns.inria.org/edoal/1.0/#greater-than"),
								new NonNegativeInteger(0));
						foundAORsClass.add(aor);
					}
				}
				// Increment mapping support for all the ADRs found for this class
				for(AbstractExpression aor: foundAORsClass)
					incrementMappingSupport(c1, aor);
				// Add classes AORs to list of ADRs for this instance si
				foundAORsInstance.addAll(foundAORsClass);
			}
			// Increment entity support for all the ADRs found for this instance
			for(AbstractExpression aor: foundAORsInstance)
				incrementEntitySupport(aor);
		}
	}
}

