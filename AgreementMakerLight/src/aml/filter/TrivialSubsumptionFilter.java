package aml.filter;
import java.util.Objects;

import org.semanticweb.owlapi.model.OWLDatatype;

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
import aml.alignment.rdf.Comparator;
import aml.alignment.rdf.Expression;
import aml.alignment.rdf.PropertyExpression;
import aml.alignment.rdf.PropertyId;
import aml.alignment.rdf.RelationExpression;
import aml.alignment.rdf.RelationId;
import aml.alignment.rdf.ValueExpression;
import aml.ontology.ValueMap;
import aml.ontology.semantics.EntityMap;
import aml.util.data.Couple;
import aml.util.data.Map2Map2Set;

public class TrivialSubsumptionFilter implements Filterer
{
	EDOALAlignment a;
	protected EntityMap entMap;
	Map2Map2Set<String, AbstractExpression, AbstractExpression> eqvMaps;
	Map2Map2Set<String, AbstractExpression, AbstractExpression> subsMaps;
	ValueMap srcValueMap;
	ValueMap tgtValueMap;

	public TrivialSubsumptionFilter()
	{
		entMap = AML.getInstance().getEntityMap();
		eqvMaps = new Map2Map2Set<String, AbstractExpression, AbstractExpression>();
		subsMaps = new Map2Map2Set<String, AbstractExpression, AbstractExpression>();
		srcValueMap = AML.getInstance().getSource().getValueMap();
		tgtValueMap = AML.getInstance().getTarget().getValueMap();
	}

	@SuppressWarnings("rawtypes")
	/*
	 * This method checks for the following redundancy cases:
	 * if a=exp(b), a (< or >) exp(c) and b (< or >) c, then only a=exp(b) is relevant
	 * if a=exp(b), a (< or >) exp’(b) and exp (< or >) exp’ then only a=exp(b) is relevant
	 * @param out: alignment to filter
	 */
	public Alignment filter(Alignment a) 
	{
		if(!(a instanceof EDOALAlignment))
		{
			System.out.println("Warning: cannot filter non-EDOAL alignment!");
			return a;
		}
		System.out.println("Filtering trivial subsumption mappings");
		System.out.println("Size before: "+ a.size());
		long time = System.currentTimeMillis()/1000;
		this.a = (EDOALAlignment)a;

		// Separate mappings into simple or complex, equivalence or subsumption
		for(Mapping<AbstractExpression> m: this.a) 
		{
			AbstractExpression src = m.getEntity1();
			AbstractExpression tgt = m.getEntity2();
			String pattern = getClassPattern(src, tgt).get1();
			boolean srcIsComplex = getClassPattern(src, tgt).get2();

			// Populate maps
			if(srcIsComplex) // Map organised by simple entity
			{ // tgt -> src
				if(m.getRelationship().equals(MappingRelation.EQUIVALENCE)) 
					eqvMaps.add(pattern, m.getEntity2(), m.getEntity1()); 
				else if(m.getRelationship().equals(MappingRelation.SUBSUMED_BY) || 
						m.getRelationship().equals(MappingRelation.SUBSUMES)) 
					subsMaps.add(pattern, m.getEntity2(), m.getEntity1());
			}
			else 
			{ // src -> tgt
				if(m.getRelationship().equals(MappingRelation.EQUIVALENCE)) 
					eqvMaps.add(pattern, m.getEntity1(), m.getEntity2()); 
				else if(m.getRelationship().equals(MappingRelation.SUBSUMED_BY) || 
						m.getRelationship().equals(MappingRelation.SUBSUMES)) 
					subsMaps.add(pattern, m.getEntity1(), m.getEntity2());
			}		
		}

		if(eqvMaps.contains("Simple") && subsMaps.contains("Simple")) 
			simpleSubsumptionFilter();
		if(eqvMaps.contains("ADR") && subsMaps.contains("ADR")) 
			patternSubsumptionFilter("ADR");
		if(eqvMaps.contains("AOR") && subsMaps.contains("AOR")) 
			patternSubsumptionFilter("AOR");
		if(eqvMaps.contains("AVR") && subsMaps.contains("AVR")) 
			patternSubsumptionFilter("AVR");
		if(eqvMaps.contains("ATR") && subsMaps.contains("ATR")) 
			patternSubsumptionFilter("ATR");

		System.out.println("Size after: "+ a.size());
		System.out.println("Finished in " +	(System.currentTimeMillis()/1000-time) + " seconds");
		return a;
	}

