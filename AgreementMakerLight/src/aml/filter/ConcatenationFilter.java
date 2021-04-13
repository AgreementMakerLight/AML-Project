package aml.filter;

import java.util.HashSet;
import java.util.Set;
import aml.alignment.Alignment;
import aml.alignment.EDOALAlignment;
import aml.alignment.mapping.EDOALMapping;
import aml.alignment.mapping.Mapping;
import aml.alignment.mapping.MappingRelation;
import aml.alignment.rdf.AbstractExpression;
import aml.alignment.rdf.ClassExpression;
import aml.alignment.rdf.ClassId;
import aml.alignment.rdf.ClassIntersection;
import aml.alignment.rdf.ClassUnion;
import aml.alignment.rdf.PropertyExpression;
import aml.alignment.rdf.PropertyId;
import aml.alignment.rdf.PropertyIntersection;
import aml.alignment.rdf.PropertyUnion;
import aml.alignment.rdf.RelationExpression;
import aml.alignment.rdf.RelationId;
import aml.alignment.rdf.RelationIntersection;
import aml.alignment.rdf.RelationUnion;
import aml.util.data.Map2Set;

public class ConcatenationFilter implements Filterer
{
	EDOALAlignment in;
	EDOALAlignment out;

	// Constructor
	public ConcatenationFilter() 
	{
		out = new EDOALAlignment();
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
		System.out.println("Concatenating");
		long time = System.currentTimeMillis()/1000;
		this.in = (EDOALAlignment)a;

		for(Mapping<AbstractExpression> m1 : in)
		{
			if(!in.superContainsConflict(m1))
				out.add(m1);
			else 
			{	
				// Get mapping type
				String mappingType = null;
				if(m1.getEntity1() instanceof ClassExpression)
					mappingType = "Class";
				else if (m1.getEntity1() instanceof RelationExpression)
					mappingType = "Relation";
				else
					mappingType = "Property";

				//Separate in Src and Tgt 
				Map2Set<MappingRelation, Mapping<AbstractExpression>> srcConflicts = new Map2Set<MappingRelation, Mapping<AbstractExpression>>();
				Map2Set<MappingRelation,Mapping<AbstractExpression>> tgtConflicts = new Map2Set<MappingRelation, Mapping<AbstractExpression>>();
				Map2Set<MappingRelation, Double> srcConfidence = new Map2Set<MappingRelation, Double>();
				Map2Set<MappingRelation, Double> tgtConfidence = new Map2Set<MappingRelation, Double>();

				for(Mapping<AbstractExpression> c: in.superGetConflicts(m1))
				{
					if(c.getEntity1().equals(m1.getEntity1())) 
					{
						srcConflicts.add(m1.getRelationship(), m1); // add itself
						srcConfidence.add(m1.getRelationship(),m1.getSimilarity());
						srcConflicts.add(c.getRelationship(),c);
						srcConfidence.add(c.getRelationship(),c.getSimilarity());
					}
					else 
					{
						tgtConflicts.add(m1.getRelationship(), m1); // add itself
						tgtConfidence.add(m1.getRelationship(),m1.getSimilarity());
						tgtConflicts.add(c.getRelationship(), c);
						tgtConfidence.add(c.getRelationship(), c.getSimilarity());
					}
				}

				if(srcConflicts.size() ==0) 
				{
					if(tgtConflicts.size() ==0) 
					{
						out.add(m1);
						continue;
					}
					else  // Concatenate with AND or OR 
					{
						for(Mapping<AbstractExpression> m: concatenate(tgtConflicts, tgtConfidence, mappingType, false))
							if(!out.contains(m))
								out.add(m);
					}
				}
				else if(tgtConflicts.size() ==0) 
				{
					for(Mapping<AbstractExpression> m: concatenate(srcConflicts, srcConfidence, mappingType, true))
						if(!out.contains(m))
							out.add(m);
				}
				else  
				{ 
					// Find out which side is complex
					int count=0;
					AbstractExpression src = null;
					for(MappingRelation r: srcConflicts.keySet()) 
					{
						if(count ==1)
							break;
						src = srcConflicts.get(r).iterator().next().getEntity1();
						count++;
					}
					//Concatenate complex side
					if(src instanceof ClassId || src instanceof RelationId || src instanceof PropertyId) 
					{
						for(Mapping<AbstractExpression> m: concatenate(tgtConflicts, tgtConfidence, mappingType, false))
							if(!out.contains(m))
								out.add(m);
					}
					else 
					{
						for(Mapping<AbstractExpression> m: concatenate(srcConflicts, srcConfidence, mappingType, true))
							if(!out.contains(m))
								out.add(m);
					}
				}
			}
		}
		System.out.println("Filtered out "+ (in.size() - out.size()) + " mappings");
		System.out.println("Finished in " +	(System.currentTimeMillis()/1000-time) + " seconds");
		return out;
	}

