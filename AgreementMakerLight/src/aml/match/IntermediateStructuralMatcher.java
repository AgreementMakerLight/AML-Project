package aml.match;

import java.util.Set;

import aml.AML;
import aml.ontology.Ontology;
import aml.ontology.RelationshipMap;

public class IntermediateStructuralMatcher implements SecondaryMatcher, Rematcher
{

//Attributes
	
	//Links to ontology data structures
	private RelationshipMap rels;
	private Ontology source;
	private Ontology target;
	private Alignment input;
	
//Constructors
		
	public IntermediateStructuralMatcher()
	{
		AML aml = AML.getInstance();
		rels = aml.getRelationshipMap();
		source = aml.getSource();
		target = aml.getTarget();
	}
	
//Public Methods
		
	@Override
	public Alignment extendAlignment(Alignment a, double thresh)
	{
		input = a;
		Alignment maps = new Alignment();
		for(int i = 0; i < input.size(); i++)
		{
			Mapping m = input.get(i);
			Set<Integer> sourceSuperClasses = rels.getSuperClasses(m.getSourceId(),true);
			Set<Integer> targetSuperClasses = rels.getSuperClasses(m.getTargetId(),true);
			for(Integer s : sourceSuperClasses)
			{
				if(input.containsSource(s) || maps.containsSource(s))
					continue;
				for(Integer t : targetSuperClasses)
				{
					if(input.containsTarget(t) || maps.containsTarget(t))
						continue;
					double sim = mapTwoTerms(s, t);
					if(sim >= thresh)
						maps.add(s,t,sim);
				}
			}
		}
		return maps;
	}

	@Override
	public Alignment rematch(Alignment a)
	{
		Alignment maps = new Alignment();
		for(Mapping m : a)
		{
			int sId = m.getSourceId();
			int tId = m.getTargetId();
			maps.add(sId,tId,mapTwoTerms(sId,tId));
		}
		return maps;
	}
	
//Private Methods
	
	//Computes the intermediate structural similarity between two terms by
	//checking for mappings between all their ancestors
	private double mapTwoTerms(int sId, int tId)
	{
		if(!source.isClass(sId) || ! target.isClass(tId))
			return 0.0;
		Set<Integer> sourceParents = rels.getSuperClasses(sId,false);
		Set<Integer> targetParents = rels.getSuperClasses(tId,false);
		double union = 0.0;
		double sim = 0.0;
		for(Integer i : sourceParents)
		{
			for(Integer j : targetParents)
			{
				if(input.containsMapping(i,j))
					sim += 2 / (rels.getDistance(sId,i) + rels.getDistance(tId, j));
				else
					union += 2 / (rels.getDistance(sId,i) + rels.getDistance(tId, j));
			}
		}
		return sim/(sim+union);
	}
}