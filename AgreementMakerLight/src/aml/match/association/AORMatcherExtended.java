package aml.match.association;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import aml.AML;
import aml.alignment.rdf.AbstractExpression;
import aml.alignment.rdf.AttributeOccurrenceRestriction;
import aml.alignment.rdf.ClassId;
import aml.alignment.rdf.Comparator;
import aml.alignment.rdf.NonNegativeInteger;
import aml.alignment.rdf.RelationId;
import aml.ontology.Ontology;
import aml.ontology.semantics.EntityMap;
import aml.util.data.Map2Map;
import aml.util.data.Map2Map2Map;

public class AORMatcherExtended extends AbstractAssociationRuleMatcher
{
	// Attributes
	protected Map2Map<String, Integer, Integer> preEntitySupport; // PropertyURI -> Restriction -> Support
	protected Map2Map2Map<String, String, Integer, Integer> preMappingSupport; // ClassURI -> PropertyURI -> Restriction -> Support
	protected Map2Map<String, String, Integer> min; // ClassURI -> PropertyURI -> Min Restriction
	protected Map2Map<String, String, Integer> max; // ClassURI -> PropertyURI -> Max Restriction

	//Constructor
	public AORMatcherExtended() 
	{
		preEntitySupport = new Map2Map<String, Integer, Integer>();
		preMappingSupport = new Map2Map2Map<String, String, Integer, Integer>();
		min = new Map2Map<String, String, Integer>();
		max = new Map2Map<String, String, Integer>();
	}

	protected void computeSupport(Ontology o1, Ontology o2) 
	{
		System.out.println("Initialising Attribute Occurence Matcher");
		// Get entity map of relations
		Set<String> sharedInstances = new HashSet<String>(getSharedInstances(o1,o2));
		EntityMap rels = AML.getInstance().getEntityMap();

		for(String si : sharedInstances) 
		{
			// Find all classes associated to that instance
			List<String> i1Classes = new ArrayList<String>(rels.getIndividualClassesTransitive(si));
			// If empty set of classes, move on to next instance
			int len = i1Classes.size();
			if(len < 1)
				continue;

			// Increment class support
			for (String cURI: i1Classes) 
				incrementEntitySupport(new ClassId(cURI));

			// Map of frequency of attribute in this instance (si)
			HashMap<String, Integer> foundAORsInstance = new HashMap<String, Integer>();

			// If map does not have any other triples involving this instance, move on to next instance
			Set<String> activeRelations = new HashSet<String>(rels.getIndividualActiveRelations(si));
			if(activeRelations.size()==0)
				continue;

			for(String i2: activeRelations) 
			{
				//Iterate relations between si and i2 and increment attribute count
				for (String attributeURI: rels.getIndividualProperties(si, i2)) 
				{
					if(foundAORsInstance.containsKey(attributeURI))
						foundAORsInstance.put(attributeURI, foundAORsInstance.get(attributeURI)+1);
					else
						foundAORsInstance.put(attributeURI, 1);
				}
			}

			for(String propURI: foundAORsInstance.keySet()) 
			{
				int freq = foundAORsInstance.get(propURI);
				// Increment support for all restrictions ranging from 0 to freq
				for(int i=0; i<freq; i++) 
				{
					incrementPreEntitySupport(propURI, freq-i);
					// Increment mapping support
					for (String c1URI: i1Classes) 
					{
						// Filter out cases where relation is from the same ontology as c1
						if (o1.contains(c1URI) && o1.contains(propURI)) {continue;}
						else if(o2.contains(c1URI) && o2.contains(propURI)){continue;}
						incrementPreMappingSupport(c1URI, propURI, freq-i);
					}		
				}
				// Find min/ max
				for (String cURI: i1Classes) 
				{
					// Filter out cases where relation is from the same ontology as c1
					if (o1.contains(cURI) && o1.contains(propURI)) {continue;}
					else if(o2.contains(cURI) && o2.contains(propURI)){continue;}

					//Min
					if(!min.contains(cURI, propURI)) 
						min.add(cURI, propURI, freq);
					else if(min.get(cURI, propURI) > freq)
						min.add(cURI, propURI, freq);
					// Max
					if(!max.contains(cURI, propURI)) 
						max.add(cURI, propURI, freq);
					else if(max.get(cURI, propURI) < freq)
						max.add(cURI, propURI, freq);
				}
			}
		}

		// Populate support tables according to min and max
		for(String cURI: min.keySet())
		{
			ClassId cId = new ClassId(cURI);
			for(String propURI: min.get(cURI).keySet()) 
			{
				int minFreq = min.get(cURI, propURI);
				int maxFreq = max.get(cURI, propURI);

				// If min=max, we have an exact cardinality restriction
				if(minFreq == maxFreq)
				{
					AttributeOccurrenceRestriction aor = new AttributeOccurrenceRestriction(
							new RelationId(propURI), 
							new Comparator("http://ns.inria.org/edoal/1.0/#equals"),
							new NonNegativeInteger(minFreq));

					entitySupport.put(aor, preEntitySupport.get(propURI, minFreq));
					mappingSupport.add(cId, aor, preMappingSupport.get(cURI, propURI, minFreq));
					mappingSupport.add(aor, cId, preMappingSupport.get(cURI, propURI, minFreq));
				}
				else 
				{
					// MIN
					AttributeOccurrenceRestriction aor1 = new AttributeOccurrenceRestriction(
							new RelationId(propURI), 
							new Comparator("http://ns.inria.org/edoal/1.0/#greater-than"),
							new NonNegativeInteger(minFreq-1));

					entitySupport.put(aor1, preEntitySupport.get(propURI, minFreq));
					mappingSupport.add(cId, aor1, preMappingSupport.get(cURI, propURI, minFreq));
					mappingSupport.add(aor1, cId, preMappingSupport.get(cURI, propURI, minFreq));

					//MAX
					AttributeOccurrenceRestriction aor2 = new AttributeOccurrenceRestriction(
							new RelationId(propURI), 
							new Comparator("http://ns.inria.org/edoal/1.0/#lesser-than"),
							new NonNegativeInteger(maxFreq+1));

					// NOTE: the fact that this is a lesser-than aor means that its support is actually that of
					// all the restrictions that have a cardinality lower than maxFreq (i.e. support of "propURI equals 1")
					entitySupport.put(aor2, preEntitySupport.get(propURI, 1)); 
					mappingSupport.add(cId, aor2, preMappingSupport.get(cURI, propURI, 1));
					mappingSupport.add(aor2, cId, preMappingSupport.get(cURI, propURI, 1));
				}
			}
		}	
	}
	/*
	 * Increments entity support
	 * @param e: entity to account for
	 */
	protected void incrementPreEntitySupport(String e, Integer rest) 
	{
		if(!preEntitySupport.contains(e, rest))
			preEntitySupport.add(e, rest, 1);
		else 
			preEntitySupport.add(e, rest, preEntitySupport.get(e,rest)+1);
	}

	/*
	 * Increments mapping support for entities e1 and e2
	 * It's a symmetric map to facilitate searches
	 */
	protected void incrementPreMappingSupport(String clazz, String prop, Integer rest) 
	{
		if(!preMappingSupport.contains(clazz, prop, rest)) 
			preMappingSupport.add(clazz, prop, rest, 1);
		else
			preMappingSupport.add(clazz, prop, rest, preMappingSupport.get(clazz, prop, rest)+1);
	}
}
