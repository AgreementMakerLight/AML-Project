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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;
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
import aml.filter.QualityFlagger;
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
import aml.util.Table2Set;
import processing.core.PApplet;
import aml.settings.EntityType;
 

public class ViewMapping extends JDialog implements ActionListener
{
	
//Attributes
	
	//Constants
	private static final long serialVersionUID = 4516245633857479148L;
  	private static final int MAX_RETRIES = 5;
  	
  	//Ontology and Alignment attributes
	private AML aml;
	private URIMap uris;
	private RelationshipMap rm;
	private Ontology2Match source, target;
  	private Alignment a;
	private int mapping, sourceId, targetId;
	private Mapping m;
	private EntityType t;

	//Dimensions
	private int width;
	private int height;
	
	//Components
	private JMenuBar menu;
	private JMenu view;
	private JMenuItem next, previous, options, redraw;
	private JTabbedPane tabbedPane;
	private PApplet mappingViewer;
	private JPanel details, conflicts, sourcePanel, targetPanel;
	private Vector<JCheckBox> check;
	private Vector<Mapping> mappings;
	private Vector<MappingButton> mappingButtons;
	private Vector<JLabel> labels;
	private JButton reset, setCorrect, setIncorrect;
	
	//Graph components
	private GraphModel model;
  	private DirectedGraph directedGraph;
  	private HashSet<Integer> nodes;
  	private Table2Set<Integer,Integer> edges;
  	private int classDistance, individualDistance;
  	private float[] sourceColor, targetColor;

//Constructors
  	
    public ViewMapping()
    {
        super();
        //Set the size & colors
        GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
		Dimension screenSize = env.getMaximumWindowBounds().getSize();
		width = screenSize.width;
		height = screenSize.height;
     	this.setMinimumSize(new Dimension((int)(width*0.9),(int)(height*0.9)));
     	this.setPreferredSize(new Dimension((int)(width*0.9),(int)(height*0.9)));
		sourceColor = new float[3];
		AMLColor.BLUE.getRGBColorComponents(sourceColor);
		targetColor = new float[3];
		AMLColor.BROWN.getRGBColorComponents(targetColor);
		
     	//Get the ontologies and alignment
     	aml = AML.getInstance();
		source = aml.getSource();
		target = aml.getTarget();
		uris = aml.getURIMap();
        rm = aml.getRelationshipMap();
        
        //Setup the menu bar
        menu = new JMenuBar();
        view = new JMenu("View");
        next = new JMenuItem("Next Mapping");
        next.addActionListener(this);
        view.add(next);
        previous = new JMenuItem("Previous Mapping");
        previous.addActionListener(this);
        view.add(previous);
        view.addSeparator();
        redraw = new JMenuItem("Refresh");
        redraw.addActionListener(this);
        view.add(redraw);
        view.addSeparator();
        options = new JMenuItem("Graph Options");
        options.addActionListener(this);
        view.add(options);
        menu.add(view);
        this.setJMenuBar(menu);
        
        refresh();
    }
    
//Public Methods
    
