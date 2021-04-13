package aml.filter;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import aml.AML;
import aml.alignment.Alignment;
import aml.alignment.EDOALAlignment;
import aml.alignment.mapping.Mapping;
import aml.alignment.mapping.MappingRelation;
import aml.alignment.rdf.AbstractExpression;
import aml.alignment.rdf.InverseRelation;
import aml.alignment.rdf.PropertyExpression;
import aml.alignment.rdf.PropertyId;
import aml.alignment.rdf.PropertyIntersection;
import aml.alignment.rdf.RelationExpression;
import aml.alignment.rdf.RelationId;
import aml.alignment.rdf.RelationIntersection;
import aml.ontology.semantics.EntityMap;
import aml.util.data.Map2Map2Set;
import aml.util.data.Map2Set;

public class SemanticPropertyFilter implements Filterer
{
	EDOALAlignment in;
	EDOALAlignment out;
	protected EntityMap map;	
	Map2Map2Set<AbstractExpression, MappingRelation, Mapping<AbstractExpression>> subsumption;
	Set<AbstractExpression> srcLeftover;
	Set<AbstractExpression> tgtLeftover;

	// Constructor
	public SemanticPropertyFilter() 
	{
		out = new EDOALAlignment();
		map = AML.getInstance().getEntityMap();
		subsumption = new Map2Map2Set<AbstractExpression, MappingRelation, Mapping<AbstractExpression>>();
		srcLeftover = new HashSet<AbstractExpression>();
		tgtLeftover = new HashSet<AbstractExpression>();
	}

	//Public Methods
	@SuppressWarnings("rawtypes")
	public Alignment filter(Alignment a) 
	{
		if(!(a instanceof EDOALAlignment))
		{
			System.out.println("Warning: cannot filter non-EDOAL alignment!");
			return a;
		}
		System.out.println("Performing Selection");
		long time = System.currentTimeMillis()/1000;
		this.in = (EDOALAlignment)a;

		filterPipeline(aml.alignment.rdf.RelationId.class, "Relation-Relation");
		filterPipeline(aml.alignment.rdf.PropertyId.class, "Property-Property");

		System.out.println("Filtered out "+ (in.size() - out.size()) + " mappings");
		System.out.println("Finished in " +	(System.currentTimeMillis()/1000-time) + " seconds");
		return out;
	}

	@SuppressWarnings("rawtypes")
	private void filterPipeline(Class clazz, String mappingType) 
	{	
		//REALTION - RELATION
		// 1) Iterate through source object properties and search for perfect equivalence mappings 
		for(AbstractExpression src: in.getSourceExpressions())
		{		
			if(!clazz.isInstance(src))
				continue;
			// Add best candidates to final alignment
			Set<Mapping<AbstractExpression>> bestEquivalence = new HashSet<Mapping<AbstractExpression>>(
					findBestEquivalence(src, false, 1.0, mappingType));
			if(bestEquivalence.size() > 0)
				out.addAll(bestEquivalence);
			else
				srcLeftover.add(src);
		}
		// 2) Iterate through target classes and search for perfect equivalence mappings, 
		// skipping those that have been already mapped
		for(AbstractExpression tgt: in.getTargetExpressions())
		{		
			if(!(clazz.isInstance(tgt)) || out.containsTarget(tgt))
				continue;
			// Add best candidates to final alignment
			Set<Mapping<AbstractExpression>> bestEquivalence = new HashSet<Mapping<AbstractExpression>>(
					findBestEquivalence(tgt, true, 1.0, mappingType));
			if(bestEquivalence.size() > 0)
				out.addAll(bestEquivalence);
			else
				tgtLeftover.add(tgt);
		}
		// 3) Attempt to find equivalence mappings again, this time lowering the threshold
		for(AbstractExpression src: srcLeftover) 
		{
			if(out.containsSource(src))
				continue;
			Set<Mapping<AbstractExpression>> bestEquivalence = new HashSet<Mapping<AbstractExpression>>(
					findBestEquivalence(src, false, null, mappingType));
			if(bestEquivalence.size() >0)
				out.addAll(bestEquivalence);

		}	
		for(AbstractExpression tgt: tgtLeftover) 
		{
			if(out.containsTarget(tgt))
				continue;
			Set<Mapping<AbstractExpression>> bestEquivalence = new HashSet<Mapping<AbstractExpression>>(
					findBestEquivalence(tgt, true, null, mappingType));
			if(bestEquivalence.size() >0)
				out.addAll(bestEquivalence);				
		}
		// 4) Iterate through remaining source object properties and search for subsumption mappings
		if(srcLeftover.size() > 0) 
		{
			for(AbstractExpression src: srcLeftover)
			{		
				if(out.containsSource(src))
					continue;
				if(subsumption.contains(src, MappingRelation.SUBSUMED_BY))
					out.addAll(findBestSubsumption(subsumption.get(src, MappingRelation.SUBSUMED_BY), false, null, mappingType));
				if(subsumption.contains(src, MappingRelation.SUBSUMES)) 
					out.addAll(findBestSubsumption(subsumption.get(src, MappingRelation.SUBSUMES), false, null, mappingType));
			}
		}
		// 5) Iterate through remaining object properties and search for subsumption mappings
		if(tgtLeftover.size()>0) 
		{
			for(AbstractExpression tgt: in.getTargetExpressions())
			{		
				if(out.containsTarget(tgt))
					continue;
				if(subsumption.contains(tgt, MappingRelation.SUBSUMED_BY))
					out.addAll(findBestSubsumption(subsumption.get(tgt, MappingRelation.SUBSUMED_BY), true, null, mappingType));
				if(subsumption.contains(tgt, MappingRelation.SUBSUMES))
					out.addAll(findBestSubsumption(subsumption.get(tgt, MappingRelation.SUBSUMES), true, null, mappingType));
			}
		}
	}

