/******************************************************************************
* Copyright 2013-2015 LASIGE                                                  *
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
* @author Catarina Martins & Daniel Faria                                     *
* @date 20-05-2015                                                            *
******************************************************************************/
package aml.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import aml.ontology.Ontology;
import aml.settings.MappingRelation;

public class AlignmentReviewer extends JInternalFrame implements ActionListener
{
	private static final long serialVersionUID = 3893526178457620945L;
	private AML aml;
	private Ontology source, target;
	private Alignment a;
	private MappingRelation rel;
	private Vector<JCheckBox> check;
	private Vector<JButton> details;
	private Vector<String> mappings;
	private JButton remove, addClass, addProp;
	private JPanel panel, buttonPanel, dialogPanel;
	private JScrollPane scrollPane;

	public AlignmentReviewer(Dimension max, Dimension min)
	{
		super("Alignment Reviewer",false,false,false,false);
		aml = AML.getInstance();
		//Set the size
		this.setMaximumSize(max);
		this.setPreferredSize(min);
		
		dialogPanel = new JPanel();
		dialogPanel.setLayout(new BoxLayout(dialogPanel, BoxLayout.PAGE_AXIS));
		if(aml.hasAlignment())
		{
			panel = new JPanel(new GridLayout(0,1));
			a = aml.getAlignment();

			mappings = new Vector<String>();
			check = new Vector<JCheckBox>();
			details = new Vector<JButton>(mappings.size());

			for(Mapping m : a)
			{
				String map = toString(m);
				mappings.add(map);
			}

			for(String m : mappings)
			{
				JCheckBox c = new JCheckBox(""); 
				check.add(c);
				JButton b = new JButton(m);
				details.add(b);
				b.addActionListener(this);
				JPanel subPanel = new JPanel(new BorderLayout());
				subPanel.add(c,BorderLayout.LINE_START);
				JPanel subSubPanel = new JPanel(new BorderLayout());
				subSubPanel.add(b,BorderLayout.LINE_START);
				subPanel.add(subSubPanel, BorderLayout.CENTER);
				panel.add(subPanel);
			}           

			scrollPane = new JScrollPane(panel);

			remove = new JButton("Remove Selected Mappings");
			remove.setPreferredSize(new Dimension(200,28));
			remove.addActionListener(this);
			addClass = new JButton("Add Class Mapping");
			addClass.setPreferredSize(new Dimension(200,28));
			addClass.addActionListener(this);
			addProp = new JButton("Add Property Mapping");
			addProp.setPreferredSize(new Dimension(200,28));
			addProp.addActionListener(this);
			buttonPanel = new JPanel(new FlowLayout());
			buttonPanel.add(remove);
			buttonPanel.add(addClass);
			buttonPanel.add(addProp);

			dialogPanel.add(scrollPane);
			dialogPanel.add(buttonPanel);
			add(dialogPanel);
		}

		dialogPanel.revalidate();
		dialogPanel.repaint();
		this.pack();
		this.setVisible(true);
	}

	public void actionPerformed(ActionEvent e)
	{
		JButton b = (JButton)e.getSource();
		int index = details.indexOf(b);
		if(b == remove)
		{
			for(JCheckBox box : check)
			{
				int indexBox = check.indexOf(box);
				if(box.isSelected())
				{
					Mapping m = a.get(indexBox);
					a.remove(m);
				}
			}
			aml.goTo(0);
		}
		else if(b == addClass)
		{
			new AddClassMapping();
		}
		else if(b == addProp)
		{
			new AddPropertyMapping();
		}
		if(index > -1)
		{
			new ViewMapping(index);
		}
	}

	public String toString(Mapping m)
	{
		source = aml.getSource();
		target = aml.getTarget();
		rel = MappingRelation.EQUIVALENCE;

		return source.getName(m.getSourceId()) + " " + rel.toString() + " " +
		target.getName(m.getTargetId());
	}
}