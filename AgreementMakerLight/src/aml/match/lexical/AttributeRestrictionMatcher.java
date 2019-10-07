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
 * Matches Ontologies by measuring the maximum String similarity between their *  ////FIXME
 * classes, using one of the four available String similarity measures.        *
 *                                                                             *
 * WARNING: This matching algorithm takes O(N^2) time, and thus should be used *
 * either to match small ontologies or as a SecondaryMatcher.                  *
 *                                                                             *
 * @authors Daniel Faria                                                       *
 ******************************************************************************/
package aml.match.lexical;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;

import aml.AML;
import aml.alignment.EDOALAlignment;
import aml.alignment.SimpleAlignment;
import aml.alignment.mapping.EDOALMapping;
import aml.alignment.rdf.*;
import aml.match.BiDirectionalMatcher;
import aml.ontology.EntityType;
import aml.ontology.Ontology;
import aml.ontology.lexicon.StopList;
import aml.ontology.semantics.EntityMap;
import aml.util.data.Couple;
import aml.util.similarity.Similarity;
import aml.util.similarity.StringSimMeasure;

public class AttributeRestrictionMatcher extends BiDirectionalMatcher
{
	//Attributes
	protected static final String DESCRIPTION = "Generates mappings corresponding to EDOAL expressions AttributeDomainRestriction and AttributeOccurenceRestriction";
	protected static final String NAME = "Attribute Restriction Matcher";
	protected static final EntityType[] SUPPORT = {EntityType.CLASS,EntityType.INDIVIDUAL,EntityType.DATA_PROP,EntityType.OBJECT_PROP};

	private StringSimMeasure measure = StringSimMeasure.ISUB; // The similarity measure used for entity label comparison
	Set<String> stopWords;

	//Constructors

	/**
	 * Constructs a new AttributeRestrictionMatcher with default string similarity measure (ISub)
	 */
	public AttributeRestrictionMatcher()
	{
		super();
		description = DESCRIPTION;
		name = NAME;
		support = SUPPORT;
		stopWords = StopList.read();
	}

	/**
	 * Constructs a new AttributeRestrictionMatcher with the given String similarity measure
	 * @param m the string similarity measure
	 */
	public AttributeRestrictionMatcher(StringSimMeasure m)
	{
		this();
		measure = m;
	}

	//Protected Methods
	/**
	 * Matches the source and target Ontologies, returning an Alignment between them
	 * @param o1 the source Ontology
	 * @param o2 the target Ontology
	 * @param e the EntityType to match
	 * @param thresh the similarity threshold for the alignment
	 * @return the alignment between the source and target ontologies
	 */
	protected EDOALAlignment uniMatch(Ontology o1, Ontology o2, EntityType e, double thresh)
	{
		EDOALAlignment aor = AttributeOccurenceRestrictionMatch(o1, o2, thresh);
		EDOALAlignment adr = AttributeDomainRestrictionMatch(o1, o2, thresh);
		aor.addAll(adr);
		
		return aor;
	}

	//Public Methods

	/**
	 * Returns the intersection of sets a and b
	 * @param a
	 * @param b
	 */
	public static <A> Set<A> setIntersection(Collection<A> a, Collection<A> b) {
		Set<A> c = new HashSet<A>(a);
		c.retainAll(b);
		return c;
	}

	/**
	 * Splits the string into a list of words. Separate words can be denoted
	 * by whitespace, special characters and camel case.
	 * @param s
	 * @return
	 */
	public static ArrayList<String> splitString(String s) {
		ArrayList<String> ret = new ArrayList<String>();
		
		for (String word : StringUtils.splitByCharacterTypeCamelCase(s)) {
			word = word.trim();
			
			if (!StringUtils.isBlank(word) && StringUtils.isAlphanumeric(word))
				ret.add(word);
		}
		
		return ret;
	}

	/**
	 * @param s
	 * @param words
	 * @return The string s with each word in the set removed.
	 */
	public static String removeWords(String s, Collection<String> words) {
		String ret = "";
		
		Set<String> x = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
		x.addAll(words);

		for (String word : splitString(s))
			if (!x.contains(word))
				ret += word + " ";			

		return ret.trim();		
	}
	
	/**
	 * @param s
	 * @param t
	 * @return The string s wich each word in string t removed.
	 */
	public static String removeWords(String s, String t) {
		return removeWords(s, splitString(t));
	}

