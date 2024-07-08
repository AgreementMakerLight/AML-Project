package aml.applications.skills;

import java.io.File;
import java.util.HashSet;

import aml.util.ExtensionFilter;

public class SkillsBatch
{
	private static String root = "store/skills/";
	private static String ontology = "skills_ontology.owl";
	
//Main Method
	
	public static void main(String[] args) throws Exception
	{
		HashSet<String> authors = new HashSet<String>();
		HashSet<String> tasks = new HashSet<String>();
		
		File r = new File(root);
		File[] csvs = r.listFiles(new ExtensionFilter("CSV", ".csv", false));
		for(File f : csvs)
		{
			String[] a = new String[7];
			a[0] = "-O";
			a[1] = root + ontology;
			a[2] = "-i";
			a[3] = f.getAbsolutePath();
			a[4] = "-o";
			String name = f.getName().replace(".csv", "");
			a[5] = root + name + "_GT.owl";
			if(name.startsWith("authors_"))
			{
				authors.add(name);
				a[6] = "-gt";
				SkillsOntology.main(a);
			}
			else
			{
				tasks.add(name);
				a[6] = "-t";
				String[] b = new String[8];
				System.arraycopy(a, 0, b, 0, 7);
				a[5] = root + name + "_RF.owl";
				b[7] = "-gt"; 
				SkillsOntology.main(a);
				SkillsOntology.main(b);
			}
		}
		String[] c = new String[6];
		c[0] = "-s";
		c[2] = "-t";
		c[4] = "-o";
		String[] d = new String[8];
		d[0] = "-s";
		d[2] = "-t";
		d[4] = "-o";
		d[6] = "-r";
		for(String t : tasks)
		{
			c[1] = root + t + "_GT.owl";
			d[1] = root + t + "_RF.owl";
			for(String a : authors)
			{
				c[3] = root + a + "_GT.owl";
				d[3] = root + a + "_GT.owl";
				String ref = root + t + "_x_" + a + "_ref.rdf";
				c[5] = ref;
				d[5] = root + t + "_x_" + a + ".rdf";
				d[7] = ref;
				SkillsMatcher.main(c);
				SkillsMatcher.main(d);
			}
		}
	}
}