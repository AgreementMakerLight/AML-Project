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
 * @authors Teemu Tervo                                                        *
 ******************************************************************************/
package aml.match.lexical;

import java.util.HashSet;
import java.util.Set;

import aml.AML;
import aml.alignment.EDOALAlignment;
import aml.alignment.mapping.EDOALMapping;
import aml.alignment.mapping.Mapping;
import aml.alignment.rdf.*;
import aml.match.BiDirectionalMatcher;
import aml.ontology.EntityType;
import aml.ontology.Ontology;
import aml.ontology.lexicon.StopList;
import aml.util.similarity.Similarity;
import aml.util.similarity.StringSimMeasure;
import edu.smu.tspell.wordnet.Synset;
import edu.smu.tspell.wordnet.SynsetType;
import edu.smu.tspell.wordnet.WordNetDatabase;

public class UnionMatcher extends BiDirectionalMatcher
{
	//Attributes
	protected static final String DESCRIPTION = "Generates mappings corresponding to class unions";
	protected static final String NAME = "Union Matcher";
	protected static final EntityType[] SUPPORT = {EntityType.CLASS};
	
	private StringSimMeasure measure = StringSimMeasure.ISUB; // Similarity measured used to compare class labels

	private String WordNetPath = "store/knowledge/wordnet/";
	private WordNetDatabase wordNet;
	Set<String> stopWords;

	//Constructors

	/**
	 * Constructs a new UnionMatcher with default
	 * String similarity measure (ISub)
	 */
	public UnionMatcher()
	{
		super();
		description = DESCRIPTION;
		name = NAME;
		support = SUPPORT;
		
		stopWords = StopList.read();

		// Setup the wordnet database directory and instantiate WordNet
		System.setProperty("wordnet.database.dir", AML.getInstance().getPath() + WordNetPath);
		wordNet = WordNetDatabase.getFileInstance();
	}

	/**
	 * Constructs a new UnionMatcher with the given String similarity measure
	 * @param m the string similarity measure
	 */
	public UnionMatcher(StringSimMeasure m)
	{
		this();
		measure = m;
	}

	//Public Methods
	//Protected Methods	
	/**
	 * Finds mappings corresponding to a union between classes,
	 * e.g. o1:AorBorC(x) <-> o2:A(x) or o2:B(x) or o2:C(x).
	 * Also combines labels of the pattern o1:Class(x) <-> o2:Class(x) or 02:AnotherClass(x),
	 * provided the active alignment contains no mapping between o1:Class and o2:Class.
	 * 
	 * @param o1 the source Ontology
	 * @param o2 the target Ontology
	 * @param thresh the similarity threshold for the alignment
	 * @return the alignment between the source and target ontologies
	 */
	protected EDOALAlignment uniMatch(Ontology o1, Ontology o2, EntityType e, double thresh)
	{
		EDOALAlignment a = new EDOALAlignment(o1, o2);

		Set<String> srcClasses = o1.getEntities(EntityType.CLASS);
		Set<String> tgtClasses = o2.getEntities(EntityType.CLASS);
		
		srcClasses = AttributeRestrictionMatcher.removeMappedEntities(srcClasses);
		tgtClasses = AttributeRestrictionMatcher.removeMappedEntities(tgtClasses);

		for (String sourceUri : srcClasses) {
			for (String name : o1.getLexicon().getNames(sourceUri)) {
				String[] words = AttributeRestrictionMatcher.removeWords(name, stopWords).split(" ");
				
				// Only look for source ontology classes with two or more words
				if (words.length < 2)
					continue;

				// Look for target ontology classes corresponding to each individual word
				Set<String> matches = new HashSet<String>();
				double sim = 1.0;
				for (String part : words) {
					for (String targetUri : tgtClasses) {
						double wordSim = Similarity.stringSimilarity(part, o2.getName(targetUri), measure);
						
						if (wordSim >= thresh) {
							matches.add(targetUri);
							sim = sim*wordSim;
							break;
						}
					}
				}
				
				if (words.length == matches.size()) {
					ClassId e1 = new ClassId(sourceUri);
					
					Set<ClassExpression> union = new HashSet<ClassExpression>();
					for (String targetUri : matches)
						union.add(new ClassId(targetUri));
					
					ClassUnion e2 = new ClassUnion(union);
					a.add(new EDOALMapping(e1, e2, sim));
				}
			}
		}
		
		// Next, look for correspondences of the type
		// Class(x) <-> Class(x) or AnotherClass(x).
		// This expects all simple mappings to have been mapped already.
		for (String o1Class : srcClasses) {
			for (String o2Class : tgtClasses) {
				String o1name = o1.getName(o1Class);
				String o2name = o2.getName(o2Class);

				// the two class labels must be exactly equal
				if (!o1name.equalsIgnoreCase(o2name))
					continue;

				// Look for class labels ending in the common name in O1
				Set<String> candidates = new HashSet<String>();
				for (String o1c : srcClasses) {
					
					// Ensure class is not a subclass of o1class
					if (AML.getInstance().getEntityMap().getSubclasses(o1Class).contains(o1c))
						continue;
					
					// candidate name must end with o1class name
					if (!o1c.toLowerCase().endsWith(o1name))
						continue;

					// finally, check whether the remaining part of the name is a noun or an adjective
					// (verb is generally wrong, e.g. "request (for) X" is a request, not an X)
					String candidateName = o1.getName(o1c);
					candidateName = candidateName.replaceAll(o1name, "").trim();
					
					for (Synset ss : wordNet.getSynsets(candidateName))
						if (ss.getType() == SynsetType.ADJECTIVE || ss.getType() == SynsetType.NOUN)
							candidates.add(o1c);
				}

				if (!candidates.isEmpty()) {
					candidates.add(o1Class);

					Set<ClassExpression> union = new HashSet<ClassExpression>();
					for (String uri : candidates)
						union.add(new ClassId(uri));
					ClassUnion e1 = new ClassUnion(union);

					ClassId e2 = new ClassId(o2Class);
					a.add(new EDOALMapping(e1, e2, thresh)); // Use a fixed confidence value set to the threshold
				}
			}
		}
		return a;
	}

	//Private Methods
}