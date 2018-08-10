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
* A PropertyDomainRestriction represents the set of properties whose range    *
* falls under the given type restriction.                                     *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.alignment.rdf;

import java.util.Collection;
import java.util.Vector;

public class PropertyTypeRestriction extends PropertyExpression
{

//Attributes
	
	private Datatype type;
	
//Constructor
	
	/**
	 * Constructs a new PropertyTypeRestriction with the range
	 * @param type: the datatype to restrict the range of the property
	 */
	public PropertyTypeRestriction(Datatype type)
	{
		super();
		this.type = type;
	}
	
//Public Methods
	
	@Override
	public boolean equals(Object o)
	{
		return o instanceof PropertyTypeRestriction &&
				((PropertyTypeRestriction)o).type.equals(this.type);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	/**
	 * A PropertyTypeRestriction has as single subcomponent the datatype
	 * restricting the range of the property
	 */
	public Collection<Datatype> getComponents()
	{
		Vector<Datatype> components = new Vector<Datatype>();
		components.add(type);
		return components;
	}
	
	@Override
	public String toRDF()
	{
		return "<edoal:PropertyTypeRestriction>\n" +
				"<edoal:datatype>\n" +
				type.toRDF() +
				"\n</edoal:datatype>\n" +
				"</edoal:PropertyTypeRestriction>\n";
	}

	@Override
	public String toString()
	{
		return "Data Range(" + type.toString() + ")";
	}
}