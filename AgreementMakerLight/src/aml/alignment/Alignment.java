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
* An alignment between two Ontologies, represented both as a list of Mappings *
* and as a table of mapped entities.                                          *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.alignment;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import aml.alignment.mapping.Mapping;
import aml.alignment.mapping.MappingRelation;
import aml.alignment.mapping.MappingStatus;
import aml.alignment.rdf.RDFElement;
import aml.ontology.EntityType;
import aml.ontology.Ontology;
import aml.settings.Namespace;
import aml.util.data.Map2List;

public abstract class Alignment<A> implements Collection<Mapping<A>>
{

//Attributes

	//The level of the alignment
	protected String level;
	//The type of the alignment
	protected String type;
	//Ontology info (as listed in alignment file)
	protected String sourceURI;
	protected String sourceLocation;
	protected String sourceFormalismName;
	protected String sourceFormalismURI;
	protected String targetURI;
	protected String targetLocation;
	protected String targetFormalismName;
	protected String targetFormalismURI;
	//Links to the Ontologies mapped in this Alignment
	protected Ontology source;
	protected Ontology target;
	//Mappings organized in list
	protected Vector<Mapping<A>> maps;
	//Mappings organized by entity1
	protected Map2List<A,Mapping<A>> sourceMaps;
	//Mappings organized by entity2
	protected Map2List<A,Mapping<A>> targetMaps;
	
//Constructors

	/**
	 * Creates a new empty Alignment with no ontologies
	 * [Use when you want to manipulate an Alignment
	 * without opening its ontolologies]
	 */
	public Alignment()
	{
		maps = new Vector<Mapping<A>>(0,1);
		sourceMaps = new Map2List<A,Mapping<A>>();
		targetMaps = new Map2List<A,Mapping<A>>();
		level = null;
	}

	/**
	 * Creates a new empty Alignment between the source and target ontologies
	 * @param source: the source ontology
	 * @param target: the target ontology
	 */
	public Alignment(Ontology source, Ontology target)
	{
		this();
		this.source = source;
		this.sourceURI = source.getURI();
		this.target = target;
		this.targetURI = target.getURI();
	}

//Public Methods

