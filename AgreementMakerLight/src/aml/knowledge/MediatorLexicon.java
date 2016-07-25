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
* A Mediator between the source and target Ontologies.                        *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.knowledge;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import aml.util.Table2Map;
import aml.util.Table2Set;
import aml.util.StringParser;


public class MediatorLexicon
{

//Attributes
	
	//The map of names (String) to entity indexes (Integer)
	private Table2Map<String,Integer,Double> entityNames;
	private Table2Set<Integer,String> nameEntities;
	
//Constructors

	/**
	 * Creates a new empty MediatorLexicon
	 */
	public MediatorLexicon()
	{
		entityNames = new Table2Map<String,Integer,Double>();
		nameEntities = new Table2Set<Integer,String>();
	}
	
	
	/**
	 * Reads a MediatorLexicon from a given Lexicon file
	 * @param file: the MediatorLexicon file
	 */
	public MediatorLexicon(String file) throws IOException
	{
		this();
		BufferedReader inStream = new BufferedReader(new FileReader(file));
		String line;
		while((line = inStream.readLine()) != null)
		{
			String[] lex = line.split("\t");
			int id = Integer.parseInt(lex[0]);
			String name = lex[1];
			double weight = Double.parseDouble(lex[2]);
			add(id,name,weight);
		}
		inStream.close();
	}

//Public Methods

	/**
	 * Adds a new entry to the MediatorLexicon
	 * @param index: the index of the entity to which the name belongs
	 * @param name: the name to add to the MediatorLexicon
	 * @param weight: the weight of the name for the index entry
	 */
	public void add(int classId, String name, double weight)
	{
		//First ensure that the name is not null or empty
		if(name == null || name.equals(""))
			return;

		String s;
		//If it is a formula, parse it and label it as such
		if(StringParser.isFormula(name))
			s = StringParser.normalizeFormula(name);
		//Otherwise, parse it normally
		else
			s = StringParser.normalizeName(name);
		//Then update the table
		if(!entityNames.contains(s, classId) || entityNames.get(s, classId) < weight)
		{
			entityNames.add(s,classId,weight);
			nameEntities.add(classId, s);
		}
	}
	
	/**
	 * @param name: the name to check in the MediatorLexicon
	 * @return whether a class in the MediatorLexicon contains the name
	 */
	public boolean contains(String name)
	{
		return entityNames.contains(name);
	}
	
	/**
	 * @param name: the name to search in the MediatorLexicon
	 * @return the entity associated with the name that has the highest
	 * weight, or -1 if there are either no entities or two or more entities
	 */
	public int getBestClass(String name)
	{
		Set<Integer> hits = getEntities(name);
		if(hits == null)
			return -1;
		
		Vector<Integer> bestClasses = new Vector<Integer>(1,1);
		double weight;
		double maxWeight = 0.0;
		
		for(Integer i : hits)
		{
			weight = getWeight(name,i);
			if(weight > maxWeight)
			{
				maxWeight = weight;
				bestClasses.clear();
				bestClasses.add(i);
			}
			else if(weight == maxWeight)
			{
				bestClasses.add(i);
			}
		}
		if(bestClasses.size() != 1)
			return -1;
		return bestClasses.get(0);
	}
	
	/**
	 * @param name: the class name to search in the Lexicon
	 * @return the list of classes associated with the name
	 */
	public Set<Integer> getEntities(String name)
	{
		return entityNames.keySet(name);
	}
	
	/**
	 * @return the set of class names in the MediatorLexicon
	 */
	public Set<String> getNames()
	{
		return entityNames.keySet();
	}
	
	/**
	 * @param index: the index of the entity to search in the MediatorLexicon
	 * @return the set of names of the given entity in the MediatorLexicon
	 */
	public Set<String> getNames(int index)
	{
		if(nameEntities.contains(index))
			return nameEntities.get(index);
		return new HashSet<String>();
	}
	
	/**
	 * @param name: the name to search in the MediatorLexicon
	 * @param entityId: the entity to search in the MediatorLexicon
	 * @return the best weight of the name for that entity
	 */
	public double getWeight(String name, int entityId)
	{
		if(entityNames.contains(name,entityId))
			return entityNames.get(name, entityId);
		return 0.0;
	}
	
	/**
	 * @return the number of names in the MediatorLexicon
	 */
	public int nameCount()
	{
		return entityNames.keyCount();
	}
	
	/**
	 * @param entityId: the entity to search in the MediatorLexicon
	 * @return the number of names associated with the class
	 */
	public int nameCount(int entityId)
	{
		return nameEntities.entryCount(entityId);
	}
	
	/**
	 * Saves this MediatorLexicon to the specified file
	 * @param file: the file on which to save the MediatorLexicon
	 */
	public void save(String file) throws Exception
	{
		PrintWriter outStream = new PrintWriter(new FileOutputStream(file));
		for(Integer i : nameEntities.keySet())
			for(String n : nameEntities.get(i))
				outStream.println(i + "\t" + n + "\t" + getWeight(n,i));
		outStream.close();
	}
}