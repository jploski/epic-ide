/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.epic.perleditor.templates.perl;

import org.epic.perleditor.templates.TemplateContext;

/**
 * A context type for java code.
 */
public class PerlContextType extends CompilationUnitContextType {
/*
	protected static class Array extends TemplateVariable {
		public Array() {
			super(JavaTemplateMessages.getString("JavaContextType.variable.name.array"), JavaTemplateMessages.getString("JavaContextType.variable.description.array")); //$NON-NLS-1$ //$NON-NLS-2$
		}
	    public String evaluate(TemplateContext context) {
	        return ((JavaContext) context).guessArray();
	    }
	}

	protected static class ArrayType extends TemplateVariable {
	    public ArrayType() {
	     	super(JavaTemplateMessages.getString("JavaContextType.variable.name.array.type"), JavaTemplateMessages.getString("JavaContextType.variable.description.array.type")); //$NON-NLS-1$ //$NON-NLS-2$
	    }
	    public String evaluate(TemplateContext context) {
	        return ((JavaContext) context).guessArrayType();
	    }
	}

	protected static class ArrayElement extends TemplateVariable {
	    public ArrayElement() {
	     	super(JavaTemplateMessages.getString("JavaContextType.variable.name.array.element"), JavaTemplateMessages.getString("JavaContextType.variable.description.array.element"));	//$NON-NLS-1$ //$NON-NLS-2$    
	    }
	    public String evaluate(TemplateContext context) {
	        return ((JavaContext) context).guessArrayElement();
	    }	    
	}

	protected static class Index extends TemplateVariable {
	    public Index() {
	     	super(JavaTemplateMessages.getString("JavaContextType.variable.name.index"), JavaTemplateMessages.getString("JavaContextType.variable.description.index")); //$NON-NLS-1$ //$NON-NLS-2$
	    }
	    public String evaluate(TemplateContext context) {
	        return ((JavaContext) context).getIndex();
	    }	    
	}

	protected static class Collection extends TemplateVariable {
	    public Collection() {
		    super(JavaTemplateMessages.getString("JavaContextType.variable.name.collection"), JavaTemplateMessages.getString("JavaContextType.variable.description.collection")); //$NON-NLS-1$ //$NON-NLS-2$
		}
	    public String evaluate(TemplateContext context) {
	        return ((JavaContext) context).guessCollection();
	    }
	}

	protected static class Iterator extends TemplateVariable {
	    public Iterator() {
		    super(JavaTemplateMessages.getString("JavaContextType.variable.name.iterator"), JavaTemplateMessages.getString("JavaContextType.variable.description.iterator")); //$NON-NLS-1$ //$NON-NLS-2$
		}
	    public String evaluate(TemplateContext context) {
	        return ((JavaContext) context).getIterator();
	    }	    
	}

*/	


	/**
	 * Creates a java context type.
	 */
	public PerlContextType() {
		super("perl"); //$NON-NLS-1$
		
		// global
		addVariable(new GlobalVariables.Cursor());
		addVariable(new GlobalVariables.Dollar());
		addVariable(new GlobalVariables.Date());
    	addVariable(new GlobalVariables.Year());
		addVariable(new GlobalVariables.Time());
		addVariable(new GlobalVariables.User());
		addVariable(new GlobalVariables.Filename());
		addVariable(new GlobalVariables.PerlInterpreter());
		
	}
	
	/*
	 * @see ContextType#createContext()
	 */	
	public TemplateContext createContext() {
		return new PerlUnitContext(this, fDocument, fOffset); //, fCompilationUnit);
	}

}
