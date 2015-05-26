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
* FileFilter based on file extensions for use in the GUI.                     *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 23-06-2014                                                            *
******************************************************************************/
package aml.util;

import java.io.File;

public class ExtensionFilter extends javax.swing.filechooser.FileFilter implements java.io.FileFilter
{
	
//Attributes
	
	private String ext[];
	private String desc;
	private boolean listDirectories;
	
//Constructors
	
	public ExtensionFilter(String d, String e, boolean dir)
	{
		desc = d;
		ext = new String[] { e };
    	listDirectories = dir;
    }

    public ExtensionFilter(String d, String[] e, boolean dir)
    {
    	desc = d;
    	ext = (String[]) e.clone();
    	listDirectories = dir;
    }

//Public Methods
    
    @Override
    public boolean accept(File file)
    {
		if(file.isDirectory())
    		return listDirectories;
		int count = ext.length;
		String path = file.getAbsolutePath();
		for(int i = 0; i < count; i++)
		{
			String s = ext[i];
			if(path.endsWith(s) && (path.charAt(path.length() - s.length()) == '.'))
				return true;
		}
		return false;
    }

    public String getDescription()
    {
    	if(desc == null)
    		return ext[0];
   		return desc;
    }
}