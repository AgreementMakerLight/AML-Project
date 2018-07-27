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
* A transformation is a form of mapping that expresses constraints on         *
* instances that should match. They typically should include an Apply (i.e.,  *
* an operation/function) in one of the entities, but that is not enforced.    *
* Although this is not clearly stated in the documentation, I'm assuming that *
* transformation mappings have no similarity or relation.                     *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.alignment.mapping;

import aml.alignment.edoal.AbstractExpression;

public class Transformation extends EDOALMapping
{

//Attributes

	//The direction of the transformation
	private String direction;
	
//Constructors
	
	/**
	 * Creates a transformation mapping between entity1 and entity2 with the given similarity
	 * @param entity1: the EDOAL expression of the source ontology
	 * @param entity2: the EDOAL expression of the target ontology
	 * @param sim: the similarity between the entities
	 * @param dir: the direction of the transformation
	 */
	public Transformation(AbstractExpression entity1, AbstractExpression entity2, String dir)
	{
		super(entity1,entity2,1.0,MappingRelation.EQUIVALENCE,MappingStatus.UNKNOWN);
		direction = dir;
	}
	
	/**
	 * Creates a new transformation mapping that is a copy of m
	 * @param m: the mapping to copy
	 */
	public Transformation(Transformation m)
	{
		this(m.getEntity1(),m.getEntity2(),m.direction);
	}
	
//Public Methods
	
	@Override
	public boolean equals(Object o)
	{
		return o instanceof Transformation &&
				((Transformation)o).direction.equals(this.direction) &&
				super.equals(o);
	}
	
	@Override
	public String toRDF()
	{
		return "\t<map>\n" +
				"\t\t<Cell>\n" +
				"\t\t<edoal:transformation>\n" +
				"\t\t<edoal:Transformation edoal:direction=\"" + direction + "\">\n" +
				"\t\t\t<entity1>\n" +
				((AbstractExpression)entity1).toRDF() +
				"\n\t\t\t</entity1>\n\n" +
				"\t\t\t<entity2>\n" +
				((AbstractExpression)entity2).toRDF() +
				"\n\t\t\t</entity2>\n\n" +
				"\t\t<edoal:Transformation>\n" +
				"\t\t<edoal:transformation>\n" +
				"\t\t</Cell>\n" +
				"\t</map>\n";
	}
	
	@Override
	public String toString()
	{
		return  "TRANSF[" + entity1.toString() + " " + direction + " " + entity2.toString() + "]";
	}
	
	@Override
	public String toTSV()
	{
		String out = entity1.toString() + "\t\t" + entity2.toString() + "\t\t" + direction + "\tTRANSFORMATION";
		if(!status.equals(MappingStatus.UNKNOWN))
			out += "\t" + status;
		return out;
	}
}