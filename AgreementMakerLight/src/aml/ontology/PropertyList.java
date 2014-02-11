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
* The list of properties in an Ontology; the list of lexical types and their  *
* default weights; and a translator from synonym properties to lexical types. *
*                                                                             *
* @authors Daniel Faria & Catia Pesquita                                      *
* @date 22-10-2013                                                            *
******************************************************************************/
package aml.ontology;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

public class PropertyList implements Iterable<Property>
{

//Attributes

	//The list of default lexical types
	private final String[] LEXICAL_TYPES = {"localName", "label", "exactSynonym", "otherSynonym"};
	//The map of default weights for lexical types
	private HashMap<String,Double> typeWeights;
	//The list of properties
	private Vector<Property> properties;
	
//Constructors

	public PropertyList()
	{
		typeWeights = new HashMap<String,Double>();
		properties = new Vector<Property>(0,1);
		setTypeWeights();
	}

//Public Methods

	public void add(Property p)
	{
		properties.add(p);
	}
	
	public String getDefaultLexicalType(String prop)
	{
		if(prop.equals("localName") || prop.equals("label"))
			return prop;
		if(prop.equals("hasExactSynonym") || prop.equals("FULL_SYN"))
			return "exactSynonym";
		if(prop.contains("synonym") || prop.contains("Synonym") || prop.contains("SYN"))
			return "otherSynonym";
		return "";
	}
	
	public Property getProperty(int index)
	{
		return properties.get(index);
	}
	
	public String getName(int index)
	{
		return properties.get(index).getName();
	}

	public double getWeight(String prop)
	{
		return typeWeights.get(prop);
	}
	
	@Override
	public Iterator<Property> iterator()
	{
		return properties.iterator();
	}
	
	public int size()
	{
		return properties.size();
	}

//Private Methods
	
	//Computes the weights for name properties based on ranks and the interval
	//between ranks, considering that rank 1 = 1.0
	private void setTypeWeights()
	{
		for(int i = 0; i < LEXICAL_TYPES.length; i++)
		{
			double weight = 1.0 - (0.05 * i);
			typeWeights.put(LEXICAL_TYPES[i], weight);
		}
	}
}
