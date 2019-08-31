/******************************************************************************
 * Copyright 2013-2019 LASIGE                                                  *
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
 * Finds complex mappings corresponding to property chains, e.g.               *
 * O1:   InstanceA -> PropertyA -> InstanceB/ValueB                            *
 * O2:   InstanceA -> PropertyB -> InstanceC -> Property C -> InstanceB/ValueB *
 * @authors Teemu Tervo                                                        *
 ******************************************************************************/
package aml.match.structural;

import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import aml.AML;
import aml.alignment.EDOALAlignment;
import aml.alignment.SimpleAlignment;
import aml.alignment.mapping.EDOALMapping;
import aml.alignment.mapping.MappingRelation;
import aml.alignment.rdf.*;
import aml.match.BiDirectionalMatcher;
import aml.match.lexical.AttributeRestrictionMatcher;
import aml.ontology.EntityType;
import aml.ontology.lexicon.StopList;
import aml.ontology.Ontology;
import aml.ontology.ValueMap;
import aml.ontology.semantics.EntityMap;
import aml.util.data.Triple;
import aml.util.similarity.Similarity;
import aml.util.similarity.StringSimMeasure;

public class PropertyChainMatcher extends BiDirectionalMatcher
//public class PropertyChainMatcher extends Matcher
{
	//Attributes
	protected static final String DESCRIPTION = "Generates complex mappings corresponding to property chains";
	protected static final String NAME = "Property Chain Matcher";
	protected static final EntityType[] SUPPORT = {EntityType.DATA_PROP,EntityType.OBJECT_PROP};
	
	private StringSimMeasure measure = StringSimMeasure.ISUB; // Similarity measured used for entity label comparison
	private Set<String> stopWords;

	//Constructors

	/**
	 * Constructs a new PropertyChainMatcher with default
	 * String similarity measure (ISub)
	 */
	public PropertyChainMatcher()
	{
		super();
		description = DESCRIPTION;
		name = NAME;
		support = SUPPORT;
		stopWords = StopList.read();
	}

	/**
	 * Constructs a new PropertyChainMatcher with the given String similarity measure
	 * @param m the string similarity measure
	 */
	public PropertyChainMatcher(StringSimMeasure m)
	{
		this();
		measure = m;
	}

	//Public Methods
	/**
	 * Matches the source and target Ontologies, returning an Alignment between them
	 * @param o1 the source Ontology
	 * @param o2 the target Ontology
	 * @param e the EntityType to match
	 * @param thresh the similarity threshold for the alignment
	 * @return the alignment between the source and target ontologies
	 */
	public EDOALAlignment match(Ontology o1, Ontology o2, double thresh)
	{
		EDOALAlignment a = new EDOALAlignment(o1, o2);
		for (EntityType e : support)
			a.addAll(match(o1, o2, e, thresh));
		return a;
	}

	/**
	 * Matches the source and target Ontologies, returning an Alignment between them
	 * @param o1 the source Ontology
	 * @param o2 the target Ontology
	 * @param e the EntityType to match
	 * @param thresh the similarity threshold for the alignment
	 * @return the alignment between the source and target ontologies
	 */
	public EDOALAlignment uniMatch(Ontology o1, Ontology o2, EntityType e, double thresh)
	{
		if (e.equals(EntityType.DATA_PROP))
			return dataPropertyMatch(o1, o2, thresh);
		else if (e.equals(EntityType.OBJECT_PROP))
			return ChainMatch(o1, o2, thresh);
		else
			return null;
	}

	//Protected Methods	
	//Private Methods
	/**
	 * Finds data property mappings corresponding to a chain relation, e.g.
	 * in O1: A prop x, while in O2: A prop B prop x.
	 * @param o1 the source Ontology to match
	 * @param o2 the target Ontology to match
	 * @param thresh the similarity threshold for the alignment
	 * @return the alignment between the source and target ontologies
	 */
	private EDOALAlignment dataPropertyMatch(Ontology o1, Ontology o2, double thresh)
	{
		EDOALAlignment a = new EDOALAlignment(o1, o2);
		EntityMap e = AML.getInstance().getEntityMap();
		
		Set<Triple<String,String,String>> o1Triplets = dataPropertyTriplets(o1);
		Set<Triple<String,String,String>> o2Triplets = dataPropertyTriplets(o2);
		
		for (Triple<String,String,String> triplet : o1Triplets) {
			String individual = triplet.get1();
			String o1Prop = triplet.get2();
			String value = triplet.get3();
			
			if (!o2.contains(individual))
				continue;
			if (value.trim().isEmpty()) // Skip empty values
				continue;
			
			// Iterate over data property triplets in o2, looking for instances linked to the same value
			for (Triple<String,String,String> o2triplet : o2Triplets) {
				if (!value.equals(o2triplet.get3()))
					continue;
				// Ensure individual and o2MiddleMan are related by some object property
				if (!e.getIndividualPassiveRelations(o2triplet.get1()).contains(individual))
					continue;
				
				// o2Prop1 is the object property linking individual to the middle man in o2
				// generally there will only be one property and one class, but iterate the possibilities regardless
				for (String o2Prop1 : e.getIndividualProperties(individual,  o2triplet.get1())) {
					for (String o2MiddleManClass : e.getIndividualClasses(o2triplet.get1())) {
						// o2Prop2 is the data property linking middle man and value in O2:
						String o2Prop2 = o2triplet.get2();
						
						// Strip any stop words from the property names for similarity comparison
						String o1PropName = AttributeRestrictionMatcher.removeWords(o1.getName(o1Prop), stopWords);
						String o2Prop1Name = AttributeRestrictionMatcher.removeWords(o2.getName(o2Prop1), stopWords);
						String o2Prop2Name = AttributeRestrictionMatcher.removeWords(o2.getName(o2Prop2), stopWords);
						
						// 1 Compare semantic similarity of O1 property and the first property in the O2 chain
						// e.g. O1: A hasStartDate X, o2: A startsOnDate B inXSDDate X
						double sim = Similarity.stringSimilarity(o1PropName, o2Prop1Name, measure);
						if (sim >= thresh) {
							// If property domains are equivalent, domain restrictions are not needed
							if (domainsEquivalent(o1, o2, o1Prop, o2Prop1)) {
								PropertyExpression e1 = createDataProp(o1Prop, null);
								RelationExpression prop1 = createObjProp(o2Prop1, null, o2MiddleManClass);
								PropertyExpression prop2 = createDataProp(o2Prop2, null);
								PropertyExpression e2 = createDataPropChain(prop1, prop2);
								
								a.add(new EDOALMapping(e1, e2, sim));
							} else {
								for (String o1Domain : e.getDomains(o1Prop)) {
									for (String o2Domain : findMappings(o1Domain)) {
										PropertyExpression e1 = createDataProp(o1Prop, o1Domain);
										RelationExpression prop1 = createObjProp(o2Prop1, o2Domain, o2MiddleManClass);
										PropertyExpression prop2 = createDataProp(o2Prop2, null);
										PropertyExpression e2 = createDataPropChain(prop1, prop2);
												
										a.add(new EDOALMapping(e1, e2, sim));
									}
								}
							}
							continue;
						}
				
						// 2 Compare semantic similarity of O1 property and the second property in the O2 chain
						// e.g. O1: A hasFullName X, O2: A hasPersonName B fullName X
						sim = Similarity.stringSimilarity(o1PropName, o2Prop2Name, measure);
						if (sim >= thresh) {
							// If property domains are equivalent, domain restrictions are not needed
							if (domainsEquivalent(o1, o2, o1Prop, o2Prop1)) {
								PropertyExpression e1 = createDataProp(o1Prop, null);
								RelationExpression prop1 = createObjProp(o2Prop1, null, o2MiddleManClass);
								PropertyExpression prop2 = createDataProp(o2Prop2, null);
								PropertyExpression e2 = createDataPropChain(prop1, prop2);

								a.add(new EDOALMapping(e1, e2, sim));
							} else {
								for (String o1Domain : e.getDomains(o1Prop)) {
									for (String o2Domain : findMappings(o1Domain)) {
										PropertyExpression e1 = createDataProp(o1Prop, o1Domain);
										RelationExpression prop1 = createObjProp(o2Prop1, o2Domain, o2MiddleManClass);
										PropertyExpression prop2 = createDataProp(o2Prop2, null);
										PropertyExpression e2 = createDataPropChain(prop1, prop2);
												
										a.add(new EDOALMapping(e1, e2, sim));
									}
								}
							}
							
							continue;
						}
				
						// 3 Everything else gets classified as a subsumption relation, as long as
						// the range of the object property is xsd:string (numeric values give too many false positives)
						if (e.getRanges(o1Prop).contains("http://www.w3.org/2001/XMLSchema#string")) {
							// If property domains are equivalent, domain restrictions are not needed
							// Same applies for the case when o2Prop1 domain is not declared, as we are using a subsumption relation
							if (domainsEquivalent(o1, o2, o1Prop, o2Prop1) || e.getDomains(o2Prop1).isEmpty()) {
								PropertyExpression e1 = createDataProp(o1Prop, null);
								RelationExpression prop1 = createObjProp(o2Prop1, null, o2MiddleManClass);
								PropertyExpression prop2 = createDataProp(o2Prop2, null);
								PropertyExpression e2 = createDataPropChain(prop1, prop2);
										
								a.add(new EDOALMapping(e1, e2, 1.0, MappingRelation.SUBSUMED_BY));
							} else {
								for (String o1Domain : e.getDomains(o1Prop)) {
									for (String o2Domain : findMappings(o1Domain)) {
										PropertyExpression e1 = createDataProp(o1Prop, o1Domain);
										RelationExpression prop1 = createObjProp(o2Prop1, o2Domain, o2MiddleManClass);
										PropertyExpression prop2 = createDataProp(o2Prop2, null);
										PropertyExpression e2 = createDataPropChain(prop1, prop2);
												
										a.add(new EDOALMapping(e1, e2, 1.0, MappingRelation.SUBSUMED_BY));
									}
								}
							}
						}
					}
				}
			}
		}
		return a;
	}

	/**
	 * Finds object property mappings corresponding to a chain relation, e.g.
	 * in O1: A prop B, while in O2: A prop X prop B.
	 * @param o1 the source Ontology to match
	 * @param o2 the target Ontology to match
	 * @param thresh the similarity threshold for the alignment
	 * @return the alignment between the source and target ontologies
	 */
	private EDOALAlignment ChainMatch(Ontology o1, Ontology o2, double thresh)
	{
		EDOALAlignment a = new EDOALAlignment(o1, o2);
		EntityMap e = AML.getInstance().getEntityMap();
		
		Set<Triple<String,String,String>> o1Triplets = objectPropertyTriplets(o1);
		Set<Triple<String,String,String>> o2Triplets = objectPropertyTriplets(o2);

		// Next, look at individuals with object properties
		for (Triple<String,String,String> triplet : o1Triplets) {
			String individual1 = triplet.get1();
			String o1Prop = triplet.get2();
			String individual2 = triplet.get3();
			
			if (!o2.contains(individual1) || !o2.contains(individual2))
				continue;
			
			// Iterate over object property triplets in o2, looking for instances linked to individual1
			for (Triple<String,String,String> o2triplet : o2Triplets) {
				if (!o2triplet.get1().equals(individual1))
					continue;
				String o2Prop1 = o2triplet.get2();
				
				for (String o2Prop2 : e.getIndividualProperties(o2triplet.get3(), individual2)) {
					// Strip any stop words from the property names for similarity comparison
					String o1PropName = AttributeRestrictionMatcher.removeWords(o1.getName(o1Prop), stopWords);
	
					for (String o2MiddleManClass : e.getIndividualClasses(o2triplet.get3())) {
						// Filter cases based on the similarity of o1Prop and the class of o2Middleman
						// e.g. o1: X rel Y, o2: X rel1 A rel2 Y, where the class of A is similar to rel
						String className = o2.getName(o2MiddleManClass);
						double sim = Similarity.stringSimilarity(o1PropName, className, measure);
						
						if (sim >= thresh) {
							// Domain restriction not necessary if domains are equivalent
							if (domainsEquivalent(o1, o2, o1Prop, o2Prop1)) {
								RelationExpression e1 = createObjProp(o1Prop, null, null);												
								RelationExpression prop1 = createObjProp(o2Prop1, null, o2MiddleManClass);
								RelationExpression prop2 = new RelationId(o2Prop2);
								RelationExpression e2 = createObjPropChain(prop1, prop2);
								
								a.add(new EDOALMapping(e1, e2, sim));										
							} else {
								for (String o1domain : e.getDomains(o1Prop)) {
									for (String o2domain : findMappings(o1domain)) {
										RelationExpression e1 = createObjProp(o1Prop, o1domain, null);												
										RelationExpression prop1 = createObjProp(o2Prop1, o2domain, o2MiddleManClass);
										RelationExpression prop2 = new RelationId(o2Prop2);
										RelationExpression e2 = createObjPropChain(prop1, prop2);
										
										a.add(new EDOALMapping(e1, e2, sim));
									}
								}
							}
						}
						
						// Find cases where the similarity lies in between the relation and the passive individual class label
						// e.g. o1: X rel Y, o2: X rel1 A rel2 B, where the class of B is similar to rel
						for (String indiv2Class : e.getIndividualClasses(individual2)) {
							sim = Similarity.nameSimilarity(o1PropName, o2.getName(indiv2Class), false);

							if (sim >= thresh) {
								RelationExpression e1 = createObjProp(o1Prop, null, null);
								RelationExpression prop1 = createObjProp(o2Prop1, null, o2MiddleManClass);
								RelationExpression prop2 = createObjProp(o2Prop2, null, null);
								RelationExpression e2 = createObjPropChain(prop1, prop2);
								a.add(new EDOALMapping(e1, e2, sim));	
							}
						}
					}
				}
			}
		}
		
		// Find chain relations based not on instance data
		// but on property name similarities
		for (String o1Prop : removeMappedEntities(o1.getEntities(EntityType.OBJECT_PROP))) {
			
			// Skip properties for which we already have a mapping
			if (a.containsSource(o1Prop))
				continue;
			else if (a.getSources().containsAll(e.getInverseProperties(o1Prop)))
				continue;
			
			for (String o2Prop1 : o2.getEntities(EntityType.OBJECT_PROP)) {
				for (String o2Prop2 : o2.getEntities(EntityType.OBJECT_PROP)) {
					if (o2Prop1.equals(o2Prop2))
						continue;
					if (e.getInverseProperties(o2Prop1).contains(o2Prop2))
						continue;
					
					// check whether o1prop domain conflicts with o2prop domain
					boolean conflict = false;
					for (String z : e.getDomains(o1Prop)) {
						if (!isSubsumedBy(o2, findMappings(z), e.getDomains(o2Prop1)))
							conflict = true;
					}
					for (String z : e.getRanges(o1Prop)) {
						if (!isSubsumedBy(o2, findMappings(z), e.getRanges(o2Prop2)))
							conflict = true;
					}
					if (conflict)
						continue;
					
					// It should be possible to chain o2Prop1 and o2Prop2
					for (String middleManCandidates : AttributeRestrictionMatcher.setIntersection(e.getRanges(o2Prop1), e.getDomains(o2Prop2))) {
						for (String o2MiddleManClass : e.getSubclasses(middleManCandidates)) {
							String o1PropName = AttributeRestrictionMatcher.removeWords(o1.getName(o1Prop), stopWords);
							String o2MiddleManName = o2.getName(o2MiddleManClass);
							o2MiddleManName = AttributeRestrictionMatcher.removeWords(o2MiddleManName, AttributeRestrictionMatcher.splitString(o2.getName(o2Prop1)));
							o2MiddleManName = AttributeRestrictionMatcher.removeWords(o2MiddleManName, AttributeRestrictionMatcher.splitString(o2.getName(o2Prop2)));
							
							double sim = Similarity.nameSimilarity(o1PropName, o2MiddleManName, false);
							if (sim <  thresh)
								continue;
							
							RelationExpression e1 = createObjProp(o1Prop, null, null);
							RelationExpression prop1 = createObjProp(o2Prop1, null, o2MiddleManClass);
							RelationExpression prop2 = createObjProp(o2Prop2, null, null);
							RelationExpression e2 = createObjPropChain(prop1, prop2);
							a.add(new EDOALMapping(e1, e2, sim));
						}
					}
					
				}
			}
			
		}
		
		return a;
	}

	/**
	 * @param o The ontology as and bs are part of
	 * @param as set of class URIs
	 * @param bs set of class URIs
	 * @return whether the union of as is subsumed by the union of bs
	 */
	private static boolean isSubsumedBy(Ontology o, Set<String> as, Set<String >bs) {
		if (bs.isEmpty()) // Treat an empty set as everything
			return true;
		
		loop:
		for (String a : as) {
			if (bs.contains(a))
				continue;
			
			for (String sc : AML.getInstance().getEntityMap().getSuperclasses(a))
				if (bs.contains(sc))
					continue loop;
			
			return false;
		}
		
		return true;
	}

	
	private static PropertyExpression createDataProp(String uri, String domainRestriction) {
		if (domainRestriction == null)
			return new PropertyId(uri, null);

		// Domain restriction only necessary if the domain is a union
		if (AML.getInstance().getEntityMap().getDomains(uri).size() == 1)
			return new PropertyId(uri, null);
		
		Set<PropertyExpression> propertyExp1 = new HashSet<PropertyExpression>();
		propertyExp1.add(new PropertyId(uri, null));
		propertyExp1.add(new PropertyDomainRestriction(new ClassId(domainRestriction)));
	    return new PropertyIntersection(propertyExp1);
    }
	
	private static RelationExpression createObjProp(String uri, String domainRestriction, String coDomainRestriction) {
		if (domainRestriction == null && coDomainRestriction == null)
			return new RelationId(uri);

	    Set<RelationExpression> relationSet = new HashSet<RelationExpression>();
	    relationSet.add(new RelationId(uri));
	    
	    if (domainRestriction != null)
	    	relationSet.add(new RelationDomainRestriction(new ClassId(domainRestriction)));
	    if (coDomainRestriction != null)
	    	relationSet.add(new RelationCoDomainRestriction(new ClassId(coDomainRestriction)));

	    return new RelationIntersection(relationSet);
	}
	
	private static PropertyExpression createDataPropChain(RelationExpression prop1, PropertyExpression prop2) {
		Vector<RelationExpression> propChain = new Vector<RelationExpression>();
		propChain.add(prop1);
		return new PropertyComposition(propChain, prop2);
	}

	private static RelationExpression createObjPropChain(RelationExpression prop1, RelationExpression prop2) {
		Vector<RelationExpression> propChain = new Vector<RelationExpression>();
		propChain.add(prop1);
		propChain.add(prop2);
		return new RelationComposition(propChain);
	}

	/**
	 * Finds mappings for an entity in the active alignment
	 * @param entity the URI of the entity
	 * @return the mappings for the entity in the other ontology
	 */
	private static Set<String> findMappings(String entity) {
		SimpleAlignment a = (SimpleAlignment) AML.getInstance().getAlignment();
		Set<String> ret = new HashSet<String>();
		
		if (a == null)
			return ret;
		
		if (a.containsSource(entity)) {
			for (String t : a.getTargets())
				if (a.get(entity, t) != null)
					ret.add(t);
		} else if (a.containsTarget(entity)) {
			for (String t : a.getSources())
				if (a.get(entity, t) != null)
					ret.add(t);			
		}
		
		return ret;
	}
		
	/**
	 * Check whether the domains of two properties are mapped to each other in the active alignment.
	 * @param o1 source ontology
	 * @param o2 target ontology
	 * @param p1 a property in the source ontology
	 * @param p2 a property in the target ontology
	 * @return whether a mapping exists
	 */
	private static boolean domainsEquivalent(Ontology o1, Ontology o2, String p1, String p2) {
		EntityMap e = AML.getInstance().getEntityMap();

		Set<String> p1Domains = new HashSet<String>(e.getDomains(p1));
		Set<String> p2Domains = new HashSet<String>(e.getDomains(p2));
		
		// An empty domain is equal to Owl:Thing
		p1Domains.remove("http://www.w3.org/2002/07/owl#Thing");
		p2Domains.remove("http://www.w3.org/2002/07/owl#Thing");
		
		if (p1Domains.size() != p2Domains.size())
			return false;
		for (String c1 : p1Domains) {
			boolean found = false;
			for (String c2 : p2Domains) {
				if (AML.getInstance().getAlignment().get(c1, c2) != null)
					found = true;
			}
			
			if (!found)
				return false;
		}
		
		return true;
	}
	
	/**
	 * Removes entities from the set, if there already exists a mapping
	 * for it in the current alignment.
	 * @param entities the set of entities to filter.
	 * @return the set with the mapped entities removed.
	 */
	private static Set<String> removeMappedEntities(Set<String> entities)
	{
		Set<String> ret = new HashSet<String>(entities);
		SimpleAlignment ref = (SimpleAlignment) AML.getInstance().getAlignment();
		if (ref == null)
			return ret;
				
		if (ref.getSourceOntology().containsAll(entities))
			ret.removeAll(ref.getSources());
		else if (ref.getTargetOntology().containsAll(entities))
			ret.removeAll(ref.getTargets());

		return ret;
	}

	/**
	 * @param o An ontology
	 * @return all data property triplets in the ontology
	 */
	public static Set<Triple<String,String,String>> dataPropertyTriplets(Ontology o) {
		Set<Triple<String,String,String>> triples = new HashSet<Triple<String,String,String>>();
				
		ValueMap v = o.getValueMap();
		
		for (String individual : v.getIndividuals())
			for (String property : v.getProperties(individual))
				for (String value : v.getValues(individual, property))
					triples.add(new Triple<>(individual, property, value));
		
		return triples;
	}
	
	/**
	 * @param o An ontology
	 * @return all object property triplets in the ontology
	 */
	public static Set<Triple<String,String,String>> objectPropertyTriplets(Ontology o) {
		Set<Triple<String,String,String>> triples = new HashSet<Triple<String,String,String>>();
		
		EntityMap e = AML.getInstance().getEntityMap();

		for (String individual1 : o.getEntities(EntityType.INDIVIDUAL))
			for (String individual2 : e.getIndividualActiveRelations(individual1))
				for (String property : e.getIndividualProperties(individual1, individual2))
					if (o.contains(property)) // Ensure the property is also in this ontology
						triples.add(new Triple<>(individual1, property, individual2));
		
		return triples;
	}


}