	/**
	 * Removes all entities from the set, if there already exists a mapping
	 * for it in the current alignment.
	 * @param entities the set of entities to filter.
	 * @return the set with the mapped entities removed.
	 */
	public static Set<String> removeMappedEntities(Set<String> entities)
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

	//Private Methods
		
	/**
	 * Finds mappings corresponding to an AttributeOccurenceRestriction
	 * @param o1 the source Ontology to match
	 * @param o2 the target Ontology to match
	 * @param thresh the similarity threshold for the alignment
	 * @return the alignment between the source and target ontologies
	 */
	private EDOALAlignment AttributeOccurenceRestrictionMatch(Ontology o1, Ontology o2, double thresh)
	{
		EDOALAlignment a = new EDOALAlignment(o1, o2);
		
		// Retrieve all classes and object properties in each ontology
		Set<String> srcClasses = o1.getEntities(EntityType.CLASS);
		Set<String> tgtClasses = o2.getEntities(EntityType.CLASS);
		
		// Remove from consideration any classes which already have a mapping
		srcClasses = removeMappedEntities(srcClasses);
		tgtClasses = removeMappedEntities(tgtClasses);

		// For each class c in o1, find object properties in o2 whose
		// domain is equivalent to a superclass of c
		for (String c : srcClasses) {
			EDOALMapping bestMapping = null;
				
			for (String p : o2.getEntities(EntityType.OBJECT_PROP)) {
				Couple<String,String> commonDomain = getCommonDomain(c, p);
				Couple<String,String> commonRange = getCommonRange(c, p);
				RelationExpression relation;
				
				// Filter common words from names for similarity comparison
				String className = removeWordsCommonWithParent(c, o1);
				String propName = o2.getName(p);

				// A superclass of c should be equal to the domain or range of the property
				if (commonDomain != null) {
					relation = new RelationId(p);
					propName = removeWords(propName, o2.getName(commonDomain.get2()));
				} else if (commonRange != null) {
					relation = getInverseRelation(p);
					propName = removeWords(propName, o2.getName(commonRange.get2()));
				} else
					continue;
				
				// Labels of the class and the property should also be similar
				double sim = stringSimilarity(className, propName, thresh, true);
				if (sim < thresh)
					continue;
				
				ClassId e1 = new ClassId(c);
				AttributeOccurrenceRestriction e2 =
						new AttributeOccurrenceRestriction(
								relation,
								new Comparator("http://ns.inria.org/edoal/1.0/#greater-than"),
								new NonNegativeInteger(0));
				
				if (bestMapping == null || bestMapping.getSimilarity() < sim)
					bestMapping = new EDOALMapping(e1, e2, sim);
			}
			
			if (bestMapping != null)
				a.add(bestMapping);
		}

		return a;
	}

	/**
	 * Given a class C in o1 and an object property P in o2, finds a superclass of
	 * C for which there is a mapping to either P's domain or to the parent of the domain.
	 * @param classURI the URI of the class
	 * @param propertyURI the URI of the property
	 * @return The URIs of the superclass and the domain as a pair, null if not found
	 */
	private Couple<String,String> getCommonDomain(String classURI, String propertyURI)
	{
		AML aml = AML.getInstance();
		SimpleAlignment ref = (SimpleAlignment) aml.getAlignment();
		if (ref == null)
			return null;

		// Treat undeclared superclasses as common
		if (aml.getEntityMap().getSuperclasses(classURI).isEmpty())
			return new Couple<>("", "");
		
		for (String sc : aml.getEntityMap().getSuperclasses(classURI)) {
			for (String domain : aml.getEntityMap().getDomains(propertyURI)) {
				// Check in both directions as o1 and o2 could be either source or target
				if (ref.get(sc, domain) != null)
					return new Couple<>(sc, domain);
				if (ref.get(domain, sc) != null)
					return new Couple<>(sc, domain);

				// Check superclasses
				for (String parent : aml.getEntityMap().getSuperclasses(domain, 1))
					if (ref.get(sc, parent) != null || ref.get(parent, sc) != null)
						return new Couple<>(sc, parent);
				for (String parent : aml.getEntityMap().getSuperclasses(domain, 2))
					if (ref.get(sc, parent) != null || ref.get(parent, sc) != null)
						return new Couple<>(sc, parent);
			}
		}
		
		return null;
	}

