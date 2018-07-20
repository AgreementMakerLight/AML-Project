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
* Lists the SKOS vocabulary.                                                  *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.settings;

import org.semanticweb.owlapi.model.IRI;

public enum SKOS
{
    ALTLABEL		("altLabel"),
    BROAD_MATCH		("broadMatch"),
    BROADER			("broader"),
    BROADER_TRANS	("broaderTransitive"),
    CHANGE_NOTE		("changeNote"),
    CLOSE_MATCH		("closeMatch"),
    COLLECT_PROP	("CollectableProperty"),
    COLLECTION		("Collection"),
    COMMENT			("comment"),
    CONCEPT			("Concept"),
    CONCEPT_SCHEME	("ConceptScheme"),
    DEFINITION		("definition"),
    DOCUMENT		("Document"),
    EDITORIAL_NOTE	("editorialNote"),
    EXACT_MATCH		("exactMatch"),
    EXAMPLE			("example"),
    HAS_TOP_CONCEPT	("hasTopConcept"),
    HIDDEN_LABEL	("hiddenLabel"),
    HISTORY_NOTE	("historyNote"),
    IMAGE			("Image"),
    IN_SCHEME		("inScheme"),
    LABEL_REL		("LabelRelation"),
    LABEL_RELATED	("labelRelated"),
    MAPPING_REL		("mappingRelation"),
    MEMBER			("member"),
    MEMBER_LIST		("memberList"),
    NARROW_MATCH	("narrowMatch"),
    NARROWER		("narrower"),
    NARROWER_TRANS	("narrowerTransitive"),
    NOTATION		("notation"),
    NOTE			("note"),
    ORDERED_COLECT	("OrderedCollection"),
    PREFLABEL		("prefLabel"),
    RELATED			("related"),
    RELATED_MATCH	("relatedMatch"),
    RESOURCE		("Resource"),
    SCOPE_NOTE		("scopeNote"),
    SEE_LABEL_REL	("seeLabelRelation"),
    SEMANTIC_REL	("semanticRelation"),
    TOP_CONCEPT		("topConceptOf");
	
    private String uri;
    
    SKOS(String s)
    {
    	uri = s;
    }
    
    /**
     * @return the SKOS term
     */
    public IRI toIRI()
    {
    	return IRI.create(Namespace.SKOS + uri);
    }
    
    public String toString()
    {
    	return uri;
    }
}