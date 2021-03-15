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
 * This matcher searches for Relation Domain/ CoDomain Restrictions that can   *
 * be extracted from simple relation-relation mappings present in the input    *
 * alignment.													               *
 * @authors Beatriz Lima, Daniel Faria                                         *
 ******************************************************************************/
package aml.match.association;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import aml.AML;
import aml.alignment.EDOALAlignment;
import aml.alignment.mapping.EDOALMapping;
import aml.alignment.mapping.Mapping;
import aml.alignment.mapping.MappingRelation;
import aml.alignment.rdf.AbstractExpression;
import aml.alignment.rdf.ClassExpression;
import aml.alignment.rdf.ClassId;
import aml.alignment.rdf.RelationCoDomainRestriction;
import aml.alignment.rdf.RelationDomainRestriction;
import aml.alignment.rdf.RelationExpression;
import aml.alignment.rdf.RelationId;
import aml.alignment.rdf.RelationIntersection;
import aml.ontology.EntityType;
import aml.ontology.Ontology;
import aml.ontology.semantics.EntityMap;

public class RelationRestrictionMatcher
{
	// Constructor
	public RelationRestrictionMatcher(){};

	// Attributes
	private EntityMap map = AML.getInstance().getEntityMap();
	private Ontology o1;
	private Ontology o2;
	private EDOALAlignment in;

	// Public methods
	/**
	 * Extends the given Alignment between the source and target Ontologies
	 * @param o1: the source Ontology to match
	 * @param o2: the target Ontology to match
	 * @param a: the existing alignment to extend
	 * @param e: the EntityType to match
	 * @param thresh: the similarity threshold for the extention
	 * @return the alignment with (only) the new mappings between the Ontologies
	 */
	public EDOALAlignment extendAlignment(Ontology o1, Ontology o2, EDOALAlignment a, EntityType e, double thresh)
	{
		System.out.println("Searching for Relation Domain/CoDomain Restrictions");
		long time = System.currentTimeMillis()/1000;
		EDOALAlignment out = new EDOALAlignment();
		this.o1 = o1;
		this.o2 = o1;
		this.in = a;

		for(Mapping<AbstractExpression> m: in) 
		{
			AbstractExpression src = m.getEntity1();
			AbstractExpression tgt = m.getEntity2();

			// Relation-relation mappings 
			if(src instanceof aml.alignment.rdf.RelationId && tgt instanceof aml.alignment.rdf.RelationId) 
			{
				// We want to apply the restriction to the broader relation in the subsumption mapping
				if (m.getRelationship() == MappingRelation.SUBSUMED_BY) // src < tgt
					out.addAll(getRelationRestriction((RelationId)src, (RelationId)tgt));
				else if (m.getRelationship() == MappingRelation.SUBSUMES) // tgt < src
					out.addAll(getRelationRestriction((RelationId)tgt, (RelationId)src));
			}	
		}
		System.out.println("Finished in " +	(System.currentTimeMillis()/1000-time) + " seconds");
		return out;
	}

