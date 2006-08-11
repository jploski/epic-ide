package org.epic.perleditor.editors;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.runtime.ILog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.projection.ProjectionAnnotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;
import org.epic.core.model.IMultilineElement;
import org.epic.core.util.StatusFactory;
import org.epic.perleditor.PerlEditorPlugin;
import org.epic.perleditor.preferences.PreferenceConstants;

/**
 * Responsible for keeping folds in sync with a source file's text
 * within a PerlEditor. This class relies on
 * {@link org.epic.core.model.SourceFile} to obtain positions of
 * foldable {@link org.epic.core.model.SourceElement}s.
 *
 * @author jploski
 */
public class FoldReconciler
{
    private final PerlEditor editor;
    private final Set folds; // of Annotation instances
    private final ILog log;

    /**
     * Creates a FoldReconciler for the given editor.
     */
    public FoldReconciler(PerlEditor editor)
    {
        this.editor = editor;
        this.log = PerlEditorPlugin.getDefault().getLog();
        this.folds = new HashSet();
    }

    /**
     * Updates folds based on the current state of the editor's SourceFile.
     * An invocation results in removing/adding zero or more fold annotations
     * to the document's annotation model.
     *
     * @see PerlEditor#getSourceFile()
     * @see org.eclipse.jface.text.source.projection.ProjectionAnnotationModel
     */
    public void reconcile()
    {
        if (! getPref(PreferenceConstants.SOURCE_FOLDING)) return;

        try
        {
            IAnnotationModel annotations = getAnnotations();
            if (annotations == null) return;

            Set pod = computeFoldPositions(editor.getSourceFile().getPODs());
            Set source = computeFoldPositions(editor.getSourceFile().getSubs());

            removeFolds(pod);
            removeFolds(source);

            addFolds(pod, getPref(PreferenceConstants.PERLDOC_FOLDING));
            addFolds(source, getPref(PreferenceConstants.SUBROUTINE_FOLDING));
        }
        catch (BadLocationException e)
        {
            // this one should never occur
            String pluginId = PerlEditorPlugin.getPluginId();
            log.log(StatusFactory.createError(pluginId, "Unexpected exception; report it as " +
                    "a bug in plug-in " + pluginId, e));
        }
    }

    /**
     * Adds the specified set of new folds to the document's annotation model
     * (and the <code>folds</code> instance variable).
     *
     * @param newPositions
     *        a set of Position instances representing new folds that
     *        should be added
     */
    private void addFolds(Set newPositions, boolean collapse)
    {
        IAnnotationModel annotations = getAnnotations();

        for (Iterator i = newPositions.iterator(); i.hasNext();)
        {
            Position p = (Position) i.next();
            Annotation fold = new ProjectionAnnotation(collapse);

            annotations.addAnnotation(fold, p);
            folds.add(fold);
        }
    }

    /**
     * @param sourceElems
     *        a collection of SourceElement instances; for some of these
     *        instances, fold positions will be determined and added to the
     *        positions set
     */
    private Set computeFoldPositions(Iterator sourceElems) throws BadLocationException
    {
        Set positions = new HashSet();
        IDocument doc = editor.getSourceFile().getDocument();

        while (sourceElems.hasNext())
        {
            IMultilineElement m = (IMultilineElement) sourceElems.next();
            if (m.getStartLine() == m.getEndLine()) continue;

            int offset = doc.getLineOffset(m.getStartLine());
            int length =
                doc.getLineOffset(m.getEndLine()) - offset +
                doc.getLineLength(m.getEndLine());

            Position p = new Position(offset, length);
            positions.add(p);
        }

        return positions;
    }

    /**
     * @return the annotation model used for adding/removing folds
     */
    private IAnnotationModel getAnnotations()
    {
        return (IAnnotationModel) editor.getAdapter(ProjectionAnnotationModel.class);
    }

    /**
     * Removes no longer required folds from the document's annotation model
     * (and the <code>folds</code> instance variable).
     * Removes positions of existing required folds from the argument set.
     *
     * @param positions
     *        the set of Position instances representing required positions
     *        of folds according to the current SourceFile of the editor;
     *        this set is updated by the method
     */
    private void removeFolds(Set positions)
    {
        IAnnotationModel annotations = getAnnotations();
        Set existingPositions = new HashSet();

        for (Iterator i = folds.iterator(); i.hasNext();)
        {
            Annotation a = (Annotation) i.next();
            Position p = annotations.getPosition(a);

            if (p != null && (p.isDeleted() || !positions.contains(p)))
            {
                annotations.removeAnnotation(a);
                i.remove();
            }
            else existingPositions.add(p);
        }
        positions.removeAll(existingPositions);
    }

    private boolean getPref(String name)
    {
        IPreferenceStore store = PerlEditorPlugin.getDefault().getPreferenceStore();
        return store.getBoolean(name);
    }
}
