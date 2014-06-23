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
* The global map of URIs and numeric indexes in the source and target         *
* ontologies.                                                                 *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 23-06-2014                                                            *
* @version 2.0                                                                *
******************************************************************************/
package aml.ontology;

import java.util.HashMap;
import java.util.Set;

public class URIMap
{

//Attributes
	
	//The index <-> URI maps of ontology entities 
	private HashMap<Integer,String> indexURI;
	private HashMap<String,Integer> URIindex;
	//The total number of stored URIs
	private int size;
	
//Constructors
	
	public URIMap()
	{
		indexURI = new HashMap<Integer,String>();
		URIindex = new HashMap<String,Integer>();
		size = 0;
	}
	
//Public Methods
	
	/**
	 * @param uri: the URI to add to AML
	 * @return the index of the added URI
	 */
	public int addURI(String uri)
	{
		if(URIindex.containsKey(uri))
			return URIindex.get(uri);
		else
		{
			size++;
			Integer i = new Integer(size);
			indexURI.put(i,uri);
			URIindex.put(uri,i);
			return size;
		}
	}
	
	/**
	 * @param uri: the URI to search in AML
	 * @return the index of the input URI
	 */
	public int getIndex(String uri)
	{
		if(URIindex.containsKey(uri))
			return URIindex.get(uri);
		else
			return -1;
	}
	
	/**
	 * @return the indexes in the URIMap
	 */
	public Set<Integer> getIndexes()
	{
		return indexURI.keySet();
	}
	
	/**
	 * @param index: the index to search in AML
	 * @return the URI of the input index
	 */
	public String getURI(int index)
	{
		if(indexURI.containsKey(index))
			return indexURI.get(index);
		else
			return null;
	}
	
	/**
	 * @return the URIs in the URIMap
	 */
	public Set<String> getURIS()
	{
		return URIindex.keySet();
	}
	
	/**
	 * @return the number of entries in the URI map
	 */
	public int size()
	{
		return indexURI.size();
	}
}
