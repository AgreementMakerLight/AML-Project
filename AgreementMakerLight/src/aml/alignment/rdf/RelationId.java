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
* An EDOAL Relation / OWL Object Property.                                    *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.alignment.rdf;

import java.util.Collection;

import aml.AML;

public class RelationId extends RelationExpression
{

//Constructor
	
	/**
	 * Constructs a new PropertyId from the given uri
	 * @param uri: the URI of the data property
	 */
	public RelationId(String uri)
	{
		super();
		elements.add(uri);
	}
	
//Public Methods
	
	@Override
	public boolean equals(Object o)
	{
		return o instanceof RelationId &&
				((RelationId)o).elements.equals(this.elements);
	}
	
	@Override
	/**
	 * A RelationId has no subcomponents
	 */
	public Collection<Expression> getComponents()
	{
		return null;
	}
	
	@Override
	public String toRDF()
	{
		return "<" + RDFElement.RELATION_.toRDF() + " " +  RDFElement.RDF_ABOUT.toRDF() + "=\"" +
				AML.getInstance().getEntityMap().getLocalName(elements.iterator().next()) +
				"\"/>";
	}

	@Override
	public String toString()
	{
		return AML.getInstance().getEntityMap().getLocalName(elements.iterator().next());
	}
}