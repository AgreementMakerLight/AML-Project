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
		// Get entity map of relations
		Set<String> sharedInstances = new HashSet<String>(getSharedInstances(o1,o2));
		EntityMap rels = AML.getInstance().getEntityMap();
		Map2Set<String, String> activeRelations = null;

		for(String si : sharedInstances) 
		{
			// Find all classes associated to that instance
			List<String> i1Classes = new ArrayList<String>(rels.getIndividualClasses(si));
			// If empty set of classes, move on to next instance
			int len = i1Classes.size();
			if(len < 1)
				continue;
			if (i1Classes.contains("http://gmo#Place")) {
				System.out.println("HERE");
				if (rels.getIndividualActiveRelations().get(si) != null) 
				{
					System.out.println("It does have relations");
				}
				else {
					System.out.println("It doenst have relations");
				}
				
			}

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
					Set<AttributeOccurrenceRestriction> foundAORs = new HashSet<AttributeOccurrenceRestriction>();

					// Only proceed to populate mappingSupport if there are any other triples involving that instance
					// besides class assignment
					if (activeRelations.size() > 0) 
					{
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

								// Only increment mapping and entity support if that AOR hasn't yet been found for si
								if (!foundAORs.contains(aor)) 
								{
									foundAORs.add(aor);
									incrementEntitySupport(aor);
									incrementMappingSupport(c1,aor);
								}
							}
						}
					}
				}
			}
		}
		/*
		 * Set<String> relevant = new HashSet<String>(); relevant.add("Place");
		 * relevant.add("Platform"); relevant.add("Cruise");
		 * relevant.add("PhysicalSample");
		 * 
		 * Set<String> relevantRel = new HashSet<String>();
		 * relevant.add("hasGeoFeatureType"); relevant.add("hasPlatformType");
		 * relevant.add("hasCruiseType"); relevant.add("gbo#hasPlaceType");
		 * relevant.add("gbo#hasSampleType");
		 * 
		 * 
		 * //print mappingsupport for (AbstractExpression l1: mappingSupport.keySet()) {
		 * for(AbstractExpression l2: mappingSupport.keySet(l1)) {
		 * if(relevant.contains(l1.toString()) || relevant.contains(l2.toString())) {
		 * System.out.println("1: " + l1.toRDF()); System.out.println("2: " +
		 * l2.toRDF()); System.out.println("Sup: " + mappingSupport.get(l1,l2));
		 * System.out.println(" "); } } }
		 * 55
		 * //print entitySupport for (AbstractExpression l1: entitySupport.keySet()) {
		 * if(relevant.contains(l1.toString())) { System.out.println(l1.toRDF());
		 * System.out.println("Sup: " + entitySupport.get(l1)); System.out.println(" ");
		 * } if(relevantRel.contains(l1.toString())) { System.out.println(l1.toRDF());
		 * System.out.println("Sup: " + entitySupport.get(l1)); System.out.println(" ");
		 * } }
		 */

	}
}

