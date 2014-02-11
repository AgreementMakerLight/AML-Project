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
* An alignment between two Ontologies, separated into class alignment and     *
* property alignment. The former is stored both as a list of Mappings and     *
* as a Table, whereas the latter is stored only as a list of Mappings.        *
* The Alignment includes methods for input and output.                        *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 05-02-2014                                                            *
******************************************************************************/
package aml.match;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import aml.ontology.Ontology;
import aml.ontology.RelationshipMap;
import aml.util.Table2Plus;


public class Alignment implements Iterable<Mapping>
{

//Attributes

	//The links to the source and target Ontologies
	private Ontology source;
	private Ontology target;
	
	//Term mappings organized in list
	private Vector<Mapping> termMappings;
	//Term mappings organized by source term
	private Table2Plus<Integer,Integer,Double> sourceMaps;
	//Term mappings organized by target term
	private Table2Plus<Integer,Integer,Double> targetMaps;
	//Property mappings organized in list
	private Vector<Mapping> propMappings;

//Constructors

	/**
	 * Creates a new empty Alignment between Ontologys s and t
	 * @param s: the source Ontology
	 * @param t: the target Ontology
	 */
	public Alignment(Ontology s, Ontology t)
	{
		source = s;
		target = t;
		termMappings = new Vector<Mapping>(0,1);
		sourceMaps = new Table2Plus<Integer,Integer,Double>();
		targetMaps = new Table2Plus<Integer,Integer,Double>();
		propMappings = new Vector<Mapping>(0,1);
	}

	/**
	 * Reads an Alignment between two Ontologies from an
	 * input file
	 * @param s: the source Ontology
	 * @param t: the target Ontology
	 * @param file: the path to the input file
	 */
	public Alignment(Ontology s, Ontology t, String file) throws Exception
	{
		source = s;
		target = t;
		termMappings = new Vector<Mapping>(0,1);
		sourceMaps = new Table2Plus<Integer,Integer,Double>();
		targetMaps = new Table2Plus<Integer,Integer,Double>();
		propMappings = new Vector<Mapping>(0,1);
		
		if(file.endsWith(".rdf"))
			loadMappingsRDF(file);
		else if(file.endsWith(".tsv"))
			loadMappingsTSV(file);
	}
	
	/**
	 * Creates a new Alignment that is a copy of the input alignment
	 * @param a: the Alignment to copy
	 */
	public Alignment(Alignment a)
	{
		source = a.source;
		target = a.target;
		sourceMaps = new Table2Plus<Integer,Integer,Double>();
		targetMaps = new Table2Plus<Integer,Integer,Double>();
		termMappings = new Vector<Mapping>(0,1);
		propMappings = new Vector<Mapping>(0,1);
		addAll(a);
	}
	
//Public Methods

	/**
	 * Adds a new Mapping to the alignment if it is non-redundant
	 * Otherwise, updates the similarity of the already present Mapping
	 * to the maximum similarity of the two redundant Mappings
	 * @param sourceId: the index of the source term to add to the alignment
	 * @param targetId: the index of the target term to add to the alignment
	 * @param sim: the similarity between the terms
	 */
	public void add(int sourceId, int targetId, double sim)
	{
		//Construct the Mapping
		Mapping m = new Mapping(sourceId, targetId, sim);
		//If it isn't listed yet, add it
		int index = termMappings.indexOf(m);
		if(index < 0)
			termMappings.add(m);
		//Otherwise update the similarity
		else
		{
			m = termMappings.get(index);
			if(m.getSimilarity() < sim)
				m.setSimilarity(sim);
		}
		//Then update the MultiMaps
		sourceMaps.addUpgrade(sourceId, targetId, sim);
		targetMaps.addUpgrade(targetId, sourceId, sim);
	}
	
	/**
	 * Adds a new Mapping to the alignment if it is non-redundant
	 * Otherwise, updates the similarity of the already present Mapping
	 * to the maximum similarity of the two redundant Mappings
	 * @param m: the Mapping to add to the alignment
	 */
	public void add(Mapping m)
	{
		add(m.getSourceId(), m.getTargetId(), m.getSimilarity());
	}

