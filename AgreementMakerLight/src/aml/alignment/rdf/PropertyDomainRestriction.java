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
* A PropertyDomainRestriction represents the set of properties whose domain   *
* falls under the given restriction.                                          *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.alignment.rdf;

import java.util.Collection;
import java.util.Vector;

public class PropertyDomainRestriction extends PropertyExpression
{

//Propertys
	
	private ClassExpression rest;
	
//Constructor
	
	/**
	 * Constructs a new PropertyDomainRestriction with the class expression as domain
	 * @param rest: the class expression defining the domain
	 */
	public PropertyDomainRestriction(ClassExpression rest)
	{
		super();
		this.rest = rest;
		elements.addAll(rest.getElements());
	}
	
//Public Methods
	
	@Override
	public boolean equals(Object o)
	{
		return o instanceof PropertyDomainRestriction &&
				((PropertyDomainRestriction)o).rest.equals(this.rest);
	}
	
	public ClassExpression getClassRestriction() 
	{
		return rest;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	/**
	 * A PropertyDomainRestriction has as single subcomponent the class expression
	 * restricting the domain of the property
	 */
	public Collection<ClassExpression> getComponents()
	{
		Vector<ClassExpression> components = new Vector<ClassExpression>();
		components.add(rest);
		return components;
	}
	
	@Override
	public String toRDF()
	{
		String rdf = "<" + RDFElement.PROPERTY_DOMAIN_REST_.toRDF() + ">\n" +
				"<" + RDFElement.CLASS.toRDF() + ">\n" +
				rest.toRDF() + 
				"\n</" + RDFElement.CLASS.toRDF() + ">\n" +
				"</" + RDFElement.PROPERTY_DOMAIN_REST_.toRDF() + ">";
		return rdf;
	}

	@Override
	public String toString()
	{
		return "Data Domain(" + rest.toString() + ")";
	}
}