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
* The map of relationships in an Ontology, including relationships between    *
* classes (hierarchical and disjoint), between individuals and classes,       *
* between individuals, and between properties.                                *
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
import aml.util.table.Map2Map2List;
import aml.util.table.Map2Map2Set;


public class RelationshipMap
{
	
//Attributes

	//Relationships between classes
	//Hierarchical relations and property restrictions (with transitive closure)
	private Map2Map2List<String,String,Relationship> ancestorClasses; //Class -> Ancestor -> Relationship
	private Map2Map2List<String,String,Relationship> descendantClasses;	//Class -> Descendant -> Relationship
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
	//Transitivity relations (transitive properties will be mapped to themselves)
	private Map2Set<String,String> transitiveOver; //Property1 -> Property2 over which 1 is transitive
	//List of symmetric properties
	private HashSet<String> symmetric;
	//List of functional properties
	private HashSet<String> functional;
	
	//Property domains and ranges (property to class or to String)
	private Map2Set<String,String> domain; //Property -> Class
	private Map2Set<String,String> objectRange; //Property -> Class
	private Map2Set<String,String> dataRange; //Property -> String
	
//Constructors

	/**
	 * Creates a new empty RelationshipMap
	 */
	public RelationshipMap()
	{
		descendantClasses = new Map2Map2List<String,String,Relationship>();
		ancestorClasses = new Map2Map2List<String,String,Relationship>();
		disjointMap = new Map2Set<String,String>();
		instanceOfMap = new Map2Set<String,String>();
		hasInstanceMap = new Map2Set<String,String>();
		activeRelation = new Map2Map2Set<String,String,String>();		
		passiveRelation = new Map2Map2Set<String,String,String>();		
		subProp = new Map2Set<String,String>();
		superProp = new Map2Set<String,String>();
		inverseProp = new Map2Set<String,String>();
		transitiveOver = new Map2Set<String,String>();
		symmetric = new HashSet<String>();
		functional = new HashSet<String>();
		domain = new Map2Set<String,String>();
		objectRange = new Map2Set<String,String>();
		dataRange = new Map2Set<String,String>();
	}
	
//Public Methods

	/**
	 * Adds a direct relationship between two classes with a given property and restriction
	 * @param child: the uri of the child class
	 * @param parent: the uri of the parent class
	 * @param distance: the distance (number of edges) between the classes
	 * @param prop: the property in the subclass relationship
	 * @param rest: the restriction in the subclass relationship
	 */
	public void addClassRelationship(String child, String parent, String prop, boolean rest)
	{
		addClassRelationship(child,parent,1,prop,rest);
	}
	
	/**
	 * Adds a relationship between two classes with a given distance, property and restriction
	 * @param child: the uri of the child class
	 * @param parent: the uri of the parent class
	 * @param distance: the distance (number of edges) between the classes
	 * @param prop: the property in the subclass relationship
	 * @param rest: the restriction in the subclass relationship
	 */
	public void addClassRelationship(String child, String parent, int distance, String prop, boolean rest)
	{
		//Create the relationship
		Relationship r = new Relationship(distance,prop,rest);
		//Then update the MultiMaps
		descendantClasses.add(parent,child,r);
		ancestorClasses.add(child,parent,r);
	}
	
	/**
	 * Adds a new range (data type) to a given data property
	 * @param propId: the uri of the property with the range
	 * @param type: the data type in the range of the property
	 */
	public void addDataRange(String propId, String type)
	{
		dataRange.add(propId, type);
	}
	
