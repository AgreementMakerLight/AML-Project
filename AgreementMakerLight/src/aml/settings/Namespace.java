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
* Lists relevant namespaces.                                                  *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.settings;

public enum Namespace
{
    ALIGN_SVC		("alignsvc","http://exmo.inrialpes.fr/align/service","#"),
    ALIGN_EXT		("alext","http://exmo.inrialpes.fr/align/ext/1.0/",""),
    ALIGNMENT		("align","http://knowledgeweb.semanticweb.org/heterogeneity/alignment","#"),
    DUBLIN_CORE		("dc","http://purl.org/dc/elements/1.1/",""),
    EDOAL			("edoal","http://ns.inria.org/edoal/1.0/","#"),
    OWL				("owl","http://www.w3.org/2002/07/owl#",""),
    RDF				("rdf","http://www.w3.org/1999/02/22-rdf-syntax-ns#",""),
    RDFS			("rdfs","http://www.w3.org/2000/01/rdf-schema#",""),
    SKOS			("skos","http://www.w3.org/2004/02/skos/core#",""),
    SOAP			("SOAP-ENV","http://schemas.xmlsoap.org/soap/envelope/",""),
    XSD				("xsd","http://www.w3.org/2001/XMLSchema","#"),
    XSI				("xsi","http://www.w3.org/1999/XMLSchema-instance","");
	
	final public String ns;
	final public String uri;
	final public String separator;
	
	Namespace(String ns, String uri, String separator)
	{
		this.ns = ns;
		this.uri = uri;
		this.separator = separator;
	}
	
	public String prefix()
	{
		return uri + separator;
	}
}