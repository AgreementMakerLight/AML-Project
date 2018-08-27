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
* Filtering algorithm for mappings between properties that checks if their    *
* domains and ranges are compatible.                                          *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.filter;

import java.util.Set;

import aml.AML;
import aml.alignment.Alignment;
import aml.alignment.SimpleAlignment;
import aml.alignment.mapping.Mapping;
import aml.alignment.mapping.MappingStatus;
import aml.ontology.EntityType;
import aml.ontology.semantics.EntityMap;
import aml.util.similarity.Similarity;

public class DomainAndRangeFilterer implements Filterer
{

//Attributes
	
	private AML aml;
	private EntityMap rm;
	private SimpleAlignment in;
	
//Constructors
	
	public DomainAndRangeFilterer()
	{
		aml = AML.getInstance();
		rm = aml.getEntityMap();
	}
	
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
		System.out.println("Running Domain & Range Filter");
		long time = System.currentTimeMillis()/1000;
		in = (SimpleAlignment)a;
		SimpleAlignment out = new SimpleAlignment();
		for(Mapping<String> m : in)
		{
			if(m.getStatus().equals(MappingStatus.CORRECT))
				continue;
			if(rm.getTypes(m.getEntity1()).contains(EntityType.DATA_PROP))
			{
				if(idsMatch(rm.getDomains(m.getEntity1()),rm.getDomains(m.getEntity2())) && 
						valuesMatch(rm.getRanges(m.getEntity1()),rm.getRanges(m.getEntity2())))
					out.add(m);				
			}
			else if(rm.getTypes(m.getEntity1()).contains(EntityType.OBJECT_PROP))
			{
				if(idsMatch(rm.getDomains(m.getEntity1()),rm.getDomains(m.getEntity2())) || 
						idsMatch(rm.getRanges(m.getEntity1()),rm.getRanges(m.getEntity2())))
					out.add(m);
			}
		}
		System.out.println("Finished in " +	(System.currentTimeMillis()/1000-time) + " seconds");
		return out;
	}
	
//Private Methods
	
	//Checks if two lists of ids match (i.e., have Jaccard similarity above 50%)
	private boolean idsMatch(Set<String> sIds, Set<String> tIds)
	{
		if(sIds.size() == 0 && tIds.size() == 0)
			return true;
		if(sIds.size() == 0 || tIds.size() == 0)
			return false;
		double matches = 0.0;
		for(String i : sIds)
		{
			for(String j : tIds)
			{
				if(idsMatch(i,j))
				{
					matches++;
					break;
				}
			}
		}
		matches /= sIds.size()+tIds.size()-matches;
		return (matches > 0.5);
	}
	
	//Checks if two ids match (i.e., are either equal, aligned
	//or one is aligned to the parent of the other)
	private boolean idsMatch(String sIndex, String tIndex)
	{
		if(sIndex == tIndex || in.contains(sIndex, tIndex))
	    	return true;
		
		Set<String> sParent = rm.getSuperclasses(sIndex);
		if(sParent.size() == 1)
		{
			String spId = sParent.iterator().next();
			if(in.contains(spId, tIndex))
				return true;
		}
		Set<String> tParent= rm.getSuperclasses(tIndex);
		if(tParent.size() == 1)
		{
			String tpId = tParent.iterator().next();
			if(in.contains(sIndex, tpId))
				return true;
		}
		return false;
	}
	
	//Checks if two lists of values match (i.e., have Jaccard similarity above 50%)
	private boolean valuesMatch(Set<String> sRange, Set<String> tRange)
	{
		if(sRange.size() == 0 && tRange.size() == 0)
			return true;
		if(sRange.size() == 0 || tRange.size() == 0)
			return false;
		double sim = Similarity.jaccardSimilarity(sRange,tRange);
		return (sim > 0.5);
	}	
}