	/**
	 * Adds a direct hierarchical relationship between two classes
	 * @param child: the uri of the child class
	 * @param parent: the uri of the parent class
	 */
	public void addDirectSubclass(String child, String parent)
	{
		addClassRelationship(child,parent,1,"",false);
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
	 * @param classId: the uri of the class in the domain of the property
	 */
	public void addDomain(String propId, String classId)
	{
		domain.add(propId, classId);
	}
	
	/**
	 * Adds an equivalence relationship between two classes with a given property and restriction
	 * @param class1: the uri of the first equivalent class
	 * @param class2: the uri of the second equivalent class
	 * @param prop: the property in the subclass relationship
	 * @param rest: the restriction in the subclass relationship
	 */
	public void addEquivalence(String class1, String class2, String prop, boolean rest)
	{
		addClassRelationship(class1,class2,0,prop,rest);
		if(symmetric.contains(prop))
			addClassRelationship(class2,class1,0,prop,rest);
	}
	
	/**
	 * Adds an equivalence relationship between two classes
	 * @param class1: the uri of the first equivalent class
	 * @param class2: the uri of the second equivalent class
	 */
	public void addEquivalentClass(String class1, String class2)
	{
		addEquivalence(class1,class2,"",false);
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
	 * @param classId: the uri of the class
	 */
	public void addInstance(String individualId, String classId)
	{
		instanceOfMap.add(individualId,classId);
		hasInstanceMap.add(classId,individualId);
	}
	
	/**
	 * Adds a new inverse relationship between two properties if it doesn't exist
	 * @param property1: the uri of the first property
	 * @param property2: the uri of the second property
	 */
	public void addInverseProp(String property1, String property2)
	{
		if(property1 != property2)
		{
			inverseProp.add(property1, property2);
			inverseProp.add(property2, property1);
		}
	}
	
	/**
	 * Adds a new range (class) to a given object property
	 * @param propId: the uri of the property with the range
	 * @param classId: the uri of the class in the range of the property
	 */
	public void addObjectRange(String propId, String classId)
	{
		objectRange.add(propId, classId);
	}
	
	/**
	 * Adds a relationship between two properties
	 * @param child: the uri of the child property
	 * @param parent: the uri of the parent property
	 */
	public void addSubProperty(String child, String parent)
	{
		//Then update the MultiMaps
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
	 * @param classId: the uri of the class to check
	 * @return whether indivId is an instance of classId or
	 * of one of its subclasses
	 */
	public boolean belongsToClass(String indivId, String classId)
	{
		if(instanceOfMap.contains(indivId, classId))
			return true;
		for(String subclassId : getSubClasses(classId,false))
			if(instanceOfMap.contains(indivId, subclassId))
				return true;
		return false;	
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
	 * @return the list of all siblings of the given class
	 */
	public Set<String> getAllSiblings(String classId)
	{
		Set<String> parents = getAncestors(classId,1);
		HashSet<String> siblings = new HashSet<String>();
		for(String i : parents)
		{
			for(Relationship r : getRelationships(classId,i))
			{
				Set<String> children = getDescendants(i,1,r.getProperty());
				for(String j : children)
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
	public Set<String> getAncestors(String classId)
	{
		if(ancestorClasses.contains(classId))
			return ancestorClasses.keySet(classId);
		return new HashSet<String>();
	}
	
	/**
	 * @param classId: the id of the class to search in the map
	 * @param distance: the distance between the class and its ancestors
	 * @return the list of ancestors at the given distance from the input class
	 */
	public Set<String> getAncestors(String classId, int distance)
	{
		HashSet<String> asc = new HashSet<String>();
		if(!ancestorClasses.contains(classId))
			return asc;
		for(String i : ancestorClasses.keySet(classId))
			for(Relationship r : ancestorClasses.get(classId, i))
				if(r.getDistance() == distance)
					asc.add(i);
		return asc;
	}
	
	/**
	 * @param classId: the id of the class to search in the map
	 * @param prop: the relationship property between the class and its ancestors
	 * @return the list of ancestors at the given distance from the input class
	 */
	public Set<String> getAncestorsProperty(String classId, String prop)
	{
		HashSet<String> asc = new HashSet<String>();
		if(!ancestorClasses.contains(classId))
			return asc;
		for(String i : ancestorClasses.keySet(classId))
			for(Relationship r : ancestorClasses.get(classId, i))
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
	public Set<String> getAncestors(String classId, int distance, String prop)
	{
		HashSet<String> asc = new HashSet<String>();
		if(!ancestorClasses.contains(classId))
			return asc;
		for(String i : ancestorClasses.keySet(classId))
			for(Relationship r : ancestorClasses.get(classId, i))
				if(r.getDistance() == distance && r.getProperty() == prop)
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
	 * @param classId: the id of the class to search in the map
	 * @return the list of direct children of the given class
	 */
	public Set<String> getChildren(String classId)
	{
		return getDescendants(classId,1);
	}

	/**
	 * @param classId: the id of the class to search in the map
	 * @return the list of individuals that instantiate the given class
	 */
	public Set<String> getClassIndividuals(String classId)
	{
		if(hasInstanceMap.contains(classId))
			return hasInstanceMap.get(classId);
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
	 * @return the list of data types in the range of the input property
	 */
	public Set<String> getDataRanges(String propId)
	{
		if(dataRange.contains(propId))
			return dataRange.get(propId);
		return new HashSet<String>();
	}
	
	/**
	 * @param classId: the id of the class to search in the map
	 * @return the list of descendants of the input class
	 */
	public Set<String> getDescendants(String classId)
	{
		if(descendantClasses.contains(classId))
			return descendantClasses.keySet(classId);
		return new HashSet<String>();
	}
	
	/**
	 * @param classId: the id of the class to search in the map
	 * @param distance: the distance between the class and its ancestors
	 * @return the list of descendants at the given distance from the input class
	 */
	public Set<String> getDescendants(String classId, int distance)
	{
		HashSet<String> desc = new HashSet<String>();
		if(!descendantClasses.contains(classId))
			return desc;
		for(String i : descendantClasses.keySet(classId))
			for(Relationship r : descendantClasses.get(classId, i))
				if(r.getDistance() == distance)
					desc.add(i);
		return desc;
	}
	
	/**
	 * @param classId: the id of the class to search in the map
	 * @param prop: the relationship property between the class and its ancestors
	 * @return the list of descendants at the given distance from the input class
	 */
	public Set<String> getDescendantsProperty(String classId, String prop)
	{
		HashSet<String> desc = new HashSet<String>();
		if(!descendantClasses.contains(classId))
			return desc;
		for(String i : descendantClasses.keySet(classId))
			for(Relationship r : descendantClasses.get(classId, i))
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
	public Set<String> getDescendants(String classId, int distance, String prop)
	{
		HashSet<String> desc = new HashSet<String>();
		if(!descendantClasses.contains(classId))
			return desc;
		for(String i : descendantClasses.keySet(classId))
			for(Relationship r : descendantClasses.get(classId, i))
				if(r.getDistance() == distance && r.getProperty() == prop)
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
	 * @param classId: the id of the class to search in the map
	 * @return the list of classes disjoint with the given class
	 */
	public Set<String> getDisjoint(String classId)
	{
		if(disjointMap.contains(classId))
			return disjointMap.get(classId);
		return new HashSet<String>();
	}
	
	/**
	 * @param classId: the id of the class to search in the map
	 * @return the list of classes disjoint with the given class
	 * or any of its 'is_a' ancestors
	 */
	public Set<String> getDisjointTransitive(String classId)
	{
		//Get the disjoint clauses for the class
		Set<String> disj = getDisjoint(classId);
		//Then get all superclasses of the class
		Set<String> ancestors = getSuperClasses(classId,false);
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
		if(child == parent)
			return 0;
		if(!ancestorClasses.contains(child, parent))
			return -1;
		Vector<Relationship> rels = ancestorClasses.get(child,parent);
		int distance = rels.get(0).getDistance();
		for(Relationship r : rels)
			if(r.getDistance() < distance)
				distance = r.getDistance();
		return distance;
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
	 * @param classId: the id of the class to search in the map
	 * @return the list of equivalences of the given class
	 */
	public Set<String> getEquivalences(String classId)
	{
		return getDescendants(classId, 0);
	}
	
	/**
	 * @param classId: the id of the class to search in the map
	 * @return the list of classes equivalent to the given class
	 */
	public Set<String> getEquivalentClasses(String classId)
	{
		return getDescendants(classId,0,"");
	}
	
	/**
	 * @param classId: the id of the class to search in the map
	 * @return the list of high level ancestors of the given class
	 */
	public Set<String> getHighLevelAncestors(String classId)
	{
		if(highLevelClasses == null)
			getHighLevelClasses();
		Set<String> ancestors = getAncestors(classId);
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
	 * @param propId: the id of the property to search in the map
	 * @return the list of classes in the range of the input property
	 */
	public Set<String> getObjectRanges(String propId)
	{
		if(objectRange.contains(propId))
			return objectRange.get(propId);
		return new HashSet<String>();
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
	 * @param classId: the id of the class to search in the map
	 * @return the list of direct parents of the given class
	 */
	public Set<String> getParents(String classId)
	{
		return getAncestors(classId,1);
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
	 * @param child: the id of the child class to search in the map
	 * @param parent: the id of the parent class to search in the map
	 * @return the 'best' relationship between the two classes
	 */
	public Relationship getRelationship(String child, String parent)
	{
		if(!ancestorClasses.contains(child, parent))
			return null;
		Relationship rel = ancestorClasses.get(child).get(parent).get(0);
		for(Relationship r : ancestorClasses.get(child).get(parent))
			if(r.compareTo(rel) > 0)
				rel = r;
		return rel;
	}

	/**
	 * @param child: the id of the child class to search in the map
	 * @param parent: the id of the parent class to search in the map
	 * @return the relationships between the two classes
	 */
	public Vector<Relationship> getRelationships(String child, String parent)
	{
		return ancestorClasses.get(child).get(parent);
	}
	
	/**
	 * @param classId: the id of the class to search in the map
	 * @param prop: the relationship property between the class and its ancestors
	 * @return the list of strict siblings of the given class (through the subclass relation)
	 */
	public Set<String> getSiblings(String classId)
	{
		Set<String> parents = getAncestors(classId,1,"");
		HashSet<String> siblings = new HashSet<String>();
		for(String i : parents)
		{
			Set<String> children = getDescendants(i,1,"");
			for(String j : children)
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
	public Set<String> getSiblingsProperty(String classId, String prop)
	{
		Set<String> parents = getAncestors(classId,1,prop);
		HashSet<String> siblings = new HashSet<String>();
		for(String i : parents)
		{
			Set<String> children = getDescendants(i,1,prop);
			for(String j : children)
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
	public Set<String> getSubClasses(String classId, boolean direct)
	{
		if(direct)
			return getDescendants(classId,1,"");
		else
			return getDescendantsProperty(classId,"");
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
	 * @param classId: the id of the class to search in the map
	 * @param direct: whether to return all superclasses or just the direct ones
	 * @return the list of direct or indirect superclasses of the input class
	 */
	public Set<String> getSuperClasses(String classId, boolean direct)
	{
		if(direct)
			return getAncestors(classId,1,"");
		else
			return getAncestorsProperty(classId,"");
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
	 * @param class: the uri of the class to search in the map
	 * @return whether there is a disjoint clause associated with the class
	 */
	public boolean hasDisjoint(String classId)
	{
		return disjointMap.contains(classId);
	}

	/**
	 * @param classId: the uri of the class to search in the map
	 * @return whether there is a disjoint clause associated with the class
	 * or any of its 'is_a' ancestors
	 */
	public boolean hasDisjointTransitive(String classId)
	{
		//Get all superclasses of the class
		Set<String> ancestors = getSuperClasses(classId,false);
		//Plus the parent itself
		ancestors.add(classId);
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
	 * @param child: the id of the child class to search in the map
	 * @param parent: the id of the parent class to search in the map
	 * @param property: the id of the property between child and parent
	 * @return whether there is a relationship between child and parent
	 *  with the given property
	 */
	public boolean hasProperty(String child, String parent, String property)
	{
		Vector<Relationship> rels = getRelationships(child,parent);
		for(Relationship r : rels)
			if(r.getProperty() == property)
				return true;
		return false;
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
	 * @return whether the property is functional
	 */
	public boolean isFunctional(String prop)
	{
		return functional.contains(prop);
	}
	
	/**
	 * @param child: the uri of the child class
	 * @param parent: the uri of the parent class
	 * @return whether the RelationshipMap contains an 'is_a' relationship between child and parent
	 */	
	public boolean isSubclass(String child, String parent)
	{
		if(!descendantClasses.contains(parent,child))
			return false;
		Vector<Relationship> rels = descendantClasses.get(parent,child);
		for(Relationship r : rels)
			if(r.getProperty().equals(""))
				return true;
		return false;
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
	 * @param classId: the id of the class to search in the map
	 * @param direct: whether to return all subclasses or just the direct ones
	 * @return the number of direct or indirect subclasses of the input class
	 */
	public int subClassCount(String classId, boolean direct)
	{
		return getSubClasses(classId,direct).size();
	}
	
	/**
	 * @param classId: the id of the class to search in the map
	 * @param direct: whether to return all superclasses or just the direct ones
	 * @return the number of direct or indirect superclasses of the input class
	 */
	public int superClassCount(String classId, boolean direct)
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
				{
					Vector<Relationship> rel1 = getRelationships(i,j);
					for(int k = 0; k < rel1.size(); k++)
					{
						Relationship r1 = rel1.get(k);
						String p1 = r1.getProperty();
						for(String h : childs)
						{
							Vector<Relationship> rel2 = getRelationships(h,i);
							for(int l = 0; l < rel2.size(); l++)
							{
								Relationship r2 = rel2.get(l);
								String p2 = r2.getProperty();
								//We only do transitive closure if the property is the same (and transitive)
								//for two relationships or one of the properties is 'is_a' (-1)
								if(!(p1.equals("") || p2.equals("") || transitiveOver.contains(p2,p1)))
									continue;
								int dist = r1.getDistance() + r2.getDistance();
								String prop;
								if(p1.equals(p2) || !p1.equals(""))
									prop = p1;
								else
									prop = p2;
								boolean rest = r1.isExclusive() && r2.isExclusive();
								addClassRelationship(h, j, dist, prop, rest);
							}
						}
					}
				}
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