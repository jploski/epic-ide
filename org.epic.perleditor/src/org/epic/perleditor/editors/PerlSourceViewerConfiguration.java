package org.epic.perleditor.editors;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.reconciler.MonoReconciler;
import org.eclipse.jface.text.source.*;
import org.eclipse.swt.graphics.RGB;
import org.epic.perleditor.PerlEditorPlugin;
import org.epic.perleditor.editors.perl.*;
import org.epic.perleditor.editors.util.PreferenceUtil;
import org.epic.perleditor.preferences.PreferenceConstants;

/**
 * @author luelljoc
 * 
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class PerlSourceViewerConfiguration extends SourceViewerConfiguration
{
    private final IPreferenceStore prefs;
    private final PerlEditor editor;

    public PerlSourceViewerConfiguration(
        IPreferenceStore store, PerlEditor textEditor)
    {
        assert store != null;
        
        prefs = store;
        editor = textEditor;
    }

    public IAnnotationHover getAnnotationHover(ISourceViewer sourceViewer)
    {
        if (editor == null) return null;
        return new PerlAnnotationHover(editor);
    }

    public IAutoIndentStrategy getAutoIndentStrategy(
        ISourceViewer sourceViewer, String contentType)
    {
        return (IDocument.DEFAULT_CONTENT_TYPE.equals(contentType)
            ? new PerlAutoIndentStrategy()
            : new DefaultAutoIndentStrategy());
    }
    
    public String[] getConfiguredContentTypes(ISourceViewer sourceViewer)
    {
        return PartitionTypes.getTypes();
    }

    public IContentAssistant getContentAssistant(ISourceViewer sourceViewer)
    {
        if (editor == null) return null;
        
        ContentAssistant assistant = new ContentAssistant();

        // Enable content assist for all content types
        String[] contentTypes = this.getConfiguredContentTypes(sourceViewer);
        for (int i = 0; i < contentTypes.length; i++)
        {
            assistant.setContentAssistProcessor(
                new PerlCompletionProcessor(editor), contentTypes[i]);
        }

        assistant.enableAutoActivation(true);
        assistant.enableAutoInsert(true);
        assistant.setAutoActivationDelay(500);
        assistant.setProposalPopupOrientation(ContentAssistant.PROPOSAL_OVERLAY);
        assistant.setContextInformationPopupOrientation(ContentAssistant.CONTEXT_INFO_ABOVE);
        assistant.setContextInformationPopupBackground(
            PerlEditorPlugin.getDefault().getColor(new RGB(0, 0, 0)));
        assistant.setProposalSelectorBackground(
            PerlEditorPlugin.getDefault().getColor(new RGB(255, 255, 255)));
        assistant.setInformationControlCreator(getInformationControlCreator(sourceViewer));
        
        return assistant;
    }

    public String[] getDefaultPrefixes(
        ISourceViewer sourceViewer,
        String contentType)
    {
        return new String[] { "#", "" }; //$NON-NLS-1$

    }

    public ITextDoubleClickStrategy getDoubleClickStrategy(
        ISourceViewer sourceViewer, String contentType)
    {
        return new PerlDoubleClickSelector();
    }

    public IHyperlinkDetector[] getHyperlinkDetectors(ISourceViewer sourceViewer)
    {
        if (editor == null) return null;
        
        // Add PerlSubHyperlinkDetector to the list provided by the superclass        

        IHyperlinkDetector[] superDetectors = super.getHyperlinkDetectors(sourceViewer);
        if (superDetectors == null) superDetectors = new IHyperlinkDetector[0];

        IHyperlinkDetector[] ourDetectors =
            new IHyperlinkDetector[superDetectors.length + 1];
        ourDetectors[ourDetectors.length - 1] =
            new PerlSubHyperlinkDetector((PerlEditor) editor);

        return ourDetectors;
    }

    public String[] getIndentPrefixes(ISourceViewer sourceViewer, String contentType)
    {
        return new String[] { PreferenceUtil.getTab(0), "\t" };
    }

    public int getTabWidth(ISourceViewer sourceViewer)
    {
        return prefs.getInt(PreferenceConstants.EDITOR_TAB_WIDTH);
    }

    public ITextHover getTextHover(ISourceViewer sourceViewer, String contentType)
    {
        if (editor == null) return null;
        
        return new PerlTextHover(editor);
    }
    
    public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer)
    {
        return new PerlPresentationReconciler(prefs);
    }

    public IReconciler getReconciler(ISourceViewer sourceViewer)
    {
        if (editor == null) return null;
        
        MonoReconciler r = new MonoReconciler(
            new PerlReconcilingStrategy(sourceViewer, editor),
            false);

        r.setDelay(prefs.getInt(
            PerlEditorPlugin.SYNTAX_VALIDATION_INTERVAL_PREFERENCE));

        return r;
    }
}