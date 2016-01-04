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
* Lists the Selection Types.                                                  *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.settings;

import aml.AML;

public enum SelectionType
{
   	STRICT ("Strict"),
   	PERMISSIVE ("Permissive"),
   	HYBRID ("Hybrid");
	    	
   	final String value;
    	
   	SelectionType(String s)
   	{
   		value = s;
   	}
	    	
   	public String toString()
   	{
   		return value;
   	}
   	
	public static SelectionType getSelectionType()
	{
		SizeCategory size = AML.getInstance().getSizeCategory();
		if(size.equals(SizeCategory.SMALL))
			return SelectionType.STRICT;
		else if(size.equals(SizeCategory.MEDIUM))
			return SelectionType.PERMISSIVE;
		else
			return SelectionType.HYBRID;
	}
	    	
	public static SelectionType parseSelector(String selector)
	{
		for(SelectionType s : SelectionType.values())
			if(selector.equalsIgnoreCase(s.toString()))
				return s;
		return null;
	}
}