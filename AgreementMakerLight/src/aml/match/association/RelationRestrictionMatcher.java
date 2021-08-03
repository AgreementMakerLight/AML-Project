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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import aml.alignment.mapping.EDOALMapping;
import aml.alignment.mapping.Mapping;
import aml.alignment.mapping.MappingRelation;
import aml.alignment.rdf.AbstractExpression;
import aml.alignment.rdf.ClassExpression;
import aml.alignment.rdf.ClassId;
import aml.alignment.rdf.ClassIntersection;
import aml.alignment.rdf.Expression;
import aml.alignment.rdf.RelationCoDomainRestriction;
import aml.alignment.rdf.RelationDomainRestriction;
import aml.alignment.rdf.RelationExpression;
import aml.alignment.rdf.RelationId;
import aml.alignment.rdf.RelationIntersection;
import aml.util.data.Map2Map;
import aml.util.data.Map2Set;

public class RelationRestrictionMatcher extends AbstractRestrictionMatcher
{
	// Constructor
	public RelationRestrictionMatcher(){}

	// Private methods
	/**
	 * Extracts both RelationDomainRestrictions and RelationRangeRestrictions given a subsumption mapping 
	 * such as "smallerRelation < broaderRelation"
	 * @return whether the computation of support was successful
	 */
	protected boolean computeSupport(AbstractExpression smallerExpression, AbstractExpression broaderExpression) 
	{
		if(!(smallerExpression instanceof RelationId && broaderExpression instanceof RelationId))
			return false;

		RelationId smallerProperty = (RelationId) smallerExpression;
		RelationId broaderProperty = (RelationId) broaderExpression;
		String smallerPropertyURI = smallerProperty.toURI();
		String broaderPropertyURI = broaderProperty.toURI();

		// Populate support DBs
		// TransactionDB will only encompass instances that have a broaderRelation to some other instance
		Set<String> individuals1 = map.getActiveRelationIndividuals(broaderPropertyURI).keySet();
		Set<String> smallerIndividuals = map.getActiveRelationIndividuals(smallerPropertyURI).keySet();
		individuals1.retainAll(sharedInstances); // only work with shared instances
		if(individuals1.size()<=minSup)
			return false;

		entitySupport = new HashMap<AbstractExpression, Integer>();
		mappingSupport = new Map2Map<AbstractExpression, AbstractExpression, Integer>();
		ARules = new Map2Map<AbstractExpression, AbstractExpression, Double>();

		for (String i1: individuals1) 
		{
			boolean containsBothRelations = false;
			if(smallerIndividuals.contains(i1)) 
			{
				incrementEntitySupport(smallerProperty);
				containsBothRelations = true;
			}

			// DOMAIN RESTRICTION
			// Find all classes associated to instance i1
			Set<String> domainClasses = map.getIndividualClassesTransitive(i1);
			Set<String> propDomain = new HashSet<String>();
			for(String d: map.getDomains(broaderPropertyURI))
				propDomain.addAll(map.getSubclasses(d));
			for (String classURI: domainClasses) 
			{
				if((o1.contains(classURI) && o1.contains(smallerPropertyURI))
						| (!o1.contains(classURI) && !o1.contains(smallerPropertyURI)))
					continue;
				//Filter those are not the domain or a subclass of the domain of property
				if(!propDomain.contains(classURI))
					continue;
				// Transform string uri into correspondent AbstractExpression
				ClassId classId = new ClassId(classURI);
				RelationExpression restriction = constructRelationRestriction(broaderProperty, classId, "Domain");
				// Increment entity support 
				incrementEntitySupport(restriction);

				if(containsBothRelations)
					incrementMappingSupport(smallerProperty, restriction);
			}

			// RANGE RESTRICTION
			Set<String> propRange = new HashSet<String>();
			for(String r: map.getRanges(broaderPropertyURI))
				propRange.addAll(map.getSubclasses(r));
			Set<String> individuals2 = map.getIndividualActiveRelations(i1);
			Set<RelationExpression> foundRangeRestriction = new HashSet<RelationExpression>();

			// Find all classes associated to instance i2
			for(String i2: individuals2) 
			{
				Set<String> rangeClasses = map.getIndividualClassesTransitive(i2);
				for (String classURI: rangeClasses) 
				{
					if((o1.contains(classURI) && o1.contains(smallerPropertyURI))
							| (!o1.contains(classURI) && !o1.contains(smallerPropertyURI)))
						continue;
					//Filter those are not the range or a subclass of the range of property
					if(!propRange.contains(classURI))
						continue;
					// Transform string uri into correspondent AbstractExpression
					ClassId classId = new ClassId(classURI);
					foundRangeRestriction.add(constructRelationRestriction(broaderProperty, classId, "Range"));
				}
			}
			// Increment range restriction support 
			for(RelationExpression restriction: foundRangeRestriction) 
			{
				incrementEntitySupport(restriction);
				if(containsBothRelations)
					incrementMappingSupport(smallerProperty, restriction);
			}
		}
		return true;
	}

