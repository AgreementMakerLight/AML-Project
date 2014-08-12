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
* A simple table with two columns, represented by a HashMap of HashSets.      *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 11-08-2014                                                            *
* @version 2.0                                                                *
******************************************************************************/
package aml.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class Table2Set<A,B>
{

//Attributes
	
	private HashMap<A,HashSet<B>> multimap;
	private int size;
	
//Constructors

	/**
	 * Constructs a new empty Table
	 */
	public Table2Set()
	{
		multimap = new HashMap<A,HashSet<B>>();
		size = 0;
	}
	
	/**
	 * Constructs a new Table that is a copy of
	 * the given Table
	 * @param m: the Table to copy
	 */
	public Table2Set(Table2Set<A,B> m)
	{
		multimap = new HashMap<A,HashSet<B>>();
		size = m.size;
		Set<A> keys = m.keySet();
		for(A a : keys)
			multimap.put(a, new HashSet<B>(m.get(a)));
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
		HashSet<B> set = multimap.get(key);
		if(set == null)
		{
			set = new HashSet<B>();
			set.add(value);
			multimap.put(key, set);
			size++;
		}
		else
			set.add(value);
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
		Set<B> set = multimap.get(key);
		if(set == null)
			return 0;
		return set.size();
	}
	
	/**
	 * @param key: the key to search in the Table
	 * @return the set of all entries for key
	 */
	public Set<B> get(A key)
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
		Set<B> values = multimap.get(key);
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
