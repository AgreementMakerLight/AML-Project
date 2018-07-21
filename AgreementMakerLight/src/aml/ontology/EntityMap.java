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
* Registry of entities in the ontologies opened by AML and all of their       *
* semantics, including relationships, restrictions, properties, etc...        *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.ontology;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import aml.AML;
import aml.util.table.Map2Set;
import aml.util.table.Map2Map;
import aml.util.table.Map2Map2Set;


public class EntityMap
{
	
//Attributes

	//The map of entities to EntityTypes 
	private Map2Set<String,EntityType> entityType;
	
	//Relationships between classes / class expressions
	//Subclass & equivalence relations (with transitive closure, but min distance only)
	private Map2Map<String,String,Integer> ancestorClasses; //Class -> Ancestor -> Distance
	private Map2Map<String,String,Integer> descendantClasses; //Class -> Descendant -> Distance
	//Disjointness (direct only, no transitive closure)
	private Map2Set<String,String> disjointMap; //Class -> Disjoint Classes
	//List of high level classes
	private HashSet<String> highLevelClasses;
	
	//Relationships between individuals and classes
	private Map2Set<String,String> instanceOfMap; //Individual -> Class 
	private Map2Set<String,String> hasInstanceMap; //Class -> Individual

	//Relationships between individuals
	private Map2Map2Set<String,String,String> activeRelation; //Source Individual -> Target Individual -> Property
	private Map2Map2Set<String,String,String> passiveRelation; //Target Individual -> Source Individual -> Property

	//Relationships between properties
	//Hierarchical and inverse relations
	private Map2Set<String,String> subProp; //Property -> SubProperty
	private Map2Set<String,String> superProp; //Property -> SuperProperty
	private Map2Set<String,String> inverseProp; //Property -> InverseProperty
	//Transitivity (transitive: P * P = P; transitive over: P * Q = P)
	private Map2Set<String,String> transitiveOver; //Property1 -> Property2 over which 1 is transitive
	//Other (more complex) property chains (e.g. father * father = grandfather)
	private Map2Set<String,Vector<String>> propChain; //Property -> Property Chain
	
	//Properties of properties
	//List of asymmetric properties
	private HashSet<String> asymmetric;
	//List of functional properties
	private HashSet<String> functional;
	//List of irreflexive properties
	private HashSet<String> irreflexive;
	//List of symmetric properties
	private HashSet<String> reflexive;
	//List of symmetric properties
	private HashSet<String> symmetric;
	//Property domains and ranges (property to class or to String)
	private Map2Set<String,String> domain; //Property -> Domain (Class)
	private Map2Set<String,String> range; //Property -> Range (Class/Datatype)
	
//Constructors

	/**
	 * Creates a new empty RelationshipMap
	 */
	public EntityMap()
	{
		entityType = new Map2Set<String,EntityType>();
		descendantClasses = new Map2Map<String,String,Integer>();
		ancestorClasses = new Map2Map<String,String,Integer>();
		disjointMap = new Map2Set<String,String>();
		instanceOfMap = new Map2Set<String,String>();
		hasInstanceMap = new Map2Set<String,String>();
		activeRelation = new Map2Map2Set<String,String,String>();		
		passiveRelation = new Map2Map2Set<String,String,String>();		
		subProp = new Map2Set<String,String>();
		superProp = new Map2Set<String,String>();
		inverseProp = new Map2Set<String,String>();
		transitiveOver = new Map2Set<String,String>();
		asymmetric = new HashSet<String>();
		functional = new HashSet<String>();
		irreflexive = new HashSet<String>();
		reflexive = new HashSet<String>();
		symmetric = new HashSet<String>();
		domain = new Map2Set<String,String>();
		range = new Map2Set<String,String>();
	}
	
//Public Methods

