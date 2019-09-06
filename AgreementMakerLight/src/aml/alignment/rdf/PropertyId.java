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
* An EDOAL Property / OWL Data Property.                                      *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.alignment.rdf;

import java.util.Collection;

import aml.AML;

public class PropertyId extends PropertyExpression
{
	
//Attributes
	
	private String lang;
	
//Constructor
	
	/**
	 * Constructs a new PropertyId from the given uri
	 * @param uri: the URI of the data property
	 * @param lang: the language of the data property
	 */
	public PropertyId(String uri, String lang)
	{
		super();
		elements.add(uri);
		this.lang = lang;
	}
	
//Public Methods
	
	@Override
	public boolean equals(Object o)
	{
		if(!(o instanceof PropertyId))
			return false;
		PropertyId p = ((PropertyId)o);
		return p.elements.equals(this.elements) &&
				((p.lang == null && this.lang == null) ||
				(p.lang != null && this.lang != null && p.lang.equals(this.lang)));
	}
	
	@Override
	/**
	 * A PropertyId has no subcomponents
	 */
	public Collection<Expression> getComponents()
	{
		return null;
	}
	
	@Override
	public String toRDF()
	{
		String s = "<" + RDFElement.PROPERTY_.toRDF() + " " +  RDFElement.RDF_ABOUT.toRDF() + "=\"" +
				elements.iterator().next();
		if(lang != null)
			s += "\" edoal:lang=\"" + lang;
		s += "\"/>";
		return s;
	}

	@Override
	public String toString()
	{
		return AML.getInstance().getEntityMap().getLocalName(elements.iterator().next());
	}
}