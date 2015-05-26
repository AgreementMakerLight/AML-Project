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
* A basic Ontology entity, represented by its local name.                     *
* Classes and Annotation Properties can use this class directly; Data and     *
* Object Properties should use the corresponding subclasses.                  *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 21-05-2015                                                            *
******************************************************************************/
package aml.ontology;

public class Entity
{
	
//Attributes
	
	private String name;
	
//Constructors

	public Entity(String n)
	{
		name = n;
	}
	
//Public Methods

	public String getName()
	{
		return name;
	}
}