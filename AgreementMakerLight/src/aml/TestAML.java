/******************************************************************************
* Copyright 2013-2013 LASIGE                                                  *
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
* Tests AgreementMakerLight in Eclipse, with manually configured options.     *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 22-10-2013                                                            *
******************************************************************************/
package aml;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import org.apache.log4j.PropertyConfigurator;
import org.dom4j.DocumentException;

import aml.match.Alignment;

public class TestAML
{

//Attributes

	//AgreementMakerLight options
	private static boolean useBK = true;
	private static boolean ignoreUMLS = true;
	private static boolean repair = true;
	
	//Path to input ontology files
	private static String sourcePath = "store/anatomy/mouse.owl";
	private static String targetPath = "store/anatomy/human.owl";

	//Path to reference alignment (if left blank, alignment is not evaluated)
	private static String referencePath = "store/anatomy/reference.rdf";
	
	//Path to output alignment file (if left blank, alignment is not saved)
	private static String alignPath = "store/anatomy.rdf";
	
//Main Method
	
	public static void main(String[] args) throws IOException, DocumentException
	{
		//Configure log4j (writes to store/error.log)
		PropertyConfigurator.configure("log4j.properties");

		URI source = (new File(sourcePath)).toURI();
		URI target = (new File(targetPath)).toURI();
		
		AML aml = new AML(useBK,ignoreUMLS,repair);
		
		//Alignment a = aml.match(source,target);
		Alignment a = aml.match(source,target,referencePath);
		
		if(!referencePath.equals(""))
			System.out.println(aml.evaluate(referencePath));
		
		if(!alignPath.equals(""))
			a.save(alignPath);
	}
}