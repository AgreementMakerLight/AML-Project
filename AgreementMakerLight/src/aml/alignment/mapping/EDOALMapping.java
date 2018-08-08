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
* A complex mapping based on EDOAL syntax.                                    *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.alignment.mapping;

import org.apache.commons.lang.StringEscapeUtils;

import aml.alignment.rdf.AbstractExpression;

public class EDOALMapping extends Mapping<AbstractExpression>
{
	
//Constructors

	/**
	 * Creates a mapping between entity1 and entity2 with the given similarity
	 * @param entity1: the EDOAL expression of the source ontology
	 * @param entity2: the EDOAL expression of the target ontology
	 * @param sim: the similarity between the entities
	 */
	public EDOALMapping(AbstractExpression entity1, AbstractExpression entity2, double sim)
	{
		this(entity1,entity2,sim,MappingRelation.EQUIVALENCE);
	}
	
	/**
	 * Creates a mapping between entity1 and entity2 with the given similarity and relation
	 * @param entity1: the uri of the source ontology entity
	 * @param entity2: the uri of the target ontology entity
	 * @param sim: the similarity between the entities
	 * @param r: the mapping relationship between the entities
	 */
	public EDOALMapping(AbstractExpression entity1, AbstractExpression entity2, double sim, MappingRelation r)
	{
		super(entity1,entity2,sim,r);
	}
	
	
	/**
	 * Creates a new mapping that is a copy of m
	 * @param m: the mapping to copy
	 */
	public EDOALMapping(EDOALMapping m)
	{
		this(m.getEntity1(),m.getEntity2(),m.similarity,m.rel);
		this.status = m.status;
	}

//Public Methods

	@Override
	public AbstractExpression getEntity1()
	{
		return (AbstractExpression)entity1;
	}

	@Override
	public AbstractExpression getEntity2()
	{
		return (AbstractExpression)entity2;
	}

	@Override
	public String toRDF()
	{
		String out = "<map>\n" +
				"<Cell>\n" +
				"<entity1>\n" +
				((AbstractExpression)entity1).toRDF() +
				"\n</entity1>\n\n" +
				"<entity2>\n" +
				((AbstractExpression)entity2).toRDF() +
				"\n</entity2>\n\n" +
				"<measure rdf:datatype=\"http://www.w3.org/2001/XMLSchema#float\">"+ similarity +"</measure>\n" +
				"<relation>" + StringEscapeUtils.escapeXml(rel.toString()) + "</relation>\n";
			out += "</Cell>\n" +
				"</map>\n";
			return out;
	}
	
	@Override
	public String toString()
	{
		return entity1.toString() + " " + rel.toString() + " " + entity2.toString() +
				" (" + getSimilarityPercent() + ") ";
	}
}