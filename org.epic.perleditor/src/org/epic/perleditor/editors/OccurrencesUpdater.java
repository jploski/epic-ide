package org.epic.perleditor.editors;

import java.util.*;
import java.util.regex.Pattern;

import org.eclipse.jface.text.*;
import org.eclipse.jface.text.source.*;
import org.eclipse.jface.viewers.*;
import org.epic.core.util.StatusFactory;
import org.epic.perleditor.PerlEditorPlugin;
import org.epic.perleditor.preferences.MarkOccurrencesPreferences;

/**
 * This class marks occurrences of a selection in the current document. The
 * class uses the document partitioner to mark the surrounding text. Which types
 * are provided, can be determined by the preference page "Mark Occurences".
 *
 * @author Katrin Dust
 */
public class OccurrencesUpdater implements ISelectionChangedListener
{
    // ~ Static fields/initializers

    /** Pattern used to match a char (a-z or A-Z) or a digit */
    private static final Pattern LETTER_PATTERN = Pattern.compile("[a-zA-Z]||\\d");

    /** Annotation type used in extension point */
    private static final String ANNOTATION_TYPE = "org.epic.perleditor.occurrence";
    
    /** Pattern used to match an attribute or subroutine name */
    private static final Pattern NAME_PATTERN = Pattern.compile("[a-zA-Z_&][a-zA-Z0-9_]*");

    // ~ Instance fields

    /** The currently monitored ISourceViewer */
    private ISourceViewer sourceViewer;

    /** List of current annotations (used for removing) */
    private final LinkedList annotations = new LinkedList();

    /** Stores the last marked text */
    private String lastMarkedText = "";

    // ~ Methods

    /**
     * Starts listening to selection events (caret movements) in
     * the given ISourceViewer. Also adds occurrence annotations
     * according to the current caret location.
     * <p>
     * This method must not be called after the OccurrencesUpdater
     * has been already installed.
     */
    public void install(ISourceViewer sourceViewer)
    {
        assert this.sourceViewer == null : "already installed";
        this.sourceViewer = sourceViewer;

        ISelectionProvider selectionProvider = sourceViewer.getSelectionProvider();

        // If the selection provider is a post selection provider, post
        // selection changed events are the preferred choice, otherwise normal
        // selection changed events are requested.

        if (selectionProvider instanceof IPostSelectionProvider)
        {
            IPostSelectionProvider provider = (IPostSelectionProvider) selectionProvider;
            provider.addPostSelectionChangedListener(this);
        }
        else
        {
            selectionProvider.addSelectionChangedListener(this);
        }

        assert annotations.isEmpty();
        updateAnnotations((ITextSelection) selectionProvider.getSelection());
    }

    /**
     * Reacts to an updated caret location or new selection by highlighting
     * occurrences.
     * <p>
     * The newly selected text and its further occurrences will be marked
     * provided that the type of the text is selected in the preference page
     * "Mark Occurrences". Old markers are removed. If there is no selection,
     * the typedRegion of the document partitioner in which the caret is
     * located determines the marked text. Further occurrences of the text
     * will be marked if they have the same contentType. Variables are also
     * marked in strings.
     */
    public void selectionChanged(SelectionChangedEvent event)
    {
        ITextSelection textSelection = (ITextSelection) event.getSelection();
        updateAnnotations(textSelection);
	}

    /**
     * Stops listening to selection events (caret movements) in
     * the current ISourceViewer. Also removes any occurrence annotations
     * that may already be present.
     */
    public void uninstall()
    {
        assert sourceViewer != null;

        ISelectionProvider selectionProvider = sourceViewer.getSelectionProvider();

        if (selectionProvider instanceof IPostSelectionProvider)
        {
            ((IPostSelectionProvider) selectionProvider)
                .removePostSelectionChangedListener(this);
        }
        else
        {
            selectionProvider.removeSelectionChangedListener(this);
        }

        removeAnnotations();
        lastMarkedText = "";
        sourceViewer = null;
    }

