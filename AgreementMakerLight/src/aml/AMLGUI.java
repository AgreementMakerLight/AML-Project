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
* Runs the AgreementMakerLight Graphic User Interface.                        *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 06-02-2014                                                            *
******************************************************************************/
package aml;

import java.io.File;
import java.io.FileFilter;
import java.util.Vector;

import org.apache.log4j.PropertyConfigurator;

import aml.match.AMLMatcher;
import aml.match.Alignment;
import aml.match.LexicalMatcher;
import aml.match.Mapping;
import aml.match.MatchingConfigurations.MatchingAlgorithm;
import aml.match.MatchingConfigurations.SelectionType;
import aml.match.OAEI2013Matcher;
import aml.match.Selector;
import aml.ontology.Ontology;
import aml.ui.AlignmentFileChooser;
import aml.ui.OntologyFileChooser;
import aml.ui.GUI;
import aml.util.ExtensionFilter;

public class AMLGUI
{

//Attributes
	
	//The user interface
	private static GUI userInterface;
	//The file choosers for the GUI
	private static OntologyFileChooser ofc;
	private static AlignmentFileChooser afc;
	//The path to the background knowledge directory and the list of sources
	private static final String BK_PATH = "store/knowledge/";
	private static Vector<String> bkSources;
	//The open ontologies and alignment
	private static Ontology source;
	private static Ontology target;
	private static Alignment a;
	//The evaluation of the alignment
	private static String evaluation;
	//The index of the mapping currently being viewed
	private static int currentMapping = -1;
	//Mapping view properties
	private static int maxDistance = 2;
	private static boolean showAncestors = true;
	private static boolean showDescendants = true;
	//Matching properties
    private static MatchingAlgorithm matcher = MatchingAlgorithm.AML;
    private static SelectionType sType = SelectionType.AUTO;
    private static boolean useBK = false;
    private static boolean ignoreUMLS = true;
    private static boolean repairAlignment = false;
    private static boolean matchProperties = false;
    private static Vector<String> selectedSources;
	private static double threshold = 0.6;

//Main Method	
	
