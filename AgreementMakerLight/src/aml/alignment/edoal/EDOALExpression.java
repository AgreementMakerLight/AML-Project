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
* An EDOAL Expression.                                                        *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.alignment.edoal;

import java.util.HashSet;
import java.util.Set;

public abstract class EDOALExpression
{
	
//Attributes
	
	//To enable efficient hashing, an EDOAL Expression 
	//must list all elements that compose it
	private HashSet<String> elements;
	
	/**
	 * @return the ontology entities listed in this EDOAL expression
	 */
	public Set<String> getElements()
	{
		return elements;
	}
	
	@Override
	public int hashCode()
	{
		return elements.hashCode();
	}
}