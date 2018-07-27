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
* Matches Ontologies by measuring the maximum String similarity between their *
* classes, using one of the four available String similarity measures.        *
*                                                                             *
* WARNING: This matching algorithm takes O(N^2) time, and thus should be used *
* either to match small ontologies or as a SecondaryMatcher.                  *
*                                                                             *
* @authors Daniel Faria, Cosmin Stroe                                         *
******************************************************************************/
package aml.match.lexical;

import java.util.Set;

import aml.AML;
import aml.alignment.SimpleAlignment;
import aml.alignment.mapping.Mapping;
import aml.alignment.mapping.SimpleMapping;
import aml.match.AbstractParallelMatcher;
import aml.ontology.EntityType;
import aml.ontology.Ontology;
import aml.ontology.lexicon.LexicalType;
import aml.ontology.semantics.EntityMap;
import aml.settings.LanguageSetting;
import aml.util.data.Map2Set;
import aml.util.similarity.Similarity;
import aml.util.similarity.StringSimMeasure;

public class StringMatcher extends AbstractParallelMatcher
{

//Attributes

	protected static final String DESCRIPTION = "Matches entities by computing the maximum\n" +
											  "String similarity between their Lexicon\n" +
											  "entries, using a String similarity measure";
	protected static final String NAME = "String Matcher";
	protected static final EntityType[] SUPPORT = {EntityType.CLASS,EntityType.INDIVIDUAL,EntityType.DATA_PROP,EntityType.OBJECT_PROP};
	//Similarity measure
	private StringSimMeasure measure = StringSimMeasure.ISUB;
	//Correction factor (to make string similarity values comparable to word similarity values
	//and thus enable their combination and proper selection; 0.8 is optimized for the ISub measure)
	private final double CORRECTION = 0.80;

//Constructors
	
	/**
	 * Constructs a new ParametricStringMatcher with default
	 * String similarity measure (ISub)
	 */
	public StringMatcher()
	{
		super();
	}

	/**
	 * Constructs a new ParametricStringMatcher with the given String similarity measure
	 * @args m: the string similarity measure
	 */
	public StringMatcher(StringSimMeasure m)
	{
		this();
		measure = m;
	}

//Public Methods
	
	@Override
	public SimpleAlignment extendAlignment(Ontology o1, Ontology o2, SimpleAlignment a, EntityType e, double thresh)
	{	
		if(e.equals(EntityType.CLASS))
		{
			System.out.println("Running " + NAME  + " in alignment extension mode");
			long time = System.currentTimeMillis()/1000;
			sLex = o1.getLexicon();
			tLex = o2.getLexicon();
			SimpleAlignment ext = new SimpleAlignment(o1.getURI(),o2.getURI());
			System.out.println("Matching Children & Parents");
			ext.addAll(extendChildrenAndParents(a,thresh));
			SimpleAlignment aux = extendChildrenAndParents(ext,thresh);
			int size = 0;
			for(int i = 0; i < 10 && ext.size() > size; i++)
			{
				size = ext.size();
				for(Mapping m : aux)
					if(!a.containsConflict(m))
						ext.add(m);
				aux = extendChildrenAndParents(aux,thresh);
			}
			System.out.println("Matching Siblings");
			ext.addAll(extendSiblings(a,thresh));
			time = System.currentTimeMillis()/1000 - time;
			System.out.println("Finished in " + time + " seconds");
			return ext;
		}
		else
			return super.extendAlignment(o1, o2, a, e, thresh);
	}
	
//Protected Methods

	@Override
	protected double mapTwoEntities(String sId, String tId)
	{
		double maxSim = 0.0;
		double sim, weight;
		
		if(AML.getInstance().getLanguageSetting().equals(LanguageSetting.MULTI))
		{
			for(String l : AML.getInstance().getLanguages())
			{
				Set<String> sourceNames = sLex.getNamesWithLanguage(sId,l);
				Set<String> targetNames = tLex.getNamesWithLanguage(tId,l);
				if(sourceNames == null || targetNames == null)
					continue;
			
				for(String s : sourceNames)
				{
					if(sLex.getTypes(s,sId).contains(LexicalType.FORMULA))
						continue;
					weight = sLex.getCorrectedWeight(s, sId, l);
					
					for(String t : targetNames)
					{
						if(tLex.getTypes(t,tId).contains(LexicalType.FORMULA))
							continue;
						sim = weight * tLex.getCorrectedWeight(t, tId, l);
						sim *= Similarity.stringSimilarity(s,t,measure)*CORRECTION;
						if(sim > maxSim)
							maxSim = sim;
					}
				}
			}
		}
		else
		{
			Set<String> sourceNames = sLex.getNames(sId);
			Set<String> targetNames = tLex.getNames(tId);
			if(sourceNames == null || targetNames == null)
				return maxSim;
			for(String s : sourceNames)
			{
				if(sLex.getTypes(s,sId).contains(LexicalType.FORMULA))
					continue;
				weight = sLex.getCorrectedWeight(s, sId);
				
				for(String t : targetNames)
				{
					if(tLex.getTypes(t,tId).contains(LexicalType.FORMULA))
						continue;
					sim = weight * tLex.getCorrectedWeight(t, tId);
					sim *= Similarity.stringSimilarity(s,t,measure)*CORRECTION;
					if(sim > maxSim)
						maxSim = sim;
				}
			}
		}
		return maxSim;
	}
	
//Private Methods
	
	private SimpleAlignment extendChildrenAndParents(SimpleAlignment a, double thresh)
	{
		AML aml = AML.getInstance();
		EntityMap rels = aml.getEntityMap();
		Map2Set<String,String> toMap = new Map2Set<String,String>();
		for(Mapping input : a)
		{
			if(input instanceof SimpleMapping && rels.isClass((String)input.getEntity1()))
			{
				Set<String> sourceChildren = rels.getSubclasses((String)input.getEntity1(),1);
				Set<String> targetChildren = rels.getSubclasses((String)input.getEntity2(),1);
				for(String s : sourceChildren)
				{
					if(a.containsSource(s))
						continue;
					for(String t : targetChildren)
					{
						if(!a.containsTarget(t))
							toMap.add(s,t);
					}
				}
				Set<String> sourceParents = rels.getSuperclasses((String)input.getEntity1(),1);
				Set<String> targetParents = rels.getSuperclasses((String)input.getEntity2(),1);
				for(String s : sourceParents)
				{
					if(a.containsSource(s))
						continue;
					for(String t : targetParents)
					{
						if(!a.containsTarget(t))
							toMap.add(s, t);
					}
				}
			}
		}
		return mapInParallel(toMap,thresh);
	}
	
	private SimpleAlignment extendSiblings(SimpleAlignment a, double thresh)
	{		
		AML aml = AML.getInstance();
		EntityMap rels = aml.getEntityMap();
		Map2Set<String,String> toMap = new Map2Set<String,String>();
		for(Mapping input : a)
		{
			if(input instanceof SimpleMapping && aml.getEntityMap().isClass((String)input.getEntity1()))
			{
				Set<String> sourceSiblings = rels.getSiblings((String)input.getEntity1());
				Set<String> targetSiblings = rels.getSiblings((String)input.getEntity2());
				if(sourceSiblings.size() > 200 || targetSiblings.size() > 200)
					continue;
				for(String s : sourceSiblings)
				{
					if(a.containsSource(s))
						continue;
					for(String t : targetSiblings)
					{
						if(!a.containsTarget(t))
							toMap.add(s, t);
					}
				}
			}
		}
		return mapInParallel(toMap,thresh);
	}
}