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
* A Mapping represents an element in an Alignment.                            *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.alignment.mapping;

public abstract class Mapping<A> implements Comparable<Mapping<A>>
{
	
//Attributes

	//The source ontology entity
	protected A entity1;
	//The target ontology entity
	protected A entity2;
	//The similarity between the terms
	protected double similarity;
	//The relationship between the terms
	protected MappingRelation rel;
	//The status of the Mapping
	protected MappingStatus status;
	
//Constructors
	
	/**
	 * Creates a mapping between entity1 and entity2 with the given similarity, relation and status
	 * @param entity1: the source ontology entity
	 * @param entity2: the target ontology entity
	 * @param sim: the similarity between the entities
	 * @param r: the mapping relationship between the entities
	 * @param s: the status of the maping
	 */
	public Mapping(A entity1, A entity2, double sim, MappingRelation r)
	{
		this.entity1 = entity1;
		this.entity2 = entity2;
		if(sim < 0)
			this.similarity = 0;
		else if(sim > 1)
			this.similarity = 1;
		else
			this.similarity = sim;
		this.similarity = Math.round(this.similarity*1000)/1000.0;
		this.rel = r;
		this.status = MappingStatus.UNKNOWN;
	}
	
	/**
	 * Creates a new mapping that is a copy of m
	 * @param m: the mapping to copy
	 */
	public Mapping(Mapping<A> m)
	{
		this(m.entity1,m.entity2,m.similarity,m.rel);
		this.status = m.status;
	}

//Public Methods	
	
	/**
	 * Mappings are compared first based on their status, then
	 * based on their similarity. This enables both sorting by
	 * status for the GUI and sorting by similarity during the
	 * matching procedure, as all mappings will have UNKNOWN
	 * status at that stage.
	 */
	@Override
	public int compareTo(Mapping<A> o)
	{
		if(this.getStatus().equals(o.getStatus()))
		{
			double diff = this.getSimilarity() - o.getSimilarity();
			if(diff < 0)
				return -1;
			if(diff > 0)
				return 1;
			return 0;
		}
		else return this.getStatus().compareTo(o.getStatus());
	}
	
	/**
	 * Two Mappings are equal if they map the same two entities
	 * irrespective of the similarity or relationship
	 * (this enables finding redundant Mappings)
	 */
	@Override
	@SuppressWarnings("rawtypes")
	public boolean equals(Object o)
	{
		if(!(o instanceof Mapping))
			return false;
		Mapping m = (Mapping)o;
		return (this.entity1.equals(m.entity1) && this.entity2.equals(m.entity2));
	}

	/**
	 * @return the source entity in the Mapping
	 */
	public A getEntity1()
	{
		return entity1;
	}

	/**
	 * @return the target entity in the Mapping
	 */
	public A getEntity2()
	{
		return entity2;
	}
	
	/**
	 * @return the MappingRelation of the Mapping
	 */
	public MappingRelation getRelationship()
	{
		return rel;
	}
	
	/**
	 * @return the similarity of the Mapping
	 */
	public double getSimilarity()
	{
		return similarity;
	}
	
	/**
	 * @return the similarity of the Mapping formatted as percentage
	 */
	public String getSimilarityPercent()
	{
		return (Math.round(similarity*10000) * 1.0 / 100) + "%";
	}
	
	/**
	 * @return the MappingStatus of the Mapping
	 */
	public MappingStatus getStatus()
	{
		return status;
	}
	
	/**
	 * Sets the MappingRelation of the Mapping
	 * @param r: the MappingRelation to set
	 */
	public void setRelationship(MappingRelation r)
	{
		this.rel = r;
	}
	
	/**
	 * Sets the similarity of the Mapping
	 * @param sim: the similarity to set
	 */
	public void setSimilarity(double sim)
	{
		if(sim < 0)
			this.similarity = 0;
		else if(sim > 1)
			this.similarity = 1;
		else
			this.similarity = sim;
	}
	
	
	/**
	 * Sets the MappingStatus of the Mapping
	 * @param s: the MappingStatus to set
	 */
	public void setStatus(MappingStatus s)
	{
		this.status = s;
	}
	
	/**
	 * @return the Mapping in RDF form
	 */
	public abstract String toRDF();
}