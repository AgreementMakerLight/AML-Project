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
* An Individual (which is the only type of Individual Expression in EDOAL).   *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.alignment.edoal;

import java.util.Collection;

import aml.AML;

public class IndividualId extends AbstractExpression implements ValueExpression
{
	
//Constructor
	
	/**
	 * Constructs a new IndividualId from the given uri
	 * @param uri: the URI of the class
	 */
	public IndividualId(String uri)
	{
		super();
		elements.add(uri);
	}
	
//Public Methods
	
	@Override
	public boolean equals(Object o)
	{
		return o instanceof IndividualId &&
				((IndividualId)o).elements.equals(this.elements);
	}
	
	@Override
	/**
	 * An IndividualId has no subcomponents
	 */
	public Collection<Expression> getComponents()
	{
		return null;
	}
	
	@Override
	public String toRDF()
	{
		return "<edoal:Instance rdf:about=\"" +
				AML.getInstance().getEntityMap().getLocalName(elements.iterator().next()) +
				"\"/>";
	}

	@Override
	public String toString()
	{
		return AML.getInstance().getEntityMap().getLocalName(elements.iterator().next());
	}
}