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
* A repair algorithm that removes incoherence-causing mappings from an input  *
* Alignment.                                                                  *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 01-08-2014                                                            *
******************************************************************************/
package aml.filter;

import aml.match.Alignment;

public interface Repairer
{
	/**
	 * Repairs a given Alignment to obtain a (more) coherent Alignment
	 * @param a: the Alignment to repair
	 * @return the repaired Alignment
	 */
	public Alignment repair(Alignment a);
}