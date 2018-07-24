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

package aml.match.structural;

import java.util.HashSet;
import java.util.Set;

import aml.AML;
import aml.alignment.SimpleAlignment;
import aml.match.PrimaryMatcher;
import aml.match.UnsupportedEntityTypeException;
import aml.ontology.EntityType;
import aml.ontology.Ontology;
import aml.ontology.semantics.EntityMap;
import aml.util.data.Map2Set;
import aml.util.similarity.Similarity;

public class InstanceBasedClassMatcher implements PrimaryMatcher
{
	
//Attributes
	
	private static final String DESCRIPTION = "Matches classes that have a high fraction\n" +
											  "of instances in common.";
	private static final String NAME = "Instance-Based Class Matcher";
	private static final EntityType[] SUPPORT = {EntityType.CLASS};
	
//Constructors
	
	public InstanceBasedClassMatcher(){}
	
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
	public SimpleAlignment match(EntityType e, double thresh) throws UnsupportedEntityTypeException
	{
		checkEntityType(e);
		SimpleAlignment a = new SimpleAlignment();
		AML aml = AML.getInstance();
		Ontology source = aml.getSource();
		Ontology target = aml.getTarget();
		EntityMap rm = aml.getEntityMap();
		System.out.println(rm.instanceCount());
		
		Map2Set<Integer,Integer> pairs = new Map2Set<Integer,Integer>();
		for(int i : source.getEntities(EntityType.INDIVIDUAL))
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
				double sim = Similarity.jaccardSimilarity(si, ti);
				if(sim >= thresh)
					a.add(s,t,sim);
			}			
		}
		return a;
	}
	
//Private Methods
	
	private void checkEntityType(EntityType e) throws UnsupportedEntityTypeException
	{
		boolean check = false;
		for(EntityType t : SUPPORT)
		{
			if(t.equals(e))
			{
				check = true;
				break;
			}
		}
		if(!check)
			throw new UnsupportedEntityTypeException(e.toString());
	}
}