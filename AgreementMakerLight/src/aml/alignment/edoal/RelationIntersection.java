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
* An intersection of EDOAL Relations / OWL Object Properies.                  *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.alignment.edoal;

import java.util.Set;

public class RelationIntersection extends RelationExpression
{

//Attributes
	
	private Set<RelationExpression> intersect;
	
//Constructor
	
	/**
	 * Constructs a new RelationIntersection from the given set of relation expressions
	 * @param intersect: the relation expressions in the intersection
	 */
	public RelationIntersection(Set<RelationExpression> intersect)
	{
		this.intersect = intersect;
		for(RelationExpression e : intersect)
			elements.addAll(e.getElements());
	}
	
//Public Methods
	
	@Override
	public boolean equals(Object o)
	{
		return o instanceof RelationIntersection &&
				((RelationIntersection)o).intersect.equals(this.intersect);
	}
	
	@Override
	public String toRDF()
	{
		String rdf = "<edoal:Relation>\n" +
				"<edoal:and rdf:parseType=\"Collection\">\n";
		for(RelationExpression e : intersect)
			rdf += e.toRDF() + "\n";
		rdf += "</edoal:and>\n";
		rdf += "</edoal:Relation>\n";
		return rdf;
	}

	@Override
	public String toString()
	{
		String s = "AND[";
		for(RelationExpression e : intersect)
			s += e.toString() + ", ";
		s = s.substring(0, s.lastIndexOf(',')) + "]";
		return s;
	}
}