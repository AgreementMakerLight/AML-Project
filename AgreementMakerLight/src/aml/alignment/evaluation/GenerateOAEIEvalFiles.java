package aml.alignment.evaluation;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import aml.alignment.Alignment;
import aml.alignment.EDOALAlignment;
import aml.alignment.mapping.Mapping;
import aml.alignment.rdf.AbstractExpression;
import aml.util.data.Map2List;

public class GenerateOAEIEvalFiles 
{
	//Attributes

	// Reference
	Vector<String> simpleRefEntities;
	Set<String> complexRefEntities;
	Map2List<String,String> refEntityMap;
	HashMap<String,String> refRelationshipMap;

	//Alignment
	Map2List<String,String> algnEntityMap;
	HashMap<String,String> algnRelationshipMap;


	//Constructor
	public GenerateOAEIEvalFiles() 
	{
		// Reference
		simpleRefEntities = new Vector<String>();
		Set<String> complexRefEntities = new HashSet<String>();
		refEntityMap = new Map2List<String,String>();
		refRelationshipMap = new HashMap<String, String>();

		// Alignment
		algnEntityMap = new Map2List<String,String>();
		algnRelationshipMap = new HashMap<String, String>();
	}


	public void evaluate(Alignment alignment, Alignment reference, String file) throws IOException 
	{
		EDOALAlignment algn = (EDOALAlignment)alignment;
		EDOALAlignment ref = (EDOALAlignment)reference;

		// Process entities from reference
		processReference(ref);
		processAlignment(algn);

		//  Write files
		writeToFile(file, algnEntityMap, algnRelationshipMap);
		writeToFile("Ref", refEntityMap, refRelationshipMap);
	}

