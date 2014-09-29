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
* Mapping selection dialog box for the GUI.                                   *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 23-06-2014                                                            *
* @version 2.1                                                                *
******************************************************************************/
package aml.ui;

import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import aml.AML;
import aml.match.Alignment;
import aml.match.Mapping;

public class SelectMapping extends JDialog implements ActionListener
{

//Attributes
	
	private static final long serialVersionUID = -3901206021275961468L;
	private JButton cancel, ok;
	private JComboBox<String> mappingSelector;
	
//Constructor
	
	public SelectMapping()
	{
		super();
		
		this.setTitle("Select Mapping");
		this.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
		this.setPreferredSize(new Dimension(700,140));

		AML aml = AML.getInstance();
		
		JLabel desc = new JLabel("Select a Mapping from the Alignment:");
		Font font = desc.getFont();
		Font boldFont = new Font(font.getFontName(), Font.BOLD, font.getSize());
		desc.setFont(boldFont);
		JPanel labelPanel = new JPanel();
		labelPanel.add(desc);
		
		Alignment a = AML.getInstance().getAlignment();
		String[] mappings = new String[a.size()];
		for(int i = 0; i < mappings.length; i++)
		{
			Mapping m = a.get(i);
			String map = (i+1) + " (";
			map += aml.getSource().getName(m.getSourceId());
			map += " = ";
			map += aml.getTarget().getName(m.getTargetId());
			map += ")";
			mappings[i] = map;
		}
		mappingSelector = new JComboBox<String>(mappings);
		mappingSelector.setPreferredSize(new Dimension(650,28));
		mappingSelector.setSelectedIndex(aml.getCurrentIndex());
		JPanel selectionPanel = new JPanel();
		selectionPanel.add(mappingSelector);
		
		cancel = new JButton("Cancel");
		cancel.setPreferredSize(new Dimension(70,28));
		cancel.addActionListener(this);
		ok = new JButton("OK");
		ok.setPreferredSize(new Dimension(70,28));
		ok.addActionListener(this);
		JPanel buttonPanel = new JPanel();
		buttonPanel.add(cancel);
		buttonPanel.add(ok);
		
		JPanel dialogPanel = new JPanel();
		dialogPanel.setLayout(new BoxLayout(dialogPanel, BoxLayout.PAGE_AXIS));
		dialogPanel.add(labelPanel);
		dialogPanel.add(selectionPanel);
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
		else if(o == ok)
		{
			AML.getInstance().goTo(mappingSelector.getSelectedIndex());
			this.dispose();
		}
	}
}
