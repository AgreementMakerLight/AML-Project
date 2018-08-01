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
import aml.alignment.mapping.Mapping;
import aml.alignment.mapping.MappingRelation;
import aml.alignment.mapping.MappingStatus;
import aml.alignment.mapping.SimpleMapping;
import aml.ontology.EntityType;
import aml.ontology.Ontology;
import aml.ontology.semantics.EntityMap;

public class SimpleAlignment extends Alignment<String>
{

//Attributes

	//The level of the alignment
	public final String LEVEL = "0";
	
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
	
	@Override
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
		if(!rels.getTypes(entity1).contains(EntityType.CLASS))
			return false;
		
		Set<String> sourceAncestors = rels.getSuperclasses(entity1);
		Set<String> targetAncestors = rels.getSuperclasses(entity2);
		
		for(String sa : sourceAncestors)
			for(String ta : targetAncestors)
				if(contains(sa,ta))
					return true;
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
			for(String ta : targetDescendants)
				if(contains(sa,ta))
					return true;
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
		
		Set<String> sourceAncestors = rels.getSuperclasses(entity1,1);
		Set<String> targetAncestors = rels.getSuperclasses(entity2,1);
		
		for(String sa : sourceAncestors)
			if(contains(sa,entity2))
				return true;
		for(String ta : targetAncestors)
			if(contains(entity1,ta))
				return true;
		return false;
	}
	
	@Override
	public boolean containsSource(String entity1)
	{
		return sourceMaps.contains(entity1);
	}

	@Override
	public boolean containsTarget(String entity2)
	{
		return targetMaps.contains(entity2);
	}
	
	@Override
	public boolean equals(Object o)
	{
		return o instanceof SimpleAlignment && containsAll((SimpleAlignment)o);
	}
	
	@Override
	public Set<EntityType> getEntityTypes()
	{
		HashSet<EntityType> types = new HashSet<EntityType>();
		for(Mapping<String> m : maps)
		{
			types.addAll(AML.getInstance().getEntityMap().getTypes(m.getEntity1()));
			types.addAll(AML.getInstance().getEntityMap().getTypes(m.getEntity2()));
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
		for(Mapping<String> m : maps)
		{
			Set<String> sourceAncestors = rels.getHighLevelAncestors(m.getEntity1());
			Set<String> targetAncestors = rels.getHighLevelAncestors(m.getEntity2());
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
		for(Mapping<String> m : a)
			if(m.getSimilarity() >= 0.01)
				b.add(m);
		return b;
	}
	
	/**
	 * @param entity1: the entity1
	 * @param entity2: the entity2
	 * @return the index of the Mapping between the given classes in
	 * the list of Mappings, or -1 if the Mapping doesn't exist
	 */
	public int getIndex(String entity1, String entity2)
	{
		return maps.indexOf(new SimpleMapping(entity1,entity2,1.0));
	}
	
	/**
	 * @param entity1: the entity1 to check in the Alignment
 	 * @return the list of all Mappings including entity1
	 */
	public Vector<Mapping<String>> getSourceMappings(String entity1)
	{
		if(sourceMaps.contains(entity1))
			return sourceMaps.get(entity1);
		return new Vector<Mapping<String>>();
	}
	
	@Override
	public Set<String> getSources()
	{
		return new HashSet<String>(sourceMaps.keySet());
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
 	 * @return the list of all Mappings including entity2
	 */
	public Vector<Mapping<String>> getTargetMappings(String entity2)
	{
		if(targetMaps.contains(entity2))
			return targetMaps.get(entity2);
		return new Vector<Mapping<String>>();
	}
	
	/**
 	 * @return the list of all entity2 classes that have mappings
	 */
	public Set<String> getTargets()
	{
		return new HashSet<String>(targetMaps.keySet());
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
	 * @return the maximum cardinality of this Alignment
	 */
	public double maxCardinality()
	{
		double cardinality;
		double max = 0.0;
		Set<String> sources = sourceMaps.keySet();
		for(String i : sources)
		{
			cardinality = sourceMaps.get(i).size();
			if(cardinality > max)
				max = cardinality;
		}
		Set<String> targets = targetMaps.keySet();
		for(String i : targets)
		{
			cardinality = targetMaps.get(i).size();
			if(cardinality > max)
				max = cardinality;
		}
		return max;		
	}
	
	/**
	 * Removes the Mapping between the given classes from the Alignment
	 * @param entity1: the entity1 class to remove from the Alignment
	 * @param entity2: the entity2 class to remove from the Alignment
	 */
	public boolean remove(String entity1, String entity2)
	{
		SimpleMapping m = new SimpleMapping(entity1, entity2, 1.0);
		return remove(m);
	}	
}