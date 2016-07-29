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
* Matching algorithm that maps Ontology properties by comparing their names,  *
* types, domains and ranges. Can use an input class Alignment to check for    *
* domain and range matches. Can use WordNet to boost the name similarity.     *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.match;

import java.util.HashSet;
import java.util.Set;

import aml.AML;
import aml.knowledge.WordNet;
import aml.match.Alignment;
import aml.match.Mapping;
import aml.match.SecondaryMatcher;
import aml.ontology.Ontology;
import aml.settings.EntityType;
import aml.util.ISub;
import aml.util.Similarity;

public class PropertyMatcher implements SecondaryMatcher
{
	
//Attributes
	
	private static final String DESCRIPTION = "Matches Data and Object Properties by\n" +
											  "comparing their Lexicon entries (String\n" +
											  "and word similarity)";
	private static final String NAME = "Property Matcher";
	private static final EntityType[] SUPPORT = {EntityType.DATA,EntityType.OBJECT};
	private AML aml;
	private Ontology source;
	private Ontology target;
	private WordNet wn = null;
	
//Constructors
	
	public PropertyMatcher(boolean useWordNet)
	{
		if(useWordNet)
			wn = new WordNet();
		aml = AML.getInstance();
		source = aml.getSource();
		target = aml.getTarget();
	}
	
//Public Methods
	
	@Override
	public Alignment extendAlignment(Alignment a, EntityType e, double threshold) throws UnsupportedEntityTypeException
	{
		checkEntityType(e);
		System.out.println("Extending Alignment with Property Matcher");
		long time = System.currentTimeMillis()/1000;
		Alignment propMaps = new Alignment();
		//Map data properties
		Set<Integer> sourceKeys = source.getEntities(e);
		Set<Integer> targetKeys = target.getEntities(e);
		for(Integer i : sourceKeys)
		{
			for(Integer j : targetKeys)
			{
				double sim = nameSimilarity(i,j);
				if(sim >= threshold)
					propMaps.add(new Mapping(i,j,sim));
			}
		}
		time = System.currentTimeMillis()/1000 - time;
		System.out.println("Finished in " + time + " seconds");
		return propMaps;
	}
	
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
	
	//Measures the name similarity between two properties, given by the
	//maximum similarity between their names
	private double nameSimilarity(int s, int t)
	{
		Set<String> sourceNames = source.getLexicon().getNames(s);
		Set<String> targetNames = target.getLexicon().getNames(t);
		for(String sName : sourceNames)
			for(String tName : targetNames)
				if(sName.equals(tName))
					return 1.0;

		double sim = 0.0;
		for(String sName : sourceNames)
		{
			for(String tName : targetNames)
			{
				double newSim = nameSimilarity(sName,tName,wn != null);
				if(newSim > sim)
					sim = newSim;
			}
		}
		return sim;
	}
			
	//Computes the similarity between two property names using a Jaccard
	//index between their words. When using WordNet, the WordNet similarity
	//is given by the Jaccard index between all WordNet synonyms, and is
	//returned instead of the name similarity if it is higher
	private double nameSimilarity(String n1, String n2, boolean useWordNet)
	{
		//Check if the names are equal
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
			if(useWordNet && w.length() > 2)
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
			if(useWordNet && w.length() > 3)
				tSyns.addAll(wn.getAllWordForms(w));
		}
		
		//Compute the Jaccard word similarity between the properties
		double wordSim = Similarity.jaccard(sWords,tWords)*0.9;
		//and the String similarity
		double simString = ISub.stringSimilarity(n1,n2)*0.9;
		//Combine the two
		double sim = 1 - ((1-wordSim) * (1-simString));
		if(useWordNet)
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