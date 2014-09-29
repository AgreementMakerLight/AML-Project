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
* Emulates the SEALS Oracle class from the OAEI Interactive Matching track.   *
* To use for testing purposes only.                                           *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 28-08-2014                                                            *
* @version 2.1                                                                *
******************************************************************************/
package aml.util;

import aml.AML;
import aml.match.Alignment;
import aml.ontology.URIMap;
import aml.settings.MappingRelation;


public class Oracle
{

//Attributes	
	
	//The reference alignment to use in this Oracle
	private static Alignment reference;
	private static int trueCount;
	private static int falseCount;

//Constructors
	
	private Oracle(){}
	
//Public Methods

	/**
	 * Checks if a given mapping exists in the reference alignment
	 * @param uri1: the URI of the first mapped entity
	 * @param uri2: the URI of the second mapped entity
	 * @param r: the Relation between the first and second entities
	 * @return whether the reference alignment contains a mapping between uri1 and uri2
	 * with MappingRelation r or a mapping between uri2 and uri1 with the inverse Relation
	 */
	public static boolean check(String uri1, String uri2, MappingRelation r)
	{
		URIMap uris = AML.getInstance().getURIMap();
		int id1 = uris.getIndex(uri1);
		int id2 = uris.getIndex(uri2);
		boolean check = (reference.containsMapping(id1, id2) &&
				reference.getRelationship(id1, id2).equals(r)) ||
				(reference.containsMapping(id2, id1) &&
				reference.getRelationship(id1, id2).equals(r.inverse()));
		if(check)
			trueCount++;
		else
			falseCount++;
		return check;
	}
	
	public static void close()
	{
		reference = null;
	}
	
	public static boolean isInteractive()
	{
		return reference != null;
	}
	
	public static void makeOracle(Alignment ref)
	{
		reference = ref;
		trueCount = 0;
		falseCount = 0;
	}
	
	public static int negativeInteractions()
	{
		return falseCount;
	}
	
	public static int positiveInteractions()
	{
		return trueCount;
	}	
}
