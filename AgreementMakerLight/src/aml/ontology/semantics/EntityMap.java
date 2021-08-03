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
package aml.ontology.semantics;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import aml.AML;
import aml.ontology.EntityType;
import aml.util.data.Map2Couple;
import aml.util.data.Map2Map;
import aml.util.data.Map2Map2Set;
import aml.util.data.Map2Set;
import aml.util.data.Map2Triple;


public class EntityMap
{

	//Attributes

	//0) The map of entities to EntityTypes 
	private Map2Set<String,EntityType> entityType;
	private Map2Set<EntityType,String> typeEntity;

	//1) Structural data on classes (& class expressions)
	//Equivalence (distance=0) and Subclass relations (distance>0) between classes with transitive closure (min distance only)
	private Map2Map<String,String,Integer> ancestorClasses; //Class -> Ancestor -> Distance
	private Map2Map<String,String,Integer> descendantClasses; //Class -> Descendant -> Distance
	//Relations between classes and class expressions with transitive closure
	private Map2Map<String,String,Boolean> classExpressions; //Class -> Expression -> isEquivalence (or isSubclass)
	//Subclass relations between class expressions with transitive closure
	private Map2Set<String,String> ancestorExpressions; //Expression -> Broader Expression
	//Disjointness relations between classes (direct only, no transitive closure)
	private Map2Set<String,String> disjointMap; //Class/Expression -> Disjoint Class/Expression
	//High level classes
	private HashSet<String> highLevelClasses;

	//2) Structural data on individuals
	//Relationships between individuals and classes
	private Map2Set<String,String> instanceOfMap; //Individual -> Class 
	private Map2Set<String,String> hasInstanceMap; //Class -> Individual
	//Relationships between individuals
	private Map2Set<String,String> sameIndivAs; //Individual -> Set of Equivalent Individuals
	private Map2Map2Set<String,String,String> activeRelation; //Source Individual -> Target Individual -> Property
	private Map2Map2Set<String,String,String> activeRelationIndividual; //Property -> Source Individual -> Target Individual 
	private Map2Map2Set<String,String,String> passiveRelation; //Target Individual -> Source Individual -> Property

	//3) Structural data on properties
	//Hierarchical and inverse relations between properties (and expressions)
	private Map2Set<String,String> subProp; //Property -> SubProperty
	private Map2Set<String,String> superProp; //Property -> SuperProperty
	private Map2Set<String,String> inverseProp; //Property -> InverseProperty
	//Property domains and ranges (property to class or datatype)
	private Map2Set<String,String> domain; //Property -> Domain (class)
	private Map2Set<String,String> range; //Property -> Range (class/datatype)

	//4) Properties of properties
	//Asymmetric, functional, irreflexive, reflexive, symmetric properties
	private HashSet<String> asymmetric;
	private HashSet<String> functional;
	private HashSet<String> irreflexive;
	private HashSet<String> reflexive;
	private HashSet<String> symmetric;
	//Simple property chains (transitive: P * P = P; transitive over: P * Q = P)
	private Map2Set<String,String> transitiveOver; //Property1 -> Property2 over which 1 is transitive
	//Complex property chains (e.g. father * father = grandfather)
	private Map2Set<String,Vector<String>> propChain; //Property -> Property Chain

	//5) Class expressions
	//Expression -> ClassExpressionType
	private HashMap<String,ClassExpressionType> expressionTypes;
	//Intersection expression -> set of Expressions in the intersection
	private Map2Set<String,String> intersect;
	//Union expression -> set of Expressions in the union
	private Map2Set<String,String> union;
	//AllValues restriction -> {restricted Property; Expression in the restricted range}
	private Map2Couple<String,String,String> allValues;
	//HasValue restriction -> {restricted Property; Literal in the restricted range}
	private Map2Couple<String,String,String> hasValue;
	//MinCardinality restriction -> {restricted Property; Expression in the restricted range; Cardinality}
	private Map2Triple<String,String,String,Integer> minCard; //includes someValues = minCard(1)
	//MaxCardinality restriction -> {restricted Property; Expression in the restricted range; Cardinality}
	private Map2Triple<String,String,String,Integer> maxCard;
	//ExactCardinality restriction -> {restricted Property; Expression in the restricted range; Cardinality}
	private Map2Triple<String,String,String,Integer> exactCard;

