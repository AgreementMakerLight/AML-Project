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
* Matches Ontologies by finding partial matches between their names with      *
* either 2 or 3 words. It checks whether the words are equal, synonyms in     *
* WordNet, or have a high Wu-Palmer score.                                    *
*                                                                             *
* WARNING: This matching algorithm takes O(N^2) time, and thus should be used *
* only to match small ontologies.                                             *
*                                                                             *
* @author Amruta Nanavaty, Daniel Faria                                       *
******************************************************************************/
package aml.match;

import java.util.HashSet;
import java.util.Set;

import aml.AML;
import aml.ontology.Lexicon;
import aml.ontology.WordLexicon;
import aml.util.StopList;
import aml.util.WordNet;

public class MultiWordMatcher implements PrimaryMatcher
{
	
//Attributes
	
	private WordNet wn;
	//The set of stop words
	private Set<String> stopset;

//Constructors
	
	public MultiWordMatcher()
	{
		wn = new WordNet();
		stopset=StopList.read();
	}

//Public Methods
	
	@Override
	public Alignment match(double thresh)
	{
		System.out.println("Running Multi-Word Matcher");
		long time = System.currentTimeMillis()/1000;
		AML aml = AML.getInstance();
		Lexicon sourceLex = aml.getSource().getLexicon();
		Lexicon targetLex = aml.getTarget().getLexicon();
		WordLexicon sourceWLex = aml.getSource().getWordLexicon();
		WordLexicon targetWLex = aml.getTarget().getWordLexicon();
		
		Alignment maps = new Alignment();
		for(String sName : sourceLex.getNames())
		{
			String[] sWords = sName.split(" ");
			if(sWords.length < 2 || sWords.length > 3)
				continue;
			
			for(String tName : targetLex.getNames())
			{
				if(sName.equals(tName))
					continue;
				String[] tWords = tName.split(" ");
				if(tWords.length < 2 || tWords.length > 3 || sWords.length != tWords.length)
					continue;
				double sim = 0.0;
				for(int i = 0; i < sWords.length; i++)
				{
					String sw = sWords[i];
					String tw = tWords[i];
					if(stopset.contains(sw) || stopset.contains(tw))
						continue;
					if(sw.equals(tw))
					{
						sim++;
						continue;
					}
					HashSet<String> sList = getAllWordForms(sw);
					HashSet<String> tList = getAllWordForms(tw);
					if(sList.contains(tw) || tList.contains(sw))
					{
						sim+=0.8;
						continue;
					}
					if(sourceWLex.getWordEC(sw) < 0.75 && targetWLex.getWordEC(tw) < 0.75)
					{
						double score = wn.wuPalmerScore(sw,tw);
						if(score > 0.5)
							sim+=0.5;
					}
				}
				if(sim < 1.5)
					continue;
				sim /= sWords.length;
				for(Integer srcId : sourceLex.getClasses(sName))
				{
					for(Integer tgtId : targetLex.getClasses(tName))
					{
						double finalSim = sim * sourceLex.getCorrectedWeight(sName, srcId) *
								targetLex.getCorrectedWeight(tName, tgtId);
						if(finalSim < thresh)
							finalSim = thresh;
						maps.add(srcId, tgtId, finalSim);
					}
				}
			}
		}
		time = System.currentTimeMillis()/1000 - time;
		System.out.println("Finished in " + time + " seconds");
		return maps;
	}

//Private Methods
	
	private HashSet<String> getAllWordForms(String s)
	{
		HashSet<String> wordForms = wn.getAllWordForms(s);
		wordForms.addAll(wn.getHypernyms(s));
		return wordForms;
	}
}