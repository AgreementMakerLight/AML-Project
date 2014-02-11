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
* A relationship between two terms (classes) in an Ontology, which is an      *
* element in the RelationshipMap. The Relationship includes the distance      *
* between the terms (number of edges) and the type of relationship (true for  *
* 'is a' or false for 'part of').                                             *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 22-10-2013                                                            *
******************************************************************************/
package aml.ontology;

public class Relationship implements Comparable<Relationship>
{
	
//Attributes

	//The distance between the terms
	private int distance;
	//The type of relationship (true for 'is_a', false for 'part_of')
	private boolean type;

//Constructors
	
	/**
	 * Constructs a new Relationship with the given distance and type
	 * @param dist: the distance in the Relationship
	 * @param t: the type of Relationship (whether it is 'is_a' or 'part_of')
	 */
	public Relationship(int dist, boolean t)
	{
		distance = dist;
		type = t;
	}
	
	/**
	 * Constructs a new Relationship with the distance and type of
	 * the given Relationship
	 * @param r: the Relationship to copy
	 */
	public Relationship(Relationship r)
	{
		distance = r.distance;
		type = r.type;
	}
	
//Public Methods

	/**
	 * Extends this Relationship with the given Relationship by
	 * adding the distance and conjugating the type (thus getting
	 * the transitive Relationship)
	 * @param r: the Relationship to combine with this Relationship
	 */
	public void extendWith(Relationship r)
	{
		distance += r.distance;
		type = type && r.type;
	}
	
	@Override
	/**
	 * Relationships are compared only regarding type, with
	 * 'is_a' being "greater" than 'part_of'
	 */
	public int compareTo(Relationship o)
	{
		if(this.type && !o.type)
			return 1;
		if(!this.type && o.type)
			return -1;
		return 0;
	}
	
	/**
	 * Two Relationships are equal if they have the same type
	 */
	public boolean equals(Object o)
	{
		Relationship r = (Relationship)o;
		return this.type == r.type;
	}
	
	/**
	 * @return the distance of the Relationship
	 */
	public int getDistance()
	{
		return distance;
	}
	
	/**
	 * @return the type of the Relationship
	 */
	public boolean getType()
	{
		return type;
	}
	
	/**
	 * Sets the distance to the given value
	 * @param dist: the distance to set in the Relationship
	 */
	public void setDistance(int dist)
	{
		distance = dist;
	}
	
	/**
	 * Sets the type to the given value
	 * @param t: the type to set in the Relationship
	 */
	public void setType(boolean t)
	{
		type = t;
	}
}
