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
 * The map of data property and annotation property values of individuals in   *
 * an ontology.                                                                *
 *                                                                             *
 * @author Daniel Faria                                                        *
 ******************************************************************************/

package aml.ontology;

import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLDatatype;

import aml.alignment.rdf.Datatype;
import aml.util.data.Map2Map2Map;
import aml.util.data.Map2Set;

public class ValueMap
{

	//Attributes	

	//The table of individual indexes (String), property indexes (String), property values (String) and type of value (Datatype)
	public Map2Map2Map<String,String,String,OWLDatatype> valueIndividuals;
	//The table of property indexes (String), property values (String), individual indexes (String) and type of value (Datatype)
	private Map2Map2Map<String,String,String,OWLDatatype> individualValues;

	//Constructors

	/**
	 * Constructs a new empty ValueMap
	 */
	public ValueMap()
	{
		valueIndividuals = new Map2Map2Map<String,String,String,OWLDatatype>();
		individualValues = new Map2Map2Map<String,String,String,OWLDatatype>();
	}

	//Public Methods

	/**
	 * Adds a new entry to the ValueMap
	 * @param indivId: the index of the individual with the value
	 * @param propId: the index of the data or annotation property
	 * for which the individual has the value
	 * @param value: the value of the individual
	 */
	public void add(String indivId, String propId, String value, OWLDatatype type)
	{
		valueIndividuals.add(indivId, propId, value, type);
		individualValues.add(propId, value, indivId, type);
	}
	
	/**
	 * Extends the ValueMap in order to contain the equivalent classes as keys
	 * @param sameIndivAs the map of equivalent individuals
	 */
	public void extend(Map2Set<String, String> sameIndivAs) 
	{
		for (String i1: sameIndivAs.keySet()) 
		{
			if(valueIndividuals.contains(i1)) 
			{
				for(String i2: sameIndivAs.get(i1)) // for this i2, add al of its equivalent i1 info
				{
					for(String propId: getProperties(i1)) 
					{
						for(String value: getValues(i1, propId)) 
						{
							valueIndividuals.add(i2, propId, value, getDataType(i1, propId, value));
						}
					}
				}
			}
		}
	} 

	/**
	 * @param propId: the index of the property to search in the ValueMap
	 * @param value: the value of that property to search in the ValueMap
	 * @return the set of datatypes of the given value for the given property
	 */
	public Set<OWLDatatype> getDataType(String propId, String value)
	{
		Set<OWLDatatype> types = new HashSet<OWLDatatype>();
		if(individualValues.contains(propId,value)) 
		{
			for(String indv: individualValues.get(propId,value).keySet())
				types.add(individualValues.get(propId,value, indv));
		}	
		return types;
	}
	
	/**
	 * @param propId: the index of the property to search in the ValueMap
	 * @param value: the value of that property to search in the ValueMap
	 * @return the set of Individuals that have the given value for the given property
	 */
	public OWLDatatype getDataType(String indivId, String propId, String value)
	{
		if(valueIndividuals.contains(indivId, propId,value)) 
		{
			return valueIndividuals.get(indivId,propId,value);
		}	
		return null;
	}

	/**
	 * @return the set of individuals with values in the ValueMap
	 */
	public Set<String> getIndividuals()
	{
		return valueIndividuals.keySet();
	}
	
	/**
	 * @param propId: the index of the property to search in the ValueMap
	 * @return the set of Individuals that have the given property
	 */
	public Set<String> getIndividuals(String propId)
	{
		Set<String> individuals = new HashSet<String>();
		if(individualValues.contains(propId)) 
		{
			Set<String> values =  getValues(propId);
			for (String v: values)
				individuals.addAll(getIndividuals(propId,v));	
			return individuals;
		}	
		return new HashSet<String>();
	}

	/**
	 * @param propId: the index of the property to search in the ValueMap
	 * @param value: the value of that property to search in the ValueMap
	 * @return the set of Individuals that have the given value for the given property
	 */
	public Set<String> getIndividuals(String propId, String value)
	{
		if(individualValues.contains(propId,value))
			return individualValues.get(propId,value).keySet();
		return new HashSet<String>();
	}

	/**
	 * @return the set of data and annotation properties with values in the ValueMap
	 */
	public Set<String> getProperties()
	{
		return individualValues.keySet();
	}

	/**
	 * @param indivId: the index of the individual to search in the ValueMap
	 * @return the set of data and annotation properties with values for the given individual
	 */
	public Set<String> getProperties(String indivId)
	{
		if(valueIndividuals.contains(indivId))
			return valueIndividuals.keySet(indivId);
		return new HashSet<String>();
	}

	/**
	 * @param propId: the index of the property to search in the ValueMap
	 * @return the set of values for that property in the ValueMap
	 */
	public Set<String> getValues(String propId)
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
	public Set<String> getValues(String indivId, String propId)
	{
		if(valueIndividuals.contains(indivId,propId))
			return valueIndividuals.get(indivId,propId).keySet();
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
