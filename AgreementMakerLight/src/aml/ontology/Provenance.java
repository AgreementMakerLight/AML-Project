/******************************************************************************
* Copyright 2013-2013 LASIGE                                                  *
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
* The provenance (type and source) of an entry in the Lexicon and its         *
* associated weight.                                                          *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 22-10-2013                                                            *
******************************************************************************/
package aml.ontology;

public class Provenance implements Comparable<Provenance>
{

//Attributes

	private String type;
	private String source;
	private double weight;
	
//Constructors
	
	/**
	 * Constructs a new Provenance object with the given values
	 * @param t: the type of the lexical entry (localName, label, etc)
	 * @param s: the source of the lexical entry (ontology uri, etc)
	 * @param w: the weight of the lexical entry
	 */
	public Provenance(String t, String s, double w)
	{
		type = t;
		source = s;
		weight = w;
	}

//Public Methods
	
	@Override
	/**
	 * Provenances are compared by weight, which enables
	 * sorting and querying by weight
	 */
	public int compareTo(Provenance o)
	{
		if(this.weight == o.weight)
			return 0;
		if(this.weight > o.weight)
			return 1;
		return -1;
	}
	
	/**
	 * Two Provenances are "equal" if they have the same type
	 * which enables querying by type
	 */
	public boolean equals(Object o)
	{
		Provenance p = (Provenance)o;
		return this.type.equals(p.type);
	}

	/**
	 * @return the source of this Provenance object
	 */
	public String getSource()
	{
		return source;
	}
	
	/**
	 * @return the type of this Provenance
	 */
	public String getType()
	{
		return type;
	}
	
	/**
	 * @return the weight of this Provenance
	 */
	public double getWeight()
	{
		return weight;
	}
	
	/**
	 * @return whether this Provenance is external
	 */
	public boolean isExternal()
	{
		return !source.equals("");
	}
	
	/**
	 * Sets the weight of this Provenance to the given value
	 * @param w: the weight to set
	 */
	public void setWeight(double w)
	{
		weight = w;
	}
}
