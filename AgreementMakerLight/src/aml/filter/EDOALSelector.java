package aml.filter;

import java.util.Vector;

import aml.AML;
import aml.alignment.Alignment;
import aml.alignment.EDOALAlignment;
import aml.alignment.mapping.Mapping;
import aml.alignment.rdf.AbstractExpression;
import aml.ontology.semantics.EntityMap;

public class EDOALSelector implements Filterer {

	// Constructors
	/**
	 * Constructs an EDOALSelector 
	 */
	public EDOALSelector(){}

	//Public Methods
	public Alignment filter(Alignment a) 
	{
		if(!(a instanceof EDOALAlignment))
		{
			System.out.println("Warning: cannot filter non-EDOAL alignment!");
			return a;
		}

		System.out.println("Performing Selection");
		long time = System.currentTimeMillis()/1000;
		EDOALAlignment in = (EDOALAlignment)a;
		EDOALAlignment out = new EDOALAlignment();
		in.sortDescending();

		for(Mapping<AbstractExpression> m1 : in)
		{
			if(out.containsConflict(m1)) 
				continue;

			if (m1.getSimilarity() == 1.0)
			{
				out.add(m1);
				continue;
			}

			AbstractExpression src = m1.getEntity1();
			AbstractExpression tgt = m1.getEntity2();

			// Criteria 1: Simple vs complex mappings
			if((src instanceof aml.alignment.rdf.ClassId &&
					tgt instanceof aml.alignment.rdf.ClassId) |
					(src instanceof aml.alignment.rdf.RelationId &&
							tgt instanceof aml.alignment.rdf.RelationId) |
					(src instanceof aml.alignment.rdf.PropertyId &&
							tgt instanceof aml.alignment.rdf.PropertyId))
				out.add(m1);

			// Check conflicts
			else 
			{
				Vector<Mapping<AbstractExpression>> conflicts = in.getConflicts(m1);
				int len = conflicts.size();
				EntityMap r = AML.getInstance().getEntityMap();

				for (int i = 0; i<len; i++)
				{
					// Watch out for duplicates
					if(!m1.equals(conflicts.get(i)))
					{
						//Criteria 2: Specific vs general mappings
						for (int j=0; j<len; j++)
						{
							AbstractExpression tgt2 = conflicts.get(j).getEntity2();
							// Is tgt1 any other target expression parent?
							if(r.isSubclass(tgt2.toString(), tgt.toString())) 
							{
								conflicts.remove(i);
								break;
							}
							// Is it anyone's child?
							else if(r.isSubclass(tgt.toString(), tgt2.toString())) 
							{
								conflicts.remove(j);
							}	
						}	
					}
				}

				len = conflicts.size();
				if (len < 1) 
					out.add(m1);

				else 
				{
					double maxConf = 0;
					Mapping<AbstractExpression> bestMapping = null;

					for (Mapping<AbstractExpression> c: conflicts)
					{
						if(c.getSimilarity()>maxConf) 
						{
							maxConf = c.getSimilarity();
							bestMapping = c;
						}
					}
					out.add(bestMapping);
				}

			}
		}
		System.out.println("Finished in " +	(System.currentTimeMillis()/1000-time) + " seconds");
		return out;
	}
}




