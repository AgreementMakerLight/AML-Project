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
import aml.alignment.rdf.RelationId;
import aml.ontology.semantics.EntityMap;
import aml.util.data.Map2Map2Set;
import aml.util.data.Map2Set;

public class SemanticClassFilter implements Filterer
{
	EDOALAlignment in;
	EDOALAlignment out;
	protected EntityMap map;
	protected final double minConf = 0.7;

	// Constructor
	public SemanticClassFilter() 
	{
		out = new EDOALAlignment();
		map = AML.getInstance().getEntityMap();
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
		System.out.println("Size before: "+ a.size());
		long time = System.currentTimeMillis()/1000;
		this.in = (EDOALAlignment)a;
		in.sortDescending();

		for(Mapping<AbstractExpression> m : in)
		{
			if(out.superContainsConflict(m))
				continue;
			// Find Class mappings
			if((m.getEntity1() instanceof aml.alignment.rdf.ClassExpression)) 
			{
				Vector<Mapping<AbstractExpression>> conflicts = in.superGetConflicts(m);
				conflicts.add(m); // Add itself
				Map2Map2Set< MappingRelation, String, Mapping<AbstractExpression>> candidates = new Map2Map2Set<MappingRelation, String, Mapping<AbstractExpression>>();
				Set<Mapping<AbstractExpression>> bestCandidates = new HashSet<Mapping<AbstractExpression>>();
				double maxConfEquiv = 0.0;
				double maxConfSubs = 0.0;
				if(conflicts.size()==1) 
					out.add(m);

				// Separate conflicting mappings into simple or complex, equivalence or subsumption
				for(Mapping<AbstractExpression> option: conflicts) 
				{
					// Find maximum confidence in equivalence and subsumption mappings
					if(option.getRelationship() == MappingRelation.EQUIVALENCE && option.getSimilarity() >= maxConfEquiv)
						maxConfEquiv = option.getSimilarity();
					else if ((option.getRelationship() == MappingRelation.SUBSUMED_BY || option.getRelationship() == MappingRelation.SUBSUMES) 
							&& option.getSimilarity() >= maxConfSubs)
						maxConfSubs = option.getSimilarity();

					// Add to candidates map
					candidates.add(option.getRelationship(), getClassMappingPattern(option), option);
				}

				// EQUIVALENCE MAPPINGS
				if(candidates.keySet().contains(MappingRelation.EQUIVALENCE))
					bestCandidates.addAll(processClassCandidates(candidates, MappingRelation.EQUIVALENCE, maxConfEquiv));
				// SUBSUMPTION MAPPINGS
				if(candidates.keySet().contains(MappingRelation.SUBSUMED_BY)) 
					bestCandidates.addAll(processClassCandidates(candidates, MappingRelation.SUBSUMED_BY, maxConfSubs));
				if(candidates.keySet().contains(MappingRelation.SUBSUMES)) 
					bestCandidates.addAll(processClassCandidates(candidates, MappingRelation.SUBSUMES, maxConfSubs));

				// Add best candidates to final alignment
				out.addAll(bestCandidates);
			}
			else
				out.add(m);
		}
		System.out.println("Size after: "+ out.size());
		System.out.println("Finished in " +	(System.currentTimeMillis()/1000-time) + " seconds");
		return out;
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

	private Set<Mapping<AbstractExpression>> processClassCandidates(Map2Map2Set< MappingRelation, String, Mapping<AbstractExpression>> candidates, 
			MappingRelation mapRelation, double thres) 
	{
		Set<Mapping<AbstractExpression>> bestCandidates = new HashSet<Mapping<AbstractExpression>>();
		// SIMPLE
		if(candidates.get(mapRelation).keySet().contains("Simple")) 
		{
			for(Mapping<AbstractExpression> option: candidates.get(mapRelation, "Simple")) 
			{
				if(option.getSimilarity() >= thres) 
					bestCandidates.add(option);
			}
		}
		// COMPLEX
		if(bestCandidates.size() == 0 & candidates.get(mapRelation).keySet().contains("ADR")) 
		{
			Vector<Mapping<AbstractExpression>> complexSrc = new Vector<Mapping<AbstractExpression>>();
			Vector<Mapping<AbstractExpression>> complexTgt = new Vector<Mapping<AbstractExpression>>();

			// Separate source and target ADRs
			for(Mapping<AbstractExpression> option: candidates.get(mapRelation, "ADR")) 
			{
				if(option.getSimilarity() < thres)
					continue;
				if(option.getEntity1() instanceof ClassId)
					complexTgt.add(option);
				else
					complexSrc.add(option);
			}
			// Remove ancestors and redundant mappings
			if(complexSrc.size()>0)
				bestCandidates.addAll(removeADRAncestor(complexSrc, true));
			if(complexTgt.size()>0)
				bestCandidates.addAll(removeADRAncestor(complexTgt, false));
		}
		// AOR
		if(bestCandidates.size() == 0 & candidates.get(mapRelation).keySet().contains("AOR")) 
		{
			for(Mapping<AbstractExpression> option: candidates.get(mapRelation, "AOR")) 
			{
				if(option.getSimilarity() >= thres) 
					bestCandidates.add(option);
			}
		}
		// AVR
		if(bestCandidates.size() == 0 & candidates.get(mapRelation).keySet().contains("AVR")) 
		{
			for(Mapping<AbstractExpression> option: candidates.get(mapRelation, "AVR")) 
			{
				if(option.getSimilarity() >= thres) 
					bestCandidates.add(option);
			}
		}
		// ATR
		if(bestCandidates.size() == 0 & candidates.get(mapRelation).keySet().contains("ATR")) 
		{
			for(Mapping<AbstractExpression> option: candidates.get(mapRelation, "AVR")) 
			{
				if(option.getSimilarity() >= thres) 
					bestCandidates.add(option);
			}
		}
		return bestCandidates;
	}

	/*
	 * This method removes redundant ancestor ADRs from a set of ADR mappings, i.e. if an ADR property or class restriction
	 * is another ADR's superproperty/class, then that less specific mapping is removed
	 * @param complexMappings: the set of ADR mappings from which we want to remove the ancestors
	 * @param srcIsComplex: true if the source expression is the complex one
	 * @return: a clean set of ADRs (no ancestors)
	 */
	private Set<Mapping<AbstractExpression>> removeADRAncestor(Vector<Mapping<AbstractExpression>> complexMappings, boolean srcIsComplex) 
	{
		Vector<String> classURIs = new Vector<String>();
		Vector<String> relationURIs = new Vector<String>();
		Set<Mapping<AbstractExpression>> result = new HashSet<Mapping<AbstractExpression>>();

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
				Set<String> ontRanges = map.getRanges(relation);
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


