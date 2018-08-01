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
* An EDOAL mapping that includes linkkeys, and thus declares the conditions   *
* under which instances of the two mapped classes can be considered equal.    *
* Although documentation is unclear, I am assuming that no similarity should  *
* be provided in such a mapping, and that the relation is always equivalence 
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.alignment.mapping;

import org.apache.commons.lang.StringEscapeUtils;

import aml.alignment.rdf.ClassExpression;
import aml.alignment.rdf.LinkKey;

public class LinkKeyMapping extends EDOALMapping
{

//Attributes
	
	private LinkKey l;
	
//Constructors

	/**
	 * Creates a mapping between entity1 and entity2 with the given similarity
	 * @param entity1: the EDOAL class expression of the source ontology
	 * @param entity2: the EDOAL class expression of the target ontology
	 * @param l: the link keys between properties of instances of the class expressions
	 */
	public LinkKeyMapping(ClassExpression entity1, ClassExpression entity2, LinkKey l)
	{
		super(entity1,entity2,1.0,MappingRelation.EQUIVALENCE);
		this.l = l;
	}
	
	/**
	 * Creates a new mapping that is a copy of m
	 * @param m: the mapping to copy
	 */
	public LinkKeyMapping(LinkKeyMapping m)
	{
		this(m.getEntity1(),m.getEntity2(),m.l);
	}

//Public Methods

	@Override
	public boolean equals(Object o)
	{
		return o instanceof LinkKeyMapping &&
				((LinkKeyMapping)o).l.equals(this.l) &&
				super.equals(o);
	}
	
	@Override
	public ClassExpression getEntity1()
	{
		return (ClassExpression)entity1;
	}

	@Override
	public ClassExpression getEntity2()
	{
		return (ClassExpression)entity2;
	}
	
	/**
	 * @return the LinkKey of this mapping
	 */
	public LinkKey getLinkKey()
	{
		return l;
	}

	@Override
	public String toRDF()
	{
		return "<map>\n" +
				"<align:Cell rdf:about=\"#cell-with-linkkey\">\n" +
				"<entity1>\n" +
				((ClassExpression)entity1).toRDF() +
				"\n</entity1>\n\n" +
				"<entity2>\n" +
				((ClassExpression)entity2).toRDF() +
				"\n</entity2>\n\n" +
				"<relation>" + StringEscapeUtils.escapeXml(rel.toString()) + "</relation>\n" +
				l.toRDF() +
				"</Cell>\n" +
				"</map>\n";
	}
	
	@Override
	public String toString()
	{
		return entity1.toString() + " " + rel.toString() + " " + entity2.toString() +
				" (" + getSimilarityPercent() + ") ";
	}
	
	@Override
	public String toTSV()
	{
		String out = entity1.toString() + "\t\t" + entity2.toString() + "\t\t" + similarity + "\t" + rel.toString();
		if(!status.equals(MappingStatus.UNKNOWN))
			out += "\t" + status;
		return out;
	}
}