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
* Matches individuals in a process (i.e., individuals that are part of a      *
* workflow and thereby organized sequentially).                               *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/

package aml.match;

import java.util.HashSet;
import java.util.Set;

import aml.AML;
import aml.alignment.Alignment;
import aml.alignment.SimpleMapping;
import aml.ontology.EntityType;
import aml.ontology.lexicon.StopList;
import aml.ontology.lexicon.StringParser;
import aml.ontology.semantics.EntityMap;
import aml.util.similarity.ISub;
import aml.util.similarity.Similarity;

public class ProcessMatcher implements PrimaryMatcher
{
	
//Attributes
	
	private static final String DESCRIPTION = "Matches individuals in a process (i.e.," +
											  "individuals that are part of a workflow" +
											  "and thereby organized sequentially)" +
											  "through a combination of String matching" +
											  "and similarity flooding";
	private static final String NAME = "Process Matcher";
	private static final EntityType[] SUPPORT = {EntityType.INDIVIDUAL};
	private Set<String> stopSet;
	private AML aml;
	private EntityMap rels;
	
//Constructors
	
	public ProcessMatcher()
	{
		aml = AML.getInstance();
		rels = aml.getEntityMap();
		stopSet = StopList.read();
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
	public Alignment match(EntityType e, double threshold)
	{
		Alignment a = new Alignment();
		//For each combination of individuals, do a string and word match
		for(Integer s : aml.getSource().getEntities(e))
		{
			for(Integer t : aml.getTarget().getEntities(e))
			{
				double sim = nameSimilarity(s, t, true);
				if(sim > 0)
					a.add(s,t,sim);
			}
		}
		a = neighborSimilarity(a);
		double names = aml.getSource().getLexicon().nameCount(EntityType.INDIVIDUAL)*1.0/aml.getSource().count(EntityType.INDIVIDUAL);
		names = Math.min(names, aml.getTarget().getLexicon().nameCount(EntityType.INDIVIDUAL)*1.0/aml.getTarget().count(EntityType.INDIVIDUAL));
		if(aml.getIndividualConnectivity() < 0.9)
		{
			a = neighborSimilarity(a);
			a = neighborSimilarity(a);
		}
		Alignment b = new Alignment();
		for(SimpleMapping m : a)
		{
			if(aml.isToMatchSource(m.getSourceId()) && aml.isToMatchTarget(m.getTargetId()) &&
					m.getSimilarity() >= threshold)
				b.add(m);
		}
		return b;
	}

//Private Methods
	
	protected double nameSimilarity(int i1, int i2, boolean useWordNet)
	{
		double sim = 0.0;
		for(String n1 : aml.getSource().getLexicon().getNames(i1))
			for(String n2 : aml.getTarget().getLexicon().getNames(i2))
				sim = Math.max(sim, nameSimilarity(n1,n2,useWordNet));
		return sim;
	}
	
	protected double nameSimilarity(String n1, String n2, boolean useWordNet)
	{
		//Check if the names are equal
		if(n1.equals(n2))
			return 1.0;
		
		//Since we cannot use string or word similarity on formulas
		//if the names are (non-equal) formulas their similarity is zero
		if(StringParser.isFormula(n1) || StringParser.isFormula(n2))
			return 0.0;

		//Compute the String similarity
		double stringSim = ISub.stringSimilarity(n1,n2);
		//Then the String similarity after removing stop words
		String n1S = n1;
		String n2S = n2;
		for(String s : stopSet)
		{
			n1S = n1S.replace(s, "").trim();
			n2S = n2S.replace(s, "").trim();
		}
		stringSim = Math.max(stringSim, ISub.stringSimilarity(n1S,n2S)*0.95);
		stringSim *= 0.8;
		
		//Compute the Word similarity (ignoring stop words)
		double wordSim = 0.0;
		//Split the source name into words
		String[] sW = n1.split(" ");
		HashSet<String> n1Words = new HashSet<String>();
		for(String s : sW)
			if(!stopSet.contains(s))
				n1Words.add(s);
		String[] tW = n2.split(" ");
		HashSet<String> n2Words = new HashSet<String>();
		for(String s : tW)
			if(!stopSet.contains(s))
				n2Words.add(s);
		wordSim = Similarity.jaccardSimilarity(n1Words, n2Words);
		
		//Return the maximum of the string and word similarity
		return Math.max(stringSim, wordSim);
	}
	
	public Alignment neighborSimilarity(Alignment a)
	{
		Alignment b = new Alignment();
		for(SimpleMapping m : a)
		{
			double maxSim = 0.0;
			HashSet<Integer> sourceChildren = getChildren(m.getSourceId(),false);
			HashSet<Integer> targetChildren = getChildren(m.getTargetId(),false);
			for(Integer s : sourceChildren)
			{
				for(Integer t : targetChildren)
				{
					SimpleMapping n = a.getBidirectional(s, t);
					if(n != null && n.getSimilarity() > maxSim)
						maxSim = n.getSimilarity();
				}
			}
				
			HashSet<Integer> sourceParents = getParents(m.getSourceId(),false);
			HashSet<Integer> targetParents = getParents(m.getTargetId(),false);
			for(Integer s : sourceParents)
			{
				for(Integer t : targetParents)
				{
					SimpleMapping n = a.getBidirectional(s, t);
					if(n != null && n.getSimilarity() > maxSim)
						maxSim = n.getSimilarity();
				}
			}
			b.add(m.getSourceId(),m.getTargetId(), (maxSim * 0.25) + (m.getSimilarity() * 0.75));
		}
		return b;
	}
	
	private HashSet<Integer> getChildren(int index, boolean recursive)
	{
		HashSet<Integer> children = new HashSet<Integer>(rels.getIndividualActiveRelations(index));
		if(recursive)
			for(Integer c : rels.getIndividualActiveRelations(index))
				for(Integer rel : rels.getIndividualProperties(index, c))
					for(Integer cc : rels.getIndividualActiveRelations(c))
						if(rels.getIndividualProperties(c, cc).contains(rel))
							children.add(cc);
		return children;
	}
	
	private HashSet<Integer> getParents(int index, boolean recursive)
	{
		HashSet<Integer> parents = new HashSet<Integer>(rels.getIndividualPassiveRelations(index));
		if(recursive)
			for(Integer p : rels.getIndividualPassiveRelations(index))
				for(Integer rel : rels.getIndividualProperties(p, index))
					for(Integer pp : rels.getIndividualPassiveRelations(p))
						if(rels.getIndividualProperties(pp, p).contains(rel))
							parents.add(pp);
		return parents;
	}
}