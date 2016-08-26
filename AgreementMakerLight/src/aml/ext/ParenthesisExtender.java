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
* by removing sections between parenthesis.                                   *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.ext;

import java.util.Vector;

import aml.AML;
import aml.ontology.Lexicon;
import aml.ontology.Provenance;
import aml.settings.EntityType;
import aml.settings.LexicalType;
import aml.util.StringParser;

public class ParenthesisExtender implements LexiconExtender
{
	@Override
	public void extendLexicons()
	{
		AML aml = AML.getInstance();
		Lexicon source = aml.getSource().getLexicon();
		extend(source);
		Lexicon target = aml.getTarget().getLexicon();
		extend(target);
	}
	
	private void extend(Lexicon l)
	{
		for(EntityType e : EntityType.values())
		{
			Vector<String> nm = new Vector<String>(l.getNames(e));
			for(String n: nm)
			{
				if(StringParser.isFormula(n) || !n.contains("(") || !n.contains(")"))
					continue;
				String newName;
				double weight = 0.0;
				if(n.matches("\\([^()]+\\)") || n.contains(") or ("))
				{
					newName = n.replaceAll("[()]", "");
					weight = 1.0;
				}
				else if(n.contains(")("))
					continue;
				else
				{
					newName = "";
					char[] chars = n.toCharArray();
					boolean copy = true;
					for(char c : chars)
					{
						if(c == '(')
							copy = false;
						if(copy)
							newName += c;
						if(c == ')')
							copy = true;					
					}
					newName = newName.trim();
					weight = Math.sqrt(newName.length() * 1.0 / n.length());
				}
				if(newName.equals(""))
					continue;
				//Get the classes with the name
				Vector<Integer> tr = new Vector<Integer>(l.getInternalEntities(e, n));
				for(Integer j : tr)
					for(Provenance p : l.get(n, j))
						l.add(j, newName, p.getLanguage(),
								LexicalType.INTERNAL_SYNONYM, p.getSource(), weight*p.getWeight());
			}
		}
	}
}