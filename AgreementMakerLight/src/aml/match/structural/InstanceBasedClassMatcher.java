/******************************************************************************
* Copyright 2013-2019 LASIGE                                                  *
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
* Matches classes based on shared individuals that instantiate them.          *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/

package aml.match.structural;

import java.util.HashSet;
import java.util.Set;

import aml.AML;
import aml.alignment.SimpleAlignment;
import aml.match.AbstractHashMatcher;
import aml.ontology.EntityType;
import aml.ontology.Ontology;
import aml.ontology.ValueMap;
import aml.ontology.semantics.EntityMap;
import aml.util.data.Map2Set;
import aml.util.similarity.Similarity;

public class InstanceBasedClassMatcher extends AbstractHashMatcher
{
	
//Attributes
	
	protected static final String DESCRIPTION = "Matches classes and properties that have a\n" +
											  "high fraction of instances in common.";
	protected static final String NAME = "Instance-Based Class Matcher";
	protected static final EntityType[] SUPPORT = {EntityType.CLASS, EntityType.DATA_PROP, EntityType.OBJECT_PROP};
	
//Constructors
	
	public InstanceBasedClassMatcher()
	{
		description = DESCRIPTION;
		name = NAME;
		support = SUPPORT;
	}
	
//Protected Methods

	public SimpleAlignment match(Ontology o1, Ontology o2, EntityType e, double thresh, boolean fillClasses)
	{
		SimpleAlignment a = new SimpleAlignment(o1,o2);
		if(!checkEntityType(e))
			return a;
		System.out.println("Running " + name + " in match mode");
		long time = System.currentTimeMillis()/1000;
		a.addAll(hashMatch(o1,o2,e,thresh,fillClasses));
		time = System.currentTimeMillis()/1000 - time;
		System.out.println("Finished in " + time + " seconds");
		return a;
	}

	@Override
	public SimpleAlignment hashMatch(Ontology o1, Ontology o2, EntityType e, double thresh)
	{
		return hashMatch(o1, o2, e, thresh, false);
	}

	public SimpleAlignment hashMatch(Ontology o1, Ontology o2, EntityType e, double thresh, boolean fillClasses)
	{
		if (fillClasses)
			fillInstanceClasses(o1, o2, thresh);
		
		if (e.equals(EntityType.CLASS))
			return classMatch(o1, o2, thresh);
		if (e.equals(EntityType.DATA_PROP))
			return dataPropertyMatch(o1, o2, thresh);
		if (e.equals(EntityType.OBJECT_PROP))
			return objectPropertyMatch(o1, o2, thresh);
		return null;
	}
		
	//Private Methods
	private SimpleAlignment classMatch(Ontology o1, Ontology o2, double thresh)
	{
		SimpleAlignment a = new SimpleAlignment();
		AML aml = AML.getInstance();
		EntityMap rm = aml.getEntityMap();
		
		Map2Set<String,String> pairs = new Map2Set<String,String>();
		for(String i : o1.getEntities(EntityType.INDIVIDUAL))
		{
			Set<String> classes = rm.getIndividualClasses(i);
			Set<String> sources = new HashSet<String>();
			Set<String> targets = new HashSet<String>();
			for(String c : classes)
			{
				if(o1.contains(c))
					sources.add(c);
				else if(o2.contains(c))
					targets.add(c);
			}
			for(String s : sources)
				for(String t : targets)
					pairs.add(s, t);
		}
		System.out.println("Pairs: " + pairs.size());
		for(String s : pairs.keySet())
		{
			Set<String> si = rm.getClassIndividuals(s);
			for(String t : pairs.get(s))
			{
				Set<String> ti = rm.getClassIndividuals(t);
				double sim = Similarity.jaccardSimilarity(si, ti);
				if(sim >= thresh)
					a.add(s,t,sim);
			}			
		}
		
		return a;
	}

	/**
	 * Find 1:1 correspondences between data properties, when the two
	 * properties link the same individual to the same value in both ontologies.
	 */
	private SimpleAlignment dataPropertyMatch(Ontology o1, Ontology o2, double thresh)
	{
		SimpleAlignment a = new SimpleAlignment();
		
		ValueMap v1 = o1.getValueMap();
		ValueMap v2 = o2.getValueMap();
		
		Set<String> commonIndividuals = new HashSet<String>(v1.getIndividuals());
		commonIndividuals.retainAll(v2.getIndividuals());
		
		for (String individual : commonIndividuals) {
			for (String o1Prop : v1.getProperties(individual)) {
				for (String o2Prop : v2.getProperties(individual)) {
					Set<String> o1Values = v1.getValues(individual, o1Prop);
					Set<String> o2Values = v2.getValues(individual, o2Prop);
					
					if (!o1Values.containsAll(o2Values) || !o2Values.containsAll(o1Values))
						continue;
					
					if (a.containsSource(o1Prop) || a.containsTarget(o2Prop))
						continue;
					
					a.add(o1Prop, o2Prop, 1.0);
				}
			}
		}
		
		return a;
	}

	/**
	 * Finds 1:1 correspondences between object properties based on
	 * them connecting matched instances in the two ontologies. 
	 */
	private SimpleAlignment objectPropertyMatch(Ontology o1, Ontology o2, double thresh)
	{
		SimpleAlignment a = new SimpleAlignment();

		// object properties
		Set<String> allIndividuals = o1.getEntities(EntityType.INDIVIDUAL);
		allIndividuals.retainAll(o2.getEntities(EntityType.INDIVIDUAL));
		for (String individual1 : allIndividuals) {
			for (String individual2 : allIndividuals) {
				String p1 = getLinkingProperty(o1, individual1, individual2);
				String p2 = getLinkingProperty(o2, individual1, individual2);
				
				if (p1 == null || p2 == null)
					continue;
				
				if (a.containsSource(p1) || a.containsTarget(p2))
					continue;
				
				a.add(p1, p2, 1.0);
				System.out.println("Found a link between " + p1 +  " and  "  + p2);
			}
		}
		
		return a;
	}



	/**
	 * Sometimes ontologies fail to declare instance classes. This fills in the
	 * missing classes by using domains and ranges of the instances' object and
	 * data properties. The classes are filtered using the lexical similarity
	 * to the class label in the other ontology.
	 * @param o1 the source ontology
	 * @param o2 the target ontology
	 * @param thresh threshold for similarity measure
	 */
	public void fillInstanceClasses(Ontology o1, Ontology o2, double thresh) {
		EntityMap e = AML.getInstance().getEntityMap();
		
		// Find individuals declared in both ontologies
		Set<String> o1Individuals = o1.getEntities(EntityType.INDIVIDUAL);
		Set<String> o2Individuals = o2.getEntities(EntityType.INDIVIDUAL);
		Set<String> commonIndividuals = new HashSet<String>(o1Individuals);
		commonIndividuals.retainAll(o2Individuals);
		
		for (String i : commonIndividuals) {
			Set<String> o1classes = getIndividualClass(i, o1);
			Set<String> o2classes = getIndividualClass(i, o2);
			Set<String> candidateClasses = new HashSet<String>();

			// Get domains from individual's active object properties:
			for (String r : e.getIndividualActiveRelations(i))
				for (String p : e.getIndividualProperties(i,  r))
					candidateClasses.addAll(e.getDomains(p));

			// Get ranges from individual's passive object properties:
			for (String r : e.getIndividualPassiveRelations(i))
				for (String p : e.getIndividualProperties(r, i))
					candidateClasses.addAll(e.getRanges(p));
			
			// Get domains from individual's data properties:
			for (String p : o1.getValueMap().getProperties(i))
				candidateClasses.addAll(e.getDomains(p));

			// Fill in empty class in o1 using known class in o2 as the filter
			if (o1classes.isEmpty() && !o2classes.isEmpty()) {
				for (String c : candidateClasses) {
					if (!o1.contains(c))
						continue;

					for (String c2 : o2classes)
						if (Similarity.nameSimilarity(o1.getName(c), o2.getName(c2), false) >= thresh)
							e.addInstance(i, c);
				}
			}
			
			// Fill in empty class in o2 using known class in o1 as the filter
			if (!o1classes.isEmpty() && o2classes.isEmpty()) {
				for (String c : candidateClasses) {
					if (!o2.contains(c))
						continue;

					for (String c1 : o1classes)
						if (Similarity.nameSimilarity(o1.getName(c1), o2.getName(c), false) >= thresh)
							e.addInstance(i, c);
				}
			}
		}
	}
	

	//Private Methods
		/**
		 * @param uri the URI of the individual
		 * @param o the ontology
		 * @return the classes of the individual in the given ontology
		 */
		private static Set<String> getIndividualClass(String uri, Ontology o) {
			Set<String> classes = new HashSet<String>();

			for (String c : AML.getInstance().getEntityMap().getIndividualClasses(uri)) {
				if (o.contains(c))
					classes.add(c);
			}
			
			return classes;
		}

		/**
		 * Returns the object 
		 * @param o
		 * @param e1 URI of an individual
		 * @param e2 URI of an individual
		 * @return The object property linking the two individuals in the ontology o.
		 */
		private static String getLinkingProperty(Ontology o, String e1, String e2) {
			EntityMap e = AML.getInstance().getEntityMap();
			for (String prop : e.getIndividualProperties(e1, e2))
				if (o.contains(prop))
					return prop;
			return null;
		}
}