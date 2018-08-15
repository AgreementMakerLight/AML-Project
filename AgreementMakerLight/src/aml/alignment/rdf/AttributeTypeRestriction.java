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
* An AttributeTypeRestriction represents the set of individuals whose value   *
* for a given property falls under the specified restriction.                 *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.alignment.rdf;

import java.util.Collection;
import java.util.Vector;

public class AttributeTypeRestriction extends ClassExpression
{

//Attributes
	
	private PropertyExpression onAttribute;
	private Datatype type;
	
//Constructor
	
	/**
	 * Constructs a new AttributeTypeRestriction on the given attribute with the given datatype
	 * @param onAttribute: the restricted property
	 * @param type: the datatype to restrict the range of the property
	 */
	public AttributeTypeRestriction(PropertyExpression onAttribute, Datatype type)
	{
		super();
		this.onAttribute = onAttribute;
		this.type = type;
		elements.addAll(onAttribute.getElements());
	}
	
//Public Methods
	
	@Override
	public boolean equals(Object o)
	{
		return o instanceof AttributeTypeRestriction &&
				((AttributeTypeRestriction)o).type.equals(this.type) &&
				((AttributeTypeRestriction)o).onAttribute.equals(this.onAttribute);
	}
	
	@Override
	/**
	 * The components of an AttributeTypeRestriction are {relation expression,datatype}
	 * corresponding to the restricted property and the range restriction
	 */
	public Collection<Expression> getComponents()
	{
		Vector<Expression> components = new Vector<Expression>();
		components.add(onAttribute);
		components.add(type);
		return components;
	}
	
	@Override
	public String toRDF()
	{
		String rdf = "<" + RDFElement.ATTR_TYPE_REST_.toRDF() + ">\n" +
				"<" + RDFElement.ON_ATTRIBUTE.toRDF() + ">\n" +
				onAttribute.toRDF() +
				"\n</" + RDFElement.ON_ATTRIBUTE.toRDF() + ">\n";
		rdf += "<" + RDFElement.DATATYPE + ">\n" + type.toRDF() + "\n</" + RDFElement.DATATYPE + ">\n";
		rdf += "</" + RDFElement.ATTR_TYPE_REST_.toRDF() + ">";
		return rdf;
	}

	@Override
	public String toString()
	{
		return "range(" + onAttribute.toString() + ") " + type.toString();
	}
}