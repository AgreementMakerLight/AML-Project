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
* Lists the size category of the ontology matching problem.                   *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.settings;

public enum SizeCategory
{
	SMALL,
	MEDIUM,
	LARGE,
	HUGE;
	
	SizeCategory(){}
		
	/**
	 * Computes the size category of the matching problem
	 * based on the number of classes of the input ontologies
	 */
	public static SizeCategory getSizeCategory(int sourceEntities, int targetEntities)
	{
		int max = Math.max(sourceEntities, targetEntities);
		int min = Math.min(sourceEntities, targetEntities);
		if(max > 60000 || min > 40000)
			return HUGE;
		else if(max > 5000)
			return LARGE;
		else if(max > 500)
			return MEDIUM;
		else
			return SMALL;
	}
}