    /**
     * Adds a new Annotation to the model. The offset and the length
     * determine the position of the annotation. The annotation is not
     * persistent.
     *
     * @param text
     *            the associated text of the annotation
     * @param newAnnotations
     *            the model to which the annotation will be added
     * @param offset
     *            the offset of the annotation position
     * @param length
     *            the length of the annotation position
     */
    private void addAnnotation(
        String text, Map newAnnotations, int offset, int length)
    {
        Annotation annotation = new Annotation(ANNOTATION_TYPE, false, text);
        Position position = new Position(offset, length);
        annotations.add(annotation);
        newAnnotations.put(annotation, position);
    }

    /**
     * Returns the text considered as the "occurrence" to be marked for
     * a given textSelection and document. If there is no selection,
     * the text of the typedRegion with the caret, as determined by
     * the document partitioner, is returned; otherwise the selected text.
     *
     * @param doc
     *            doc of the textselection
     * @param textSelection
     *            textselection within the document
     * @return the occurrence's text
     *
     * @throws BadLocationException
     *             thrown if the textSelection is not within the document
     */
    private String getMarkedText(IDocument doc, ITextSelection textSelection)
        throws BadLocationException
    {
        String text;
        if (textSelection.getLength() < 1)
        {
            ITypedRegion typedRegion = doc.getDocumentPartitioner()
                .getPartition(textSelection.getOffset());
            text = doc.get(typedRegion.getOffset(), typedRegion.getLength());
        }
        else
        {
            text = textSelection.getText();
        }
        return text != null ? text : "";
    }

    /**
     * Marks occurrences of the text in the document by adding annotations
     * to the given model. Further occurrences of the text and the
     * text itself will be marked, if they have the same contentType.
     * Variables are also marked in strings.
     *
     * @param doc
     *            the document, used to get a FindReplaceDocumentAdapter
     *            and the contentTypes
     * @param type
     *            the contentType of the associated text
     * @param text
     *            the text, further occurrences will be marked
     * @param model
     *            the model, the annotation were added
     */
    private void markText(
        IDocument doc, String type, String text, IAnnotationModelExtension model)
        throws BadLocationException
    {
        int offset = 0;
        String docText = doc.get();
        int index = docText.indexOf(text, offset);

        Map newAnnotations = new HashMap();
        while (index != -1)
        {
            offset = index + text.length();
            String contentType = doc.getContentType(index);
            if (contentType.equals(type)
                || (contentType.equals(PartitionTypes.LITERAL1) && type
                	.equals(PartitionTypes.VARIABLE))
                || (contentType.equals(PartitionTypes.VARIABLE) && type
                    .equals(PartitionTypes.LITERAL1)))
            {
                if (offset < doc.getLength())
                {
                    String behind = "" + doc.getChar(offset);
    
                    if (!(LETTER_PATTERN.matcher(behind)).matches())
                    {
                        addAnnotation(text, newAnnotations, index, text.length());
                    }
                }
            }
            index = docText.indexOf(text, offset);
        }
        model.replaceAnnotations(new Annotation[] {}, newAnnotations);
    }

    /**
     * Removes all occurrence annotations.
     */
    private void removeAnnotations()
    {
        IAnnotationModel _model = this.sourceViewer.getAnnotationModel();
        IAnnotationModelExtension model = (IAnnotationModelExtension) _model;

        Annotation[] array = (Annotation[]) annotations
            .toArray(new Annotation[annotations.size()]);

        if (model != null) // the viewer's AnnotationModel may have been already disposed
            model.replaceAnnotations(array, Collections.EMPTY_MAP);

        annotations.clear();
    }
    
    /**
     * @return true if the given text selection should be marked as an occurrence;
     *         false otherwise
     */
    private boolean shouldMark(IDocument doc, ITextSelection selection)
    {
        IDocumentPartitioner partitioner = doc.getDocumentPartitioner();
        ITypedRegion partition = partitioner.getPartition(selection.getOffset()); 
        String contentType = partition.getType();
        
        // First, check to see if it among one of the configured content types
        if (shouldMarkContentType(contentType)) return true;               
        
        // Second, check if it could be a subroutine or attribute name.
        // These cannot be currently distinguished by content type alone,
        // so we do regexp-based name matching.
        if (!getBoolPref(MarkOccurrencesPreferences.NAME)) return false;
        if (!contentType.equals(PartitionTypes.DEFAULT)) return false;
        
        // Avoid matching very short names (for performance)
        if (partition.getLength() < 3) return false;
        
        try
        {
            String text = doc.get(partition.getOffset(), partition.getLength());
            return NAME_PATTERN.matcher(text).matches();
        }
        catch (BadLocationException e) // should never occur
        {
            logUnexpected(e);            
            return false;
        }
    }

