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
* System-wide Alignment file chooser for the GUI.                             *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 23-06-2014                                                            *
******************************************************************************/
package aml.ui;

import java.io.File;

import javax.swing.JFileChooser;

import aml.util.ExtensionFilter;

public class AlignmentFileChooser extends JFileChooser
{
	
//Attributes
	
	private static final long serialVersionUID = 6575050907005443531L;
	
//Constructors
	
	public AlignmentFileChooser()
	{
		super(new File("store/"));
		setAcceptAllFileFilterUsed(false);
		ExtensionFilter oaei = new ExtensionFilter("OAEI Alignment Files (*.rdf)", new String[] { ".rdf" }, true);
		ExtensionFilter aml = new ExtensionFilter("AML Alignment Files (*.tsv)", new String[] { ".tsv" }, true);
		setFileFilter(aml);
		setFileFilter(oaei);
	}
}
