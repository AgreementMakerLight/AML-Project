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

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import aml.AML;
import aml.alignment.edoal.AbstractExpression;
import aml.alignment.mapping.EDOALMapping;
import aml.alignment.mapping.Mapping;
import aml.alignment.mapping.MappingRelation;
import aml.alignment.mapping.MappingStatus;
import aml.ontology.EntityType;
import aml.ontology.Ontology;
import aml.util.data.Map2Map;

public class EDOALAlignment extends Alignment
{

//Attributes

	//The EDOAL Alignment Level
	public static final String LEVEL = "2EDOAL";
	//Simple mappings organized by entity1 (entity1, entity2, Mapping)
	private Map2Map<AbstractExpression,AbstractExpression,EDOALMapping> sourceMaps;
	//Simple mappings organized by entity2 (entity2, entity1, Mapping)
	private Map2Map<AbstractExpression,AbstractExpression,EDOALMapping> targetMaps;
	
//Constructors

	/**
	 * Creates a new empty Alignment
	 */
	public EDOALAlignment()
	{
		super();
		sourceMaps = new Map2Map<AbstractExpression,AbstractExpression,EDOALMapping>();
		targetMaps = new Map2Map<AbstractExpression,AbstractExpression,EDOALMapping>();
	}

	/**
	 * Creates a new empty Alignment between the source and target ontologies
	 */
	public EDOALAlignment(Ontology source, Ontology target)
	{
		super(source,target);
		sourceMaps = new Map2Map<AbstractExpression,AbstractExpression,EDOALMapping>();
		targetMaps = new Map2Map<AbstractExpression,AbstractExpression,EDOALMapping>();
	}

//Public Methods

	/**
	 * Adds a new EDOALMapping to the Alignment if it is non-redundant
	 * Otherwise, updates the similarity of the already present Mapping
	 * to the maximum similarity of the two redundant Mappings
	 * @param entity1: the entity1 to add to the Alignment
	 * @param entity2: the entity2 to add to the Alignment
	 * @param sim: the similarity between the classes
	 */
	public boolean add(AbstractExpression entity1, AbstractExpression entity2, double sim)
	{
		return add(entity1,entity2,sim,MappingRelation.EQUIVALENCE,MappingStatus.UNKNOWN);
	}
	
	/**
	 * Adds a new EDOALMapping to the Alignment if it is non-redundant
	 * Otherwise, updates the similarity of the already present Mapping
	 * to the maximum similarity of the two redundant Mappings
	 * @param entity1: the entity1 to add to the Alignment
	 * @param entity2: the entity2 to add to the Alignment
	 * @param sim: the similarity between the classes
	 * @param r: the mapping relationship between the classes
	 */
	public boolean add(AbstractExpression entity1, AbstractExpression entity2, double sim, MappingRelation r)
	{
		return add(entity1,entity2,sim,r,MappingStatus.UNKNOWN);
	}
	
	/**
	 * Adds a new EDOALMapping to the Alignment if it is non-redundant
	 * Otherwise, updates the similarity of the already present Mapping
	 * to the maximum similarity of the two redundant Mappings
	 * @param entity1: the entity1 to add to the Alignment
	 * @param entity2: the entity2 to add to the Alignment
	 * @param sim: the similarity between the classes
	 * @param r: the mapping relationship between the classes
	 * @param s: the mapping status
	 */
	public boolean add(AbstractExpression entity1, AbstractExpression entity2, double sim, MappingRelation r, MappingStatus s)
	{
		//We shouldn't have a mapping involving entities that exist in
		//both ontologies as they are the same entity, and therefore
		//shouldn't map with other entities in either ontology, unless
		//same URI matching is turned on
		if(!AML.getInstance().matchSameURI() && entity1.equals(entity2))
			return false;
		
		//Construct the Mapping
		EDOALMapping m = new EDOALMapping(entity1, entity2, sim, r);
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
		if(m instanceof EDOALMapping)
			return add((AbstractExpression)m.getEntity1(),(AbstractExpression)m.getEntity2(),m.getSimilarity(),m.getRelationship(),m.getStatus());
		else
			return false;
	}

