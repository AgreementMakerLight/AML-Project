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
* An intersection of EDOAL/OWL Classes.                                       *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.alignment.edoal;

import java.util.Set;

public class ClassIntersection extends ClassExpression
{

//Attributes
	
	private Set<ClassExpression> intersect;
	
//Constructor
	
	/**
	 * Constructs a new ClassIntersection from the given set of class expressions
	 * @param intersect: the class expressions in the intersection
	 */
	public ClassIntersection(Set<ClassExpression> intersect)
	{
		this.intersect = intersect;
		for(ClassExpression e : intersect)
			elements.addAll(e.getElements());
	}
	
//Public Methods
	
	@Override
	public boolean equals(Object o)
	{
		return o instanceof ClassIntersection &&
				((ClassIntersection)o).intersect.equals(this.intersect);
	}
	
	@Override
	public String toRDF()
	{
		String rdf = "<edoal:Class>\n" +
				"<edoal:and rdf:parseType=\"Collection\">\n";
		for(ClassExpression e : intersect)
			rdf += e.toRDF() + "\n";
		rdf += "</edoal:and>\n";
		rdf += "</edoal:Class>\n";
		return rdf;
	}

	@Override
	public String toString()
	{
		String s = "AND[";
		for(ClassExpression e : intersect)
			s += e.toString() + ", ";
		s = s.substring(0, s.lastIndexOf(',')) + "]";
		return s;
	}
}