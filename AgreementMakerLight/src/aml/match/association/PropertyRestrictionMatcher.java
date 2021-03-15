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
import aml.alignment.rdf.PropertyDomainRestriction;
import aml.alignment.rdf.PropertyExpression;
import aml.alignment.rdf.PropertyId;
import aml.alignment.rdf.PropertyIntersection;
import aml.ontology.EntityType;
import aml.ontology.Ontology;
import aml.ontology.ValueMap;
import aml.ontology.semantics.EntityMap;

public class PropertyRestrictionMatcher 
{
	// Constructor
		public PropertyRestrictionMatcher(){};

		// Attributes
		private Ontology o1;
		private Ontology o2;
		private EDOALAlignment in;
		private ValueMap srcValueMap;
		private ValueMap tgtValueMap;
		private EntityMap map = AML.getInstance().getEntityMap();

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
			System.out.println("Searching for Property Domain Restrictions");
			long time = System.currentTimeMillis()/1000;
			EDOALAlignment out = new EDOALAlignment();
			this.o1 = o1;
			this.o2 = o1;
			this.in = a;
			this.srcValueMap = o1.getValueMap();
			this.tgtValueMap = o2.getValueMap();

			for(Mapping<AbstractExpression> m: in) 
			{
				AbstractExpression src = m.getEntity1();
				AbstractExpression tgt = m.getEntity2();

				// Property-property mappings 
				if(src instanceof aml.alignment.rdf.PropertyId && tgt instanceof aml.alignment.rdf.PropertyId) 
				{
					// We want to apply the restriction to the broader property in the subsumption mapping
					if (m.getRelationship() == MappingRelation.SUBSUMED_BY) // src < tgt
						out.addAll(getPropertyRestriction((PropertyId)src, (PropertyId)tgt));
					else if (m.getRelationship() == MappingRelation.SUBSUMES) // tgt < src
						out.addAll(getPropertyRestriction((PropertyId)tgt, (PropertyId)src));
				}	
			}
			System.out.println("Finished in " +	(System.currentTimeMillis()/1000-time) + " seconds");
			return out;
		}

		// Private methods
		/**
		 * Extracts PropertyDomainRestrictions given a subsumption mapping 
		 * such as "smallerProperty < broaderProperty"
		 * @return a set of PropertyExpression objects that include PropertyDomainRestrictions
		 */
		private Set<Mapping<AbstractExpression>> getPropertyRestriction(PropertyId smallerProperty, PropertyId broaderProperty)
		{
			HashMap<AbstractExpression, Integer> restrictionSupport = new HashMap<AbstractExpression, Integer>();
			String smallerPropertyURI = smallerProperty.toURI();
			Set<String> sharedInstances = AbstractAssociationRuleMatcher.getSharedInstances(o1, o2);
			Set<String> individuals1 = new HashSet<String>();
			boolean smallIsSource = false;

			if(o1.contains(smallerPropertyURI)) 
			{
				smallIsSource = true;
				individuals1 = srcValueMap.getIndividuals(smallerPropertyURI);
			}
			else 
			{
				individuals1 = tgtValueMap.getIndividuals(smallerPropertyURI);
			}
			
			individuals1.retainAll(sharedInstances); // only work with shared instances
			// Compute Support
			// TransactionDB will only encompass instances that have a smallerProperty to some other instance
			for (String i1: individuals1) 
			{
				// DOMAIN RESTRICTION
				// Find all classes associated to instance i1
				Set<String> domainClasses = map.getIndividualClasses(i1);
				for (String classURI: domainClasses) 
				{
					if((o1.contains(classURI) && smallIsSource)
							| (!o1.contains(classURI) && !smallIsSource))
						continue;

					// Transform string uri into correspondent AbstractExpression
					ClassId classId = new ClassId(classURI);
					PropertyExpression restriction = constructPropertyRestriction(broaderProperty, classId);
					// Increment restriction support 
					if(!restrictionSupport.containsKey(restriction))
						restrictionSupport.put(restriction, 1);
					else 
						restrictionSupport.put(restriction, restrictionSupport.get(restriction)+1);
				}
			}
			// Compute confidence in rules of type smallerProperty -> broaderProperty ˆ domain
			// Given that all instances in this transactionDB contain the smallerProperty:
			// sup(smallerProperty U (broaderProperty ˆ domainw)) =  restrictionSupport
			// sup(smallerProperty) = # of individual that have the smallerProperty (size of DB)
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
				mappings.add(new EDOALMapping(smallerProperty, b, max, MappingRelation.EQUIVALENCE));
			
			return mappings;
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
}
