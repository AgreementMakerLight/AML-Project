/******************************************************************************
* Copyright 2013-2015 LASIGE                                                  *
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
* The map of relationships in an Ontology, including relationships between    *
* classes (hierarchical and disjoint), between individuals and classes,       *
* between individuals, and between properties.                                *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 24-11-2015                                                            *
******************************************************************************/
package aml.ontology;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import aml.AML;
import aml.util.Table2Set;
import aml.util.Table3List;
import aml.util.Table3Set;


public class RelationshipMap
{
	
//Attributes

	//Relationships between classes
	//Hierarchical relations and property restrictions (with transitive closure)
	private Table3List<Integer,Integer,Relationship> ancestorMap; //Class -> Ancestor -> Relationship
	private Table3List<Integer,Integer,Relationship> descendantMap;	//Class -> Descendant -> Relationship
	//Disjointness (direct only, no transitive closure)
	private Table2Set<Integer,Integer> disjointMap; //Class -> Disjoint Classes
	
	//Relationships between individuals and classes (with transitive closure)
	private Table2Set<Integer,Integer> instanceOfMap; //Individual -> Class 
	private Table2Set<Integer,Integer> hasInstanceMap; //Class -> Individual

	//Relationships between individuals
	private Table3Set<Integer,Integer,Integer> ancestorIndividuals; //'Child' -> Property -> 'Parent'
	private Table3Set<Integer,Integer,Integer> descendantIndividuals; //'Parent' -> Property -> 'Child'

	//Relationships between properties
	//Hierarchical and inverse relations
	private Table2Set<Integer,Integer> subProp; //Property -> SubProperty
	private Table2Set<Integer,Integer> superProp; //Property -> SuperProperty
	private Table2Set<Integer,Integer> inverseProp; //Property -> InverseProperty
	//Transitivity relations (transitive properties will be mapped to themselves)
	private Table2Set<Integer,Integer> transitiveOver; //Property1 -> Property2 over which 1 is transitive
	
	//List of high level classes
	private HashSet<Integer> highLevelClasses;
	
//Constructors

	/**
	 * Creates a new empty RelationshipMap
	 */
	public RelationshipMap()
	{
		descendantMap = new Table3List<Integer,Integer,Relationship>();
		ancestorMap = new Table3List<Integer,Integer,Relationship>();
		disjointMap = new Table2Set<Integer,Integer>();
		instanceOfMap = new Table2Set<Integer,Integer>();
		hasInstanceMap = new Table2Set<Integer,Integer>();
		ancestorIndividuals = new Table3Set<Integer,Integer,Integer>();		
		descendantIndividuals = new Table3Set<Integer,Integer,Integer>();		
		subProp = new Table2Set<Integer,Integer>();
		superProp = new Table2Set<Integer,Integer>();
		inverseProp = new Table2Set<Integer,Integer>();
		transitiveOver = new Table2Set<Integer,Integer>();
	}
	
//Public Methods

	/**
	 * Adds a direct relationship between two classes with a given property and restriction
	 * @param child: the index of the child class
	 * @param parent: the index of the parent class
	 * @param distance: the distance (number of edges) between the classes
	 * @param prop: the property in the subclass relationship
	 * @param rest: the restriction in the subclass relationship
	 */
	public void addClassRelationship(int child, int parent, int prop, boolean rest)
	{
		addClassRelationship(child,parent,1,prop,rest);
	}
	
	/**
	 * Adds a relationship between two classes with a given distance, property and restriction
	 * @param child: the index of the child class
	 * @param parent: the index of the parent class
	 * @param distance: the distance (number of edges) between the classes
	 * @param prop: the property in the subclass relationship
	 * @param rest: the restriction in the subclass relationship
	 */
	public void addClassRelationship(int child, int parent, int distance, int prop, boolean rest)
	{
		//Create the relationship
		Relationship r = new Relationship(distance,prop,rest);
		//Then update the MultiMaps
		descendantMap.add(parent,child,r);
		ancestorMap.add(child,parent,r);
	}
	
	/**
	 * Adds a direct hierarchical relationship between two classes
	 * @param child: the index of the child class
	 * @param parent: the index of the parent class
	 */
	public void addDirectSubclass(int child, int parent)
	{
		addClassRelationship(child,parent,1,-1,false);
	}
	
