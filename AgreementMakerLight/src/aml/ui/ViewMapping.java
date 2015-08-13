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
* Displays the detailed information about a mapping.                          *
*                                                                             *
* @author Daniel Faria & Catarina Martins                                     *
* @date 13-08-2015                                                            *
******************************************************************************/
package aml.ui;
 
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Set;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import aml.AML;
import aml.match.Mapping;
import aml.ontology.DataProperty;
import aml.ontology.Lexicon;
import aml.ontology.ObjectProperty;
import aml.ontology.Ontology2Match;
import aml.ontology.RelationshipMap;
import aml.ontology.URIMap;
import aml.settings.LexicalType;
import aml.settings.EntityType;
 

public class ViewMapping extends JDialog implements ItemListener, ActionListener
{
	
	private static final long serialVersionUID = 4516245633857479148L;
	private AML aml;
	private URIMap uris;
	private Ontology2Match source, target;
	private Mapping m;
	private int currentMapping;
	private RelationshipMap rm;
	private JPanel cards, lexic, struct;
	private JButton close, view;
	private Font boldFont;
	private final static String LEXICINFO = "Lexical Information";
    private final static String STRUCTINFO = "Structural Information";
    
    public ViewMapping(int index)
    {
        super();
        this.setMinimumSize(new Dimension(400,this.getHeight()));
        
        aml = AML.getInstance();
        uris = aml.getURIMap();
        source = aml.getSource();
        target = aml.getTarget();
        rm = aml.getRelationshipMap();
        m = aml.getAlignment().get(index);
		int sourceId = m.getSourceId();
		int targetId = m.getTargetId();
        currentMapping = index;
        
        this.setTitle("Mapping Details");
		this.setModalityType(ModalityType.APPLICATION_MODAL);
		
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
		panel.setBorder(new EmptyBorder(4,4,4,4));
		
		//Type
		EntityType t = uris.getType(sourceId);
        JLabel type = new JLabel();
		Font font =  type.getFont();
		boldFont = new Font(font.getFontName(), Font.BOLD, font.getSize());
		type.setFont(boldFont);
		type.setText("Type: " + t + " Mapping");
		JPanel title = new JPanel();
		title.add(type);
		panel.add(title);

		if(t.equals(EntityType.CLASS))
		{
			//Info Selection
			Vector<String> types = new Vector<String>(2);
	        String[] typeStrings = {LEXICINFO, STRUCTINFO};
			for(String s : typeStrings)
				types.add(s);
	        JComboBox<String> typeList = new JComboBox<String>(types);
			typeList.setSelectedItem(0);
			typeList.addItemListener(this);
			JPanel typePane = new JPanel();
			typePane.add(typeList);
			panel.add(typePane);
					
			//Create the Lexical Information Card.
	        lexic = new JPanel();
	        lexic.setLayout(new BoxLayout(lexic, BoxLayout.PAGE_AXIS));
	        
	        //For the Source Ontology
			Lexicon srcLex = source.getLexicon();
	        JLabel sourceO = new JLabel("Source Class:");
			sourceO.setFont(boldFont);
			JPanel src = new JPanel();
			src.add(sourceO);
			lexic.add(src);
			
			JPanel sLex = new JPanel(new GridLayout(0,1));
	        JLabel localNameS = new JLabel("Local Name: " +
	        		uris.getLocalName(sourceId));
	        sLex.add(localNameS);
	        
	        String lab = "Label(s): ";
	        Set<String> names = srcLex.getNames(sourceId,LexicalType.LABEL);
			for(String s : names)
				lab += s + "; ";
			if(names.size() == 0)
				lab += "N/A";
			else
				lab = lab.substring(0, lab.length()-2);
	        JLabel labelS = new JLabel(lab);
	        sLex.add(labelS);
	        
	        names = srcLex.getNames(sourceId,LexicalType.EXACT_SYNONYM);
			if(names.size() > 0)
			{
		        lab = "Exact Synonyms(s): ";
				for(String s : names)
					lab += s + "; ";
				lab = lab.substring(0, lab.length()-2);
		        JLabel exactS = new JLabel(lab);
		        sLex.add(exactS);
			}
	        
	        names = srcLex.getNames(sourceId,LexicalType.OTHER_SYNONYM);
			if(names.size() > 0)
			{
		        lab = "Other Synonyms(s): ";
				for(String s : names)
					lab += s + "; ";
				lab = lab.substring(0, lab.length()-2);
		        JLabel otherS = new JLabel(lab);
		        sLex.add(otherS);
			}
	        
	        names = srcLex.getNames(sourceId,LexicalType.FORMULA);
			if(names.size() > 0)
			{
		        lab = "Formula(s): ";
				for(String s : names)
					lab += s + "; ";
				lab = lab.substring(0, lab.length()-2);
		        JLabel formS = new JLabel(lab);
		        sLex.add(formS);
			}
			lexic.add(sLex);
			
	        //For the Target Ontology
			Lexicon tgtLex = target.getLexicon();
	        JLabel targetO = new JLabel("Target Class:");
			targetO.setFont(boldFont);
			sourceO.setFont(boldFont);
			JPanel tgt = new JPanel();
			tgt.add(targetO);
			lexic.add(tgt);
			
			JPanel tLex = new JPanel(new GridLayout(0,1));
	        JLabel localNameT = new JLabel("Local Name: " +
	        		uris.getLocalName(targetId));
	        tLex.add(localNameT);
	        
	        lab = "Label(s): ";
	        names = tgtLex.getNames(targetId,LexicalType.LABEL);
			for(String s : names)
				lab += s + "; ";
			if(names.size() == 0)
				lab += "N/A";
			else
				lab = lab.substring(0, lab.length()-2);
	        JLabel labelT = new JLabel(lab);
	        tLex.add(labelT);
	        
	        names = tgtLex.getNames(targetId,LexicalType.EXACT_SYNONYM);
			if(names.size() > 0)
			{
		        lab = "Exact Synonyms(s): ";
				for(String s : names)
					lab += s + "; ";
				lab = lab.substring(0, lab.length()-2);
		        JLabel exactT = new JLabel(lab);
		        tLex.add(exactT);
			}
	        
	        names = tgtLex.getNames(targetId,LexicalType.OTHER_SYNONYM);
			if(names.size() > 0)
			{
		        lab = "Other Synonyms(s): ";
				for(String s : names)
					lab += s + "; ";
				lab = lab.substring(0, lab.length()-2);
		        JLabel otherT = new JLabel(lab);
		        tLex.add(otherT);
			}
	        
	        names = tgtLex.getNames(targetId,LexicalType.FORMULA);
			if(names.size() > 0)
			{
		        lab = "Formula(s): ";
				for(String s : names)
					lab += s + "; ";
				lab = lab.substring(0, lab.length()-2);
		        JLabel formT = new JLabel(lab);
		        tLex.add(formT);
			}
			lexic.add(tLex);

	        //Create the Structural Information Card.
	        struct = new JPanel();
	        struct.setLayout(new BoxLayout(struct, BoxLayout.PAGE_AXIS));
	
			//For the Source Ontology
	        JLabel ontologyS = new JLabel("Source Class:");
			ontologyS.setFont(boldFont);
			JPanel titleS = new JPanel();
			titleS.add(ontologyS);
			struct.add(titleS);
			
			Set<Integer> directSetSource = rm.getSuperClasses(sourceId,true);
			lab = "Direct Superclass(es): ";
			for(Integer i : directSetSource)
				lab += source.getName(i) + "; ";
			if(directSetSource.size() == 0)
				lab += "N/A";
			else
				lab = lab.substring(0, lab.length()-2);
			JLabel directS = new JLabel(lab);
			
			Set<Integer> highSetSource = rm.getHighLevelAncestors(sourceId);
			lab = "High-Level Ancestors: ";
			for(Integer i : highSetSource)
				lab += source.getName(i) + "; ";
			if(highSetSource.size() == 0)
				lab += "N/A";
			else
				lab = lab.substring(0, lab.length()-2);
			JLabel highS = new JLabel(lab);
			
			Set<Integer> disjointSetSource = rm.getDisjointTransitive(sourceId);
			lab = "Disjoint Classes: ";
			for(Integer i : disjointSetSource)
				lab += source.getName(i) + "; ";
			if(disjointSetSource.size() == 0)
				lab += "N/A";
			else
				lab = lab.substring(0, lab.length()-2);
			JLabel disjointsS = new JLabel(lab);
	        
			JPanel panelSource = new JPanel(new GridLayout(0,1));
			panelSource.add(directS);
			panelSource.add(highS);
			panelSource.add(disjointsS);
			struct.add(panelSource);
			
			//For the Target Ontology
			JLabel ontologyT = new JLabel("Target Class:");
			font =  ontologyT.getFont();
			boldFont = new Font(font.getFontName(), Font.BOLD, font.getSize());
			ontologyT.setFont(boldFont);
			JPanel titleT = new JPanel();
			titleT.add(ontologyT);
			struct.add(titleT);
			
			Set<Integer> directSetTarget = rm.getSuperClasses(targetId,true);
			lab = "Direct Superclass(es): ";
			for(Integer i : directSetTarget)
				lab += target.getName(i) + "; ";
			if(directSetTarget.size() == 0)
				lab += "N/A";
			else
				lab = lab.substring(0, lab.length()-2);
			JLabel directT = new JLabel(lab);
			
			Set<Integer> highSetTarget = rm.getHighLevelAncestors(targetId);
			lab = "High-Level Ancestors: ";
			for(Integer i : highSetTarget)
				lab += target.getName(i) + "; ";
			if(highSetTarget.size() == 0)
				lab += "N/A";
			else
				lab = lab.substring(0, lab.length()-2);
			JLabel highT = new JLabel(lab);
			
			Set<Integer> disjointSetTarget = rm.getDisjointTransitive(targetId);
			lab = "Disjoint Classes: ";
			for(Integer i : disjointSetTarget)
				lab += target.getName(i) + "; ";
			if(disjointSetTarget.size() == 0)
				lab += "N/A";
			else
				lab = lab.substring(0, lab.length()-2);
			JLabel disjointsT = new JLabel(lab);
			
			JPanel panelTarget = new JPanel(new GridLayout(0,1));
			panelTarget.add(directT);
			panelTarget.add(highT);
			panelTarget.add(disjointsT);
			struct.add(panelTarget);
			
	        //Create the panel that contains the "cards".
	        cards = new JPanel(new CardLayout());
	        cards.add(lexic, LEXICINFO);
	        cards.add(struct, STRUCTINFO);
	 
	        panel.add(cards, BorderLayout.CENTER);     
		}
		else
		{
			//For the Source Ontology
	        JLabel sourceO = new JLabel("Source Property:");
			sourceO.setFont(boldFont);
			JPanel titleSource = new JPanel();
			titleSource.add(sourceO);
			panel.add(titleSource);
			
			JPanel sourcePanel = new JPanel(new GridLayout(0,1));
			
			JLabel nameS = new JLabel("Name: " + source.getName(sourceId));
			sourcePanel.add(nameS);
			
			if(t.equals(EntityType.OBJECT))
			{
				ObjectProperty pSource = source.getObjectProperty(sourceId);
				Set<Integer> domain = pSource.getDomain();
				String lab = "Domain: ";
				for(Integer i : domain)
					lab += source.getName(i) + "; ";
				if(domain.size() > 0)
					lab = lab.substring(0, lab.length()-2);
				else
					lab += "N/A";
				JLabel domainS = new JLabel(lab);
				sourcePanel.add(domainS);
				
				Set<Integer> range = pSource.getRange();
				lab = "Range: ";
				for(Integer i : range)
					lab += source.getName(i) + "; ";
				if(range.size() > 0)
					lab = lab.substring(0, lab.length()-2);
				else
					lab += "N/A";
				JLabel rangeS = new JLabel(lab);
				sourcePanel.add(rangeS);
				if(pSource.isFunctional())
				{
					JLabel funS = new JLabel("Functional Property");
					sourcePanel.add(funS);
				}
			}
			else if(t.equals(EntityType.DATA))
			{
				DataProperty pSource = source.getDataProperty(sourceId);
				Set<Integer> domain = pSource.getDomain();
				String lab = "Domain: ";
				for(Integer i : domain)
					lab += source.getName(i) + "; ";
				if(domain.size() > 0)
					lab = lab.substring(0, lab.length()-2);
				else
					lab += "N/A";
				JLabel domainS = new JLabel(lab);
				sourcePanel.add(domainS);
				
				Set<String> range = pSource.getRange();
				lab = "Range: ";
				for(String s : range)
					lab += s + "; ";
				if(range.size() > 0)
					lab = lab.substring(0, lab.length()-2);
				else
					lab += "N/A";
				JLabel rangeS = new JLabel(lab);
				sourcePanel.add(rangeS);
				if(pSource.isFunctional())
				{
					JLabel funS = new JLabel("Functional Property");
					sourcePanel.add(funS);
				}
			}
			panel.add(sourcePanel);

			//For the Target Ontology
	        JLabel targetO = new JLabel("Target Property:");
			targetO.setFont(boldFont);
			JPanel titleTarget = new JPanel();
			titleTarget.add(targetO);
			panel.add(titleTarget);
			
			JPanel targetPanel = new JPanel(new GridLayout(0,1));
			
			JLabel nameT = new JLabel("Name: " + target.getName(targetId));
			targetPanel.add(nameT);
			
			if(t.equals(EntityType.OBJECT))
			{
				ObjectProperty pTarget = target.getObjectProperty(targetId);
				Set<Integer> domain = pTarget.getDomain();
				String lab = "Domain: ";
				for(Integer i : domain)
					lab += target.getName(i) + "; ";
				if(domain.size() > 0)
					lab = lab.substring(0, lab.length()-2);
				else
					lab += "N/A";
				JLabel domainT = new JLabel(lab);
				targetPanel.add(domainT);
				
				Set<Integer> range = pTarget.getRange();
				lab = "Range: ";
				for(Integer i : range)
					lab += target.getName(i) + "; ";
				if(range.size() > 0)
					lab = lab.substring(0, lab.length()-2);
				else
					lab += "N/A";
				JLabel rangeT = new JLabel(lab);
				targetPanel.add(rangeT);
				if(pTarget.isFunctional())
				{
					JLabel funS = new JLabel("Functional Property");
					targetPanel.add(funS);
				}
			}
			else if(t.equals(EntityType.DATA))
			{
				DataProperty pTarget = target.getDataProperty(targetId);
				Set<Integer> domain = pTarget.getDomain();
				String lab = "Domain: ";
				for(Integer i : domain)
					lab += target.getName(i) + "; ";
				if(domain.size() > 0)
					lab = lab.substring(0, lab.length()-2);
				else
					lab += "N/A";
				JLabel domainT = new JLabel(lab);
				targetPanel.add(domainT);
				
				Set<String> range = pTarget.getRange();
				lab = "Range: ";
				for(String s : range)
					lab += s + "; ";
				if(range.size() > 0)
					lab = lab.substring(0, lab.length()-2);
				else
					lab += "N/A";
				JLabel rangeT = new JLabel(lab);
				targetPanel.add(rangeT);
				if(pTarget.isFunctional())
				{
					JLabel funS = new JLabel("Functional Property");
					targetPanel.add(funS);
				}
			}
			panel.add(targetPanel);
		}
		
        //Buttons
        close = new JButton("Close");
		close.setPreferredSize(new Dimension(70,28));
		close.addActionListener(this);
		view = new JButton("View in Graph");
		view.setPreferredSize(new Dimension(125,28));
		view.addActionListener(this);
		JPanel buttonPanel = new JPanel();
		buttonPanel.add(close);
		buttonPanel.add(view);
		panel.add(buttonPanel);

		add(panel);
		
        this.pack();
        this.setVisible(true);
		 
    }
    
    public void itemStateChanged(ItemEvent evt)
    {
        CardLayout cl = (CardLayout)(cards.getLayout());
        cl.show(cards, (String)evt.getItem());
    }
    
    public void actionPerformed(ActionEvent e)
    {
 	   JButton b = (JButton)e.getSource();
 	   
 	   if(b == close)
 	   {
  		  this.dispose();
 	   }
 	   
 	   else if(b == view)
 	   {
 		  AML.getInstance().goTo(currentMapping);
 		  this.dispose();
 	   }
    }
}