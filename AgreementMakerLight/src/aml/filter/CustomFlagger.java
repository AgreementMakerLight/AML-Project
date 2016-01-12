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
* Customizable ensemble of problem flagging algorithms.                       *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.filter;

import java.util.Vector;

import aml.AML;
import aml.settings.Problem;

public class CustomFlagger
{
	
//Constructors	
	
	private CustomFlagger(){}
	
//Public Methods

	public static void flag()
	{
		//Get the AML instance and settings
		AML aml = AML.getInstance();
		Vector<Problem> steps = aml.getFlagSteps();
		//Start the matching procedure
		if(steps.contains(Problem.CARDINALITY))
		{
			Selector s = new Selector(0.0);
			s.flag();
		}
		if(steps.contains(Problem.COHERENCE))
		{
			Repairer r = new Repairer();
			r.flag();
		}
		if(steps.contains(Problem.OBSOLETION))
		{
			ObsoleteFilter o = new ObsoleteFilter();
			o.flag();
		}
		if(steps.contains(Problem.QUALITY))
		{
			QualityFlagger q = aml.buildQualityFlagger();
			q.flag();
		}
	}
}