	/**
	 * Given a class C in o1 and an object property P in o2, finds a superclass of
	 * C for which there is a mapping to either P's domain or to the parent of the domain.
	 * @param classURI the URI of the class
	 * @param propertyURI the URI of the property
	 * @return The URIs of the superclass and the domain as a pair, null if not found
	 */
	private Couple<String,String> getCommonRange(String classURI, String propertyURI)
	{
		AML aml = AML.getInstance();
		SimpleAlignment ref = (SimpleAlignment) aml.getAlignment();
		if (ref == null)
			return null;
		
		// Treat undeclared superclasses as common
		if (aml.getEntityMap().getSuperclasses(classURI).isEmpty())
			return new Couple<>("", "");

		for (String sc : aml.getEntityMap().getSuperclasses(classURI)) {
			for (String domain : aml.getEntityMap().getRanges(propertyURI)) {
				// Check in both directions as o1 and o2 could be either source or target
				if (ref.get(sc, domain) != null)
					return new Couple<>(sc, domain);
				if (ref.get(domain, sc) != null)
					return new Couple<>(sc, domain);

				// Check superclasses
				for (String parent : aml.getEntityMap().getSuperclasses(domain, 1))
					if (ref.get(sc, parent) != null || ref.get(parent, sc) != null)
						return new Couple<>(sc, parent);
				for (String parent : aml.getEntityMap().getSuperclasses(domain, 2))
					if (ref.get(sc, parent) != null || ref.get(parent, sc) != null)
						return new Couple<>(sc, parent);
			}
		}
		
		return null;
	}
	
	/**
	 * Finds mappings corresponding to an AttributeDomainRestriction
	 * @param o1 the source Ontology to match
	 * @param o2 the target Ontology to match
	 * @param thresh the similarity threshold for the alignment
	 * @return the alignment between the source and target ontologies
	 */
	private EDOALAlignment AttributeDomainRestrictionMatch(Ontology o1, Ontology o2, double thresh)
	{
		EntityMap e = AML.getInstance().getEntityMap();
		EDOALAlignment a = new EDOALAlignment(o1, o2);
		
		// Retrieve all classes and object properties in each ontology
		Set<String> srcClasses = o1.getEntities(EntityType.CLASS);
		Set<String> tgtClasses = o2.getEntities(EntityType.CLASS);
		
		// Remove from consideration any classes which already have a mapping
		srcClasses = removeMappedEntities(srcClasses);
		tgtClasses = removeMappedEntities(tgtClasses);

		// For each class in o1, find properties pin o2 such that the
		// superclass of the class and the superclass of the domain of p2 are equivalent.
		for (String c : srcClasses) {
			EDOALMapping bestMapping = null;
			
			for (String p : o2.getEntities(EntityType.OBJECT_PROP)) {
				// c should have a superclass equal to the domain of p or one of its parents
				if (getCommonDomain(c, p) == null)
					continue;
				
				// Find a subclass of p's range s.t. the label is similar to c
				for (String r : e.getRanges(p)) {
					for (String rangeSubclass : e.getSubclasses(r)) {
						// 1 - check cases where c is similar to both p and rangeSubclass
						// e.g. O1: ClubMember(x), O2: isMemberOf(x, y) where the class of y is Club
						Set<String> commonWords = setIntersection(splitString(o1.getName(c)), splitString(o2.getName(p)));
						
						if (!commonWords.isEmpty()) {
							// Remove shared words for similarity comparison
							String className = removeWords(e.getLocalName(c), commonWords);
							String rangeName = removeWords(o2.getName(rangeSubclass), commonWords);

							double sim = stringSimilarity(className, rangeName, thresh);
							if (sim >= thresh) {
								ClassId e1 = new ClassId(c);
								AttributeDomainRestriction e2 =
										new AttributeDomainRestriction(new RelationId(p), 
												new ClassId(rangeSubclass),
												RestrictionElement.ALL);
							
								if (bestMapping == null || bestMapping.getSimilarity() < sim)
									bestMapping = new EDOALMapping(e1, e2, sim);
							}
						}
						
						// 2 - check cases where c is similar only to rangeSubclass
						// From the class, remove any words shared with the parent class
						// (i.e. XXXed_YYY becomes XXXed, if YYY is the superclass)
						String className = removeWordsCommonWithParent(c, o1);
						double sim = stringSimilarity(className, o2.getName(rangeSubclass), thresh, true);

						// Use a slightly stricter threshold to account for the fact that we are
						// filtering only by the class labels
						if (sim >= Math.sqrt(thresh)) {
							ClassId e1 = new ClassId(c);
							AttributeDomainRestriction e2 =
									new AttributeDomainRestriction(new RelationId(p), 
											new ClassId(rangeSubclass),
											RestrictionElement.ALL);
						
							if (bestMapping == null || bestMapping.getSimilarity() < sim)
								bestMapping = new EDOALMapping(e1, e2, sim);
						}
					}
				}
			}
			
			if (bestMapping != null)
				a.add(bestMapping);
		}
		return a;
	}
	