	/**
	 * Adds all Mappings in a to this Alignment
	 * @param a: the Alignment to add to this Alignment
	 */
	public void addAll(Alignment a)
	{
		//If the alignments aren't between the same ontologies, do nothing
		if(source != a.source || target != a.target)
			return;
		addAll(a.termMappings);
		addAllPropMappings(a.propMappings);
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
		//If the alignments aren't between the same ontologies, do nothing
		if(source != a.source || target != a.target)
			return;
		for(Mapping m : a.termMappings)
			if(!this.containsConflict(m))
				add(m);
	}
	
	/**
	 * Adds all property Mappings in a to this Alignment
	 * @param v: the list of Mappings to add to this Alignment
	 */
	public void addAllPropMappings(Vector<Mapping> v)
	{
		for(Mapping m : v)
			addPropMapping(m);
	}
	
	/**
	 * Adds a new property Mapping to the alignment
	 * @param sourceId: the index of the source term to add to the alignment
	 * @param targetId: the index of the target term to add to the alignment
	 * @param sim: the similarity between the terms
	 */
	public void addPropMapping(int sourceId, int targetId, double sim)
	{
		Mapping m = new Mapping(sourceId, targetId, sim);
		propMappings.add(m);
	}
	
	/**
	 * Adds the given property Mapping to this Alignment
	 * @param m: the Mapping to add to this Alignment
	 */
	public void addPropMapping(Mapping m)
	{
		propMappings.add(m);
	}
	
	/**
	 * Boosts the similarity of reciprocal best matches by the given fraction
	 * @param fraction: the fraction by which to boost the best matches
	 */
	public void boostBestMatches(double fraction)
	{
		//The factor by which similarities will be multiplied
		double factor = 1.0 + fraction;
		//First get the maximum similarity for each source term mapped
		HashMap<Integer,Double> sourceMax = new HashMap<Integer,Double>();
		Set<Integer> sourceList = sourceMaps.keySet();
		for(Integer i : sourceList)
			sourceMax.put(i, getMaxSourceSim(i));
		//Then do the same for each target term mapped
		HashMap<Integer,Double> targetMax = new HashMap<Integer,Double>();
		Set<Integer> targetList = targetMaps.keySet();
		for(Integer i : targetList)
			targetMax.put(i, getMaxSourceSim(i));
		//Now boost each mapping
		for(Mapping m: termMappings)
		{
			int sourceId = m.getSourceId();
			int targetId = m.getTargetId();
			double sim = m.getSimilarity();
			if(sourceMax.get(sourceId) == sim && targetMax.get(targetId) == sim)
				//Adding an existing Mapping to the Alignment results in updating
				//the similarity if it is higher than the previous similarity
				add(sourceId, targetId, Math.min(1.0, sim*factor));
		}
	}

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
	 * @param sourceId: the index of the source term to check in the alignment
 	 * @param targetId: the index of the target term to check in the alignment 
	 * @return whether the alignment contains a Mapping that is ancestral to the given pair of terms
	 * (i.e. includes one ancestor of sourceId and one ancestor of targetId)
	 */
	public boolean containsAncestralMapping(int sourceId, int targetId)
	{
		//Get the RelationshipMaps of both Ontologies
		RelationshipMap sRels = (source).getRelationshipMap();
		RelationshipMap tRels = (target).getRelationshipMap();
		
		Vector<Integer> sourceAncestors = sRels.getAncestors(sourceId);
		Vector<Integer> targetAncestors = tRels.getAncestors(targetId);
		
		for(Integer sa : sourceAncestors)
		{
			Vector<Integer> over = getSourceMappings(sa);
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
			HashMap<Integer,Double> src = sourceMaps.get(source);
			Set<Integer> sources = src.keySet();
			for(Integer i : sources)
				if(src.get(i) > sim)
					return true;
		}
		if(containsTarget(target))
		{
			HashMap<Integer,Double> tgt = targetMaps.get(target);
			Set<Integer> targets = tgt.keySet();
			for(Integer i : targets)
				if(tgt.get(i) > sim)
					return true;
		}
		return false;
	}
	
