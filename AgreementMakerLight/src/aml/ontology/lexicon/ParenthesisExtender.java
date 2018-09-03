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
* An algorithm that extends a Lexicon by removing name sections between       *
* parenthesis.                                                                *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.ontology.lexicon;

import java.util.HashSet;
import java.util.Vector;

import aml.ontology.EntityType;
import aml.ontology.Ontology;

public class ParenthesisExtender implements LexiconExtender
{

//Attributes
	
	private EntityType type;
	
//Constructors
	
	public ParenthesisExtender(EntityType e)
	{
		type = e;
	}
	
//Public Methods
	
	@Override
	public void extendLexicon(Ontology o)
	{
		Lexicon l = o.getLexicon();
		Vector<String> nm = new Vector<String>(l.getNames(type));
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
			HashSet<String> tr = new HashSet<String>(l.getInternalEntities(type, n));
			for(String j : tr)
				for(LexicalMetadata p : l.get(n, j))
					l.add(j, newName, p.getLanguage(),
							LexicalType.INTERNAL_SYNONYM, p.getSource(), weight*p.getWeight());
		}
	}
}