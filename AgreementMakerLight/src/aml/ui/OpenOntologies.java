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
* Ontology opening dialog box for the GUI.                                    *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 23-06-2014                                                            *
* @version 2.0                                                                *
******************************************************************************/
package aml.ui;

import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import aml.AML;

public class OpenOntologies extends JDialog implements ActionListener
{

//Attributes
	
	private static final long serialVersionUID = -3900206021275961468L;
	private JPanel dialogPanel, sourcePanel, targetPanel, buttonPanel;
	private JButton selectSource, selectTarget, cancel, open;
    private JTextArea sourcePath, targetPath;
	private JComboBox<String> sourceFormat, targetFormat;
	private final String[] FORMAT = {"OWL","RDFS"};
    private File source = null;
    private File target = null;
    
//Constructor
    
	public OpenOntologies()
	{
		super();
		
		this.setTitle("Open Ontologies");
		this.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);

		selectSource = new JButton("Select Source Ontology");
		selectSource.setPreferredSize(new Dimension(160,28));
		selectSource.addActionListener(this);
		sourcePath = new JTextArea(1,30);
		sourcePath.setEditable(false);
		sourceFormat = new JComboBox<String>(FORMAT);
		sourceFormat.setSelectedIndex(0);
		sourcePanel = new JPanel();
		sourcePanel.add(selectSource);
		sourcePanel.add(sourcePath);
		sourcePanel.add(sourceFormat);
		
		selectTarget = new JButton("Select Target Ontology");
		selectTarget.setPreferredSize(new Dimension(160,28));
		selectTarget.addActionListener(this);
		targetPath = new JTextArea(1,30);
		targetPath.setEditable(false);
		targetFormat = new JComboBox<String>(FORMAT);
		targetFormat.setSelectedIndex(0);
		targetPanel = new JPanel();
		targetPanel.add(selectTarget);
		targetPanel.add(targetPath);
		targetPanel.add(targetFormat);

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
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			AML aml = AML.getInstance();
			aml.openOntologies(source.toString(), target.toString());
			this.dispose();
		}
		//Update the status of the open button
		open.setEnabled(source != null && target != null);
	}
}
