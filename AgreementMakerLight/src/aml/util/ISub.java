/******************************************************************************
* Copyright 2004-2016 by the National and Technical University of Athens      *
*                                                                             *
* This program is free software: you can redistribute it and/or modify it     *
* under the terms of the GNU Lesser General Public License as published by    *
* the Free Software Foundation, either version 2 of the License, or (at your  *
* option) any later version.                                                  *
*                                                                             *
* This program is distributed in the hope that it will be useful, but         *
* WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY  *
* or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public      *
* License for more details.                                                   *
*                                                                             *
* You should have received a copy of the GNU Lesser General Public License    *
* along with this program. If not, see <http://www.gnu.org/licenses/>.        *
*                                                                             *
*******************************************************************************
* ISub String similarity measure.                                             *
*                                                                             *
* @author Giorgos Stoilos (adapted and modified by Daniel Faria)              *
******************************************************************************/
package aml.util;

public class ISub
{
	
//Constructors
	
	private ISub(){}
	
//Public Methods
	
	/**
	 * Computes the similarity between two Strings
	 * @param st1: the first string to compare
	 * @param st2: the second string to compare
	 * @return the ISub similarity between st1 and st2
	 */
	public static double stringSimilarity(String st1, String st2)
	{
		String s1 = st1, s2 = st2;
		if(s1 == null || s2 == null)
			return 0;
		int L1 = s1.length(), L2 = s2.length();
		if(L1 == 0 || L2 == 0)
			return 0;
		int l1, l2;
		double common = 0;
		int best = 2;
		while (s1.length() > 0 && s2.length() > 0 && best != 0)
		{
			best = 0;
			l1 = s1.length();
			l2 = s2.length();
			int i = 0, j = 0;
			int startS1 = 0, endS1 = 0;
			int startS2 = 0, endS2 = 0;
			int p = 0;
			for(i = 0; (i < l1) && (l1 - i > best); i++)
			{
				j = 0;
				while (l2 - j > best)
				{
					int k = i;
					while(j < l2 && s1.charAt(k) != s2.charAt(j))
						j++;
					if(j != l2)
					{
						p = j;
						for(j++, k ++; (j < l2) && (k < l1) && (s1.charAt(k) == s2.charAt(j)); j++, k++);
						if(k - i > best)
						{
							best = k - i;
							startS1 = i;
							endS1 = k;
							startS2 = p;
							endS2 = j;
						}
					}
				}
			}
			char[] newString = new char[s1.length() - (endS1 - startS1)];
			j = 0;
			for(i = 0; i < s1.length(); i++)
			{
				if(i >= startS1 && i < endS1)
					continue;
				newString[j++] = s1.charAt(i);
			}
			s1 = new String(newString);
			newString = new char[s2.length() - (endS2 - startS2)];
			j = 0;
			for(i = 0; i < s2.length(); i++)
			{
				if(i >= startS2 && i < endS2)
					continue;
				newString[j++] = s2.charAt(i);
			}
			s2 = new String(newString);
			if(best > 2)
				common += best;
			else
				best = 0;
		}
		double commonality = 0;
		double scaledCommon = (double) (2 * common) / (L1 + L2);
		commonality = scaledCommon;
		double winklerImprovement = winklerImprovement(st1, st2, commonality);
		double dissimilarity = 0;
		double rest1 = L1 - common;
		double rest2 = L2 - common;
		double unmatchedS1 = Math.max(rest1, 0);
		double unmatchedS2 = Math.max(rest2, 0);
		unmatchedS1 = rest1 / L1;
		unmatchedS2 = rest2 / L2;
		double suma = unmatchedS1 + unmatchedS2;
		double product = unmatchedS1 * unmatchedS2;
		double p = 0.6;
		if((suma - product) == 0)
			dissimilarity = 0;
		else
			dissimilarity = (product) / (p + (1 - p) * (suma - product));
		double result = commonality - dissimilarity + winklerImprovement; 
		if(result < 0)
			result = 0;
		return result;
	}

//Private Methods
	
	private static double winklerImprovement(String s1, String s2, double commonality)
	{
		int i, n = Math.min(s1.length(), s2.length());
		for(i = 0; i < n; i++)
			if(s1.charAt(i) != s2.charAt(i))
				break;
		double commonPrefixLength = Math.min(4, i);
		double winkler = commonPrefixLength * 0.1 * (1 - commonality);
		return winkler;
	}
}