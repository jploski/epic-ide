/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.epic.perleditor.templates.perl;

import org.epic.perleditor.templates.TemplateContext;
import org.epic.perleditor.templates.TemplateMessages;
//import org.epic.perleditor.templates.perl.PerlUnitContext;
//import org.epic.perleditor.templates.perl.IPerlCompletionProposal;
import org.epic.perleditor.templates.ui.LinkedPositionManager;
import org.epic.perleditor.templates.ui.LinkedPositionUI;
//import org.epic.perleditor.PerlEditorPlugin;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;
//import org.eclipse.jdt.internal.ui.text.link.LinkedPositionManager;
//import org.eclipse.jdt.internal.ui.text.link.LinkedPositionUI;
//import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

/**
 * A PHP identifier proposal.
 */
public class VariableProposal implements IPerlCompletionProposal {

  private final String fTemplate;
  private final TemplateContext fContext;
  private final ITextViewer fViewer;
  private final Image fImage_fun;
  private final Image fImage_var;
  private final IRegion fRegion;

  //private TemplateBuffer fTemplateBuffer;
  private String fOldText;
  private IRegion fSelectedRegion; // initialized by apply()

  /**
   * Creates a template proposal with a template and its context.
   * @param template  the template
   * @param context   the context in which the template was requested.
   * @param image     the icon of the proposal.
   */
  public VariableProposal(String template, TemplateContext context, IRegion region, ITextViewer viewer, Image image_fun, Image image_var) {
	//		Assert.isNotNull(template);
	//		Assert.isNotNull(context);
	//		Assert.isNotNull(region);
	//		Assert.isNotNull(viewer);

	fTemplate = template;
	fContext = context;
	fViewer = viewer;
	fImage_fun= image_fun;
	fImage_var= image_var;
	fRegion = region;
  }

  /*
   * @see ICompletionProposal#apply(IDocument)
   */
  public void apply(IDocument document) {
	try {
	  //		    if (fTemplateBuffer == null)
	  //				fTemplateBuffer= fContext.evaluate(fTemplate);

	  int start = fRegion.getOffset();
	  int end = fRegion.getOffset() + fRegion.getLength();

	  // insert template string
	//  String templateString = fTemplate; // fTemplateBuffer.getString();	
	  document.replace(start, end - start, fTemplate);

	  // translate positions
	  LinkedPositionManager manager = new LinkedPositionManager(document);
	  //			TemplatePosition[] variables= fTemplateBuffer.getVariables();
	  //			for (int i= 0; i != variables.length; i++) {
	  //				TemplatePosition variable= variables[i];
	  //
	  //				if (variable.isResolved())
	  //					continue;
	  //				
	  //				int[] offsets= variable.getOffsets();
	  //				int length= variable.getLength();
	  //				
	  //				for (int j= 0; j != offsets.length; j++)
	  //					manager.addPosition(offsets[j] + start, length);
	  //			}

	  LinkedPositionUI editor = new LinkedPositionUI(fViewer, manager);
	  editor.setFinalCaretOffset(fTemplate.length()+start);
   //   editor.setFinalCaretOffset(getCaretOffset(fTemplateBuffer) + start);
	  editor.enter();

	  fSelectedRegion = editor.getSelectedRegion();

	} catch (BadLocationException e) {
	  //PHPeclipsePlugin.log(e);
	  e.printStackTrace();
	  openErrorDialog(e);

	}
	//      catch (CoreException e) {
	//			handleException(e);
	//	    }	    
  }

//	private static int getCaretOffset(TemplateBuffer buffer) {
//	  TemplatePosition[] variables = buffer.getVariables();
//	  for (int i = 0; i != variables.length; i++) {
//		TemplatePosition variable = variables[i];
//
//		if (variable.getName().equals(JavaTemplateMessages.getString("GlobalVariables.variable.name.cursor"))) //$NON-NLS-1$
//		  return variable.getOffsets()[0];
//	  }
//
//	  return buffer.getString().length();
//	}

  /*
   * @see ICompletionProposal#getSelection(IDocument)
   */
  public Point getSelection(IDocument document) {
	return new Point(fSelectedRegion.getOffset(), fSelectedRegion.getLength());
	//	  return null;
  }

  /*
   * @see ICompletionProposal#getAdditionalProposalInfo()
   */
  public String getAdditionalProposalInfo() {
	//	    try {
	//			if (fTemplateBuffer == null)
	//				fTemplateBuffer= fContext.evaluate(fTemplate);


	//return textToHTML(fTemplate); // fTemplateBuffer.getString());
    
	//Do not return additional info for subroutines
	return null;

	//	    } catch (CoreException e) {
	//			handleException(e);		    
	//			return null;
	//	    }
  }

  /*
   * @see ICompletionProposal#getDisplayString()
   */
  public String getDisplayString() {
	// No description available for Perl subroutines
	return(fTemplate);
	//return fTemplate + TemplateMessages.getString("TemplateProposal.delimiter") + fTemplate; // $NON-NLS-1$ //$NON-NLS-1$
	//		return fTemplate.getName() + TemplateMessages.getString("TemplateProposal.delimiter") + fTemplate.getDescription(); // $NON-NLS-1$ //$NON-NLS-1$
  }

  /*
   * @see ICompletionProposal#getImage()
   */
  public Image getImage() {
	if (fTemplate.charAt(0)=='$') {
	  return fImage_var;
	}
		return fImage_fun;
  }

  /*
   * @see ICompletionProposal#getContextInformation()
   */
  public IContextInformation getContextInformation() {
	return null;
  }

  private static String textToHTML(String string) {
	StringBuffer buffer = new StringBuffer(string.length());
	buffer.append("<pre>"); //$NON-NLS-1$

	for (int i = 0; i != string.length(); i++) {
	  char ch = string.charAt(i);

	  switch (ch) {
		case '&' :
		  buffer.append("&amp;"); //$NON-NLS-1$
		  break;

		case '<' :
		  buffer.append("&lt;"); //$NON-NLS-1$
		  break;

		case '>' :
		  buffer.append("&gt;"); //$NON-NLS-1$
		  break;

		case '\t' :
		  buffer.append("    "); //$NON-NLS-1$
		  break;

		case '\n' :
		  buffer.append("<br>"); //$NON-NLS-1$
		  break;

		default :
		  buffer.append(ch);
		  break;
	  }
	}

	buffer.append("</pre>"); //$NON-NLS-1$
	return buffer.toString();
  }

  private void openErrorDialog(BadLocationException e) {
	Shell shell = fViewer.getTextWidget().getShell();
	MessageDialog.openError(shell, TemplateMessages.getString("TemplateEvaluator.error.title"), e.getMessage()); //$NON-NLS-1$
  }

  private void handleException(CoreException e) {
	Shell shell = fViewer.getTextWidget().getShell();
	//PHPeclipsePlugin.log(e);
	e.printStackTrace();
	//		ExceptionHandler.handle(e, shell, TemplateMessages.getString("TemplateEvaluator.error.title"), null); //$NON-NLS-1$
  }

  /*
   * @see IJavaCompletionProposal#getRelevance()
   */
  public int getRelevance() {

	if (fContext instanceof PerlUnitContext) {
	  PerlUnitContext context = (PerlUnitContext) fContext;
	  switch (context.getCharacterBeforeStart()) {
		// high relevance after whitespace
		case ' ' :
		case '\r' :
		case '\n' :
		case '\t' :
		  return 90;

		default :
		  return 0;
	  }
	} else {
	  return 90;
	}
  }

}