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
* JButton extension for representing a Mapping in the GUI.                    *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/

package aml.ui;

import javax.swing.JButton;

import aml.match.Mapping;
import aml.settings.MappingStatus;


public class MappingButton extends JButton
{
	
//Attributes
	
	private static final long serialVersionUID = 738835377450644263L;
	private Mapping m;
	
//Constructors
	
	/**
	 * Constructs a new MappingButton for the given Mapping
	 * @param m: the Mapping to "buttonize"
	 */
	public MappingButton(Mapping m)
	{
		super(m.toGUI());
		this.m = m;
		if(m.getStatus().equals(MappingStatus.UNKNOWN))
			this.setBackground(AMLColor.GRAY);
		else if(m.getStatus().equals(MappingStatus.CORRECT))
			this.setBackground(AMLColor.GREEN);
		else if(m.getStatus().equals(MappingStatus.INCORRECT))
			this.setBackground(AMLColor.RED);
		else if(m.getStatus().equals(MappingStatus.FLAGGED))
			this.setBackground(AMLColor.ORANGE);
	}
	
	public void refresh()
	{
		if(m.getStatus().equals(MappingStatus.UNKNOWN))
			this.setBackground(AMLColor.GRAY);
		else if(m.getStatus().equals(MappingStatus.CORRECT))
			this.setBackground(AMLColor.GREEN);
		else if(m.getStatus().equals(MappingStatus.INCORRECT))
			this.setBackground(AMLColor.RED);
		else if(m.getStatus().equals(MappingStatus.FLAGGED))
			this.setBackground(AMLColor.ORANGE);
	}
}