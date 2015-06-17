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
* A table with two columns, represented by a HashMap of Vectors.              *
* Adapted from AgreementMakerLight.                                           *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.Vector;

public class Table2List<A,B extends Comparable<B>>
{

//Attributes
	
	private HashMap<A,Vector<B>> multimap;
	private int size;
	
//Constructors

	/**
	 * Constructs a new empty Table
	 */
	public Table2List()
	{
		multimap = new HashMap<A,Vector<B>>();
		size = 0;
	}
	
	/**
	 * Constructs a new Table that is a copy of
	 * the given Table
	 * @param m: the Table to copy
	 */
	public Table2List(Table2List<A,B> m)
	{
		multimap = new HashMap<A,Vector<B>>();
		size = m.size;
		Set<A> keys = m.keySet();
		for(A a : keys)
			multimap.put(a, new Vector<B>(m.get(a)));
	}

//Public Methods
	
	/**
	 * Adds the value for the given key to the Table, or
	 * upgrades the value if an equal value already exists
	 * (and if compareTo and equals differ)
	 * @param key: the key to add to the Table
	 * @param value: the value to add to the Table
	 */
	public void add(A key, B value)
	{
		Vector<B> list = multimap.get(key);
		if(!contains(key,value))
			size++;
		if(list == null)
		{
			list = new Vector<B>(0,1);
			list.add(value);
			multimap.put(key, list);
		}
		else
		{
			int index = list.indexOf(value);
			if(index == -1)
				list.add(value);
			else if(value.compareTo(list.get(index)) > 0)
			{
				list.remove(index);
				list.add(value);
			}
		}
	}
	
	/**
	 * Adds the value for the given key to the Table
	 * @param key: the key to add to the Table
	 * @param values: the value to add to the Table
	 */
	public void addAll(A key, Collection<B> values)
	{
		for(B val : values)
			add(key, val);
	}

	/**
	 * @param key: the key to search in the Table
	 * @return whether the Table contains the key
	 */
	public boolean contains(A key)
	{
		return multimap.containsKey(key);
	}

	/**
	 * @param key: the key to search in the Table
	 * @param value: the value to search in the Table
	 * @return whether the Table contains an entry with the key and value
	 */
	public boolean contains(A key, B value)
	{
		return multimap.containsKey(key) &&
			multimap.get(key).contains(value);
	}
	
	/**
	 * @param key: the key to search in the Table
	 * @return the number of entries with key
	 */
	public int entryCount(A key)
	{
		Vector<B> list = multimap.get(key);
		if(list == null)
			return 0;
		return list.size();
	}
	
	/**
	 * @param key: the key to search in the Table
	 * @return the Vector with all entries for key
	 */
	public Vector<B> get(A key)
	{
		return multimap.get(key);
	}
	
	/**
	 * @return the set of keys in the Table
	 */
	public Set<A> keySet()
	{
		return multimap.keySet();
	}
	
	/**
	 * @return the number of keys in the Table
	 */
	public int keyCount()
	{
		return multimap.size();
	}
	
	/**
	 * Removes all values for the given key
	 * @param key: the key to remove from the Table
	 */
	public void remove(A key)
	{
		if(multimap.get(key) != null)
			size -= multimap.get(key).size();
		multimap.remove(key);
	}
	
	/**
	 * Removes the given value for the given key
	 * @param key: the key to search in the Table
	 * @param value: the value to remove from the Table
	 */
	public void remove(A key, B value)
	{
		Vector<B> values = multimap.get(key);
		if(values != null)
		{
			values.remove(value);
			size--;
		}
	}
	
	/**
	 * @return the total number of entries in the Table
	 */
	public int size()
	{
		return size;
	}
}
