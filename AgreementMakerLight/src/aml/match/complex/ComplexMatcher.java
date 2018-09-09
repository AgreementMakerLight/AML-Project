/******************************************************************************
 * Copyright 2013-2018 LASIGE                                                  *
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
package aml.match.complex;

import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import aml.AML;
import aml.alignment.EDOALAlignment;
import aml.alignment.SimpleAlignment;
import aml.alignment.mapping.EDOALMapping;
import aml.alignment.rdf.AttributeDomainRestriction;
import aml.alignment.rdf.AttributeOccurrenceRestriction;
import aml.alignment.rdf.ClassExpression;
import aml.alignment.rdf.ClassId;
import aml.alignment.rdf.ClassUnion;
import aml.alignment.rdf.Comparator;
import aml.alignment.rdf.InverseRelation;
import aml.alignment.rdf.NonNegativeInteger;
import aml.alignment.rdf.RelationId;
import aml.alignment.rdf.RestrictionElement;
import aml.ontology.EntityType;
import aml.ontology.Ontology;
import aml.ontology.semantics.EntityMap;
import aml.util.similarity.Similarity;

public class ComplexMatcher
{

//Public Methods
	
	/**
	 * Matches the source and target Ontologies, returning an Alignment between them
	 * @param o1: the source Ontology to match
	 * @param o2: the target Ontology to match
	 * @param e: the EntityType to match
	 * @param thresh: the similarity threshold for the alignment
	 * @return the alignment between the source and target ontologies
	 */
	public EDOALAlignment match(Ontology o1, Ontology o2, SimpleAlignment input, double thresh)
	{
		EDOALAlignment a = AORmatch(o1, o2, input, thresh, false);
		a.addAll(AORmatch(o2, o1, input, thresh, true));
		a.addAll(ADRmatch(o1, o2, input, thresh, false));
		a.addAll(ADRmatch(o2, o1, input, thresh, false));
		return a;
	}