	//Constructors

	/**
	 * Creates a new empty RelationshipMap
	 */
	public EntityMap()
	{
		entityType = new Map2Set<String,EntityType>();
		typeEntity = new Map2Set<EntityType,String>();
		descendantClasses = new Map2Map<String,String,Integer>();
		ancestorClasses = new Map2Map<String,String,Integer>();
		classExpressions = new Map2Map<String,String,Boolean>();
		ancestorExpressions = new Map2Set<String,String>();
		disjointMap = new Map2Set<String,String>();
		instanceOfMap = new Map2Set<String,String>();
		hasInstanceMap = new Map2Set<String,String>();
		sameIndivAs = new Map2Set<String,String>();
		activeRelation = new Map2Map2Set<String,String,String>();
		activeRelationIndividual = new Map2Map2Set<String,String,String>();
		passiveRelation = new Map2Map2Set<String,String,String>();
		subProp = new Map2Set<String,String>();
		superProp = new Map2Set<String,String>();
		inverseProp = new Map2Set<String,String>();
		domain = new Map2Set<String,String>();
		range = new Map2Set<String,String>();
		asymmetric = new HashSet<String>();
		functional = new HashSet<String>();
		irreflexive = new HashSet<String>();
		reflexive = new HashSet<String>();
		symmetric = new HashSet<String>();
		transitiveOver = new Map2Set<String,String>();
		propChain = new Map2Set<String,Vector<String>>();
		expressionTypes = new HashMap<String,ClassExpressionType>();
		intersect = new Map2Set<String,String>();
		union = new Map2Set<String,String>();
		allValues = new Map2Couple<String,String,String>();
		hasValue = new Map2Couple<String,String,String>();
		minCard = new Map2Triple<String,String,String,Integer>();
		maxCard = new Map2Triple<String,String,String,Integer>();
		exactCard = new Map2Triple<String,String,String,Integer>();
	}

	//Public Methods

	/**
	 * Adds an allValues restriction to the EntityMap
	 * @param exp: the uri of the allValues class expression
	 * @param prop: the restricted property
	 * @param range: the restricted range (class expression)
	 */
	public void addAllValues(String exp, String prop, String range)
	{
		expressionTypes.put(exp,ClassExpressionType.ALL_VALUES);
		allValues.add(exp, prop, range);
	}

	/**
	 * Sets a  property as asymmetric
	 * @param prop: the property to set as asymmetric
	 */
	public void addAsymmetric(String prop)
	{
		asymmetric.add(prop);
	}

	/**
	 * Adds a new relation between a class and a class expression
	 * @param classUri: the uri of the class
	 * @param expUri: the uri of the class expression
	 * @param isEquiv: whether the class is equivalent or subclass of the expression
	 */
	public void addClassExpression(String classUri, String expUri, boolean isEquiv)
	{
		classExpressions.add(classUri, expUri, isEquiv);
	}

