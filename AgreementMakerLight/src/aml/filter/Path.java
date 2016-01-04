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
 * A path between two classes, given as a set of Mapping indexes in the        *
 * RepairMap.                                                                  *
 *                                                                             *
 * @authors Daniel Faria & Emanuel Santos                                      *
 ******************************************************************************/
package aml.filter;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

public class Path implements Comparable<Path>, Iterable<Integer>
{

//Attributes
	
	private HashSet<Integer> path;
	
//Constructors
	
	public Path()
	{
		path = new HashSet<Integer>();
	}
	
	public Path(Integer i)
	{
		path = new HashSet<Integer>();
		add(i);
	}
	
	public Path(Collection<Integer> p)
	{
		path = new HashSet<Integer>(p);
	}
	
	public Path(Path p)
	{
		path = new HashSet<Integer>(p.path);
	}
	
//Public Methods
	
	public void add(int i)
	{
		path.add(i);
	}
	
	public void addAll(Path p)
	{
		path.addAll(p.path);
	}
	
	@Override
	public int compareTo(Path p)
	{
		return path.size()-p.path.size();
	}

	/**
	 * Tests if this path contains all elements of another
	 * given path. Note that it deliberately does not check
	 * the size of the paths, which therefore should be
	 * checked beforehand.
	 * @param p: the path to test for containment by this path
	 * @return whether this path contains all elements of p
	 */
	public boolean contains(Path p)
	{
		for(Integer i : p.path)
			if(!path.contains(i))
				return false;
		return true;
	}
		
	public boolean contains(int m)
	{
		return path.contains(m);
	}
	
	public boolean equals(Object o)
	{
		return o instanceof Path && path.equals(((Path)o).path);
	}

	@Override
	public Iterator<Integer> iterator() 
	{
		return path.iterator();
	}
	
	/**
	 * Merges this path with another given path by
	 * adding all distinct entries and removing all
	 * shared entries, so as to obtain a minimal path
	 * (i.e., this _UNION_ p - this _INTERSECTION_ p)
	 * @param p: the path to merge with this path
	 */
	public void merge(Path p)
	{
		for(Integer i : p)
		{
			if(path.contains(i))
				path.remove(i);
			else
				path.add(i);
		}
	}
	
	public void remove(int i)
	{
		path.remove(i);
	}
	
	public void removeAll(Path p)
	{
		path.removeAll(p.path);
	}
	
	public int size()
	{
		return path.size();
	}
	
	public String toString()
	{
		String a = "[";
		for(int i: path)
			a+= " " + i;
		a+="]";
		return a;
	}	
}
