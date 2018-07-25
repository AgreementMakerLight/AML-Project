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
* An EDOAL Property Composition is an ordered path of Relations ending with   *
* a Property. It is equivalent to an OWL Object Property Chain, except that   *
* the last element is a Data Property.                                        *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.alignment.edoal;

import java.util.Collection;
import java.util.Vector;

public class PropertyComposition extends PropertyExpression
{

//Attributes
	
	private Vector<RelationExpression> path;
	private PropertyExpression end;
	
//Constructor
	
	/**
	 * Constructs a new PropertyComposition from the given list of relation expressions plus property expression
	 * @param path: the list of relation expressions in the composition
	 * @param end: the property expression that is the last element of the composition
	 */
	public PropertyComposition(Vector<RelationExpression> path, PropertyExpression end)
	{
		super();
		this.path = path;
		this.end = end;
		for(RelationExpression e : path)
			elements.addAll(e.getElements());
		elements.addAll(end.getElements());
	}
	
//Public Methods
	
	@Override
	public boolean equals(Object o)
	{
		return o instanceof PropertyComposition &&
				((PropertyComposition)o).end.equals(this.end) &&
				((PropertyComposition)o).path.equals(this.path);
	}
	
	@Override
	public Collection<EDOALExpression> getComponents()
	{
		Vector<EDOALExpression> components = new Vector<EDOALExpression>(path);
		components.add(end);
		return components;
	}
	
	@Override
	public String toRDF()
	{
		String rdf = "<edoal:Property>\n" +
				"<edoal:compose rdf:parseType=\"Collection\">\n";
		for(RelationExpression e : path)
			rdf += e.toRDF() + "\n";
		rdf += end.toRDF() + "\n";
		rdf += "</edoal:compose>\n";
		rdf += "</edoal:Property>\n";
		return rdf;
	}

	@Override
	public String toString()
	{
		String s = "PATH[";
		for(RelationExpression e : path)
			s += e.toString() + ", ";
		s += end.toString() + "]";
		return s;
	}
}