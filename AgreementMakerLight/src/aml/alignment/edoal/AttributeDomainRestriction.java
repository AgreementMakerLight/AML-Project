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
* An AttributeDomainRestriction represents the set of individuals whose value *
* for a given relation falls under the specified restrictions.                *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.alignment.edoal;

import java.util.Collection;
import java.util.Vector;

public class AttributeDomainRestriction extends ClassExpression
{

//Attributes
	
	private AttributeExpression onAttribute;
	private ClassExpression rest;
	
//Constructor
	
	/**
	 * Constructs a new AttributeOccurrenceRestriction on the given attribute with the given comparator and value
	 * @param onAttribute: the restricted attribute
	 * @param comp: the comparator (typically an EDOALComparator)
	 * @param val: the value (must be a non-negative integer)
	 */
	public AttributeDomainRestriction(AttributeExpression onAttribute, ClassExpression rest)
	{
		super();
		this.onAttribute = onAttribute;
		this.rest = rest;
		elements.addAll(onAttribute.getElements());
		elements.addAll(rest.getElements());
	}
	
//Public Methods
	
	@Override
	public boolean equals(Object o)
	{
		return o instanceof AttributeDomainRestriction &&
				((AttributeDomainRestriction)o).rest.equals(this.rest) &&
				((AttributeDomainRestriction)o).onAttribute.equals(this.onAttribute);
	}
	
	@Override
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
		String rdf = "<edoal:AttributeDomainRestriction>\n" +
				"<onAttribute>\n";
		rdf += onAttribute.toRDF() + "\n";
		rdf += "</onAttribute>\n";
		rdf += "<edoal:class>\n" + rest.toRDF() + "\n</edoal:class>\n";
		rdf += "</edoal:AttributeDomainRestriction>\n";
		return rdf;
	}

	@Override
	public String toString()
	{
		return "range(" + onAttribute.toString() + ") " + rest.toString();
	}
}