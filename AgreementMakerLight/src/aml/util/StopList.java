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
* Reads the list of stop words to use in AgreementMakerLight from the         *
* StopList.txt file.                                                          *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 23-06-2014                                                            *
******************************************************************************/
package aml.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashSet;
import java.util.Set;

public class StopList
{
	
//Attributes
	
	private static String file = "store/StopList.txt";
	
//Constructors
	
	private StopList(){}
	
//Public Methods
	
	public static Set<String> read()
	{
		HashSet<String> stopWords = new HashSet<String>();
		try
		{
			BufferedReader inStream = new BufferedReader(new FileReader(file));
			String line;
			while((line = inStream.readLine()) != null)
				stopWords.add(line);
			inStream.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return stopWords;
	}
}