	/**
	 * @param prop: the property to set as asymmetric
	 */
	public void addAsymmetric(String prop)
	{
		asymmetric.add(prop);
	}
	/**
	 * Adds a relationship between two classes with a given distance
	 * @param child: the uri of the child class
	 * @param parent: the uri of the parent class
	 * @param distance: the distance (number of edges) between the classes
	 */
	public void addClassRelationship(String child, String parent, int distance)
	{
		descendantClasses.add(parent,child,distance);
		ancestorClasses.add(child,parent,distance);
	}
		
	/**
	 * Adds a new disjointness relations between two classes
	 * @param class1: the uri of the first disjoint class
	 * @param class2: the uri of the second disjoint class
	 */
	public void addDisjoint(String class1, String class2)
	{
		if(class1 != class2 && !areDisjoint(class1,class2))
		{
			//The disjointMap keeps disjoint clauses in both directions
			disjointMap.add(class1, class2);
			disjointMap.add(class2, class1);
		}
	}
	
	/**
	 * Adds a new domain (class) to a given property
	 * @param propId: the uri of the property with the domain
	 * @param uri: the uri of the class in the domain of the property
	 */
	public void addDomain(String propId, String uri)
	{
		domain.add(propId, uri);
	}
	
	/**
	 * Adds an equivalence relationship between two classes
	 * @param class1: the uri of the first equivalent class
	 * @param class2: the uri of the second equivalent class
	 */
	public void addEquivalence(String class1, String class2)
	{
		addClassRelationship(class1,class2,0);
	}
	
	/**
	 * @param prop: the property to set as functional
	 */
	public void addFunctional(String prop)
	{
		functional.add(prop);
	}
	
	/**
	 * Adds a relationship between two individuals through a given property
	 * @param indiv1: the uri of the first individual
	 * @param indiv2: the uri of the second individual
	 * @param prop: the property in the relationship
	 */
	public void addIndividualRelationship(String indiv1, String indiv2, String prop)
	{
		activeRelation.add(indiv1,indiv2,prop);
		passiveRelation.add(indiv2,indiv1,prop);
	}
	
	/**
	 * Adds an instantiation relationship between an individual and a class
	 * @param individualId: the uri of the individual
	 * @param uri: the uri of the class
	 */
	public void addInstance(String individualId, String uri)
	{
		instanceOfMap.add(individualId,uri);
		hasInstanceMap.add(uri,individualId);
	}
	
	/**
	 * Adds a new inverse relationship between two properties if it doesn't exist
	 * @param property1: the uri of the first property
	 * @param property2: the uri of the second property
	 */
	public void addInverseProp(String property1, String property2)
	{
		if(!property1.equals(property2))
		{
			inverseProp.add(property1, property2);
			inverseProp.add(property2, property1);
		}
	}
	
	/**
	 * @param prop: the property to set as irreflexive
	 */
	public void addIrreflexive(String prop)
	{
		irreflexive.add(prop);
	}
	
	/**
	 * Adds a new range (class) to a given object property
	 * @param propId: the uri of the property with the range
	 * @param uri: the uri of the class in the range of the property
	 */
	public void addRange(String propId, String uri)
	{
		range.add(propId, uri);
	}
	
	/**
	 * @param prop: the property to set as reflexive
	 */
	public void addReflexive(String prop)
	{
		reflexive.add(prop);
	}
	
	/**
	 * Adds a direct hierarchical relationship between two classes
	 * @param child: the uri of the child class
	 * @param parent: the uri of the parent class
	 */
	public void addSubclass(String child, String parent)
	{
		addClassRelationship(child,parent,1);
	}

	/**
	 * Adds a relationship between two properties
	 * @param child: the uri of the child property
	 * @param parent: the uri of the parent property
	 */
	public void addSubProperty(String child, String parent)
	{
		subProp.add(parent,child);
		superProp.add(child,parent);
	}
	
	/**
	 * @param prop: the property to set as symmetric
	 */
	public void addSymmetric(String prop)
	{
		symmetric.add(prop);
	}
	
	/**
	 * @param prop: the property to set as transitive
	 */
	public void addTransitive(String prop)
	{
		transitiveOver.add(prop,prop);
	}
	
