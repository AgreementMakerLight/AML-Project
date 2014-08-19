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
* AgreementMakerLight decision & matching system used in OAEI 2013.           *
* Note: Replace the AML Oracle with the SEALS Oracle for OAEI participation.  *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 23-06-2014                                                            *
* @version 2.0                                                                *
******************************************************************************/
package aml.match;

import aml.AML;
import aml.AML.SelectionType;
import aml.filter.CardinalitySelector;
import aml.filter.CardinalityRepairer;
import aml.filter.RankedSelector;
import aml.ontology.Ontology;
import aml.ontology.URIMap;
import aml.util.Oracle;

public class OAEI2013Matcher
{
	
//Attributes

	//Settings
	private boolean useBK = false;
	private boolean ignoreUMLS = false;
	private boolean repair = false;
	private boolean interactive = false;
	//Profiling parameters
	private int size; //1 = small; 2 = medium; 3 = large
	private SelectionType sType;
	private boolean matchProps;
	private double threshold;
	//The oracle for interactive matching
	private Oracle oracle;
	//The path to UMLS and Uberon
	private final String UMLS = "store/knowledge/UMLS.lexicon";
	private final String UBERON_ONT = "store/knowledge/uberon.owl";
	private final String UBERON_REF = "store/knowledge/uberon.xrefs";
	//Links to the ontologies and alignments
	private Ontology source;
	private Ontology target;
	private Alignment base;
	private Alignment a;
	private Alignment umls = null;
	//Parameter thresholds
	private final double BASE_THRESH = 0.5;
	private final double PROP_THRESH = 0.45;
	private final double GAIN_MIN = 0.03;
	private final double GAIN_HIGH = 0.1;
	private final double COVERAGE = 0.2;
	
//Constructors	
	
	/**
	 * Builds a new OAEI2013 regular matcher with the given settings
	 * @param b: whether to use background knowledge
	 * @param u: whether to ignore UMLS as a source of background knowledge
	 * @param r: whether to perform repair after the matching procedure
	 */
	public OAEI2013Matcher(boolean b, boolean u, boolean r)
	{
		AML aml = AML.getInstance();
		source = aml.getSource();
		target = aml.getTarget();
		useBK = b;
		ignoreUMLS = u;
		repair = r;
		interactive = false;
	}
	
	/**
	 * Builds a new OAEI2013 "interactive" matcher with the given settings
	 * @param b: whether to use background knowledge
	 * @param u: whether to ignore UMLS as a source of background knowledge
	 * @param r: whether to perform repair after the matching procedure
	 * @param ref: the path to the reference alignment to use with the oracle
	 */
	public OAEI2013Matcher(boolean b, boolean u, boolean r, String ref)
	{
		AML aml = AML.getInstance();
		source = aml.getSource();
		target = aml.getTarget();
		useBK = b;
		ignoreUMLS = u;
		repair = r;
		interactive = true;
		oracle = new Oracle(ref);
	}
	
//Public Methods

	/**
	 * Matches the two give ontologies
	 * @return the Alignment between the ontologies
	 */
	public Alignment match()
	{
		long time = System.currentTimeMillis()/1000;
		System.out.println("Starting matching procedure");
		System.out.println("Baseline matching and profiling finished in " +
				baseMatchingAndProfiling() + " seconds");
		if(useBK)
			System.out.println("Background knowledge matching finished in " + backgroundKnowledgeMatching() + " seconds");
		if(umls != null)
			a = umls;
		else
			System.out.println("Extension matching and selection finished in " + extensionAndSelection() + " seconds");
		if(matchProps)
			System.out.println("Property matching finished in " + propertyMatching() + " seconds");
		System.out.println("Matching procedure finished in " + (System.currentTimeMillis()/1000 - time) + " seconds");
		if(repair)
		{
			System.out.println("Starting repair procedure");
			System.out.println("Alignment repair finished in " + repair() + " seconds");
		}
		System.out.println("Mappings: " + a.size());
		return a;
	}
	
//Private Methods
	
