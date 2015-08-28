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
* Simulated user interaction manager that serves as an interface to the       *
* Oracle class (either AML's Oracle for internal use, or the SEALS OMT        *
* client's class for the OAEI) and also as a store, to prevent different      *
* interactive components from making redundant queries.                       *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 19-08-2015                                                            *
******************************************************************************/

package aml.util;

import aml.AML;
import aml.match.Alignment;
import aml.match.Mapping;
import aml.ontology.URIMap;

//Uncomment below to switch to the Oracle from the SEALS OMT client
//import eu.sealsproject.omt.client.interactive.Oracle;

public class InteractionManager
{
	
//Attributes
	
	//The alignment built from the positive queries
	private Alignment positive;
	//The alignment built from the negative queries
	private Alignment negative;
	//The query limit
	private int limit;
	//Whether user interaction is available
	private boolean isInteractive;
	//The URI map used to convert ids to URIs for
	//interfacing with the Oracle
	private URIMap uris;
	
//Constructors
	
	/**
	 * Initializes the InteractionManager.
	 * Should only be called after the input ontologies are open.
	 * When used internally, requires that the Oracle be initialized beforehand.
	 */
	public InteractionManager()
	{
		positive = new Alignment();
		negative = new Alignment();
		limit = 0;
		isInteractive = Oracle.isInteractive();
		uris = AML.getInstance().getURIMap();
	}
	
//Public Methods
	
	/**
	 * Checks whether a Mapping is correct by first looking at
	 * the stored alignments, and then querying the Oracle if
	 * necessary
	 * @param m: the Mapping to check
	 * @return whether the Mapping is correct according to the
	 * simulated user
	 */
	public boolean check(Mapping m)
	{
		boolean check = positive.contains(m);
		if(!check && !negative.contains(m) && isInteractive() && positive.size()+negative.size() < limit)
		{
			check = Oracle.check(uris.getURI(m.getSourceId()),
				uris.getURI(m.getTargetId()), m.getRelationship().toString());
			if(check)
				positive.add(m);
			else
				negative.add(m);
		}
		return check;
	}

	/**
	 * @return the query limit
	 */
	public int getLimit()
	{
		return limit;
	}

	/**
	 * @return whether user interaction is available
	 */
	public boolean isInteractive()
	{
		return isInteractive;
	}
	
	/**
	 * @return the current query count
	 */
	public int queryCount()
	{
		return positive.size()+negative.size();
	}
	
	/**
	 * Sets the query limit
	 * @param limit: the limit to set
	 */
	public void setLimit(int limit)
	{
		this.limit = limit;
	}
}