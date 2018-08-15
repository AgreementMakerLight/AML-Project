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
* A negation of an EDOAL/OWL Class.                                           *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.alignment.rdf;

import java.util.Collection;
import java.util.HashSet;

public class ClassNegation extends ClassExpression
{

//Attributes
	
	private ClassExpression neg;
	
//Constructor
	
	/**
	 * Constructs a new ClassNegation from the given class expression
	 * @param neg: the class expression in the negation
	 */
	public ClassNegation(ClassExpression neg)
	{
		super();
		this.neg = neg;
		elements.addAll(neg.getElements());
	}
	
//Public Methods
	
	@Override
	public boolean equals(Object o)
	{
		return o instanceof ClassNegation &&
				((ClassNegation)o).neg.equals(this.neg);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	/**
	 * A ClassNegation has as single subcomponent the negated class expression
	 */
	public Collection<ClassExpression> getComponents()
	{
		HashSet<ClassExpression> components = new HashSet<ClassExpression>();
		components.add(neg);
		return components;
	}
	
	@Override
	public String toRDF()
	{
		return "<" + RDFElement.CLASS_.toRDF() + ">\n" +
				"<" + RDFElement.NOT.toRDF() + ">\n" +
				neg.toRDF() + 
				"\n</" + RDFElement.NOT.toRDF() + ">\n" +
				"</" + RDFElement.CLASS_.toRDF() + ">";
	}

	@Override
	public String toString()
	{
		return "NOT[" + neg.toString() + "]";
	}
}