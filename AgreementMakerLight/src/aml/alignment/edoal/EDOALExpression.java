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
* An EDOAL Expression.                                                        *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.alignment.edoal;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public abstract class EDOALExpression
{
	
//Attributes
	
	//To enable efficient hashing, an EDOAL Expression 
	//must list all elements that compose it
	protected HashSet<String> elements;
	
//Constructors
	
	protected EDOALExpression()
	{
		elements = new HashSet<String>();
	}
	
//Public Methods
	
	@Override
	public abstract boolean equals(Object o);
	
	/**
	 * @return the ontology entities listed in this EDOAL expression
	 */
	public Set<String> getElements()
	{
		return elements;
	}
	
	/**
	 * @return the EDOALExpressions that compose this EDOALExpression
	 */
	public abstract <E extends EDOALExpression> Collection<E> getComponents();
	
	@Override
	public int hashCode()
	{
		return elements.hashCode();
	}

	/**
	 * @return this EDOAL expression in RDF form
	 */
	public abstract String toRDF();

	@Override
	public abstract String toString();
}