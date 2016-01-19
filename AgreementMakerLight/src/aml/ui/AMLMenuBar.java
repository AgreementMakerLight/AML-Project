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

public class AMLMenuBar extends JMenuBar implements ActionListener, Runnable, WindowListener
{
	
//Attributes
	
	private static final long serialVersionUID = 4336946114423673015L;
	private AML aml;
	private Console c;
    private JMenu file, match, filter;
    private JMenuItem openO, closeO, openA,	closeA, saveA,
    				  matchAuto, matchManual, addC, addP,
    				  evaluate, resolve, flag, remove;
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
        resolve = new JMenuItem("Resolve Problems");
        resolve.addActionListener(this);
        filter.add(resolve);
        flag = new JMenuItem("Flag Problems");
        flag.addActionListener(this);
        filter.add(flag);
        filter.addSeparator();
        remove = new JMenuItem("Remove Incorrect Mappings");
        remove.addActionListener(this);
        filter.add(remove);
        add(filter);
        
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
			if(aml.needSave())
			{
				int option = JOptionPane.showConfirmDialog(this, "Save current alignment before proceeding?",
						"Unsaved Alignment", JOptionPane.YES_NO_CANCEL_OPTION);
				if(option == JOptionPane.YES_OPTION)
					saveAlignment();
				else if(option == JOptionPane.CANCEL_OPTION)
					return;
			}
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
			saveAlignment();
		}
		else if(o == closeA)
		{
			if(aml.needSave())
			{
				int option = JOptionPane.showConfirmDialog(this, "Save alignment before closing?",
						"Unsaved Alignment", JOptionPane.YES_NO_CANCEL_OPTION);
				if(option == JOptionPane.YES_OPTION)
					saveAlignment();
				else if(option == JOptionPane.CANCEL_OPTION)
					return;
			}
			aml.closeAlignment();
		}
		//Match Options
		else if(o == matchAuto)
		{
			if(aml.needSave())
			{
				int option = JOptionPane.showConfirmDialog(this, "Save current alignment before proceeding?",
						"Unsaved Alignment", JOptionPane.YES_NO_CANCEL_OPTION);
				if(option == JOptionPane.YES_OPTION)
					saveAlignment();
				else if(option == JOptionPane.CANCEL_OPTION)
					return;
			}
			c = new Console();
			c.addWindowListener(this);
			console = new Thread(c);
			console.start();
			action = new Thread(this);
			action.start();
		}
		else if(o == matchManual)
		{
			if(aml.needSave())
			{
				int option = JOptionPane.showConfirmDialog(this, "Save current alignment before proceeding?",
						"Unsaved Alignment", JOptionPane.YES_NO_CANCEL_OPTION);
				if(option == JOptionPane.YES_OPTION)
					saveAlignment();
				else if(option == JOptionPane.CANCEL_OPTION)
					return;
			}
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
		else if(o == resolve)
		{
			new FilterOptions();
		}
		else if(o == flag)
		{
			new FlagOptions();
		}
		else if(o == remove)
		{
			aml.removeIncorrect();
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
		resolve.setEnabled(aml.hasAlignment());
		flag.setEnabled(aml.hasAlignment());
		remove.setEnabled(aml.hasAlignment());
	}

	@Override
	public void run()
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
	
//Private Methods
	
	private void saveAlignment()
	{
		AlignmentFileChooser fc = aml.getAlignmentFileChooser();
		int returnVal = fc.showSaveDialog(this);
		if(returnVal == JFileChooser.APPROVE_OPTION)
		{
			String f = fc.getSelectedFile().getAbsolutePath();
			String filter = fc.getFileFilter().getDescription();
			if(filter.startsWith("OAEI"))
			{
				if(!f.endsWith(".rdf"))
					f += ".rdf";
				try{aml.saveAlignmentRDF(f);}
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
				try{aml.saveAlignmentTSV(f);}
				catch(Exception x)
				{
					JOptionPane.showMessageDialog(this, "Could not save alignment!\n" + 
							x.getMessage(),	"Error", JOptionPane.ERROR_MESSAGE); 
				}
			}
		}
	}
}