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
* Automatic AgreementMakerLight decision & matching system (as used in OAEI). *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 27-08-2014                                                            *
* @version 2.0                                                                *
******************************************************************************/
package aml.match;

import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import aml.AML;
import aml.filter.CardinalityRepairer;
import aml.filter.InteractiveRepairer;
import aml.filter.InteractiveSelector;
import aml.filter.ObsoleteRepairer;
import aml.filter.RankedCoSelector;
import aml.filter.RankedSelector;
import aml.filter.Repairer;
import aml.settings.LanguageSetting;
import aml.settings.SelectionType;
import aml.settings.SizeCategory;

public class AutomaticMatcher
{
	
//Attributes

	//Link to the AML class
	private AML aml;
	//Settings
	private boolean isInteractive;
	private double thresh;
	private double psmThresh;
	private SizeCategory size;
	private LanguageSetting lang;
	private final String BK_PATH = "store/knowledge/";
	private Alignment a;
	private Alignment lex;
	private Set<Alignment> alignSet;
	
//Constructors	
	
	public AutomaticMatcher()
	{
		aml = AML.getInstance();
		size = aml.getSizeCategory();
		lang = aml.getLanguageSetting();
		thresh = aml.getThreshold();
		if(lang.equals(LanguageSetting.TRANSLATE))
			psmThresh = thresh;
		else
			psmThresh = 0.7;
		isInteractive = aml.isInteractive();
	}
	
//Public Methods

	public Alignment match()
	{
		if(isInteractive)
			alignSet = new HashSet<Alignment>();
		a = new Alignment();
		translate();
		lexicalMatch();
		bkMatch();
		wordMatch();
		stringMatch();
		structuralMatch();
		propertyMatch();
		selection();
		repair();
		return a;
	}
		
//Private Methods

	//Step 0 - Translate
	private void translate()
	{
		if(lang.equals(LanguageSetting.TRANSLATE))
		{
			aml.translateOntologies();
			lang = aml.getLanguageSetting();
		}
	}
	
	//Step 1 - Lexical Match
	private void lexicalMatch()
	{
		LexicalMatcher lm = new LexicalMatcher();
		lex = lm.match(thresh);
		a.addAll(lex);
	}

	//Step 2 - Background Knowledge Match
	private void bkMatch()
	{
		//Only if the task is single-language
		if(!lang.equals(LanguageSetting.SINGLE))
			return;
		
		//We use only WordNet for very small ontologies
		if(size.equals(SizeCategory.SMALL))
		{
			WordNetMatcher wn = new WordNetMatcher();
			Alignment wordNet = wn.match(thresh);
			double gain = wordNet.gainOneToOne(lex);
			double coverage = Math.min(wordNet.sourceCoverage(),wordNet.targetCoverage());
			if(isInteractive)
			{
				alignSet.add(wordNet);
				if(gain >= 0.1 && coverage >= 0.07)
					a.addAllOneToOne(wordNet);
			}
			else if(gain >= 0.15 && coverage >= 0.08)
				a.addAllOneToOne(wordNet);
			return;
		}
		//We test all sources other than WordNet for larger ontologies
		Vector<String> bkSources = new Vector<String>();
		bkSources.addAll(aml.getBKSources());
		bkSources.remove("WordNet");
		for(String bk : bkSources)
		{
			if(bk.endsWith(".lexicon"))
			{
				MediatingMatcher mm = new MediatingMatcher(BK_PATH + bk);
				Alignment med = mm.match(thresh);
				double gain = med.gain(lex);
				if(gain >= 0.02)
					a.addAll(med);
			}
			else
			{
				aml.openBKOntology(bk);
				XRefMatcher xr = new XRefMatcher(aml.getBKOntology());
				Alignment ref = xr.match(thresh);
				double gain = ref.gain(lex);
				if(gain >= 0.25)
				{
					xr.extendLexicons(thresh);
					LexicalMatcher lm = new LexicalMatcher();
					a.addAll(lm.match(thresh));
				}
				else if(gain > 0.02)
					a.addAll(ref);					
			}
		}
	}
	