	/**
	 * Returns a RelationId for the inverse of p, using its real name if available.
	 * @param p the URI of the relation to invert
	 * @return the RelationId
	 */
	private static RelationExpression getInverseRelation(String p)
	{
		Set<String> inverse = AML.getInstance().getEntityMap().getInverseProperties(p);
		
		// If there is no named inverse relation, use the inverseOf constructor
		if (inverse.isEmpty())
			return new InverseRelation(new RelationId(p));
		else // Otherwise, just pick the first named inverse relation from the list
			return new RelationId(inverse.iterator().next());
	}

	/**
	 * @param uri the URI of the class
	 * @param o the ontology the class is part of
	 * @return`the name of the class stripped of any word also appearing in its parent classes
	 */
	private static String removeWordsCommonWithParent(String uri, Ontology o)
	{
		String name = o.getName(uri);
		
		for (String parent : AML.getInstance().getEntityMap().getSuperclasses(uri, 1))
			name = removeWords(name, o.getName(parent));
		
		return name;
	}

	/**
	 * Checks whether the characters in a are all upper case and that they match
	 * the initial characters of the words in b.
	 * @param a
	 * @param b
	 * @return whether a is an abbreviation of b 
	 */
	private static boolean isAcronym(String a, String b) {
		return WordUtils.initials(b).toUpperCase().equals(a);
	}

	/**
	 * Computes the string similarity between the two strings, ignoring stop
	 * words. If the similarity is less than the threshold, checks whether
	 * one of the strings is an abbreviation of the other one. In this case,
	 * a fixed confidence value equal to threshold is used.
	 * @param s
	 * @param t
	 * @param threshold
	 * @return The similarity
	 */
	private double stringSimilarity(String s, String t, double threshold) {
		String u = removeWords(s.toLowerCase(), stopWords);
		String v = removeWords(t.toLowerCase(), stopWords);
		
		double sim = Similarity.stringSimilarity(u, v, measure);
		
		if (sim < threshold && (isAcronym(s, t) || isAcronym(t, s)))
			sim = threshold;
		
		return sim;
	}
	
	/**
	 * Computes the string similarity between the two strings, stemming the words in
	 * the strings, if stem is set to true.
	 * @param s
	 * @param t
	 * @param threshold
	 * @param stem
	 * @return The similarity
	 */
	private double stringSimilarity(String s, String t, double threshold, boolean stem) {
		if (stem == true)
			return stringSimilarity(stemmedName(s), stemmedName(t), threshold);
		else
			return stringSimilarity(s, t, threshold);
	}
	
	/**
	 * Stems each word in the string s
	 * @param s
	 * @return
	 */
	private static String stemmedName(String s) {
		String ret = "";
		
		for (String word : splitString(s))
			ret += lazyStemmer(word) + " ";
		
		return ret.trim();
	}
	
	/**
	 * A very simple implementation of a stemmer
	 * @param s
	 * @return s with suffixes removed
	 */
	private static String lazyStemmer(String s)
	{
		String r = s.trim();

		r = r.replaceAll("zer$", "se");
		r = r.replaceAll("ed$", "");
		r = r.replaceAll("es$", "e");
		r = r.replaceAll("er$", "");
		r = r.replaceAll("ion$", "");
		r = r.replaceAll("ance$", "");
		
		// Remove one of any doubled letters at the end
		if (r.length() > 2 && r.charAt(r.length() - 1) == r.charAt(r.length() - 2)) {
			// But not tripled:
			if (r.length() > 3 && r.charAt(r.length() - 1) != r.charAt(r.length() - 3)) {
				// And only consonants:
				if ("aeouiy".indexOf(r.charAt(r.length() - 1)) == -1) {
					r = r.substring(0, r.length() - 1);
				}
			}
		}
		
		return r;
	}
}