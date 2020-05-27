package eu.unicore.workflow.pe.iterators;

import org.junit.Test;

import eu.unicore.workflow.pe.iterators.FileSetIterator.FileSet;

public class TestSMSResolver {

	@Test
	public void testMakeURL()throws Exception{
		SMSResolver r=new SMSResolver();
		String b1="BFT:http://localhost:8080/site/rest/core/storages/WORK/files/basedir";
		String u1=r.getURL(b1);
		assert "http://localhost:8080/site/rest/core/storages/WORK".equals(u1);
		String baseDir=r.getBaseDir(b1);
		System.out.println(baseDir);
		assert "/basedir".equals(baseDir);
	}
	
	@Test
	public void testMatchIncludes(){
		SMSResolver r=new SMSResolver();
		String[]includes=new String[]{"*.pdf"};
		FileSet f=new FileSet("/",includes,null,false,false);
		
		assert r.matches("/a.pdf", f);
		assert !r.matches("/a.txt", f);
		
		assert r.matches("/foo/a.pdf", f);
		assert !r.matches("/foo/a.txt", f);
	}
	
	@Test
	public void testMatchExcludes(){
		SMSResolver r=new SMSResolver();
		String[]excludes=new String[]{"*.txt"};
		FileSet f=new FileSet("/",null,excludes,false,false);
		
		assert r.matches("/a.pdf", f);
		assert !r.matches("/a.txt", f);
		
		assert r.matches("/foo/a.pdf", f);
		assert !r.matches("/foo/a.txt", f);
	}
	
	@Test
	public void testMatchIncludeExclude(){
		SMSResolver r=new SMSResolver();
		String[]includes=new String[]{"*.txt"};
		String[]excludes=new String[]{"a.txt"};
		FileSet f=new FileSet("/",includes,excludes,false,false);
		
		assert r.matches("/b.txt", f);
		assert !r.matches("/a.txt", f);
		assert !r.matches("/a.pdf", f);
		
		assert r.matches("/b.txt", f);
		assert !r.matches("/foo/a.pdf", f);
		assert !r.matches("/foo/a.txt", f);
	}
	
	
}
