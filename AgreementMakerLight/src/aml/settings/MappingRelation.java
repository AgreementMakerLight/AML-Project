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
* Lists the Mapping relationships.                                            *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 22-08-2014                                                            *
* @version 2.1                                                                *
******************************************************************************/
package aml.settings;

public enum MappingRelation
{
	EQUIVALENCE	("="),
	SUPERCLASS	(">"),
	SUBCLASS	("<"),
	OVERLAP		("^"),
	UNKNOWN		("?");
    	
	private String representation;
	
	private MappingRelation(String rep)
	{
		representation = rep;
	}
    	
	public MappingRelation inverse()
	{
		if(this.equals(SUBCLASS))
			return SUPERCLASS;
		else if(this.equals(SUPERCLASS))
			return SUBCLASS;
		else
			return this;
	}
    	
	public String toString()
	{
		return representation;
	}
	
	public static MappingRelation parseRelation(String relation)
	{
		for(MappingRelation rel : MappingRelation.values())
			if(relation.equals(rel.toString()))
				return rel;
		return UNKNOWN;
	}
}
