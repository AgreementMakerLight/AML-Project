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
* Menu bar for the GUI.                                                       *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import aml.AML;
import aml.match.Alignment;

public class AMLMenuBar extends JMenuBar implements ActionListener, Runnable, WindowListener
{
	
//Attributes
	
	private static final long serialVersionUID = 4336946114423673015L;
	private AML aml;
	private Console c;
    private JMenu file, match, filter, view;
    private JMenuItem openO, closeO, openA,	closeA, saveA,
    				  matchAuto, matchManual, addC, addP,
    				  evaluate, obsolete, select, repair, flag,
    				  search, sortAsc, sortDesc, options;
    private enum Mode{ MATCH, REPAIR, REVISE; }
    private Mode m;
    private Thread action, console;
    
//Constructors
    
    /**
     * Builds a new AMLMenuBar
     */
    public AMLMenuBar()
    {
    	super();
        aml = AML.getInstance();
    	
    	//File Menu
    	file = new JMenu("File");
        openO = new JMenuItem("Open Ontologies");
        openO.addActionListener(this);
        file.add(openO);
        closeO = new JMenuItem("Close Ontologies");
        closeO.addActionListener(this);
        file.add(closeO);
        file.addSeparator();
        openA = new JMenuItem("Open Alignment");
        openA.addActionListener(this);
        file.add(openA);
        saveA = new JMenuItem("Save Alignment");
        saveA.addActionListener(this);
        file.add(saveA);
        closeA = new JMenuItem("Close Alignment");
        closeA.addActionListener(this);
        file.add(closeA);
        add(file);
        
        //Match Menu
        match = new JMenu("Match");
        matchAuto = new JMenuItem("Automatic Match");
        matchAuto.addActionListener(this);
        match.add(matchAuto);
        matchManual = new JMenuItem("Custom Match");
        matchManual.addActionListener(this);
        match.add(matchManual);
        match.addSeparator();
        addC = new JMenuItem();
        addC.setText("Add Class Mapping");
        addC.addActionListener(this);
        match.add(addC);
        addP = new JMenuItem();
        addP.setText("Add Property Mapping");
        addP.addActionListener(this);
        match.add(addP);        
        add(match);
        
        //Filter Menu
    	filter = new JMenu("Filter");
        evaluate = new JMenuItem("Evaluate with Reference");
        evaluate.addActionListener(this);
        filter.add(evaluate);
        filter.addSeparator();
        obsolete = new JMenuItem("Obsoletion");
        obsolete.addActionListener(this);
        filter.add(obsolete);
        select = new JMenuItem("Cardinality (Selection)");
        select.addActionListener(this);
        filter.add(select);
        repair = new JMenuItem("Coherence (Repair)");
        repair.addActionListener(this);
        filter.add(repair);
        filter.addSeparator();
        flag = new JMenuItem("Flag Problems");
        flag.addActionListener(this);
        filter.add(flag);
        add(filter);
        
        //View Menu
        view = new JMenu("View");
        search = new JMenuItem("Search Alignment");
        search.addActionListener(this);
        view.add(search);
        view.addSeparator();
        sortAsc = new JMenuItem("Sort Ascending");
        sortAsc.addActionListener(this);
        view.add(sortAsc);
        sortDesc = new JMenuItem("Sort Descending");
        sortDesc.addActionListener(this);
        view.add(sortDesc);
        view.addSeparator();
        options = new JMenuItem("Graph Options");
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
		//File Options
		if(o == openO)
		{
			new OpenOntologies();
		}
		else if(o == closeO)
		{
			aml.closeOntologies();
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
			aml.closeAlignment();
		}
		//Match Options
		else if(o == matchAuto)
		{
			m = Mode.MATCH;
			c = new Console();
			c.addWindowListener(this);
			console = new Thread(c);
			console.start();
			action = new Thread(this);
			action.start();
		}
		else if(o == matchManual)
		{
			new MatchOptions();
		}
		else if(o == addC)
		{
			new AddClassMapping();
		}
		else if(o == addP)
		{
			new AddPropertyMapping();
		}
		//Filter Options
		else if(o == evaluate)
		{
			new EvaluateAlignment();
		}
		else if(o == obsolete)
		{
			aml.removeObsolete();
		}
		else if(o == select)
		{
			aml.select();
		}
		else if(o == repair)
		{
			m = Mode.REPAIR;
			c = new Console();
			c.addWindowListener(this);
			console = new Thread(c);
			console.start();
			action = new Thread(this);
			action.start();
		}
		else if(o == flag)
		{
			new FlagOptions();
		}
		//View Options
		else if(o == search)
		{
			new SearchAlignment();
		}
		else if(o == sortAsc)
		{
			aml.sortAscending();
		}
		else if(o == sortDesc)
		{
			aml.sortDescending();
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
		evaluate.setEnabled(aml.hasAlignment());
		addC.setEnabled(aml.hasAlignment() && aml.hasClasses());
		addP.setEnabled(aml.hasAlignment() && aml.hasProperties());
		repair.setEnabled(aml.hasAlignment());
		obsolete.setEnabled(aml.hasAlignment());
		select.setEnabled(aml.hasAlignment());
		flag.setEnabled(aml.hasAlignment());
		search.setEnabled(aml.hasAlignment());
		sortAsc.setEnabled(aml.hasAlignment());
		sortDesc.setEnabled(aml.hasAlignment());
		options.setEnabled(aml.hasOntologies());
	}

	@Override
	public void run()
	{
		if(m.equals(Mode.MATCH))
		{
			if(aml.hasAlignment())
				aml.closeAlignment();
			try
			{
				Thread.sleep(1000);
			}
			catch(InterruptedException e)
			{
				//Do nothing
			}
			aml.matchAuto();
		}
		else if(m.equals(Mode.REPAIR))
			aml.repair();
		else if(m.equals(Mode.REVISE))
			aml.repair();
		try
		{
			Thread.sleep(1500);
		}
		catch(InterruptedException e)
		{
			//Do nothing
		}
		c.finish();
	}

	@Override
	public void windowOpened(WindowEvent e){}

	@SuppressWarnings("deprecation")
	@Override
	public void windowClosing(WindowEvent e)
	{
		//Stop should be relatively safe in this case
		action.stop();
		c.finish();
	}

	@Override
	public void windowClosed(WindowEvent e){}

	@Override
	public void windowIconified(WindowEvent e){}

	@Override
	public void windowDeiconified(WindowEvent e){}

	@Override
	public void windowActivated(WindowEvent e){}

	@Override
	public void windowDeactivated(WindowEvent e){}
}