	/**
	 * @param algn: the aligment to be evaluated
	 * @return populate algnEntityMap and algnRelationshipMap
	 */
	private void processAlignment(EDOALAlignment algn) 
	{
		HashSet<Mapping<AbstractExpression>> leftoverMappings = new HashSet<Mapping<AbstractExpression>>();
		// 1) Simple entities
		for(Mapping<AbstractExpression> m : algn.getSimpleMappings())
		{	
			for(String src: m.getEntity1().getElements()) 
			{
				// Save map
				for(String tgt: m.getEntity2().getElements()) 
					algnEntityMap.add(src, tgt);
				algnRelationshipMap.put(src, m.getRelationship().toString());
			}
		}
		// 2) Process complex entities
		for(Mapping<AbstractExpression> m : algn.getComplexMappings())
		{	
			HashSet<String> srcElements = new HashSet<String>(m.getEntity1().getElements());
			HashSet<String> tgtElements = new HashSet<String>(m.getEntity2().getElements());
			HashSet<String> eligibleSrc = new HashSet<String>();
			String mappingRelation = m.getRelationship().toString();

			// Choosing a representative entitiy
			for(String src: srcElements) // Discard entities already involved in simple mappings
			{
				if(simpleRefEntities.contains(src))
					continue;
				eligibleSrc.add(src);
			}

			if (eligibleSrc.size() > 0) 
			{
				Set<String> intersect = new HashSet<String>(eligibleSrc);
				intersect.retainAll(complexRefEntities);

				// All those that are also in reference can be added
				if(intersect.size() > 0) 
				{
					for(String src : intersect) 
					{	
						for(String tgt: tgtElements) 
							algnEntityMap.add(src, tgt);
						algnRelationshipMap.put(src, mappingRelation);

						// Add other sources that didn't make the cut
						srcElements.removeAll(intersect);
						for(String src2: srcElements) 
							algnEntityMap.add(src, src2);
					}
				}
				else // from those that are not, only one (random) will be added
				{
					for(String src : eligibleSrc) 
					{	
						for(String tgt: tgtElements) 
							algnEntityMap.add(src, tgt);
						algnRelationshipMap.put(src, mappingRelation);

						// Add other sources that didn't make the cut
						srcElements.removeAll(eligibleSrc);
						for(String src2: srcElements) 
							algnEntityMap.add(src, src2);

						break;
					}
				}
			}
			// No candidates; Generate compound representative entity
			else
			{
				// only 1 source element: add to existing line
				if(srcElements.size() == 1) 
				{
					for(String src: srcElements) 
					{
						for(String tgt: tgtElements)
							algnEntityMap.add(src, tgt);
						algnRelationshipMap.put(src, mappingRelation);
					}
				}
				// 2 source elements: create compound expression -> both ways
				else if(srcElements.size() == 2) 
				{					
					Object[] elements = srcElements.toArray();
					String compoundSource1 = elements[0] + "|" + elements[1];

					// Check if ref contains compound sources; if not add random one
					if(!complexRefEntities.contains(compoundSource1)) 
					{
						String compoundSource2 = elements[1] + "|" + elements[0];
						for(String tgt: tgtElements)
							algnEntityMap.add(compoundSource2, tgt);
						algnRelationshipMap.put(compoundSource2, mappingRelation);
					}
				}
				else
					leftoverMappings.add(m);
			}
		}

		// 3) Leftover mappings: those with sources too complex (> 2 source entities with no simple map)
		for(Mapping<AbstractExpression> m : leftoverMappings) 
		{
			HashSet<String> pairwiseSources = new HashSet<String>();
			Vector<String> srcElements = new Vector<String>(m.getEntity1().getElements());
			String compoundSource = "";

			// Generate pairwise combinations that are not already mapped 
			for(int i=0; i<srcElements.size(); i++) 
			{
				for(int j=i+1; j<srcElements.size(); j++) 
				{
					compoundSource = srcElements.get(i) + "|" + srcElements.get(j);
					if(!algnEntityMap.contains(compoundSource))
						pairwiseSources.add(compoundSource);

					// Check both ways
					compoundSource = srcElements.get(j) + "|" + srcElements.get(i);
					if(!algnEntityMap.contains(compoundSource))
						pairwiseSources.add(compoundSource);
				}

			}
			if(pairwiseSources.size() != 0) // compoundSource will contain a pair of elements
			{
				// Is any of the pairs in the reference?
				Set<String> intersect = new HashSet<String>(pairwiseSources);
				intersect.retainAll(complexRefEntities);

				// All those that are also in reference can be added
				if(intersect.size() > 0) 
				{
					for(String src : intersect) 
					{	
						for(String tgt:  m.getEntity2().getElements()) 
							algnEntityMap.add(src, tgt);
						algnRelationshipMap.put(src, m.getRelationship().toString());

						// Add remaining sources (those that are not part of pair)
						String[] pair = new String[2];
						pair = src.split("|");
						for(String src2: srcElements) 
						{
							if(!Arrays.asList(pair).contains(src2))
								algnEntityMap.add(src, src2);
						}
					}
				}
				else // from those that are not, only one (random) will be added
				{
					for(String src : pairwiseSources) 
					{	
						for(String tgt: m.getEntity2().getElements()) 
							algnEntityMap.add(src, tgt);
						algnRelationshipMap.put(src, m.getRelationship().toString());

						// Add remaining sources (those that are not part of pair)
						String[] pair = new String[2];
						pair = src.split("|");
						for(String src2: srcElements) 
						{
							if(!Arrays.asList(pair).contains(src2))
								algnEntityMap.add(src, src2);
						}
					}
				}
			}	
			else // compoundSource will contain all elements
			{
				compoundSource = "";
				for(String src: srcElements) 
					compoundSource += src + "|";

				compoundSource = compoundSource.substring(0, compoundSource.length() - 1);
				for(String tgt: m.getEntity2().getElements())
					algnEntityMap.add(compoundSource, tgt);
				algnRelationshipMap.put(compoundSource, m.getRelationship().toString());
			}	
		}
	}


