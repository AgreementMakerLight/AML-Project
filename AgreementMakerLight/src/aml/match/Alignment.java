/******************************************************************************
* Copyright 2013-2016 LASIGE                                                  *
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
package aml.match;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.lang.StringEscapeUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import aml.AML;
import aml.ontology.Ontology2Match;
import aml.ontology.RelationshipMap;
import aml.ontology.URIMap;
import aml.settings.MappingRelation;
import aml.settings.MappingStatus;
import aml.util.Table2Map;

public class Alignment implements Collection<Mapping>
{

//Attributes

	//Term mappings organized in list
	private Vector<Mapping> maps;
	//Term mappings organized by source class (Source Id, Target Id, Mapping)
	private Table2Map<Integer,Integer,Mapping> sourceMaps;
	//Term mappings organized by target class (Target Id, Source Id, Mapping)
	private Table2Map<Integer,Integer,Mapping> targetMaps;
	//Whether the Alignment is internal
	private boolean internal;
	//Link to AML and the Ontologies
	private AML aml;
	private Ontology2Match source;
	private Ontology2Match target;
	//Link to the URIMap
	private URIMap uris;
	
//Constructors

	/**
	 * Creates a new empty Alignment
	 */
	public Alignment()
	{
		maps = new Vector<Mapping>(0,1);
		sourceMaps = new Table2Map<Integer,Integer,Mapping>();
		targetMaps = new Table2Map<Integer,Integer,Mapping>();
		aml = AML.getInstance();
		source = aml.getSource();
		target = aml.getTarget();
		uris = aml.getURIMap();
		internal = false;
	}

	/**
	 * Creates a new empty Alignment
	 */
	public Alignment(boolean internal)
	{
		maps = new Vector<Mapping>(0,1);
		sourceMaps = new Table2Map<Integer,Integer,Mapping>();
		targetMaps = new Table2Map<Integer,Integer,Mapping>();
		aml = AML.getInstance();
		if(!internal)
			source = aml.getSource();
			target = aml.getTarget();
		uris = aml.getURIMap();
		this.internal = internal;
	}

	/**
	 * Reads an Alignment from an input file
	 * @param file: the path to the input file
	 */
	public Alignment(String file) throws Exception
	{
		this();
		if(file.endsWith(".rdf"))
			loadMappingsRDF(file);
		else if(file.endsWith(".tsv"))
			loadMappingsTSV(file);
		else
			throw new Exception("Unrecognized alignment format!");
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
	 * @param sourceId: the index of the source class to add to the Alignment
	 * @param targetId: the index of the target class to add to the Alignment
	 * @param sim: the similarity between the classes
	 */
	public void add(int sourceId, int targetId, double sim)
	{
		add(sourceId,targetId,sim,MappingRelation.EQUIVALENCE);
	}
	
	/**
	 * Adds a new Mapping to the Alignment if it is non-redundant
	 * Otherwise, updates the similarity of the already present Mapping
	 * to the maximum similarity of the two redundant Mappings
	 * @param sourceId: the index of the source class to add to the Alignment
	 * @param targetId: the index of the target class to add to the Alignment
	 * @param sim: the similarity between the classes
	 * @param r: the mapping relationship between the classes
	 */
	public boolean add(int sourceId, int targetId, double sim, MappingRelation r)
	{
		//Unless the Alignment is internal, we can't have a mapping
		//involving entities that exist in both ontologies (they are
		//the same entity, and therefore shouldn't map with other
		//entities in either ontology)
		if(!internal && (source.contains(targetId) || target.contains(sourceId)))
			return false;
		
		//Construct the Mapping
		Mapping m = new Mapping(sourceId, targetId, sim, r);
		//If it isn't listed yet, add it
		if(!sourceMaps.contains(sourceId,targetId))
		{
			maps.add(m);
			sourceMaps.add(sourceId, targetId, m);
			targetMaps.add(targetId, sourceId, m);
			return true;
		}
		//Otherwise update the similarity
		else
		{
			m = sourceMaps.get(sourceId,targetId);
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
			return check;
		}
	}
	
	/**
	 * Adds a new Mapping to the Alignment if it is non-redundant
	 * Otherwise, updates the similarity of the already present Mapping
	 * to the maximum similarity of the two redundant Mappings
	 * @param sourceId: the index of the source class to add to the Alignment
	 * @param targetId: the index of the target class to add to the Alignment
	 * @param sim: the similarity between the classes
	 * @param r: the mapping relationship between the classes
	 * @param s: the mapping status
	 */
	public boolean add(int sourceId, int targetId, double sim, MappingRelation r, MappingStatus s)
	{
		//Unless the Alignment is internal, we can't have a mapping
		//involving entities that exist in both ontologies (they are
		//the same entity, and therefore shouldn't map with other
		//entities in either ontology)
		if(!internal && (source.contains(targetId) || target.contains(sourceId)))
			return false;
		
		//Construct the Mapping
		Mapping m = new Mapping(sourceId, targetId, sim, r);
		m.setStatus(s);
		//If it isn't listed yet, add it
		if(!sourceMaps.contains(sourceId,targetId))
		{
			maps.add(m);
			sourceMaps.add(sourceId, targetId, m);
			targetMaps.add(targetId, sourceId, m);
			return true;
		}
		//Otherwise update the similarity
		else
		{
			m = sourceMaps.get(sourceId,targetId);
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
	
	/**
	 * Adds a new Mapping to the Alignment if it is non-redundant
	 * Otherwise, updates the similarity of the already present Mapping
	 * to the maximum similarity of the two redundant Mappings
	 * @param sourceURI: the URI of the source class to add to the Alignment
	 * @param targetURI: the URI of the target class to add to the Alignment
	 * @param sim: the similarity between the classes
	 */
	public boolean add(String sourceURI, String targetURI, double sim)
	{
		return add(sourceURI,targetURI,sim,MappingRelation.EQUIVALENCE,MappingStatus.UNKNOWN);
	}
	
	/**
	 * Adds a new Mapping to the Alignment if it is non-redundant
	 * Otherwise, updates the similarity of the already present Mapping
	 * to the maximum similarity of the two redundant Mappings
	 * @param sourceURI: the URI of the source class to add to the Alignment
	 * @param targetURI: the URI of the target class to add to the Alignment
	 * @param sim: the similarity between the classes
	 * @param r: the mapping relationship between the classes
	 * @param s: the mapping status
	 */
	public boolean add(String sourceURI, String targetURI, double sim, MappingRelation r, MappingStatus s)
	{
		int id1 = uris.getIndex(sourceURI);
		int id2 = uris.getIndex(targetURI);
		if(id1 == -1 || id2 == -1)
			return false;
		if(aml.getSource().contains(id1) && aml.getTarget().contains(id2))
			return add(id1,id2,sim,r,s);
		else if(aml.getSource().contains(id2) && aml.getTarget().contains(id1))
			return add(id2,id1,sim,r,s);
		return false;
	}
	
	@Override
	public boolean add(Mapping m)
	{
		int sourceId = m.getSourceId();
		int targetId = m.getTargetId();
		double sim = m.getSimilarity();
		MappingRelation r = m.getRelationship();
		Mapping clone = new Mapping(m);
		//Unless the Alignment is internal, we can't have a mapping
		//involving entities that exist in both ontologies (they are
		//the same entity, and therefore shouldn't map with other
		//entities in either ontology)
		if(!internal && (source.contains(targetId) || target.contains(sourceId)))
			return false;
		
		//If it isn't listed yet, add it
		if(!sourceMaps.contains(sourceId,targetId))
		{
			maps.add(clone);
			sourceMaps.add(sourceId, targetId, clone);
			targetMaps.add(targetId, sourceId, clone);
			return true;
		}
		//Otherwise update the similarity
		else
		{
			m = sourceMaps.get(sourceId,targetId);
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
			return check;
		}
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
		
		Set<Integer> sources = sourceMaps.keySet();
		for(Integer i : sources)
			cardinality += sourceMaps.keySet(i).size();
		
		Set<Integer> targets = targetMaps.keySet();
		for(Integer i : targets)
			cardinality += targetMaps.keySet(i).size();
		cardinality /= sources.size() + targets.size();
		
		return cardinality;		
	}
	
	/**
	 * @param id: the index of the entity to check in the Alignment
	 * @return the cardinality of the entity in the Alignment
	 */
	public int cardinality(int id)
	{
		if(sourceMaps.contains(id))
			return sourceMaps.get(id).size();
		if(targetMaps.contains(id))
			return targetMaps.get(id).size();
		return 0;
	}
	
	@Override
	public void clear()
	{
		maps = new Vector<Mapping>(0,1);
		sourceMaps = new Table2Map<Integer,Integer,Mapping>();
		targetMaps = new Table2Map<Integer,Integer,Mapping>();		
	}
	
	/**
	 * @param sourceId: the index of the source class to check in the Alignment
 	 * @param targetId: the index of the target class to check in the Alignment
 	 * @param r: the MappingRelation to check in the Alignment
	 * @return whether the Alignment contains a Mapping between sourceId and targetId
	 * with relationship r
	 */
	public boolean contains(int sourceId, int targetId, MappingRelation r)
	{
		return sourceMaps.contains(sourceId, targetId) &&
				getRelationship(sourceId,targetId).equals(r);
	}

	@Override
	public boolean contains(Object o)
	{
		return o instanceof Mapping && contains(((Mapping)o).getSourceId(),
				((Mapping)o).getTargetId(), ((Mapping)o).getRelationship());
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
	 * @param sourceId: the index of the source class to check in the Alignment
 	 * @param targetId: the index of the target class to check in the Alignment 
	 * @return whether the Alignment contains a Mapping that is ancestral to the given pair of classes
	 * (i.e. includes one ancestor of sourceId and one ancestor of targetId)
	 */
	public boolean containsAncestralMapping(int sourceId, int targetId)
	{
		AML aml = AML.getInstance();
		RelationshipMap rels = aml.getRelationshipMap();
		
		Set<Integer> sourceAncestors = rels.getAncestors(sourceId);
		Set<Integer> targetAncestors = rels.getAncestors(targetId);
		
		for(Integer sa : sourceAncestors)
		{
			Set<Integer> over = getSourceMappings(sa);
			for(Integer ta : targetAncestors)
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
		int source = m.getSourceId();
		int target = m.getTargetId();
		double sim = m.getSimilarity();
		
		if(containsSource(source))
		{
			Set<Integer> targets = sourceMaps.keySet(source);
			for(Integer i : targets)
				if(getSimilarity(source,i) > sim)
					return true;
		}
		if(containsTarget(target))
		{
			Set<Integer> sources = targetMaps.keySet(target);
			for(Integer i : sources)
				if(getSimilarity(i,target) > sim)
					return true;
		}
		return false;
	}
	
	/**
 	 * @param classId: the index of the class to check in the Alignment 
	 * @return whether the Alignment contains a Mapping with that class
	 * (either as a source or as a target class)
	 */
	public boolean containsClass(int classId)
	{
		return containsSource(classId) || containsTarget(classId);
	}
	
	/**
	 * @param sourceId: the index of the source class to check in the Alignment
 	 * @param targetId: the index of the target class to check in the Alignment 
	 * @return whether the Alignment contains another Mapping for sourceId or for targetId
	 */
	public boolean containsConflict(int sourceId, int targetId)
	{
		for(int s : getTargetMappings(targetId))
			if(s != sourceId)
				return true;
		for(int t : getSourceMappings(sourceId))
			if(t != targetId)
				return true;
		return false;
	}
	
	/**
 	 * @param m: the Mapping to check in the Alignment 
	 * @return whether the Alignment contains another Mapping involving either class in m
	 */
	public boolean containsConflict(Mapping m)
	{
		return containsConflict(m.getSourceId(),m.getTargetId());
	}
	
	/**
	 * @param sourceId: the index of the source class to check in the Alignment
 	 * @param targetId: the index of the target class to check in the Alignment 
	 * @return whether the Alignment contains a Mapping that is descendant of the given pair of classes
	 * (i.e. includes one descendant of sourceId and one descendant of targetId)
	 */
	public boolean containsDescendantMapping(int sourceId, int targetId)
	{
		AML aml = AML.getInstance();
		RelationshipMap rels = aml.getRelationshipMap();
		
		Set<Integer> sourceDescendants = rels.getDescendants(sourceId);
		Set<Integer> targetDescendants = rels.getDescendants(targetId);
		
		for(Integer sa : sourceDescendants)
		{
			Set<Integer> over = getSourceMappings(sa);
			for(Integer ta : targetDescendants)
				if(over.contains(ta))
					return true;
		}
		return false;
	}
	
	/**
	 * @param sourceId: the index of the source class to check in the Alignment
	 * @param targetId: the index of the target class to check in the Alignment
	 * @return whether the Alignment contains a Mapping between sourceId and targetId
	 */
	public boolean containsMapping(int sourceId, int targetId)
	{
		return sourceMaps.contains(sourceId, targetId);
	}
	
	/**
	 * @param m: the Mapping to check in the Alignment
	 * @return whether the Alignment contains a Mapping with the same sourceId
	 * and targetId as m (regardless of the mapping relation)
	 */
	public boolean containsMapping(Mapping m)
	{
		return sourceMaps.contains(m.getSourceId(), m.getTargetId());
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
	 * @param sourceId: the index of the source class to check in the Alignment
 	 * @param targetId: the index of the target class to check in the Alignment 
	 * @return whether the Alignment contains a Mapping that is parent to the
	 * given pair of classes on one side only
	 */
	public boolean containsParentMapping(int sourceId, int targetId)
	{
		AML aml = AML.getInstance();
		RelationshipMap rels = aml.getRelationshipMap();
		
		Set<Integer> sourceAncestors = rels.getParents(sourceId);
		Set<Integer> targetAncestors = rels.getParents(targetId);
		
		for(Integer sa : sourceAncestors)
			if(containsMapping(sa,targetId))
				return true;
		for(Integer ta : targetAncestors)
			if(containsMapping(sourceId,ta))
				return true;
		return false;
	}
	
	/**
	 * @param sourceId: the index of the source class to check in the Alignment
 	 * @return whether the Alignment contains a Mapping for sourceId
	 */
	public boolean containsSource(int sourceId)
	{
		return sourceMaps.contains(sourceId);
	}

	/**
	 * @param targetId: the index of the target class to check in the Alignment
 	 * @return whether the Alignment contains a Mapping for targetId
	 */
	public boolean containsTarget(int targetId)
	{
		return targetMaps.contains(targetId);
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
			else if(ref.contains(m.getSourceId(),m.getTargetId(),MappingRelation.UNKNOWN))
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
		Set<Integer> sources = sourceMaps.keySet();
		for(Integer i : sources)
			if(!a.containsSource(i))
				sourceGain++;
		sourceGain /= a.sourceCount();
		double targetGain = 0.0;
		Set<Integer> targets = targetMaps.keySet();
		for(Integer i : targets)
			if(!a.containsTarget(i))
				targetGain++;
		targetGain /= a.targetCount();
		return Math.min(sourceGain, targetGain);
	}
	
	/**
	 * @param index: the index of the Mapping to return in the list of Mappings
 	 * @return the Mapping at the input index (note that the index will change
 	 * during sorting) or null if the index falls outside the list
	 */
	public Mapping get(int index)
	{
		if(index < 0 || index >= maps.size())
			return null;
		return maps.get(index);
	}
	
	/**
	 * @param sourceId: the index of the source class to check in the Alignment
	 * @param targetId: the index of the target class to check in the Alignment
 	 * @return the Mapping between the source and target classes or null if no
 	 * such Mapping exists
	 */
	public Mapping get(int sourceId, int targetId)
	{
		return sourceMaps.get(sourceId, targetId);
	}
	
	/**
	 * @param id1: the index of the first class to check in the Alignment
	 * @param targetId: the index of the second class to check in the Alignment
 	 * @return the Mapping between the classes or null if no such Mapping exists
 	 * in either direction
	 */
	public Mapping getBidirectional(int id1, int id2)
	{
		if(sourceMaps.contains(id1, id2))
			return sourceMaps.get(id1, id2);
		else if(sourceMaps.contains(id2, id1))
			return  sourceMaps.get(id2, id1);
		else
			return null;
	}
	
	/**
	 * @param sourceId: the index of the source class to check in the Alignment
 	 * @return the index of the target class that best matches source
	 */
	public int getBestSourceMatch(int sourceId)
	{
		double max = 0;
		int target = -1;
		Set<Integer> targets = sourceMaps.keySet(sourceId);
		for(Integer i : targets)
		{
			double sim = getSimilarity(sourceId,i);
			if(sim > max)
			{
				max = sim;
				target = i;
			}
		}
		return target;
	}

	/**
	 * @param targetId: the index of the target class to check in the Alignment
 	 * @return the index of the source class that best matches target
	 */
	public int getBestTargetMatch(int targetId)
	{
		double max = 0;
		int source = -1;
		Set<Integer> sources = sourceMaps.keySet(targetId);
		for(Integer i : sources)
		{
			double sim = getSimilarity(i,targetId);
			if(sim > max)
			{
				max = sim;
				source = i;
			}
		}
		return source;
	}
	
	/**
	 * @param m: the Mapping to check on the Alignment
	 * @return the list of all Mappings that have a cardinality conflict with the given Mapping
	 */
	public Vector<Mapping> getConflicts(Mapping m)
	{
		Vector<Mapping> conflicts = new Vector<Mapping>();
		for(Integer t : sourceMaps.keySet(m.getSourceId()))
			if(t != m.getTargetId())
				conflicts.add(sourceMaps.get(m.getSourceId(),t));
		for(Integer s : targetMaps.keySet(m.getTargetId()))
			if(s != m.getSourceId())
				conflicts.add(sourceMaps.get(s,m.getTargetId()));
		return conflicts;
	}
	
	/**
	 * @return the high level Alignment induced from this Alignment
	 * (the similarity between high level classes is given by the
	 * fraction of classes in this Alignment that are their descendents)
	 */
	public Alignment getHighLevelAlignment()
	{
		AML aml = AML.getInstance();
		RelationshipMap rels = aml.getRelationshipMap();
		
		Alignment a = new Alignment();
		int total = maps.size();
		for(Mapping m : maps)
		{
			Set<Integer> sourceAncestors = rels.getHighLevelAncestors(m.getSourceId());
			Set<Integer> targetAncestors = rels.getHighLevelAncestors(m.getTargetId());
			for(int i : sourceAncestors)
			{
				for(int j : targetAncestors)
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
	 * @param sourceId: the index of the source class
	 * @param targetId: the index of the target class
	 * @return the index of the Mapping between the given classes in
	 * the list of Mappings, or -1 if the Mapping doesn't exist
	 */
	public int getIndex(int sourceId, int targetId)
	{
		if(sourceMaps.contains(sourceId, targetId))
			return maps.indexOf(sourceMaps.get(sourceId, targetId));
		else
			return -1;
	}
	
	/**
	 * @param id1: the index of the first class
	 * @param id2: the index of the second class
	 * @return the index of the Mapping between the given classes in
	 * the list of Mappings (in any order), or -1 if the Mapping doesn't exist
	 */
	public int getIndexBidirectional(int id1, int id2)
	{
		if(sourceMaps.contains(id1, id2))
			return maps.indexOf(sourceMaps.get(id1, id2));
		else if(targetMaps.contains(id1, id2))
			return maps.indexOf(targetMaps.get(id1, id2));
		else
			return -1;
	}
	
	/**
	 * @param id: the index of the class to check in the Alignment
 	 * @return the list of all classes mapped to the given class
	 */
	public Set<Integer> getMappingsBidirectional(int id)
	{
		HashSet<Integer> mappings = new HashSet<Integer>();
		if(sourceMaps.contains(id))
			mappings.addAll(sourceMaps.keySet(id));
		if(targetMaps.contains(id))
			mappings.addAll(targetMaps.keySet(id));
		return mappings;
	}
	
	/**
	 * @param sourceId: the index of the source class to check in the Alignment
 	 * @return the index of the target class that best matches source
	 */
	public double getMaxSourceSim(int sourceId)
	{
		double max = 0;
		Set<Integer> targets = sourceMaps.keySet(sourceId);
		for(Integer i : targets)
		{
			double sim = getSimilarity(sourceId,i);
			if(sim > max)
				max = sim;
		}
		return max;
	}

	/**
	 * @param targetId: the index of the target class to check in the Alignment
 	 * @return the index of the source class that best matches target
	 */
	public double getMaxTargetSim(int targetId)
	{
		double max = 0;
		Set<Integer> sources = targetMaps.keySet(targetId);
		for(Integer i : sources)
		{
			double sim = getSimilarity(i,targetId);
			if(sim > max)
				max = sim;
		}
		return max;
	}
	
	/**
	 * @param sourceId: the index of the source class in the Alignment
	 * @param targetId: the index of the target class in the Alignment
	 * @return the mapping relationship between source and target
	 */
	public MappingRelation getRelationship(int sourceId, int targetId)
	{
		Mapping m = sourceMaps.get(sourceId, targetId);
		if(m == null)
			return null;
		return m.getRelationship();
	}
	
	/**
	 * @param sourceId: the index of the source class in the Alignment
	 * @param targetId: the index of the target class in the Alignment
	 * @return the similarity between source and target
	 */
	public double getSimilarity(int sourceId, int targetId)
	{
		Mapping m = sourceMaps.get(sourceId, targetId);
		if(m == null)
			return 0.0;
		return m.getSimilarity();
	}
	
	/**
	 * @param sourceId: the index of the source class in the Alignment
	 * @param targetId: the index of the target class in the Alignment
	 * @return the similarity between source and target in percentage
	 */
	public String getSimilarityPercent(int sourceId, int targetId)
	{
		Mapping m = sourceMaps.get(sourceId, targetId);
		if(m == null)
			return "0%";
		return m.getSimilarityPercent();
	}
	
	/**
	 * @param sourceId: the index of the source class to check in the Alignment
 	 * @return the list of all target classes mapped to the source class
	 */
	public Set<Integer> getSourceMappings(int sourceId)
	{
		if(sourceMaps.contains(sourceId))
			return sourceMaps.keySet(sourceId);
		return new HashSet<Integer>();
	}
	
	/**
 	 * @return the list of all source classes that have mappings
	 */
	public Set<Integer> getSources()
	{
		HashSet<Integer> sMaps = new HashSet<Integer>();
		sMaps.addAll(sourceMaps.keySet());
		return sMaps;
	}
	
	/**
	 * @param targetId: the index of the target class to check in the Alignment
 	 * @return the list of all source classes mapped to the target class
	 */
	public Set<Integer> getTargetMappings(int targetId)
	{
		if(targetMaps.contains(targetId))
			return targetMaps.keySet(targetId);
		return new HashSet<Integer>();
	}
	
	/**
 	 * @return the list of all target classes that have mappings
	 */
	public Set<Integer> getTargets()
	{
		HashSet<Integer> tMaps = new HashSet<Integer>();
		tMaps.addAll(targetMaps.keySet());
		return tMaps;
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
		
		Set<Integer> sources = sourceMaps.keySet();
		for(Integer i : sources)
		{
			cardinality = sourceMaps.keySet(i).size();
			if(cardinality > max)
				max = cardinality;
		}
		Set<Integer> targets = targetMaps.keySet();
		for(Integer i : targets)
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
		if(o instanceof Mapping && contains(o))
		{
			Mapping m = (Mapping)o;
			int sourceId = m.getSourceId();
			int targetId = m.getTargetId();
			sourceMaps.remove(sourceId, targetId);
			targetMaps.remove(targetId, sourceId);
			maps.remove(m);
			return true;
		}
		else
			return false;
	}
	
	/**
	 * Removes the Mapping between the given classes from the Alignment
	 * @param sourceId: the source class to remove from the Alignment
	 * @param targetId: the target class to remove from the Alignment
	 */
	public boolean remove(int sourceId, int targetId)
	{
		Mapping m = new Mapping(sourceId, targetId, 1.0);
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

	/**
	 * Saves the Alignment into an .rdf file in OAEI format
	 * @param file: the output file
	 */
	public void saveRDF(String file) throws FileNotFoundException
	{
		AML aml = AML.getInstance();
		String sourceURI = aml.getSource().getURI();
		String targetURI = aml.getTarget().getURI();
		
		PrintWriter outStream = new PrintWriter(new FileOutputStream(file));
		outStream.println("<?xml version='1.0' encoding='utf-8'?>");
		outStream.println("<rdf:RDF xmlns='http://knowledgeweb.semanticweb.org/heterogeneity/alignment'"); 
		outStream.println("\t xmlns:rdf='http://www.w3.org/1999/02/22-rdf-syntax-ns#' "); 
		outStream.println("\t xmlns:xsd='http://www.w3.org/2001/XMLSchema#' ");
		outStream.println("\t alignmentSource='AgreementMakerLight'>\n");
		outStream.println("<Alignment>");
		outStream.println("\t<xml>yes</xml>");
		outStream.println("\t<level>0</level>");
		double card = cardinality();
		if(card < 1.02)
			outStream.println("\t<type>11</type>");
		else
			outStream.println("\t<type>??</type>");
		outStream.println("\t<onto1>" + sourceURI + "</onto1>");
		outStream.println("\t<onto2>" + targetURI + "</onto2>");
		outStream.println("\t<uri1>" + sourceURI + "</uri1>");
		outStream.println("\t<uri2>" + targetURI + "</uri2>");
		for(Mapping m : maps)
			outStream.println(m.toRDF());
		outStream.println("</Alignment>");
		outStream.println("</rdf:RDF>");		
		outStream.close();
	}
	
	/**
	 * Saves the Alignment into a .tsv file in AML format
	 * @param file: the output file
	 */
	public void saveTSV(String file) throws FileNotFoundException
	{
		PrintWriter outStream = new PrintWriter(new FileOutputStream(file));
		outStream.println("#AgreementMakerLight Alignment File");
		outStream.println("#Source ontology:\t" + source.getURI());
		outStream.println("#Target ontology:\t" + target.getURI());
		outStream.println("Source URI\tSource Label\tTarget URI\tTarget Label\tSimilarity\tRelationship\tStatus");
		for(Mapping m : maps)
			outStream.println(m.toString());
		outStream.close();
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
	 * @return the number of source classes mapped in this Alignment
	 */
	public int sourceCount()
	{
		return sourceMaps.keyCount();
	}
	
	/**
	 * @return the fraction of source classes mapped in this Alignment
	 */
	public double sourceCoverage()
	{
		AML aml = AML.getInstance();
		double coverage = sourceMaps.keyCount();
		int count = aml.getSource().classCount();
		coverage /= count;
		return coverage;
	}
	
	/**
	 * @return the number of target classes mapped in this Alignment
	 */
	public int targetCount()
	{
		return targetMaps.keyCount();
	}
	
	/**
	 * @return the fraction of target classes mapped in this Alignment
	 */
	public double targetCoverage()
	{
		AML aml = AML.getInstance();
		double coverage = targetMaps.keyCount();
		int count = aml.getTarget().classCount();
		coverage /= count;
		return coverage;
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
	
//Private Methods

	private void loadMappingsRDF(String file) throws DocumentException
	{
		//Open the Alignment file using SAXReader
		SAXReader reader = new SAXReader();
		File f = new File(file);

		Document doc = reader.read(f);
		//Read the root, then go to the "Alignment" element
		Element root = doc.getRootElement();
		Element align = root.element("Alignment");
		//Get an iterator over the mappings
		Iterator<?> map = align.elementIterator("map");
		while(map.hasNext())
		{
			//Get the "Cell" in each mapping
			Element e = ((Element)map.next()).element("Cell");
			if(e == null)
				continue;
			//Get the source class
			String sourceURI = e.element("entity1").attributeValue("resource");
			//Get the target class
			String targetURI = e.element("entity2").attributeValue("resource");
			//Get the similarity measure
			String measure = e.elementText("measure");
			//Parse it, assuming 1 if a valid measure is not found
			double similarity = 1;
			if(measure != null)
			{
				try
				{
					similarity = Double.parseDouble(measure);
		            if(similarity < 0 || similarity > 1)
		            	similarity = 1;
				}
            	catch(Exception ex){/*Do nothing - use the default value*/};
            }
            //Get the relation
            String r = e.elementText("relation");
            if(r == null)
            	r = "?";
            MappingRelation rel = MappingRelation.parseRelation(StringEscapeUtils.unescapeXml(r));
            //Get the status
            String s = e.elementText("status");
            if(s == null)
            	s = "?";
            MappingStatus st = MappingStatus.parseStatus(s);
			add(sourceURI, targetURI, similarity, rel, st);
		}
	}
	
	private void loadMappingsTSV(String file) throws Exception
	{
		BufferedReader inStream = new BufferedReader(new FileReader(file));
		//First line contains the reference to AML
		inStream.readLine();
		//Second line contains the source ontology
		inStream.readLine();
		//Third line contains the target ontology
		inStream.readLine();
		//Fourth line contains the headers
		inStream.readLine();
		//And from the fifth line forward we have mappings
		String line;
		while((line = inStream.readLine()) != null)
		{
			String[] col = line.split("\t");
			//First column contains the source uri
			String sourceURI = col[0];
			//Third contains the target uri
			String targetURI = col[2];
			//Fifth contains the similarity
			String measure = col[4];
			//Parse it, assuming 1 if a valid measure is not found
			double similarity = 1;
			if(measure != null)
			{
				try
				{
					similarity = Double.parseDouble(measure);
		            if(similarity < 0 || similarity > 1)
		            	similarity = 1;
				}
            	catch(Exception ex){/*Do nothing - use the default value*/};
            }
			//The sixth column contains the type of relation
			MappingRelation rel;
			if(col.length > 5)
				rel = MappingRelation.parseRelation(col[5]);
			//For compatibility with previous tsv format without listed relation
			else
				rel = MappingRelation.EQUIVALENCE;
			//The seventh column, if it exists, contains the status of the Mapping
			MappingStatus st;
			if(col.length > 6)
				st = MappingStatus.parseStatus(col[6]);
			//For compatibility with previous tsv format without listed relation
			else
				st = MappingStatus.UNKNOWN;
			add(sourceURI, targetURI, similarity, rel, st);
		}
		inStream.close();
	}
}