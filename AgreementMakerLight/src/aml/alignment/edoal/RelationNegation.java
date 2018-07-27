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
* A negation of an EDOAL Relation / OWL Object Property.                      *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.alignment.edoal;

import java.util.Collection;
import java.util.HashSet;

public class RelationNegation extends RelationExpression
{

//Attributes
	
	private RelationExpression neg;
	
//Constructor
	
	/**
	 * Constructs a new RelationNegation from the given relation expression
	 * @param neg: the relation expression in the negation
	 */
	public RelationNegation(RelationExpression neg)
	{
		super();
		this.neg = neg;
		elements.addAll(neg.getElements());
	}
	
//Public Methods
	
	@Override
	public boolean equals(Object o)
	{
		return o instanceof RelationNegation &&
				((RelationNegation)o).neg.equals(this.neg);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	/**
	 * A RelationNegation has as single subcomponent the negated relation expression
	 */
	public Collection<RelationExpression> getComponents()
	{
		HashSet<RelationExpression> components = new HashSet<RelationExpression>();
		components.add(neg);
		return components;
	}
	
	@Override
	public String toRDF()
	{
		return "<edoal:Relation>\n" +
				"<edoal:not>\n" +
				neg.toRDF() +
				"\n</edoal:not>\n" +
				"</edoal:Relation>\n";
	}

	@Override
	public String toString()
	{
		return "NOT[" + neg.toString() + "]";
	}
}