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
* @date 29-09-2014                                                            *
* @version 2.1                                                                *
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

public class AMLMenuBar extends JMenuBar implements ActionListener, Runnable
{
	
//Attributes
	
	private static final long serialVersionUID = 4336946114423673015L;
	private AML aml;
	private Console c;
    private JMenu ontologies, alignment, view;
    private JMenuItem openO, closeO, matchAuto, matchManual;
    private JMenuItem openA, closeA, saveA, add, repair, evaluate;
    private JMenuItem next, previous, goTo, search, options;
    private boolean match;
    
//Constructors
    
    /**
     * Builds a new AMLMenuBar
     */
    public AMLMenuBar()
    {
    	super();
        aml = AML.getInstance();
    	
    	//Ontologies Menu
    	ontologies = new JMenu();
        ontologies.setText("Ontologies");
        openO = new JMenuItem();
        openO.setText("Open Ontologies");
        openO.addActionListener(this);
        ontologies.add(openO);
        closeO = new JMenuItem();
        closeO.setText("Close Ontologies");
        closeO.addActionListener(this);
        ontologies.add(closeO);
        ontologies.addSeparator();
        matchAuto = new JMenuItem();
        matchAuto.setText("Automatic Match");
        matchAuto.addActionListener(this);
        ontologies.add(matchAuto);
        matchManual = new JMenuItem();
        matchManual.setText("Custom Match");
        matchManual.addActionListener(this);
        ontologies.add(matchManual);
        add(ontologies);
        
        //Alignment Menu
        alignment = new JMenu();
        alignment.setText("Alignment");
        openA = new JMenuItem();
        openA.setText("Open Alignment");
        openA.addActionListener(this);
        alignment.add(openA);
        saveA = new JMenuItem();
        saveA.setText("Save Alignment");
        saveA.addActionListener(this);
        alignment.add(saveA);
        closeA = new JMenuItem();
        closeA.setText("Close Alignment");
        closeA.addActionListener(this);
        alignment.add(closeA);
        alignment.addSeparator();
        //add = new JMenuItem();
        //add.setText("Add Mapping");
        //add.addActionListener(this);
        //alignment.add(add);
        //alignment.addSeparator();
        repair = new JMenuItem();
        repair.setText("Repair Alignment");
        repair.addActionListener(this);
        alignment.add(repair);
        alignment.addSeparator();
        evaluate = new JMenuItem();
        evaluate.setText("Evaluate Alignment");
        evaluate.addActionListener(this);
        alignment.add(evaluate);
        add(alignment);
                
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
		//Ontology Options
		if(o == openO)
		{
			new OpenOntologies();
		}
		else if(o == closeO)
		{
			aml.closeOntologies();
		}
		else if(o == matchAuto)
		{
			c = new Console();
			new Thread(c).start();
			match = true;
			new Thread(this).start();
		}
		else if(o == matchManual)
		{
			new MatchOptions();
		}
		//Alignment Options
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
			aml.closeAlignment();
		}
		else if(o == add)
		{
			//new AddMapping();
		}
		else if(o == repair)
		{
			c = new Console();
			match = false;
			new Thread(this).start();
		}
		else if(o == evaluate)
		{
			new EvaluateAlignment();
		}
		//View Options
		else if(o == next)
		{
			aml.nextMapping();
		}
		else if(o == previous)
		{
			aml.previousMapping();
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
		closeO.setEnabled(aml.hasOntologies());
		matchAuto.setEnabled(aml.hasOntologies());
		matchManual.setEnabled(aml.hasOntologies());
		openA.setEnabled(aml.hasOntologies());
		closeA.setEnabled(aml.hasAlignment());
		saveA.setEnabled(aml.hasAlignment());
		//add.setEnabled(aml.hasAlignment());
		repair.setEnabled(aml.hasAlignment());
		evaluate.setEnabled(aml.hasAlignment());
		next.setEnabled(aml.hasAlignment());
		previous.setEnabled(aml.hasAlignment());
		goTo.setEnabled(aml.hasAlignment());
		search.setEnabled(aml.hasAlignment());
		options.setEnabled(aml.hasOntologies());
	}

	@Override
	public void run()
	{
		if(match)
		{
			try
			{
				Thread.sleep(1000);
			}
			catch (InterruptedException e)
			{
				//Do nothing
			}
			aml.matchAuto();
		}
		else
			aml.repair();
		try
		{
			Thread.sleep(1500);
		}
		catch (InterruptedException e)
		{
			//Do nothing
		}
		c.finish();
	}
}
