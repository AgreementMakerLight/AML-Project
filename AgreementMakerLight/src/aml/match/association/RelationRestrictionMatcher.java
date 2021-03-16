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
 * This matcher searches for Relation Domain/ Range Restrictions that can   *
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
import aml.util.data.Map2Map;

public class RelationRestrictionMatcher
{
	// Constructor
	public RelationRestrictionMatcher(){};

	// Attributes
	private EntityMap map = AML.getInstance().getEntityMap();
	private Ontology o1;
	private Ontology o2;
	private EDOALAlignment in;
	protected int minSup; //about 1% of all transactions
	protected final double minConf = 0.5;

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
		System.out.println("Searching for Relation Domain/Range Restrictions");
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
				//	else 
				//		out.add(m); // Equivalence or unknown
			}	
			//else
			//	out.add(m);	
		}
		System.out.println("Finished in " +	(System.currentTimeMillis()/1000-time) + " seconds");
		return out;
	}

	// Private methods
	/**
	 * Extracts both RelationDomainRestrictions and RelationRangeRestrictions given a subsumption mapping 
	 * such as "smallerRelation < broaderRelation"
	 * @return a set of RelationExpression objects that include RelationRestrictions
	 */
	private Set<Mapping<AbstractExpression>> getRelationRestriction(RelationId smallerRelation, RelationId broaderRelation)
	{
		HashMap<AbstractExpression, Integer> entitySupport = new HashMap<AbstractExpression, Integer>();
		Map2Map<AbstractExpression, AbstractExpression, Integer> mappingSupport = new Map2Map<AbstractExpression, AbstractExpression, Integer>();
		Map2Map<AbstractExpression, AbstractExpression, Double> ARules = new Map2Map<>();
		Set<Mapping<AbstractExpression>> mappings = new HashSet<Mapping<AbstractExpression>>();
		String smallerRelationURI = smallerRelation.toURI();
		String broaderRelationURI = broaderRelation.toURI();
		Set<String> sharedInstances = AbstractAssociationRuleMatcher.getSharedInstances(o1, o2);
		minSup = sharedInstances.size()/100;
		
		// Compute Support
		// TransactionDB will only encompass instances that have a broaderRelation to some other instance
		Set<String> individuals1 = map.getActiveRelationIndividuals(broaderRelationURI).keySet();
		Set<String> smallerIndividuals = map.getActiveRelationIndividuals(smallerRelationURI).keySet();
		individuals1.retainAll(sharedInstances); // only work with shared instances
		
		if(individuals1.size()<=minSup)
			return mappings;
		
		for (String i1: individuals1) 
		{
			boolean containsBothRelations = false;
			if(smallerIndividuals.contains(i1)) 
			{
				incrementEntitySupport(smallerRelation, entitySupport);
				containsBothRelations = true;
			}

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
				// Increment entity support 
				incrementEntitySupport(restriction, entitySupport);

				if(containsBothRelations) 
					incrementMappingSupport(smallerRelation, restriction, mappingSupport);
			}

			// RANGE RESTRICTION
			Set<String> individuals2 = map.getActiveRelationIndividuals(broaderRelationURI).get(i1);
			Set<RelationExpression> foundRangeRestriction = new HashSet<RelationExpression>();

			// Find all classes associated to instance i2
			for(String i2: individuals2) 
			{
				Set<String> rangeClasses = map.getIndividualClasses(i2);
				for (String classURI: rangeClasses) 
				{
					if((o1.contains(classURI) && o1.contains(smallerRelationURI))
							| (!o1.contains(classURI) && !o1.contains(smallerRelationURI)))
						continue;

					// Transform string uri into correspondent AbstractExpression
					ClassId classId = new ClassId(classURI);
					foundRangeRestriction.add(constructRelationRestriction(broaderRelation, classId, "Range"));
				}
			}
			// Increment range restriction support 
			for(RelationExpression restriction: foundRangeRestriction) 
			{
				incrementEntitySupport(restriction, entitySupport);
				if(containsBothRelations)
					incrementMappingSupport(smallerRelation, restriction, mappingSupport);
			}
		}

		// Compute confidence in rules
		for (AbstractExpression e1 : mappingSupport.keySet()) 
		{
			for (AbstractExpression e2 : mappingSupport.get(e1).keySet()) 
			{
				//Filter by support then confidence
				if (mappingSupport.get(e1, e2) >= minSup)
				{
					double conf = (double)mappingSupport.get(e1, e2) / (double)entitySupport.get(e1);
					if (conf > minConf) 
						ARules.add(e1, e2, conf);	
				}
			}
		}
		
		for (AbstractExpression e1 : ARules.keySet()) 
		{
			for (AbstractExpression e2 : ARules.get(e1).keySet()) 
			{
				// If the rule is bidirectional, then it is an equivalence relation
				if(ARules.contains(e2,e1)) 
				{
					double conf = Math.sqrt(ARules.get(e1, e2) * ARules.get(e2, e1));
					// Make sure that mapping is directional (src->tgt)
					if(o1.containsAll(e1.getElements())) 
						mappings.add(new EDOALMapping(e1, e2, conf, MappingRelation.EQUIVALENCE));
					else
						mappings.add(new EDOALMapping(e2, e1, conf, MappingRelation.EQUIVALENCE));
				}
			}
		}
		return mappings;	
	}
	/**
	 * Constructs a Relation expression that includes a relation and restriction
	 * @param mode: either "Domain" for DomainRestriction or "Range" for RangeRestriction
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
		else if (mode.equals("Range")) 
		{
			RelationCoDomainRestriction restriction = new RelationCoDomainRestriction(clas);
			relationComplexExpression.add(restriction);
		}
		return new RelationIntersection(relationComplexExpression);
	}	
	/*
	 * Increments entity support
	 * @param e: entity to account for
	 */
	private void incrementEntitySupport(AbstractExpression e, HashMap<AbstractExpression, Integer> entitySupport) 
	{
		if(!entitySupport.containsKey(e))
			entitySupport.put(e, 1);
		else 
			entitySupport.put(e, entitySupport.get(e)+1);
	}
	/*
	 * Increments mapping support for entities e1 and e2
	 * It's a symmetric map to facilitate searches
	 */
	private void incrementMappingSupport(AbstractExpression e1, AbstractExpression e2, Map2Map<AbstractExpression, AbstractExpression, Integer> mappingSupport) 
	{
		if(!mappingSupport.contains(e1,e2)) 
		{	
			mappingSupport.add(e1,e2, 1);
			mappingSupport.add(e2,e1, 1);
		}
		else 
		{
			mappingSupport.add(e1,e2, mappingSupport.get(e1,e2)+1);
			mappingSupport.add(e2,e1, mappingSupport.get(e2,e1)+1);
		}
	}
}
