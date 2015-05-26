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
* A matching algorithm that maps the source and target Ontologies globally.   *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 10-09-2014                                                            *
******************************************************************************/
package aml.match;

public interface PrimaryMatcher
{
	/**
	 * Matches the source and target Ontologies, returning an Alignment between them
	 * @param source: the source Ontology
	 * @param target: the target Ontology
	 * @param thresh: the similarity threshold for the alignment
	 * @return the alignment between the source and target ontologies
	 */
	public Alignment match(double thresh);
}
