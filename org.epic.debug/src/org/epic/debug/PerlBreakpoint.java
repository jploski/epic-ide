/*
 * Created on 12.04.2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */

package org.epic.debug;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.model.Breakpoint;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.part.FileEditorInput;
import org.epic.core.util.FileUtilities;





/**
 * @author ruehl
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public abstract class PerlBreakpoint extends Breakpoint {

	/**
	 * 
	 */
	//IPath mResourcePath;
	Set  mDebuger;
	Set  mInstallations;

	public final static String INVALID_POS = "PerlDebug_INVALID_POS" ;
	public final static String IS_INSTALLED = "PerlDebug_IS_INSTALLED" ;
	//public final static String RESOURCE_PATH = "PerlDebug_BreakPointResourcePath" ;
	
	boolean mIsNoValidBreakpointPosition;
	
//	public PerlBreakpoint(IResource resource) {
//		super();
//		mResourcePath = resource.getRawLocation();
//		mDebuger = new HashSet();
//		mInstallations = new HashSet();
//		mIsNoValidBreakpointPosition = false;
//	}

	public PerlBreakpoint() {
			super();
			mDebuger = new HashSet();
			mInstallations = new HashSet();
			mIsNoValidBreakpointPosition = false;
			//mResourcePath = null;
		}
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IBreakpoint#getModelIdentifier()
	 */
	public String getModelIdentifier() 
	{
		return PerlDebugPlugin.getUniqueIdentifier();
	}
	
	public IPath getResourcePath()
	{
		return getMarker().getResource().getLocation();
	}

	public boolean isInstalled()
	{
		return( mInstallations.size() > 0);
		//return( getMarker().getAttribute(IS_INSTALLED,false) );
	}
	
	public void setIsNoValidBreakpointPosition( boolean fVal)
	{
		try{
				getMarker().setAttribute(INVALID_POS,fVal);
		}catch ( Exception e){PerlDebugPlugin.log(e);}	
		
		//DebugPlugin.getDefault().getBreakpointManager().fireBreakpointChanged(this);
		
	}
	public boolean isNoValidBreakpointPosition()
	{
		return getMarker().getAttribute(INVALID_POS,false);	
	}
	public void addDebuger(PerlDB fDB)
	{
		mDebuger.add(fDB);	
	}
	
	public void removeDebuger(PerlDB fDb)
	{
		mDebuger.remove(fDb);
		removeInstallation(fDb);	
	}
	
	
	
	public Object[] getDebuggers()
	{
		return(  mDebuger.toArray());
	}
	
	public void addInstallation(PerlDB fDb)
	{
		boolean erg;
		
		erg  = mInstallations.add(fDb);
		if( erg && (mInstallations.size() == 1) )
		{	
			try
			{
			//	getMarker().setAttribute(IS_INSTALLED,true);
			}catch ( Exception e){PerlDebugPlugin.log(e);}
		}
			
	}
	
	public void removeInstallation(PerlDB fDb)
	{
			boolean erg;
		
			erg  = mInstallations.remove(fDb);
			if( erg && (mInstallations.size() == 0) )
			{	
				try
				{
			//		getMarker().setAttribute(IS_INSTALLED,false);
				}catch ( Exception e){PerlDebugPlugin.log(e);}
			}
		
	}
	
	
	/**
			 * @see IBreakpoint#setMarker(IMarker)
			 */
			public void setMarker(IMarker marker) throws CoreException {
				marker.setAttribute(IS_INSTALLED,false);
				super.setMarker(marker);
//				if( mResourcePath == null )
//				{
//					String pathString = (String)marker.getAttribute(RESOURCE_PATH);
//					if (pathString != null)
//						mResourcePath =  new Path( marker.getAttribute(RESOURCE_PATH,pathString));
//				}
				//configureAtStartup();
				
			}
			
//	void addBreakPointAttributes(Map attributes)
//	{
//		attributes.put(RESOURCE_PATH,mResourcePath.toString());
//
//	}
		
			
			
	public		IDocument getDocument()
			{
				IDocument doc = null;
				IWorkbench bench = PerlDebugPlugin.getDefault().getWorkbench();
				if (bench != null) {
					IWorkbenchWindow window = bench.getActiveWorkbenchWindow();
					if (window != null) {
						IWorkbenchPage page = window.getActivePage();
						if (page != null) {
							FileEditorInput input = new FileEditorInput(
									(IFile) getMarker().getResource());
							TextEditor editor = (TextEditor) page.findEditor(input);
							doc = editor.getDocumentProvider().getDocument(input);
						}
					}
				}
				if( doc == null ){

				
						StringBuffer sourceCode = new StringBuffer();
				
						int BUF_SIZE = 1024;
				
						// Get the file content
						char[] buf = new char[BUF_SIZE];
						File inputFile = new File(getMarker().getResource().getLocation()
								.toString());
						BufferedReader in;
						try {
							in = new BufferedReader(new FileReader(inputFile));
				
							int read = 0;
							while ((read = in.read(buf)) > 0) {
								sourceCode.append(buf, 0, read);
							}
							in.close();
						} catch (FileNotFoundException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							return null;
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							return null;
						}
						String line = null;
						 doc = new Document(sourceCode.toString());
						}	
			
			return doc;
			}
	
	public		IEditorInput getEditorInput()
	{
		
		IWorkbench bench = PerlDebugPlugin.getDefault().getWorkbench();
		if (bench != null) {
			IWorkbenchWindow window = bench.getActiveWorkbenchWindow();
			if (window != null) {
				IWorkbenchPage page = window.getActivePage();
				if (page != null) {
					FileEditorInput input = new FileEditorInput(
							(IFile) getMarker().getResource());
					return input;
				}
			}
		}
		
	return	FileUtilities.getFileEditorInput(getMarker().getResource().getLocation());
		
		
		
		
			
	}

}