	@Override
	public boolean add(Mapping<A> m)
	{
		boolean isNew = !this.contains(m);
		if(isNew)
		{
			maps.add(m);
			sourceMaps.add(m.getEntity1(), m);
			targetMaps.add(m.getEntity2(), m);
		}
		else
		{
			Mapping<A> n = (maps.get(this.getIndex(m)));
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
	public boolean addAll(Collection<? extends Mapping<A>> a)
	{
		boolean check = false;
		for(Mapping<A> m : a)
			check = add(m) || check;
		return check;
	}
	
	/**
	 * Adds all Mappings in a to this Alignment as long as
	 * they don't conflict with any Mapping already present
	 * in this Alignment. In case of any conflict, the inferior
	 * Mapping is removed.
	 * @param a: the collection of Mappings to add to this Alignment
	 */
	public void addAllImprovements(Collection<? extends Mapping<A>> a)
	{
		Vector<Mapping<A>> improvements = new Vector<Mapping<A>>();
		for(Mapping<A> m : a)
			if(!this.containsConflict(m))
				if(!this.containsBetterMapping(m)) {
					this.removeAll(this.getConflicts(m));
					improvements.add(m);
				}
		addAll(improvements);
	}
	
	/**
	 * Adds all Mappings in a to this Alignment as long as
	 * they don't conflict with any Mapping already present
	 * in this Alignment
	 * @param a: the collection of Mappings to add to this Alignment
	 */
	public void addAllNonConflicting(Collection<? extends Mapping<A>> a)
	{
		Vector<Mapping<A>> nonConflicting = new Vector<Mapping<A>>();
		for(Mapping<A> m : a)
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
	public void addAllOneToOne(Alignment<A> a)
	{
		a.sortDescending();
		for(Mapping<A> m : a.maps)
			if(!this.containsConflict(m))
				add(m);
	}
	
	/**
	 * @return the average cardinality of this Alignment
	 */
	public double cardinality()
	{
		return (this.sourceCount()*0.5+this.targetCount()*0.5)/this.size();
	}
	
	/**
	 * @param uri: the uri of the entity to check in the Alignment
	 * @return the cardinality of the entity in the Alignment
	 */
	public abstract int cardinality(String uri);
	
	@Override
	public void clear()
	{
		maps = new Vector<Mapping<A>>(0,1);
		sourceMaps = new Map2List<A,Mapping<A>>();
		targetMaps = new Map2List<A,Mapping<A>>();		
	}
	
	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public boolean contains(Object o)
	{
		return o instanceof Mapping && sourceMaps.contains(((Mapping<A>)o).getEntity1()) &&
				sourceMaps.get(((Mapping<A>)o).getEntity1()).contains((Mapping)o);
	}
	
	/**
	 * @param m: the Mapping to search in the Alignment
	 * @return whether the Alignment contains a mapping equivalent to m but with unknown relation
	 */
	public boolean containsUnknown(Mapping<A> m)
	{
		for(Mapping<A> n : sourceMaps.get(m.getEntity1()))
		{
			if(n.equals(m))
			{
				if(n.getRelationship().equals(MappingRelation.UNKNOWN))
					return true;
				else
					break;
			}
		}
		return false;
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
	public boolean containsBetterMapping(Mapping<A> m)
	{
		if(sourceMaps.contains(m.getEntity1()))
		{
			for(Mapping<A> n : sourceMaps.get(m.getEntity1()))
				if(n.getSimilarity() > m.getSimilarity())
					return true;
		}
		if(targetMaps.contains(m.getEntity2()))
		{
			for(Mapping<A> n : targetMaps.get(m.getEntity2()))
				if(n.getSimilarity() > m.getSimilarity())
					return true;
		}
		return false;
	}
	
	/**
 	 * @param m: the Mapping to check in the Alignment 
	 * @return whether the Alignment contains another Mapping involving either entity in m
	 */
	public boolean containsConflict(Mapping<A> m)
	{
		if(sourceMaps.contains(m.getEntity1()))
		{
			for(Mapping<A> n : sourceMaps.get(m.getEntity1()))
				if(!n.equals(m))
					return true;
		}
		if(targetMaps.contains(m.getEntity2()))
		{
			for(Mapping<A> n : targetMaps.get(m.getEntity2()))
				if(!n.equals(m))
					return true;
		}
		return false;
	}
	
	/**
 	 * @param entity: the entity to check in the Alignment 
	 * @return whether the Alignment contains a Mapping with that entity
	 * (either as entity1 or entity2)
	 */
	public boolean containsEntity(String entity)
	{
		return containsSource(entity) || containsTarget(entity);
	}
	
	/**
	 * @param s: the element of the source Ontology to check in the Alignment
 	 * @return whether the Alignment contains a Mapping for s
	 */
	public abstract boolean containsSource(String s);

	/**
	 * @param t: the element of the target Ontology to check in the Alignment
 	 * @return whether the Alignment contains a Mapping for t
	 */
	public abstract boolean containsTarget(String t);
	
	/**
 	 * @return the number of conflict mappings in this alignment
	 */
	public int countConflicts()
	{
		int count = 0;
		for(Mapping<A> m : maps)
			if(m.getRelationship().equals(MappingRelation.UNKNOWN))
				count++;
		return count;
	}
	
	/**
	 * Removes all mappings in the given Alignment from this Alignment
	 * @param a: the Alignment to subtract from this Alignment
	 */
	public boolean difference(Alignment<A> a)
	{
		boolean check = false;
		for(Mapping<A> m : a.maps)
			check = check || this.maps.remove(m);
		return check;
	}
	
	@Override
	@SuppressWarnings("rawtypes")
	public boolean equals(Object o)
	{
		return o instanceof Alignment && containsAll((Alignment)o);
	}
	
	/**
	 * @param ref: the reference Alignment to evaluate this Alignment
	 * @return the evaluation of this Alignment {# correct mappings, # conflict mappings}
	 */
	public int[] evaluate(Alignment<A> ref)
	{
		int[] count = new int[2];
		for(Mapping<A> m : maps)
		{
			if(ref.containsUnknown(m))
			{
				count[1]++;
				m.setStatus(MappingStatus.UNKNOWN);
			}
			else if(ref.contains(m))
			{
				count[0]++;
				m.setStatus(MappingStatus.CORRECT);
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
	public double gain(Alignment<A> a)
	{
		double gain = 0.0;
		for(Mapping<A> m : maps)
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
	public double gainOneToOne(Alignment<A> a)
	{
		double sourceGain = 0.0;
		for(String i : this.getSources())
			if(!a.containsSource(i))
				sourceGain++;
		sourceGain /= a.sourceCount();
		double targetGain = 0.0;
		for(String i : this.getTargets())
			if(!a.containsTarget(i))
				targetGain++;
		targetGain /= a.targetCount();
		return Math.min(sourceGain, targetGain);
	}
	
	/**
	 * @param index: the index of the Mapping to return in the list of Mappings
 	 * @return the Mapping at the input index (note that the index will change
 	 * during sorting) or null if the uri falls outside the list
	 */
	public Mapping<A> get(int index)
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
	public Mapping<A> get(A entity1, A entity2)
	{
		if(sourceMaps.contains(entity1) && targetMaps.contains(entity2))
			for(Mapping<A> m : sourceMaps.get(entity1))
				if(m.getEntity2().equals(entity2))
					return m;
		return null;
	}
	
	/**
	 * @param m: the Mapping to check on the Alignment
	 * @return the list of all Mappings that have a cardinality conflict with the given Mapping
	 */
	public Vector<Mapping<A>> getConflicts(Mapping<A> m)
	{
		Vector<Mapping<A>> conflicts = new Vector<Mapping<A>>();
		if(sourceMaps.contains(m.getEntity1()))
		{
			for(Mapping<A> n : sourceMaps.get(m.getEntity1()))
				if(!n.equals(m) && !conflicts.contains(m))
					conflicts.add(n);
		}
		if(targetMaps.contains(m.getEntity2()))
		{
			for(Mapping<A> n : targetMaps.get(m.getEntity2()))
				if(!n.equals(m) && !conflicts.contains(m))
					conflicts.add(n);
		}
		return conflicts;
	}
	
	/**
	 * @return the EntityTypes of all entities mapped in this Alignment
	 */
	public abstract Set<EntityType> getEntityTypes();
	
	/**
	 * @param m: the Mapping to search in the Alignment
	 * @return the index of the Mapping
	 */
	public int getIndex(Mapping<A> m)
	{
		for(int i = 0; i < maps.size(); i++)
		{
			if(maps.get(i).equals(m))
				return i;
		}
		return -1;
	}
	
	/**
	 * @param entity1: the entity1 in the Alignment
	 * @param entity2: the entity2 in the Alignment
	 * @return the similarity between entity1 and entity2
	 */
	public double getSimilarity(A entity1, A entity2)
	{
		return get(entity1,entity2).getSimilarity();
	}
	
	/**
	 * @param entity1: the entity1 in the Alignment
	 * @param entity2: the entity2 in the Alignment
	 * @return the similarity between entity1 and entity2 in percentage
	 */
	public String getSimilarityPercent(A entity1, A entity2)
	{
		return get(entity1,entity2).getSimilarityPercent();
	}
	
	/**
	 * @return the name of the formalism of the source ontology
	 */
	public String getSourceFormalismName()
	{
		return sourceFormalismName;
	}

	/**
	 * @return the URI of the formalism of the source ontology
	 */
	public String getSourceFormalismURI()
	{
		return sourceFormalismURI;
	}
	
	/**
	 * @return the location of the source ontology
	 */
	public String getSourceLocation()
	{
		return sourceLocation;
	}
	
	/**
	 * @return the source ontology of this alignment
	 */
	public Ontology getSourceOntology()
	{
		return source;
	}
	
	/**
	 * @return the source entities mapped in this alignment
	 */
	public abstract Set<String> getSources();
	
	/**
	 * @return the URI of the source ontology
	 */
	public String getSourceURI()
	{
		return sourceURI;
	}
	
	/**
	 * @return the name of the formalism of the target ontology
	 */
	public String getTargetFormalismName()
	{
		return targetFormalismName;
	}

	/**
	 * @return the URI of the formalism of the target ontology
	 */
	public String getTargetFormalismURI()
	{
		return targetFormalismURI;
	}
	
	/**
	 * @return the location of the target ontology
	 */
	public String getTargetLocation()
	{
		return targetLocation;
	}
	
	/**
	 * @return the target ontology of this alignment
	 */
	public Ontology getTargetOntology()
	{
		return target;
	}
	
	/**
	 * @return the target entities mapped in this alignment
	 */
	public abstract Set<String> getTargets();
	
	/**
	 * @return the URI of the target ontology
	 */
	public String getTargetURI()
	{
		return targetURI;
	}
	
	/**
	 * @return the type of this Alignment, which is the 
	 * two-character string, either originally provided
	 * for the Alignment, or one generated automatically
	 * using the following notation:
	 * "1" for injective and total
	 * "?" for injective
	 * "+" for total
	 * "*" for neither injective nor total
	 */
	public String getType()
	{
		if(type == null)
		{
			type = "";
			double sourceCard = maps.size() * 1.0 / sourceCount();
			double sourceCov = sourceCoverage();
			if(sourceCard <= 1.1)
			{
				if(sourceCov >= 0.9)
					type += "1";
				else
					type += "?";
			}
			else if(sourceCov >= 0.9)
				type += "+";
			else
				type += "*";
			double targetCard = maps.size() * 1.0 / targetCount();
			double targetCov = targetCoverage();
			if(targetCard <= 1.1)
			{
				if(targetCov >= 0.9)
					type += "1";
				else
					type += "?";
			}
			else if(targetCov >= 0.9)
				type += "+";
			else
				type += "*";

		}
		return type;
		
	}

	@Override
	public int hashCode()
	{
		return maps.hashCode();
	}
	
	/**
	 * Intersects this Alignment with a given Aligmment, retaining only
	 * the common mappings
	 * @param a: the Alignment to intersect with this Alignment 
	 */
	public boolean intersection(Alignment<A> a)
	{
		HashSet<Mapping<A>> toRemove = new HashSet<Mapping<A>>();
		for(Mapping<A> m : a)
			if(!this.contains(m))
				toRemove.add(m);
		for(Mapping<A> m : this)
			if(!a.contains(m))
				toRemove.add(m);
		return this.removeAll(toRemove);
	}
	
	@Override
	public boolean isEmpty()
	{
		return maps.isEmpty();
	}
	
	@Override
	public Iterator<Mapping<A>> iterator()
	{
		return maps.iterator();
	}
	
	/**
	 * @return the maximum cardinality of this Alignment
	 */
	public abstract double maxCardinality();
	
	@Override
	@SuppressWarnings("unchecked")
	public boolean remove(Object o)
	{
		if(o instanceof Mapping)
		{			
			Mapping<A> m = (Mapping<A>)o;
			sourceMaps.remove(m.getEntity1(), m);
			targetMaps.remove(m.getEntity2(), m);
			return maps.remove(o);
		}
		return false;
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
		for(Mapping<A> m : this)
			if(!c.contains(m))
				check = remove(m) || check;
		return check;
	}

	/**
	 * Sets the name of the formalism of the source ontology
	 * @param s: the name to set
	 */
	public void setSourceFormalismName(String s)
	{
		sourceFormalismName = s;
	}

	/**
	 * Sets the URI of the formalism of the source ontology
	 * @param s: the URI to set
	 */
	public void setSourceFormalismURI(String s)
	{
		sourceFormalismURI = s;
	}
	
	/**
	 * Sets the location of the source ontology
	 * @param s: the location to set
	 */
	public void setSourceLocation(String s)
	{
		sourceLocation = s;
	}
	
	/**
	 * Sets the URI of the source ontology (only if the
	 * Alignment holds no link to the Ontology)
	 * @param s: the URI to set
	 */
	public void setSourceURI(String s)
	{
		if(source != null)
			sourceURI = s;
	}
	
	/**
	 * Sets the name of the formalism of the target ontology
	 * @param s: the name to set
	 */
	public void setTargetFormalismName(String s)
	{
		targetFormalismName = s;
	}

	/**
	 * Sets the URI of the formalism of the target ontology
	 * @param s: the URI to set
	 */
	public void setTargetFormalismURI(String s)
	{
		targetFormalismURI = s;
	}
	
	/**
	 * Sets the location of the target ontology
	 * @param s: the location to set
	 */
	public void setTargetLocation(String s)
	{
		targetLocation = s;
	}
	
	/**
	 * Sets the URI of the target ontology (only if the
	 * Alignment holds no link to the Ontology)
	 * @param s: the URI to set
	 */
	public void setTargetURI(String s)
	{
		if(target != null)
			targetURI = s;
	}
	
	/**
	 * Sets the type of the alignment
	 * @param type: the alignment type, a two-character string
	 * recommended to use the following notation:
	 * "1" for injective and total
	 * "?" for injective
	 * "+" for total
	 * "*" for neither injective nor total
	 * Alternatively "1m", "n1", and "nm" may also be used 
	 */
	public void setType(String type)
	{
		this.type = type;
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
		Collections.sort(maps,new Comparator<Mapping<A>>()
        {
			//Sorting in descending order can be done simply by
			//reversing the order of the elements in the comparison
            public int compare(Mapping<A> m1, Mapping<A> m2)
            {
        		return m2.compareTo(m1);
            }
        } );
	}
	
	/**
	 * @return the number of entity1 mapped in this Alignment
	 */
	public int sourceCount()
	{
		return getSources().size();
	}
	
	/**
	 * @return the fraction of entities from the source ontology
	 * mapped in this Alignment (counting only entity types that
	 * are mapped)
	 */
	public double sourceCoverage()
	{
		if(source == null)
			return 0;
		int count = 0;
		for(EntityType e : this.getEntityTypes())
			count += source.count(e);
		return sourceCount()*1.0/count;
	}
	
	/**
	 * @return the number of entity2 mapped in this Alignment
	 */
	public int targetCount()
	{
		return getTargets().size();
	}
	
	/**
	 * @return the fraction of entities from the target ontology
	 * mapped in this Alignment (counting only entity types that
	 * are mapped)
	 */
	public double targetCoverage()
	{
		if(target == null)
			return 0;
		int count = 0;
		for(EntityType e : this.getEntityTypes())
			count += target.count(e);
		return targetCount()*1.0/count;
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
	
	@SuppressWarnings("rawtypes")
	public String toRDF()
	{
		String s = "<?xml version='1.0' encoding='utf-8'?>\n" +
				"<rdf:RDF xmlns='" + Namespace.ALIGNMENT.uri + "'\n" + 
				"xmlns:rdf='" + Namespace.RDF.uri + "'\n" + 
				"xmlns:xsd='" + Namespace.XSD.uri + "'";
		if(level.equals(EDOALAlignment.LEVEL))
			s += "\nxmlns:xsd='" + Namespace.EDOAL.uri + "'";
		s += ">\n";
		s += "<" + RDFElement.ALIGNMENT_ + ">\n" +
				"<" + RDFElement.XML + ">yes</" + RDFElement.XML +">\n" +
				"<" + RDFElement.LEVEL + ">" + level + "</" + RDFElement.LEVEL + ">\n" +
				"<" + RDFElement.TYPE + ">" + type + "</" + RDFElement.TYPE + ">\n";
		if(sourceURI != null && targetURI != null)
		{
			if(sourceLocation == null && sourceFormalismName == null && sourceFormalismURI != null)
				s += "<" + RDFElement.ONTO1 + ">" + sourceURI + "</" + RDFElement.ONTO1 + ">\n";
			else
			{
				s += "<" + RDFElement.ONTO1 + ">\n" +
						"<" + RDFElement.ONTOLOGY_ + " " + RDFElement.RDF_ABOUT.toRDF() +
						"=\"" + sourceURI + "\">\n";
				if(sourceLocation != null)
					s += "<" + RDFElement.LOCATION + ">" + sourceLocation + "</" + RDFElement.LOCATION + ">\n";
				if(sourceFormalismName != null || sourceFormalismURI != null)
				{
					s += "<" + RDFElement.FORMALISM + ">\n" +
							"<" + RDFElement.FORMALISM_;
					if(sourceFormalismName != null)
						s += " " + RDFElement.NAME + "=\"" + sourceFormalismName + "\"";
					if(sourceFormalismURI != null)
						s += " " + RDFElement.URI + "=\"" + sourceFormalismURI + "\"";
					s += "/>\n" +
							"</" + RDFElement.FORMALISM + ">\n";

				}
				s += "</" + RDFElement.ONTOLOGY + ">\n" +
						"</" + RDFElement.ONTO1 + ">\n";
			}
			if(targetLocation == null && targetFormalismName == null && targetFormalismURI != null)
				s += "<" + RDFElement.ONTO2 + ">" + targetURI + "</" + RDFElement.ONTO2 + ">\n";
			else
			{
				s += "<" + RDFElement.ONTO2 + ">\n" +
						"<" + RDFElement.ONTOLOGY_ + " " + RDFElement.RDF_ABOUT.toRDF() +
						"=\"" + targetURI + "\">\n";
				if(targetLocation != null)
					s += "<" + RDFElement.LOCATION + ">" + targetLocation + "</" + RDFElement.LOCATION + ">\n";
				if(targetFormalismName != null || targetFormalismURI != null)
				{
					s += "<" + RDFElement.FORMALISM + ">\n" +
							"<" + RDFElement.FORMALISM_;
					if(targetFormalismName != null)
						s += " " + RDFElement.NAME + "=\"" + targetFormalismName + "\"";
					if(targetFormalismURI != null)
						s += " " + RDFElement.URI + "=\"" + targetFormalismURI + "\"";
					s += "/>\n" +
							"</" + RDFElement.FORMALISM + ">\n";

				}
				s += "</" + RDFElement.ONTOLOGY + ">\n" +
						"</" + RDFElement.ONTO2 + ">\n";
			}
		}
		for(Mapping m : this)
			s += m.toRDF() + "\n";
		s += "</" + RDFElement.ALIGNMENT_ + ">\n" +
			"</rdf:RDF>";
		return s;
	}
}