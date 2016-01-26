/******************************************************************************
* Copyright 2013-2016 LASIGE                                                  *
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
* Customizable ensemble of problem filtering algorithms.                      *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.filter;

import java.util.Vector;

import aml.AML;
import aml.settings.Problem;
import aml.settings.SelectionType;

public class CustomFilterer
{
	
//Constructors	
	
	private CustomFilterer(){}
	
//Public Methods

	public static void filter()
	{
		//Get the AML instance and settings
		AML aml = AML.getInstance();
		Vector<Problem> steps = aml.getFlagSteps();
		if(steps.contains(Problem.OBSOLETION))
		{
			ObsoleteFilter o = new ObsoleteFilter();
			o.filter();
		}
		if(steps.contains(Problem.CARDINALITY))
		{
			Selector s = new Selector(0.0, SelectionType.STRICT);
			s.filter();
		}
		if(steps.contains(Problem.COHERENCE))
		{
			Repairer r = new Repairer();
			r.filter();
		}
	}
}