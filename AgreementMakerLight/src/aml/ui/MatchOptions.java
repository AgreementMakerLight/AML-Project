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
* Ontology matching configuration dialog box for the GUI.                     *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.ui;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
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

public class MatchOptions extends JDialog implements ActionListener, ItemListener, ListSelectionListener, Runnable, WindowListener
{
	
//Attributes
	
	private static final long serialVersionUID = -4255910788613313495L;
	private AML aml;
	private Console c;
	private JButton cancel, match, detail;
    private JComboBox<Double> threshold;
	private JList<String> matchList;
	private JCheckBox allSteps;
    private Vector<String> matchSteps, selectedSteps;
    private Thread action, console;
    
//Constructor
    
    //TODO: This Dialog doesn't look great - improve its appearance! - also add combination option
	public MatchOptions()
	{
		//Initialize
		super();
		
		//Get the AML instance
		aml = AML.getInstance();
		//And the lists of sources, match steps & match configurations

		matchSteps = new Vector<String>(8);
		for(MatchStep m : MatchStep.values())
			matchSteps.add(m.toString());
		Vector<MatchStep> ms = aml.getSelectedSteps();
		selectedSteps = new Vector<String>(ms.size());
		for(MatchStep m : ms)
			selectedSteps.add(m.toString());

		//Set the title and modality
		this.setTitle("Match Options");
		this.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
		
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
		
		//Match Steps
		allSteps = new JCheckBox("Select All/None");
		allSteps.setSelected(selectedSteps.size() == matchSteps.size());
		allSteps.addItemListener(this);
		matchList = new JList<String>(matchSteps);
		matchList.addListSelectionListener(this);
		matchList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		if(matchSteps.size() == selectedSteps.size())
			matchList.setSelectionInterval(0, matchSteps.size()-1);
		else
		{
			for(String n : selectedSteps)
			{
				int index = matchSteps.indexOf(n);
				matchList.addSelectionInterval(index, index);
			}
		}
		JScrollPane listPane2 = new JScrollPane(matchList);
		listPane2.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		listPane2.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		listPane2.setAutoscrolls(true);
		listPane2.setPreferredSize(new Dimension(200,90));
		listPane2.setMaximumSize(new Dimension(200,90));
		detail = new JButton("Configure Steps");
		detail.setPreferredSize(new Dimension(140,28));
		detail.addActionListener(this);
		JPanel stepPanel = new JPanel();
		stepPanel.setBorder(new TitledBorder("Matching Steps"));
		stepPanel.setPreferredSize(new Dimension(300,200));
		stepPanel.setMaximumSize(new Dimension(300,200));
		stepPanel.add(allSteps, BorderLayout.PAGE_START);
		stepPanel.add(listPane2, BorderLayout.CENTER);
		stepPanel.add(detail, BorderLayout.PAGE_END);
		panel.add(stepPanel);

		//Threshold
		Vector<Double> thresh = new Vector<Double>(50);
		for(int i = 50; i < 100; i++)
			thresh.add(Double.parseDouble("0." + i));
		threshold = new JComboBox<Double>(thresh);
		threshold.setSelectedItem(aml.getThreshold());
		JPanel thresholdPane = new JPanel();
		thresholdPane.setBorder(new TitledBorder("Similarity Threshold"));
		thresholdPane.setPreferredSize(new Dimension(300,80));
		thresholdPane.setMaximumSize(new Dimension(300,80));
		thresholdPane.add(threshold);
		panel.add(thresholdPane);

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
		panel.add(buttonPanel);

		add(panel);
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
			if(aml.hasAlignment())
				aml.closeAlignment();
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			aml.setThreshold((Double)threshold.getSelectedItem());
			Vector<MatchStep> selection = new Vector<MatchStep>();
			for(String s : matchList.getSelectedValuesList())
				selection.add(MatchStep.parseStep(s));
			aml.setSelectedSteps(selection);
			//Then match the ontologies
			c = new Console();
			c.addWindowListener(this);
			console = new Thread(c);
			console.start();
			action = new Thread(this);
			action.start();
		}
		else if(o == detail)
		{
			new DetailedOptions(matchList.getSelectedValuesList());
		}
	}
	
	@Override
	public void itemStateChanged(ItemEvent i)
	{
		if(i.getStateChange() == ItemEvent.SELECTED)
			matchList.setSelectionInterval(0, matchSteps.size()-1);
		else
			matchList.clearSelection();
	}
	
	@Override
	public void valueChanged(ListSelectionEvent l)
	{
		allSteps.setSelected(matchList.getSelectedIndices().length == matchSteps.size());
	}

	@Override
	public void run()
	{
		try
		{
			Thread.sleep(1000);
		}
		catch (InterruptedException e)
		{
			//Do nothing
		}
		aml.matchManual();
		Audio.finished();
		try
		{
			Thread.sleep(1500);
		}
		catch (InterruptedException e)
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