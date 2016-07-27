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
* An algorithm that extends the Lexicons of the source and target ontologies  *
* by removing stop words.                                                     *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.ext;

import java.util.Set;
import java.util.Vector;

import aml.AML;
import aml.ontology.Lexicon;
import aml.ontology.Provenance;
import aml.settings.EntityType;
import aml.settings.LexicalType;
import aml.settings.SizeCategory;
import aml.util.StopList;
import aml.util.StringParser;

public class StopWordExtender implements LexiconExtender
{
	//The source of this LexiconExtender
	private SizeCategory s;
	private Set<String> stopList;
	
	@Override
	public void extendLexicons()
	{
		stopList = StopList.read();
		AML aml = AML.getInstance();
		s = aml.getSizeCategory();
		Lexicon source = aml.getSource().getLexicon();
		extend(source);
		Lexicon target = aml.getTarget().getLexicon();
		extend(target);
	}
	
	private void extend(Lexicon l)
	{
		//Process Classes (remove only leading and trailing stop words)
		Vector<String> nm = new Vector<String>(l.getNames(EntityType.CLASS));
		for(String n: nm)
		{
			if(StringParser.isFormula(n))
				continue;
			//Build a synonym by removing all leading and trailing stopWords
			String[] nameWords = n.split(" ");
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
			String newName = "";
			for(int i = start; i < end; i++)
				newName += nameWords[i] + " ";
			newName = newName.trim();

			//Get the entities with the name
			Vector<Integer> tr = new Vector<Integer>(l.getInternalEntities(EntityType.CLASS, n));
			for(Integer i : tr)
			{
				for(Provenance p : l.get(n, i))
				{
					double weight = p.getWeight() * 0.9;
					l.add(i, newName, p.getLanguage(),
							LexicalType.INTERNAL_SYNONYM, p.getSource(), weight);
				}
			}
		}
		EntityType[] types = EntityType.values();
		//Process Individuals and Properties (remove all stop words)
		//If the SizeCategory is Large or Huge, process Classes this
		//was as well
		int start = 1;
		if(s.equals(SizeCategory.LARGE) || s.equals(SizeCategory.HUGE))
			start = 0;
		for(int h = start; h < types.length; h++)
		{
			nm = new Vector<String>(l.getNames(types[h]));
			for(String n: nm)
			{
				if(StringParser.isFormula(n))
					continue;
				//Build a synonym by removing all leading and trailing stopWords
				String[] nameWords = n.split(" ");
				String newName = "";
				for(int i = 0; i < nameWords.length; i++)
					if(!stopList.contains(nameWords[i]))
						newName += nameWords[i] + " ";
				newName = newName.trim();
	
				//Get the entities with the name
				Vector<Integer> tr = new Vector<Integer>(l.getInternalEntities(types[h], n));
				for(Integer i : tr)
				{
					for(Provenance p : l.get(n, i))
					{
						double weight = p.getWeight() * 0.9;
						l.add(i, newName, p.getLanguage(),
								LexicalType.INTERNAL_SYNONYM, p.getSource(), weight);
					}
				}
			}
		}
	}
}