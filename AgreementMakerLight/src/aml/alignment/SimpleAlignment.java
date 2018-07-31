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
* A simple alignment between two Ontologies, stored both as a list of         *
* Mappings and as a bidirectional table of mapped uris.                       *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.alignment;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import aml.AML;
import aml.alignment.mapping.Mapping;
import aml.alignment.mapping.MappingRelation;
import aml.alignment.mapping.MappingStatus;
import aml.alignment.mapping.SimpleMapping;
import aml.ontology.EntityType;
import aml.ontology.Ontology;
import aml.ontology.semantics.EntityMap;
import aml.util.data.Map2List;

public class SimpleAlignment extends Alignment<String>
{

//Attributes

	//The level of the alignment
	protected final String LEVEL = "0";
	
//Constructors

	/**
	 * Creates a new empty Alignment
	 */
	public SimpleAlignment()
	{
		super();
	}

	/**
	 * Creates a new empty Alignment between the source and target ontologies
	 */
	public SimpleAlignment(Ontology source, Ontology target)
	{
		super(source,target);
	}
	
//Public Methods

	/**
	 * Adds a new SimpleMapping to the Alignment if it is non-redundant
	 * Otherwise, updates the similarity of the already present Mapping
	 * to the maximum similarity of the two redundant Mappings
	 * @param entity1: the entity1 to add to the Alignment
	 * @param entity2: the entity2 to add to the Alignment
	 * @param sim: the similarity between the classes
	 */
	public boolean add(String entity1, String entity2, double sim)
	{
		return add(entity1,entity2,sim,MappingRelation.EQUIVALENCE,MappingStatus.UNKNOWN);
	}
	
	/**
	 * Adds a new SimpleMapping to the Alignment if it is non-redundant
	 * Otherwise, updates the similarity of the already present Mapping
	 * to the maximum similarity of the two redundant Mappings
	 * @param entity1: the entity1 to add to the Alignment
	 * @param entity2: the entity2 to add to the Alignment
	 * @param sim: the similarity between the classes
	 * @param r: the mapping relationship between the classes
	 */
	public boolean add(String entity1, String entity2, double sim, MappingRelation r)
	{
		return add(entity1,entity2,sim,r,MappingStatus.UNKNOWN);
	}
	
	/**
	 * Adds a new SimpleMapping to the Alignment if it is non-redundant
	 * Otherwise, updates the similarity of the already present Mapping
	 * to the maximum similarity of the two redundant Mappings
	 * @param entity1: the entity1 to add to the Alignment
	 * @param entity2: the entity2 to add to the Alignment
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
		SimpleMapping m = new SimpleMapping(entity1, entity2, sim, r);
		m.setStatus(s);
		return this.add(m);
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
	
	/**
	 * @param entity1: the entity1 to check in the Alignment
	 * @param entity2: the entity2 to check in the Alignment
	 * @return whether the Alignment contains a Mapping between entity1 and entity2
	 */
	public boolean contains(String entity1, String entity2)
	{
		return this.contains(new SimpleMapping(entity1,entity2,1.0));
	}
	
	/**
	 * @param entity1: the entity1 to check in the Alignment
 	 * @param entity2: the entity2 to check in the Alignment
 	 * @param r: the MappingRelation to check in the Alignment
	 * @return whether the Alignment contains a Mapping between entity1 and entity2
	 * with relationship r
	 */
	public boolean contains(String entity1, String entity2, MappingRelation r)
	{
		return this.contains(new SimpleMapping(entity1,entity2,1.0,r));
	}

	@Override
	public boolean contains(Object o)
	{
		return o instanceof SimpleMapping && super.contains(o);
	}
	
