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
* An AttributeOccurrenceRestriction represents the set of individuals whose   *
* cardinality for a given property or relation falls under the specified      *
* restriction.                                                                *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.alignment.rdf;

import java.util.Collection;
import java.util.Vector;

public class AttributeOccurrenceRestriction extends ClassExpression
{

//Attributes
	
	private AttributeExpression onAttribute;
	private Comparator comp;
	private NonNegativeInteger val;
	
//Constructor
	
	/**
	 * Constructs a new AttributeOccurrenceRestriction on the given attribute with the given comparator and value
	 * @param onAttribute: the restricted attribute
	 * @param comp: the comparator (typically an EDOALComparator)
	 * @param val: the value (must be a non-negative integer)
	 */
	public AttributeOccurrenceRestriction(AttributeExpression onAttribute, Comparator comp, NonNegativeInteger val)
	{
		super();
		this.onAttribute = onAttribute;
		this.comp = comp;
		this.val = val;
		elements.addAll(onAttribute.getElements());
	}
	
//Public Methods
	
	@Override
	public boolean equals(Object o)
	{
		return o instanceof AttributeOccurrenceRestriction &&
				((AttributeOccurrenceRestriction)o).comp.equals(this.comp) &&
				((AttributeOccurrenceRestriction)o).val.equals(this.val) &&
				((AttributeOccurrenceRestriction)o).onAttribute.equals(this.onAttribute);
	}
	
	@Override
	/**
	 * The components of an AttributeDomainRestriction are
	 * {attribute expression,comparator,non-negative integer (literal)}
	 * corresponding to the restricted relation/propertye, the restriction
	 * comparator, and the restricted cardinality
	 */
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
		return "<" + RDFElement.ATTR_OCCURRENCE_REST_.toRDF() + ">\n" +
				"<" + RDFElement.ON_ATTRIBUTE.toRDF() + ">\n" +
				onAttribute.toRDF() +
				"\n</" + RDFElement.ON_ATTRIBUTE.toRDF() + ">\n" +
				comp.toRDF() + 
				"\n<" + RDFElement.VALUE.toRDF() + ">" +
				val.toRDF() +
				"</" + RDFElement.VALUE.toRDF() + ">\n" +
				"</" + RDFElement.ATTR_OCCURRENCE_REST_.toRDF() + ">";
	}

	@Override
	public String toString()
	{
		return "occurrence(" + onAttribute.toString() + ") " + comp.toString() + " " + val.toString();
	}
}