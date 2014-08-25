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
* Lists the Selection Types.                                                  *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 22-08-2014                                                            *
* @version 2.0                                                                *
******************************************************************************/
package aml.settings;

import aml.match.Alignment;

public enum SelectionType
{
   	STRICT ("Strict 1-to-1"),
   	PERMISSIVE ("Permissive 1-to-1"),
   	HYBRID ("Hybrid 1-to-1"),
   	MANY ("N-to-N");
	    	
   	final String value;
    	
   	SelectionType(String s)
   	{
   		value = s;
   	}
	    	
   	public String toString()
   	{
   		return value;
   	}
   	
	public static SelectionType getSelectionType(Alignment a)
	{
		double cardinality = a.cardinality();
		if(cardinality > 1.4)
			return SelectionType.MANY;
		else if(cardinality > 1.1)
			return SelectionType.HYBRID;
		else if(cardinality > 1.02)
			return SelectionType.PERMISSIVE;
		else
			return SelectionType.STRICT;
	}
	    	
	public static SelectionType parseSelector(String selector)
	{
		for(SelectionType s : SelectionType.values())
			if(selector.equals(s.toString()))
				return s;
		return null;
	}
}