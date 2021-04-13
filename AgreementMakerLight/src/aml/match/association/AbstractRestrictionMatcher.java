package aml.match.association;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import aml.AML;
import aml.alignment.EDOALAlignment;
import aml.alignment.mapping.EDOALMapping;
import aml.alignment.mapping.Mapping;
import aml.alignment.mapping.MappingRelation;
import aml.alignment.rdf.AbstractExpression;
import aml.ontology.EntityType;
import aml.ontology.Ontology;
import aml.ontology.semantics.EntityMap;
import aml.util.data.Map2Map;

public abstract class AbstractRestrictionMatcher 
{
	// Attributes
	protected Ontology o1;
	protected Ontology o2;
	protected EDOALAlignment in;
	protected EDOALAlignment out;
	protected EntityMap map;
	protected Set<String> sharedInstances; 
	protected HashMap<AbstractExpression, Integer> entitySupport;
	protected Map2Map<AbstractExpression, AbstractExpression, Integer> mappingSupport;
	protected Map2Map<AbstractExpression, AbstractExpression, Double> ARules;
	protected int minSup;
	protected final double minConf = 1.0;

	// Constructor
	public AbstractRestrictionMatcher()
	{
		out = new EDOALAlignment();
		map = AML.getInstance().getEntityMap();
		entitySupport = new HashMap<AbstractExpression, Integer>();
		mappingSupport = new Map2Map<AbstractExpression, AbstractExpression, Integer>();
		ARules = new Map2Map<AbstractExpression, AbstractExpression, Double>();
	}

	// Public methods
	/**
	 * Extends the given Alignment between the source and target Ontologies
	 * @param o1: the source Ontology to match
	 * @param o2: the target Ontology to match
	 * @param a: the existing alignment to extend
	 * @param e: the EntityType to match
	 * @param thresh: the similarity threshold for the extention
	 * @return the alignment with the new mappings between the Ontologies
	 */
	public EDOALAlignment extendAlignment(Ontology o1, Ontology o2, EDOALAlignment a)
	{
		System.out.println("Searching for Property Domain/Range Restrictions");
		long time = System.currentTimeMillis()/1000;
		this.o1 = o1;
		this.o2 = o2;
		this.in = a;
		sharedInstances = getSharedInstances(o1, o2);
		minSup = sharedInstances.size()/100;
		
		for(Mapping<AbstractExpression> m: in) 
		{
			AbstractExpression src = m.getEntity1();
			AbstractExpression tgt = m.getEntity2();
			
			// We want to apply the restriction to the broader relation in the subsumption mapping
			if (m.getRelationship() == MappingRelation.SUBSUMED_BY) // src < tgt
			{
				if(computeSupport(src,tgt)) 
				{
					Set<Mapping<AbstractExpression>> candidates = generateRules();
					if(candidates.size()==1)
						out.add(candidates.iterator().next());
					else if(candidates.size()>1)
						out.add(filter(candidates));
					else
						out.add(m); //no candidates
				}
				else 
					out.add(m);
			}			
			else if (m.getRelationship() == MappingRelation.SUBSUMES) // tgt < src
			{
				if(computeSupport(tgt, src)) 
				{
					Set<Mapping<AbstractExpression>> candidates = generateRules();
					if(candidates.size()==1)
						out.add(candidates.iterator().next());
					else if(candidates.size()>1)
						out.add(filter(candidates));
					else
						out.add(m); //no candidates
				}
				else
					out.add(m);
			}
			else 
				out.add(m); // Equivalence or unknown
		}
		System.out.println("Finished in " +	(System.currentTimeMillis()/1000-time) + " seconds");
		return out;
	}
	
	/**
	 * Extracts Domain/ Range Restrictions given a subsumption mapping 
	 * such as "smallerProperty < broaderProperty"
	 */
	protected abstract boolean computeSupport(AbstractExpression smallerExpression, AbstractExpression broaderExpression);

	/*
	 * Filter mappings
	 */
	protected abstract Mapping<AbstractExpression> filter(Set<Mapping<AbstractExpression>> candidates);