	/**
	 * Adds a new disjointness relations between two classes
	 * @param class1: the index of the first disjoint class
	 * @param class2: the index of the second disjoint class
	 */
	public void addDisjoint(int class1, int class2)
	{
		if(class1 != class2 && !areDisjoint(class1,class2))
		{
			//The disjointMap keeps disjoint clauses in both directions
			disjointMap.add(class1, class2);
			disjointMap.add(class2, class1);
		}
	}
	
	/**
	 * Adds an equivalence relationship between two classes with a given property and restriction
	 * @param class1: the index of the first equivalent class
	 * @param class2: the index of the second equivalent class
	 * @param prop: the property in the subclass relationship
	 * @param rest: the restriction in the subclass relationship
	 */
	public void addEquivalence(int class1, int class2, int prop, boolean rest)
	{
		addClassRelationship(class1,class2,0,prop,rest);
	}
	
	/**
	 * Adds an equivalence relationship between two classes
	 * @param class1: the index of the first equivalent class
	 * @param class2: the index of the second equivalent class
	 */
	public void addEquivalentClass(int class1, int class2)
	{
		//Add the relationship in both directions
		addClassRelationship(class1,class2,0,-1,false);
		addClassRelationship(class2,class1,0,-1,false);
	}
	
	/**
	 * Adds a relationship between two individuals through a given property
	 * @param indiv1: the index of the first individual
	 * @param prop: the property in the relationship
	 * @param indiv2: the index of the second individual
	 */
	public void addIndividualRelationship(int indiv1, int prop, int indiv2)
	{
		ancestorIndividuals.add(indiv1,prop,indiv2);
		descendantIndividuals.add(indiv2,prop,indiv1);
	}
	
	/**
	 * Adds an instantiation relationship between an individual and a class
	 * @param individualId: the index of the individual
	 * @param classId: the index of the class
	 */
	public void addInstance(int individualId, int classId)
	{
		instanceOfMap.add(individualId,classId);
		hasInstanceMap.add(classId,individualId);
	}
	
	/**
	 * Adds a new inverse relationship between two properties if it doesn't exist
	 * @param property1: the index of the first property
	 * @param property2: the index of the second property
	 */
	public void addInverseProp(int property1, int property2)
	{
		if(property1 != property2)
		{
			inverseProp.add(property1, property2);
			inverseProp.add(property2, property1);
		}
	}
	
	/**
	 * Adds a relationship between two properties
	 * @param child: the index of the child property
	 * @param parent: the index of the parent property
	 */
	public void addSubProperty(int child, int parent)
	{
		//Then update the MultiMaps
		subProp.add(parent,child);
		superProp.add(child,parent);
	}
	
	/**
	 * @param prop: the property to set as transitive
	 */
	public void addTransitive(int prop)
	{
		transitiveOver.add(prop,prop);
	}
	
	/**
	 * @param prop1: the property to set as transitive over prop2
	 * @param prop2: the property over which prop1 is transitive
	 */
	public void addTransitiveOver(int prop1, int prop2)
	{
		transitiveOver.add(prop1,prop2);
	}
	
	/**
	 * @param class1: the first class to check for disjointness
	 * @param class2: the second class to check for disjointness
	 * @return whether one and two are disjoint considering transitivity
	 */
	public boolean areDisjoint(int class1, int class2)
	{
		//Get the transitive disjoint clauses involving class one
		Set<Integer> disj = getDisjointTransitive(class1);
		if(disj.size() > 0)
		{
			//Then get the list of superclasses of class two
			Set<Integer> ancs = getSuperClasses(class2,false);
			//Including class two itself
			ancs.add(class2);
		
			//Two classes are disjoint if the list of transitive disjoint clauses
			//involving one of them contains the other or any of its 'is_a' ancestors
			for(Integer i : ancs)
				if(disj.contains(i))
					return true;
		}
		return false;
	}
	
