package eu.unicore.workflow;

import org.junit.Test;

import eu.unicore.workflow.builder.ForEach;
import eu.unicore.workflow.builder.Workflow;

public class TestBuilderAPI {

	@Test
	public void testBuilder1() throws Exception {
		Workflow w = new Workflow();
		w.variable("COUNTER");
		w.job("date1").at_site("DEMO-SITE").application("Date");
		w.job("date2").at_site("DEMO-SITE").application("Date");
		w.modify_variable("mod").name("COUNTER").expression("COUNTER++");
		w.transition("date1", "date2");
		w.transition("date2", "mod");
		System.out.println(w.getJSON().toString(2));
	}
	
	@Test
	public void testBuilder2() throws Exception {
		Workflow w = new Workflow();
		ForEach fe = w.for_each("foreach1");
		fe.values("a", "b", "c");
		fe.body().job("date1").at_site("DEMO-SITE").application("Date");
		fe.body().job("date2").at_site("DEMO-SITE").application("Date");
		fe.body().transition("date1", "date2");
		System.out.println(w.getJSON().toString(2));
	}
	
	@Test
	public void testBuilder3() throws Exception {
		Workflow w = new Workflow();
		ForEach fe = w.for_each("foreach1").iterator_name("IT");
		fe.fileset()
			.base_url("https://localhost:8080/DEMO-SITE/rest/core/storages/HOME")
			.includes("Documents/*.pdf").recurse();
		fe.body().job("date1").at_site("DEMO-SITE").executable("ls").arguments("-l")
			.stagein().from("${IT}").to("input.pdf");
		System.out.println(w.getJSON().toString(2));
	}

}
