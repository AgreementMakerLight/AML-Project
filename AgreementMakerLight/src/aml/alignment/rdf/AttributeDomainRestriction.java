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
* An AttributeDomainRestriction, despite the VERY misleading name, represents *
* the set of individuals whose value for a given relation falls under the     *
* specified range restriction.                                                *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.alignment.rdf;

import java.util.Collection;
import java.util.Vector;

public class AttributeDomainRestriction extends ClassExpression
{

//Attributes
	
	private RelationExpression onAttribute;
	private ClassExpression rest;
	private RestrictionElement e;
	
//Constructor
	
	/**
	 * Constructs a new AttributeDomainRestriction on the given attribute with the class expression as range
	 * @param onAttribute: the restricted relation
	 * @param rest: the class expression restricting the range of the attribute
	 */
	public AttributeDomainRestriction(RelationExpression onAttribute, ClassExpression rest, RestrictionElement e)
	{
		super();
		this.onAttribute = onAttribute;
		this.rest = rest;
		this.e = e;
		elements.addAll(onAttribute.getElements());
		elements.addAll(rest.getElements());
	}
	
//Public Methods
	
	@Override
	public boolean equals(Object o)
	{
		return o instanceof AttributeDomainRestriction &&
				((AttributeDomainRestriction)o).rest.equals(this.rest) &&
				((AttributeDomainRestriction)o).e.equals(this.e) &&
				((AttributeDomainRestriction)o).onAttribute.equals(this.onAttribute);
	}
	
	@Override
	/**
	 * The components of an AttributeDomainRestriction are {relation expression,class expression}
	 * corresponding to the restricted relation and the range restriction
	 */
	public Collection<Expression> getComponents()
	{
		Vector<Expression> components = new Vector<Expression>();
		components.add(onAttribute);
		components.add(rest);
		return components;
	}
	
	@Override
	public String toRDF()
	{
		return "<" + RDFElement.ATTR_DOMAIN_REST_.toRDF() + ">\n" +
				"<" + RDFElement.ON_ATTRIBUTE.toRDF() + ">\n" +
				onAttribute.toRDF() +
				"\n</" + RDFElement.ON_ATTRIBUTE.toRDF() + ">\n" +
				"<" + e.toString() + ">\n" +
				rest.toRDF() +
				"\n</" + e.toString() + ">\n" +
				"</" + RDFElement.ATTR_DOMAIN_REST_.toRDF() + ">\n";
	}

	@Override
	public String toString()
	{
		return "range(" + onAttribute.toString() + ") " + rest.toString();
	}
}