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
* Displays a mapping graphically, and shows detailed information about it.    *
*                                                                             *
* @author Daniel Faria, Catia Pesquita & Catarina Martins                     *
******************************************************************************/
package aml.ui;
 
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;

import org.gephi.graph.api.DirectedGraph;
import org.gephi.graph.api.Edge;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.Node;
import org.gephi.layout.plugin.force.StepDisplacement;
import org.gephi.layout.plugin.force.yifanHu.YifanHuLayout;
import org.gephi.layout.plugin.labelAdjust.LabelAdjust;
import org.gephi.preview.api.PreviewController;
import org.gephi.preview.api.PreviewModel;
import org.gephi.preview.api.PreviewProperty;
import org.gephi.preview.api.ProcessingTarget;
import org.gephi.preview.api.RenderTarget;
import org.gephi.preview.types.DependantColor;
import org.gephi.preview.types.DependantOriginalColor;
import org.gephi.preview.types.EdgeColor;
import org.gephi.project.api.ProjectController;
import org.openide.util.Lookup;

import aml.AML;
import aml.filter.RepairMap;
import aml.match.Alignment;
import aml.match.Mapping;
import aml.ontology.DataProperty;
import aml.ontology.Lexicon;
import aml.ontology.ObjectProperty;
import aml.ontology.Ontology2Match;
import aml.ontology.RelationshipMap;
import aml.ontology.URIMap;
import aml.settings.LexicalType;
import aml.settings.MappingRelation;
import aml.settings.MappingStatus;
import processing.core.PApplet;
import aml.settings.EntityType;
 

public class ViewMapping extends JDialog implements ActionListener
{
	
//Attributes
	
	//Constants
	private static final long serialVersionUID = 4516245633857479148L;
  	private static final int MAX_RETRIES = 5;
  	private static final Color BK = new Color(214,217,223);
  	
  	//Ontology and Alignment attributes
	private AML aml;
	private URIMap uris;
	private RelationshipMap rm;
	private Ontology2Match source, target;
  	private Alignment a;
	private int mapping, sourceId, targetId;
	private Mapping m;

	//Parent Container
	private Container parent;

	//Dimensions
	private int width;
	private int height;
	
	//Components
	private JTabbedPane tabbedPane;
	private PApplet mappingViewer;
	private JPanel details, conflicts;
	private Vector<JCheckBox> check;
	private Vector<Mapping> mappings;
	private Vector<JButton> mappingButtons;
	private Vector<JLabel> labels;
	private JButton reset, setCorrect, setIncorrect;
	
	//Graph components
	private GraphModel model;
  	private DirectedGraph directedGraph;
  	private HashSet<Integer> sourceNodes, targetNodes;
  	private int maxDistance;

//Constructors
  	
    public ViewMapping(Container parent, int index)
    {
        super();
        this.parent = parent;
        //Set the size
        GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
		Dimension screenSize = env.getMaximumWindowBounds().getSize();
		width = screenSize.width;
		height = screenSize.height;
     	this.setMinimumSize(new Dimension((int)(width*0.8),(int)(height*0.8)));
     	this.setPreferredSize(new Dimension((int)(width*0.8),(int)(height*0.8)));
     	this.setMaximumSize(new Dimension((int)(width*0.9),(int)(height*0.9)));
     
     	//Get the ontologies and alignment
     	aml = AML.getInstance();
		source = aml.getSource();
		target = aml.getTarget();
		uris = aml.getURIMap();
        rm = aml.getRelationshipMap();
		a = aml.getAlignment();
		mapping = index;
        m = a.get(mapping);
        sourceId = m.getSourceId();
        targetId = m.getTargetId();

        //Set the title and modality
        this.setTitle(m.toGUI());
		this.setModalityType(ModalityType.APPLICATION_MODAL);

        //Setup the Tabbed Pane
        tabbedPane = new JTabbedPane();

        //Add the graph
        buildGraph();
        if(mappingViewer != null)
        	tabbedPane.addTab("Graph View", mappingViewer);
        else
        	tabbedPane.addTab("Graph View", new JPanel());
        JLabel lab1 = new JLabel("Graph View",SwingConstants.CENTER);
        lab1.setPreferredSize(new Dimension(85, 15));
        tabbedPane.setTabComponentAt(0, lab1);
        //Add the details
        buildDetailPanel();
        tabbedPane.addTab("Details", details);
        JLabel lab2 = new JLabel("Details",SwingConstants.CENTER);
        lab2.setPreferredSize(new Dimension(85, 15));
        tabbedPane.setTabComponentAt(1, lab2);
        //Add the issues
        buildConflictPanel();
        tabbedPane.addTab("Conflicts", conflicts);
        JLabel lab3 = new JLabel("Conflicts",SwingConstants.CENTER);
        lab3.setPreferredSize(new Dimension(85, 15));
        tabbedPane.setTabComponentAt(2, lab3);

        //Set the tabbed pane as the content pane
		setContentPane(tabbedPane);
		
		//Wrap up
        this.pack();
        this.setVisible(true);
    }
    
//Public Methods
    
