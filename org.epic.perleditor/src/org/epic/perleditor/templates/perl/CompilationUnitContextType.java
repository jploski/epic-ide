/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.epic.perleditor.templates.perl;

import org.epic.perleditor.templates.ContextType;

import org.eclipse.jface.text.IDocument;

/**
 * Compilation unit context type.
 */
public abstract class CompilationUnitContextType extends ContextType {
	
	/** the document */
	protected IDocument fDocument;

	/** the completion position within the document string */
	protected int fOffset;
  
  /** the completion length */
  protected int fLength;
  
	/** the associated compilation unit, may be <code>null</code> */
	//protected ICompilationUnit fCompilationUnit;
/*
	protected static class ReturnType extends TemplateVariable {
	 	public ReturnType() {
	 	 	super(JavaTemplateMessages.getString("CompilationUnitContextType.variable.name.return.type"), JavaTemplateMessages.getString("CompilationUnitContextType.variable.description.return.type")); //$NON-NLS-1$ //$NON-NLS-2$
	 	}
    
	 	public String evaluate(TemplateContext context) {
			IJavaElement element= ((CompilationUnitContext) context).findEnclosingElement(IJavaElement.METHOD);
			if (element == null)
				return null;

			try {
				return Signature.toString(((IMethod) element).getReturnType());
			} catch (JavaModelException e) {
				return null;
			}
		}
    
		public boolean isResolved(TemplateContext context) {
			return evaluate(context) != null;
		}		
	}

	protected static class File extends TemplateVariable {
		public File() {
			super(JavaTemplateMessages.getString("CompilationUnitContextType.variable.name.file"), JavaTemplateMessages.getString("CompilationUnitContextType.variable.description.file")); //$NON-NLS-1$ //$NON-NLS-2$
		}
		public String evaluate(TemplateContext context) {
			ICompilationUnit unit= ((CompilationUnitContext) context).getCompilationUnit();
			
			return (unit == null) ? null : unit.getElementName();
      return null;
		}
		public boolean isResolved(TemplateContext context) {
			return evaluate(context) != null;
		}		
	}

	protected static class EnclosingJavaElement extends TemplateVariable {
		protected final int fElementType;
		
		public EnclosingJavaElement(String name, String description, int elementType) {
			super(name, description);
			fElementType= elementType;
		}
		public String evaluate(TemplateContext context) {
			IJavaElement element= ((CompilationUnitContext) context).findEnclosingElement(fElementType);
			return (element == null) ? null : element.getElementName();			
		}
		public boolean isResolved(TemplateContext context) {
			return evaluate(context) != null;
		}
	}
	
	protected static class Method extends EnclosingJavaElement {
		public Method() {
			super(JavaTemplateMessages.getString("CompilationUnitContextType.variable.name.enclosing.method"), JavaTemplateMessages.getString("CompilationUnitContextType.variable.description.enclosing.method"), IJavaElement.METHOD); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	protected static class Type extends EnclosingJavaElement {
		public Type() {
			super(JavaTemplateMessages.getString("CompilationUnitContextType.variable.name.enclosing.type"), JavaTemplateMessages.getString("CompilationUnitContextType.variable.description.enclosing.type"), IJavaElement.TYPE); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	protected static class Package extends EnclosingJavaElement {
		public Package() {
			super(JavaTemplateMessages.getString("CompilationUnitContextType.variable.name.enclosing.package"), JavaTemplateMessages.getString("CompilationUnitContextType.variable.description.enclosing.package"), IJavaElement.PACKAGE_FRAGMENT); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}	

	protected static class Project extends EnclosingJavaElement {
		public Project() {
			super(JavaTemplateMessages.getString("CompilationUnitContextType.variable.name.enclosing.project"), JavaTemplateMessages.getString("CompilationUnitContextType.variable.description.enclosing.project"), IJavaElement.JAVA_PROJECT); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}	


	protected static class Arguments extends TemplateVariable {
		public Arguments() {
			super(JavaTemplateMessages.getString("CompilationUnitContextType.variable.name.enclosing.method.arguments"), JavaTemplateMessages.getString("CompilationUnitContextType.variable.description.enclosing.method.arguments")); //$NON-NLS-1$ //$NON-NLS-2$
		}
		public String evaluate(TemplateContext context) {
			IJavaElement element= ((CompilationUnitContext) context).findEnclosingElement(IJavaElement.METHOD);
			if (element == null)
				return null;
				
			IMethod method= (IMethod) element;
			
			try {
				String[] arguments= method.getParameterNames();
				StringBuffer buffer= new StringBuffer();
				
				for (int i= 0; i < arguments.length; i++) {
					if (i > 0)
						buffer.append(", "); //$NON-NLS-1$
					buffer.append(arguments[i]);				
				}
				
				return buffer.toString();

			} catch (JavaModelException e) {
				return null;
			}
		}
	}
*/

	/*
	 * @see ContextType#ContextType(String)
	 */
	public CompilationUnitContextType(String name) {
		super(name);	
	}

	/**
	 * Sets context parameters. Needs to be called before createContext().
	 */
	public void setContextParameters(IDocument document, int position, int length) {//, ICompilationUnit compilationUnit) {
		fDocument= document;
		fOffset= position;
    fLength= length;
//		fCompilationUnit= compilationUnit;
	}

}
