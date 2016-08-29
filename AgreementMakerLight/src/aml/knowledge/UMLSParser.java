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
* Parses the UMLS MRCONSO.RRF file into a lexicon file, which can be read by  *
* class Mediator. Requires the UMLS MRCONSO.RRF file, which is not released   *
* with AgreementMakerLight. Please see https://www.nlm.nih.gov/research/umls/ *
* for instructions on obtaining a UMLS license.                               * 
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.knowledge;

import java.io.BufferedReader;
import java.io.FileReader;

import aml.settings.LexicalType;
import aml.util.Table2Set;

public class UMLSParser
{
	public static void main(String[] args) throws Exception
	{
		Table2Set<Integer,String> termSources = new Table2Set<Integer,String>();
		MediatorLexicon med = new MediatorLexicon();
		
		BufferedReader inStream = new BufferedReader(new FileReader("store/knowledge/MRCONSO.RRF"));
		String line;
		while((line = inStream.readLine()) != null)
		{
			String[] cols = line.split("\\|");
			int id = Integer.parseInt(cols[0].substring(1));
			String source = cols[11];
			String name = cols[14];
			LexicalType type = LexicalType.LABEL;
			if(termSources.contains(id, source))
				type = LexicalType.EXACT_SYNONYM;
			else
				termSources.add(id, source);
			double weight = type.getDefaultWeight();
			med.add(id,name,weight);
		}
		inStream.close();
		med.save("store/knowledge/UMLS.lexicon");
	}
}