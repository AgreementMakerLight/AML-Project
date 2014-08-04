/******************************************************************************
* Copyright 2013-2014 LASIGE                                                  *
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
* @date 23-06-2014                                                            *
* @version 2.0                                                                *
******************************************************************************/
package aml.match;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintWriter;
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
import aml.AML.MappingRelation;
import aml.ontology.Ontology;
import aml.ontology.RelationshipMap;
import aml.ontology.URIMap;
import aml.util.Table2Plus;

public class Alignment implements Iterable<Mapping>
{

//Attributes

	//Term mappings organized in list
	private Vector<Mapping> maps;
	//Term mappings organized by source class
	private Table2Plus<Integer,Integer,Mapping> sourceMaps;
	//Term mappings organized by target class
	private Table2Plus<Integer,Integer,Mapping> targetMaps;
	
//Constructors

	/**
	 * Creates a new empty Alignment
	 */
	public Alignment()
	{
		maps = new Vector<Mapping>(0,1);
		sourceMaps = new Table2Plus<Integer,Integer,Mapping>();
		targetMaps = new Table2Plus<Integer,Integer,Mapping>();
	}

	/**
	 * Reads an Alignment from an input file
	 * @param file: the path to the input file
	 */
	public Alignment(String file) throws Exception
	{
		maps = new Vector<Mapping>(0,1);
		sourceMaps = new Table2Plus<Integer,Integer,Mapping>();
		targetMaps = new Table2Plus<Integer,Integer,Mapping>();
		if(file.endsWith(".rdf"))
			loadMappingsRDF(file);
		else if(file.endsWith(".tsv"))
			loadMappingsTSV(file);
		else
			throw new Exception("Unrecognized alignment format!");
	}
	
	/**
	 * Creates a new Alignment that is a copy of the input alignment
	 * @param a: the Alignment to copy
	 */
	public Alignment(Alignment a)
	{
		maps = new Vector<Mapping>(0,1);
		sourceMaps = new Table2Plus<Integer,Integer,Mapping>();
		targetMaps = new Table2Plus<Integer,Integer,Mapping>();
		addAll(a);
	}
	
//Public Methods

	/**
	 * Adds a new Mapping to the alignment if it is non-redundant
	 * Otherwise, updates the similarity of the already present Mapping
	 * to the maximum similarity of the two redundant Mappings
	 * @param sourceId: the index of the source class to add to the alignment
	 * @param targetId: the index of the target class to add to the alignment
	 * @param sim: the similarity between the classes
	 */
	public void add(int sourceId, int targetId, double sim)
	{
		//We can't have a mapping between entities with the same URI
		if(sourceId == targetId)
			return;
		//Construct the Mapping
		Mapping m = new Mapping(sourceId, targetId, sim, MappingRelation.EQUIVALENCE);
		//If it isn't listed yet, add it
		if(!sourceMaps.contains(sourceId,targetId))
		{
			maps.add(m);
			sourceMaps.add(sourceId, targetId, m);
			targetMaps.add(targetId, sourceId, m);
		}
		//Otherwise update the similarity
		else
		{
			m = sourceMaps.get(sourceId,targetId);
			if(m.getSimilarity() < sim)
				m.setSimilarity(sim);
			if(!m.getRelationship().equals(MappingRelation.EQUIVALENCE))
				m.setRelationship(MappingRelation.EQUIVALENCE);		
		}
	}
	
	/**
	 * Adds a new Mapping to the alignment if it is non-redundant
	 * Otherwise, updates the similarity of the already present Mapping
	 * to the maximum similarity of the two redundant Mappings
	 * @param sourceId: the index of the source class to add to the alignment
	 * @param targetId: the index of the target class to add to the alignment
	 * @param sim: the similarity between the classes
	 * @param r: the mapping relationship between the classes
	 */
	public void add(int sourceId, int targetId, double sim, MappingRelation r)
	{
		//We can't have a mapping between entities with the same URI
		if(sourceId == targetId)
			return;
		//Construct the Mapping
		Mapping m = new Mapping(sourceId, targetId, sim, r);
		//If it isn't listed yet, add it
		if(!sourceMaps.contains(sourceId,targetId))
		{
			maps.add(m);
			sourceMaps.add(sourceId, targetId, m);
			targetMaps.add(targetId, sourceId, m);
		}
		//Otherwise update the similarity
		else
		{
			m = sourceMaps.get(sourceId,targetId);
			if(m.getSimilarity() < sim)
				m.setSimilarity(sim);
			if(!m.getRelationship().equals(r))
				m.setRelationship(r);		
		}
	}
	
