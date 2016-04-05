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
* Stores all system information for common access by all AgreementMakerLight  *
* classes. This is a singleton java pattern, so there can be only one         *
* instance of this class in the whole application. All classes can access it  *
* without reference, by invoking the static method AML.getInstance()          *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml;

import java.io.File;
import java.io.FileFilter;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import javax.swing.UIManager;

import org.apache.log4j.PropertyConfigurator;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import aml.filter.CustomFilterer;
import aml.filter.CustomFlagger;
import aml.filter.QualityFlagger;
import aml.filter.RepairMap;
import aml.filter.Repairer;
import aml.match.ManualMatcher;
import aml.match.Mapping;
import aml.match.Alignment;
import aml.match.AutomaticMatcher;
import aml.ontology.BKOntology;
import aml.ontology.Ontology2Match;
import aml.ontology.RelationshipMap;
import aml.ontology.URIMap;
import aml.settings.Problem;
import aml.settings.LanguageSetting;
import aml.settings.MappingStatus;
import aml.settings.MatchStep;
import aml.settings.NeighborSimilarityStrategy;
import aml.settings.SelectionType;
import aml.settings.SizeCategory;
import aml.settings.StringSimMeasure;
import aml.settings.WordMatchStrategy;
import aml.translate.Dictionary;
import aml.ui.AMLColor;
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
	//Path to AML
	private String dir;
	//The ontology and alignment data structures
	private URIMap uris;
	private RelationshipMap rels;
	private Ontology2Match source;
	private Ontology2Match target;
	private BKOntology bk;
	private Alignment a;
	private Alignment ref;
	private RepairMap rep;
	private QualityFlagger qf;
	//The user interaction manager
	//(for handling simulated interactions)
	private InteractionManager im;
	//Evaluation parameters
	private String evaluation;
	private double precision;
	private double recall;
	private double fMeasure;
	//General matching settings
	private boolean useReasoner = true;
	private final String LOG = "log4j.properties";
	private final String BK_PATH = "store/knowledge/";
	private Vector<String> bkSources; //The list of files under the BK_PATH
    private LanguageSetting lang;
	private SizeCategory size;
	private Set<String> languages;
    private SelectionType sType;
	//Manual matching settings
	private double threshold = 0.6;
	private boolean hierarchic;
	private Vector<MatchStep> matchSteps;
    private Vector<Problem> flagSteps;
    private Vector<String> selectedSources; //The selected files under the BK_PATH
    private WordMatchStrategy wms;
    private StringSimMeasure ssm;
    private boolean primaryStringMatcher; //Whether to use the String Matcher globally (TRUE) or locally (FALSE)
    private NeighborSimilarityStrategy nss;
    private boolean directNeighbors = false;
    private boolean structuralSelection;    
	//User interface and settings
	private GUI userInterface;
	private int activeMapping;
	private boolean needSave = false;
	private OntologyFileChooser ofc;
	private AlignmentFileChooser afc;
	private int classDistance = 2;
	private int individualDistance = 2;
	private boolean showAncestors = true;
	private boolean showDescendants = true;
    private String language = "en";
	
//Constructors
	
	//It's private so that no other instances can be created 
	private AML()
	{
        //Initialize the URIMap and RelationshipMap
		uris = new URIMap();
		rels = new RelationshipMap();
		dir = "";
		try
		{
			File start = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
			start = start.getParentFile();
			if(start != null)
				dir = start.getAbsolutePath() + "/";
		}
		catch(Exception e)
		{
			//Do nothing
		}
	}

