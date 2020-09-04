/******************************************************************************
* Copyright 2013-2020 LASIGE                                                  *
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
* Abstract Matcher based on association rules.                                *
*                                                                             *
* @authors Beatriz Lima, Daniel Faria                                         *
******************************************************************************/
package aml.match.association;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import aml.alignment.EDOALAlignment;
import aml.alignment.mapping.MappingRelation;
import aml.alignment.rdf.AbstractExpression;
import aml.ontology.EntityType;
import aml.ontology.Ontology;
import aml.util.data.Map2Map;

public abstract class AbstractAssociationRuleMatcher
{

// Attributes
	
	protected HashMap<AbstractExpression, Integer> entitySupport;
	protected Map2Map<AbstractExpression, AbstractExpression, Double> mappingSupport;
	protected final int minSup = 10;
	protected final double minConf = 0.8;
	
// Constructors
	
	protected AbstractAssociationRuleMatcher()
	{
		entitySupport = new HashMap<AbstractExpression, Integer>();
		mappingSupport = new Map2Map<AbstractExpression, AbstractExpression, Double>();
	}
	
// Public methods
	
	public EDOALAlignment match(Ontology o1, Ontology o2) {
		computeSupport(o1, o2);
		
		Map2Map<AbstractExpression, AbstractExpression, Double> ARules = 
				new Map2Map<AbstractExpression, AbstractExpression, Double>();

		for (AbstractExpression e1 : mappingSupport.keySet()) {
			for (AbstractExpression e2 : mappingSupport.get(e1).keySet()) {

				//Filter by support then confidence
				if (mappingSupport.get(e1, e2) >= minSup) {
					double conf = getConfidence(e1, e2);
					if (conf > minConf)
						ARules.add(e1, e2, conf);
				}
			}
		}
		EDOALAlignment a = new EDOALAlignment();

		for (AbstractExpression e1 : ARules.keySet()) {
			for (AbstractExpression e2 : ARules.get(e1).keySet()) {
				// If the rule is bidirectional, then it is an equivalence relation
				if(ARules.contains(e2,e1))
					a.add(e1, e2, ARules.get(e1, e2), MappingRelation.EQUIVALENCE);
				// If rule is unidirectional (A->B) then A is subsummed by B
				else 
					a.add(e1, e2, ARules.get(e1, e2), MappingRelation.SUBSUMED_BY);
			}
		}
		return a;
	}

// Protected methods
	
	/**
	 * Populates EntitySupport and MappingSupport tables
	 */
	protected abstract void computeSupport(Ontology o1, Ontology o2);
	
	/**
	 * Gets the individuals shared by the two ontologies
	 */
	protected Set<String> getSharedInstances(Ontology o1, Ontology o2) {
		// Find shared instances in the two ontologies
		Set<String> sharedInstances = new HashSet<String>(o1.getEntities(EntityType.INDIVIDUAL));
		sharedInstances.retainAll(o2.getEntities(EntityType.INDIVIDUAL));
		System.out.println("Shared Individuals: " + sharedInstances.size());
		return sharedInstances;
	}

// Private methods	
	
	private double getConfidence(AbstractExpression e1, AbstractExpression e2) {
		double conf = mappingSupport.get(e1, e2) / entitySupport.get(e1);
		return conf;
	}
}
