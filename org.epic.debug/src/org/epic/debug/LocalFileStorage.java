package org.epic.debug;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.PlatformObject;

/**
 * @author ruehl
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */

public class LocalFileStorage extends PlatformObject implements IStorage {

	/**
	 * The file this storage refers to.
	 */
	private File fFile;

	/**
	 * Constructs and returns storage for the given file.
	 *
	 * @param file a local file
	 */
	public LocalFileStorage(File file){
		setFile(file);
	}

	public LocalFileStorage(String filename){
		setFile(new File(filename) );
	}

	/**
	 * @see IStorage#getContents()
	 */
	public InputStream getContents() throws CoreException {
			try {
    			return new FileInputStream(getFile());
    			} catch (IOException e) 
    				{ return null; }
	}

	/**
	 * @see IStorage#getFullPath
	 */
	public IPath getFullPath() {
		try {
			return new Path(getFile().getCanonicalPath());
		} catch (IOException e) {
			return null;
		}
	}
	/**
	 * @see IStorage#getName
	 */
	public String getName() {
		return getFile().getName();
	}
	/**
	 * @see IStorage#isReadOnly()
	 */
	public boolean isReadOnly() {
		return true;
	}

	/**
	 * Sets the file associated with this storage
	 *
	 * @param file a local file
	 */
	private void setFile(File file) {
		fFile = file;
	}

	/**
	 * Returns the file asscoiated with this storage
	 *
	 * @return file
	 */
	public File getFile() {
		return fFile;
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object object) {
		return object instanceof LocalFileStorage &&
			 getFile().equals(((LocalFileStorage)object).getFile());
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return getFile().hashCode();
	}
}