//Public Methods

	/**
	 * Constructs a new QualityFlagger for the current Alignment
	 * @return the QualityFlagger
	 */
	public QualityFlagger buildQualityFlagger()
	{
		qf = new QualityFlagger();
		return qf;
	}
	
	/**
	 * Constructs a new RepairMap for the current Alignment
	 * @return the RepairMap
	 */
	public RepairMap buildRepairMap()
    {
		rep = new RepairMap();
		return rep;
    }
	
	/**
	 * Closes the active alignment (GUI)
	 */
	public void closeAlignment()
    {
    	a = null;
    	activeMapping = -1;
    	evaluation = null;
    	if(userInterface != null)
    		userInterface.refresh();
    	needSave = false;
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
    	closeAlignment();
    }
    
    /**
     * Sets up the default matching configuration for the ontologies
     */
    public void defaultConfig()
    {
		bkSources = new Vector<String>();		
		File ontRoot = new File(dir + BK_PATH);
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
		matchSteps = new Vector<MatchStep>();
		if(lang.equals(LanguageSetting.TRANSLATE))
			matchSteps.add(MatchStep.TRANSLATE);
		matchSteps.add(MatchStep.LEXICAL);
		if(lang.equals(LanguageSetting.SINGLE))
			matchSteps.add(MatchStep.BK);
		if(!size.equals(SizeCategory.HUGE))
			matchSteps.add(MatchStep.WORD);
		matchSteps.add(MatchStep.STRING);
		if(size.equals(SizeCategory.SMALL) || size.equals(SizeCategory.MEDIUM))
			matchSteps.add(MatchStep.STRUCT);
		double sourceRatio = (source.dataPropertyCount() + source.objectPropertyCount()) * 1.0 / source.classCount();
		double targetRatio = (target.dataPropertyCount() + target.objectPropertyCount()) * 1.0 / target.classCount();
		if(sourceRatio >= 0.05 && targetRatio >= 0.05)
			matchSteps.add(MatchStep.PROPERTY);
		if(size.equals(SizeCategory.HUGE))
			matchSteps.add(MatchStep.OBSOLETE);
		matchSteps.add(MatchStep.SELECT);
		matchSteps.add(MatchStep.REPAIR);
		hierarchic = true;
		wms = WordMatchStrategy.AVERAGE;
		ssm = StringSimMeasure.ISUB;
		primaryStringMatcher = size.equals(SizeCategory.SMALL);
		nss = NeighborSimilarityStrategy.DESCENDANTS;
		directNeighbors = false;
		sType = SelectionType.getSelectionType();
		structuralSelection = size.equals(SizeCategory.HUGE);
		flagSteps = new Vector<Problem>();
		for(Problem f : Problem.values())
			flagSteps.add(f);
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
			userInterface.refresh();
		}
		else
			evaluation = "Precision\tRecall\tF-measure\tFound\tCorrect\tReference\n" + prc +
					"\t" + rec + "\t" + fms + "\t" + found + "\t" + correct + "\t" + total;
	}
	
    /**
     * Filters problem mappings in the active alignment
     */
    public void filter()
    {
   		CustomFilterer.filter();
    	if(userInterface != null)
    		userInterface.refresh();
    	needSave = true;
    }
    
    /**
     * Flags problem mappings in the active alignment
     */
    public void flag()
    {
   		CustomFlagger.flag();
    	if(userInterface != null)
    		userInterface.refresh();
    	needSave = true;
    }
    
    /**
     * @return the index of the active Mapping
     */
    public int getActiveMapping()
    {
    	return activeMapping;
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
     * @return the maximum class edge-distance to plot in the Mapping Viewer
     */
	public int getClassDistance()
    {
		return classDistance;
	}
	
    /**
     * @return the evaluation of the current alignment
     */
    public String getEvaluation()
    {
    	return evaluation;
    }
    
	/**
	 * @return the selected flagging steps
	 */
	public Vector<Problem> getFlagSteps()
	{
		return flagSteps;
	}

	/**
     * @return the maximum individual edge-distance to plot in the Mapping Viewer
     */
	public int getIndividualDistance()
    {
		return individualDistance;
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
		if(im == null)
			im = new InteractionManager();
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
	 * @return the selected matching steps
	 */
	public Vector<MatchStep> getMatchSteps()
	{
		return matchSteps;
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
	 * @return the Path to AML's directory
	 */
	public String getPath()
	{
		return dir;
	}

	public QualityFlagger getQualityFlagger()
	{
		return qf;
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
	 * @return the RepairMap
	 */
	public RepairMap getRepairMap()
	{
		return rep;
	}
	
	/**
	 * @return the selected background knowledge sources
	 */
	public Vector<String> getSelectedBKSources()
	{
		return selectedSources;
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
    
    public void goTo(int index)
    {
    	activeMapping = index;
    	userInterface.goTo(activeMapping);
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
    
    public boolean isHierarchic()
    {
		return hierarchic;
	}

    /**
     * Matches the active ontologies using the default configuration
     */
    public void matchAuto()
    {
    	defaultConfig();
    	im = new InteractionManager();
   		AutomaticMatcher.match();
    	evaluation = null;
    	rep = null;
    	if(a.size() > 0)
    		activeMapping = 0;
    	if(userInterface != null)
    		userInterface.refresh();
    	needSave = true;
    }

    /**
     * Matches the active ontologies using manual configurations
     */
    public void matchManual()
    {
    	im = new InteractionManager();
   		ManualMatcher.match();
    	evaluation = null;
    	rep = null;
    	if(a.size() > 0)
    		activeMapping = 0;
    	if(userInterface != null)
    		userInterface.refresh();
    	needSave = true;
    }
    
    /**
     * @return whether the active alignment needs saving
     */
    public boolean needSave()
    {
    	return needSave;
    }
    
    /**
     * Sets whether the active alignment needs saving
     * @param s: the value to set
     */
    public void needSave(boolean s)
    {
    	needSave = s;
    }
    
    /**
     * Opens an alignment from a file as the active alignment
     * @param path: the path to the alignment file
     * @throws Exception if the alignment file can't be read or interpreted
     */
    public void openAlignment(String path) throws Exception
    {
    	a = new Alignment(path);
    	evaluation = null;
    	if(a.size() > 0)
    		activeMapping = 0;
    	if(userInterface != null)
    		userInterface.refresh();
    	needSave = false;
    }
	
	public void openBKOntology(String name) throws OWLOntologyCreationException
	{
		long time = System.currentTimeMillis()/1000;
		if(bk != null)
			bk.close();
		bk = new BKOntology(dir + BK_PATH + name);
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
			PropertyConfigurator.configure(dir + LOG);
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
    	activeMapping = -1;
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
			PropertyConfigurator.configure(dir + LOG);
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
    	activeMapping = -1;
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
    
    public boolean primaryStringMatcher()
    {
		return primaryStringMatcher;
	}
    
    public void refreshMapping(int index)
    {
    	userInterface.refresh(index);
    }
    
    public void refreshGUI()
    {
    	userInterface.refresh();
    }

	public void removeIncorrect()
	{
		Alignment reviewed = new Alignment();
		for(Mapping m : a)
			if(!m.getStatus().equals(MappingStatus.INCORRECT))
				reviewed.add(m);
		if(a.size() > reviewed.size())
		{
			aml.setAlignment(reviewed);
			if(a.size() > 0)
				activeMapping = 0;
			if(userInterface != null)
				userInterface.refresh();
			needSave = true;
		}
	}
	
	public void repair()
	{
		im = new InteractionManager();
		rep = new RepairMap();
		Repairer r = new Repairer();
		r.filter();
		needSave = true;
	}

    public void saveAlignmentRDF(String file) throws Exception
    {
    	a.saveRDF(file);
    	needSave = false;
    }
    
    public void saveAlignmentTSV(String file) throws Exception
    {
    	a.saveTSV(file);
    	needSave = false;
    }
    
	public void setAlignment(Alignment maps)
	{
		a = maps;
		if(a.size() > 0)
			activeMapping = 0;
		qf = null;
    	evaluation = null;
    	rep = null;
    	needSave = false;
	}
	
	public void setDirectNeighbors(boolean directNeighbors)
	{
		this.directNeighbors = directNeighbors;
	}

	public void setFlagSteps(Vector<Problem> steps)
	{
		flagSteps = steps;
	}
	
	public void setHierarchic(boolean hierarchic)
	{
		this.hierarchic = hierarchic;
	}
	
	public void setLabelLanguage(String language)
	{
		this.language = language;
	}
	
	public void setLanguageSetting()
	{
		lang = LanguageSetting.getLanguageSetting();
	}
	
	public void setMatchSteps(Vector<MatchStep> steps)
	{
		matchSteps = steps;
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
	
	public void setSelectedSources(Vector<String> sources)
	{
		selectedSources = sources;
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

	public void setViewOptions(boolean a, boolean d, int c, int i)
	{
		showAncestors = a;
		showDescendants = d;
		classDistance = c;
		individualDistance = i;
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
	
	public void sortAscending()
	{
		a.sortAscending();
		if(a.size() > 0)
			activeMapping = 0;
		if(userInterface != null)
			userInterface.refresh();
	}

	public void sortDescending()
	{
		a.sortDescending();
		if(a.size() > 0)
			activeMapping = 0;
		if(userInterface != null)
			userInterface.refresh();
	}

	public void startGUI()
	{
        //Set the Nimbus look and feel
        try
        {
            for(UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels())
            {
                if ("Nimbus".equals(info.getName()))
                {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
            UIManager.put("control", AMLColor.WHITE);
            UIManager.put("background", AMLColor.WHITE);
            UIManager.put("scrollbar", AMLColor.LIGHT_GRAY);
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