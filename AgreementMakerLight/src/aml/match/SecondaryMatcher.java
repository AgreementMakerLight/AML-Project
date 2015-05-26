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
* A matching algorithm that extends a previous Alignment, returning new       *
* Mappings between the source and target ontologies.                          *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 10-09-2014                                                            *
******************************************************************************/
package aml.match;

public interface SecondaryMatcher
{
	/**
	 * Extends the given Alignment between the source and target Ontologies
	 * @param a: the existing alignment
	 * @param thresh: the similarity threshold for the extention
	 * @return the alignment with the new mappings between the Ontologies
	 */
	public Alignment extendAlignment(Alignment a, double thresh);
}
