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
* Ontology opening dialog box for the GUI.                                    *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.ui;

import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import aml.AML;

public class OpenOntologies extends JDialog implements ActionListener, Runnable, WindowListener
{

//Attributes
	
	private static final long serialVersionUID = -3900206021275961468L;
	private AML aml;
	private Console c;
	private JPanel dialogPanel, sourcePanel, targetPanel, buttonPanel;
	private JButton selectSource, selectTarget, cancel, open;
    private JTextArea sourcePath, targetPath;
    private File source = null;
    private File target = null;
    private Thread action, console;
    
//Constructor
    
	public OpenOntologies()
	{
		super();
		aml = AML.getInstance();
		this.setTitle("Open Ontologies");
		this.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);

		selectSource = new JButton("Select Source Ontology");
		selectSource.setMinimumSize(new Dimension(175,28));
		selectSource.setPreferredSize(new Dimension(175,28));
		selectSource.addActionListener(this);
		sourcePath = new JTextArea(1,30);
		sourcePath.setEditable(false);
		sourcePanel = new JPanel();
		sourcePanel.add(selectSource);
		sourcePanel.add(sourcePath);
		
		selectTarget = new JButton("Select Target Ontology");
		selectTarget.setMinimumSize(new Dimension(175,28));
		selectTarget.setPreferredSize(new Dimension(175,28));
		selectTarget.addActionListener(this);
		targetPath = new JTextArea(1,30);
		targetPath.setEditable(false);
		targetPanel = new JPanel();
		targetPanel.add(selectTarget);
		targetPanel.add(targetPath);

		cancel = new JButton("Cancel");
		cancel.setPreferredSize(new Dimension(70,28));
		cancel.addActionListener(this);
		open = new JButton("Open");
		open.setPreferredSize(new Dimension(70,28));
		open.setEnabled(false);
		open.addActionListener(this);
		buttonPanel = new JPanel();
		buttonPanel.add(cancel);
		buttonPanel.add(open);
		
		dialogPanel = new JPanel();
		dialogPanel.setLayout(new BoxLayout(dialogPanel, BoxLayout.PAGE_AXIS));
		dialogPanel.add(sourcePanel);
		dialogPanel.add(targetPanel);
		dialogPanel.add(buttonPanel);
		
		add(dialogPanel);
        
		this.pack();
        GraphicsEnvironment g = GraphicsEnvironment.getLocalGraphicsEnvironment();
		int left = g.getCenterPoint().x - (int)(this.getPreferredSize().width / 2);
		this.setLocation(left, 0);
        this.setVisible(true);
	}

//Public Methods
	
	@Override
	public void actionPerformed(ActionEvent e)
	{
		Object o = e.getSource();
		if(o == cancel)
		{
			this.dispose();
		}
		else if(o == selectSource)
		{
			OntologyFileChooser fc = AML.getInstance().getOntologyFileChooser();
			int returnVal = fc.showOpenDialog(this);
			if (returnVal == JFileChooser.APPROVE_OPTION)
			{
				source = fc.getSelectedFile();
				sourcePath.setText(source.getName());
			}
		}
		else if(o == selectTarget)
		{
			OntologyFileChooser fc = AML.getInstance().getOntologyFileChooser();
			int returnVal = fc.showOpenDialog(this);
			if (returnVal == JFileChooser.APPROVE_OPTION)
			{
				target = fc.getSelectedFile();
				targetPath.setText(target.getName());
			}
		}
		else if(o == open)
		{
			c = new Console();
			c.addWindowListener(this);
			console = new Thread(c);
			console.start();
			action = new Thread(this);
			action.start();
		}
		//Update the status of the open button
		open.setEnabled(source != null && target != null);
	}

	@Override
	public void run()
	{
		try
		{
			aml.openOntologies(source.toString(), target.toString());
			Audio.finished();
		}
		catch (OWLOntologyCreationException e)
		{
			System.out.println("ERROR: Could not open ontologies!");
			Audio.error();
			e.printStackTrace();
		}
		try
		{
			Thread.sleep(1500);
		}
		catch(InterruptedException e)
		{
			//Do nothing
		}
		c.finish();
		dispose();
	}
	
	@Override
	public void windowOpened(WindowEvent e){}

	@SuppressWarnings("deprecation")
	@Override
	public void windowClosing(WindowEvent e)
	{
		//Stop should be relatively safe in this case
		action.stop();
		c.finish();
		this.dispose();
	}

	@Override
	public void windowClosed(WindowEvent e){}

	@Override
	public void windowIconified(WindowEvent e){}

	@Override
	public void windowDeiconified(WindowEvent e){}

	@Override
	public void windowActivated(WindowEvent e){}

	@Override
	public void windowDeactivated(WindowEvent e){}
}