	private long baseMatchingAndProfiling()
	{
		long startTime = System.currentTimeMillis()/1000;
		//Step 1 - Compute the baseline alignment
		LexicalMatcher lm = new LexicalMatcher();
		base = lm.match(BASE_THRESH);
		a = new Alignment(base);
		//Step 2 - Check the size category of the problem and set the threshold
		int sSize = source.classCount();
		int tSize = target.classCount();
		if(Math.min(sSize,tSize) > 30000 || Math.max(sSize, tSize) > 60000)
		{
			size = 3;
			threshold = 0.7;
		}
		else if(Math.max(sSize, tSize) > 500)
		{
			size = 2;
			threshold = 0.59;
		}
		else
		{
			size = 1;
			threshold = 0.6;
		}
		//Step 3 - Detect the selection type
		sType = AML.getInstance().getSelectionType();
		//Step 4 - Check the property/class ratio
		double sProps = source.propertyCount() * 1.0 / sSize;
		double tProps = target.propertyCount() * 1.0 / tSize;
		if(sProps >= 0.1 && tProps >= 0.1)
			matchProps = true;
		return System.currentTimeMillis()/1000 - startTime;
	}
	
	private long backgroundKnowledgeMatching()
	{
		long startTime = System.currentTimeMillis()/1000;
		LexicalMatcher lm = new LexicalMatcher();
		switch(size)
		{
			case 3:
			{
				//For large ontologies, we start with UMLS, since it is more likely to have suitable coverage
				if(!ignoreUMLS)
				{
					MediatingMatcher um = new MediatingMatcher(UMLS);
					umls = um.match(BASE_THRESH);
					double umlsGain = umls.gainOneToOne(base);
					//If UMLS has high gain we use the UMLS matcher exclusively
					if(umlsGain >= GAIN_HIGH)
						return System.currentTimeMillis()/1000 - startTime;
					//Otherwise, if it has a significant gain, we use it as a mediating matcher
					else if(umlsGain >= GAIN_MIN)
					{
						a.addAll(umls);
						umls = null;
					}
				}			
				//We proceed to Uberon
				Ontology ub = new Ontology(UBERON_ONT,false);
				ub.getReferenceMap().extend(UBERON_REF);
				XRefMatcher xr = new XRefMatcher(ub);
				Alignment uberon = xr.match(BASE_THRESH);
				double uberonGain = uberon.gainOneToOne(base);
				//If Uberon has a significant gain, we use it as a mediating matcher
				if(uberonGain >= GAIN_MIN)
					a.addAll(uberon);
				//We don't use Uberon for lexical extension in large ontologies because it is likely to induce
				//errors during string matching, and we don't use WordNet at all for the same reason and because
				//it is very time-consuming
			}
			case 2:
			{
				//For medium ontologies we start with Uberon, since the cost of testing it is lower than UMLS
				//and we will use it exclusively if we get a strong match
				Ontology ub = new Ontology(UBERON_ONT,false);
				ub.getReferenceMap().extend(UBERON_REF);
				XRefMatcher xr = new XRefMatcher(ub);
				Alignment uberon = xr.match(BASE_THRESH);
				double uberonGain = uberon.gainOneToOne(base);
				double uberonCoverage = Math.min(uberon.sourceCoverage(),uberon.targetCoverage());
				//If Uberon has high gain and coverage, we use it for Lexicon extension and ignore other sources
				if(uberonGain >= GAIN_HIGH && uberonCoverage >= COVERAGE)
				{
					xr.extendLexicons(0.6);
					a = lm.match(BASE_THRESH);
					return System.currentTimeMillis()/1000 - startTime;
				}
				//Otherwise, we proceed to UMLS
				if(!ignoreUMLS)
				{
					MediatingMatcher um = new MediatingMatcher(UMLS);
					umls = um.match(BASE_THRESH);
					double umlsGain = umls.gainOneToOne(base);
					double umlsCoverage = Math.min(umls.sourceCoverage(),umls.targetCoverage());
					//If UMLS has high gain and coverage, we use the UMLS matcher exclusively
					if(umlsGain >= GAIN_HIGH && umlsCoverage >= COVERAGE)
						return System.currentTimeMillis()/1000 - startTime;
					//Otherwise, if it has a significant gain, we use it as a mediating matcher
					else if(umlsGain >= GAIN_MIN)
					{
						a.addAll(umls);
						umls = null;
					}
				}
				//If Uberon had a significant gain, we use it as a mediating matcher
				if(uberonGain >= GAIN_MIN)
					a.addAll(uberon);
				//And finally, if neither of the previous resources was selected exclusively
				//we test WordNet, which computationally is the most expensive knowledge source 
				WordNetMatcher wn = new WordNetMatcher();
				//WordNet is less reliable, so it is used with the final threshold
				Alignment wordNet = wn.match(threshold);
				double wordNetGain = wordNet.gainOneToOne(base);
				//If WordNet has a significant gain, we use it as a mediating matcher
				//but in extension mode and with strict selection, to avoid errors
				if(wordNetGain >= GAIN_MIN)
					a.addAllOneToOne(wordNet);
			}
			case 1:
			{
				//In the case of small ontologies, WordNet is the only source of background knowledge
				//we test, as Uberon and UMLS are both too large for this scale 
				WordNetMatcher wn = new WordNetMatcher();
				Alignment wordNet = wn.match(threshold);
				double wordNetGain = wordNet.gainOneToOne(base);
				double wordNetCoverage = Math.min(wordNet.sourceCoverage(),wordNet.targetCoverage());
				//And we only use WordNet if its gain and coverage are both high (given the small
				//dimension of the ontologies, we have to enforce higher thresholds)
				if(wordNetGain >= 0.16 && wordNetCoverage >= 0.08)
					a.addAllNonConflicting(wordNet);			
			}
		}
		return System.currentTimeMillis()/1000 - startTime;
	}
		
