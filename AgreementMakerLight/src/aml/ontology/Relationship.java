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
* @date 23-06-2014                                                            *
* @version 2.0                                                                *
******************************************************************************/
package aml.ontology;

public class Relationship implements Comparable<Relationship>
{
	
//Attributes

	//The distance between the terms
	private int distance;
	//The subclass property of the relationship (-1 for 'is_a' relationships)
	private int property;
	//The type of restriction on the property (true for 'all values', false for 'some values')
	private boolean restriction;

//Constructors
	
	/**
	 * Constructs a new Relationship with the given distance and type
	 * @param dist: the distance in the Relationship
	 * @param p: the property of the Relationship
	 * @param r: the restriction on the property of the Relationship
	 */
	public Relationship(int dist, int p, boolean r)
	{
		distance = dist;
		property = p;
		restriction = r;
	}
	
	/**
	 * Constructs a new Relationship with the distance and type of
	 * the given Relationship
	 * @param r: the Relationship to copy
	 */
	public Relationship(Relationship r)
	{
		distance = r.distance;
		property = r.property;
		restriction = r.restriction;
	}
	
//Public Methods

	@Override
	/**
	 * Relationships are compared with regard to property, restriction,
	 * then distance ('is_a' relationships supersede all other properties,
	 * then 'all values' supersede 'some values' restrictions, then if all
	 * else is equal, distance is the tie-breaker)
	 */
	public int compareTo(Relationship r)
	{
		int value = 0;
		if(property == -1 && r.property == -1)
			value = distance - r.distance;
		else if(property == -1)
			value = 1;
		else if(r.property == -1)
			value = -1;
		else if(restriction == r.restriction)
			value = distance - r.distance;
		else if(restriction)
			value = 1;
		else if(r.restriction)
			value = -1;
		return value;
	}
	
	/**
	 * Two Relationships are equal if they have the same property and restriction
	 * regardless of distance, to enable checking whether two entities are
	 * related via a given type of relationship
	 */
	public boolean equals(Object o)
	{
		if(o instanceof Relationship)
		{
			Relationship r = (Relationship)o;
			return property == r.property && restriction == r.restriction;
		}
		else
			return false;
	}
	
	/**
	 * @return the distance of the Relationship
	 */
	public int getDistance()
	{
		return distance;
	}
	
	/**
	 * @return the property of the Relationship
	 */
	public int getProperty()
	{
		return property;
	}
	
	/**
	 * @return the restriction of the Relationship
	 */
	public boolean getRestriction()
	{
		return restriction;
	}
	
	/**
	 * @return whether this Relationship is an 'is_a' relationship
	 */
	public boolean isA()
	{
		return property == -1;
	}
}