	@Override
	public void clear()
	{
		super.clear();
		sourceMaps = new Map2Map<AbstractExpression,AbstractExpression,EDOALMapping>();
		targetMaps = new Map2Map<AbstractExpression,AbstractExpression,EDOALMapping>();		
	}

	/**
	 * @param entity1: the entity1 to check in the Alignment
	 * @param entity2: the entity2 to check in the Alignment
	 * @return whether the Alignment contains a Mapping between entity1 and entity2
	 */
	public boolean contains(AbstractExpression entity1, AbstractExpression entity2)
	{
		return sourceMaps.contains(entity1, entity2);
	}
	
	/**
	 * @param entity1: the entity1 to check in the Alignment
 	 * @param entity2: the entity2 to check in the Alignment
 	 * @param r: the MappingRelation to check in the Alignment
	 * @return whether the Alignment contains a Mapping between entity1 and entity2
	 * with relationship r
	 */
	public boolean contains(AbstractExpression entity1, AbstractExpression entity2, MappingRelation r)
	{
		return sourceMaps.contains(entity1, entity2) &&
				getRelationship(entity1,entity2).equals(r);
	}

	@Override
	public boolean contains(Object o)
	{
		return o instanceof EDOALMapping && contains((AbstractExpression)((EDOALMapping)o).getEntity1(),
				(AbstractExpression)((EDOALMapping)o).getEntity2(), ((EDOALMapping)o).getRelationship());
	}

	/**
 	 * @param m: the Mapping to check in the Alignment 
	 * @return whether the Alignment contains a Mapping that conflicts with the given
	 * Mapping and has a higher similarity
	 */
	public boolean containsBetterMapping(Mapping m)
	{
		if(!(m instanceof EDOALMapping))
			return false;
		//TODO: Revise this implementation; we probably want to compare elements rather than full entities
		AbstractExpression entity1 = (AbstractExpression)m.getEntity1();
		AbstractExpression entity2 = (AbstractExpression)m.getEntity2();
		double sim = m.getSimilarity();
		if(containsSource(entity1))
		{
			Set<AbstractExpression> targets = sourceMaps.keySet(entity1);
			for(AbstractExpression i : targets)
				if(getSimilarity(entity1,i) > sim)
					return true;
		}
		if(containsTarget(entity2))
		{
			Set<AbstractExpression> sources = targetMaps.keySet(entity2);
			for(AbstractExpression i : sources)
				if(getSimilarity(i,entity2) > sim)
					return true;
		}
		return false;
	}
	
	/**
	 * @param entity1: the entity1 to check in the Alignment
 	 * @param entity2: the entity2 to check in the Alignment 
	 * @return whether the Alignment contains another Mapping that includes entity1 or entity2
	 */	
	public boolean containsConflict(AbstractExpression entity1, AbstractExpression entity2)
	{
		//TODO: Revise this implementation; we probably want to compare elements rather than full entities
		for(AbstractExpression s : getTargetMappings((AbstractExpression)entity2))
			if(!s.equals(entity1))
				return true;
		for(AbstractExpression t : getSourceMappings((AbstractExpression)entity1))
			if(!t.equals(entity2))
				return true;
		return false;
	}
	
	@Override
	public boolean containsConflict(Mapping m)
	{
		if(m instanceof EDOALMapping)
			return containsConflict((AbstractExpression)m.getEntity1(),(AbstractExpression)m.getEntity2());
		return false;
	}
	
	@Override
	public boolean containsSource(Object entity1)
	{
		return entity1 instanceof AbstractExpression && sourceMaps.contains((AbstractExpression)entity1);
	}

	@Override
	public boolean containsTarget(Object entity2)
	{
		return entity2 instanceof AbstractExpression && sourceMaps.contains((AbstractExpression)entity2);
	}
	
	@Override
	public EDOALAlignment difference(Alignment a)
	{
		EDOALAlignment diff = new EDOALAlignment();
		if(a instanceof EDOALAlignment)
		{
			for(Mapping m : maps)
				if(!a.contains(m))
					diff.add(m);
		}
		else
			diff.addAll(maps);
		return diff;
	}
	
