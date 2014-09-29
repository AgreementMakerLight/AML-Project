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
* Alignment evaluation dialog box for the GUI.                                *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 23-06-2014                                                            *
* @version 2.1                                                                *
******************************************************************************/
package aml.ui;

import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import aml.AML;

public class EvaluateAlignment extends JDialog implements ActionListener
{
	
//Attributes
	
	private static final long serialVersionUID = -4392269920597223842L;
	private JButton selectRef, cancel, evaluate;
    private JTextArea refPath;
    private File ref;

//Constructors
    
	public EvaluateAlignment()
	{
		super();
		//Set the title and modality type
		this.setTitle("Evaluate Alignment");
		this.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);

		selectRef = new JButton("Select Reference Alignment");
		selectRef.addActionListener(this);
		refPath = new JTextArea(1,30);
		refPath.setEditable(false);
		JPanel selectPanel = new JPanel();
		selectPanel.add(selectRef);
		selectPanel.add(refPath);
		
		cancel = new JButton("Cancel");
		cancel.addActionListener(this);
		evaluate = new JButton("Evaluate");
		evaluate.setEnabled(false);
		evaluate.addActionListener(this);
		JPanel buttonPanel = new JPanel();
		buttonPanel.add(cancel);
		buttonPanel.add(evaluate);
		
		JPanel dialogPanel = new JPanel();
		dialogPanel.setLayout(new BoxLayout(dialogPanel, BoxLayout.PAGE_AXIS));
		dialogPanel.add(selectPanel);
		dialogPanel.add(buttonPanel);
		
		add(dialogPanel);
        
        this.pack();
        this.setVisible(true);
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		Object o = e.getSource();
		if(o == cancel)
		{
			this.dispose();
		}
		else if(o == selectRef)
		{
			AlignmentFileChooser fc = AML.getInstance().getAlignmentFileChooser();
			int returnVal = fc.showOpenDialog(this);
			if (returnVal == JFileChooser.APPROVE_OPTION)
			{
				ref = fc.getSelectedFile();
				refPath.setText(ref.getName());
				evaluate.setEnabled(true);
			}
		}
		else if(o == evaluate)
		{
			try{AML.getInstance().openReferenceAlignment(ref.getAbsolutePath());}
			catch(Exception x)
			{
				JOptionPane.showMessageDialog(this, "Could not read alignment file!\n" + 
						x.getMessage(),	"Error", JOptionPane.ERROR_MESSAGE);
			}
			AML.getInstance().evaluate();
			this.dispose();
		}
	}
}
