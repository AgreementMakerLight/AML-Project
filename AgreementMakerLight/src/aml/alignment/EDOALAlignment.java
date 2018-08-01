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
import java.util.Set;
import java.util.Vector;

import aml.AML;
import aml.alignment.mapping.EDOALMapping;
import aml.alignment.mapping.Mapping;
import aml.alignment.mapping.MappingRelation;
import aml.alignment.mapping.MappingStatus;
import aml.alignment.rdf.AbstractExpression;
import aml.ontology.EntityType;
import aml.ontology.Ontology;
import aml.util.data.Map2List;

public class EDOALAlignment extends Alignment<AbstractExpression>
{

//Attributes

	//The EDOAL Alignment Level
	public static final String LEVEL = "2EDOAL";
	//Mappings organized by ontology entities mentioned in entity1
	private Map2List<String,Mapping<AbstractExpression>> sourceComponentMaps;
	//Mappings organized by entity2
	private Map2List<String,Mapping<AbstractExpression>> targetComponentMaps;
	
	
//Constructors

	/**
	 * Creates a new empty Alignment
	 */
	public EDOALAlignment()
	{
		super();
		sourceComponentMaps = new Map2List<String,Mapping<AbstractExpression>>();
		targetComponentMaps = new Map2List<String,Mapping<AbstractExpression>>();
	}

	/**
	 * Creates a new empty Alignment between the source and target ontologies
	 */
	public EDOALAlignment(Ontology source, Ontology target)
	{
		super(source,target);
		sourceComponentMaps = new Map2List<String,Mapping<AbstractExpression>>();
		targetComponentMaps = new Map2List<String,Mapping<AbstractExpression>>();
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
		//Construct the Mapping
		EDOALMapping m = new EDOALMapping(entity1, entity2, sim, r);
		return add(m);
	}
	
	@Override
	public boolean add(Mapping<AbstractExpression> m)
	{
		boolean isNew = !this.contains(m);
		if(isNew)
		{
			maps.add(m);
			sourceMaps.add(m.getEntity1(), m);
			targetMaps.add(m.getEntity2(), m);
			for(String s : m.getEntity1().getElements())
				sourceComponentMaps.add(s, m);
			for(String t : m.getEntity2().getElements())
				targetComponentMaps.add(t, m);
		}
		else
		{
			Mapping<AbstractExpression> n = (maps.get(this.getIndex(m)));
			if(m.getSimilarity() > n.getSimilarity())
			{
				n.setSimilarity(m.getSimilarity());
				isNew = true;
			}
			if(!m.getRelationship().equals(n.getRelationship()))
			{
				m.setRelationship(n.getRelationship());
				isNew = true;
			}
			if(!m.getStatus().equals(n.getStatus()))
			{
				m.setStatus(n.getStatus());
				isNew = true;
			}
		}
		return isNew;
	}
	
	@Override
	public int cardinality(String uri)
	{
		if(sourceComponentMaps.contains(uri))
			return sourceComponentMaps.get(uri).size();
		if(targetComponentMaps.contains(uri))
			return targetComponentMaps.get(uri).size();
		return 0;
	}

	/**
	 * @param entity1: the source entity to check in the Alignment
	 * @param entity2: the target entity to check in the Alignment
	 * @return whether the Alignment contains a Mapping that includes entity1 and entity2
	 */
	public boolean contains(String entity1, String entity2)
	{
		if(sourceComponentMaps.contains(entity1) && targetComponentMaps.contains(entity2))
		{
			for(Mapping<AbstractExpression> m : sourceComponentMaps.get(entity1))
				if(m.getEntity2().getComponents().contains(entity2))
					return true;
		}
		return false;
	}
	
	/**
 	 * @param m: the Mapping to check in the Alignment 
	 * @return whether the Alignment contains a Mapping that conflicts with the given
	 * Mapping and has a higher similarity
	 */
	public boolean containsBetterMapping(Mapping<AbstractExpression> m)
	{
		Set<String> sources = m.getEntity1().getElements();
		Set<String> targets = m.getEntity2().getElements();
		for(String s : sources)
			for(Mapping<AbstractExpression> n : sourceComponentMaps.get(s))
				if(n.getSimilarity() > m.getSimilarity())
					return true;
		for(String t : targets)
			for(Mapping<AbstractExpression> n : targetComponentMaps.get(t))
				if(n.getSimilarity() > m.getSimilarity())
					return true;
		return false;
	}
	
