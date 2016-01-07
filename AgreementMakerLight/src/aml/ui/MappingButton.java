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

import java.awt.Color;

import javax.swing.JButton;

import aml.match.Mapping;
import aml.settings.MappingStatus;


public class MappingButton extends JButton
{
	private static final long serialVersionUID = 738835377450644263L;
	
	public MappingButton(Mapping m)
	{
		super(m.toGUI());
		if(m.getStatus().equals(MappingStatus.UNKNOWN))
			this.setBackground(Color.LIGHT_GRAY);
		else if(m.getStatus().equals(MappingStatus.CORRECT))
			this.setBackground(Color.GREEN);
		else if(m.getStatus().equals(MappingStatus.INCORRECT))
			this.setBackground(Color.RED);
		else if(m.getStatus().equals(MappingStatus.FLAGGED))
			this.setBackground(Color.YELLOW);
	}
}