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
* Lists the EDOAL Comparators.                                                *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.alignment.edoal;

public enum EDOALComparator
{
//Values
	
	EQUALS("edoal:equals"),
	LOWER("edoal:lower-than"),
	GREATER("edoal:greater-than");
	
//Attributes
	
	public final String uri;
	
//Constructors
	
	private EDOALComparator(String u)
	{
		uri = u;
	}
	
	/**
	 * Parses a comparator from a string source (such as an rdf document)
	 * @param u: the string value to parse
	 * @return the EDOALComparator corresponding to u
	 */
	public static EDOALComparator parseComparator(String u)
	{
		for(EDOALComparator e : EDOALComparator.values())
			if(e.uri.equals(u) || e.uri.endsWith(":" + u))
				return e;
		return null;
	}
}
