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
* Flagger that identifies low quality mappings in the Alignment by computing  *
* auxiliary Alignments.                                                       *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.filter;

import java.util.Vector;

import aml.AML;
import aml.match.Alignment;
import aml.match.BlockRematcher;
import aml.match.Mapping;
import aml.match.NeighborSimilarityMatcher;
import aml.match.StringMatcher;
import aml.match.WordMatcher;
import aml.settings.MappingStatus;
import aml.settings.NeighborSimilarityStrategy;
import aml.settings.SizeCategory;
import aml.settings.WordMatchStrategy;

public class QualityFlagger implements Flagger
{
	
//Attributes
	
	private AML aml;
	private Alignment a;
	private SizeCategory size;
	private Vector<String> auxMatchers;
	private Vector<Alignment> auxAlignments;
	private final double AVERAGE_THRESH = 0.2;
	
//Constructors
	
	public QualityFlagger()
	{
		aml = AML.getInstance();
		a = aml.getAlignment();
		size = aml.getSizeCategory();
		//Construct the list of auxiliary (re)matchers and alignments
		auxMatchers = new Vector<String>();
		auxAlignments = new Vector<Alignment>();
		auxMatchers.add("Word Similarity: ");
		WordMatcher wm = new WordMatcher(WordMatchStrategy.AVERAGE);
		auxAlignments.add(wm.rematch(a));
		auxMatchers.add("String Similarity: ");
		StringMatcher sm = new StringMatcher();
		auxAlignments.add(sm.rematch(a));
		auxMatchers.add("Descendant Similarity: ");
		NeighborSimilarityMatcher nm = new NeighborSimilarityMatcher(
				NeighborSimilarityStrategy.DESCENDANTS,
				!size.equals(SizeCategory.SMALL));
		auxAlignments.add(nm.rematch(a));
		auxMatchers.add("Ancestor Similarity: ");
		nm = new NeighborSimilarityMatcher(
				NeighborSimilarityStrategy.ANCESTORS,
				!size.equals(SizeCategory.SMALL));
		auxAlignments.add(nm.rematch(a));
		if(size.equals(SizeCategory.HUGE))
		{
			auxMatchers.add("High-Level Similarity: ");
			BlockRematcher br = new BlockRematcher();
			auxAlignments.add(br.rematch(a));
		}
	}
	
//Public Methods
	
	@Override
	public void flag()
	{
		System.out.println("Running Quality Flagger");
		long time = System.currentTimeMillis()/1000;
		for(Mapping m : a)
		{
			int sourceId = m.getSourceId();
			int targetId = m.getTargetId();
			double average = m.getSimilarity();
			int support = 0;
			for(int i = 0; i < auxAlignments.size(); i++)
			{
				double sim = auxAlignments.get(i).getSimilarity(sourceId, targetId);
				//High level similarity doesn't count towards the support
				if(i != 4 && sim > 0)
					support++;
				//Ancestor similarity doesn't count towards the average
				if(i != 3)
					average += sim;
			}
			average /= auxAlignments.size();
			
			if((support < 2 || average < AVERAGE_THRESH) &&
					m.getStatus().equals(MappingStatus.UNKNOWN))
				m.setStatus(MappingStatus.FLAGGED);
		}		
		System.out.println("Finished in " +	(System.currentTimeMillis()/1000-time) + " seconds");
	}
	
	/**
	 * @param sourceId: the id of the source Ontology Entity
	 * @param targetId: the id of the target Ontology Entity
	 * @return the average similarity between the Entities
	 */
	public double getAverageSimilarity(int sourceId, int targetId)
	{
		double avg = a.getSimilarity(sourceId, targetId);
		for(int i = 0; i < auxAlignments.size(); i++)
		{
			double sim = auxAlignments.get(i).getSimilarity(sourceId, targetId);
			//Ancestor similarity doesn't count towards the average
			if(i != 3)
				avg += sim;
		}
		return avg / auxAlignments.size();
	}

	/**
	 * @return the labels of the auxiliary Alignments
	 */
	public Vector<String> getLabels()
	{
		return auxMatchers;
	}
	
	/**
	 * @param sourceId: the id of the source Ontology Entity
	 * @param targetId: the id of the target Ontology Entity
	 * @return the maximum similarity between the Entities
	 */
	public double getMaxSimilarity(int sourceId, int targetId)
	{
		double max = a.getSimilarity(sourceId, targetId);
		for(Alignment aux : auxAlignments)
			max = Math.max(max, aux.getSimilarity(sourceId, targetId));
		return max;
	}
	
	/**
	 * @param sourceId: the id of the source Ontology Entity
	 * @param targetId: the id of the target Ontology Entity
	 * @param matcher: the index of the auxiliary Alignment
	 * @return the similarity between the Entities in the auxiliary Alignment
	 */
	public double getSimilarity(int sourceId, int targetId, int matcher)
	{
		return auxAlignments.get(matcher).getSimilarity(sourceId, targetId);
	}
	
	/**
	 * @param sourceId: the id of the source Ontology Entity
	 * @param targetId: the id of the target Ontology Entity
	 * @param matcher: the index of the auxiliary Alignment
	 * @return the similarity between the Entities in the auxiliary Alignment in percentage
	 */
	public String getSimilarityPercent(int sourceId, int targetId, int matcher)
	{
		return auxAlignments.get(matcher).getSimilarityPercent(sourceId, targetId);
	}
	
	/**
	 * @param sourceId: the id of the source Ontology Entity
	 * @param targetId: the id of the target Ontology Entity
	 * @return the support for a mapping between the Entities
	 */
	public int getSupport(int sourceId, int targetId)
	{
		int support = 0;
		for(int i = 0; i < auxAlignments.size(); i++)
		{
			double sim = auxAlignments.get(i).getSimilarity(sourceId, targetId);
			//High level similarity doesn't count towards the support
			if(i != 4 && sim > 0)
				support++;
		}
		return support;
	}
}