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
* Matches Ontologies by finding literal full-name matches between their       *
* Lexicons. Weighs matches according to the provenance of the names.          *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 12-08-2014                                                            *
* @version 2.0                                                                *
******************************************************************************/
package aml.match;

import java.util.Set;

import aml.AML;
import aml.AML.LanguageSetting;
import aml.ontology.Lexicon;

public class LexicalMatcher implements PrimaryMatcher
{
	
//Constructors

	/**
	 * Constructs a new LexicalMatcher
	 */
	public LexicalMatcher(){}
	
//Public Methods
	
	@Override
	public Alignment match(double thresh)
	{
		System.out.println("Running Lexical Matcher");
		long time = System.currentTimeMillis()/1000;
		//Get the lexicons of the source and target Ontologies
		AML aml = AML.getInstance();
		Lexicon sLex = aml.getSource().getLexicon();
		Lexicon tLex = aml.getTarget().getLexicon();
		//And match them
		Alignment a = match(sLex,tLex,thresh);
		time = System.currentTimeMillis()/1000 - time;
		System.out.println("Finished in " + time + " seconds");
		return a;
	}
	
	/**
	 * Matches two Lexicons
	 * @param sLex: the source Lexicon to match
	 * @param tLex: the target Lexicon to match
	 * @param thresh: the similarity threshold
	 * @return the Alignment between the ontologies containing
	 * the two Lexicons
	 */
	public Alignment match(Lexicon sLex, Lexicon tLex, double thresh)
	{
		//Initialize the alignment
		Alignment maps = new Alignment();
		//To minimize iterations, we want to iterate through the
		//Ontology with the smallest Lexicon
		boolean sourceIsSmaller = (sLex.nameCount() <= tLex.nameCount());
		Set<String> names;
		if(sourceIsSmaller)
			names = sLex.getNames();
		else
			names = tLex.getNames();
		
		//If we have a multi-language Lexicon, we must match language by language
		if(AML.getInstance().getLanguageSetting().equals(LanguageSetting.MULTI))
		{
			for(String s : names)
			{
				Set<String> languages = sLex.getLanguages(s);
				languages.addAll(tLex.getLanguages(s));
				
				for(String l : languages)
				{
					//Get all term indexes for the name in both ontologies
					Set<Integer> sourceIndexes = sLex.getClassesWithLanguage(s,l);
					Set<Integer> targetIndexes = tLex.getClassesWithLanguage(s,l);
					//If the name doesn't exist in either ontology, skip it
					if(sourceIndexes == null || targetIndexes == null)
						continue;
					//Otherwise, match all indexes
					for(Integer i : sourceIndexes)
					{
						//Get the weight of the name for the term in the smaller lexicon
						double weight = sLex.getCorrectedWeight(s, i);
						for(Integer j : targetIndexes)
						{
							//Get the weight of the name for the term in the larger lexicon
							double similarity = tLex.getCorrectedWeight(s, j);
							//Then compute the similarity, by multiplying the two weights
							similarity *= weight;
							//If the similarity is above threshold
							if(similarity >= thresh)
								maps.add(i, j, similarity);
						}
					}
				}
			}
		}
		//Otherwise we can just match everything
		else
		{
			for(String s : names)
			{
				Set<Integer> sourceIndexes = sLex.getClasses(s);
				Set<Integer> targetIndexes = tLex.getClasses(s);
				//If the name doesn't exist in either ontology, skip it
				if(sourceIndexes == null || targetIndexes == null)
					continue;
				//Otherwise, match all indexes
				for(Integer i : sourceIndexes)
				{
					//Get the weight of the name for the term in the smaller lexicon
					double weight = sLex.getCorrectedWeight(s, i);
					for(Integer j : targetIndexes)
					{
						//Get the weight of the name for the term in the larger lexicon
						double similarity = tLex.getCorrectedWeight(s, j);
						//Then compute the similarity, by multiplying the two weights
						similarity *= weight;
						//If the similarity is above threshold
						if(similarity >= thresh)
							maps.add(i, j, similarity);
					}
				}
			}
		}
		return maps;
	}
}