	@Override
	public void actionPerformed(ActionEvent e)
	{
		Object b = e.getSource();
		if(b == redraw)
		{
			this.refresh();			
		}
		else if(b == next)
		{
			mapping++;
			if(mapping >= a.size())
				aml.goTo(0);
			else
				aml.goTo(mapping);
			this.refresh();			
		}
		else if(b == previous)
		{
			mapping--;
			if(mapping < 0)
				aml.goTo(a.size() - 1);
			else
				aml.goTo(mapping);
			this.refresh();
		}
		else if(b == options)
		{
			String lang = aml.getLabelLanguage();
			boolean anc = aml.showAncestors();
			boolean des = aml.showDescendants();
			int cls = aml.getClassDistance();
			int ind = aml.getIndividualDistance();
			new ViewOptions();
			if(!lang.equals(aml.getLabelLanguage()) ||
					anc != aml.showAncestors() ||
					des != aml.showDescendants() ||
					cls != aml.getClassDistance() ||
					ind != aml.getIndividualDistance())
				this.refresh();
		}
		else if(b == reset)
		{
			for(int i = 0; i < check.size(); i++)
			{
				if(check.get(i).isSelected())
				{
					Mapping n = mappings.get(i);
					check.get(i).setSelected(false);
					if(n.getStatus().equals(MappingStatus.UNKNOWN))
						continue;
					int index = a.getIndex(n.getSourceId(),n.getTargetId());
					a.get(index).setStatus(MappingStatus.UNKNOWN);
					aml.refreshMapping(index);
					mappingButtons.get(i).refresh();
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
					check.get(i).setSelected(false);
					if(n.getStatus().equals(MappingStatus.CORRECT))
						continue;
					int index = a.getIndex(n.getSourceId(),n.getTargetId());
					a.get(index).setStatus(MappingStatus.CORRECT);
					aml.refreshMapping(index);
					mappingButtons.get(i).refresh();
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
					check.get(i).setSelected(false);
					if(n.getStatus().equals(MappingStatus.INCORRECT))
						continue;
					int index = a.getIndex(n.getSourceId(),n.getTargetId());
					a.get(index).setStatus(MappingStatus.INCORRECT);
					aml.refreshMapping(index);
					mappingButtons.get(i).refresh();
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
				aml.goTo(index);
				refresh();
			}
		}		
	}

//Private Methods
    
	private void refresh()
	{
		a = aml.getAlignment();
		mapping = aml.getActiveMapping();
        m = a.get(mapping);
        sourceId = m.getSourceId();
        targetId = m.getTargetId();
        t = uris.getType(sourceId);
        
        //Set the title and modality
        this.setTitle(m.toGUI());
		this.setModalityType(ModalityType.APPLICATION_MODAL);

        //Setup the Tabbed Pane
        tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(AMLColor.WHITE);

        //Add the graph
        buildGraph();
        if(mappingViewer != null)
        {
        	tabbedPane.addTab("Graph View", mappingViewer);
        	mappingViewer.mouseClicked();
        }
        else
        	tabbedPane.addTab("Graph View", new JPanel());
        JLabel lab1 = new JLabel("Graph View",SwingConstants.CENTER);
        lab1.setPreferredSize(new Dimension(120, 15));
        tabbedPane.setTabComponentAt(0, lab1);
        //Add the details
        buildDetailPanel();
        tabbedPane.addTab("Details", details);
        JLabel lab2 = new JLabel("Details",SwingConstants.CENTER);
        lab2.setPreferredSize(new Dimension(120, 15));
        tabbedPane.setTabComponentAt(1, lab2);
        //Add the issues
        buildConflictPanel();
        tabbedPane.addTab("Status & Conflicts", conflicts);
        JLabel lab3 = new JLabel("Status & Conflicts",SwingConstants.CENTER);
        lab3.setPreferredSize(new Dimension(120, 15));
        tabbedPane.setTabComponentAt(2, lab3);
        
        //Set the tabbed pane as the content pane
		setContentPane(tabbedPane);
		
		//Wrap up
		this.pack();
		GraphicsEnvironment g = GraphicsEnvironment.getLocalGraphicsEnvironment();
		int left = g.getCenterPoint().x - (int)(this.getPreferredSize().width / 2);
		this.setLocation(left, 0);
        this.setVisible(true);
	}
	
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
		//Initialize the node sets (which don't include the starting nodes)
		nodes = new HashSet<Integer>();
		edges = new Table2Set<Integer,Integer>();
		//Get the maximum distance
		classDistance = aml.getClassDistance();
		individualDistance = aml.getIndividualDistance();
		
		//Add the starting source and target nodes to the graph
		addNode(sourceId,8);
		addNode(targetId,8);
		//And the mapping between them
		addMapping(sourceId,targetId);
		//Add all ancestors and descendants as per the view parameters
		if(aml.showAncestors())
		{
			addAncestors(sourceId);
			addAncestors(targetId);
		}
		if(aml.showDescendants())
		{
			addDescendants(sourceId);
			addDescendants(targetId);
		}
		if(t.equals(EntityType.INDIVIDUAL))
			addInstancedClasses();
		else if(t.equals(EntityType.CLASS))
		{
			addEquiv(sourceId);
			addEquiv(targetId);
		}
		//Add all additional mappings of the initial nodes
		addOtherMappings(sourceId,targetId);
		//Now find if there are any mappings between the node sets and add them
		addAllMappings();
		//Finally, render the graph
		renderGraph();
	}
	
