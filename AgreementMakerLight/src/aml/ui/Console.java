/******************************************************************************
* Copyright 2013-2014 LASIGE                                                  *
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
* Java console for the GUI.                                                   *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 29-09-2014                                                            *
* @version 2.1                                                                *
******************************************************************************/
package aml.ui;

import java.awt.Cursor;
import java.awt.Dialog;
import java.io.PrintStream;

import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;


public class Console extends JDialog implements Runnable
{
	private static final long serialVersionUID = 8550240765482376323L;
	private JTextArea console;
	
	public Console()
	{
		super();
		this.setTitle("Console");
		this.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
		console = new JTextArea(25,50);
		console.setEditable(false);
		ConsoleOutputStream out = new ConsoleOutputStream(console);
       	System.setOut(new PrintStream(out, true));
       	System.setErr(new PrintStream(out, true));		
        JScrollPane scroll = new JScrollPane(console,
        		JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, 
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        add(scroll);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        this.pack();
    }
	
	public void finish()
	{
		this.setVisible(false);
		this.dispose();
	}

	@Override
	public void run()
	{
		this.setVisible(true);
	}
}