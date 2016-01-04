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
* Lists the WordMatcher strategy options.                                     *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.settings;

public enum WordMatchStrategy
{
	BY_CLASS ("By_Class"),
	BY_NAME ("By_Name"),
	AVERAGE ("Average"),
	MAXIMUM ("Maximum"),
	MINIMUM ("Minimum");
	
	String label;
	
	WordMatchStrategy(String s)
    {
    	label = s;
    }
	
	public static WordMatchStrategy parseStrategy(String strat)
	{
		for(WordMatchStrategy s : WordMatchStrategy.values())
			if(strat.equalsIgnoreCase(s.toString()))
				return s;
		return null;
	}
	
    public String toString()
    {
    	return label;
	}
}