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
* The transitive closure of an EDOAL Relation / OWL Object Property.          *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.alignment.rdf;

import java.util.Collection;
import java.util.HashSet;

public class TransitiveRelation extends RelationExpression
{

//Attributes
	
	private RelationExpression trans;
	
//Constructor
	
	/**
	 * Constructs a new ReflexiveRelation from the given relation expression
	 * @param trans: the relation expression to reflect
	 */
	public TransitiveRelation(RelationExpression trans)
	{
		super();
		this.trans = trans;
		elements.addAll(trans.getElements());
	}
	
//Public Methods
	
	@Override
	public boolean equals(Object o)
	{
		return o instanceof TransitiveRelation &&
				((TransitiveRelation)o).trans.equals(this.trans);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	/**
	 * A TransitiveRelation has as single subcomponent the transitive relation expression
	 */
	public Collection<RelationExpression> getComponents()
	{
		HashSet<RelationExpression> components = new HashSet<RelationExpression>();
		components.add(trans);
		return components;
	}
	
	@Override
	public String toRDF()
	{
		return "<" + RDFElement.RELATION_.toRDF() + ">\n" +
				"<" + RDFElement.TRANSITIVE.toRDF() + ">\n" +
				trans.toRDF() +
				"\n</" + RDFElement.TRANSITIVE.toRDF() + ">\n" +
				"</" + RDFElement.RELATION_.toRDF() + ">\n";
	}

	@Override
	public String toString()
	{
		return "TRANSITIVE[" + trans.toString() + "]";
	}
}