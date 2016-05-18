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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import aml.AML;
import aml.ontology.Individual;
import aml.ontology.RelationshipMap;
import aml.util.ISub;
import aml.util.Similarity;
import aml.util.WordNet;

public abstract class AbstractInstanceMatcher implements PrimaryMatcher
{
	protected HashMap<Integer,Individual> sourceInd, targetInd;
	protected RelationshipMap rels;
	protected WordNet wn;
	protected boolean useWordNet = true;
	
	public AbstractInstanceMatcher()
	{
		AML aml = AML.getInstance();
		sourceInd = aml.getSource().getIndividualMap();
		targetInd = aml.getTarget().getIndividualMap();
		rels = aml.getRelationshipMap();
		wn = new WordNet();
	}

	public AbstractInstanceMatcher(boolean useWordNet)
	{
		AML aml = AML.getInstance();
		sourceInd = aml.getSource().getIndividualMap();
		targetInd = aml.getTarget().getIndividualMap();
		rels = aml.getRelationshipMap();
		wn = new WordNet();
		this.useWordNet = useWordNet;
	}

	@Override
	/* 
	 * Note that this method assumes that the classes instanced by the individuals
	 * we want to match belong to the same name space (i.e., are the same class).
	 * If this isn't true, then InstanceMatcher must be implemented as a SecondaryMatcher
	 * so that the classes are matched before the individuals.
	 */
	public Alignment match(double thresh)
	{
		Alignment a = new Alignment();
		//Iterate through the source individuals
		for(Integer i : sourceInd.keySet())
		{
			//Get their classes and object properties
			Set<Integer> sourceClasses = rels.getIndividualClasses(i);
			Set<Integer> sourceProps = rels.getIndividualProperties(i);
			//Iterate through the target individuals
			for(Integer j : targetInd.keySet())
			{
				//Get their classes, and verify that at least one class is
				//shared by the source and the target individuals
				Set<Integer> targetClasses = rels.getIndividualClasses(j);
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
				double nameSim = nameSimilarity(sourceInd.get(i).getName(), targetInd.get(j).getName(), useWordNet);
				
				//Compare the data properties and their values
				double dataSim = 0;
				for(Integer sd : sourceInd.get(i).getDataValues().keySet())
				{
					if(targetInd.get(j).getDataValues().keySet().contains(sd))
					{
						for(String sv : sourceInd.get(i).getDataValue(sd))
							for(String tv: targetInd.get(j).getDataValue(sd))
								dataSim = Math.max(dataSim, ISub.stringSimilarity(sv,tv));
					}
				}
				
				//Get the target object properties and compare them with the source
				Set<Integer> targetProps = rels.getIndividualProperties(j);
				double objectSim = Similarity.jaccard(sourceProps, targetProps);
				//Now compare the individuals related through these properties
				double relatedSim = 0;
				for(Integer so : sourceProps)
				{
					if(targetProps.contains(so))
					{
						for(Integer si : rels.getParentIndividuals(i, so))
							for(Integer ti : rels.getParentIndividuals(j, so))
								relatedSim = Math.max(relatedSim,
										nameSimilarity(sourceInd.get(si).getName(),targetInd.get(ti).getName(), useWordNet));
					}
				}
				//The final similarity should be some combination of all the similarities we've computed
				double finalSim = Math.max(nameSim, Math.max(dataSim, Math.max(objectSim, relatedSim)));
				if(finalSim >= thresh)
					a.add(i,j,finalSim);
			}
		}
		return a;		
	}
	
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