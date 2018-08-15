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
* An EDOAL mapping that includes transformations, thus expressing constraints *
* on instances that should match.                                             *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.alignment.mapping;

import java.util.Set;

import org.apache.commons.lang.StringEscapeUtils;

import aml.alignment.rdf.ClassExpression;
import aml.alignment.rdf.RDFElement;
import aml.alignment.rdf.Transformation;
import aml.settings.Namespace;

public class TransformationMapping extends EDOALMapping
{

//Attributes

	//The direction of the transformation
	private Set<Transformation> transformations;
	
//Constructors
	
	/**
	 * Creates a transformation mapping between entity1 and entity2 with the given similarity
	 * @param entity1: the EDOAL expression of the source ontology
	 * @param entity2: the EDOAL expression of the target ontology
	 * @param sim: the similarity between the entities
	 * @param dir: the direction of the transformation
	 */
	public TransformationMapping(ClassExpression entity1, ClassExpression entity2, double similarity, MappingRelation r, Set<Transformation> t)
	{
		super(entity1,entity2,1.0,MappingRelation.EQUIVALENCE);
		transformations = t;
	}
	
	/**
	 * Creates a new transformation mapping that is a copy of m
	 * @param m: the mapping to copy
	 */
	public TransformationMapping(TransformationMapping m)
	{
		this(m.getEntity1(),m.getEntity2(),m.similarity,m.rel,m.transformations);
		this.status = m.status;
	}
	
//Public Methods
	
	@Override
	public boolean equals(Object o)
	{
		return o instanceof TransformationMapping &&
				((TransformationMapping)o).transformations.equals(this.transformations) &&
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
	 * @return the set of transformations in this mapping
	 */
	public Set<Transformation> getTransformations()
	{
		return transformations;
	}
	
	@Override
	public String toRDF()
	{
		String s = "<map>\n" +
				"<align:Cell rdf:about=\"#cell-with-linkkey\">\n" +
				"<entity1>\n" + entity1.toRDF() + "\n</entity1>\n" +
				"<entity2>\n" +	entity2.toRDF() + "\n</entity2>\n" +
				"<relation>" + StringEscapeUtils.escapeXml(rel.toString()) + "</relation>\n" +
				"<measure " + RDFElement.RDF_DATATYPE.toRDF() + "=\"" + Namespace.XSD.prefix() + "float\">"+ similarity +"</measure>\n";
		for(Transformation t : transformations)
			s += t.toRDF();
		s += "</Cell>\n</map>";
		return s;
	}
}