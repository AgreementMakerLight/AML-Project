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
* An Ontology Data Property Entity.                                           *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 13-08-2015                                                            *
******************************************************************************/
package aml.ontology;

import java.util.HashSet;
import java.util.Set;

public class DataProperty extends Property
{
	
//Attributes
	
	private Set<String> range;
	
//Constructors

	public DataProperty()
	{
		super();
		range = new HashSet<String>();
	}
	
//Public Methods

	public void addRange(String s)
	{
		range.add(s);
	}
	
	public Set<String> getRange()
	{
		return range;
	}
}