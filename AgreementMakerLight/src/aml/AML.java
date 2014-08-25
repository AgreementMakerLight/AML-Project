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
* Stores all system information for common access by all AgreementMakerLight  *
* classes. This is a singleton java pattern, so there can be only one         *
* instance of this class in the whole application. All classes can access it  *
* without reference, by invoking the static method AML.getInstance()          *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 13-08-2014                                                            *
* @version 2.0                                                                *
******************************************************************************/
package aml;

import java.io.File;
import java.io.FileFilter;
import java.net.URI;
import java.util.Vector;

import org.apache.log4j.PropertyConfigurator;

import aml.filter.CardinalityRepairer;
import aml.filter.RankedSelector;
import aml.match.AMLMatcher;
import aml.match.Alignment;
import aml.match.LexicalMatcher;
import aml.match.Mapping;
import aml.match.OAEI2013Matcher;
import aml.ontology.Ontology;
import aml.ontology.RelationshipMap;
import aml.ontology.URIMap;
import aml.settings.LanguageSetting;
import aml.settings.MatchingAlgorithm;
import aml.settings.SelectionType;
import aml.settings.SizeCategory;
import aml.ui.AlignmentFileChooser;
import aml.ui.GUI;
import aml.ui.OntologyFileChooser;
import aml.util.ExtensionFilter;

public class AML
{

//Attributes
	
	//Singleton pattern: unique instance
	private static AML aml = new AML();
	//Ontology variables
	private boolean useReasoner = true;
	//Global data structures (URI map and Relationship Map)
	private URIMap uris;
	private RelationshipMap rels;
	//Ontologies
	private Ontology source;
	private Ontology target;
	//Current alignment, reference alignment and evaluation
	private Alignment a;
	private Alignment ref;
	private String evaluation;
	//Background knowledge path and sources
	private final String BK_PATH = "store/knowledge/";
	private Vector<String> bkSources;
    private Vector<String> selectedSources;
	//User interface and associated variables
	private GUI userInterface;
	private OntologyFileChooser ofc;
	private AlignmentFileChooser afc;
	private int currentMapping = -1;
	private int maxDistance = 2;
	private boolean showAncestors = true;
	private boolean showDescendants = true;
	//Matching properties
	private LanguageSetting lang;
	private MatchingAlgorithm matcher = MatchingAlgorithm.AML;
    private SelectionType sType = SelectionType.PERMISSIVE;
    private SizeCategory sCat;
    private boolean useBK = false;
    private boolean ignoreUMLS = true;
    private boolean repairAlignment = false;
    private boolean matchProperties = false;
	private double threshold = 0.6;
	
//Constructors
	
	//It's private so that no other instances can be created 
	private AML()
	{
		//List the background knowledge sources
		bkSources = listBKSources();
        selectedSources = new Vector<String>(0,1);
	}

//Public Methods
	
    public void closeAlignment()
    {
    	a = null;
    	currentMapping = -1;
    	if(userInterface != null)
    		userInterface.refresh();
    }
    
    public void closeOntologies()
    {
    	source = null;
    	target = null;
    	uris = null;
    	rels = null;
    	a = null;
    	currentMapping = -1;
    	if(userInterface != null)
    		userInterface.refresh();
    }
    
	public void evaluate()
	{
		boolean gui = userInterface != null;
		
		evaluation = a.evaluate(ref, gui);
		
		if(gui)
			userInterface.refreshPanel();
	}
    
	public Alignment getAlignment()
    {
    	return a;
    }
	
    public AlignmentFileChooser getAlignmentFileChooser()
    {
    	return afc;
    }
	
	public Vector<String> getBKSources()
	{
		return bkSources;
	}

    public int getCurrentIndex()
    {
   		return currentMapping;
    }
    
    public Mapping getCurrentMapping()
    {
    	if(currentMapping == -1)
    		return null;
    	else
    		return a.get(currentMapping);
    }
    
