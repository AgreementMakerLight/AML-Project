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
* Matches Ontologies by finding literal full-name matches between their       *
* Lexicons after extension with the WordNet.                                  *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.match.bk;

import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import aml.AML;
import aml.alignment.SimpleAlignment;
import aml.alignment.mapping.SimpleMapping;
import aml.match.AbstractHashMatcher;
import aml.ontology.EntityType;
import aml.ontology.Ontology;
import aml.ontology.lexicon.Lexicon;
import aml.ontology.lexicon.StringParser;
import aml.settings.InstanceMatchingCategory;
import aml.util.data.Map2MapComparable;
import aml.util.similarity.WordNet;

public class WordNetMatcher extends AbstractHashMatcher
{
	
//Attributes

	protected static final String DESCRIPTION = "Matches entities that have one or more exact\n" +
			  								  "String matches between a Lexicon entry and a\n" +
			  								  "WordNet synonym or between WordNet synonyms";
	protected static final String NAME = "WordNet Matcher";
	protected static final EntityType[] SUPPORT = {EntityType.CLASS,EntityType.INDIVIDUAL,EntityType.DATA_PROP,EntityType.OBJECT_PROP};
	//The WordNet class
	private WordNet wn;
	//The confidence score of WordNet
	private final double CONFIDENCE = 0.9;
	
//Constructors

	/**
	 * Constructs a new WordNetMatcher
	 */
	public WordNetMatcher()
	{
		wn = new WordNet();
		description = DESCRIPTION;
		name = NAME;
		support = SUPPORT;
	}

//Protected Methods
	
	@Override
	protected SimpleAlignment hashMatch(Ontology o1, Ontology o2, EntityType e, double thresh)
	{
		SimpleAlignment a = new SimpleAlignment(o1,o2);
		AML aml = AML.getInstance();
		System.out.println("Building WordNet Lexicon for " + o1.getURI());
		Map2MapComparable<String,String,Double> source = wordNetLexicon(o1.getLexicon(),e,thresh);
		System.out.println("Building WordNet Lexicon for " + o2.getURI());
		Map2MapComparable<String,String,Double> target = wordNetLexicon(o2.getLexicon(),e,thresh);
		System.out.println("Matching WordNet Lexicons");
		for(String s : source.keySet())
		{
			//Get all term indexes for the name in both ontologies
			Set<String> sIndexes = source.keySet(s);
			Set<String> tIndexes = target.keySet(s);
			if(tIndexes == null)
				continue;
			//Otherwise, match all indexes
			for(String i : sIndexes)
			{
				if(e.equals(EntityType.INDIVIDUAL) && !aml.isToMatchSource(i))
					continue;
				//Get the weight of the name for the term in the source lexicon
				double weight = source.get(s,i);
				for(String j : tIndexes)
				{
					if(e.equals(EntityType.INDIVIDUAL) && (!aml.isToMatchTarget(j) ||
							(aml.getInstanceMatchingCategory().equals(InstanceMatchingCategory.SAME_CLASSES) &&
							!aml.getEntityMap().shareClass(i,j))))
						continue;
					//Get the weight of the name for the term in the target lexicon
					double similarity = target.get(s,j);
					//Then compute the similarity, by multiplying the two weights
					similarity *= weight;
					//If the similarity is above threshold add the mapping
					if(similarity >= thresh)
						a.add(new SimpleMapping(i, j, similarity));
				}
			}
		}
		return a;
	}
	
//Private Methods

	private Map2MapComparable<String,String,Double> wordNetLexicon(Lexicon l, EntityType e, double thresh)
	{
		Map2MapComparable<String,String,Double> wnLexicon = new Map2MapComparable<String,String,Double>();
		//Get the original Lexicon names into a Vector since the
		//Lexicon will be extended during the iteration (otherwise
		//we'd get a concurrentModificationException)
		Vector<String> names = new Vector<String>(l.getNames(e));
		//Iterate through the original Lexicon names
		for(String s : names)
		{
			//We don't match formulas to WordNet
			if(StringParser.isFormula(s))
				continue;
			//Find all wordForms in WordNet for each full name
			HashSet<String> wordForms = wn.getAllNounWordForms(s);
			//If there aren't any, break the name into words
			//(if it is a multi-word name) and look for wordForms
			//of each word
			if(wordForms.size() == 0 && s.contains(" "))
			{
				String[] words = s.split(" ");
				for(String w : words)
				{
					if(w.length() < 3)
						continue;
					HashSet<String> wf = wn.getAllNounWordForms(w);
					if(wf.size() == 0)
						continue;
					for(String f : wf)
						if(!f.contains(" "))
							wordForms.add(s.replace(w, f));
				}
			}
			//If there are still no wordForms, proceed to next name
			if(wordForms.size() == 0)
				continue;
			double conf = CONFIDENCE - 0.01*wordForms.size();
			if(conf < thresh)
				continue;
			Set<String> terms = l.getInternalEntities(e,s);
			//Add each term with the name to the extension Lexicon
			for(String i : terms)
			{
				double weight = conf * l.getWeight(s, i);
				if(weight < thresh)
					continue;
				for(String w : wordForms)
					if(!l.contains(i, w) && weight >= thresh)
						wnLexicon.addUpgrade(i, w, weight);
			}
		}
		return wnLexicon;
	}
}