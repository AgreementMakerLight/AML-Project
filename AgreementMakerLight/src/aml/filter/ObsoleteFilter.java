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
* Filtering algorithm based on obsolete classes.                              *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.filter;

import aml.AML;
import aml.match.Alignment;
import aml.match.Mapping;
import aml.ontology.Ontology2Match;
import aml.settings.MappingStatus;

public class ObsoleteFilter implements Filterer, Flagger
{

//Constructors
	
	public ObsoleteFilter(){}
	
//Public Methods
	
	@Override
	public void filter()
	{
		AML aml = AML.getInstance();
		Ontology2Match source = aml.getSource();
		Ontology2Match target = aml.getTarget();
		Alignment a = aml.getAlignment();
		for(Mapping m : a)
			if((source.isObsoleteClass(m.getSourceId()) ||
					target.isObsoleteClass(m.getTargetId())) &&
					!m.getStatus().equals(MappingStatus.CORRECT))
				m.setStatus(MappingStatus.INCORRECT);
		aml.removeIncorrect();
	}
	
	@Override
	public void flag()
	{
		AML aml = AML.getInstance();
		Ontology2Match source = aml.getSource();
		Ontology2Match target = aml.getTarget();
		Alignment a = aml.getAlignment();
		for(Mapping m : a)
			if((source.isObsoleteClass(m.getSourceId()) ||
					target.isObsoleteClass(m.getTargetId())) &&
					m.getStatus().equals(MappingStatus.UNKNOWN))
				m.setStatus(MappingStatus.FLAGGED);		
	}
}
