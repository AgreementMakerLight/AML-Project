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
* Linkkey is a generalisation of the concept of foreign key from relational   *
* databases. A Linkkey object defines the conditions under which two          *
* individuals should be considered equal, by declaring linked pairs of        *
* properties for which they must have equal values, as well as those for      *
* which there must be intersecting values.                                    *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.alignment.rdf;

import java.util.Collection;
import java.util.HashMap;
import java.util.Vector;

public class LinkKey extends AbstractExpression
{

//Attributes
	
	private String type;
	private HashMap<AttributeExpression,AttributeExpression> equals;
	private HashMap<AttributeExpression,AttributeExpression> intersects;
	
//Constructor
	
	/**
	 * Constructs a new LinkKey from the given maps of attribute expressions
	 * @param equals: the map of attribute expressions whose values must be equal
	 * @param intersects: the map of attribute expressions whose values must intersect
	 */
	public LinkKey(HashMap<AttributeExpression,AttributeExpression> equals, HashMap<AttributeExpression,AttributeExpression> intersects)
	{
		super();
		this.type = null;
		this.equals = equals;
		this.intersects = intersects;
		for(AttributeExpression e : equals.keySet())
		{
			elements.addAll(e.getElements());
			elements.addAll(equals.get(e).getElements());
		}
		for(AttributeExpression e : intersects.keySet())
		{
			elements.addAll(e.getElements());
			elements.addAll(intersects.get(e).getElements());
		}
	}
	
	/**
	 * Constructs a new PropertyUnion from the given set of property expressions
	 * @param union: the property expressions in the union
	 */
	public LinkKey(String type, HashMap<AttributeExpression,AttributeExpression> equals, HashMap<AttributeExpression,AttributeExpression> intersects)
	{
		this(equals,intersects);
		this.type = type;
	}
	
//Public Methods
	
	@Override
	public boolean equals(Object o)
	{
		return o instanceof LinkKey &&
				((LinkKey)o).equals.equals(this.equals) &&
				((LinkKey)o).intersects.equals(this.intersects);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	/**
	 * The components of a LinkKey are an intercallated list of attribute
	 * expressions where every even index has the entity1 key and every
	 * following odd index has the corresponding entity2 key
	 */
	public Collection<AttributeExpression> getComponents()
	{
		Vector<AttributeExpression> components = new Vector<AttributeExpression>();
		for(AttributeExpression e : equals.keySet())
		{
			components.add(e);
			components.add(equals.get(e));
		}
		for(AttributeExpression e : intersects.keySet())
		{
			components.add(e);
			components.add(intersects.get(e));
		}
		return components;
	}
	
	/**
	 * @return the type of this LinkKey
	 */
	public String getType()
	{
		return type;
	}
	
	@Override
	public String toRDF()
	{
		String rdf = "<edoal:linkkey>\n" +
					"<edoal:Linkkey>\n";
		if(type != null)
			rdf += "<lk:type>" + type + "</lk:type>\n";
		for(AttributeExpression e : equals.keySet())
		{
			rdf += "<edoal:binding>\n" + 
					"<edoal:Equals>\n" +
					"<edoal:property1>\n" +
					e.toRDF() +
					"</edoal:property1>" +
					"<edoal:property2>\n" +
					equals.get(e).toRDF() +
					"</edoal:property2>" +
					"</edoal:Equals>\n" +
					"</edoal:binding>\n";
		}
		for(AttributeExpression e : intersects.keySet())
		{
			rdf += "<edoal:binding>\n" + 
					"<edoal:Intersects>\n" +
					"<edoal:property1>\n" +
					e.toRDF() +
					"</edoal:property1>" +
					"<edoal:property2>\n" +
					intersects.get(e).toRDF() +
					"</edoal:property2>" +
					"</edoal:Intersects>\n" +
					"</edoal:binding>\n";
		}
		rdf += "</edoal:Linkkey>\n";
		rdf += "</edoal:linkkey>\n";
		return rdf;
	}

	@Override
	public String toString()
	{
		String s = "LinkKey";
		if(type != null)
			s += "(" + type + ")";
		if(!equals.isEmpty())
		{
			s += " EQUALS:";
			for(AttributeExpression e : equals.keySet())
				s += "<" + e.toString() + "," + equals.get(e).toString() + ">";
		}
		if(!intersects.isEmpty())
		{
			s += " INTERSECTS:";
			for(AttributeExpression e : intersects.keySet())
				s += "<" + e.toString() + "," + intersects.get(e).toString() + ">";
		}
		return s;
	}
}