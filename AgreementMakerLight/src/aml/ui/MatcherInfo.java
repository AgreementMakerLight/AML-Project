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
* Description for AML matching algorithms.                                    *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 23-06-2014                                                            *
* @version 2.0                                                                *
******************************************************************************/

package aml.ui;

import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;

import aml.settings.MatchingAlgorithm;

public class MatcherInfo extends JDialog implements ActionListener
{
	private static final long serialVersionUID = -5858067907963709840L;
	private JButton close;
	
	public MatcherInfo(Dialog parent, String m)
	{
		super(parent,m,true);

		JPanel dialogPanel = new JPanel();
		dialogPanel.setLayout(new BoxLayout(dialogPanel, BoxLayout.PAGE_AXIS));
		
		JTextArea info = new JTextArea();
		info.setBackground(new Color(214,217,223));
		info.setBorder(new EmptyBorder(5,5,5,5));
		info.setEditable(false);
		info.setText(getInfo(m));
		
		close = new JButton("Close");
		close.setPreferredSize(new Dimension(70,28));
		close.addActionListener(this);
		
		dialogPanel.add(info);
		dialogPanel.add(close);
		add(dialogPanel);
		
		pack();
		setVisible(true);		
	}
	
	@Override
	public void actionPerformed(ActionEvent a)
	{
		this.dispose();
	}

	private String getInfo(String m)
	{
		String details = "";
		if(m.equals(MatchingAlgorithm.AML.toString()))
			details = "AML Matcher is an ensemble of lexical and string matching algorithms\n" +
					  "analogous to the OAEI 2013 Matcher, but manually configurable.\n" +
					  "In addition to the listed options, you can use additional sources of\n" +
					  "background knowledge by placing them in \"./store/knowledge/\".\n" +
					  "It may take several minutes to run for large ontologies, particularly if\n" +
					  "repair is selected and/or WordNet is used as background knowledge.";
		else if(m.equals(MatchingAlgorithm.OAEI.toString()))
			details = "OAEI 2013 Matcher is an ensemble of lexical and string matching\n" +
					  "algorithms used by AgreementMakerLight in OAEI 2013.\n" +
					  "It performs automatic threshold, cardinality, and background\n" +
					  "knowledge (when on) selection, testing Uberon, WordNet and\n" +
					  "UMLS (unless excluded) as background knowledge sources.\n" +
					  "It may take several minutes to run for large ontologies, particularly\n" +
					  "if repair is selected.";
		else if(m.equals(MatchingAlgorithm.LEXICAL.toString()))
			details = "Lexical Matcher is a very efficient and generally precise name and\n" +
					  "synonym matching algorithm, which runs in a few seconds for even\n" +
					  "very large ontologies.";
		return details;
	}


}