	@Override
	public void actionPerformed(ActionEvent e)
	{
		JButton b = (JButton)e.getSource();
		if(b == reset)
		{
			for(int i = 0; i < check.size(); i++)
			{
				if(check.get(i).isSelected())
				{
					Mapping n = mappings.get(i);
					if(n.getStatus().equals(MappingStatus.UNKNOWN))
						continue;
					int index = a.getIndex(n.getSourceId(),n.getTargetId());
					//mappings.get(i).setStatus(MappingStatus.CORRECT);
					a.get(index).setStatus(MappingStatus.UNKNOWN);
					if(parent instanceof AlignmentPanel)
						((AlignmentPanel)parent).refresh(index);
					mappingButtons.get(i).setBackground(Color.GRAY);
				}
			}
		}
		else if(b == setCorrect)
		{
			for(int i = 0; i < check.size(); i++)
			{
				if(check.get(i).isSelected())
				{
					Mapping n = mappings.get(i);
					if(n.getStatus().equals(MappingStatus.CORRECT))
						continue;
					int index = a.getIndex(n.getSourceId(),n.getTargetId());
					//mappings.get(i).setStatus(MappingStatus.CORRECT);
					a.get(index).setStatus(MappingStatus.CORRECT);
					if(parent instanceof AlignmentPanel)
						((AlignmentPanel)parent).refresh(index);
					mappingButtons.get(i).setBackground(Color.GREEN);
				}
			}
		}
		else if(b == setIncorrect)
		{
			for(int i = 0; i < check.size(); i++)
			{
				if(check.get(i).isSelected())
				{
					Mapping n = mappings.get(i);
					if(n.getStatus().equals(MappingStatus.INCORRECT))
						continue;
					int index = a.getIndex(n.getSourceId(),n.getTargetId());
					//mappings.get(i).setStatus(MappingStatus.CORRECT);
					a.get(index).setStatus(MappingStatus.INCORRECT);
					if(parent instanceof AlignmentPanel)
						((AlignmentPanel)parent).refresh(index);
					mappingButtons.get(i).setBackground(Color.RED);
				}
			}
		}
		else
		{
			int index = mappingButtons.indexOf(b);
			if(index > -1)
			{
				Mapping n = mappings.get(index);
				index = a.getIndex(n.getSourceId(),n.getTargetId());
				this.dispose();
				new ViewMapping(parent,index);
			}
		}		
	}

//Private Methods
    
