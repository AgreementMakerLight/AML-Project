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
import aml.alignment.rdf.ClassId;
import aml.ontology.semantics.EntityMap;
import aml.util.data.Map2Set;

public class RedundantComplexFilter implements Filterer
{
	EDOALAlignment in;
	EDOALAlignment out;
	protected EntityMap map;

	// Constructor
	public RedundantComplexFilter() 
	{
		out = new EDOALAlignment();
		map = AML.getInstance().getEntityMap();
	}

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
		this.in = (EDOALAlignment) a;
		
		for(Mapping<AbstractExpression> m: in) 
		{
			if(m.getEntity1() instanceof ClassId && m.getEntity2() instanceof ClassId) 
			{
				out.add(m);
				continue;
			}
			// Organize conflicting mappings (conflict in the complex side) by mapping relation
			Map2Set<MappingRelation, Mapping<AbstractExpression>> conflicts = new Map2Set<MappingRelation, Mapping<AbstractExpression>>();
			conflicts.add(m.getRelationship(), m); // add itself
			if(m.getEntity1() instanceof ClassId) // tgt is complex
			{
				for(Mapping<AbstractExpression> conflict: in.getTargetConflicts(m)) 
					conflicts.add(conflict.getRelationship(), conflict);	
			}
			else // src is complex
			{
				for(Mapping<AbstractExpression> conflict: in.getSourceConflicts(m)) 
					conflicts.add(conflict.getRelationship(), conflict);
			}
			if(conflicts.size()==1)
				out.add(m);
			
			// Remove redundant mappings for each mapping relation type
			for(MappingRelation relation: conflicts.keySet()) 
			{
				Vector<Mapping<AbstractExpression>> complexMappings = new Vector<Mapping<AbstractExpression>>(conflicts.get(relation));
				out.addAll(removeComplexChildren(complexMappings));
			}
		}

		System.out.println("Filtered out "+ (in.size() - out.size()) + " mappings");
		System.out.println("Finished in " +	(System.currentTimeMillis()/1000-time) + " seconds");
		return out;
	}
	
	/*
	 * This method removes mappings containing child simple classes from a set of complex mappings and also 
	 * removes those with lower confidence
	 * @param mappings: the set of mappings containing at least one simple class from which we want to remove the children
	 * @return: a clean set of mappings (no children)
	 */
	private Vector<Mapping<AbstractExpression>> removeComplexChildren(Vector<Mapping<AbstractExpression>> mappings) 
	{
		if (mappings.size()==1)
			return mappings;

		Vector<String> classURIs = new Vector<String>();
		Set<String> children = new HashSet<String>();
		Vector<Mapping<AbstractExpression>> result = new Vector<Mapping<AbstractExpression>>();
		double maxConf = 0.0;
		
		for(Mapping<AbstractExpression> m: mappings) 
		{	
			String simpleEntity = null;
			if(m.getEntity1() instanceof ClassId)
				simpleEntity = ((ClassId) m.getEntity1()).toURI();
			else 
				simpleEntity = ((ClassId) m.getEntity2()).toURI();

			classURIs.add(simpleEntity);
			children.addAll(map.getSubclasses(simpleEntity));
			
			if(m.getSimilarity() > maxConf)
				maxConf = m.getSimilarity();
		}
		for(int i=0; i<mappings.size(); i++) 
		{	
			if(!children.contains(classURIs.get(i)) && mappings.get(i).getSimilarity() >= maxConf) 
				result.add(mappings.get(i));
		}
		return result;
	}
}