	/**
	 * @param prop1: the property to set as transitive over prop2
	 * @param prop2: the property over which prop1 is transitive
	 */
	public void addTransitiveOver(String prop1, String prop2)
	{
		transitiveOver.add(prop1,prop2);
	}
	
	/**
	 * @param uri: the uri to add to AML
	 */
	public void addURI(String uri, EntityType t)
	{
		entityType.add(uri,t);
	}
	
	/**
	 * @param class1: the first class to check for disjointness
	 * @param class2: the second class to check for disjointness
	 * @return whether one and two are disjoint considering transitivity
	 */
	public boolean areDisjoint(String class1, String class2)
	{
		//Get the transitive disjoint clauses involving class one
		Set<String> disj = getDisjointTransitive(class1);
		if(disj.size() > 0)
		{
			//Then get the list of superclasses of class two
			Set<String> ancs = getSuperClasses(class2,false);
			//Including class two itself
			ancs.add(class2);
		
			//Two classes are disjoint if the list of transitive disjoint clauses
			//involving one of them contains the other or any of its 'is_a' ancestors
			for(String i : ancs)
				if(disj.contains(i))
					return true;
		}
		return false;
	}
	
	/**
	 * @param child: the uri of the child class
	 * @param parent: the uri of the parent class
	 * @return whether the RelationshipMap contains a relationship between child and parent
	 */
	public boolean areRelatedClasses(String child, String parent)
	{
		return descendantClasses.contains(parent,child);
	}
	
	/**
	 * Checks whether an individual belongs to a class
	 * @param indivId: the uri of the individual to check
	 * @param uri: the uri of the class to check
	 * @return whether indivId is an instance of uri or
	 * of one of its subclasses
	 */
	public boolean belongsToClass(String indivId, String uri)
	{
		if(instanceOfMap.contains(indivId, uri))
			return true;
		for(String suburi : getSubClasses(uri,false))
			if(instanceOfMap.contains(indivId, suburi))
				return true;
		return false;	
	}
	
	/**
	 * @param uri: the uri to search in the EntityMap
	 * @return whether the EntityMap contains the uri
	 */
	public boolean contains(String uri)
	{
		return entityType.contains(uri);
	}
	
	/**
	 * @return the number of disjoint clauses
	 */
	public int disjointCount()
	{
		//The size is divided by 2 since the disjoint
		//clauses are stored in both directions
		return disjointMap.size()/2;
	}
	
	/**
	 * @return the number of entities registered in the EntityMap
	 */
	public int entityCount()
	{
		return entityType.keyCount();
	}
	
	/**
	 * @param uri: the id of the class to search in the map
	 * @return the list of ancestors of the given class
	 */
	public Set<String> getAncestors(String uri)
	{
		if(ancestorClasses.contains(uri))
			return ancestorClasses.keySet(uri);
		return new HashSet<String>();
	}
	
	/**
	 * @param uri: the uri of the class to search in the map
	 * @param distance: the distance between the class and its ancestors
	 * @return the list of ancestors at the given distance from the input class
	 */
	public Set<String> getAncestors(String uri, int distance)
	{
		HashSet<String> asc = new HashSet<String>();
		if(!ancestorClasses.contains(uri))
			return asc;
		for(String i : ancestorClasses.keySet(uri))
			if(ancestorClasses.get(uri,i) == distance)
				asc.add(i);
		return asc;
	}
	
	/**
	 * @return the set of classes with ancestors in the map
	 */
	public Set<String> getChildren()
	{
		if(ancestorClasses != null)
			return ancestorClasses.keySet();
		return new HashSet<String>();
	}
	
	/**
	 * @param uri: the id of the class to search in the map
	 * @return the list of direct children of the given class
	 */
	public Set<String> getChildren(String uri)
	{
		return getDescendants(uri,1);
	}

	/**
	 * @param uri: the id of the class to search in the map
	 * @return the list of individuals that instantiate the given class
	 */
	public Set<String> getClassIndividuals(String uri)
	{
		if(hasInstanceMap.contains(uri))
			return hasInstanceMap.get(uri);
		return new HashSet<String>();
	}
	