    //Builds the Mapping graph using the Gephi Toolkit
	private void buildGraph()
	{
		//Initialize a project and therefore a workspace
		ProjectController pc = Lookup.getDefault().lookup(ProjectController.class);
		pc.newProject();
		//Initialize the graph-building variables
		GraphController graphController = Lookup.getDefault().lookup(GraphController.class);
		model = graphController.getModel();
		directedGraph = model.getDirectedGraph();
		//Add the starting source and target nodes to the graph
		addSourceNode(sourceId,8);
		addTargetNode(targetId,8);
		//And the mapping between them
		addMapping(sourceId,targetId);
		//Get the maximum distance
		maxDistance = aml.getMaxDistance();
		//Initialize the node sets (which don't include the starting nodes)
		sourceNodes = new HashSet<Integer>();
		targetNodes = new HashSet<Integer>();
		//Add all ancestors and descendants as per the view parameters
		if(aml.showAncestors())
		{
			addSourceAncestors(sourceId);
			addTargetAncestors(targetId);
		}
		if(aml.showDescendants())
		{
			addSourceDescendants(sourceId);
			addTargetDescendants(targetId);
		}
		//Add all additional mappings of the initial nodes
		addOtherMappings(sourceId,targetId);
		//Now find if there are any mappings between the node sets and add them
		addAllMappings();
		
		//Finally, try to render the graph
		for(int i = 0; i < MAX_RETRIES; i++)
		{
			try
			{
				//Run YifanHuLayout for 100 passes - The layout always takes the current visible view
				YifanHuLayout layout = new YifanHuLayout(null, new StepDisplacement(1f));
				layout.setGraphModel(model);
				layout.resetPropertiesValues();
				layout.setOptimalDistance(300f);
				layout.setBarnesHutTheta(0.2f);
				layout.initAlgo();
				for (int j = 0; j < 100 && layout.canAlgo(); j++)
					layout.goAlgo();
			
				//Run LabelAdjust to stop labels from overlapping
				LabelAdjust labela = new LabelAdjust(null);
				labela.resetPropertiesValues();
				labela.initAlgo();
				for (int j = 0; j < 30 && labela.canAlgo(); j++)
					labela.goAlgo();
				
				//Initialize and configure preview
				PreviewController previewController = Lookup.getDefault().lookup(PreviewController.class);
				PreviewModel previewModel = previewController.getModel();
				//Configure node labels
				previewModel.getProperties().putValue(PreviewProperty.SHOW_NODE_LABELS, true);
				previewModel.getProperties().putValue(PreviewProperty.NODE_LABEL_OUTLINE_COLOR, new DependantColor(BK));
				previewModel.getProperties().putValue(PreviewProperty.NODE_LABEL_OUTLINE_SIZE, 5f);
				previewModel.getProperties().putValue(PreviewProperty.NODE_LABEL_COLOR, new DependantOriginalColor(Color.BLACK));
				//Configure edges
				previewModel.getProperties().putValue(PreviewProperty.EDGE_CURVED, false);
				previewModel.getProperties().putValue(PreviewProperty.EDGE_OPACITY, 100);
				previewModel.getProperties().putValue(PreviewProperty.EDGE_RADIUS, 5f);
				previewModel.getProperties().putValue(PreviewProperty.EDGE_COLOR, new EdgeColor(EdgeColor.Mode.ORIGINAL));
				//Configure edge labels
				previewModel.getProperties().putValue(PreviewProperty.SHOW_EDGE_LABELS, true);
				previewModel.getProperties().putValue(PreviewProperty.EDGE_LABEL_COLOR, new DependantOriginalColor(Color.BLACK));
				previewModel.getProperties().putValue(PreviewProperty.EDGE_LABEL_OUTLINE_COLOR, new DependantColor(BK));
				previewModel.getProperties().putValue(PreviewProperty.EDGE_LABEL_OUTLINE_SIZE, 2f);
				//Configure background color
				previewModel.getProperties().putValue(PreviewProperty.BACKGROUND_COLOR, BK);
				previewController.refreshPreview();
				//Initialize the processing target and the PApplet
				ProcessingTarget target = (ProcessingTarget) previewController.getRenderTarget(RenderTarget.PROCESSING_TARGET);
				mappingViewer = target.getApplet();
				mappingViewer.init();
				//Refresh the preview and reset the zoom
				previewController.render(target);
				target.refresh();
				target.resetZoom();
				//If successful, return
				return;
			}
			catch(Exception e)
			{
				//Otherwise keep trying
				continue;
			}
		}
		//If not successful after the retry limit, set mappingViewer to null
		mappingViewer = null;
	}
	