    public String getEvaluation()
    {
    	return evaluation;
    }
    
	/**
	 * @return the unique instance of the core
	 */
	public static AML getInstance()
	{
		return aml;
	}
	
	public LanguageSetting getLanguageSetting()
	{
		return lang;
	}
	
    public MatchingAlgorithm getMatcher()
    {
    	return matcher;
    }

    public int getMaxDistance()
    {
		return maxDistance;
	}
    
    public OntologyFileChooser getOntologyFileChooser()
    {
    	return ofc;
    }

	/**
	 * @return the relationship map
	 */
	public RelationshipMap getRelationshipMap()
	{
		return rels;
	}
	
	public Vector<String> getSelectedBKSources()
	{
		return selectedSources;
	}
    
	public SelectionType getSelectionType()
	{
		return sType;
	}
	
	public SizeCategory getSizeCategory()
	{
		return sCat;
	}
	
	/**
	 * @return the source ontology
	 */
	public Ontology getSource()
	{
		return source;
	}
	
	/**
	 * @return the target ontology
	 */
	public Ontology getTarget()
	{
		return target;
	}
    
    public double getThreshold()
    {
    	return threshold;
    }
    
	/**
	 * @return the URI map
	 */
	public URIMap getURIMap()
	{
		return uris;
	}
        
    public void goTo(int index)
    {
   		currentMapping = index;
   		userInterface.refreshPanel();
   		userInterface.refreshGraph();
    }
    
    public boolean hasAlignment()
    {
    	return a != null && a.size() > 0;
    }
    
    public boolean hasOntologies()
    {
    	return source != null && target != null;
    }
    
    public boolean ignoreUMLS()
    {
    	return ignoreUMLS;
    }
    
    public void match()
    {
    	if(matcher.equals(MatchingAlgorithm.AML))
    	{
    		AMLMatcher m = new AMLMatcher(selectedSources,sType,matchProperties,repairAlignment);
    		a = m.match(threshold);
    	}
    	else if(matcher.equals(MatchingAlgorithm.OAEI))
    	{
    		OAEI2013Matcher m = new OAEI2013Matcher(useBK,ignoreUMLS,repairAlignment);
    		a = m.match();
    	}
    	else if(matcher.equals(MatchingAlgorithm.LEXICAL))
    	{
    		LexicalMatcher m = new LexicalMatcher();
    		a = m.match(threshold);
    		RankedSelector s = new RankedSelector(sType);
    		a = s.select(a, threshold);
    	}
    	if(a.size() >= 1)
    		currentMapping = 0;
    	else
    		currentMapping = -1;
    	evaluation = null;
    	if(userInterface != null)
    		userInterface.refresh();
    }
    
    public boolean matchProperties()
    {
    	return matchProperties;
    }
    
    public void matchProperties(boolean m)
    {
    	matchProperties = m;
    }
    
    public void nextMapping()
    {
    	if(currentMapping == a.size()-1)
    		currentMapping = 0;
    	else
    		currentMapping++;
		userInterface.refreshPanel();
		userInterface.refreshGraph();
    }
    
    public void openAlignment(String path) throws Exception
    {
    	a = new Alignment(path);
    	evaluation = null;
    	if(a.size() >= 1)
    		currentMapping = 0;
    	else
    		currentMapping = -1;
    	if(userInterface != null)
    		userInterface.refresh();
    }
    
