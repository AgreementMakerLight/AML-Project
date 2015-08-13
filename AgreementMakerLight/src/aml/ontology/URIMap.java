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
* The global map of URIs, numeric indexes, and entity types in the opened     *
* ontologies.                                                                 *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 13-08-2015                                                            *
******************************************************************************/
package aml.ontology;

import java.util.HashMap;
import java.util.Set;

import aml.settings.EntityType;

/**
 * The global map of URIs, numeric indexes, and entity types in the open ontologies.
 */

public class URIMap
{

//Attributes
	
	//The numeric index (Integer) <-> URI (String) maps of ontology entities 
	private HashMap<Integer,String> indexURI;
	private HashMap<String,Integer> URIindex;
	//The numeric index (Integer) -> EntityType map of ontology entities 
	private HashMap<Integer,EntityType> indexType;
	//The total number of stored URIs
	private int size;
	
//Constructors
	
	public URIMap()
	{
		indexURI = new HashMap<Integer,String>();
		URIindex = new HashMap<String,Integer>();
		indexType = new HashMap<Integer,EntityType>();
		size = 0;
	}
	
//Public Methods
	
	/**
	 * @param uri: the URI to add to AML
	 * @return the index of the added URI
	 */
	public int addURI(String uri, EntityType t)
	{
		if(URIindex.containsKey(uri))
			return URIindex.get(uri);
		else
		{
			size++;
			Integer i = new Integer(size);
			indexURI.put(i,uri);
			URIindex.put(uri,i);
			indexType.put(i, t);
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
	 * @param index: the index of the entity to get the name
	 * @return the local name of the entity with the given index
	 */
	public String getLocalName(int index)
	{
		String uri = indexURI.get(index);
		if(uri == null)
			return null;
		int i = uri.indexOf("#") + 1;
		if(i == 0)
			i = uri.lastIndexOf("/") + 1;
		return uri.substring(i);
	}

	/**
	 * @param index: the index of the Ontology entity
	 * @return the EntityType of the input index
	 */
	public EntityType getType(int index)
	{
		return indexType.get(index);
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
	 * @param index: the index of the Ontology entity
	 * @return whether the entity is a Class
	 */
	public boolean isClass(int index)
	{
		return indexType.get(index).equals(EntityType.CLASS);
	}
	
	/**
	 * @param index: the index of the Ontology entity
	 * @return whether the entity is a Property
	 */
	public boolean isProperty(int index)
	{
		return indexType.get(index).equals(EntityType.ANNOTATION) ||
				indexType.get(index).equals(EntityType.DATA) ||
				indexType.get(index).equals(EntityType.OBJECT);
	}
	
	/**
	 * @return the number of entries in the URI map
	 */
	public int size()
	{
		return indexURI.size();
	}
}