	@Override
	public boolean containsConflict(Mapping<AbstractExpression> m)
	{
		Set<String> sources = m.getEntity1().getElements();
		Set<String> targets = m.getEntity2().getElements();
		for(String s : sources)
			for(Mapping<AbstractExpression> n : sourceComponentMaps.get(s))
				if(!n.equals(m))
					return true;
		for(String t : targets)
			for(Mapping<AbstractExpression> n : targetComponentMaps.get(t))
				if(!n.equals(m))
					return true;
		return false;
	}
	
	@Override
	public boolean containsSource(String entity1)
	{
		return sourceComponentMaps.contains(entity1);
	}

	@Override
	public boolean containsTarget(String entity2)
	{
		return sourceComponentMaps.contains(entity2);
	}
	
	@Override
	public boolean equals(Object o)
	{
		return o instanceof EDOALAlignment && containsAll((EDOALAlignment)o);
	}
	
	@Override
	public Vector<Mapping<AbstractExpression>> getConflicts(Mapping<AbstractExpression> m)
	{
		Vector<Mapping<AbstractExpression>> conflicts = new Vector<Mapping<AbstractExpression>>();
		Set<String> sources = m.getEntity1().getElements();
		Set<String> targets = m.getEntity2().getElements();
		for(String s : sources)
			for(Mapping<AbstractExpression> n : sourceComponentMaps.get(s))
				if(!n.equals(m) && !conflicts.contains(m))
					conflicts.add(m);
		for(String t : targets)
			for(Mapping<AbstractExpression> n : targetComponentMaps.get(t))
				if(!n.equals(m) && !conflicts.contains(m))
					conflicts.add(m);
		return conflicts;
	}
	
	@Override
	public Set<EntityType> getEntityTypes()
	{
		HashSet<EntityType> types = new HashSet<EntityType>();
		for(String s : sourceComponentMaps.keySet())
			types.addAll(AML.getInstance().getEntityMap().getTypes(s));
		for(String t : targetComponentMaps.keySet())
			types.addAll(AML.getInstance().getEntityMap().getTypes(t));
		return types;
	}
	
	/**
	 * @param uri: the source entity to check in the Alignment
 	 * @return the list of all Mappings where entity1 includes the source entity
	 */
	public Vector<Mapping<AbstractExpression>> getSourceMappings(String uri)
	{
		if(sourceComponentMaps.contains(uri))
			return sourceComponentMaps.get(uri);
		return new Vector<Mapping<AbstractExpression>>();
	}
	
	/**
 	 * @return the list of all source entities involved in mappings
	 */
	public Set<String> getSources()
	{
		return new HashSet<String>(sourceComponentMaps.keySet());
	}

	/**
	 * @return the URI of the source ontology
	 */
	public String getSourceURI()
	{
		return sourceURI;
	}
	
	/**
	 * @param uri: the target entity to check in the Alignment
 	 * @return the list of all Mappings where entity1 includes the source entity
	 */
	public Vector<Mapping<AbstractExpression>> getTargetMappings(String uri)
	{
		if(targetComponentMaps.contains(uri))
			return targetComponentMaps.get(uri);
		return new Vector<Mapping<AbstractExpression>>();
	}
	
	/**
 	 * @return the list of all target entities involved in mappings
	 */
	public Set<String> getTargets()
	{
		return new HashSet<String>(targetComponentMaps.keySet());
	}
	
	/**
	 * @return the URI of the target ontology
	 */
	public String getTargetURI()
	{
		return targetURI;
	}

	/**
	 * @return the maximum cardinality of this Alignment
	 */
	public double maxCardinality()
	{
		double cardinality;
		double max = 0.0;
		Set<String> sources = sourceComponentMaps.keySet();
		for(String i : sources)
		{
			cardinality = sourceComponentMaps.get(i).size();
			if(cardinality > max)
				max = cardinality;
		}
		Set<String> targets = targetComponentMaps.keySet();
		for(String i : targets)
		{
			cardinality = targetComponentMaps.get(i).size();
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
			EDOALMapping m = (EDOALMapping)o;
			AbstractExpression entity1 = m.getEntity1();
			AbstractExpression entity2 = m.getEntity2();
			sourceMaps.remove(entity1, m);
			targetMaps.remove(entity2, m);
			maps.remove(m);
			for(String s : entity1.getElements())
				sourceComponentMaps.remove(s, m);
			for(String t : entity2.getElements())
				targetComponentMaps.remove(t, m);
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
		EDOALMapping m = new EDOALMapping(entity1, entity2, 1.0);
		return remove(m);
	}
	
	@Override
	public int sourceCount()
	{
		return sourceComponentMaps.keyCount();
	}
	
	@Override
	public int targetCount()
	{
		return targetComponentMaps.keyCount();
	}
}