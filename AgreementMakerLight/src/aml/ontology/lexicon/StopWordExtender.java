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
* An algorithm that extends a Lexicon by removing stop words.                 *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.ontology.lexicon;

import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import aml.ontology.EntityType;
import aml.ontology.Ontology;

public class StopWordExtender implements LexiconExtender
{

//Attributes
	
	private EntityType type;
	private boolean removeAll;
	private Set<String> stopList;
	private final double WEIGHT = 0.98;
	
//Constructors
	
	/**
	 * Constructs a new StopWordExtender for the given EntityType and with
	 * the option to remove all or just leading and trailing stop-words
	 * @param type: the EntityType for which to extend the Lexicon
	 * @param removeAll: whether to remove all or just leading and trailing stop-words
	 */
	public StopWordExtender(EntityType type, boolean removeAll)
	{
		this.type = type;
		this.removeAll = removeAll;
		stopList = StopList.read();
	}
	
//Public Methods
	
	@Override
	public void extendLexicon(Ontology o)
	{
		Lexicon l = o.getLexicon();
		Vector<String> nm = new Vector<String>(l.getNames(type));
		for(String n: nm)
		{
			if(StringParser.isFormula(n))
				continue;
			String[] nameWords = n.split(" ");
			String newName = "";
			if(removeAll)
			{
				for(int i = 0; i < nameWords.length; i++)
					if(!stopList.contains(nameWords[i]))
						newName += nameWords[i] + " ";
				newName = newName.trim();
			}
			//Build a synonym by removing all leading and trailing stopWords
			else
			{
				//First find the first word in the name that is not a stopWord
				int start = 0;
				for(int i = 0; i < nameWords.length; i++)
				{
					if(!stopList.contains(nameWords[i]))
					{
						start = i;
						break;
					}
				}
				//Then find the last word in the name that is not a stopWord
				int end = nameWords.length;
				for(int i = nameWords.length - 1; i > 0; i--)
				{
					if(!stopList.contains(nameWords[i]))
					{
						end = i+1;
						break;
					}
				}
				//If the name contains no leading or trailing stopWords proceed to next name
				if(start == 0 && end == nameWords.length)
					continue;
				//Otherwise build the synonym
				for(int i = start; i < end; i++)
					newName += nameWords[i] + " ";
				newName = newName.trim();
			}
			//If the name is empty or unchanged, skip to next name
			if(newName.equals("") || newName.equals(n))
				continue;
			//Otherwise, gGet the entities with the name
			HashSet<String> tr = new HashSet<String>(l.getInternalEntities(type, n));
			for(String i : tr)
			{
				for(LexicalMetadata p : l.get(n, i))
				{
					double weight = p.getWeight() * WEIGHT;
					l.add(i, newName, p.getLanguage(),
							LexicalType.INTERNAL_SYNONYM, p.getSource(), weight);
				}
			}
		}
	}
}