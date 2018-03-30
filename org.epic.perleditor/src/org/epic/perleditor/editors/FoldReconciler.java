package org.epic.perleditor.editors;

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
import org.epic.core.model.SourceFile;
import org.epic.core.util.StatusFactory;
import org.epic.perleditor.PerlEditorPlugin;
import org.epic.perleditor.preferences.PreferenceConstants;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;


/**
 *Responsible for keeping folds in sync with a source file's text within a PerlEditor. This class
 *relies on {@link org.epic.core.model.SourceFile} to obtain positions of foldable
 *{@link org.epic.core.model.SourceElement}s.
 *
 *@author jploski
 */
public class FoldReconciler
{
    //~ Instance fields

    private final PerlEditor editor;

    private final Set<Tuple> folds; // of Annotation instances

    private boolean initialized = false;

    //~ Constructors

    /**
     * Creates a FoldReconciler for the given editor.
     */
    public FoldReconciler(PerlEditor editor)
    {
        this.editor = editor;
        this.folds = new HashSet<Tuple>();
    }

    //~ Methods

    /**
     * Updates folds based on the current state of the editor's SourceFile. An invocation results in
     * removing/adding zero or more fold annotations to the document's annotation model.
     *
     * @see PerlEditor#getSourceFile()
     * @see org.eclipse.jface.text.source.projection.ProjectionAnnotationModel
     */
    public void reconcile()
    {
        if (! isFoldingEnabled()) { return; }

        try
        {
            IAnnotationModel annotations = getAnnotations();
            if (annotations == null) { return; }

            Set<Tuple> tuples = computeFoldPositions();

            removeFolds(tuples);
            addFolds(tuples);

            /*
             * this should probably be handled via some kind of initialization that occurs in the
             * constructor to set up the initial folds. due to the way the editor calls this method
             * after the class has been instanciated, this achieves the desired behavior
             */
            initialized = true;
        }
        catch (BadLocationException e)
        {
            // this one should never occur
            String pluginId = PerlEditorPlugin.getPluginId();
            getLog().log(StatusFactory.createError(pluginId, "Unexpected exception; report it as "
                    + "a bug in plug-in " + pluginId, e));
        }
    }

    protected ILog getLog()
    {
        return PerlEditorPlugin.getDefault().getLog();
    }

    protected SourceFile getSourceFile()
    {
        return editor.getSourceFile();
    }

    /**
     * Adds the specified set of new folds to the document's annotation model (and the <code>
     * folds</code> instance variable).
     *
     * @param tuples <code>Tuple</code> instances representing new folds
     */
    private void addFolds(Set<Tuple> tuples)
    {
        for (Iterator<Tuple> iter = tuples.iterator(); iter.hasNext();)
        {
            Tuple t = iter.next();
            if (! folds.contains(t))
            {
                getAnnotations().addAnnotation(t.annotation, t.position);
                folds.add(t);
            }
        }
    }

    /**
     * Computes fold positions for <code>SourceElement</code>s
     */
    private Set<Tuple> computeFoldPositions() throws BadLocationException
    {
        HashSet<Tuple> tuples = new HashSet<Tuple>();

        computeFoldPositions(tuples, getSourceFile().getPODs(),
            initialized ? false : isFoldPerldoc());

        computeFoldPositions(tuples, getSourceFile().getSubs(),
            initialized ? false : isFoldSubroutines());

        // TODO: add new fold position computations here

        return tuples;
    }

    /**
     * Computes fold elements for a given collection of <code>SourceElement</code>s
     *
     * @param tuples object <code>Tuple</code>s representing folds will be added to
     * @param elements iterator for a collection of <code>SourceElement</code>s
     * @param collapse true if fold is initially collapsed, false otherwise
     */
    private void computeFoldPositions(Set<Tuple> tuples, Iterator<? extends IMultilineElement> elements, boolean collapse)
        throws BadLocationException
    {
        IDocument doc = getSourceFile().getDocument();

        while (elements.hasNext())
        {
            IMultilineElement e = elements.next();

            if (e.getStartLine() == e.getEndLine())
            {
                continue;
            }

            int offset = doc.getLineOffset(e.getStartLine());
            int length =
                doc.getLineOffset(e.getEndLine()) - offset + doc.getLineLength(e.getEndLine());

            /*
             * store the position and annotation - the position is needed to create the fold, while
             * the annotation is needed to remove it
             */
            Tuple t = new Tuple(new Position(offset, length), new ProjectionAnnotation(collapse));
            tuples.add(t);
        }
    }

    /**
     * @return the annotation model used for adding/removing folds
     */
    protected IAnnotationModel getAnnotations()
    {
        return (IAnnotationModel) editor.getAdapter(ProjectionAnnotationModel.class);
    }

    protected boolean getPreference(String name)
    {
        IPreferenceStore store = PerlEditorPlugin.getDefault().getPreferenceStore();
        return store.getBoolean(name);
    }

    private boolean isFoldingEnabled()
    {
        return getPreference(PreferenceConstants.SOURCE_FOLDING);
    }

    private boolean isFoldPerldoc()
    {
        return getPreference(PreferenceConstants.PERLDOC_FOLDING);
    }

    private boolean isFoldSubroutines()
    {
        return getPreference(PreferenceConstants.SUBROUTINE_FOLDING);
    }

    /**
     * Removes no longer required folds from the document's annotation model (and the <code>
     * folds</code> instance variable). Removes positions of existing required folds from the
     * argument set.
     *
     * @param toRemove the set of Tuple instances representing required positions of folds according
     *                 to the current SourceFile of the editor; this set is updated by the method
     */
    private void removeFolds(Set<Tuple> toRemove)
    {
        for (Iterator<Tuple> iter = folds.iterator(); iter.hasNext();)
        {
            Tuple t = iter.next();
            Position p = getAnnotations().getPosition(t.annotation);

            if ((p != null) && (p.isDeleted() || ! toRemove.contains(t)))
            {
                getAnnotations().removeAnnotation(t.annotation);
                iter.remove();
            }
            else
            {
                // filter out any tuple instances that already exist
                toRemove.remove(t);
            }
        }
    }

    //~ Inner Classes

    /**
     *Fold data container
     */
    private class Tuple
    {
        Annotation annotation;
        Position position;

        Tuple(Position p, Annotation a)
        {
            this.position = p;
            this.annotation = a;
        }

        /*
         * @see java.lang.Object#equals(java.lang.Object)
         */
        public boolean equals(Object obj)
        {
            if (this == obj) { return true; }

            if (obj == null) { return false; }

            if (getClass() != obj.getClass()) { return false; }

            final Tuple other = (Tuple) obj;
            if (position == null)
            {
                if (other.position != null) { return false; }
            }
            else if (! position.equals(other.position))
            {
                return false;
            }

            return true;
        }

        /*
         * @see java.lang.Object#hashCode()
         */
        public int hashCode()
        {
            final int PRIME = 31;
            int result = 1;
            result = (PRIME * result) + ((position == null) ? 0 : position.hashCode());
            return result;
        }
    }

}
