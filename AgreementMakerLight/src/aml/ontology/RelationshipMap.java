/******************************************************************************
* Copyright 2013-2013 LASIGE                                                  *
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
* The map of relationships in an Ontology, including 'is a' and 'part of'     *
* relationships and disjoint clauses.                                         *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 22-10-2013                                                            *
******************************************************************************/
package aml.ontology;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import aml.util.Table2;
import aml.util.Table2Plus;


public class RelationshipMap
{
	
//Attributes

	//Map between ancestor terms and their descendants
	private Table2Plus<Integer,Integer,Relationship> descendantMap;
	//Map between descendant terms and their ancestors
	private Table2Plus<Integer,Integer,Relationship> ancestorMap;
	//List of high level terms
	private HashSet<Integer> highLevelTerms;
	//Map between disjoint terms
	private Table2<Integer,Integer> disjointMap;
	//Map between terms and their intersections
	private HashMap<Set<Integer>,Integer> equivalenceMap;
	
//Constructors

	/**
	 * Creates a new empty RelationshipMap, initializing the storage HashMap
	 */
	public RelationshipMap()
	{
		descendantMap = new Table2Plus<Integer,Integer,Relationship>();
		ancestorMap = new Table2Plus<Integer,Integer,Relationship>();
		disjointMap = new Table2<Integer,Integer>();
		equivalenceMap = new HashMap<Set<Integer>,Integer>();
	}
	
	/**
	 * Creates a new RelationshipMap that is a copy of rm
	 * @param rm: the RelationshipMap to copy
	 */
	public RelationshipMap(RelationshipMap rm)
	{
		descendantMap = new Table2Plus<Integer,Integer,Relationship>();
		ancestorMap = new Table2Plus<Integer,Integer,Relationship>();
		Set<Integer> childs = rm.ancestorMap.keySet();
		for(Integer i : childs)
		{
			Set<Integer> pars = rm.ancestorMap.keySet(i);
			for(Integer j : pars)
			{
				Relationship r = rm.ancestorMap.get(i,j);
				addRelationship(i, j, r.getDistance(), r.getType());
			}
		}
		disjointMap = new Table2<Integer,Integer>();
		Set<Integer> disj = rm.disjointMap.keySet();
		for(Integer i : disj)
		{
			Vector<Integer> d = rm.disjointMap.get(i);
			for(Integer j : d)
				addDisjoint(i, j);
		}
	}

//Public Methods

	/**
	 * Adds a new disjoint clause between two terms if it doesn't exist
	 * @param one: the index of the first disjoint term
	 * @param two: the index of the second disjoint term
	 */
	public void addDisjoint(int one, int two)
	{
		//The disjointMap keeps disjoint clauses in both directions
		disjointMap.add(one, two);
		disjointMap.add(two, one);
	}
	
	/**
	 * Adds a new equivalence clause between the intersection list and its equivalent term
	 * @param interList: the list of terms that when intersected are equivalent to equiv
	 * @param equiv: the term that is equivalent to the intersection list
	 */
	public void addEquivalence(Set<Integer> interList, int equiv)
	{
		equivalenceMap.put(interList,equiv);
	}
	
	/**
	 * Adds a direct 'is_a' relationship between two terms
	 * @param child: the index of the child term
	 * @param parent: the index of the parent term
	 */
	public void addIsAEdge(int child, int parent)
	{
		addRelationship(child,parent,1,true);
	}
	
	/**
	 * Adds a direct 'part_of' relationship between two terms
	 * @param child: the index of the child term
	 * @param parent: the index of the parent term
	 */
	public void addPartOfEdge(int child, int parent)
	{
		addRelationship(child,parent,1,false);
	}
	
	/**
	 * Adds a relationship between two terms with a given distance and type
	 * @param child: the index of the child term
	 * @param parent: the index of the parent term
	 * @param distance: the distance (number of edges) between the terms
	 * @param isA: the type of relationship (true if 'is_a', false if 'part_of')
	 */
	public void addRelationship(int child, int parent, int distance, boolean isA)
	{
		//Create the relationship
		Relationship r = new Relationship(distance,isA);
		//Then update the MultiMaps
		descendantMap.addUpgrade(parent,child,r);
		ancestorMap.addUpgrade(child,parent,r);
	}
	
