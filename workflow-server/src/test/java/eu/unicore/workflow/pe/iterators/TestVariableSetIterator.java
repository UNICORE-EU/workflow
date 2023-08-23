package eu.unicore.workflow.pe.iterators;

import java.util.List;

import org.junit.Test;

import eu.unicore.workflow.pe.iterators.VariableSetIterator.VariableSet;
import eu.unicore.workflow.pe.xnjs.ProcessVariables;

public class TestVariableSetIterator {
	
	@Test
	public void testVariableSetBuildValues()throws Exception{
		int N=10;
		VariableSet vs=new VariableSet("TEST","0","TEST<"+N,"TEST++");
		ProcessVariables vars=new ProcessVariables();
		List<String> values=vs.values(vars);
		assert values.size()==N;
		for(int i=0;i<10;i++){
			assert String.valueOf(i).equals(values.get(i));
		}
	}
	
	@Test
	public void testVariableSetIterator()throws Exception{
		int N=10;
		VariableSet vs1=new VariableSet("FOO","0","FOO<"+N,"FOO++");
		VariableSetIterator vsi = new VariableSetIterator(vs1);
		ProcessVariables vars=new ProcessVariables();
		vsi.reInit(vars);
		String [] values=vsi.values;
		assert values.length==N;
		for(int i=0;i<10;i++){
			assert String.valueOf(i).equals(values[i]);
		}
	}
	
	@Test
	public void testVariableSetFoldValues()throws Exception{
		int N=10;
		VariableSet vs1=new VariableSet("FOO","0","FOO<"+N,"FOO++");
		VariableSet vs2=new VariableSet("BAR","0","BAR<"+N,"BAR++");
		VariableSetIterator vsi = new VariableSetIterator(vs1, vs2);
		ProcessVariables vars=new ProcessVariables();
		vsi.reInit(vars);
		String [] values=vsi.values;
		assert values.length==N*N;
		assert "0_0".equals(values[0]);
		assert "9_9".equals(values[N*N-1]);
	}
	
	@Test
	public void testEmptySet()throws Exception{
		VariableSet vs=new VariableSet("TEST","0","TEST<0","TEST++");
		ProcessVariables vars=new ProcessVariables();
		List<String> values=vs.values(vars);
		assert values.size()==0;
	}
	
	@Test
	public void testGenerateNewValuesUsingOtherVariables()throws Exception{
		int N=3;
		VariableSet vs1=new VariableSet("FOO","0","FOO<"+N,"FOO++");
		VariableSet vs2=new VariableSet("BAR","0","BAR<"+N,"BAR=FOO+BAR+1");
		VariableSetIterator vsi = new VariableSetIterator(vs1, vs2);
		ProcessVariables vars=new ProcessVariables();
		vsi.reInit(vars);
		assert(6 == vsi.values.length);
	}

}
