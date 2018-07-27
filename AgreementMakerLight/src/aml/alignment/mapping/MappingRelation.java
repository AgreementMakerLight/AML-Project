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
* Lists the Mapping relationships.                                            *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.alignment.mapping;

public enum MappingRelation
{
	EQUIVALENCE	("=","Equivalence"),
	SUBSUMED_BY	("<","SubsumedBy"),
	SUBSUMES	(">","Subsumes"),
	OVERLAP		("^","Overlap"),
	UNKNOWN		("?","Unknown");
    	
	private String representation;
	private String label;
	
	private MappingRelation(String rep, String l)
	{
		representation = rep;
		label = l;
	}
	
	public String getLabel()
	{
		return label;
	}
    	
	public MappingRelation inverse()
	{
		if(this.equals(SUBSUMES))
			return SUBSUMED_BY;
		else if(this.equals(SUBSUMED_BY))
			return SUBSUMES;
		else
			return this;
	}
    	
	public String toString()
	{
		return representation;
	}
	
	public static MappingRelation parseRelation(String relation)
	{
		if(relation.length() == 1)
		{
			for(MappingRelation rel : MappingRelation.values())
				if(relation.equals(rel.toString()))
					return rel;
		}
		else
		{
			for(MappingRelation rel : MappingRelation.values())
				if(relation.equalsIgnoreCase(rel.getLabel()))
					return rel;
		}
		return UNKNOWN;
	}
}