    public static void main(String args[])
    {
		//Configure log4j (writes to store/error.log)
		try
		{
			PropertyConfigurator.configure("log4j.properties");
		}
		catch(Exception e)
		{
			System.out.println("Warning: Could not configure log4j.properties");
			e.printStackTrace();
		}
		
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

        bkSources = listBKSources();
        selectedSources = new Vector<String>(0,1);
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
    
//Public Methods (static functions called from the GUI)
    
    public static void closeAlignment()
    {
    	a = null;
    	currentMapping = -1;
    	userInterface.refresh();
    }
    
    public static void closeOntologies()
    {
    	source = null;
    	target = null;
    	a = null;
    	currentMapping = -1;
    	userInterface.refresh();
    }
    
	public static void evaluate(Alignment ref)
	{
		int found = a.size();		
		int correct = a.evaluate(ref);
		int total = ref.size();

		double precision = 1.0*correct/found;
		String prc = Math.round(precision*1000)/10.0 + "%";
		double recall = 1.0*correct/total;
		String rec = Math.round(recall*1000)/10.0 + "%";
		double fmeasure = 2*precision*recall/(precision+recall);
		String fms = Math.round(fmeasure*1000)/10.0 + "%";
		
		evaluation = "Precision: " + prc + "; Recall: " + rec + "; F-measure: " + fms;
		userInterface.refreshPanel();
	}
    
	public static Alignment getAlignment()
    {
    	return a;
    }
	
    public static AlignmentFileChooser getAlignmentFileChooser()
    {
    	return afc;
    }
	
	public static Vector<String> getBKSources()
	{
		return bkSources;
	}

    public static int getCurrentIndex()
    {
   		return currentMapping;
    }
    
    public static Mapping getCurrentMapping()
    {
    	if(currentMapping == -1)
    		return null;
    	else
    		return a.get(currentMapping);
    }
    
    public static String getEvaluation()
    {
    	return evaluation;
    }
    
    public static MatchingAlgorithm getMatcher()
    {
    	return matcher;
    }

    public static int getMaxDistance()
    {
		return maxDistance;
	}
    
    public static OntologyFileChooser getOntologyFileChooser()
    {
    	return ofc;
    }
    
	public static Vector<String> getSelectedBKSources()
	{
		return selectedSources;
	}
    
	public static SelectionType getSelectionType()
	{
		return sType;
	}
	
    public static Ontology getSourceOntology()
    {
    	return source;
    }
    
    public static Ontology getTargetOntology()
    {
    	return target;
    }
    
    public static double getThreshold()
    {
    	return threshold;
    }
    
    public static void goTo(int index)
    {
   		currentMapping = index;
   		userInterface.refreshPanel();
   		userInterface.refreshGraph();
    }
    
    public static boolean hasAlignment()
    {
    	return a != null && a.termMappingCount() > 0;
    }
    
    public static boolean hasOntologies()
    {
    	return source != null && target != null;
    }
    
    public static boolean ignoreUMLS()
    {
    	return ignoreUMLS;
    }
    
    public static void match()
    {
    	if(matcher.equals(MatchingAlgorithm.AML))
    	{
    		AMLMatcher m = new AMLMatcher(selectedSources,sType,matchProperties,repairAlignment);
    		a = m.match(source,target,threshold);
    	}
    	else if(matcher.equals(MatchingAlgorithm.OAEI))
    	{
    		OAEI2013Matcher m = new OAEI2013Matcher(useBK,ignoreUMLS,repairAlignment);
    		a = m.match(source,target);
    	}
    	else if(matcher.equals(MatchingAlgorithm.LEXICAL))
    	{
    		LexicalMatcher m = new LexicalMatcher(true);
    		a = m.match(source,target,threshold);
    		a = Selector.select(a, threshold, sType);
    	}
    	if(a.size() >= 1)
    		currentMapping = 0;
    	else
    		currentMapping = -1;
    	evaluation = null;
    	userInterface.refresh();
    }
    
    public static boolean matchProperties()
    {
    	return matchProperties;
    }
    
    public static void nextMapping()
    {
    	if(currentMapping == a.termMappingCount()-1)
    		currentMapping = 0;
    	else
    		currentMapping++;
		userInterface.refreshPanel();
		userInterface.refreshGraph();
    }
    
    public static void previousMapping()
    {
    	if(currentMapping == 0)
    		currentMapping = a.termMappingCount()-1;
    	else
    		currentMapping--;
		userInterface.refreshPanel();
		userInterface.refreshGraph();
    }
    
    public static boolean repairAlignment()
    {
    	return repairAlignment;
    }
    
    public static void setAlignment(Alignment al)
    {
    	a = al;
    	if(a.size() >= 1)
    		currentMapping = 0;
    	else
    		currentMapping = -1;
    	evaluation = null;
    	userInterface.refresh();
    }
    
	public static void setMatchOptions(SelectionType s, double thresh)
	{
	    sType = s;
	    threshold = thresh;
	}
    
	public static void setMatchOptions(boolean bk, boolean um, boolean re)
	{
		useBK = bk;
	    ignoreUMLS = um;
	    repairAlignment = re;
	}
	
	public static void setMatchOptions(boolean pr, boolean re, Vector<String> bk, SelectionType s, double thresh)
	{
	    matchProperties = pr;
	    repairAlignment = re;
	    selectedSources = bk;
	    sType = s;
	    threshold = thresh;
	}
	
	public static void setMatcher(MatchingAlgorithm m)
	{
		matcher = m;
	}

    public static void setOntologies(Ontology s, Ontology t)
    {
    	//Set the ontologies
    	source = s;
    	target = t;
    	//Reset the alignment, mapping, and evaluation
    	a = null;
    	currentMapping = -1;
    	evaluation = null;
    	//Refresh the user interface
    	userInterface.refresh();
    }

	public static void setViewOptions(boolean a, boolean d, int m)
	{
		showAncestors = a;
		showDescendants = d;
		maxDistance = m;
		userInterface.refreshGraph();
	}

	public static boolean showAncestors()
	{
		return showAncestors;
	}

	public static boolean showDescendants()
	{
		return showDescendants;
	}
	
	public static boolean useBK()
	{
		return useBK;
	}

//Private Methods
	
	private static Vector<String> listBKSources()
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
}