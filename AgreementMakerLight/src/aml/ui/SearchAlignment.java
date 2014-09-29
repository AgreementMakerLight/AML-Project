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
* Alignment search dialog box for the GUI.                                    *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 23-06-2014                                                            *
* @version 2.1                                                                *
******************************************************************************/
package aml.ui;

import java.awt.CardLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import org.jdesktop.swingx.autocomplete.AutoCompleteDecorator;

import aml.AML;
import aml.match.Alignment;
import aml.match.Mapping;
import aml.util.MapSorter;

public class SearchAlignment extends JDialog implements ActionListener
{
	
//Attributes
	
	private static final long serialVersionUID = -3901206021275961468L;
	private JPanel dialogPanel, searchPanel, resultsPanel;
	private CardLayout cl;
	private JButton cancel, find, back, select, quit;
	private JTextArea searchField;
	private JComboBox<String> mappingSelector;
	private ArrayList<String> mappings;
	private String[] results;
	private Font boldFont;
	
//Constructor
	
	public SearchAlignment()
	{
		super();
		
		this.setTitle("Search Alignment");
		this.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
		this.setPreferredSize(new Dimension(700,140));

		AML aml = AML.getInstance();
		
		//The containing panel
        dialogPanel = new JPanel();
        cl = new CardLayout();
        dialogPanel.setLayout(cl);
		
        //The search panel
		JLabel desc = new JLabel("Enter Search Term: (min 3 characters)");
		Font font = desc.getFont();
		boldFont = new Font(font.getFontName(), Font.BOLD, font.getSize());
		desc.setFont(boldFont);
		JPanel labelPanel = new JPanel();
		labelPanel.add(desc);
		Alignment a = AML.getInstance().getAlignment();
		int total = a.size();
		mappings = new ArrayList<String>(total);
		for(int i = 0; i < total; i++)
		{
			Mapping m = a.get(i);
			String map = aml.getSource().getName(m.getSourceId());
			map += " = ";
			map += aml.getTarget().getName(m.getTargetId());
			mappings.add(map);
		}
		searchField = new JTextArea(1,60);
		searchField.setEditable(true);
		AutoCompleteDecorator.decorate(searchField,mappings,false);
		JPanel selectionPanel = new JPanel();
		selectionPanel.add(searchField);
		cancel = new JButton("Cancel");
		cancel.setPreferredSize(new Dimension(70,28));
		cancel.addActionListener(this);
		find = new JButton("Find");
		find.setPreferredSize(new Dimension(70,28));
		find.addActionListener(this);
		JPanel buttonPanel = new JPanel();
		buttonPanel.add(cancel);
		buttonPanel.add(find);
		
		searchPanel = new JPanel();
		searchPanel.setLayout(new BoxLayout(searchPanel, BoxLayout.PAGE_AXIS));
		searchPanel.add(labelPanel);
		searchPanel.add(selectionPanel);
		searchPanel.add(buttonPanel);
		
       	dialogPanel.add(searchPanel, "Search");
        cl.show(dialogPanel, "Search");

		this.add(dialogPanel);
        this.pack();
        this.setVisible(true);
	}
	
//Public Methods
	
	@Override
	public void actionPerformed(ActionEvent e)
	{
		Object o = e.getSource();
		if(o == cancel || o == quit)
		{
			this.dispose();
		}
		else if(o == back)
		{
			cl.show(dialogPanel, "Search");
		}
		else if(o == select)
		{
			AML.getInstance().goTo(mappings.indexOf(results[mappingSelector.getSelectedIndex()]));
			this.dispose();
		}
		else if(o == find)
		{
			//Get the entered text
			String text = searchField.getText();
			//Check that the user entered a valid input (at least 3 consecutive characters)
			if(text.matches(".*[a-zA-Z0-9]{3,}.*"))
			{
				//If so, clean the input
				text = text.toLowerCase().trim();
				text = text.replaceAll("\t\n\f\r", " ");
				//If the input is a listed mapping go there
				int index = mappings.indexOf(text);
				if(index > -1)
				{
					AML.getInstance().goTo(index);
					this.dispose();
				}
				//Otherwise, search for it
				else
				{
					search(text);
					buildResultsPanel();
			       	dialogPanel.add(resultsPanel, "Results");
			        cl.show(dialogPanel, "Results");
				}
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
	
	private void search(String query)
	{
		HashMap<String,Double> hits = new HashMap<String,Double>();
		for(String m : mappings)
		{
			if(m.contains(query))
				hits.put(m, similarity(m,query));
		}
		Set<String> orderedHits = MapSorter.sortDescending(hits).keySet();
		results = new String[hits.size()];
		results = orderedHits.toArray(results);
	}

	private Double similarity(String mapping, String query)
	{
		String m = mapping.replace(query, "");
		return 1.0*(mapping.length()-m.length())/mapping.length();
	}
	
	private void buildResultsPanel()
	{
		if(results.length == 0)
		{
			JLabel desc = new JLabel("No Results Found!");
			desc.setFont(boldFont);
			JPanel labelPanel = new JPanel();
			labelPanel.setBorder(new EmptyBorder(20,0,18,0));
			labelPanel.add(desc);
			
			back = new JButton("Back");
			back.setPreferredSize(new Dimension(70,28));
			back.addActionListener(this);
			quit = new JButton("Quit");
			quit.setPreferredSize(new Dimension(70,28));
			quit.addActionListener(this);
			JPanel buttonPanel = new JPanel();
			buttonPanel.add(back);
			buttonPanel.add(quit);
			
			resultsPanel = new JPanel();
			resultsPanel.setLayout(new BoxLayout(resultsPanel, BoxLayout.PAGE_AXIS));
			resultsPanel.add(labelPanel);
			resultsPanel.add(buttonPanel);
		}
		else
		{
			JLabel desc = new JLabel("Select a Mapping from the Search Results:");
			desc.setFont(boldFont);
			JPanel labelPanel = new JPanel();
			labelPanel.add(desc);
			
			mappingSelector = new JComboBox<String>(results);
			mappingSelector.setPreferredSize(new Dimension(650,28));
			mappingSelector.setSelectedIndex(0);
			JPanel selectionPanel = new JPanel();
			selectionPanel.add(mappingSelector);
			
			back = new JButton("Back");
			back.setPreferredSize(new Dimension(70,28));
			back.addActionListener(this);
			select = new JButton("Select");
			select.setPreferredSize(new Dimension(70,28));
			select.addActionListener(this);
			JPanel buttonPanel = new JPanel();
			buttonPanel.add(back);
			buttonPanel.add(select);
			
			resultsPanel = new JPanel();
			resultsPanel.setLayout(new BoxLayout(resultsPanel, BoxLayout.PAGE_AXIS));
			resultsPanel.add(labelPanel);
			resultsPanel.add(selectionPanel);
			resultsPanel.add(buttonPanel);
		}
	}
}