	/**
	 * Adds a clone of the given Mapping to the alignment if it is non-redundant
	 * Otherwise, updates the similarity of the already present Mapping
	 * to the maximum similarity of the two redundant Mappings
	 * @param m: the Mapping to add to the alignment
	 */
	public void add(Mapping m)
	{
		add(m.getSourceId(), m.getTargetId(), m.getSimilarity(), m.getRelationship());
	}

	/**
	 * Adds all Mappings in a to this Alignment
	 * @param a: the Alignment to add to this Alignment
	 */
	public void addAll(Alignment a)
	{
		addAll(a.maps);
	}
	
	/**
	 * Adds all Mappings in the given list to this Alignment
	 * @param maps: the list of Mappings to add to this Alignment
	 */
	public void addAll(List<Mapping> maps)
	{
		for(Mapping m : maps)
			add(m);
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
		a.sort();
		for(Mapping m : a.maps)
			if(!this.containsConflict(m))
				add(m);
	}
	
	/**
	 * @return the average cardinality of this alignment
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
	 * @param sourceId: the index of the source class to check in the alignment
 	 * @param targetId: the index of the target class to check in the alignment 
	 * @return whether the alignment contains a Mapping that is ancestral to the given pair of classes
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
 	 * @param m: the Mapping to check in the alignment 
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
	 * @param sourceId: the index of the source class to check in the alignment
 	 * @param targetId: the index of the target class to check in the alignment 
	 * @return whether the Alignment contains a Mapping for sourceId or for targetId
	 */
	public boolean containsConflict(int sourceId, int targetId)
	{
		return containsSource(sourceId) || containsTarget(targetId);
	}
	
	/**
 	 * @param m: the Mapping to check in the alignment 
	 * @return whether the Alignment contains a Mapping involving either class in m
	 */
	public boolean containsConflict(Mapping m)
	{
		return containsConflict(m.getSourceId(),m.getTargetId());
	}
	
	/**
	 * @param sourceId: the index of the source class to check in the alignment
 	 * @param targetId: the index of the target class to check in the alignment 
	 * @return whether the alignment contains a Mapping that is descendant of the given pair of classes
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
	 * @param sourceId: the index of the source class to check in the alignment
	 * @param targetId: the index of the target class to check in the alignment
	 * @return whether the Alignment contains a Mapping between sourceId and targetId
	 */
	public boolean containsMapping(int sourceId, int targetId)
	{
		return sourceMaps.contains(sourceId, targetId);
	}
	
	/**
	 * @param m: the Mapping to check in the alignment
	 * @return whether the Alignment contains a Mapping equivalent to m
	 */
	public boolean containsMapping(Mapping m)
	{
		return sourceMaps.contains(m.getSourceId(), m.getTargetId());
	}
	
	/**
	 * @param lm: the List of Mapping to check in the alignment
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
	 * @param sourceId: the index of the source class to check in the alignment
 	 * @param targetId: the index of the target class to check in the alignment 
	 * @return whether the alignment contains a Mapping that is parent to the
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
	 * @param sourceId: the index of the source class to check in the alignment
 	 * @return whether the Alignment contains a Mapping for sourceId
	 */
	public boolean containsSource(int sourceId)
	{
		return sourceMaps.contains(sourceId);
	}

	/**
	 * @param targetId: the index of the target class to check in the alignment
 	 * @return whether the Alignment contains a Mapping for targetId
	 */
	public boolean containsTarget(int targetId)
	{
		return targetMaps.contains(targetId);
	}
	
	/**
	 * @param a: the Alignment to subtract from this Alignment 
	 * @return the Alignment corresponding to the difference between this Alignment and a
	 */
	public Alignment difference(Alignment a)
	{
		//Otherwise, compute the intersection
		Alignment diff = new Alignment();
		for(Mapping m : maps)
			if(!a.containsMapping(m))
				diff.add(m);
		return diff;
	}
	
