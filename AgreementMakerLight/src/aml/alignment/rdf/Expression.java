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
* An Expression.                                                              *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.alignment.rdf;

import java.util.Collection;
import java.util.Set;

public interface Expression
{
	
//Public Methods
	
	@Override
	public boolean equals(Object o);
	
	/**
	 * @return the ontology entities listed in this Expression
	 */
	public Set<String> getElements();
	
	/**
	 * @return the Expressions that compose this Expression
	 */
	public <E extends Expression> Collection<E> getComponents();
	
	@Override
	public int hashCode();

	/**
	 * @return this Expression in RDF form
	 */
	public String toRDF();

	@Override
	public String toString();
}