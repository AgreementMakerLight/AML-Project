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
* Matches Ontologies by finding partial matches between their names with      *
* either 2 or 3 words. It checks whether the words are equal, synonyms in     *
* WordNet, or have a high Wu-Palmer score.                                    *
*                                                                             *
* WARNING: This matching algorithm takes O(N^2) time, and thus should be used *
* only to match small ontologies.                                             *
*                                                                             *
* @author Daniel Faria, Amruta Nanavaty                                       *
******************************************************************************/
package aml.match.bk;

import java.util.HashSet;
import java.util.Set;

import aml.alignment.SimpleAlignment;
import aml.match.AbstractParallelMatcher;
import aml.ontology.EntityType;
import aml.ontology.Ontology;
import aml.ontology.lexicon.Lexicon;
import aml.ontology.lexicon.StopList;
import aml.ontology.lexicon.WordLexicon;
import aml.util.similarity.WordNet;

public class MultiWordMatcher extends AbstractParallelMatcher
{
	
//Attributes
	
	protected static final String DESCRIPTION = "Matches entities that have English Lexicon\n" +
											  "entries with two words where one word is\n"+
											  "shared between them and the other word is\n" + 
											  "related through WordNet.";
	protected static final String NAME = "Multi-Word Matcher";
	protected static final EntityType[] SUPPORT = {EntityType.CLASS,EntityType.INDIVIDUAL,EntityType.DATA_PROP,EntityType.OBJECT_PROP};
	private WordNet wn;
	//The set of stop words
	private Set<String> stopset;
	//The Lexicons
	private Lexicon sourceLex, targetLex;
	private WordLexicon sourceWLex, targetWLex;
	//The language
	private static final String LANG = "en";
	//The confidence score
	private final double CONFIDENCE = 0.9;

//Constructors
	
	public MultiWordMatcher()
	{
		super();
		wn = new WordNet();
		stopset=StopList.read();
	}

//Public Methods
	
	@Override
	public SimpleAlignment match(Ontology o1, Ontology o2, EntityType e, double thresh)
	{
		sourceLex = o1.getLexicon();
		targetLex = o2.getLexicon();
		sourceWLex = o1.getWordLexicon(e, LANG);
		targetWLex = o2.getWordLexicon(e, LANG);
		
		System.out.println("Running Multi-Word Matcher");
		long time = System.currentTimeMillis()/1000;
		SimpleAlignment a = super.match(o1, o2, e, thresh);
		time = System.currentTimeMillis()/1000 - time;
		System.out.println("Finished in " + time + " seconds");
		return a;
	}

//Protected Methods
	
	@Override
	protected double mapTwoEntities(String uri1, String uri2)
	{
		double maxSim = 0.0;
		for(String sName : sourceLex.getNamesWithLanguage(uri1,LANG))
		{
			String[] sWords = sName.split(" ");
			if(sWords.length < 2 || sWords.length > 3)
				continue;
			
			for(String tName : targetLex.getNamesWithLanguage(uri2,LANG))
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
				double finalSim = sim * sourceLex.getCorrectedWeight(sName, uri1) *
								targetLex.getCorrectedWeight(tName, uri2);
				if(finalSim > maxSim)
					maxSim = finalSim;
			}
		}
		return maxSim*CONFIDENCE;
	}
	
//Private Methods
	
	private HashSet<String> getAllWordForms(String s)
	{
		HashSet<String> wordForms = wn.getAllWordForms(s);
		wordForms.addAll(wn.getHypernyms(s));
		return wordForms;
	}
}