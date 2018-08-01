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
* A Datatype.                                                                 *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.alignment.rdf;

import java.util.Collection;

public class Datatype extends AbstractExpression
{

//Attributes
	
	protected String uri;
	
//Constructor

	/**
	 * Constructs a new Datatype with the given uri
	 * @param uri: the uri of the Datatype
	 */
	public Datatype(String uri)
	{
		super();
		this.uri = uri;
		//Datatypes are not matchable, so even if the datatype in question is
		//specific to any of the input ontologies, we don't add it to the entities
	}

//Public Methods
	
	@Override
	public boolean equals(Object o)
	{
		return o instanceof Datatype && ((Datatype)o).uri.equals(this.uri);
	}
	
	@Override
	/**
	 * A Datatype has no subcomponents
	 */
	public Collection<Expression> getComponents()
	{
		return null;
	}
	
	@Override
	/**
	 * A Datatype has no (matchable) elements, so its hashCode is defined by its URI
	 */
	public int hashCode()
	{
		return uri.hashCode();
	}
	
	@Override
	public String toRDF()
	{
		return "<edoal:Datatype rdf:about=\"" + uri + "\"/>";
	}

	@Override
	public String toString()
	{
		return uri;
	}
}