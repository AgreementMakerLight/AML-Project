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
* A Literal.                                                                  *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.alignment.rdf;

import java.util.Collection;

public class Literal extends AbstractExpression implements ValueExpression
{

//Attributes
	
	protected String type;
	protected String lang;
	protected String value;
	
//Constructor

	/**
	 * Constructs a new Literal of the given type, language and value
	 * @param type: the type of the Literal (use null for rdfs:PlainLiteral)
	 * @param lang: the language of the Literal (use null if none is declared)
	 * @param value: the value of the Literal (must not be null)
	 */
	public Literal(String value, String type, String lang)
	{
		super();
		this.value = value;
		this.type = type;
		this.lang = lang;
	}

//Public Methods
	
	@Override
	public boolean equals(Object o)
	{
		if(o instanceof Literal)
		{
			Literal l = (Literal)o;
			return l.type.equals(this.type) &&
				l.value.equals(this.value) &&
				((l.lang == null && this.lang == null) || l.lang.equals(this.lang));
		}
		else
			return false;
	}
	
	@Override
	/**
	 * A Literal has no subcomponents
	 */
	public Collection<Expression> getComponents()
	{
		return null;
	}
	
	@Override
	/**
	 * A Literal has no (matchable) elements, so its hashCode is defined by its value
	 */
	public int hashCode()
	{
		return value.hashCode();
	}
	
	@Override
	public String toRDF()
	{
		String rdf = "<edoal:Literal "; 
		if(type != null)
			rdf += "edoal:type=\"" + type + "\" ";
		if(lang != null)
			rdf += "edoal:lang=\"" + lang + "\" ";
		rdf += "edoal:string=\"" + value + "\"/>";
		return rdf;
	}

	@Override
	public String toString()
	{
		return "\"" + value + (lang == null ? "" : "@" + lang) + "\"" +
				(type == null ? "" : "^^" + type);
	}
}