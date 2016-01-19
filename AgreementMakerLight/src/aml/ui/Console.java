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
* Java console for the GUI.                                                   *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.ui;

import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.PrintStream;

import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.text.DefaultCaret;


public class Console extends JDialog implements Runnable
{
	
//Attributes
	
	private static final long serialVersionUID = 8550240765482376323L;
	private JTextArea console;
	private ConsoleOutputStream out;
	private PrintStream stdout, stderr;
	
//Constructors
	
	public Console()
	{
		super();
		this.setTitle("Console");
		this.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
		this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		console = new JTextArea(25,50);
		console.setEditable(false);
		out = new ConsoleOutputStream(console);
		stdout = System.out;
		stderr = System.err;
       	System.setOut(new PrintStream(out, true));
       	System.setErr(new PrintStream(out, true));
        JScrollPane scroll = new JScrollPane(console,
        		JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, 
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        DefaultCaret caret = (DefaultCaret)console.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        add(scroll);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        
        this.pack();
		GraphicsEnvironment g = GraphicsEnvironment.getLocalGraphicsEnvironment();
		int left = g.getCenterPoint().x - (int)(this.getPreferredSize().width / 2);
		this.setLocation(left, 0);
    }
	
//Public Methods
	
	public void finish()
	{
		try{ out.close(); }
		catch(IOException e){ e.printStackTrace(); }
       	System.setOut(stdout);
       	System.setErr(stderr);		
		this.setVisible(false);
		this.dispose();
	}

	@Override
	public void run()
	{
		this.setVisible(true);
	}
}