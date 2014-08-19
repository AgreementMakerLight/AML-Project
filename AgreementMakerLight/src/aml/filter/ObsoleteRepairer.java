/******************************************************************************
* Copyright 2013-2014 LASIGE                                                  *
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
* Repair algorithm that excludes mappings between obsolete classes from an    *
* Alignment.                                                                  *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 19-08-2014                                                            *
* @version 2.0                                                                *
******************************************************************************/
package aml.filter;

import aml.AML;
import aml.match.Alignment;
import aml.match.Mapping;
import aml.ontology.Ontology;

public class ObsoleteRepairer implements Repairer
{
	@Override
	public Alignment repair(Alignment a)
	{
		Ontology source = AML.getInstance().getSource();
		Ontology target = AML.getInstance().getTarget();
		
		Alignment selected = new Alignment();
		for(Mapping m : a)
			if(!source.isObsoleteClass(m.getSourceId()) &&
					!target.isObsoleteClass(m.getTargetId()))
				selected.add(m);
		return selected;
	}
}
