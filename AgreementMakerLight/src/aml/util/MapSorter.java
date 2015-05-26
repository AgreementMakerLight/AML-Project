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
* Sorts Maps by value rather than by key.                                     *
*                                                                             *
* @author Daniel Faria (adapted from Carter Page)                             *
* @date 23-06-2014                                                            *
******************************************************************************/
package aml.util;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class MapSorter
{
	
//Constructor
	
	private MapSorter(){}
	
//Public Methods
	
	/**
	 * Sorts a Map by values, in ascending order
	 * @param map: the map to sort
	 * @return the sorted (Linked)Map
	 */
    public static <K,V extends Comparable<V>> Map<K,V> sortAscending(Map<K,V> map)
    {
        List<Map.Entry<K,V>> list = new LinkedList<Map.Entry<K,V>>(map.entrySet());
        Collections.sort(list,new Comparator<Map.Entry<K,V>>()
        {
            public int compare(Map.Entry<K,V> o1,Map.Entry<K,V> o2)
            {
                return o1.getValue().compareTo(o2.getValue());
            }
        } );

        Map<K,V> result = new LinkedHashMap<K,V>();
        for (Map.Entry<K,V> entry : list)
        {
            result.put(entry.getKey(),entry.getValue());
        }
        return result;
    }

	/**
	 * Sorts a Map by values, in descending order
	 * @param map: the map to sort
	 * @return the sorted (Linked)Map
	 */
    public static <K,V extends Comparable<V>> Map<K,V> sortDescending(Map<K,V> map)
    {
        List<Map.Entry<K,V>> list = new LinkedList<Map.Entry<K,V>>(map.entrySet());
        Collections.sort(list,new Comparator<Map.Entry<K,V>>()
        {
            public int compare(Map.Entry<K,V> o1,Map.Entry<K,V> o2)
            {
                return -o1.getValue().compareTo(o2.getValue());
            }
        } );

        Map<K,V> result = new LinkedHashMap<K,V>();
        for (Map.Entry<K,V> entry : list)
        {
            result.put(entry.getKey(),entry.getValue());
        }
        return result;
    }
}