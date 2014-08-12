/******************************************************************************
* Copyright 2013-2014 LASIGE                                                  *
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
* Emulates the SEALS Oracle from the OAEI 2013 Interactive Matching track.    *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 23-06-2014                                                            *
* @version 2.0                                                                *
******************************************************************************/
package aml.oracle;

import java.io.File;
import java.util.Iterator;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import aml.util.Table2Map;

public class Oracle
{

//Attributes	
	
	//The alignment relation types
	public enum Relation
	{
		EQUIVALENCE		("="),
		SUBSUMES		(">"),
		SUBSUMED_BY	("<");
		
		private String rel;
		private Relation(String r) { rel = r; }
		
		public Relation inverse()
		{
			if(rel.equals("<"))
				return Relation.SUBSUMES;
			else if(rel.equals(">"))
				return Relation.SUBSUMED_BY;
			else
				return Relation.EQUIVALENCE;			
		}
	}
	
	//The reference alignment to use in this Oracle
	private Table2Map<String,String,Relation> reference;

//Constructors
	
	/**
	 * Builds an alignment oracle from the given reference alignment file
	 * @param file: the path to the reference alignment to use in this oracle
	 */
	public Oracle(String file)
	{
		reference = new Table2Map<String,String,Relation>();
		
		//Open the alignment file using SAXReader
		SAXReader reader = new SAXReader();
		File f = new File(file);
		try
		{
			Document doc = reader.read(f);
			//Read the root, then go to the "Alignment" element
			Element root = doc.getRootElement();
			Element align = root.element("Alignment");
			//Get an iterator over the mappings
			Iterator<?> map = align.elementIterator("map");
			while(map.hasNext())
			{
				//Get the "Cell" in each mapping
				Element e = ((Element)map.next()).element("Cell");
				if(e == null)
					continue;
				
				//Get the source term
				String sourceURI = e.element("entity1").attributeValue("resource");
				//Get the target term
				String targetURI = e.element("entity2").attributeValue("resource");
				//Get the relation
				String rel = e.elementText("relation");
				//Update the reference alignment
				if(rel.equals("="))
					reference.add(sourceURI, targetURI, Relation.EQUIVALENCE);
				else if(rel.equals("&lt;") || rel.equals("<"))
					reference.add(sourceURI, targetURI, Relation.SUBSUMED_BY);
				else if(rel.equals("&gt;") || rel.equals(">"))
					reference.add(sourceURI, targetURI, Relation.SUBSUMES);				
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
//Public Methods
	
	/**
	 * Checks if a given mapping exists in the reference alignment
	 * @param uri1: the URI of the first mapped entity
	 * @param uri2: the URI of the second mapped entity
	 * @param r: the Relation between the first and second entities
	 * @return whether the reference alignment contains a mapping between uri1 and uri2
	 * with Relation r or a mapping between uri2 and uri1 with the inverse Relation
	 */
	public boolean check(String uri1, String uri2, Relation r)
	{
		return reference.contains(uri1, uri2, r) || reference.contains(uri2, uri1, r.inverse());
	}
}
