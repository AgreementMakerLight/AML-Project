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
* Abstract Matcher with EntityType support checking methods.                  *
*                                                                             *
* @authors Daniel Faria                                                       *
******************************************************************************/
package aml.match;

import aml.ontology.EntityType;

public abstract class AbstractMatcher
{

//Attributes

	//The description of this matcher
	protected static final String DESCRIPTION = "";
	//The name of this matcher
	protected static final String NAME = "Abstract Matcher";
	//The support (empty since this cannot actually generate alignments)
	protected static final EntityType[] SUPPORT = {};
	
	
//Public Methods
	
	/**
	 * @return this Matcher's textual description
	 */
	public String getDescription()
	{
		return DESCRIPTION;
	}

	/**
	 * @return this Matcher's name
	 */
	public String getName()
	{
		return NAME;
	}
	
	/**
	 * @return the list of EntityTypes supported by this Matcher
	 */
	public EntityType[] getSupportedEntityTypes()
	{
		return SUPPORT;
	}
	
	
//Protected Methods
	
	protected boolean checkEntityType(EntityType e)
	{
		boolean check = false;
		for(EntityType t : SUPPORT)
		{
			if(t.equals(e))
			{
				check = true;
				break;
			}
		}
		return check;
	}
}