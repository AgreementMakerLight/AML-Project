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
* Customizable ensemble of problem filtering / flagging algorithms.           *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.filter;

import java.util.Vector;

import aml.AML;
import aml.alignment.Alignment;
import aml.alignment.SimpleAlignment;
import aml.settings.Problem;

public class Ensemble
{
	
//Constructors	
	
	private Ensemble(){}
	
//Public Methods

	@SuppressWarnings("rawtypes")
	public static void flag(Alignment a)
	{
		if(!(a instanceof SimpleAlignment))
		{
			System.out.println("Warning: cannot flag non-simple alignment!");
			return;
		}
		Vector<Problem> steps = AML.getInstance().getFlagSteps();
		//Start the matching procedure
		if(steps.contains(Problem.CARDINALITY))
		{
			Selector s = new Selector(0.0);
			s.flag(a);
		}
		if(steps.contains(Problem.COHERENCE))
		{
			Repairer r = new Repairer();
			r.flag(a);
		}
		if(steps.contains(Problem.OBSOLETION))
		{
			ObsoleteFilterer o = new ObsoleteFilterer();
			o.flag(a);
		}
		if(steps.contains(Problem.QUALITY))
		{
			QualityFlagger q = new QualityFlagger();
			q.flag(a);
		}
	}
	
	@SuppressWarnings("rawtypes")
	public static Alignment filter(Alignment a)
	{
		if(!(a instanceof SimpleAlignment))
		{
			System.out.println("Warning: cannot filter non-simple alignment!");
			return a;
		}
		Vector<Problem> steps = AML.getInstance().getFlagSteps();
		SimpleAlignment b = (SimpleAlignment)a;
		if(steps.contains(Problem.OBSOLETION))
		{
			ObsoleteFilterer o = new ObsoleteFilterer();
			b = (SimpleAlignment)o.filter(a);
		}
		if(steps.contains(Problem.CARDINALITY))
		{
			Selector s = new Selector(0.0, SelectionType.STRICT);
			b = (SimpleAlignment)s.filter(a);
		}
		if(steps.contains(Problem.COHERENCE))
		{
			Repairer r = new Repairer();
			b = (SimpleAlignment)r.filter(a);
		}
		return b;
	}
}