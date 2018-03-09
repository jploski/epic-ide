/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.epic.perleditor.templates.perl;

import java.util.ArrayList;

import org.epic.perleditor.templates.ContextType;
import org.epic.perleditor.templates.perl.CompilationUnitContextType;
import org.epic.perleditor.templates.perl.PerlUnitContext;
import org.epic.perleditor.PerlPluginImages;
//import net.sourceforge.phpdt.internal.ui.text.java.IPHPCompletionProposal;

//import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.swt.graphics.Point;
//import org.eclipse.jdt.internal.ui.text.link.LinkedPositionManager;

public class SubroutineEngine {

    /** The context type. */
    private ContextType fContextType;
    /** The result proposals. */
    private ArrayList<SubroutineProposal> fProposals= new ArrayList<SubroutineProposal>();

    /**
     * Creates the template engine for a particular context type.
     * See <code>TemplateContext</code> for supported context types.
     */
    public SubroutineEngine(ContextType contextType) {
    //	Assert.isNotNull(contextType);
        fContextType= contextType;
    }

    /**
     * Empties the collector.
     * 
     * @param viewer the text viewer  
     * @param unit   the compilation unit (may be <code>null</code>)
     */
    public void reset() {
        fProposals.clear();
    }

    /**
     * Returns the array of matching templates.
     */
    public IPerlCompletionProposal[] getResults() {
        return fProposals.toArray(new IPerlCompletionProposal[fProposals.size()]);
    }

    /**
     * Inspects the context of the compilation unit around <code>completionPosition</code>
     * and feeds the collector with proposals.
     * @param viewer the text viewer
     * @param completionPosition the context position in the document of the text viewer
     * @param compilationUnit the compilation unit (may be <code>null</code>)
     */
    public void complete(ITextViewer viewer, int completionPosition, Object[] identifiers)
  //,ICompilationUnit compilationUnit)
    //hrows JavaModelException
    {
        IDocument document= viewer.getDocument();
        
        // prohibit recursion
//		if (LinkedPositionManager.hasActiveManager(document))
//			return;

        if (!(fContextType instanceof CompilationUnitContextType))
            return;
        
    Point selection= viewer.getSelectedRange();
    // remember selected text
    // String selectedText= null;
    // if (selection.y != 0) {
    //  try {
    //  selectedText= document.get(selection.x, selection.y);
    // } catch (BadLocationException e) {e.printStackTrace();}
    // }

    ((CompilationUnitContextType) fContextType).setContextParameters(document, completionPosition, selection.y);//mpilationUnit);

        PerlUnitContext context= (PerlUnitContext) fContextType.createContext();
        int start= context.getStart();
        int end= context.getEnd();
        IRegion region= new Region(start, end - start);

//		Template[] templates= Templates.getInstance().getTemplates();
    String subroutine = null;
        for (int i= 0; i != identifiers.length; i++) {
            subroutine = (String) identifiers[i];
                
            if (context.canEvaluate(subroutine, true)) {
                fProposals.add(new SubroutineProposal(subroutine, context, region, viewer, PerlPluginImages.get(PerlPluginImages.IMG_ICON_SUBROUTINE), PerlPluginImages.get(PerlPluginImages.IMG_ICON_SUBROUTINE))); 
      }
    }
    }

}

