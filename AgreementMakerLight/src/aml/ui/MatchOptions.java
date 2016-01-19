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

import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import aml.AML;
import aml.settings.LanguageSetting;
import aml.settings.MatchStep;

public class MatchOptions extends JDialog implements ActionListener, Runnable, WindowListener
{
	//TODO: Add option of extending existing Alignment
	
//Attributes
	
	private static final long serialVersionUID = -4255910788613313495L;
	private AML aml;
	private Console c;
	private JButton cancel, match, detail;
    private JComboBox<Double> threshold;
    private JComboBox<String> combination;
	private Vector<JCheckBox> matchers;
    private Vector<MatchStep> selectedSteps;
    private Thread action, console;
    
//Constructor
    
	public MatchOptions()
	{
		//Initialize
		super();
		//Get the AML instance
		aml = AML.getInstance();
		//And the lists of match steps & match configurations
		selectedSteps = aml.getMatchSteps();
		matchers = new Vector<JCheckBox>();
		for(MatchStep m : MatchStep.values())
		{
			if(m.equals(MatchStep.TRANSLATE) && aml.getLanguageSetting().equals(LanguageSetting.SINGLE))
				continue;
			JCheckBox cb = new JCheckBox(m.toString());
			cb.setSelected(selectedSteps.contains(m));
			if(m.equals(MatchStep.LEXICAL))
				cb.setEnabled(false);
			matchers.add(cb);		
		}

		//Set the title and modality
		this.setTitle("Match Options");
		this.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
		
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
	
		//Match Steps
		JPanel stepPanel = new JPanel();
		stepPanel.setBorder(new TitledBorder("Matching Steps"));
		JPanel matcherPanel = new JPanel();
		matcherPanel.setLayout(new BoxLayout(matcherPanel, BoxLayout.Y_AXIS));
		for(JCheckBox cb : matchers)
			matcherPanel.add(cb);
		stepPanel.add(matcherPanel);
		panel.add(stepPanel);

		//Options
		JPanel optionPanel = new JPanel();
		optionPanel.setBorder(new TitledBorder("Options"));
		optionPanel.setLayout(new BoxLayout(optionPanel, BoxLayout.Y_AXIS));
		Vector<Double> thresh = new Vector<Double>(50);
		for(int i = 50; i < 100; i++)
			thresh.add(Double.parseDouble("0." + i));
		threshold = new JComboBox<Double>(thresh);
		threshold.setSelectedItem(aml.getThreshold());
		JLabel th = new JLabel("Similarity Threshold");
		JPanel thresholdPane = new JPanel();
		thresholdPane.add(th);
		thresholdPane.add(threshold);
		optionPanel.add(thresholdPane);
		Vector<String> comb = new Vector<String>(2);
		comb.add("Hierarchical");
		comb.add("Concurrent");
		combination = new JComboBox<String>(comb);
		JLabel mc = new JLabel("Matcher Combination");
		JPanel combPane = new JPanel();
		combPane.add(mc);
		combPane.add(combination);
		optionPanel.add(combPane);
		panel.add(optionPanel);

        //Button Panel
		cancel = new JButton("Cancel");
		cancel.setPreferredSize(new Dimension(80,28));
		cancel.addActionListener(this);
		detail = new JButton("Settings");
		detail.setPreferredSize(new Dimension(80,28));
		detail.addActionListener(this);
		match = new JButton("Match");
		match.setPreferredSize(new Dimension(80,28));
		match.addActionListener(this);

		JPanel buttonPanel = new JPanel();
		buttonPanel.add(cancel);
		buttonPanel.add(detail);
		buttonPanel.add(match);
		panel.add(buttonPanel);

		add(panel);
		
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
			this.dispose();
		else if(o == match)
		{
			if(aml.hasAlignment())
				aml.closeAlignment();
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			aml.setThreshold((Double)threshold.getSelectedItem());
			Vector<MatchStep> selection = new Vector<MatchStep>();
			for(JCheckBox c : matchers)
				if(c.isSelected())
					selection.add(MatchStep.parseStep(c.getText()));
			aml.setMatchSteps(selection);
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
			new DetailedOptions();
		}
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