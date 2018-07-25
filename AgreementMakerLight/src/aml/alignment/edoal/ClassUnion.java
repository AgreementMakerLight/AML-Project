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
* A union of EDOAL/OWL Classes.                                               *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.alignment.edoal;

import java.util.Set;

public class ClassUnion extends ClassExpression
{

//Attributes
	
	private Set<ClassExpression> union;
	
//Constructor
	
	/**
	 * Constructs a new ClassUnion from the given set of class expressions
	 * @param uri: the class expressions in the union
	 */
	public ClassUnion(Set<ClassExpression> union)
	{
		this.union = union;
		for(ClassExpression e : union)
			elements.addAll(e.getElements());
	}
	
//Public Methods
	
	@Override
	public boolean equals(Object o)
	{
		return o instanceof ClassUnion &&
				((ClassUnion)o).union.equals(this.union);
	}
	
	@Override
	public String toRDF()
	{
		String rdf = "<edoal:Class>\n" +
				"<edoal:or rdf:parseType=\"Collection\">\n";
		for(ClassExpression e : union)
			rdf += e.toRDF() + "\n";
		rdf += "</edoal:or>\n";
		rdf += "</edoal:Class>\n";
		return rdf;
	}

	@Override
	public String toString()
	{
		String s = "OR[";
		for(ClassExpression e : union)
			s += e.toString() + ", ";
		s = s.substring(0, s.lastIndexOf(',')) + "]";
		return s;
	}
}