	/**
	 * Open a pair of local ontologies
	 * @param src: the path to the source ontology
	 * @param tgt: the path to the target ontology
	 */
	public void openOntologies(String src, String tgt)
	{
        //Initialize the URIMap and RelationshipMap
		uris = new URIMap();
		rels = new RelationshipMap();
		if(useReasoner)
			PropertyConfigurator.configure("log4j.properties");
		long time = System.currentTimeMillis()/1000;
		source = new Ontology(src,true);
		time = System.currentTimeMillis()/1000 - time;
		System.out.println(source.getURI() + " loaded in " + time + " seconds");
		System.out.println("Classes: " + source.classCount());	
		System.out.println("Names: " + source.getLexicon().size());
		System.out.println("Properties: " + source.propertyCount());
		time = System.currentTimeMillis()/1000;
		target = new Ontology(tgt,true);
		time = System.currentTimeMillis()/1000 - time;
		System.out.println(target.getURI() + " loaded in " + time + " seconds");
		System.out.println("Classes: " + target.classCount());
		System.out.println("Names: " + target.getLexicon().size());
		System.out.println("Properties: " + target.propertyCount());
		System.out.println("Direct Relationships: " + rels.relationshipCount());
		time = System.currentTimeMillis()/1000;
		rels.transitiveClosure();
		time = System.currentTimeMillis()/1000 - time;
		System.out.println("Transitive closure finished in " + time + " seconds");	
		System.out.println("Extended Relationships: " + rels.relationshipCount());
		System.out.println("Disjoints: " + rels.disjointCount());
    	//Reset the alignment, mapping, and evaluation
    	a = null;
    	currentMapping = -1;
    	evaluation = null;
    	//Set the size category and language setting
    	setSizeCategory();
    	setLanguageSetting();
    	//Refresh the user interface
    	if(userInterface != null)
    		userInterface.refresh();
	}
	
	/**
	 * Open a pair of ontologies from the web
	 * @param src: the URI of the source ontology
	 * @param tgt: the URI of the target ontology
	 */
	public void openOntologies(URI src, URI tgt)
	{
        //Initialize the URIMap and RelationshipMap
		uris = new URIMap();
		rels = new RelationshipMap();
		if(useReasoner)
			PropertyConfigurator.configure("log4j.properties");
		long time = System.currentTimeMillis()/1000;
		source = new Ontology(src,true);
		time = System.currentTimeMillis()/1000 - time;
		System.out.println(source.getURI() + " loaded in " + time + " seconds");
		System.out.println("Classes: " + source.classCount());	
		System.out.println("Names: " + source.getLexicon().size());
		System.out.println("Properties: " + source.propertyCount());
		time = System.currentTimeMillis()/1000;
		target = new Ontology(tgt,true);
		time = System.currentTimeMillis()/1000 - time;
		System.out.println(target.getURI() + " loaded in " + time + " seconds");
		System.out.println("Classes: " + target.classCount());
		System.out.println("Names: " + target.getLexicon().size());
		System.out.println("Properties: " + target.propertyCount());
		System.out.println("Direct Relationships: " + rels.relationshipCount());
		time = System.currentTimeMillis()/1000;
		rels.transitiveClosure();
		time = System.currentTimeMillis()/1000 - time;
		System.out.println("Transitive closure finished in " + time + " seconds");		
		System.out.println("Extended Relationships: " + rels.relationshipCount());
		System.out.println("Disjoints: " + rels.disjointCount());
    	//Reset the alignment, mapping, and evaluation
    	a = null;
    	currentMapping = -1;
    	evaluation = null;
    	//Set the size category and language setting
    	setSizeCategory();
    	setLanguageSetting();
    	//Refresh the user interface
    	if(userInterface != null)
    		userInterface.refresh();
	}
    
    public void openReferenceAlignment(String path) throws Exception
    {
    	ref = new Alignment(path);
    }

    public void previousMapping()
    {
    	if(currentMapping == 0)
    		currentMapping = a.size()-1;
    	else
    		currentMapping--;
		userInterface.refreshPanel();
		userInterface.refreshGraph();
    }
    
    public void repair()
    {
		CardinalityRepairer r = new CardinalityRepairer();
		a = r.repair(a);
    	if(a.size() >= 1)
    		currentMapping = 0;
    	else
    		currentMapping = -1;
    	evaluation = null;
    	if(userInterface != null)
    		userInterface.refresh();
    }
    