	/**
	 * @param sourceId: the index of the source term to check in the alignment
 	 * @param targetId: the index of the target term to check in the alignment 
	 * @return whether the Alignment contains a Mapping for sourceId or for targetId
	 */
	public boolean containsConflict(int sourceId, int targetId)
	{
		return containsSource(sourceId) || containsTarget(targetId);
	}
	
	/**
 	 * @param m: the Mapping to check in the alignment 
	 * @return whether the Alignment contains a Mapping involving either term in m
	 */
	public boolean containsConflict(Mapping m)
	{
		return containsConflict(m.getSourceId(),m.getTargetId());
	}
	
	/**
	 * @param sourceId: the index of the source term to check in the alignment
 	 * @param targetId: the index of the target term to check in the alignment 
	 * @return whether the alignment contains a Mapping that is descendant of the given pair of terms
	 * (i.e. includes one descendant of sourceId and one descendant of targetId)
	 */
	public boolean containsDescendantMapping(int sourceId, int targetId)
	{
		//Get the RelationshipMaps of both Ontologies
		RelationshipMap sRels = (source).getRelationshipMap();
		RelationshipMap tRels = (target).getRelationshipMap();
		
		Vector<Integer> sourceDescendants = sRels.getDescendants(sourceId);
		Vector<Integer> targetDescendants = tRels.getDescendants(targetId);
		
		for(Integer sa : sourceDescendants)
		{
			Vector<Integer> over = getSourceMappings(sa);
			for(Integer ta : targetDescendants)
				if(over.contains(ta))
					return true;
		}
		return false;
	}
	
	/**
	 * @param sourceId: the index of the source term to check in the alignment
	 * @param targetId: the index of the target term to check in the alignment
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
	 * @param sourceId: the index of the source term to check in the alignment
 	 * @param targetId: the index of the target term to check in the alignment 
	 * @return whether the alignment contains a Mapping that is parent to the
	 * given pair of terms on one side only
	 */
	public boolean containsParentMapping(int sourceId, int targetId)
	{
		//Get the RelationshipMaps of both Ontologies
		RelationshipMap sRels = (source).getRelationshipMap();
		RelationshipMap tRels = (target).getRelationshipMap();
		
		Vector<Integer> sourceAncestors = sRels.getParents(sourceId);
		Vector<Integer> targetAncestors = tRels.getParents(targetId);
		
		for(Integer sa : sourceAncestors)
			if(containsMapping(sa,targetId))
				return true;
		for(Integer ta : targetAncestors)
			if(containsMapping(sourceId,ta))
				return true;
		return false;
	}
	
	/**
	 * @param m: the property Mapping to check in the alignment
	 * @return whether the Alignment contains a property Mapping equivalent to m
	 */
	public boolean containsPropMapping(Mapping m)
	{
		for(Mapping n : propMappings)
			if(m.getSourceId() == n.getSourceId() && m.getTargetId() == n.getTargetId())
				return true;
		return false;
	}
	
	/**
	 * @param sourceId: the index of the source term to check in the alignment
 	 * @return whether the Alignment contains a Mapping for sourceId
	 */
	public boolean containsSource(int sourceId)
	{
		return sourceMaps.contains(sourceId);
	}

	/**
	 * @param targetId: the index of the target term to check in the alignment
 	 * @return whether the Alignment contains a Mapping for targetId
	 */
	public boolean containsTarget(int targetId)
	{
		return targetMaps.contains(targetId);
	}
	
