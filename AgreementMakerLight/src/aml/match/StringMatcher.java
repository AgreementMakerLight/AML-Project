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
* Matches Ontologies by measuring the maximum String similarity between their *
* classes, using one of the four available String similarity measures.        *
*                                                                             *
* WARNING: This matching algorithm takes O(N^2) time, and thus should be used *
* either to match small ontologies or as a SecondaryMatcher.                  *
*                                                                             *
* @authors Daniel Faria, Cosmin Stroe                                         *
******************************************************************************/
package aml.match;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import uk.ac.shef.wit.simmetrics.similaritymetrics.JaroWinkler;
import uk.ac.shef.wit.simmetrics.similaritymetrics.Levenshtein;
import uk.ac.shef.wit.simmetrics.similaritymetrics.QGramsDistance;
import aml.AML;
import aml.ontology.Lexicon;
import aml.ontology.Ontology;
import aml.ontology.RelationshipMap;
import aml.settings.LanguageSetting;
import aml.settings.LexicalType;
import aml.settings.StringSimMeasure;
import aml.util.ISub;
import aml.util.Table2Set;

public class StringMatcher implements SecondaryMatcher, PrimaryMatcher, Rematcher
{

//Attributes

	//Links to the AML class and to the source and target Lexicons
	private AML aml;
	private Ontology source;
	private Ontology target;
	private Lexicon sLex;
	private Lexicon tLex;
	//Language setting and languages
	private LanguageSetting lSet;
	private Set<String> languages;
	//Similarity measure
	private StringSimMeasure measure = StringSimMeasure.ISUB;
	//Correction factor (to make string similarity values comparable to word similarity values
	//and thus enable their combination and proper selection; 0.8 is optimized for the ISub measure)
	private final double CORRECTION = 0.80;
	//The available CPU threads
	private int threads;

//Constructors
	
	/**
	 * Constructs a new ParametricStringMatcher with default
	 * String similarity measure (ISub)
	 */
	public StringMatcher()
	{
		threads = Runtime.getRuntime().availableProcessors();
		aml = AML.getInstance();
		source = aml.getSource();
		target = aml.getTarget();
		sLex = source.getLexicon();
		tLex = target.getLexicon();
		lSet = AML.getInstance().getLanguageSetting();
		languages = aml.getLanguages();
	}

	/**
	 * Constructs a new ParametricStringMatcher with the given String similarity measure
	 * @args m: the string similarity measure
	 */
	public StringMatcher(StringSimMeasure m)
	{
		this();
		measure = m;
	}

//Public Methods
	
	@Override
	public Alignment extendAlignment(Alignment a, double thresh)
	{	
		System.out.println("Extending Alignment with String Matcher");
		long time = System.currentTimeMillis()/1000;
		System.out.println("Matching Children & Parents");
		Alignment ext = extendChildrenAndParents(a,thresh);
		Alignment aux = extendChildrenAndParents(ext,thresh);
		int size = 0;
		for(int i = 0; i < 10 && ext.size() > size; i++)
		{
			size = ext.size();
			for(Mapping m : aux)
				if(!a.containsConflict(m))
					ext.add(m);
			aux = extendChildrenAndParents(aux,thresh);
		}
		System.out.println("Matching Siblings");
		ext.addAll(extendSiblings(a,thresh));
		time = System.currentTimeMillis()/1000 - time;
		System.out.println("Finished in " + time + " seconds");
		return ext;
	}
	
