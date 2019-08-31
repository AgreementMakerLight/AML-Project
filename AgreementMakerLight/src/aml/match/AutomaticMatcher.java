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
* Automatic AgreementMakerLight decision & matching system (as used in OAEI). *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.match;

import java.io.IOException;
import java.util.Vector;

import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import aml.AML;
import aml.filter.CardinalitySelector;
import aml.filter.DifferentClassPenalizer;
import aml.filter.DomainAndRangeFilterer;
import aml.filter.InteractiveFilterer;
import aml.filter.ObsoleteFilterer;
import aml.filter.Repairer;
import aml.filter.Selector;
import aml.knowledge.MediatorLexicon;
import aml.ontology.Ontology;
import aml.settings.EntityType;
import aml.settings.InstanceMatchingCategory;
import aml.settings.LanguageSetting;
import aml.settings.NeighborSimilarityStrategy;
import aml.settings.SelectionType;
import aml.settings.SizeCategory;
import aml.util.InteractionManager;

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
	private static LanguageSetting lang;
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
	private static Alignment a;
	private static Alignment lex;
	
//Constructors	
	
	private AutomaticMatcher(){}
	
//Public Methods

	public static void match() throws UnsupportedEntityTypeException
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
		//If translation is necessary, translate
		lang = LanguageSetting.getLanguageSetting();
		if(lang.equals(LanguageSetting.TRANSLATE))
		{
			aml.translateOntologies();
    		lang = LanguageSetting.getLanguageSetting();
		}
		//Initialize the alignment
		a = new Alignment();
		thresh = aml.getThreshold();
		if(matchClasses & matchProperties & matchIndividuals)
			matchAll();
		else if(matchClasses & matchProperties)
			matchClassesAndProperties();
		else if(matchClasses)
		{
			matchClasses();
			filterClasses();
		}
		else if(matchProperties)
			matchProperties();
		else if(matchIndividuals)
			matchIndividuals();
	}
		
