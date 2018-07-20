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
* Filtering algorithm for mappings between properties that checks if their    *
* domains and ranges are compatible.                                          *
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
import aml.util.Similarity;

public class DomainAndRangeFilterer implements Filterer
{

//Attributes
	
	private AML aml;
	private RelationshipMap rm;
	
//Constructors
	
	public DomainAndRangeFilterer()
	{
		aml = AML.getInstance();
		rm = aml.getRelationshipMap();
	}
	
//Public Methods
	
	@Override
	public void filter()
	{
		System.out.println("Running Domain & Range Filter");
		long time = System.currentTimeMillis()/1000;
		Alignment a = aml.getAlignment();
		URIMap uris = aml.getURIMap();
		for(Mapping m : a)
		{
			if(m.getStatus().equals(MappingStatus.CORRECT))
				continue;
			if(uris.getType(m.getSourceId()).equals(EntityType.DATA))
			{
				if(!idsMatch(rm.getDomains(m.getSourceId()),rm.getDomains(m.getTargetId())) || 
						!valuesMatch(rm.getDataRanges(m.getSourceId()),rm.getDataRanges(m.getTargetId())))
					m.setStatus(MappingStatus.INCORRECT);				
			}
			else if(uris.getType(m.getSourceId()).equals(EntityType.OBJECT))
			{
				if(!idsMatch(rm.getDomains(m.getSourceId()),rm.getDomains(m.getTargetId())) || 
						!idsMatch(rm.getObjectRanges(m.getSourceId()),rm.getObjectRanges(m.getTargetId())))
					m.setStatus(MappingStatus.INCORRECT);
			}
		}
		aml.removeIncorrect();
		System.out.println("Finished in " +	(System.currentTimeMillis()/1000-time) + " seconds");
	}
	
	//Checks if two lists of ids match (i.e., have Jaccard similarity above 50%)
	private boolean idsMatch(Set<Integer> sIds, Set<Integer> tIds)
	{
		if(sIds.size() == 0 && tIds.size() == 0)
			return true;
		if(sIds.size() == 0 || tIds.size() == 0)
			return false;
		double matches = 0.0;
		for(Integer i : sIds)
		{
			for(Integer j : tIds)
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
	private boolean idsMatch(int sIndex, int tIndex)
	{
		RelationshipMap rm = aml.getRelationshipMap();

		if(sIndex == tIndex || aml.getAlignment().containsMapping(sIndex, tIndex))
	    	return true;
		
		Set<Integer> sParent= rm.getParents(sIndex);
		if(sParent.size()==1)
		{
			int spId = sParent.iterator().next();
			if(aml.getAlignment().containsMapping(spId, tIndex))
				return true;
		}
		Set<Integer> tParent= rm.getParents(tIndex);
		if(tParent.size()==1)
		{
			int tpId=tParent.iterator().next();
			if(aml.getAlignment().containsMapping(sIndex, tpId))
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
		double sim = Similarity.jaccard(sRange,tRange);
		return (sim > 0.5);
	}	
}