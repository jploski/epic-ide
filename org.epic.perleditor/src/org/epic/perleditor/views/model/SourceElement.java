package org.epic.perleditor.views.model;

import java.util.ArrayList;
import java.util.List;

public class SourceElement extends Model {
	protected List nodes;
	protected List modules;
	protected List subroutines;
	
	public static final int SUBROUTINE_TYPE = 1;
	public static final int MODULE_TYPE = 2;

	public SourceElement() {
		nodes = new ArrayList();
		modules = new ArrayList();
		subroutines = new ArrayList();
	
	}
	
	
	public SourceElement(String name, int type) {
		this();
		this.name = name;
		this.type = type;
	}
	
	
	public void add(SourceElement node) {
		nodes.add(node);
		node.parent = this;
	}
	
	public void addModule(Module module) {
		modules.add(module);
		module.parent = this;
	}
	
	public void addSubroutine(Subroutine subroutine) {
		subroutines.add(subroutine);
		subroutine.parent = this;
	}
	
	public void addSubroutines(List subs) {
		for(int i = 0; i < subs.size(); i++) {
			Model model = (Model) subs.get(i);
			
			// Throw away subroutine prototypes
			if(model.getName().trim().endsWith(";")) {
				continue;
			}
			
			Subroutine subroutine = new Subroutine(model.getName(), model.getStart(), model.getLength());
			addSubroutine(subroutine);
		}
	}
	
	public void addModules(List subs) {
		for(int i = 0; i < subs.size(); i++) {
			Model model = (Model) subs.get(i);
			Module module = new Module(model.getName(), model.getStart(), model.getLength());
			addModule(module);
		}
	}
	
	public List getNodes() {
		return nodes;
	}
	
	public List getModules() {
		return modules;
	}
	
	public List getSubroutines() {
		return subroutines;
	}
	
	public void removeChildren() {
		modules = new ArrayList();
		subroutines = new ArrayList();
	}
	
	
}
