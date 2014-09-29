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
* Visualization options dialog box for the GUI.                               *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 29-09-2014                                                            *
* @version 2.1                                                                *
******************************************************************************/
package aml.ui;

import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import aml.AML;
import aml.settings.LanguageSetting;

public class ViewOptions extends JDialog implements ActionListener
{
	
//Attributes
	
	private static final long serialVersionUID = -3900206021275961468L;
	private AML aml;
	private JPanel dialogPanel, languagePanel, directionPanel, distancePanel, buttonPanel;
	private JButton cancel, ok;
	private JLabel languageLabel, distanceLabel;
	private JComboBox<String> languageSelector;
	private JComboBox<Integer> distanceSelector;
	private JCheckBox ancestors, descendants;
	private final Integer[] DIST = {0,1,2,3,4,5};
    
//Constructor	
	
	public ViewOptions()
	{
		super();
		aml = AML.getInstance();
		
		this.setTitle("Visualization Options");
		this.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
		
		languageLabel = new JLabel("Label Language:");
		Vector<String> languages = new Vector<String>(aml.getLanguages());
		languageSelector = new JComboBox<String>(languages);
		languageSelector.setSelectedItem(aml.getLabelLanguage());
		languagePanel = new JPanel();
		languagePanel.add(languageLabel);
		languagePanel.add(languageSelector);

		ancestors = new JCheckBox("View Ancestors");
		ancestors.setMnemonic(KeyEvent.VK_C);
		ancestors.setSelected(AML.getInstance().showAncestors());
		descendants = new JCheckBox("View Descendants");
		descendants.setMnemonic(KeyEvent.VK_C);
		descendants.setSelected(AML.getInstance().showDescendants());
		directionPanel = new JPanel();
		directionPanel.add(ancestors);
		directionPanel.add(descendants);
		
		distanceLabel = new JLabel("Extension (edges):");
		distanceSelector = new JComboBox<Integer>(DIST);
		distanceSelector.setSelectedIndex(AML.getInstance().getMaxDistance());
        distanceSelector.addActionListener(this);
		distancePanel = new JPanel();
		distancePanel.add(distanceLabel);
		distancePanel.add(distanceSelector);

		cancel = new JButton("Cancel");
		cancel.setPreferredSize(new Dimension(70,28));
		cancel.addActionListener(this);
		ok = new JButton("OK");
		ok.setPreferredSize(new Dimension(70,28));
		ok.addActionListener(this);
		buttonPanel = new JPanel();
		buttonPanel.add(cancel);
		buttonPanel.add(ok);
		
		dialogPanel = new JPanel();
		dialogPanel.setPreferredSize(new Dimension(300,150));
		dialogPanel.setLayout(new BoxLayout(dialogPanel, BoxLayout.PAGE_AXIS));
		if(!aml.getLanguageSetting().equals(LanguageSetting.SINGLE))
			dialogPanel.add(languagePanel);
		dialogPanel.add(directionPanel);
		dialogPanel.add(distancePanel);
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
			aml.setViewOptions(ancestors.isSelected(),descendants.isSelected(),
					(Integer)distanceSelector.getSelectedItem());
			if(!aml.getLanguageSetting().equals(LanguageSetting.SINGLE))
				aml.setLabelLanguage((String)languageSelector.getSelectedItem());
			this.dispose();
		}
	}
}
