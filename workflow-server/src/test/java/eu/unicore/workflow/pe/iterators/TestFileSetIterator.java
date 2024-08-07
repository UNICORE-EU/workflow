package eu.unicore.workflow.pe.iterators;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

import org.junit.jupiter.api.Test;

import eu.unicore.util.Pair;
import eu.unicore.workflow.pe.iterators.FileSetIterator.FileSet;
import eu.unicore.workflow.pe.iterators.ResolverFactory.Resolver;
import eu.unicore.workflow.pe.xnjs.ProcessVariables;
import eu.unicore.xnjs.ems.ExecutionException;

public class TestFileSetIterator {

	@Test
	public void testGetFile()throws Exception{
		FileSetIterator fsi=new FileSetIterator("wf",(FileSet[])null);
		String path="/foo/bar.txt";
		assert "bar.txt" .equals (fsi.getFile(path));
		path="test/";
		assert fsi.getFile(path)==null;
	}
	
	static long size = 0l;
	static int total = 100;
	static boolean random=false;
	static Random r=new Random();
	
	public static class R1 implements Resolver {

		public boolean acceptBase(String base) {
			return true;
		}
		public Collection<Pair<String, Long>> resolve(String workflowID, FileSet fileset)
				throws ExecutionException {
			ArrayList<Pair<String, Long>>results = new ArrayList<>();
			for(int i=0;i<total;i++){
				long thisSize=size+(random?size+r.nextInt(1024*1024):size);
				results.add(new Pair<String, Long>("file_"+i, thisSize));
			}
			return results;
		}
	}
	
	@Test
	public void testIterator()throws Exception{
		ResolverFactory.clear();
		ResolverFactory.registerResolver(R1.class);
		FileSet fs=new FileSet("test/", null, null, false, false);
		FileSetIterator fsi=new FileSetIterator("wf",fs);
		fsi.setIteratorName("IT");
		ProcessVariables vars=new ProcessVariables();
		fsi.reset(vars);
		fsi.next(vars);
		fsi.fillContext(vars);
		assert 100==fsi.values.length: "got "+fsi.values.length;
	}
	
	

	@Test
	public void testDefaultChunkedIteratorFormatString()throws Exception {
		String format = ForEachFileIterator.DEFAULT_FORMAT;
		String result=MessageFormat.format(format, 1, "test",".png");
		assert "1_test.png".equals(result);
		
		result=MessageFormat.format(format, 1, "test", "");
		assert "1_test".equals(result);
	}

	@Test
	public void testForEachFileIterator()throws Exception{
		ResolverFactory.clear();
		ResolverFactory.registerResolver(R1.class);
		
		FileSet fs=new FileSet("test/", null, null, false,false);
		FileSetIterator fsi=new FileSetIterator("wf",fs);
		fsi.setIteratorName("IT");
		ProcessVariables vars=new ProcessVariables();
		
		ForEachFileIterator iter = new ForEachFileIterator(fsi, 5);
		iter.setIteratorName("IT");
		iter.reset(vars);
		iter.next(vars);
		iter.fillContext(vars);
		
		assert Boolean.TRUE.equals(vars.get(ForEachFileIterator.PV_IS_CHUNKED));
		assert Integer.valueOf(5).equals(vars.get("IT"+ForEachFileIterator.PV_THIS_CHUNK_SIZE));

		for(int i=0;i<19;i++){
			vars.put(ForEachFileIterator.PV_IS_CHUNKED, null);
			vars.put(ForEachFileIterator.PV_THIS_CHUNK_SIZE, null);
			iter.next(vars);
			iter.fillContext(vars);
		}

		assert Boolean.TRUE.equals(vars.get(ForEachFileIterator.PV_IS_CHUNKED));
		assert Integer.valueOf(5).equals(vars.get("IT"+ForEachFileIterator.PV_THIS_CHUNK_SIZE));
		assert "file_99".equals(vars.get("IT"+ForEachFileIterator.PV_FILENAME+"_5"));
		
		assert iter.hasNext()==false;
	}

	@Test
	public void testForEachFileIteratorSizedChunks()throws Exception{
		ResolverFactory.clear();
		random=true;
		ResolverFactory.registerResolver(R1.class);
		
		FileSet fs=new FileSet("test/", null, null, false, false);
		FileSetIterator fsi=new FileSetIterator("wf",fs);
		fsi.setIteratorName("IT");
		ProcessVariables vars=new ProcessVariables();
		
		int max=2048;
		//this iterator should return chunks that are either not bigger than  "max" kbytes 
		//or contain exactly a single file
		ForEachFileIterator iter = new ForEachFileIterator(fsi, max, ForEachFileIterator.Type.SIZE);
		iter.setIteratorName("IT");
		iter.reset(vars);
		iter.next(vars);
		iter.fillContext(vars);
		
		assert Boolean.TRUE.equals(vars.get(ForEachFileIterator.PV_IS_CHUNKED));
		assert "IT".equals(vars.get(ForEachFileIterator.PV_ITERATOR_NAME));
		
		while(iter.hasNext()){
			vars.remove(ForEachFileIterator.PV_IS_CHUNKED);
			vars.remove(ForEachFileIterator.PV_THIS_CHUNK_SIZE);
			iter.next(vars);
			iter.fillContext(vars);
			Integer count =vars.get(ForEachFileIterator.PV_THIS_CHUNK_SIZE, Integer.class);
			Long total = vars.get(ForEachFileIterator.PV_AGGREGATED_CHUNK_SIZE, Long.class);
			assert count!=null;
			assert total!=null;
			assert (count==1 || total<max*1024) : "count="+count+" total="+total;
		}
		System.out.println(vars);
	}	
}
