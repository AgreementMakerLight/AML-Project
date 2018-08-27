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
* Automatic AgreementMakerLight decision & matching system (as used in OAEI). *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.match;

import java.io.IOException;
import java.util.Vector;

import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import aml.AML;
import aml.alignment.SimpleAlignment;
import aml.alignment.LWC;
import aml.filter.CoSelector;
import aml.filter.DifferentClassPenalizer;
import aml.filter.DomainAndRangeFilterer;
import aml.filter.InteractiveFilterer;
import aml.filter.ObsoleteFilterer;
import aml.filter.Repairer;
import aml.filter.SelectionType;
import aml.filter.Selector;
import aml.match.bk.DirectXRefMatcher;
import aml.match.bk.MediatingMatcher;
import aml.match.bk.MediatingXRefMatcher;
import aml.match.bk.MultiWordMatcher;
import aml.match.bk.WordNetMatcher;
import aml.match.lexical.AcronymMatcher;
import aml.match.lexical.HybridStringMatcher;
import aml.match.lexical.LexicalMatcher;
import aml.match.lexical.SpacelessLexicalMatcher;
import aml.match.lexical.StringMatcher;
import aml.match.lexical.ThesaurusMatcher;
import aml.match.lexical.WordMatcher;
import aml.match.structural.BlockRematcher;
import aml.match.structural.NeighborSimilarityMatcher;
import aml.match.structural.NeighborSimilarityStrategy;
import aml.match.structural.ProcessMatcher;
import aml.match.value.Value2LexiconMatcher;
import aml.match.value.ValueMatcher;
import aml.match.value.ValueStringMatcher;
import aml.ontology.EntityType;
import aml.ontology.Ontology;
import aml.ontology.lexicon.ExternalLexicon;
import aml.settings.InstanceMatchingCategory;
import aml.settings.LanguageSetting;
import aml.settings.SizeCategory;
import aml.util.interactive.InteractionManager;

public class AutomaticMatcher
{
	
//Attributes

	//Link to the AML class and ontologies
	private static AML aml;
	private static Ontology source,target;
	//Interaction manager
	private static InteractionManager im;
	//Settings
	private static boolean matchClasses, matchIndividuals, matchProperties;
	private static SizeCategory size;
	//BackgroundKnowledge path
	private static final String BK_PATH = "store/knowledge/";
	//Thresholds
	private static double thresh;
	private static final double HIGH_GAIN_THRESH = 0.25;
	private static final double MIN_GAIN_THRESH = 0.02;
	private static final double WN_THRESH = 0.1;
	//And their modifiers
	private static final double INTERACTIVE_MOD = -0.3;
	private static final double PSM_MOD = 0.1;
	//Alignments
	private static SimpleAlignment a;
	private static SimpleAlignment lex;
	
//Constructors	
	
	private AutomaticMatcher(){}
	
//Public Methods

	public static void match()
	{
		//Get the AML instance
		aml = AML.getInstance();
		source = aml.getSource();
		target = aml.getTarget();
		//The interaction manager
		im = aml.getInteractionManager();
		//What entity types to match
		matchClasses = aml.matchClasses();
		matchIndividuals = aml.matchIndividuals();
		matchProperties = aml.matchProperties();
		size = aml.getSizeCategory();
		//Initialize the alignment
		a = new SimpleAlignment();
		thresh = aml.getThreshold();
		if(matchClasses)
			matchClasses();
		else if(matchProperties)
			matchProperties();
		if(matchIndividuals)
			matchIndividuals();
	}
		
//Private Methods

