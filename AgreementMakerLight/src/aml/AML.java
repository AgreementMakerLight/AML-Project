/******************************************************************************
* Copyright 2013-2013 LASIGE                                                  *
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
* AgreementMakerLight decision system & matching algorithm manager, as used   *
* in the OAEI 2013 competition.                                               *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 22-10-2013                                                            *
******************************************************************************/
package aml;

import java.io.File;
import java.net.URI;

import aml.ext.AlignmentExtender;
import aml.match.Alignment;
import aml.match.LexicalMatcher;
import aml.match.Mapping;
import aml.match.PropertyMatcher;
import aml.match.ParametricStringMatcher;
import aml.match.Selector;
import aml.match.UMLSMatcher;
import aml.match.WordMatcher;
import aml.match.WordNetMatcher;
import aml.match.XRefMatcher;
import aml.ontology.Lexicon;
import aml.ontology.Ontology;
import aml.ontology.Uberon;
import aml.oracle.Oracle;
import aml.repair.Repairer;

public class AML
{
	
//Attributes

	//AgreementMakerLight settings
	private boolean useBK = false;
	private boolean ignoreUMLS = false;
	private boolean repair = true;
	private boolean interactive = false;
	
	//The core matching algorithms
	private LexicalMatcher lm;
	private WordMatcher wm;
	private ParametricStringMatcher sm;
	//The oracle for interactive matching
	private Oracle oracle;

	//The path to the Uberon ontology
	private final String UBERON = "store/uberon.owl";

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
	
	//Profiling parameters
	private int size; //1 = small; 2 = medium; 3 = large
	private int card; //1 = near-one; 2 = medium; 3 = large
	private boolean matchProps;
	private double threshold;
	private int repairMax;
	
//Constructors	
	
	/**
	 * Builds a new AgreementMakerLight object with the given settings
	 * @param b: whether to use background knowledge
	 * @param u: whether to ignore UMLS as a source of background knowledge
	 * @param r: whether to perform repair after the matching procedure
	 */
	public AML(boolean b, boolean u, boolean r)
	{
		useBK = b;
		ignoreUMLS = u;
		repair = r;
		lm = new LexicalMatcher(false);
		wm = new WordMatcher();
		sm = new ParametricStringMatcher();
	}
	
//Public Methods
	
	/**
	 * Matches two ontologies interactively, given their uris
	 * and the path to the reference alignment between them
	 * @param s: the uri of the source ontology
	 * @param t: the uri of the target ontology
	 * @param r: the path to the reference alignment
	 * @return the Alignment between the ontologies
	 */
	public Alignment match(URI s, URI t, String r)
	{
		oracle = new Oracle(r);
		interactive = true;
		return match(s,t);
	}
	
	/**
	 * Matches two ontologies given their uris
	 * @param s: the uri of the source ontology
	 * @param t: the uri of the target ontology
	 * @return the Alignment between the ontologies
	 */
	public Alignment match(URI s, URI t)
	{
		long startTime = System.currentTimeMillis()/1000;
		System.out.println("Loading source ontology");
		source = loadOntology(s);
		System.out.println("Loading target ontology");
		target = loadOntology(t);

		long time = System.currentTimeMillis()/1000;
		System.out.println("Starting matching procedure");
		System.out.println("Baseline matching and profiling finished in " + baseMatchingAndProfiling() + " seconds");
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
		
		System.out.println("AgreementMakerLight finished in " + (System.currentTimeMillis()/1000 - startTime) + " seconds");
		return a;
	}
	
