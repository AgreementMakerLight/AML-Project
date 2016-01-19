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
* Alignment filtering configuration dialog box for the GUI.                   *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.ui;

import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JPanel;

import aml.AML;
import aml.settings.Problem;

public class FilterOptions extends JDialog implements ActionListener, Runnable, WindowListener
{
	
//Attributes
	
	private static final long serialVersionUID = -4255910788613313495L;
	private AML aml;
	private Console c;
	private JButton cancel, resolve;
	private Vector<JCheckBox> filterers;
    private Vector<Problem> selectedSteps;
    private Thread action, console;
    
//Constructor
    
	public FilterOptions()
	{
		//Initialize
		super();
		this.setMinimumSize(new Dimension(200,100));
		//Get the AML instance
		aml = AML.getInstance();
		//And the lists of match steps & match configurations
		selectedSteps = aml.getFlagSteps();
		filterers = new Vector<JCheckBox>();
		for(Problem m : Problem.values())
		{
			if(m.equals(Problem.QUALITY))
				continue;
			JCheckBox cb = new JCheckBox(m.toString());
			cb.setSelected(selectedSteps.contains(m));
			filterers.add(cb);		
		}

		//Set the title and modality
		this.setTitle("Resolve Problems");
		this.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
		
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
	
		//Match Steps
		JPanel subPanel = new JPanel();
		JPanel flaggerPanel = new JPanel();
		flaggerPanel.setLayout(new BoxLayout(flaggerPanel, BoxLayout.Y_AXIS));
		for(JCheckBox cb : filterers)
			flaggerPanel.add(cb);
		subPanel.add(flaggerPanel);
		panel.add(subPanel);

        //Button Panel
		cancel = new JButton("Cancel");
		cancel.setPreferredSize(new Dimension(80,28));
		cancel.addActionListener(this);
		resolve = new JButton("Resolve");
		resolve.setPreferredSize(new Dimension(80,28));
		resolve.addActionListener(this);

		JPanel buttonPanel = new JPanel();
		buttonPanel.add(cancel);
		buttonPanel.add(resolve);
		panel.add(buttonPanel);

		add(panel);
		
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
			this.dispose();
		else if(o == resolve)
		{
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			Vector<Problem> selection = new Vector<Problem>();
			for(int i = 0; i < filterers.size(); i++)
				if(filterers.get(i).isSelected())
					selection.add(Problem.values()[i]);
			aml.setFlagSteps(selection);
			c = new Console();
			c.addWindowListener(this);
			console = new Thread(c);
			console.start();
			action = new Thread(this);
			action.start();
		}
	}
	
	@Override
	public void run()
	{
		try
		{
			Thread.sleep(1000);
		}
		catch (InterruptedException e)
		{
			//Do nothing
		}
		aml.filter();
		Audio.finished();
		try
		{
			Thread.sleep(1500);
		}
		catch (InterruptedException e)
		{
			//Do nothing
		}
		c.finish();
		dispose();
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
		this.dispose();
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