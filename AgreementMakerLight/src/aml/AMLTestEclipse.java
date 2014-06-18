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
* Tests AgreementMakerLight in Eclipse, with manually configured options.     *
*                                                                             *
* @author Daniel Faria                                                        *
* @date 06-06-2014                                                            *
* @version 2.0                                                                *
******************************************************************************/
package aml;

import java.io.FileNotFoundException;

import aml.AML.SelectionType;
import aml.filter.Selector;
import aml.match.Alignment;
import aml.match.LexicalMatcher;
import aml.match.MediatingMatcher;
import aml.match.OAEI2013Matcher;
import aml.match.ParametricStringMatcher;
import aml.match.WordMatcher;
import aml.match.XRefMatcher;
import aml.ontology.Ontology;

public class AMLTestEclipse
{

//Main Method
	
	public static void main(String[] args) throws FileNotFoundException
	{
		//Path to input ontology files (edit manually)
		String sourcePath = "store/largebio/oaei2013_SNOMED_small_overlapping_nci.owl";
		String targetPath = "store/largebio/oaei2013_NCI_small_overlapping_snomed.owl";
		//Path to reference alignment (edit manually, or leave blank for no evaluation)
		String referencePath = "store/largebio/oaei2013_SNOMED2NCI_repaired_UMLS_mappings.rdf";
		//Path to output alignment file (edit manually, or leave left blank to not save alignment)
		String alignPath = "store/anatomy/align1.rdf";

		double thresh = 0.59;
		
		long startTime = System.currentTimeMillis()/1000;
		
		AML aml = AML.getInstance();
		aml.openOntologies(sourcePath, targetPath);

		LexicalMatcher lm = new LexicalMatcher(false);
		Alignment a = lm.match(thresh);
		
		Ontology doid = new Ontology("store/knowledge/doid.owl",false);
		MediatingMatcher mm = new MediatingMatcher(doid);
		a.addAll(mm.match(thresh));
		
		Ontology ub = new Ontology("store/knowledge/uberon.owl",false);
		mm = new MediatingMatcher(ub);
		a.addAll(mm.match(thresh));

		WordMatcher wm = new WordMatcher();
		Alignment b = wm.match(thresh);
		Selector s = new Selector(b, SelectionType.PERMISSIVE);
		b = s.select(thresh);
		a.addAllNonConflicting(b);
				
		ParametricStringMatcher sm = new ParametricStringMatcher();
		b = sm.extendAlignment(a,thresh);
		s = new Selector(b, SelectionType.PERMISSIVE);
		b = s.select(thresh);
		a.addAllNonConflicting(b);

		System.out.println(evaluate(a,referencePath));
		
		
/*
		OAEI2013Matcher o = new OAEI2013Matcher(true,true,false);
		Alignment a = o.match();
		System.out.println(evaluate(a,referencePath));
*/		
		System.out.println("Finished in " + (System.currentTimeMillis()/1000-startTime) + " seconds");

	}
	
	private static String evaluate(Alignment a, String referencePath)
	{
		Alignment ref = null;
		try
		{
			ref = new Alignment(referencePath);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		int found = a.size();		
		int correct = a.evaluate(ref);
		int total = ref.size();

		double precision = 1.0*correct/found;
		String prc = Math.round(precision*1000)/10.0 + "%";
		double recall = 1.0*correct/total;
		String rec = Math.round(recall*1000)/10.0 + "%";
		double fmeasure = 2*precision*recall/(precision+recall);
		String fms = Math.round(fmeasure*1000)/10.0 + "%";
		
		return "Precision\tRecall\tF-measure\tFound\tCorrect\tReference\n" + prc +
			"\t" + rec + "\t" + fms + "\t" + found + "\t" + correct + "\t" + total;
	}
}