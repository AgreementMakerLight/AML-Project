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
* Menu bar for the GUI.                                                       *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 23-06-2014                                                            *
* @version 2.0                                                                *
******************************************************************************/
package aml.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import aml.AML;
import aml.match.Alignment;

public class AMLMenuBar extends JMenuBar implements ActionListener
{
	
//Attributes
	
	private static final long serialVersionUID = 4336946114423673015L;
    private JMenu file, match, view;
    private JMenuItem openO, closeO, openA, closeA, saveA, exit;
    private JMenuItem align, evaluate;
    private JMenuItem next, previous, goTo, search, options;
    
//Constructors
    
    /**
     * Builds a new AMLMenuBar
     */
    public AMLMenuBar()
    {
    	super();
        
    	//File Menu
    	file = new JMenu();
        file.setText("File");
        openO = new JMenuItem();
        openO.setText("Open Ontologies");
        openO.addActionListener(this);
        file.add(openO);
        closeO = new JMenuItem();
        closeO.setText("Close Ontologies");
        closeO.addActionListener(this);
        file.add(closeO);
        file.addSeparator();
        openA = new JMenuItem();
        openA.setText("Open Alignment");
        openA.addActionListener(this);
        file.add(openA);
        closeA = new JMenuItem();
        closeA.setText("Close Alignment");
        closeA.addActionListener(this);
        file.add(closeA);
        saveA = new JMenuItem();
        saveA.setText("Save Alignment");
        saveA.addActionListener(this);
        file.add(saveA);
        file.addSeparator();
        exit = new JMenuItem();
        exit.setText("Exit");
        exit.addActionListener(this);
        file.add(exit);
        add(file);
        
        //Match Menu
        match = new JMenu();
        match.setText("Match");
        align = new JMenuItem();
        align.setText("Match Ontologies");
        align.addActionListener(this);
        match.add(align);
        evaluate = new JMenuItem();
        evaluate.setText("Evaluate Alignment");
        evaluate.addActionListener(this);
        match.add(evaluate);
        add(match);
        
    	//View Menu
    	view = new JMenu();
        view.setText("View");
        next = new JMenuItem();
        next.setText("Next Mapping");
        next.addActionListener(this);
        view.add(next);
        previous = new JMenuItem();
        previous.setText("Previous Mapping");
        previous.addActionListener(this);
        view.add(previous);
        view.addSeparator();
        goTo = new JMenuItem();
        goTo.setText("Select Mapping");
        goTo.addActionListener(this);
        view.add(goTo);
        search = new JMenuItem();
        search.setText("Search Alignment");
        search.addActionListener(this);
        view.add(search);
        view.addSeparator();
        options = new JMenuItem();
        options.setText("Options");
        options.addActionListener(this);
        view.add(options);
        add(view);
        
        refresh();
    }

//Public Methods

	@Override
	public void actionPerformed(ActionEvent e)
	{
		Object o = e.getSource();
		
		if(o == exit)
		{
			System.exit(0);
		}
		else if(o == openO)
		{
			new OpenOntologies();
		}
		else if(o == closeO)
		{
			AML.getInstance().closeOntologies();
		}
		else if(o == openA)
		{
			AlignmentFileChooser fc = AML.getInstance().getAlignmentFileChooser();
			int returnVal = fc.showOpenDialog(this);
			if(returnVal == JFileChooser.APPROVE_OPTION)
			{
				try{AML.getInstance().openAlignment(fc.getSelectedFile().getAbsolutePath());}
				catch(Exception x)
				{
					JOptionPane.showMessageDialog(this, "Could not read alignment file!\n" + 
							x.getMessage(),	"Error", JOptionPane.ERROR_MESSAGE);
				}
			}
		}
		else if(o == saveA)
		{
			Alignment a = AML.getInstance().getAlignment();
			AlignmentFileChooser fc = AML.getInstance().getAlignmentFileChooser();
			int returnVal = fc.showSaveDialog(this);
			if(returnVal == JFileChooser.APPROVE_OPTION)
			{
				String f = fc.getSelectedFile().getAbsolutePath();
				String filter = fc.getFileFilter().getDescription();
				if(filter.startsWith("OAEI"))
				{
					if(!f.endsWith(".rdf"))
						f += ".rdf";
					try{a.saveRDF(f);}
					catch(Exception x)
					{
						JOptionPane.showMessageDialog(this, "Could not save alignment!\n" + 
								x.getMessage(),	"Error", JOptionPane.ERROR_MESSAGE); 
					}
				}
				else if(filter.startsWith("AML"))
				{
					if(!f.endsWith(".tsv"))
						f += ".tsv";
					try{a.saveTSV(f);}
					catch(Exception x)
					{
						JOptionPane.showMessageDialog(this, "Could not save alignment!\n" + 
								x.getMessage(),	"Error", JOptionPane.ERROR_MESSAGE); 
					}
				}
			}
		}
		else if(o == closeA)
		{
			AML.getInstance().closeAlignment();
		}
		else if(o == align)
		{
			new MatchOntologies();
		}
		else if(o == evaluate)
		{
			new EvaluateAlignment();
		}
		else if(o == next)
		{
			AML.getInstance().nextMapping();
		}
		else if(o == previous)
		{
			AML.getInstance().previousMapping();
		}
		else if(o == goTo)
		{
			new SelectMapping();
		}
		else if(o == search)
		{
			new SearchAlignment();
		}
		else if(o == options)
		{
			new ViewOptions();
		}
	}
	
	/**
	 * Refreshes the AMLMenuBar by enabling/disabling options depending
	 * on the state of the system (ontologies/alignment open or not)
	 */
	public void refresh()
	{
		closeO.setEnabled(AML.getInstance().hasOntologies());
		openA.setEnabled(AML.getInstance().hasOntologies());
		closeA.setEnabled(AML.getInstance().hasAlignment());
		saveA.setEnabled(AML.getInstance().hasAlignment());
		align.setEnabled(AML.getInstance().hasOntologies());
		evaluate.setEnabled(AML.getInstance().hasAlignment());
		next.setEnabled(AML.getInstance().hasAlignment());
		previous.setEnabled(AML.getInstance().hasAlignment());
		goTo.setEnabled(AML.getInstance().hasAlignment());
		search.setEnabled(AML.getInstance().hasAlignment());
	}
}
