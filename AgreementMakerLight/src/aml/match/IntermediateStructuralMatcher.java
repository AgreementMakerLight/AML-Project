package aml.match;

import java.util.Set;

import aml.AML;
import aml.ontology.RelationshipMap;

public class IntermediateStructuralMatcher implements SecondaryMatcher
{

	@Override
	public Alignment extendAlignment(Alignment a, double thresh)
	{
		AML aml = AML.getInstance();
		RelationshipMap rels = aml.getRelationshipMap();
		
		Alignment maps = new Alignment();
		for(int i = 0; i < a.size(); i++)
		{
			Mapping m = a.get(i);
			Set<Integer> sourceGrandpas = rels.getAncestors(m.getSourceId(),2);
			Set<Integer> targetGrandpas = rels.getAncestors(m.getTargetId(),2);
			Set<Integer> sourceParents = rels.getAncestors(m.getSourceId(),1);
			Set<Integer> targetParents = rels.getAncestors(m.getTargetId(),1);
			for(Integer s : sourceGrandpas)
			{
				for(Integer t : targetGrandpas)
				{
					if(a.containsMapping(s,t))
					{
						for(Integer u : sourceParents)
						{
							if(!rels.containsSubClass(u, s))
								continue;
							for(Integer v : targetParents)
								if(rels.containsSubClass(v, t))
									maps.add(u,v,0.6);
						}
					}
				}
			}
		}
		return maps;
	}	
}
