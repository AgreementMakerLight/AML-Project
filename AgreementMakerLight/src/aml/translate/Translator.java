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
* Uses Microsoft Translator to translate a name/word from one given language  *
* to another.                                                                 *
* WARNING: Requires a Microsoft access token ID and Password, which are not   *
* released with AgreementMakerLight. For obtaining a Microsoft access token   *
* please see 'http://msdn.microsoft.com/en-us/library/hh454950.aspx', then    *
* store the ID and Password in a file 'store/microsoft-translator-id'.        *
*                                                                             *
* @author Daniel Faria, Joana Pinto, Pedro do Vale                            *
* @date 14-08-2014                                                            *
******************************************************************************/
package aml.translate;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;

import com.memetix.mst.language.Language;
import com.memetix.mst.translate.Translate;

public class Translator
{
	
//Attributes
	
	private HashMap<String,Language> lang;
	private boolean authenticated;

//Constructors
	
	/**
	 * Constructs a new Translator
	 */
	public Translator()
	{
		File f = new File("store/microsoft-translator-id");
		if(f.exists())
		{
			try
			{
				BufferedReader inStream = new BufferedReader(new FileReader(f));
				String id = inStream.readLine();
				String password = inStream.readLine();
				Translate.setClientId(id);
				Translate.setClientSecret(password);
				authenticated = true;
				inStream.close();
			}
			catch(Exception e)
			{
				System.out.println("Error: could not authenticate Microsoft Translator!");
				System.out.println(e.getMessage());
				authenticated = false;
			}
		}
		else
		{
			System.out.println("Error: could not authenticate Microsoft Translator!");
			authenticated = false;
		}
		
		lang = new HashMap<String,Language>();
		lang.put("ar", Language.ARABIC);
		lang.put("zh", Language.CHINESE_SIMPLIFIED);
		lang.put("cs", Language.CZECH);
		lang.put("de", Language.GERMAN);
		lang.put("en", Language.ENGLISH);
		lang.put("es", Language.SPANISH);
		lang.put("fr", Language.FRENCH);
		lang.put("nl", Language.DUTCH);
		lang.put("pt", Language.PORTUGUESE);
		lang.put("ru", Language.RUSSIAN);
		lang.put("it", Language.ITALIAN);
	}

//Public Methods
	
	/**
	 * @return Whether this translator is authenticated
	 */
	public boolean isAuthenticated()
	{
		return authenticated;
	}
	
	/**
	 * Translates a given name/word
	 * @param name: the name to translate
	 * @param sourceLang: the language of the name 
	 * @param targetLang: the language to which to translate the name
	 * @return the name translated into targetLang
	 */
	public String translate(String name, String sourceLang, String targetLang)
	{
		String translation = "";
		if(!authenticated)
			return translation;
		try
		{
			translation = Translate.execute(name, lang.get(sourceLang), lang.get(targetLang));
		}
		catch(Exception e)
		{
			System.out.println("Error: could not process translation - " + e.getMessage());
		}
		return translation;
	}
}
