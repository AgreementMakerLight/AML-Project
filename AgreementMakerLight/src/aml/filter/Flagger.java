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
* A flagging algorithm that identifies problem mappings in the Alignment      *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.filter;

import aml.alignment.Alignment;

public interface Flagger
{
	/**
	 * Flags problem mappings in the given Alignment
	 * @param a: the Alignment to flag
	 */
	@SuppressWarnings("rawtypes")
	public void flag(Alignment a);
}