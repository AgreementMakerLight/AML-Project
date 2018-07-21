/******************************************************************************
* Copyright 2013-2018 LASIGE                                                  *
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
* An alignment between two Ontologies, stored both as a list of Mappings and  *
* as a Table of indexes, and including methods for input and output.          *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.alignment;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import aml.AML;
import aml.ontology.EntityType;
import aml.ontology.RelationshipMap;
import aml.util.Map2MapComparable;

public class Alignment implements Collection<Mapping>
{

//Attributes

	//Ontology uris (as listed in alignment file)
	private String sourceURI;
	private String targetURI;
	//Mappings organized in list
	private Vector<Mapping> maps;
	//Simple mappings organized by entity1 (entity1, entity2, Mapping)
	private Map2MapComparable<String,String,Mapping> sourceMaps;
	//Simple mappings organized by entity2 (entity2, entity1, Mapping)
	private Map2MapComparable<String,String,Mapping> targetMaps;

	
//Constructors

	/**
	 * Creates a new empty Alignment between the source and target ontologies
	 */
	public Alignment()
	{
		this(AML.getInstance().getSource().getURI(), AML.getInstance().getTarget().getURI());
	}

	/**
	 * Creates a new empty Alignment
	 */
	public Alignment(String sourceUri, String targetUri)
	{
		this.sourceURI = sourceUri;
		this.targetURI = targetUri;
		maps = new Vector<Mapping>(0,1);
		sourceMaps = new Map2MapComparable<String,String,Mapping>();
		targetMaps = new Map2MapComparable<String,String,Mapping>();
	}

	/**
	 * Creates a new Alignment that contains the input collection of mappings
	 * @param a: the collection of mappings to include in this Alignment
	 */
	public Alignment(Collection<Mapping> a)
	{
		this();
		addAll(a);
	}
	