	/**
	 * Evaluates the current alignment with the given reference alignment
	 * @param referencePath: the path to the reference alignment
	 * @return the evaluation of the alignment
	 */
	public String evaluate(String referencePath)
	{
		Alignment ref = null;
		try
		{
			ref = new Alignment(source, target, referencePath);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		int found = a.size();		
		int correct = a.evaluate(ref);
		int total = ref.size();

		double precision = 1.0*correct/found;
		String prc = Math.round(precision*1000)/10.0 + "%";
		double recall = 1.0*correct/total;
		String rec = Math.round(recall*1000)/10.0 + "%";
		double fmeasure = 2*precision*recall/(precision+recall);
		String fms = Math.round(fmeasure*1000)/10.0 + "%";
		
		return "Precision\tRecall\tF-measure\tFound\tCorrect\tReference\n" + prc +
			"\t" + rec + "\t" + fms + "\t" + found + "\t" + correct + "\t" + total;
	}

//Private Methods
	
	private Ontology loadOntology(URI u)
	{
		long startTime = System.currentTimeMillis()/1000;
		Ontology o = new Ontology(u,true);
		Lexicon l = o.getLexicon();
		l.generateStopWordSynonyms();
		l.generateBracketSynonyms();
		long elapsedTime = System.currentTimeMillis()/1000 - startTime;
		System.out.println(o.getURI() + " loaded in " + elapsedTime + " seconds");
		return o;
	}

	private long baseMatchingAndProfiling()
	{
		//Step 1 - Compute the baseline alignment
		long startTime = System.currentTimeMillis()/1000;
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
		//Step 3 - Check the cardinality of the problem and set the repairMax
		double cardinality = base.cardinality();
		if(cardinality > 1.4)
		{
			card = 3;
			repairMax = 4000;
		}
		else if(cardinality > 1.02)
		{
			card = 2;
			repairMax = 8000;
		}
		else
		{
			card = 1;
			repairMax = 10000;
		}
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
		switch(size)
		{
			case 3:
			{
				//For large ontologies, we start with UMLS, since it is more likely to have suitable coverage
				if(!ignoreUMLS)
				{
					UMLSMatcher um = new UMLSMatcher();
					umls = um.match(source, target, BASE_THRESH);
					double umlsGain = umls.gain(base);
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
				Ontology ub = new Uberon((new File(UBERON).toURI()),false);
				XRefMatcher xr = new XRefMatcher(ub);
				Alignment uberon = xr.match(source,target,BASE_THRESH);
				double uberonGain = uberon.gain(base);
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
				Ontology ub = new Uberon((new File(UBERON).toURI()),false);
				XRefMatcher xr = new XRefMatcher(ub);
				Alignment uberon = xr.match(source,target,BASE_THRESH);
				double uberonGain = uberon.gain(base);
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
					double umlsGain = umls.gain(base);
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
				double wordNetGain = wordNet.gain(base);
				//If WordNet has a significant gain, we use it as a mediating matcher
				//but in extension mode and with strict selection, to avoid errors
				if(wordNetGain >= GAIN_MIN)
					a.addAllNonConflicting(Selector.selectOneToOne(wordNet, threshold, true));
			}
			case 1:
			{
				//In the case of small ontologies, WordNet is the only source of background knowledge
				//we test, as Uberon and UMLS are both too large for this scale 
				WordNetMatcher wn = new WordNetMatcher();
				Alignment wordNet = wn.match(source,target,threshold);
				double wordNetGain = wordNet.gain(base);
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
		switch(card)
		{
			case 1:
			{
				if(size < 3)
					a.addAll(wm.extendAlignment(a, BASE_THRESH));
				a = Selector.selectOneToOne(a, threshold, true);
				a.addAll(sm.extendAlignment(a,threshold));
				a = Selector.selectOneToOne(a, threshold, true);
			}
			case 2:
			{
				if(size == 2)
				{
					Alignment b = wm.extendAlignment(a, BASE_THRESH);
					b = Selector.selectOneToOne(b, threshold, false);
					a.addAllNonConflicting(b);
					b = sm.extendAlignment(a, BASE_THRESH);
					b = Selector.selectOneToOne(b, threshold, false);
					a.addAllNonConflicting(b);
				}
				else
				{
					if(size == 1)
						a.addAll(wm.extendAlignment(a, BASE_THRESH));
					a = Selector.selectOneToOne(a, threshold, false);
					a.addAll(sm.extendAlignment(a,threshold));
					a = Selector.selectOneToOne(a, threshold, false);
				}
			}
			case 3:
			{
				if(size < 3)
					a.addAll(wm.extendAlignment(a, BASE_THRESH));
				a = Selector.selectCardinality(a, threshold, 6);
				a.addAll(sm.extendAlignment(a, threshold));
			}
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
		Repairer rep = new Repairer(repairMax);
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