package aml.match.association;

import java.util.Collection;
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
import aml.alignment.rdf.ClassIntersection;
import aml.alignment.rdf.Expression;
import aml.alignment.rdf.PropertyDomainRestriction;
import aml.alignment.rdf.PropertyExpression;
import aml.alignment.rdf.PropertyId;
import aml.alignment.rdf.PropertyIntersection;
import aml.alignment.rdf.RelationCoDomainRestriction;
import aml.alignment.rdf.RelationDomainRestriction;
import aml.alignment.rdf.RelationExpression;
import aml.alignment.rdf.RelationId;
import aml.alignment.rdf.RelationIntersection;
import aml.ontology.EntityType;
import aml.ontology.Ontology;
import aml.ontology.ValueMap;
import aml.ontology.semantics.EntityMap;
import aml.util.data.Map2Map;

public class PropertyRestrictionMatcher extends AbstractRestrictionMatcher
{
	// Attributes
	private ValueMap srcValueMap;
	private ValueMap tgtValueMap;

	// Constructor
	public PropertyRestrictionMatcher(){}

	// Private methods
	/**
	 * Extracts PropertyDomainRestrictions given a subsumption mapping 
	 * such as "smallerProperty < broaderProperty"
	 * @return a set of PropertyExpression objects that include PropertyDomainRestrictions
	 */
	protected boolean computeSupport(AbstractExpression smallerExpression, AbstractExpression broaderExpression)
	{
		if(!(smallerExpression instanceof PropertyId && broaderExpression instanceof PropertyId))
			return false;

		PropertyId smallerProperty = (PropertyId) smallerExpression;
		PropertyId broaderProperty = (PropertyId) broaderExpression;
		String smallerPropertyURI = smallerProperty.toURI();
		String broaderPropertyURI = broaderProperty.toURI();

		entitySupport = new HashMap<AbstractExpression, Integer>();
		mappingSupport = new Map2Map<AbstractExpression, AbstractExpression, Integer>();
		ARules = new Map2Map<AbstractExpression, AbstractExpression, Double>();

		// Compute Support
		// TransactionDB will only encompass instances that have a broaderRelation to some other instance
		Set<String> individuals1 = new HashSet<String>();
		Set<String> smallerIndividuals = new HashSet<String>();
		boolean smallIsSource = false;

		if(individuals1.size()<=minSup)
			return false;

		if(o1.contains(smallerPropertyURI)) 
		{
			smallIsSource = true;
			individuals1 = srcValueMap.getIndividuals(broaderPropertyURI);
			smallerIndividuals = tgtValueMap.getIndividuals(smallerPropertyURI);
		}
		else 
		{
			individuals1 = tgtValueMap.getIndividuals(broaderPropertyURI);
			smallerIndividuals = srcValueMap.getIndividuals(smallerPropertyURI);
		}	

		individuals1.retainAll(sharedInstances); // only work with shared instances
		if(individuals1.size()<=minSup)
			return false;

		// Compute Support
		// TransactionDB will only encompass instances that have a broaderProperty to some other instance
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
			for (String classURI: domainClasses) 
			{
				if((o1.contains(classURI) && smallIsSource)
						| (!o1.contains(classURI) && !smallIsSource))
					continue;

				// Transform string uri into correspondent AbstractExpression
				ClassId classId = new ClassId(classURI);
				PropertyExpression restriction = constructPropertyRestriction(broaderProperty, classId); 
				// Increment entity support 
				incrementEntitySupport(restriction);

				if(containsBothRelations) 
					incrementMappingSupport(smallerProperty, restriction);
			}
		}
		return true;
	}

	/** 
	 * Constructs a Property expression that includes a property and restriction
	 */
	private PropertyExpression constructPropertyRestriction(PropertyId prop, ClassExpression clas) 
	{
		Set<PropertyExpression> propComplexExpression = new HashSet<PropertyExpression>(); // Property + restriction
		propComplexExpression.add(prop);
		PropertyDomainRestriction restriction = new PropertyDomainRestriction(clas);
		propComplexExpression.add(restriction);

		return new PropertyIntersection(propComplexExpression);
	}

	protected Mapping<AbstractExpression> filter(Set<Mapping<AbstractExpression>> candidates)
	{
		Mapping<AbstractExpression> element = candidates.iterator().next();
		if (candidates.size()==1)
			return element;

		AbstractExpression e1 = element.getEntity1();
		AbstractExpression e2 = element.getEntity2();
		boolean e1Complex = false;

		//  Find out which one is complex
		if(e2 instanceof PropertyId) 
			e1Complex = true; // e1 is complex

		Set<PropertyExpression> propComplexExpression = new HashSet<PropertyExpression>();
		Set<ClassExpression> classes = new HashSet<ClassExpression>();
		PropertyId broaderProperty = null;

		// Intersection of domain classes
		for(Mapping<AbstractExpression> m: candidates) 
		{
			Collection<Expression> components = new HashSet<Expression>();
			if(e1Complex) 
				components = m.getEntity1().getComponents();
			else
				components = m.getEntity2().getComponents();
			for(Expression c: components) 
			{
				if(c instanceof PropertyDomainRestriction)
				{
					classes.add(((PropertyDomainRestriction) c).getClassRestriction());
				}
				else { //broader relation 
					broaderProperty = (PropertyId) c;
				}
			}
		}
		ClassIntersection restriction = new ClassIntersection(classes);
		propComplexExpression.add(new PropertyDomainRestriction(restriction));
		propComplexExpression.add(broaderProperty);
	
		PropertyIntersection newRestriction = new PropertyIntersection(propComplexExpression);	
		if(e1Complex)
			return new EDOALMapping(newRestriction, e2, 1.0, MappingRelation.EQUIVALENCE);
		else
			return new EDOALMapping(e1, newRestriction, 1.0, MappingRelation.EQUIVALENCE);
	}
}