	/**
	 * @param classes: the set the class to search in the map
	 * @return the list of direct subclasses shared by the set of classes
	 */
	public Set<String> getCommonSubClasses(Set<String> classes)
	{
		if(classes == null || classes.size() == 0)
			return null;
		Iterator<String> it = classes.iterator();
		Vector<String> subclasses = new Vector<String>(getSubClasses(it.next(),false));
		while(it.hasNext())
		{
			HashSet<String> s = new HashSet<String>(getSubClasses(it.next(),false));
			for(int i = 0; i < subclasses.size(); i++)
			{
				if(!s.contains(subclasses.get(i)))
				{
					subclasses.remove(i);
					i--;
				}
			}
		}
		for(int i = 0; i < subclasses.size()-1; i++)
		{
			for(int j = i+1; j < subclasses.size(); j++)
			{
				if(isSubclass(subclasses.get(i),subclasses.get(j)))
				{
					subclasses.remove(i);
					i--;
					j--;
				}
				if(isSubclass(subclasses.get(j),subclasses.get(i)))
				{
					subclasses.remove(j);
					j--;
				}
			}
		}
		return new HashSet<String>(subclasses);
	}
	
	/**
	 * @param propId: the id of the property to search in the map
	 * @return the list of classes / datatypes in the range of the input property
	 */
	public Set<String> getRanges(String propId)
	{
		if(range.contains(propId))
			return range.get(propId);
		return new HashSet<String>();
	}
	
	/**
	 * @param uri: the id of the class to search in the map
	 * @return the list of descendants of the input class
	 */
	public Set<String> getDescendants(String uri)
	{
		if(descendantClasses.contains(uri))
			return descendantClasses.keySet(uri);
		return new HashSet<String>();
	}
	
	/**
	 * @param uri: the id of the class to search in the map
	 * @param distance: the distance between the class and its ancestors
	 * @return the list of descendants at the given distance from the input class
	 */
	public Set<String> getDescendants(String uri, int distance)
	{
		HashSet<String> desc = new HashSet<String>();
		if(!descendantClasses.contains(uri))
			return desc;
		for(String i : descendantClasses.keySet(uri))
			if(descendantClasses.get(uri, i) == distance)
				desc.add(i);
		return desc;
	}
	
	/**
	 * @return the set of classes that have disjoint clauses
	 */
	public Set<String> getDisjoint()
	{
		return disjointMap.keySet();
	}

	/**
	 * @param uri: the id of the class to search in the map
	 * @return the list of classes disjoint with the given class
	 */
	public Set<String> getDisjoint(String uri)
	{
		if(disjointMap.contains(uri))
			return disjointMap.get(uri);
		return new HashSet<String>();
	}
	
	/**
	 * @param uri: the id of the class to search in the map
	 * @return the list of classes disjoint with the given class
	 * or any of its ancestors
	 */
	public Set<String> getDisjointTransitive(String uri)
	{
		//Get the disjoint clauses for the class
		Set<String> disj = getDisjoint(uri);
		//Then get all superclasses of the class
		Set<String> ancestors = getSuperClasses(uri,false);
		//For each superclass
		for(String i : ancestors)
			//Add its disjoint clauses to the list
			disj.addAll(getDisjoint(i));
		return disj;
	}
	
	/**
	 * @param child: the uri of the child class
	 * @param parent: the uri of the parent class
	 * @return the minimal distance between the child and parent,
	 * or 0 if child==parent, or -1 if they aren't related
	 */
	public int getDistance(String child, String parent)
	{
		if(child.equals(parent))
			return 0;
		if(!ancestorClasses.contains(child, parent))
			return -1;
		return ancestorClasses.get(child,parent);
	}
	
	/**
	 * @param propId: the id of the property to search in the map
	 * @return the list of class indexes in the domain of the input property
	 */
	public Set<String> getDomains(String propId)
	{
		if(domain.contains(propId))
			return domain.get(propId);
		return new HashSet<String>();
	}
	