	//Builds the details panel
	private void buildDetailPanel()
	{
		EntityType t = uris.getType(sourceId);
	
		//Setup the panels
		details = new JPanel();
		details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
		JPanel mappingPanel = new JPanel();
		mappingPanel.setLayout(new BoxLayout(mappingPanel, BoxLayout.Y_AXIS));
		JPanel sourcePanel = new JPanel();
		sourcePanel.setLayout(new BoxLayout(sourcePanel, BoxLayout.Y_AXIS));
		JPanel targetPanel = new JPanel();
		targetPanel.setLayout(new BoxLayout(targetPanel, BoxLayout.Y_AXIS));
		JPanel fillerPanel = new JPanel();

		//Initialize the mapping panel
		mappingPanel.setBorder(new TitledBorder("Mapping:"));
        JLabel type = new JLabel("Type: " + t + " Mapping");
		mappingPanel.add(type);
        JLabel sim = new JLabel("Final Similarity: " + m.getSimilarityPercent());
        mappingPanel.add(sim);
		//TODO: Add auxiliary similarity values from QualityFlagger
		if(t.equals(EntityType.CLASS))
		{
	        //For the Source Ontology
			Lexicon srcLex = source.getLexicon();
			sourcePanel.setBorder(new TitledBorder("Source Class:"));
			//Get the local name
	        JLabel localNameS = new JLabel("Local Name: " +
	        		uris.getLocalName(sourceId));
	        sourcePanel.add(localNameS);
	        //Labels
	        String lab = "Label(s): ";
	        Set<String> names = srcLex.getNames(sourceId,LexicalType.LABEL);
			for(String s : names)
				lab += s + "; ";
			if(names.size() == 0)
				lab += "N/A";
			else
				lab = lab.substring(0, lab.length()-2);
	        JLabel labelS = new JLabel(lab);
	        sourcePanel.add(labelS);
	        //Synonyms
	        names = srcLex.getNames(sourceId,LexicalType.EXACT_SYNONYM);
			if(names.size() > 0)
			{
		        lab = "Exact Synonyms(s): ";
				for(String s : names)
					lab += s + "; ";
				lab = lab.substring(0, lab.length()-2);
		        JLabel exactS = new JLabel(lab);
		        sourcePanel.add(exactS);
			}
	        names = srcLex.getNames(sourceId,LexicalType.OTHER_SYNONYM);
			if(names.size() > 0)
			{
		        lab = "Other Synonyms(s): ";
				for(String s : names)
					lab += s + "; ";
				lab = lab.substring(0, lab.length()-2);
		        JLabel otherS = new JLabel(lab);
		        sourcePanel.add(otherS);
			}
	        //Formulas
	        names = srcLex.getNames(sourceId,LexicalType.FORMULA);
			if(names.size() > 0)
			{
		        lab = "Formula(s): ";
				for(String s : names)
					lab += s + "; ";
				lab = lab.substring(0, lab.length()-2);
		        JLabel formS = new JLabel(lab);
		        sourcePanel.add(formS);
			}
			//Direct Superclasses
			Set<Integer> directSetSource = rm.getSuperClasses(sourceId,true);
			lab = "Direct Superclass(es): ";
			for(Integer i : directSetSource)
				lab += source.getName(i) + "; ";
			if(directSetSource.size() == 0)
				lab += "N/A";
			else
				lab = lab.substring(0, lab.length()-2);
			JLabel directS = new JLabel(lab);
			sourcePanel.add(directS);
			//High Level Ancestors
			Set<Integer> highSetSource = rm.getHighLevelAncestors(sourceId);
			lab = "High-Level Ancestors: ";
			for(Integer i : highSetSource)
				lab += source.getName(i) + "; ";
			if(highSetSource.size() == 0)
				lab += "N/A";
			else
				lab = lab.substring(0, lab.length()-2);
			JLabel highS = new JLabel(lab);
			sourcePanel.add(highS);
			//And Disjoints
			Set<Integer> disjointSetSource = rm.getDisjointTransitive(sourceId);
			lab = "Disjoint Classes: ";
			for(Integer i : disjointSetSource)
				lab += source.getName(i) + "; ";
			if(disjointSetSource.size() == 0)
				lab += "N/A";
			else
				lab = lab.substring(0, lab.length()-2);
			JLabel disjointsS = new JLabel(lab);
			sourcePanel.add(disjointsS);
			if(source.isObsoleteClass(sourceId))
			{
				JLabel obsS = new JLabel("Obsolete Class!");
				Font font =  obsS.getFont();
				obsS.setFont(new Font(font.getFontName(), Font.BOLD, font.getSize()));
			}
			
	        //Then do the same for the Target Ontology
			Lexicon tgtLex = target.getLexicon();
			targetPanel.setBorder(new TitledBorder("Target Class:"));
	        JLabel localNameT = new JLabel("Local Name: " +
	        		uris.getLocalName(targetId));
	        targetPanel.add(localNameT);
	        lab = "Label(s): ";
	        names = tgtLex.getNames(targetId,LexicalType.LABEL);
			for(String s : names)
				lab += s + "; ";
			if(names.size() == 0)
				lab += "N/A";
			else
				lab = lab.substring(0, lab.length()-2);
	        JLabel labelT = new JLabel(lab);
	        targetPanel.add(labelT);
	        names = tgtLex.getNames(targetId,LexicalType.EXACT_SYNONYM);
			if(names.size() > 0)
			{
		        lab = "Exact Synonyms(s): ";
				for(String s : names)
					lab += s + "; ";
				lab = lab.substring(0, lab.length()-2);
		        JLabel exactT = new JLabel(lab);
		        targetPanel.add(exactT);
			}
	        names = tgtLex.getNames(targetId,LexicalType.OTHER_SYNONYM);
			if(names.size() > 0)
			{
		        lab = "Other Synonyms(s): ";
				for(String s : names)
					lab += s + "; ";
				lab = lab.substring(0, lab.length()-2);
		        JLabel otherT = new JLabel(lab);
		        targetPanel.add(otherT);
			}
	        names = tgtLex.getNames(targetId,LexicalType.FORMULA);
			if(names.size() > 0)
			{
		        lab = "Formula(s): ";
				for(String s : names)
					lab += s + "; ";
				lab = lab.substring(0, lab.length()-2);
		        JLabel formT = new JLabel(lab);
		        targetPanel.add(formT);
			}
			Set<Integer> directSetTarget = rm.getSuperClasses(targetId,true);
			lab = "Direct Superclass(es): ";
			for(Integer i : directSetTarget)
				lab += target.getName(i) + "; ";
			if(directSetTarget.size() == 0)
				lab += "N/A";
			else
				lab = lab.substring(0, lab.length()-2);
			JLabel directT = new JLabel(lab);
			targetPanel.add(directT);
			Set<Integer> highSetTarget = rm.getHighLevelAncestors(targetId);
			lab = "High-Level Ancestors: ";
			for(Integer i : highSetTarget)
				lab += target.getName(i) + "; ";
			if(highSetTarget.size() == 0)
				lab += "N/A";
			else
				lab = lab.substring(0, lab.length()-2);
			JLabel highT = new JLabel(lab);
			targetPanel.add(highT);
			Set<Integer> disjointSetTarget = rm.getDisjointTransitive(targetId);
			lab = "Disjoint Classes: ";
			for(Integer i : disjointSetTarget)
				lab += target.getName(i) + "; ";
			if(disjointSetTarget.size() == 0)
				lab += "N/A";
			else
				lab = lab.substring(0, lab.length()-2);
			JLabel disjointsT = new JLabel(lab);
			targetPanel.add(disjointsT);
			if(target.isObsoleteClass(targetId))
			{
				JLabel obsS = new JLabel("Obsolete Class!");
				obsS.setForeground(Color.RED);
				Font font =  obsS.getFont();
				obsS.setFont(new Font(font.getFontName(), Font.BOLD, font.getSize()));
			}
		}
		else
		{
			//Get the name, domain and range for the source Ontology
			sourcePanel.setBorder(new TitledBorder("Source Property:"));
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
			//Do the same for the target property
			targetPanel.setBorder(new TitledBorder("Target Property:"));
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
		}
		//Set the sizes of the subpanels and add them to the details panel
		mappingPanel.setPreferredSize(new Dimension((int)(width*0.78),mappingPanel.getPreferredSize().height));
		mappingPanel.setMaximumSize(new Dimension((int)(width*0.78),mappingPanel.getPreferredSize().height));
		details.add(mappingPanel);
		sourcePanel.setPreferredSize(new Dimension((int)(width*0.78),sourcePanel.getPreferredSize().height));
		sourcePanel.setMaximumSize(new Dimension((int)(width*0.78),sourcePanel.getPreferredSize().height));
		details.add(sourcePanel);
		targetPanel.setPreferredSize(new Dimension((int)(width*0.78),targetPanel.getPreferredSize().height));
		targetPanel.setMaximumSize(new Dimension((int)(width*0.78),targetPanel.getPreferredSize().height));
		details.add(targetPanel);
		details.add(fillerPanel);
	}
	
