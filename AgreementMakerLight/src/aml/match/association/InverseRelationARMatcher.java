package aml.match.association;

import java.util.HashSet;
import java.util.Set;

import aml.AML;
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
		EntityMap rels = AML.getInstance().getEntityMap();

		for(String si : sharedInstances) 
		{
			// Find all relations associated to this instance
			// While filtering out instances with no relationships
			if (rels.getIndividualActiveRelations(si) != null) 
			{
				Set<String> individuals2 = new HashSet<String>(rels.getIndividualActiveRelations(si));
				for (String i2: individuals2) 
				{
					//Set of relations
					Set<String> rSet = rels.getIndividualProperties(si, i2); //Avoid duplicates
					Set<String> rInvSet = rels.getIndividualProperties(i2, si); //Avoid duplicates

					if(rSet.size()<1 || rInvSet.size()<1)
						continue;

					//Iterate list of relations to count support
					for (String rURI: rSet)
					{
						// Transform string uri into correspondent AbstractExpression
						RelationId r = new RelationId(rURI);
						// Add relation to EntitySupport
						incrementEntitySupport(r);

						for (String rInvURI: rInvSet) 
						{
							RelationId rInv = new RelationId(rInvURI);
							// Make sure we are not mapping entities from the same ontology
							if( (o1.contains(rURI) && o1.contains(rInvURI)) 
									| (!o1.contains(rURI) && !o1.contains(rInvURI)))
								continue;
							// Add to MappingSupport
							incrementMappingSupport(r, rInv);
						}
					}
					
					// Add inverse relations to entity support as well
					for (String rInvURI: rInvSet) 
					{
						RelationId rInv = new RelationId(rInvURI);
						incrementEntitySupport(rInv);
					}
				}
			}
		}
	}
}