	/*
	 * @return pair containing pattern (string) and whether source entity is complex (true if source is complex) 
	 */
	private Couple<String, Boolean>  getClassPattern(AbstractExpression src, AbstractExpression tgt) 
	{
		// Get class pattern
		if(src instanceof ClassId && tgt instanceof ClassId) 
			return new Couple<String, Boolean>("Simple", false);
		else if(src instanceof AttributeDomainRestriction) 
			return new Couple<String, Boolean>("ADR", true);
		else if(tgt instanceof AttributeDomainRestriction) 
			return new Couple<String, Boolean>("ADR", false);
		else if(src instanceof AttributeOccurrenceRestriction) 
			return new Couple<String, Boolean>("AOR", true);
		else if (tgt instanceof AttributeOccurrenceRestriction)
			return new Couple<String, Boolean>("AOR", false);
		else if(src instanceof AttributeValueRestriction) 
			return new Couple<String, Boolean>("AVR", true);
		else if (tgt instanceof AttributeValueRestriction)
			return new Couple<String, Boolean>("AVR", false);
		else if(src instanceof AttributeTypeRestriction) 
			return new Couple<String, Boolean>("ATR", true);
		else if (tgt instanceof AttributeTypeRestriction)
			return new Couple<String, Boolean>("ATR", false);
		return new Couple<String, Boolean>("", false);
	}

