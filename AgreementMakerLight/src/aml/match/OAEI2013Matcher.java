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
* @date 06-02-2014                                                            *
******************************************************************************/
package aml.match;

import java.io.File;

import aml.ext.AlignmentExtender;
import aml.match.MatchingConfigurations.SelectionType;
import aml.ontology.Ontology;
import aml.ontology.Uberon;
import aml.oracle.Oracle;
import aml.repair.Repairer;

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
	//The path to the Uberon ontology
	private final String UBERON = "store/knowledge/uberon.owl";
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
		useBK = b;
		ignoreUMLS = u;
		repair = r;
		interactive = true;
		oracle = new Oracle(ref);
	}
	
//Public Methods

	/**
	 * Matches the two give ontologies
	 * @param s: the source ontology
	 * @param t: the target ontology
	 * @return the Alignment between the ontologies
	 */
	public Alignment match(Ontology s, Ontology t)
	{
		long time = System.currentTimeMillis()/1000;
		source = s;
		target = t;

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
		LexicalMatcher lm = new LexicalMatcher(false);
		base = lm.match(source,target,BASE_THRESH);
		a = new Alignment(base);
		//Step 2 - Check the size category of the problem and set the threshold
		int sSize = source.termCount();
		int tSize = target.termCount();
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
		sType = Selector.detectSelectionType(base);
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
		LexicalMatcher lm = new LexicalMatcher(false);
		switch(size)
		{
			case 3:
			{
				//For large ontologies, we start with UMLS, since it is more likely to have suitable coverage
				if(!ignoreUMLS)
				{
					UMLSMatcher um = new UMLSMatcher();
					umls = um.match(source, target, BASE_THRESH);
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
				Ontology ub = new Uberon((new File(UBERON).toURI()),false,true);
				XRefMatcher xr = new XRefMatcher(ub);
				Alignment uberon = xr.match(source,target,BASE_THRESH);
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
				Ontology ub = new Uberon((new File(UBERON).toURI()),false,true);
				XRefMatcher xr = new XRefMatcher(ub);
				Alignment uberon = xr.match(source,target,BASE_THRESH);
				double uberonGain = uberon.gainOneToOne(base);
				double uberonCoverage = Math.min(uberon.sourceCoverage(),uberon.targetCoverage());
				//If Uberon has high gain and coverage, we use it for Lexicon extension and ignore other sources
				if(uberonGain >= GAIN_HIGH && uberonCoverage >= COVERAGE)
				{
					AlignmentExtender ae = new AlignmentExtender();
					ae.extendLexicon(source,xr.getSourceAlignment());
					ae.extendLexicon(target,xr.getTargetAlignment());
					a = lm.match(source,target,BASE_THRESH);
					return System.currentTimeMillis()/1000 - startTime;
				}
				//Otherwise, we proceed to UMLS
				if(!ignoreUMLS)
				{
					UMLSMatcher um = new UMLSMatcher();
					umls = um.match(source,target,BASE_THRESH);
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
				Alignment wordNet = wn.match(source,target,threshold);
				double wordNetGain = wordNet.gainOneToOne(base);
				//If WordNet has a significant gain, we use it as a mediating matcher
				//but in extension mode and with strict selection, to avoid errors
				if(wordNetGain >= GAIN_MIN)
					a.addAllNonConflicting(Selector.select(wordNet, threshold, SelectionType.STRICT));
			}
			case 1:
			{
				//In the case of small ontologies, WordNet is the only source of background knowledge
				//we test, as Uberon and UMLS are both too large for this scale 
				WordNetMatcher wn = new WordNetMatcher();
				Alignment wordNet = wn.match(source,target,threshold);
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
				a.addAll(wm.extendAlignment(a, BASE_THRESH));
			a = selectInteractive(a);
			Alignment b = sm.extendAlignment(a, BASE_THRESH);
			a.addAll(selectInteractive(b));
			return System.currentTimeMillis()/1000 - startTime;
		}
		//Automatic extension-selection
		if(sType.equals(SelectionType.STRICT))
		{
			if(size < 3)
				a.addAll(wm.extendAlignment(a, BASE_THRESH));
			a = Selector.select(a, threshold, sType);
			a.addAll(sm.extendAlignment(a,threshold));
			a = Selector.select(a, threshold, sType);
		}
		else if(sType.equals(SelectionType.PERMISSIVE))
		{
			if(size == 2)
			{
				Alignment b = wm.extendAlignment(a, BASE_THRESH);
				b = Selector.select(b, threshold, sType);
				a.addAllNonConflicting(b);
				b = sm.extendAlignment(a, BASE_THRESH);
				b = Selector.select(b, threshold, sType);
				a.addAllNonConflicting(b);
			}
			else
			{
				if(size == 1)
					a.addAll(wm.extendAlignment(a, BASE_THRESH));
				a = Selector.select(a, threshold, sType);
				a.addAll(sm.extendAlignment(a,threshold));
				a = Selector.select(a, threshold, sType);
			}
		}
		else
		{
			if(size < 3)
				a.addAll(wm.extendAlignment(a, BASE_THRESH));
			a = Selector.selectCardinality(a, threshold, 6);
			a.addAll(sm.extendAlignment(a, threshold));
		}
		return System.currentTimeMillis()/1000 - startTime;
	}
	
	private long propertyMatching()
	{
		long startTime = System.currentTimeMillis()/1000;
		PropertyMatcher pm = new PropertyMatcher(useBK);
		a.addAllPropMappings(pm.matchProperties(a, PROP_THRESH));
		return System.currentTimeMillis()/1000 - startTime;
	}
	
	private long repair()
	{
		long startTime = System.currentTimeMillis()/1000;
		Repairer rep = new Repairer();
		a = rep.repair(a);
		return System.currentTimeMillis()/1000 - startTime;
	}
	
	private Alignment selectInteractive(Alignment maps)
	{
		maps = Selector.selectCardinality(maps,0.5,2);
		Alignment b = new Alignment(source,target);
		maps.boostBestMatches(0.1);
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
		String sourceURI = source.getTermURI(m.getSourceId());
		String targetURI = target.getTermURI(m.getTargetId());
		return oracle.check(sourceURI,targetURI,Oracle.Relation.EQUIVALENCE);
	}
}