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
* A simple mapping between two entities of different Ontologies.              *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.alignment;

import org.apache.commons.lang.StringEscapeUtils;

import aml.AML;

public class SimpleMapping implements Mapping
{

//Attributes

	//The id of the source ontology term
	private String entity1;
	//The id of the target ontology term
	private String entity2;
	//The similarity between the terms
	private double similarity;
	//The relationship between the terms
	private MappingRelation rel;
	//The status of the Mapping
	private MappingStatus status;
	
//Constructors

	/**
	 * Creates a mapping between uri1 and uri2
	 * @param uri1: the uri of the source ontology entity
	 * @param uri2: the uri of the target ontology entity
	 */
	public SimpleMapping(String uri1, String uri2)
	{
		this(uri1,uri2,1.0,MappingRelation.EQUIVALENCE,MappingStatus.UNKNOWN);
	}
	
	/**
	 * Creates a mapping between uri1 and uri2 with the given similarity
	 * @param uri1: the uri of the source ontology entity
	 * @param uri2: the uri of the target ontology entity
	 * @param sim: the similarity between the entities
	 */
	public SimpleMapping(String uri1, String uri2, double sim)
	{
		this(uri1,uri2,1.0,MappingRelation.EQUIVALENCE,MappingStatus.UNKNOWN);
	}
	
	/**
	 * Creates a mapping between uri1 and uri2 with the given similarity and relation
	 * @param uri1: the uri of the source ontology entity
	 * @param uri2: the uri of the target ontology entity
	 * @param sim: the similarity between the entities
	 * @param r: the mapping relationship between the entities
	 */
	public SimpleMapping(String uri1, String uri2, double sim, MappingRelation r)
	{
		this(uri1,uri2,1.0,r,MappingStatus.UNKNOWN);
	}
	
	
	/**
	 * Creates a mapping between uri1 and uri2 with the given similarity, relation and status
	 * @param uri1: the uri of the source ontology entity
	 * @param uri2: the uri of the target ontology entity
	 * @param sim: the similarity between the entities
	 * @param r: the mapping relationship between the entities
	 */
	public SimpleMapping(String uri1, String uri2, double sim, MappingRelation r, MappingStatus s)
	{
		entity1 = uri1;
		entity2 = uri2;
		similarity = Math.round(sim*1000)/1000.0;
		rel = r;
		status = s;
	}
	
	/**
	 * Creates a new mapping that is a copy of m
	 * @param m: the mapping to copy
	 */
	public SimpleMapping(SimpleMapping m)
	{
		this(m.entity1,m.entity2,m.similarity,m.rel,m.status);
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
	public int compareTo(Mapping o)
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
	public boolean equals(Object o)
	{
		if(!(o instanceof SimpleMapping))
			return false;
		SimpleMapping m = (SimpleMapping)o;
		return (this.entity1 == m.entity1 && this.entity2 == m.entity2);
	}
	
	/**
	 * @return the uri of the source entity
	 */
	@Override
	public Object getEntity1()
	{
		return entity1;
	}

	/**
	 * @return the uri of the target entity
	 */
	@Override
	public Object getEntity2()
	{
		return entity2;
	}

	/**
	 * @return the mapping relation between the mapped terms
	 */
	@Override
	public MappingRelation getRelationship()
	{
		return rel;
	}
	
	/**
	 * @return the similarity between the mapped terms
	 */
	@Override
	public double getSimilarity()
	{
		return similarity;
	}
	
	/**
	 * @return the similarity between the mapped terms in percentage
	 */
	@Override
	public String getSimilarityPercent()
	{
		return (Math.round(similarity*10000) * 1.0 / 100) + "%";
	}
	
	/**
	 * @return the status of this Mapping
	 */
	@Override
	public MappingStatus getStatus()
	{
		return status;
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
	
	/**
	 * Sets the MappingStatus of this Mapping
	 * @param s: the Mapping status to set
	 */
	public void setStatus(MappingStatus s)
	{
		this.status = s;
	}

	/**
	 * @return the Mapping in RDF form
	 */
	@Override
	public String toRDF()
	{
		String out = "\t<map>\n" +
			"\t\t<Cell>\n" +
			"\t\t\t<entity1 rdf:resource=\""+ entity1 +"\"/>\n" +
			"\t\t\t<entity2 rdf:resource=\""+ entity2 +"\"/>\n" +
			"\t\t\t<measure rdf:datatype=\"http://www.w3.org/2001/XMLSchema#float\">"+ similarity +"</measure>\n" +
			"\t\t\t<relation>" + StringEscapeUtils.escapeXml(rel.toString()) + "</relation>\n";
		out += "\t\t</Cell>\n" +
			"\t</map>\n";
		return out;
	}

	/**
	 * @return the Mapping in String form, as displayed by the AML GUI
	 */
	@Override
	public String toString()
	{
		return AML.getInstance().getSource().getName(entity1) + " " +
			rel.toString() + " " + AML.getInstance().getTarget().getName(entity2) +
			" (" + getSimilarityPercent() + ") ";
	}
	
	/**
	 * @return the Mapping in TSV form
	 */
	@Override
	public String toTSV()
	{
		AML aml = AML.getInstance();
		String out = entity1 + "\t" + aml.getSource().getName(entity1) +
				"\t" + entity2 + "\t" + aml.getTarget().getName(entity2) +
				"\t" + similarity + "\t" + rel.toString();
		if(!status.equals(MappingStatus.UNKNOWN))
			out += "\t" + status;
		return out;
	}
}
