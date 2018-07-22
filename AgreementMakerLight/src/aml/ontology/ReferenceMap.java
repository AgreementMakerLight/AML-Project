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
* The map of cross-references in an Ontology.                                 *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.ontology;


import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Set;

import aml.util.data.Map2Set;

public class ReferenceMap
{
	
//Attributes
	
	//The map of entities <-> cross-reference
	private Map2Set<String,String> entityRefs;
	private Map2Set<String,String> refEntities;

//Constructors
	
	public ReferenceMap()
	{
		entityRefs = new Map2Set<String,String>();
		refEntities = new Map2Set<String,String>();
	}
	
//Public Methods
	
	public void add(String uri, String ref)
	{
		entityRefs.add(uri, ref);
		refEntities.add(ref, uri);
	}

	/**
	 * @param ref: the reference to check in the ReferenceMap
	 * @return whether the ReferenceMap contains the reference
	 */
	public boolean contains(String ref)
	{
		return refEntities.contains(ref);
	}
	
	/**
	 * @param uri: the uri to check in the ReferenceMap
	 * @param ref: the reference to check in the Lexicon
	 * @return whether the Lexicon contains the name for the uri
	 */
	public boolean contains(String uri, String ref)
	{
		return entityRefs.contains(uri) && entityRefs.get(uri).contains(ref);
	}
	
	/**
	 * @param uri: the uri to search in the ReferenceMap
	 * @return the number of external references associated with the uri
	 */
	public int countRefs(String uri)
	{
		return entityRefs.entryCount(uri);
	}
	
	/**
	 * @param ref: the reference to search in the ReferenceMap
	 * @return the number of terms associated with the external reference
	 */
	public int countTerms(String ref)
	{
		return refEntities.entryCount(ref);
	}
	
	/**
	 * Extends this ReferenceMap with the conversions in the given xref file
	 * @param file: the xref file containing the reference conversions
	 */
	public void extend(String file)
	{
		try
		{
			BufferedReader inStream = new BufferedReader(new FileReader(file));
			String line;
			while((line = inStream.readLine()) != null)
			{
				String[] words = line.split("\t");
				if(!contains(words[0]))
					continue;
				Set<String> terms = getEntities(words[0]);
				for(String i : terms)
					add(i, words[1]);
			}
			inStream.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * @param uri: the uri of the entity to search in the ReferenceMap
	 * @return the list of external references associated with the entity
	 */
	public Set<String> getReferences(String uri)
	{
		return entityRefs.get(uri);
	}

	/**
	 * @param ref: the reference to search in the ReferenceMap
	 * @return the list of entities that reference
	 */
	public Set<String> getEntities(String ref)
	{
		return refEntities.get(ref);
	}

	/**
	 * @return the set of references in the ReferenceMap
	 */
	public Set<String> getReferences()
	{
		return refEntities.keySet();
	}

	/**
	 * @return the set of terms in the ReferenceMap
	 */
	public Set<String> getEntities()
	{
		return entityRefs.keySet();
	}
	
	/**
	 * @return the number of references in the ReferenceMap
	 */
	public int refCount()
	{
		return refEntities.keyCount();
	}

	/**
	 * @return the number of entries in the ReferenceMap
	 */
	public int size()
	{
		return refEntities.size();
	}
	
	/**
	 * @return the number of terms in the ReferenceMap
	 */
	public int termCount()
	{
		return entityRefs.keyCount();
	}
}