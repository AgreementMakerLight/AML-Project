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
* The global map of URIs, numeric indexes, and entity types in the opened     *
* ontologies.                                                                 *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.ontology;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
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
		String newUri = uri;
		if(newUri.contains("%") || newUri.contains("&"))
		{
			try
			{
				newUri = URLDecoder.decode(newUri,"UTF-8");
			}
			catch(UnsupportedEncodingException e)
			{
				//Do nothing
			}
		}
		if(URIindex.containsKey(newUri))
			return URIindex.get(newUri);
		else
		{
			size++;
			Integer i = new Integer(size);
			indexURI.put(i,newUri);
			URIindex.put(newUri,i);
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
		String newUri = uri;
		if(newUri.contains("%") || newUri.contains("&"))
		{
			try
			{
				newUri = URLDecoder.decode(newUri,"UTF-8");
			}
			catch(UnsupportedEncodingException e)
			{
				//Do nothing
			}
		}
		if(URIindex.containsKey(newUri))
			return URIindex.get(newUri);
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
	 * @param index: the index to search in AML
	 * @return the URI of the input index
	 */
	public String getURIEncoded(int index)
	{
		if(!indexURI.containsKey(index))
			return null;
		String uri = indexURI.get(index);
		uri = uri.replace("&","&amp;").replace("&&","&");
		uri = uri.replace("'","&apos;");
		uri = uri.replace("À","%C3%80");
		uri = uri.replace("Á","%C3%81");
		uri = uri.replace("Â","%C3%82");
		uri = uri.replace("Ã","%C3%83");
		uri = uri.replace("Ä","%C3%84");
		uri = uri.replace("Å","%C3%85");
		uri = uri.replace("Æ","%C3%86");
		uri = uri.replace("Ç","%C3%87");
		uri = uri.replace("È","%C3%88");
		uri = uri.replace("É","%C3%89");
		uri = uri.replace("Ê","%C3%8A");
		uri = uri.replace("Ë","%C3%8B");
		uri = uri.replace("Ì","%C3%8C");
		uri = uri.replace("Í","%C3%8D");
		uri = uri.replace("Î","%C3%8E");
		uri = uri.replace("Ï","%C3%8F");
		uri = uri.replace("Ð","%C3%90");
		uri = uri.replace("Ñ","%C3%91");
		uri = uri.replace("Ò","%C3%92");
		uri = uri.replace("Ó","%C3%93");
		uri = uri.replace("Ô","%C3%94");
		uri = uri.replace("Õ","%C3%95");
		uri = uri.replace("Ö","%C3%96");
		uri = uri.replace("Ù","%C3%99");
		uri = uri.replace("Ú","%C3%9A");
		uri = uri.replace("Û","%C3%9B");
		uri = uri.replace("Ü","%C3%9C");
		uri = uri.replace("Ý","%C3%9D");
		uri = uri.replace("Þ","%C3%9E");
		uri = uri.replace("ß","%C3%9F");
		uri = uri.replace("à","%C3%A0");
		uri = uri.replace("á","%C3%A1");
		uri = uri.replace("â","%C3%A2");
		uri = uri.replace("ã","%C3%A3");
		uri = uri.replace("ä","%C3%A4");
		uri = uri.replace("å","%C3%A5");
		uri = uri.replace("æ","%C3%A6");
		uri = uri.replace("ç","%C3%A7");
		uri = uri.replace("è","%C3%A8");
		uri = uri.replace("é","%C3%A9");
		uri = uri.replace("ê","%C3%AA");
		uri = uri.replace("ë","%C3%AB");
		uri = uri.replace("ì","%C3%AC");
		uri = uri.replace("í","%C3%AD");
		uri = uri.replace("î","%C3%AE");
		uri = uri.replace("ï","%C3%AF");
		uri = uri.replace("ð","%C3%B0");
		uri = uri.replace("ñ","%C3%B1");
		uri = uri.replace("ò","%C3%B2");
		uri = uri.replace("ó","%C3%B3");
		uri = uri.replace("ô","%C3%B4");
		uri = uri.replace("õ","%C3%B5");
		uri = uri.replace("ö","%C3%B6");
		uri = uri.replace("ø","%C3%B8");
		uri = uri.replace("ù","%C3%B9");
		uri = uri.replace("ú","%C3%BA");
		uri = uri.replace("û","%C3%BB");
		uri = uri.replace("ü","%C3%BC");
		uri = uri.replace("ý","%C3%BD");
		uri = uri.replace("ÿ","%C3%BF");
		return uri;
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
	 * @return whether the entity is an Individual
	 */
	public boolean isIndividual(int index)
	{
		return indexType.get(index).equals(EntityType.INDIVIDUAL);
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