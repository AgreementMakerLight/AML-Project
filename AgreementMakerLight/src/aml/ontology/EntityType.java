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
* Lists the types of ontology entities.                                       *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.ontology;

public enum EntityType
{
    ANNOTATION_PROP		("Annotation Property", false),
	CLASS				("Class", true),
	CLASS_EXPRESSION	("Class Expression", false),
    DATA_PROP			("Data Property", true),
	DATATYPE			("Datatype", false),
	INDIVIDUAL			("Individual", true),
    OBJECT_PROP			("Object Property", true),
    OBJECT_EXPRESSION	("Object Property Expression", false);
    
    String label;
    boolean matchable;
    
    EntityType(String s, boolean m)
    {
    	label = s;
    	matchable = m;
    }
    
    public boolean isMatchable()
    {
    	return matchable;
    }
    
    public String toString()
    {
    	return label;
    }
}