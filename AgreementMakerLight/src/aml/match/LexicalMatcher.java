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
* Matches two Ontologies by finding literal full-name matches between their   *
* Lexicons. Weighs matches according to the provenance of the names.          *
* Ignores external Lexicon names when in internal mode.                       *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 22-10-2013                                                            *
******************************************************************************/
package aml.match;

import java.util.Set;

import aml.ontology.Lexicon;
import aml.ontology.Ontology;


public class LexicalMatcher implements Matcher
{
	
//Attributes
	
	//Whether to ignore external names or not
	private boolean internal;
	
//Constructors

	/**
	 * Constructs a new LexicalMatcher with the given options
	 * @param i: Whether the Matcher will be internal and ignore
	 * external names in the Lexicon
	 */
	public LexicalMatcher(boolean i)
	{
		internal = i;
	}
	
//Public Methods
	
	@Override
	public Alignment extendAlignment(Alignment a, double thresh)
	{
		Ontology source = a.getSource();
		Ontology target = a.getTarget();
		Alignment maps = match(source,target,thresh);
		for(Mapping m : maps)
			if(a.containsConflict(m))
				maps.remove(m);
		return maps;
	}
	
	@Override
	public Alignment match(Ontology source, Ontology target, double thresh)
	{
		//Get the lexicons of both Ontologies
		Lexicon sLex = source.getLexicon();
		Lexicon tLex = target.getLexicon();
		
		//Initialize the alignment
		Alignment maps = new Alignment(source,target);

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
		//Get the smaller Ontology names
		Set<String> names = smaller.getNames();
		for(String s : names)
		{
			//Get all term indexes for the name in both ontologies
			Set<Integer> largerIndexes;
			Set<Integer> smallerIndexes;
			//If in internal mode consider only terms for which the name is local
			if(internal)
			{
				largerIndexes = larger.getInternalTerms(s);
				smallerIndexes = smaller.getInternalTerms(s);
			}
			//Otherwise consider all terms
			else
			{
				largerIndexes = larger.getTerms(s);
				smallerIndexes = smaller.getTerms(s);
			}
			//If the name doesn't exist in either ontology, skip it
			//(it may not exist in the smaller ontology when in internal mode)
			if(largerIndexes == null || smallerIndexes == null)
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
		return maps;
	}
}