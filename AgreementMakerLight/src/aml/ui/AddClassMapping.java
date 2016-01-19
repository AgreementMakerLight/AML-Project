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
* Class Mapping addition dialog box for the GUI.                              *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.ui;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Set;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import org.jdesktop.swingx.autocomplete.AutoCompleteDecorator;

import aml.AML;
import aml.ontology.Lexicon;
import aml.settings.MappingRelation;
import aml.util.MapSorter;

public class AddClassMapping extends JDialog implements ActionListener
{
	
//Attributes
	
	private static final long serialVersionUID = -2112060212742314680L;
	private AML aml;
	private JPanel sourcePanel, sStart, sResults, targetPanel, tStart, tResults, relationPanel, buttonPanel;
	private CardLayout clS, clT;
	private JButton add, cancel, searchS, backS, searchT, backT;
	private JTextArea sourceSearch, sourceResult, targetSearch, targetResult;
	private JComboBox<String> sourceClasses, targetClasses, sourceResults, targetResults, rels;
	private Lexicon source, target;
	private Vector<Integer> sourceRes, targetRes;
	
//Constructor
	
	public AddClassMapping()
	{
		super();
		
		//Get the AML instance and the ontologies
		aml = AML.getInstance();
		source = aml.getSource().getLexicon();
		target = aml.getTarget().getLexicon();
		//Get the lists of classes from the ontologies
		Vector<Integer> sources = new Vector<Integer>(source.getClasses());
		Vector<Integer> targets = new Vector<Integer>(target.getClasses());
		
		this.setTitle("Add Class Mapping");
		this.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);

		//Source panel: lists source classes and enables searching them
        sourcePanel = new JPanel();
        sourcePanel.setBorder(new TitledBorder("Source Ontology Class"));
        clS = new CardLayout();
        sourcePanel.setLayout(clS);
        //Starting panel: contains the list of all classes and a search field
        sStart = new JPanel();
        sStart.setLayout(new GridLayout(0,1));
        //Create a label
        JLabel sLabel = new JLabel("Select a source class from the class list or use the search field to filter classes:");
        JPanel sLPanel = new JPanel();
        sLPanel.add(sLabel);
        sStart.add(sLPanel);
        Vector<String> sNames = new Vector<String>(sources.size());
        for(int i : sources)
        	sNames.add(source.getBestName(i));
        //Create the search area and button
		sourceSearch = new JTextArea(1,37);
		sourceSearch.setEditable(true);
		AutoCompleteDecorator.decorate(sourceSearch,sNames,false);
		searchS = new JButton("Search");
		searchS.setPreferredSize(new Dimension(80,28));
		searchS.addActionListener(this);
		//Put them in a subpanel, side by side
		JPanel sSearchPanel = new JPanel();
		sSearchPanel.add(sourceSearch);
        sSearchPanel.add(searchS);
        sStart.add(sSearchPanel);
        //Build the combo box with all the primary class names from the Lexicon
        sourceClasses = new JComboBox<String>(sNames);
        sourceClasses.setPreferredSize(new Dimension(500,28));
        //Put it in a subpanel so that it doesn't resize automatically
        JPanel sClassPanel = new JPanel();
        sClassPanel.add(sourceClasses);
        sStart.add(sClassPanel);
        sourcePanel.add(sStart,"Start");
        clS.show(sourcePanel, "Start");

		//Target panel: lists target classes and enables searching them
        targetPanel = new JPanel();
        targetPanel.setBorder(new TitledBorder("Target Ontology Class"));
        clT = new CardLayout();
        targetPanel.setLayout(clT);
        //Starting panel: contains the list of all classes and a search field
        tStart = new JPanel();
        tStart.setLayout(new GridLayout(0,1));
        //Create a label
        JLabel tLabel = new JLabel("Select a target class from the class list or use the search field to filter classes:");
        JPanel tLPanel = new JPanel();
        tLPanel.add(tLabel);
        tStart.add(tLPanel);
        Vector<String> tNames = new Vector<String>(targets.size());
        for(int i : targets)
        	tNames.add(target.getBestName(i));
        //Create the search area and button
		targetSearch = new JTextArea(1,37);
		targetSearch.setEditable(true);
		AutoCompleteDecorator.decorate(targetSearch,tNames,false);
		searchT = new JButton("Search");
		searchT.setPreferredSize(new Dimension(80,28));
		searchT.addActionListener(this);
		//Put them in a subpanel, side by side
		JPanel tSearchPanel = new JPanel();
		tSearchPanel.add(targetSearch);
        tSearchPanel.add(searchT);
        tStart.add(tSearchPanel);
        //Build the combo box with all the primary class names from the Lexicon
        targetClasses = new JComboBox<String>(tNames);
        targetClasses.setPreferredSize(new Dimension(500,28));
        //Put it in a subpanel so that it doesn't resize automatically
        JPanel tClassPanel = new JPanel();
        tClassPanel.add(targetClasses);
        tStart.add(tClassPanel);
        targetPanel.add(tStart,"Start");
        clT.show(targetPanel, "Start");
        
