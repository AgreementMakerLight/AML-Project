package aml.ontology.semantics;

import java.util.Set;
import java.util.Vector;

public class Reasoner
{

	public void reason()
	{
//		//Finally process the semantically disjoint classes
//		//Classes that have incompatible cardinalities on the same property
//		//First exact cardinalities vs exact, min and max cardinalities
//		for(Integer prop : card.keySet())
//		{
//			//TODO: cardinality restrictions on object properties need to be processed differently
//			Vector<Integer> exact = new Vector<Integer>(card.keySet(prop));
//			for(int i = 0; i < exact.size()-1; i++)
//				for(int j = i+1; j < exact.size(); j++)
//					if(card.get(prop, exact.get(i)) != card.get(prop, exact.get(j)))
//						rm.addDisjoint(exact.get(i), exact.get(j));
//			Set<Integer> max = maxCard.keySet(prop);
//			if(max != null)
//				for(int i = 0; i < exact.size(); i++)
//					for(Integer j : max)
//						if(card.get(prop, exact.get(i)) > maxCard.get(prop, j))
//							rm.addDisjoint(exact.get(i), j);
//			Set<Integer> min = minCard.keySet(prop);
//			if(min != null)
//				for(int i = 0; i < exact.size(); i++)
//					for(Integer j : min)
//						if(card.get(prop, exact.get(i)) > minCard.get(prop, j))
//							rm.addDisjoint(exact.get(i), j);				
//		}
//		//Then min vs max cardinalities
//		for(Integer prop : minCard.keySet())
//		{
//			//TODO: cardinality restrictions on object properties need to be processed differently
//			Set<Integer> min = minCard.keySet(prop);
//			Set<Integer> max = maxCard.keySet(prop);
//			if(max == null)
//				continue;
//			for(Integer i : min)
//				for(Integer j : max)
//					if(minCard.get(prop, i) > maxCard.get(prop, j))
//						rm.addDisjoint(i, j);
//		}
//		//Data properties with incompatible values
//		//First hasValue restrictions on functional data properties
//		for(Integer prop : dataHasValue.keySet())
//		{
//			Vector<Integer> cl = new Vector<Integer>(dataHasValue.keySet(prop));
//			for(int i = 0; i < cl.size()-1; i++)
//				for(int j = i+1; j < cl.size(); j++)
//					if(!dataHasValue.get(prop, cl.get(i)).equals(dataHasValue.get(prop, cl.get(j))))
//						rm.addDisjoint(cl.get(i), cl.get(j));
//		}
//		//Then incompatible someValues restrictions on functional data properties
//		for(Integer prop : dataSomeValues.keySet())
//		{
//			Vector<Integer> cl = new Vector<Integer>(dataSomeValues.keySet(prop));
//			for(int i = 0; i < cl.size()-1; i++)
//			{
//				for(int j = i+1; j < cl.size(); j++)
//				{
//					String[] datatypes = dataSomeValues.get(prop, cl.get(j)).split(" ");
//					for(String d: datatypes)
//					{
//						if(!dataSomeValues.get(prop, cl.get(i)).contains(d))
//						{
//							rm.addDisjoint(cl.get(i), cl.get(j));
//							break;
//						}
//					}
//				}
//			}
//		}
//		//Then incompatible allValues restrictions on all data properties
//		//(allValues vs allValues and allValues vs someValues)
//		for(Integer prop : dataAllValues.keySet())
//		{
//			Vector<Integer> cl = new Vector<Integer>(dataAllValues.keySet(prop));
//			for(int i = 0; i < cl.size()-1; i++)
//			{
//				for(int j = i+1; j < cl.size(); j++)
//				{
//					String[] datatypes = dataAllValues.get(prop, cl.get(j)).split(" ");
//					for(String d: datatypes)
//					{
//						if(!dataAllValues.get(prop, cl.get(i)).contains(d))
//						{
//							rm.addDisjoint(cl.get(i), cl.get(j));
//							break;
//						}
//					}
//				}
//			}
//			Set<Integer> sv = dataSomeValues.keySet(prop);
//			if(sv == null)
//				continue;
//			for(Integer i : cl)
//			{
//				for(Integer j : sv)
//				{
//					String[] datatypes = dataSomeValues.get(prop, j).split(" ");
//					for(String d: datatypes)
//					{
//						if(!dataAllValues.get(prop, i).contains(d))
//						{
//							rm.addDisjoint(i, j);
//							break;
//						}
//					}
//				}
//			}
//		}
//		//Classes with incompatible value restrictions for the same object property
//		//(i.e., the restrictions point to disjoint classes)
//		//First allValues restrictions
//		for(Integer prop : objectAllValues.keySet())
//		{
//			Vector<Integer> cl = new Vector<Integer>(objectAllValues.keySet(prop));
//			for(int i = 0; i < cl.size() - 1; i++)
//			{
//				int c1 = objectAllValues.get(prop, cl.get(i));
//				for(int j = i + 1; j < cl.size(); j++)
//				{
//					int c2 = objectAllValues.get(prop, cl.get(j));
//					if(c1 != c2 && rm.areDisjoint(c1, c2))
//						rm.addDisjoint(cl.get(i), cl.get(j));
//				}
//			}
//
//			Set<Integer> sv = objectSomeValues.keySet(prop);
//			if(sv == null)
//				continue;
//			for(Integer i : cl)
//			{
//				int c1 = objectAllValues.get(prop, i);
//				for(Integer j : sv)
//				{
//					int c2 = objectSomeValues.get(prop, j);
//					if(c1 != c2 && rm.areDisjoint(c1, c2))
//						rm.addDisjoint(i, j);
//				}
//			}
//		}
//		//Finally someValues restrictions on functional properties
//		for(Integer prop : objectSomeValues.keySet())
//		{
//			if(!rm.isFunctional(prop))
//				continue;
//			Set<Integer> sv = objectSomeValues.keySet(prop);
//			if(sv == null)
//				continue;
//			Vector<Integer> cl = new Vector<Integer>(sv);
//			for(int i = 0; i < cl.size() - 1; i++)
//			{
//				int c1 = objectSomeValues.get(prop, cl.get(i));
//				for(int j = i + 1; j < cl.size(); j++)
//				{
//					int c2 = objectSomeValues.get(prop, cl.get(j));
//					if(c1 != c2 && rm.areDisjoint(c1, c2))
//						rm.addDisjoint(cl.get(i), cl.get(j));
//				}
//			}
//		}

	}
}
