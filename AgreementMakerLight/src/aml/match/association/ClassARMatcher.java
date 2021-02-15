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
 * Matcher based on association rules. Finds simple (1:1) relationships        *
 * between source and target classes.                                          *
 * @authors Beatriz Lima, Daniel Faria                                         *
 ******************************************************************************/
package aml.match.association;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import aml.AML;
import aml.alignment.rdf.AbstractExpression;
import aml.alignment.rdf.ClassId;
import aml.ontology.EntityType;
import aml.ontology.Ontology;
import aml.ontology.ValueMap;
import aml.ontology.lexicon.Lexicon;
import aml.ontology.semantics.EntityMap;


public class ClassARMatcher extends AbstractAssociationRuleMatcher {

	// Constructor
	public ClassARMatcher() 
	{
		super();
	}


	// Protected methods
	/*
	 * Populates EntitySupport and MappingSupport tables
	 * @param o1: source ontology
	 * @param o2: target ontology
	 */
	protected void computeSupport(Ontology o1, Ontology o2) 
	{
		System.out.println("Get class support");
		Set<String> sharedInstances = new HashSet<String>(getSharedInstances(o1,o2));
		EntityMap rels = AML.getInstance().getEntityMap();

		for(String si : sharedInstances) 
		{	
			// Find all classes associated to that instance
			Set<String> cSet = rels.getIndividualClasses(si);

			// Switch to list since we need indexes 
			List<String> cList = new ArrayList<String>(cSet);
			// If empty list of classes, move on to next instance
			int len = cList.size();
			if(len < 1)
				continue;

			for (int i = 0; i < len; i++) 
			{
				// Transform string uri into correspondent AbstractExpression
				ClassId c1 = new ClassId(cList.get(i));

				// Add class to EntitySupport  already in keys
				incrementEntitySupport(c1);

				// If there are not at least two classes for this instance (one for each ontology)
				// do not continue to MappingSupport and move to next instance
				if(len < 2) {continue;}

				// Get entity pairs' support (MappingSupport)
				// But only those from the opposite ontology
				for (int j = i + 1; j < len; j++) 
				{
					ClassId c2 = new ClassId(cList.get(j));
					// Make sure we are not mapping entities from the same ontology
					if( (o1.contains(cList.get(j)) && o1.contains(cList.get(i)))
							| (!o1.contains(cList.get(j)) && !o1.contains(cList.get(i))))
						continue;

					// Add class to MappingSupport
					incrementMappingSupport(c1, c2);
				}
			}
		}
	}
}

