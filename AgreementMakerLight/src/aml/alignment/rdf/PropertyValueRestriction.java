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
* A PropertyValueRestriction represents the set of properties whose range     *
* falls under the given value restriction.                                    *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.alignment.rdf;

import java.util.Collection;
import java.util.Vector;

public class PropertyValueRestriction extends PropertyExpression
{

//Attributes
	
	private Comparator comp;
	private ValueExpression val;
	
//Constructor
	
	/**
	 * Constructs a new PropertyValueRestriction on the given attribute with the given comparator and value
	 * @param onAttribute: the restricted attribute
	 * @param comp: the comparator (typically an EDOALComparator)
	 * @param val: the value (must be a non-negative integer)
	 */
	public PropertyValueRestriction(Comparator comp, ValueExpression val)
	{
		super();
		this.comp = comp;
		this.val = val;
		//The ValueExpression may be an IndividualId or AttributeExpression, so we must add its elements as well
		elements.addAll(val.getElements());
	}
	
//Public Methods
	
	@Override
	public boolean equals(Object o)
	{
		return o instanceof PropertyValueRestriction &&
				((PropertyValueRestriction)o).comp.equals(this.comp) &&
				((PropertyValueRestriction)o).val.equals(this.val);
	}
	
	@Override
	/**
	 * The components of a PropertyValueRestriction are {comparator, value expression}
	 */
	public Collection<Expression> getComponents()
	{
		Vector<Expression> components = new Vector<Expression>();
		components.add(comp);
		components.add(val);
		return components;
	}
	
	@Override
	public String toRDF()
	{
		return "<edoal:PropertyValueRestriction>\n" +
				comp.toRDF() + 
				"\n<edoal:value>\n" +
				val.toRDF() +
				"\n</edoal:value>\n" +
				"</edoal:PropertyValueRestriction>\n";
	}

	@Override
	public String toString()
	{
		return "Data Range(" + comp.toString() + " " + val.toString() + ")";
	}
}