	//Builds the conflict panel
	private void buildConflictPanel()
	{
		conflicts = new JPanel();
		conflicts.setLayout(new BoxLayout(conflicts, BoxLayout.Y_AXIS));
		
		//The header button panel
		setCorrect = new JButton("Set Correct");
		setCorrect.setBackground(Color.GREEN);
		setCorrect.setPreferredSize(new Dimension(140,28));
		setCorrect.addActionListener(this);
		reset = new JButton("Reset");
		reset.setBackground(Color.LIGHT_GRAY);
		reset.setPreferredSize(new Dimension(140,28));
		reset.addActionListener(this);
		setIncorrect = new JButton("Set Incorrect");
		setIncorrect.setBackground(Color.RED);
		setIncorrect.setPreferredSize(new Dimension(140,28));
		setIncorrect.addActionListener(this);
		JPanel headerPanel = new JPanel(new FlowLayout());
		headerPanel.setMaximumSize(new Dimension((int)(width*0.8),35));
		headerPanel.add(setCorrect);
		headerPanel.add(reset);
		headerPanel.add(setIncorrect);
		
		JPanel mappingPanel = new JPanel();
		mappingPanel.setLayout(new BoxLayout(mappingPanel, BoxLayout.Y_AXIS));
		check = new Vector<JCheckBox>();
		mappingButtons = new Vector<JButton>();
		mappings = new Vector<Mapping>();
		labels = new Vector<JLabel>();
		
		mappings.add(m);
		labels.add(new JLabel("[Active Mapping]"));
		for(Mapping n : a.getConflicts(m))
		{
			mappings.add(n);
			labels.add(new JLabel("[Cardinality Conflict]"));
		}
		RepairMap rMap = aml.getRepairMap();
		if(rMap != null)
		{
			for(Mapping n : rMap.getConflictMappings(a.getIndex(sourceId, targetId)))
			{
				if(!mappings.contains(n))
				{
					mappings.add(n);
					labels.add(new JLabel("[Coherence Conflict]"));
				}
				else
				{
					labels.get(mappings.indexOf(n)).setText("[Cardinality & Coherence Conflict]");
				}
			}
		}
		for(int i = 0; i < mappings.size(); i++)
		{
			JCheckBox c = new JCheckBox(""); 
			check.add(c);
			JButton b = new MappingButton(mappings.get(i));
			if(mappings.get(i).equals(m))
				b.setEnabled(false);
			mappingButtons.add(b);
			b.addActionListener(this);
			JPanel subPanel = new JPanel(new BorderLayout());
			subPanel.add(c,BorderLayout.LINE_START);
			JPanel subSubPanel = new JPanel(new BorderLayout());
			subSubPanel.add(b,BorderLayout.LINE_START);
			subSubPanel.add(labels.get(i), BorderLayout.CENTER);
			subPanel.add(subSubPanel, BorderLayout.CENTER);
			subPanel.setMaximumSize(new Dimension(subPanel.getMaximumSize().width,29));
			subPanel.setPreferredSize(new Dimension(subPanel.getPreferredSize().width,29));
			mappingPanel.add(subPanel);
		}		
		JPanel filler = new JPanel();
		mappingPanel.add(filler);
		JScrollPane scrollPane = new JScrollPane(mappingPanel);
		conflicts.add(scrollPane);
	}

//Auxiliary Methods for buildGraph()

