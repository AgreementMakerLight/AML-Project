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
* An AttributeValueRestriction restricts the value of properties or relations *
* to a particular value.                                                      *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.alignment.edoal;

import java.util.Collection;
import java.util.Vector;

public class AttributeValueRestriction extends ClassExpression
{

//Attributes
	
	private AttributeExpression onAttribute;
	private Comparator comp;
	private ValueExpression val;
	
//Constructor
	
	/**
	 * Constructs a new AttributeOccurrenceRestriction on the given attribute with the given comparator and value
	 * @param onAttribute: the restricted attribute
	 * @param comp: the comparator (typically an EDOALComparator)
	 * @param val: the value (must be a non-negative integer)
	 */
	public AttributeValueRestriction(AttributeExpression onAttribute, Comparator comp, ValueExpression val)
	{
		super();
		this.onAttribute = onAttribute;
		this.comp = comp;
		this.val = val;
		elements.addAll(onAttribute.getElements());
		//The ValueExpression may be an IndividualId or AttributeExpression, so we must add its elements as well
		elements.addAll(val.getElements());
	}
	
//Public Methods
	
	@Override
	public boolean equals(Object o)
	{
		return o instanceof AttributeValueRestriction &&
				((AttributeValueRestriction)o).comp.equals(this.comp) &&
				((AttributeValueRestriction)o).val.equals(this.val) &&
				((AttributeValueRestriction)o).onAttribute.equals(this.onAttribute);
	}
	
	@Override
	public Collection<Expression> getComponents()
	{
		Vector<Expression> components = new Vector<Expression>();
		components.add(onAttribute);
		components.add(comp);
		components.add(val);
		return components;
	}
	
	@Override
	public String toRDF()
	{
		//In RDF we have to stick with the typo on "occurence" as it is now part of the official syntax...
		String rdf = "<edoal:AttributeOccurenceRestriction>\n" +
				"<onAttribute>\n";
		rdf += onAttribute.toRDF() + "\n";
		rdf += "</onAttribute>\n";
		rdf += comp.toRDF() + "\n";
		rdf += "<edoal:value>" + val.toRDF() + "<edoal:value>\n";
		return rdf;
	}

	@Override
	public String toString()
	{
		return "occurrence(" + onAttribute.toString() + ") " + comp.toString() + " " + val.toString();
	}
}