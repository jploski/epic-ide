package org.epic.perleditor.views;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;

import org.epic.perleditor.views.model.Model;
import org.epic.perleditor.views.model.SourceElement;

public class SourceElementContentProvider implements ITreeContentProvider {
	private static Object[] EMPTY_ARRAY = new Object[0];
	protected TreeViewer viewer;
	
	/*
	 * @see IContentProvider#dispose()
	 */
	public void dispose() {}

	
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		
	}

	
	

	/*
	 * @see ITreeContentProvider#getChildren(Object)
	 */
	public Object[] getChildren(Object parentElement) {
		if(parentElement instanceof SourceElement) {
			SourceElement node = (SourceElement)parentElement;
			return concat(node.getNodes().toArray(), 
				node.getModules().toArray(), node.getSubroutines().toArray());
		}
		return EMPTY_ARRAY;
	}
	
	protected Object[] concat(Object[] object, Object[] more, Object[] more2) {
		Object[] both = new Object[object.length + more.length + more2.length];
		System.arraycopy(object, 0, both, 0, object.length);
		System.arraycopy(more, 0, both, object.length, more.length);
		System.arraycopy(more2, 0, both, object.length + more.length, more2.length);		
		return both;
	}

	/*
	 * @see ITreeContentProvider#getParent(Object)
	 */
	public Object getParent(Object element) {
		if(element instanceof Model) {
			return ((Model)element).getParent();
		}
		return null;
	}

	/*
	 * @see ITreeContentProvider#hasChildren(Object)
	 */
	public boolean hasChildren(Object element) {
		return getChildren(element).length > 0;
	}

	/*
	 * @see IStructuredContentProvider#getElements(Object)
	 */
	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

	

}
