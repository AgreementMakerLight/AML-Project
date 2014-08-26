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
* Matches Ontologies by finding partial matches between their names with      *
* either 2 or 3 words. It checks whether the words are equal, synonyms in     *
* WordNet, or have a high Wu Palmer score.                                    *
*                                                                             *
* WARNING: This matching algorithm takes O(N^2) time, and thus should be used *
* only to match small ontologies.                                             *
*                                                                             *
* @author Amruta Nanavaty, Daniel Faria                                       *
* @date 26-08-2014                                                            *
* @version 2.0                                                                *
******************************************************************************/
package aml.match;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import aml.AML;
import aml.ontology.Lexicon;
import aml.ontology.WordLexicon;
import aml.util.StopList;
import edu.cmu.lti.jawjaw.pobj.POS;
import edu.cmu.lti.lexical_db.ILexicalDatabase;
import edu.cmu.lti.lexical_db.NictWordNet;
import edu.cmu.lti.lexical_db.data.Concept;
import edu.cmu.lti.ws4j.RelatednessCalculator;
import edu.cmu.lti.ws4j.impl.WuPalmer;
import edu.smu.tspell.wordnet.NounSynset;
import edu.smu.tspell.wordnet.Synset;
import edu.smu.tspell.wordnet.SynsetType;
import edu.smu.tspell.wordnet.WordNetDatabase;

public class MultiWordMatcher implements PrimaryMatcher
{
	
//Attributes
	
	//WordNet-related variables
	private WordNetDatabase wordNet;
	private final String PATH = "store/knowledge/wordnet/";
	private ILexicalDatabase db;		
	private RelatednessCalculator wup;
	//The set of stop words
	private Set<String> stopset;

//Constructors
	
	public MultiWordMatcher()
	{
		String path = new File(PATH).getAbsolutePath();
		System.setProperty("wordnet.database.dir", path);
		wordNet = WordNetDatabase.getFileInstance();
		db = new NictWordNet();
		wup = new WuPalmer(db);
		stopset=StopList.read();
	}

//Public Methods
	
	@Override
	public Alignment match(double thresh)
	{
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
						double score = wuPalmerScore(sw,tw);
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
		return maps;
	}

//Private Methods
	
	private HashSet<String> getAllWordForms(String s)
	{
		HashSet<String> wordForms = new HashSet<String>();
		
		Synset[] synsets = wordNet.getSynsets(s, SynsetType.NOUN); 
		for(Synset ss : synsets )
		{ 
			NounSynset ns = (NounSynset)ss; 
			NounSynset[] hypernyms = ns.getHypernyms();
			for(NounSynset hs : hypernyms)
			{
		    	String[] wf = hs.getWordForms();
		    	for(String w : wf)
		    		if(!w.contains(" ") && !w.contains("-"))
		    			wordForms.add(w.toLowerCase());
		    }
		}		
		//Look for the name on WordNet
		synsets = wordNet.getSynsets(s);
		//For each Synset found
		for(Synset ss : synsets)
		{
			//Get the WordForms
			String[] words = ss.getWordForms();
			//And add each one to the Lexicon
			for(String w : words)
				if(!w.contains(" ") && !w.contains("-"))
					wordForms.add(w.toLowerCase());
		}
		return wordForms;
	}

	private double wuPalmerScore(String src, String trg)
	{
		double maxScore = 0.0;
	    List<Concept> synsets1 = (List<Concept>)db.getAllConcepts(src,POS.n.name());
	    List<Concept> synsets2 = (List<Concept>)db.getAllConcepts(trg,POS.n.name());

	    for(Concept synset1: synsets1)
	    {
	    	for (Concept synset2: synsets2)
	        {
	            double score = wup.calcRelatednessOfSynset(synset1, synset2).getScore();
	            if (score > maxScore) 
	                maxScore = score;
	        }
	    }
		return maxScore;
	}
}