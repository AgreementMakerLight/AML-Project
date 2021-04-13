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
import aml.alignment.rdf.AttributeDomainRestriction;
import aml.alignment.rdf.AttributeOccurrenceRestriction;
import aml.alignment.rdf.AttributeTypeRestriction;
import aml.alignment.rdf.AttributeValueRestriction;
import aml.alignment.rdf.ClassId;
import aml.alignment.rdf.Expression;
import aml.alignment.rdf.PropertyExpression;
import aml.alignment.rdf.RelationExpression;
import aml.alignment.rdf.RelationId;
import aml.ontology.semantics.EntityMap;
import aml.util.data.Map2Map2Set;
import aml.util.data.Map2Set;

public class SemanticClassFilter3 implements Filterer
{
	EDOALAlignment in;
	EDOALAlignment out;
	protected EntityMap map;
	Map2Map2Set<AbstractExpression, MappingRelation, Mapping<AbstractExpression>> subsumption;
	Set<AbstractExpression> srcLeftover;
	Set<AbstractExpression> tgtLeftover;

	// Constructor
	public SemanticClassFilter3() 
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
		System.out.println("Performing Class Selection");
		long time = System.currentTimeMillis()/1000;
		this.in = (EDOALAlignment) a;

		// 1) Iterate through source classes and search for perfect equivalence mappings 
		for(AbstractExpression src: in.getSourceExpressions())
		{		
			if(!(src instanceof aml.alignment.rdf.ClassId))
				continue;
			// Add best candidates to final alignment
			Set<Mapping<AbstractExpression>> bestEquivalence = new HashSet<Mapping<AbstractExpression>>(findBestEquivalence(src, false, 1.0));
			if(bestEquivalence.size() > 0)
				out.addAll(bestEquivalence);
			else
				srcLeftover.add(src);
		}
		// 2) Iterate through target classes and search for perfect equivalence mappings, 
		// skipping those that have been already mapped
		for(AbstractExpression tgt: in.getTargetExpressions())
		{		
			if(!(tgt instanceof aml.alignment.rdf.ClassId) || out.containsTarget(tgt))
				continue;
			// Add best candidates to final alignment
			Set<Mapping<AbstractExpression>> bestEquivalence = new HashSet<Mapping<AbstractExpression>>(findBestEquivalence(tgt, true, 1.0));
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
			Set<Mapping<AbstractExpression>> bestEquivalence = new HashSet<Mapping<AbstractExpression>>(findBestEquivalence(src, false, null));
			if(bestEquivalence.size() >0)
				out.addAll(bestEquivalence);

		}	
		for(AbstractExpression tgt: tgtLeftover) 
		{
			if(out.containsTarget(tgt))
				continue;
			Set<Mapping<AbstractExpression>> bestEquivalence = new HashSet<Mapping<AbstractExpression>>(findBestEquivalence(tgt, true, null));
			if(bestEquivalence.size() >0)
				out.addAll(bestEquivalence);				
		}
		// 4) Iterate through remaining source classes and search for subsumption mappings
		if(srcLeftover.size() > 0) 
		{
			for(AbstractExpression src: srcLeftover)
			{		
				if(out.containsSource(src))
					continue;
				if(subsumption.contains(src, MappingRelation.SUBSUMED_BY))
					out.addAll(findBestSubsumption(subsumption.get(src, MappingRelation.SUBSUMED_BY), false, 1.0));
				if(subsumption.contains(src, MappingRelation.SUBSUMES)) 
					out.addAll(findBestSubsumption(subsumption.get(src, MappingRelation.SUBSUMES), false, 1.0));
			}
		}
		// 5) Iterate through remaining target classes and search for subsumption mappings
		if(tgtLeftover.size()>0) 
		{
			for(AbstractExpression tgt: in.getTargetExpressions())
			{		
				if(out.containsTarget(tgt))
					continue;
				if(subsumption.contains(tgt, MappingRelation.SUBSUMED_BY))
					out.addAll(findBestSubsumption(subsumption.get(tgt, MappingRelation.SUBSUMED_BY), true, 1.0));
				if(subsumption.contains(tgt, MappingRelation.SUBSUMES))
					out.addAll(findBestSubsumption(subsumption.get(tgt, MappingRelation.SUBSUMES), true, 1.0));
			}
		}

