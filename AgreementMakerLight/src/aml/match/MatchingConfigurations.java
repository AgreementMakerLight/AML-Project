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
* Enumerates matching algorithms and selection types.                         *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 03-02-2014                                                            *
******************************************************************************/
package aml.match;

public class MatchingConfigurations
{
	
//Enumerations
	
	/**
	 * Lists the Matching Algorithms
	 */
	public enum MatchingAlgorithm
	{
	    AML ("AML Matcher"),
	    OAEI ("OAEI2013 Matcher"),
	    LEXICAL ("Lexical Matcher");
	    
	    String label;
	    
	    MatchingAlgorithm(String s)
	    {
	    	label = s;
	    }
	    
	    public String toString()
	    {
	    	return label;
	    }
	}
	
	/**
	 * Lists the Selection Types
	 */
    public enum SelectionType
    {
    	AUTO ("Auto Detect"),
    	STRICT ("Strict 1-to-1"),
    	PERMISSIVE ("Permissive 1-to-1"),
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
    }
}