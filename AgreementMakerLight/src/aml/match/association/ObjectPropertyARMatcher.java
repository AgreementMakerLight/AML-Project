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
* Abstract Matcher based on association rules. Finds 1:1 simple equivalences  *
* between object-to-object, data-to-data and object-to-data (and vice-versa)  *
* properties.																  *
* @authors Beatriz Lima, Daniel Faria                                         *
******************************************************************************/
package aml.match.association;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import aml.AML;
import aml.alignment.rdf.PropertyId;
import aml.ontology.Ontology;
import aml.ontology.ValueMap;
import aml.ontology.semantics.EntityMap;
import aml.util.data.Map2Set;

public class ObjectPropertyARMatcher extends AbstractAssociationRuleMatcher{

// Constructor
	
	public ObjectPropertyARMatcher() {
		super();
	}

//Protected methods
	
	protected void computeSupport(Ontology o1, Ontology o2) 
	{
		System.out.println("Get property support");
		Set<String> sharedInstances = new HashSet<String>(getSharedInstances(o1,o2));
		EntityMap rels = AML.getInstance().getEntityMap();
		
		for(String si : sharedInstances) {
			
			// Filter instances with no relationships
			if (rels.getIndividualActiveRelations().get(si) != null) 
			{
				// Find all object/data properties associated to this instance
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
						PropertyId p1 = new PropertyId(pList.get(i), null);
					
						// Add property to EntitySupport if not already in keys
						incrementEntitySupport(p1);

						// If there are not at least two properties for this instance (one for each ontology)
						// do not continue to MappingSupport and move to next instance
						if(len < 2) {continue;}

						// Get entity pairs' support (MappingSupport)
						for (int j = i + 1; j < len; j++) {
							PropertyId p2 = new PropertyId(pList.get(j), null);
							
							// Make sure we are not mapping entities from the same ontology
							if( (o1.contains(pList.get(j)) && o1.contains(pList.get(i)))
								| (!o1.contains(pList.get(j)) && !o1.contains(pList.get(i))))
								continue;

							// Add class to MappingSupport if not already in keys
							incrementMappingSupport(p1, p2);
						}
					}
				}
			}		
		}
	}
}
