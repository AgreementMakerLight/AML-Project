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
* A Comparator such as "equals", "lower than" or "greater than".              *
* EDOALComparator are recommended, but technically any Comparator that can be *
* referenced by URI is valid.                                                 *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.alignment.edoal;

import java.util.Collection;

import aml.AML;

public class Comparator extends AbstractExpression
{

//Attributes
	
	//The Comparator is normally not part of the input ontologies
	//and thus its URI shouldn't be included in the entities set
	private String uri;
	
//Constructor

	/**
	 * Constructs a new Comparator from the given uri
	 * @param uri: the URI of the class
	 */
	public Comparator(String uri)
	{
		super();
		this.uri = uri;
	}

//Public Methods
	
	@Override
	public boolean equals(Object o)
	{
		return o instanceof Comparator && ((Comparator)o).uri.equals(this.uri);
	}
	
	@Override
	public Collection<Expression> getComponents()
	{
		return null;
	}
	
	@Override
	public String toRDF()
	{
		return "<edoal:comparator rdf:resource=\"" + uri + "\"/>"; 
	}

	@Override
	public String toString()
	{
		return AML.getInstance().getEntityMap().getLocalName(uri);
	}
}