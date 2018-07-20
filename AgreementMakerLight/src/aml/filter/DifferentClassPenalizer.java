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
* A filtering algorithm based on cardinality.                                 *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/

package aml.filter;

import aml.AML;
import aml.match.Alignment;
import aml.match.Mapping;

public class DifferentClassPenalizer
{

//Constructors
	
	private DifferentClassPenalizer(){}
	
	
//Public Methods
	
	public static void penalize()
	{
		AML aml = AML.getInstance();
		Alignment a = aml.getAlignment();
		for(Mapping m : a)
		{
			if(!aml.getRelationshipMap().shareClass(m.getSourceId(),m.getTargetId()))
				m.setSimilarity(m.getSimilarity() * 0.9);
		}
	}
}