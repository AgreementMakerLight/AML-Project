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
* An Ontology property.                                                       *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 23-06-2014                                                            *
* @version 2.0                                                                *
******************************************************************************/
package aml.ontology;

import java.util.Vector;

import aml.util.StringParser;

public class Property
{
	
//Attributes
	
	private int index;
	private String name;
	private String type;
	private Vector<String> domain;
	private Vector<String> range;
	private boolean isFunctional;
	
//Constructors

	public Property(int i, String n, String t)
	{
		index = i;
		name = StringParser.normalizeProperty(n);
		type = t;
		domain = new Vector<String>(0,1);
		range = new Vector<String>(0,1);
		isFunctional = false;
	}
	
	public Property(int i, String n, String t, boolean f)
	{
		index = i;
		name = StringParser.normalizeProperty(n);
		type = t;
		domain = new Vector<String>(0,1);
		range = new Vector<String>(0,1);
		isFunctional = f;
	}
	
//Public Methods

	public void addDomain(String d)
	{
		domain.add(d);
	}
	
	public void addRange(String r)
	{
		range.add(r);
	}
	
	public Vector<String> getDomain()
	{
		return domain;
	}
	
	public int getIndex()
	{
		return index;
	}
	
	public String getName()
	{
		return name;
	}
	
	public Vector<String> getRange()
	{
		return range;
	}
	
	public String getType()
	{
		return type;
	}
	
	public boolean isFunctional()
	{
		return isFunctional;
	}
	
	public void isFunctional(boolean f)
	{
		isFunctional = f;
	}
	
	public String toString()
	{
		String s = "name: " + name + "\ntype: " + type;
		s += "\ndomain: ";
		for(String d : domain)
			s += d + "; ";
		s += "\nrange: ";
		for(String r : range)
			s += r + "; ";
		return s;
	}
}
