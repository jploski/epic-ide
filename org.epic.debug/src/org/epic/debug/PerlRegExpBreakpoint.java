/*
 * Created on 11.04.2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.epic.debug;

import gnu.regexp.RE;
import gnu.regexp.REException;
import gnu.regexp.REMatch;
import gnu.regexp.RESyntax;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.epic.regexp.views.RegExpView;

/**
 * @author ruehl
 * 
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */

public class PerlRegExpBreakpoint extends PerlLineBreakpoint {

	private static final String Perl_REGEXP_BREAKPOINT = "org.epic.debug.perlRegExpBreakpointMarker"; //$NON-NLS-1$
	private static final String SOURCE_LINE = "EPIC_SOURCE_LINE";
	private static final String REG_EXP = "EPIC_REG_EXP";
	private static final String MATCH_TEXT = "EPIC_MATCH_TEXT";
	private IResource mResource;
	private RegExpBPSettingsDalog dialog;

	public PerlRegExpBreakpoint() {
		super();

	}

	public PerlRegExpBreakpoint(IResource resource, int lineNumber)
			throws DebugException, CoreException {
		super(resource, lineNumber);
		mResource = resource;
		calculateRegExp();
	}

	public PerlRegExpBreakpoint(IResource resource, int lineNumber,
			int charStart, int charEnd, boolean add, Map attributes)
			throws DebugException, CoreException {
		super(resource, lineNumber, charStart, charEnd, add, attributes);
		mResource = resource;
		calculateRegExp();
	}

	String getMarkerID() {
		return (Perl_REGEXP_BREAKPOINT);
	}

	public String getSourceLine() {
		return (String) getAttribute(SOURCE_LINE);
	}

	private Object getAttribute(String fID) {
		try {
			return (String) (getMarker().getAttribute(fID));
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public String getRegExp() {
		return (String) getAttribute(REG_EXP);
	}

	public String getMatchText() {
		return (String) getAttribute(MATCH_TEXT);
	}

	private void setAttributeValue(String fID, Object fValue) {
		try {
			getMarker().setAttribute(fID, fValue);
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void setMatchText(String fText) {
		setAttributeValue(MATCH_TEXT, fText);
	}

	public void setRegExp(String fText) {
		setAttributeValue(REG_EXP, fText);
	}

	public void setSourceLine(String fText) {
		setAttributeValue(SOURCE_LINE, fText);
	}

	public boolean isStoredDataValid() {
		return isStoredDataValid(getCurrentSourceLine());

	}

	public boolean isStoredDataValid(String fCurrentSourceLine) {
		String lineStored = getSourceLine();

		return (fCurrentSourceLine != null && lineStored != null && fCurrentSourceLine
				.equals(lineStored) && getRegExp()!= null && getMatchText() != null );

	}
void calculateRegExp() {

		
	String lineCurrent = getCurrentSourceLine();
		if( !isStoredDataValid(lineCurrent))
		{	
			extractRegExp(lineCurrent);
			setSourceLine(lineCurrent);
			Shell shell = PerlDebugPlugin.getActiveWorkbenchShell();
			dialog = new RegExpBPSettingsDalog(shell, this,"Could not extract Regular Expression...");
			if (shell != null) {
				shell.getDisplay().syncExec(new Runnable() {
					public void run() {
					dialog.open();
					}

				});

			
		}
		}
		

	}
	private boolean extractRegExp(String line) {

		try {
			String delim;

			RE findDelim = new RE("[$%@].+[\\s]*=~[\\s]*[m]?(.)", 0,
					RESyntax.RE_SYNTAX_PERL5);

			REMatch match = findDelim.getMatch(line);
			if (match == null)
				return false;
			delim = match.toString(1);
			if (delim == null)
				return false;
			String temp = line;
			temp.replaceAll("\\" + delim, "xx");
			RE findRegExp = new RE("([$%@][^\\s]+)[\\s]*=~[\\s]*[m]?" + delim
					+ "(.*)" + delim, 0, RESyntax.RE_SYNTAX_PERL5);
			match = findRegExp.getMatch(temp);
			String var = line.substring(match.getStartIndex(1), match
					.getEndIndex(1));
			String text = line.substring(match.getStartIndex(2), match
					.getEndIndex(2));

			//System.out.println("\n" + var + ":" + text + "\n");
			setRegExp(text);
			setMatchText(var);
		} catch (REException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		return (true);
	}

	private String getCurrentSourceLine() {

		StringBuffer sourceCode = new StringBuffer();

		int BUF_SIZE = 1024;

		//	Get the file content
		char[] buf = new char[BUF_SIZE];
		File inputFile = new File(mResource.getLocation().toString());
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
		Document doc = new Document(sourceCode.toString());
		try {
			int length = doc.getLineLength(getLineNumber()-1);
			int offset = doc.getLineOffset(getLineNumber()-1);
			line = doc.get(offset, length);
		} catch (BadLocationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return null;
		}

		return line.trim();
	}

}