	//Matching procedure for classes (or classes+properties)
	private static void matchClasses()
	{
    	if(im.isInteractive())
    		thresh += INTERACTIVE_MOD;
		//If translation is necessary, translate
		LanguageSetting lang = LanguageSetting.getLanguageSetting();
		if(lang.equals(LanguageSetting.TRANSLATE))
		{
			aml.translateOntologies();
    		lang = LanguageSetting.getLanguageSetting();
		}
		
		if(aml.hasReferences())
		{
			DirectXRefMatcher dx = new DirectXRefMatcher();
			a.addAll(dx.match(source,target,EntityType.CLASS, thresh));
		}
		
		LexicalMatcher lm = new LexicalMatcher();
		lex = lm.match(source,target,EntityType.CLASS, thresh);
		a.addAll(lex);
		
		if(lang.equals(LanguageSetting.SINGLE))
		{
			if(size.equals(SizeCategory.SMALL))
			{
				WordNetMatcher wn = new WordNetMatcher();
				SimpleAlignment wordNet = wn.match(source,target,EntityType.CLASS, thresh);
				//Deciding whether to use it based on its coverage of the input ontologies
				//(as we expect a high gain if the coverage is high given that WordNet will
				//generate numerous synonyms)
				double coverage = Math.min(wordNet.sourceCoverage(),
						wordNet.targetCoverage());
				
				if(coverage >= WN_THRESH)
				{
					System.out.println("WordNet selected");
					a.addAllOneToOne(wordNet);
				}
				else
					System.out.println("WordNet discarded");
			}
			else
			{
				Vector<String> bkSources = new Vector<String>();
				bkSources.addAll(aml.getBKSources());
				for(String bk : bkSources)
				{
					//In the case of BK Lexicons and Ontologies, we decide whether to use them
					//based on their mapping gain (over the direct Lexical alignment)
					if(bk.endsWith(".lexicon"))
					{
						try
						{
							ExternalLexicon ml = new ExternalLexicon(BK_PATH + bk);
							MediatingMatcher mm = new MediatingMatcher(ml, BK_PATH + bk);
							SimpleAlignment med = mm.match(source,target,EntityType.CLASS, thresh);
							double gain = med.gain(lex);
							if(gain >= MIN_GAIN_THRESH)
							{
								System.out.println(bk + " selected");
								a.addAll(med);
							}
							else
								System.out.println(bk + " discarded");
						}
						catch(IOException e)
						{
							System.out.println("WARNING: Could not open lexicon " + bk);
							e.printStackTrace();
							continue;						
						}
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
						MediatingXRefMatcher xr = new MediatingXRefMatcher(aml.getBKOntology());
						SimpleAlignment ref = xr.match(source,target,EntityType.CLASS, thresh);
						double gain = ref.gain(lex);
						//In the case of Ontologies, if the mapping gain is very high, we can
						//use them for Lexical Extension, which will effectively enable Word-
						//and String-Matching with the BK Ontologies' names
						if(gain >= HIGH_GAIN_THRESH)
						{
							System.out.println(bk + " selected for lexical extension");
							xr.extendLexicon(source);
							xr.extendLexicon(target);
							
							//If that is the case, we must compute a new Lexical alignment
							//after the extension
							a.addAll(lm.match(source,target,EntityType.CLASS, thresh));
						}
						//Otherwise, we add the BK alignment as normal
						else if(gain >= MIN_GAIN_THRESH)
						{
							System.out.println(bk + " selected as a mediator");
							a.addAll(ref);
						}
						else
							System.out.println(bk + " discarded");
					}
				}
			}
		}
		if(!size.equals(SizeCategory.HUGE))
		{
			SimpleAlignment word = new SimpleAlignment();
			for(String l : aml.getLanguages())
			{
				WordMatcher wm = new WordMatcher(l);
				word.addAll(wm.match(source,target,EntityType.CLASS, thresh));
			}
			a.addAllOneToOne(word);
		}
		StringMatcher psm = new StringMatcher();
		//If the task is small, we can use the PSM in match mode
		if(size.equals(SizeCategory.SMALL))
		{
			if(lang.equals(LanguageSetting.SINGLE))
			{
				a.addAll(psm.match(source,target,EntityType.CLASS, thresh + PSM_MOD));
				MultiWordMatcher mwm = new MultiWordMatcher();
				a.addAllOneToOne(mwm.match(source,target,EntityType.CLASS, thresh));
				AcronymMatcher am = new AcronymMatcher();
				a.addAllOneToOne(am.match(source,target,EntityType.CLASS, thresh));
			}
			else
				a.addAll(psm.match(source,target,EntityType.CLASS, thresh));
		}
		//Otherwise we use it in extendAlignment mode
		else
			a.addAllOneToOne(psm.extendAlignment(source,target,a,EntityType.CLASS,thresh));

		if(!size.equals(SizeCategory.HUGE))
		{
			SpacelessLexicalMatcher sl = new SpacelessLexicalMatcher();
			a.addAllNonConflicting(sl.match(source,target,EntityType.CLASS, thresh));
			double nameRatio = Math.max(1.0*source.getLexicon().nameCount(EntityType.CLASS)/source.count(EntityType.CLASS),
					1.0*target.getLexicon().nameCount(EntityType.CLASS)/target.count(EntityType.CLASS));
			if(nameRatio >= 1.2)
			{
				ThesaurusMatcher tm = new ThesaurusMatcher();
				a.addAllOneToOne(tm.match(source,target,EntityType.CLASS, thresh));
			}
		}
		if(size.equals(SizeCategory.SMALL) || size.equals(SizeCategory.MEDIUM))
		{
			NeighborSimilarityMatcher nsm = new NeighborSimilarityMatcher(
					aml.getNeighborSimilarityStrategy(),aml.directNeighbors());
			a.addAllOneToOne(nsm.extendAlignment(source,target,a,EntityType.CLASS,thresh));
		}
		if(matchProperties)
		{
			HybridStringMatcher pm = new HybridStringMatcher(true);
			a.addAll(pm.match(source,target,EntityType.DATA_PROP, thresh));
			a.addAll(pm.match(source,target,EntityType.OBJECT_PROP, thresh));
			DomainAndRangeFilterer dr = new DomainAndRangeFilterer();
			a = (SimpleAlignment) dr.filter(a);
		}
		SelectionType sType = aml.getSelectionType();
		if(size.equals(SizeCategory.HUGE))
		{
			ObsoleteFilterer or = new ObsoleteFilterer();
			a = (SimpleAlignment) or.filter(a);
				
			BlockRematcher hl = new BlockRematcher();
			SimpleAlignment b = hl.rematch(source,target,a,EntityType.CLASS);
			NeighborSimilarityMatcher nb = new NeighborSimilarityMatcher(
					NeighborSimilarityStrategy.MAXIMUM,true);
			SimpleAlignment c = nb.rematch(source,target,a,EntityType.CLASS);
			b = LWC.combine(b, c, 0.75);
			b = LWC.combine(a, b, 0.8);
			Selector s = new Selector(thresh-0.05,sType);
			b = (SimpleAlignment) s.filter(b);
			CoSelector cs = new CoSelector(thresh,sType,b);
			a = (SimpleAlignment) cs.filter(a);
		}
		else if(!im.isInteractive())
		{
			Selector s = new Selector(thresh,sType);
			a = (SimpleAlignment) s.filter(a);
		}
		if(im.isInteractive())
		{
			if(size.equals(SizeCategory.SMALL))
				im.setLimit((int)Math.round(a.size()*0.45));
			else
				im.setLimit((int)Math.round(a.size()*0.15));
			InteractiveFilterer in = new InteractiveFilterer();
			a = (SimpleAlignment) in.filter(a);
			im.setLimit((int)Math.round(a.size()*0.05));
		}
		else
			im.setLimit(0);
		if(!size.equals(SizeCategory.HUGE) || aml.getAlignment().cardinality() < 1.5)
		{
			aml.setAlignment(a);
			Repairer r = new Repairer();
			a = (SimpleAlignment) r.filter(a);
		}
		aml.setAlignment(a);
	}
	
