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
* Matching algorithm for individuals, that compares their entries in the      *
* Lexicon and ValueMap.                                                       *
*                                                                             *
* @author Daniel Faria, Catia Pesquita                                        *
******************************************************************************/
package aml.match;

import java.util.Set;

import aml.settings.EntityType;
import aml.util.ISub;

public class InstanceMatcher extends AbstractInstanceMatcher
{

	public InstanceMatcher()
	{
		this(true);
	}

	public InstanceMatcher(boolean useWordNet)
	{
		super(useWordNet);
	}

	@Override
	public Alignment match(EntityType e, double thresh) throws UnsupportedEntityTypeException
	{
		if(!e.equals(EntityType.INDIVIDUAL))
			throw new UnsupportedEntityTypeException(e.toString());
		Alignment a = new Alignment();
		for(Integer i : sourceInd)
		{
			Set<Integer> sourceClasses = rels.getIndividualClasses(i);
			for(Integer j : targetInd)
			{
				Set<Integer> targetClasses = rels.getIndividualClasses(j);
				//TODO: Make this verification contingent on a matching option
				//as not all instance matching problems enforce class correspondence
				boolean check = false;
				for(Integer sc : sourceClasses)
				{
					if(targetClasses.contains(sc))
					{
						check = true;
						break;
					}
				}
				if(!check)
					continue;
				double nameSim = nameSimilarity(i, j, useWordNet);
				double dataSim = dataSim(i, j);
				double crossPropSim = crossTableSim(i, j, useWordNet);
				double finalSim = Math.max(nameSim,Math.max(dataSim,crossPropSim));
				if(finalSim >= thresh)
					a.add(i,j,finalSim);
			}
		}
		return a;
	}

	//Compares the data and annotation values of the individuals
	private double dataSim(Integer i, Integer j)
	{		
		double dataSim = 0;
		for(Integer sd : sValues.getProperties(i))
			for(String sv : sValues.getValues(i,sd))
				for(Integer td : tValues.getProperties(j))
					for(String tv : tValues.getValues(j,td))
						//Compare the values using ISub
						dataSim = Math.max(dataSim,ISub.stringSimilarity(sv,tv) *
								//If the properties are different, penalize the score
								(td==sd ? 1.0 : 0.9));
		return dataSim;
	}

	//Compares the data and annotation values of an individual against
	//the lexical entries of the other and vice-versa
	private double crossTableSim(Integer i, Integer j, boolean useWordNet)
	{		
		double crossSim = 0;
		for(String n1 : sLex.getNames(i))
			for(Integer td : tValues.getProperties(j))
				for(String tv : tValues.getValues(j,td))
					crossSim = Math.max(crossSim,nameSimilarity(n1,tv,useWordNet));
		for(String n2 : tLex.getNames(j))
			for(Integer sd : sValues.getProperties(i))
				for(String sv : sValues.getValues(i,sd))
					crossSim = Math.max(crossSim,nameSimilarity(n2,sv,useWordNet));
		return crossSim;
	}
}