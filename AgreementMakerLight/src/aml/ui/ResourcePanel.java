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
* GUI panel that details the open Ontologies and Alignment and the Mapping    *
* currently being viewed.                                                     *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 23-06-2014                                                            *
******************************************************************************/
package aml.ui;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JInternalFrame;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;

import aml.AML;
import aml.match.Alignment;
import aml.match.Mapping;
import aml.ontology.Ontology2Match;

public class ResourcePanel extends JInternalFrame
{

//Attributes
	
	private static final long serialVersionUID = 4634954326864686562L;
	private JTextPane desc;
	private StyledDocument doc;
	private Style def,bold,s,t,u;
	
//Constructor
	
	public ResourcePanel(Dimension max, Dimension min)
	{
		super("Resource Panel",false,false,false,false);
		
		this.setMaximumSize(max);
		this.setPreferredSize(min);
		
		desc = new JTextPane();
		desc.setEditable(false);
		
		doc = desc.getStyledDocument();
		def = StyleContext.getDefaultStyleContext(). getStyle(StyleContext.DEFAULT_STYLE);
		bold = doc.addStyle("bold", def);
		StyleConstants.setBold(bold, true);
		s = doc.addStyle("source", def);
		StyleConstants.setBold(s, true);
		StyleConstants.setForeground(s, Color.red);
		t = doc.addStyle("target", def);
		StyleConstants.setBold(t, true);
		StyleConstants.setForeground(t, Color.blue);
		u = doc.addStyle("uri", def);
		StyleConstants.setUnderline(u, true);
		
		setContentPane(desc);
		pack();
		setVisible(true);

		refresh();
	}
	
//Public Methods
	
	public void refresh()
	{
		try
		{
			doc.remove(0, doc.getLength());
			doc.insertString(doc.getLength(), "Source Ontology: ", s);
			Ontology2Match source = AML.getInstance().getSource();
			if(source == null)
				doc.insertString(doc.getLength(), "N/A\n", def);
			else
			{
				doc.insertString(doc.getLength(), source.getURI(), u);
				String src = " (" + source.classCount() + " classes, " +
						(source.dataPropertyCount()+source.objectPropertyCount()) +
						" properties)\n";
				doc.insertString(doc.getLength(), src, def);
			}
			
			doc.insertString(doc.getLength(), "Target Ontology: ", t);
			Ontology2Match target = AML.getInstance().getTarget();
			if(target == null)
				doc.insertString(doc.getLength(), "N/A\n", def);
			else
			{
				doc.insertString(doc.getLength(), target.getURI(), u);
				String tgt = " (" + target.classCount() + " classes, " +
						(target.dataPropertyCount()+target.objectPropertyCount()) +
						" properties)\n";
				doc.insertString(doc.getLength(), tgt, def);
			}
			
			doc.insertString(doc.getLength(), "Alignment: ", bold);
			Alignment a = AML.getInstance().getAlignment();
			if(a == null)
				doc.insertString(doc.getLength(), "N/A\n", def);
			else
			{
				String al = a.size() + " mappings";
				String eval = AML.getInstance().getEvaluation();
				if(eval != null)
					al += " (" + eval + ")";
				al += "\n";
				doc.insertString(doc.getLength(), al, def);
			}
			
			doc.insertString(doc.getLength(), "Current Mapping: ", bold);
			Mapping m = AML.getInstance().getCurrentMapping();
			if(m == null)
				doc.insertString(doc.getLength(), "N/A", def);
			else
			{
				String mapDesc = source.getLexicon().getBestName(m.getSourceId()) +
						" = " + target.getLexicon().getBestName(m.getTargetId()) +
						" (" + (AML.getInstance().getCurrentIndex()+1) + "/" + a.size() + ")";
				doc.insertString(doc.getLength(), mapDesc, def);
			}
		}
		catch (BadLocationException e)
		{
			e.printStackTrace();
		}
	}
}
