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
* The element used to encode a class restriction in the range of an EDOAL     *
* AttributeDomainRestriction, which can be all, class or exists.              *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.alignment.rdf;

public enum RestrictionElement
{
	ALL		("all"),
	CLASS	("class"),
	EXISTS	("exists");
	
	String text;
	
	private RestrictionElement(String t)
	{
		text = t;
	}
	
	public static RestrictionElement parse(String t)
	{
		for(RestrictionElement r : RestrictionElement.values())
			if(r.text.equals(t))
				return r;
		return null;
	}
	
	public String toString()
	{
		return "edoal:" + text;
	}
}

