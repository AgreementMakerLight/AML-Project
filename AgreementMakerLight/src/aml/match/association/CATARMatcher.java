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
 * between classes and AttributeDomainRestrictions - Class by Attribute Type   *
 * (CAT) pattern                                                               *
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
import aml.alignment.rdf.ClassId;
import aml.alignment.rdf.RelationId;
import aml.alignment.rdf.RestrictionElement;
import aml.ontology.Ontology;
import aml.ontology.semantics.EntityMap;
import aml.util.data.Map2Set;

public class CATARMatcher extends aml.match.association.AbstractAssociationRuleMatcher
{
	//Constructor
	public CATARMatcher(){}

	//Protected methods
	/*
	 * Populates EntitySupport and MappingSupport tables
	 * @param o1: source ontology
	 * @param o2: target ontology
	 */
	public void computeSupport(Ontology o1, Ontology o2) 
	{
		System.out.println("Initialising Class by Attribute Type Matcher");
		// Get entity map of relations
		Set<String> sharedInstances = new HashSet<String>(getSharedInstances(o1,o2));
		EntityMap rels = AML.getInstance().getEntityMap();
		Map2Set<String, String> ontRanges = new Map2Set<String, String>();

		for(String si : sharedInstances) 
		{
			// Find all classes associated to that instance
			List<String> i1Classes = new ArrayList<String>(rels.getIndividualClassesTransitive(si));
			// If empty set of classes, move on to next instance
			int len = i1Classes.size();
			if(len < 1)
				continue;
			
			// If map does not have any other triples involving this instance, move on to next instance
			Set<String> activeRelations = new HashSet<String>(rels.getIndividualActiveRelations(si));
			if(activeRelations.size()==0)
				continue;
			Set<AttributeDomainRestriction> foundADRsInstance = new HashSet<AttributeDomainRestriction>(); //ADRs found for this instance si

			for (String c1URI: i1Classes) 
			{
				// Transform string uri into correspondent AbstractExpression
				ClassId c1 = new ClassId(c1URI);
				incrementEntitySupport(c1); 
				Set<AttributeDomainRestriction> foundADRsClass = new HashSet<AttributeDomainRestriction>(); // ADRs found for this class c1
				
				for(String i2: activeRelations) 
				{
					// Find out i2's classes
					Set<String> i2Classes = new HashSet<String>(rels.getIndividualClassesTransitive(i2));
					for(String c2URI: i2Classes) 
					{
						// Filter out cases where both classes are from the same ontology
						if (o1.contains(c1URI) && o1.contains(c2URI)) {continue;}
						else if(o2.contains(c1URI) && o2.contains(c2URI)){continue;}

						ClassId c2 = new ClassId(c2URI);
						//Iterate relations between si and i2 and save the corresponding attribute domain restriction
						for (String attributeURI: rels.getIndividualProperties(si, i2)) 
						{
							// Filter out cases where relation is from the same ontology as c1
							if (o1.contains(c1URI) && o1.contains(attributeURI)) {continue;}
							else if(o2.contains(c1URI) && o2.contains(attributeURI)){continue;}

							// Filter out cases where c2 is not a subclass of the relation's ontology range (or the range itself)
							if(ontRanges.contains(attributeURI)) 
							{
								if(!ontRanges.get(attributeURI).contains(c2URI)) 
									continue;
							}
							else // add to ontRanges
							{
								ontRanges.addAll(attributeURI, rels.getRanges(attributeURI));
								for(String ontRange: rels.getRanges(attributeURI))
									ontRanges.addAll(attributeURI, rels.getSubclasses(ontRange));
							}
							RelationId attribute = new RelationId(attributeURI);
							AttributeDomainRestriction adr = new AttributeDomainRestriction(attribute, c2, RestrictionElement.CLASS);
							foundADRsClass.add(adr);
						}
					}
				}
				// Increment mapping support for all the ADRs found for this class
				for(AbstractExpression adr: foundADRsClass)
					incrementMappingSupport(c1, adr);
				// Add classes ADRs to list of ADRs for this instance si
				foundADRsInstance.addAll(foundADRsClass);
			}
			// Increment entity support for all the ADRs found for this instance
			for(AbstractExpression adr: foundADRsInstance)
				incrementEntitySupport(adr);
		}
	}
}