	/* This method finds the best equivalence mapping for a given source or target single class
	 * @param simpleClass: the simple class that we want to map
	 * @param srcIsComplex: whether the source is complex, i.e. true if the simpleClass is from the target
	 * ontology, false if it's from the source ontology
	 * @param thres: confidence threshold; null if we want the maximum confidence among options
	 * @param mode: "Relation-Relation", "Property-Property" or "Relation-Property"
	 * @return a set of the best equivalence mappings found for the simpleClass
	 * */
	private Set<Mapping<AbstractExpression>> findBestEquivalence(AbstractExpression simpleProperty, boolean srcIsComplex, Double thres, String mode) 
	{
		// Type of mapping (simple / patterns) -> list of mappings
		Map2Set<String, Mapping<AbstractExpression>> candidates = new Map2Set<String, Mapping<AbstractExpression>>();
		Vector<Mapping<AbstractExpression>> options = new Vector<Mapping<AbstractExpression>>();

		if(srcIsComplex)
			options = in.getTargetMappings(simpleProperty);
		else
			options = in.getSourceMappings(simpleProperty);

		// Find maximum confidence in equivalent mappings
		double maxConf = 0.0;
		if(thres == null) 
		{
			for(Mapping<AbstractExpression> m: options) 
			{
				if(m.getRelationship().equals(MappingRelation.EQUIVALENCE) && m.getSimilarity() > maxConf)
					maxConf = m.getSimilarity();
			}
		}
		else
			maxConf = thres;

		// Apply filters and separate conflicting mappings into simple or complex patterns
		loop:
			for(Mapping<AbstractExpression> m: options) 
			{
				if(m.getRelationship().equals(MappingRelation.EQUIVALENCE)) 
				{	
					// Discard bellow thres
					if(m.getSimilarity() < maxConf)
						continue loop;
					AbstractExpression src = m.getEntity1();
					AbstractExpression tgt = m.getEntity2();
					switch(mode) {
					case "Relation-Relation":
						if(!(src instanceof RelationExpression) || !(tgt instanceof RelationExpression))
							continue loop;
						// Discard mappings if they will cause conflict (case simple -> simple)
						if(srcIsComplex && src instanceof RelationId && out.containsSource(src))
							continue loop;
						else if(tgt instanceof RelationId && out.containsTarget(tgt))
							continue loop;
						break;
					case "Property-Property":
						if(!(src instanceof PropertyExpression) || !(tgt instanceof PropertyExpression))
							continue loop;
						// Discard mappings if they will cause conflict (case simple -> simple)
						if(srcIsComplex && src instanceof RelationId && out.containsSource(src))
							continue loop;
						else if(tgt instanceof RelationId && out.containsTarget(tgt))
							continue loop;
						break;
					}
					candidates.add(getPropertyMappingPattern(src, tgt), m);
				}
				// Save subsumption mappings for later in case we need them
				else if(m.getRelationship().equals(MappingRelation.SUBSUMED_BY)) 
					subsumption.add(simpleProperty, MappingRelation.SUBSUMED_BY, m);
				else if(m.getRelationship().equals(MappingRelation.SUBSUMES)) 
					subsumption.add(simpleProperty, MappingRelation.SUBSUMES, m);
			}

		if(candidates.size()>0)
			return processPropertyCandidates(candidates, srcIsComplex); 

		return new HashSet<Mapping<AbstractExpression>>();
	}

