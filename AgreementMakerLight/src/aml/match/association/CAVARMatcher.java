package aml.match.association;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLDatatype;
import aml.AML;
import aml.alignment.rdf.AttributeTypeRestriction;
import aml.alignment.rdf.ClassId;
import aml.alignment.rdf.Datatype;
import aml.alignment.rdf.PropertyId;
import aml.ontology.Ontology;
import aml.ontology.ValueMap;
import aml.ontology.semantics.EntityMap;

public class CAVARMatcher extends aml.match.association.AbstractAssociationRuleMatcher
{

	// Attributes

	//Constructor
	public CAVARMatcher() 
	{
		super();
	}

	//Protected methods
	/**
	 * Populates EntitySupport and MappingSupport tables
	 * CAV - Class by attribute value
	 * We want to find rules of the type: Class -> object property| data type
	 * being the antecedent and consequent from different ontologies 
	 */
	protected void computeSupport(Ontology o1, Ontology o2) 
	{
		// Get entity map of relations
		Set<String> sharedInstances = new HashSet<String>(getSharedInstances(o1,o2));
		EntityMap rels = AML.getInstance().getEntityMap();
		ValueMap srcValueMap = o1.getValueMap();
		ValueMap tgtValueMap = o2.getValueMap();
		Set<String> srcProperties = null;
		Set<String> tgtProperties = null;
		Set<String> equivIndv = new HashSet<String>();


		for(String si : sharedInstances) 
		{
			// Find all classes of that instance
			Set<String> cSet = rels.getIndividualClasses(si);

			// See if equivalent instances have classes as well
			if(rels.getEquivalentIndividuals(si) != null) 
			{
				equivIndv= rels.getEquivalentIndividuals(si);
				for(String i: equivIndv) 
					cSet.addAll(rels.getIndividualClasses(i));
			}

			// Switch to list since we need indexes 
			List<String> i1Classes = new ArrayList<String>(cSet);
			// If empty list of classes, move on to next instance
			int len = i1Classes.size();
			if(len < 1) {continue;}

			// If map contains any properties linking to that instance, get them
			if (srcValueMap.getProperties(si) != null) 
				srcProperties = new HashSet<String>(srcValueMap.getProperties(si));
			if (tgtValueMap.getProperties(si) != null) 
				tgtProperties = new HashSet<String>(tgtValueMap.getProperties(si));

			// See if equivalent instances have more properties to be added to the list
			if (equivIndv.size()>0) 
			{
				for(String eqv: equivIndv) 
					srcProperties.addAll(srcValueMap.getProperties(eqv));
				for(String eqv: equivIndv) 
					tgtProperties.addAll(tgtValueMap.getProperties(eqv));
			}
			
			for (int i = 0; i < len; i++) 
			{
				// Transform string uri into correspondent AbstractExpression
				String c1URI = i1Classes.get(i);
				if (c1URI.equals("http://www.w3.org/2002/07/owl#Thing")) {continue;}
				ClassId c1 = new ClassId(c1URI);

				// Add class to EntitySupport if not already in keys
				incrementEntitySupport(c1);

				// Only proceed to populate mappingSupport if there are any relationships 
				// for that instance besides class assignment
				Set<AttributeTypeRestriction> atrs = new HashSet<AttributeTypeRestriction>();

				if(o1.contains(c1URI))
					atrs.addAll(findATRs(si, tgtProperties, tgtValueMap));
				else 
					atrs.addAll(findATRs(si, srcProperties, srcValueMap));

				for(AttributeTypeRestriction atr: atrs)
				{
					incrementEntitySupport(atr);
					incrementMappingSupport(c1, atr);
				}
			}	
		}
	}

	/**
	 * Finds the possible AttributeTypeRestrictions for an instance
	 * @param instance: The URI of the instance
	 * @param properties: The set of data properties pertaining that instance
	 * @param vmap: The valueMap in which to look for (either source or target)
	 */
	private Set<AttributeTypeRestriction> findATRs(String instance, Set<String> properties, ValueMap vmap) 
	{
		// Since atrs is a set, no repeated elements will be added.
		Set<AttributeTypeRestriction> atrs = new HashSet<AttributeTypeRestriction>();

		if (properties != null) 
		{
			for(String pURI: properties) 
			{
				PropertyId p = new PropertyId(pURI, null);
				for(String v: vmap.getValues(instance, pURI)) 
				{
					OWLDatatype dataType = vmap.getDataType(instance, pURI, v);
					Datatype dt = new Datatype(dataType.toString());
					AttributeTypeRestriction atr = new AttributeTypeRestriction(p, dt);
					atrs.add(atr);
				}
			}
		}
		return atrs;
	}
}


