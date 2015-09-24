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
* Uses a stored dictionary or the Microsoft Translator to translate a name    *
* from one given language to another.                                         *
*                                                                             *
* @author Daniel Faria, Joana Pinto, Pedro do Vale                            *
* @date 26-05-2014                                                            *
******************************************************************************/
package aml.translate;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.HashSet;

import aml.AML;
import aml.ontology.Lexicon;
import aml.settings.LexicalType;

public class Dictionary
{

//Attributes
	
	//The dictionary itself
	private HashMap<String, String> dictionary;
	//The path to the root directory with dictionary files
	private final String ROOT = "store/dictionary/";
	//The dictionary file
	private String file;
	//The source and target languages
	private String sourceLang;
	private String targetLang;
	//The translator
	private Translator translator;
	//The attributes for Lexicon extension
	private final LexicalType TYPE = LexicalType.EXTERNAL_MATCH;
	private final String SOURCE = "ms-translator";
	//The control variables
	private boolean useTranslator;
	private boolean haveDictionary;

//Constructors
	
	/**
	 * Constructs a new Dictionary for the given pair of languages
	 * @param sourceLang: the language from which to translate
	 * @param targetLang: the language to which to translate
	 */
	public Dictionary(String sourceLang, String targetLang)
	{
		System.out.println("Opening " + sourceLang + "-" +
				targetLang + " dictionary");
		this.sourceLang = sourceLang;
		this.targetLang = targetLang;
		init();
		if(haveDictionary && useTranslator)
			System.out.println("Using stored dictionary and complement with MS Translator.");
		else if(haveDictionary)
			System.out.println("Using stored dictionary only.");
		else if(useTranslator)
			System.out.println("Using MS Translator only.");
		else
			System.out.println("Warning! Unable to translate.");
		System.out.println("Finished");
	}

//Public Methods
	
	/**
	 * Translates a given Lexicon
	 * @param l: the Lexicon to translate
	 */
	public void translateLexicon(Lexicon l)
	{
		//Initialize the file writer
		BufferedWriter outStream = null;
		//If we can use the Translator, we'll want to save
		//the new translations to the dictionary file
		if(useTranslator)
		{
			try
			{
				outStream = new BufferedWriter(new OutputStreamWriter(
				        new FileOutputStream(file,true), "UTF8"));
			}
			catch(IOException e)
			{
				System.out.println("Unable to save translations to file: " +
						e.getMessage());
			}
		}
		//Get the Lexicon's class names
		HashSet<String> names = new HashSet<String>(l.getNames());
		for(String n : names)
		{
			//Check if they are in the source language
			if(!l.getLanguages(n).contains(sourceLang) || n.equals("null"))
				continue;
			String trans = "";
			//If the dictionary contains the name, get the translation
			if(dictionary.containsKey(n))
			{
				trans = dictionary.get(n);
			}
			//Otherwise use the translator to make the translation
			else if(useTranslator)
			{
				System.out.println("Translating: " + n);
				trans = translator.translate(n, sourceLang, targetLang);
				if(trans.startsWith("ArgumentException") || trans.equals(""))
					continue;
				//Update the dictionary
				dictionary.put(n, trans);
				//Update the dictionary file
				try { outStream.write(n + "\t" + trans + "\n");	}
				catch(IOException e) {/*Do nothing*/}
			}
			//If we have a translation, extend the Lexicon with it
			if(!trans.equals(""))
				for(Integer i : l.getClassesWithLanguage(n,sourceLang))
					l.addClass(i, trans, targetLang, TYPE, SOURCE, l.getWeight(n, i));
		}
		//Get the Lexicon's properties
		HashSet<Integer> props = new HashSet<Integer>(l.getProperties());
		for(int i : props)
		{
			//Get each property's names
			HashSet<String> pNames = new HashSet<String>(l.getNamesWithLanguage(i, sourceLang));
			//And translate them
			for(String n : pNames)
			{
				if(n.equals("null"))
					continue;
				String trans = "";
				//If the dictionary contains the name, get the translation
				if(dictionary.containsKey(n))
				{
					trans = dictionary.get(n);
				}
				//Otherwise use the translator to make the translation
				else if(useTranslator)
				{
					System.out.println("Translating: " + n);
					trans = translator.translate(n, sourceLang, targetLang);
					if(trans.startsWith("ArgumentException") || trans.equals(""))
						continue;
					//Update the dictionary
					dictionary.put(n, trans);
					//Update the dictionary file
					try { outStream.write(n + "\t" + trans + "\n");	}
					catch(IOException e) {/*Do nothing*/}
				}
				//If we have a translation, extend the Lexicon with it
				if(!trans.equals(""))
					l.addProperty(i, trans, targetLang, TYPE, SOURCE, l.getWeight(n, i));
			}
		}
		if(outStream != null)
		{
			try	{ outStream.close(); }
			catch(IOException e) {/*Do nothing*/}
		}
		AML.getInstance().setLanguageSetting();
	}
	
//Private methods
	
	private void init()
	{
		//Initialize the data structure and set the languages
		dictionary = new HashMap<String, String>();
		//Compose the file name
		file = ROOT + sourceLang + "-" + targetLang + ".txt";
		//Open the Translator and check if we can use it
		translator = new Translator();
		useTranslator = translator.isAuthenticated();
		//Read the dictionary file (if exists)
		try
		{
			BufferedReader inStream = new BufferedReader(new InputStreamReader(
					new FileInputStream(file),"UTF8"));
			String line;
			while((line = inStream.readLine()) != null)
			{
				if(!line.contains("\t"))
					continue;
				String[] words = line.split("\t");
				dictionary.put(words[0], words[1]);
			}
			haveDictionary = true;
			inStream.close();
		}
		//This will happen if the dictionary file was not created yet,
		//in which case we'll be in translator mode only
		catch(FileNotFoundException e)
		{
			System.out.println("Dictionary file not found: " + file);
			haveDictionary = false;
		}
		//An encoding problem should not happen unless external files are used
		catch(UnsupportedEncodingException e)
		{
			e.printStackTrace();
			haveDictionary = false;
		}
		//A reading error also shouldn't happen unless external files are used
		catch (IOException e)
		{
			System.out.println("Error reading dictionary file: " + e.getMessage());
			//Check if we've read anything before the error
			haveDictionary = dictionary.size() > 0;
		}
	}
}
