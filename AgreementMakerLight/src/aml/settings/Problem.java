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
* Lists the Flagging Steps.                                                   *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.settings;

public enum Problem
{
   	OBSOLETION	("Obsolete Classes"),
   	CARDINALITY ("Cardinality Conflicts"),
   	COHERENCE	("Coherence Conflicts"),
   	QUALITY		("Low Quality Mappings");
   	
   	final String value;
    	
   	Problem(String s)
   	{
   		value = s;
   	}
	    	
   	public String toString()
   	{
   		return value;
   	}
   	
	public static Problem parseStep(String step)
	{
		for(Problem s : Problem.values())
			if(step.equalsIgnoreCase(s.toString()))
				return s;
		return null;
	}
}