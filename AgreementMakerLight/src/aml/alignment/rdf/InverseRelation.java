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
* The inverse of an EDOAL Relation / OWL Object Property.                     *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.alignment.rdf;

import java.util.Collection;
import java.util.HashSet;

public class InverseRelation extends RelationExpression
{

//Attributes
	
	private RelationExpression inv;
	
//Constructor
	
	/**
	 * Constructs a new InverseRelation from the given relation expression
	 * @param inv: the relation expression to invert
	 */
	public InverseRelation(RelationExpression inv)
	{
		super();
		this.inv = inv;
		elements.addAll(inv.getElements());
	}
	
//Public Methods
	
	@Override
	public boolean equals(Object o)
	{
		return o instanceof InverseRelation &&
				((InverseRelation)o).inv.equals(this.inv);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	/**
	 * An InverseRelation has as single subcomponent the inversed relation expression
	 */
	public Collection<RelationExpression> getComponents()
	{
		HashSet<RelationExpression> components = new HashSet<RelationExpression>();
		components.add(inv);
		return components;
	}
	
	@Override
	public String toRDF()
	{
		return "<" + RDFElement.RELATION_.toRDF() + ">\n" +
				"<" + RDFElement.INVERSE.toRDF() + ">\n" +
				inv.toRDF() +
				"\n</" + RDFElement.INVERSE.toRDF() + ">\n" +
				"</" + RDFElement.RELATION_.toRDF() + ">";
	}

	@Override
	public String toString()
	{
		return "INVERSE[" + inv.toString() + "]";
	}
}