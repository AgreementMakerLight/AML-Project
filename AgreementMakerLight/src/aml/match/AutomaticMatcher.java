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
* @version 2.1                                                                *
******************************************************************************/
package aml.match;

import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import aml.AML;
import aml.filter.CardinalityRepairer;
import aml.filter.InteractiveRepairer;
import aml.filter.InteractiveSelector;
import aml.filter.ObsoleteRepairer;
import aml.filter.RankedCoSelector;
import aml.filter.RankedSelector;
import aml.filter.Repairer;
import aml.settings.LanguageSetting;
import aml.settings.MatchStep;
import aml.settings.NeighborSimilarityStrategy;
import aml.settings.SelectionType;
import aml.settings.SizeCategory;
import aml.util.Oracle;

public class AutomaticMatcher
{
	
//Attributes

	//Link to the AML class
	private static AML aml;
	//Settings
	private static boolean isInteractive;
	private static SizeCategory size;
	private static LanguageSetting lang;
	private static SelectionType sType;
	//BackgroundKnowledge path
	private static final String BK_PATH = "store/knowledge/";
	//Thresholds
	private static double thresh;
	private static double psmThresh;
	private static double wnThresh;
	private static final double BASE_THRESH = 0.6;
	private static final double HIGH_GAIN_THRESH = 0.25;
	private static final double MIN_GAIN_THRESH = 0.02;
	//And their modifiers
	private static final double INTER_MOD = -0.3;
	private static final double MULTI_MOD = 0.05;
	private static final double TRANS_MOD = -0.15;
	private static final double SIZE_MOD = 0.1;
	//Alignments
	private static Alignment a;
	private static Alignment lex;
	private static Set<Alignment> alignSet;
	
//Constructors	
	
	private AutomaticMatcher(){}
	
//Public Methods

	public static Alignment match()
	{
		//Get the AML instance
		aml = AML.getInstance();
		//And the size and language configuration
		size = aml.getSizeCategory();
		lang = aml.getLanguageSetting();
		sType = aml.getSelectionType();
		//Check if the task is interactive
		isInteractive = Oracle.isInteractive();
		if(isInteractive)
			alignSet = new HashSet<Alignment>();
		//Initialize the alignment
		a = new Alignment();
		//And start the matching procedure
		Vector<MatchStep> selectedSteps = aml.getSelectedSteps();
		setThresholds();
		if(selectedSteps.contains(MatchStep.TRANSLATE))
			translate();
		lexicalMatch();
		bkMatch();
		if(selectedSteps.contains(MatchStep.WORD))
			wordMatch();
		stringMatch();
		if(selectedSteps.contains(MatchStep.STRUCT))
			structuralMatch();
		if(selectedSteps.contains(MatchStep.PROPERTY))
			propertyMatch();
		selection();
		repair();
		return a;
	}
		
//Private Methods

	//Step 1 - Set Threshold
    public static void setThresholds()
    {
    	thresh = BASE_THRESH;
		psmThresh = 0.7;
		wnThresh = 0.1;

    	if(isInteractive)
    	{
    		thresh += INTER_MOD;
			wnThresh = 0.04;
    	}
    	if(size.equals(SizeCategory.HUGE))
    		thresh += SIZE_MOD;
    	if(lang.equals(LanguageSetting.TRANSLATE))
    	{
    		thresh += TRANS_MOD;
			psmThresh = thresh;
    	}
    	else if(lang.equals(LanguageSetting.MULTI))
    		thresh += MULTI_MOD;
    }
    
	//Step 2 - Translate
	private static void translate()
	{
		aml.translateOntologies();
		lang = aml.getLanguageSetting();
	}
	
	//Step 3 - Lexical Match
	private static void lexicalMatch()
	{
		LexicalMatcher lm = new LexicalMatcher();
		lex = lm.match(thresh);
		a.addAll(lex);
	}

