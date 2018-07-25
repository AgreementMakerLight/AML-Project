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
* Matches Ontologies by using cross-references between them.                  *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.match.bk;

import aml.AML;
import aml.alignment.SimpleAlignment;
import aml.match.AbstractMatcher;
import aml.match.PrimaryMatcher;
import aml.ontology.EntityType;
import aml.ontology.Ontology;
import aml.ontology.ReferenceMap;

public class DirectXRefMatcher extends AbstractMatcher implements PrimaryMatcher
{
	
//Attributes

	protected static final String DESCRIPTION = "Matches entities that have the same cross-reference\n" +
											  "or the where one cross-references the other.";
	protected static final String NAME = "Direct Cross-Reference Matcher";
	protected static final EntityType[] SUPPORT = {EntityType.CLASS};
	//The weight used for matching and Lexicon extension
	private final double WEIGHT1 = 0.99;
	private final double WEIGHT2 = 0.95;
	
//Constructors

	/**
	 * Constructs a XRefDirectMatcher with the given external Ontology
	 * @param x: the external Ontology
	 */
	public DirectXRefMatcher(){}

//Public Methods

	@Override
	public SimpleAlignment match(Ontology o1, Ontology o2, EntityType e, double thresh)
	{
		SimpleAlignment maps = new SimpleAlignment();
		if(!checkEntityType(e))
			return maps;
		System.out.println("Running " + NAME);
		long time = System.currentTimeMillis()/1000;
		AML aml = AML.getInstance();
		ReferenceMap sourceRefs = o1.getReferenceMap();
		ReferenceMap targetRefs = o2.getReferenceMap();
				
		//1 - Check for direct references between the ontologies in one direction
		for(String r : sourceRefs.getReferences())
		{
			//Test the reference with no processing
			String t = o2.getURI(r);
			//If not found, test it after truncating the prexif
			if(t == null)
			{
				int index = r.indexOf('_');
				if(index < 0)
					continue;
				t = o2.getURI(r.substring(index+1));
				if(t == null)
					continue;
			}
			for(String s : sourceRefs.getEntities(r))
				maps.add(s,t,WEIGHT1);
		}
		//2 - Check for direct references between the ontologies in the opposite direction
		for(String r : targetRefs.getReferences())
		{
			//Test the reference with no processing
			String s = o1.getURI(r);
			//If not found, test it after truncating the prexif
			if(s == null)
			{
				int index = r.indexOf('_');
				if(index < 0)
					continue;
				s = o1.getURI(r.substring(index+1));
				if(s == null)
					continue;
			}
			for(String t : targetRefs.getEntities(r))
				maps.add(s,t,WEIGHT1);
		}
		//3 - Check for common references of the ontologies
		//Start by determining the smallest ReferenceMap to minimize computations
		ReferenceMap largest, smallest;
		boolean sourceIsSmallest = true;
		if(sourceRefs.size() < targetRefs.size())
		{
			smallest = sourceRefs;
			largest = targetRefs;
		}
		else
		{
			smallest = targetRefs;
			largest = sourceRefs;
			sourceIsSmallest = false;
		}
		for(String r : smallest.getReferences())
		{
			if(!largest.contains(r))
				continue;
			for(String i : smallest.getEntities(r))
			{
				for(String j : largest.getEntities(r))
				{
					if(sourceIsSmallest)
						maps.add(i,j,WEIGHT2);
					else
						maps.add(j,i,WEIGHT2);
				}
			}
		}
		time = System.currentTimeMillis()/1000 - time;
		System.out.println("Finished in " + time + " seconds");
		return maps;
	}
}