       	//Relation panel: allows the user to select the mapping relationship
        Vector<String> relations = new Vector<String>();
        for(MappingRelation m : MappingRelation.values())
        	relations.add(m.getLabel());
        rels = new JComboBox<String>(relations);
		relationPanel = new JPanel();
		relationPanel.setBorder(new TitledBorder("Mapping Relation"));
		relationPanel.add(rels);

        //Button panel: contains the cancel and add buttons
		cancel = new JButton("Cancel");
		cancel.setPreferredSize(new Dimension(70,28));
		cancel.addActionListener(this);
		add = new JButton("Add");
		add.setPreferredSize(new Dimension(70,28));
		add.addActionListener(this);
		buttonPanel = new JPanel();
		buttonPanel.add(cancel);
		buttonPanel.add(add);
        
        //Dialog panel: contains source, target, and button panels
		JPanel dialogPanel = new JPanel();
		dialogPanel.setLayout(new BoxLayout(dialogPanel, BoxLayout.PAGE_AXIS));
		dialogPanel.add(sourcePanel);
		dialogPanel.add(targetPanel);
		dialogPanel.add(relationPanel);
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
		else if(o == backS)
		{
			clS.removeLayoutComponent(sResults);
			sourceResults = null;
		}
		else if(o == backT)
		{
			clT.removeLayoutComponent(tResults);
			targetResults = null;
		}
		else if(o == add)
		{
			int sourceId;
			if(sourceResults == null)
				sourceId = source.getBestClass((String)sourceClasses.getSelectedItem(),false);
			else
				sourceId = source.getBestClass((String)sourceResults.getSelectedItem(),false);
			
			int targetId;
			if(targetResults == null)
				targetId = target.getBestClass((String)targetClasses.getSelectedItem(),false);
			else
				targetId = target.getBestClass((String)targetResults.getSelectedItem(),false);

			if(sourceId == targetId)
			{
				JOptionPane.showMessageDialog(this,
					    "Source and target classes are the same class (they" +
					    "\nhave the same URI) and thus can't be mapped.",
					    "Warning",
					    JOptionPane.WARNING_MESSAGE);
			}
			else if(aml.getAlignment().containsMapping(sourceId, targetId))
			{
				JOptionPane.showMessageDialog(this,
					    "Source and target classes are already mapped.",
					    "Warning",
					    JOptionPane.WARNING_MESSAGE);
			}
			else
			{
				aml.getAlignment().add(sourceId,targetId,1.0,
						MappingRelation.parseRelation((String)rels.getSelectedItem()));
				aml.needSave(true);
				aml.refreshGUI();
				this.dispose();
			}			
		}
		else if(o == searchS)
		{
			//Get the entered text
			String text = sourceSearch.getText();
			//Check that the user entered a valid input (at least 3 consecutive characters)
			if(text.matches(".*[a-zA-Z0-9]{3,}.*"))
			{
				//If so, clean the input
				text = text.toLowerCase().trim();
				text = text.replaceAll("\t\n\f\r", " ");
				//Search the Lexicon for it
				searchSource(text);
				//Build the results panel
				buildSourceResultsPanel();
		       	sourcePanel.add(sResults, "Results");
		        clS.show(sourcePanel, "Results");
			}
			else
			{
				JOptionPane.showMessageDialog(this, "Invalid input!\n" + 
						"Please enter a word with at least three characters!",
						"Error", JOptionPane.ERROR_MESSAGE); 
			}
		}
		else if(o == searchT)
		{
			//Get the entered text
			String text = targetSearch.getText();
			//Check that the user entered a valid input (at least 3 consecutive characters)
			if(text.matches(".*[a-zA-Z0-9]{3,}.*"))
			{
				//If so, clean the input
				text = text.toLowerCase().trim();
				text = text.replaceAll("\t\n\f\r", " ");
				//Search the Lexicon for it
				searchTarget(text);
				//Build the results panel
				buildTargetResultsPanel();
		       	targetPanel.add(tResults, "Results");
		        clT.show(targetPanel, "Results");
			}
			else
			{
				JOptionPane.showMessageDialog(this, "Invalid input!\n" + 
						"Please enter a word with at least three characters!",
						"Error", JOptionPane.ERROR_MESSAGE); 
			}
		}
	}

