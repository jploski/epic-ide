/*
 * Created on 14.02.2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.epic.debug;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.part.FileEditorInput;

/**
 * @author ST
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class LocalFileEditorInput implements IFileEditorInput {

	/**
	 * 
	 */
	private IStorageEditorInput mStorageEditorInput;
	private IFile mIFile;
	
	
	static FileEditorInput getFileEditorInput(IPath fPath)
	{	
		IFile iFile=null;
		IFile[] files = PerlDebugPlugin.getWorkspace().getRoot().findFilesForLocation(fPath);

				if( files.length == 0)
				{
					IProject prj = PerlDebugPlugin.getWorkspace().getRoot().getProject("EPIC_LinkedPerlFiles");
					if (!prj.exists())
					{
						try{
						prj.create(null);
						}catch(Exception e){System.out.println(e);}
					}	
			
					try{
						prj.open(null);
					}catch(Exception e){System.out.println(e);}
		
			
					long time = System.currentTimeMillis();
					String name;
					name = Long.toString(time);
			
					IFolder link = prj.getFolder(name);
					while(link.exists())
					{
						name=name+"_";
						link = prj.getFolder(name);
					}
			
			
					try{
					link.createLink(fPath.removeLastSegments(1),IResource.NONE,null);
					}catch(Exception e){System.out.println(e);}
			
					files = PerlDebugPlugin.getWorkspace().getRoot().findFilesForLocation(fPath);
					if( files.length > 0)
						iFile = files[0];
				}
				else
				 iFile = files[0];
			
			return(new FileEditorInput(iFile));
	}
	
	
	public LocalFileEditorInput(IStorageEditorInput fInput) {
		super();
		IPath path=null;
		mStorageEditorInput=fInput;
		try{
		  path = mStorageEditorInput.getStorage().getFullPath();
		} catch (Exception e) {System.out.println(e);}
		
		IFile[] files = PerlDebugPlugin.getWorkspace().getRoot().findFilesForLocation(path);

		if( files.length == 0)
		{
			IProject prj = PerlDebugPlugin.getWorkspace().getRoot().getProject("EPIC_LinkedPerlFiles");
			if (!prj.exists())
			{
				try{
				prj.create(null);
				}catch(Exception e){System.out.println(e);}
			}	
			
			try{
				prj.open(null);
			}catch(Exception e){System.out.println(e);}
		
			
			long time = System.currentTimeMillis();
			String name;
			name = Long.toString(time);
			
			IFolder link = prj.getFolder(name);
			while(link.exists())
			{
				name=name+"_";
				link = prj.getFolder(name);
			}
			
			
			try{
			link.createLink(path.removeLastSegments(1),IResource.NONE,null);
			}catch(Exception e){System.out.println(e);}
			
			files = PerlDebugPlugin.getWorkspace().getRoot().findFilesForLocation(path);
			if( files.length > 0)
				mIFile = files[0];
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IFileEditorInput#getFile()
	 */
	public IFile getFile() {
		return mIFile;
	}

	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IEditorInput#getImageDescriptor()
	 */


	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	public Object getAdapter(Class adapter) {
		if( adapter == IFileEditorInput.class)
			return this;
			
		if( adapter == IStorageEditorInput.class)
			return mStorageEditorInput;
		
		return null;
	}

	

		/**
		 * @see IStorageEditorInput#getStorage()
		 */
		public IStorage getStorage() {
			try{
				return mStorageEditorInput.getStorage();
			}catch(Exception e){System.out.println(e);}
			return null;
		}

		/**
		 * @see IEditorInput#getImageDescriptor()
		 */
		public ImageDescriptor getImageDescriptor() {
//			return JavaUI.getSharedImages().getImageDescriptor(ISharedImages.IMG_OBJS_CUNIT);
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
		try{
	
			return object instanceof IStorageEditorInput &&
			 getStorage().equals(((IStorageEditorInput)object).getStorage());
		}catch(Exception e){System.out.println(e);}
		
		return(false);
		}

		/**
		 * @see java.lang.Object#hashCode()
		 */
		public int hashCode() {
			return getStorage().hashCode();
		}
	
		public boolean exists() {
			return ((IStorage)getStorage()).getFullPath().toFile().exists();
		}

	}

