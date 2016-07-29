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
* Barebone matcher for instances.                                             *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/

package aml.match;

import java.util.HashSet;
import java.util.Set;

import aml.AML;
import aml.knowledge.WordNet;
import aml.ontology.Lexicon;
import aml.ontology.RelationshipMap;
import aml.ontology.ValueMap;
import aml.settings.EntityType;
import aml.util.ISub;
import aml.util.Similarity;

public abstract class AbstractInstanceMatcher implements PrimaryMatcher
{
	
//Attributes
	
	private static final String DESCRIPTION = "Matches individuals.";
	private static final String NAME = "Abstract Individual Matcher";
	private static final EntityType[] SUPPORT = {EntityType.INDIVIDUAL};
	protected Set<Integer> sourceInd, targetInd;
	protected RelationshipMap rels;
	protected Lexicon sLex, tLex;
	protected ValueMap sValues, tValues;
	protected WordNet wn;
	protected boolean useWordNet = true;
	
//Constructors
	
	public AbstractInstanceMatcher()
	{
		this(true);
	}

	public AbstractInstanceMatcher(boolean useWordNet)
	{
		AML aml = AML.getInstance();
		sourceInd = aml.getSource().getEntities(EntityType.INDIVIDUAL);
		targetInd = aml.getTarget().getEntities(EntityType.INDIVIDUAL);
		rels = aml.getRelationshipMap();
		sLex = aml.getSource().getLexicon();
		tLex = aml.getTarget().getLexicon();
		sValues = aml.getSource().getValueMap();
		tValues = aml.getTarget().getValueMap();
		this.useWordNet = useWordNet;
		if(useWordNet)
			wn = new WordNet();
	}
	
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
	/* 
	 * Note that this method assumes that the classes instanced by the individuals
	 * we want to match belong to the same name space (i.e., are the same class).
	 * If this isn't true, then InstanceMatcher must be implemented as a SecondaryMatcher
	 * so that the classes are matched before the individuals.
	 */
	public Alignment match(EntityType e, double thresh) throws UnsupportedEntityTypeException
	{
		if(!e.equals(EntityType.INDIVIDUAL))
			throw new UnsupportedEntityTypeException(e.toString());
		Alignment a = new Alignment();
		//Iterate through the source individuals
		for(Integer i : sourceInd)
		{
			//Get their classes and relations
			Set<Integer> sourceClasses = rels.getIndividualClasses(i);
			Set<Integer> sourceRels = rels.getIndividualActiveRelations(i);
			
			//Iterate through the target individuals
			for(Integer j : targetInd)
			{
				//Get their classes, and verify that at least one class is
				//shared by the source and the target individuals
				//Note: this only works if the ontologies share the Tbox
				Set<Integer> targetClasses = rels.getIndividualClasses(j);
				Set<Integer> targetRels = rels.getIndividualActiveRelations(j);
				boolean check = false;
				for(Integer sc : sourceClasses)
				{
					if(targetClasses.contains(sc))
					{
						check = true;
						break;
					}
				}
				if(!check)
					continue;
				//Compute the string similarity between the individuals' names
				double nameSim = nameSimilarity(i, j, useWordNet);
				
				//Compare the data properties and their values
				double dataSim = 0;
				for(Integer sd : sValues.getProperties(i))
				{
					if(tValues.getProperties(j).contains(sd))
					{
						for(String sv : sValues.getValues(i,sd))
							for(String tv: tValues.getValues(j,sd))
								dataSim = Math.max(dataSim, ISub.stringSimilarity(sv,tv));
					}
				}
				
				//Compare the related individuals
				double relatedSim = 0;
				for(Integer so : sourceRels)
				{
					Set<Integer> sourceProps = rels.getIndividualProperties(i, so);
					for(Integer to : targetRels)
					{
						Set<Integer> targetProps = rels.getIndividualProperties(j, to);
						//Check if there is at least one property in common
						//Note: again, this only works if the ontologies share the Tbox
						if(Similarity.jaccard(sourceProps, targetProps) == 0)
							continue;
						
						relatedSim = Math.max(relatedSim,nameSimilarity(so,to,useWordNet));
					}
				}
				//The final similarity should be some combination of all the similarities we've computed
				double finalSim = Math.max(nameSim, Math.max(dataSim, relatedSim));
				if(finalSim >= thresh)
					a.add(i,j,finalSim);
			}
		}
		return a;		
	}
	
//Private Methods
	
	protected double nameSimilarity(int i1, int i2, boolean useWordNet)
	{
		double sim = 0.0;
		for(String n1 : sLex.getNames(i1))
			for(String n2 : tLex.getNames(i2))
				sim = Math.max(sim, nameSimilarity(n1,n2,useWordNet));
		return sim;
	}
	
	protected double nameSimilarity(String n1, String n2, boolean useWordNet)
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