	//Private methods
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

	protected Mapping<AbstractExpression> filter(Set<Mapping<AbstractExpression>> candidates) 
	{
		Mapping<AbstractExpression> element = candidates.iterator().next();
		if (candidates.size()==1)
			return element;

		AbstractExpression e1 = element.getEntity1();
		AbstractExpression e2 = element.getEntity2();
		boolean e1Complex = false;
		RelationId broaderRelation = new RelationId(null);

		//  Find out which one is complex
		if(e2 instanceof RelationId) 
			e1Complex = true; // e1 is complex

		// Separate in domain and range restriction candidates
		Set<RelationDomainRestriction> domainCandidates = new HashSet<RelationDomainRestriction>();
		Set<RelationCoDomainRestriction> rangeCandidates = new HashSet<RelationCoDomainRestriction>();
		for(Mapping<AbstractExpression> m: candidates) 
		{
			Collection<Expression> components = new HashSet<Expression>();
			if(e1Complex)
				components = m.getEntity1().getComponents();
			else
				components = m.getEntity2().getComponents();
			for(Expression c: components) 
			{
				if(c instanceof RelationDomainRestriction)
					domainCandidates.add((RelationDomainRestriction) c);
				else if(c instanceof RelationCoDomainRestriction)
					rangeCandidates.add((RelationCoDomainRestriction) c);
				else //broader relation 
					broaderRelation= (RelationId) c;
			}
		}
		Set<RelationExpression> relationComplexExpression = new HashSet<RelationExpression>();
		relationComplexExpression.add(broaderRelation);

		if(domainCandidates.size()==1) 
			relationComplexExpression.add(domainCandidates.iterator().next());
		else if(domainCandidates.size()>1)
		{
			// Intersection of domain classes
			Set<ClassExpression> classes = new HashSet<ClassExpression>();
			for(RelationDomainRestriction r: domainCandidates) 
				classes.add(r.getClassRestriction());
			// Filter redundancies and children
			ClassIntersection restriction = new ClassIntersection(removeRRChildren(classes, broaderRelation, "Domain"));
			relationComplexExpression.add(new RelationDomainRestriction(restriction));
		}
		if(rangeCandidates.size()==1)
			relationComplexExpression.add(rangeCandidates.iterator().next());
		else if(rangeCandidates.size()>1)
		{
			// Intersection of range classes
			Set<ClassExpression> classes = new HashSet<ClassExpression>();
			for(RelationCoDomainRestriction r: rangeCandidates) 
				classes.add(r.getClassRestriction());
			ClassIntersection restriction = new ClassIntersection(removeRRChildren(classes, broaderRelation, "Range"));
			relationComplexExpression.add(new RelationCoDomainRestriction(restriction));
		}
		RelationIntersection newRestriction = new RelationIntersection(relationComplexExpression);	
		if(e1Complex)
			return new EDOALMapping(newRestriction, e2, 1.0, MappingRelation.EQUIVALENCE);
		else
			return new EDOALMapping(e1, newRestriction, 1.0, MappingRelation.EQUIVALENCE);
	}

	/*
	 * This method removes child classes from a set of classes, and also the redundant 
	 * @param complexMappings: the set of RR mappings from which we want to remove the children
	 * @return: a clean set of ADRs (no children)
	 */
	private Set<ClassExpression> removeRRChildren(Set<ClassExpression> classes, RelationId relation, String mode) 
	{
		Set<ClassExpression> result = new HashSet<ClassExpression>();

		// Find children mappings indexes
		Set<String> children = new HashSet<String>();
		for(ClassExpression c: classes) 
			children.addAll(map.getSubclasses(((ClassId) c).toURI()));

		// Add more generic classes to result set
		for(ClassExpression c: classes) 
		{
			if(!children.contains(((ClassId) c).toURI()))
				result.add(c);
		}
		return result;
	}
}