//Private Methods	
	
	/**
	 * Finds complex correspondences where the same concept is expressed as a subclass C in o1
	 * and as an object property P in o2. There should be an existing simple correspondence between
	 * the parent class of C and the domain or range of P. nameSimilarity of P and C is used as
	 * the similarity measure.
	 * @param o1: the source Ontology to match
	 * @param o2: the target Ontology to match
	 * @param input: the input SimpleAlignment between o1 and o2
	 * @param thresh: the similarity threshold for the alignment
	 * @param reverse: whether the alignment is to be reversed
	 * @return the alignment between the source and target ontologies
	 */
	private EDOALAlignment AORmatch(Ontology o1, Ontology o2, SimpleAlignment input, double thresh, boolean reverse)
	{
		EntityMap rels = AML.getInstance().getEntityMap();
		EDOALAlignment a = new EDOALAlignment(o1, o2);

		Set<String> srcClasses = o1.getEntities(EntityType.CLASS);
		Set<String> tgtProperties = o2.getEntities(EntityType.OBJECT_PROP);

		for(String c : srcClasses)
		{
			//Skip classes in the source ontology that already have a simple mapping
			if(input.containsEntity(c))
				continue;

			List<ClassExpression> targets = new ArrayList<ClassExpression>();
			List<Double> similarities = new ArrayList<Double>();

			for(String parent : rels.getSuperclasses(c))
			{
				for(String p : tgtProperties)
				{
					for(String d : rels.getDomains(p))
					{
						// Check whether the source ontology superclass is the domain of the relation (or ancestors of domain)
						boolean direct = false;
						if(input.get(parent, d) != null || input.get(d, parent) != null)
						{
							double similarity = Similarity.nameSimilarity(o1.getName(c), o2.getName(p),true);
							if(similarity >= thresh)
							{
								AttributeOccurrenceRestriction e2 =	new AttributeOccurrenceRestriction(
												new RelationId(p),
												new Comparator("http://ns.inria.org/edoal/1.0/#greater-than"),
												new NonNegativeInteger(0));
								targets.add(e2);
								similarities.add(similarity);
								direct = true;
							}

						}
						if(!direct)
						{
							for(String d_parent:rels.getSuperclasses(d))
							{
								if(input.get(parent, d_parent) != null || input.get(d_parent, parent) != null)
								{
									double similarity = Similarity.nameSimilarity(o1.getName(c), o2.getName(p), true);
									if(similarity >= thresh)
									{
										AttributeOccurrenceRestriction e2 =	new AttributeOccurrenceRestriction(
														new RelationId(p),
														new Comparator("http://ns.inria.org/edoal/1.0/#greater-than"),
														new NonNegativeInteger(0));
										if(!targets.contains(e2))
										{
											targets.add(e2);
											similarities.add(similarity);
										}
									}
								}
							}
						}
					}
					for(String r : rels.getRanges(p))
					{
						//Check whether the source ontology superclass is the range of the relation
						boolean direct = false;
						if(input.get(parent, r) != null || input.get(r, parent) != null)
						{
							double similarity = Similarity.nameSimilarity(o1.getName(c), o2.getName(p),true);
							if(similarity >= thresh)
							{
								AttributeOccurrenceRestriction e2 =
										new AttributeOccurrenceRestriction(
												new InverseRelation(new RelationId(p)),
												new Comparator("http://ns.inria.org/edoal/1.0/#greater-than"),
												new NonNegativeInteger(0));
								targets.add(e2);
								similarities.add(similarity);
								direct = true;
							}
						}
						if(!direct)
						{
							for(String r_parent:rels.getSuperclasses(r))
							{
								if (input.get(parent, r_parent) != null || input.get(r_parent, parent) != null)
								{
									double similarity = Similarity.nameSimilarity(o1.getName(c), o2.getName(p),true);
									if(similarity >= thresh)
									{
										AttributeOccurrenceRestriction e2 = new AttributeOccurrenceRestriction(
														new InverseRelation(new RelationId(p)),
														new Comparator("http://ns.inria.org/edoal/1.0/#greater-than"),
														new NonNegativeInteger(0));
										if(!targets.contains(e2))
										{
											targets.add(e2);
											similarities.add(similarity);
										}

									}
								}
							}
						}
					}
				}

				if (targets.isEmpty())
					continue;

				ClassId e1 = new ClassId(c);	
				if(targets.size() == 1)
				{
					if(!reverse)
						a.add(new EDOALMapping(e1, targets.iterator().next(),
								similarities.iterator().next()));
					else
						a.add(new EDOALMapping(targets.iterator().next(), e1,
								similarities.iterator().next()));
				}
				else
				{
					ClassUnion union = new ClassUnion(new HashSet<ClassExpression>(targets));
					double similarity = Collections.min(similarities);
					if(!reverse)
						a.add(new EDOALMapping(e1, union, similarity));
					else
						a.add(new EDOALMapping(union, e1, similarity));
				}
			}
		}
		return a;
	}

	private EDOALAlignment ADRmatch(Ontology o1, Ontology o2, SimpleAlignment input, double thresh, boolean reverse)
	{
		boolean stemming = true;
		EntityMap rels = AML.getInstance().getEntityMap();

		EDOALAlignment a = new EDOALAlignment(o1, o2);

		Set<String> srcClasses = o1.getEntities(EntityType.CLASS);
		Set<String> tgtProperties = o2.getEntities(EntityType.OBJECT_PROP);
		Set<String> tgtClasses = o2.getEntities(EntityType.CLASS);

		for(String c : srcClasses)
		{
			// Skip classes in the source ontology that already have a simple mapping
			if(input.containsEntity(c))
				continue;
			Set<String> sNames = o1.getLexicon().getNamesWithLanguage(c, "en");
			for(String sName:sNames)
			{
				List<ClassExpression> targets = new ArrayList<ClassExpression>();
				List<Double> similarities = new ArrayList<Double>();
				//calculate word sim to classes in target
				for(String tc: tgtClasses)
				{
					Set<String> tcNames = o2.getLexicon().getNamesWithLanguage(tc, "en");
					for(String tcName:tcNames)
					{
						if(tcName.length()>sName.length())
							continue;
						double classSim = Similarity.wordSimilarity(sName, tcName, stemming);
						if(classSim >= thresh/2)
						{
							//remove shared words
							String partialSName=removeSharedWords(sName, tcName);
							for(String tp: tgtProperties)
							{
								String tpName = o2.getName(tp);
								double propSim = Similarity.wordSimilarity(partialSName, tpName, stemming);
								if(propSim > 0)
								{
									//check if range of tp matches tc or tc superclasses
									Set<String> tp_ranges=rels.getRanges(tp);
									for(String tc_parent:rels.getSuperclasses(tc))
									{
										if(tp_ranges.contains(tc_parent))
										{
											AttributeDomainRestriction adr = new AttributeDomainRestriction(new RelationId(tp), 
															new ClassId(tc), 
															RestrictionElement.ALL);
											targets.add(adr);
											similarities.add((propSim*0.3+classSim*0.7));
										}
									}
								}
								//check if domain of tp matches source superclasses
								else
								{
									Set<String> tp_domains=rels.getDomains(tp);
									for(String tp_domain:tp_domains)
									{
										if(input.get(c,tp_domain)!=null || input.get(tp_domain,c)!=null || input.containsAncestralMapping(c, tp_domain) || input.containsAncestralMapping(tp_domain,c))
										{
											Set<String> tp_ranges=rels.getRanges(tp);
											for(String tc_parent:rels.getSuperclasses(tc))
											{
												if(tp_ranges.contains(tc_parent))
												{
													AttributeDomainRestriction adr = new AttributeDomainRestriction(new RelationId(tp), 
																	new ClassId(tc), 
																	RestrictionElement.ALL);
													targets.add(adr);
													similarities.add(classSim*0.7);
												}
											}
										}
									}
								}
							}
						}
					}
				}
				if(targets.isEmpty())
					continue;
				ClassId e1 = new ClassId(c);	
				double maxSim=0;
				int bestMapping=0;
				for(int i=0; i<targets.size();i++)
				{
					if(similarities.get(i)>maxSim)
					{
						bestMapping=i;
						maxSim=similarities.get(i);
					}
				}
				if(!reverse)
					a.add(new EDOALMapping(e1, targets.get(bestMapping),
							similarities.get(bestMapping)));
				else
					a.add(new EDOALMapping( targets.get(bestMapping), e1,
							similarities.get(bestMapping)));
			}
		}
		return a;
	}

	private String removeSharedWords(String s, String t)
	{
		String r="";
		String[] sWords=s.split(" ");
		for(String w: sWords)
			if (!t.contains(w))
				r+=" "+w;
		return r.trim();
	}
}