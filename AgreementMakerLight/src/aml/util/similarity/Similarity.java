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
* Metrics for measuring similarity between collections and/or lists.          *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.util.similarity;

import java.util.Collection;
import java.util.HashSet;

import uk.ac.shef.wit.simmetrics.similaritymetrics.JaroWinkler;
import uk.ac.shef.wit.simmetrics.similaritymetrics.Levenshtein;
import uk.ac.shef.wit.simmetrics.similaritymetrics.QGramsDistance;

public class Similarity
{

	/**
	 * Computes the Jaccard similarity between two Collections of Objects
	 * @param <X>
	 * @param c1: the first Collection 
	 * @param c2: the second Collection
	 * @return the Jaccard similarity between c1 and c2
	 */
	public static <X extends Object> double jaccardSimilarity(Collection<X> c1, Collection<X> c2)
	{
		if(c1.size() == 0 || c2.size() == 0)
			return 0.0;
		double intersection = 0.0;
		double union = 0.0;
		for(Object o : c1)
		{
			if(c2.contains(o))
				intersection++;
			else
				union++;
		}
		union += c2.size();
		return intersection/union;
	}
	
	/**
	 * Computes the similarity between two names using a combination of String-, Word-
	 * and WordNet-Similarity measures
	 * @param n1: the first name to compare
	 * @param n2: the second name to compare
	 * @param useWord: whether to use the WordNet
	 * @return the similarity between the two names
	 */
	public static double nameSimilarity(String n1, String n2, boolean useWordNet)
	{
		WordNet wn = null;
		if(useWordNet)
			wn = new WordNet();
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
		double wordSim = Similarity.jaccardSimilarity(sWords,tWords)*0.9;
		//and the String similarity
		double simString = ISub.stringSimilarity(n1,n2)*0.9;
		//Combine the two
		double sim = 1 - ((1-wordSim) * (1-simString));
		if(wn != null)
		{
			//Check if the WordNet similarity
			double wordNetSim = Similarity.jaccardSimilarity(sSyns,tSyns);
			//Is greater than the name similarity
			if(wordNetSim > sim)
				//And if so, return it
				sim = wordNetSim;
		}
		return sim;
	}
	
	/**
	 * Computes the string the similarity between two Strings
	 * @param s: the first String
	 * @param t: the second String
	 * @param measure: the String Similarity Measure
	 * @return
	 */
	public static double stringSimilarity(String s, String t, StringSimMeasure measure)
	{
		double sim = 0.0;
		if(measure.equals(StringSimMeasure.ISUB))
			sim = ISub.stringSimilarity(s,t);
		else if(measure.equals(StringSimMeasure.EDIT))
		{
			Levenshtein lv = new Levenshtein();
			sim = lv.getSimilarity(s, t);
		}
		else if(measure.equals(StringSimMeasure.JW))
		{
			JaroWinkler jv = new JaroWinkler();
			sim = jv.getSimilarity(s, t);
		}
		else if(measure.equals(StringSimMeasure.QGRAM))
		{
			QGramsDistance q = new QGramsDistance();
			sim = q.getSimilarity(s, t);
		}
		return sim;
	}
}