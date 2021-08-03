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
 * between relations, properties and also relation-to-property                 *
 * @authors Beatriz Lima, Daniel Faria                                         *
 ******************************************************************************/
package aml.match.association;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import aml.AML;
import aml.alignment.rdf.AbstractExpression;
import aml.alignment.rdf.PropertyId;
import aml.alignment.rdf.RelationId;
import aml.ontology.Ontology;
import aml.ontology.ValueMap;
import aml.ontology.semantics.EntityMap;

public class PropertyARMatcher extends AbstractAssociationRuleMatcher 
{
	// Constructor
	public PropertyARMatcher(){}

	// Protected methods
	/*
	 * Populates EntitySupport and MappingSupport tables
	 * @param o1: source ontology
	 * @param o2: target ontology
	 */
	protected void computeSupport(Ontology o1, Ontology o2) 
	{
		System.out.println("Get object/ data property support");
		Set<String> sharedInstances = new HashSet<String>(getSharedInstances(o1, o2));
		EntityMap rels = AML.getInstance().getEntityMap();
		ValueMap srcValueMap = o1.getValueMap();
		ValueMap tgtValueMap = o2.getValueMap();
		Set<String> srcDataProperties = null;
		Set<String> tgtDataProperties = null;

		for(String si : sharedInstances) 
		{
			// Find all properties associated to this instance
			if (srcValueMap.getProperties(si) != null)
				srcDataProperties = new HashSet<String>(srcValueMap.getProperties(si));

			if (tgtValueMap.getProperties(si) != null)
				tgtDataProperties = new HashSet<String>(tgtValueMap.getProperties(si));

			// Find all relations associated to this instance
			// While filtering out instances with no relationships
			Set<String> individuals2 = new HashSet<String>(rels.getIndividualActiveRelations(si));
			if(individuals2.size()==0)
				continue;

			for (String i2 : individuals2)
			{
				// Set of relations
				Set<String> rSet = new HashSet<String>(rels.getIndividualPropertiesTransitive(si, i2)); // Avoid duplicates
				// Switch to list since we need indexes
				List<String> rList = new ArrayList<String>(rSet);
				int len = rList.size();

				// Iterate list of relations to count support
				for (int i=0; i<len; i++) 
				{
					// Transform string uri into correspondent AbstractExpression
					RelationId r1 = new RelationId(rList.get(i));
					incrementEntitySupport(r1); // Add relation to EntitySupport
				
					// OBJ - OBJ property mappings
					// If there are not at least two properties for this instance (one for each
					// ontology do not continue to MappingSupport and move to next instance
					for (int j=i+1; j<len; j++) 
					{
						RelationId r2 = new RelationId(rList.get(j));
						// Make sure we are not mapping entities from the same ontology
						if (o1.contains(rList.get(j)) && o1.contains(rList.get(i)))
							continue;
						if(!o1.contains(rList.get(j)) && !o1.contains(rList.get(i)))
							continue;
						// Add to MappingSupport
						incrementMappingSupport(r1, r2);
					}
//					//  OBJ - DATA property mappings
//					// Make sure the mapping is between entities of opposite ontologies
//					if (o1.contains(rList.get(i)))
//						relationToPropMapping(r1, tgtDataProperties);
//					else if (o2.contains(rList.get(i)))
//						relationToPropMapping(r1, srcDataProperties);
				}
			}
			// PROPERTY - PROPERTY mappings
			for (String srcUri : srcDataProperties) 
			{
				PropertyId p1 = new PropertyId(srcUri, null);
				incrementEntitySupport(p1);

				// Add to MappingSupport 
				Set<String> srcValues = srcValueMap.getValues(si, srcUri);
				if(srcValues.size()<1)
					continue;
				for(String tgtUri: tgtDataProperties) 
				{
					Set<String> tgtValues = tgtValueMap.getValues(si, tgtUri);
					for (String srcV: srcValues) // Only map properties if they have the same value
					{
						for (String tgtV: tgtValues) 
						{
							if(srcV.equals(tgtV))
							{
								PropertyId p2 = new PropertyId(tgtUri, null);
								incrementMappingSupport(p1, p2);
								break;
							}
						}
					}
				} 
			}
			// Also increment entity support for tgt properties
			for (String tgtUri : tgtDataProperties) 
			{
				PropertyId p = new PropertyId(tgtUri, null);
				// Add property to EntitySupport
				incrementEntitySupport(p);
			}
		}
	}

	protected void relationToPropMapping(RelationId r1, Set<String> dataProperties) 
	{
		if (dataProperties == null)
			return;

		for (String p2URI: dataProperties) 
		{
			PropertyId p2 = new PropertyId(p2URI, null);
			// Add to MappingSupport
			incrementMappingSupport(r1, p2);
		}
	}

}
