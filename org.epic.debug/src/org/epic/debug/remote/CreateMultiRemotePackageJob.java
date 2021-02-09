package org.epic.debug.remote;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorRegistry;
import org.epic.core.PerlCore;
import org.epic.core.PerlProject;
import org.epic.debug.HelperScript;
import org.epic.debug.PerlDebugPlugin;
import org.epic.debug.util.CygwinPathMapper;
import org.epic.debug.util.IPathMapper;
import org.epic.debug.util.NullPathMapper;
import org.epic.debug.util.RemotePathMapper;
import org.epic.perleditor.PerlEditorPlugin;
import org.epic.perleditor.preferences.PreferenceConstants;

/**
 * Creates a perl script which can launch another perl script in the EPIC
 * Debugger. A multi Remote Debug Session must be launched in Eclipse in order
 * for this script to work Obviously, this does not include the perl script you
 * want to run, or its dependencies, they must already be deployed. The way a
 * multi remote session works, eclipse/epic will first look in the project, then
 * the entire workspace, for a matching script. If not found it will download
 * it. The emphasis is that this allows EPIC to remote-debug arbitrary scripts
 * without needing a perfect project match.
 */
public class CreateMultiRemotePackageJob extends Job {
	private final byte[] buffer;
	private final File scriptFile;
	private final ILaunch launch;
	private final MultiRemoteLaunchConfigurationDelegate launchDelegate;

	public CreateMultiRemotePackageJob(
			MultiRemoteLaunchConfigurationDelegate launchDelegate,
			ILaunch launch) throws CoreException {
		super("Create Remote Debug Package");

		this.launch = launch;
		this.launchDelegate = launchDelegate;
		this.buffer = new byte[1024];
		this.scriptFile = launchDelegate.getDebugPackageFile(launch);
	}

	protected IStatus run(IProgressMonitor monitor) {
		monitor.beginTask("Create Remote Debug Package", 1);
		PrintWriter w = null;
		try {
			w = new PrintWriter(scriptFile);
			w.append(getStartScript());
			return Status.OK_STATUS;
		} catch (Exception e) {
			scriptFile.delete();
			PerlDebugPlugin.log(e);
			return Status.CANCEL_STATUS;
		} finally {
			monitor.done();
			if (w != null)
				w.close();
		}
	}

	public String getStartScript() throws CoreException {
		String startDB = HelperScript.load("mstart_epicDB.pl");
		startDB = HelperScript.replace(
				startDB,
				"#SET MULTIADDRDEFAULT#",
				"my $multiAddrDefault = '"
						+ launchDelegate.getEpicDebuggerIP(launch)
						+ ":"
						+ launchDelegate.getEpicDebuggerPort(launch)
						+ (launchDelegate.getShouldRedirectIO(launch) ? "r"
								: "") + "';");
		startDB = HelperScript.replace(startDB, "#SET LAUNCHDEFAULT#", "");
		startDB = HelperScript.replace(startDB, "#DUMPVAREPIC TEXT#",
				HelperScript.load("dumpvar_epic.pm"));
		startDB = HelperScript.replace(startDB, "#AUTOFLUSHEPIC TEXT#",
				HelperScript.load("autoflush_epic.pm"));
		startDB = HelperScript.replace(startDB, "#EPICBREAKPOINTS TEXT#",
				HelperScript.load("epic_breakpoints.pm"));
		return startDB;
	}
}
