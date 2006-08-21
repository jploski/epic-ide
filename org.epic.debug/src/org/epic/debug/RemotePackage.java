/*
 * Created on 24.12.2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.epic.debug;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.IEditorDescriptor;
import org.epic.core.util.XMLUtilities;
import org.epic.debug.util.PathMapping;

/**
 * @author ST
 * 
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class RemotePackage {

	private static ZipOutputStream mOut;
	private static byte[] mBuf = new byte[1024];
	private static HashMap mLinkedFoldersMap = new HashMap();
	private static RemoteTarget mTarget;

	static public void create(RemoteTarget fTarget, IProgressMonitor fMon) {
		mTarget = fTarget;
		mLinkedFoldersMap.clear();
		count(fTarget);
		fMon.beginTask("Create Debug Package", count(fTarget));

		String outFilename = null;
		try {
			outFilename = fTarget.mLaunch
					.getLaunchConfiguration()
					.getAttribute(
							PerlLaunchConfigurationConstants.ATTR_REMOTE_DEBUG_PACKAGE_PATH,
							"");
		} catch (CoreException e3) {
			// TODO Auto-generated catch block
			e3.printStackTrace();
		}

		try {
			mOut = new ZipOutputStream(new FileOutputStream(outFilename));
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		String projectName = fTarget.getProjectName();
		if (projectName == null || projectName.length() == 0) {
			return;
		}

		IWorkspaceRoot workspaceRoot = PerlDebugPlugin.getWorkspace().getRoot();
		IProject project = workspaceRoot.getProject(projectName);
		IResourceVisitor visitor = new PerlProjectVisitor(fMon);

		try {
			project.accept(visitor);
		} catch (CoreException e) {
			e.printStackTrace();
		}
		try {
			InputStream in =
                PerlDebugPlugin.getDefault().getBundle().getEntry(
                    "dumpvar_epic.pm").openStream();

			// Add ZIP entry to output stream.
			mOut.putNextEntry(new ZipEntry(fTarget.getStartUpFileDirPath()
					+ "/" + "dumpvar_epic.pm"));

			// Transfer bytes from the file to the ZIP file
			int len;
			while ((len = in.read(mBuf)) > 0) {
				mOut.write(mBuf, 0, len);
			}
			in.close();

			String inc = createIncPath(fTarget);
			if (inc == null)
				inc = " ";

			mOut.putNextEntry(new ZipEntry("start_epicDB.pl"));
			String startDB = "$ENV{PERLDB_OPTS}=\"RemotePort="
					+ fTarget.getIP() + ":" + fTarget.getPort()
					+ " DumpReused ReadLine=0\";\n" + "if( ! -d \""
					+ fTarget.getRemoteDest()
					+ "\" ) {die(\"Target Directory is invalid !!!\")};\n"
					+ "chdir(\"" + fTarget.getRemoteDest() + "/"
					+ fTarget.getStartUpFileDirPath() + "\");"
					+ "\nsystem(\"perl -d " + inc + " ./"
					+ fTarget.getStartupFile() + "\");";
			mOut.write(startDB.getBytes());
			mOut.close();
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		fMon.done();
	}

	static String createIncPath(RemoteTarget fTarget) {
		String path;
		String erg = null;
		String tmp;
		String error = null;

		String projectDir = fTarget.getProjectDir().toString();
		XMLUtilities xml = new XMLUtilities();

		String[] includes = xml.getIncludeEntries(fTarget.getProject(), true);
		for (int i = 0; i < includes.length; ++i) {
			path = new Path(includes[i]).toString();
			if (!path.startsWith(projectDir)) {
				tmp = (String) mLinkedFoldersMap.get(path);
				if (tmp != null) {
					if (erg == null)
						erg = " -I \\\"" + fTarget.getRemoteDest() + "/" + tmp
								+ "\\\"";
					else
						erg += " -I \\\"" + fTarget.getRemoteDest() + "/" + tmp
								+ "\\\"";
				} else if (error == null) {
					error = path + "\n";
				} else
					error += path + "\n";
			} else {
				if (erg == null)
					erg = " -I \\\""
							+ path.replaceFirst(projectDir,
									fTarget.getRemoteDest())+ "\\\"";
				else
					erg += " -I \""
							+ path.replaceFirst(projectDir,
									fTarget.getRemoteDest()) + "\\\"";
			}

		}

		if (error != null) {
			PerlDebugPlugin
					.errorDialog("Possible Problems for Remote Debugging detected:\n"
							+ "The following include pathes might not be handeld properly:\n\n"
							+ error
							+ "\nPleases either\n"
							+ "a) make shure that a copy of each module within these directories is available on your remote machine via the default include path on your target machine"
							+ "\nb) or create a link to each of these directories within your project ");
		}

		return (erg);
	}

	static public int count(RemoteTarget fTarget) {

		String projectName = fTarget.getProjectName();
		if (projectName == null || projectName.length() == 0)
			return 0;

		IWorkspaceRoot workspaceRoot = PerlDebugPlugin.getWorkspace().getRoot();
		IProject project = workspaceRoot.getProject(projectName);
		PerlProjectVisitorCount visitor = new PerlProjectVisitorCount();

		try {
			project.accept(visitor);
		} catch (CoreException e) {
			e.printStackTrace();
		}

		return (visitor.getCount());
	}
	static class PerlProjectVisitor implements IResourceVisitor {
		private static final String PERL_EDITOR_ID = "org.epic.perleditor.editors.PerlEditor";
		private static final String EMB_PERL_FILE_EXTENSION = "epl";
		IProgressMonitor mMon;
		int mCount;

		private List fileList = new ArrayList();
		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.core.resources.IResourceVisitor#visit(org.eclipse.core.resources.IResource)
		 */
		public PerlProjectVisitor(IProgressMonitor fMon) {
			mMon = fMon;
			mCount = 0;
		}

		public boolean visit(IResource resource) throws CoreException {

			IEditorDescriptor defaultEditorDescriptor = PerlDebugPlugin
					.getDefault().getWorkbench().getEditorRegistry()
					.getDefaultEditor(resource.getFullPath().toString());

			if (resource.isLinked() && resource instanceof IFolder) {
				String in = mTarget.getRemoteDest() + "/"
						+ resource.getProjectRelativePath().toString();
				String out = resource.getLocation().toString();
				mTarget.getPathMapper().add(new PathMapping(in, out));
				mLinkedFoldersMap.put(resource.getLocation().toString(),
						resource.getProjectRelativePath().toString());
			}

			if (resource instanceof IFile) {
				String pathname = resource.getFullPath().removeFirstSegments(1)
						.toString();

				FileInputStream in;
				try {
					in = new FileInputStream(resource.getLocation().toString());

					// Add ZIP entry to output stream.
					mOut.putNextEntry(new ZipEntry(pathname));

					// Transfer bytes from the file to the ZIP file
					int len;
					while ((len = in.read(mBuf)) > 0) {
						mOut.write(mBuf, 0, len);
					}
					in.close();
					mCount++;
					mMon.worked(mCount);

				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			return true;
		}
	}

	static class PerlProjectVisitorCount implements IResourceVisitor {
		private static final String PERL_EDITOR_ID = "org.epic.perleditor.editors.PerlEditor";
		private static final String EMB_PERL_FILE_EXTENSION = "epl";
		private int mCount;
		public PerlProjectVisitorCount() {
			mCount = 0;
		}
		public int getCount() {
			return mCount;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.core.resources.IResourceVisitor#visit(org.eclipse.core.resources.IResource)
		 */
		public boolean visit(IResource resource) throws CoreException {
			IEditorDescriptor defaultEditorDescriptor = PerlDebugPlugin
					.getDefault().getWorkbench().getEditorRegistry()
					.getDefaultEditor(resource.getFullPath().toString());

			if (defaultEditorDescriptor == null) {
				return true;
			}

			if (defaultEditorDescriptor.getId().equals(PERL_EDITOR_ID)
					&& !resource.getFileExtension().equals(
							EMB_PERL_FILE_EXTENSION)) {
				mCount++;
			}

			return true;
		}

	}
}

