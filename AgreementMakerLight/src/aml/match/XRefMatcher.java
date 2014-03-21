/******************************************************************************
* Copyright 2013-2014 LASIGE                                                  *
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
* Matches two Ontologies by using cross-references between them and a third   *
* mediating Ontology, or using Lexical matches when there are few or no       *
* cross-references.                                                           *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 12-02-2014                                                            *
******************************************************************************/
package aml.match;

import java.util.Set;
import java.util.Vector;

import aml.ontology.Ontology;
import aml.ontology.ReferenceMap;

public class XRefMatcher implements Matcher
{
	
//Attributes

	//The external ontology
	private Ontology ext;
	//Links to the intermediate alignments
	private Alignment src;
	private Alignment tgt;
	
//Constructors

	/**
	 * Constructs a XRefMatcher with the given external Ontology
	 * @param x: the external Ontology
	 */
	public XRefMatcher(Ontology x)
	{
		ext = x;
	}

//Public Methods

	@Override
	public Alignment extendAlignment(Alignment a, double thresh)
	{
		Ontology source = a.getSource();
		Ontology target = a.getTarget();
		src = match(source);
		tgt = match(target);
		Alignment maps = new Alignment(source,target);
		for(Mapping m : src)
		{
			int sourceId = m.getSourceId();
			if(a.containsSource(sourceId))
				continue;
			int medId = m.getTargetId();
			Vector<Integer> matches = tgt.getTargetMappings(medId);
			for(Integer j : matches)
			{
				if(a.containsTarget(j))
					continue;
				double similarity = Math.min(m.getSimilarity(),
						tgt.getSimilarity(j, medId));
				maps.add(new Mapping(sourceId,j,similarity));
			}
		}
		return maps;
	}
	
	/**
	 * @return the intermediate alignment between the mediating and the source ontologies
	 * or null if MediatingMatcher has not been used to match or extendAlignment
	 */
	public Alignment getSourceAlignment()
	{
		return src;
	}

	/**
	 * @return the intermediate alignment between the mediating and the target ontologies
	 * or null if MediatingMatcher has not been used to match or extendAlignment
	 */
	public Alignment getTargetAlignment()
	{
		return tgt;
	}
	
	@Override
	public Alignment match(Ontology source, Ontology target, double thresh)
	{
		src = match(source);
		tgt = match(target);
		Alignment maps = new Alignment(source,target);
		for(Mapping m : src)
		{
			int sourceId = m.getSourceId();
			int medId = m.getTargetId();
			Vector<Integer> matches = tgt.getTargetMappings(medId);
			for(Integer j : matches)
			{
				double similarity = Math.min(m.getSimilarity(),
						tgt.getSimilarity(j, medId));
				maps.add(new Mapping(sourceId,j,similarity));
			}
		}
		return maps;
	}
	
//Private Methods
	
	private Alignment match(Ontology o)
	{
		//Step 1 - Do a xref match
		Alignment a = new Alignment(o, ext);
		ReferenceMap rm = ext.getReferenceMap();
		if(rm != null)
		{
			Set<String> refs = rm.getReferences();
			Vector<String> names = o.getLocalNames();
			for(String r : refs)
			{
				int i = names.indexOf(r);
				if(i > -1)
				{
					Vector<Integer> terms = rm.get(r);
					//Penalize cases where multiple terms have the same xref
					//(note that sim = 1 when the xref is unique) 
					double sim = 1.3 - (terms.size() * 0.3);
					for(Integer j : terms)
						a.add(i, j, sim);
				}
			}
		}
		
		//Step 2 - Do a lexical match
		LexicalMatcher lm = new LexicalMatcher(false);
		Alignment b = lm.match(o,ext,0.0);

		//Step 3 - Compare the two
		//If the coverage of the lexical match is at least double
		//the coverage of the xref match (such as when there are
		//few or no xrefs) return the lexical match
		if(b.sourceCount() > a.sourceCount() * 2)
			return b;
		//Otherwise, the xref match is the preferred choice
		else
			return a;
	}
}