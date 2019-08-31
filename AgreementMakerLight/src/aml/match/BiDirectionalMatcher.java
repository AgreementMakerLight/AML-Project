/******************************************************************************
 * Copyright 2013-2019 LASIGE                                                  *
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
 * Superclass for asymmetric matchers, i.e. match(o1, o2) != match(o2,o1).     *
 * Handles merging the two alignments, allowing the subclasses to only         *
 * implement searching in one direction,                                       *
 *                                                                             *
 * @authors Teemu Tervo                                                        *
 ******************************************************************************/
package aml.match;

import aml.alignment.EDOALAlignment;
import aml.ontology.EntityType;
import aml.ontology.Ontology;

public abstract class BiDirectionalMatcher extends Matcher
{
	//Constructors
	public BiDirectionalMatcher()
	{
		super();
	}

	//Public Methods
	public EDOALAlignment match(Ontology o1, Ontology o2, EntityType e, double thresh)
	{
		EDOALAlignment a = uniMatch(o1, o2, e, thresh);
		EDOALAlignment b = uniMatch(o2, o1, e, thresh);
		a.addAll(b.reverse());
		return a;
	}

	//Protected methods
	protected abstract EDOALAlignment uniMatch(Ontology o1, Ontology o2, EntityType e, double thresh);
}