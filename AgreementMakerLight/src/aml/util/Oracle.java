/******************************************************************************
* Copyright 2013-2015 LASIGE                                                  *
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
	private static double error;
	private static Table2Map<Integer,Integer,MappingRelation> positive;
	private static Table2Map<Integer,Integer,MappingRelation> negative;

//Constructors
	
	private Oracle(){}
	
//Public Methods

	/**
	 * Checks if a given mapping exists in the reference alignment
	 * @param uri1: the URI of the first mapped entity
	 * @param uri2: the URI of the second mapped entity
	 * @param rel: the Relation between the first and second entities
	 * @return whether the reference alignment contains a mapping between uri1 and uri2
	 * with relation rel or a mapping between uri2 and uri1 with the inverse relation
	 */
	public static boolean check(String uri1, String uri2, String rel)
	{
		MappingRelation r = MappingRelation.parseRelation(rel);
		URIMap uris = AML.getInstance().getURIMap();
		int id1 = uris.getIndex(uri1);
		int id2 = uris.getIndex(uri2);
		//If the query was already done, return the result
		if(positive.contains(id1, id2, r))
			return true;
		if(negative.contains(id1, id2, r))
			return false;
		//Otherwise, if the mapping between uri1 and uri2 is 'unknown' in the
		//reference alignment return true by default, but do not store it or
		//count it as a query (it will also not count in the evaluation)
		if((reference.contains(id1, id2, MappingRelation.UNKNOWN)))
			return true;
		//Check if the query is present in the reference alignment
		boolean classification = reference.contains(id1, id2, r);
		//Reverse the classification with probability given by the error
		if(Math.random() < error)
			classification = !classification;
		//Store the request
		if(classification)
			positive.add(id1, id2, r);
		else
			negative.add(id1, id2, r);
		return classification;
	}
	
	public static void close()
	{
		reference = null;
		positive = null;
		negative = null;
	}
	
	public static boolean isInteractive()
	{
		return reference != null;
	}
	
	public static void makeOracle(Alignment ref, double err)
	{
		reference = ref;
		error = err;
		positive = new Table2Map<Integer,Integer,MappingRelation>();
		negative = new Table2Map<Integer,Integer,MappingRelation>();
	}
	
	public static int negativeInteractions()
	{
		return negative.size();
	}
	
	public static int positiveInteractions()
	{
		return positive.size();
	}	
}
