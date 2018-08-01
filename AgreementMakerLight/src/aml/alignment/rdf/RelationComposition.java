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
* An EDOAL Relation Composition is an ordered path of Relations that is       *
* equivalent to an OWL Object Relation Chain.                                 *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.alignment.rdf;

import java.util.Collection;
import java.util.Vector;

public class RelationComposition extends RelationExpression
{

//Attributes
	
	private Vector<RelationExpression> path;
	
//Constructor
	
	/**
	 * Constructs a new RelationComposition from the given list of relation expressions
	 * @param path: the list of relation expressions in this composition
	 */
	public RelationComposition(Vector<RelationExpression> path)
	{
		super();
		this.path = path;
		for(RelationExpression e : path)
			elements.addAll(e.getElements());
	}
	
//Public Methods
	
	@Override
	public boolean equals(Object o)
	{
		return o instanceof RelationComposition &&
				((RelationComposition)o).path.equals(this.path);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	/**
	 * The components of a RelationComposition are the list of relation
	 * expressions in the composition
	 */
	public Collection<RelationExpression> getComponents()
	{
		return path;
	}
	
	@Override
	public String toRDF()
	{
		String rdf = "<edoal:Relation>\n" +
				"<edoal:compose rdf:parseType=\"Collection\">\n";
		for(RelationExpression e : path)
			rdf += e.toRDF() + "\n";
		rdf += "</edoal:compose>\n";
		rdf += "</edoal:Relation>\n";
		return rdf;
	}

	@Override
	public String toString()
	{
		String s = "PATH[";
		for(RelationExpression e : path)
			s += e.toString() + ", ";
		s = s.substring(0, s.lastIndexOf(',')) + "]";
		return s;
	}
}