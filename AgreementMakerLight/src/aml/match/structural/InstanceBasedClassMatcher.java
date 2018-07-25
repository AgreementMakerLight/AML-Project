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
* Matches classes based on shared individuals that instantiate them.          *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/

package aml.match.structural;

import java.util.HashSet;
import java.util.Set;

import aml.AML;
import aml.alignment.SimpleAlignment;
import aml.match.AbstractHashMatcher;
import aml.ontology.EntityType;
import aml.ontology.Ontology;
import aml.ontology.semantics.EntityMap;
import aml.util.data.Map2Set;
import aml.util.similarity.Similarity;

public class InstanceBasedClassMatcher extends AbstractHashMatcher
{
	
//Attributes
	
	protected static final String DESCRIPTION = "Matches classes that have a high fraction\n" +
											  "of instances in common.";
	protected static final String NAME = "Instance-Based Class Matcher";
	protected static final EntityType[] SUPPORT = {EntityType.CLASS};
	
//Constructors
	
	public InstanceBasedClassMatcher(){}
	
//Protected Methods
	
	@Override
	public SimpleAlignment hashMatch(Ontology o1, Ontology o2, EntityType e, double thresh)
	{
		SimpleAlignment a = new SimpleAlignment();
		AML aml = AML.getInstance();
		EntityMap rm = aml.getEntityMap();
		System.out.println(rm.instanceCount());
		
		Map2Set<String,String> pairs = new Map2Set<String,String>();
		for(String i : o1.getEntities(EntityType.INDIVIDUAL))
		{
			Set<String> classes = rm.getIndividualClasses(i);
			Set<String> sources = new HashSet<String>();
			Set<String> targets = new HashSet<String>();
			for(String c : classes)
			{
				if(o1.contains(c))
					sources.add(c);
				else if(o2.contains(c))
					targets.add(c);
			}
			for(String s : sources)
				for(String t : targets)
					pairs.add(s, t);
		}
		System.out.println("Pairs: " + pairs.size());
		for(String s : pairs.keySet())
		{
			Set<String> si = rm.getClassIndividuals(s);
			for(String t : pairs.get(s))
			{
				Set<String> ti = rm.getClassIndividuals(t);
				double sim = Similarity.jaccardSimilarity(si, ti);
				if(sim >= thresh)
					a.add(s,t,sim);
			}			
		}
		return a;
	}
}