	//Step 3 - Word Match
	private void wordMatch()
	{
		//Only if the task is not huge
		if(size.equals(SizeCategory.HUGE))
			return;
		
		Alignment word = new Alignment();
		if(lang.equals(LanguageSetting.SINGLE))
		{
			WordMatcher wm = new WordMatcher();
			word.addAll(wm.match(thresh));
		}
		else if(lang.equals(LanguageSetting.MULTI))
		{
			for(String l : aml.getLanguages())
			{
				WordMatcher wm = new WordMatcher(l);
				word.addAll(wm.match(thresh));
			}
		}
		if(isInteractive)
			alignSet.add(word);
		a.addAllOneToOne(word);
	}
	
	//Step 4 - String Match
	private void stringMatch()
	{
		ParametricStringMatcher psm = new ParametricStringMatcher();
		//If the task is small, we can use the PSM in match mode
		if(size.equals(SizeCategory.SMALL))
		{
			a.addAllOneToOne(psm.match(psmThresh));
			//And the MultiWordMatcher as well
			MultiWordMatcher mwm = new MultiWordMatcher();
			a.addAllOneToOne(mwm.match(thresh));
		}
		//Otherwise we use it in extendAlignment mode
		else
			a.addAllOneToOne(psm.extendAlignment(a,thresh));
	}	
	
	//Step 5 - Structural Match
	private void structuralMatch()
	{
		//Only if the size is small or medium
		if(size.equals(SizeCategory.SMALL) || size.equals(SizeCategory.MEDIUM))
		{
			NeighborSimilarityMatcher nsm = new NeighborSimilarityMatcher(false,false);
			if(isInteractive)
				alignSet.add(nsm.rematch(a));
			a.addAllOneToOne(nsm.extendAlignment(a,thresh));
		}		
	}
	
	//Step 6 - Property Match
	private void propertyMatch()
	{
		double sourceRatio = aml.getSource().propertyCount() * 1.0 / aml.getSource().classCount();
		double targetRatio = aml.getTarget().propertyCount() * 1.0 / aml.getTarget().classCount();
		if(sourceRatio < 0.05 && targetRatio < 0.05)
			return;
		PropertyMatcher pm = new PropertyMatcher(true);
		a.addAllOneToOne(pm.extendAlignment(a, thresh));
	}
	
	//Step 7 - Selection
	private void selection()
	{
		if(size.equals(SizeCategory.SMALL))
		{
			RankedSelector rs = new RankedSelector(SelectionType.STRICT);
			a = rs.select(a, thresh);
		}
		else if(size.equals(SizeCategory.MEDIUM))
		{
			RankedSelector rs = new RankedSelector(SelectionType.PERMISSIVE);
			a = rs.select(a, thresh);
		}
		else if(size.equals(SizeCategory.LARGE))
		{
			RankedSelector rs = new RankedSelector(SelectionType.HYBRID);
			a = rs.select(a, thresh);
		}
		else
		{
			ObsoleteRepairer or = new ObsoleteRepairer();
			a = or.repair(a);
				
			HighLevelStructuralRematcher hl = new HighLevelStructuralRematcher();
			Alignment b = hl.rematch(a);
			NeighborSimilarityMatcher nb = new NeighborSimilarityMatcher(false,true);
			Alignment c = nb.rematch(a);
			b = LWC.combine(b, c, 0.75);
			b = LWC.combine(a, b, 0.8);
		
			RankedSelector rs = new RankedSelector(SelectionType.HYBRID);
			b = rs.select(b, thresh-0.05);
				
			RankedCoSelector s = new RankedCoSelector(b, SelectionType.HYBRID);
			a = s.select(a, thresh);
		}
		if(isInteractive)
		{
			alignSet.add(a);
			InteractiveSelector is = new InteractiveSelector(aml.getOracle(),alignSet);
			a = is.select(a, thresh);
		}
	}
	
	//Step 8 - Repair
	private void repair()
	{
		Repairer r;
		if(isInteractive)
			r = new InteractiveRepairer(aml.getOracle());
		else
			r = new CardinalityRepairer();
		a = r.repair(a);
	}
}