		System.out.println("Filtered out "+ (in.size() - out.size()) + " mappings");
		System.out.println("Finished in " +	(System.currentTimeMillis()/1000-time) + " seconds");
		return out;
	}

	private boolean containsAnalogousPattern(Mapping<AbstractExpression> m1, Set<Mapping<AbstractExpression>> bestCandidates, boolean srcIsComplex) 
	{
		AbstractExpression property1 = null;
		AbstractExpression complexExpression1 = null;
		AbstractExpression complexExpression2 = null;

		if(srcIsComplex) 
			complexExpression1 = m1.getEntity1();	
		else 
			complexExpression1 = m1.getEntity2();

		// Get property1
		for(Expression e: complexExpression1.getComponents()) 
		{
			if(e instanceof RelationExpression || e instanceof PropertyExpression) 
			{
				property1 =(AbstractExpression) e;
				break;
			}
		}
		// Get property2
		for(Mapping<AbstractExpression> m2: bestCandidates) 
		{
			if(srcIsComplex) 
				complexExpression2 = m2.getEntity1();
			else 
				complexExpression2 = m2.getEntity2();
			AbstractExpression property2 = null;
			for(Expression e: complexExpression2.getComponents()) 
			{
				if(e instanceof RelationExpression || e instanceof PropertyExpression) 
				{
					property2 =(AbstractExpression) e;
					break;
				}
			}
			// Found analogous pattern
			if(property1.equals(property2))
				return true;
		}
		return false;
	}

	/* This method finds the best equivalence mapping for a given source or target single class
	 * @param simpleClass: the simple class that we want to map
	 * @param srcIsComplex: whether the source is complex, i.e. true if the simpleClass is from the target
	 * ontology, false if it's from the source ontology
	 * @param thres: confidence threshold; null if we want the maximum confidence among options
	 * @return a set of the best equivalence mappings found for the simpleClass
	 * */
	private Set<Mapping<AbstractExpression>> findBestEquivalence(AbstractExpression simpleClass, boolean srcIsComplex, Double thres) 
	{
		// Type of mapping (simple / patterns) -> list of mappings
		Map2Set<String, Mapping<AbstractExpression>> candidates = new Map2Set<String, Mapping<AbstractExpression>>();
		Vector<Mapping<AbstractExpression>> options = new Vector<Mapping<AbstractExpression>>();

		if(srcIsComplex)
			options = in.getTargetMappings(simpleClass);
		else
			options = in.getSourceMappings(simpleClass);
		
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
		for(Mapping<AbstractExpression> m: options) 
		{
			if(m.getRelationship().equals(MappingRelation.EQUIVALENCE)) 
			{	
				// Discard bellow thres
				if(m.getSimilarity() < maxConf)
					continue;
				// Discard mappings if they will cause conflict (case simple -> simple)
				if(srcIsComplex && m.getEntity1() instanceof ClassId && out.containsSource(m.getEntity1()))
					continue;
				else if( m.getEntity2() instanceof ClassId && out.containsTarget(m.getEntity2()))
					continue;
				
				candidates.add(getClassMappingPattern(m), m);
			}
			// Save subsumption mappings for later in case we need them
			else if(m.getRelationship().equals(MappingRelation.SUBSUMED_BY)) 
				subsumption.add(simpleClass, MappingRelation.SUBSUMED_BY, m);
			else if(m.getRelationship().equals(MappingRelation.SUBSUMES)) 
				subsumption.add(simpleClass, MappingRelation.SUBSUMES, m);
		}
		if(candidates.size()>0)
			return processClassCandidates(candidates, srcIsComplex); 

		return new HashSet<Mapping<AbstractExpression>>();
	}

	private Set<Mapping<AbstractExpression>> findBestSubsumption(Set<Mapping<AbstractExpression>> options, boolean srcIsComplex, double thres) 
	{
		// Type of mapping (simple / patterns) -> list of mappings
		Map2Set<String, Mapping<AbstractExpression>> candidates = new Map2Set<String, Mapping<AbstractExpression>>();

		// Apply filters and separate conflicting mappings into simple or complex patterns
		for(Mapping<AbstractExpression> m: options) 
		{
			// Discard mappings if they will cause conflict (of any kind)
			if(srcIsComplex && out.containsSource(m.getEntity1()))
				continue;
			else if(out.containsTarget(m.getEntity2()))
				continue;
			// If above threshold consider as candidate
			if(m.getSimilarity() >= thres)
				candidates.add(getClassMappingPattern(m), m);
		}
		if(candidates.size()>0)
			return processClassCandidates(candidates, srcIsComplex); 

		return new HashSet<Mapping<AbstractExpression>>();
	}

	private String getClassMappingPattern(Mapping<AbstractExpression> m) 
	{
		AbstractExpression src = m.getEntity1();
		AbstractExpression tgt = m.getEntity2();

		if(src instanceof ClassId && tgt instanceof ClassId)
			return "Simple";
		else if(src instanceof AttributeDomainRestriction || tgt instanceof AttributeDomainRestriction)
			return "ADR";
		else if(src instanceof AttributeOccurrenceRestriction || tgt instanceof AttributeOccurrenceRestriction)
			return "AOR";
		else if(src instanceof AttributeValueRestriction || tgt instanceof AttributeValueRestriction)
			return "AVR";
		else if(src instanceof AttributeTypeRestriction || tgt instanceof AttributeTypeRestriction)
			return "ATR";
		return null;
	}

	/* This method finds processes class candidates. To note that it does not concern with cardinality; 
	 * It is assumed that all mappings in candidates have single entities that have not been mapped yet.
	 * However, the mappings resulting from this method may contain complex entities with cardinality>1 
	 * in the final alignment.
	 * @param candidates: map of candidates; Type of mapping (simple / patterns) -> set of mappings
	 * @param thres: confidence threshold
	 * @param srcIsComplex: whether the source is complex
	 * @return set of best candidates out of candidates
	 * */	
	private Set<Mapping<AbstractExpression>> processClassCandidates(Map2Set<String, Mapping<AbstractExpression>> candidates, boolean srcIsComplex) 
	{
		Set<Mapping<AbstractExpression>> bestCandidates = new HashSet<Mapping<AbstractExpression>>();

		// SIMPLE
		if(candidates.keySet().contains("Simple")) 
		{
			Vector<Mapping<AbstractExpression>> options = new Vector<Mapping<AbstractExpression>>(candidates.get("Simple"));
			for(Mapping<AbstractExpression> option: removeSimpleChildren(options))
				bestCandidates.add(option);
		}
		// Only search for complex mappings if there aren't any valid simple ones
		if(bestCandidates.size()>0)
			return bestCandidates;
		
		// COMPLEX
		// AOR
		if(candidates.keySet().contains("AOR")) 
		{
			Vector<Mapping<AbstractExpression>> aboveThres = new Vector<Mapping<AbstractExpression>>();
			for(Mapping<AbstractExpression> option: candidates.get("AOR")) 
				if(option.getSimilarity() >= 1.0) 
					aboveThres.add(option);
			for(Mapping<AbstractExpression> option: aboveThres) 
				bestCandidates.add(option);
		}
		// ADR
		if(candidates.keySet().contains("ADR")) 
		{
			Vector<Mapping<AbstractExpression>> options = new Vector<Mapping<AbstractExpression>>(candidates.get("ADR"));
			// Discard redundant mappings (both terms of mapping)
			for(Mapping<AbstractExpression> m: removeADRChildren(options, srcIsComplex)) 
			{
				// check if there isn't already an analogous AOR in bestCandidates
				if(bestCandidates.size()>0) 
				{
					if(!containsAnalogousPattern(m, bestCandidates, srcIsComplex))
						bestCandidates.add(m);
				}
				else
					bestCandidates.add(m);
			}	
		}
		// AVR
		if(candidates.keySet().contains("AVR")) 
		{
			Vector<Mapping<AbstractExpression>> options = new Vector<Mapping<AbstractExpression>>(candidates.get("AVR"));
			for(Mapping<AbstractExpression> option: options) 
				bestCandidates.add(option);
		}
		// ATR
		if(candidates.keySet().contains("ATR")) 
		{
			Vector<Mapping<AbstractExpression>> options = new Vector<Mapping<AbstractExpression>>(candidates.get("ATR")); 
			for(Mapping<AbstractExpression> option: options) 
			{
				// check if there isn't already an analogous AVR in bestCandidates
				if(candidates.contains("AVR")) 
				{
					if(!containsAnalogousPattern(option, bestCandidates, srcIsComplex))
						bestCandidates.add(option);
				}	
				else
					bestCandidates.add(option);
			}
		}
		return bestCandidates;
	}

	

	/*
	 * This method removes child ADRs from a set of ADR mappings, i.e. if an ADR property or class restriction
	 * is another ADR's subproperty/class, then that more specific mapping is removed
	 * @param complexMappings: the set of ADR mappings from which we want to remove the children
	 * @param srcIsComplex: true if the source expression is the complex one
	 * @return: a clean set of ADRs (no children)
	 */
	private Set<Mapping<AbstractExpression>> removeADRChildren(Vector<Mapping<AbstractExpression>> complexMappings, boolean srcIsComplex) 
	{
		Set<Mapping<AbstractExpression>> result = new HashSet<Mapping<AbstractExpression>>();

		// Separate ADR components
		Vector<String> classURIs = new Vector<String>();
		Vector<String> relationURIs = new Vector<String>();

		for(Mapping<AbstractExpression> m: complexMappings) 
		{	
			AbstractExpression complexEntity = null;
			if(srcIsComplex)
				complexEntity = m.getEntity1();
			else
				complexEntity = m.getEntity2();

			for(Expression exp: complexEntity.getComponents())
			{
				if(exp instanceof ClassId)
					classURIs.add(((ClassId) exp).toURI());
				else
					relationURIs.add(((RelationId) exp).toURI());
			}
		}
		// Find children mappings indexes
		Map2Set<String, String> children = new Map2Set<String, String>(); //{ADR relation -> [subclasses and subrelations]}
		for(int i=0; i<complexMappings.size(); i++) 
		{
			String relation = relationURIs.get(i);
			children.addAll(relation, map.getSubclasses(classURIs.get(i)));
			children.addAll(relation, map.getSubproperties(relation));
		}
		// Add ancestor mappings to result set
		for(int i=0; i<complexMappings.size(); i++) 
		{
			String clas = classURIs.get(i);
			String relation = relationURIs.get(i);

			if(!children.contains(relation,clas) && !children.contains(relation, relation)) 
			{
				// Check if class specified in ontology as range and the class restricting the
				// range are the same. If so, mapping is redundant
				Set<String> ontRanges = new HashSet<String>(map.getRanges(relation));
				if(!ontRanges.contains(clas)) 
					result.add(complexMappings.get(i));
			}		
		}
		return result;
	}

	/*
	 * This method removes mappings containing child simple classes from a set of mappings
	 * @param mappings: the set of mappings containing at least one simple class from which we want to remove the children
	 * @return: a clean set of mappings (no children)
	 */
	private Set<Mapping<AbstractExpression>> removeSimpleChildren(Vector<Mapping<AbstractExpression>> mappings) 
	{
		Set<Mapping<AbstractExpression>> result = new HashSet<Mapping<AbstractExpression>>();
		if (mappings.size()==1) 
		{
			result.addAll(mappings);
			return result;
		}

		Vector<String> srcClassURIs = new Vector<String>();
		Vector<String> tgtClassURIs = new Vector<String>();
		Set<String> children = new HashSet<String>();

		for(Mapping<AbstractExpression> m: mappings) 
		{	
			String src = ((ClassId) m.getEntity1()).toURI();
			String tgt =  ((ClassId) m.getEntity2()).toURI();
			srcClassURIs.add(src);
			tgtClassURIs.add(tgt);
			children.addAll(map.getSubclasses(src));
			children.addAll(map.getSubclasses(tgt));
		}
		for(int i=0; i<mappings.size(); i++) 
		{	
			if(!children.contains(srcClassURIs.get(i)) && !children.contains(tgtClassURIs.get(i))) 
				result.add(mappings.get(i));
		}

		return result;
	}
	
	@Deprecated
	/*
	 * This method removes redundant ancestor ADRs from a set of ADR mappings, i.e. if an ADR property or class restriction
	 * is another ADR's superproperty/class, then that less specific mapping is removed
	 * @param complexMappings: the set of ADR mappings from which we want to remove the ancestors
	 * @param srcIsComplex: true if the source expression is the complex one
	 * @return: a clean set of ADRs (no ancestors)
	 */
	private Set<Mapping<AbstractExpression>> removeADRAncestor(Vector<Mapping<AbstractExpression>> complexMappings, boolean srcIsComplex) 
	{
		Set<Mapping<AbstractExpression>> result = new HashSet<Mapping<AbstractExpression>>();

		if (complexMappings.size()==1) 
		{
			result.addAll(complexMappings);
			return (result);
		}
		Vector<String> classURIs = new Vector<String>();
		Vector<String> relationURIs = new Vector<String>();

		// Separate ADR components
		for(Mapping<AbstractExpression> m: complexMappings) 
		{	
			AbstractExpression complexEntity = null;
			if(srcIsComplex)
				complexEntity = m.getEntity1();
			else
				complexEntity = m.getEntity2();

			for(Expression exp: complexEntity.getComponents())
			{
				if(exp instanceof ClassId)
					classURIs.add(((ClassId) exp).toURI());
				else
					relationURIs.add(((RelationId) exp).toURI());
			}
		}
		// Find ancestor mappings indexes
		Map2Set<String, String> ancestors = new Map2Set<String, String>(); //{relation -> class and relation ancestors}
		for(int i=0; i<complexMappings.size(); i++) 
		{
			String relation = relationURIs.get(i);
			ancestors.addAll(relation, map.getSuperclasses(classURIs.get(i)));
			ancestors.addAll(relation, map.getSuperproperties(relation));
		}
		// Add child mappings to result set
		for(int i=0; i<complexMappings.size(); i++) 
		{
			String clas = classURIs.get(i);
			String relation = relationURIs.get(i);

			if(!ancestors.contains(relation,clas) && !ancestors.contains(relation, relation)) 
			{
				// Check if class specified in ontology as range and the class restricting the
				// range are the same. If so, mapping is redundant
				Set<String> ontRanges = new HashSet<String>(map.getRanges(relation));
				boolean clear = true;
				for(String ontRangeURI: ontRanges) 
				{
					if(ontRangeURI.equals(clas)) 
					{
						clear=false;
						break;
					}
				}
				if(clear)
					result.add(complexMappings.get(i));
			}		
		}
		return result;
	}
}