	// Private methods
	/**
	 * Extracts both RelationDomainRestrictions and  RelationCoDomainRestrictions given a subsumption mapping 
	 * such as "smallerRelation < broaderRelation"
	 * @return a set of RelationExpression objects that include RelationRestrictions
	 */
	private Set<Mapping<AbstractExpression>> getRelationRestriction(RelationId smallerRelation, RelationId broaderRelation)
	{
		HashMap<AbstractExpression, Integer> restrictionSupport = new HashMap<AbstractExpression, Integer>();
		String smallerRelationURI = smallerRelation.toURI();
		Set<String> sharedInstances = AbstractAssociationRuleMatcher.getSharedInstances(o1, o2);

		// Compute Support
		// TransactionDB will only encompass instances that have a smallerRelation to some other instance
		Set<String> individuals1 = map.getActiveRelationIndividuals(smallerRelationURI).keySet();
		individuals1.retainAll(sharedInstances); // only work with shared instances

		for (String i1: individuals1) 
		{
			// DOMAIN RESTRICTION
			// Find all classes associated to instance i1
			Set<String> domainClasses = map.getIndividualClasses(i1);
			for (String classURI: domainClasses) 
			{
				if((o1.contains(classURI) && o1.contains(smallerRelationURI))
						| (!o1.contains(classURI) && !o1.contains(smallerRelationURI)))
					continue;

				// Transform string uri into correspondent AbstractExpression
				ClassId classId = new ClassId(classURI);
				RelationExpression restriction = constructRelationRestriction(broaderRelation, classId, "Domain");
				// Increment restriction support 
				if(!restrictionSupport.containsKey(restriction))
					restrictionSupport.put(restriction, 1);
				else 
					restrictionSupport.put(restriction, restrictionSupport.get(restriction)+1);
			}

			// CO-DOMAIN RESTRICTION
			Set<String> individuals2 = map.getActiveRelationIndividuals(smallerRelationURI).get(i1);
			Set<RelationExpression> foundCoDomainRestriction = new HashSet<RelationExpression>();

			// Find all classes associated to instance i2
			for(String i2: individuals2) 
			{
				Set<String> coDomainClasses = map.getIndividualClasses(i2);
				for (String classURI: coDomainClasses) 
				{
					if((o1.contains(classURI) && o1.contains(smallerRelationURI))
							| (!o1.contains(classURI) && !o1.contains(smallerRelationURI)))
						continue;

					// Transform string uri into correspondent AbstractExpression
					ClassId classId = new ClassId(classURI);
					foundCoDomainRestriction.add(constructRelationRestriction(broaderRelation, classId, "CoDomain"));
				}
			}
			// Increment restriction support 
			for(RelationExpression restriction: foundCoDomainRestriction) 
			{
				if(!restrictionSupport.containsKey(restriction))
					restrictionSupport.put(restriction, 1);
				else 
					restrictionSupport.put(restriction, restrictionSupport.get(restriction)+1);
			}
		}
		// Compute confidence in rules of type smallerRelation -> broaderRelation ˆ restriction
		// Given that all instances in this transactionDB contain the smallerRelation:
		// sup(smallerRelation U (broaderRelation ˆ restriction)) =  restrictionSupport
		// sup(smallerRelation) = # of individual that have the smallerRelation (size of DB)
		double max = 0.0;
		Set<AbstractExpression> bestRestriction = new HashSet<AbstractExpression>();
		for(AbstractExpression restriction: restrictionSupport.keySet()) 
		{
			double conf = restrictionSupport.get(restriction)*1.0 / individuals1.size();
			if (conf>=max) 
			{
				max = conf;
				bestRestriction.add(restriction);
			}		
		}
		Set<Mapping<AbstractExpression>> mappings = new HashSet<Mapping<AbstractExpression>>(); 
		for(AbstractExpression b: bestRestriction) 
			mappings.add(new EDOALMapping(smallerRelation, b, max, MappingRelation.EQUIVALENCE));
		
		return mappings;
	}

	/**
	 * Constructs a Relation expression that includes a relation and restriction
	 * @param mode: either "Domain" for DomainRestriction or "CoDomain" for CoDomainRestriction
	 */
	private RelationExpression constructRelationRestriction(RelationId relation, ClassExpression clas, String mode) 
	{
		Set<RelationExpression> relationComplexExpression = new HashSet<RelationExpression>(); // Relation + restriction
		relationComplexExpression.add(relation);

		if(mode.equals("Domain")) 
		{
			RelationDomainRestriction restriction = new RelationDomainRestriction(clas);
			relationComplexExpression.add(restriction);
		}
		else if (mode.equals("CoDomain")) 
		{
			RelationCoDomainRestriction restriction = new RelationCoDomainRestriction(clas);
			relationComplexExpression.add(restriction);
		}
		return new RelationIntersection(relationComplexExpression);
	}

}