	@Override
	public boolean equals(Object o)
	{
		return o instanceof EDOALAlignment && containsAll((EDOALAlignment)o);
	}
	
	/**
	 * @param ref: the reference Alignment to evaluate this Alignment
	 * @return the evaluation of this Alignment {# correct mappings, # conflict mappings}
	 */
	public int[] evaluate(Alignment ref)
	{
		int[] count = new int[2];
		if(ref instanceof EDOALAlignment)
		{
			for(Mapping m : maps)
			{
				if(ref.contains(m))
				{
					count[0]++;
					m.setStatus(MappingStatus.CORRECT);
				}
				else if(((EDOALAlignment)ref).contains((AbstractExpression)m.getEntity1(),(AbstractExpression)m.getEntity2(),MappingRelation.UNKNOWN))
				{
					count[1]++;
					m.setStatus(MappingStatus.UNKNOWN);
				}
				else
					m.setStatus(MappingStatus.INCORRECT);
			}
		}
		return count;
	}
	
	/**
	 * @param a: the base Alignment to which this Alignment will be compared 
	 * @return the gain (i.e. the fraction of new Mappings) of this Alignment
	 * in comparison with the base Alignment
	 */
	public double gainOneToOne(Alignment a)
	{
		double sourceGain = 0.0;
		Set<AbstractExpression> sources = sourceMaps.keySet();
		for(AbstractExpression i : sources)
			if(!a.containsSource(i))
				sourceGain++;
		sourceGain /= a.sourceCount();
		double targetGain = 0.0;
		Set<AbstractExpression> targets = targetMaps.keySet();
		for(AbstractExpression i : targets)
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
	 * @param entity1: the entity1 to check in the Alignment
	 * @param entity2: the entity2 to check in the Alignment
 	 * @return the Mapping between the entity1 and entity2 classes or null if no
 	 * such Mapping exists
	 */
	public Mapping get(AbstractExpression entity1, AbstractExpression entity2)
	{
		return sourceMaps.get(entity1, entity2);
	}
	
	/**
	 * @param id1: the uri of the first class to check in the Alignment
	 * @param entity2: the uri of the second class to check in the Alignment
 	 * @return the Mapping between the classes or null if no such Mapping exists
 	 * in either direction
	 */
	public Mapping getBidirectional(AbstractExpression uri1, AbstractExpression uri2)
	{
		if(sourceMaps.contains(uri1, uri2))
			return sourceMaps.get(uri1, uri2);
		else if(sourceMaps.contains(uri2, uri1))
			return  sourceMaps.get(uri2, uri1);
		else
			return null;
	}
	
	/**
	 * @param m: the Mapping to check on the Alignment
	 * @return the list of all Mappings that have a cardinality conflict with the given Mapping
	 */
	public Vector<Mapping> getConflicts(Mapping m)
	{
		Vector<Mapping> conflicts = new Vector<Mapping>();
		for(AbstractExpression t : sourceMaps.keySet((AbstractExpression)m.getEntity1()))
			if(t != (AbstractExpression)m.getEntity2())
				conflicts.add(sourceMaps.get((AbstractExpression)m.getEntity1(),t));
		for(AbstractExpression s : targetMaps.keySet((AbstractExpression)m.getEntity2()))
			if(s != (AbstractExpression)m.getEntity1())
				conflicts.add(sourceMaps.get(s,(AbstractExpression)m.getEntity2()));
		return conflicts;
	}
	
	/**
	 * @param entity1: the entity1
	 * @param entity2: the entity2
	 * @return the uri of the Mapping between the given classes in
	 * the list of Mappings, or -1 if the Mapping doesn't exist
	 */
	public int getIndex(AbstractExpression entity1, AbstractExpression entity2)
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
	public int getIndexBidirectional(AbstractExpression uri1, AbstractExpression uri2)
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
	public Set<AbstractExpression> getMappingsBidirectional(AbstractExpression uri)
	{
		HashSet<AbstractExpression> mappings = new HashSet<AbstractExpression>();
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
	public double getMaxSourceSim(AbstractExpression entity1)
	{
		double max = 0;
		Set<AbstractExpression> targets = sourceMaps.keySet(entity1);
		for(AbstractExpression i : targets)
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
	public double getMaxTargetSim(AbstractExpression entity2)
	{
		double max = 0;
		Set<AbstractExpression> sources = targetMaps.keySet(entity2);
		for(AbstractExpression i : sources)
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
	public MappingRelation getRelationship(AbstractExpression entity1, AbstractExpression entity2)
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
	public double getSimilarity(AbstractExpression entity1, AbstractExpression entity2)
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
	public String getSimilarityPercent(AbstractExpression entity1, AbstractExpression entity2)
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
	public Set<AbstractExpression> getSourceMappings(AbstractExpression entity1)
	{
		if(sourceMaps.contains(entity1))
			return sourceMaps.keySet(entity1);
		return new HashSet<AbstractExpression>();
	}
	
	/**
 	 * @return the list of all entity1 classes that have mappings
	 */
	public Set<AbstractExpression> getSources()
	{
		HashSet<AbstractExpression> sMaps = new HashSet<AbstractExpression>();
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
	public Set<AbstractExpression> getTargetMappings(AbstractExpression entity2)
	{
		if(targetMaps.contains(entity2))
			return targetMaps.keySet(entity2);
		return new HashSet<AbstractExpression>();
	}
	
	/**
 	 * @return the list of all entity2 classes that have mappings
	 */
	public Set<AbstractExpression> getTargets()
	{
		HashSet<AbstractExpression> tMaps = new HashSet<AbstractExpression>();
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
	public EDOALAlignment intersection(Alignment a)
	{
		EDOALAlignment intersection = new EDOALAlignment();
		if(a instanceof EDOALAlignment)
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
		
		Set<AbstractExpression> sources = sourceMaps.keySet();
		for(AbstractExpression i : sources)
		{
			cardinality = sourceMaps.keySet(i).size();
			if(cardinality > max)
				max = cardinality;
		}
		Set<AbstractExpression> targets = targetMaps.keySet();
		for(AbstractExpression i : targets)
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
		if(o instanceof EDOALMapping && contains(o))
		{
			Mapping m = (Mapping)o;
			AbstractExpression entity1 = (AbstractExpression)m.getEntity1();
			AbstractExpression entity2 = (AbstractExpression)m.getEntity2();
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
	public boolean remove(AbstractExpression entity1, AbstractExpression entity2)
	{
		Mapping m = new EDOALMapping(entity1, entity2, 1.0);
		return remove(m);
	}
	
	@Override
	public int sourceCount()
	{
		return sourceMaps.keyCount();
	}
	
	@Override
	public double sourceCoverage(EntityType e)
	{
		double coverage = 0.0;
		HashSet<String> src = new HashSet<String>();
		for(AbstractExpression i : sourceMaps.keySet())
			src.addAll(i.getElements());
		for(String s : src)
			if(AML.getInstance().getEntityMap().getTypes(s).contains(e))
				coverage++;
		int count;
		if(e.equals(EntityType.INDIVIDUAL))
			count = AML.getInstance().getSourceIndividualsToMatch().size();
		else
			count = AML.getInstance().getSource().count(e);
		return coverage / count;
	}
	
	@Override
	public int targetCount()
	{
		return targetMaps.keyCount();
	}
	
	@Override
	public double targetCoverage(EntityType e)
	{
		double coverage = 0.0;
		HashSet<String> tgt = new HashSet<String>();
		for(AbstractExpression i : targetMaps.keySet())
			tgt.addAll(i.getElements());
		for(String t : tgt)
			if(AML.getInstance().getEntityMap().getTypes(t).contains(e))
				coverage++;
		int count;
		if(e.equals(EntityType.INDIVIDUAL))
			count = AML.getInstance().getTargetIndividualsToMatch().size();
		else
			count = AML.getInstance().getTarget().count(e);
		return coverage / count;
	}

	@Override
	public double sourceCoverage() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double targetCoverage() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Set<EntityType> getEntityTypes() {
		// TODO Auto-generated method stub
		return null;
	}
}