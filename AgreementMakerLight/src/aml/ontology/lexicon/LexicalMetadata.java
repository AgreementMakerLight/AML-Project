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
* The metadata associated with an entry in the Lexicon: the type, source,     *
* language and weight.                                                        *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.ontology.lexicon;

public class LexicalMetadata implements Comparable<LexicalMetadata>
{

//Attributes

	//The lexical type of the name for the entity
	private LexicalType type;
	//The source of the name for the entity ("" if the name is internal, the URI/name of the external resource otherwise)
	private String source;
	//The language of the name
	private String language;
	//The weight of the name for the entity
	private double weight;
	
//Constructors
	
	/**
	 * Constructs a new LexicalMetadata object with the given values
	 * @param t: the type of the lexical entry (localName, label, etc)
	 * @param s: the source of the lexical entry (ontology uri, etc)
 	 * @param l: the language of the lexical entry ("en", "de", "pt", etc)
	 * @param w: the weight of the lexical entry
	 */
	public LexicalMetadata(LexicalType t, String s, String l, double w)
	{
		type = t;
		source = s;
		language = l;
		weight = w;
	}

//Public Methods
	
	@Override
	/**
	 * LexicalMetadata are compared first with regard to whether
	 * they are internal or external, and then by weight
	 */
	public int compareTo(LexicalMetadata o)
	{
		if(this.isExternal() && !o.isExternal())
			return -1;
		if(!this.isExternal() && o.isExternal())
			return 1;
		if(this.weight > o.weight)
			return 1;
		if(this.weight < o.weight)
			return -1;
		return 0;
	}
	
	/**
	 * Two LexicalMetadata objects are "equal" if they have the same
	 * language so that an (entity,name) pair can only occur
	 * multiple times if it comes from different languages
	 */
	public boolean equals(Object o)
	{
		if(o instanceof LexicalMetadata)
		{
			LexicalMetadata p = (LexicalMetadata)o;
			return language.equals(p.language);
		}
		else
			return false;
	}
	
	/**
	 * @return the language of this LexicalMetadata object
	 */
	public String getLanguage()
	{
		return language;
	}

	/**
	 * @return the source of this LexicalMetadata object
	 */
	public String getSource()
	{
		return source;
	}
	
	/**
	 * @return the type of this LexicalMetadata
	 */
	public LexicalType getType()
	{
		return type;
	}
	
	/**
	 * @return the weight of this LexicalMetadata
	 */
	public double getWeight()
	{
		return weight;
	}
	
	@Override
	public int hashCode()
	{
		String toHash = type.toString() + "_" + language + "_" + source;
		return toHash.hashCode();
	}
	
	/**
	 * @return whether this LexicalMetadata is external
	 */
	public boolean isExternal()
	{
		return !source.equals("");
	}
	
	/**
	 * Sets the weight of this LexicalMetadata to the given value
	 * @param w: the weight to set
	 */
	public void setWeight(double w)
	{
		weight = w;
	}
}