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
* An intersection of EDOAL Properties / OWL Data Properies.                   *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.alignment.rdf;

import java.util.Collection;
import java.util.Set;

public class PropertyIntersection extends PropertyExpression
{

//Attributes
	
	private Set<PropertyExpression> intersect;
	
//Constructor
	
	/**
	 * Constructs a new PropertyIntersection from the given set of property expressions
	 * @param intersect: the property expressions in the intersection
	 */
	public PropertyIntersection(Set<PropertyExpression> intersect)
	{
		super();
		this.intersect = intersect;
		for(PropertyExpression e : intersect)
			elements.addAll(e.getElements());
	}
	
//Public Methods
	
	@Override
	public boolean equals(Object o)
	{
		return o instanceof PropertyIntersection &&
				((PropertyIntersection)o).intersect.equals(this.intersect);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	/**
	 * The components of a PropertyIntersection are the set of property
	 * expressions in the intersection
	 */
	public Collection<PropertyExpression> getComponents()
	{
		return intersect;
	}
	
	@Override
	public String toRDF()
	{
		String rdf = "<" + RDFElement.PROPERTY_.toRDF() + ">\n" +
				"<" + RDFElement.AND.toRDF() + " " + RDFElement.RDF_PARSETYPE.toRDF() + "=\"Collection\">\n";
		for(PropertyExpression e : intersect)
			rdf += e.toRDF() + "\n";
		rdf += "</" + RDFElement.AND.toRDF() + ">\n";
		rdf += "</" + RDFElement.PROPERTY_.toRDF() + ">";
		return rdf;
	}

	@Override
	public String toString()
	{
		String s = "AND[";
		for(PropertyExpression e : intersect)
			s += e.toString() + ", ";
		s = s.substring(0, s.lastIndexOf(',')) + "]";
		return s;
	}
}