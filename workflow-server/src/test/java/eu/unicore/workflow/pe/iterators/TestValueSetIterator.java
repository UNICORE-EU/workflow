package eu.unicore.workflow.pe.iterators;

import org.junit.jupiter.api.Test;

import eu.unicore.workflow.pe.model.Iterate;
import eu.unicore.workflow.pe.xnjs.ProcessVariables;

public class TestValueSetIterator {

	@Test
	public void testNoBase(){
		String[] values=new String[]{"foo","bar","baz"};
		ValueSetIterator vsi=new ValueSetIterator(values);
		ProcessVariables vars=new ProcessVariables();
		for(int i=0;i< values.length;i++){
			assert vsi.hasNext();
			vsi.next(vars);
			String next=vsi.getCurrentValue();
			assert next.equals(String.valueOf(i));
		}
		assert !vsi.hasNext();
	}
	
	@Test
	public void testBase(){
		String[] values=new String[]{"foo","bar","baz"};
		ValueSetIterator vsi=new ValueSetIterator(values);
		ProcessVariables vars=new ProcessVariables();
		String base="base";
		vsi.setBase(base);
		for(int i=0;i< values.length;i++){
			assert vsi.hasNext();
			vsi.next(vars);
			String next=vsi.getCurrentValue();
			assert (base+Iterate.SEPARATOR+String.valueOf(i)).equals(next);
		}
		assert !vsi.hasNext();
	}
	
	@Test
	public void testResolvedBase(){
		String[] values=new String[]{"foo","bar","baz"};
		ValueSetIterator vsi=new ValueSetIterator(values);
		ProcessVariables vars=new ProcessVariables();
		String base="BASE";
		String baseVal="base";
		vars.put(base, baseVal);
		vsi.setBase("${"+base+"}");
		for(int i=0;i< values.length;i++){
			assert vsi.hasNext();
			vsi.next(vars);
			String next=vsi.getCurrentValue();
			assert (baseVal+Iterate.SEPARATOR+String.valueOf(i)).equals(next);
		}
		assert !vsi.hasNext();
	}
	
	
}
