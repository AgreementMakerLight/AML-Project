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
* A tuple with 3 elements.                                                    *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.util.data;

public class Triple<A,B,C>
{

//Attributes
	
	private A element1;
	private B element2;
	private C element3;
	
//Constructors

	/**
	 * Constructs a new Triple with the given elements
	 * @param elA: the first element
	 * @param elB: the second element
	 * @param elC: the third element
	 */
	public Triple(A elA, B elB, C elC)
	{
		element1 = elA;
		element2 = elB;
		element3 = elC;
	}
	
//Public Methods
	
	@SuppressWarnings("rawtypes")
	public boolean equals(Object o)
	{
		return o instanceof Triple && element1.equals(((Triple)o).element1) &&
				element2.equals(((Triple)o).element2) && element3.equals(((Triple)o).element3);
	}
	
	/**
	 * @return the first element in the Triple
	 */
	public A get1()
	{
		return element1;
	}
	
	/**
	 * @return the second element in the Triple
	 */
	public B get2()
	{
		return element2;
	}
	
	/**
	 * @return the third element in the Triple
	 */
	public C get3()
	{
		return element3;
	}
}