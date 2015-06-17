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
* A table with two variable columns and one fixed column, represented by a    *
* HashMap of HashMaps.                                                        *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 23-06-2014                                                            *
******************************************************************************/
package aml.util;

import java.util.HashMap;
import java.util.Set;
import java.util.Vector;

public class Table2Map<A,B,C extends Comparable<C>>
{

//Attributes
	
	private HashMap<A,HashMap<B,C>> multimap;
	private int size;
	
//Constructors

	/**
	 * Constructs a new empty Table
	 */
	public Table2Map()
	{
		multimap = new HashMap<A,HashMap<B,C>>();
		size = 0;
	}
	
	/**
	 * Constructs a new Table that is a copy of
	 * the given Table
	 * @param m: the Table to copy
	 */
	public Table2Map(Table2Map<A,B,C> m)
	{
		multimap = new HashMap<A,HashMap<B,C>>();
		size = m.size;
		Set<A> keys = m.keySet();
		for(A a : keys)
			multimap.put(a, new HashMap<B,C>(m.get(a)));
	}

//Public Methods
	
	/**
	 * Adds the value for the given keys to the Table
	 * If there is already a value for the given keys, the
	 * value will be replaced
	 * @param keyA: the first level key to add to the Table
	 * @param keyB: the second level key to add to the Table
	 * @param valueC: the value for the pair of keys to add to the Table
	 */
	public void add(A keyA, B keyB, C valueC)
	{
		HashMap<B,C> mapsA = multimap.get(keyA);
		if(!contains(keyA,keyB))
			size++;
		if(mapsA == null)
		{
			mapsA = new HashMap<B,C>();
			mapsA.put(keyB, valueC);
			multimap.put(keyA, mapsA);
		}
		else
			mapsA.put(keyB, valueC);
	}
	
	/**
	 * Adds the value for the given keys to the Table
	 * unless there is already a value for the given keys
	 * @param keyA: the first level key to add to the Table
	 * @param keyB: the second level key to add to the Table
	 * @param valueC: the value for the pair of keys to add to the Table
	 */
	public void addIgnore(A keyA, B keyB, C valueC)
	{
		HashMap<B,C> mapsA = multimap.get(keyA);
		if(mapsA == null)
		{
			mapsA = new HashMap<B,C>();
			mapsA.put(keyB, valueC);
			multimap.put(keyA, mapsA);
			size++;
		}
		else if(!mapsA.containsKey(keyB))
		{
			mapsA.put(keyB, valueC);
			size++;
		}
	}
	
	/**
	 * Adds the value for the given keys to the Table
	 * If there is already a value for the given keys, the
	 * new value will replace the previous value only if it
	 * compares favorably as determined by the compareTo test
	 * @param keyA: the first level key to add to the Table
	 * @param keyB: the second level key to add to the Table
	 * @param valueC: the value for the pair of keys to add to the Table
	 */
	public void addUpgrade(A keyA, B keyB, C valueC)
	{
		HashMap<B,C> mapsA = multimap.get(keyA);
		if(mapsA == null)
		{
			mapsA = new HashMap<B,C>();
			mapsA.put(keyB, valueC);
			multimap.put(keyA, mapsA);
			size++;
		}
		else if(!mapsA.containsKey(keyB))
		{
			mapsA.put(keyB, valueC);
			size++;
		}
		else if(mapsA.get(keyB).compareTo(valueC) < 0)
			mapsA.put(keyB, valueC);
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
			multimap.get(keyA).containsKey(keyB);
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
			multimap.get(keyA).containsKey(keyB) &&
			multimap.get(keyA).get(keyB).equals(valueC);
	}

	/**
	 * @param keyA: the first level key to search in the Table
	 * @return the number of entries with keyA
	 */
	public int entryCount(A keyA)
	{
		HashMap<B,C> mapsA = multimap.get(keyA);
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
		HashMap<B,C> mapsA = multimap.get(keyA);
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
	public HashMap<B,C> get(A keyA)
	{
		return multimap.get(keyA);
	}
	
	/**
	 * @param keyA: the first level key to search in the Table
	 * @param keyB: the second level key to search in the Table
	 * @return the value for the entry with the two keys or null
	 * if no such entry exists
	 */	
	public C get(A keyA, B keyB)
	{
		HashMap<B,C> mapsA = multimap.get(keyA);
		if(mapsA == null || !mapsA.containsKey(keyB))
			return null;
		return mapsA.get(keyB);
	}
	
	/**
	 * @param keyA: the first level key to search in the Table
	 * @return the maximum value in entries with keyA
	 */
	public B getKeyMaximum(A keyA)
	{
		HashMap<B,C> mapsA = multimap.get(keyA);
		if(mapsA == null)
			return null;
		Vector<B> setA = new Vector<B>(mapsA.keySet());
		B max = setA.get(0);
		C maxVal = mapsA.get(max);
		for(B b : setA)
		{
			C value = mapsA.get(b);
			if(value.compareTo(maxVal) > 0)
			{
				maxVal = value;
				max = b;
			}
		}
		return max;
	}
	
	/**
	 * @param keyA: the first level key to search in the Table
	 * @param valueC: the value to search in the Table
	 * @return the list of second level keys in entries with keyA and valueC
	 */	
	public Vector<B> getMatchingKeys(A keyA, C valueC)
	{
		Vector<B> keysB = new Vector<B>(0,1);
		HashMap<B,C> mapsA = multimap.get(keyA);
		if(mapsA == null)
			return keysB;
		Set<B> setA = mapsA.keySet();
		for(B b : setA)
			if(mapsA.get(b).equals(valueC))
				keysB.add(b);
		return keysB;
	}
	
	/**
	 * @param keyA: the first level key to search in the Table
	 * @return the maximum value in entries with keyA
	 */
	public C getMaximumValue(A keyA)
	{
		HashMap<B,C> mapsA = multimap.get(keyA);
		if(mapsA == null)
			return null;
		Vector<B> setA = new Vector<B>(mapsA.keySet());
		C max = mapsA.get(setA.get(0));
		for(B b : setA)
		{
			C value = mapsA.get(b);
			if(value.compareTo(max) > 0)
				max = value;
		}
		return max;
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
		HashMap<B,C> mapsA = multimap.get(keyA);
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
		HashMap<B,C> maps = multimap.get(keyA);
		if(maps != null)
		{
			maps.remove(keyB);
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
