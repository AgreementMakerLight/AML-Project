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
* A Mediator between the source and target Ontologies.                        *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.knowledge;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import aml.ontology.lexicon.StringParser;
import aml.util.table.Map2MapComparable;
import aml.util.table.Map2Set;


public class MediatorLexicon
{

//Attributes
	
	//The map of names to entities
	private Map2MapComparable<String,String,Double> entityNames;
	private Map2Set<String,String> nameEntities;
	
//Constructors

	/**
	 * Creates a new empty MediatorLexicon
	 */
	public MediatorLexicon()
	{
		entityNames = new Map2MapComparable<String,String,Double>();
		nameEntities = new Map2Set<String,String>();
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
			String uri = (new File(file)).toURI() + "#" + lex[0];
			String name = lex[1];
			double weight = Double.parseDouble(lex[2]);
			add(uri,name,weight);
		}
		inStream.close();
	}

//Public Methods

	/**
	 * Adds a new entry to the MediatorLexicon
	 * @param uri: the uri of the entity to which the name belongs
	 * @param name: the name to add to the MediatorLexicon
	 * @param weight: the weight of the name for the index entry
	 */
	public void add(String uri, String name, double weight)
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
		if(!entityNames.contains(s,uri) || entityNames.get(s,uri) < weight)
		{
			entityNames.add(s,uri,weight);
			nameEntities.add(uri, s);
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
	public String getBestClass(String name)
	{
		Set<String> hits = getEntities(name);
		if(hits == null)
			return null;
		
		Vector<String> bestClasses = new Vector<String>(1,1);
		double weight;
		double maxWeight = 0.0;
		
		for(String i : hits)
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
			return null;
		return bestClasses.get(0);
	}
	
	/**
	 * @param name: the class name to search in the Lexicon
	 * @return the list of classes associated with the name
	 */
	public Set<String> getEntities(String name)
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
	 * @param uri: the uri of the entity to search in the MediatorLexicon
	 * @return the set of names of the given entity in the MediatorLexicon
	 */
	public Set<String> getNames(String uri)
	{
		if(nameEntities.contains(uri))
			return nameEntities.get(uri);
		return new HashSet<String>();
	}
	
	/**
	 * @param name: the name to search in the MediatorLexicon
	 * @param uri: the uri of the entity to search in the MediatorLexicon
	 * @return the best weight of the name for that entity
	 */
	public double getWeight(String name, String uri)
	{
		if(entityNames.contains(name,uri))
			return entityNames.get(name,uri);
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
	 * @param uri: the uri of the entity to search in the MediatorLexicon
	 * @return the number of names associated with the class
	 */
	public int nameCount(String uri)
	{
		return nameEntities.entryCount(uri);
	}
	
	/**
	 * Saves this MediatorLexicon to the specified file
	 * @param file: the file on which to save the MediatorLexicon
	 */
	public void save(String file) throws Exception
	{
		PrintWriter outStream = new PrintWriter(new FileOutputStream(file));
		for(String i : nameEntities.keySet())
			for(String n : nameEntities.get(i))
				outStream.println(i.substring(i.indexOf("#")+1) + "\t" + n + "\t" + getWeight(n,i));
		outStream.close();
	}
}