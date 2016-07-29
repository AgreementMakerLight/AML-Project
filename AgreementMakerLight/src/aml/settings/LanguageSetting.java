/******************************************************************************
* Copyright 2013-2015 LASIGE                                                  *
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
* Lists the Language Settings.                                                *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.settings;

import java.util.HashMap;

import aml.AML;
import aml.ontology.Ontology;

public enum LanguageSetting
{
    SINGLE ("Single-Language Ontologies"),
    MULTI ("Multi-Language Ontologies"),
    TRANSLATE ("Different-Language Ontologies");
	    
    String label;
	    
    LanguageSetting(String s)
    {
    	label = s;
    }
	
	/**
	 * 	Computes and returns the language setting of the matching problem
	 *  based on the language overlap between the input ontologies
	 */
	public static LanguageSetting getLanguageSetting()
	{
		Ontology source = AML.getInstance().getSource();
		Ontology target = AML.getInstance().getTarget();
		HashMap<String,Double> sLangs = new HashMap<String,Double>();
		int sTotal = 0;
		double sMax = 0.0;
		String sLang = "";
		for(String l : source.getLexicon().getLanguages())
		{
			if(!l.equals("Formula"))
			{
				double count = source.getLexicon().getLanguageCount(l);
				sLangs.put(l, count);
				sTotal += count;
				if(count > sMax)
				{
					sMax = count;
					sLang = l;
				}
			}
		}
		sMax /= sTotal;
		for(String l : sLangs.keySet())
			sLangs.put(l, sLangs.get(l)/sTotal);
		//Do the same for the target ontology
		HashMap<String,Double> tLangs = new HashMap<String,Double>();
		int tTotal = 0;
		double tMax = 0.0;
		String tLang = "";
		for(String l : target.getLexicon().getLanguages())
		{
			if(!l.equals("Formula"))
			{
				double count = target.getLexicon().getLanguageCount(l);
				tLangs.put(l, count);
				tTotal += count;
				if(count > tMax)
				{
					tMax = count;
					tLang = l;
				}
			}
		}
		tMax /= (1.0*tTotal);
		for(String l : tLangs.keySet())
			tLangs.put(l, tLangs.get(l)/tTotal);

		//If both ontologies have the same main language, setting is single language
		if(sLang.equals(tLang) && sMax > 0.8 && tMax > 0.8)
			return SINGLE;
		//If the main language of each ontology is not present in the other in significant
		//amount, setting is translate
		else if((sLangs.get(tLang) == null || sLangs.get(tLang) < 0.2) &&
				(tLangs.get(sLang) == null || tLangs.get(sLang) < 0.2))
			return TRANSLATE;
		//Otherwise, setting is multi-language
		else
			return MULTI;
	}
    
    public String toString()
    {
    	return label;
	}
}