package aml.alignment.evaluation;

import aml.alignment.Alignment;
import aml.alignment.mapping.Mapping;
import aml.alignment.mapping.MappingStatus;

public class SimpleEvaluator implements Evaluator
{
	/**
	 * @param ref: the reference Alignment to evaluate this Alignment
	 * @return the evaluation of this Alignment {# correct mappings, # conflict mappings}
	 */
	@SuppressWarnings("rawtypes")
	public int[] evaluate(Alignment algn, Alignment ref)
	{
		System.out.println("Performing Evaluation");
		long time = System.currentTimeMillis()/1000;
		
		int[] count = new int[2];
		for(int i=0; i<algn.size(); i++)
		{
			Mapping m = algn.get(i);
			if(ref.containsUnknown(m))
			{
				count[1]++;
				m.setStatus(MappingStatus.UNKNOWN);
			}
			else if(ref.contains(m))
			{
				count[0]++;
				m.setStatus(MappingStatus.CORRECT);
			}
			else
				m.setStatus(MappingStatus.INCORRECT);
		}
		
		System.out.println("Finished in " +	(System.currentTimeMillis()/1000-time) + " seconds");
		return count;
	}
}
