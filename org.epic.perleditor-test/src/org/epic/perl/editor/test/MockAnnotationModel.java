package org.epic.perl.editor.test;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelListener;

/**
 * Simple implementation of <code>IAnnotationModel</code> for use in unit-testing.
 */
public class MockAnnotationModel implements IAnnotationModel
{
    private Map<Annotation, Position> map = new HashMap<Annotation, Position>();

    /** @returns the number of annotations held in the model */
    public int size()
    {
        return map.size();
    }

    /*
     * @see org.eclipse.jface.text.source.IAnnotationModel#addAnnotation(org.eclipse.jface.text.source.Annotation, org.eclipse.jface.text.Position)
     */
    public void addAnnotation(Annotation annotation, Position position)
    {
        map.put(annotation, position);
    }

    /*
     * @see org.eclipse.jface.text.source.IAnnotationModel#addAnnotationModelListener(org.eclipse.jface.text.source.IAnnotationModelListener)
     */
    public void addAnnotationModelListener(IAnnotationModelListener listener)
    {
        throw new RuntimeException("unimplemented");
    }

    /*
     * @see org.eclipse.jface.text.source.IAnnotationModel#connect(org.eclipse.jface.text.IDocument)
     */
    public void connect(IDocument document)
    {
        throw new RuntimeException("unimplemented");
    }

    /*
     * @see org.eclipse.jface.text.source.IAnnotationModel#disconnect(org.eclipse.jface.text.IDocument)
     */
    public void disconnect(IDocument document)
    {
        throw new RuntimeException("unimplemented");
    }

    /*
     * @see org.eclipse.jface.text.source.IAnnotationModel#getAnnotationIterator()
     */
    public Iterator<Annotation> getAnnotationIterator()
    {
        throw new RuntimeException("unimplemented");
    }

    /*
     * @see org.eclipse.jface.text.source.IAnnotationModel#getPosition(org.eclipse.jface.text.source.Annotation)
     */
    public Position getPosition(Annotation annotation)
    {
        return map.get(annotation);
    }

    /*
     * @see org.eclipse.jface.text.source.IAnnotationModel#removeAnnotation(org.eclipse.jface.text.source.Annotation)
     */
    public void removeAnnotation(Annotation annotation)
    {
       map.remove(annotation);
    }

    /*
     * @see org.eclipse.jface.text.source.IAnnotationModel#removeAnnotationModelListener(org.eclipse.jface.text.source.IAnnotationModelListener)
     */
    public void removeAnnotationModelListener(IAnnotationModelListener listener)
    {
        throw new RuntimeException("unimplemented");
    }

}