	/**
	 * @param uri: the id of the class to search in the map
	 * @return the list of equivalences of the given class
	 */
	public Set<String> getEquivalences(String uri)
	{
		return getDescendants(uri, 0);
	}
	
	/**
	 * @param uri: the id of the class to search in the map
	 * @return the list of classes equivalent to the given class
	 */
	public Set<String> getEquivalentClasses(String uri)
	{
		HashSet<String> equivs = new HashSet<String>();
		for(String e : getEquivalences(uri))
			if(isClass(e))
				equivs.add(e);
		return equivs;
	}
	
	/**
	 * @param uri: the id of the class to search in the map
	 * @return the list of high level ancestors of the given class
	 */
	public Set<String> getHighLevelAncestors(String uri)
	{
		if(highLevelClasses == null)
			getHighLevelClasses();
		Set<String> ancestors = getAncestors(uri);
		HashSet<String> highAncs = new HashSet<String>();
		for(String i : ancestors)
			if(highLevelClasses.contains(i))
				highAncs.add(i);
		return highAncs;
	}
	
	/**
	 * @return the set of high level classes in the ontology
	 */
	public Set<String> getHighLevelClasses()
	{
		if(highLevelClasses != null)
			return highLevelClasses;
		
		highLevelClasses = new HashSet<String>();
		
		AML aml = AML.getInstance();
		
		//First get the very top classes
		HashSet<String> sourceTop = new HashSet<String>();
		HashSet<String> targetTop = new HashSet<String>();
		Set<String> ancestors = descendantClasses.keySet();
		//Which are classes that have children but not parents
		//NOTE: This may not work out well if the ontologies are not is_a complete
		for(String a : ancestors)
		{
			if(getParents(a).size() == 0 && getChildren(a).size() > 0)
			{
				if(aml.getSource().contains(a))
					sourceTop.add(a);
				if(aml.getTarget().contains(a))
					targetTop.add(a);
			}
		}
		//Now we go down the ontologies until we reach a significant branching
		while(sourceTop.size() < 3)
		{
			HashSet<String> newTop = new HashSet<String>();
			for(String a : sourceTop)
				newTop.addAll(getChildren(a));
			sourceTop = newTop;
		}
		while(targetTop.size() < 3)
		{
			HashSet<String> newTop = new HashSet<String>();
			for(String a : targetTop)
				newTop.addAll(getChildren(a));
			targetTop = newTop;
		}
		highLevelClasses.addAll(sourceTop);
		highLevelClasses.addAll(targetTop);
		
		return highLevelClasses;
	}
	
	/**
	 * @param indivId: the id of the individual to search in the map
	 * @return the list of individuals to which the given individual is actively related
	 */
	public Set<String> getIndividualActiveRelations(String indivId)
	{
		if(activeRelation.contains(indivId))
			return activeRelation.keySet(indivId);
		return new HashSet<String>();
	}
	
	/**
	 * @param indivId: the id of the individual to search in the map
	 * @return the list of classes instanced by the given individual
	 */
	public Set<String> getIndividualClasses(String indivId)
	{
		if(instanceOfMap.contains(indivId))
			return instanceOfMap.get(indivId);
		return new HashSet<String>();
	}
	
	/**
	 * @param indivId: the id of the individual to search in the map
	 * @return the list of individuals that are actively related with of the given individual
	 */
	public Set<String> getIndividualPassiveRelations(String indivId)
	{
		if(passiveRelation.contains(indivId))
			return passiveRelation.keySet(indivId);
		return new HashSet<String>();
	}

	/**
	 * @param sourceInd: the id of the source individual in the relation
	 * @param targetInd: the id of the target individual in the relation
	 * @return the list of object properties actively relating sourceInd to targetInd
	 */
	public Set<String> getIndividualProperties(String sourceInd, String targetInd)
	{
		if(activeRelation.contains(sourceInd,targetInd))
			return activeRelation.get(sourceInd,targetInd);
		return new HashSet<String>();
	}
	
