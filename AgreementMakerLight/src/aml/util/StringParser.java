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
* String classifier and normalizer, which:                                    *
* 1) Identifies and Normalizes Strings that are formulas (i.e. not composed   *
*    by regular words)                                                        *
* 2) Identifies Strings that are alpha-numeric identifiers                    *
* 3) Normalizes Strings that are composed by regular words (including a rule  *
*    based case-change normalization algorithm)                               *
* 4) Normalizes property names                                                *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 23-06-2014                                                            *
******************************************************************************/
package aml.util;

public class StringParser
{

//Constructors
	
	private StringParser(){}
	
//Public Methods

	/**
	 * @param name: the name to analyze
	 * @return whether the name is formula (i.e., not normal text)
	 */
	public static boolean isFormula(String name)
	{
		//A name is a formula if:
		return
			//It doesn't contain at least 3 contiguous letters
			!name.matches(".*[a-zA-Z]{3,}.*") ||
			//It is composed by a block of 3 contiguous letters followed by only non-letters
			name.matches("[a-zA-Z]{3}[^a-zA-Z]+") ||
			//It is composed by a block of lower case letters followed by only upper case letters
			name.matches("[a-z]+[A-Z]+") ||
			//It contains only upper case letters, numbers and dashes but not just upper case letters 
			(name.matches("[A-Z0-9/\\\\-]+") && name.matches(".*[0-9/\\\\-].*")) ||
			//It starts with a digit-dash-word sequence
			name.matches("[0-9]{1,2}'?-[a-zA-Z]{3,}.*") ||
			//or contains a digit-dash-word sequence preceded by a space, comma or dash
			//with possibly a bracket or apostrophe in between
			name.matches(".*[ ,\\-][0-9]{1,2}'?\\)?-[a-zA-Z]{3,}.*");
	}
	
	/**
	 * @param name: the name to analyze
	 * @return whether the name is a numeric or alpha-numeric identifier
	 */
	public static boolean isNumericId(String name)
	{
		return name.matches("[0-9]*") || name.matches("[0-9]+[_:\\.\\-][0-9]+")
				|| name.matches("[a-zA-Z]+[_:]?[a-zA-Z]?[0-9]+")
				|| name.matches("[a-z]{1,3}-[0-9]{3,}-[0-9]{3,}");
	}

	/**
	 * @param formula: the formula to normalize
	 * @return the normalized formula
	 */
	public static String normalizeFormula(String formula)
	{
		//Formulas are parsed to lower case and stripped of
		//underscores but otherwise unprocessed
		String parsed = formula;
		parsed = parsed.toLowerCase();
		parsed = parsed.replace("_"," ");
		//The only exception is that if a formula closes brackets
		//before opening brackets (which happens in NCI) we correct
		//it by opening brackets at the start
		int index1 = parsed.indexOf('(');
		int index2 = parsed.indexOf(')');
		if(index2 > -1 && (index1 == -1 || index1 > index2))
			parsed = "(" + parsed;
		return parsed;
	}
	
	/**
	 * @param s: the name to normalize
	 * @return the normalized name
	 */
	public static String normalizeName(String name)
	{
		//First replace codes with their word equivalents 
		String parsed = name.replace("&amp","and");
		parsed = parsed.replace("(+)","positive");
		parsed = parsed.replace("(-)","negative");
		
		//Then replace all non-word characters with white spaces
		//except for apostrophes and brackets
		parsed = parsed.replaceAll(" *[^a-zA-Z0-9'()] *"," ");
		
		//Then remove multiple, leading and trailing spaces
		parsed = parsed.replaceAll(" {2,}"," ");
		parsed = parsed.trim();
		
		//Then normalize the case changes and return the result
		parsed = normalizeCaseChanges(parsed,false);
		return parsed;
	}
	
	/**
	 * @param s: the name to normalize
	 * @return the normalized name
	 */
	public static String normalizeProperty(String name)
	{
		//First replace codes with their word equivalents 
		String parsed = name.replace("&amp","and");
		//Remove dashes
		parsed = parsed.replaceAll("-","");
		//Then replace all other non-word characters with white spaces
		//except for apostrophes and brackets
		parsed = parsed.replaceAll(" *[^a-zA-Z0-9'()] *"," ");
		
		//Then remove multiple, leading and trailing spaces
		parsed = parsed.replaceAll(" {2,}"," ");
		parsed = parsed.trim();
		
		//Then normalize the case changes and return the result
		parsed = normalizeCaseChanges(parsed,true);
		return parsed;
	}

//Private Methods
	
	private static String normalizeCaseChanges(String name, boolean allChanges)
	{
		//If the name contains no within word case changes
		if(!name.matches(".*[a-z][A-Z].*"))
			//Just convert it to lower case and return it
			return name.toLowerCase();
		//Otherwise initialize the String to return
		String parsed = "";
		//Then split the name into words (by space)
		String[] words = name.split(" ");
		//And run through each word
		for(String w : words)
		{
			String[] subwords = splitOnCaseChanges(w);
			boolean useSubWords = (subwords.length > 1);
			if(subwords.length == 2 && !allChanges)
			{
				for(String s : subwords)
				{
					if(s.length() < 4 &&
							!s.equalsIgnoreCase("a") &&
							!s.equalsIgnoreCase("and") &&
							!s.equalsIgnoreCase("by") &&
							!s.equalsIgnoreCase("has") &&
							!s.equalsIgnoreCase("is") &&
							!s.equalsIgnoreCase("non") &&
							!s.equalsIgnoreCase("or") &&
							!s.equalsIgnoreCase("of") &&
							!s.equalsIgnoreCase("to"))
					{
						useSubWords = false;
						break;
					}
				}
			}
			if(useSubWords)
				for(String s : subwords)
					parsed += " " + s;
			else
				parsed += " " + w.toLowerCase();
		}
		parsed = parsed.substring(1);
		return parsed;
	}

	private static String[] splitOnCaseChanges(String s)
	{
		//Split the input String
		char[] chars = s.toCharArray();
		int max = chars.length-1;
		//Initialize the parsed String with the first character
		String parsed = "" + Character.toLowerCase(chars[0]);
		//Run through the input String
		for(int i = 1 ; i < max; i++)
		{	
			parsed += Character.toLowerCase(chars[i]);
			//If the next character is upper case
			if(Character.isLowerCase(chars[i]) && Character.isUpperCase(chars[i+1]))
				parsed += " ";
		}
		if(max > 0)
			parsed += "" + Character.toLowerCase(chars[max]);
		String[] words = parsed.split(" ");
		return words;		
	}
}