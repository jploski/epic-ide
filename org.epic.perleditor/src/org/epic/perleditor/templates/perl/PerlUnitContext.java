/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.epic.perleditor.templates.perl;

import org.epic.perleditor.templates.ContextType;
import org.epic.perleditor.templates.DocumentTemplateContext;
import org.epic.perleditor.templates.Template;
import org.epic.perleditor.templates.TemplateBuffer;
import org.epic.perleditor.templates.TemplateTranslator;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

/**
 * A compilation unit context.
 */
public class PerlUnitContext extends DocumentTemplateContext {

  /** The platform default line delimiter. */
  private static final String PLATFORM_LINE_DELIMITER = System.getProperty("line.separator"); //$NON-NLS-1$

  // To allow templates starting with "= # $ @ %"
  private static final String specialChars = "=#$@%<";
  /** The compilation unit, may be <code>null</code>. */
  //	private final ICompilationUnit fCompilationUnit;

  /**
   * Creates a compilation unit context.
   * 
   * @param type   the context type.
   * @param document the document.
   * @param completionPosition the completion position within the document.
   * @param compilationUnit the compilation unit (may be <code>null</code>).
   */
  protected PerlUnitContext(ContextType type, IDocument document, int completionPosition)
  //,ICompilationUnit compilationUnit)
  {
    super(type, document, completionPosition, 0);
    //	fCompilationUnit= compilationUnit;
  }

  /*
  * @see TemplateContext#canEvaluate(Template templates)
  */
  public boolean canEvaluate(Template template) {
    // return fForceEvaluation || 
    return template.matches(getKey(), getContextType().getName());
    
	// Testing: if nothing is specified, return all templates
	//String prefix = getKey();
	//return (prefix.length() == 0) || template.matches(getKey(), getContextType().getName());
  }

  /**
   * Returns <code>true</code> if template matches the prefix and context,
   * <code>false</code> otherwise.
   */
  public boolean canEvaluate(String identifier, boolean showAllOnEmpty) {
    String prefix = getKey();
    
    if(showAllOnEmpty) {
    	return (prefix.length() == 0) || identifier.toLowerCase().startsWith(prefix.toLowerCase());
    }
    else {
    	return (prefix.length() != 0) && identifier.toLowerCase().startsWith(prefix.toLowerCase());
    }
// TODO delete!
//    return
//    //      fEnabled &&
//    //      fContextTypeName.equals(contextTypeName) &&
//  //(prefix.length() != 0) && identifier.toLowerCase().startsWith(prefix.toLowerCase());
//   
//    // If nothing is specified, return all identifiers
//	(prefix.length() == 0) || identifier.toLowerCase().startsWith(prefix.toLowerCase());
  }

  /*
  * @see TemplateContext#evaluate(Template template)
  */
  public TemplateBuffer evaluate(Template template) throws CoreException {
    if (!canEvaluate(template))
      return null;

    TemplateTranslator translator = new TemplateTranslator();
    TemplateBuffer buffer = translator.translate(template.getPattern());

    getContextType().edit(buffer, this);

    String lineDelimiter = null;
    try {
      lineDelimiter = getDocument().getLineDelimiter(0);
    } catch (BadLocationException e) {
    }

    if (lineDelimiter == null)
      lineDelimiter = PLATFORM_LINE_DELIMITER;

    //    ITemplateEditor formatter= new JavaFormatter(lineDelimiter);
    //    formatter.edit(buffer, this);

    return buffer;
  }

  /*
   * @see DocumentTemplateContext#getCompletionPosition();
   */
  public int getStart() {
    IDocument document = getDocument();
    try {
      int start = getCompletionOffset();

      while (((start != 0) && Character.isUnicodeIdentifierPart(document.getChar(start - 1)))
        || ((start != 0) && specialChars.indexOf(document.getChar(start - 1)) != (-1) )) {
        start--;
      }

      if (((start != 0) && Character.isUnicodeIdentifierStart(document.getChar(start - 1)))
        || ((start != 0) && specialChars.indexOf(document.getChar(start - 1)) != (-1) )) {
        start--;
      }

      return start;

    } catch (BadLocationException e) {
      return getCompletionOffset();
    }
  }

  /**
   * Returns the character before start position of completion.
   */
  public char getCharacterBeforeStart() {
    int start = getStart();

    try {
      return start == 0 ? ' ' : getDocument().getChar(start - 1);

    } catch (BadLocationException e) {
      return ' ';
    }
  }
  /**
   * Returns the compilation unit if one is associated with this context, <code>null</code> otherwise.
   */
  //	public final ICompilationUnit getCompilationUnit() {
  //		return fCompilationUnit;
  //	}

  /**
   * Returns the enclosing element of a particular element type, <code>null</code>
   * if no enclosing element of that type exists.
   */
  //	public IJavaElement findEnclosingElement(int elementType) {
  //		if (fCompilationUnit == null)
  //			return null;
  //
  //		try {
  //			IJavaElement element= fCompilationUnit.getElementAt(getStart());
  //			while (element != null && element.getElementType() != elementType)
  //				element= element.getParent();
  //			
  //			return element;
  //
  //		} catch (JavaModelException e) {
  //			return null;
  //		}	
  //	}

}
