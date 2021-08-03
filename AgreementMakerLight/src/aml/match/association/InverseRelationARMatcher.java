package aml.match.association;

import java.util.HashSet;
import java.util.Set;

import aml.AML;
import aml.alignment.rdf.AbstractExpression;
import aml.alignment.rdf.InverseRelation;
import aml.alignment.rdf.RelationId;
import aml.ontology.Ontology;
import aml.ontology.semantics.EntityMap;

public class InverseRelationARMatcher extends AbstractAssociationRuleMatcher
{
	// Constructor
	public InverseRelationARMatcher() {}

	//Protected methods
	/*
	 * Populates EntitySupport and MappingSupport tables
	 * @param o1: source ontology
	 * @param o2: target ontology
	 */
	protected void computeSupport(Ontology o1, Ontology o2) 
	{
		System.out.println("Initialising Inverse Relation Matcher");
		Set<String> sharedInstances = new HashSet<String>(getSharedInstances(o1,o2));
		EntityMap map = AML.getInstance().getEntityMap();

		for(String si : sharedInstances) 
		{
			// Find all relations associated to this instance
			Set<String> individuals2 = new HashSet<String>(map.getIndividualActiveRelations(si));
			for (String i2: individuals2) 
			{
				//Set of relations
				Set<String> r1Set = map.getIndividualPropertiesTransitive(si, i2);
				Set<String> r2Set = map.getIndividualPropertiesTransitive(i2, si);

				//Iterate list of relations to count support
				for (String r1URI: r1Set)
				{
					// Transform string uri into correspondent AbstractExpression
					RelationId r1 = new RelationId(r1URI);
					InverseRelation r1Inv = new InverseRelation(r1);
					// Add relation to EntitySupport
					incrementEntitySupport(r1);
					incrementEntitySupport(r1Inv);

					for (String r2URI: r2Set) 
					{
						// Make sure we are not mapping entities from the same ontology
						if((o1.contains(r1URI) && o1.contains(r2URI)) || 
								(!o1.contains(r1URI) && !o1.contains(r2URI)))
							continue;

						RelationId r2 = new RelationId(r2URI);
						InverseRelation r2Inv = new InverseRelation(r2);
						// Add to MappingSupport
						incrementMappingSupport(r1, r2Inv);
						incrementMappingSupport(r1Inv, r2);
					}
				}			
				// Add inverse relations to entity support as well
				for (String r2URI: r2Set) 
				{
					RelationId r2 = new RelationId(r2URI);
					InverseRelation r2Inv = new InverseRelation(r2);
					incrementEntitySupport(r2);
					incrementEntitySupport(r2Inv);
				}
			}
		}
	}
}

