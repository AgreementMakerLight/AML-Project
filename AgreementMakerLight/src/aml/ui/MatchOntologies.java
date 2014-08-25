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
* Ontology matching configuration dialog box for the GUI.                     *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 23-06-2014                                                            *
* @version 2.0                                                                *
******************************************************************************/
package aml.ui;

import java.awt.CardLayout;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import aml.AML;
import aml.settings.MatchingAlgorithm;
import aml.settings.SelectionType;

public class MatchOntologies extends JDialog implements ActionListener, ItemListener, ListSelectionListener
{
	
//Attributes
	
	private static final long serialVersionUID = -5425591078861331349L;
	private JComboBox<String> config;
	private JButton cancel, match, info;
    private JCheckBox useBK, ignoreUMLS, repair, properties, allBK;
    private JComboBox<String> selection1, selection2;
    private JComboBox<Double> threshold1, threshold2;
    private JList<String> bk;
    private Vector<String> bkSources, selected;
    private JPanel optionPanel;
    private CardLayout cl;
    private String selectedMatcher;
    
//Constructor
    
	public MatchOntologies()
	{
		//Initialize
		super();
		//Set the title and modality
		this.setTitle("Match Ontologies");
		this.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
		//Get the lists of all and selected BK sources
		bkSources = AML.getInstance().getBKSources();
		selected = AML.getInstance().getSelectedBKSources();
		
		//Matching Algorithms Panel
		Vector<String> matchers = new Vector<String>(0,1);
		for(MatchingAlgorithm m : MatchingAlgorithm.values())
			matchers.add(m.toString());
		config = new JComboBox<String>(matchers);
		selectedMatcher = AML.getInstance().getMatcher().toString();
		config.setSelectedItem(selectedMatcher);
		config.addActionListener(this);
		info = new JButton("Info");
		info.setPreferredSize(new Dimension(70,28));
		info.addActionListener(this);
		JPanel configPanel = new JPanel();
		configPanel.setBorder(new TitledBorder("Matching Algorithm"));
        configPanel.add(config);
        configPanel.add(info);
        
        //Options Panel
        optionPanel = new JPanel();
        cl = new CardLayout();
        optionPanel.setBorder(new TitledBorder("Options"));
        optionPanel.setLayout(cl);
        for(String m : matchers)
        	optionPanel.add(matchOptions(m), m);
        cl.show(optionPanel, selectedMatcher);

        //Button Panel
		cancel = new JButton("Cancel");
		cancel.setPreferredSize(new Dimension(70,28));
		cancel.addActionListener(this);
		match = new JButton("Match");
		match.setPreferredSize(new Dimension(70,28));
		match.addActionListener(this);
		JPanel buttonPanel = new JPanel();
		buttonPanel.add(cancel);
		buttonPanel.add(match);

		//Containing Panel
		JPanel dialogPanel = new JPanel();
		dialogPanel.setLayout(new BoxLayout(dialogPanel, BoxLayout.PAGE_AXIS));
		dialogPanel.add(configPanel);
		dialogPanel.add(optionPanel);
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
			this.dispose();
		else if(o == match)
		{
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			//Set the matching algorithm
			MatchingAlgorithm matcher = MatchingAlgorithm.parseMatcher((String)config.getSelectedItem());
			AML.getInstance().setMatcher(matcher);
			//Then set the options
			if(matcher.equals(MatchingAlgorithm.OAEI))
				AML.getInstance().setMatchOptions(useBK.isSelected(), ignoreUMLS.isSelected(), repair.isSelected());
			else if(matcher.equals(MatchingAlgorithm.AML))
			{
				Vector<String> selectedBK = new Vector<String>(bk.getSelectedValuesList());
				AML.getInstance().setMatchOptions(properties.isSelected(), repair.isSelected(), selectedBK,
						SelectionType.parseSelector((String)selection1.getSelectedItem()),
						(Double)threshold1.getSelectedItem());
			}
			else if(matcher.equals(MatchingAlgorithm.LEXICAL))
			{
				AML.getInstance().setMatchOptions(SelectionType.parseSelector((String)selection2.getSelectedItem()),
						(Double)threshold2.getSelectedItem());
			}
			//Then match the ontologies
			AML.getInstance().match();
			this.dispose();
		}
		else if(o == config)
		{
			selectedMatcher = (String)config.getSelectedItem();
			cl.show(optionPanel, selectedMatcher);
		}
		else if(o == info)
		{
			selectedMatcher = (String)config.getSelectedItem();
			new MatcherInfo(this,selectedMatcher);
		}
	}
	
	@Override
	public void itemStateChanged(ItemEvent i)
	{
		if(i.getStateChange() == ItemEvent.SELECTED)
			bk.setSelectionInterval(0, bkSources.size()-1);
		else
			bk.clearSelection();
	}
	
	@Override
	public void valueChanged(ListSelectionEvent l)
	{
		allBK.setSelected(bk.getSelectedIndices().length == bkSources.size());
	}
	
//Private Methods
	
	private JPanel matchOptions(String s)
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
		