	//Step 4 - Background Knowledge Match
	private static void bkMatch()
	{
		//Only if the task is single-language
		if(!lang.equals(LanguageSetting.SINGLE))
			return;
		//We use only WordNet for very small ontologies
		if(size.equals(SizeCategory.SMALL))
		{
			WordNetMatcher wn = new WordNetMatcher();
			Alignment wordNet = wn.match(thresh);
			//Deciding whether to use it based on its coverage of the input ontologies
			//(as we expect a high gain if the coverage is high given that WordNet will
			//generate numerous synonyms)
			double coverage = Math.min(wordNet.sourceCoverage(),wordNet.targetCoverage());
			if(coverage >= wnThresh)
				a.addAllOneToOne(wordNet);
			//Regardless of coverage, the WordNet alignment is used as an auxiliary
			//alignment in interactive selection
			if(isInteractive)
				alignSet.add(wordNet);
		}
		else
		{
			//We test all sources for larger ontologies
			Vector<String> bkSources = new Vector<String>();
			bkSources.addAll(aml.getBKSources());
			//Except WordNet which is not only slow but also error prone
			bkSources.remove("WordNet");
			for(String bk : bkSources)
			{
				//In the case of BK Lexicons and Ontologies, we decide whether to use them
				//based on their mapping gain (over the direct Lexical alignment)
				if(bk.endsWith(".lexicon"))
				{
					MediatingMatcher mm = new MediatingMatcher(BK_PATH + bk);
					Alignment med = mm.match(thresh);
					double gain = med.gain(lex);
					if(gain >= MIN_GAIN_THRESH)
						a.addAll(med);
				}
				else
				{
					try
					{
						aml.openBKOntology(bk);
					}
					catch(OWLOntologyCreationException e)
					{
						System.out.println("WARNING: Could not open ontology " + bk);
						System.out.println(e.getMessage());
						continue;
					}
					XRefMatcher xr = new XRefMatcher(aml.getBKOntology());
					Alignment ref = xr.match(thresh);
					double gain = ref.gain(lex);
					//In the case of Ontologies, if the mapping gain is very high, we can
					//use them for Lexical Extension, which will effectively enable Word-
					//and String-Matching with the BK Ontologies' names
					if(gain >= HIGH_GAIN_THRESH)
					{
						xr.extendLexicons(thresh);
						//If that is the case, we must compute a new Lexical alignment
						//after the extension
						LexicalMatcher lm = new LexicalMatcher();
						a.addAll(lm.match(thresh));
					}
					//Otherwise, we add the BK alignment as normal
					else if(gain >= MIN_GAIN_THRESH)
						a.addAll(ref);					
				}
			}
		}
	}
	
	//Step 5 - Word Match
	private static void wordMatch()
	{
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
	
	//Step 6 - String Match
	private static void stringMatch()
	{
		StringMatcher psm = new StringMatcher();
		//If the task is small, we can use the PSM in match mode
		if(aml.primaryStringMatcher())
		{
			a.addAllOneToOne(psm.match(psmThresh));
			//And if the task is single-language we can use the
			//MultiWordMatcher as well (which uses WordNet)
			if(lang.equals(LanguageSetting.SINGLE))
			{
				MultiWordMatcher mwm = new MultiWordMatcher();
				a.addAllOneToOne(mwm.match(thresh));
			}
		}
		//Otherwise we use it in extendAlignment mode
		else
			a.addAllOneToOne(psm.extendAlignment(a,thresh));
	}	
	
	//Step 7 - Structural Match
	private static void structuralMatch()
	{
		NeighborSimilarityMatcher nsm = new NeighborSimilarityMatcher(
				NeighborSimilarityStrategy.MAXIMUM,false);
		if(isInteractive)
			alignSet.add(nsm.rematch(a));
		a.addAllOneToOne(nsm.extendAlignment(a,thresh));
	}
	
	//Step 8 - Property Match
	private static void propertyMatch()
	{
		PropertyMatcher pm = new PropertyMatcher(true);
		a.addAllOneToOne(pm.extendAlignment(a, thresh));
	}
	
	//Step 9 - Selection
	private static void selection()
	{
		if(aml.structuralSelection())
		{
			ObsoleteRepairer or = new ObsoleteRepairer();
			a = or.repair(a);
				
			BlockRematcher hl = new BlockRematcher();
			Alignment b = hl.rematch(a);
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
		if(isInteractive)
		{
			alignSet.add(a);
			InteractiveSelector is = new InteractiveSelector(alignSet);
			a = is.select(a, thresh);
		}
	}
	
	//Step 10 - Repair
	private static void repair()
	{
		Repairer r;
		if(isInteractive)
			r = new InteractiveRepairer();
		else
			r = new CardinalityRepairer();
		a = r.repair(a);
	}
}