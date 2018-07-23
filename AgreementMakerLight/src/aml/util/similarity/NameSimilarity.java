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
* Computes the similarity between two entity names using a combination of     *
* methods: 1) Jaccard index between their words; 2) Jaccard index between all *
* WordNet synonyms, when the useWordNet option is on; 3) ISub String          *
* similarity. The maximum of the three methods is returned.                   *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.util.similarity;

import java.util.HashSet;

public class NameSimilarity
{
	private WordNet wn = null;
	
	public NameSimilarity(boolean useWordNet)
	{
		if(useWordNet)
			wn = new WordNet();
	}
	
	/**
	 * Computes the similarity between two names
	 * @param n1: the first name to compare
	 * @param n2: the second name to compare
	 * @param thresh: the similarity threshold
	 * @return the similarity between the two names if it is above the threshold
	 * or 0.0 otherwise
	 */
	public double nameSimilarity(String n1, String n2, double thresh)
	{
		//We don't compare two-character names at all
		if(n1.length() < 3 || n2.length() < 3)
			return 0.0;
		//If the names are equal, no need to compute similarity
		if(n1.equals(n2))
			return 1.0;
		
		//Split the source name into words
		String[] sW = n1.split(" ");
		HashSet<String> sWords = new HashSet<String>();
		HashSet<String> sSyns = new HashSet<String>();
		for(String w : sW)
		{
			sWords.add(w);
			sSyns.add(w);
			//And compute the WordNet synonyms of each word
			if(wn != null && w.length() > 2)
				sSyns.addAll(wn.getAllNounWordForms(w));
		}
		
		//Split the target name into words
		String[] tW = n2.split(" ");
		HashSet<String> tWords = new HashSet<String>();
		HashSet<String> tSyns = new HashSet<String>();		
		for(String w : tW)
		{
			tWords.add(w);
			tSyns.add(w);
			//And compute the WordNet synonyms of each word
			if(wn != null && w.length() > 3)
				tSyns.addAll(wn.getAllWordForms(w));
		}
		
		//Compute the Jaccard word similarity between the properties
		double wordSim = Similarity.jaccard(sWords,tWords)*0.9;
		//and the String similarity
		double simString = ISub.stringSimilarity(n1,n2)*0.9;
		//Combine the two
		double sim = 1 - ((1-wordSim) * (1-simString));
		if(wn != null)
		{
			//Check if the WordNet similarity
			double wordNetSim = Similarity.jaccard(sSyns,tSyns);
			//Is greater than the name similarity
			if(wordNetSim > sim)
				//And if so, return it
				sim = wordNetSim;
		}
		return sim;
	}
}
