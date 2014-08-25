/******************************************************************************
* Copyright 2013-2014 LASIGE                                                  *
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
* A mapping between terms or properties of two Ontologies, including the      *
* similarity and type of relationship between them.                           *
* An element in an Alignment.                                                 *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 23-06-2014                                                            *
* @version 2.0                                                                *
******************************************************************************/
package aml.match;

import aml.AML;
import aml.ontology.URIMap;
import aml.settings.MappingRelation;

public class Mapping implements Comparable<Mapping>
{

//Attributes

	//The id of the source ontology term
	private int sourceId;
	//The id of the target ontology term
	private int targetId;
	//The similarity between the terms
	private double similarity;
	//The relationship between the terms
	private MappingRelation rel;
	
//Constructors

	/**
	 * Creates a mapping between sId and tId
	 * @param sId: the id of the source ontology term
	 * @param tId: the id of the target ontology term
	 */
	public Mapping(int sId, int tId)
	{
		sourceId = sId;
		targetId = tId;
		similarity = 1.0;
		rel = MappingRelation.EQUIVALENCE;
	}
	
	/**
	 * Creates a mapping between sId and tId with similarity = sim
	 * @param sId: the id of the source ontology term
	 * @param tId: the id of the target ontology term
	 * @param sim: the similarity between the terms
	 */
	public Mapping(int sId, int tId, double sim)
	{
		sourceId = sId;
		targetId = tId;
		similarity = Math.round(sim*10000)/10000.0;
		rel = MappingRelation.EQUIVALENCE;
	}
	
	/**
	 * Creates a mapping between sId and tId with similarity = sim
	 * @param sId: the id of the source ontology term
	 * @param tId: the id of the target ontology term
	 * @param sim: the similarity between the terms
	 * @param r: the mapping relationship between the terms
	 */
	public Mapping(int sId, int tId, double sim, MappingRelation r)
	{
		sourceId = sId;
		targetId = tId;
		similarity = Math.round(sim*10000)/10000.0;
		rel = r;
	}
	
	/**
	 * Creates a new mapping that is a copy of m
	 * @param m: the mapping to copy
	 */
	public Mapping(Mapping m)
	{
		sourceId = m.sourceId;
		targetId = m.targetId;
		similarity = m.similarity;
		rel = m.rel;
	}

//Public Methods

	@Override
	/**
	 * Mappings are compared based only on their similarity,
	 * which enables sorting the Alignment and selecting the
	 * best Mapping for a given term
	 */
	public int compareTo(Mapping o)
	{
		double diff = this.similarity - o.similarity;
		if(diff < 0)
			return -1;
		if(diff > 0)
			return 1;
		return 0;
	}
	
	/**
	 * Two Mappings are equal if they map the same two terms
	 * irrespective of the similarity or relationship, which
	 * enables finding redundant Mappings
	 */
	public boolean equals(Object o)
	{
		Mapping m = (Mapping)o;
		return (this.sourceId == m.sourceId && this.targetId == m.targetId);
	}
	
	/**
	 * @return the mapping relation between the mapped terms
	 */
	public MappingRelation getRelationship()
	{
		return rel;
	}
	
	/**
	 * @return the similarity between the mapped terms
	 */
	public double getSimilarity()
	{
		return similarity;
	}
	
	/**
	 * @return the id of the source term
	 */
	public int getSourceId()
	{
		return sourceId;
	}

	/**
	 * @return the id of the target term
	 */
	public int getTargetId()
	{
		return targetId;
	}
	
	/**
	 * Sets the similarity of the Mapping to sim
	 * @param r: the relationship between the mapped terms
	 */
	public void setRelationship(MappingRelation r)
	{
		rel = r;
	}
	
	/**
	 * Sets the similarity of the Mapping to sim
	 * @param sim: the similarity between the mapped terms
	 */
	public void setSimilarity(double sim)
	{
		similarity = Math.round(sim*10000)/10000.0;
	}
	
	@Override
	public String toString()
	{
		URIMap uris = AML.getInstance().getURIMap();
		return uris.getURI(sourceId) + " " + rel.toString() + " " +
				uris.getURI(targetId) + " " + similarity;
	}
}
