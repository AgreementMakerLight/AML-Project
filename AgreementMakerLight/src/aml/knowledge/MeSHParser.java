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
* Parses the MeSH xml file into a Lexicon.                                    *
* WARNING: Requires the MeSH xml and dtd files, which are not released with   *
* AgreementMakerLight                                                         * 
*                                                                             *
* @author Daniel Faria, Claudia Duarte                                        *
******************************************************************************/
package aml.knowledge;

import java.io.File;
import java.util.Iterator;
import java.util.Vector;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

public class MeSHParser
{
	public static void main(String[] args) throws Exception
	{
		Vector<String> concepts = new Vector<String>();
		MediatorLexicon med = new MediatorLexicon();
		
		SAXReader reader = new SAXReader();
		File f = new File("store/knowledge/mesh.xml");
		Document doc = reader.read(f);
		Element root = doc.getRootElement();
		
		Iterator<?> records = root.elementIterator("DescriptorRecord");
		int index = 0;
		while(records.hasNext())
		{
			Element concList = ((Element)records.next()).element("ConceptList");
			Iterator<?> conc = concList.elementIterator("Concept");
			while(conc.hasNext())
			{
				Element c = (Element)conc.next();
				String conceptName = c.element("ConceptName").elementText("String");
				concepts.add(conceptName);
				med.add(index, conceptName, 0.90);
				
				String casN1Name = c.elementText("CASN1Name");
				if(casN1Name != null)
					med.add(index, casN1Name, 0.85);
					
				Element termList = c.element("TermList");
				Iterator<?> terms = termList.elementIterator("Term");
				while(terms.hasNext())
				{
					Element t = (Element)terms.next();
					String termName = t.elementText("String");
					if(!conceptName.equals(termName))
						med.add(index, termName, 0.85);
				}
				index++;
			}
		}
		med.save("store/knowledge/mesh.lexicon");
	}
}