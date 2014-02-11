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
* An Ontology property which is an element of the PropertyList.               *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 22-10-2013                                                            *
******************************************************************************/
package aml.ontology;

import java.util.Vector;

import aml.util.StringParser;

public class Property
{
	
//Attributes
	
	private int index;
	private String name;
	private Vector<String> type;
	private Vector<String> domain;
	private Vector<String> range;
	
//Constructors
	
	public Property(int i, String n)
	{
		index = i;
		name = StringParser.normalizeProperty(n);
		type = new Vector<String>(0,1);
		domain = new Vector<String>(0,1);
		range = new Vector<String>(0,1);
	}
	
	public int getIndex()
	{
		return index;
	}
	
	public String getName()
	{
		return name;
	}
	
	public void addDomain(String d)
	{
		domain.add(d);
	}
	
	public Vector<String> getDomain()
	{
		return domain;
	}
	
	public void addRange(String r)
	{
		range.add(r);
	}
	
	public Vector<String> getRange()
	{
		return range;
	}
	
	public void addType(String t)
	{
		type.add(t);
	}
	
	public Vector<String> getType()
	{
		return type;
	}
	
	public String toString()
	{
		String s = "name: " + name;
		s += "\ntype: ";
		for(String t : type)
			s += t + "; ";
		s += "\ndomain: ";
		for(String d : domain)
			s += d + "; ";
		s += "\nrange: ";
		for(String r : range)
			s += r + "; ";
		return s;
	}
}
