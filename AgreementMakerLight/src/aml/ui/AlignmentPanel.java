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
* Displays the Alignment in list format, with clickable mappings for detailed *
* information, and checkboxes to enable removing erroneous mappings.          *
*                                                                             *
* @author Daniel Faria & Catarina Martins                                     *
******************************************************************************/
package aml.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import aml.AML;
import aml.match.Alignment;
import aml.match.Mapping;
import aml.settings.MappingStatus;

public class AlignmentPanel extends JInternalFrame implements ActionListener, ItemListener
{
	
//Attributes
	
	private static final long serialVersionUID = 3893526178457620945L;
	private AML aml;
	private Alignment a;
	private Vector<JCheckBox> check;
	private Vector<JButton> mappings;
	private JCheckBox selectAll;
	private JButton remove, reset, setCorrect, setIncorrect;
	private JPanel dialogPanel, headerPanel, mappingPanel;
	private JScrollPane scrollPane;

//Constructors
	
	/**
	 * Creates a new AlignmentReviewer panel with specified dimension constraints
	 * @param max: the maximum dimension
	 * @param pref: the preferred dimension
	 */
	public AlignmentPanel(Dimension max, Dimension pref)
	{
		super("Alignment Panel",false,false,false,false);
		aml = AML.getInstance();
		//Set the size
		this.setMaximumSize(max);
		this.setPreferredSize(pref);

		refresh();
	}

//Public Methods
	
	@Override
	public void actionPerformed(ActionEvent e)
	{
		JButton b = (JButton)e.getSource();
		if(b == remove)
		{
			aml.removeIncorrect();
		}
		else if(b == reset)
		{
			for(int i = 0; i < check.size(); i++)
			{
				if(check.get(i).isSelected() && !a.get(i).getStatus().equals(MappingStatus.UNKNOWN))
				{
					a.get(i).setStatus(MappingStatus.UNKNOWN);
					mappings.get(i).setBackground(Color.LIGHT_GRAY);
				}
			}
		}
		else if(b == setCorrect)
		{
			for(int i = 0; i < check.size(); i++)
			{
				if(check.get(i).isSelected() && !a.get(i).getStatus().equals(MappingStatus.CORRECT))
				{
					a.get(i).setStatus(MappingStatus.CORRECT);
					mappings.get(i).setBackground(Color.GREEN);
				}
			}
		}
		else if(b == setIncorrect)
		{
			for(int i = 0; i < check.size(); i++)
			{
				if(check.get(i).isSelected() && !a.get(i).getStatus().equals(MappingStatus.INCORRECT))
				{
					a.get(i).setStatus(MappingStatus.INCORRECT);
					mappings.get(i).setBackground(Color.RED);
				}
			}
		}
		else
		{
			int index = mappings.indexOf(b);
			if(index > -1)
				goTo(index);
		}
	}
	
	/**
	 * Centers the AlignmentReviewer on the given index and
	 * selects its button and checkbox
	 */
	public void goTo(int index)
	{
		scrollPane.getViewport().setViewPosition(new Point(0,index*27));
		new ViewMapping(this,index);
	}
	
	@Override
	public void itemStateChanged(ItemEvent i)
	{
		if(i.getStateChange() == ItemEvent.SELECTED)
		{
			for(JCheckBox c : check)
				c.setSelected(true);
		}
		else
		{
			for(JCheckBox c : check)
				c.setSelected(false);
		}
	}
	
	/**
	 * Refreshes the AlignmentReviewer
	 */
	public void refresh()
	{
		dialogPanel = new JPanel();
		dialogPanel.setLayout(new BoxLayout(dialogPanel, BoxLayout.PAGE_AXIS));
		if(aml.hasAlignment())
		{
			//The header button panel
			selectAll = new JCheckBox("Select All/None");
			selectAll.addItemListener(this);
			setCorrect = new JButton("Set Correct");
			setCorrect.setBackground(Color.GREEN);
			setCorrect.setPreferredSize(new Dimension(140,28));
			setCorrect.addActionListener(this);
			reset = new JButton("Reset");
			reset.setBackground(Color.LIGHT_GRAY);
			reset.setPreferredSize(new Dimension(140,28));
			reset.addActionListener(this);
			setIncorrect = new JButton("Set Incorrect");
			setIncorrect.setBackground(Color.RED);
			setIncorrect.setPreferredSize(new Dimension(140,28));
			setIncorrect.addActionListener(this);
			remove = new JButton("Remove Incorrect");
			remove.setBackground(Color.BLACK);
			remove.setForeground(Color.RED);
			remove.setPreferredSize(new Dimension(140,28));
			remove.addActionListener(this);
			headerPanel = new JPanel(new FlowLayout());
			headerPanel.add(selectAll);
			headerPanel.add(setCorrect);
			headerPanel.add(reset);
			headerPanel.add(setIncorrect);
			headerPanel.add(remove);
			
			//The mapping list
			mappingPanel = new JPanel(new GridLayout(0,1));
			a = aml.getAlignment();
			check = new Vector<JCheckBox>();
			mappings = new Vector<JButton>(a.size());

			for(Mapping m : a)
			{
				JCheckBox c = new JCheckBox(""); 
				check.add(c);
				JButton b = new MappingButton(m);
				mappings.add(b);
				b.addActionListener(this);
				JPanel subPanel = new JPanel(new BorderLayout());
				subPanel.add(c,BorderLayout.LINE_START);
				JPanel subSubPanel = new JPanel(new BorderLayout());
				subSubPanel.add(b,BorderLayout.LINE_START);
				subPanel.add(subSubPanel, BorderLayout.CENTER);
				subPanel.setMaximumSize(new Dimension(subPanel.getMaximumSize().width,29));
				subPanel.setPreferredSize(new Dimension(subPanel.getPreferredSize().width,29));
				mappingPanel.add(subPanel);
			}
			JPanel filler = new JPanel();
			mappingPanel.add(filler);
			scrollPane = new JScrollPane(mappingPanel);
			
			dialogPanel.add(headerPanel);
			dialogPanel.add(scrollPane);
		}
		setContentPane(dialogPanel);
		dialogPanel.revalidate();
		dialogPanel.repaint();
		this.pack();
		this.setVisible(true);
	}
	
	/**
	 * Refreshes a given Mapping in the AlignmentReviewer
	 */
	public void refresh(int index)
	{
		Mapping m = a.get(index);
		if(m.getStatus().equals(MappingStatus.CORRECT))
			mappings.get(index).setBackground(Color.GREEN);
		else if(m.getStatus().equals(MappingStatus.INCORRECT))
			mappings.get(index).setBackground(Color.RED);
		else if(m.getStatus().equals(MappingStatus.FLAGGED))
			mappings.get(index).setBackground(Color.YELLOW);
		else
			mappings.get(index).setBackground(Color.GRAY);
	}
}