	/**
	 * @param a: the reference Alignment to evaluate this Alignment 
	 * @return the number of Mappings in this Alignment that are correct
	 * (i.e., found in the reference Alignment)
	 */
	public int evaluate(Alignment a)
	{
		//If the alignments aren't between the same ontologies, the intersection is null
		if(source != a.source || target != a.target)
			return 0;
		int correct = 0;
		for(Mapping m : a.termMappings)
			if(this.containsMapping(m))
				correct++;
		for(Mapping m : a.propMappings)
			if(this.containsPropMapping(m))
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
		for(Mapping m : termMappings)
			if(!a.containsMapping(m))
				gain++;
		gain /= a.termMappingCount();
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
	 * @param index: the index of the Mapping to return
 	 * @return the Mapping at the input index
	 */
	public Mapping get(int index)
	{
		return termMappings.get(index);
	}
	
	/**
	 * @param sourceId: the index of the source term to check in the alignment
 	 * @return the index of the target term that best matches source
	 */
	public int getBestSourceMatch(int sourceId)
	{
		Integer bestMatch = sourceMaps.getKeyMaximum(sourceId);
		if(bestMatch == null)
			return -1;
		return bestMatch;
	}

	/**
	 * @param targetId: the index of the target term to check in the alignment
 	 * @return the index of the source term that best matches target
	 */
	public int getBestTargetMatch(int targetId)
	{
		Integer bestMatch = targetMaps.getKeyMaximum(targetId);
		if(bestMatch == null)
			return -1;
		return bestMatch;
	}
	
	/**
	 * @return the high level Alignment induced from this alignment 
	 */
	public Alignment getHighLevelAlignment(double threshold)
	{
		RelationshipMap sourceMap = source.getRelationshipMap();
		RelationshipMap targetMap = target.getRelationshipMap();
		Alignment a = new Alignment(source, target);
		int total = termMappings.size();
		for(Mapping m : termMappings)
		{
			Vector<Integer> sourceAncestors = sourceMap.getHighLevelAncestors(m.getSourceId());
			Vector<Integer> targetAncestors = targetMap.getHighLevelAncestors(m.getTargetId());
			for(int i : sourceAncestors)
			{
				for(int j : targetAncestors)
				{
					double sim = a.getSimilarity(i, j) + 1.0 / total;
					a.add(i,j,sim);
				}
			}
		}
		Alignment b = new Alignment(source, target);
		for(Mapping m : a)
			if(m.getSimilarity() >= threshold)
				b.add(m);
		return b;
	}
	
	/**
	 * @param sourceId: the index of the source term
	 * @param targetId: the index of the target term
	 * @return the index of the Mapping between the given terms in
	 * the list of Mappings, or -1 if the Mapping doesn't exist
	 */
	public int getIndex(int sourceId, int targetId)
	{
		Mapping m = new Mapping(sourceId,targetId,1.0);
		return termMappings.indexOf(m);
	}
	
	/**
	 * @param sourceId: the index of the source term to check in the alignment
 	 * @return the index of the target term that best matches source
	 */
	public double getMaxSourceSim(int sourceId)
	{
		Double sim = sourceMaps.getMaximumValue(sourceId);
		if(sim == null)
			return 0.0;
		return sim;
	}

	/**
	 * @param targetId: the index of the target term to check in the alignment
 	 * @return the index of the source term that best matches target
	 */
	public double getMaxTargetSim(int targetId)
	{
		Double sim = targetMaps.getMaximumValue(targetId);
		if(sim == null)
			return 0.0;
		return sim;
	}
	
	/**
	 * @param sourceId: the index of the source term in the alignment
	 * @param targetId: the index of the target term in the alignment
	 * @return the similarity between source and target
	 */
	public double getSimilarity(int sourceId, int targetId)
	{
		Double sim = sourceMaps.get(sourceId, targetId);
		if(sim == null)
			return 0.0;
		return sim;
	}
	
	/**
	 * @param sourceId: the index of the source term to check in the alignment
 	 * @return the list of all target terms mapped to the source term
	 */
	public Vector<Integer> getSourceMappings(int sourceId)
	{
		Vector<Integer> maps = new Vector<Integer>(0,1);
		Set<Integer> sourceList = sourceMaps.keySet(sourceId);
		if(sourceList != null)
			maps.addAll(sourceList);
		return maps;
	}
	
	/**
	 * @return the source Ontology of the Alignment
	 */
	public Ontology getSource()
	{
		return source;
	}
	
	/**
 	 * @return the list of all source terms that have mappings
	 */
	public Vector<Integer> getSources()
	{
		Vector<Integer> maps = new Vector<Integer>(0,1);
		maps.addAll(sourceMaps.keySet());
		return maps;
	}
	
	/**
	 * @param targetId: the index of the target term to check in the alignment
 	 * @return the list of all source terms mapped to the target term
	 */
	public Vector<Integer> getTargetMappings(int targetId)
	{
		Vector<Integer> maps = new Vector<Integer>(0,1);
		Set<Integer> targetList = targetMaps.keySet(targetId);
		if(targetList != null)
			maps.addAll(targetList);
		return maps;
	}
	
	/**
	 * @return the target Ontology of the Alignment
	 */
	public Ontology getTarget()
	{
		return target;
	}
	
	/**
 	 * @return the list of all target terms that have mappings
	 */
	public Vector<Integer> getTargets()
	{
		Vector<Integer> maps = new Vector<Integer>(0,1);
		maps.addAll(targetMaps.keySet());
		return maps;
	}

	/**
	 * @param a: the Alignment to intersect with this Alignment 
	 * @return the Alignment corresponding to the intersection between this Alignment and a
	 */
	public Alignment intersection(Alignment a)
	{
		//If the alignments aren't between the same ontologies, the intersection is null
		if(source != a.source || target != a.target)
			return null;
		//Otherwise, compute the intersection
		Alignment intersection = new Alignment(source,target);
		for(Mapping m : termMappings)
			if(a.containsMapping(m))
				intersection.add(m);
		for(Mapping m : propMappings)
			if(a.containsPropMapping(m))
				intersection.addPropMapping(m);
		return intersection;
	}
	
	/**
	 * @return whether this Alignment is sorted
	 */
	public boolean isSorted()
	{
		double sim = termMappings.get(0).getSimilarity();
		for(Mapping m : termMappings)
		{
			if(m.getSimilarity() > sim)
				return false;
			else
				sim = m.getSimilarity();
		}
		return true;
	}
	
	@Override
	/**
	 * @return an Iterator over the list of term Mappings
	 */
	public Iterator<Mapping> iterator()
	{
		return termMappings.iterator();
	}
	
	/**
	 * @return the number of property Mappings in this alignment
	 */
	public int propertyMappingCount()
	{
		return propMappings.size();
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
		termMappings.remove(m);
	}
	
	/**
	 * Removes the Mapping between the given terms from the Alignment
	 * @param sourceId: the source term to remove from the Alignment
	 * @param targetId: the target term to remove from the Alignment
	 */
	public void remove(int sourceId, int targetId)
	{
		Mapping m = new Mapping(sourceId, targetId, 1.0);
		sourceMaps.remove(sourceId, targetId);
		targetMaps.remove(targetId, sourceId);
		termMappings.remove(m);
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
		PrintWriter outStream = new PrintWriter(new FileOutputStream(file));
		outStream.println("<?xml version='1.0' encoding='utf-8'?>");
		outStream.println("<rdf:RDF xmlns='http://knowledgeweb.semanticweb.org/heterogeneity/alignment'"); 
		outStream.println("\t xmlns:rdf='http://www.w3.org/1999/02/22-rdf-syntax-ns#' "); 
		outStream.println("\t xmlns:xsd='http://www.w3.org/2001/XMLSchema#' ");
		outStream.println("\t alignmentSource='AgreementMakerLight'>\n");
		outStream.println("<Alignment>");
		outStream.println("\t<xml>yes</xml>");
		outStream.println("\t<level>0</level>");
		outStream.println("\t<type>11</type>");
		outStream.println("\t<onto1>" + source.getURI() + "</onto1>");
		outStream.println("\t<onto2>" + target.getURI() + "</onto2>");
		outStream.println("\t<uri1>" + source.getURI() + "</uri1>");
		outStream.println("\t<uri2>" + target.getURI() + "</uri2>");

		for(Mapping m : termMappings)
		{
			outStream.println("\t<map>");
			outStream.println("\t\t<Cell>");
			outStream.println("\t\t\t<entity1 rdf:resource=\""+source.getTermURI(m.getSourceId())+"\"/>");
			outStream.println("\t\t\t<entity2 rdf:resource=\""+target.getTermURI(m.getTargetId())+"\"/>");
			outStream.println("\t\t\t<measure rdf:datatype=\"http://www.w3.org/2001/XMLSchema#float\">"+
					m.getSimilarity()+"</measure>");
			outStream.println("\t\t\t<relation>=</relation>");
			outStream.println("\t\t</Cell>");
			outStream.println("\t</map>");
		}
		for(Mapping m : propMappings)
		{
			outStream.println("\t<map>");
			outStream.println("\t\t<Cell>");
			outStream.println("\t\t\t<entity1 rdf:resource=\""+source.getPropertyURI(m.getSourceId())+"\"/>");
			outStream.println("\t\t\t<entity2 rdf:resource=\""+target.getPropertyURI(m.getTargetId())+"\"/>");
			outStream.println("\t\t\t<measure rdf:datatype=\"http://www.w3.org/2001/XMLSchema#float\">"+
					m.getSimilarity()+"</measure>");
			outStream.println("\t\t\t<relation>=</relation>");
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
		PrintWriter outStream = new PrintWriter(new FileOutputStream(file));
		outStream.println("#AgreementMakerLight Alignment File");
		outStream.println("#Source ontology:\t" + source.getURI());
		outStream.println("#Target ontology:\t" + target.getURI());
		outStream.println("Source URI\tSource Label\tTarget URI\tTarget Label\tSimilarity");
		for(Mapping m : termMappings)
		{
			int sId = m.getSourceId();
			int tId = m.getTargetId();
			outStream.println(source.getTermURI(sId) + "\t" + source.getLexicon().getBestName(sId) +
					"\t" + target.getTermURI(tId) + "\t" + target.getLexicon().getBestName(tId) +
					"\t" + m.getSimilarity());
		}
		for(Mapping m : propMappings)
		{
			int sId = m.getSourceId();
			int tId = m.getTargetId();
			outStream.println(source.getPropertyURI(sId) + "\t" + source.getPropertyList().getName(sId) +
					"\t" + target.getPropertyURI(tId) + "\t" + target.getPropertyList().getName(tId) +
					"\t" + m.getSimilarity());
		}
		outStream.close();
	}

	/**
	 * @return the number of Mappings in this Alignment
	 */
	public int size()
	{
		return termMappings.size() + propMappings.size();
	}
	
	/**
	 * Sorts the Alignment descendingly, by similarity
	 */
	public void sort()
	{
		quickSort(0,termMappings.size()-1);
	}
	
	/**
	 * @return the number of source terms mapped in this Alignment
	 */
	public int sourceCount()
	{
		return sourceMaps.keyCount();
	}
	
	/**
	 * @return the fraction of source terms mapped in this Alignment
	 */
	public double sourceCoverage()
	{
		double coverage = sourceMaps.keyCount();
		coverage /= source.termCount();
		return coverage;
	}
	
	/**
	 * @param a: the Alignment to subtract from this Alignment 
	 * @return the Alignment containing all mappings in this that aren't in a
	 */
	public Alignment subtract(Alignment a)
	{
		//If the alignments aren't between the same ontologies, the difference is this Alignment
		if(source != a.source || target != a.target)
			return this;
		//Otherwise, compute the difference
		Alignment difference = new Alignment(source,target);
		for(Mapping m : termMappings)
			if(!a.containsMapping(m))
				difference.add(m);
		return difference;
	}
	
	/**
	 * @return the number of target terms mapped in this Alignment
	 */
	public int targetCount()
	{
		return targetMaps.keyCount();
	}
	
	/**
	 * @return the fraction of target terms mapped in this Alignment
	 */
	public double targetCoverage()
	{
		double coverage = targetMaps.keyCount();
		coverage /= target.termCount();
		return coverage;
	}
	
	/**
	 * @return the number of term mappings in this Alignment
	 */
	public int termMappingCount()
	{
		return termMappings.size();
	}

	/**
	 * @param a: the Alignment to unite with this Alignment 
	 * @return the Alignment corresponding to the union between this Alignment and a
	 */
	public Alignment union(Alignment a)
	{
		//If the alignments aren't between the same ontologies, the union is null
		if(source != a.source || target != a.target)
			return null;
		//Otherwise the setup the union as a copy of this Alignment
		Alignment union = new Alignment(this);
		//Then add the Mappings in a
		union.addAll(a);
		return union;
	}
	
//Private Methods

	private void loadMappingsRDF(String file) throws DocumentException
	{
		//Open the alignment file using SAXReader
		SAXReader reader = new SAXReader();
		File f = new File(file);

		Document doc = reader.read(f);
		//Read the root, then go to the "Alignment" element
		Element root = doc.getRootElement();
		Element align = root.element("Alignment");
		//Control variables to check if the order of the ontologies
		//in the Alignment file is reversed
		boolean reverseOrder = false;
		boolean orderSet = false;
		//Get an iterator over the mappings
		Iterator<?> map = align.elementIterator("map");
		while(map.hasNext())
		{
			//Get the "Cell" in each mapping
			Element e = ((Element)map.next()).element("Cell");
			if(e == null)
				continue;
			//Get the source term
			String sourceURI = e.element("entity1").attributeValue("resource");
			//Get the target term
			String targetURI = e.element("entity2").attributeValue("resource");
			//Get the similarity measure
			String measure = e.elementText("measure");
			//Parse it, assuming 1 if a valid measure is not found
			double similarity = 1;
			if(measure != null)
			{
				try{similarity = Double.parseDouble(measure);}
            	catch(Exception ex){/*Do nothing - use the default value*/};
            }
            if(similarity < 0 || similarity > 1)
            	similarity = 1;
            //First we test ontologies in the order given unless we know the order is reversed
            if(!reverseOrder)
            {
				//Check if the URIs are listed as terms in the Ontologies
				int sourceIndex = source.getTermIndex(sourceURI);
				int targetIndex = target.getTermIndex(targetURI);
				//If they are, add the mapping to the termMappings and proceed to next mapping
				if(sourceIndex > -1 && targetIndex > -1)
				{
					add(sourceIndex, targetIndex, similarity);
					//setting the order so we don't continue testing the reverse order
					orderSet = true;
					continue;
				}
				//Otherwise see if they are listed as properties and do the same
				sourceIndex = source.getPropertyIndex(sourceURI);
				targetIndex = target.getPropertyIndex(targetURI);
				if(sourceIndex > -1 && targetIndex > -1)
				{
					addPropMapping(sourceIndex, targetIndex, similarity);
					orderSet = true;
					continue;
				}
            }
            //We check the reverse order if in doubt or sure it is reversed
            if(reverseOrder || !orderSet)
            {
				//We proceed as before, but switching source and target URIs
				int sourceIndex = source.getTermIndex(targetURI);
				int targetIndex = target.getTermIndex(sourceURI);
				if(sourceIndex > -1 && targetIndex > -1)
				{
					add(sourceIndex, targetIndex, similarity);
					reverseOrder = true;
					continue;
				}
				sourceIndex = source.getPropertyIndex(targetURI);
				targetIndex = target.getPropertyIndex(sourceURI);
				if(sourceIndex > -1 && targetIndex > -1)
				{
					addPropMapping(sourceIndex, targetIndex, similarity);
					reverseOrder = true;
					continue;
				}
            }
		}
	}
	
	private void loadMappingsTSV(String file) throws Exception
	{
		BufferedReader inStream = new BufferedReader(new FileReader(file));
		//First line contains the reference to AML
		inStream.readLine();
		//Second line contains the source ontology
		String line = inStream.readLine();
		String src = line.substring(line.indexOf("\t") + 1);
		//Third line contains the target ontology
		line = inStream.readLine();
		String tgt = line.substring(line.indexOf("\t") + 1);
		//Check if the ontologies in the file match those in this alignment
		boolean rightOrder = source.getURI().equals(src) && target.getURI().equals(tgt);
		//If not and they are not reversed, we can't proceed
		if(!rightOrder && !(source.getURI().equals(tgt) && target.getURI().equals(src)))
			return;
		//Fourth line contains the headers
		inStream.readLine();
		//And from the fifth line forward we have mappings
		while((line = inStream.readLine()) != null)
		{
			String[] col = line.split("\t");
			//First column contains the source uri
			String sourceURI = col[0];
			//Third contains the target uri
			String targetURI = col[2];
			//And fifth contains the similarity
			String measure = col[4];
			//Parse it, assuming 1 if a valid measure is not found
			double similarity = 1;
			if(measure != null)
			{
				try{similarity = Double.parseDouble(measure);}
            	catch(Exception ex){/*Do nothing - use the default value*/};
            }
            if(similarity < 0 || similarity > 1)
            	similarity = 1;
            //Get the indexes
            int sourceIndex,targetIndex;
            if(rightOrder)
            {
                //First checking if they are terms
				sourceIndex = source.getTermIndex(sourceURI);
				targetIndex = target.getTermIndex(targetURI);
				if(sourceIndex > -1 && targetIndex > -1)
				{
					add(sourceIndex, targetIndex, similarity);
					continue;
				}
				//If not, check if they are properties
				sourceIndex = source.getPropertyIndex(sourceURI);
				targetIndex = target.getPropertyIndex(targetURI);
				if(sourceIndex > -1 && targetIndex > -1)
					addPropMapping(sourceIndex, targetIndex, similarity);
            }
            else
            {
				sourceIndex = source.getTermIndex(targetURI);
				targetIndex = target.getTermIndex(sourceURI);
				if(sourceIndex > -1 && targetIndex > -1)
				{
					add(sourceIndex, targetIndex, similarity);
					continue;
				}
				sourceIndex = source.getPropertyIndex(targetURI);
				targetIndex = target.getPropertyIndex(sourceURI);
				if(sourceIndex > -1 && targetIndex > -1)
					addPropMapping(sourceIndex, targetIndex, similarity);
            }
		}
		inStream.close();
	}
	
	//Recursive QuickSort implementation that does the actual sorting
	private void quickSort(int begin, int end)
	{
		//If there's something to sort...
		if (end>begin)
    	{
			//Initialize the auxiliary indexes
	    	int left = begin;
	    	int right = end;

	        //Choose a pivot in the middle of the list
	    	double pivot = get((begin+end)/2).getSimilarity();
	    	
			//Then place elements > pivot to the left, < pivot to the right
	    	while(left<=right)
	    	{
	    		//Find the first left element that is smaller than the pivot
	        	while(left<end && get(left).getSimilarity()>pivot)
	        		left++;
	        	//Find the first right element that is larger than the pivot 
	        	while(right>begin && get(right).getSimilarity()<pivot)
	        		right--;
	        	//If two out-of-sort elements were found, swap them
	        	if(left<=right) {
	        		swap(left,right);
	        		//Then update the indexes
	        		left++;
	        		right--;
	        	}
	      	}
		    //Now sort each half of the list
	      	if(begin<right)
	      		quickSort(begin,right);
	      	if(left<end)
	      		quickSort(left,end);
	    }
	}

	//Auxiliary method for sorting that swaps two Mappings in the alignment
	private void swap (int i, int j)
	{
		if(i != j)
		{
			Mapping mi = new Mapping(get(i));
			Mapping mj = new Mapping(get(j));
			termMappings.set(i,mj);
			termMappings.set(j,mi);			
		}
	}
}