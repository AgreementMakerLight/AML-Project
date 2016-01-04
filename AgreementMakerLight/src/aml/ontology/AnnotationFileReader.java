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
* Reads an annotation file associated with a given ontology, adding the       *
* instances and their class associations to that ontology.                    *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/

package aml.ontology;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import aml.AML;
import aml.settings.EntityType;

public class AnnotationFileReader
{
	//Private constructor so that the class cannot be instantiated
	private AnnotationFileReader(String annotFile){}
	
	/**
	 * Reads the set of instances and associations listed in the given
	 * GO Annotation File (GAF) into the given ontology 
	 * @param annotFile: the GAF formatted annotation file to read 
	 * @param o: the ontology with which the annotation file is associated
	 */
	public static void readGAF(String annotFile, Ontology2Match o)
	{
		AML aml = AML.getInstance();
		URIMap uris = aml.getURIMap();
		RelationshipMap rels = aml.getRelationshipMap();
		
		try
		{
			//Open the input file and read it line by line
			BufferedReader in = new BufferedReader(new FileReader(annotFile));
			String line;
			while((line = in.readLine()) != null)
			{
				//Annotation files typically start with a commented section,
				//with '!' being the comment sign
				if(line.startsWith("!"))
					continue;
				//Split the line into columns
				String[] values = line.split("\t");
				//Columns 1 and 2 contain the main identifier and alternative
				//identifier of the instance, either of which can be used
				//(just ensure the same is used for both source and target ontologies)
				String instance = values[1];
				//Column 4 contains the identifier of the ontology class
				String term = values[4].replace(':', '_');
				//Check that the ontology contains the named class
				if(!o.getLocalNames().contains(term))
					continue;
				//Get the class index
				int classIndex = o.getIndex(term);
				//Add/get the instance index
				int instanceIndex = uris.addURI(instance, EntityType.INDIVIDUAL);
				//Construct the individual and add it to the ontology
				Individual i = new Individual(instanceIndex,instance);
				o.addIndividual(instanceIndex, i);
				//Add the relation between the individual and class
				rels.addInstance(instanceIndex, classIndex);
				//Then a relation between the individual and each superclass
				for(Integer parent : rels.getSuperClasses(classIndex,false))
					rels.addInstance(instanceIndex, parent);
			}
			in.close();
		}
		catch(IOException e)
		{
			System.out.println("Could not read annotation file: " + annotFile);
			e.printStackTrace();			
		}
	}
	
	/**
	 * Reads the set of instances and associations listed in the given
	 * tab delimited text file into the given ontology 
	 * @param annotFile: the TSV annotation file to read 
	 * @param o: the ontology with which the annotation file is associated
	 * @param header: whether the TSV file contains a header
	 */
	public static void readTSV(String annotFile, Ontology2Match o, boolean header)
	{
		AML aml = AML.getInstance();
		URIMap uris = aml.getURIMap();
		RelationshipMap rels = aml.getRelationshipMap();
		
		try
		{
			//Open the input file and read it line by line
			BufferedReader in = new BufferedReader(new FileReader(annotFile));
			String line;
			if(header)
				line = in.readLine();
			while((line = in.readLine()) != null)
			{
				//Split the line into columns
				String[] values = line.split("\t");
				//Column 0 should contain the main identifier of the instance
				String instance = values[0];
				//Column 1 should contains the identifier of the ontology class
				String term = values[1].replace(':', '_');
				//Check that the ontology contains the named class
				if(!o.getLocalNames().contains(term))
					continue;
				//Get the class index
				int classIndex = o.getIndex(term);
				//Add/get the instance index
				int instanceIndex = uris.addURI(instance, EntityType.INDIVIDUAL);
				//Construct the individual and add it to the ontology
				Individual i = new Individual(instanceIndex,instance);
				o.addIndividual(instanceIndex, i);
				//Add the relation between the individual and class
				rels.addInstance(instanceIndex, classIndex);
				//Then a relation between the individual and each superclass
				for(Integer parent : rels.getSuperClasses(classIndex,false))
					rels.addInstance(instanceIndex, parent);
			}
			in.close();
		}
		catch(IOException e)
		{
			System.out.println("Could not read annotation file: " + annotFile);
			e.printStackTrace();			
		}
	}
}