/******************************************************************************
* Copyright 2013-2016 LASIGE                                                  *
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
******************************************************************************/

package aml.util;

import aml.AML;
import aml.match.Mapping;
import aml.ontology.URIMap;
import aml.settings.MappingStatus;

//Uncomment below to switch to the Oracle from the SEALS OMT client
//import eu.sealsproject.omt.client.interactive.Oracle;

public class InteractionManager
{
	
//Attributes
	
	//The query limit & count
	private int limit;
	private int count;
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
		limit = 0;
		count = 0;
		isInteractive = Oracle.isInteractive();
		uris = AML.getInstance().getURIMap();
	}
	
//Public Methods
	
	/**
	 * Classifies a Mapping by checking the Oracle
	 * @param m: the Mapping to check
	 */
	public void classify(Mapping m)
	{
		if(m.getStatus().equals(MappingStatus.CORRECT) || m.getStatus().equals(MappingStatus.INCORRECT))
			return;
		if(limit > count++)
		{
			boolean check = Oracle.check(uris.getURI(m.getSourceId()),
				uris.getURI(m.getTargetId()), m.getRelationship().toString());
			if(check)
				m.setStatus(MappingStatus.CORRECT);
			else
				m.setStatus(MappingStatus.INCORRECT);
		}
		if(limit <= count)
			isInteractive = false;
	}

	/**
	 * @return whether user interaction is available
	 */
	public boolean isInteractive()
	{
		return isInteractive;
	}
	
	/**
	 * Sets the query limit
	 * @param limit: the limit to set
	 */
	public void setLimit(int limit)
	{
		this.limit = limit;
		this.count = 0;
		if(limit == 0)
			isInteractive = false;
	}
}