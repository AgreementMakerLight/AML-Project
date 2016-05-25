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
* An Ontology individual (or instance).                                       *
*                                                                             *
* @author Catia Pesquita                                                      *
******************************************************************************/
package aml.ontology;


import java.util.Set;

import aml.util.StringParser;
import aml.util.Table2Set;

public class Individual
{
	
//Attributes
	
	private int index;
	private String name;
	//the table of properties indexes and their values for the instance
	private Table2Set<Integer,String> dataValues;

	
//Constructors

	/**
	 * Constructs a new Individual with given id and name
	 * @param i: the id of the Individual
	 * @param n: the name of the Individual
	 */
	public Individual(int i, String n)
	{
		index = i;
		if(StringParser.isFormula(n))
			name = StringParser.normalizeFormula(n);
		else
			name = StringParser.normalizeProperty(n);
		dataValues = new Table2Set<Integer,String>();
	}
	
//Public Methods

	/**
	 * @return the index of this Individual
	 */
	public int getIndex()
	{
		return index;
	}
	
	/**	
	 * @return the name of this Individual
	 */
	public String getName()
	{
		return name;
	}
	
	/**
	 * Adds a given value to this Individual
	 * @param propIndex: the index of the DataProperty associated with this Individual
	 * @param value: the value of the DataProperty for this Individual
	 */
	public void addDataValue(int propIndex, String value)
	{
		dataValues.add(propIndex,value);
	}
	
	/**
	 * @param propIndex: the index of the DataProperty associated with this Individual
	 * @return the value of the DataProperty for this Individual 
	 */
	public Set<String> getDataValue(int propIndex)
	{
		return dataValues.get(propIndex);
	}
	
	/**
	 * @return the Data Properties and their values for this Individual 
	 */
	public Table2Set<Integer,String> getDataValues()
	{
		return dataValues;
	}
	
	@Override
	public String toString()
	{
		String s = "name: " + name;
		return s;
	}
}
