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
* The symmetric closure of an EDOAL Relation / OWL Object Property, denoting  *
* the disjunction of the relation or its inverse.                             *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.alignment.rdf;

import java.util.Collection;
import java.util.HashSet;

public class SymmetricRelation extends RelationExpression
{

//Attributes
	
	private RelationExpression sym;
	
//Constructor
	
	/**
	 * Constructs a new SymmetricRelation from the given relation expression
	 * @param sym: the relation expression to symmetrify
	 */
	public SymmetricRelation(RelationExpression sym)
	{
		super();
		this.sym = sym;
		elements.addAll(sym.getElements());
	}
	
//Public Methods
	
	@Override
	public boolean equals(Object o)
	{
		return o instanceof SymmetricRelation &&
				((SymmetricRelation)o).sym.equals(this.sym);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	/**
	 * A SymmetricRelation has as single subcomponent the symmetric relation expression
	 */
	public Collection<RelationExpression> getComponents()
	{
		HashSet<RelationExpression> components = new HashSet<RelationExpression>();
		components.add(sym);
		return components;
	}
	
	@Override
	public String toRDF()
	{
		return "<edoal:Relation>\n" +
				"<edoal:symmetric>\n" +
				sym.toRDF() +
				"\n</edoal:symmetric>\n" +
				"</edoal:Relation>\n";
	}

	@Override
	public String toString()
	{
		return "SYMMETRIC[" + sym.toString() + "]";
	}
}