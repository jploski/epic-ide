package org.epic.perleditor.views.model;

public class Module extends Model {
	
	public Module(String name, int offset, int numberOfLines,
	              int length) {
		super(name, offset, numberOfLines, length);
	}
	
	public Module(String name, int offset, int length) {
		super(name, offset, length);
	}
	


}
