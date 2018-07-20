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
* A Mapping represents an element in an Alignment.                            *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.alignment;

public interface Mapping extends Comparable<Mapping>
{
	public Object getEntity1();
	
	public Object getEntity2();
	
	public MappingRelation getRelationship();
	
	public double getSimilarity();
	
	public String getSimilarityPercent();
	
	public MappingStatus getStatus();
	
	public void setRelationship(MappingRelation r);
	
	public void setSimilarity(double sim);
	
	public void setStatus(MappingStatus s);
	
	public String toRDF();
	
	public String toTSV();
}
