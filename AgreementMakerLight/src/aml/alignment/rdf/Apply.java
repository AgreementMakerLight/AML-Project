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
* Application of an operator to a list of arguments (which can be any type of *
* Expression). Apply is "not operational" according to EDOAL documentation.   *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.alignment.rdf;

import java.util.Collection;
import java.util.Vector;

import aml.AML;

public class Apply extends AbstractExpression implements ValueExpression
{

//Attributes
	
	private String operator;
	//It is unclear whether the order of the arguments matters or not
	//so to be on the safe side, we'll preserve it
	private Vector<AbstractExpression> arguments;
	
//Constructor
	
	/**
	 * Constructs a new Apply with the given operator and arguments
	 * @param operator: the uri of the operator to apply
	 * @param arguments: the expressions that are arguments of the operation
	 */
	public Apply(String operator, Vector<AbstractExpression> arguments)
	{
		super();
		this.operator = operator;
		this.arguments = arguments;
		for(AbstractExpression e : arguments)
			elements.addAll(e.getElements());
	}
	
//Public Methods
	
	@Override
	public boolean equals(Object o)
	{
		return o instanceof Apply &&
				((Apply)o).operator.equals(this.operator) &&
				((Apply)o).arguments.equals(this.arguments);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	/**
	 * The components of an Apply are the list of arguments
	 */
	public Collection<AbstractExpression> getComponents()
	{
		return arguments;
	}
	
	/**
	 * @return the operator of this apply
	 */
	public String getOperator()
	{
		return operator;
	}
	
	@Override
	public String toRDF()
	{
		String rdf = "<edoal:Apply edoal:operator=\"" + operator + "\">" +
				"<edoal:arguments rdf:parseType=\"Collection\">\n";
		for(AbstractExpression e : arguments)
			rdf += e.toRDF() + "\n";
		rdf += "</edoal:arguments>\n";
		rdf += "</edoal:Apply>\n";
		return rdf;
	}

	@Override
	public String toString()
	{
		String s = AML.getInstance().getEntityMap().getLocalName(operator) + " [";
		for(AbstractExpression e : arguments)
			s += e.toString() + ", ";
		s = s.substring(0, s.lastIndexOf(',')) + "]";
		return s;
	}
}