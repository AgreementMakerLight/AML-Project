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
* Matches Ontologies by measuring the global word similarity between their    *
* classes, using a weighted Jaccard index.                                    *
* NOTE: This matching algorithm requires O(N^2) memory in the worst case and  *
* thus should not be used with very large Ontologies unless adequate memory   *
* is available.                                                               *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 31-07-2014                                                            *
* @version 2.0                                                                *
******************************************************************************/
package aml.match;

import java.util.Set;
import java.util.Vector;

import aml.AML;
import aml.ontology.WordLexicon;
import aml.util.Table2Plus;

public class WordMatcher implements PrimaryMatcher
{

//Constructors
	
	/**
	 * Constructs a new WordMatcher
	 */
	public WordMatcher(){}
	
//Public Methods
	
	@Override
	public Alignment match(double thresh)
	{
		Alignment a = new Alignment();
		AML aml = AML.getInstance();
		WordLexicon sWLex = new WordLexicon(aml.getSource().getLexicon());
		WordLexicon tWLex = new WordLexicon(aml.getTarget().getLexicon());
		a.addAll(matchWordLexicons(sWLex,tWLex,thresh));
		return a;
	}
	
	/**
	 * Matches two WordLexicons
	 * @param sWLex: the source WordLexicon to match
	 * @param tWLex: the target WordLexicon to match
	 * @param thresh: the similarity threshold
	 * @return the Alignment between the ontologies containing
	 * the two WordLexicons
	 */
	public Alignment matchWordLexicons(WordLexicon sWLex, WordLexicon tWLex, double thresh)
	{
		Table2Plus<Integer,Integer,Double> maps = new Table2Plus<Integer,Integer,Double>();
		WordLexicon larger, smaller;
		//To minimize iterations, we want to iterate through the smallest Lexicon
		boolean sourceIsSmaller = (sWLex.wordCount() <= tWLex.wordCount());
		if(sourceIsSmaller)
		{
			smaller = sWLex;
			larger = tWLex;
		}
		else
		{
			smaller = tWLex;
			larger = sWLex;
		}
		//Get the smaller ontology words
		Set<String> words = smaller.getWords();
		for(String s : words)
		{
			//Get all term indexes for the name in both ontologies
			Vector<Integer> largerIndexes = larger.getClasses(s);
			Vector<Integer> smallerIndexes = smaller.getClasses(s);
			if(largerIndexes == null)
				continue;
			//Otherwise, compute the average EC
			double smallerEc = smaller.getEC(s);
			double largerEc = larger.getEC(s);
			//Then match all indexes
			for(Integer i : smallerIndexes)
			{
				double smallerSim = smallerEc * smaller.getWeight(s, i);
				for(Integer j : largerIndexes)
				{
					double largerSim = largerEc * larger.getWeight(s, j);
					int sourceId, targetId;
					if(sourceIsSmaller)
					{
						sourceId = i;
						targetId = j;
					}
					else
					{
						sourceId = j;
						targetId = i;
					}
					Double sim = maps.get(sourceId,targetId);
					if(sim == null)
						sim = 0.0;
					sim += Math.sqrt(smallerSim * largerSim);
					maps.add(sourceId,targetId,sim);
				}
			}
		}
		Set<Integer> sources = maps.keySet();
		Alignment a = new Alignment();
		for(Integer i : sources)
		{
			Set<Integer> targets = maps.keySet(i);
			for(Integer j : targets)
			{
				double sim = maps.get(i,j);
				sim /= sWLex.getEC(i) + tWLex.getEC(j) - sim;
				if(sim >= thresh)
					a.add(new Mapping(i, j, sim));
			}
		}
		return a;
	}
}