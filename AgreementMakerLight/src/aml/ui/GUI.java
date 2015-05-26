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
* AgreementMakerLight Graphic User Interface.                                 *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 20-05-2015                                                            *
******************************************************************************/
package aml.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;

public class GUI extends JFrame
{

//Attributes
	
	private static final long serialVersionUID = 4515937956299538275L;
	private AMLMenuBar amlMenuBar;
	private JDesktopPane desktop;
	private ResourcePanel resourcePanel;
	private MappingViewerGephi mappingViewer;
	private	AlignmentReviewer alignReviewer;
	private JTabbedPane tabbedPane;
	private JButton cancel;
	private Dimension panelMin, panelMax, viewerMin, viewerMax;
	
//Constructors
	
    public GUI()
    {
    	setTitle("AgreementMakerLight");
    	setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
    	
        amlMenuBar = new AMLMenuBar();
        setJMenuBar(amlMenuBar);

		desktop = new JDesktopPane();
		desktop.setLayout(new BorderLayout());
		
		//Get the effective screen size
		GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
		Dimension screenSize = env.getMaximumWindowBounds().getSize();
		//Get the insets (shouldn't be necessary, but just in case)
		Insets insets = this.getInsets();
		int w = insets.left + insets.right + 10;
		int h = insets.top + insets.bottom;
		//Determine width and height of the panels
		int width = screenSize.width - w;
		int panel = 105;
		int viewer = screenSize.height - h - panel - (4*amlMenuBar.getPreferredSize().height);
		//Set their dimensions
		panelMin = new Dimension(width - 20, panel);
		panelMax = new Dimension(width, panel);
		viewerMin = new Dimension(width, viewer);
		viewerMax = new Dimension(width, viewer);
		//Create the resource panel
		resourcePanel = new ResourcePanel(panelMax,panelMin);
    	//resourcePanel.setBounds(0, 0, width, panelHeight);
		desktop.add(resourcePanel,BorderLayout.NORTH);
		//Create the mapping viewer
		mappingViewer = new MappingViewerGephi(viewerMax,viewerMin);
		tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Mapping Viewer", mappingViewer);
        alignReviewer = new AlignmentReviewer(viewerMax,viewerMin);
    	tabbedPane.addTab("Alignment Reviewer",alignReviewer);
        cancel = new JButton("Cancel");
		cancel.setPreferredSize(new Dimension(70,28));

		desktop.add(tabbedPane);
		setContentPane(desktop);

		this.pack();
		setExtendedState(JFrame.MAXIMIZED_BOTH);
		setVisible(true);
    }
    
//Public Methods (to refresh GUI)

    public void refresh()
    {
    	amlMenuBar.refresh();
    	resourcePanel.refresh();
    	tabbedPane.removeAll();
    	mappingViewer = new MappingViewerGephi(viewerMax,viewerMin);
        tabbedPane.addTab("Mapping Viewer", mappingViewer);
        alignReviewer = new AlignmentReviewer(viewerMax,viewerMin);
    	tabbedPane.addTab("Alignment Reviewer",alignReviewer);
    	this.pack();
    	setExtendedState(JFrame.MAXIMIZED_BOTH);
    	setVisible(true);
    }
    
    public void refreshPanel()
    {
    	resourcePanel.refresh();
    }    
}