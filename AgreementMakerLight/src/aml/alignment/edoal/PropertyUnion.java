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
* A union of EDOAL Properties / OWL Data Properies.                           *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.alignment.edoal;

import java.util.Set;

public class PropertyUnion extends PropertyExpression
{

//Attributes
	
	private Set<PropertyExpression> union;
	
//Constructor
	
	/**
	 * Constructs a new PropertyUnion from the given set of property expressions
	 * @param union: the property expressions in the union
	 */
	public PropertyUnion(Set<PropertyExpression> union)
	{
		this.union = union;
		for(PropertyExpression e : union)
			elements.addAll(e.getElements());
	}
	
//Public Methods
	
	@Override
	public boolean equals(Object o)
	{
		return o instanceof PropertyUnion &&
				((PropertyUnion)o).union.equals(this.union);
	}
	
	@Override
	public String toRDF()
	{
		String rdf = "<edoal:Property>\n" +
				"<edoal:or rdf:parseType=\"Collection\">\n";
		for(PropertyExpression e : union)
			rdf += e.toRDF() + "\n";
		rdf += "</edoal:or>\n";
		rdf += "</edoal:Property>\n";
		return rdf;
	}

	@Override
	public String toString()
	{
		String s = "OR[";
		for(PropertyExpression e : union)
			s += e.toString() + ", ";
		s = s.substring(0, s.lastIndexOf(',')) + "]";
		return s;
	}
}