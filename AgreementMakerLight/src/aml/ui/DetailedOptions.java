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
* @date 29-09-2014                                                            *
* @version 2.1                                                                *
******************************************************************************/
package aml.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;
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
import aml.settings.MatchStep;
import aml.settings.NeighborSimilarityStrategy;
import aml.settings.SelectionType;
import aml.settings.SizeCategory;
import aml.settings.StringSimMeasure;
import aml.settings.WordMatchStrategy;

public class DetailedOptions extends JDialog implements ActionListener, ItemListener, ListSelectionListener
{
	
//Attributes
	
	private static final long serialVersionUID = -5425591078861331349L;
	private AML aml;
	//Common Options
	private JComboBox<String> step, word, string, struct, select;
    private Vector<String> steps, bkSources, selectedSources;
	private JButton cancel, ok;
	private JList<String> bkList;
	private JCheckBox allBK, primaryString, direct, removeObsolete, structSelection;
    private JPanel optionPanel;
    private CardLayout cl;
    
//Constructor
    
	public DetailedOptions(List<String> s)
	{
		//Initialize
		super();
		aml = AML.getInstance();
		
		//Set the title and modality
		this.setTitle("Configure Matching Steps");
		this.setModalityType(Dialog.ModalityType.MODELESS);
		
		steps = new Vector<String>(s);
		steps.remove(MatchStep.TRANSLATE.toString());
		steps.remove(MatchStep.PROPERTY.toString());
		steps.remove(MatchStep.REPAIR.toString());
		
		JPanel configPanel = new JPanel();
		configPanel.setBorder(new TitledBorder("Matching Step"));
		step = new JComboBox<String>(steps);
		step.addActionListener(this);
        configPanel.add(step);
        
		//BK Matcher
		bkSources = aml.getBKSources();
		selectedSources = aml.getSelectedBKSources();
		allBK = new JCheckBox("Select All");
		allBK.setSelected(selectedSources.size() == bkSources.size());
		allBK.addItemListener(this);
		bkList = new JList<String>(bkSources);
		bkList.addListSelectionListener(this);
		bkList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		if(bkSources.size() == selectedSources.size())
			bkList.setSelectionInterval(0, bkSources.size()-1);
		else
		{
			for(String n : selectedSources)
			{
				int index = bkSources.indexOf(n);
				bkList.addSelectionInterval(index, index);
			}
		}
		//Word Matcher
		Vector<String> words = new Vector<String>(5);
		for(WordMatchStrategy wm : WordMatchStrategy.values())
			words.add(wm.toString());
		word = new JComboBox<String>(words);
		//String Matcher
		Vector<String> measures = new Vector<String>(4);
		for(StringSimMeasure ssm : StringSimMeasure.values())
			measures.add(ssm.toString());
		string = new JComboBox<String>(measures);
		primaryString = new JCheckBox("Global Match");
		primaryString.setSelected(aml.getSizeCategory().equals(SizeCategory.SMALL));
		//Structural Matcher
		Vector<String> strategies = new Vector<String>(5);
		for(NeighborSimilarityStrategy ns : NeighborSimilarityStrategy.values())
			strategies.add(ns.toString());
		struct = new JComboBox<String>(strategies);
		direct = new JCheckBox("Direct Ancestors");
		direct.setSelected(false);
		//Selector
		Vector<String> types = new Vector<String>(3);
		for(SelectionType st : SelectionType.values())
			types.add(st.toString());
		select = new JComboBox<String>(types);
		removeObsolete = new JCheckBox("Remove Obsolete Class Mappings");
		removeObsolete.setSelected(false);
		structSelection = new JCheckBox("Structure-Based Selection");
		structSelection.setSelected(aml.getSizeCategory().equals(SizeCategory.HUGE));
		
        //Options Panel
        optionPanel = new JPanel();
        cl = new CardLayout();
        optionPanel.setBorder(new TitledBorder("Options"));
        optionPanel.setLayout(cl);
        for(String m : steps)
        	optionPanel.add(matchOptions(m), m);

        //Button Panel
		JPanel buttonPanel = new JPanel();
		cancel = new JButton("Cancel");
		cancel.setPreferredSize(new Dimension(70,28));
		cancel.addActionListener(this);
		buttonPanel.add(cancel);
		ok = new JButton("OK");
		ok.setPreferredSize(new Dimension(70,28));
		ok.addActionListener(this);
		buttonPanel.add(ok);

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
		else if(o == ok)
		{
			aml.setSelectedSources(new Vector<String>(bkList.getSelectedValuesList()));
			aml.setWordMatchStrategy(WordMatchStrategy.parseStrategy((String)word.getSelectedItem()));
			aml.setStringSimMeasure(StringSimMeasure.parseMeasure((String)string.getSelectedItem()));
			aml.setPrimaryStringMatcher(primaryString.isSelected());
			aml.setNeighborSimilarityStrategy(NeighborSimilarityStrategy.parseStrategy((String)struct.getSelectedItem()));
			aml.setDirectNeighbors(direct.isSelected());
			aml.setSelectionType(SelectionType.parseSelector((String)select.getSelectedItem()));
			aml.setRemoveObsolete(removeObsolete.isSelected());
			aml.setStructuralSelection(structSelection.isSelected());
			this.dispose();
		}
		else if(o == step)
		{
			cl.show(optionPanel, (String)step.getSelectedItem());
		}
	}
	