	/**
	 * @return the list of individuals with active relations
	 */
	public Set<String> getIndividualsWithActiveRelations()
	{
		return activeRelation.keySet();
	}

	/**
	 * @return the list of individuals with active relations
	 */
	public Set<String> getIndividualsWithPassiveRelations()
	{
		return passiveRelation.keySet();
	}
	
	/**
	 * @param indivId: the id of the individual to search in the map
	 * @return the list of classes instanced by the given individual
	 */
	public Set<String> getInstancedClasses()
	{
		return hasInstanceMap.keySet();
	}
	
	/**
	 * @param propId: the id of the property to search in the map
	 * @return the list of inverse properties of the input property
	 */
	public Set<String> getInverseProperties(String propId)
	{
		if(inverseProp.contains(propId))
			return new HashSet<String>(inverseProp.get(propId));
		else
			return new HashSet<String>();
	}
	
	/**
	 * @param uri: the uri of the entity to get the name
	 * @return the local name of the entity with the given index
	 */
	public String getLocalName(String uri)
	{
		if(uri == null)
			return null;
		int i = uri.indexOf("#") + 1;
		if(i == 0)
			i = uri.lastIndexOf("/") + 1;
		return uri.substring(i);
	}

	/**
	 * @param uri: the uri of the Ontology entity
	 * @return the EntityType of the input index
	 */
	public Set<EntityType> getMatchableTypes(String uri)
	{
		HashSet<EntityType> types = new HashSet<EntityType>();
		if(entityType.contains(uri))
			for(EntityType e : entityType.get(uri))
				if(e.isMatchable())
					types.add(e);
		return types;
	}
	
	/**
	 * @return the set of classes with ancestors in the map
	 */
	public Set<String> getParents()
	{
		if(descendantClasses != null)
			return descendantClasses.keySet();
		return new HashSet<String>();
	}

	/**
	 * @param uri: the id of the class to search in the map
	 * @return the list of direct parents of the given class
	 */
	public Set<String> getParents(String uri)
	{
		return getAncestors(uri,1);
	}
	
	/**
	 * @param indivId: the id of the individual to search in the map
	 * @param prop: the property relating the individuals
	 * @return the list of 'parent' relations of the given individual
	 */
	public Set<String> getParentIndividuals(String indivId, String prop)
	{
		if(activeRelation.contains(indivId,prop))
			return activeRelation.get(indivId,prop);
		return new HashSet<String>();
	}
	
	/**
	 * @param uri: the id of the class to search in the map
	 * @param prop: the relationship property between the class and its ancestors
	 * @return the list of strict siblings of the given class (through the subclass relation)
	 */
	public Set<String> getSiblings(String uri)
	{
		Set<String> parents = getAncestors(uri,1);
		HashSet<String> siblings = new HashSet<String>();
		for(String i : parents)
		{
			Set<String> children = getDescendants(i,1);
			for(String j : children)
				if(j != uri)
					siblings.add(j);
		}
		return siblings;
	}

	/**
	 * @param uri: the id of the class to search in the map
	 * @param direct: whether to return all subclasses or just the direct ones
	 * @return the list of direct or indirect subclasses of the input class
	 */
	public Set<String> getSubClasses(String uri, boolean direct)
	{
		HashSet<String> desc = new HashSet<String>();
		Set<String> toSearch;
		if(direct)
			toSearch = getDescendants(uri,1);
		else
			toSearch = getDescendants(uri);
		for(String e : toSearch)
			if(isClass(e))
				desc.add(e);
		return desc;
	}
	
	/**
	 * @param propId: the id of the property to search in the map
	 * @return the list of sub-properties of the input property
	 */
	public Set<String> getSubProperties(String propId)
	{
		if(subProp.contains(propId))
			return new HashSet<String>(subProp.get(propId));
		else
			return new HashSet<String>();
	}
	