	/**
	 * Adds a relationship between two terms
	 * @param child: the index of the child term
	 * @param parent: the index of the parent term
	 * @param rel: the relationship between the terms
	 */
	public void addRelationship(int child, int parent, Relationship rel)
	{
		//Update the MultiMaps
		descendantMap.addUpgrade(parent,child,rel);
		ancestorMap.addUpgrade(child,parent,rel);
	}
	
	/**
	 * Adds a relationship between two terms with transitive closure
	 * @param child: the index of the child term
	 * @param parent: the index of the parent term
	 * @param rel: the relationship between the terms
	 */
	public void addRelationshipTransitive(int child, int parent, Relationship rel)
	{
		//If the terms are already related, do nothing
		if(this.contains(child,parent))
			return;
		//Get all descendants of the child
		Vector<Integer> descendants = getDescendants(child);
		//Plus the child itself
		descendants.add(child);
		//Then all ancestors of the parent
		Vector<Integer> ancestors = getAncestors(parent);
		//Plus the parent itself
		ancestors.add(parent);
		
		//For each descendant
		for(Integer i : descendants)
		{
			//And each ancestor
			for(Integer j : ancestors)
			{
				Relationship r = new Relationship(rel.getDistance(),rel.getType());
				r.extendWith(getRelationship(i,child));
				r.extendWith(getRelationship(parent,j));
				//Then add the relationship
				addRelationship(i, j, r);
			}
		}
	}
		
	/**
	 * @param one: the first term to check for disjointness
	 * @param two: the second term to check for disjointness
	 * @return whether one and two are disjoint considering transitivity
	 */
	public boolean areDisjoint(int one, int two)
	{
		//Get the transitive disjoint clauses involving term one
		Vector<Integer> disj = getDisjointTransitive(one);
		if(disj.size() > 0)
		{
			//Then get the list of 'is_a' ancestors of term two
			Vector<Integer> ancs = getAncestors(two,true);
			//Including term two itself
			ancs.add(two);
		
			//Two terms are disjoint if the list of transitive disjoint clauses
			//involving one of them contains the other or any of its 'is_a' ancestors
			for(Integer i : ancs)
				if(disj.contains(i))
					return true;
		}
		return false;
	}
	
	/**
	 * @param child: the index of the child term
	 * @param parent: the index of the parent term
	 * @return whether the RelationshipMap contains a relationship between child and parent
	 */
	public boolean contains(int child, int parent)
	{
		return ancestorMap.contains(child,parent);
	}
	
	/**
	 * @param child: the index of the child term
	 * @param parent: the index of the parent term
	 * @return whether the RelationshipMap contains an 'is_a' relationship between child and parent
	 */	
	public boolean containsIsA(int child, int parent)
	{
		Relationship r = new Relationship(1,true);
		return ancestorMap.contains(child,parent,r);
	}
	
	/**
	 * @param term: the id of the term to search in the map
	 * @return the number of direct is_a parents of the given term
	 */
	public int countIsAParents(int term)
	{
		return getAncestors(term,1,true).size();
	}

