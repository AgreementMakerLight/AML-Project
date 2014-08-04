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
* @date 23-06-2014                                                            *
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
		//Get the lexicons of the source and target Ontologies
		AML aml = AML.getInstance();
		Lexicon sLex = aml.getSource().getLexicon();
		Lexicon tLex = aml.getTarget().getLexicon();
		//And match them
		return match(sLex,tLex,thresh);
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
		Lexicon larger, smaller;
		boolean sourceIsSmaller = (sLex.nameCount() <= tLex.nameCount());
		double weight, similarity;
		if(sourceIsSmaller)
		{
			smaller = sLex;
			larger = tLex;
		}
		else
		{
			smaller = tLex;
			larger = sLex;
		}
		//If we have a multi-language Lexicon, we must match language by language
		if(AML.getInstance().getLanguageSetting().equals(LanguageSetting.MULTI))
		{
			//Get the smaller Ontology names
			Set<String> names = smaller.getNames();
			for(String s : names)
			{
				Set<String> languages = smaller.getLanguages(s);
				languages.addAll(larger.getLanguages(s));
				
				for(String l : languages)
				{
					//Get all term indexes for the name in both ontologies
					Set<Integer> smallerIndexes = smaller.getClassesWithLanguage(s,l);
					Set<Integer> largerIndexes = larger.getClassesWithLanguage(s,l);
					//If the name doesn't exist in either ontology, skip it
					if(smallerIndexes == null || largerIndexes == null)
						continue;
					//Otherwise, match all indexes
					for(Integer i : smallerIndexes)
					{
						//Get the weight of the name for the term in the smaller lexicon
						weight = smaller.getCorrectedWeight(s, i);
						for(Integer j : largerIndexes)
						{
							//Get the weight of the name for the term in the larger lexicon
							similarity = larger.getCorrectedWeight(s, j);
							//Then compute the similarity, by multiplying the two weights
							similarity *= weight;
							//If the similarity is above threshold
							if(similarity >= thresh)
							{
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
								maps.add(sourceId, targetId, similarity);
							}
						}
					}
				}
			}
		}
		//Otherwise we can just match everything
		else
		{
			//Get the smaller Ontology names
			Set<String> names = smaller.getNames();
			for(String s : names)
			{
				//Get all term indexes for the name in both ontologies
				Set<Integer> smallerIndexes = smaller.getClasses(s);
				Set<Integer> largerIndexes = larger.getClasses(s);
				//If the name doesn't exist in the larger ontology, skip it
				if(largerIndexes == null)
					continue;
				//Otherwise, match all indexes
				for(Integer i : smallerIndexes)
				{
					//Get the weight of the name for the term in the smaller lexicon
					weight = smaller.getCorrectedWeight(s, i);
					for(Integer j : largerIndexes)
					{
						//Get the weight of the name for the term in the larger lexicon
						similarity = larger.getCorrectedWeight(s, j);
						//Then compute the similarity, by multiplying the two weights
						similarity *= weight;
						//If the similarity is above threshold
						if(similarity >= thresh)
						{
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
							maps.add(sourceId, targetId, similarity);
						}
					}
				}
			}
		}
		return maps;
	}
}