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
* Matching algorithm that maps individuals by comparing the Lexicon entries   *
* of one with the ValueMap entries of the other using a combination of        *
* String- and Word-Matching algorithms and optionally the WordNet.            *
*                                                                             *
* @author Daniel Faria, Catia Pesquita                                        *
******************************************************************************/
package aml.match.value;

import aml.match.AbstractParallelMatcher;
import aml.ontology.EntityType;
import aml.util.similarity.Similarity;

public class Value2LexiconMatcher extends AbstractParallelMatcher
{
	
//Attributes
	
	protected String description = "Matches individuals by comparing the Lexicon\n" +
											  "entries of one to the ValueMap entries of the\n" +
											  "other using a combination of string- and word-\n" +
											  "matching algorithms, and optionally the WordNet";
	protected String name = "Value-to-Lexicon Matcher";
	protected EntityType[] support = {EntityType.INDIVIDUAL};
	boolean useWordNet;
	
//Constructors
	
	public Value2LexiconMatcher(boolean useWordNet)
	{
		super();
		this.useWordNet = useWordNet;
	}
	
//Protected Methods
	
	//Computes the maximum String similarity between two Classes by doing a
	//pairwise comparison of all their names
	protected double mapTwoEntities(String sId, String tId)
	{
		double crossSim = 0;
		for(String n1 : sLex.getNames(sId))
			for(String td : tVal.getProperties(tId))
				for(String tv : tVal.getValues(tId,td))
					crossSim = Math.max(crossSim,Similarity.nameSimilarity(n1,tv,useWordNet));
		for(String n2 : tLex.getNames(tId))
			for(String sd : sVal.getProperties(sId))
				for(String sv : sVal.getValues(sId,sd))
					crossSim = Math.max(crossSim,Similarity.nameSimilarity(n2,sv,useWordNet));
		return crossSim;
	}
}