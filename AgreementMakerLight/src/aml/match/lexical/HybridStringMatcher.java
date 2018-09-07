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
* Matching algorithm that maps Ontology entities by comparing their Lexicon   *
* entries through String- and Word-Matching algorithms with the optional use  *
* of WordNet.                                                                 *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.match.lexical;

import java.util.Set;

import aml.AML;
import aml.match.AbstractParallelMatcher;
import aml.ontology.EntityType;
import aml.settings.LanguageSetting;
import aml.util.similarity.Similarity;

public class HybridStringMatcher extends AbstractParallelMatcher
{
	
//Attributes
	
	protected static final String DESCRIPTION = "Matches entities by comparing their Lexicon\n" +
											  "entries through a combination of string- and\n" +
											  "word-matching algorithms, with the optional\n" +
											  "use of WordNet";
	protected static final String NAME = "Hybrid String Matcher";
	protected static final EntityType[] SUPPORT = {EntityType.CLASS,EntityType.DATA_PROP,EntityType.INDIVIDUAL,EntityType.OBJECT_PROP};
	private boolean useWordNet;
	
//Constructors
	
	public HybridStringMatcher(boolean useWordNet)
	{
		super();
		this.useWordNet = useWordNet;
		description = DESCRIPTION;
		name = NAME;
		support = SUPPORT;
	}
	
	
//Protected Methods
	
	@Override
	protected double mapTwoEntities(String sId, String tId)
	{
		double maxSim = 0.0;
		
		if(AML.getInstance().getLanguageSetting().equals(LanguageSetting.MULTI))
		{
			for(String l : AML.getInstance().getLanguages())
			{
				Set<String> sourceNames = sLex.getNamesWithLanguage(sId,l);
				Set<String> targetNames = tLex.getNamesWithLanguage(tId,l);
				if(sourceNames == null || targetNames == null)
					continue;
				for(String s : sourceNames)
					for(String t : targetNames)
						maxSim = Math.max(maxSim,Similarity.nameSimilarity(s,t,useWordNet));
			}
		}
		else
		{
			Set<String> sourceNames = sLex.getNames(sId);
			Set<String> targetNames = tLex.getNames(tId);
			if(sourceNames == null || targetNames == null)
				return maxSim;
			for(String s : sourceNames)
				for(String t : targetNames)
					maxSim = Math.max(maxSim,Similarity.nameSimilarity(s,t,useWordNet));
		}
		return maxSim;
	}
}