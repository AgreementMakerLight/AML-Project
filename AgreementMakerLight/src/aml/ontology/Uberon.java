/******************************************************************************
* Copyright 2013-2013 LASIGE                                                  *
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
* Extension to the Ontology class for the Uberon Ontology, with additional    *
* cross-references from the uberon.xref file.                                 *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 22-10-2013                                                            *
******************************************************************************/
package aml.ontology;

import java.io.BufferedReader;
import java.io.FileReader;
import java.net.URI;
import java.util.Vector;

public class Uberon extends Ontology
{
	
//Attributes

	//Paths to the Uberon xrefs
	private final String XREF = "store/uberon.xrefs";
	
//Constructors

	public Uberon(URI path, boolean isInput)
	{
		super(path,isInput);
		if(!isInput)
			extendReferences();
	}
	
//Private Methods
	
	private void extendReferences()
	{
		try
		{
			BufferedReader inStream = new BufferedReader(new FileReader(XREF));
			String line;
			while((line = inStream.readLine()) != null)
			{
				String[] words = line.split("\t");
				if(!refs.contains(words[0]))
					continue;
				Vector<Integer> terms = refs.get(words[0]);
				for(Integer i : terms)
					refs.add(i, words[1]);
			}
			inStream.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
}