    /**
     * @return true if the given contentType should be marked
     *         according to the Mark Occurrences preference page;
     *         false otherwise
     */
    private boolean shouldMarkContentType(String contentType)
    {
        if (contentType == null || contentType.equals(PartitionTypes.DEFAULT))
        {
            return false;
        }
        else if (contentType.equals(PartitionTypes.VARIABLE))
        {
            if (!getBoolPref(MarkOccurrencesPreferences.VARIABLE))
            {
                return false;
            }
        }
        else if (contentType.equals(PartitionTypes.COMMENT))
        {
            if (!getBoolPref(MarkOccurrencesPreferences.COMMENT))
            {
                return false;
            }
        }
        else if (contentType.equals(PartitionTypes.KEYWORD1)
            || contentType.equals(PartitionTypes.KEYWORD2))
        {
            if (!getBoolPref(MarkOccurrencesPreferences.KEYWORD))
            {
                return false;
            }
        }
        else if (contentType.equals(PartitionTypes.LITERAL1)
            || contentType.equals(PartitionTypes.LITERAL2))
        {
            if (!getBoolPref(MarkOccurrencesPreferences.LITERAL))
            {
                return false;
            }
        }
        else if (contentType.equals(PartitionTypes.NUMBER))
        {
            if (!getBoolPref(MarkOccurrencesPreferences.NUMBER))
            {
                return false;
            }
        }
        else if (contentType.equals(PartitionTypes.OPERATOR))
        {
            if (!getBoolPref(MarkOccurrencesPreferences.OPERATOR))
            {
                return false;
            }
        }
        if (contentType.equals(PartitionTypes.POD))
        {
            if (!getBoolPref(MarkOccurrencesPreferences.POD))
            {
                return false;
            }
        }
        return true;
    }

    /**
     * @return returns the boolean value for the given preference name
     */
    private boolean getBoolPref(String name)
    {
        return PerlEditorPlugin.getDefault().getPreferenceStore().getBoolean(name);
    }

    /**
     * Updates the current set of occurrence annotations.
     */
    private void updateAnnotations(ITextSelection textSelection)
    {
        IAnnotationModel _model = sourceViewer.getAnnotationModel();
        IAnnotationModelExtension model = (IAnnotationModelExtension) _model;
        
        if (model == null)
        {
            // This is apparently the case when opening older revisions
            // using Subclipse
            return;
        }

        IDocument doc = sourceViewer.getDocument();

        if (!getBoolPref(MarkOccurrencesPreferences.MARK_OCCURRENCES) ||
            !shouldMark(doc, textSelection))
        {
            if (!getBoolPref(MarkOccurrencesPreferences.KEEP_MARKS))
            {
                lastMarkedText = "";
                removeAnnotations();
            }
            return;
        }

        try
        {
            String text = getMarkedText(doc, textSelection);

            // Same text as before? avoid constantly removing/adding
            // occurrences while the caret is being moved within a marked
            // occurrence or jumping from one occurrrence to another
            if (text.equals(this.lastMarkedText)) return;

            this.lastMarkedText = text;
            removeAnnotations();

            String type = doc.getContentType(textSelection.getOffset());
            markText(doc, type, text, model);
        }
        catch (BadLocationException e)
        {
            logUnexpected(e);

            // emergency clean-up
            lastMarkedText = "";
            removeAnnotations();
        }
    }
    
    private void logUnexpected(Throwable t)
    {
        PerlEditorPlugin.getDefault().getLog().log(
            StatusFactory.createError(
                PerlEditorPlugin.getPluginId(),
                "An unexpected exception occurred in OccurrencesUpdater",
                t));
    }
}