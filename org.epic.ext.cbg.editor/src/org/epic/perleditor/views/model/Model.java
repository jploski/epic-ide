package org.epic.perleditor.views.model;
public class Model {
	protected SourceElement parent;
	protected String name;
	protected int offset,
	               numberOfLines,
	               length;	
	protected int type;
	
	
	
	public void setName(String name) {
		this.name = name;
	}
	
	public Model(String name, int offset, int numberOfLines,
	              int length) {
		this.name = name;
		this.offset = offset;
		this.numberOfLines = numberOfLines;
		this.length = length;
	}
	
	public Model(String name, int offset, int length) {
		this.name = name;
		this.offset = offset;
		this.numberOfLines = 0;
		this.length = length;
	}
	
	public SourceElement getParent() {
		return parent;
	}
	
	public String getName() {
		return name;
	}
	
	public int getType() {
		return type;
	}
		
	public Model(String name) {
		this.name = name;
	}
	
	public int getStart() {
		return offset;
	}
	
	public int getLength() {
		return length;
	}
	
	public Model() {
	}	


}
