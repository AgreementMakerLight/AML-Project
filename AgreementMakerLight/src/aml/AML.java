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
* Stores all system information for common access by all AgreementMakerLight  *
* classes. This is a singleton java pattern, so there can be only one         *
* instance of this class in the whole application. All classes can access it  *
* without reference, by invoking the static method AML.getInstance()          *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 13-08-2015                                                            *
******************************************************************************/
package aml;

import java.io.File;
import java.io.FileFilter;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import org.apache.log4j.PropertyConfigurator;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import aml.filter.SemanticRepairer;
import aml.match.ManualMatcher;
import aml.match.Alignment;
import aml.match.Mapping;
import aml.match.AutomaticMatcher;
import aml.ontology.BKOntology;
import aml.ontology.Ontology2Match;
import aml.ontology.RelationshipMap;
import aml.ontology.URIMap;
import aml.settings.LanguageSetting;
import aml.settings.MatchStep;
import aml.settings.NeighborSimilarityStrategy;
import aml.settings.SelectionType;
import aml.settings.SizeCategory;
import aml.settings.StringSimMeasure;
import aml.settings.WordMatchStrategy;
import aml.translate.Dictionary;
import aml.ui.AlignmentFileChooser;
import aml.ui.GUI;
import aml.ui.OntologyFileChooser;
import aml.util.ExtensionFilter;
import aml.util.InteractionManager;

public class AML
{

//Attributes
	
	//Singleton pattern: unique instance
	private static AML aml = new AML();
	//The ontology and alignment data structures
	private URIMap uris;
	private RelationshipMap rels;
	private Ontology2Match source;
	private Ontology2Match target;
	private BKOntology bk;
	private Alignment a;
	private Alignment ref;
	//The user interaction manager
	//(for handling simulated interactions)
	private InteractionManager im;
	//Evaluation parameters
	private String evaluation;
	private double precision;
	private double recall;
	private double fMeasure;
	//General matching settings
	private boolean useReasoner = false;
	private final String BK_PATH = "store/knowledge/";
	private Vector<String> bkSources; //The list of files under the BK_PATH
    private LanguageSetting lang;
	private SizeCategory size;
	private Set<String> languages;
    private SelectionType sType;
	//Manual matching settings
	private double threshold = 0.6;
    private Vector<MatchStep> selectedSteps;
    private Vector<String> selectedSources; //The selected files under the BK_PATH
    private WordMatchStrategy wms;
    private StringSimMeasure ssm;
    private boolean primaryStringMatcher; //Whether to use the String Matcher globally (TRUE) or locally (FALSE)
    private NeighborSimilarityStrategy nss;
    private boolean directNeighbors = false;
    private boolean removeObsolete;
    private boolean structuralSelection;    
	//User interface and settings
	private GUI userInterface;
	private OntologyFileChooser ofc;
	private AlignmentFileChooser afc;
	private int currentMapping = -1;
	private int maxDistance = 2;
	private boolean showAncestors = true;
	private boolean showDescendants = true;
    private String language;
	
//Constructors
	
	//It's private so that no other instances can be created 
	private AML()
	{
        //Initialize the URIMap and RelationshipMap
		uris = new URIMap();
		rels = new RelationshipMap();
	}

//Public Methods

	/**
	 * Closes the active alignment (GUI)
	 */
	public void closeAlignment()
    {
    	a = null;
    	currentMapping = -1;
    	if(userInterface != null)
    		userInterface.refresh();
    }
    
	/**
	 * Closes the active ontology pair (GUI)
	 */
    public void closeOntologies()
    {
    	source = null;
    	target = null;
    	bk = null;
    	uris = null;
    	rels = null;
    	a = null;
    	currentMapping = -1;
    	if(userInterface != null)
    		userInterface.refresh();
    }
    
