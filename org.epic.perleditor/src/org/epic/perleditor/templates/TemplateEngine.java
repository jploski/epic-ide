/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.epic.perleditor.templates;

import java.util.ArrayList;
import java.util.List;

import org.epic.perleditor.templates.ContextType;
import org.epic.perleditor.templates.DocumentTemplateContext;
import org.epic.perleditor.templates.Template;
import org.epic.perleditor.templates.Templates;
import org.epic.perleditor.templates.perl.CompilationUnitContextType;
import org.epic.perleditor.PerlPluginImages;
import org.epic.perleditor.templates.perl.IPerlCompletionProposal;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.swt.graphics.Point;

public class TemplateEngine
{
    /** The context type. */
    private final ContextType contextType;

    /** The result proposals. */
    private final List<TemplateProposal> proposals;

    /**
     * Creates the template engine for a particular context type. See
     * <code>TemplateContext</code> for supported context types.
     */
    public TemplateEngine(ContextType contextType)
    {
        this.contextType = contextType;
        this.proposals = new ArrayList<TemplateProposal>();
    }

    /**
     * Empties the collector.
     */
    public void reset()
    {
        proposals.clear();
    }

    /**
     * Returns the array of matching templates.
     */
    public IPerlCompletionProposal[] getResults()
    {
        return proposals.toArray(new IPerlCompletionProposal[proposals.size()]);
    }

    /**
     * Inspects the context of the source text around
     * <code>completionPosition</code> and feeds the collector
     * with proposals.
     * 
     * @param viewer
     *        the text viewer
     * @param completionPosition
     *        the context position in the document of the text viewer
     */
    public void complete(ITextViewer viewer, int completionPosition)
    {
        IDocument document = viewer.getDocument();

        if (!(contextType instanceof CompilationUnitContextType)) return;

        Point selection = viewer.getSelectedRange();
        ((CompilationUnitContextType) contextType).setContextParameters(
            document, completionPosition, selection.y);

        DocumentTemplateContext context =
            (DocumentTemplateContext) contextType.createContext();

        int start = context.getStart();
        int end = context.getEnd();
        IRegion region = new Region(start, end - start);

        Template[] templates = Templates.getInstance().getTemplates();
        for (int i = 0; i != templates.length; i++)
        {
            if (context.canEvaluate(templates[i]))
            {
                proposals.add(new TemplateProposal(
                    templates[i],
                    context,
                    region,
                    viewer,
                    PerlPluginImages.get(PerlPluginImages.IMG_OBJS_TEMPLATE)));
            }
        }
    }
}