	private Set<Mapping<AbstractExpression>> findBestSubsumption(Set<Mapping<AbstractExpression>> options, boolean srcIsComplex, Double thres, String mode) 
	{
		// Type of mapping (simple / patterns) -> list of mappings
		Map2Set<String, Mapping<AbstractExpression>> candidates = new Map2Set<String, Mapping<AbstractExpression>>();

		// Find maximum confidence in equivalent mappings
		double maxConf = 0.0;
		if(thres == null) 
		{
			for(Mapping<AbstractExpression> m: options) 
			{
				if(m.getSimilarity() > maxConf)
					maxConf = m.getSimilarity();
			}
		}
		else
			maxConf = thres;

		// Apply filters and separate conflicting mappings into simple or complex patterns
		loop:
			for(Mapping<AbstractExpression> m: options) 
			{
				// Discard bellow thres
				if(m.getSimilarity() < maxConf)
					continue loop;

				AbstractExpression src = m.getEntity1();
				AbstractExpression tgt = m.getEntity2();
				// Discard mappings if they will cause conflict (of any kind)
				if(srcIsComplex && out.containsSource(src))
					continue loop;
				else if(out.containsTarget(tgt))
					continue loop;
				// Discard mappings according to mode
				switch(mode) {
				case "Relation-Relation":
					if(!(src instanceof RelationExpression) || !(tgt instanceof RelationExpression))
						continue loop;
					// Discard mappings if they will cause conflict (case simple -> simple)
					if(srcIsComplex && src instanceof RelationId && out.containsSource(src))
						continue loop;
					else if(tgt instanceof RelationId && out.containsTarget(tgt))
						continue loop;
					break;
				case "Property-Property":
					if(!(src instanceof PropertyExpression) || !(tgt instanceof PropertyExpression))
						continue loop;
					// Discard mappings if they will cause conflict (case simple -> simple)
					if(srcIsComplex && src instanceof RelationId && out.containsSource(src))
						continue loop;
					else if(tgt instanceof RelationId && out.containsTarget(tgt))
						continue loop;
					break;
				}
				candidates.add(getPropertyMappingPattern(src, tgt), m);
			}
		if(candidates.size()>0)
			return processPropertyCandidates(candidates, srcIsComplex); 
		return new HashSet<Mapping<AbstractExpression>>();
	}

	private String getPropertyMappingPattern(AbstractExpression src, AbstractExpression tgt) 
	{
		if(src instanceof RelationId && tgt instanceof RelationId)
			return "Simple";
		if(src instanceof PropertyId && tgt instanceof PropertyId)
			return "Simple";
		else if(src instanceof InverseRelation || tgt instanceof InverseRelation)
			return "InverseRelation";
		else if(src instanceof RelationIntersection || tgt instanceof RelationIntersection)
			return "Restriction";
		else if(src instanceof PropertyIntersection || tgt instanceof PropertyIntersection)
			return "Restriction";
		return null;
	}

	/* This method finds processes property candidates. To note that it is not concerned with cardinality; 
	 * It is assumed that all mappings in candidates have single entities that have not been mapped yet.
	 * However, the mappings resulting from this method may contain complex entities with cardinality>1 
	 * in the final alignment.
	 * @param candidates: map of candidates; Type of mapping (simple / patterns) -> set of mappings
	 * @param thres: confidence threshold
	 * @param srcIsComplex: whether the source is complex
	 * @return set of best candidates out of candidates
	 * */
	private Set<Mapping<AbstractExpression>> processPropertyCandidates(
			Map2Set<String, Mapping<AbstractExpression>> candidates, boolean srcIsComplex) 
	{
		Set<Mapping<AbstractExpression>> bestCandidates = new HashSet<Mapping<AbstractExpression>>();

		// SIMPLE
		if(candidates.keySet().contains("Simple")) 
		{
			Vector<Mapping<AbstractExpression>> options = new Vector<Mapping<AbstractExpression>>(candidates.get("Simple"));
			for(Mapping<AbstractExpression> option: options)
				bestCandidates.add(option);
		}
		// Only search for complex mappings if there aren't any valid simple ones
		if(bestCandidates.size()>0)
			return bestCandidates;

		//COMPLEX
		//RESTRICTION
		if(candidates.keySet().contains("Restriction")) 
		{
			Vector<Mapping<AbstractExpression>> options = new Vector<Mapping<AbstractExpression>>(candidates.get("Restriction"));
			for(Mapping<AbstractExpression> option: options)
				bestCandidates.add(option);
		}
		if(candidates.keySet().contains("InverseRelation")) 
		{
			Vector<Mapping<AbstractExpression>> options = new Vector<Mapping<AbstractExpression>>(candidates.get("InverseRelation"));
			for(Mapping<AbstractExpression> option: options)
				bestCandidates.add(option);
		}
		return bestCandidates;
	}

}