	private void patternSubsumptionFilter(String pattern) 
	{
		for(AbstractExpression c1: eqvMaps.get(pattern).keySet()) // c1 = eqvProperty ^ eqvRestriction
		{	
			if(!subsMaps.contains(pattern, c1))
				continue;
			for(AbstractExpression eqvComplexExpression: eqvMaps.get(pattern, c1))
			{
				AbstractExpression eqvProperty = null;
				AbstractExpression eqvRestriction = null;
				Comparator eqvComparator = null;

				for(Expression e: eqvComplexExpression.getComponents()) 
				{
					if(e instanceof Comparator)
						eqvComparator = (Comparator) e;
					else if (e instanceof PropertyExpression | e instanceof RelationExpression)
						eqvProperty = (AbstractExpression) e;
					else
						eqvRestriction = (AbstractExpression) e;
				}

				// Pattern eqv = pattern subsumption
				for(AbstractExpression subsComplexExpression: subsMaps.get(pattern, c1)) 
				{
					AbstractExpression subsProperty = null;
					AbstractExpression subsRestriction = null;

					for(Expression e: subsComplexExpression.getComponents()) 
					{
						if(e instanceof Comparator)
							continue;
						else if (e instanceof PropertyExpression | e instanceof RelationExpression)
							subsProperty = (AbstractExpression) e;
						else
							subsRestriction = (AbstractExpression) e;
					}

					// Compare properties -> for all class patterns
					if(eqvRestriction.equals(subsRestriction) && Objects.equals(eqvComparator, eqvRestriction)) 
					{
						String eqvPropertyURI = ((RelationId) eqvProperty).toURI();
						String subsPropertyURI = ((RelationId) subsProperty).toURI();

						if(entMap.getSuperproperties(eqvPropertyURI).contains(subsPropertyURI) || 
								entMap.getSuperproperties(subsPropertyURI).contains(eqvPropertyURI)) 
						{
							// At this point we lost track which are src and tgt entities so we need 
							// to check for both direction mappings
							if(a.contains(c1, subsComplexExpression))   
								a.remove(c1, subsComplexExpression);
							else 
								a.remove(subsComplexExpression, c1);
						}	 
					}
					// Compare other elements
					if(eqvProperty.equals(subsProperty)) 
					{
						if(pattern.equals("ADR"))
						{
							String eqvRestrictionURI = ((ClassId) eqvRestriction).toURI();
							String subsRestrictionURI = ((ClassId) subsRestriction).toURI();

							if(entMap.getSuperclasses(eqvRestrictionURI).contains(subsRestrictionURI) || 
									entMap.getSuperproperties(subsRestrictionURI).contains(eqvRestrictionURI))
							{
								// At this point we lost track which are src and tgt entities so we need 
								// to check for both direction mappings
								if(a.contains(c1, subsComplexExpression))   
									a.remove(c1, subsComplexExpression); // c1 < or > subsADR
								else 
									a.remove(subsComplexExpression, c1);
							}	 
						}
					}
				}

				// Pattern eqv != pattern subsumption
				if(pattern.equals("ATR")) 
				{
					for(AbstractExpression subsComplexExpression: subsMaps.get("AVR", c1)) 
					{
						AbstractExpression subsProperty = null;
						AbstractExpression subsRestriction = null; // value
						for(Expression e: subsComplexExpression.getComponents()) 
						{
							if(e instanceof Comparator)
								continue;
							else if (e instanceof PropertyExpression | e instanceof RelationExpression)
								subsProperty = (AbstractExpression) e;
							else
								subsRestriction = (AbstractExpression) e;
						}

						if(eqvProperty.equals(subsProperty)) 
						{
							// Check if value is the same type as ATR
							OWLDatatype eqvRestrictionType = (OWLDatatype) eqvRestriction;
							ValueExpression subsRestrictionValue = (ValueExpression) subsRestriction;
							String subsPropertyURI =  ((PropertyId) subsProperty).toURI();
							
							// subsRestrictionValue is of type eqvRestrictionType
							if(srcValueMap.getDataType(subsPropertyURI, subsRestrictionValue.toString()).contains(eqvRestrictionType)) 
							{
								// At this point we lost track which are src and tgt entities so we need 
								// to check for both direction mappings
								if(a.contains(c1, subsComplexExpression))   
									a.remove(c1, subsComplexExpression); // c1 < or > AVR
								else 
									a.remove(subsComplexExpression, c1);
							}
						}
					}
				}
				// Pattern eqv != pattern subsumption
				if(pattern.equals("AVR")) 
				{
					for(AbstractExpression subsComplexExpression: subsMaps.get("ATR", c1)) 
					{
						AbstractExpression subsProperty = null;
						AbstractExpression subsRestriction = null; // type

						for(Expression e: subsComplexExpression.getComponents()) 
						{
							if (e instanceof PropertyExpression | e instanceof RelationExpression)
								subsProperty = (AbstractExpression) e;
							else
								subsRestriction = (AbstractExpression) e;
						}

						if(eqvProperty.equals(subsProperty)) 
						{
							// Check if value is the same type as ATR
							ValueExpression eqvRestrictionValue = (ValueExpression) eqvRestriction;
							OWLDatatype subsRestrictionType = (OWLDatatype) subsRestriction;
							String eqvPropertyURI =  ((PropertyId) eqvProperty).toURI();
							
							// subsRestrictionValue is of type eqvRestrictionType
							if(srcValueMap.getDataType(eqvPropertyURI, eqvRestrictionValue.toString()).contains(subsRestrictionType)) 
							{
								// At this point we lost track which are src and tgt entities so we need 
								// to check for both direction mappings
								if(a.contains(c1, subsComplexExpression))   
									a.remove(c1, subsComplexExpression); // c1 < or > AVR
								else 
									a.remove(subsComplexExpression, c1);
							}
						}
					}

				}
			}
		}
	}

	private void simpleSubsumptionFilter() 
	{
		for(AbstractExpression c1: eqvMaps.get("Simple").keySet()) // c1 = c2
		{	
			if(!subsMaps.contains("Simple", c1))
				continue;
			for(AbstractExpression eqvClass: eqvMaps.get("Simple", c1))
			{
				for(AbstractExpression subsClass: subsMaps.get("Simple", c1)) // c1 > c3
				{
					String eqvClassURI = ((ClassId) eqvClass).toURI();
					String subsClassURI = ((ClassId) subsClass).toURI();
					
					if(entMap.getSuperclasses(eqvClassURI).contains(subsClassURI) || 
						entMap.getSuperproperties(subsClassURI).contains(eqvClassURI)) 
					{
						// At this point we lost track which are src and tgt entities so we need 
						// to check for both direction mappings
						if(a.contains(c1, subsClass))   
							a.remove(c1, subsClass); // c1 < or > subsADR
						else 
							a.remove(subsClass, c1);
					}
						
				}
			}
		}
	}

}

