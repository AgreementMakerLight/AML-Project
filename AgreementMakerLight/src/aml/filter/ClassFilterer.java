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
* Filtering algorithm for mappings between individuals based on the list of   *
* classes to match from the two ontologies.                                   *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.filter;

import java.util.Set;

import aml.AML;
import aml.match.Alignment;
import aml.match.Mapping;
import aml.ontology.RelationshipMap;
import aml.ontology.URIMap;
import aml.settings.EntityType;
import aml.settings.MappingStatus;

public class ClassFilterer implements Filterer
{

//Constructors
	
	public ClassFilterer(){}
	
//Public Methods
	
	@Override
	public void filter()
	{
		System.out.println("Running Class Filter");
		long time = System.currentTimeMillis()/1000;
		AML aml = AML.getInstance();
		Set<Integer> sourcesToMatch = aml.getSourceClassesToMatch();
		Set<Integer> targetsToMatch = aml.getTargetClassesToMatch();
		RelationshipMap rels = aml.getRelationshipMap();
		URIMap uris = aml.getURIMap();
		Alignment a = aml.getAlignment();
		for(Mapping m : a)
		{
			int source = m.getSourceId();
			int target = m.getTargetId();
			if(uris.getType(source).equals(EntityType.INDIVIDUAL) &&
					uris.getType(target).equals(EntityType.INDIVIDUAL) &&
					!m.getStatus().equals(MappingStatus.CORRECT))
			{
				boolean check = false;
				for(int i : sourcesToMatch)
				{
					if(rels.belongsToClass(source, i))
					{
						check = true;
						break;
					}
				}
				if(check)
				{
					check = false;
					for(int i : targetsToMatch)
					{
						if(rels.belongsToClass(target, i))
						{
							check = true;
							break;
						}
					}					
				}
				if(!check)
					m.setStatus(MappingStatus.INCORRECT);
				else if(m.getStatus().equals(MappingStatus.FLAGGED))
					m.setStatus(MappingStatus.UNKNOWN);
			}
		}
		aml.removeIncorrect();
		System.out.println("Finished in " +	(System.currentTimeMillis()/1000-time) + " seconds");
	}
}