	@Override
	public void itemStateChanged(ItemEvent i)
	{
		if(i.getStateChange() == ItemEvent.SELECTED)
			bkList.setSelectionInterval(0, bkSources.size()-1);
		else
			bkList.clearSelection();
	}
	
	@Override
	public void valueChanged(ListSelectionEvent l)
	{
		allBK.setSelected(bkList.getSelectedIndices().length == bkSources.size());
	}
	
//Private Methods
	
	private JPanel matchOptions(String s)
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
		panel.setPreferredSize(new Dimension(250,130));
		panel.setMaximumSize(new Dimension(250,130));
		
		if(s.equals(MatchStep.BK.toString()))
		{
			JScrollPane listPane = new JScrollPane(bkList);
			listPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
			listPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			listPane.setAutoscrolls(true);
			listPane.setPreferredSize(new Dimension(200,90));
			listPane.setMaximumSize(new Dimension(200,90));
			JPanel p1 = new JPanel();
			p1.setLayout(new FlowLayout(FlowLayout.CENTER));
			p1.add(allBK);
			panel.add(p1);
			JPanel p2 = new JPanel();
			p2.setLayout(new FlowLayout(FlowLayout.CENTER));
			p2.add(listPane);
			panel.add(p2);
		}
		else if(s.equals(MatchStep.WORD.toString()))
		{
			JLabel wordMatch = new JLabel("Word Match Strategy");
			JPanel p1 = new JPanel();
			p1.setLayout(new FlowLayout(FlowLayout.CENTER));
			p1.add(wordMatch);
			p1.add(word);
			panel.add(p1);
		}
		else if(s.equals(MatchStep.STRING.toString()))
		{
			JLabel stringMatch = new JLabel("Similarity Measure");
			JPanel p1 = new JPanel();
			p1.setLayout(new FlowLayout(FlowLayout.CENTER));
			p1.add(stringMatch);
			p1.add(string);
			panel.add(p1);
			JPanel p2 = new JPanel();
			p2.add(primaryString, BorderLayout.CENTER);
			if(!aml.getSizeCategory().equals(SizeCategory.SMALL))
			{
				JLabel warning = new JLabel("Warning: quadratic time-complexity!");
				warning.setForeground(Color.RED);
				p2.add(warning, BorderLayout.CENTER);
			}
			panel.add(p2);

		}
		else if(s.equals(MatchStep.STRUCT.toString()))
		{
			JLabel structMatch = new JLabel("Neighborhood");
			JPanel p1 = new JPanel();
			p1.setLayout(new FlowLayout(FlowLayout.CENTER));
			p1.add(structMatch);
			p1.add(struct);
			panel.add(p1);
			JPanel p2 = new JPanel();
			p2.setLayout(new FlowLayout(FlowLayout.CENTER));
			p2.add(direct);
			panel.add(p2);
		}
		else if(s.equals(MatchStep.SELECT.toString()))
		{
			JLabel sel = new JLabel("Selection Type");
			JPanel p1 = new JPanel();
			p1.setLayout(new FlowLayout(FlowLayout.CENTER));
			p1.add(sel);
			p1.add(select);
			panel.add(p1);
			JPanel p2 = new JPanel();
			p2.setLayout(new FlowLayout(FlowLayout.CENTER));
			p2.add(removeObsolete);
			panel.add(p2);
			JPanel p3 = new JPanel();
			p3.setLayout(new FlowLayout(FlowLayout.CENTER));
			p3.add(structSelection);
			panel.add(p3);
		}
		return panel;
	}
}