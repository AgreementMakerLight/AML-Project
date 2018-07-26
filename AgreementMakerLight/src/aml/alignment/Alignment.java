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
* An alignment between two Ontologies, represented as a list of Mappings.     *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.alignment;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Vector;

import aml.AML;
import aml.ontology.EntityType;

public abstract class Alignment implements Collection<Mapping>
{

//Attributes

	//Ontology uris (as listed in alignment file)
	protected String sourceURI;
	protected String targetURI;
	//Mappings organized in list
	protected Vector<Mapping> maps;
	
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

	@Override
	public boolean add(Mapping m)
	{
		return maps.add(m);
	}

	@Override
	public boolean addAll(Collection<? extends Mapping> a)
	{
		return maps.addAll(a);
	}
	
	/**
	 * Adds all Mappings in a to this Alignment as long as
	 * they don't conflict with any Mapping already present
	 * in this Alignment
	 * @param a: the collection of Mappings to add to this Alignment
	 */
	public void addAllNonConflicting(Collection<? extends Mapping> a)
	{
		Vector<Mapping> nonConflicting = new Vector<Mapping>();
		for(Mapping m : a)
			if(!this.containsConflict(m))
				nonConflicting.add(m);
		addAll(nonConflicting);
	}
	
	/**
	 * Adds all Mappings in a to this Alignment in descending
	 * order of similarity, as long as they don't conflict with
	 * any Mapping already present or previously added to this
	 * Alignment
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
	public abstract double cardinality();
	
	@Override
	public void clear()
	{
		maps = new Vector<Mapping>(0,1);
	}
	
	@Override
	public boolean contains(Object o)
	{
		return o instanceof Mapping && maps.contains((SimpleMapping)o);
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
 	 * @param m: the Mapping to check in the Alignment 
	 * @return whether the Alignment contains a Mapping that conflicts with the given
	 * Mapping and has a higher similarity
	 */
	public abstract boolean containsBetterMapping(Mapping m);
	
	/**
 	 * @param m: the Mapping to check in the Alignment 
	 * @return whether the Alignment contains another Mapping involving either entity in m
	 */
	public abstract boolean containsConflict(Mapping m);
	
	/**
 	 * @param entity: the entity to check in the Alignment 
	 * @return whether the Alignment contains a Mapping with that entity
	 * (either as entity1 or entity2)
	 */
	public boolean containsEntity(Object entity)
	{
		return containsSource(entity) || containsTarget(entity);
	}
	
	/**
	 * @param entity1: the entity1 to check in the Alignment
 	 * @return whether the Alignment contains a Mapping for entity1
	 */
	public abstract boolean containsSource(Object entity1);

	/**
	 * @param entity2: the entity2 to check in the Alignment
 	 * @return whether the Alignment contains a Mapping for entity1
	 */
	public abstract boolean containsTarget(Object entity2);
	
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
	public abstract Alignment difference(Alignment a);
	
	@Override
	public boolean equals(Object o)
	{
		return o instanceof Alignment && containsAll((Alignment)o);
	}
	
	/**
	 * @param ref: the reference Alignment to evaluate this Alignment
	 * @return the evaluation of this Alignment {# correct mappings, # conflict mappings}
	 */
	public abstract int[] evaluate(Alignment ref);

	/**
	 * @param a: the base Alignment to which this Alignment will be compared 
	 * @return the gain (i.e. the fraction of new Mappings) of this Alignment
	 * in comparison with the base Alignment
	 */
	public double gain(Alignment a)
	{
		double gain = 0.0;
		for(Mapping m : maps)
			if(!a.contains(m))
				gain++;
		gain /= a.size();
		return gain;
	}
	
	/**
	 * @param a: the base Alignment to which this Alignment will be compared 
	 * @return the gain (i.e. the fraction of new Mappings) of this Alignment
	 * in comparison with the base Alignment
	 */
	public abstract double gainOneToOne(Alignment a);
	
	/**
	 * @param index: the index of the Mapping to return in the list of Mappings
 	 * @return the Mapping at the input index (note that the index will change
 	 * during sorting) or null if the uri falls outside the list
	 */
	public Mapping get(int index)
	{
		if(index < 0 || index >= maps.size())
			return null;
		return maps.get(index);
	}
	
	/**
	 * @param m: the Mapping to check on the Alignment
	 * @return the list of all Mappings that have a cardinality conflict with the given Mapping
	 */
	public abstract Vector<Mapping> getConflicts(Mapping m);
	
	/**
	 * @param m: the Mapping to search in the Alignment
	 * @return the index of the Mapping
	 */
	public int getIndex(Mapping m)
	{
		for(int i = 0; i < maps.size(); i++)
		{
			if(maps.get(i).equals(m))
				return i;
		}
		return -1;
	}
	
	/**
	 * @return the URI of the source ontology
	 */
	public String getSourceURI()
	{
		return sourceURI;
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
	public abstract Alignment intersection(Alignment a);
	
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
	public abstract double maxCardinality();
	
	@Override
	public boolean remove(Object o)
	{
		return maps.remove(o);
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
	 * @return the number of entity1 mapped in this Alignment
	 */
	public abstract int sourceCount();
	
	/**
	 * @param e: the EntityType to search in the Alignment
	 * @return the fraction of entity1 of the given type mapped in this Alignment
	 */
	public abstract double sourceCoverage(EntityType e);
	
	/**
	 * @return the number of entity2 mapped in this Alignment
	 */
	public abstract int targetCount();
	
	/**
	 * @param e: the EntityType to search in the Alignment
	 * @return the fraction of entity2 of the given type mapped in this Alignment
	 */
	public abstract double targetCoverage(EntityType e);
	
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