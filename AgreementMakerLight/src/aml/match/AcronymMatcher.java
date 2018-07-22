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
* Selector that uses (simulated) user interaction (through the Interaction    *
* Manager) to help perform alignment selection.                               *
*                                                                             *
* @author Amruta Nanavaty, Daniel Faria                                       *
******************************************************************************/

package aml.match;

import java.util.ArrayList;

import aml.AML;
import aml.alignment.Alignment;
import aml.ontology.EntityType;
import aml.ontology.lexicon.Lexicon;
import aml.settings.InstanceMatchingCategory;

public class AcronymMatcher implements PrimaryMatcher
{

//Attributes
	
	private static final String DESCRIPTION = "Matches entities where the Lexicon entry of one\n" +
											  "is an acronym of the Lexicon entry of the other\n";
	private static final String NAME = "Acronym Matcher";
	private static final EntityType[] SUPPORT = {EntityType.CLASS,EntityType.INDIVIDUAL,EntityType.DATA_PROP,EntityType.OBJECT_PROP};
			
//Constructors

	public AcronymMatcher(){}
	
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
		AML aml = AML.getInstance();
		Lexicon sourceLex = aml.getSource().getLexicon();
		Lexicon targetLex = aml.getTarget().getLexicon();
		
		Alignment maps = new Alignment();

		for(String sName : sourceLex.getNames(e))
		{
			//Split the source name into words
			String[] srcWords = sName.split(" ");
			for(String tName : targetLex.getNames(e))
			{
				//Do the same for the target name
				String[] tgtWords = tName.split(" ");
				//Initialize the similarity
				double sim = 0.0;
				//Check whether source or target name is longer (has more words)
				//and put them both into ArrayLists
				ArrayList<String> longer = new ArrayList<String>();
				ArrayList<String> shorter = new ArrayList<String>();
				if(srcWords.length == tgtWords.length)
					continue;
				boolean sourceIsLonger = srcWords.length > tgtWords.length;
				if(sourceIsLonger)
				{
					for(String word : srcWords)
						longer.add(word);
					for(String word : tgtWords)
						shorter.add(word);
				}
				else
				{
					for(String word : srcWords)
						shorter.add(word);
					for(String word : tgtWords)
						longer.add(word);
				}
				int total = longer.size();
				//Check if they have shared words, and remove them
				for(int i = 0; i < shorter.size(); i++)
				{
					String word = shorter.get(i);
					if(longer.remove(word))
					{
						shorter.remove(i--);
						sim += 1.0;
					}
				}
				//We test for accronyms if the shorter name has exactly one word left after the removal step
				//AND that word has either 2 or 3 characters (longer potential acronyms will be ignored due
				//to the risk of being actual words) AND the length of the word corresponds to the number of
				//words left in the longer name
				if(shorter.size() != 1)
					continue;
				String acronym = shorter.get(0);
				if(acronym.length() < 2 || acronym.length() > 3 || acronym.length() != longer.size())
					continue;
				boolean match = true;
				for(int i = 0; i < longer.size(); i++)
				{
					String word = longer.get(i);
					match = word.startsWith(acronym.substring(i,i+1));
					if(match)
						sim += 0.5;
					else
						break;
				}
				if(!match)
					continue;
				sim /= total;
				if(sim >= thresh)
				{
					for(int sourceId : sourceLex.getEntities(e,sName))
					{
						if(e.equals(EntityType.INDIVIDUAL) && !aml.isToMatchSource(sourceId))
							continue;
						for(int targetId : targetLex.getEntities(e,tName))
						{
							if(e.equals(EntityType.INDIVIDUAL) && (!aml.isToMatchTarget(targetId) ||
									(aml.getInstanceMatchingCategory().equals(InstanceMatchingCategory.SAME_CLASSES) &&
									!aml.getEntityMap().shareClass(sourceId,targetId))))
								continue;
							maps.add(sourceId, targetId, sim * 
								Math.sqrt(sourceLex.getCorrectedWeight(sName, sourceId) *
									targetLex.getCorrectedWeight(tName, targetId)));
						}
					}
				}
			}
		}
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