	/**
	 * @param child: the index of the child class
	 * @param parent: the index of the parent class
	 * @return whether the RelationshipMap contains a relationship between child and parent
	 */
	public boolean areRelatedClasses(int child, int parent)
	{
		return descendantMap.contains(parent,child);
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
	 * @param classId: the id of the class to search in the map
	 * @return the list of siblings of the given class with the given property
	 */
	public Set<Integer> getAllSiblings(int classId)
	{
		Set<Integer> parents = getAncestors(classId,1);
		HashSet<Integer> siblings = new HashSet<Integer>();
		for(Integer i : parents)
		{
			for(Relationship r : getRelationships(classId,i))
			{
				Set<Integer> children = getDescendants(i,1,r.getProperty());
				for(Integer j : children)
					if(j != classId)
						siblings.add(j);
			}
		}
		return siblings;
	}
	
	/**
	 * @param classId: the id of the class to search in the map
	 * @return the list of ancestors of the given class
	 */
	public Set<Integer> getAncestors(int classId)
	{
		if(ancestorMap.contains(classId))
			return ancestorMap.keySet(classId);
		return new HashSet<Integer>();
	}
	
	/**
	 * @param classId: the id of the class to search in the map
	 * @param distance: the distance between the class and its ancestors
	 * @return the list of ancestors at the given distance from the input class
	 */
	public Set<Integer> getAncestors(int classId, int distance)
	{
		HashSet<Integer> asc = new HashSet<Integer>();
		if(!ancestorMap.contains(classId))
			return asc;
		for(Integer i : ancestorMap.keySet(classId))
			for(Relationship r : ancestorMap.get(classId, i))
				if(r.getDistance() == distance)
					asc.add(i);
		return asc;
	}
	
	/**
	 * @param classId: the id of the class to search in the map
	 * @param prop: the relationship property between the class and its ancestors
	 * @return the list of ancestors at the given distance from the input class
	 */
	public Set<Integer> getAncestorsProperty(int classId, int prop)
	{
		HashSet<Integer> asc = new HashSet<Integer>();
		if(!ancestorMap.contains(classId))
			return asc;
		for(Integer i : ancestorMap.keySet(classId))
			for(Relationship r : ancestorMap.get(classId, i))
				if(r.getProperty() == prop)
					asc.add(i);
		return asc;
	}
	
	/**
	 * @param classId: the id of the class to search in the map
	 * @param distance: the distance between the class and its ancestors
	 * @param prop: the relationship property between the class and its ancestors
	 * @return the list of ancestors of the input class that are at the given
	 * distance and with the given property
	 */
	public Set<Integer> getAncestors(int classId, int distance, int prop)
	{
		HashSet<Integer> asc = new HashSet<Integer>();
		if(!ancestorMap.contains(classId))
			return asc;
		for(Integer i : ancestorMap.keySet(classId))
			for(Relationship r : ancestorMap.get(classId, i))
				if(r.getDistance() == distance && r.getProperty() == prop)
					asc.add(i);
		return asc;
	}
	
	/**
	 * @return the set of classes with ancestors in the map
	 */
	public Set<Integer> getChildren()
	{
		if(ancestorMap != null)
			return ancestorMap.keySet();
		return new HashSet<Integer>();
	}
	
	/**
	 * @param classId: the id of the class to search in the map
	 * @return the list of direct children of the given class
	 */
	public Set<Integer> getChildren(int classId)
	{
		return getDescendants(classId,1);
	}

	/**
	 * @param indivId: the id of the individual to search in the map
	 * @param prop: the property relating the individuals
	 * @return the list of 'children' relations of the given individual
	 */
	public Set<Integer> getChildrenIndividuals(int indivId, int prop)
	{
		return descendantIndividuals.get(indivId,prop);
	}
	
	/**
	 * @param classId: the id of the class to search in the map
	 * @return the list of individuals that instantiate the given class
	 */
	public Set<Integer> getClassIndividuals(int classId)
	{
		return hasInstanceMap.get(classId);
	}
	
	/**
	 * @param classes: the set the class to search in the map
	 * @return the list of direct subclasses shared by the set of classes
	 */
	public Set<Integer> getCommonSubClasses(Set<Integer> classes)
	{
		if(classes == null || classes.size() == 0)
			return null;
		Iterator<Integer> it = classes.iterator();
		Vector<Integer> subclasses = new Vector<Integer>(getSubClasses(it.next(),false));
		while(it.hasNext())
		{
			HashSet<Integer> s = new HashSet<Integer>(getSubClasses(it.next(),false));
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
		return new HashSet<Integer>(subclasses);
	}
	
	/**
	 * @param classId: the id of the class to search in the map
	 * @return the list of descendants of the input class
	 */
	public Set<Integer> getDescendants(int classId)
	{
		if(descendantMap.contains(classId))
			return descendantMap.keySet(classId);
		return new HashSet<Integer>();
	}
	
	/**
	 * @param classId: the id of the class to search in the map
	 * @param distance: the distance between the class and its ancestors
	 * @return the list of descendants at the given distance from the input class
	 */
	public Set<Integer> getDescendants(int classId, int distance)
	{
		HashSet<Integer> desc = new HashSet<Integer>();
		if(!descendantMap.contains(classId))
			return desc;
		for(Integer i : descendantMap.keySet(classId))
			for(Relationship r : descendantMap.get(classId, i))
				if(r.getDistance() == distance)
					desc.add(i);
		return desc;
	}
	
	/**
	 * @param classId: the id of the class to search in the map
	 * @param prop: the relationship property between the class and its ancestors
	 * @return the list of descendants at the given distance from the input class
	 */
	public Set<Integer> getDescendantsProperty(int classId, int prop)
	{
		HashSet<Integer> desc = new HashSet<Integer>();
		if(!descendantMap.contains(classId))
			return desc;
		for(Integer i : descendantMap.keySet(classId))
			for(Relationship r : descendantMap.get(classId, i))
				if(r.getProperty() == prop)
					desc.add(i);
		return desc;
	}
	
	/**
	 * @param classId: the id of the class to search in the map
	 * @param distance: the distance between the class and its ancestors
	 * @param prop: the relationship property between the class and its ancestors
	 * @return the list of descendants of the input class at the given distance
	 * and with the given property
	 */
	public Set<Integer> getDescendants(int classId, int distance, int prop)
	{
		HashSet<Integer> desc = new HashSet<Integer>();
		if(!descendantMap.contains(classId))
			return desc;
		for(Integer i : descendantMap.keySet(classId))
			for(Relationship r : descendantMap.get(classId, i))
				if(r.getDistance() == distance && r.getProperty() == prop)
					desc.add(i);
		return desc;
	}
	
	/**
	 * @return the set of classes that have disjoint clauses
	 */
	public Set<Integer> getDisjoint()
	{
		return disjointMap.keySet();
	}

	/**
	 * @param classId: the id of the class to search in the map
	 * @return the list of classes disjoint with the given class
	 */
	public Set<Integer> getDisjoint(int classId)
	{
		if(disjointMap.contains(classId))
			return disjointMap.get(classId);
		return new HashSet<Integer>();
	}
	
	/**
	 * @param classId: the id of the class to search in the map
	 * @return the list of classes disjoint with the given class
	 * or any of its 'is_a' ancestors
	 */
	public Set<Integer> getDisjointTransitive(int classId)
	{
		//Get the disjoint clauses for the class
		Set<Integer> disj = getDisjoint(classId);
		//Then get all superclasses of the class
		Set<Integer> ancestors = getSuperClasses(classId,false);
		//For each superclass
		for(Integer i : ancestors)
			//Add its disjoint clauses to the list
			disj.addAll(getDisjoint(i));
		return disj;
	}
	
	/**
	 * @param child: the index of the child class
	 * @param parent: the index of the parent class
	 * @return the minimal distance between the child and parent,
	 * or 0 if child==parent, or -1 if they aren't related
	 */
	public int getDistance(int child, int parent)
	{
		if(child == parent)
			return 0;
		if(!ancestorMap.contains(child, parent))
			return -1;
		Vector<Relationship> rels = ancestorMap.get(child,parent);
		int distance = rels.get(0).getDistance();
		for(Relationship r : rels)
			if(r.getDistance() < distance)
				distance = r.getDistance();
		return distance;
	}
	
	/**
	 * @param classId: the id of the class to search in the map
	 * @return the list of equivalences of the given class
	 */
	public Set<Integer> getEquivalences(int classId)
	{
		return getDescendants(classId, 0);
	}
	
	/**
	 * @param classId: the id of the class to search in the map
	 * @return the list of classes equivalent to the given class
	 */
	public Set<Integer> getEquivalentClasses(int classId)
	{
		return getDescendants(classId,0,-1);
	}
	
	/**
	 * @param classId: the id of the class to search in the map
	 * @return the list of high level ancestors of the given class
	 */
	public Set<Integer> getHighLevelAncestors(int classId)
	{
		if(highLevelClasses == null)
			getHighLevelClasses();
		Set<Integer> ancestors = getAncestors(classId);
		HashSet<Integer> highAncs = new HashSet<Integer>();
		for(Integer i : ancestors)
			if(highLevelClasses.contains(i))
				highAncs.add(i);
		return highAncs;
	}
	
	/**
	 * @return the set of high level classes in the ontology
	 */
	public Set<Integer> getHighLevelClasses()
	{
		if(highLevelClasses != null)
			return highLevelClasses;
		
		highLevelClasses = new HashSet<Integer>();
		
		AML aml = AML.getInstance();
		
		//First get the very top classes
		HashSet<Integer> sourceTop = new HashSet<Integer>();
		HashSet<Integer> targetTop = new HashSet<Integer>();
		Set<Integer> ancestors = descendantMap.keySet();
		//Which are classes that have children but not parents
		//NOTE: This may not work out well if the ontologies are not is_a complete
		for(Integer a : ancestors)
		{
			if(getParents(a).size() == 0 && getChildren(a).size() > 0)
			{
				if(aml.getSource().containsClass(a))
					sourceTop.add(a);
				if(aml.getTarget().containsClass(a))
					targetTop.add(a);
			}
		}
		//Now we go down the ontologies until we reach a significant branching
		while(sourceTop.size() < 3)
		{
			HashSet<Integer> newTop = new HashSet<Integer>();
			for(Integer a : sourceTop)
				newTop.addAll(getChildren(a));
			sourceTop = newTop;
		}
		while(targetTop.size() < 3)
		{
			HashSet<Integer> newTop = new HashSet<Integer>();
			for(Integer a : targetTop)
				newTop.addAll(getChildren(a));
			targetTop = newTop;
		}
		highLevelClasses.addAll(sourceTop);
		highLevelClasses.addAll(targetTop);
		
		return highLevelClasses;
	}
	
	/**
	 * @param indivId: the id of the individual to search in the map
	 * @return the list of classes instanced by the given individual
	 */
	public Set<Integer> getIndividualClasses(int indivId)
	{
		return instanceOfMap.get(indivId);
	}
	
	/**
	 * @param indivId: the id of the individual to search in the map
	 * @return the list of object properties associated with the given individual
	 */
	public Set<Integer> getIndividualProperties(int indivId)
	{
		if(ancestorIndividuals.contains(indivId))
			return ancestorIndividuals.keySet(indivId);
		return new HashSet<Integer>();
	}
	
	/**
	 * @param propId: the id of the property to search in the map
	 * @return the list of inverse properties of the input property
	 */
	public Set<Integer> getInverseProperties(int propId)
	{
		if(inverseProp.contains(propId))
			return new HashSet<Integer>(inverseProp.get(propId));
		else
			return new HashSet<Integer>();
	}
	
	/**
	 * @return the set of classes with ancestors in the map
	 */
	public Set<Integer> getParents()
	{
		if(descendantMap != null)
			return descendantMap.keySet();
		return new HashSet<Integer>();
	}

	/**
	 * @param classId: the id of the class to search in the map
	 * @return the list of direct parents of the given class
	 */
	public Set<Integer> getParents(int classId)
	{
		return getAncestors(classId,1);
	}
	
	/**
	 * @param indivId: the id of the individual to search in the map
	 * @param prop: the property relating the individuals
	 * @return the list of 'parent' relations of the given individual
	 */
	public Set<Integer> getParentIndividuals(int indivId, int prop)
	{
		return ancestorIndividuals.get(indivId,prop);
	}
	
	/**
	 * @param child: the id of the child class to search in the map
	 * @param parent: the id of the parent class to search in the map
	 * @return the 'best' relationship between the two classes
	 */
	public Relationship getRelationship(int child, int parent)
	{
		if(!ancestorMap.contains(child, parent))
			return null;
		Relationship rel = ancestorMap.get(child).get(parent).get(0);
		for(Relationship r : ancestorMap.get(child).get(parent))
			if(r.compareTo(rel) > 0)
				rel = r;
		return rel;
	}

	/**
	 * @param child: the id of the child class to search in the map
	 * @param parent: the id of the parent class to search in the map
	 * @return the relationships between the two classes
	 */
	public Vector<Relationship> getRelationships(int child, int parent)
	{
		return ancestorMap.get(child).get(parent);
	}
	
	/**
	 * @param classId: the id of the class to search in the map
	 * @param prop: the relationship property between the class and its ancestors
	 * @return the list of siblings of the given class with the given property
	 */
	public Set<Integer> getSiblings(int classId)
	{
		Set<Integer> parents = getAncestors(classId,1,-1);
		HashSet<Integer> siblings = new HashSet<Integer>();
		for(Integer i : parents)
		{
			Set<Integer> children = getDescendants(i,1,-1);
			for(Integer j : children)
				if(j != classId)
					siblings.add(j);
		}
		return siblings;
	}
	
	/**
	 * @param classId: the id of the class to search in the map
	 * @return the list of siblings of the given class for all
	 * subclass relationships
	 */
	public Set<Integer> getSiblingsProperty(int classId, int prop)
	{
		Set<Integer> parents = getAncestors(classId,1,prop);
		HashSet<Integer> siblings = new HashSet<Integer>();
		for(Integer i : parents)
		{
			Set<Integer> children = getDescendants(i,1,prop);
			for(Integer j : children)
				if(j != classId)
					siblings.add(j);
		}
		return siblings;
	}
	
	/**
	 * @param classId: the id of the class to search in the map
	 * @param direct: whether to return all subclasses or just the direct ones
	 * @return the list of direct or indirect subclasses of the input class
	 */
	public Set<Integer> getSubClasses(int classId, boolean direct)
	{
		if(direct)
			return getDescendants(classId,1,-1);
		else
			return getDescendantsProperty(classId,-1);
	}
	
	/**
	 * @param propId: the id of the property to search in the map
	 * @return the list of sub-properties of the input property
	 */
	public Set<Integer> getSubProperties(int propId)
	{
		if(subProp.contains(propId))
			return new HashSet<Integer>(subProp.get(propId));
		else
			return new HashSet<Integer>();
	}
	
	/**
	 * @param classId: the id of the class to search in the map
	 * @param direct: whether to return all superclasses or just the direct ones
	 * @return the list of direct or indirect superclasses of the input class
	 */
	public Set<Integer> getSuperClasses(int classId, boolean direct)
	{
		if(direct)
			return getAncestors(classId,1,-1);
		else
			return getAncestorsProperty(classId,-1);
	}
	
	/**
	 * @param propId: the id of the property to search in the map
	 * @return the list of super-properties of the input property
	 */
	public Set<Integer> getSuperProperties(int propId)
	{
		if(superProp.contains(propId))
			return new HashSet<Integer>(superProp.get(propId));
		else
			return new HashSet<Integer>();
	}
	
	/**
	 * @return the table of transitive properties
	 */
	public Table2Set<Integer,Integer> getTransitiveProperties()
	{
		return transitiveOver;
	}
	
	/**
	 * @param class: the index of the class to search in the map
	 * @return whether there is a disjoint clause associated with the class
	 */
	public boolean hasDisjoint(int classId)
	{
		return disjointMap.contains(classId);
	}

	/**
	 * @param classId: the index of the class to search in the map
	 * @return whether there is a disjoint clause associated with the class
	 * or any of its 'is_a' ancestors
	 */
	public boolean hasDisjointTransitive(int classId)
	{
		//Get all superclasses of the class
		Set<Integer> ancestors = getSuperClasses(classId,false);
		//Plus the parent itself
		ancestors.add(classId);
		//Run through the list of superclasses
		for(Integer i : ancestors)
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
	public boolean hasDisjointClause(int one, int two)
	{
		return (disjointMap.contains(one) && disjointMap.contains(one,two));
	}
	
	/**
	 * @param child: the id of the child class to search in the map
	 * @param parent: the id of the parent class to search in the map
	 * @param property: the id of the property between child and parent
	 * @return whether there is a relationship between child and parent
	 *  with the given property
	 */
	public boolean hasProperty(int child, int parent, int property)
	{
		Vector<Relationship> rels = getRelationships(child,parent);
		for(Relationship r : rels)
			if(r.getProperty() == property)
				return true;
		return false;
	}
	
	/**
	 * @param child: the index of the child class
	 * @param parent: the index of the parent class
	 * @return whether the RelationshipMap contains an 'is_a' relationship between child and parent
	 */	
	public boolean isSubclass(int child, int parent)
	{
		if(!descendantMap.contains(parent,child))
			return false;
		Vector<Relationship> rels = descendantMap.get(parent,child);
		for(Relationship r : rels)
			if(r.getProperty() == -1)
				return true;
		return false;
	}
	
	/**
	 * @return the number of relationships in the map
	 */
	public int relationshipCount()
	{
		return ancestorMap.size();
	}
	
	/**
	 * @param classId: the id of the class to search in the map
	 * @param direct: whether to return all subclasses or just the direct ones
	 * @return the number of direct or indirect subclasses of the input class
	 */
	public int subClassCount(int classId, boolean direct)
	{
		return getSubClasses(classId,direct).size();
	}
	
	/**
	 * @param classId: the id of the class to search in the map
	 * @param direct: whether to return all superclasses or just the direct ones
	 * @return the number of direct or indirect superclasses of the input class
	 */
	public int superClassCount(int classId, boolean direct)
	{
		return getSuperClasses(classId,direct).size();
	}
	
	/**
	 * Compute the transitive closure of the RelationshipMap
	 * by adding inherited relationships (and their distances)
	 * This is an implementation of the Semi-Naive Algorithm
	 */
	public void transitiveClosure()
	{
		//Transitive closure for class relations
		Set<Integer> t = descendantMap.keySet();
		int lastCount = 0;
		for(int distance = 1; lastCount != descendantMap.size(); distance++)
		{
			lastCount = descendantMap.size();
			for(Integer i : t)
			{
				Set<Integer> childs = getChildren(i);
				childs.addAll(getEquivalences(i));
				Set<Integer> pars = getAncestors(i,distance);
				for(Integer j : pars)
				{
					Vector<Relationship> rel1 = getRelationships(i,j);
					for(int k = 0; k < rel1.size(); k++)
					{
						Relationship r1 = rel1.get(k);
						int p1 = r1.getProperty();
						for(Integer h : childs)
						{
							Vector<Relationship> rel2 = getRelationships(h,i);
							for(int l = 0; l < rel2.size(); l++)
							{
								Relationship r2 = rel2.get(l);
								int p2 = r2.getProperty();
								//We only do transitive closure if the property is the same (and transitive)
								//for two relationships or one of the properties is 'is_a' (-1)
								if(!(p1 == -1 || p2 == -1 || transitiveOver.contains(p2,p1)))
									continue;
								int dist = r1.getDistance() + r2.getDistance();
								int prop;
								if(p1 == p2 || p1 != -1)
									prop = p1;
								else
									prop = p2;
								boolean rest = r1.getRestriction() && r2.getRestriction();
								addClassRelationship(h, j, dist, prop, rest);
							}
						}
					}
				}
			}
		}
		//Transitive closure for individual-class instantiation
		for(Integer in : instanceOfMap.keySet())
			for(Integer cl : instanceOfMap.get(in))
				for(Integer an : getSuperClasses(cl,false))
					addInstance(in,an);
	}
	
	/**
	 * @param child: the child class in the relationship
	 * @param parent: the parent class in the relationship
	 * @return whether adding the relationship between child and parent
	 * to the RelationshipMap would violate a disjoint clause
	 */
	public boolean violatesDisjoint(int child, int parent)
	{
		//Get all descendants of the child
		Set<Integer> descendants = getDescendants(child);
		//Plus the child itself
		descendants.add(child);
		//Then all ancestors of the parent
		Set<Integer> ancestors = getAncestors(parent);
		//Plus the parent itself
		ancestors.add(parent);
		
		//For each descendant
		for(Integer i : descendants)
			//And each ancestor
			for(Integer j : ancestors)
				//Check for disjointness
				if(areDisjoint(i,j))
					return true;
		return false;
	}
}