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
* The reflexive closure of an EDOAL Relation / OWL Object Property.           *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.alignment.edoal;

import java.util.Collection;
import java.util.HashSet;

public class ReflexiveRelation extends RelationExpression
{

//Attributes
	
	private RelationExpression ref;
	
//Constructor
	
	/**
	 * Constructs a new ReflexiveRelation from the given relation expression
	 * @param ref: the relation expression to reflect
	 */
	public ReflexiveRelation(RelationExpression ref)
	{
		this.ref = ref;
		elements.addAll(ref.getElements());
	}
	
//Public Methods
	
	@Override
	public boolean equals(Object o)
	{
		return o instanceof ReflexiveRelation &&
				((ReflexiveRelation)o).ref.equals(this.ref);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	/**
	 * A ReflexiveRelation has as single subcomponent the reflexive relation expression
	 */
	public Collection<RelationExpression> getComponents()
	{
		HashSet<RelationExpression> components = new HashSet<RelationExpression>();
		components.add(ref);
		return components;
	}
	
	@Override
	public String toRDF()
	{
		return "<edoal:Relation>\n" +
				"<edoal:reflexive>\n" +
				ref.toRDF() +
				"\n</edoal:reflexive>\n" +
				"</edoal:Relation>\n";
	}

	@Override
	public String toString()
	{
		return "REFLEXIVE[" + ref.toString() + "]";
	}
}