	@Override
	public Alignment match(double thresh)
	{
		System.out.println("Running String Matcher");
		long time = System.currentTimeMillis()/1000;
		Set<Integer> sources = sLex.getClasses();
		Set<Integer> targets = tLex.getClasses();
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
	public Alignment rematch(Alignment a)
	{
		System.out.println("Computing String Similarity");
		long time = System.currentTimeMillis()/1000;
		Alignment maps = new Alignment();
		Table2Set<Integer,Integer> toMap = new Table2Set<Integer,Integer>();
		for(Mapping m : a)
		{
			toMap.add(m.getSourceId(),m.getTargetId());
		}
		maps.addAll(mapInParallel(toMap,0.0));
		time = System.currentTimeMillis()/1000 - time;
		System.out.println("Finished in " + time + " seconds");
		return maps;
	}
	
//Private Methods
	
	private Alignment extendChildrenAndParents(Alignment a, double thresh)
	{
		RelationshipMap rels = aml.getRelationshipMap();
		Table2Set<Integer,Integer> toMap = new Table2Set<Integer,Integer>();
		for(int i = 0; i < a.size(); i++)
		{
			Mapping input = a.get(i);
			if(!aml.getURIMap().isClass(input.getSourceId()))
				continue;
			Set<Integer> sourceChildren = rels.getChildren(input.getSourceId());
			Set<Integer> targetChildren = rels.getChildren(input.getTargetId());
			for(Integer s : sourceChildren)
			{
				if(a.containsSource(s) || !aml.getURIMap().isClass(s))
					continue;
				for(Integer t : targetChildren)
				{
					if(!a.containsTarget(t))
						toMap.add(s,t);
				}
			}
			Set<Integer> sourceParents = rels.getParents(input.getSourceId());
			Set<Integer> targetParents = rels.getParents(input.getTargetId());
			for(Integer s : sourceParents)
			{
				if(a.containsSource(s))
					continue;
				for(Integer t : targetParents)
				{
					if(!a.containsTarget(t))
						toMap.add(s, t);
				}
			}
		}
		return mapInParallel(toMap,thresh);
	}
	
	private Alignment extendSiblings(Alignment a, double thresh)
	{		
		RelationshipMap rels = aml.getRelationshipMap();
		Table2Set<Integer,Integer> toMap = new Table2Set<Integer,Integer>();
		for(int i = 0; i < a.size(); i++)
		{
			Mapping input = a.get(i);
			if(!aml.getURIMap().isClass(input.getSourceId()))
				continue;
			Set<Integer> sourceSiblings = rels.getAllSiblings(input.getSourceId());
			Set<Integer> targetSiblings = rels.getAllSiblings(input.getTargetId());
			if(sourceSiblings.size() > 200 || targetSiblings.size() > 200)
				continue;
			for(Integer s : sourceSiblings)
			{
				if(a.containsSource(s))
					continue;
				for(Integer t : targetSiblings)
				{
					if(!a.containsTarget(t))
						toMap.add(s, t);
				}
			}
		}
		return mapInParallel(toMap,thresh);
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
		double maxSim = 0.0;
		double sim, weight;
		
		if(lSet.equals(LanguageSetting.MULTI))
		{
			for(String l : languages)
			{
				Set<String> sourceNames = sLex.getNamesWithLanguage(sId,l);
				Set<String> targetNames = tLex.getNamesWithLanguage(tId,l);
				if(sourceNames == null || targetNames == null)
					continue;
			
				for(String s : sourceNames)
				{
					if(sLex.getTypes(s,sId).contains(LexicalType.FORMULA))
						continue;
					weight = sLex.getCorrectedWeight(s, sId, l);
					
					for(String t : targetNames)
					{
						if(tLex.getTypes(t,tId).contains(LexicalType.FORMULA))
							continue;
						sim = weight * tLex.getCorrectedWeight(t, tId, l);
						sim *= stringSimilarity(s,t);
						if(sim > maxSim)
							maxSim = sim;
					}
				}
			}
		}
		else
		{
			Set<String> sourceNames = sLex.getNames(sId);
			Set<String> targetNames = tLex.getNames(tId);
			if(sourceNames == null || targetNames == null)
				return maxSim;
			for(String s : sourceNames)
			{
				if(sLex.getTypes(s,sId).contains(LexicalType.FORMULA))
					continue;
				weight = sLex.getCorrectedWeight(s, sId);
				
				for(String t : targetNames)
				{
					if(tLex.getTypes(t,tId).contains(LexicalType.FORMULA))
						continue;
					sim = weight * tLex.getCorrectedWeight(t, tId);
					sim *= stringSimilarity(s,t);
					if(sim > maxSim)
						maxSim = sim;
				}
			}
		}
		return maxSim;
	}
	
	// Computes the string the similarity between two Strings
	private double stringSimilarity(String s, String t)
	{
		double sim = 0.0;
		if(measure.equals(StringSimMeasure.ISUB))
			sim = ISub.stringSimilarity(s,t);
		else if(measure.equals(StringSimMeasure.EDIT))
		{
			Levenshtein lv = new Levenshtein();
			sim = lv.getSimilarity(s, t);
		}
		else if(measure.equals(StringSimMeasure.JW))
		{
			JaroWinkler jv = new JaroWinkler();
			sim = jv.getSimilarity(s, t);
		}
		else if(measure.equals(StringSimMeasure.QGRAM))
		{
			QGramsDistance q = new QGramsDistance();
			sim = q.getSimilarity(s, t);
		}
		sim *= CORRECTION;
		return sim;
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
}