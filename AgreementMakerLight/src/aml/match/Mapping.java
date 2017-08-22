/******************************************************************************
* Copyright 2013-2016 LASIGE                                                  *
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
******************************************************************************/
package aml.match;

import org.apache.commons.lang.StringEscapeUtils;

import aml.AML;
import aml.ontology.Ontology;
import aml.ontology.URIMap;
import aml.settings.MappingRelation;
import aml.settings.MappingStatus;

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
	//The status of the Mapping
	private MappingStatus s;
	
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
		s = MappingStatus.UNKNOWN;
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
		s = MappingStatus.UNKNOWN;
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
		s = MappingStatus.UNKNOWN;
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
		s = m.s;
	}

//Public Methods

	@Override
	/**
	 * Mappings are compared first based on their status, then
	 * based on their similarity. This enables both sorting by
	 * status for the GUI and sorting by similarity during the
	 * matching procedure, as all mappings will have UNKNOWN
	 * status at that stage.
	 */
	public int compareTo(Mapping o)
	{
		if(this.s.equals(o.s))
		{
			double diff = this.similarity - o.similarity;
			if(diff < 0)
				return -1;
			if(diff > 0)
				return 1;
			return 0;
		}
		else return this.s.compareTo(o.s);
	}
	
	/**
	 * Two Mappings are equal if they map the same two terms
	 * irrespective of the similarity or relationship, which
	 * enables finding redundant Mappings
	 */
	public boolean equals(Object o)
	{
		if(!(o instanceof Mapping))
			return false;
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
	 * @return the similarity between the mapped terms in percentage
	 */
	public String getSimilarityPercent()
	{
		return (Math.round(similarity*10000) * 1.0 / 100) + "%";
	}
	
	/**
	 * @return the id of the source term
	 */
	public int getSourceId()
	{
		return sourceId;
	}
	
	/**
	 * @return the URI of the source term
	 */
	public String getSourceURI()
	{
		return AML.getInstance().getURIMap().getURI(sourceId);
	}

	/**
	 * @return the status of this Mapping
	 */
	public MappingStatus getStatus()
	{
		return s;
	}
	
	/**
	 * @return the id of the target term
	 */
	public int getTargetId()
	{
		return targetId;
	}
	
	/**
	 * @return the URI of the target term
	 */
	public String getTargetURI()
	{
		return AML.getInstance().getURIMap().getURI(targetId);
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
		this.s = s;
	}
	
	/**
	 * @return the Mapping in String form, formatted for the AML GUI
	 */
	public String toGUI()
	{
		return AML.getInstance().getSource().getName(sourceId) + " " +
			rel.toString() + " " + AML.getInstance().getTarget().getName(targetId) +
			" (" + getSimilarityPercent() + ") ";
	}
	
	public String toRDF()
	{
		URIMap uris = AML.getInstance().getURIMap();
		String out = "\t<map>\n" +
			"\t\t<Cell>\n" +
			"\t\t\t<entity1 rdf:resource=\""+uris.getURI(sourceId)+"\"/>\n" +
			"\t\t\t<entity2 rdf:resource=\""+uris.getURI(targetId)+"\"/>\n" +
			"\t\t\t<measure rdf:datatype=\"http://www.w3.org/2001/XMLSchema#float\">"+ similarity +"</measure>\n" +
			"\t\t\t<relation>" + StringEscapeUtils.escapeXml(rel.toString()) + "</relation>\n";
		if(!s.equals(MappingStatus.UNKNOWN))
			out += "\t\t\t<status>" + s.toString() + "</status>\n";
		out += "\t\t</Cell>\n" +
			"\t</map>\n";
		return out;
	}
	
	@Override
	public String toString()
	{
		AML aml = AML.getInstance();
		URIMap uris = aml.getURIMap();
		Ontology source = aml.getSource();
		Ontology target = aml.getTarget();
		String out = uris.getURI(sourceId) + "\t" + source.getName(sourceId) +
				"\t" + uris.getURI(targetId) + "\t" + target.getName(targetId) +
				"\t" + similarity + "\t" + rel.toString();
		if(!s.equals(MappingStatus.UNKNOWN))
			out += "\t" + s;
		return out;
	}
}
