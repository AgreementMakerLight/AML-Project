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
* A matching algorithm that derives a thesaurus from the Lexicon of each of   *
* input ontologies, then uses that thesaurus to create a temporary extended   *
* Lexicon which is used to match the ontologies.                              *
*                                                                             *
* @authors Catia Pesquita, Daniel Faria                                       *
******************************************************************************/
package aml.match.lexical;

import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import aml.AML;
import aml.alignment.SimpleAlignment;
import aml.alignment.SimpleMapping;
import aml.match.AbstractMatcher;
import aml.match.PrimaryMatcher;
import aml.ontology.EntityType;
import aml.ontology.Ontology;
import aml.ontology.lexicon.Lexicon;
import aml.ontology.lexicon.StringParser;
import aml.settings.InstanceMatchingCategory;
import aml.util.data.Map2List;
import aml.util.data.Map2MapComparable;

public class ThesaurusMatcher extends AbstractMatcher implements PrimaryMatcher
{
	
//Attributes
	
	protected static final String DESCRIPTION = "Matches entities that have one or more exact\n" +
											  "String matches between their Lexicon entries\n" +
											  "after extension using an internally generated\n" +
											  "Thesaurus";
	protected static final String NAME = "WordNet Matcher";
	protected static final EntityType[] SUPPORT = {EntityType.CLASS,EntityType.INDIVIDUAL,EntityType.DATA_PROP,EntityType.OBJECT_PROP};
	//The confidence score
	private final double CONFIDENCE = 0.85;
	//The Thesaurus of synonym words (String,String)
	public Map2List<String,String> thesaurus;
	
//Constructors
	
	public ThesaurusMatcher(){}

//Public Methods
	
	@Override
	public SimpleAlignment match(Ontology o1, Ontology o2, EntityType e, double thresh)
	{
		SimpleAlignment maps = new SimpleAlignment();
		if(!checkEntityType(e))
			return maps;
		System.out.println("Running Thesaurus Matcher");
		long time = System.currentTimeMillis()/1000;
		AML aml = AML.getInstance();
		Map2MapComparable<String,String,Double> source = thesaurusLexicon(o1,e);
		Map2MapComparable<String,String,Double> target = thesaurusLexicon(o2,e);
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
				//Get the weight of the name for the term in the smaller lexicon
				double weight = source.get(s,i);
				for(String j : tIndexes)
				{
					if(e.equals(EntityType.INDIVIDUAL) && (!aml.isToMatchTarget(j) ||
							(aml.getInstanceMatchingCategory().equals(InstanceMatchingCategory.SAME_CLASSES) &&
							!aml.getEntityMap().shareClass(i,j))))
						continue;
					//Get the weight of the name for the term in the larger lexicon
					double similarity = target.get(s,j);
					//Then compute the similarity, by multiplying the two weights
					similarity *= weight;
					//If the similarity is above threshold add the mapping
					if(similarity >= thresh)
						maps.add(new SimpleMapping(i, j, similarity));
				}
			}
		}
		time = System.currentTimeMillis()/1000 - time;
		System.out.println("Finished in " + time + " seconds");
		return maps;	
	}
	
//Private Methods
	
	//Extends a Lexicon with Thesaurus synonyms
	private Map2MapComparable<String,String,Double> thesaurusLexicon(Ontology o, EntityType e)
	{
		Map2MapComparable<String,String,Double> thesaurusLexicon = new Map2MapComparable<String,String,Double>();
		//Get the entities and the Lexicon
		Set<String> ents = o.getEntities(e);
		Lexicon lex = o.getLexicon();
		//Step 1 - Build the Thesaurus
		thesaurus = new Map2List<String,String>();
		//And compute the synonyms for each entity
		for(String i : ents)
			addSynonymTerms(lex.getInternalNames(i));

		//Step 2 - Extend the Lexicon
		Set<String> names = new HashSet<String>(lex.getNames(e));
		//For each name in the Lexicon
		for(String n: names)
		{
			//If it is a formula, skip to the next name
			if(StringParser.isFormula(n))
				continue;
			//Otherwise, for each entry in the Thesaurus
			for(String s: thesaurus.keySet())
			{
				//If the entry is not contained in the name, skip to next entry
				if(!n.contains(s))
					continue;
				//Otherwise, get the Thesaurus synonyms for that entry
				Vector<String> thesEntries = thesaurus.get(s);
				//For each Thesaurus synonym, create a new synonym in the Lexicon
				Set<String> terms = lex.getInternalEntities(EntityType.CLASS,n);
				for(String t: thesEntries)
				{
					String newName = n.replace(s,t);
					for(String i: terms)
					{
						double weight = lex.getWeight(n,i) * CONFIDENCE;
						if(!lex.contains(i, newName))
							thesaurusLexicon.addUpgrade(newName, i, weight);
					}
				}
			}
		}
		return thesaurusLexicon;
	}
	
	//Adds entries to the Thesaurus based on a set of names for a concept
	private void addSynonymTerms(Set<String> names)
	{
		//Compare the set of names pairwise
		String[] namesarray = names.toArray(new String[0]);
		for(int i = 0; i < namesarray.length; i++)
			for(int j = i; j < namesarray.length; j++)
				compareSynonyms(namesarray[i], namesarray[j]);
	}
	
	//Extracts subconcept synonyms from two given synonyms
	private void compareSynonyms(String synonym1, String synonym2)
	{
		//Step 0. Check if either synonym is a formula, and if so, return
		if(StringParser.isFormula(synonym1) || StringParser.isFormula(synonym2))
			return;
			
		//Step 1. Setup
		//Split the synonyms
		String[] words1 = synonym1.split(" ");
		String[] words2 = synonym2.split(" ");
		//Check if they have the same number of words
		if(words1.length != words2.length)
			return;

		//Step 2. Comparison
		//Check that only one word is different between the two synonyms
		//Trivial case - one-word names
		if(words1.length == 1)
		{
			thesaurus.add(synonym1, synonym2);
			thesaurus.add(synonym2, synonym1);
			return;
		}
		//Multi-word names
		int index = -1;
		for(int i = 0; i < words1.length; i++)
		{
			if(!words1[i].equals(words2[i]))
			{
				//More than one mismatch
				if(index != -1)
					return;
				else
					index = i;
			}
		}
		if(index != -1 && words1[index].length() > 2 && words2[index].length() > 2)
		{
			thesaurus.add(words1[index], words2[index]);
			thesaurus.add(words2[index], words1[index]);
		}
	}
}