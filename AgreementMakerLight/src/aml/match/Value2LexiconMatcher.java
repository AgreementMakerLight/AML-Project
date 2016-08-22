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
* Matching algorithm that maps individuals by comparing the Lexicon entries   *
* of one with the ValueMap entries of the other using a combination of        *
* String- and Word-Matching algorithms and optionally the WordNet.            *
*                                                                             *
* @author Daniel Faria, Catia Pesquita                                        *
******************************************************************************/
package aml.match;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import aml.AML;
import aml.knowledge.WordNet;
import aml.match.Alignment;
import aml.match.Mapping;
import aml.ontology.Lexicon;
import aml.ontology.Ontology;
import aml.ontology.ValueMap;
import aml.settings.EntityType;
import aml.util.ISub;
import aml.util.Similarity;
import aml.util.Table2Set;

public class Value2LexiconMatcher implements PrimaryMatcher, Rematcher
{
	
//Attributes
	
	private static final String DESCRIPTION = "Matches individuals by comparing the Lexicon\n" +
											  "entries of one to the ValueMap entries of the\n" +
											  "other using a combination of string- and word-\n" +
											  "matching algorithms, and optionally the WordNet";
	private static final String NAME = "Value-to-Lexicon Matcher";
	private static final EntityType[] SUPPORT = {EntityType.INDIVIDUAL};
	private AML aml;
	private Ontology source;
	private Ontology target;
	private Lexicon sLex;
	private Lexicon tLex;
	private ValueMap sVal;
	private ValueMap tVal;
	private WordNet wn = null;
	//The available CPU threads
	private int threads;
	
//Constructors
	
	public Value2LexiconMatcher(boolean useWordNet)
	{
		if(useWordNet)
			wn = new WordNet();
		aml = AML.getInstance();
		source = aml.getSource();
		target = aml.getTarget();
		sLex = source.getLexicon();
		tLex = target.getLexicon();
		sVal = source.getValueMap();
		tVal = target.getValueMap();
		threads = Runtime.getRuntime().availableProcessors();
	}
	
//Public Methods
	
	@Override
	public String getDescription()
	{
		return DESCRIPTION;
	}

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public EntityType[] getSupportedEntityTypes()
	{
		return SUPPORT;
	}
	
	@Override
	public Alignment match(EntityType e, double thresh) throws UnsupportedEntityTypeException
	{
		checkEntityType(e);
		System.out.println("Running Value-to-Lexicon Matcher");
		long time = System.currentTimeMillis()/1000;
		Set<Integer> sources = sLex.getEntities(e);
		Set<Integer> targets = tLex.getEntities(e);
		Alignment a = new Alignment();
		for(Integer i : sources)
		{
			Table2Set<Integer,Integer> toMap = new Table2Set<Integer,Integer>();
			for(Integer j : targets)
				toMap.add(i,j);
			a.addAll(mapInParallel(toMap,thresh));
		}
		time = System.currentTimeMillis()/1000 - time;
		System.out.println("Finished in " + time + " seconds");
		return a;
	}
		
	@Override
	public Alignment rematch(Alignment a, EntityType e) throws UnsupportedEntityTypeException
	{
		checkEntityType(e);
		System.out.println("Computing String Similarity");
		long time = System.currentTimeMillis()/1000;
		Alignment maps = new Alignment();
		Table2Set<Integer,Integer> toMap = new Table2Set<Integer,Integer>();
		for(Mapping m : a)
		{
			if(aml.getURIMap().getType(m.getSourceId()).equals(e))
				toMap.add(m.getSourceId(),m.getTargetId());
		}
		maps.addAll(mapInParallel(toMap,0.0));
		time = System.currentTimeMillis()/1000 - time;
		System.out.println("Finished in " + time + " seconds");
		return maps;
	}
	
//Private Methods
	
