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
* Output stream supporting the Console.                                       *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.ui;

import java.io.IOException;
import java.io.OutputStream;

import javax.swing.JTextArea;

public class ConsoleOutputStream extends OutputStream
{
	private JTextArea console;
	
	public ConsoleOutputStream(JTextArea t)
	{
		super();
		console = t;
	}
	
    @Override
    public void write(int b) throws IOException
    {
    	console.append(String.valueOf((char) b));
    }
	    
    @Override
    public void write(byte[] b, int off, int len) throws IOException
    {
    	console.append(new String(b, off, len));
    }
	 
    @Override
    public void write(byte[] b) throws IOException
    {
    	write(b, 0, b.length);
    }
}
