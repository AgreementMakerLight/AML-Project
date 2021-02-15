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
import aml.util.data.Map2Map;
import aml.util.data.Map2Set;

public class CATARMatcher extends aml.match.association.AbstractAssociationRuleMatcher
{
	// Attributes
	// Holds the information on the number of times a given triple (mapping) appears in the database
	public Map2Map<AbstractExpression, AbstractExpression, Integer> cardSupport;

	//Constructor
	public CATARMatcher() 
	{
		cardSupport = new Map2Map<AbstractExpression, AbstractExpression, Integer>();
	}

	//Protected methods
	/*
	 * Populates EntitySupport and MappingSupport tables
	 * @param o1: source ontology
	 * @param o2: target ontology
	 */
	public void computeSupport(Ontology o1, Ontology o2) 
	{
		// Get entity map of relations
		Set<String> sharedInstances = new HashSet<String>(getSharedInstances(o1,o2));
		EntityMap rels = AML.getInstance().getEntityMap();
		Map2Set<String, String> activeRelations = null;

		for(String si : sharedInstances) 
		{
			// Find all classes associated to that instance
			List<String> i1Classes = new ArrayList<String>(rels.getIndividualClasses(si));
			/*spo
			 * if(si.contains("paper-1062030162172257")) { System.out.println("Here!");
			 * System.out.println(si); }
			 */
			// If empty set of classes, move on to next instance
			int len = i1Classes.size();
			if(len < 1)
				continue;

			// If map contains any other triples involving that instance, get them
			if (rels.getIndividualActiveRelations().get(si) != null) 
			{
				activeRelations = new Map2Set<String, String>(rels.getIndividualActiveRelations().get(si));
				for (int i = 0; i < len; i++) 
				{
					// Transform string uri into correspondent AbstractExpression
					String c1URI = i1Classes.get(i);
					if (c1URI.equals("http://www.w3.org/2002/07/owl#Thing")) {continue;}
					ClassId c1 = new ClassId(c1URI);
					// Add class to EntitySupport
					incrementEntitySupport(c1);
					Set<AttributeDomainRestriction> foundADRs = new HashSet<AttributeDomainRestriction>();

					// Only proceed to populate mappingSupport if there are any other triples involving that instance
					// besides class assignment
					if (activeRelations.size() > 0) 
					{
						for(String i2: activeRelations.keySet()) 
						{
							// Find out i2's classes
							Set<String> i2Classes = new HashSet<String>(rels.getIndividualClasses(i2));
							for(String c2URI: i2Classes) 
							{
								// Filter out cases where both classes are from the same ontology
								if (o1.contains(c1URI) && o1.contains(c2URI)) {continue;}
								else if(o2.contains(c1URI) && o2.contains(c2URI)){continue;}

								ClassId c2 = new ClassId(c2URI);
								// We also take into account all of the 2nd class' ancestors
								Set<String> c2Ancestors = new HashSet<String>(rels.getSuperclasses(c2URI));

								//Iterate relations between si and i2 and save the corresponding attribute domain restriction
								for (String attributeURI: activeRelations.get(i2)) 
								{
									// Filter out cases where relation is from the same ontology as c1
									if (o1.contains(c1URI) && o1.contains(attributeURI)) {continue;}
									else if(o2.contains(c1URI) && o2.contains(attributeURI)){continue;}

									RelationId attribute = new RelationId(attributeURI);
									AttributeDomainRestriction adr = new AttributeDomainRestriction(attribute, c2, RestrictionElement.CLASS);

									//Increment cardinality support
									if(!cardSupport.contains(c1,adr)) {	
										cardSupport.add(c1,adr, 1);
										cardSupport.add(adr,c1, 1);
									}
									else {
										cardSupport.add(c1,adr, cardSupport.get(c1,adr)+1);
										cardSupport.add(adr,c1, cardSupport.get(adr,c1)+1);
									}

									//Increment cardinality support for ancestors as well?

									// Only increment mapping and entity support if that ADR hasn't yet been found for si
									if (!foundADRs.contains(adr)) 
									{
										foundADRs.add(adr);
										incrementEntitySupport(adr);
										incrementMappingSupport(c1,adr);

										// Also increment support for all of c2's ancestors
										for(String ancestorURI: c2Ancestors) {
											ClassId ancestor = new ClassId(ancestorURI);
											AttributeDomainRestriction adr2 = new AttributeDomainRestriction(attribute, ancestor, RestrictionElement.CLASS);

											if (!foundADRs.contains(adr2)) {
												foundADRs.add(adr2);
												incrementEntitySupport(adr2);
												incrementMappingSupport(c1,adr2);
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}

	protected Integer getCardinality(AbstractExpression e1, AbstractExpression e2) 
	{
		return cardSupport.get(e1, e2);
	}
}
