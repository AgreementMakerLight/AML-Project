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
* Emulates the SEALS Oracle from the OAEI Interactive Matching track.         *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 12-08-2014                                                            *
* @version 2.0                                                                *
******************************************************************************/
package aml.util;

import aml.AML;
import aml.AML.MappingRelation;
import aml.match.Alignment;
import aml.ontology.URIMap;


public class Oracle
{

//Attributes	
	
	//The reference alignment to use in this Oracle
	private Alignment reference;

//Constructors
	
	/**
	 * Builds an alignment oracle from the given reference alignment file
	 * @param file: the path to the reference alignment to use in this oracle
	 */
	public Oracle(String file)
	{
		try
		{
			reference = new Alignment(file);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
//Public Methods
	
	/**
	 * Checks if a given mapping exists in the reference alignment
	 * @param uri1: the URI of the first mapped entity
	 * @param uri2: the URI of the second mapped entity
	 * @param r: the Relation between the first and second entities
	 * @return whether the reference alignment contains a mapping between uri1 and uri2
	 * with Relation r or a mapping between uri2 and uri1 with the inverse Relation
	 */
	public boolean check(String uri1, String uri2, MappingRelation r)
	{
		URIMap uris = AML.getInstance().getURIMap();
		int id1 = uris.getIndex(uri1);
		int id2 = uris.getIndex(uri2);
		return (reference.containsMapping(id1, id2) &&
				reference.getRelationship(id1, id2).equals(r)) ||
				(reference.containsMapping(id2, id1) &&
				reference.getRelationship(id1, id2).equals(r.inverse()));
	}
}
