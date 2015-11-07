/******************************************************************************
* Copyright 2013-2015 LASIGE                                                  *
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
* Lists the types of ontology entities (class, instance, and the different    *
* properties).                                                                *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 21-05-2010                                                            *
******************************************************************************/
package aml.settings;

public enum EntityType
{
	CLASS ("Class"),
	INDIVIDUAL ("Individual"),
    DATA ("Data Property"),
    OBJECT ("Object Property"),
    ANNOTATION ("Annotation Property");
    
    String label;
    
    EntityType(String s)
    {
    	label = s;
    }
    
    public String toString()
    {
    	return label;
    }
}