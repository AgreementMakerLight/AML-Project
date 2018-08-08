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
* A transformation expresses constraints on instances that should match.      *
* It lists the direction of the transformation, and two entities, one of      *
* which must be an operation (Aggregate or Apply), as indicated by the        *
* direction.                                                                  *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.alignment.rdf;

import java.util.Collection;
import java.util.Vector;

import aml.alignment.rdf.AbstractExpression;

public class Transformation extends AbstractExpression
{

//Attributes

	//The direction of the transformation
	private String direction;
	private ValueExpression entity1;
	private ValueExpression entity2;
	
//Constructors
	
	/**
	 * Creates a transformation with the given direction between entity1 and entity2, one of
	 * which should be an operation
	 * @param dir: the direction of the transformation
	 * @param entity1: the EDOAL expression of the source ontology
	 * @param entity2: the EDOAL expression of the target ontology
	 */
	public Transformation(String direction, ValueExpression entity1, ValueExpression entity2)
	{
		this.direction = direction;
		this.entity1 = entity1;
		this.entity2 = entity2;
		elements.addAll(entity1.getElements());
		elements.addAll(entity2.getElements());
	}
	
//Public Methods
	
	@Override
	public boolean equals(Object o)
	{
		return o instanceof Transformation &&
				((Transformation)o).direction.equals(this.direction) &&
				((Transformation)o).entity1.equals(this.entity1) &&
				((Transformation)o).entity2.equals(this.entity2);
	}
	
	/**
	 * @return the direction of this transformation
	 */
	public String getDirection()
	{
		return direction;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Collection<ValueExpression> getComponents()
	{
		Vector<ValueExpression> components = new Vector<ValueExpression>(2);
		components.add(entity1);
		components.add(entity2);
		return components;
	}
	
	@Override
	public String toRDF()
	{
		return "<map>\n" +
				"<edoal:transformation>\n" +
				"<edoal:Transformation edoal:direction=\"" + direction + "\">\n" +
				"<entity1>\n" + entity1.toRDF() + "\n</entity1>\n\n" +
				"<entity2>\n" + entity2.toRDF() + "\n</entity2>\n\n" +
				"<edoal:Transformation>\n" +
				"<edoal:transformation>\n" +
				"</map>\n";
	}
	
	@Override
	public String toString()
	{
		return  "TRANSF[" + entity1.toString() + " " + direction + " " + entity2.toString() + "]";
	}
}