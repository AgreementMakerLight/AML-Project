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
* Abstract Matcher based on association rules.                                *
*                                                                             *
* @authors Beatriz Lima, Daniel Faria                                         *
******************************************************************************/
package aml.match.association;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import aml.AML;
import aml.alignment.rdf.PropertyId;
import aml.ontology.Ontology;
import aml.ontology.semantics.EntityMap;
import aml.util.data.Map2Set;

public class PropertyARMatcher extends AbstractAssociationRuleMatcher{

// Constructor
	
	public PropertyARMatcher() {
		super();
	}

//Protected methods
	
	protected void computeSupport(Ontology o1, Ontology o2) {
		System.out.println("Get property support");
		EntityMap rels = AML.getInstance().getEntityMap();

		Set<String> sharedInstances = getSharedInstances(o1,o2);

		for(String si : sharedInstances) {
			// Filtered instances with no relationships
			if (rels.getIndividualActiveRelations().get(si) != null) {
				// Find all triples containing object/data property with range r
				Map2Set<String, String> activeRelations = new Map2Set<String, String>(rels.getIndividualActiveRelations().get(si));

				// activeRelations is a HashMap for each individual i with the form <i2: Set(properties)>
				for (String i2: activeRelations.keySet()) {

					//List of properties
					List<String> pList = new ArrayList<String>(activeRelations.get(i2));
					int len = pList.size();

					//Iterate list of properties to count support
					for (int i = 0; i < len; i++)
					{ 
						// Transform string uri into correspondent AbstractExpression
						PropertyId p1 = new PropertyId(pList.get(i), "En");

						// Add property to EntitySupport if not already in keys
						if(!entitySupport.containsKey(p1)) {
							entitySupport.put(p1, 1);
						}
						// Else update count
						else {
							entitySupport.put(p1, entitySupport.get(p1) + 1);
						}

						// If there are not at least two properties for this instance (one for each ontology)
						// do not continue to MappingSupport and move to next instance
						if(len < 2) {continue;}

						// Get entity pairs' support (MappingSupport)
						for (int j = i + 1; j < len; j++) {
							PropertyId p2 = new PropertyId(pList.get(j), "En");

							// Add class to MappingSupport if not already in keys
							if(mappingSupport.contains(p1,p2)) {						
								mappingSupport.add(p1,p2, mappingSupport.get(p1,p2)+1);
								mappingSupport.add(p2,p1, mappingSupport.get(p2,p1)+1);
							}
							else {
								mappingSupport.add(p1,p2, (double) 1);
								mappingSupport.add(p2,p1, (double) 1);
							}
						}
					}
				}
			}		
		}
		System.out.println("Done!");
	}
}