	private void addAllMappings()
	{
		if(a == null)
			return;
		for(int i : sourceNodes)
			for(int j : targetNodes)
				if(a.containsMapping(i, j))
					addMapping(i, j);
	}
	
	private void addMapping(int sId, int tId)
	{
		Node n1 = directedGraph.getNode("NS" + sId);
		Node n2 = directedGraph.getNode("NT" + tId);
		if(directedGraph.getEdge(n1,n2) != null)
			return;
		MappingRelation r = a.getRelationship(sId,tId);
		if(!r.equals(MappingRelation.SUPERCLASS))
		{
			Edge e1 = model.factory().newEdge(n1, n2, 3, true);
			if(sId == sourceId && tId == targetId)
				e1.getEdgeData().setColor(1, 1, 0);
			else
				e1.getEdgeData().setColor(1, 1, 1);
			e1.getEdgeData().setLabel(a.getSimilarityPercent(sId,tId));
			directedGraph.addEdge(e1);
		}
		if(!r.equals(MappingRelation.SUBCLASS))
		{
			Edge e2 = model.factory().newEdge(n2, n1, 3, true);
			if(sId == sourceId && tId == targetId)
				e2.getEdgeData().setColor(1, 1, 0);
			else
				e2.getEdgeData().setColor(1, 1, 1);
			if(r.equals(MappingRelation.SUPERCLASS))
				e2.getEdgeData().setLabel(a.getSimilarityPercent(sId,tId));
			directedGraph.addEdge(e2);
		}
	}
	