		if(s.equals(MatchingAlgorithm.OAEI.toString()))
		{
			useBK = new JCheckBox("Use Background Knowledge");
			useBK.setSelected(AML.getInstance().useBK());
			JPanel usePanel = new JPanel();
			usePanel.setLayout(new FlowLayout(FlowLayout.LEFT));
			usePanel.setPreferredSize(new Dimension(270,30));
			usePanel.setMaximumSize(new Dimension(270,30));
			usePanel.add(useBK);
			panel.add(usePanel);
			
			ignoreUMLS = new JCheckBox("Exclude UMLS");
			ignoreUMLS.setSelected(AML.getInstance().ignoreUMLS());
			JPanel ignorePanel = new JPanel();
			ignorePanel.setLayout(new FlowLayout(FlowLayout.LEFT));
			ignorePanel.setPreferredSize(new Dimension(270,30));
			ignorePanel.setMaximumSize(new Dimension(270,30));
			ignorePanel.add(ignoreUMLS);
			panel.add(ignorePanel);
			
			repair = new JCheckBox("Repair Alignment");
	        repair.setSelected(AML.getInstance().repairAlignment());
			JPanel repPanel = new JPanel();
			repPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
			repPanel.add(repair);
			panel.add(repPanel);
		}
		else if(s.equals(MatchingAlgorithm.AML.toString()))
		{
			properties = new JCheckBox("Match Properties");
			properties.setSelected(AML.getInstance().matchProperties());
			repair = new JCheckBox("Repair Alignment");
	        repair.setSelected(AML.getInstance().repairAlignment());
			JPanel opPane = new JPanel();
			opPane.setPreferredSize(new Dimension(270,30));
			opPane.setMaximumSize(new Dimension(270,30));
	        opPane.add(properties);
			opPane.add(repair);
			panel.add(opPane);
			
			JLabel card = new JLabel("Cardinality:");
			Vector<String> selectionTypes = new Vector<String>(0,1);
			selectionTypes.add("Auto-Detect");
			for(SelectionType t: SelectionType.values())
				selectionTypes.add(t.toString());
			selection1 = new JComboBox<String>(selectionTypes);
			selection1.setSelectedItem(AML.getInstance().getSelectionType().toString());
			JPanel selectionPane = new JPanel();
			selectionPane.setPreferredSize(new Dimension(270,40));
			selectionPane.setMaximumSize(new Dimension(270,40));
			selectionPane.add(card);
			selectionPane.add(selection1);
			panel.add(selectionPane);
	        
			JLabel th = new JLabel("Similarity Threshold:");
			Vector<Double> thresh = new Vector<Double>(50);
			for(int i = 50; i < 100; i++)
				thresh.add(Double.parseDouble("0." + i));
			threshold1 = new JComboBox<Double>(thresh);
			threshold1.setSelectedItem(AML.getInstance().getThreshold());
			JPanel thresholdPane = new JPanel();
			thresholdPane.setPreferredSize(new Dimension(270,40));
			thresholdPane.setMaximumSize(new Dimension(270,40));
			thresholdPane.add(th);
			thresholdPane.add(threshold1);
			panel.add(thresholdPane);
			
			JLabel bkLabel = new JLabel("Background Knowledge:");
			
			allBK = new JCheckBox("Select All");
			allBK.setSelected(selected.size() == bkSources.size());
			allBK.addItemListener(this);
			JPanel bkPanel = new JPanel();
			bkPanel.add(bkLabel);
			bkPanel.add(allBK);
			panel.add(bkPanel);
			bk = new JList<String>(bkSources);
			bk.addListSelectionListener(this);
			bk.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
			if(bkSources.size() == selected.size())
				bk.setSelectionInterval(0, bkSources.size()-1);
			else
			{
				for(String n : selected)
				{
					int index = bkSources.indexOf(n);
					bk.addSelectionInterval(index, index);
				}
			}
			JScrollPane listPane = new JScrollPane(bk);
			listPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
			listPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			listPane.setAutoscrolls(true);
			listPane.setPreferredSize(new Dimension(250,90));
			listPane.setMaximumSize(new Dimension(250,90));
			panel.add(listPane);
		}
		else if(s.equals(MatchingAlgorithm.LEXICAL.toString()))
		{
			JLabel card = new JLabel("Cardinality:");
			Vector<String> selectionTypes = new Vector<String>(0,1);
			selectionTypes.add("Auto-Detect");
			for(SelectionType t: SelectionType.values())
				selectionTypes.add(t.toString());
			selection2 = new JComboBox<String>(selectionTypes);
			selection2.setSelectedItem(AML.getInstance().getSelectionType().toString());
			JPanel selectionPane = new JPanel();
			selectionPane.setPreferredSize(new Dimension(270,40));
			selectionPane.setMaximumSize(new Dimension(270,40));
			selectionPane.add(card);
			selectionPane.add(selection2);
			panel.add(selectionPane);
	        
			JLabel th = new JLabel("Similarity Threshold:");
			Vector<Double> thresh = new Vector<Double>(50);
			for(int i = 50; i < 100; i++)
				thresh.add(Double.parseDouble("0." + i));
			threshold2 = new JComboBox<Double>(thresh);
			threshold2.setSelectedItem(AML.getInstance().getThreshold());
			JPanel thresholdPane = new JPanel();
			thresholdPane.add(th);
			thresholdPane.add(threshold2);
			panel.add(thresholdPane);
		}
		return panel;
	}
}