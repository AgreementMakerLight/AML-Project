package aml.alignment.evaluation;

import aml.alignment.Alignment;

public interface Evaluator
{	
	// Public Methods
	/**
	 * @param ref: the reference Alignment to evaluate this Alignment
	 * @return the evaluation of this Alignment {# correct mappings, # conflict mappings}
	 */
	@SuppressWarnings("rawtypes")
	public int[] evaluate(Alignment algn, Alignment ref);	
}