//Public Methods

	/**
	 * Adds a new Mapping to the Alignment if it is non-redundant
	 * Otherwise, updates the similarity of the already present Mapping
	 * to the maximum similarity of the two redundant Mappings
	 * @param entity1: the uri of the entity1 class to add to the Alignment
	 * @param entity2: the uri of the entity2 class to add to the Alignment
	 * @param sim: the similarity between the classes
	 */
	public void add(String entity1, String entity2, double sim)
	{
		add(entity1,entity2,sim,MappingRelation.EQUIVALENCE,MappingStatus.UNKNOWN);
	}
	
	/**
	 * Adds a new Mapping to the Alignment if it is non-redundant
	 * Otherwise, updates the similarity of the already present Mapping
	 * to the maximum similarity of the two redundant Mappings
	 * @param entity1: the uri of the entity1 class to add to the Alignment
	 * @param entity2: the uri of the entity2 class to add to the Alignment
	 * @param sim: the similarity between the classes
	 * @param r: the mapping relationship between the classes
	 */
	public boolean add(String entity1, String entity2, double sim, MappingRelation r)
	{
		return add(entity1,entity2,sim,r,MappingStatus.UNKNOWN);
	}
	
	/**
	 * Adds a new Mapping to the Alignment if it is non-redundant
	 * Otherwise, updates the similarity of the already present Mapping
	 * to the maximum similarity of the two redundant Mappings
	 * @param entity1: the uri of the entity1 class to add to the Alignment
	 * @param entity2: the uri of the entity2 class to add to the Alignment
	 * @param sim: the similarity between the classes
	 * @param r: the mapping relationship between the classes
	 * @param s: the mapping status
	 */
	public boolean add(String entity1, String entity2, double sim, MappingRelation r, MappingStatus s)
	{
		//We shouldn't have a mapping involving entities that exist in
		//both ontologies as they are the same entity, and therefore
		//shouldn't map with other entities in either ontology, unless
		//same URI matching is turned on
		if(!AML.getInstance().matchSameURI() && entity1.equals(entity2))
			return false;
		
		//Construct the Mapping
		Mapping m = new SimpleMapping(entity1, entity2, sim, r);
		m.setStatus(s);
		//If it isn't listed yet, add it
		if(!sourceMaps.contains(entity1,entity2))
		{
			maps.add(m);
			sourceMaps.add(entity1, entity2, m);
			targetMaps.add(entity2, entity1, m);
			return true;
		}
		//Otherwise update the similarity
		else
		{
			m = sourceMaps.get(entity1,entity2);
			boolean check = false;
			if(m.getSimilarity() < sim)
			{
				m.setSimilarity(sim);
				check = true;
			}
			if(!m.getRelationship().equals(r))
			{
				m.setRelationship(r);
				check = true;
			}
			if(!m.getStatus().equals(s))
			{
				m.setStatus(s);
				check = true;
			}
			return check;
		}
	}
	
	
	@Override
	public boolean add(Mapping m)
	{
		if(m instanceof Mapping)
			return add((String)m.getEntity1(),(String)m.getEntity2(),m.getSimilarity(),m.getRelationship(),m.getStatus());
		else
			return false;
	}

	@Override
	public boolean addAll(Collection<? extends Mapping> a)
	{
		boolean check = false;
		for(Mapping m : a)
			check = add(m) || check;
		return check;
	}
	
	/**
	 * Adds all Mappings in a to this Alignment as long as
	 * they don't conflict with any Mapping in a
	 * @param a: the Alignment to add to this Alignment
	 */
	public void addAllNonConflicting(Alignment a)
	{
		Vector<Mapping> nonConflicting = new Vector<Mapping>();
		for(Mapping m : a.maps)
			if(!this.containsConflict(m))
				nonConflicting.add(m);
		addAll(nonConflicting);
	}
	
	/**
	 * Adds all Mappings in a to this Alignment as long as
	 * they don't conflict with any Mapping in a
	 * @param a: the Alignment to add to this Alignment
	 */
	public void addAllOneToOne(Alignment a)
	{
		a.sortDescending();
		for(Mapping m : a.maps)
			if(!this.containsConflict(m))
				add(m);
	}
	
	/**
	 * @return the average cardinality of this Alignment
	 */
	public double cardinality()
	{
		double cardinality = 0.0;
		
		Set<String> sources = sourceMaps.keySet();
		for(String i : sources)
			cardinality += sourceMaps.keySet(i).size();
		
		Set<String> targets = targetMaps.keySet();
		for(String i : targets)
			cardinality += targetMaps.keySet(i).size();
		cardinality /= sources.size() + targets.size();
		
		return cardinality;		
	}
	
	/**
	 * @param uri: the uri of the entity to check in the Alignment
	 * @return the cardinality of the entity in the Alignment
	 */
	public int cardinality(String uri)
	{
		if(sourceMaps.contains(uri))
			return sourceMaps.get(uri).size();
		if(targetMaps.contains(uri))
			return targetMaps.get(uri).size();
		return 0;
	}
	
	@Override
	public void clear()
	{
		maps = new Vector<Mapping>(0,1);
		sourceMaps = new Map2MapComparable<String,String,Mapping>();
		targetMaps = new Map2MapComparable<String,String,Mapping>();		
	}
	
	/**
	 * @param entity1: the uri of the entity1 class to check in the Alignment
 	 * @param entity2: the uri of the entity2 class to check in the Alignment
 	 * @param r: the MappingRelation to check in the Alignment
	 * @return whether the Alignment contains a Mapping between entity1 and entity2
	 * with relationship r
	 */
	public boolean contains(String entity1, String entity2, MappingRelation r)
	{
		return sourceMaps.contains(entity1, entity2) &&
				getRelationship(entity1,entity2).equals(r);
	}

	@Override
	public boolean contains(Object o)
	{
		return o instanceof SimpleMapping && contains((String)((SimpleMapping)o).getEntity1(),
				(String)((SimpleMapping)o).getEntity2(), ((SimpleMapping)o).getRelationship());
	}
	
	@Override
	public boolean containsAll(Collection<?> c)
	{
		for(Object o : c)
			if(!contains(o))
				return false;
		return true;
	}
	
	/**
	 * @param entity1: the uri of the entity1 class to check in the Alignment
 	 * @param entity2: the uri of the entity2 class to check in the Alignment 
	 * @return whether the Alignment contains a Mapping that is ancestral to the given pair of classes
	 * (i.e. includes one ancestor of entity1 and one ancestor of entity2)
	 */
	public boolean containsAncestralMapping(String entity1, String entity2)
	{
		RelationshipMap rels = AML.getInstance().getRelationshipMap();
		
		Set<String> sourceAncestors = rels.getAncestors(entity1);
		Set<String> targetAncestors = rels.getAncestors(entity2);
		
		for(String sa : sourceAncestors)
		{
			Set<String> over = getSourceMappings(sa);
			for(String ta : targetAncestors)
				if(over.contains(ta))
					return true;
		}
		return false;
	}

	/**
 	 * @param m: the Mapping to check in the Alignment 
	 * @return whether the Alignment contains a Mapping that conflicts with the given
	 * Mapping and has a higher similarity
	 */
	public boolean containsBetterMapping(Mapping m)
	{
		if(!(m instanceof SimpleMapping))
			return false;
		
		String entity1 = (String)m.getEntity1();
		String entity2 = (String)m.getEntity2();
		double sim = m.getSimilarity();
		if(containsSource(entity1))
		{
			Set<String> targets = sourceMaps.keySet(entity1);
			for(String i : targets)
				if(getSimilarity(entity1,i) > sim)
					return true;
		}
		if(containsTarget(entity2))
		{
			Set<String> sources = targetMaps.keySet(entity2);
			for(String i : sources)
				if(getSimilarity(i,entity2) > sim)
					return true;
		}
		return false;
	}
	
	/**
 	 * @param uri: the uri of the class to check in the Alignment 
	 * @return whether the Alignment contains a Mapping with that class
	 * (either as a entity1 or as a entity2 class)
	 */
	public boolean containsEntity(String uri)
	{
		return containsSource(uri) || containsTarget(uri);
	}
	
	/**
	 * @param entity1: the uri of the entity1 class to check in the Alignment
 	 * @param entity2: the uri of the entity2 class to check in the Alignment 
	 * @return whether the Alignment contains another Mapping for entity1 or for entity2
	 */
	public boolean containsConflict(String entity1, String entity2)
	{
		for(String s : getTargetMappings(entity2))
			if(!s.equals(entity1))
				return true;
		for(String t : getSourceMappings(entity1))
			if(!t.equals(entity2))
				return true;
		return false;
	}
	
	/**
 	 * @param m: the Mapping to check in the Alignment 
	 * @return whether the Alignment contains another Mapping involving either class in m
	 */
	public boolean containsConflict(Mapping m)
	{
		if(m instanceof SimpleMapping)
			return containsConflict((String)m.getEntity1(),(String)m.getEntity2());
		return false;
	}
	
	/**
	 * @param entity1: the uri of the entity1 to check in the Alignment
 	 * @param entity2: the uri of the entity2 to check in the Alignment 
	 * @return whether the Alignment contains a Mapping that is descendant of the given pair of classes
	 * (i.e. includes one descendant of entity1 and one descendant of entity2)
	 */
	public boolean containsDescendantMapping(String entity1, String entity2)
	{
		RelationshipMap rels = AML.getInstance().getRelationshipMap();
		
		Set<String> sourceDescendants = rels.getDescendants(entity1);
		Set<String> targetDescendants = rels.getDescendants(entity2);
		
		for(String sa : sourceDescendants)
		{
			Set<String> over = getSourceMappings(sa);
			for(String ta : targetDescendants)
				if(over.contains(ta))
					return true;
		}
		return false;
	}
	
	/**
	 * @param entity1: the uri of the entity1 class to check in the Alignment
	 * @param entity2: the uri of the entity2 class to check in the Alignment
	 * @return whether the Alignment contains a Mapping between entity1 and entity2
	 */
	public boolean containsMapping(String entity1, String entity2)
	{
		return sourceMaps.contains(entity1, entity2);
	}
	
	/**
	 * @param m: the Mapping to check in the Alignment
	 * @return whether the Alignment contains a Mapping with the same entity1
	 * and entity2 as m (regardless of the mapping relation)
	 */
	public boolean containsMapping(Mapping m)
	{
		return sourceMaps.contains((String)m.getEntity1(), (String)m.getEntity2());
	}
	
	/**
	 * @param lm: the List of Mapping to check in the Alignment
	 * @return whether the Alignment contains all the Mapping listed in m
	 */
	public boolean containsMappings(List<Mapping> lm)
	{
		for(Mapping m: lm)
			if(!containsMapping(m))
				return false;
		return true;
	}
	
	/**
	 * @param entity1: the uri of the entity1 class to check in the Alignment
 	 * @param entity2: the uri of the entity2 class to check in the Alignment 
	 * @return whether the Alignment contains a Mapping that is parent to the
	 * given pair of classes on one side only
	 */
	public boolean containsParentMapping(String entity1, String entity2)
	{
		RelationshipMap rels = AML.getInstance().getRelationshipMap();
		
		Set<String> sourceAncestors = rels.getParents(entity1);
		Set<String> targetAncestors = rels.getParents(entity2);
		
		for(String sa : sourceAncestors)
			if(containsMapping(sa,entity2))
				return true;
		for(String ta : targetAncestors)
			if(containsMapping(entity1,ta))
				return true;
		return false;
	}
	
	/**
	 * @param entity1: the uri of the entity1 class to check in the Alignment
 	 * @return whether the Alignment contains a Mapping for entity1
	 */
	public boolean containsSource(String entity1)
	{
		return sourceMaps.contains(entity1);
	}

	/**
	 * @param entity2: the uri of the entity2 class to check in the Alignment
 	 * @return whether the Alignment contains a Mapping for entity2
	 */
	public boolean containsTarget(String entity2)
	{
		return targetMaps.contains(entity2);
	}
	
	/**
 	 * @return the number of conflict mappings in this alignment
	 */
	public int countConflicts()
	{
		int count = 0;
		for(Mapping m : maps)
			if(m.getRelationship().equals(MappingRelation.UNKNOWN))
				count++;
		return count;
	}
	
	/**
	 * @param a: the Alignment to subtract from this Alignment 
	 * @return the Alignment corresponding to the difference between this Alignment and a
	 */
	public Alignment difference(Alignment a)
	{
		Alignment diff = new Alignment();
		for(Mapping m : maps)
			if(!a.contains(m))
				diff.add(m);
		return diff;
	}
	
	@Override
	public boolean equals(Object o)
	{
		return o instanceof Alignment && containsAll((Alignment)o);
	}
	
	/**
	 * @param ref: the reference Alignment to evaluate this Alignment
	 * @return the evaluation of this Alignment {# correct mappings, # conflict mappings}
	 */
	public int[] evaluate(Alignment ref)
	{
		int[] count = new int[2];
		for(Mapping m : maps)
		{
			if(ref.contains(m))
			{
				count[0]++;
				m.setStatus(MappingStatus.CORRECT);
			}
			else if(ref.contains((String)m.getEntity1(),(String)m.getEntity2(),MappingRelation.UNKNOWN))
			{
				count[1]++;
				m.setStatus(MappingStatus.UNKNOWN);
			}
			else
				m.setStatus(MappingStatus.INCORRECT);
		}
		return count;
	}

	/**
	 * @param a: the base Alignment to which this Alignment will be compared 
	 * @return the gain (i.e. the fraction of new Mappings) of this Alignment
	 * in comparison with the base Alignment
	 */
	public double gain(Alignment a)
	{
		double gain = 0.0;
		for(Mapping m : maps)
			if(!a.containsMapping(m))
				gain++;
		gain /= a.size();
		return gain;
	}
	
	/**
	 * @param a: the base Alignment to which this Alignment will be compared 
	 * @return the gain (i.e. the fraction of new Mappings) of this Alignment
	 * in comparison with the base Alignment
	 */
	public double gainOneToOne(Alignment a)
	{
		double sourceGain = 0.0;
		Set<String> sources = sourceMaps.keySet();
		for(String i : sources)
			if(!a.containsSource(i))
				sourceGain++;
		sourceGain /= a.sourceCount();
		double targetGain = 0.0;
		Set<String> targets = targetMaps.keySet();
		for(String i : targets)
			if(!a.containsTarget(i))
				targetGain++;
		targetGain /= a.targetCount();
		return Math.min(sourceGain, targetGain);
	}
	
	/**
	 * @param index: the uri of the Mapping to return in the list of Mappings
 	 * @return the Mapping at the input index (note that the uri will change
 	 * during sorting) or null if the uri falls outside the list
	 */
	public Mapping get(int index)
	{
		if(index < 0 || index >= maps.size())
			return null;
		return maps.get(index);
	}
	
	/**
	 * @param entity1: the uri of the entity1 class to check in the Alignment
	 * @param entity2: the uri of the entity2 class to check in the Alignment
 	 * @return the Mapping between the entity1 and entity2 classes or null if no
 	 * such Mapping exists
	 */
	public Mapping get(String entity1, String entity2)
	{
		return sourceMaps.get(entity1, entity2);
	}
	
	/**
	 * @param id1: the uri of the first class to check in the Alignment
	 * @param entity2: the uri of the second class to check in the Alignment
 	 * @return the Mapping between the classes or null if no such Mapping exists
 	 * in either direction
	 */
	public Mapping getBidirectional(String uri1, String uri2)
	{
		if(sourceMaps.contains(uri1, uri2))
			return sourceMaps.get(uri1, uri2);
		else if(sourceMaps.contains(uri2, uri1))
			return  sourceMaps.get(uri2, uri1);
		else
			return null;
	}
	
	/**
	 * @param entity1: the uri of the entity1 class to check in the Alignment
 	 * @return the uri of the entity2 class that best matches entity1
	 */
	public String getBestSourceMatch(String entity1)
	{
		double max = 0;
		String entity2 = "";
		Set<String> targets = sourceMaps.keySet(entity1);
		for(String i : targets)
		{
			double sim = getSimilarity(entity1,i);
			if(sim > max)
			{
				max = sim;
				entity2 = i;
			}
		}
		return entity2;
	}

	/**
	 * @param entity2: the uri of the entity2 class to check in the Alignment
 	 * @return the uri of the entity1 class that best matches entity2
	 */
	public String getBestTargetMatch(String entity2)
	{
		double max = 0;
		String entity1 = "";
		Set<String> sources = sourceMaps.keySet(entity2);
		for(String i : sources)
		{
			double sim = getSimilarity(i,entity2);
			if(sim > max)
			{
				max = sim;
				entity1 = i;
			}
		}
		return entity1;
	}
	
	/**
	 * @param m: the Mapping to check on the Alignment
	 * @return the list of all Mappings that have a cardinality conflict with the given Mapping
	 */
	public Vector<Mapping> getConflicts(Mapping m)
	{
		Vector<Mapping> conflicts = new Vector<Mapping>();
		for(String t : sourceMaps.keySet((String)m.getEntity1()))
			if(t != (String)m.getEntity2())
				conflicts.add(sourceMaps.get((String)m.getEntity1(),t));
		for(String s : targetMaps.keySet((String)m.getEntity2()))
			if(s != (String)m.getEntity1())
				conflicts.add(sourceMaps.get(s,(String)m.getEntity2()));
		return conflicts;
	}
	
	/**
	 * @return the high level Alignment induced from this Alignment
	 * (the similarity between high level classes is given by the
	 * fraction of classes in this Alignment that are their descendents)
	 */
	public Alignment getHighLevelAlignment()
	{
		RelationshipMap rels = AML.getInstance().getRelationshipMap();
		
		Alignment a = new Alignment();
		int total = maps.size();
		for(Mapping m : maps)
		{
			Set<String> sourceAncestors = rels.getHighLevelAncestors((String)m.getEntity1());
			Set<String> targetAncestors = rels.getHighLevelAncestors((String)m.getEntity2());
			for(String i : sourceAncestors)
			{
				for(String j : targetAncestors)
				{
					double sim = a.getSimilarity(i, j) + 1.0 / total;
					a.add(i,j,sim,MappingRelation.OVERLAP);
				}
			}
		}
		Alignment b = new Alignment();
		for(Mapping m : a)
			if(m.getSimilarity() >= 0.01)
				b.add(m);
		return b;
	}
	
	/**
	 * @param entity1: the uri of the entity1 class
	 * @param entity2: the uri of the entity2 class
	 * @return the uri of the Mapping between the given classes in
	 * the list of Mappings, or -1 if the Mapping doesn't exist
	 */
	public int getIndex(String entity1, String entity2)
	{
		if(sourceMaps.contains(entity1, entity2))
			return maps.indexOf(sourceMaps.get(entity1, entity2));
		else
			return -1;
	}
	
	/**
	 * @param uri1: the uri of the first class
	 * @param uri2: the uri of the second class
	 * @return the uri of the Mapping between the given classes in
	 * the list of Mappings (in any order), or -1 if the Mapping doesn't exist
	 */
	public int getIndexBidirectional(String uri1, String uri2)
	{
		if(sourceMaps.contains(uri1, uri2))
			return maps.indexOf(sourceMaps.get(uri1, uri2));
		else if(targetMaps.contains(uri1, uri2))
			return maps.indexOf(targetMaps.get(uri1, uri2));
		else
			return -1;
	}
	
	/**
	 * @param uri: the uri of the class to check in the Alignment
 	 * @return the list of all classes mapped to the given class
	 */
	public Set<String> getMappingsBidirectional(String uri)
	{
		HashSet<String> mappings = new HashSet<String>();
		if(sourceMaps.contains(uri))
			mappings.addAll(sourceMaps.keySet(uri));
		if(targetMaps.contains(uri))
			mappings.addAll(targetMaps.keySet(uri));
		return mappings;
	}
	
	/**
	 * @param entity1: the uri of the entity1 class to check in the Alignment
 	 * @return the uri of the entity2 class that best matches entity1
	 */
	public double getMaxSourceSim(String entity1)
	{
		double max = 0;
		Set<String> targets = sourceMaps.keySet(entity1);
		for(String i : targets)
		{
			double sim = getSimilarity(entity1,i);
			if(sim > max)
				max = sim;
		}
		return max;
	}

	/**
	 * @param entity2: the uri of the entity2 class to check in the Alignment
 	 * @return the uri of the entity1 class that best matches entity2
	 */
	public double getMaxTargetSim(String entity2)
	{
		double max = 0;
		Set<String> sources = targetMaps.keySet(entity2);
		for(String i : sources)
		{
			double sim = getSimilarity(i,entity2);
			if(sim > max)
				max = sim;
		}
		return max;
	}
	
	/**
	 * @param entity1: the uri of the entity1 class in the Alignment
	 * @param entity2: the uri of the entity2 class in the Alignment
	 * @return the mapping relationship between entity1 and entity2
	 */
	public MappingRelation getRelationship(String entity1, String entity2)
	{
		Mapping m = sourceMaps.get(entity1, entity2);
		if(m == null)
			return null;
		return m.getRelationship();
	}
	
	/**
	 * @param entity1: the uri of the entity1 class in the Alignment
	 * @param entity2: the uri of the entity2 class in the Alignment
	 * @return the similarity between entity1 and entity2
	 */
	public double getSimilarity(String entity1, String entity2)
	{
		Mapping m = sourceMaps.get(entity1, entity2);
		if(m == null)
			return 0.0;
		return m.getSimilarity();
	}
	
	/**
	 * @param entity1: the uri of the entity1 class in the Alignment
	 * @param entity2: the uri of the entity2 class in the Alignment
	 * @return the similarity between entity1 and entity2 in percentage
	 */
	public String getSimilarityPercent(String entity1, String entity2)
	{
		Mapping m = sourceMaps.get(entity1, entity2);
		if(m == null)
			return "0%";
		return m.getSimilarityPercent();
	}
	
	/**
	 * @param entity1: the uri of the entity1 class to check in the Alignment
 	 * @return the list of all entity2 classes mapped to the entity1 class
	 */
	public Set<String> getSourceMappings(String entity1)
	{
		if(sourceMaps.contains(entity1))
			return sourceMaps.keySet(entity1);
		return new HashSet<String>();
	}
	
	/**
 	 * @return the list of all entity1 classes that have mappings
	 */
	public Set<String> getSources()
	{
		HashSet<String> sMaps = new HashSet<String>();
		sMaps.addAll(sourceMaps.keySet());
		return sMaps;
	}

	/**
	 * @return the URI of the source ontology
	 */
	public String getSourceURI()
	{
		return sourceURI;
	}
	
	/**
	 * @param entity2: the uri of the entity2 class to check in the Alignment
 	 * @return the list of all entity1 classes mapped to the entity2 class
	 */
	public Set<String> getTargetMappings(String entity2)
	{
		if(targetMaps.contains(entity2))
			return targetMaps.keySet(entity2);
		return new HashSet<String>();
	}
	
	/**
 	 * @return the list of all entity2 classes that have mappings
	 */
	public Set<String> getTargets()
	{
		HashSet<String> tMaps = new HashSet<String>();
		tMaps.addAll(targetMaps.keySet());
		return tMaps;
	}
	
	/**
	 * @return the URI of the target ontology
	 */
	public String getTargetURI()
	{
		return targetURI;
	}


	@Override
	public int hashCode()
	{
		return maps.hashCode();
	}
	
	/**
	 * @param a: the Alignment to intersect with this Alignment 
	 * @return the Alignment corresponding to the intersection between this Alignment and a
	 */
	public Alignment intersection(Alignment a)
	{
		//Otherwise, compute the intersection
		Alignment intersection = new Alignment();
		for(Mapping m : maps)
			if(a.contains(m))
				intersection.add(m);
		return intersection;
	}
	
	@Override
	public boolean isEmpty()
	{
		return maps.isEmpty();
	}
	
	@Override
	public Iterator<Mapping> iterator()
	{
		return maps.iterator();
	}
	
	/**
	 * @return the maximum cardinality of this Alignment
	 */
	public double maxCardinality()
	{
		double cardinality;
		double max = 0.0;
		
		Set<String> sources = sourceMaps.keySet();
		for(String i : sources)
		{
			cardinality = sourceMaps.keySet(i).size();
			if(cardinality > max)
				max = cardinality;
		}
		Set<String> targets = targetMaps.keySet();
		for(String i : targets)
		{
			cardinality = targetMaps.keySet(i).size();
			if(cardinality > max)
				max = cardinality;
		}
		return max;		
	}
	
	@Override
	public boolean remove(Object o)
	{
		if(o instanceof SimpleMapping && contains(o))
		{
			Mapping m = (Mapping)o;
			String entity1 = (String)m.getEntity1();
			String entity2 = (String)m.getEntity2();
			sourceMaps.remove(entity1, entity2);
			targetMaps.remove(entity2, entity1);
			maps.remove(m);
			return true;
		}
		else
			return false;
	}
	
	/**
	 * Removes the Mapping between the given classes from the Alignment
	 * @param entity1: the entity1 class to remove from the Alignment
	 * @param entity2: the entity2 class to remove from the Alignment
	 */
	public boolean remove(String entity1, String entity2)
	{
		Mapping m = new SimpleMapping(entity1, entity2, 1.0);
		return remove(m);
	}
	
	@Override
	public boolean removeAll(Collection<?> c)
	{
		boolean check = false;
		for(Object o : c)
			check = remove(o) || check;
		return check;
	}
	
	@Override
	public boolean retainAll(Collection<?> c)
	{
		boolean check = false;
		for(Mapping m : this)
			if(!c.contains(m))
				check = remove(m) || check;
		return check;
	}
	
	@Override
	public int size()
	{
		return maps.size();
	}

	/**
	 * Sorts the Alignment ascendingly
	 */
	public void sortAscending()
	{
		Collections.sort(maps);
	}
	
	/**
	 * Sorts the Alignment descendingly
	 */
	public void sortDescending()
	{
		Collections.sort(maps,new Comparator<Mapping>()
        {
			//Sorting in descending order can be done simply by
			//reversing the order of the elements in the comparison
            public int compare(Mapping m1, Mapping m2)
            {
        		return m2.compareTo(m1);
            }
        } );
	}
	
	/**
	 * @return the number of entity1 entities mapped in this Alignment
	 */
	public int sourceCount()
	{
		return sourceMaps.keyCount();
	}
	
	/**
	 * @return the fraction of entity1 classes mapped in this Alignment
	 */
	public double sourceCoverage(EntityType e)
	{
		double coverage = 0.0;
		for(String i : sourceMaps.keySet())
			if(AML.getInstance().getURIMap().getTypes(i).contains(e))
				coverage++;
		int count;
		if(e.equals(EntityType.INDIVIDUAL))
			count = AML.getInstance().getSourceIndividualsToMatch().size();
		else
			count = AML.getInstance().getSource().count(e);
		return coverage / count;
	}
	
	/**
	 * @return the number of entity2 entities mapped in this Alignment
	 */
	public int targetCount()
	{
		return targetMaps.keyCount();
	}
	
	/**
	 * @return the fraction of entity2 classes mapped in this Alignment
	 */
	public double targetCoverage(EntityType e)
	{
		double coverage = 0.0;
		for(String i : targetMaps.keySet())
			if(AML.getInstance().getURIMap().getTypes(i).equals(e))
				coverage++;
		int count;
		if(e.equals(EntityType.INDIVIDUAL))
			count = AML.getInstance().getTargetIndividualsToMatch().size();
		else
			count = AML.getInstance().getTarget().count(e);
		return coverage / count;
	}
	
	@Override
	public Object[] toArray()
	{
		return maps.toArray();
	}
	
	@Override
	public <T> T[] toArray(T[] a)
	{
		return maps.toArray(a);
	}
}