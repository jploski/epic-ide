package org.epic.debug;

/**
 * @author ruehl
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IStorageEditorInput;

public  class LocalFileStorageEditorInput extends PlatformObject implements IStorageEditorInput {

	/**
	 * Storage associated with this editor input
	 */
	private LocalFileStorage fStorage;

	/**
	 * Constructs an editor input on the given storage
	 */
	public LocalFileStorageEditorInput(LocalFileStorage storage) {
		fStorage = storage;
	}

	public LocalFileStorageEditorInput(String fileName) {
		fStorage = new LocalFileStorage(fileName);
	}

	/**
	 * @see IStorageEditorInput#getStorage()
	 */
	public IStorage getStorage() {
		return fStorage;
	}

	/**
	 * @see IEditorInput#getImageDescriptor()
	 */
	public ImageDescriptor getImageDescriptor() {
//		return JavaUI.getSharedImages().getImageDescriptor(ISharedImages.IMG_OBJS_CUNIT);
		return null;
	}

	/**
	 * @see IEditorInput#getName()
	 */
	public String getName() {
		return getStorage().getName();
	}

	/**
	 * @see IEditorInput#getPersistable()
	 */
	public IPersistableElement getPersistable() {
		return null;
	}

	/**
	 * @see IEditorInput#getToolTipText()
	 */
	public String getToolTipText() {
		return getStorage().getFullPath().toOSString();
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object object) {
		return object instanceof LocalFileStorageEditorInput &&
		 getStorage().equals(((LocalFileStorageEditorInput)object).getStorage());
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return getStorage().hashCode();
	}
	
	public boolean exists() {
		return ((LocalFileStorage)getStorage()).getFile().exists();
	}

}