    /**
     * Sets up the default matching configuration for the ontologies
     */
    public void defaultConfig()
    {
		bkSources = new Vector<String>();		
		File ontRoot = new File(BK_PATH);
		if(ontRoot.exists())
		{
			FileFilter ont = new ExtensionFilter("Ontology Files (*.owl, *.rdf, *.rdfs, *.xml)",
					new String[] { ".owl", ".rdf", ".rdfs", ".xml" }, false);
			File[] ontFiles = ontRoot.listFiles(ont);
			for(File f : ontFiles)
				bkSources.add(f.getName());
			FileFilter lex = new ExtensionFilter("Lexicon Files (*.lexicon)",
					new String[] { ".lexicon" }, false);
			File[] lexFiles = ontRoot.listFiles(lex);
			for(File f : lexFiles)
				bkSources.add(f.getName());
			bkSources.add("WordNet");
		}
		else
		{
			System.out.println("WARNING: 'store/knowledge' directory not found!");
		}
		selectedSources = new Vector<String>(bkSources);
		
    	size = SizeCategory.getSizeCategory();
    	lang = LanguageSetting.getLanguageSetting();
		languages = new HashSet<String>();
		for(String s : source.getLexicon().getLanguages())
			if(target.getLexicon().getLanguages().contains(s))
				languages.add(s);
		if(languages.contains("en") || languages.size() == 0)
			language = "en";
		else
			language = languages.iterator().next();
		selectedSteps = new Vector<MatchStep>();
		if(lang.equals(LanguageSetting.TRANSLATE))
			selectedSteps.add(MatchStep.TRANSLATE);
		if(lang.equals(LanguageSetting.SINGLE))
			selectedSteps.add(MatchStep.BK);
		if(!size.equals(SizeCategory.HUGE))
			selectedSteps.add(MatchStep.WORD);
		selectedSteps.add(MatchStep.STRING);
		if(size.equals(SizeCategory.SMALL) || size.equals(SizeCategory.MEDIUM))
			selectedSteps.add(MatchStep.STRUCT);
		double sourceRatio = (source.dataPropertyCount() + source.objectPropertyCount()) * 1.0 / source.classCount();
		double targetRatio = (target.dataPropertyCount() + target.objectPropertyCount()) * 1.0 / target.classCount();
		if(sourceRatio >= 0.05 && targetRatio >= 0.05)
			selectedSteps.add(MatchStep.PROPERTY);
		selectedSteps.add(MatchStep.SELECT);
		selectedSteps.add(MatchStep.REPAIR);
		wms = WordMatchStrategy.AVERAGE;
		ssm = StringSimMeasure.ISUB;
		primaryStringMatcher = size.equals(SizeCategory.SMALL);
		nss = NeighborSimilarityStrategy.DESCENDANTS;
		directNeighbors = false;
		sType = SelectionType.getSelectionType();
		removeObsolete = size.equals(SizeCategory.HUGE);
		structuralSelection = size.equals(SizeCategory.HUGE);		
    }
    
    /**
     * @return whether to use direct neighbors only in the NeighborSimilarityMatcher
     */
	public boolean directNeighbors()
	{
		return directNeighbors;
	}

	/**
	 * Evaluates the current alignment using the reference alignment
	 */
	public void evaluate()
	{
		boolean gui = userInterface != null;
		int[] eval = a.evaluate(ref); 
		int found = a.size() - eval[1];
		int correct = eval[0];
		int total = ref.size() - ref.countConflicts();
		
		precision = 1.0*correct/found;
		String prc = Math.round(precision*1000)/10.0 + "%";
		recall = 1.0*correct/total;
		String rec = Math.round(recall*1000)/10.0 + "%";
		fMeasure = 2*precision*recall/(precision+recall);
		String fms = Math.round(fMeasure*1000)/10.0 + "%";
		
		if(gui)
		{
			evaluation = "Precision: " + prc + "; Recall: " + rec + "; F-measure: " + fms;
			userInterface.refreshPanel();
		}
		else
			evaluation = "Precision\tRecall\tF-measure\tFound\tCorrect\tReference\n" + prc +
					"\t" + rec + "\t" + fms + "\t" + found + "\t" + correct + "\t" + total;
	}
    
	/**
	 * @return the current alignment
	 */
	public Alignment getAlignment()
    {
    	return a;
    }
	
	/**
	 * @return the file chooser for alignment files
	 */
    public AlignmentFileChooser getAlignmentFileChooser()
    {
    	return afc;
    }
    
    /**
     * @return the current background knowledge ontology
     */
	public BKOntology getBKOntology()
	{
		return bk;
	}

	/**
	 * @return the available background knowledge sources
	 */
	public Vector<String> getBKSources()
	{
		return bkSources;
	}
	
	/**
	 * @return the index of the current mapping in the current alignment
	 */
	public int getCurrentIndex()
    {
   		return currentMapping;
    }
    
