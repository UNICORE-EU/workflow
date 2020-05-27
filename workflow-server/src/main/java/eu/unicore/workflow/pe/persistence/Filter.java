package eu.unicore.workflow.pe.persistence;

public interface Filter {

	public boolean accept(String value);
	
}
