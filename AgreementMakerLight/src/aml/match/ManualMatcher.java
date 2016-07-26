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
* Customizable ensemble of matching, selection & repair algorithms.           *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.match;

import java.util.Vector;

import aml.AML;
import aml.filter.ObsoleteFilterer;
import aml.filter.Repairer;
import aml.filter.Selector;
import aml.settings.EntityType;
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

	public static void match() throws UnsupportedEntityTypeException
	{
		//Get the AML instance and settings
		AML aml = AML.getInstance();
		Vector<MatchStep> steps = aml.getMatchSteps();
		double thresh = aml.getThreshold();
		boolean hierarchic = aml.isHierarchic();
		
		//Initialize the alignment
		Alignment a = new Alignment();
		Alignment aux;
		
		//Start the matching procedure
		if(steps.contains(MatchStep.TRANSLATE))
			aml.translateOntologies();
		if(steps.contains(MatchStep.BK))
		{
			BackgroundKnowledgeMatcher bk = new BackgroundKnowledgeMatcher();
			a = bk.match(EntityType.CLASS,thresh);
		}
		else
		{
			LexicalMatcher lm = new LexicalMatcher();
			a = lm.match(EntityType.CLASS,thresh);
		}
		if(steps.contains(MatchStep.WORD))
		{
			WordMatchStrategy wms = aml.getWordMatchStrategy();
			if(aml.getLanguageSetting().equals(LanguageSetting.SINGLE))
			{
				WordMatcher wm = new WordMatcher(wms);
				aux = wm.match(EntityType.CLASS,thresh);
			}
			else
			{
				aux = new Alignment();
				for(String l : aml.getLanguages())
				{
					WordMatcher wm = new WordMatcher(l,wms);
					aux.addAll(wm.match(EntityType.CLASS,thresh));
				}
			}
			if(hierarchic)
				a.addAllOneToOne(aux);
			else
				a.addAll(aux);
		}
		if(steps.contains(MatchStep.STRING))
		{
			StringMatcher sm = new StringMatcher(aml.getStringSimMeasure());
			if(aml.primaryStringMatcher())
				aux = sm.match(EntityType.CLASS,thresh);
			else
				aux = sm.extendAlignment(a,EntityType.CLASS,thresh);
			if(hierarchic)
				a.addAllOneToOne(aux);
			else
				a.addAll(aux);
		}
		if(steps.contains(MatchStep.STRUCT))
		{
			NeighborSimilarityMatcher nsm = new NeighborSimilarityMatcher(
					aml.getNeighborSimilarityStrategy(),aml.directNeighbors());
			aux = nsm.extendAlignment(a,EntityType.CLASS,thresh);
			if(hierarchic)
				a.addAllOneToOne(aux);
			else
				a.addAll(aux);
		}
		if(steps.contains(MatchStep.PROPERTY))
		{
			PropertyMatcher pm = new PropertyMatcher(true);
			aux = pm.extendAlignment(a,EntityType.DATA,thresh);
			aux.addAll(pm.extendAlignment(a,EntityType.OBJECT,thresh));
			if(hierarchic)
				a.addAllOneToOne(aux);
			else
				a.addAll(aux);
		}
		aml.setAlignment(a);
		if(steps.contains(MatchStep.OBSOLETE))
		{
			ObsoleteFilterer or = new ObsoleteFilterer();
			or.filter();
		}
		if(steps.contains(MatchStep.SELECT))
		{
			SelectionType sType = aml.getSelectionType();
			if(aml.structuralSelection())
			{
				BlockRematcher br = new BlockRematcher();
				Alignment b = br.rematch(a,EntityType.CLASS);
				NeighborSimilarityMatcher nb = new NeighborSimilarityMatcher(
						NeighborSimilarityStrategy.MAXIMUM,true);
				Alignment c = nb.rematch(a,EntityType.CLASS);
				b = LWC.combine(b, c, 0.75);
				b = LWC.combine(a, b, 0.8);
				Selector s = new Selector(thresh-0.05,sType);
				b = s.filter(b);
				s = new Selector(thresh, sType, b);
				s.filter();
				
			}
			else
			{
				Selector s = new Selector(thresh, sType);
				s.filter();
			}
		}
		if(steps.contains(MatchStep.REPAIR))
		{
			Repairer r = new Repairer();
			r.filter();
		}
	}
}