	protected Set<Mapping<AbstractExpression>> generateRules() 
	{
		Set<Mapping<AbstractExpression>> mappings = new HashSet<Mapping<AbstractExpression>>();
		// Compute confidence in rules
		for (AbstractExpression e1 : mappingSupport.keySet()) 
		{
			for (AbstractExpression e2 : mappingSupport.get(e1).keySet()) 
			{
				//Filter by support then confidence
				if (mappingSupport.get(e1, e2) >= minSup)
				{
					double conf = mappingSupport.get(e1, e2)*1.0 / entitySupport.get(e1);
					if (conf >= minConf) 
						ARules.add(e1, e2, conf);	
				}
			}
		}
		for (AbstractExpression e1 : ARules.keySet()) 
		{
			for (AbstractExpression e2 : ARules.get(e1).keySet()) 
			{
				// If the rule is bidirectional, then it is an equivalence relation
				if(ARules.contains(e2,e1)) 
				{	
					double conf = Math.sqrt(ARules.get(e1, e2) * ARules.get(e2, e1));
					// Make sure that mapping is directional (src->tgt)
					if(o1.containsAll(e1.getElements())) 
						mappings.add(new EDOALMapping(e1, e2, conf, MappingRelation.EQUIVALENCE));
					else
						mappings.add(new EDOALMapping(e2, e1, conf, MappingRelation.EQUIVALENCE));
				}
				// If rule is unidirectional (A->B) then A is subsumed by B (<)
				else 
				{
					if(o1.containsAll(e1.getElements())) 
						mappings.add(new EDOALMapping(e1, e2, ARules.get(e1, e2), MappingRelation.SUBSUMED_BY));
					else
						mappings.add(new EDOALMapping(e2, e1, ARules.get(e1, e2), MappingRelation.SUBSUMES));
				}
			}
		}
		return mappings;
	}
	
	/**
	 * Gets the individuals shared by the two ontologies
	 */
	protected static Set<String> getSharedInstances(Ontology o1, Ontology o2) 
	{
		// Find shared instances in the two ontologies
		Set<String> sharedInstances = new HashSet<String>();
		sharedInstances = o1.getEntities(EntityType.INDIVIDUAL);
		sharedInstances.retainAll(o2.getEntities(EntityType.INDIVIDUAL));

		//Remove equivalent instances --> multiplicated info
		EntityMap rels = AML.getInstance().getEntityMap();
		Set<String> sharedInstancesCopy = new HashSet<String>(sharedInstances);

		if(rels.getEquivalentIndividuals().size()>0) 
		{
			for(String instance: sharedInstancesCopy) 
			{
				Set<String> eqvIndividuals = new HashSet<String>(rels.getEquivalentIndividuals(instance));
				if(eqvIndividuals.size()>0) 
				{
					// Skip instances from third parties - those don't have any relationships
					if(rels.getIndividualActiveRelations().get(instance) == null) 
						continue;
					sharedInstances.removeAll(rels.getEquivalentIndividuals(instance));
					sharedInstances.add(instance); // Add itself -- we are left with only one ontology instance - the spokesman
				}
			}
		}
		return sharedInstances;
	}
	
	/*
	 * Increments entity support
	 * @param e: entity to account for
	 */
	protected void incrementEntitySupport(AbstractExpression e) 
	{
		if(!entitySupport.containsKey(e))
			entitySupport.put(e, 1);
		else 
			entitySupport.put(e, entitySupport.get(e)+1);
	}
	
	/*
	 * Increments mapping support for entities e1 and e2
	 * It's a symmetric map to facilitate searches
	 */
	protected void incrementMappingSupport(AbstractExpression e1, AbstractExpression e2) 
	{
		if(!mappingSupport.contains(e1,e2)) 
		{	
			mappingSupport.add(e1,e2, 1);
			mappingSupport.add(e2,e1, 1);
		}
		else 
		{
			mappingSupport.add(e1,e2, mappingSupport.get(e1,e2)+1);
			mappingSupport.add(e2,e1, mappingSupport.get(e2,e1)+1);
		}
	}
}