	//Builds the details panel
	private void buildDetailPanel()
	{
		//Setup the panels
		details = new JPanel();
		details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
		JPanel topFiller = new JPanel();
		topFiller.setPreferredSize(new Dimension(topFiller.getPreferredSize().width,10));
		topFiller.setMaximumSize(new Dimension(topFiller.getMaximumSize().width,10));
		details.add(topFiller);
		
		if(t.equals(EntityType.CLASS))
		{
			sourcePanel = buildClassDetailPanel(sourceId);
			sourcePanel.setBorder(new TitledBorder("Source Class:"));
			targetPanel = buildClassDetailPanel(targetId);
			targetPanel.setBorder(new TitledBorder("Target Class:"));
		}
		else if(t.equals(EntityType.INDIVIDUAL))
		{
			sourcePanel = buildIndivDetailPanel(sourceId);
			sourcePanel.setBorder(new TitledBorder("Source Individual:"));
			targetPanel = buildIndivDetailPanel(targetId);
			targetPanel.setBorder(new TitledBorder("Target Individual:"));
		}
		else
		{
			sourcePanel = buildPropDetailPanel(sourceId);
			sourcePanel.setBorder(new TitledBorder("Source Property:"));
			targetPanel = buildPropDetailPanel(targetId);
			targetPanel.setBorder(new TitledBorder("Target Property:"));
		}
		
		//Set the sizes of the subpanels and add them to the details panel
		sourcePanel.setPreferredSize(new Dimension((int)(width*0.85),sourcePanel.getPreferredSize().height));
		sourcePanel.setMaximumSize(new Dimension((int)(width*0.85),sourcePanel.getPreferredSize().height));
		details.add(sourcePanel);
		JPanel midFiller1 = new JPanel();
		midFiller1.setPreferredSize(new Dimension(midFiller1.getPreferredSize().width,10));
		midFiller1.setMaximumSize(new Dimension(midFiller1.getMaximumSize().width,10));
		details.add(midFiller1);
		targetPanel.setPreferredSize(new Dimension((int)(width*0.85),targetPanel.getPreferredSize().height));
		targetPanel.setMaximumSize(new Dimension((int)(width*0.85),targetPanel.getPreferredSize().height));
		details.add(targetPanel);
		JPanel midFiller2 = new JPanel();
		midFiller2.setPreferredSize(new Dimension(midFiller2.getPreferredSize().width,10));
		midFiller2.setMaximumSize(new Dimension(midFiller2.getMaximumSize().width,10));
		details.add(midFiller2);
		//Initialize and construct the mapping panel
		JPanel mappingPanel = new JPanel();
		mappingPanel.setLayout(new BoxLayout(mappingPanel, BoxLayout.Y_AXIS));
		mappingPanel.setBorder(new TitledBorder("Mapping:"));
        JLabel type = new JLabel("<html>Type: <i>" + t + " Mapping</i></html>");
		mappingPanel.add(type);
        JLabel sim = new JLabel("<html>Final Similarity: <i>" + m.getSimilarityPercent() + "</i></html>");
        mappingPanel.add(sim);
        if(t.equals(EntityType.CLASS))
        {
	        QualityFlagger qf = aml.getQualityFlagger();
	        if(qf != null)
	        {
	        	Vector<String> labels = qf.getLabels();
	        	for(int i = 0; i < labels.size(); i++)
	        	{
	        		JLabel simQ = new JLabel("<html>" + labels.get(i) + "<i>" +
	        				qf.getSimilarityPercent(sourceId,targetId,i) + "</i></html>");
	        		mappingPanel.add(simQ);
	        	}
	        }
        }
		//Set its size and add it to the details panel
		mappingPanel.setPreferredSize(new Dimension((int)(width*0.85),mappingPanel.getPreferredSize().height));
		mappingPanel.setMaximumSize(new Dimension((int)(width*0.85),mappingPanel.getPreferredSize().height));
		details.add(mappingPanel);
		JPanel bottomFiller = new JPanel();
		details.add(bottomFiller);
	}
	