	/**
	 * @param term: the id of the term to search in the map
	 * @return the number of direct is_a parents of the given term
	 */
	public int countParents(int term)
	{
		return getAncestors(term,1).size();
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
	 * @param term: the id of the term to search in the map
	 * @return the list of ancestors of the given term
	 */
	public Vector<Integer> getAncestors(int term)
	{
		Vector<Integer> asc = new Vector<Integer>(0,1);
		Set<Integer> c = ancestorMap.keySet(term);
		if(c != null)
			asc.addAll(c);
		return asc;
	}
	
	/**
	 * @param term: the id of the term to search in the map
	 * @param distance: the distance between the term and its ancestors
	 * @return the list of ancestors at the given distance from the input term
	 */
	public Vector<Integer> getAncestors(int term, int distance)
	{
		Vector<Integer> asc = new Vector<Integer>(0,1);
		Set<Integer> c = ancestorMap.keySet(term);
		if(c != null)
		{
			for(Integer i : c)
				if(ancestorMap.get(term,i).getDistance() == distance)
					asc.add(i);
		}
		return asc;
	}
	
	/**
	 * @param term: the id of the term to search in the map
	 * @param type: the type of relationship between the term and its ancestors
	 * @return the list of ancestors of the input term that are of the given type
	 */
	public Vector<Integer> getAncestors(int term, boolean type)
	{
		Vector<Integer> asc = new Vector<Integer>(0,1);
		Set<Integer> c = ancestorMap.keySet(term);
		if(c != null)
		{
			for(Integer i : c)
				if(ancestorMap.get(term,i).getType() == type)
					asc.add(i);
		}
		return asc;
	}
	
	/**
	 * @param term: the id of the term to search in the map
	 * @param distance: the distance between the term and its ancestors
	 * @param type: the type of relationship between the term and its ancestors
	 * @return the list of ancestors of the input term that are at the given
	 * distance and of the given type
	 */
	public Vector<Integer> getAncestors(int term, int distance, boolean type)
	{
		Vector<Integer> asc = new Vector<Integer>(0,1);
		Set<Integer> c = ancestorMap.keySet(term);
		if(c != null)
		{
			for(Integer i : c)
				if(ancestorMap.get(term,i).getType() == type &&
					ancestorMap.get(term,i).getDistance() == distance)
					asc.add(i);
		}
		return asc;
	}
	
	/**
	 * @return the set of terms with ancestors in the map
	 */
	public Vector<Integer> getChildren()
	{
		Set<Integer> childs = ancestorMap.keySet();
		if(childs == null)
			return new Vector<Integer>(0,1);
		return new Vector<Integer>(childs);
	}
	
	/**
	 * @param term: the id of the term to search in the map
	 * @return the list of direct children of the given term
	 */
	public Vector<Integer> getChildren(int term)
	{
		return getDescendants(term,1);
	}
	
	/**
	 * @param term: the id of the term to search in the map
	 * @return the list of descendants of the input term
	 */
	public Vector<Integer> getDescendants(int term)
	{
		Vector<Integer> desc = new Vector<Integer>(0,1);
		Set<Integer> c = descendantMap.keySet(term);
		if(c != null)
			desc.addAll(c);
		return desc;
	}
	
	/**
	 * @param term: the id of the term to search in the map
	 * @param distance: the distance between the term and its ancestors
	 * @return the list of descendants at the given distance from the input term
	 */
	public Vector<Integer> getDescendants(int term, int distance)
	{
		Vector<Integer> desc = new Vector<Integer>(0,1);
		Set<Integer> c = descendantMap.keySet(term);
		if(c != null)
		{
			for(Integer i : c)
				if(descendantMap.get(term,i).getDistance() == distance)
					desc.add(i);
		}
		return desc;
	}
	
	/**
	 * @param term: the id of the term to search in the map
	 * @param type: the type of relationship between the term and its ancestors
	 * @return the list of descendants of the input term that are of the given type
	 */
	public Vector<Integer> getDescendants(int term, boolean type)
	{
		Vector<Integer> desc = new Vector<Integer>(0,1);
		Set<Integer> c = descendantMap.keySet(term);
		if(c != null)
		{
			for(Integer i : c)
				if(descendantMap.get(term,i).getType() == type)
					desc.add(i);
		}
		return desc;
	}
	
	/**
	 * @param term: the id of the term to search in the map
	 * @param distance: the distance between the term and its ancestors
	 * @param type: the type of relationship between the term and its ancestors
	 * @return the list of descendants of the input term that are at the
	 * given distance and of the given type
	 */
	public Vector<Integer> getDescendants(int term, int distance, boolean type)
	{
		Vector<Integer> desc = new Vector<Integer>(0,1);
		Set<Integer> c = descendantMap.keySet(term);
		if(c != null)
		{
			for(Integer i : c)
				if(descendantMap.get(term,i).getType() == type &&
					descendantMap.get(term,i).getDistance() == distance)
					desc.add(i);
		}
		return desc;
	}
	
	/**
	 * @return the set of terms that have disjoint clauses
	 */
	public Set<Integer> getDisjoint()
	{
		return disjointMap.keySet();
	}

	/**
	 * @param term: the id of the term to search in the map
	 * @return the list of terms disjoint with the given term
	 */
	public Vector<Integer> getDisjoint(int term)
	{
		//Get the disjoint clauses for the term from the disjointMap
		Vector<Integer> disj = disjointMap.get(term);
		//If the term has disjoint clauses, return them
		if(disj != null)
			return disj;
		//Otherwise return an empty list
		return new Vector<Integer>(0,1);
	}
	
	/**
	 * @param term: the id of the term to search in the map
	 * @return the list of terms disjoint with the given term
	 * or any of its 'is_a' ancestors
	 */
	public Vector<Integer> getDisjointTransitive(int term)
	{
		//Get the disjoint clauses for the term
		Vector<Integer> disj = getDisjoint(term);
		//Then get the 'is_a' ancestors of the term
		Vector<Integer> ancestors = getAncestors(term,true);
		//For each ancestor
		for(Integer i : ancestors)
			//Add its disjoint clauses to the list
			disj.addAll(getDisjoint(i));
		return disj;
	}
	
	/**
	 * @param child: the index of the child term
	 * @param parent: the index of the parent term
	 * @return the distance between the child and parent,
	 * or 0 if child==parent, or -1 if they aren't related
	 */
	public int getDistance(int child, int parent)
	{
		if(child == parent)
			return 0;
		Relationship r = ancestorMap.get(child,parent);
		if(r == null)
			return -1;
		else
			return r.getDistance();
	}
	
	/**
	 * @param terms: the set of terms to search in the map
	 * @return the term that is equivalent to all terms in the given
	 * set, or -1 if no such term exists
	 */
	public int getEquivalence(Set<Integer> terms)
	{
		Integer i = equivalenceMap.get(terms);
		if(i == null)
			return -1;
		return i;
	}


	/**
	 * @param term: the id of the term to search in the map
	 * @return the list of high level ancestors of the given term
	 */
	public Vector<Integer> getHighLevelAncestors(int term)
	{
		if(highLevelTerms == null)
			getHighLevelTerms();
			
		Vector<Integer> ancestors = getAncestors(term);
		for(int i = 0; i < ancestors.size(); i++)
		{	
			if(!highLevelTerms.contains(ancestors.get(i)))
			{
				ancestors.remove(i);
				i--;
			}
		}
		return ancestors;
	}
	
	/**
	 * @return the set of high level terms in the ontology
	 */
	public Set<Integer> getHighLevelTerms()
	{
		if(highLevelTerms != null)
			return highLevelTerms;

		//First get the very top terms
		Vector<Integer> top = new Vector<Integer>(0,1);
		Set<Integer> ancestors = descendantMap.keySet();
		//Which are terms that have descendants but not ancestors
		for(Integer a : ancestors)
			if(!ancestorMap.contains(a))
				top.add(a);
		//If we have only one top level term, we go down the
		//ontology until we reach a branching
		while(top.size() == 1)
		{
			int root = top.get(0);
			top = getChildren(root);
		}
		//We want the terms at the level below the first branching
		highLevelTerms = new HashSet<Integer>();
		for(Integer t : top)
			highLevelTerms.addAll(getChildren(t));
			
		return highLevelTerms;
	}
	
	/**
	 * @param term: the id of the term to search in the map
	 * @return the list of direct is_a children of the given term
	 */
	public Vector<Integer> getIsAChildren(int term)
	{
		return getDescendants(term,1,true);
	}
	
	/**
	 * @param term: the id of the term to search in the map
	 * @return the list of direct is_a parents of the given term
	 */
	public Vector<Integer> getIsAParents(int term)
	{
		return getAncestors(term,1,true);
	}

	/**
	 * @param term: the id of the term to search in the map
	 * @return the list of siblings of the given term
	 */
	public Vector<Integer> getIsASiblings(int term)
	{
		Vector<Integer> parents = getIsAParents(term);
		Vector<Integer> siblings = new Vector<Integer>(0,1);
		for(Integer i : parents)
		{
			Vector<Integer> children = getIsAChildren(i);
			for(Integer j : children)
			{
				if(j != term && !siblings.contains(j))
					siblings.add(j);
			}
		}
		return siblings;
	}
	
	/**
	 * @return the set of terms with ancestors in the map
	 */
	public Vector<Integer> getParents()
	{
		return new Vector<Integer>(descendantMap.keySet());
	}

	/**
	 * @param term: the id of the term to search in the map
	 * @return the list of direct parents of the given term
	 */
	public Vector<Integer> getParents(int term)
	{
		return getAncestors(term,1);
	}

	/**
	 * @param term: the id of the term to search in the map
	 * @return the list of direct part_of children of the given term
	 */
	public Vector<Integer> getPartOfChildren(int term)
	{
		return getDescendants(term,1,false);
	}
	
	/**
	 * @param term: the id of the term to search in the map
	 * @return the list of direct part_of parents of the given term
	 */
	public Vector<Integer> getPartOfParents(int term)
	{
		return getAncestors(term,1,false);
	}
	
	/**
	 * @param term: the id of the term to search in the map
	 * @return the list of siblings of the given term
	 */
	public Vector<Integer> getPartOfSiblings(int term)
	{
		Vector<Integer> parents = getPartOfParents(term);
		Vector<Integer> siblings = new Vector<Integer>(0,1);
		for(Integer i : parents)
		{
			Vector<Integer> children = getPartOfChildren(i);
			for(Integer j : children)
			{
				if(j != term && !siblings.contains(j))
					siblings.add(j);
			}
		}
		return siblings;
	}
	
	/**
	 * @param child: the id of the child term to search in the map
	 * @param parent: the id of the parent term to search in the map
	 * @return the relationship between the two terms
	 */
	public Relationship getRelationship(int child, int parent)
	{
		return ancestorMap.get(child).get(parent);
	}
	
	/**
	 * @param term: the id of the term to search in the map
	 * @return the list of siblings of the given term
	 */
	public Vector<Integer> getSiblings(int term)
	{
		Vector<Integer> parents = getParents(term);
		Vector<Integer> siblings = new Vector<Integer>(0,1);
		for(Integer i : parents)
		{
			Vector<Integer> children = getChildren(i);
			for(Integer j : children)
			{
				if(j != term && !siblings.contains(j))
					siblings.add(j);
			}
		}
		return siblings;
	}
	
	/**
	 * @param child: the index of the child term
	 * @param parent: the index of the parent term
	 * @return the type of relationship between the child and parent
	 * true = 'is_a', false = 'part_of'
	 */
	public Boolean getType(int child, int parent)
	{
		if(child == parent)
			return true;
		Relationship r = ancestorMap.get(child,parent);
		if(r == null)
			return null;
		else
			return r.getType();
	}
	
	/**
	 * @param term: the index of the term to search in the map
	 * @return whether there is a disjoint clause associated with the term
	 */
	public boolean hasDisjoint(int term)
	{
		return disjointMap.contains(term);
	}

	/**
	 * @param term: the index of the term to search in the map
	 * @return whether there is a disjoint clause associated with the term
	 * or any of its 'is_a' ancestors
	 */
	public boolean hasDisjointTransitive(int term)
	{
		//Get the ancestors of the term
		Vector<Integer> ancestors = getAncestors(term,true);
		//Plus the parent itself
		ancestors.add(term);
		//Run through the list of ancestors
		for(Integer i : ancestors)
			//And check if any have disjoint clauses
			if(disjointMap.contains(i))
				return true;
		return false;
	}
	
	/**
	 * @param one: the first term to check for disjointness
	 * @param two: the second term to check for disjointness
	 * @return whether there is a disjoint clause between one and two
	 */
	public boolean haveDisjointClause(int one, int two)
	{
		return (disjointMap.contains(one) && disjointMap.contains(one,two));
	}
	
	/**
	 * @return the number of relationships in the map
	 */
	public int relationshipCount()
	{
		return ancestorMap.size();
	}

	/**
	 * Compute the transitive closure of the RelationshipMap
	 * by adding inherited relationships (and their distances)
	 * This is an implementation of the Semi-Naive Algorithm
	 */
	public void transitiveClosure()
	{
		Set<Integer> t = descendantMap.keySet();
		int lastCount = 0;
		for(int distance = 1; lastCount != descendantMap.size(); distance++)
		{
			lastCount = descendantMap.size();
			for(Integer i : t)
			{
				Vector<Integer> childs = getChildren(i);
				Vector<Integer> pars = getAncestors(i,distance);
				for(Integer j : pars)
				{
					for(Integer h : childs)
					{
						Relationship rel = new Relationship(getRelationship(i,j));
						rel.extendWith(getRelationship(h,i));
						addRelationship(h, j, rel);
					}
				}
			}
		}
	}
	
	/**
	 * @param child: the child term in the relationship
	 * @param parent: the parent term in the relationship
	 * @return whether adding the relationship between child and parent
	 * to the RelationshipMap would violate a disjoint clause
	 */
	public boolean violatesDisjoint(int child, int parent)
	{
		//Get all descendants of the child
		Vector<Integer> descendants = getDescendants(child);
		//Plus the child itself
		descendants.add(child);
		//Then all ancestors of the parent
		Vector<Integer> ancestors = getAncestors(parent);
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