	/**
	 * @return the current mapping
	 */
    public Mapping getCurrentMapping()
    {
    	if(currentMapping == -1)
    		return null;
    	else
    		return a.get(currentMapping);
    }
	
    /**
     * @return the evaluation of the current alignment
     */
    public String getEvaluation()
    {
    	return evaluation;
    }

    /**
     * @return this (single) instance of the AML class
     */
	public static AML getInstance()
	{
		return aml;
	}
	
	public InteractionManager getInteractionManager()
	{
		return im;
	}
	
	/**
	 * @return the preferred language for entity names
	 */
    public String getLabelLanguage()
    {
		return language;
	}

    /**
     * @return the languages of entity names in the current ontology pair
     */
	public Set<String> getLanguages()
	{
		return languages;
	}

	/**
     * @return the active LanguageSetting
     */
	public LanguageSetting getLanguageSetting()
	{
		return lang;
	}

	/**
     * @return the maximum edge-distance to plot in the Mapping Viewer
     */
	public int getMaxDistance()
    {
		return maxDistance;
	}
    
	/**
     * @return the active NeighborSimilarityStrategy 
     */
    public NeighborSimilarityStrategy getNeighborSimilarityStrategy()
    {
		return nss;
	}

	/**
     * @return the file chooser for ontology files 
     */
	public OntologyFileChooser getOntologyFileChooser()
    {
    	return ofc;
    }

	/**
	 * @return the current reference alignment
	 */
	public Alignment getReferenceAlignment()
    {
    	return ref;
    }
    
	/**
	 * @return the RelationshipMap
	 */
	public RelationshipMap getRelationshipMap()
	{
		return rels;
	}
	
	/**
	 * @return the selected background knowledge sources
	 */
	public Vector<String> getSelectedBKSources()
	{
		return selectedSources;
	}

	/**
	 * @return the selected matching steps
	 */
	public Vector<MatchStep> getSelectedSteps()
	{
		return selectedSteps;
	}
	
	/**
	 * @return the active SelectionType
	 */
	public SelectionType getSelectionType()
	{
		return sType;
	}
	
	/**
	 * @return the SizeCategory of the current ontology pair
	 */
	public SizeCategory getSizeCategory()
	{
		return size;
	}
	
	/**
	 * @return the current source ontology
	 */
	public Ontology2Match getSource()
	{
		return source;
	}
	
	/**
	 * @return the active StringSimMeasure
	 */
	public StringSimMeasure getStringSimMeasure()
	{
		return ssm;
	}

	/**
	 * @return the current target ontology
	 */
	public Ontology2Match getTarget()
	{
		return target;
	}
    
	/**
	 * @return the active similarity threshold
	 */
	public double getThreshold()
    {
    	return threshold;
    }
    
	/**
	 * @return the URIMap
	 */
	public URIMap getURIMap()
	{
		return uris;
	}
	
	/**
	 * @return the active WordMatchStrategy
	 */
    public WordMatchStrategy getWordMatchStrategy()
    {
		return wms;
	}

	/**
	 * @param index: the index of the mapping to plot
	 * Plots the mapping at the selected index
	 */
	public void goTo(int index)
    {
   		currentMapping = index;
   		userInterface.refresh();
    }
    
	/**
	 * @return whether there is a non-empty active alignment
	 */
    public boolean hasAlignment()
    {
    	return a != null && a.size() > 0;
    }
    
    /**
     * @return whether there are classes in both active ontologies
     */
    public boolean hasClasses()
    {
    	return hasOntologies() &&
    		source.classCount() > 0 && target.classCount() > 0;
    }
    
    /**
     * @return whether there is an active pair of ontologies
     */
    public boolean hasOntologies()
    {
    	return source != null && target != null;
    }
    
    /**
     * @return whether there are properties of corresponding types in both
     * active ontologies
     */
    public boolean hasProperties()
    {
    	return hasOntologies() &&
   			((source.dataPropertyCount() > 0 && target.dataPropertyCount() > 0) ||
   			(source.objectPropertyCount() > 0 && target.objectPropertyCount() > 0));
    }
	
