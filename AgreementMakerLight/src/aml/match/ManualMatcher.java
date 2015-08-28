/******************************************************************************
* Copyright 2013-2015 LASIGE                                                  *
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
* Customizable ensemble of matching, selection & repair algorithms.           *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 12-09-2014                                                            *
******************************************************************************/
package aml.match;

import java.util.Vector;

import aml.AML;
import aml.filter.SemanticRepairer;
import aml.filter.ObsoleteRepairer;
import aml.filter.RankedCoSelector;
import aml.filter.RankedSelector;
import aml.filter.Repairer;
import aml.settings.LanguageSetting;
import aml.settings.MatchStep;
import aml.settings.NeighborSimilarityStrategy;
import aml.settings.SelectionType;
import aml.settings.WordMatchStrategy;

public class ManualMatcher
{
	
//Constructors	
	
	private ManualMatcher(){}
	
//Public Methods

	public static Alignment match()
	{
		//Get the AML instance and settings
		AML aml = AML.getInstance();
		Vector<MatchStep> steps = aml.getSelectedSteps();
		double thresh = aml.getThreshold();
		
		//Initialize the alignment
		Alignment a = new Alignment();
		
		//Start the matching procedure
		if(steps.contains(MatchStep.TRANSLATE))
			aml.translateOntologies();
		if(steps.contains(MatchStep.BK))
		{
			BackgroundKnowledgeMatcher bk = new BackgroundKnowledgeMatcher();
			a = bk.match(thresh);
		}
		else
		{
			LexicalMatcher lm = new LexicalMatcher();
			a = lm.match(thresh);
		}
		if(steps.contains(MatchStep.WORD))
		{
			WordMatchStrategy wms = aml.getWordMatchStrategy();
			if(aml.getLanguageSetting().equals(LanguageSetting.SINGLE))
			{
				WordMatcher wm = new WordMatcher(wms);
				a.addAllOneToOne(wm.match(thresh));
			}
			else
			{
				Alignment word = new Alignment();
				for(String l : aml.getLanguages())
				{
					WordMatcher wm = new WordMatcher(l,wms);
					word.addAll(wm.match(thresh));
				}
				a.addAllOneToOne(word);
			}
		}
		if(steps.contains(MatchStep.STRING))
		{
			StringMatcher sm = new StringMatcher(aml.getStringSimMeasure());
			if(aml.primaryStringMatcher())
				a.addAllOneToOne(sm.match(thresh));
			else
				a.addAllOneToOne(sm.extendAlignment(a, thresh));
		}
		if(steps.contains(MatchStep.STRUCT))
		{
			NeighborSimilarityMatcher nsm = new NeighborSimilarityMatcher(
					aml.getNeighborSimilarityStrategy(),aml.directNeighbors());
			a.addAllOneToOne(nsm.extendAlignment(a,thresh));
		}
		if(steps.contains(MatchStep.PROPERTY))
		{
			PropertyMatcher pm = new PropertyMatcher(true);
			a.addAllOneToOne(pm.extendAlignment(a, thresh));
		}
		if(steps.contains(MatchStep.SELECT))
		{
			if(aml.removeObsolete())
			{
				ObsoleteRepairer or = new ObsoleteRepairer();
				a = or.repair(a);
			}
			SelectionType sType = aml.getSelectionType();
			if(aml.structuralSelection())
			{
				BlockRematcher br = new BlockRematcher();
				Alignment b = br.rematch(a);
				NeighborSimilarityMatcher nb = new NeighborSimilarityMatcher(
						NeighborSimilarityStrategy.MAXIMUM,true);
				Alignment c = nb.rematch(a);
				b = LWC.combine(b, c, 0.75);
				b = LWC.combine(a, b, 0.8);
			
				RankedSelector rs = new RankedSelector(sType);
				b = rs.select(b, thresh-0.05);
					
				RankedCoSelector s = new RankedCoSelector(b, sType);
				a = s.select(a, thresh);
				
			}
			else
			{
				RankedSelector rs = new RankedSelector(sType);
				a = rs.select(a, thresh);
			}
		}
		if(steps.contains(MatchStep.REPAIR))
		{
			Repairer r = new SemanticRepairer();
			a = r.repair(a);
		}
		return a;
	}
}