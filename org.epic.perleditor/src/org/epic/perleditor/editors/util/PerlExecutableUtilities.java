/*
 * Created on 18.04.2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.epic.perleditor.editors.util;

import java.util.List;
import java.util.ArrayList;

import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.editors.text.TextEditor;

import gnu.regexp.RE;

import org.epic.perleditor.PerlEditorPlugin;
import org.epic.core.util.XMLUtilities;

/**
 * @author luelljoc
 *
 */
public class PerlExecutableUtilities {

	public static List getPerlExecutableCommandLine(TextEditor textEditor) {
		// Get perl executable and extra parameters
		String preExe =
			PerlEditorPlugin.getDefault().getExecutablePreference().trim();

		int startParams = 0;
		String perlExe = "";
		String perlParams = "";

		if (preExe.startsWith("\"")) {
			startParams = preExe.indexOf("\"", 1);
			if (startParams != -1) {
				perlExe = preExe.substring(1, startParams);
			} else {
				perlExe = preExe;
			}
		} else {
			startParams = preExe.indexOf(" ", 0);
			if (startParams != -1) {
				perlExe = preExe.substring(0, startParams);
			} else {
				perlExe = preExe;
			}

		}

		if (startParams != -1 && (startParams + 1 < preExe.length())) {
			perlParams = preExe.substring(startParams + 1);
		}

		List cmdList = new ArrayList();
		cmdList.add(perlExe.trim());

		if (perlParams.length() > 0) {
			cmdList.add(perlParams.trim());
		}

		String currentPath =
			((IFileEditorInput) textEditor.getEditorInput())
				.getFile()
				.getLocation()
				.makeAbsolute()
				.removeLastSegments(1)
				.toString();
		currentPath = preparePath(currentPath);

		cmdList.add("-I");
		cmdList.add(currentPath);

		// Add other project include paths
		XMLUtilities xmlUtil = new XMLUtilities();
		String[] includes =
			xmlUtil.getIncludeEntries(
				((IFileEditorInput) textEditor.getEditorInput())
					.getFile()
					.getProject());
		for(int i=0; i<includes.length; i++) {
			String path = preparePath(includes[i]);
			cmdList.add("-I");
			cmdList.add(path);			
		}

		return cmdList;
	}

	private static String preparePath(String path) {
		String interpreterType =
			PerlEditorPlugin.getDefault().getPreferenceStore().getString(
				PerlEditorPlugin.INTERPRETER_TYPE_PREFERENCE);

		// Do Cygwin conversion
		if (interpreterType.equals(PerlEditorPlugin.INTERPRETER_TYPE_CYGWIN)) {
			try {
				path = path.replace('\\', '/');
				path = path.toLowerCase();

				RE re = new RE("^([a-z]):(.*)$");
				if (re.getAllMatches(path).length > 0) {
					path = re.substitute(path, "/cygdrive/$1$2");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		return path;
	}

}