	private void checkEntityType(EntityType e) throws UnsupportedEntityTypeException
	{
		boolean check = false;
		for(EntityType t : SUPPORT)
		{
			if(t.equals(e))
			{
				check = true;
				break;
			}
		}
		if(!check)
			throw new UnsupportedEntityTypeException(e.toString());
	}
	
	//Maps a table of classes in parallel, using all available threads
	private Alignment mapInParallel(Table2Set<Integer,Integer> toMap, double thresh)
	{
		Alignment maps = new Alignment();
		ArrayList<MappingTask> tasks = new ArrayList<MappingTask>();
		for(Integer i : toMap.keySet())
			for(Integer j : toMap.get(i))
				tasks.add(new MappingTask(i,j));
        List<Future<Mapping>> results;
		ExecutorService exec = Executors.newFixedThreadPool(threads);
		try
		{
			results = exec.invokeAll(tasks);
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
	        results = new ArrayList<Future<Mapping>>();
		}
		exec.shutdown();
		for(Future<Mapping> fm : results)
		{
			try
			{
				Mapping m = fm.get();
				if(m.getSimilarity() >= thresh)
					maps.add(m);
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
		return maps;
	}
	
	//Computes the maximum String similarity between two Classes by doing a
	//pairwise comparison of all their names
	private double mapTwoEntities(int sId, int tId)
	{
		double crossSim = 0;
		for(String n1 : sLex.getNames(sId))
			for(Integer td : tVal.getProperties(tId))
				for(String tv : tVal.getValues(tId,td))
					crossSim = Math.max(crossSim,nameSimilarity(n1,tv));
		for(String n2 : tLex.getNames(tId))
			for(Integer sd : sVal.getProperties(sId))
				for(String sv : sVal.getValues(sId,sd))
					crossSim = Math.max(crossSim,nameSimilarity(n2,sv));
		return crossSim;
	}
	
	//Callable class for mapping two classes
	private class MappingTask implements Callable<Mapping>
	{
		private int source;
		private int target;
		
		MappingTask(int s, int t)
	    {
			source = s;
	        target = t;
	    }
	        
	    @Override
	    public Mapping call()
	    {
       		return new Mapping(source,target,mapTwoEntities(source,target));
        }
	}
	
	//Computes the similarity between two property names using a Jaccard
	//index between their words. When using WordNet, the WordNet similarity
	//is given by the Jaccard index between all WordNet synonyms, and is
	//returned instead of the name similarity if it is higher
	private double nameSimilarity(String n1, String n2)
	{
		//Check if the names are equal
		if(n1.equals(n2))
			return 1.0;
		
		//Split the source name into words
		String[] sW = n1.split(" ");
		HashSet<String> sWords = new HashSet<String>();
		HashSet<String> sSyns = new HashSet<String>();
		for(String w : sW)
		{
			sWords.add(w);
			sSyns.add(w);
			//And compute the WordNet synonyms of each word
			if(wn != null && w.length() > 2)
				sSyns.addAll(wn.getAllNounWordForms(w));
		}
		
		//Split the target name into words
		String[] tW = n2.split(" ");
		HashSet<String> tWords = new HashSet<String>();
		HashSet<String> tSyns = new HashSet<String>();		
		for(String w : tW)
		{
			tWords.add(w);
			tSyns.add(w);
			//And compute the WordNet synonyms of each word
			if(wn != null && w.length() > 3)
				tSyns.addAll(wn.getAllWordForms(w));
		}
		
		//Compute the Jaccard word similarity between the properties
		double wordSim = Similarity.jaccard(sWords,tWords)*0.9;
		//and the String similarity
		double simString = ISub.stringSimilarity(n1,n2)*0.9;
		//Combine the two
		double sim = 1 - ((1-wordSim) * (1-simString));
		if(wn != null)
		{
			//Check if the WordNet similarity
			double wordNetSim = Similarity.jaccard(sSyns,tSyns);
			//Is greater than the name similarity
			if(wordNetSim > sim)
				//And if so, return it
				sim = wordNetSim;
		}
		return sim;
	}
}