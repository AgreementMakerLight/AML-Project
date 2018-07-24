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
import aml.alignment.SimpleAlignment;
import aml.match.AbstractParallelMatcher;
import aml.ontology.EntityType;
import aml.ontology.Ontology;
import aml.ontology.lexicon.Lexicon;
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
	private Lexicon sLex;
	private Lexicon tLex;
	private boolean useWordNet;
	
//Constructors
	
	public HybridStringMatcher(boolean useWordNet)
	{
		this.useWordNet = useWordNet;
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
	public SimpleAlignment match(Ontology o1, Ontology o2, EntityType e, double thresh)
	{
		sLex = o1.getLexicon();
		tLex = o2.getLexicon();
		System.out.println("Running Hybrid String Matcher");
		long time = System.currentTimeMillis()/1000;
		SimpleAlignment a = super.match(o1, o2, e, thresh);
		time = System.currentTimeMillis()/1000 - time;
		System.out.println("Finished in " + time + " seconds");
		return a;
	}
		
	@Override
	public SimpleAlignment rematch(Ontology o1, Ontology o2, SimpleAlignment a, EntityType e)
	{
		sLex = o1.getLexicon();
		tLex = o2.getLexicon();
		System.out.println("Computing Hybrid String Similarity");
		long time = System.currentTimeMillis()/1000;
		SimpleAlignment maps = super.rematch(o1, o2, a, e);
		time = System.currentTimeMillis()/1000 - time;
		System.out.println("Finished in " + time + " seconds");
		return maps;
	}
	
//Protected Methods
	
	/**
	 * Computes the maximum String similarity between two Classes by doing a
	 * pairwise comparison of all their names
	 */
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