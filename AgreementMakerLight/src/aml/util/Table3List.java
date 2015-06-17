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
* A table with three columns, represented by a HashMap of Table2List.         *
* Adapted from AgreementMakerLight.                                           *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.util;

import java.util.HashMap;
import java.util.Set;
import java.util.Vector;

public class Table3List<A,B,C extends Comparable<C>>
{

//Attributes
	
	private HashMap<A,Table2List<B,C>> multimap;
	private int size;
	
//Constructors

	/**
	 * Constructs a new empty Table
	 */
	public Table3List()
	{
		multimap = new HashMap<A,Table2List<B,C>>();
		size = 0;
	}
	
	/**
	 * Constructs a new Table that is a copy of
	 * the given Table
	 * @param m: the Table to copy
	 */
	public Table3List(Table3List<A,B,C> m)
	{
		multimap = new HashMap<A,Table2List<B,C>>();
		size = m.size;
		Set<A> keys = m.keySet();
		for(A a : keys)
			multimap.put(a, new Table2List<B,C>(m.get(a)));
	}

//Public Methods
	
	/**
	 * Adds the value for the given keys to the Table
	 * @param keyA: the first level key to add to the Table
	 * @param keyB: the second level key to add to the Table
	 * @param valueC: the value for the pair of keys to add to the Table
	 */
	public void add(A keyA, B keyB, C valueC)
	{
		Table2List<B,C> mapsA = multimap.get(keyA);
		if(!contains(keyA,keyB,valueC))
			size++;
		if(mapsA == null)
		{
			mapsA = new Table2List<B,C>();
			mapsA.add(keyB, valueC);
			multimap.put(keyA, mapsA);
		}
		else
			mapsA.add(keyB, valueC);
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
	 * @param valueC: the value to search in the Table
	 * @return whether the Table contains an entry with the two keys
	 * and the given value
	 */
	public boolean contains(A keyA, B keyB, C valueC)
	{
		return multimap.containsKey(keyA) &&
			multimap.get(keyA).contains(keyB) &&
			multimap.get(keyA).get(keyB).contains(valueC);
	}

	/**
	 * @param keyA: the first level key to search in the Table
	 * @return the number of entries with keyA
	 */
	public int entryCount(A keyA)
	{
		Table2List<B,C> mapsA = multimap.get(keyA);
		if(mapsA == null)
			return 0;
		return mapsA.size();
	}
	
	/**
	 * @param keyA: the first level key to search in the Table
	 * @param valueC: the value to search in the Table
	 * @return the number of entries with keyA that have valueC
	 */
	public int entryCount(A keyA, C valueC)
	{
		int count = 0;
		Table2List<B,C> mapsA = multimap.get(keyA);
		if(mapsA == null)
			return count;
		Set<B> setA = mapsA.keySet();
		for(B b : setA)
			if(mapsA.get(b).equals(valueC))
				count++;
		return count;
	}
	
	/**
	 * @param keyA: the first level key to search in the Table
	 * @return the HashMap with all entries for keyA
	 */
	public Table2List<B,C> get(A keyA)
	{
		return multimap.get(keyA);
	}
	
	/**
	 * @param keyA: the first level key to search in the Table
	 * @param keyB: the second level key to search in the Table
	 * @return the value for the entry with the two keys or null
	 * if no such entry exists
	 */	
	public Vector<C> get(A keyA, B keyB)
	{
		Table2List<B,C> mapsA = multimap.get(keyA);
		if(mapsA == null || !mapsA.contains(keyB))
			return null;
		return mapsA.get(keyB);
	}
	
	/**
	 * @param keyA: the first level key to search in the Table
	 * @param valueC: the value to search in the Table
	 * @return the list of second level keys in entries with keyA and valueC
	 */	
	public Vector<B> getMatchingKeys(A keyA, C valueC)
	{
		Vector<B> keysB = new Vector<B>(0,1);
		Table2List<B,C> mapsA = multimap.get(keyA);
		if(mapsA == null)
			return keysB;
		Set<B> setA = mapsA.keySet();
		for(B b : setA)
			if(mapsA.get(b).contains(valueC))
				keysB.add(b);
		return keysB;
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
		Table2List<B,C> mapsA = multimap.get(keyA);
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
		Table2List<B,C> maps = multimap.get(keyA);
		if(maps != null)
		{
			size -= maps.get(keyB).size();
			maps.remove(keyB);
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
