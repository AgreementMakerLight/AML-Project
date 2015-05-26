/******************************************************************************
* Copyright 2013-2015 LASIGE                                                  *
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
* Metrics for measuring similarity between collections and/or lists.          *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 14-07-2014                                                            *
******************************************************************************/
package aml.util;

import java.util.Collection;

public class Similarity
{

	/**
	 * Computes the Jaccard similarity between two Collections of Objects
	 * @param <X>
	 * @param c1: the first Collection 
	 * @param c2: the second Collection
	 * @return the Jaccard similarity between c1 and c2
	 */
	public static <X extends Object> double jaccard(Collection<X> c1, Collection<X> c2)
	{
		if(c1.size() == 0 || c2.size() == 0)
			return 0.0;
		double intersection = 0.0;
		double union = 0.0;
		for(Object o : c1)
		{
			if(c2.contains(o))
				intersection++;
			else
				union++;
		}
		union += c2.size();
		return intersection/union;
	}
}
