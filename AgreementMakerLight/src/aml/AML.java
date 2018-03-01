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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import javax.swing.UIManager;

import org.apache.log4j.PropertyConfigurator;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import aml.ext.ParenthesisExtender;
import aml.ext.StopWordExtender;
import aml.filter.CustomFilterer;
import aml.filter.CustomFlagger;
import aml.filter.QualityFlagger;
import aml.filter.RepairMap;
import aml.filter.Repairer;
import aml.knowledge.Dictionary;
import aml.knowledge.MediatorOntology;
import aml.match.ManualMatcher;
import aml.match.Mapping;
import aml.match.UnsupportedEntityTypeException;
import aml.match.Alignment;
import aml.match.AutomaticMatcher;
import aml.ontology.Ontology;
import aml.ontology.RelationshipMap;
import aml.ontology.URIMap;
import aml.settings.Problem;
import aml.settings.EntityType;
import aml.settings.InstanceMatchingCategory;
import aml.settings.LanguageSetting;
import aml.settings.MappingStatus;
import aml.settings.MatchStep;
import aml.settings.NeighborSimilarityStrategy;
import aml.settings.SelectionType;
import aml.settings.SizeCategory;
import aml.settings.StringSimMeasure;
import aml.settings.WordMatchStrategy;
import aml.ui.AMLColor;
import aml.ui.AlignmentFileChooser;
import aml.ui.GUI;
import aml.ui.OntologyFileChooser;
import aml.util.ExtensionFilter;
import aml.util.InteractionManager;
import aml.util.Similarity;

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
	private Ontology source;
	private Ontology target;
	private MediatorOntology bk;
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
	private boolean useReasoner = false;
	private boolean matchSameURI = false;
	private boolean matchClasses;
	private boolean matchIndividuals;
	private boolean matchProperties;
	private final String LOG = "log4j.properties";
	private final String BK_PATH = "store/knowledge/";
	private Vector<String> bkSources; //The list of files under the BK_PATH
	private Set<Integer> sourceIndividualsToMatch;
	private Set<Integer> targetIndividualsToMatch;
	private InstanceMatchingCategory inst;
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
		}
		else
		{
			System.out.println("WARNING: 'store/knowledge' directory not found!");
		}
		selectedSources = new Vector<String>(bkSources);
		
		//Custom Match
		matchSteps = new Vector<MatchStep>();
		for(MatchStep s : MatchStep.values())
			matchSteps.add(s);
		
		matchClasses = hasClasses();
		double sourceRatio = (source.count(EntityType.DATA) + source.count(EntityType.OBJECT)) * 1.0 / source.count(EntityType.CLASS);
		double targetRatio = (target.count(EntityType.DATA) + target.count(EntityType.OBJECT)) * 1.0 / target.count(EntityType.CLASS);
		matchProperties = hasProperties() && sourceRatio >= 0.05 && targetRatio >= 0.05;
		sourceRatio = source.count(EntityType.INDIVIDUAL) * 1.0 / source.count(EntityType.CLASS);
		targetRatio = target.count(EntityType.INDIVIDUAL) * 1.0 / target.count(EntityType.CLASS);
		matchIndividuals = hasIndividuals() && sourceRatio >= 0.25 && targetRatio >= 0.25;
		if(matchIndividuals)
		{
			inst = InstanceMatchingCategory.DIFFERENT_ONTOLOGIES;
			double share = Similarity.jaccard(source.getEntities(EntityType.CLASS),
					target.getEntities(EntityType.CLASS));
			if(share >= 0.5)
			{
				inst = InstanceMatchingCategory.SAME_ONTOLOGY;
				matchClasses = false;
				matchProperties = false;
			}
			else if(sourceRatio > 1 && targetRatio > 1)
			{
				matchClasses = false;
				matchProperties = false;
			}
			sourceIndividualsToMatch = source.getEntities(EntityType.INDIVIDUAL);
			targetIndividualsToMatch = target.getEntities(EntityType.INDIVIDUAL);
		}
		
    	size = SizeCategory.getSizeCategory();
    	if(size.equals(SizeCategory.HUGE))
    		threshold = 0.7;
    	lang = LanguageSetting.getLanguageSetting();
		languages = new HashSet<String>();
		for(String s : source.getLexicon().getLanguages())
			if(target.getLexicon().getLanguages().contains(s))
				languages.add(s);
		if(languages.contains("en") || languages.size() == 0)
			language = "en";
		else
			language = languages.iterator().next();
		wms = WordMatchStrategy.AVERAGE;
		ssm = StringSimMeasure.ISUB;
		nss = NeighborSimilarityStrategy.DESCENDANTS;
		directNeighbors = false;
		sType = SelectionType.getSelectionType();
		structuralSelection = size.equals(SizeCategory.HUGE);
		flagSteps = new Vector<Problem>();
		for(Problem f : Problem.values())
			flagSteps.add(f);
		readConfigFile();
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
	public MediatorOntology getBKOntology()
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

	public double getIndividualConnectivity()
	{
		double connectivity = Math.min(rels.getIndividualsWithPassiveRelations().size(),
				rels.getIndividualsWithActiveRelations().size());
		connectivity /= source.count(EntityType.INDIVIDUAL) + target.count(EntityType.INDIVIDUAL);
		return connectivity;
	}

	/**
     * @return the maximum individual edge-distance to plot in the Mapping Viewer
     */
	public int getIndividualDistance()
    {
		return individualDistance;
	}
	
	/**
	 * @return the number of Data and Annotation properties in the ValueMaps
	 */
	public double getIndividualValueDensity()
	{
		return Math.min(source.getValueMap().size()*1.0/source.count(EntityType.INDIVIDUAL),
				target.getValueMap().size()*1.0/target.count(EntityType.INDIVIDUAL));
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
	public Ontology getSource()
	{
		return source;
	}

	/**
	 * @return the set of individuals of the source ontology to match
	 */
	public Set<Integer> getSourceIndividualsToMatch()
	{
		return sourceIndividualsToMatch;
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
	public Ontology getTarget()
	{
		return target;
	}
 
	/**
	 * @return the set of individuals of the target ontology to match
	 */
    public Set<Integer> getTargetIndividualsToMatch()
    {
		return targetIndividualsToMatch;
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
    		source.count(EntityType.CLASS) > 0 && target.count(EntityType.CLASS) > 0;
    }
    
    /**
     * @return whether there are individuals in both active ontologies
     */
    private boolean hasIndividuals()
    {
    	return hasOntologies() &&
        		source.count(EntityType.INDIVIDUAL) > 0 && target.count(EntityType.INDIVIDUAL) > 0;
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
   			((source.count(EntityType.DATA) > 0 && target.count(EntityType.DATA) > 0) ||
   			(source.count(EntityType.OBJECT) > 0 && target.count(EntityType.OBJECT) > 0));
    }
    
    /**
     * @return whether there are cross-references in at least one ontology
     */
    public boolean hasReferences()
    {
    	return hasOntologies() &&
   			(source.getReferenceMap().size() > 0 || target.getReferenceMap().size() > 0);
    }
    
    public boolean isHierarchic()
    {
		return hierarchic;
	}
    
    public boolean isToMatchSource(int index)
    {
    	return sourceIndividualsToMatch.contains(index);
	}

    public boolean isToMatchTarget(int index)
    {
    	return targetIndividualsToMatch.contains(index);
	}
    
    /**
     * Matches the active ontologies using the default configuration
     */
    public void matchAuto()
    {
    	im = new InteractionManager();
   		try
   		{
			AutomaticMatcher.match();
		}
   		catch(UnsupportedEntityTypeException e)
   		{
			e.printStackTrace();
		}
    	evaluation = null;
    	rep = null;
    	if(a.size() > 0)
    		activeMapping = 0;
    	if(userInterface != null)
    		userInterface.refresh();
    	needSave = true;
    }
    
    /**
     * @return whether class matching is on
     */
    public boolean matchClasses()
    {
    	return matchClasses;
    }
    
    /**
     * Sets the matchClasses parameter that determines
     * whether ontology classes will be matched
     * @param match: whether to match classes
     */
    public void matchClasses(boolean match)
    {
    	matchClasses = match;
    }
    
    /**
     * @return whether individual matching is on
     */
    public boolean matchIndividuals()
    {
    	return matchIndividuals;
    }
    
    /**
     * Sets the matchIndividuals parameter that determines
     * whether ontology individuals will be matched
     * @param match: whether to match individuals
     */
    public void matchIndividuals(boolean match)
    {
    	matchIndividuals = match;
    }

    /**
     * Matches the active ontologies using manual configurations
     */
    public void matchManual()
    {
    	im = new InteractionManager();
   		try
   		{
			ManualMatcher.match();
		}
   		catch(UnsupportedEntityTypeException e)
   		{
			e.printStackTrace();
		}
    	evaluation = null;
    	rep = null;
    	if(a.size() > 0)
    		activeMapping = 0;
    	if(userInterface != null)
    		userInterface.refresh();
    	needSave = true;
    }
    
    /**
     * @return whether property matching is on
     */
    public boolean matchProperties()
    {
    	return matchProperties;
    }
    
    /**
     * Sets the matchProperties parameter that determines
     * whether ontology properties will be matched
     * @param match: whether to match properties
     */
    public void matchProperties(boolean match)
    {
    	matchProperties = match;
    }
    
    /**
     * @return whether same URI matching is on
     */
    public boolean matchSameURI()
    {
    	return matchSameURI;
    }
    
    /**
     * Sets the matchSameURI parameter that determines
     * whether entities with the same URI will be matched
     * @param match: whether to match entities with the same URI
     */
    public void matchSameURI(boolean match)
    {
    	matchSameURI = match;
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
		System.out.println("Loading mediating ontology " + name);
		long time = System.currentTimeMillis()/1000;
		if(bk != null)
			bk.close();
		bk = new MediatorOntology(dir + BK_PATH + name);
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
		source = new Ontology(src);
		time = System.currentTimeMillis()/1000 - time;
		System.out.println(source.getURI() + " loaded in " + time + " seconds");
		System.out.println("Classes: " + source.count(EntityType.CLASS));
		System.out.println("Individuals: " + source.count(EntityType.INDIVIDUAL));
		System.out.println("Properties: " + (source.count(EntityType.DATA)+source.count(EntityType.OBJECT)));
		time = System.currentTimeMillis()/1000;
		System.out.println("Loading target ontology");
		target = new Ontology(tgt);
		time = System.currentTimeMillis()/1000 - time;
		System.out.println(target.getURI() + " loaded in " + time + " seconds");
		System.out.println("Classes: " + target.count(EntityType.CLASS));
		System.out.println("Individuals: " + target.count(EntityType.INDIVIDUAL));
		System.out.println("Properties: " + (target.count(EntityType.DATA)+target.count(EntityType.OBJECT)));
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
    	StopWordExtender sw = new StopWordExtender();
    	sw.extendLexicons();
    	ParenthesisExtender p = new ParenthesisExtender();
    	p.extendLexicons();
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
		source = new Ontology(src);
		time = System.currentTimeMillis()/1000 - time;
		System.out.println(source.getURI() + " loaded in " + time + " seconds");
		System.out.println("Classes: " + source.count(EntityType.CLASS));
		System.out.println("Individuals: " + source.count(EntityType.INDIVIDUAL));
		System.out.println("Properties: " + (source.count(EntityType.DATA)+source.count(EntityType.OBJECT)));
		time = System.currentTimeMillis()/1000;
		System.out.println("Loading target ontology");
		target = new Ontology(tgt);
		time = System.currentTimeMillis()/1000 - time;
		System.out.println(target.getURI() + " loaded in " + time + " seconds");
		System.out.println("Classes: " + target.count(EntityType.CLASS));
		System.out.println("Individuals: " + target.count(EntityType.INDIVIDUAL));
		System.out.println("Properties: " + (target.count(EntityType.DATA)+target.count(EntityType.OBJECT)));
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
    	StopWordExtender sw = new StopWordExtender();
    	sw.extendLexicons();
    	ParenthesisExtender p = new ParenthesisExtender();
    	p.extendLexicons();
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
    
	public void readConfigFile()
	{
		File conf = new File(dir + "store/config.ini");
		if(!conf.canRead())
		{
			System.out.println("Warning: config.ini file not found");
			System.out.println("Matching will proceed with default configuration");
		}
		else
		{
			try
			{
				System.out.println("Reading config.ini file");
				BufferedReader in = new BufferedReader(new FileReader(conf));
				String line;
				while((line=in.readLine()) != null)
				{
					if(line.startsWith("#") || line.isEmpty())
						continue;
					String[] option = line.split("=");
					option[0] = option[0].trim();
					option[1] = option[1].trim();
					if(option[0].equals("match_classes"))
						matchClasses = option[1].equalsIgnoreCase("true");
					else if(option[0].equals("match_individuals"))
						matchIndividuals = option[1].equalsIgnoreCase("true");
					else if(option[0].equals("match_properties"))
						matchProperties = option[1].equalsIgnoreCase("true");
					else if(option[0].equals("threshold"))
						threshold = Double.parseDouble(option[1]);
					else if(option[0].equals("class_correspondence"))
					{
						if(option[1].equalsIgnoreCase("true"))
							inst = InstanceMatchingCategory.SAME_CLASSES;
					}
					else if(option[0].equals("use_reasoner"))
						useReasoner = option[1].equalsIgnoreCase("true");
					else if(option[0].equals("match_same_uri"))
						matchSameURI = option[1].equalsIgnoreCase("true");
					else if(option[0].equals("source_classes"))
					{
						if(option[1].equals(""))
							continue;
						HashSet<String> toMatch = new HashSet<String>();
						String[] iris = option[1].split(";");
						for(String i : iris)
							toMatch.add(i);
						setSourceClassesToMatch(toMatch);
					}
					else if(option[0].equals("target_classes"))
					{
						if(option[1].equals(""))
							continue;
						HashSet<String> toMatch = new HashSet<String>();
						String[] iris = option[1].split(";");
						for(String i : iris)
							toMatch.add(i);
						setTargetClassesToMatch(toMatch);
					}
				}
				in.close();
			}
			catch(Exception e)
			{
				System.out.println("Error: Could not read config file");
				e.printStackTrace();
				System.out.println("Matching will proceed with default configuration");
			}
		}
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
	
	public InstanceMatchingCategory getInstanceMatchingCategory()
	{
		return inst;
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

	public void setOntologies(Ontology s, Ontology t)
	{
		source = s;
		target = t;
		defaultConfig();
	}
	
	public void setPrimaryStringMatcher(boolean primary)
	{
		primaryStringMatcher = primary;
	}
	
	public void setInstanceMatchingCategory(InstanceMatchingCategory cat)
	{
		inst = cat;
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
	
	/**
	 * Sets the set of classes of the source ontology to which the individuals
	 * to match belong to
	 * @param sourcesToMatch: the set of source classes to match
	 */
	public void setSourceClassesToMatch(Set<String> sourcesToMatch)
	{
		sourceIndividualsToMatch = new HashSet<Integer>();
		HashSet<Integer> sourceClasses = new HashSet<Integer>();
		for(String s : sourcesToMatch)
		{
			int id = source.getIndex(s);
			if(id != -1)
				sourceClasses.add(id);
		}
		for(Integer i : source.getEntities(EntityType.INDIVIDUAL))
		{
			for(Integer c : sourceClasses)
			{
				if(rels.belongsToClass(i, c))
				{
					sourceIndividualsToMatch.add(i);
					break;
				}
			}
		}
	}
	
	public void setStringSimMeasure(StringSimMeasure ssm)
	{
		this.ssm = ssm;
	}
	
	public void setStructuralSelection(boolean structuralSelection)
	{
		this.structuralSelection = structuralSelection;
	}

	/**
	 * Sets the set of classes of the target ontology to which the individuals
	 * to match belong to
	 * @param targetsToMatch: the set of target classes to match
	 */	
	public void setTargetClassesToMatch(Set<String> targetsToMatch)
	{
		targetIndividualsToMatch = new HashSet<Integer>();
		HashSet<Integer> targetClasses = new HashSet<Integer>();
		for(String s : targetsToMatch)
		{
			int id = target.getIndex(s);
			if(id != -1)
				targetClasses.add(id);
		}
		for(Integer i : target.getEntities(EntityType.INDIVIDUAL))
		{
			for(Integer c : targetClasses)
			{
				if(rels.belongsToClass(i, c))
				{
					targetIndividualsToMatch.add(i);
					break;
				}
			}
		}
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
		if(!sLangs.contains("en") && !tLangs.contains("en"))
		{
			for(String l1 : sLangs)
			{
				Dictionary d = new Dictionary(l1,"en");
				d.translateLexicon(source.getLexicon());
			}
			for(String l2 : tLangs)
			{
				Dictionary d = new Dictionary(l2,"en");
				d.translateLexicon(target.getLexicon());
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