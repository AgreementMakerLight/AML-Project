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
* Lists the Match Steps.                                                      *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.settings;

public enum MatchStep
{
   	TRANSLATE	("Translator"),
   	LEXICAL		("Lexical Matcher"),
   	BK			("Background Knowledge Matcher"),
   	WORD		("Word Matcher"),
   	STRING		("String Matcher"),
   	STRUCT		("Structural Matcher"),
   	PROPERTY	("Property Matcher"),
   	OBSOLETE	("Obsolete Filter"),
   	SELECT		("Cardinality Filter"),
   	REPAIR		("Coherence Filter");
	    	
   	final String value;
    	
   	MatchStep(String s)
   	{
   		value = s;
   	}
	    	
   	public String toString()
   	{
   		return value;
   	}
   	
	public static MatchStep parseStep(String step)
	{
		for(MatchStep s : MatchStep.values())
			if(step.equalsIgnoreCase(s.toString()))
				return s;
		return null;
	}
}