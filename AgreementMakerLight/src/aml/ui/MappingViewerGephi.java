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
* Displays the graph surrounding a Mapping in a JInternalFrame, using a       *
* PApplet from the Gephi toolbox.                                             *
*                                                                             *
* @author Daniel Faria & Catia Pesquita                                       *
* @date 20-05-2015                                                            *
******************************************************************************/
package aml.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JInternalFrame;
import javax.swing.JOptionPane;

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
import aml.match.Alignment;
import aml.match.Mapping;
import aml.ontology.Ontology;
import aml.ontology.RelationshipMap;
import aml.settings.MappingRelation;
import processing.core.PApplet;

public class MappingViewerGephi extends JInternalFrame
{
	
//Attributes
	
	private static final long serialVersionUID = 1800784763390236491L;
	private static final int MAX_RETRIES = 5;
	private GraphController graphController;
	private GraphModel model;
	private DirectedGraph directedGraph;
	private Ontology source, target;
	private RelationshipMap rm;
	private Alignment a;
	private HashSet<Integer> sourceNodes, targetNodes;
	private int maxDistance;
	private final Color BK = new Color(214,217,223);
	
//Constructor
	
	public MappingViewerGephi(Dimension max, Dimension min)
	{
		super("Mapping Viewer (Gephi Toolkit)",false,false,false,false);
		//Set the size
		this.setMaximumSize(max);
		this.setPreferredSize(min);
		//Build the graph for the current mapping
		buildGraph(0);
		//Pack and go
		this.pack();
		setVisible(true);
	}

//Public Methods (Mapping to Gephi)
	
	public void buildGraph(int check)
	{
		//Check if there is a mapping to view
		Mapping m = AML.getInstance().getCurrentMapping();
		if(m == null)
			return;
		try
		{
			//Initialize a project and therefore a workspace
			ProjectController pc = Lookup.getDefault().lookup(ProjectController.class);
			pc.newProject();
			
			//Initialize the graph-building variables
			graphController = Lookup.getDefault().lookup(GraphController.class);
			model = graphController.getModel();
			directedGraph = model.getDirectedGraph();
			
			//Get the ontologies, relationship map and alignment
			AML aml = AML.getInstance();
			source = aml.getSource();
			target = aml.getTarget();
			rm = aml.getRelationshipMap();
			a = aml.getAlignment();
			
			//Add the starting source and target nodes to the graph
			int sId = m.getSourceId();
			addSourceNode(sId,8);
			int tId = m.getTargetId();
			addTargetNode(tId,8);
			//And the mapping between them
			addMapping(sId,tId);
			//Get the maximum distance
			maxDistance = aml.getMaxDistance();
			//Initialize the node sets (which don't include the starting nodes)
			sourceNodes = new HashSet<Integer>();
			targetNodes = new HashSet<Integer>();
			//Add all ancestors and descendants as per the view parameters
			if(aml.showAncestors())
			{
				addSourceAncestors(sId);
				addTargetAncestors(tId);
			}
			if(aml.showDescendants())
			{
				addSourceDescendants(sId);
				addTargetDescendants(tId);
			}
			//Add all additional mappings of the initial nodes
			addOtherMappings(sId,tId);
			//Now find if there are any mappings between the node sets and add them
			addAllMappings();

			
			//Run YifanHuLayout for 100 passes - The layout always takes the current visible view
			YifanHuLayout layout = new YifanHuLayout(null, new StepDisplacement(1f));
			layout.setGraphModel(model);
			layout.resetPropertiesValues();
			layout.setOptimalDistance(300f);
			layout.setBarnesHutTheta(0.2f);
			layout.initAlgo();
			for (int i = 0; i < 100 && layout.canAlgo(); i++)
			{
				layout.goAlgo();
			}
		
			//Run LabelAdjust to stop labels from overlapping
			LabelAdjust labela=new LabelAdjust(null);
			labela.resetPropertiesValues();
			labela.initAlgo();
			for (int i = 0; i < 30 && labela.canAlgo(); i++)
			{
				labela.goAlgo();
			}
			
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
			PApplet applet = target.getApplet();
			applet.init();
			//Refresh the preview and reset the zoom
			previewController.render(target);
			target.refresh();
			target.resetZoom();
			//Put the PApplet in the MappingViewer
			setContentPane(applet);
		}
		catch(Exception e)
		{
			if(check < MAX_RETRIES)
			{
				buildGraph(check+1);
				System.out.println(e.getMessage());
			}
			else
			{
				JOptionPane.showMessageDialog(this,
						"Cannot render mapping in Gephi!\nPlease restart AgreementMakerLight!",
                        "Error", JOptionPane.ERROR_MESSAGE);
			}
		}
	}

//Private Methods (Mapping to Graph)
	
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
			e1.getEdgeData().setColor(1, 1, 0);
			e1.getEdgeData().setLabel("" + a.getSimilarity(sId,tId));
			directedGraph.addEdge(e1);
		}
		if(!r.equals(MappingRelation.SUBCLASS))
		{
			Edge e2 = model.factory().newEdge(n2, n1, 3, true);
			e2.getEdgeData().setColor(1, 1, 0);
			if(r.equals(MappingRelation.SUPERCLASS))
				e2.getEdgeData().setLabel("" + a.getSimilarity(sId,tId));
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