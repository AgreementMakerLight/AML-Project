/******************************************************************************
* Copyright 2013-2014 LASIGE                                                  *
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
* Customizable ensemble of matching, selection & repair algorithms.           *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 31-07-2014                                                            *
* @version 2.0                                                                *
******************************************************************************/
package aml.match;

import java.util.Vector;

import aml.AML;
import aml.filter.CardinalityRepairer;
import aml.filter.RankedSelector;
import aml.settings.SelectionType;

public class AMLMatcher implements PrimaryMatcher
{
	
//Attributes

	//Settings
	private Vector<String> bkSources;
	private SelectionType sType;
	private boolean matchProps, repair;
	//Parameter thresholds
	private final double BASE_THRESH = 0.5;
	private final double PROP_THRESH = 0.45;
	
//Constructors	
	
	public AMLMatcher(Vector<String> bk, SelectionType s, boolean mProp, boolean rep)
	{
		bkSources = bk;
		sType = s;
		matchProps = mProp;
		repair = rep;
	}
	
//Public Methods

	@Override
	public Alignment match(double thresh)
	{
		AML aml = AML.getInstance();
    	//Check the size of the problem
		int sSize = aml.getSource().classCount();
		int tSize = aml.getTarget().classCount();
		boolean isLarge = (Math.min(sSize,tSize) > 30000 || Math.max(sSize, tSize) > 60000);
		//Do the lexical match
    	LexicalMatcher lm = new LexicalMatcher();
		Alignment a = lm.match(BASE_THRESH);
		//If background knowledge is on auto, call the AutoBKMatcher
		if(bkSources != null && bkSources.size() > 0)
		{
			BackgroundKnowledgeMatcher bk = new BackgroundKnowledgeMatcher(bkSources, !sType.equals(SelectionType.MANY));
			a.addAll(bk.match(thresh));
		}
		if(!isLarge)
		{
			WordMatcher wm = new WordMatcher();
			a.addAllNonConflicting(wm.match(thresh));
		}
		ParametricStringMatcher sm = new ParametricStringMatcher();
		a.addAll(sm.extendAlignment(a, thresh));
		RankedSelector s = new RankedSelector(sType);
		a = s.select(a, thresh);
		if(matchProps)
		{
			PropertyMatcher pm = new PropertyMatcher(bkSources.contains("WordNet"));
			a.addAll(pm.matchProperties(a, PROP_THRESH));
		}
		if(repair)
		{
			CardinalityRepairer rep = new CardinalityRepairer();
			a = rep.repair(a);
		}
		return a;
	}
}