/******************************************************************************
* Copyright 2013-2017 LASIGE                                                  *
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
import aml.alignment.Alignment;
import aml.match.PrimaryMatcher;
import aml.match.UnsupportedEntityTypeException;
import aml.ontology.EntityType;
import aml.ontology.Ontology;
import aml.ontology.ReferenceMap;

public class DirectXRefMatcher implements PrimaryMatcher
{
	
//Attributes

	private static final String DESCRIPTION = "Matches entities that have the same cross-reference\n" +
											  "or the where one cross-references the other.";
	private static final String NAME = "Direct Cross-Reference Matcher";
	private static final EntityType[] SUPPORT = {EntityType.CLASS};
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
	public Alignment match(EntityType e, double thresh) throws UnsupportedEntityTypeException
	{
		checkEntityType(e);
		System.out.println("Running " + NAME);
		long time = System.currentTimeMillis()/1000;
		AML aml = AML.getInstance();
		Alignment maps = new Alignment();
		Ontology source = aml.getSource();
		Ontology target = aml.getTarget();
		ReferenceMap sourceRefs = source.getReferenceMap();
		ReferenceMap targetRefs = target.getReferenceMap();
				
		//1 - Check for direct references between the ontologies in one direction
		for(String r : sourceRefs.getReferences())
		{
			//Test the reference with no processing
			int j = target.getIndex(r);
			//If not found, test it after truncating the prexif
			if(j == -1)
			{
				int index = r.indexOf('_');
				if(index < 0)
					continue;
				j = target.getIndex(r.substring(index+1));
				if(j == -1)
					continue;
			}
			for(Integer i : sourceRefs.getEntities(r))
				maps.add(i,j,WEIGHT1);
		}
		//2 - Check for direct references between the ontologies in the opposite direction
		for(String r : targetRefs.getReferences())
		{
			//Test the reference with no processing
			int j = source.getIndex(r);
			//If not found, test it after truncating the prexif
			if(j == -1)
			{
				int index = r.indexOf('_');
				if(index < 0)
					continue;
				j = target.getIndex(r.substring(index+1));
				if(j == -1)
					continue;
			}
			for(Integer i : targetRefs.getEntities(r))
				maps.add(j,i,WEIGHT1);
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
			for(Integer i : smallest.getEntities(r))
			{
				for(Integer j : largest.getEntities(r))
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