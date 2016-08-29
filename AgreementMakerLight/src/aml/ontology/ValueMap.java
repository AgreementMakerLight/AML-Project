/******************************************************************************
* Copyright 2013-2016 LASIGE                                                  *
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
* The map of data property and annotation property values of individuals in   *
* an ontology.                                                                *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/

package aml.ontology;

import java.util.HashSet;
import java.util.Set;

import aml.util.Table3Set;

public class ValueMap
{

//Attributes	

	//The table of individual indexes (Integer), property indexes (Integer), and property values (String)
	private Table3Set<Integer,Integer,String> valueIndividuals;
	//The table of property indexes (Integer), property values (String), and individual indexes (Integer)
	private Table3Set<Integer,String,Integer> individualValues;
	
//Constructors
	
	/**
	 * Constructs a new empty ValueMap
	 */
	public ValueMap()
	{
		valueIndividuals = new Table3Set<Integer,Integer,String>();
		individualValues = new Table3Set<Integer,String,Integer>();
	}
	
//Public Methods
	
	/**
	 * Adds a new entry to the ValueMap
	 * @param indivId: the index of the individual with the value
	 * @param propId: the index of the data or annotation property
	 * for which the individual has the value
	 * @param value: the value of the individual
	 */
	public void add(int indivId, int propId, String value)
	{
		valueIndividuals.add(indivId, propId, value);
		individualValues.add(propId, value, indivId);
	}
	
	/**
	 * @return the set of individuals with values in the ValueMap
	 */
	public Set<Integer> getIndividuals()
	{
		return valueIndividuals.keySet();
	}
	
	/**
	 * @param propId: the index of the property to search in the ValueMap
	 * @param value: the value of that property to search in the ValueMap
	 * @return the set of Individuals that have the given value for the given property
	 */
	public Set<Integer> getIndividuals(int propId, String value)
	{
		if(individualValues.contains(propId,value))
			return individualValues.get(propId,value);
		return new HashSet<Integer>();
	}
	
	/**
	 * @return the set of data and annotation properties with values in the ValueMap
	 */
	public Set<Integer> getProperties()
	{
		return individualValues.keySet();
	}
	
	/**
	 * @param indivId: the index of the individual to search in the ValueMap
	 * @return the set of data and annotation properties with values for the given individual
	 */
	public Set<Integer> getProperties(int indivId)
	{
		if(valueIndividuals.contains(indivId))
			return valueIndividuals.keySet(indivId);
		return new HashSet<Integer>();
	}
	
	/**
	 * @param propId: the index of the property to search in the ValueMap
	 * @return the set of values for that property in the ValueMap
	 */
	public Set<String> getValues(int propId)
	{
		if(individualValues.contains(propId))
			return individualValues.keySet(propId);
		return new HashSet<String>();
	}
	
	/**
	 * @param indivId: the index of the individual to search in the ValueMap
	 * @param propId: the index of the property to search in the ValueMap
	 * @return the set of values for the individual and property pair
	 */
	public Set<String> getValues(int indivId, int propId)
	{
		if(valueIndividuals.contains(indivId,propId))
			return valueIndividuals.get(indivId,propId);
		return new HashSet<String>();
	}
	
	/**
	 * @return the size of the ValueMap
	 */
	public int size()
	{
		return valueIndividuals.size();
	}
}