	//Builds the conflict panel
	private void buildConflictPanel()
	{
		conflicts = new JPanel();
		conflicts.setLayout(new BoxLayout(conflicts, BoxLayout.Y_AXIS));
		
		//The header button panel
		setCorrect = new JButton("Set Correct");
		setCorrect.setBackground(AMLColor.GREEN);
		setCorrect.setPreferredSize(new Dimension(110,28));
		setCorrect.addActionListener(this);
		reset = new JButton("Reset");
		reset.setBackground(AMLColor.GRAY);
		reset.setPreferredSize(new Dimension(110,28));
		reset.addActionListener(this);
		setIncorrect = new JButton("Set Incorrect");
		setIncorrect.setBackground(AMLColor.RED);
		setIncorrect.setPreferredSize(new Dimension(110,28));
		setIncorrect.addActionListener(this);
		JPanel sub = new JPanel();
		sub.setBorder(new BevelBorder(1));
		sub.add(setCorrect);
		sub.add(reset);
		sub.add(setIncorrect);
		JPanel headerPanel = new JPanel(new FlowLayout());
		headerPanel.setMaximumSize(new Dimension((int)(width*0.9),40));
		headerPanel.add(sub);
		conflicts.add(headerPanel);
		
		JPanel mappingPanel = new JPanel();
		mappingPanel.setLayout(new BoxLayout(mappingPanel, BoxLayout.Y_AXIS));
		check = new Vector<JCheckBox>();
		mappingButtons = new Vector<MappingButton>();
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
			for(Mapping n : rMap.getConflictMappings(m))
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
			MappingButton b = new MappingButton(mappings.get(i));
			mappingButtons.add(b);
			if(!mappings.get(i).equals(m))
				b.addActionListener(this);
			JPanel subPanel = new JPanel(new BorderLayout());
			subPanel.add(c,BorderLayout.LINE_START);
			JPanel subSubPanel = new JPanel(new BorderLayout());
			subSubPanel.add(b,BorderLayout.LINE_START);
			subSubPanel.add(labels.get(i), BorderLayout.CENTER);
			subPanel.add(subSubPanel, BorderLayout.CENTER);
			subPanel.setMaximumSize(new Dimension(subPanel.getMaximumSize().width,28));
			subPanel.setPreferredSize(new Dimension(subPanel.getPreferredSize().width,28));
			mappingPanel.add(subPanel);
		}		
		JPanel filler = new JPanel();
		mappingPanel.add(filler);
		JScrollPane scrollPane = new JScrollPane(mappingPanel);
		scrollPane.setBorder(new BevelBorder(1));
		conflicts.add(scrollPane);
	}