	private long extensionAndSelection()
	{
		long startTime = System.currentTimeMillis()/1000;
		WordMatcher wm = new WordMatcher();
		ParametricStringMatcher sm = new ParametricStringMatcher();
		//Interactive extension-selection
		if(interactive)
		{
			//For large problems we don't use WordMatcher due to its propensity for errors
			if(size < 3)
				a.addAll(wm.match(BASE_THRESH));
			a = selectInteractive(a);
			Alignment b = sm.extendAlignment(a, BASE_THRESH);
			a.addAll(selectInteractive(b));
			return System.currentTimeMillis()/1000 - startTime;
		}
		//Automatic extension-selection
		else if(sType.equals(SelectionType.STRICT))
		{
			//TODO: Fix the selection type
			if(size < 3)
				a.addAllNonConflicting(wm.match(BASE_THRESH));
			a.addAllNonConflicting(sm.extendAlignment(a,threshold));
			RankedSelector s = new RankedSelector(SelectionType.PERMISSIVE);
			a = s.select(a, threshold);
		}
		else if(sType.equals(SelectionType.PERMISSIVE))
		{
			if(size == 2)
			{
				Alignment b = wm.match(BASE_THRESH);
				RankedSelector s = new RankedSelector(sType);
				b = s.select(b, threshold);
				a.addAllNonConflicting(b);
				b = sm.extendAlignment(a, BASE_THRESH);
				s = new RankedSelector(sType);
				b = s.select(b, threshold);
				a.addAllNonConflicting(b);
			}
			else
			{
				if(size == 1)
					a.addAll(wm.match(BASE_THRESH));
				RankedSelector s = new RankedSelector(sType);
				a = s.select(a, threshold);
				a.addAll(sm.extendAlignment(a,threshold));
				s = new RankedSelector(sType);
				a = s.select(a, threshold);
			}
		}
		else
		{
			if(size < 3)
				a.addAll(wm.match(BASE_THRESH));
			CardinalitySelector s = new CardinalitySelector(6);
			a = s.select(a, threshold);
			a.addAll(sm.extendAlignment(a, threshold));
		}
		return System.currentTimeMillis()/1000 - startTime;
	}
	
	private long propertyMatching()
	{
		long startTime = System.currentTimeMillis()/1000;
		PropertyMatcher pm = new PropertyMatcher(useBK);
		a.addAll(pm.matchProperties(a, PROP_THRESH));
		return System.currentTimeMillis()/1000 - startTime;
	}
	
	private long repair()
	{
		long startTime = System.currentTimeMillis()/1000;
		CardinalityRepairer rep = new CardinalityRepairer();
		a = rep.repair(a);
		return System.currentTimeMillis()/1000 - startTime;
	}
	
	private Alignment selectInteractive(Alignment maps)
	{
		CardinalitySelector s = new CardinalitySelector(2);
		maps = s.select(maps,0.5);
		Alignment b = new Alignment();
		maps.sort();
		int trueCount = 0;
		int falseCount = 0;
		for(Mapping m : maps)
		{
			double sim = m.getSimilarity(); 
			if(sim >= 0.7 && !b.containsConflict(m))
				b.add(m);
			else 
			{
				if(mappingIsTrue(m))
				{
					b.add(m);
					trueCount++;
				}
				else
					falseCount++;
			}
			if(falseCount > a.size() * 0.3)
				break;
		}
		System.out.println("Interactive selection: " + (trueCount+falseCount) +
				" user queries, " + trueCount + " positive");
		return b;
	}

	private boolean mappingIsTrue(Mapping m)
	{
		URIMap map = AML.getInstance().getURIMap();
		String sourceURI = map.getURI(m.getSourceId());
		String targetURI = map.getURI(m.getTargetId());
		return oracle.check(sourceURI,targetURI,m.getRelationship());
	}
}