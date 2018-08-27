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
* Filtering algorithm that removes/flags mappings involving obsolete classes. *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.filter;

import aml.AML;
import aml.alignment.Alignment;
import aml.alignment.SimpleAlignment;
import aml.alignment.mapping.Mapping;
import aml.alignment.mapping.MappingStatus;
import aml.ontology.Ontology;

public class ObsoleteFilterer implements Filterer, Flagger
{

//Constructors
	
	public ObsoleteFilterer(){}
	
//Public Methods
	
	@Override
	@SuppressWarnings("rawtypes")
	public Alignment filter(Alignment a)
	{
		if(!(a instanceof SimpleAlignment))
		{
			System.out.println("Warning: cannot filter non-simple alignment!");
			return a;
		}
		System.out.println("Running Obsoletion Filter");
		long time = System.currentTimeMillis()/1000;
		AML aml = AML.getInstance();
		Ontology source = aml.getSource();
		Ontology target = aml.getTarget();
		SimpleAlignment in = (SimpleAlignment)a;
		SimpleAlignment out = new SimpleAlignment(in.getSourceOntology(), in.getTargetOntology());
		for(Mapping<String> m : in)
		{
			if(m.getStatus().equals(MappingStatus.CORRECT) ||
					(!source.isObsoleteClass(m.getEntity1()) &&
					!target.isObsoleteClass(m.getEntity2())))
				out.add(m);
		}
		if(out.size() < a.size())
		{
			for(Mapping<String> m : out)
				if(m.getStatus().equals(MappingStatus.FLAGGED))
					m.setStatus(MappingStatus.UNKNOWN);
		}
		System.out.println("Finished in " +	(System.currentTimeMillis()/1000-time) + " seconds");
		return out;
	}
	
	@Override
	@SuppressWarnings("rawtypes")
	public void flag(Alignment a)
	{
		if(!(a instanceof SimpleAlignment))
		{
			System.out.println("Warning: cannot flag non-simple alignment!");
			return;
		}
		System.out.println("Running Obsoletion Flagger");
		long time = System.currentTimeMillis()/1000;
		AML aml = AML.getInstance();
		Ontology source = aml.getSource();
		Ontology target = aml.getTarget();
		SimpleAlignment in = (SimpleAlignment)a;
		for(Mapping<String> m : in)
			if((source.isObsoleteClass(m.getEntity1()) ||
					target.isObsoleteClass(m.getEntity1())) &&
					m.getStatus().equals(MappingStatus.UNKNOWN))
				m.setStatus(MappingStatus.FLAGGED);
		System.out.println("Finished in " +	(System.currentTimeMillis()/1000-time) + " seconds");
	}
}