	/**
	 * @param uri: the id of the class to search in the map
	 * @param direct: whether to return all superclasses or just the direct ones
	 * @return the list of direct or indirect superclasses of the input class
	 */
	public Set<String> getSuperClasses(String uri, boolean direct)
	{
		HashSet<String> anc = new HashSet<String>();
		Set<String> toSearch;
		if(direct)
			toSearch = getAncestors(uri,1);
		else
			toSearch = getAncestors(uri);
		for(String e : toSearch)
			if(isClass(e))
				anc.add(e);
		return anc;
	}
	
	/**
	 * @param propId: the id of the property to search in the map
	 * @return the list of super-properties of the input property
	 */
	public Set<String> getSuperProperties(String propId)
	{
		if(superProp.contains(propId))
			return new HashSet<String>(superProp.get(propId));
		else
			return new HashSet<String>();
	}
	
	/**
	 * @return the table of transitive properties
	 */
	public Map2Set<String,String> getTransitiveProperties()
	{
		return transitiveOver;
	}
	
	/**
	 * @param uri: the uri of the Ontology entity
	 * @return the EntityTypes of the entity
	 */
	public Set<EntityType> getTypes(String uri)
	{
		if(entityType.contains(uri))
			return entityType.get(uri);
		return new HashSet<EntityType>();
	}
	
	/**
	 * @return the URIs in the EntityMap
	 */
	public Set<String> getURIS()
	{
		return entityType.keySet();
	}
	/**
	 * @param class: the uri of the class to search in the map
	 * @return whether there is a disjoint clause associated with the class
	 */
	public boolean hasDisjoint(String uri)
	{
		return disjointMap.contains(uri);
	}

	/**
	 * @param uri: the uri of the class to search in the map
	 * @return whether there is a disjoint clause associated with the class
	 * or any of its 'is_a' ancestors
	 */
	public boolean hasDisjointTransitive(String uri)
	{
		//Get all superclasses of the class
		Set<String> ancestors = getSuperClasses(uri,false);
		//Plus the parent itself
		ancestors.add(uri);
		//Run through the list of superclasses
		for(String i : ancestors)
			//And check if any have disjoint clauses
			if(disjointMap.contains(i))
				return true;
		return false;
	}
	
	/**
	 * @param one: the first class to check for disjointness
	 * @param two: the second class to check for disjointness
	 * @return whether there is a disjoint clause between one and two
	 */
	public boolean hasDisjointClause(String one, String two)
	{
		return (disjointMap.contains(one) && disjointMap.contains(one,two));
	}
	
	/**
	 * @return the number of instantiations in the map
	 */
	public int individualRelationshipCount()
	{
		return activeRelation.size();
	}
	
	/**
	 * @return the number of instantiations in the map
	 */
	public int instanceCount()
	{
		return instanceOfMap.size();
	}
	
	/**
	 * @param prop: the uri of the property to check
	 * @return whether the property is asymmetric
	 */
	public boolean isAsymmetric(String prop)
	{
		return asymmetric.contains(prop);
	}
	
	/**
	 * @param uri: the uri of the Ontology entity
	 * @return whether the entity is a Class
	 */
	public boolean isClass(String uri)
	{
		return entityType.get(uri).contains(EntityType.CLASS);
	}

	/**
	 * @param uri: the uri of the Ontology entity
	 * @return whether the entity is a Data Property
	 */
	public boolean isDataProperty(String uri)
	{
		return entityType.get(uri).contains(EntityType.DATA_PROP);
	}
	
	/**
	 * @param prop: the uri of the property to check
	 * @return whether the property is functional
	 */
	public boolean isFunctional(String prop)
	{
		return functional.contains(prop);
	}

	/**
	 * @param uri: the uri of the Ontology entity
	 * @return whether the entity is an Individual
	 */
	public boolean isIndividual(String uri)
	{
		return entityType.get(uri).contains(EntityType.INDIVIDUAL);
	}
	