    /**
     * Matches the active ontologies using the default configuration
     */
    public void matchAuto()
    {
    	im = new InteractionManager();
   		a = AutomaticMatcher.match();
    	if(a.size() >= 1)
    		currentMapping = 0;
    	else
    		currentMapping = -1;
    	evaluation = null;
    	if(userInterface != null)
    		userInterface.refresh();
    }

    /**
     * Matches the active ontologies using manual configurations
     */
    public void matchManual()
    {
   		a = ManualMatcher.match();
    	if(a.size() >= 1)
    		currentMapping = 0;
    	else
    		currentMapping = -1;
    	evaluation = null;
    	if(userInterface != null)
    		userInterface.refresh();
    }
    
    public void nextMapping()
    {
    	if(currentMapping == a.size()-1)
    		currentMapping = 0;
    	else
    		currentMapping++;
		userInterface.refresh();
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
	
	public void openBKOntology(String name) throws OWLOntologyCreationException
	{
		long time = System.currentTimeMillis()/1000;
		if(bk != null)
			bk.close();
		bk = new BKOntology(BK_PATH + name);
		time = System.currentTimeMillis()/1000 - time;
		System.out.println(bk.getURI() + " loaded in " + time + " seconds");
	}
	
	public void openOntologies(String src, String tgt) throws OWLOntologyCreationException
	{
		closeOntologies();
        //Initialize the URIMap and RelationshipMap
		uris = new URIMap();
		rels = new RelationshipMap();
		if(useReasoner)
			PropertyConfigurator.configure("log4j.properties");
		long time = System.currentTimeMillis()/1000;
		System.out.println("Loading source ontology");	
		source = new Ontology2Match(src);
		time = System.currentTimeMillis()/1000 - time;
		System.out.println(source.getURI() + " loaded in " + time + " seconds");
		System.out.println("Classes: " + source.classCount());
		System.out.println("Names: " + source.getLexicon().size());
		System.out.println("Individuals: " + source.individualCount());
		System.out.println("Properties: " + (source.dataPropertyCount()+source.objectPropertyCount()));
		time = System.currentTimeMillis()/1000;
		System.out.println("Loading target ontology");
		target = new Ontology2Match(tgt);
		time = System.currentTimeMillis()/1000 - time;
		System.out.println(target.getURI() + " loaded in " + time + " seconds");
		System.out.println("Classes: " + target.classCount());
		System.out.println("Names: " + target.getLexicon().size());
		System.out.println("Individuals: " + target.individualCount());
		System.out.println("Properties: " + (target.dataPropertyCount()+target.objectPropertyCount()));
		System.out.println("Direct Relationships: " + rels.relationshipCount());
		time = System.currentTimeMillis()/1000;
		System.out.println("Running transitive closure on RelationshipMap");
		rels.transitiveClosure();
		time = System.currentTimeMillis()/1000 - time;
		System.out.println("Transitive closure finished in " + time + " seconds");	
		System.out.println("Extended Relationships: " + rels.relationshipCount());
		System.out.println("Disjoints: " + rels.disjointCount());
    	//Reset the alignment, mapping, and evaluation
    	a = null;
    	currentMapping = -1;
    	evaluation = null;
    	//Refresh the user interface
    	if(userInterface != null)
    		userInterface.refresh();
    	defaultConfig();
    	System.out.println("Finished!");	
	}
	
	public void openOntologies(URI src, URI tgt) throws OWLOntologyCreationException
	{
		closeOntologies();
        //Initialize the URIMap and RelationshipMap
		uris = new URIMap();
		rels = new RelationshipMap();
		if(useReasoner)
			PropertyConfigurator.configure("log4j.properties");
		long time = System.currentTimeMillis()/1000;
		System.out.println("Loading source ontology");	
		source = new Ontology2Match(src);
		time = System.currentTimeMillis()/1000 - time;
		System.out.println(source.getURI() + " loaded in " + time + " seconds");
		System.out.println("Classes: " + source.classCount());	
		System.out.println("Names: " + source.getLexicon().size());
		System.out.println("Individuals: " + source.individualCount());
		System.out.println("Properties: " + (source.dataPropertyCount()+source.objectPropertyCount()));
		time = System.currentTimeMillis()/1000;
		System.out.println("Loading target ontology");
		target = new Ontology2Match(tgt);
		time = System.currentTimeMillis()/1000 - time;
		System.out.println(target.getURI() + " loaded in " + time + " seconds");
		System.out.println("Classes: " + target.classCount());
		System.out.println("Names: " + target.getLexicon().size());
		System.out.println("Individuals: " + target.individualCount());
		System.out.println("Properties: " + (target.dataPropertyCount()+target.objectPropertyCount()));
		System.out.println("Direct Relationships: " + rels.relationshipCount());
		time = System.currentTimeMillis()/1000;
		System.out.println("Running transitive closure on RelationshipMap");
		rels.transitiveClosure();
		time = System.currentTimeMillis()/1000 - time;
		System.out.println("Transitive closure finished in " + time + " seconds");	
		System.out.println("Extended Relationships: " + rels.relationshipCount());
		System.out.println("Disjoints: " + rels.disjointCount());
    	//Reset the alignment, mapping, and evaluation
    	a = null;
    	currentMapping = -1;
    	evaluation = null;
    	//Refresh the user interface
    	if(userInterface != null)
    		userInterface.refresh();
    	defaultConfig();
    	System.out.println("Finished!");	
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
		userInterface.refresh();
    }
    
    public boolean primaryStringMatcher()
    {
		return primaryStringMatcher;
	}
    
    public void refreshGUI()
    {
    	userInterface.refresh();
    }

	public boolean removeObsolete()
	{
		return removeObsolete;
	}

	public void repair()
    {
		SemanticRepairer r = new SemanticRepairer();
		a = r.repair(a);
    	if(a.size() >= 1)
    		currentMapping = 0;
    	else
    		currentMapping = -1;
    	evaluation = null;
    	if(userInterface != null)
    		userInterface.refresh();
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

	public void setDirectNeighbors(boolean directNeighbors)
	{
		this.directNeighbors = directNeighbors;
	}

	public void setLabelLanguage(String language)
	{
		this.language = language;
	}
	
	public void setLanguageSetting()
	{
		lang = LanguageSetting.getLanguageSetting();
	}

	public void setNeighborSimilarityStrategy(NeighborSimilarityStrategy nss)
	{
		this.nss = nss;
	}

	public void setOntologies(Ontology2Match s, Ontology2Match t)
	{
		source = s;
		target = t;
		defaultConfig();
	}
	
	public void setPrimaryStringMatcher(boolean primary)
	{
		primaryStringMatcher = primary;
	}
	
	public void setRemoveObsolete(boolean removeObsolete)
	{
		this.removeObsolete = removeObsolete;
	}
	
	public void setSelectedSources(Vector<String> sources)
	{
		selectedSources = sources;
	}

	public void setSelectedSteps(Vector<MatchStep> steps)
	{
		selectedSteps = steps;
	}


	public void setSelectionType(SelectionType s)
	{
		if(s == null)
			sType = SelectionType.getSelectionType();
		else
			sType = s;
	}
	
	public void setStringSimMeasure(StringSimMeasure ssm)
	{
		this.ssm = ssm;
	}
	
	public void setStructuralSelection(boolean structuralSelection)
	{
		this.structuralSelection = structuralSelection;
	}

	public void setThreshold(double thresh)
	{
		threshold = thresh;
	}
	
	public void setUseReasoner(boolean b)
	{
		useReasoner = b;
	}

	public void setViewOptions(boolean a, boolean d, int m)
	{
		showAncestors = a;
		showDescendants = d;
		maxDistance = m;
		userInterface.refresh();
	}
	
	public void setWordMatchStrategy(WordMatchStrategy wms)
	{
		this.wms = wms;
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
	
	public boolean structuralSelection()
	{
		return structuralSelection;
	}

	public void translateOntologies()
	{
		Vector<String> sLangs = new Vector<String>(source.getLexicon().getLanguages());
		Vector<String> tLangs = new Vector<String>(target.getLexicon().getLanguages());
		for(String l1 : sLangs)
		{
			for(String l2 : tLangs)
			{
				if(!l1.equals(l2))
				{
					Dictionary d = new Dictionary(l1,l2);
					d.translateLexicon(source.getLexicon());
					d = new Dictionary(l2,l1);
					d.translateLexicon(target.getLexicon());
				}
			}
		}
		languages = new HashSet<String>();
		for(String s : source.getLexicon().getLanguages())
			if(target.getLexicon().getLanguages().contains(s))
				languages.add(s);
	}
	
	public boolean useReasoner()
	{
		return useReasoner;
	}
}