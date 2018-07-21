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
* The global map of URIs and entity types in the open ontologies.             *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.ontology;

import java.util.HashSet;
import java.util.Set;

import aml.util.table.Map2Set;

public class URIMap
{

//Attributes
	
	//The numeric index (Integer) -> EntityType map of ontology entities 
	private Map2Set<String,EntityType> entityType;
	
//Constructors
	
	public URIMap()
	{
		entityType = new Map2Set<String,EntityType>();
	}
	
//Public Methods
	
	/**
	 * @param uri: the uri to add to AML
	 */
	public void addURI(String uri, EntityType t)
	{
		entityType.add(uri,t);
	}
	
	/**
	 * @param uri: the uri to search in the URIMap
	 * @return whether the URIMap contains the uri
	 */
	public boolean contains(String uri)
	{
		return entityType.contains(uri);
	}
	
	/**
	 * @param uri: the uri of the entity to get the name
	 * @return the local name of the entity with the given index
	 */
	public String getLocalName(String uri)
	{
		if(uri == null)
			return null;
		int i = uri.indexOf("#") + 1;
		if(i == 0)
			i = uri.lastIndexOf("/") + 1;
		return uri.substring(i);
	}

	/**
	 * @param uri: the uri of the Ontology entity
	 * @return the EntityType of the input index
	 */
	public Set<EntityType> getMatchableTypes(String uri)
	{
		HashSet<EntityType> types = new HashSet<EntityType>();
		if(entityType.contains(uri))
			for(EntityType e : entityType.get(uri))
				if(e.isMatchable())
					types.add(e);
		return types;
	}

	/**
	 * @param uri: the uri of the Ontology entity
	 * @return the EntityType of the input index
	 */
	public Set<EntityType> getTypes(String uri)
	{
		if(entityType.contains(uri))
			return entityType.get(uri);
		return new HashSet<EntityType>();
	}
	
	/**
	 * @return the URIs in the URIMap
	 */
	public Set<String> getURIS()
	{
		return entityType.keySet();
	}
	
	/**
	 * @param uri: the uri of the Ontology entity
	 * @return whether the entity is a Class
	 */
	public boolean isClass(String uri)
	{
		return entityType.get(uri).contains(EntityType.CLASS);
	}

	/**
	 * @param uri: the uri of the Ontology entity
	 * @return whether the entity is a Data Property
	 */
	public boolean isDataProperty(String uri)
	{
		return entityType.get(uri).contains(EntityType.DATA_PROP);
	}

	/**
	 * @param uri: the uri of the Ontology entity
	 * @return whether the entity is an Individual
	 */
	public boolean isIndividual(String uri)
	{
		return entityType.get(uri).contains(EntityType.INDIVIDUAL);
	}	
	
	/**
	 * @param uri: the uri of the Ontology entity
	 * @return whether the entity is an Object Property
	 */
	public boolean isObjectProperty(String uri)
	{
		return entityType.get(uri).contains(EntityType.OBJECT_PROP);
	}
	
	/**
	 * @return the number of entries in the URI map
	 */
	public int size()
	{
		return entityType.keyCount();
	}
}