//Private Methods
	
	private void buildSourceResultsPanel()
	{
        //Results panel: contains the list of all classes that match que query
        sResults = new JPanel();
        sResults.setLayout(new BoxLayout(sResults, BoxLayout.PAGE_AXIS));
        //Create a label
        JLabel sLabel = new JLabel("Select a source class from the list of search results or go back to the full class list:");
        JPanel sLPanel = new JPanel();
        sLPanel.add(sLabel);
        sResults.add(sLPanel);
        Vector<String> sNames = new Vector<String>(sourceRes.size());
        for(int i : sourceRes)
        	sNames.add(source.getBestName(i));
        //Create the search area and button
		sourceResult = new JTextArea(1,37);
		sourceResult.setText(sourceSearch.getText());
		sourceResult.setEditable(false);
		sourceResult.setBackground(Color.LIGHT_GRAY);
		backS = new JButton("Back");
		backS.setPreferredSize(new Dimension(70,28));
		backS.addActionListener(this);
		//Put them in a subpanel, side by side
		JPanel sSearchPanel = new JPanel();
		sSearchPanel.add(sourceResult);
        sSearchPanel.add(backS);
        sResults.add(sSearchPanel);
        //Build the combo box with all the primary class names from the results
        sourceResults = new JComboBox<String>(sNames);
        sourceResults.setPreferredSize(new Dimension(500,28));
        //Put it in a subpanel so that it doesn't resize automatically
        JPanel sClassPanel = new JPanel();
        sClassPanel.add(sourceResults);
        sResults.add(sClassPanel);


	}
	
	private void buildTargetResultsPanel()
	{
        //Results panel: contains the list of all classes that match que query
        tResults = new JPanel();
        tResults.setLayout(new BoxLayout(tResults, BoxLayout.PAGE_AXIS));
        //Create a label
        JLabel tLabel = new JLabel("Select a target class from the list of search results or go back to the full class list:");
        JPanel tLPanel = new JPanel();
        tLPanel.add(tLabel);
        tResults.add(tLPanel);
        Vector<String> sNames = new Vector<String>(targetRes.size());
        for(int i : targetRes)
        	sNames.add(target.getBestName(i));
        //Create the search area and button
		targetResult = new JTextArea(1,37);
		targetResult.setText(targetSearch.getText());
		targetResult.setEditable(false);
		targetResult.setBackground(Color.LIGHT_GRAY);
		backT = new JButton("Back");
		backT.setPreferredSize(new Dimension(70,28));
		backT.addActionListener(this);
		//Put them in a subpanel, side by side
		JPanel tSearchPanel = new JPanel();
		tSearchPanel.add(targetResult);
        tSearchPanel.add(backT);
        tResults.add(tSearchPanel);
        //Build the combo box with all the primary class names from the results
        targetResults = new JComboBox<String>(sNames);
        targetResults.setPreferredSize(new Dimension(500,28));
        //Put it in a subpanel so that it doesn't resize automatically
        JPanel tClassPanel = new JPanel();
        tClassPanel.add(targetResults);
        tResults.add(tClassPanel);
	}
	
	private void searchSource(String query)
	{
		HashMap<Integer,Double> hits = new HashMap<Integer,Double>();
		for(String n : source.getNames())
		{
			if(n.contains(query))
			{
				double sim = similarity(n,query);
				for(Integer i : source.getClasses(n))
				{
					double classSim = sim*source.getCorrectedWeight(n, i);
					if(hits.containsKey(i))
						classSim += hits.get(i);
					hits.put(i, classSim);
				}
			}
		}
		Set<Integer> orderedHits = MapSorter.sortDescending(hits).keySet();
		sourceRes = new Vector<Integer>(orderedHits);
	}
	
	private void searchTarget(String query)
	{
		HashMap<Integer,Double> hits = new HashMap<Integer,Double>();
		for(String n : target.getNames())
		{
			if(n.contains(query))
			{
				double sim = similarity(n,query);
				for(Integer i : target.getClasses(n))
				{
					double classSim = sim*target.getCorrectedWeight(n, i);
					if(hits.containsKey(i))
						classSim += hits.get(i);
					hits.put(i, classSim);
				}
			}
		}
		Set<Integer> orderedHits = MapSorter.sortDescending(hits).keySet();
		targetRes = new Vector<Integer>(orderedHits);
	}
	
	private Double similarity(String name, String query)
	{
		return 1.0*query.length()/name.length();
	}
}