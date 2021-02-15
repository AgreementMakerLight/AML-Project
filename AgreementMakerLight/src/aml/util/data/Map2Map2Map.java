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
* A table with three columns, represented by a HashMap of Map2Set.            *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.util.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class Map2Map2Map<A,B,C,D>
{

//Attributes
	
	private HashMap<A,Map2Map<B,C,D>> multimap;
	private int size;
	
//Constructors

	/**
	 * Constructs a new empty Table
	 */
	public Map2Map2Map()
	{
		multimap = new HashMap<A,Map2Map<B,C,D>>();
		size = 0;
	}
	
	/**
	 * Constructs a new Table that is a copy of
	 * the given Table
	 * @param m: the Table to copy
	 */
	public Map2Map2Map(Map2Map2Map<A,B,C,D> m)
	{
		multimap = new HashMap<A,Map2Map<B,C,D>>();
		size = m.size;
		Set<A> keys = m.keySet();
		for(A a : keys)
			multimap.put(a, new Map2Map<B,C,D>(m.get(a)));
	}

//Public Methods
	
	/**
	 * Adds the value for the given keys to the Table
	 * @param keyA: the first level key to add to the Table
	 * @param keyB: the second level key to add to the Table
	 * @param keyC: the third level key to add to the Table
	 * @param valueD: the value for the triple of keys to add to the Table
	 */
	public void add(A keyA, B keyB, C keyC, D valueD)
	{
		Map2Map<B,C,D> mapsA = multimap.get(keyA);
		if(!contains(keyA,keyB,keyC))
			size++;
		if(mapsA == null)
		{
			mapsA = new Map2Map<B,C,D>();
			mapsA.add(keyB, keyC,valueD);
			multimap.put(keyA, mapsA);
		}
		else
			mapsA.add(keyB, keyC, valueD);
	}
	
	/**
	 * @param keyA: the first level key to search in the Table
	 * @return whether the Table contains the first level keyA
	 */
	public boolean contains(A keyA)
	{
		return multimap.containsKey(keyA);
	}

	/**
	 * @param keyA: the first level key to search in the Table
	 * @param keyB: the second level key to search in the Table
	 * @return whether the Table contains an entry with the two keys
	 */
	public boolean contains(A keyA, B keyB)
	{
		return multimap.containsKey(keyA) &&
			multimap.get(keyA).contains(keyB);
	}
	
	/**
	 * @param keyA: the first level key to search in the Table
	 * @param keyB: the second level key to search in the Table
	 * @param keyC: the third level key to search in the Table
	 * @return whether the Table contains an entry with the three keys
	 * and the given value
	 */
	public boolean contains(A keyA, B keyB, C keyC)
	{
		return multimap.containsKey(keyA) &&
			   multimap.get(keyA).contains(keyB, keyC);
	}

	/**
	 * @param keyA: the first level key to search in the Table
	 * @param keyB: the second level key to search in the Table
	 * @param keyC: the third level key to search in the Table
	 * @param valueD: the value to search in the Table
	 * @return whether the Table contains an entry with the three keys
	 * and the given value
	 */
	public boolean contains(A keyA, B keyB, C keyC, D valueD)
	{
		return multimap.containsKey(keyA) &&
			   multimap.get(keyA).contains(keyB, keyC) &&
			   multimap.get(keyA).get(keyB, keyC).equals(valueD);
			
	}
	
	/**
	 * @param keyA: the first level key to search in the Table
	 * @return the number of entries with keyA
	 */
	public int entryCount(A keyA)
	{
		Map2Map<B,C,D> mapsA = multimap.get(keyA);
		if(mapsA == null)
			return 0;
		return mapsA.size();
	}
	
	/**
	 * @param keyA: the first level key to search in the Table
	 * @param valueD: the value to search in the Table
	 * @return the number of entries with keyA that have valueD
	 */
	public int entryCount(A keyA, D valueD)
	{
		int count = 0;
		Map2Map<B,C,D> mapsA = multimap.get(keyA);
		if(mapsA == null)
			return count;
		Set<B> setB = mapsA.keySet();
		for(B b : setB)
			count += mapsA.entryCount(b, valueD);
		return count;
	}
	
	/**
	 * @param keyA: the first level key to search in the Table
	 * @return the HashMap with all entries for keyA
	 */
	public Map2Map<B,C,D> get(A keyA)
	{
		return multimap.get(keyA);
	}
	
	/**
	 * @param keyA: the first level key to search in the Table
	 * @param keyB: the second level key to search in the Table
	 * @return the values for the entries with the two keys or null
	 * if no such entries exist
	 */	
	public HashMap<C,D> get(A keyA, B keyB)
	{
		Map2Map<B,C,D> mapsA = multimap.get(keyA);
		if(mapsA == null || !mapsA.contains(keyB))
			return null;
		return mapsA.get(keyB);
	}
	
	/**
	 * @param keyA: the first level key to search in the Table
	 * @param keyB: the second level key to search in the Table
	 * @param keyC: the third level key to search in the Table
	 * @return the values for the entries with the two keys or null
	 * if no such entries exist
	 */	
	public D get(A keyA, B keyB, C keyC)
	{
		Map2Map<B,C,D> mapsA = multimap.get(keyA);
		if(mapsA == null || !mapsA.contains(keyB))
			return null;
		return mapsA.get(keyB, keyC);
	}
	
	/**
	 * @return the set of first level keys in the Table
	 */
	public Set<A> keySet()
	{
		return multimap.keySet();
	}
	
	/**
	 * @param keyA: the first level key to search in the Table
	 * @return the set of second level keys in all entries with keyA
	 */
	public Set<B> keySet(A keyA)
	{
		Map2Map<B,C,D> mapsA = multimap.get(keyA);
		if(mapsA == null)
			return null;
		return mapsA.keySet();
	}
	
	/**
	 * @return the number of first level keys in the Table
	 */
	public int keyCount()
	{
		return multimap.size();
	}
	
	
	/**
	 * Removes all entries for the given first level key
	 * @param keyA: the key to remove from the Table
	 */
	public void remove(A keyA)
	{
		if(multimap.get(keyA) != null)
			size -= multimap.get(keyA).size();
		multimap.remove(keyA);
	}
	
	/**
	 * Removes the entry for the given key pair
	 * @param keyA: the first level key to search in the Table
	 * @param keyB: the second level key to remove from the Table
	 */
	public void remove(A keyA, B keyB)
	{
		Map2Map<B,C,D> maps = multimap.get(keyA);
		if(maps != null)
		{
			size -= maps.get(keyB).size();
			maps.remove(keyB);
		}
	}
	
	/**
	 * Removes the entry for the given key triple
	 * @param keyA: the first level key to search in the Table
	 * @param keyB: the second level key to remove from the Table
	 * @param keyC: the third level key to remove from the Table
	 */
	public void remove(A keyA, B keyB, C keyC)
	{
		Map2Map<B,C,D> maps = multimap.get(keyA);
		if(maps != null) 
		{
			size--;
			maps.remove(keyB, keyC);
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
