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
* JButton extension with preset AML color scheme.                             *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/

package aml.ui;

import javax.swing.JButton;

public class AMLButton extends JButton
{
	
//Attributes
	
	private static final long serialVersionUID = 738835377450644263L;
	
//Constructors
	
	/**
	 * Constructs a new MappingButton for the given Mapping
	 */
	public AMLButton(String text)
	{
		super(text);
		if(text.equals("Reset"))
			this.setBackground(AMLColor.GRAY);
		else if(text.equals("Set Correct"))
			this.setBackground(AMLColor.GREEN);
		else if(text.equals("Set Incorrect"))
			this.setBackground(AMLColor.RED);
		else
			this.setBackground(AMLColor.DARK_GRAY);
	}	
}