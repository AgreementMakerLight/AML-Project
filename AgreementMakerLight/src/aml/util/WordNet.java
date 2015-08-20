/******************************************************************************
* Copyright 2013-2015 LASIGE                                                  *
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
* Utility class for getting synonyms and hypernyms from WordNet and for       *
* computing Wu-Palmer similarity between two words.                           *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 26-05-2015                                                            *
******************************************************************************/
package aml.util;

import java.io.File;
import java.util.HashSet;

import edu.smu.tspell.wordnet.NounSynset;
import edu.smu.tspell.wordnet.Synset;
import edu.smu.tspell.wordnet.SynsetType;
import edu.smu.tspell.wordnet.WordNetDatabase;

public class WordNet
{
	
//Attributes

	//The WordNet Interface
	private WordNetDatabase wordNet;
	//The path to the WordNet database
	private final String PATH = "store/knowledge/wordnet/";
	
//Constructors

	public WordNet()
	{
		//Setup the wordnet database directory
		String path = new File(PATH).getAbsolutePath();
		System.setProperty("wordnet.database.dir", path);
		//Instantiate WordNet
		wordNet = WordNetDatabase.getFileInstance();
	}

//Public Methods

	/**
	 * @param s: the String to search in WordNet
	 * @return the set of noun word forms for the given String
	 */
	public HashSet<String> getAllNounWordForms(String s)
	{
		HashSet<String> wordForms = new HashSet<String>();

		//Look for the name on WordNet
		Synset[] synsets = wordNet.getSynsets(s,SynsetType.NOUN);
		//For each Synset found
		for(Synset ss : synsets)
		{
			//Get the WordForms
			String[] words = ss.getWordForms();
			for(String w : words)
				if(!w.trim().equals(""))
					wordForms.add(w);
		}
		return wordForms;
	}
	
	/**
	 * @param s: the String to search in WordNet
	 * @return the set of word forms for the given String
	 */
	public HashSet<String> getAllWordForms(String s)
	{
		HashSet<String> wordForms = new HashSet<String>();

		//Look for the name on WordNet
		Synset[] synsets = wordNet.getSynsets(s);
		//For each Synset found
		for(Synset ss : synsets)
		{
			//Get the WordForms
			String[] words = ss.getWordForms();
			for(String w : words)
				if(!w.trim().equals(""))
					wordForms.add(w);
		}
		return wordForms;
	}
	
	/**
	 * @param s: the String to search in WordNet
	 * @return the set of hypernyms for the given String
	 */
	public HashSet<String> getHypernyms(String s)
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
					if(!w.trim().equals(""))
						wordForms.add(w);
		    }
		}
		return wordForms;
	}
	
	/**
	 * Computes the Wu-Palmer score between two terms
	 * @param s: the source term
	 * @param t: the target term
	 * @return the Wu-Palmer score between s and t
	 */
	public double wuPalmerScore(String s, String t)
	{
		if(s.equals(t))
			return 1.0;
		Synset[] sources = wordNet.getSynsets(s, SynsetType.NOUN);
		Synset[] targets = wordNet.getSynsets(t, SynsetType.NOUN);
		if(sources.length == 0 || targets.length == 0)
			return 0.0;
		for(Synset ss : sources)
			for(Synset ts : targets)
				if(ss.equals(ts))
					return 1.0;
		return getLowestCommonAncestor(s,t)*2.0/
				(getMinRootDistance(s)+getMinRootDistance(t));
	}
	
//Private Methods
	
	private int getLowestCommonAncestor(String s, String t)
	{
		HashSet<NounSynset> lowest = new HashSet<NounSynset>();
		
		HashSet<NounSynset> sSet = new HashSet<NounSynset>();
		for(Synset ss : wordNet.getSynsets(s, SynsetType.NOUN))
			sSet.add((NounSynset)ss);
		HashSet<NounSynset> totalSSet = new HashSet<NounSynset>(sSet);
		
		HashSet<NounSynset> tSet = new HashSet<NounSynset>();
		for(Synset ss : wordNet.getSynsets(t, SynsetType.NOUN))
			tSet.add((NounSynset)ss);
		HashSet<NounSynset> totalTSet = new HashSet<NounSynset>(tSet);
		
		int size = 0;
		boolean done = false;
		while(size != totalSSet.size()+totalTSet.size() && !done)
		{
			size = totalSSet.size()+totalTSet.size();
			HashSet<NounSynset> parentSSet = new HashSet<NounSynset>();
			for(NounSynset ns : sSet)
			{ 
				NounSynset[] hypernyms = ns.getHypernyms();
				for(NounSynset ss : hypernyms)
				{
					if(totalTSet.contains(ss))
					{
						lowest.add(ss);
						done = true;
					}
					parentSSet.add(ss);
					totalSSet.add(ss);
				}
			}
			sSet = new HashSet<NounSynset>(parentSSet);
			HashSet<NounSynset> parentTSet = new HashSet<NounSynset>();
			for(NounSynset ns : tSet)
			{ 
				NounSynset[] hypernyms = ns.getHypernyms();
				for(NounSynset ss : hypernyms)
				{
					if(totalSSet.contains(ss))
					{
						lowest.add(ss);
						done = true;
					}
					parentTSet.add(ss);
					totalTSet.add(ss);
				}
			}
			tSet = new HashSet<NounSynset>(parentTSet);
		}
		if(lowest.size() > 0)
			return getMinRootDistance(lowest);
		else
			return 0;
	}
	
	private int getMinRootDistance(String s)
	{
		HashSet<NounSynset> synset = new HashSet<NounSynset>();
		for(Synset ss : wordNet.getSynsets(s, SynsetType.NOUN))
			synset.add((NounSynset)ss);
		HashSet<NounSynset> checkSet = new HashSet<NounSynset>(synset);
		int distance = 1;
		while(true)
		{
			HashSet<NounSynset> parentSet = new HashSet<NounSynset>();
			for(NounSynset ns : synset)
			{ 
				NounSynset[] hypernyms = ns.getHypernyms();
				if(hypernyms.length == 0)
					return distance;
				for(NounSynset ss : hypernyms)
				{
					if(!checkSet.contains(ss))
					{
						parentSet.add(ss);
						checkSet.add(ss);
					}
				}
			}
			synset = new HashSet<NounSynset>(parentSet);
		}
	}
	
	private int getMinRootDistance(HashSet<NounSynset> s)
	{
		HashSet<NounSynset> synset = new HashSet<NounSynset>(s);
		HashSet<NounSynset> checkSet = new HashSet<NounSynset>(s);
		int distance = 1;
		while(true)
		{
			HashSet<NounSynset> parentSet = new HashSet<NounSynset>();
			for(NounSynset ns : synset)
			{ 
				NounSynset[] hypernyms = ns.getHypernyms();
				if(hypernyms.length == 0)
					return distance;
				for(NounSynset ss : hypernyms)
				{
					if(!checkSet.contains(ss))
					{
						parentSet.add(ss);
						checkSet.add(ss);
					}
				}
			}
			synset = new HashSet<NounSynset>(parentSet);
		}
	}
}