	//Matching procedure for individuals
	private static void matchIndividuals()
	{
		LanguageSetting lang = LanguageSetting.getLanguageSetting();
		double connectivity = aml.getIndividualConnectivity();
		double valueCoverage = aml.getIndividualValueDensity();
		//Translation problem
		if(lang.equals(LanguageSetting.TRANSLATE))
		{
			aml.translateOntologies();
			LexicalMatcher lm = new LexicalMatcher();
			SimpleAlignment a = lm.match(source,target,EntityType.INDIVIDUAL,thresh);
			StringMatcher sm = new StringMatcher();
			a.addAll(sm.match(source,target,EntityType.INDIVIDUAL,thresh));
			for(String l : aml.getLanguages())
			{
				WordMatcher wm = new WordMatcher(l);
				a.addAll(wm.match(source,target,EntityType.INDIVIDUAL, thresh));
			}
			if(aml.getInstanceMatchingCategory().equals(InstanceMatchingCategory.SAME_ONTOLOGY))
				DifferentClassPenalizer.penalize(a);
			Selector s = new Selector(thresh,SelectionType.PERMISSIVE);
			a = (SimpleAlignment) s.filter(a);
		}
		//Process matching problem
		else if(connectivity >= 0.9 || (connectivity >= 0.4 && valueCoverage < 0.2))
		{
			ProcessMatcher pm = new ProcessMatcher();
			a = pm.match(source,target,EntityType.INDIVIDUAL, thresh);
			if(aml.getInstanceMatchingCategory().equals(InstanceMatchingCategory.SAME_ONTOLOGY))
				DifferentClassPenalizer.penalize(a);
			Selector s;
			if(a.cardinality() >= 2.0)
				s = new Selector(thresh,SelectionType.HYBRID);
			else
				s = new Selector(thresh,SelectionType.PERMISSIVE);
			a = (SimpleAlignment) s.filter(a);
		}
		else
		{
			ValueMatcher vm = new ValueMatcher();
			SimpleAlignment b = vm.match(source,target,EntityType.INDIVIDUAL, thresh);
			double cov = Math.min(b.sourceCoverage(),
					b.targetCoverage());
			System.out.println("ValueMatcher coverage : " + cov);
			//ValueMatcher based strategy
			if(cov >= 0.5)
			{
				HybridStringMatcher sm = new HybridStringMatcher(aml.getSizeCategory().equals(SizeCategory.SMALL));
				a = sm.match(source,target,EntityType.INDIVIDUAL, thresh);
				a.addAll(b);
				aml.setAlignment(a);
				if(aml.getInstanceMatchingCategory().equals(InstanceMatchingCategory.SAME_ONTOLOGY))
					DifferentClassPenalizer.penalize(a);
				Selector s = new Selector(thresh,SelectionType.PERMISSIVE);
				a = (SimpleAlignment) s.filter(a);
			}
			//Default strategy
			else
			{
				thresh = 0.2;
				b = vm.match(source,target,EntityType.INDIVIDUAL, thresh);
				HybridStringMatcher sm = new HybridStringMatcher(size.equals(SizeCategory.SMALL));
				a = sm.match(source,target,EntityType.INDIVIDUAL, thresh);
				ValueStringMatcher vsm = new ValueStringMatcher();
				a.addAll(vsm.match(source,target,EntityType.INDIVIDUAL, thresh));
				Value2LexiconMatcher vlm = new Value2LexiconMatcher(size.equals(SizeCategory.SMALL)); 
				a.addAll(vlm.match(source,target,EntityType.INDIVIDUAL, thresh));
				aml.setAlignment(a);
				if(aml.getInstanceMatchingCategory().equals(InstanceMatchingCategory.SAME_ONTOLOGY))
					DifferentClassPenalizer.penalize(a);

				SimpleAlignment c = vsm.rematch(source,target,a,EntityType.INDIVIDUAL);
				SimpleAlignment d = vlm.rematch(source,target,a,EntityType.INDIVIDUAL);
				SimpleAlignment aux = LWC.combine(c, d, 0.75);
				aux = LWC.combine(aux, b, 0.65);
				aux = LWC.combine(aux, a, 0.8);
				
				CoSelector s = new CoSelector(thresh,SelectionType.PERMISSIVE,aux);
				a = (SimpleAlignment) s.filter(a);
			}
		}
		aml.setAlignment(a);
	}
	
	//Matching procedure for properties only
	private static void matchProperties()
	{
		HybridStringMatcher pm = new HybridStringMatcher(true);
		a.addAll(pm.match(source,target,EntityType.DATA_PROP, thresh));
		a.addAll(pm.match(source,target,EntityType.OBJECT_PROP, thresh));
		DomainAndRangeFilterer f = new DomainAndRangeFilterer();
		a = (SimpleAlignment) f.filter(a);
		aml.setAlignment(a);
	}
}