/******************************************************************************
* Copyright 2013-2016 LASIGE                                                  *
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
******************************************************************************/
package aml.match;

import java.util.HashSet;
import java.util.Set;

import aml.AML;
import aml.alignment.Alignment;
import aml.ontology.EntityType;
import aml.ontology.lexicon.Lexicon;
import aml.ontology.lexicon.StringParser;
import aml.settings.InstanceMatchingCategory;
import aml.settings.LanguageSetting;

public class LexicalMatcher implements PrimaryMatcher
{
	
//Attributes
	
	private static final String DESCRIPTION = "Matches entities that have one or more exact\n" +
											  "String matches between their Lexicon entries";
	private static final String NAME = "Lexical Matcher";
	private static final EntityType[] SUPPORT = {EntityType.CLASS,EntityType.INDIVIDUAL,EntityType.DATA,EntityType.OBJECT};
		
//Constructors

	public LexicalMatcher(){}
	
//Public Methods
	
	@Override
	public String getDescription()
	{
		return DESCRIPTION;
	}

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public EntityType[] getSupportedEntityTypes()
	{
		return SUPPORT;
	}

	@Override
	public Alignment match(EntityType e, double thresh) throws UnsupportedEntityTypeException
	{
		checkEntityType(e);
		System.out.println("Running Lexical Matcher");
		long time = System.currentTimeMillis()/1000;
		//Get the lexicons of the source and target Ontologies
		AML aml = AML.getInstance();
		Lexicon sLex = aml.getSource().getLexicon();
		Lexicon tLex = aml.getTarget().getLexicon();
		//Initialize the alignment
		Alignment maps = new Alignment();
		//To minimize iterations, we want to iterate through the
		//Ontology with the smallest Lexicon
		boolean sourceIsSmaller = (sLex.nameCount(e) <= tLex.nameCount(e));
		Set<String> names;
		if(sourceIsSmaller)
			names = sLex.getNames(e);
		else
			names = tLex.getNames(e);
		
		//If we have a multi-language Lexicon, we must match language by language
		if(aml.getLanguageSetting().equals(LanguageSetting.MULTI))
		{
			for(String s : names)
			{
				HashSet<String> languages = new HashSet<String>();
				for(String l : sLex.getLanguages(e,s))
					if(tLex.getLanguages().contains(l))
						languages.add(l);
				
				for(String l : languages)
				{
					//Get all term indexes for the name in both ontologies
					Set<Integer> sourceIndexes = sLex.getEntitiesWithLanguage(e,s,l);
					Set<Integer> targetIndexes = tLex.getEntitiesWithLanguage(e,s,l);
					//If the name doesn't exist in either ontology, skip it
					if(sourceIndexes == null || targetIndexes == null)
						continue;
					//Otherwise, match all indexes
					for(Integer i : sourceIndexes)
					{
						if(e.equals(EntityType.INDIVIDUAL) && !aml.isToMatchSource(i))
							continue;
						//Get the weight of the name for the term in the smaller lexicon
						double weight = sLex.getCorrectedWeight(s, i);
						for(Integer j : targetIndexes)
						{
							if(e.equals(EntityType.INDIVIDUAL) && (!aml.isToMatchTarget(j) ||
									(aml.getInstanceMatchingCategory().equals(InstanceMatchingCategory.SAME_CLASSES) &&
									!aml.getRelationshipMap().shareClass(i,j))))
								continue;
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
				boolean isSmallFormula = StringParser.isFormula(s) && s.length() < 10;
				Set<Integer> sourceIndexes = sLex.getEntities(e,s);
				Set<Integer> targetIndexes = tLex.getEntities(e,s);
				//If the name doesn't exist in either ontology, skip it
				if(sourceIndexes == null || targetIndexes == null)
					continue;
				//Otherwise, match all indexes
				for(Integer i : sourceIndexes)
				{
					if(e.equals(EntityType.INDIVIDUAL) && !aml.isToMatchSource(i))
						continue;
					if(isSmallFormula && sLex.containsNonSmallFormula(i))
						continue;
					//Get the weight of the name for the term in the smaller lexicon
					double weight = sLex.getCorrectedWeight(s, i);
					for(Integer j : targetIndexes)
					{
						if(e.equals(EntityType.INDIVIDUAL) && (!aml.isToMatchTarget(j) ||
								(aml.getInstanceMatchingCategory().equals(InstanceMatchingCategory.SAME_CLASSES) &&
								!aml.getRelationshipMap().shareClass(i,j))))
							continue;
						if(isSmallFormula && tLex.containsNonSmallFormula(j))
							continue;
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
		//And match them
		time = System.currentTimeMillis()/1000 - time;
		System.out.println("Finished in " + time + " seconds");
		return maps;
	}
	
//Private Methods
	
	private void checkEntityType(EntityType e) throws UnsupportedEntityTypeException
	{
		boolean check = false;
		for(EntityType t : SUPPORT)
		{
			if(t.equals(e))
			{
				check = true;
				break;
			}
		}
		if(!check)
			throw new UnsupportedEntityTypeException(e.toString());
	}
}