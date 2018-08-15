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
* A union of EDOAL Relations / OWL Object Properies.                          *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.alignment.rdf;

import java.util.Collection;
import java.util.Set;

public class RelationUnion extends RelationExpression
{

//Attributes
	
	private Set<RelationExpression> union;
	
//Constructor
	
	/**
	 * Constructs a new RelationUnion from the given set of relation expressions
	 * @param union: the relation expressions in the union
	 */
	public RelationUnion(Set<RelationExpression> union)
	{
		super();
		this.union = union;
		for(RelationExpression e : union)
			elements.addAll(e.getElements());
	}
	
//Public Methods
	
	@Override
	public boolean equals(Object o)
	{
		return o instanceof RelationUnion &&
				((RelationUnion)o).union.equals(this.union);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	/**
	 * The components of a RelationUnion are the set of relation
	 * expressions in the union
	 */
	public Collection<RelationExpression> getComponents()
	{
		return union;
	}
	
	@Override
	public String toRDF()
	{
		String rdf =  "<" + RDFElement.RELATION_.toRDF() + ">\n" +
				"<" + RDFElement.OR.toRDF() + " " + RDFElement.RDF_PARSETYPE.toRDF() + "=\"Collection\">\n";
		for(RelationExpression e : union)
			rdf += e.toRDF() + "\n";
		rdf += "</" + RDFElement.OR.toRDF() + ">\n";
		rdf += "</" + RDFElement.RELATION_.toRDF() + ">";
		return rdf;
	}

	@Override
	public String toString()
	{
		String s = "OR[";
		for(RelationExpression e : union)
			s += e.toString() + ", ";
		s = s.substring(0, s.lastIndexOf(',')) + "]";
		return s;
	}
}