	/**
	 * @param ref: the reference aligment
	 * @return populate refEntityMap and redRelationshipMap
	 */
	private void processReference(EDOALAlignment ref) 
	{
		HashSet<Mapping<AbstractExpression>> leftoverMappings = new HashSet<Mapping<AbstractExpression>>();
		
		// 1) Process reference simple entities
		for(Mapping<AbstractExpression> m : ref.getSimpleMappings())
		{	
			for(String src: m.getEntity1().getElements()) 
			{
				// Save entity
				simpleRefEntities.add(src);
				// Save map
				for(String tgt: m.getEntity2().getElements()) 
					refEntityMap.add(src, tgt);
				refRelationshipMap.put(src, m.getRelationship().toString());
			}
		}
		
		// 2) Process reference complex entities
		for(Mapping<AbstractExpression> m : ref.getComplexMappings())
		{	
			HashSet<String> srcElements = new HashSet(m.getEntity1().getElements());
			HashSet<String> tgtElements = new HashSet(m.getEntity2().getElements());
			HashSet<String> eligibleSrc = new HashSet<String>();
			String mappingRelation = m.getRelationship().toString();

			for(String src: srcElements)  // Check if there are simple mappings
			{
				if(simpleRefEntities.contains(src)) 
					continue;
				eligibleSrc.add(src);
			}
			if(eligibleSrc.size() != 0) // Save map
			{
				for(String src : eligibleSrc) 
				{	
					for(String tgt: tgtElements) 
						refEntityMap.add(src, tgt);
					refRelationshipMap.put(src, mappingRelation);

					// Add other sources that didn't make the cut
					srcElements.removeAll(eligibleSrc);
					for(String src2: srcElements) 
						refEntityMap.add(src, src2);
				}
			}
			else	// Generate compound representative entity
			{
				// only 1 source element: add to existing line
				if(srcElements.size() == 1) 
				{
					for(String src: srcElements) 
						for(String tgt: tgtElements)
							refEntityMap.add(src, tgt);
				}
				// 2 source elements: create compound expression
				if(srcElements.size() == 2) 
				{
					String compoundSource = "";
					for(String src: srcElements) 
						compoundSource += src + "|";

					compoundSource = compoundSource.substring(0, compoundSource.length() - 1);
					for(String tgt: tgtElements)
						refEntityMap.add(compoundSource, tgt);
					refRelationshipMap.put(compoundSource, mappingRelation);
				}
				else
					leftoverMappings.add(m);
			}
		}

		// 3) Leftover mappings: those with sources too complex (> 2 source entities with no simple map)
		for(Mapping<AbstractExpression> m : leftoverMappings) 
		{
			HashSet<String> pairwiseSources = new HashSet<String>();
			Vector<String> srcElements = new Vector<String>(m.getEntity1().getElements());
			String compoundSource = "";

			// Generate pairwise combinations that are not already mapped 
			for(int i=0; i<srcElements.size(); i++) 
			{
				for(int j=i+1; j<srcElements.size(); j++) 
				{
					compoundSource = srcElements.get(i) + "|" + srcElements.get(j);
					if(!refEntityMap.contains(compoundSource))
						pairwiseSources.add(compoundSource);
				}
			}
			if(pairwiseSources.size() != 0) // compoundSource will contain a pair of elements
			{
				for(String src: pairwiseSources) 
				{
					for(String tgt: m.getEntity2().getElements()) 
						refEntityMap.add(src, tgt);
					refRelationshipMap.put(src, m.getRelationship().toString());

					// Add remaining sources (those that are not part of pair)
					String[] pair = new String[2];
					pair = src.split("|");
					for(String src2: srcElements) 
					{
						if(!Arrays.asList(pair).contains(src2))
							refEntityMap.add(src, src2);
					}
				}	
			}
			else // compoundSource will contain all elements
			{
				compoundSource = "";
				for(String src: srcElements) 
					compoundSource += src + "|";

				compoundSource = compoundSource.substring(0, compoundSource.length() - 1);
				for(String tgt: m.getEntity2().getElements())
					refEntityMap.add(compoundSource, tgt);
				refRelationshipMap.put(compoundSource, m.getRelationship().toString());
			}	
		}
		complexRefEntities = new HashSet<String>(refEntityMap.keySet());
		complexRefEntities.removeAll(simpleRefEntities);
	}


	private void writeToFile(String file, Map2List<String,String> entityMap, HashMap<String,String> relationshipMap) throws IOException 
	{
		FileWriter entityWriter = new FileWriter("store/OAEI_generated_files/hydro/hydroOnt-swo/entity"+ file +".txt");
		FileWriter relationshipWriter = new FileWriter("store/OAEI_generated_files/hydro/hydroOnt-swo/relationship"+ file +".txt");

		int count=0;
		for(String src: entityMap.keySet()) 
		{
			count++;
			String targets = "";
			for(String tgt: entityMap.get(src)) 
			{
				targets += tgt + ",";
			}
			if(count == entityMap.keyCount()) // last line
			{
				entityWriter.write(src + "\t" + targets.substring(0, targets.length()-1));
				relationshipWriter.write(src + "\t" + relationshipMap.get(src));
			}
			else 
			{
				entityWriter.write(src + "\t" + targets.substring(0, targets.length()-1)+"\n" );
				relationshipWriter.write(src + "\t" + relationshipMap.get(src)+"\n" );
			}
		}
		entityWriter.close();
		relationshipWriter.close();
	}
}


