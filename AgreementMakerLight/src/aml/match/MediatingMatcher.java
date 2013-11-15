/******************************************************************************
* Copyright 2013-2013 LASIGE                                                  *
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
* Matches two Ontologies by finding literal full-name matches between their   *
* Lexicons and the Lexicon of a third mediating Ontology, by employing the    *
* LexicalMatcher.                                                             *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 22-10-2013                                                            *
******************************************************************************/
package aml.match;

import java.util.Vector;

import aml.match.LexicalMatcher;
import aml.match.Matcher;
import aml.ontology.Ontology;

public class MediatingMatcher implements Matcher
{

//Attributes

	//The external ontology used as a mediator
	private Ontology ext;
	//Links to the intermediate alignments
	private Alignment src;
	private Alignment tgt;
	
//Constructors

	/**
	 * Constructs a MediatingMatcher with the given external Ontology
	 * @param x: the external Ontology
	 */
	public MediatingMatcher(Ontology x)
	{
		ext = x;
		src = null;
		tgt = null;
	}

//Public Methods
	
	@Override
	public Alignment extendAlignment(Alignment a, double thresh)
	{
		Ontology source = a.getSource();
		Ontology target = a.getTarget();
		LexicalMatcher lm = new LexicalMatcher(false);
		src = lm.match(source,ext,thresh);
		tgt = lm.match(target,ext,thresh);
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
		LexicalMatcher lm = new LexicalMatcher(false);
		src = lm.match(source,ext,thresh);
		tgt = lm.match(target,ext,thresh);
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
}
