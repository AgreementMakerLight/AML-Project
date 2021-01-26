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

import org.semanticweb.owlapi.model.OWLDatatype;

import aml.AML;
import aml.alignment.rdf.AbstractExpression;
import aml.alignment.rdf.AttributeTypeRestriction;
import aml.alignment.rdf.Datatype;
import aml.alignment.rdf.PropertyId;
import aml.alignment.rdf.RelationId;
import aml.ontology.Ontology;
import aml.ontology.ValueMap;
import aml.ontology.semantics.EntityMap;
import aml.util.data.Map2Set;

public class PropertyARMatcher extends AbstractAssociationRuleMatcher{

	// Constructor

	public PropertyARMatcher() {
		super();
	}

	//Protected methods

	protected void computeSupport(Ontology o1, Ontology o2) 
	{
		System.out.println("Get property support");
		Set<String> sharedInstances = new HashSet<String>(getSharedInstances(o1,o2));
		EntityMap rels = AML.getInstance().getEntityMap();
		ValueMap srcValueMap = o1.getValueMap();
		ValueMap tgtValueMap = o2.getValueMap();
		Set<String> equivIndv = new HashSet<String>();
		Set<String> srcDataProperties = null;
		Set<String> tgtDataProperties = null;

		boolean eqvInstances = false;

		for(String si : sharedInstances) 
		{
			eqvInstances = false;

			// Find all properties associated to this instance
			if (srcValueMap.getProperties(si) != null) 
				srcDataProperties = new HashSet<String>(srcValueMap.getProperties(si));
			if (tgtValueMap.getProperties(si) != null) 
				tgtDataProperties = new HashSet<String>(tgtValueMap.getProperties(si));

			// See if equivalent instances have more properties to be added to the list
			if (rels.getEquivalentIndividuals(si) != null) 
			{
				eqvInstances = true;
				equivIndv = rels.getEquivalentIndividuals(si);
				for(String eqv: equivIndv) 
					srcDataProperties.addAll(srcValueMap.getProperties(eqv));
				for(String eqv: equivIndv) 
					tgtDataProperties.addAll(tgtValueMap.getProperties(eqv));	
			}

			// Find all relations associated to this instance
			// While filtering out instances with no relationships
			if (rels.getIndividualActiveRelations(si) != null) 
			{
				Set<String> individuals2 = new HashSet<String>(rels.getIndividualActiveRelations(si));
				for (String i2: individuals2) 
				{
					//Set of relations
					Set<String> rSet = rels.getIndividualProperties(si, i2); //Avoid duplicates

					// Check if equivalent individuals (to both si and i2)
					// also have relations to add to the list
					if(eqvInstances & rels.getEquivalentIndividuals(i2) != null) 
					{
						Set<String> equivIndv2 = rels.getEquivalentIndividuals(i2);
						for(String eqv: equivIndv) 
						{ 
							for(String eqv2: equivIndv2) 
							{
								if(rels.getIndividualProperties(eqv, eqv2)!= null)
									rSet.addAll(rels.getIndividualProperties(eqv, eqv2)); 
							} 
						}
					}

					// Switch to list since we need indexes 
					List<String> rList = new ArrayList<String>(rSet);
					int len = rList.size();

					//Iterate list of relations to count support
					for (int i = 0; i < len; i++)
					{ 
						// Transform string uri into correspondent AbstractExpression
						RelationId r1 = new RelationId(rList.get(i));
						// Add relation to EntitySupport if not already in keys
						incrementEntitySupport(r1);

						// OBJECT - OBJECT property alignment
						// If there are not at least two classes for this instance (one for each ontology)
						// do not continue to MappingSupport and move to next instance
						if(len > 1) 
						{
							for (int j = i + 1; j < len; j++) 
							{
								RelationId r2 = new RelationId(rList.get(j));
								// Make sure we are not mapping entities from the same ontology
								if( (o1.contains(rList.get(j)) && o1.contains(rList.get(i)))
										| (!o1.contains(rList.get(j)) && !o1.contains(rList.get(i))))
									continue;
								// Add to MappingSupport if not already in keys
								incrementMappingSupport(r1, r2);
							}
						}
						
						// OBJECT - DATA property alignment
						// Make sure the mapping is between entities of opposite ontologies
						if (o1.contains(rList.get(i)))
							relationToPropMapping(r1, tgtDataProperties);
						else if(o2.contains(rList.get(i)))
							relationToPropMapping(r1, srcDataProperties);		
					}
				}
			}

			// DATA-DATA property alignment
			// Mapping support for both directions
			for(String srcUri: srcDataProperties) 
			{ 
				PropertyId p1 = new PropertyId(srcUri, null);
				incrementEntitySupport(p1);

				// Add to MappingSupport if not already in keys
				Set<String> srcValues = srcValueMap.getValues(si, srcUri);
				for(String tgtUri: tgtDataProperties) 
				{
					PropertyId p2 = new PropertyId(tgtUri, null); 
					// Add p1 and p2 to mappingSupport if not already in keys
					incrementMappingSupport(p1, p2); 
				}
			}
			// Also increment entity support for tgt properties
			for(String tgtUri: tgtDataProperties) 
			{ 
				PropertyId p = new PropertyId(tgtUri, null);
				// Add property to EntitySupport if not already in keys
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
			// Add to MappingSupport if not already in keys
			incrementMappingSupport(r1, p2);
		}
	}


}

