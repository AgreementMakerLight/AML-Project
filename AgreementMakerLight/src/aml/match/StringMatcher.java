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
import aml.alignment.Alignment;
import aml.alignment.SimpleMapping;
import aml.ontology.EntityType;
import aml.ontology.Ontology;
import aml.ontology.RelationshipMap;
import aml.ontology.lexicon.LexicalType;
import aml.ontology.lexicon.Lexicon;
import aml.settings.InstanceMatchingCategory;
import aml.settings.LanguageSetting;
import aml.settings.StringSimMeasure;
import aml.util.ISub;
import aml.util.Table2Set;

public class StringMatcher implements PrimaryMatcher, Rematcher, SecondaryMatcher
{

//Attributes

	private static final String DESCRIPTION = "Matches entities by computing the maximum\n" +
											  "String similarity between their Lexicon\n" +
											  "entries, using a String similarity measure";
	private static final String NAME = "String Matcher";
	private static final EntityType[] SUPPORT = {EntityType.CLASS,EntityType.INDIVIDUAL,EntityType.DATA,EntityType.OBJECT};
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
	public Alignment extendAlignment(Alignment a, EntityType e, double thresh) throws UnsupportedEntityTypeException
	{	
		checkEntityType(e);
		System.out.println("Extending Alignment with String Matcher");
		long time = System.currentTimeMillis()/1000;
		Alignment ext;
		if(e.equals(EntityType.CLASS))
		{
			System.out.println("Matching Children & Parents");
			ext = extendChildrenAndParents(a,thresh);
			Alignment aux = extendChildrenAndParents(ext,thresh);
			int size = 0;
			for(int i = 0; i < 10 && ext.size() > size; i++)
			{
				size = ext.size();
				for(SimpleMapping m : aux)
					if(!a.containsConflict(m))
						ext.add(m);
				aux = extendChildrenAndParents(aux,thresh);
			}
			System.out.println("Matching Siblings");
			ext.addAll(extendSiblings(a,thresh));
		}
		else
		{
			//TODO: Add support for other EntityTypes
			ext = new Alignment();
		}
		time = System.currentTimeMillis()/1000 - time;
		System.out.println("Finished in " + time + " seconds");
		return ext;
	}
	
	@Override
	public Alignment match(EntityType e, double thresh) throws UnsupportedEntityTypeException
	{
		checkEntityType(e);
		System.out.println("Running String Matcher");
		long time = System.currentTimeMillis()/1000;
		Set<Integer> sources = sLex.getEntities(e);
		Set<Integer> targets = tLex.getEntities(e);
		Alignment a = new Alignment();
		for(Integer i : sources)
		{
			if(e.equals(EntityType.INDIVIDUAL) && !aml.isToMatchSource(i))
				continue;
			Table2Set<Integer,Integer> toMap = new Table2Set<Integer,Integer>();
			for(Integer j : targets)
			{
				if(e.equals(EntityType.INDIVIDUAL) && (!aml.isToMatchTarget(j) ||
						(aml.getInstanceMatchingCategory().equals(InstanceMatchingCategory.SAME_CLASSES) &&
						!aml.getRelationshipMap().shareClass(i,j))))
					continue;
				toMap.add(i,j);
			}
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
		for(SimpleMapping m : a)
		{
			if(aml.getURIMap().getTypes(m.getSourceId()).equals(e))
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
	
	private Alignment extendChildrenAndParents(Alignment a, double thresh)
	{
		RelationshipMap rels = aml.getRelationshipMap();
		Table2Set<Integer,Integer> toMap = new Table2Set<Integer,Integer>();
		for(int i = 0; i < a.size(); i++)
		{
			SimpleMapping input = a.get(i);
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
			SimpleMapping input = a.get(i);
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
        List<Future<SimpleMapping>> results;
		ExecutorService exec = Executors.newFixedThreadPool(threads);
		try
		{
			results = exec.invokeAll(tasks);
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
	        results = new ArrayList<Future<SimpleMapping>>();
		}
		exec.shutdown();
		for(Future<SimpleMapping> fm : results)
		{
			try
			{
				SimpleMapping m = fm.get();
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
	private class MappingTask implements Callable<SimpleMapping>
	{
		private int source;
		private int target;
		
		MappingTask(int s, int t)
	    {
			source = s;
	        target = t;
	    }
	        
	    @Override
	    public SimpleMapping call()
	    {
       		return new SimpleMapping(source,target,mapTwoEntities(source,target));
        }
	}
}