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
* A negation of an EDOAL Property / OWL Data Property.                        *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.alignment.rdf;

import java.util.Collection;
import java.util.HashSet;

public class PropertyNegation extends PropertyExpression
{

//Attributes
	
	private PropertyExpression neg;
	
//Constructor
	
	/**
	 * Constructs a new PropertyNegation from the given property expression
	 * @param neg: the property expression in the negation
	 */
	public PropertyNegation(PropertyExpression neg)
	{
		super();
		this.neg = neg;
		elements.addAll(neg.getElements());
	}
	
//Public Methods
	
	@Override
	public boolean equals(Object o)
	{
		return o instanceof PropertyNegation &&
				((PropertyNegation)o).neg.equals(this.neg);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	/**
	 * A PropertyNegation has as single subcomponent the property expression
	 * restricting the domain of the property
	 */
	public Collection<PropertyExpression> getComponents()
	{
		HashSet<PropertyExpression> components = new HashSet<PropertyExpression>();
		components.add(neg);
		return components;
	}
	
	@Override
	public String toRDF()
	{
		return "<" + RDFElement.PROPERTY_.toRDF() + ">\n" +
				"<" + RDFElement.NOT.toRDF() + ">\n" +
				neg.toRDF() +
				"\\n</" + RDFElement.NOT.toRDF() + ">\n" +
				"</" + RDFElement.PROPERTY_.toRDF() + ">\n";
	}

	@Override
	public String toString()
	{
		return "NOT[" + neg.toString() + "]";
	}
}