//Auxiliary Methods for buildGraph()
	
	//Adds all mappings between listed nodes to the graph
	private void addAllMappings()
	{
		if(a == null)
			return;
		for(Mapping m : a)
			if(nodes.contains(m.getSourceId()) && nodes.contains(m.getTargetId()))
				addMapping(m.getSourceId(), m.getTargetId());
	}
	
	//Adds all ancestors of the given entity to the graph
	private void addAncestors(int id)
	{
		HashSet<Integer> ancestors = new HashSet<Integer>();
		ancestors.add(id);
		HashSet<Integer> descendants;
		if(t.equals(EntityType.CLASS))
		{
			for(int i = 0; i < classDistance; i++)
			{
				descendants = new HashSet<Integer>(ancestors);
				ancestors = new HashSet<Integer>();
				for(int j : descendants)
				{
					Set<Integer> parents = rm.getParents(j);
					for(int k : parents)
					{
						if(directedGraph.getNode("" + k) == null)
							addNode(k, 6);
						if(!edges.contains(j,k) && !edges.contains(k,j))
							addEdge(j, k, rm.getRelationship(j, k).getProperty());
					}
					ancestors.addAll(parents);
				}
				nodes.addAll(ancestors);
			}
		}
		else if(t.equals(EntityType.INDIVIDUAL))
		{
			for(int i = 0; i < individualDistance; i++)
			{
				descendants = new HashSet<Integer>(ancestors);
				ancestors = new HashSet<Integer>();
				for(int j : descendants)
				{
					for(int p : rm.getIndividualProperties(j))
					{
						Set<Integer> parents = rm.getParentIndividuals(j, p);
						for(int k : parents)
						{
							if(directedGraph.getNode("" + k) == null)
								addNode(k, 6);
							if(!edges.contains(j,k) && !edges.contains(k,j))
								addEdge(j, k, p);
						}
						ancestors.addAll(parents);
					}
				}
				nodes.addAll(ancestors);
			}
		}
	}
	
	//Adds all descendants of the given entity to the graph
	private void addDescendants(int id)
	{
		HashSet<Integer> descendants = new HashSet<Integer>();
		descendants.add(id);
		HashSet<Integer> ancestors;
		if(t.equals(EntityType.CLASS))
		{
			for(int i = 0; i < classDistance; i++)
			{
				ancestors = new HashSet<Integer>(descendants);
				descendants = new HashSet<Integer>();
				for(int j : ancestors)
				{
					Set<Integer> children = rm.getChildren(j);
					for(int k : children)
					{
						if(directedGraph.getNode("" + k) == null)
							addNode(k, 6);
						if(!edges.contains(j,k) && !edges.contains(k,j))
							addEdge(k, j, rm.getRelationship(k, j).getProperty());
					}
					descendants.addAll(children);
				}
				nodes.addAll(descendants);
			}
		}
		else if(t.equals(EntityType.INDIVIDUAL))
		{
			for(int i = 0; i < individualDistance; i++)
			{
				ancestors = new HashSet<Integer>(descendants);
				descendants = new HashSet<Integer>();
				for(int j : ancestors)
				{
					for(int p : rm.getIndividualRangeProperties(j))
					{
						Set<Integer> children = rm.getChildrenIndividuals(j, p);
						for(int k : children)
						{
							if(directedGraph.getNode("" + k) == null)
								addNode(k, 6);
							if(!edges.contains(j,k) && !edges.contains(k,j))
								addEdge(k, j, p);
						}
						descendants.addAll(children);
					}
				}
				nodes.addAll(descendants);
			}
		}
	}
	
	//Adds an anonymous edge between two entities to the graph (for subclass relations)
	private void addEdge(int child, int parent)
	{
		edges.add(child, parent);
		Node c = directedGraph.getNode("" + child);
		Node p = directedGraph.getNode("" + parent);
		Edge e = model.factory().newEdge(c, p, 3, true);
		if(source.contains(child) && !target.contains(child))
			e.getEdgeData().setColor(sourceColor[0], sourceColor[1], sourceColor[2]);
		else if(!source.contains(child) && target.contains(child))
			e.getEdgeData().setColor(targetColor[0], targetColor[1], targetColor[2]);
		directedGraph.addEdge(e);
	}
	
	//Adds an edge between two entities to the graph with the given property's label
	private void addEdge(int child, int parent, int prop)
	{
		edges.add(child, parent);
		Node c = directedGraph.getNode("" + child);
		Node p = directedGraph.getNode("" + parent);
		Edge e = model.factory().newEdge(c, p, 3, true);
		if(source.contains(child))
		{
			if(prop > -1)
				e.getEdgeData().setLabel(source.getName(prop));
			if(!target.contains(child))
				e.getEdgeData().setColor(sourceColor[0], sourceColor[1], sourceColor[2]);
		}
		else if(target.contains(child))
		{
			if(prop > -1)
				e.getEdgeData().setLabel(target.getName(prop));
			e.getEdgeData().setColor(targetColor[0], targetColor[1], targetColor[2]);
		}
		directedGraph.addEdge(e);
		if(rm.isSymmetric(prop) || (prop == -1 && rm.getDistance(child, parent) == 0))
		{
			Edge f = model.factory().newEdge(p, c, 3, true);
			f.getEdgeData().setColor(sourceColor[0], sourceColor[1], sourceColor[2]);
			directedGraph.addEdge(f);
		}
	}
	
	//Adds an edge between two entities to the graph with the given label
	private void addEdge(int child, int parent, String label)
	{
		edges.add(child, parent);
		Node c = directedGraph.getNode("" + child);
		Node p = directedGraph.getNode("" + parent);
		Edge e = model.factory().newEdge(c, p, 3, true);
		if(source.contains(child) && !target.contains(child))
			e.getEdgeData().setColor(sourceColor[0], sourceColor[1], sourceColor[2]);
		else if(!source.contains(child) && target.contains(child))
			e.getEdgeData().setColor(targetColor[0], targetColor[1], targetColor[2]);
		e.getEdgeData().setLabel(label);
		directedGraph.addEdge(e);
	}
	
	//Adds all equivalent classes of the given class to the graph
	private void addEquiv(int id)
	{
		Set<Integer> eq = rm.getEquivalences(id);
		for(int i : eq)
		{
			if(directedGraph.getNode("" + i) == null)
				addNode(i, 6);
			addEdge(i, id);
		}
		nodes.addAll(eq);
	}
	
	//Adds all classes instanced by the listed individuals to the graph
	//plus their ancestors up to the classDistance limit
	private void addInstancedClasses()
	{
		Set<Integer> classes = new HashSet<Integer>();
		Set<Integer> individuals = new HashSet<Integer>(nodes);
		for(int i : individuals)
		{
			for(int c : rm.getIndividualClasses(i))
			{
				nodes.add(c);
				classes.add(c);
				if(directedGraph.getNode("" + c) == null)
					addNode(c,10);
				if(!edges.contains(i,c) && !edges.contains(c,i))
					addEdge(i, c, "instanceOf");
			}
		}
		HashSet<Integer> ancestors = new HashSet<Integer>(classes);
		HashSet<Integer> descendants;
		for(int i = 0; i < classDistance; i++)
		{
			descendants = new HashSet<Integer>(ancestors);
			ancestors = new HashSet<Integer>();
			for(int j : descendants)
			{
				Set<Integer> parents = rm.getParents(j);
				for(int k : parents)
				{
					if(directedGraph.getNode("" + k) == null)
						addNode(k,10);
					if(!edges.contains(j,k) && !edges.contains(k,j))
						addEdge(j, k);
				}
				ancestors.addAll(parents);
			}
			nodes.addAll(ancestors);
		}
	}
	
	//Adds a mapping between two classes to the graph
	private void addMapping(int sId, int tId)
	{
		Node n1 = directedGraph.getNode("" + sId);
		Node n2 = directedGraph.getNode("" + tId);
		if(directedGraph.getEdge(n1,n2) != null)
			return;
		MappingRelation r = a.getRelationship(sId,tId);
		MappingStatus s = a.get(sId,tId).getStatus();
		float[] edgeColor = new float[3];
		if(s.equals(MappingStatus.CORRECT))
			edgeColor = Color.GREEN.getRGBColorComponents(edgeColor);
		else if(s.equals(MappingStatus.INCORRECT))
			edgeColor = Color.RED.getRGBColorComponents(edgeColor);
		else if(s.equals(MappingStatus.FLAGGED))
			edgeColor = Color.ORANGE.getRGBColorComponents(edgeColor);
		else
			edgeColor = Color.GRAY.getRGBColorComponents(edgeColor);
		if(!r.equals(MappingRelation.SUPERCLASS))
		{
			Edge e1 = model.factory().newEdge(n1, n2, 3, true);
			e1.getEdgeData().setColor(edgeColor[0], edgeColor[1], edgeColor[2]);
			e1.getEdgeData().setLabel(a.getSimilarityPercent(sId,tId));
			directedGraph.addEdge(e1);
		}
		if(!r.equals(MappingRelation.SUBCLASS))
		{
			Edge e2 = model.factory().newEdge(n2, n1, 3, true);
			e2.getEdgeData().setColor(edgeColor[0], edgeColor[1], edgeColor[2]);
			if(r.equals(MappingRelation.SUPERCLASS))
				e2.getEdgeData().setLabel(a.getSimilarityPercent(sId,tId));
			directedGraph.addEdge(e2);
		}
	}
	
	//Adds a node to the graph
	private void addNode(int id, int size)
	{
		Node n = model.factory().newNode("" + id);
		if(source.contains(id))
		{
			n.getNodeData().setLabel(source.getName(id));
			if(!target.contains(id))
				n.getNodeData().setColor(sourceColor[0], sourceColor[1], sourceColor[2]);
		}
		else if(target.contains(id))
		{
			n.getNodeData().setLabel(target.getName(id));
			n.getNodeData().setColor(targetColor[0], targetColor[1], targetColor[2]);			
		}
		n.getNodeData().setSize(size);
		directedGraph.addNode(n);
	}
	
	//Adds other entities mapped to the entities in the central mapping
	private void addOtherMappings(int sId, int tId)
	{
		Set<Integer> sourceMappings = a.getSourceMappings(sId);
		for(Integer i : sourceMappings)
		{
			if(i == tId)
				continue;
			if(!nodes.contains(i))
			{
				nodes.add(i);
				addNode(i,6);
			}
			addMapping(sId,i);
			if(aml.showAncestors())
				addAncestors(i);
			if(aml.showDescendants())
				addDescendants(i);
		}
		Set<Integer> targetMappings = a.getTargetMappings(tId);
		for(Integer i : targetMappings)
		{
			if(i == sId)
				continue;
			if(!nodes.contains(i))
			{
				nodes.add(i);
				addNode(i,6);
			}
			addMapping(i,tId);
			if(AML.getInstance().showAncestors())
				addAncestors(i);
			if(AML.getInstance().showDescendants())
				addDescendants(i);
		}
	}
	
	//Graph rendering procedure with placement heuristic
	private void renderGraph()
	{
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
				previewModel.getProperties().putValue(PreviewProperty.NODE_LABEL_OUTLINE_COLOR, new DependantColor(AMLColor.WHITE));
				previewModel.getProperties().putValue(PreviewProperty.NODE_LABEL_OUTLINE_SIZE, 5f);
				previewModel.getProperties().putValue(PreviewProperty.NODE_LABEL_COLOR, new DependantOriginalColor(AMLColor.BLACK));
				//Configure edges
				previewModel.getProperties().putValue(PreviewProperty.EDGE_CURVED, false);
				previewModel.getProperties().putValue(PreviewProperty.EDGE_OPACITY, 100);
				previewModel.getProperties().putValue(PreviewProperty.EDGE_RADIUS, 5f);
				previewModel.getProperties().putValue(PreviewProperty.EDGE_COLOR, new EdgeColor(EdgeColor.Mode.ORIGINAL));
				//Configure edge labels
				previewModel.getProperties().putValue(PreviewProperty.SHOW_EDGE_LABELS, true);
				previewModel.getProperties().putValue(PreviewProperty.EDGE_LABEL_COLOR, new DependantOriginalColor(AMLColor.DARK_GRAY));
				previewModel.getProperties().putValue(PreviewProperty.EDGE_LABEL_OUTLINE_COLOR, new DependantColor(AMLColor.WHITE));
				previewModel.getProperties().putValue(PreviewProperty.EDGE_LABEL_OUTLINE_SIZE, 2f);
				//Configure background color
				previewModel.getProperties().putValue(PreviewProperty.BACKGROUND_COLOR, AMLColor.WHITE);
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
	
//Auxiliary Methods for buildDetailPanel()
	
	private JPanel buildClassDetailPanel(int id)
	{
		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

		Ontology2Match o;
		if(source.contains(id))
			o = source;
		else
			o = target;
		Lexicon lex = o.getLexicon();
		
		//Get the local name
        JLabel localName = new JLabel("<html>Local Name: <i>" +
        		uris.getLocalName(id) + "</i></html>");
        p.add(localName);
        //Labels
        String lab = "<html>Label(s): ";
        Set<String> names = lex.getNames(id,LexicalType.LABEL);
		for(String s : names)
			lab += "<i>" + s + "</i>; ";
		if(names.size() == 0)
			lab += "N/A</html>";
		else
			lab = lab.substring(0, lab.length()-2) + "</html>";
        JLabel label = new JLabel(lab);
        p.add(label);
        //Synonyms
        names = lex.getNames(id,LexicalType.EXACT_SYNONYM);
		if(names.size() > 0)
		{
	        lab = "<html>Exact Synonyms(s): ";
			for(String s : names)
				lab += "<i>" + s + "</i>; ";
			lab = lab.substring(0, lab.length()-2) + "</html>";
	        JLabel exact = new JLabel(lab);
	        p.add(exact);
		}
        names = lex.getNames(id,LexicalType.OTHER_SYNONYM);
		if(names.size() > 0)
		{
	        lab = "<html>Other Synonyms(s): ";
			for(String s : names)
				lab += "<i>" + s + "</i>; ";
			lab = lab.substring(0, lab.length()-2) + "</html>";
	        JLabel other = new JLabel(lab);
	        p.add(other);
		}
        //Formulas
        names = lex.getNames(id,LexicalType.FORMULA);
		if(names.size() > 0)
		{
	        lab = "<html>Formula(s): ";
			for(String s : names)
				lab += "<i>" + s + "</i>; ";
				lab = lab.substring(0, lab.length()-2) + "</html>";
	        JLabel form = new JLabel(lab);
	        p.add(form);
		}
		
		//High Level Ancestors
		Set<Integer> highSet = rm.getHighLevelAncestors(id);
		lab = "<html>Ontology Branch(es): ";
		for(Integer i : highSet)
			lab += "<i>" + o.getName(i) + "</i>; ";
		if(highSet.size() == 0)
			lab += "N/A</html>";
		else
			lab = lab.substring(0, lab.length()-2) + "</html>";
		JLabel high = new JLabel(lab);
		p.add(high);
		
		//Parents
		Set<Integer> parents = rm.getParents(id);
		Table2Set<Integer,Integer> relParents = new Table2Set<Integer,Integer>();
		for(Integer i : parents)
			relParents.add(rm.getRelationship(id, i).getProperty(), i);
		Vector<Integer> rels = new Vector<Integer>(relParents.keySet());
		Collections.sort(rels);
		for(Integer r : rels)
		{
			if(r == -1)
				lab = "<html>subClassOf: ";
			else
				lab = "<html>" + o.getName(r) + ": ";
			for(Integer a : relParents.get(r))
				lab += "<i>" + o.getName(a) + "</i>; ";
			lab = lab.substring(0, lab.length()-2) + "</html>";
			JLabel ancs = new JLabel(lab);
			p.add(ancs);
		}
		
		//And Disjoints
		Set<Integer> disjointSetSource = rm.getDisjointTransitive(id);
		if(disjointSetSource.size() != 0)
		{
			lab = "<html>disjointWith: ";
			for(Integer i : disjointSetSource)
				lab += "<i>" + o.getName(i) + "</i>; ";
			lab = lab.substring(0, lab.length()-2) + "</html>";
			JLabel disjointsS = new JLabel(lab);
			p.add(disjointsS);
		}
		if(o.isObsoleteClass(id))
		{
			JLabel obsS = new JLabel("<html><b><font color=\"red\"> Obsolete Class!</font></b></html>");
			p.add(obsS);
		}
		return p;
	}
	
	private JPanel buildIndivDetailPanel(int id)
	{
		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
		
		Ontology2Match o;
		if(source.contains(id))
			o = source;
		else
			o = target;
		//Get the local name
        JLabel localName = new JLabel("<html>Local Name: <i>" +
        		uris.getLocalName(id) + "</i></html>");
        p.add(localName);
        //Labels
        String lab = "<html>Label: " + o.getName(id);
        JLabel label = new JLabel(lab);
        p.add(label);
		
		//Classes
		Set<Integer> classes = rm.getIndividualClasses(id);
		lab = "<html>instanceOf: ";
		for(Integer i : classes)
			lab += "<i>" + o.getName(i) + "</i>; ";
		if(classes.size() == 0)
			lab += "N/A</html>";
		else
			lab = lab.substring(0, lab.length()-2) + "</html>";
		JLabel high = new JLabel(lab);
		p.add(high);
		
		//Parents
		Vector<Integer> rels = new Vector<Integer>(rm.getIndividualProperties(id));
		Collections.sort(rels);
		for(Integer r : rels)
		{
			lab = "<html>" + o.getName(r) + ": ";
			for(Integer a : rm.getParentIndividuals(id, r))
				lab += "<i>" + o.getName(a) + "</i>; ";
			lab = lab.substring(0, lab.length()-2) + "</html>";
			JLabel ancs = new JLabel(lab);
			p.add(ancs);
		}
		
		//Data Properties
		Table2Set<Integer,String> data = o.getIndividual(id).getDataValues();
		rels = new Vector<Integer>(data.keySet());
		Collections.sort(rels);
		for(Integer r : rels)
		{
			lab = "<html>" + o.getName(r) + ": ";
			for(String s : data.get(r))
				lab += "<i>" + s + "</i>; ";
			lab = lab.substring(0, lab.length()-2) + "</html>";
			JLabel ancs = new JLabel(lab);
			p.add(ancs);
		}
		return p;
	}
	
	private JPanel buildPropDetailPanel(int id)
	{
		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

		Ontology2Match o;
		if(source.contains(id))
			o = source;
		else
			o = target;
		Lexicon lex = o.getLexicon();
		
		//Get the local name
        JLabel localName = new JLabel("<html>Local Name: <i>" +
        		uris.getLocalName(id) + "</i></html>");
        p.add(localName);
        //Labels
        String lab = "<html>Label(s): ";
        Set<String> names = lex.getNames(id,LexicalType.LABEL);
		for(String s : names)
			lab += "<i>" + s + "</i>; ";
		if(names.size() == 0)
			lab += "N/A</html>";
		else
			lab = lab.substring(0, lab.length()-2) + "</html>";
        JLabel label = new JLabel(lab);
        p.add(label);
        
		if(t.equals(EntityType.OBJECT))
		{
			ObjectProperty pr = o.getObjectProperty(sourceId);
			Set<Integer> domain = pr.getDomain();
			lab = "<html>Domain: ";
			for(Integer i : domain)
				lab += "<i>" + o.getName(i) + "</i>; ";
			if(domain.size() > 0)
				lab = lab.substring(0, lab.length()-2) + "</html>";
			else
				lab += "N/A</html>";
			JLabel domainS = new JLabel(lab);
			p.add(domainS);
			
			Set<Integer> range = pr.getRange();
			lab = "<html>Range: ";
			for(Integer i : range)
				lab += "<i>" + o.getName(i) + "</i>; ";
			if(range.size() > 0)
				lab = lab.substring(0, lab.length()-2) + "</html>";
			else
				lab += "N/A</html>";
			JLabel rangeS = new JLabel(lab);
			p.add(rangeS);
			if(pr.isFunctional())
			{
				JLabel funS = new JLabel("Functional Property");
				p.add(funS);
			}
		}
		else if(t.equals(EntityType.DATA))
		{
			DataProperty pr = o.getDataProperty(sourceId);
			Set<Integer> domain = pr.getDomain();
			lab = "<html>Domain: ";
			for(Integer i : domain)
				lab += "<i>" + o.getName(i) + "</i>; ";
			if(domain.size() > 0)
				lab = lab.substring(0, lab.length()-2) + "</html>";
			else
				lab += "N/A</html>";
			JLabel domainS = new JLabel(lab);
			p.add(domainS);
			
			Set<String> range = pr.getRange();
			lab = "Range: ";
			for(String s : range)
				lab += "<i>" + s + "</i>; ";
			if(range.size() > 0)
				lab = lab.substring(0, lab.length()-2) + "</html>";
			else
				lab += "N/A</html>";
			JLabel rangeS = new JLabel(lab);
			p.add(rangeS);
			if(pr.isFunctional())
			{
				JLabel funS = new JLabel("Functional Property");
				p.add(funS);
			}
		}
		return p;
	}
}