	private void addOtherMappings(int sId, int tId)
	{
		Set<Integer> sourceMappings = a.getSourceMappings(sId);
		targetNodes.addAll(sourceMappings);
		for(Integer i : sourceMappings)
		{
			if(i == tId)
				continue;
			addTargetNode(i,6);
			addMapping(sId,i);
			if(AML.getInstance().showAncestors())
				addTargetAncestors(i);
			if(AML.getInstance().showDescendants())
				addTargetDescendants(i);
		}
		Set<Integer> targetMappings = a.getTargetMappings(tId);
		sourceNodes.addAll(targetMappings);
		for(Integer i : targetMappings)
		{
			if(i == sId)
				continue;
			addSourceNode(i,6);
			addMapping(i,tId);
			if(AML.getInstance().showAncestors())
				addSourceAncestors(i);
			if(AML.getInstance().showDescendants())
				addSourceDescendants(i);
		}
	}
	
	private void addSourceAncestors(int id)
	{
		HashSet<Integer> ancestors = new HashSet<Integer>();
		ancestors.add(id);
		HashSet<Integer> descendants;
		for(int i = 0; i < maxDistance; i++)
		{
			descendants = new HashSet<Integer>(ancestors);
			ancestors = new HashSet<Integer>();
			for(int j : descendants)
			{
				Set<Integer> parents = rm.getParents(j);
				for(int k : parents)
				{
					if(directedGraph.getNode("NS" + k) == null)
						addSourceNode(k, 6);
					addSourceEdge(j, k);
				}
				ancestors.addAll(parents);
			}
			sourceNodes.addAll(ancestors);
		}
	}
	
