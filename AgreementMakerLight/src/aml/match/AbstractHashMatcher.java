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
* Abstract Matcher with parallel execution of match and rematch methods.      *
*                                                                             *
* @authors Daniel Faria                                                       *
******************************************************************************/
package aml.match;

import aml.alignment.SimpleAlignment;
import aml.match.PrimaryMatcher;
import aml.ontology.EntityType;
import aml.ontology.Ontology;

public abstract class AbstractHashMatcher extends Matcher implements PrimaryMatcher
{

//Constructors
	
	/**
	 * Constructs a new AbstractParallelMatcher
	 */
	public AbstractHashMatcher(){}

//Public Methods
	
	@Override
	public SimpleAlignment match(Ontology o1, Ontology o2, EntityType e, double thresh)
	{
		SimpleAlignment a = new SimpleAlignment(o1,o2);
		if(!checkEntityType(e))
			return a;
		System.out.println("Running " + NAME + " in match mode");
		long time = System.currentTimeMillis()/1000;
		a.addAll(hashMatch(o1,o2,e,thresh));
		time = System.currentTimeMillis()/1000 - time;
		System.out.println("Finished in " + time + " seconds");
		return a;
	}
		
//Protected Methods
	
	protected abstract SimpleAlignment hashMatch(Ontology o1, Ontology o2, EntityType e, double thresh);	
}