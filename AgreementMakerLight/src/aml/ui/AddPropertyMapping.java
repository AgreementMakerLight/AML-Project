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
* Property Mapping addition dialog box for the GUI.                           *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.ui;

import java.awt.CardLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import aml.AML;
import aml.ontology.Ontology2Match;
import aml.settings.EntityType;
import aml.settings.MappingRelation;

public class AddPropertyMapping extends JDialog implements ActionListener
{

//Attributes
	
	private static final long serialVersionUID = -7201206021275961468L;
    private AML aml;
	private JButton cancel, add;
	private JComboBox<String> propTypes, dataS, dataT, objectS, objectT, rels;
	private Vector<Integer> dataSource, dataTarget, objectSource, objectTarget;
    private JPanel dialogPanel, headerPanel, selectionPanel, dataPanel, objectPanel, relationPanel, buttonPanel;
    private CardLayout cl;
	
//Constructor
	
	public AddPropertyMapping()
	{
		super();
		
		//Get the AML instance and the ontologies
		aml = AML.getInstance();
		Ontology2Match source = aml.getSource();
		Ontology2Match target = aml.getTarget();
		//Get the lists of properties from the ontologies
		objectSource = new Vector<Integer>(source.getObjectProperties());
		objectTarget = new Vector<Integer>(target.getObjectProperties());
		boolean objectP = objectSource.size() > 0 && objectTarget.size() > 0;
		dataSource = new Vector<Integer>(source.getDataProperties());
		dataTarget = new Vector<Integer>(target.getDataProperties());
		boolean dataP = dataSource.size() > 0 && dataTarget.size() > 0;
				
		//Set the title
		this.setTitle("Add Property Mapping");
		this.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);

		//Header panel: allows the user to toggle between property types
		//Setup the combo box listing the property types
		Vector<String> types = new Vector<String>();
		if(objectP)
			types.add(EntityType.OBJECT.toString()); 
		if(dataP)
			types.add(EntityType.DATA.toString()); 
		propTypes = new JComboBox<String>(types);
		propTypes.addActionListener(this);
		//Setup the header panel
		headerPanel = new JPanel();
		headerPanel.setBorder(new TitledBorder("Property Type"));
		headerPanel.add(propTypes);
		
		//Selection panel: allows the user to select properties of the
		//chosen type from both ontologies; uses a card layout tied to
		//the propTypes combo box to toggle between property types
        selectionPanel = new JPanel();
        cl = new CardLayout();
        selectionPanel.setLayout(cl);
        
		//Object Properties
        if(objectP)
        {
			String[] os = new String[objectSource.size()];
			for(int i = 0; i < os.length; i++)
				os[i] = source.getName(objectSource.get(i));
			objectS = new JComboBox<String>(os);
			objectS.setPreferredSize(new Dimension(300,28));
			JPanel osp = new JPanel();
			osp.setBorder(new TitledBorder("Source Ontology Property"));
			osp.add(objectS);
			String[] ot = new String[objectTarget.size()];
			for(int i = 0; i < ot.length; i++)
				ot[i] = target.getName(objectTarget.get(i));
			objectT = new JComboBox<String>(ot);
			objectT.setPreferredSize(new Dimension(300,28));
			JPanel otp = new JPanel();
			otp.setBorder(new TitledBorder("Target Ontology Property"));
			otp.add(objectT);
			objectPanel = new JPanel();
			objectPanel.setLayout(new BoxLayout(objectPanel, BoxLayout.PAGE_AXIS));
			objectPanel.add(osp);
			objectPanel.add(otp);
			selectionPanel.add(objectPanel,EntityType.OBJECT.toString());
        }
        //Data Properties
        if(dataP)
        {
			String[] ds = new String[dataSource.size()];
			for(int i = 0; i < ds.length; i++)
				ds[i] = source.getName(dataSource.get(i));
			dataS = new JComboBox<String>(ds);
			dataS.setPreferredSize(new Dimension(300,28));
			JPanel dsp = new JPanel();
			dsp.setBorder(new TitledBorder("Source Ontology Property"));
			dsp.add(dataS);
			String[] dt = new String[dataTarget.size()];
			for(int i = 0; i < dt.length; i++)
				dt[i] = target.getName(dataTarget.get(i));
			dataT = new JComboBox<String>(dt);
			dataT.setPreferredSize(new Dimension(300,28));
			JPanel dtp = new JPanel();
			dtp.setBorder(new TitledBorder("Target Ontology Property"));
			dtp.add(dataT);
			dataPanel = new JPanel();
			dataPanel.setLayout(new BoxLayout(dataPanel, BoxLayout.PAGE_AXIS));
			dataPanel.add(dsp);
			dataPanel.add(dtp);
	      	selectionPanel.add(dataPanel,EntityType.DATA.toString());
        }
        //Start in the first panel
       	cl.first(selectionPanel);
       	
       	//Relation panel: allows the user to select the mapping relationship
        Vector<String> relations = new Vector<String>();
        for(MappingRelation m : MappingRelation.values())
        	relations.add(m.getLabel());
        rels = new JComboBox<String>(relations);
		relationPanel = new JPanel();
		relationPanel.setBorder(new TitledBorder("Mapping Relation"));
		relationPanel.add(rels);
		
		//Button panel: allows the user to add the selected mapping or exit the dialog
		cancel = new JButton("Cancel");
		cancel.setPreferredSize(new Dimension(70,28));
		cancel.addActionListener(this);
		add = new JButton("Add");
		add.setPreferredSize(new Dimension(70,28));
		add.addActionListener(this);
		buttonPanel = new JPanel();
		buttonPanel.add(cancel);
		buttonPanel.add(add);
		
		//Dialog panel: englobes all previous panels
		dialogPanel = new JPanel();
		dialogPanel.setLayout(new BoxLayout(dialogPanel, BoxLayout.PAGE_AXIS));
		dialogPanel.add(headerPanel);
		dialogPanel.add(selectionPanel);
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
		else if(o == add)
		{
			int sourceId, targetId;
			if(propTypes.getSelectedItem().equals(EntityType.OBJECT.toString()))
			{
				sourceId = objectSource.get(objectS.getSelectedIndex());
				targetId = objectTarget.get(objectT.getSelectedIndex());
			}
			else
			{
				sourceId = dataSource.get(dataS.getSelectedIndex());
				targetId = dataTarget.get(dataT.getSelectedIndex());
			}
			if(sourceId == targetId)
			{
				JOptionPane.showMessageDialog(this,
					    "Source and target properties are the same property" +
					    "\n(they have the same URI) and thus can't be mapped.",
					    "Warning",
					    JOptionPane.WARNING_MESSAGE);
			}
			else if(aml.getAlignment().containsMapping(sourceId, targetId))
			{
				JOptionPane.showMessageDialog(this,
					    "Source and target properties are already mapped.",
					    "Warning",
					    JOptionPane.WARNING_MESSAGE);
			}
			else
			{
				aml.getAlignment().add(sourceId,targetId,1.0,
						MappingRelation.parseRelation((String)rels.getSelectedItem()));
				aml.needSave(true);
				aml.refreshGUI();
			}
			this.dispose();
		}
		else if(o == propTypes)
		{
			cl.show(selectionPanel, (String)propTypes.getSelectedItem());
		}
	}
}