//Private Methods

	private static void matchAll() throws UnsupportedEntityTypeException
	{
		matchClasses();
		matchProperties();
		filterClasses();
		matchIndividuals();
	}
	
	private static void matchClassesAndProperties() throws UnsupportedEntityTypeException
	{
		matchClasses();
		matchProperties();
		DomainAndRangeFilterer dr = new DomainAndRangeFilterer();
		dr.filter();
		filterClasses();
	}
	
	//Matching procedure for classes (or classes+properties)
	private static void matchClasses() throws UnsupportedEntityTypeException
	{
		SizeCategory size = aml.getSizeClasses();
    	if(im.isInteractive())
    		thresh += INTERACTIVE_MOD;
		if(aml.hasReferences())
		{
			DirectXRefMatcher dx = new DirectXRefMatcher();
			a.addAll(dx.match(EntityType.CLASS, thresh));
		}
		
		LexicalMatcher lm = new LexicalMatcher();
		lex = lm.match(EntityType.CLASS, thresh);
		a.addAll(lex);
		
		if(lang.equals(LanguageSetting.SINGLE))
		{
			if(size.equals(SizeCategory.SMALL))
			{
				WordNetMatcher wn = new WordNetMatcher();
				Alignment wordNet = wn.match(EntityType.CLASS, thresh);
				//Deciding whether to use it based on its coverage of the input ontologies
				//(as we expect a high gain if the coverage is high given that WordNet will
				//generate numerous synonyms)
				double coverage = Math.min(wordNet.sourceCoverage(EntityType.CLASS),
						wordNet.targetCoverage(EntityType.CLASS));
				
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
							MediatorLexicon ml = new MediatorLexicon(BK_PATH + bk);
							MediatingMatcher mm = new MediatingMatcher(ml, BK_PATH + bk);
							Alignment med = mm.match(EntityType.CLASS, thresh);
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
						Alignment ref = xr.match(EntityType.CLASS, thresh);
						double gain = ref.gain(lex);
						//In the case of Ontologies, if the mapping gain is very high, we can
						//use them for Lexical Extension, which will effectively enable Word-
						//and String-Matching with the BK Ontologies' names
						if(gain >= HIGH_GAIN_THRESH)
						{
							System.out.println(bk + " selected for lexical extension");
							xr.extendLexicons();
							//If that is the case, we must compute a new Lexical alignment
							//after the extension
							a.addAll(lm.match(EntityType.CLASS, thresh));
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
			Alignment word = new Alignment();
			if(lang.equals(LanguageSetting.SINGLE))
			{
				WordMatcher wm = new WordMatcher();
				word.addAll(wm.match(EntityType.CLASS, thresh));
			}
			else if(lang.equals(LanguageSetting.MULTI))
			{
				for(String l : aml.getLanguages())
				{
					WordMatcher wm = new WordMatcher(l);
					word.addAll(wm.match(EntityType.CLASS, thresh));
				}
			}
			a.addAllOneToOne(word);
		}
		StringMatcher psm = new StringMatcher();
		//If the task is small, we can use the PSM in match mode
		if(size.equals(SizeCategory.SMALL))
		{
			if(lang.equals(LanguageSetting.SINGLE))
			{
				a.addAll(psm.match(EntityType.CLASS, thresh + PSM_MOD));
				MultiWordMatcher mwm = new MultiWordMatcher();
				a.addAllOneToOne(mwm.match(EntityType.CLASS, thresh));
				AcronymMatcher am = new AcronymMatcher();
				a.addAllOneToOne(am.match(EntityType.CLASS, thresh));
			}
			else
				a.addAll(psm.match(EntityType.CLASS, thresh));
		}
		//Otherwise we use it in extendAlignment mode
		else
			a.addAllOneToOne(psm.extendAlignment(a,EntityType.CLASS,thresh));

		if(!size.equals(SizeCategory.HUGE))
		{
			SpacelessLexicalMatcher sl = new SpacelessLexicalMatcher();
			a.addAllNonConflicting(sl.match(EntityType.CLASS, thresh));
			double nameRatio = Math.max(1.0*source.getLexicon().nameCount(EntityType.CLASS)/source.count(EntityType.CLASS),
					1.0*target.getLexicon().nameCount(EntityType.CLASS)/target.count(EntityType.CLASS));
			if(nameRatio >= 1.2)
			{
				ThesaurusMatcher tm = new ThesaurusMatcher();
				a.addAllOneToOne(tm.match(EntityType.CLASS, thresh));
			}
		}
		if(size.equals(SizeCategory.SMALL) || size.equals(SizeCategory.MEDIUM))
		{
			NeighborSimilarityMatcher nsm = new NeighborSimilarityMatcher(
					aml.getNeighborSimilarityStrategy(),aml.directNeighbors());
			a.addAllOneToOne(nsm.extendAlignment(a,EntityType.CLASS,thresh));
		}
		aml.setAlignment(a);
	}
	
	//Matching procedure for individuals
	private static void matchIndividuals() throws UnsupportedEntityTypeException
	{
		SizeCategory size = aml.getSizeIndividuals();
		double connectivity = aml.getIndividualConnectivity();
		double valueCoverage = aml.getIndividualValueDensity();
		double nameCoverage = Math.min(aml.getSource().getLexicon().nameCount(EntityType.INDIVIDUAL)*1.0/aml.getSource().count(EntityType.INDIVIDUAL),
				aml.getTarget().getLexicon().nameCount(EntityType.INDIVIDUAL)*1.0/aml.getTarget().count(EntityType.INDIVIDUAL));
		
		//Lexical problem
		if(nameCoverage > 0.8)
		{
			LexicalMatcher lm = new LexicalMatcher();
			Alignment b = lm.match(EntityType.INDIVIDUAL,thresh);
			a.addAll(b);
			StringMatcher sm = new StringMatcher();
			if(size.equals(SizeCategory.HUGE))
				a.addAll(sm.extendAlignment(a, EntityType.INDIVIDUAL, thresh));
			else
				a.addAll(sm.match(EntityType.INDIVIDUAL,thresh));
			for(String l : aml.getLanguages())
			{
				WordMatcher wm = new WordMatcher(l);
				if(size.equals(SizeCategory.HUGE))
					a.addAll(wm.extendAlignment(a, EntityType.INDIVIDUAL, thresh));
				else
					a.addAll(wm.match(EntityType.INDIVIDUAL,thresh));
			}
			ValueMatcher vm = new ValueMatcher();
			b = vm.match(EntityType.INDIVIDUAL, thresh);
			if(b.sourceCoverage(EntityType.INDIVIDUAL) > 0.3 && b.targetCoverage(EntityType.INDIVIDUAL) > 0.3)
			{
				a.addAllNonConflicting(b);
				ValueStringMatcher vsm = new ValueStringMatcher();
				if(size.equals(SizeCategory.HUGE))
					a.addAll(vsm.extendAlignment(a, EntityType.INDIVIDUAL, thresh));
				else
					a.addAll(vsm.match(EntityType.INDIVIDUAL,thresh));				
			}
			aml.setAlignment(a);
			if(aml.getInstanceMatchingCategory().equals(InstanceMatchingCategory.SAME_ONTOLOGY))
				DifferentClassPenalizer.penalize();
			Selector s = new Selector(thresh,SelectionType.PERMISSIVE);
			s.filter();
		}
		//Structural problem
		else if(connectivity >= 0.9 || (connectivity >= 0.4 && valueCoverage < 0.2))
		{
			ProcessMatcher pm = new ProcessMatcher();
			a = pm.match(EntityType.INDIVIDUAL, thresh);
			aml.setAlignment(a);
			if(aml.getInstanceMatchingCategory().equals(InstanceMatchingCategory.SAME_ONTOLOGY))
				DifferentClassPenalizer.penalize();
			Selector s;
			if(aml.getAlignment().cardinality() >= 2.0)
				s = new Selector(thresh,SelectionType.HYBRID);
			else
				s = new Selector(thresh,SelectionType.PERMISSIVE);
			s.filter();
		}
		//Data problem
		else
		{
			ValueMatcher vm = new ValueMatcher();
			Alignment b = vm.match(EntityType.INDIVIDUAL, thresh);
			double cov = Math.min(b.sourceCoverage(EntityType.INDIVIDUAL),
					b.targetCoverage(EntityType.INDIVIDUAL));
			System.out.println("ValueMatcher coverage : " + cov);
			//ValueMatcher based strategy
			if(cov >= 0.5)
			{
				HybridStringMatcher sm = new HybridStringMatcher(size.equals(SizeCategory.SMALL));
				a = sm.match(EntityType.INDIVIDUAL, thresh);
				a.addAll(b);
				aml.setAlignment(a);
				if(aml.getInstanceMatchingCategory().equals(InstanceMatchingCategory.SAME_ONTOLOGY))
					DifferentClassPenalizer.penalize();
				Selector s = new Selector(thresh,SelectionType.PERMISSIVE);
				s.filter();
			}
			//Default strategy
			else
			{
				thresh = 0.2;
				b = vm.match(EntityType.INDIVIDUAL, thresh);
				HybridStringMatcher sm = new HybridStringMatcher(size.equals(SizeCategory.SMALL));
				a = sm.match(EntityType.INDIVIDUAL, thresh);
				ValueStringMatcher vsm = new ValueStringMatcher();
				a.addAll(vsm.match(EntityType.INDIVIDUAL, thresh));
				Value2LexiconMatcher vlm = new Value2LexiconMatcher(size.equals(SizeCategory.SMALL)); 
				a.addAll(vlm.match(EntityType.INDIVIDUAL, thresh));
				aml.setAlignment(a);
				if(aml.getInstanceMatchingCategory().equals(InstanceMatchingCategory.SAME_ONTOLOGY))
					DifferentClassPenalizer.penalize();

				Alignment c = vsm.rematch(a, EntityType.INDIVIDUAL);
				Alignment d = vlm.rematch(a, EntityType.INDIVIDUAL);
				Alignment aux = LWC.combine(c, d, 0.75);
				aux = LWC.combine(aux, b, 0.65);
				aux = LWC.combine(aux, a, 0.8);
				
				Selector s = new Selector(thresh,SelectionType.PERMISSIVE,aux);
				s.filter();
				System.out.println(aml.getSelectionType());
				System.out.println(aml.getAlignment().cardinality());
			}
		}
	}
	
	//Matching procedure for properties only
	private static void matchProperties() throws UnsupportedEntityTypeException
	{
		HybridStringMatcher pm = new HybridStringMatcher(true);
		a.addAll(pm.match(EntityType.DATA, thresh));
		a.addAll(pm.match(EntityType.OBJECT, thresh));
		aml.setAlignment(a);
	}
	
	private static void filterClasses() throws UnsupportedEntityTypeException
	{
		SizeCategory size = aml.getSizeClasses();
		SelectionType sType = aml.getSelectionType();
		int card = Math.max(aml.getSource().count(EntityType.CLASS), aml.getTarget().count(EntityType.CLASS))/
				Math.min(aml.getSource().count(EntityType.CLASS), aml.getTarget().count(EntityType.CLASS));
		if(size.equals(SizeCategory.SMALL))
			card = 1;
		if(size.equals(SizeCategory.HUGE))
		{
			ObsoleteFilterer or = new ObsoleteFilterer();
			or.filter();
				
			BlockRematcher hl = new BlockRematcher();
			Alignment b = hl.rematch(a,EntityType.CLASS);
			NeighborSimilarityMatcher nb = new NeighborSimilarityMatcher(
					NeighborSimilarityStrategy.MAXIMUM,true);
			Alignment c = nb.rematch(a,EntityType.CLASS);
			b = LWC.combine(b, c, 0.75);
			b = LWC.combine(a, b, 0.8);
			CardinalitySelector s = new CardinalitySelector(thresh-0.05,card,sType);
			b = s.filter(b);
			s = new CardinalitySelector(thresh,card,sType,b);
			s.filter();
		}
		else if(!im.isInteractive())
		{
			CardinalitySelector s = new CardinalitySelector(thresh,card,sType);
			s.filter();
		}
		if(im.isInteractive())
		{
			if(size.equals(SizeCategory.SMALL))
				im.setLimit((int)Math.round(a.size()*0.45));
			else
				im.setLimit((int)Math.round(a.size()*0.15));
			InteractiveFilterer in = new InteractiveFilterer();
			in.filter();
			im.setLimit((int)Math.round(a.size()*0.05));
		}
		else
			im.setLimit(0);
		if(!size.equals(SizeCategory.HUGE) || aml.getAlignment().cardinality() < 1.5)
		{
			Repairer r = new Repairer();
			r.filter();
		}
	}
}