	private void addSourceDescendants(int id)
	{
		HashSet<Integer> descendants = new HashSet<Integer>();
		descendants.add(id);
		HashSet<Integer> ancestors;
		for(int i = 0; i < maxDistance; i++)
		{
			ancestors = new HashSet<Integer>(descendants);
			descendants = new HashSet<Integer>();
			for(int j : ancestors)
			{
				Set<Integer> children = rm.getChildren(j);
				for(int k : children)
				{
					if(directedGraph.getNode("NS" + k) == null)
						addSourceNode(k, 6);
					addSourceEdge(k, j);
				}
				descendants.addAll(children);
			}
			sourceNodes.addAll(descendants);
		}
	}

	private void addSourceEdge(int child, int parent)
	{
		Node c = directedGraph.getNode("NS" + child);
		Node p = directedGraph.getNode("NS" + parent);
		Edge e = model.factory().newEdge(c, p, 3, true);
		e.getEdgeData().setColor(1, 0, 0);
		int prop = rm.getRelationship(child, parent).getProperty();
		if(prop > -1)
			e.getEdgeData().setLabel(source.getName(prop));
		directedGraph.addEdge(e);
	}
	
	private void addSourceNode(int id, int size)
	{
		Node n = model.factory().newNode("NS" + id);
		n.getNodeData().setSize(3);
		n.getNodeData().setLabel(source.getName(id));
		n.getNodeData().setColor(1, 0, 0);
		n.getNodeData().setSize(size);
		directedGraph.addNode(n);
	}
	
	private void addTargetAncestors(int id)
	{
		HashSet<Integer> ancestors = new HashSet<Integer>();
		ancestors.add(id);
		HashSet<Integer> descendants;
		for(int i = 0; i < maxDistance; i++)
		{
			descendants = new HashSet<Integer>(ancestors);
			ancestors = new HashSet<Integer>();
			for(int j : descendants)
			{
				Set<Integer> parents = rm.getParents(j);
				for(int k : parents)
				{
					if(directedGraph.getNode("NT" + k) == null)
						addTargetNode(k, 6);
					addTargetEdge(j, k);
				}
				ancestors.addAll(parents);
			}
			targetNodes.addAll(ancestors);
		}
	}
	
	private void addTargetDescendants(int id)
	{
		HashSet<Integer> descendants = new HashSet<Integer>();
		descendants.add(id);
		HashSet<Integer> ancestors;
		for(int i = 0; i < maxDistance; i++)
		{
			ancestors = new HashSet<Integer>(descendants);
			descendants = new HashSet<Integer>();
			for(int j : ancestors)
			{
				Set<Integer> children = rm.getChildren(j);
				for(int k : children)
				{
					if(directedGraph.getNode("NT" + k) == null)
						addTargetNode(k, 6);
					addTargetEdge(k, j);
				}
				descendants.addAll(children);
			}
			targetNodes.addAll(descendants);
		}
	}
	
	private void addTargetEdge(int child, int parent)
	{
		Node c = directedGraph.getNode("NT" + child);
		Node p = directedGraph.getNode("NT" + parent);
		Edge e = model.factory().newEdge(c, p, 3, true);
		e.getEdgeData().setColor(0, 0, 1);
		int prop = rm.getRelationship(child, parent).getProperty();
		if(prop > -1)
			e.getEdgeData().setLabel(target.getName(prop));
		directedGraph.addEdge(e);
	}
	
	private void addTargetNode(int id, int size)
	{
		Node n = model.factory().newNode("NT" + id);
		n.getNodeData().setSize(3);
		n.getNodeData().setLabel(target.getName(id));
		n.getNodeData().setColor(0, 0, 1);
		n.getNodeData().setSize(size);
		directedGraph.addNode(n);
	}
}