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
* Matches classes based on shared individuals that instantiate them.          *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/

package aml.match;

import java.util.HashSet;
import java.util.Set;

import aml.AML;
import aml.ontology.Ontology2Match;
import aml.ontology.RelationshipMap;
import aml.util.Similarity;
import aml.util.Table2Set;

public class InstanceBasedClassMatcher implements PrimaryMatcher
{
	@Override
	public Alignment match(double thresh)
	{
		Alignment a = new Alignment();
		AML aml = AML.getInstance();
		Ontology2Match source = aml.getSource();
		Ontology2Match target = aml.getTarget();
		RelationshipMap rm = aml.getRelationshipMap();
		System.out.println(rm.instanceCount());
		
		Table2Set<Integer,Integer> pairs = new Table2Set<Integer,Integer>();
		for(int i : source.getIndividuals())
		{
			Set<Integer> classes = rm.getIndividualClasses(i);
			Set<Integer> sources = new HashSet<Integer>();
			Set<Integer> targets = new HashSet<Integer>();
			for(int c : classes)
			{
				if(source.contains(c))
					sources.add(c);
				else if(target.contains(c))
					targets.add(c);
			}
			for(int s : sources)
				for(int t : targets)
					pairs.add(s, t);
		}
		System.out.println("Pairs: " + pairs.size());
		for(int s : pairs.keySet())
		{
			Set<Integer> si = rm.getClassIndividuals(s);
			for(int t : pairs.get(s))
			{
				Set<Integer> ti = rm.getClassIndividuals(t);
				double sim = Similarity.jaccard(si, ti);
				if(sim >= thresh)
					a.add(s,t,sim);
			}			
		}
		return a;
	}
}