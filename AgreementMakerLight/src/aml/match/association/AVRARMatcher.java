package aml.match.association;

import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLDatatype;

import aml.AML;
import aml.alignment.rdf.AbstractExpression;
import aml.alignment.rdf.AttributeDomainRestriction;
import aml.alignment.rdf.AttributeTypeRestriction;
import aml.alignment.rdf.AttributeValueRestriction;
import aml.alignment.rdf.ClassId;
import aml.alignment.rdf.Comparator;
import aml.alignment.rdf.Datatype;
import aml.alignment.rdf.EDOALComparator;
import aml.alignment.rdf.Literal;
import aml.alignment.rdf.PropertyId;
import aml.ontology.Ontology;
import aml.ontology.ValueMap;
import aml.ontology.semantics.EntityMap;

public class AVRARMatcher extends AbstractAssociationRuleMatcher
{
	EntityMap eMap;
	
	//Constructor
	public AVRARMatcher()
	{
		eMap = AML.getInstance().getEntityMap();
	}

	//Protected methods
	/*
	 * Populates EntitySupport and MappingSupport tables
	 * @param o1: source ontology
	 * @param o2: target ontology
	 */
	protected void computeSupport(Ontology o1, Ontology o2) 
	{
		System.out.println("Initialising Attribute Type Restriction Matcher");
		// Get entity map of relations
		Set<String> sharedInstances = new HashSet<String>(getSharedInstances(o1,o2));
		ValueMap srcValueMap = o1.getValueMap();
		ValueMap tgtValueMap = o2.getValueMap();
		Set<String> srcProperties = null;
		Set<String> tgtProperties = null;

		for(String si : sharedInstances) 
		{
			// Find all classes of that instance
			Set<String> cSet = eMap.getIndividualClassesTransitive(si);
			// If empty list of classes, move on to next instance
			if(cSet.size() < 1) {continue;}

			// If map contains any properties linking to that instance, get them
			if (srcValueMap.getProperties(si) != null) 
				srcProperties = new HashSet<String>(srcValueMap.getProperties(si));
			if (tgtValueMap.getProperties(si) != null) 
				tgtProperties = new HashSet<String>(tgtValueMap.getProperties(si));
			
			Set<AttributeValueRestriction> foundAVRsInstance = new HashSet<AttributeValueRestriction>(); //AVRs found for this instance si
			for (String c1URI: cSet) 
			{
				// Transform string uri into correspondent AbstractExpression
				ClassId c1 = new ClassId(c1URI);
				incrementEntitySupport(c1);
				Set<AttributeValueRestriction> foundAVRsClass = new HashSet<AttributeValueRestriction>(); // AVRs found for this class c1

				// Only proceed to populate mappingSupport if there are any relationships 
				// for that instance besides class assignment
				if(o1.contains(c1URI))
					foundAVRsClass.addAll(findAVRs(si, tgtProperties, tgtValueMap));
				else 
					foundAVRsClass.addAll(findAVRs(si, srcProperties, srcValueMap));
				// Increment mapping support for all the AVRs found for this class
				for(AttributeValueRestriction atr: foundAVRsClass)
					incrementMappingSupport(c1, atr);
				foundAVRsInstance.addAll(foundAVRsClass);
			}
			// Increment entity support for all the AVRs found for this instance
			for(AbstractExpression avr: foundAVRsInstance)
				incrementEntitySupport(avr);
		}
	}

	/**
	 * Finds the possible AttributeValueRestrictions for an instance
	 * @param instance: The URI of the instance
	 * @param properties: The set of data properties pertaining that instance
	 * @param vmap: The valueMap in which to look for (either source or target)
	 */
	private Set<AttributeValueRestriction> findAVRs(String instance, Set<String> properties, ValueMap vmap) 
	{
		// Since atrs is a set, no repeated elements will be added.
		Set<AttributeValueRestriction> avrs = new HashSet<AttributeValueRestriction>();
		if (properties != null) 
		{
			for(String pURI: properties) 
			{
				PropertyId p = new PropertyId(pURI, null);
				for(String value: vmap.getValues(instance, pURI)) 
				{
					OWLDatatype dataType = vmap.getDataType(instance, pURI, value);
					AttributeValueRestriction avr = new AttributeValueRestriction(p, 
							new Comparator("http://ns.inria.org/edoal/1.0/#equals"), 
							new Literal(value, dataType.toString(), null));
					avrs.add(avr);
				}
			}
		}
		return avrs;
	}
}