	/**
	 * @param entity1: the entity1 to check in the Alignment
 	 * @param entity2: the entity2 to check in the Alignment 
	 * @return whether the Alignment contains a Mapping that is ancestral to the given pair of classes
	 * (i.e. includes one ancestor of entity1 and one ancestor of entity2)
	 */
	public boolean containsAncestralMapping(String entity1, String entity2)
	{
		EntityMap rels = AML.getInstance().getEntityMap();
		
		Set<String> sourceAncestors = rels.getSuperclasses(entity1);
		Set<String> targetAncestors = rels.getSuperclasses(entity2);
		
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
	public boolean containsBetterMapping(SimpleMapping m)
	{
		if(!(m instanceof SimpleMapping))
			return false;
		if(this.containsSource(m.getEntity1()))
		{
			for(Mapping<String> n : sourceMaps.get(m.getEntity1()))
				if(n.getSimilarity() > m.getSimilarity())
					return true;
		}
		if(containsTarget(m.getEntity2()))
		{
			for(Mapping<String> n : targetMaps.get(m.getEntity2()))
				if(n.getSimilarity() > m.getSimilarity())
					return true;
		}
		return false;
	}
	
	/**
	 * @param entity1: the entity1 to check in the Alignment
 	 * @param entity2: the entity2 to check in the Alignment 
	 * @return whether the Alignment contains another Mapping that includes entity1 or entity2
	 */	
	public boolean containsConflict(String entity1, String entity2)
	{
		for(SimpleMapping m : getTargetMappings(entity2))
			if(!s.equals(entity1))
				return true;
		for(String t : getSourceMappings(entity1))
			if(!t.equals(entity2))
				return true;
		return false;
	}
	
	@Override
	public boolean containsConflict(Mapping<String> m)
	{
		if(m instanceof SimpleMapping)
			return containsConflict(m.getEntity1(),m.getEntity2());
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
		EntityMap rels = AML.getInstance().getEntityMap();
		
		Set<String> sourceDescendants = rels.getSubclasses(entity1);
		Set<String> targetDescendants = rels.getSubclasses(entity2);
		
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
	 * @param entity1: the entity1 to check in the Alignment
 	 * @param entity2: the entity2 to check in the Alignment 
	 * @return whether the Alignment contains a Mapping that is parent to the
	 * given pair of classes on one side only
	 */
	public boolean containsParentMapping(String entity1, String entity2)
	{
		EntityMap rels = AML.getInstance().getEntityMap();
		
		Set<String> sourceAncestors = rels.getSubclasses(entity1,1);
		Set<String> targetAncestors = rels.getSubclasses(entity2,1);
		
		for(String sa : sourceAncestors)
			if(contains(sa,entity2))
				return true;
		for(String ta : targetAncestors)
			if(contains(entity1,ta))
				return true;
		return false;
	}
	
	@Override
	public boolean containsSource(Object entity1)
	{
		return entity1 instanceof String && sourceMaps.contains((String)entity1);
	}

	@Override
	public boolean containsTarget(Object entity2)
	{
		return entity2 instanceof String && sourceMaps.contains((String)entity2);
	}
	
	@Override
	public boolean equals(Object o)
	{
		return o instanceof SimpleAlignment && containsAll((SimpleAlignment)o);
	}
	
	/**
	 * @param entity1: the entity1 to check in the Alignment
	 * @param entity2: the entity2 to check in the Alignment
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
	 * @param entity1: the entity1 to check in the Alignment
 	 * @return the entity2 that best matches entity1
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
	 * @param entity2: the entity2 to check in the Alignment
 	 * @return the entity1 that best matches entity2
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
		if(m instanceof SimpleMapping)
		{
			for(String t : sourceMaps.keySet((String)m.getEntity1()))
				if(t != (String)m.getEntity2())
					conflicts.add(sourceMaps.get((String)m.getEntity1(),t));
			for(String s : targetMaps.keySet((String)m.getEntity2()))
				if(s != (String)m.getEntity1())
					conflicts.add(sourceMaps.get(s,(String)m.getEntity2()));
		}
		return conflicts;
	}
	
	@Override
	public Set<EntityType> getEntityTypes()
	{
		HashSet<EntityType> types = new HashSet<EntityType>();
		for(Mapping m : maps)
		{
			types.addAll(AML.getInstance().getEntityMap().getTypes((String)m.getEntity1()));
			types.addAll(AML.getInstance().getEntityMap().getTypes((String)m.getEntity2()));
		}
		return types;
	}
	
	/**
	 * @return the high level Alignment induced from this Alignment
	 * (the similarity between high level classes is given by the
	 * fraction of classes in this Alignment that are their descendents)
	 */
	public SimpleAlignment getHighLevelAlignment()
	{
		EntityMap rels = AML.getInstance().getEntityMap();
		
		SimpleAlignment a = new SimpleAlignment();
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
		SimpleAlignment b = new SimpleAlignment();
		for(Mapping m : a)
			if(m.getSimilarity() >= 0.01)
				b.add(m);
		return b;
	}
	
	/**
	 * @param entity1: the entity1
	 * @param entity2: the entity2
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
	 * @param entity1: the entity1 to check in the Alignment
 	 * @return the entity2 that best matches entity1
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
	 * @param entity2: the entity2 to check in the Alignment
 	 * @return the entity1 that best matches entity2
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
	 * @param entity1: the entity1 in the Alignment
	 * @param entity2: the entity2 in the Alignment
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
	 * @param entity1: the entity1 in the Alignment
	 * @param entity2: the entity2 in the Alignment
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
	 * @param entity1: the entity1 in the Alignment
	 * @param entity2: the entity2 in the Alignment
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
	 * @param entity1: the entity1 to check in the Alignment
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
	 * @param entity2: the entity2 to check in the Alignment
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
	public SimpleAlignment intersection(Alignment a)
	{
		SimpleAlignment intersection = new SimpleAlignment();
		if(a instanceof SimpleAlignment)
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
}