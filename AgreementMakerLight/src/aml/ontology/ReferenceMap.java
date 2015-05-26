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
* The map of cross-references in an Ontology.                                 *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 23-06-2014                                                            *
******************************************************************************/
package aml.ontology;


import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Set;

import aml.util.Table2Set;

public class ReferenceMap
{
	
//Attributes
	
	//The table of classes (Integer) <-> cross-reference URIs (String)
	Table2Set<Integer,String> termRefs;
	Table2Set<String,Integer> refTerms;

//Constructors
	
	public ReferenceMap()
	{
		termRefs = new Table2Set<Integer,String>();
		refTerms = new Table2Set<String,Integer>();
	}
	
//Public Methods
	
	public void add(int term, String ref)
	{
		termRefs.add(term, ref);
		refTerms.add(ref, term);
	}

	/**
	 * @param ref: the reference to check in the ReferenceMap
	 * @return whether the ReferenceMap contains the reference
	 */
	public boolean contains(String ref)
	{
		return refTerms.contains(ref);
	}
	
	/**
	 * @param term: the term to check in the ReferenceMap
	 * @param ref: the reference to check in the Lexicon
	 * @return whether the Lexicon contains the name for the term
	 */
	public boolean contains(int term, String ref)
	{
		return termRefs.contains(term) && termRefs.get(term).contains(ref);
	}
	
	/**
	 * @param term: the term to search in the ReferenceMap
	 * @return the number of external references associated with the term
	 */
	public int countRefs(int term)
	{
		return termRefs.entryCount(term);
	}
	
	/**
	 * @param ref: the reference to search in the ReferenceMap
	 * @return the number of terms associated with the external reference
	 */
	public int countTerms(String ref)
	{
		return refTerms.entryCount(ref);
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
				Set<Integer> terms = get(words[0]);
				for(Integer i : terms)
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
	 * @param term: the term to search in the ReferenceMap
	 * @return the list of external references associated with the term
	 */
	public Set<String> get(int term)
	{
		return termRefs.get(term);
	}

	/**
	 * @param ref: the reference to search in the ReferenceMap
	 * @return the list of terms for that reference
	 */
	public Set<Integer> get(String ref)
	{
		return refTerms.get(ref);
	}

	/**
	 * @return the set of references in the ReferenceMap
	 */
	public Set<String> getReferences()
	{
		return refTerms.keySet();
	}

	/**
	 * @return the set of terms in the ReferenceMap
	 */
	public Set<Integer> getTerms()
	{
		return termRefs.keySet();
	}
	
	/**
	 * @return the number of references in the ReferenceMap
	 */
	public int refCount()
	{
		return refTerms.keyCount();
	}

	/**
	 * @return the number of entries in the ReferenceMap
	 */
	public int size()
	{
		return refTerms.size();
	}
	
	/**
	 * @return the number of terms in the ReferenceMap
	 */
	public int termCount()
	{
		return termRefs.keyCount();
	}
}