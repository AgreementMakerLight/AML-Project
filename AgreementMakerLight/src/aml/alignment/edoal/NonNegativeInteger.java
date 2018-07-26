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
* A non-negative integer Literal.                                             *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.alignment.edoal;


public class NonNegativeInteger extends Literal
{

//Attributes
	
	private static final String TYPE = "&xsd;integer";
	
//Constructor

	/**
	 * Constructs a new non-negative int Literal
	 * @param value: the value of the Literal (must be a non-negative int)
	 */
	public NonNegativeInteger(int value)
	{
		super(TYPE, null, "" + value);
	}

//Public Methods
	
	@Override
	public boolean equals(Object o)
	{
		if(o instanceof NonNegativeInteger)
		{
			NonNegativeInteger l = (NonNegativeInteger)o;
			return l.value.equals(this.value);
		}
		else
			return false;
	}
}