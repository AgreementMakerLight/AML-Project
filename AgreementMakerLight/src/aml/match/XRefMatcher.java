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
* Matches Ontologies by using cross-references between them and a third       *
* mediating Ontology, or using Lexical matches when there are few or no       *
* cross-references.                                                           *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.match;

import java.util.Set;

import aml.AML;
import aml.knowledge.MediatorOntology;
import aml.knowledge.ReferenceMap;
import aml.ontology.Ontology;
import aml.settings.EntityType;
import aml.util.Table2Map;

public class XRefMatcher extends MediatingMatcher
{
	
//Attributes

	private static final String DESCRIPTION = "Matches entities that are cross-referenced by\n" +
											  "the same entity of a background knowledge\n" +
											  "source, and/or using the Mediating Matcher.";
	private static final String NAME = "Cross-Reference Matcher";
	private static final EntityType[] SUPPORT = {EntityType.CLASS};
	//The external ontology's ReferenceMap
	private ReferenceMap rm;
	//The weight used for matching and Lexicon extension
	private final double WEIGHT = 0.95;
	//The source and target alignments
	Table2Map<Integer,Integer,Double> src;
	Table2Map<Integer,Integer,Double> tgt;
	
//Constructors

	/**
	 * Constructs a XRefMatcher with the given external Ontology
	 * @param x: the external Ontology
	 */
	public XRefMatcher(MediatorOntology x)
	{
		super(x);
		rm = x.getReferenceMap();
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
	public void extendLexicons(EntityType e, double thresh) throws UnsupportedEntityTypeException
	{
		checkEntityType(e);
		System.out.println("Extending Lexicons with Cross-Reference Matcher using " + uri);
		long time = System.currentTimeMillis()/1000;
		AML aml = AML.getInstance();
		Ontology source = aml.getSource();
		if(src == null)
			src = match(source,thresh);
		for(Integer s : src.keySet())
		{
			for(Integer hit : src.keySet(s))
			{
				Set<String> names = ext.getNames(hit);
				for(String n : names)
				{
					double sim = src.get(s,hit) * ext.getWeight(n, hit);
					if(sim >= thresh)
						source.getLexicon().add(s, n, "en", TYPE, uri, sim);
				}
			}
		}
		Ontology target = aml.getTarget();
		if(tgt == null)
			tgt = match(target,thresh);
		for(Integer s : tgt.keySet())
		{
			for(Integer hit : tgt.keySet(s))
			{
				Set<String> names = ext.getNames(hit);
				for(String n : names)
				{
					double sim = tgt.get(s,hit) * ext.getWeight(n, hit);
					if(sim >= thresh)
						target.getLexicon().add(s, n, "en", TYPE, uri, sim);
				}
			}
		}
		time = System.currentTimeMillis()/1000 - time;
		System.out.println("Finished in " + time + " seconds");
	}
	
	@Override
	public Alignment match(EntityType e, double thresh) throws UnsupportedEntityTypeException
	{
		checkEntityType(e);
		System.out.println("Running Cross-Reference Matcher using " + uri);
		long time = System.currentTimeMillis()/1000;
		AML aml = AML.getInstance();
		Ontology source = aml.getSource();
		Ontology target = aml.getTarget();
		src = match(source,thresh);
		tgt = match(target,thresh);
		//Reverse the target alignment table
		Table2Map<Integer,Integer,Double> rev = new Table2Map<Integer,Integer,Double>();
		for(Integer s : tgt.keySet())
			for(Integer t : tgt.keySet(s))
				rev.add(t, s, tgt.get(s, t));
		Alignment maps = new Alignment();
		for(Integer s : src.keySet())
		{
			for(Integer med : src.keySet(s))
			{
				if(!rev.contains(med))
					continue;
				for(Integer t : rev.keySet(med))
				{
					double similarity = Math.min(src.get(s, med), rev.get(med, t));
					similarity = Math.min(similarity,WEIGHT);
					maps.add(s,t,similarity);
				}
			}
		}
		time = System.currentTimeMillis()/1000 - time;
		System.out.println("Finished in " + time + " seconds");
		return maps;
	}
	
//Private Methods
	
	private Table2Map<Integer,Integer,Double> match(Ontology o, double thresh)
	{
		Table2Map<Integer,Integer,Double> maps = new Table2Map<Integer,Integer,Double>();
		if(rm != null)
		{
			Set<String> refs = rm.getReferences();
			Set<String> names = o.getLocalNames();
			for(String r : refs)
			{
				if(names.contains(r))
				{
					Set<Integer> terms = rm.get(r);
					//Penalize cases where multiple terms have the same xref
					//(note that sim = 1 when the xref is unique) 
					double sim = 1.3 - (terms.size() * 0.3);
					if(sim < thresh)
						continue;
					for(Integer i : terms)
						if(!maps.contains(o.getIndex(r), i) || maps.get(o.getIndex(r), i) > sim)
							maps.add(o.getIndex(r), i, sim);
				}
			}
		}
		//Step 2 - Do a lexical match
		Table2Map<Integer,Integer,Double> lex = match(o.getLexicon(),thresh);
		
		//Step 3 - Compare the two
		//If the coverage of the lexical match is at least double
		//the coverage of the xref match (such as when there are
		//few or no xrefs) merge the two
		if(lex.keySet().size() > maps.keySet().size() * 2)
		{
			for(Integer s : lex.keySet())
			{
				if(maps.contains(s))
					continue;
				for(Integer t : lex.keySet(s))
					maps.add(s, t, lex.get(s, t));
			}
		}
		return maps;
	}
}