	/**
	 * @param prop: the uri of the property to check
	 * @return whether the property is irreflexive
	 */
	public boolean isIrreflexive(String prop)
	{
		return irreflexive.contains(prop);
	}
	
	/**
	 * @param uri: the uri of the Ontology entity
	 * @return whether the entity is an Object Property
	 */
	public boolean isObjectProperty(String uri)
	{
		return entityType.get(uri).contains(EntityType.OBJECT_PROP);
	}
	
	/**
	 * @param prop: the uri of the property to check
	 * @return whether the property is reflexive
	 */
	public boolean isReflexive(String prop)
	{
		return reflexive.contains(prop);
	}
	
	/**
	 * @param child: the uri of the child class
	 * @param parent: the uri of the parent class
	 * @return whether the RelationshipMap contains a relationship between child and parent
	 */	
	public boolean isSubclass(String child, String parent)
	{
		return descendantClasses.contains(parent,child);
	}
	
	/**
	 * @param prop: the uri of the property to check
	 * @return whether the property is symmetric
	 */
	public boolean isSymmetric(String prop)
	{
		return symmetric.contains(prop);
	}
	
	/**
	 * @return the number of class relationships in the map
	 */
	public int relationshipCount()
	{
		return ancestorClasses.size();
	}
	
	/**
	 * Checks whether two individuals share a direct class assignment
	 * @param ind1Id: the first individual to check
	 * @param ind2Id: the second individual to check
	 * @return whether ind1Id and ind2Id have at least one class in common
	 * in their direct class assignments
	 */
	public boolean shareClass(String ind1Id, String ind2Id)
	{
		if(instanceOfMap.get(ind1Id) == null || instanceOfMap.get(ind2Id) == null)
			return false;
		for(String c : instanceOfMap.get(ind1Id))
			if(instanceOfMap.get(ind2Id).contains(c))
				return true;
		return false;
	}
	
	/**
	 * @param uri: the id of the class to search in the map
	 * @param direct: whether to return all subclasses or just the direct ones
	 * @return the number of direct or indirect subclasses of the input class
	 */
	public int subClassCount(String uri, boolean direct)
	{
		return getSubClasses(uri,direct).size();
	}
	
	/**
	 * @param uri: the id of the class to search in the map
	 * @param direct: whether to return all superclasses or just the direct ones
	 * @return the number of direct or indirect superclasses of the input class
	 */
	public int superClassCount(String uri, boolean direct)
	{
		return getSuperClasses(uri,direct).size();
	}
	
	/**
	 * Compute the transitive closure of the RelationshipMap
	 * by adding inherited relationships (and their distances)
	 * This is an implementation of the Semi-Naive Algorithm
	 */
	public void transitiveClosure()
	{
		//Transitive closure for class relations
		Set<String> t = descendantClasses.keySet();
		int lastCount = 0;
		for(int distance = 1; lastCount != descendantClasses.size(); distance++)
		{
			lastCount = descendantClasses.size();
			for(String i : t)
			{
				Set<String> childs = getChildren(i);
				childs.addAll(getEquivalences(i));
				Set<String> pars = getAncestors(i,distance);
				for(String j : pars)
					for(String h : childs)
						addClassRelationship(h, j, getDistance(i,j) + getDistance(h,i));
			}
		}
	}
	
	/**
	 * @param child: the child class in the relationship
	 * @param parent: the parent class in the relationship
	 * @return whether adding the relationship between child and parent
	 * to the RelationshipMap would violate a disjoint clause
	 */
	public boolean violatesDisjoint(String child, String parent)
	{
		//Get all descendants of the child
		Set<String> descendants = getDescendants(child);
		//Plus the child itself
		descendants.add(child);
		//Then all ancestors of the parent
		Set<String> ancestors = getAncestors(parent);
		//Plus the parent itself
		ancestors.add(parent);
		
		//For each descendant
		for(String i : descendants)
			//And each ancestor
			for(String j : ancestors)
				//Check for disjointness
				if(areDisjoint(i,j))
					return true;
		return false;
	}
}