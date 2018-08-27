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
* A filtering algorithm that excludes mappings that are superseded by more    *
* specific mappings, with the same source or the same target class, and a     *
* subclass of the target or of the source class respectively.                 *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.filter;

import aml.AML;
import aml.alignment.Alignment;
import aml.alignment.SimpleAlignment;
import aml.alignment.mapping.Mapping;
import aml.alignment.mapping.MappingStatus;
import aml.ontology.EntityType;
import aml.ontology.semantics.EntityMap;

public class ParentMappingFilterer implements Filterer
{
	
//Constructors
	
	/**
	 * Constructs a ParentSelector
	 */
	public ParentMappingFilterer(){}
	

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
		System.out.println("Performing Selection");

		long time = System.currentTimeMillis()/1000;
		SimpleAlignment in = (SimpleAlignment)a;
		EntityMap r = AML.getInstance().getEntityMap();
		SimpleAlignment out = new SimpleAlignment();
		for(Mapping<String> m : in)
		{
			String src = m.getEntity1();
			String tgt = m.getEntity2();
			if(!r.getTypes(src).contains(EntityType.CLASS) && !r.getTypes(tgt).contains(EntityType.CLASS))
				continue;
			boolean add = true;
			for(Mapping<String> n : in.getSourceMappings(src))
			{
				String t = n.getEntity2();
				if(r.isSubclass(t,tgt) &&
						in.getSimilarity(src, t) >= in.getSimilarity(src, tgt))
				{
					add = false;
					break;
				}
			}
			if(!add)
				continue;
			for(Mapping<String> n : in.getTargetMappings(tgt))
			{
				String s = n.getEntity1();
				if(r.isSubclass(s,src) &&
						in.getSimilarity(s, tgt) >= in.getSimilarity(src, tgt))
				{
					add = false;
					break;
				}
			}
			if(add)
				out.add(m);
		}
		if(out.size() < in.size())
		{
			for(Mapping<String> m : out)
				if(m.getStatus().equals(MappingStatus.FLAGGED))
					m.setStatus(MappingStatus.UNKNOWN);
		}
		System.out.println("Finished in " +	(System.currentTimeMillis()/1000-time) + " seconds");
		return out;
	}
}