	/*
	 * This method concatenates conflicting mappings
	 * @param conflicts: conflicting mappings organised by type of mapping relation
	 * @param similarity: similarity of mappings organised by type of mapping relation
	 * @param mappingType: 
	 * @param srcIsCommon: true if the common member of the conflicting mappings is the soruce; false if it's the target
	 * @return
	 */
	private Set<Mapping<AbstractExpression>> concatenate(Map2Set<MappingRelation, Mapping<AbstractExpression>> conflicts,
			Map2Set<MappingRelation, Double> similarity, String mappingType, boolean srcIsCommon) 
	{
		Set<Mapping<AbstractExpression>> maps = new HashSet<Mapping<AbstractExpression>>();
		for(MappingRelation r : conflicts.keySet()) 
		{
			double minConf = findMinimum(similarity.get(r));
			//CLASSES
			if(mappingType.equals("Class")) 
			{
				Set<ClassExpression> nonCommonEntities = new HashSet<ClassExpression>();
				ClassExpression commonEntity = null;

				// Fetch conflicting members only
				if(srcIsCommon)
				{
					commonEntity = (ClassExpression) conflicts.get(r).iterator().next().getEntity1();
					for(Mapping<AbstractExpression> m: conflicts.get(r))
						nonCommonEntities.add((ClassExpression) m.getEntity2());
				}
				else 
				{
					commonEntity = (ClassExpression) conflicts.get(r).iterator().next().getEntity2();
					for(Mapping<AbstractExpression> m: conflicts.get(r))
						nonCommonEntities.add((ClassExpression) m.getEntity1());
				}
				// Concatenate
				if(minConf == 1.0) // AND
				{
					ClassIntersection intersection = new ClassIntersection(nonCommonEntities);
					maps.add(generateMapping(srcIsCommon, commonEntity, intersection, 1.0, r));
				}
				else //OR 
				{
					ClassUnion union = new ClassUnion(nonCommonEntities);
					maps.add(generateMapping(srcIsCommon, commonEntity, union, minConf, r));
				}
			}
			// RELATION
			else if(mappingType.equals("Relation")) 
			{
				Set<RelationExpression> nonCommonEntities = new HashSet<RelationExpression>();
				RelationExpression commonEntity = null;

				// Fetch conflicting members only
				if(srcIsCommon)
				{
					commonEntity = (RelationExpression) conflicts.get(r).iterator().next().getEntity1();
					for(Mapping<AbstractExpression> m: conflicts.get(r))
						nonCommonEntities.add((RelationExpression) m.getEntity2());
				}
				else 
				{
					commonEntity = (RelationExpression) conflicts.get(r).iterator().next().getEntity2();
					for(Mapping<AbstractExpression> m: conflicts.get(r))
						nonCommonEntities.add((RelationExpression) m.getEntity1());
				}

				if(minConf == 1.0) // AND
				{
					RelationIntersection intersection = new RelationIntersection(nonCommonEntities);
					maps.add(generateMapping(srcIsCommon, commonEntity, intersection, 1.0, r));
				}
				else //OR 
				{
					RelationUnion union = new RelationUnion(nonCommonEntities);
					maps.add(generateMapping(srcIsCommon, commonEntity, union, minConf, r));
				}
			}
			// PROPERTY
			else if(mappingType.equals("Property")) 
			{
				Set<PropertyExpression> nonCommonEntities = new HashSet<PropertyExpression>();
				PropertyExpression commonEntity = null;

				// Fetch conflicting members only
				if(srcIsCommon)
				{
					commonEntity = (PropertyExpression) conflicts.get(r).iterator().next().getEntity1();
					for(Mapping<AbstractExpression> m: conflicts.get(r))
						nonCommonEntities.add((PropertyExpression) m.getEntity2());
				}
				else 
				{
					commonEntity = (PropertyExpression) conflicts.get(r).iterator().next().getEntity2();
					for(Mapping<AbstractExpression> m: conflicts.get(r))
						nonCommonEntities.add((PropertyExpression) m.getEntity1());
				}

				if(minConf == 1.0) // AND
				{
					PropertyIntersection intersection = new PropertyIntersection(nonCommonEntities);
					maps.add(generateMapping(srcIsCommon, commonEntity, intersection, 1.0, r));
				}
				else //OR 
				{
					PropertyUnion union = new PropertyUnion(nonCommonEntities);
					maps.add(generateMapping(srcIsCommon, commonEntity, union, minConf, r));
				}
			}
		}
		return maps;
	}

	private double findMinimum(Set<Double> set) 
	{
		double min = 10.0;
		for(double n: set) 
		{
			if(n<min)
				min = n;
		}
		return min;
	}

	private Mapping<AbstractExpression> generateMapping(boolean srcIsCommon, AbstractExpression commonEntity, 
			AbstractExpression construct, double conf, MappingRelation r) 
	{
		if(srcIsCommon) 
			return new EDOALMapping(commonEntity, construct, conf, r);
		else 
			return new EDOALMapping(construct, commonEntity, conf, r);	
	}
}


