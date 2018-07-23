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
* Lexicons. Weighs matches according to the provenance of the names.          *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.match.lexical;


import aml.AML;
import aml.alignment.Alignment;
import aml.match.PrimaryMatcher;
import aml.match.UnsupportedEntityTypeException;
import aml.ontology.EntityType;
import aml.ontology.Ontology;
import aml.ontology.lexicon.Lexicon;
import aml.settings.InstanceMatchingCategory;
import aml.util.data.Map2Set;

public class SpacelessLexicalMatcher implements PrimaryMatcher
{
	
//Attributes
	
	private static final String DESCRIPTION = "Matches entities that have one or more exact\n" +
											  "String matches between their Lexicon entries\n" +
											  "after removing their white spaces";
	private static final String NAME = "Lexical Matcher";
	private static final EntityType[] SUPPORT = {EntityType.CLASS,EntityType.INDIVIDUAL,EntityType.DATA_PROP,EntityType.OBJECT_PROP};
	private static final double WEIGHT = 0.99;
		
//Constructors

	public SpacelessLexicalMatcher(){}
	
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
	public Alignment match(Ontology o1, Ontology o2, EntityType e, double thresh) throws UnsupportedEntityTypeException
	{
		checkEntityType(e);
		System.out.println("Running Spaceless Lexical Matcher");
		long time = System.currentTimeMillis()/1000;
		//Get the lexicons of the source and target Ontologies
		AML aml = AML.getInstance();
		Lexicon sLex = o1.getLexicon();
		Lexicon tLex = o2.getLexicon();
		//Create spaceless lexicons
		Map2Set<String,String> sourceConv = new Map2Set<String,String>();
		for(String n : sLex.getNames(e))
			sourceConv.add(n.replace(" ", ""), n);
		Map2Set<String,String> targetConv = new Map2Set<String,String>();
		for(String n : tLex.getNames(e))
			targetConv.add(n.replace(" ", ""), n);
		
		//Initialize the alignment
		Alignment maps = new Alignment();
		for(String c : sourceConv.keySet())
		{
			if(!targetConv.contains(c))
				continue;
			for(String s : sourceConv.get(c))
			{
				for(String i : sLex.getEntities(e,s))
				{
					if(e.equals(EntityType.INDIVIDUAL) && !aml.isToMatchSource(i))
						continue;
					double weight = sLex.getCorrectedWeight(s, i) * WEIGHT;
					for(String t : targetConv.get(c))
					{
						for(String j : tLex.getEntities(e,t))
						{
							if(e.equals(EntityType.INDIVIDUAL) && (!aml.isToMatchTarget(j) ||
									(aml.getInstanceMatchingCategory().equals(InstanceMatchingCategory.SAME_CLASSES) &&
									!aml.getEntityMap().shareClass(i,j))))
								continue;
							double similarity = tLex.getCorrectedWeight(t, j) * weight;
							if(similarity >= thresh)
								maps.add(i, j, similarity);
						}
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