	/**
	 * @param a: the reference Alignment to evaluate this Alignment 
	 * @return the number of Mappings in this Alignment that are correct
	 * (i.e., found in the reference Alignment)
	 */
	public int evaluate(Alignment a)
	{
		int correct = 0;
		for(Mapping m : a.maps)
			if(!m.getRelationship().equals(MappingRelation.UNKNOWN) &&
					this.containsMapping(m))
				correct++;
		return correct;
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
	 * @param sourceId: the index of the source class to check in the alignment
	 * @param targetId: the index of the target class to check in the alignment
 	 * @return the Mapping between the source and target classes or null if no
 	 * such Mapping exists
	 */
	public Mapping get(int sourceId, int targetId)
	{
		return sourceMaps.get(sourceId, targetId);
	}
	
	/**
	 * @param id1: the index of the first class to check in the alignment
	 * @param targetId: the index of the second class to check in the alignment
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
	 * @param sourceId: the index of the source class to check in the alignment
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
	 * @param targetId: the index of the target class to check in the alignment
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
	 * @return the high level Alignment induced from this alignment 
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
	 * @param id: the index of the class to check in the alignment
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
	 * @param sourceId: the index of the source class to check in the alignment
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
	 * @param targetId: the index of the target class to check in the alignment
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
	 * @param sourceId: the index of the source class in the alignment
	 * @param targetId: the index of the target class in the alignment
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
	 * @param sourceId: the index of the source class in the alignment
	 * @param targetId: the index of the target class in the alignment
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
	 * @param sourceId: the index of the source class to check in the alignment
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
	 * @param targetId: the index of the target class to check in the alignment
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

	/**
	 * @param a: the Alignment to intersect with this Alignment 
	 * @return the Alignment corresponding to the intersection between this Alignment and a
	 */
	public Alignment intersection(Alignment a)
	{
		//Otherwise, compute the intersection
		Alignment intersection = new Alignment();
		for(Mapping m : maps)
			if(a.containsMapping(m))
				intersection.add(m);
		return intersection;
	}
	
	@Override
	/**
	 * @return an Iterator over the list of class Mappings
	 */
	public Iterator<Mapping> iterator()
	{
		return maps.iterator();
	}
	
	/**
	 * @return the maximum cardinality of this alignment
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
	
	/**
	 * Removes the given Mapping from the Alignment
	 * @param m: the Mapping to remove from the Alignment
	 */
	public void remove(Mapping m)
	{
		int sourceId = m.getSourceId();
		int targetId = m.getTargetId();
		sourceMaps.remove(sourceId, targetId);
		targetMaps.remove(targetId, sourceId);
		maps.remove(m);
	}
	
	/**
	 * Removes the Mapping between the given classes from the Alignment
	 * @param sourceId: the source class to remove from the Alignment
	 * @param targetId: the target class to remove from the Alignment
	 */
	public void remove(int sourceId, int targetId)
	{
		Mapping m = new Mapping(sourceId, targetId, 1.0);
		sourceMaps.remove(sourceId, targetId);
		targetMaps.remove(targetId, sourceId);
		maps.remove(m);
	}
	
	/**
	 * Removes a list of Mappings from the alignment.
	 * @param maps: the list of Mappings to remove to this Alignment
	 */
	public void removeAll(List<Mapping> maps)
	{
		for(Mapping m : maps)
			remove(m);
	}

	/**
	 * Saves the alignment into an .rdf file in OAEI format
	 * @param file: the output file
	 */
	public void saveRDF(String file) throws FileNotFoundException
	{
		AML aml = AML.getInstance();
		String sourceURI = aml.getSource().getURI();
		String targetURI = aml.getTarget().getURI();
		URIMap uris = aml.getURIMap();
		
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
		{
			outStream.println("\t<map>");
			outStream.println("\t\t<Cell>");
			outStream.println("\t\t\t<entity1 rdf:resource=\""+uris.getURI(m.getSourceId())+"\"/>");
			outStream.println("\t\t\t<entity2 rdf:resource=\""+uris.getURI(m.getTargetId())+"\"/>");
			outStream.println("\t\t\t<measure rdf:datatype=\"http://www.w3.org/2001/XMLSchema#float\">"+
					m.getSimilarity()+"</measure>");
			outStream.println("\t\t\t<relation>" + StringEscapeUtils.escapeXml(m.getRelationship().toString()) +
					"</relation>");
			outStream.println("\t\t</Cell>");
			outStream.println("\t</map>");
		}
		outStream.println("</Alignment>");
		outStream.println("</rdf:RDF>");		
		outStream.close();
	}
	
	/**
	 * Saves the alignment into a .tsv file in AML format
	 * @param file: the output file
	 */
	public void saveTSV(String file) throws FileNotFoundException
	{
		AML aml = AML.getInstance();
		Ontology source = aml.getSource();
		Ontology target = aml.getTarget();
		URIMap uris = aml.getURIMap();
		
		PrintWriter outStream = new PrintWriter(new FileOutputStream(file));
		outStream.println("#AgreementMakerLight Alignment File");
		outStream.println("#Source ontology:\t" + source.getURI());
		outStream.println("#Target ontology:\t" + target.getURI());
		outStream.println("Source URI\tSource Label\tTarget URI\tTarget Label\tSimilarity\tRelationship");
		for(Mapping m : maps)
			outStream.println(uris.getURI(m.getSourceId()) + "\t" + source.getName(m.getSourceId()) +
						"\t" + uris.getURI(m.getTargetId()) + "\t" + target.getName(m.getTargetId()) +
						"\t" + m.getSimilarity() + "\t" + m.getRelationship().toString());
		outStream.close();
	}

	/**
	 * @return the number of Mappings in this Alignment
	 */
	public int size()
	{
		return maps.size();
	}
	
	/**
	 * Sorts the Alignment descendingly, by similarity
	 */
	public void sort()
	{
		Collections.sort(maps,new Comparator<Mapping>()
        {
            public int compare(Mapping m1, Mapping m2)
            {
        		double diff = m2.getSimilarity() - m1.getSimilarity();
        		if(diff < 0)
        			return -1;
        		if(diff > 0)
        			return 1;
        		return 0;
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
		if(aml.matchProperties())
			count += aml.getSource().propertyCount();
		coverage /= count;
		return coverage;
	}
	
	/**
	 * @param a: the Alignment to subtract from this Alignment 
	 * @return the Alignment containing all mappings in this that aren't in a
	 */
	public Alignment subtract(Alignment a)
	{
		//Otherwise, compute the difference
		Alignment difference = new Alignment();
		for(Mapping m : maps)
			if(!a.containsMapping(m))
				difference.add(m);
		return difference;
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
		if(aml.matchProperties())
			count += aml.getTarget().propertyCount();
		coverage /= count;
		return coverage;
	}
	
//Private Methods

	private void loadMappingsRDF(String file) throws DocumentException
	{
		AML aml = AML.getInstance();
		URIMap uris = aml.getURIMap();
		
		//Open the alignment file using SAXReader
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
			//Check if the URIs are listed in the URI map 
			int sourceIndex = uris.getIndex(sourceURI);
			int targetIndex = uris.getIndex(targetURI);
			//If they are, add the mapping to the maps and proceed to next mapping
			if(sourceIndex > -1 && targetIndex > -1)
			{
				if(sourceIndex < targetIndex)
					add(sourceIndex, targetIndex, similarity, rel);
				else
					add(targetIndex, sourceIndex, similarity, rel);
            }
		}
	}
	
	private void loadMappingsTSV(String file) throws Exception
	{
		AML aml = AML.getInstance();
		URIMap uris = aml.getURIMap();
		
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
			//Finally, sixth column contains the type of relation
			MappingRelation rel;
			if(col.length > 5)
				rel = MappingRelation.parseRelation(col[5]);
			//For compatibility with previous tsv format without listed relation
			else
				rel = MappingRelation.EQUIVALENCE;
            //Get the indexes
			int sourceIndex = uris.getIndex(sourceURI);
			int targetIndex = uris.getIndex(targetURI);
			if(sourceIndex > -1 && targetIndex > -1)
			{
				if(sourceIndex < targetIndex)
					add(sourceIndex, targetIndex, similarity, rel);
				else
					add(targetIndex, sourceIndex, similarity, rel);
            }
		}
		inStream.close();
	}
}