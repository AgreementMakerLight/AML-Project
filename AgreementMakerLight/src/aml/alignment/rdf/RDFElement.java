/******************************************************************************
* Copyright 2013-2018 LASIGE                                                  *
*                                                                             *
* Licensed under the Apache License, Version 2.0 (the "License"); you may     *
* not use this file except in compliance with the License. You may obtain a   *
* copy of the License at http://www.apache.org/licenses/LICENSE-2.0           *
*                                                                             *
* Unless required by applicable law or agreed to in writing, software         *
* distributed under the License is distributed on an "AS IS" BASIS,           *
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.    *
* See the License for the specific language governing permissions and         *
* limitations under the License.                                              *
*                                                                             *
*******************************************************************************
* Lists Alignment RDF syntax elements.                                        *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.alignment.rdf;

import aml.settings.Namespace;

public enum RDFElement
{
	RDF						(Namespace.RDF, "RDF"), 
	RDF_ABOUT				(Namespace.RDF, "about"),
	RDF_DATATYPE			(Namespace.RDF, "datatype"),
	RDF_ID					(Namespace.RDF, "ID"),
	RDF_PARSETYPE			(Namespace.RDF, "parseType"),
	RDF_RESOURCE			(Namespace.RDF, "resource"),
	CREATOR					(Namespace.DUBLIN_CORE, "creator"),
	DATE					(Namespace.DUBLIN_CORE, "date"),
	IDENTIFIER				(Namespace.DUBLIN_CORE, "identifier"),
	ALIGNMENT				(Namespace.ALIGNMENT, "alignment"),
	ALIGNMENT_				(Namespace.ALIGNMENT, "Alignment"),
	CELL_					(Namespace.ALIGNMENT, "Cell"),
	CONFIDENCE_CLASS		(Namespace.ALIGNMENT, "confidenceClass"),
	ENTITY1					(Namespace.ALIGNMENT, "entity1"),
	ENTITY2					(Namespace.ALIGNMENT, "entity2"),
	FORMALISM_				(Namespace.ALIGNMENT, "Formalism"),
	FORMALISM				(Namespace.ALIGNMENT, "formalism"),
	ID						(Namespace.ALIGNMENT, "id"),
	LEVEL					(Namespace.ALIGNMENT, "level"),
	LOCATION				(Namespace.ALIGNMENT, "location"),
	MAP						(Namespace.ALIGNMENT, "map"),
	MEASURE					(Namespace.ALIGNMENT, "measure"),
	NAME					(Namespace.ALIGNMENT, "name"),
	ONTO1					(Namespace.ALIGNMENT, "onto1"),
	ONTO2					(Namespace.ALIGNMENT, "onto2"),
	ONTOLOGY				(Namespace.ALIGNMENT, "ontology"),
	ONTOLOGY_				(Namespace.ALIGNMENT, "Ontology"),
	ONTOLOGY_NETWORK_		(Namespace.ALIGNMENT, "OntologyNetwork"),
	RELATION_CLASS			(Namespace.ALIGNMENT, "relationClass"),
	RULE_RELATION			(Namespace.ALIGNMENT, "relation"),
	SEMANTICS				(Namespace.ALIGNMENT, "semantics"),
	STATUS					(Namespace.ALIGNMENT, "status"), //Note: this is not official Alignment syntax
	TYPE					(Namespace.ALIGNMENT, "type"),
	URI						(Namespace.ALIGNMENT, "uri"),
	XML						(Namespace.ALIGNMENT, "xml"),
	ALL						(Namespace.EDOAL, "all"),
	AND						(Namespace.EDOAL, "and"),
	AGGREGATE_				(Namespace.EDOAL, "Aggregate"),
	APPLY_					(Namespace.EDOAL, "Apply"),
	ARGUMENTS				(Namespace.EDOAL, "arguments"),
	ATTR_DOMAIN_REST_		(Namespace.EDOAL, "AttributeDomainRestriction"),
	ATTR_OCCURRENCE_REST_	(Namespace.EDOAL, "AttributeOccurenceRestriction"),
	ATTR_TYPE_REST_			(Namespace.EDOAL, "AttributeTypeRestriction"),
	ATTR_VALUE_REST_		(Namespace.EDOAL, "AttributeValueRestriction"),
	BINDING					(Namespace.EDOAL, "binding"),
	CLASS					(Namespace.EDOAL, "class"),
	CLASS_					(Namespace.EDOAL, "Class"),
	COMPARATOR				(Namespace.EDOAL, "comparator"),
	COMPOSE					(Namespace.EDOAL, "compose"),
	DATATYPE				(Namespace.EDOAL, "datatype"),
	DATATYPE_				(Namespace.EDOAL, "Datatype"),
	DIRECTION				(Namespace.EDOAL, "direction"),
	DOMAIN_RESTRICTION_		(Namespace.EDOAL, "AttributeDomainRestriction"),
	EDOAL_ENTITY1			(Namespace.EDOAL, "entity1"),
	EDOAL_ENTITY2			(Namespace.EDOAL, "entity2"),
	EDOAL_TYPE				(Namespace.EDOAL, "type"),
	EQUALS_					(Namespace.EDOAL, "Equals"),
	EXISTS					(Namespace.EDOAL, "exists"),
	INSTANCE_				(Namespace.EDOAL, "Instance"),
	INTERSECTS_				(Namespace.EDOAL, "Intersects"),
	INVERSE					(Namespace.EDOAL, "inverse"),
	LANG					(Namespace.EDOAL, "lang"),
	LINKKEY					(Namespace.EDOAL, "linkkey"),
	LINKKEY_				(Namespace.EDOAL, "Linkkey"),
	LITERAL_				(Namespace.EDOAL, "Literal"),
	NOT						(Namespace.EDOAL, "not"),
	ON_ATTRIBUTE			(Namespace.EDOAL, "onAttribute"),
	OPERATOR				(Namespace.EDOAL, "operator"),
	OR						(Namespace.EDOAL, "or"),
	PROPERTY_				(Namespace.EDOAL, "Property"),
	PROPERTY_DOMAIN_REST_	(Namespace.EDOAL, "PropertyDomainRestriction"),
	PROPERTY_TYPE_REST_		(Namespace.EDOAL, "PropertyTypeRestriction"),
	PROPERTY_VALUE_REST_	(Namespace.EDOAL, "PropertyValueRestriction"),
	PROPERTY1				(Namespace.EDOAL, "property1"),
	PROPERTY2				(Namespace.EDOAL, "property2"),
	REFLEXIVE				(Namespace.EDOAL, "reflexive"),
	RELATION_				(Namespace.EDOAL, "Relation"),
	RELATION_CODOMAIN_REST_	(Namespace.EDOAL, "RelationCoDomainRestriction"),
	RELATION_DOMAIN_REST_	(Namespace.EDOAL, "RelationDomainRestriction"),
	STRING					(Namespace.EDOAL, "string"),
	SYMMETRIC				(Namespace.EDOAL, "symmetric"),
	TRANSF					(Namespace.EDOAL, "transf"),
	TRANSFORMATION			(Namespace.EDOAL, "transformation"),
	TRANSFORMATION_			(Namespace.EDOAL, "Transformation"),
	TRANSITIVE				(Namespace.EDOAL, "transitive"),
	VALUE					(Namespace.EDOAL, "value"),
	VAR						(Namespace.EDOAL, "var");
	
	Namespace ns;
	String element;
	
	RDFElement(Namespace ns, String element)
	{
		this.ns = ns;
		this.element = element;
	}

	public String toRDF()
	{
		return ns.toString() + ":" + element;
	}
	
	public String toString()
	{
		return element;
	}
}