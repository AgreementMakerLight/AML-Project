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
* Flagger that identifies low quality mappings in the Alignment by computing  *
* auxiliary Alignments.                                                       *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.filter;

import java.util.HashMap;
import java.util.Set;

import aml.AML;
import aml.alignment.Alignment;
import aml.alignment.SimpleAlignment;
import aml.alignment.mapping.Mapping;
import aml.alignment.mapping.MappingStatus;
import aml.match.lexical.StringMatcher;
import aml.match.lexical.WordMatchStrategy;
import aml.match.lexical.WordMatcher;
import aml.match.structural.BlockRematcher;
import aml.match.structural.NeighborSimilarityMatcher;
import aml.match.structural.NeighborSimilarityStrategy;
import aml.ontology.EntityType;
import aml.settings.SizeCategory;

public class QualityFlagger implements Flagger
{
	
//Attributes
	
	private AML aml;
	private SimpleAlignment a;
	private SizeCategory size;
	private HashMap<String,SimpleAlignment> auxAlignments;
	private final double AVERAGE_THRESH = 0.2;
	
//Constructors
	
	public QualityFlagger(){}
	
//Public Methods
	
	@Override
	@SuppressWarnings("rawtypes")
	public void flag(Alignment a)
	{
		if(!(a instanceof SimpleAlignment))
		{
			System.out.println("Warning: cannot flag non-simple alignment!");
			return;
		}
		System.out.println("Running Quality Flagger");
		SimpleAlignment in = (SimpleAlignment)a;
		long time = System.currentTimeMillis()/1000;
		aml = AML.getInstance();
		size = aml.getSizeCategory();
		//Construct the list of auxiliary (re)matchers and alignments
		auxAlignments = new HashMap<String,SimpleAlignment>();
		for(String lang : aml.getLanguages())
		{
			WordMatcher wm = new WordMatcher(lang, WordMatchStrategy.AVERAGE);
			auxAlignments.put("Word Similarity (" + lang + "): ", 
					wm.rematch(aml.getSource(), aml.getTarget(), in, EntityType.CLASS));
		}
		StringMatcher sm = new StringMatcher();
		auxAlignments.put("String Similarity: ",
				sm.rematch(aml.getSource(), aml.getTarget(), in, EntityType.CLASS));
		NeighborSimilarityMatcher nm = new NeighborSimilarityMatcher(
				NeighborSimilarityStrategy.DESCENDANTS, !size.equals(SizeCategory.SMALL));
		auxAlignments.put("Descendant Similarity: ", 
				nm.rematch(aml.getSource(), aml.getTarget(), in, EntityType.CLASS));
		nm = new NeighborSimilarityMatcher(
				NeighborSimilarityStrategy.ANCESTORS,
				!size.equals(SizeCategory.SMALL));
		auxAlignments.put("Ancestor Similarity: ",
				nm.rematch(aml.getSource(), aml.getTarget(), in, EntityType.CLASS));
		if(size.equals(SizeCategory.HUGE))
		{
			BlockRematcher br = new BlockRematcher();
			auxAlignments.put("High-Level Similarity: ",
					br.rematch(aml.getSource(), aml.getTarget(), in, EntityType.CLASS));
		}
		for(Mapping<String> m : in)
		{
			String source = m.getEntity1();
			String target = m.getEntity2();
			double average = m.getSimilarity();
			int support = 0;
			for(String s : auxAlignments.keySet())
			{
				double sim = auxAlignments.get(s).getSimilarity(source, target);
				//High level similarity doesn't count towards the support
				if(!s.equals("High-Level Similarity: ") && sim > 0)
					support++;
				//Ancestor similarity doesn't count towards the average
				if(!s.equals("Ancestor Similarity: "))
					average += sim;
			}
			average /= auxAlignments.size()-1;
			
			if((support < 2 || average < AVERAGE_THRESH) &&
					m.getStatus().equals(MappingStatus.UNKNOWN))
				m.setStatus(MappingStatus.FLAGGED);
		}		
		System.out.println("Finished in " +	(System.currentTimeMillis()/1000-time) + " seconds");
	}
	
	/**
	 * @param source: the id of the source Ontology Entity
	 * @param target: the id of the target Ontology Entity
	 * @return the average similarity between the Entities
	 */
	public double getAverageSimilarity(String source, String target)
	{
		double average = 0.0;
		for(String s : auxAlignments.keySet())
		{
			//Ancestor similarity doesn't count towards the average
			if(!s.equals("Ancestor Similarity: "))
				average += auxAlignments.get(s).getSimilarity(source, target);
		}
		average /= auxAlignments.size()-1;
		return average;
	}

	/**
	 * @return the labels of the auxiliary Alignments
	 */
	public Set<String> getLabels()
	{
		return auxAlignments.keySet();
	}
	
	/**
	 * @param source: the id of the source Ontology Entity
	 * @param target: the id of the target Ontology Entity
	 * @return the maximum similarity between the Entities
	 */
	public double getMaxSimilarity(String source, String target)
	{
		double max = a.getSimilarity(source, target);
		for(String s : auxAlignments.keySet())
			max = Math.max(max, auxAlignments.get(s).getSimilarity(source, target));
		return max;
	}
	
	/**
	 * @param source: the id of the source Ontology Entity
	 * @param target: the id of the target Ontology Entity
	 * @param matcher: the label of the auxiliary Alignment
	 * @return the similarity between the Entities in the auxiliary Alignment
	 */
	public double getSimilarity(String source, String target, String matcher)
	{
		return auxAlignments.get(matcher).getSimilarity(source, target);
	}
	
	/**
	 * @param source: the id of the source Ontology Entity
	 * @param target: the id of the target Ontology Entity
	 * @param matcher: the label of the auxiliary Alignment
	 * @return the similarity between the Entities in the auxiliary Alignment in percentage
	 */
	public String getSimilarityPercent(String source, String target, String matcher)
	{
		return auxAlignments.get(matcher).getSimilarityPercent(source, target);
	}
	
	/**
	 * @param source: the id of the source Ontology Entity
	 * @param target: the id of the target Ontology Entity
	 * @return the support for a mapping between the Entities
	 */
	public int getSupport(String source, String target)
	{
		int support = 0;
		for(String s : auxAlignments.keySet())
		{
			double sim = auxAlignments.get(s).getSimilarity(source, target);
			//High level similarity doesn't count towards the support
			if(!s.equals("High-Level Similarity: ") && sim > 0)
				support++;
		}
		return support;
	}
}