	/**
	 * Adds a new disjointness relations between two classes
	 * @param class1: the uri of the first disjoint class
	 * @param class2: the uri of the second disjoint class
	 */
	public void addDisjoint(String class1, String class2)
	{
		disjointMap.add(class1, class2);
		disjointMap.add(class2, class1);
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
	public void addEquivalentClasses(String class1, String class2)
	{
		addSubclass(class1,class2,0);
	}

	/**
	 * Adds an exact cardinality restriction to the EntityMap
	 * @param exp: the uri of the allValues class expression
	 * @param prop: the restricted property
	 * @param range: the restricted range (class expression)
	 * @param int: the restricted cardinality
	 */
	public void addExactCardinality(String exp, String prop, String range, int card)
	{
		expressionTypes.put(exp,ClassExpressionType.EXACT_CARDINALITY);
		exactCard.add(exp, prop, range, card);
	}

	/**
	 * Adds an equivalence relationship between two classes
	 * @param class1: the uri of the first equivalent class
	 * @param class2: the uri of the second equivalent class
	 */
	public void addEquivalentIndividuals(String indiv1, String indiv2)
	{
		sameIndivAs.add(indiv1, indiv2);
		sameIndivAs.add(indiv2, indiv1);
	}

	/**
	 * @param prop: the property to set as functional
	 */
	public void addFunctional(String prop)
	{
		functional.add(prop);
	}

	/**
	 * Adds a hasValues restriction to the EntityMap
	 * @param exp: the uri of the allValues class expression
	 * @param prop: the restricted property
	 * @param range: the restricted range (class expression)
	 */
	public void addHasValues(String exp, String prop, String range)
	{
		expressionTypes.put(exp,ClassExpressionType.HAS_VALUE);
		hasValue.add(exp, prop, range);
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
		activeRelationIndividual.add(prop, indiv1,indiv2);
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
	 * Adds an intersection expression to the EntityMap
	 * @param exp: the uri of the allValues class expression
	 * @param inter: the set of classes in the intersection
	 */
	public void addIntersection(String exp, Set<String> inter)
	{
		expressionTypes.put(exp,ClassExpressionType.INTERSECTION);
		intersect.addAll(exp,inter);
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
	 * Adds a max cardinality restriction to the EntityMap
	 * @param exp: the uri of the allValues class expression
	 * @param prop: the restricted property
	 * @param range: the restricted range (class expression)
	 * @param int: the restricted cardinality
	 */
	public void addMaxCardinality(String exp, String prop, String range, int card)
	{
		expressionTypes.put(exp,ClassExpressionType.MAX_CARDINALITY);
		maxCard.add(exp, prop, range, card);
	}

	/**
	 * Adds a min cardinality restriction to the EntityMap
	 * @param exp: the uri of the allValues class expression
	 * @param prop: the restricted property
	 * @param range: the restricted range (class expression)
	 * @param int: the restricted cardinality
	 */
	public void addMinCardinality(String exp, String prop, String range, int card)
	{
		expressionTypes.put(exp,ClassExpressionType.MIN_CARDINALITY);
		minCard.add(exp, prop, range, card);
	}

	/**
	 * Adds a property chain axiom to a given property
	 * @param prop: the uri of the property equivalent to the chain
	 * @param chain: the property chain equivalent to the prop
	 */
	public void addPropertyChain(String prop, Vector<String> chain)
	{
		propChain.add(prop, chain);
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
		addSubclass(child,parent,1);
	}

	/**
	 * Adds a subclass relationship between two classes with a given distance
	 * @param child: the uri of the child class
	 * @param parent: the uri of the parent class
	 * @param distance: the distance (number of edges) between the classes
	 */
	public void addSubclass(String child, String parent, int distance)
	{
		descendantClasses.add(parent,child,distance);
		ancestorClasses.add(child,parent,distance);
	}

	/**
	 * Adds a subclass relationship between two classes with a given distance
	 * @param child: the uri of the child class expression
	 * @param parent: the uri of the parent class expression
	 */
	public void addSubexpression(String child, String parent)
	{
		ancestorExpressions.add(child,parent);
	}

	/**
	 * Adds a relationship between two properties
	 * @param child: the uri of the child property
	 * @param parent: the uri of the parent property
	 */
	public void addSubproperty(String child, String parent)
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
	 * Adds an union expression to the EntityMap
	 * @param exp: the uri of the allValues class expression
	 * @param un: the set of classes in the union
	 */
	public void addUnion(String exp, Set<String> un)
	{
		expressionTypes.put(exp,ClassExpressionType.UNION);
		union.addAll(exp,un);
	}

	/**
	 * @param uri: the uri to add to AML
	 */
	public void addURI(String uri, EntityType t)
	{
		entityType.add(uri,t);
		typeEntity.add(t,uri);
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
			Set<String> ancs = new HashSet<String>(getSuperclasses(class2));
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
		for(String suburi : getSubclasses(uri))
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
	 * @param propId: the id of the property to search in the map
	 * @return the Map2Set of individuals who are related through the given property
	 */
	public Map2Set<String, String> getActiveRelationIndividuals(String propId)
	{
		if(activeRelationIndividual.contains(propId))
			return activeRelationIndividual.get(propId);
		return new Map2Set<String, String>();
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
	 * @return the set of expressions that the given class is equivalent to or a subclass of
	 */
	public Set<String> getClassExpressions(String uri)
	{
		if(classExpressions.contains(uri))
			return classExpressions.keySet(uri);
		return new HashSet<String>();
	}

	/**
	 * @param uri: the id of the class to search in the map
	 * @param isEquiv: whether to get equivalent or subclass expressions
	 * @return the set of expressions related to the given class according to isEquiv
	 */
	public Set<String> getClassExpressions(String uri, boolean isEquiv)
	{
		HashSet<String> exps = new HashSet<String>();
		if(classExpressions.contains(uri))
			for(String s : classExpressions.keySet(uri))
				if(classExpressions.get(uri, s) == isEquiv)
					exps.add(s);
		return exps;
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
		Vector<String> subclasses = new Vector<String>(getSubclasses(it.next(),1));
		while(it.hasNext())
		{
			HashSet<String> s = new HashSet<String>(getSubclasses(it.next(),1));
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
		HashSet<String> toSearch = new HashSet<String>();
		toSearch.add(uri);
		if(isClass(uri))
		{
			toSearch.addAll(getSuperclasses(uri));
			toSearch.addAll(getClassExpressions(uri));
		}
		else if(entityType.get(uri).contains(EntityType.CLASS_EXPRESSION))
			toSearch.addAll(getSuperexpressions(uri));
		//Get the disjoint clauses for the class/expression
		Set<String> disj = getDisjoint(uri);
		if(isClass(uri))
		{
			//Then get all superclasses of the class
			Set<String> ancestors = getSuperclasses(uri);
			//For each superclass
			for(String i : ancestors)
				//Add its disjoint clauses to the list
				disj.addAll(getDisjoint(i));
		}
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
	 * @param t: the EntityType to search in the map
	 * @return the set of entities of the given type
	 */
	public Set<String> getEquivalences(EntityType t)
	{
		return typeEntity.get(t);
	}

	/**
	 * @param uri: the id of the class to search in the map
	 * @return the set of equivalences of the given class
	 */
	public Set<String> getEquivalences(String uri)
	{
		return getSubclasses(uri, 0);
	}

	/**
	 * @param uri: the id of the class to search in the map
	 * @return the set of classes equivalent to the given class
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
	 * @return the whole map of equivalences between individuals
	 */
	public Map2Set<String, String> getEquivalentIndividuals()
	{
		return sameIndivAs;
	}
	/**
	 * @param uri: the id of the individual to search in the map
	 * @return the set of individuals equivalent to the given individual
	 */
	public Set<String> getEquivalentIndividuals(String uri)
	{
		if(!sameIndivAs.contains(uri))
			return new HashSet<String>();
		return sameIndivAs.get(uri);

	}

	/**
	 * @param uri: the uri of the class expression
	 * @return the type of class expression
	 */
	public ClassExpressionType getExpressionType(String uri)
	{
		return expressionTypes.get(uri);
	}

	/**
	 * @param uri: the id of the class to search in the map
	 * @return the set of high level ancestors of the given class
	 */
	public Set<String> getHighLevelAncestors(String uri)
	{
		if(highLevelClasses == null)
			getHighLevelClasses();
		Set<String> ancestors = getSuperclasses(uri);
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

		//First get the very top classes
		HashSet<String> sourceTop = new HashSet<String>();
		HashSet<String> targetTop = new HashSet<String>();
		Set<String> ancestors = descendantClasses.keySet();
		//Which are classes that have children but not parents
		//NOTE: This may not work out well if the ontologies are not is_a complete
		for(String a : ancestors)
		{
			if(getSuperclasses(a,1).size() == 0 && getSubclasses(a,1).size() > 0)
			{
				if(AML.getInstance().getSource().contains(a))
					sourceTop.add(a);
				if(AML.getInstance().getTarget().contains(a))
					targetTop.add(a);
			}
		}
		//Now we go down the ontologies until we reach a significant branching
		while(sourceTop.size() < 3)
		{
			HashSet<String> newTop = new HashSet<String>();
			for(String a : sourceTop)
				newTop.addAll(getSubclasses(a,1));
			sourceTop = newTop;
		}
		while(targetTop.size() < 3)
		{
			HashSet<String> newTop = new HashSet<String>();
			for(String a : targetTop)
				newTop.addAll(getSubclasses(a,1));
			targetTop = newTop;
		}
		highLevelClasses.addAll(sourceTop);
		highLevelClasses.addAll(targetTop);

		return highLevelClasses;
	}

	/**
	 * @return the whole table of active relations
	 */
	public Map2Map2Set<String,String,String> getIndividualActiveRelations()
	{
		return activeRelation;
	}

	/**
	 * @param indivId: the id of the individual to search in the map
	 * @return the set of individuals to which the given individual is actively related
	 */
	// TODO: temporary fix
	public Set<String> getIndividualActiveRelations(String indivId)
	{
		if(activeRelation.contains(indivId)) 
		{
			if(sameIndivAs.size()>0) 
				return removeEquivalentIndividuals(activeRelation.keySet(indivId));
			else return activeRelation.keySet(indivId);
		}	
		return new HashSet<String>();
	}

	/**
	 * @param indivId: the id of the individual to search in the map
	 * @return the set of classes instanced by the given individual
	 */
	public Set<String> getIndividualClasses(String indivId)
	{
		if(instanceOfMap.contains(indivId))
			return instanceOfMap.get(indivId);
		return new HashSet<String>();
	}

	/**
	 * @param indivId: the id of the individual to search in the map
	 * @return the set of classes instanced by the given individual and its ancestors (i.e. with transitive closure)
	 */
	public Set<String> getIndividualClassesTransitive(String indivId)
	{
		Set<String> result = new HashSet<String>();
		if(instanceOfMap.contains(indivId)) 
		{
			for (String c: instanceOfMap.get(indivId)) 
			{
				result.add(c);
				result.addAll(getSuperclasses(c));
			}
			return result;
		}
		return new HashSet<String>();
	}

	/**
	 * @param indivId: the id of the individual to search in the map
	 * @return the set of individuals that are passively related with of the given individual
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
	 * @return the set of object properties actively relating sourceInd to targetInd
	 */
	public Set<String> getIndividualProperties(String sourceInd, String targetInd)
	{
		if(activeRelation.contains(sourceInd,targetInd))
			return activeRelation.get(sourceInd,targetInd);
		return new HashSet<String>();
	}

	/**
	 * @param sourceInd: the id of the source individual in the relation
	 * @param targetInd: the id of the target individual in the relation
	 * @return the set of object properties actively relating sourceInd to targetInd
	 */
	public Set<String> getIndividualPropertiesTransitive(String sourceInd, String targetInd)
	{
		Set<String> result = new HashSet<String>();
		if(activeRelation.contains(sourceInd,targetInd)) 
		{
			for (String p: activeRelation.get(sourceInd,targetInd))
			{
				result.add(p);
				result.addAll(getSuperproperties(p));
			}
			return result;
		}
		return new HashSet<String>();
	}
	
	/**
	 * @return the set of individuals with active relations
	 */
	public Set<String> getIndividualsWithActiveRelations()
	{
		return activeRelation.keySet();
	}

	/**
	 * @return the set of individuals with active relations
	 */
	public Set<String> getIndividualsWithPassiveRelations()
	{
		return passiveRelation.keySet();
	}

	/**
	 * @param indivId: the id of the individual to search in the map
	 * @return the set of classes instanced by the given individual
	 */
	public Set<String> getInstancedClasses()
	{
		return hasInstanceMap.keySet();
	}

	/**
	 * @param uri: the uri of the intersection expression
	 * @return the class expressions in the intersection
	 */
	public Set<String> getIntersection(String uri)
	{
		return intersect.get(uri);
	}

	/**
	 * @param propId: the id of the property to search in the map
	 * @return the set of inverse properties of the input property
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
	 * @param indivId: the id of the individual to search in the map
	 * @param prop: the property relating the individuals
	 * @return the set of 'parent' relations of the given individual
	 */
	public Set<String> getParentIndividuals(String indivId, String prop)
	{
		if(activeRelation.contains(indivId,prop))
			return activeRelation.get(indivId,prop);
		return new HashSet<String>();
	}

	/**
	 * @param prop: the uri of the property with the property chain(s)
	 * @return the set of property chains that are equivalent to the prop
	 */
	public Set<Vector<String>> getPropertyChain(String prop)
	{
		return propChain.get(prop);
	}

	/**
	 * @param propId: the id of the property to search in the map
	 * @return the set of classes / datatypes in the range of the input property
	 */
	public Set<String> getRanges(String propId)
	{
		if(range.contains(propId))
			return range.get(propId);
		return new HashSet<String>();
	}

	/**
	 * @param uri: the uri of the restriction expression
	 * @return the cardinality restricted in the expression
	 */
	public Integer getRestrictedCardinality(String uri)
	{
		ClassExpressionType t = getExpressionType(uri);
		if(t.equals(ClassExpressionType.EXACT_CARDINALITY))
			return exactCard.get3(uri);
		else if(t.equals(ClassExpressionType.MAX_CARDINALITY))
			return maxCard.get3(uri);
		else if(t.equals(ClassExpressionType.MIN_CARDINALITY))
			return minCard.get3(uri);
		else
			return null;
	}

	/**
	 * @param uri: the uri of the restriction expression
	 * @return the property restricted in the expression
	 */
	public String getRestrictedProperty(String uri)
	{
		ClassExpressionType t = getExpressionType(uri);
		if(t.equals(ClassExpressionType.ALL_VALUES))
			return allValues.get1(uri);
		else if(t.equals(ClassExpressionType.HAS_VALUE))
			return hasValue.get1(uri);
		else if(t.equals(ClassExpressionType.EXACT_CARDINALITY))
			return exactCard.get1(uri);
		else if(t.equals(ClassExpressionType.MAX_CARDINALITY))
			return maxCard.get1(uri);
		else if(t.equals(ClassExpressionType.MIN_CARDINALITY))
			return minCard.get1(uri);
		else
			return null;
	}

	/**
	 * @param uri: the uri of the restriction expression
	 * @return the range restricted in the expression
	 */
	public String getRestrictedRange(String uri)
	{
		ClassExpressionType t = getExpressionType(uri);
		if(t.equals(ClassExpressionType.ALL_VALUES))
			return allValues.get2(uri);
		else if(t.equals(ClassExpressionType.HAS_VALUE))
			return hasValue.get2(uri);
		else if(t.equals(ClassExpressionType.EXACT_CARDINALITY))
			return exactCard.get2(uri);
		else if(t.equals(ClassExpressionType.MAX_CARDINALITY))
			return maxCard.get2(uri);
		else if(t.equals(ClassExpressionType.MIN_CARDINALITY))
			return minCard.get2(uri);
		else
			return null;
	}

	/**
	 * @param uri: the id of the class to search in the map
	 * @param prop: the relationship property between the class and its ancestors
	 * @return the set of strict siblings of the given class (through the subclass relation)
	 */
	public Set<String> getSiblings(String uri)
	{
		Set<String> parents = getSuperclasses(uri,1);
		HashSet<String> siblings = new HashSet<String>();
		for(String i : parents)
		{
			Set<String> children = getSubclasses(i,1);
			for(String j : children)
				if(!j.equals(uri))
					siblings.add(j);
		}
		return siblings;
	}

	/**
	 * @param uri: the id of the class to search in the map
	 * @return the set of subclasses of the input class
	 */
	public Set<String> getSubclasses(String uri)
	{
		if(descendantClasses.contains(uri))
			return descendantClasses.keySet(uri);
		return new HashSet<String>();
	}

	/**
	 * @param uri: the id of the class to search in the map
	 * @param distance: the distance between the class and its subclasses
	 * @return the set of subclasses at the given distance from the input class
	 */
	public Set<String> getSubclasses(String uri, int distance)
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
	 * @param propId: the id of the property to search in the map
	 * @return the set of sub-properties of the input property
	 */
	public Set<String> getSubproperties(String propId)
	{
		if(subProp.contains(propId))
			return new HashSet<String>(subProp.get(propId));
		else
			return new HashSet<String>();
	}

	/**
	 * @param uri: the id of the class to search in the map
	 * @return the set of all superclasses of the given class
	 */
	public Set<String> getSuperclasses(String uri)
	{
		if(ancestorClasses.contains(uri))
			return ancestorClasses.keySet(uri);
		return new HashSet<String>();
	}

	/**
	 * @param uri: the uri of the class to search in the map
	 * @param distance: the distance between the class and its superclasses
	 * @return the set of superclasses at the given distance from the input class
	 */
	public Set<String> getSuperclasses(String uri, int distance)
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
	 * @param uri: the uri of the class expression to search in the map
	 * @return the set of superexpressions of the given expression
	 */
	public Set<String> getSuperexpressions(String uri)
	{
		if(ancestorExpressions.contains(uri))
			return ancestorClasses.keySet(uri);
		return new HashSet<String>();
	}

	/**
	 * @param propId: the id of the property to search in the map
	 * @return the set of super-properties of the input property
	 */
	public Set<String> getSuperproperties(String propId)
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
	 * @param uri: the uri of the union expression
	 * @return the class expressions in the union
	 */
	public Set<String> getUnion(String uri)
	{
		return union.get(uri);
	}

	/**
	 * @return the URIs in the EntityMap
	 */
	public Set<String> getURIs()
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
		Set<String> ancestors = getSuperclasses(uri);
		//Plus the parent itself
		ancestors.add(uri);
		//Run through the set of superclasses
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
		return entityType.contains(uri) && entityType.get(uri).contains(EntityType.CLASS);
	}

	/**
	 * @param uri: the uri of the Ontology entity
	 * @return whether the entity is a Data Property
	 */
	public boolean isDataProperty(String uri)
	{
		return entityType.contains(uri) && entityType.get(uri).contains(EntityType.DATA_PROP);
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
		return isNamedIndividual(uri) || isAnonymousIndividual(uri);
	}

	/**
	 * @param uri: the uri of the Ontology entity
	 * @return whether the entity is a named Individual
	 */
	public boolean isNamedIndividual(String uri)
	{
		return entityType.contains(uri) && entityType.get(uri).contains(EntityType.INDIVIDUAL);
	}

	/**
	 * @param uri: the uri of the Ontology entity
	 * @return whether the entity is an anonymous Individual
	 */
	public boolean isAnonymousIndividual(String uri)
	{
		return entityType.contains(uri) && entityType.get(uri).contains(EntityType.ANON_INDIVIDUAL);
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
		return entityType.contains(uri) && entityType.get(uri).contains(EntityType.OBJECT_PROP);
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
	 * @return a set of individuals with no equivalent individuals in the same set
	 */
	private Set<String> removeEquivalentIndividuals(Set<String> individuals)
	{
		Set<String> result = new HashSet<String>();
		Set<String> resultEquivalents = new HashSet<String>();

		for(String i: individuals) 
		{
			if(!resultEquivalents.contains(i)) 
			{
				result.add(i);
				if(sameIndivAs.contains(i))
					resultEquivalents.addAll(getEquivalentIndividuals(i));
			}
		}
		return result;
	}

	/**
	 * Removes relationship between instance and class from instance map
	 */
	public void removeIndividualClassAssignment(String indv, String clas)
	{
		instanceOfMap.remove(indv, clas);
		hasInstanceMap.remove(clas, indv);
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
				Set<String> childs = getSubclasses(i,1);
				childs.addAll(getEquivalences(i));
				Set<String> pars = getSuperclasses(i,distance);
				for(String j : pars)
					for(String h : childs)
						addSubclass(h, j, getDistance(i,j) + getDistance(h,i));
			}
		}

		// Transitive closure for equivalent individuals
		// For the purpose of comments and variables, consider:
		// I1 = individual from ontology A
		// I2 = individual from ontology A, who has some relationship to I1
		// I3 = individual from ontology B, equivalent to I1
		// I4 = individual from ontology B, who has some relationship to I3 and is equivalent to I2 
		for (String i1: sameIndivAs.keySet()) 
		{
			// "i1" in this loop will eventually be I3, that is why we only add i3's entities to i1 map
			for(String i3: getEquivalentIndividuals(i1)) 
			{
				//Add i3's classes to i1
				for(String eqClass: getIndividualClasses(i3)) 
						addInstance(i1, eqClass);
				//Add i3's active relations to i1
				for (String i4: getIndividualActiveRelations(i3)) 
				{
					// I1:I4
					for(String relation: activeRelation.get(i3, i4)) 
					{
						addIndividualRelationship(i1, i4, relation);
						for(String i5: getEquivalentIndividuals(i4)) 
						{
							addIndividualRelationship(i1, i5, relation); // i5 may be i2
						}
					}	
				}
				//Add i3's passive relations to i1
				for (String i4: getIndividualPassiveRelations(i3)) 
				{
					// I6:I1
					for(String relation: passiveRelation.get(i3, i4)) 
					{
						addIndividualRelationship(i4, i1, relation);
						for(String i5: getEquivalentIndividuals(i4)) 
						{
							addIndividualRelationship(i5, i1, relation); // i5 may be i2
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
		Set<String> descendants = getSubclasses(child);
		//Plus the child itself
		descendants.add(child);
		//Then all ancestors of the parent
		Set<String> ancestors = getSuperclasses(parent);
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