    public boolean repairAlignment()
    {
    	return repairAlignment;
    }
    
    public void repairAlignment(boolean r)
    {
    	repairAlignment = r;
    }
    
    public void saveAlignmentRDF(String file) throws Exception
    {
    	a.saveRDF(file);
    }
    
    public void saveAlignmentTSV(String file) throws Exception
    {
    	a.saveTSV(file);
    }
    
	public void setAlignment(Alignment maps)
	{
		a = maps;
	}
	
	/**
	 * 	Computes and sets the language setting of the matching problem
	 *  based on the language overlap between the input ontologies
	 */
	public void setLanguageSetting()
	{
		lang = LanguageSetting.getLanguageSetting(source, target);
	}
    
    public void setMatcher(MatchingAlgorithm m)
	{
		matcher = m;
	}
	
	public void setMatchOptions(SelectionType s, double thresh)
	{
		sType = s;
	    threshold = thresh;
	}
    
	public void setMatchOptions(boolean bk, boolean um, boolean re)
	{
		useBK = bk;
	    ignoreUMLS = um;
	    repairAlignment = re;
	}
	
	public void setMatchOptions(boolean pr, boolean re, Vector<String> bk, SelectionType s, double thresh)
	{
	    matchProperties = pr;
	    repairAlignment = re;
	    selectedSources = bk;
		sType = s;
	    threshold = thresh;
	}
	
	public void setSelectionType(SelectionType s)
	{
		if(s == null)
			sType = SelectionType.getSelectionType(a);
		else
			sType = s;
	}
	
	public void setViewOptions(boolean a, boolean d, int m)
	{
		showAncestors = a;
		showDescendants = d;
		maxDistance = m;
		userInterface.refreshGraph();
	}

	public boolean showAncestors()
	{
		return showAncestors;
	}

	public boolean showDescendants()
	{
		return showDescendants;
	}
	
	public void startGUI()
	{
        //Set the Nimbus look and feel
        try
        {
            for(javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels())
            {
                if ("Nimbus".equals(info.getName()))
                {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        //Initialize the file choosers
        ofc = new OntologyFileChooser();
        afc = new AlignmentFileChooser();
        //Create and display the GUI
        java.awt.EventQueue.invokeLater(new Runnable()
        {
            public void run()
            {
                userInterface = new GUI();
            }
        });
	}
	
	public boolean useBK()
	{
		return useBK;
	}
	
	/**
	 * @return whether the reasoner is used to process ontology relationships
	 */
	public boolean useReasoner()
	{
		return useReasoner;
	}
	
	/**
	 * Sets whether to use the reasoner to process ontology relationships
	 * @param b: whether to use the reasoner
	 */
	public void useReasoner(boolean b)
	{
		useReasoner = b;
	}
	
//Private Methods
	
	//Lists the background knowledge ontologies in the knowledge directory
	private Vector<String> listBKSources()
	{
		File ontRoot = new File(BK_PATH);
		FileFilter filter = new ExtensionFilter("Ontology Files (*.owl, *.rdf, *.rdfs, *.xml)",
				new String[] { ".owl", ".rdf", ".rdfs", ".xml" }, false);
		File[] ontFiles = ontRoot.listFiles(filter);
		Vector<String> sources = new Vector<String>(ontFiles.length);
		for(File f : ontFiles)
			sources.add(f.getName());
		sources.add("UMLS");
		sources.add("WordNet");
		return sources;
	}

	//Computes and sets the size category of the matching problem
	//based on the number of classes of the input ontologies
	private void setSizeCategory()
	{
		int sSize = source.classCount();
		int tSize = target.classCount();
		int max = Math.max(sSize, tSize);
		if(Math.min(sSize,tSize) > 30000 || max > 60000)
			sCat = SizeCategory.LARGE;
		else if(max > 500)
			sCat = SizeCategory.MEDIUM;
		else
			sCat = SizeCategory.SMALL;
	}
}