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
import aml.alignment.rdf.ClassId;
import aml.ontology.Ontology;
import aml.ontology.semantics.EntityMap;


public class ClassARMatcher extends AbstractAssociationRuleMatcher {
	
// Constructor
	
	public ClassARMatcher() {
		super();
	}

	
// Protected methods
	

	protected void computeSupport(Ontology o1, Ontology o2) 
	{
		System.out.println("Get class support");
		EntityMap rels = AML.getInstance().getEntityMap();
		
		Set<String> sharedInstances = getSharedInstances(o1,o2);
		
		for(String si : sharedInstances) {
			// Find all triples containing rdf:type->Class
			List<String> list = new ArrayList<String>(rels.getIndividualClasses(si));
			int len = list.size();
			
			// If empty list of classes, move on to next instance
			if(len < 1) {continue;}
			
			for (int i = 0; i < len; i++) {
				// Transform string uri into correspondent AbstractExpression
				ClassId c1 = new ClassId(list.get(i));
				
				// Add class to EntitySupport if not already in keys
				if(!entitySupport.containsKey(c1)) {
					entitySupport.put(c1, 1);
				}
				// Else update count
				else {
					entitySupport.put(c1, entitySupport.get(c1) + 1);
				}
				
				// If there are not at least two classes for this instance (one for each ontology)
				// do not continue to MappingSupport and move to next instance
				if(len < 2) {continue;}
				
				// Get entity pairs' support (MappingSupport)
				for (int j = i + 1; j < len; j++) {
					ClassId c2 = new ClassId(list.get(j));
					
					// Add class to MappingSupport if not already in keys
					if(mappingSupport.contains(c1,c2)) {						
						mappingSupport.add(c1,c2, mappingSupport.get(c1,c2)+1);
						mappingSupport.add(c2,c1, mappingSupport.get(c2,c1)+1);
					}
					else {
						mappingSupport.add(c1,c2, (double) 1);
						mappingSupport.add(c2,c1, (double) 1);
					}
				}
			}
		}
		System.out.println("Done!");
	}
}
