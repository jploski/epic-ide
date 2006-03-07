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

import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.widgets.Shell;
import org.epic.debug.ui.RegExpBPSettingsDialog;

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
	private static final String IGNORE_CASE = "EPIC_IGNORE_CASE";
	private static final String MULTILINE = "EPIC_MULTILINE";
	//private IResource mResource;
	private RegExpBPSettingsDialog dialog;

	public PerlRegExpBreakpoint() {
		super();

	}

	public PerlRegExpBreakpoint(IResource resource, int lineNumber)
			throws DebugException, CoreException {
		super(resource, lineNumber);
		//mResource = resource;
		calculateRegExp();
	}

	public PerlRegExpBreakpoint(IResource resource, int lineNumber,
			int charStart, int charEnd, boolean add, Map attributes)
			throws DebugException, CoreException {
		super(resource, lineNumber, charStart, charEnd, add, attributes);
		//	mResource = resource;
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
			return (getMarker().getAttribute(fID));
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

	public void setIgnoreCase(boolean fBool) {
		setAttributeValue(IGNORE_CASE, new Boolean(fBool));
	}

	public boolean getIgnoreCase() {
		Boolean b = (Boolean) getAttribute(IGNORE_CASE);
		if (b != null)
			return b.booleanValue();
		return false;
	}

	public boolean getMultiLine() {
		Boolean b = (Boolean) getAttribute(MULTILINE);
		if (b != null)
			return b.booleanValue();
		return false;
	}

	public void setMultiLine(boolean fBool) {
		setAttributeValue(MULTILINE, new Boolean(fBool));
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

		return (fCurrentSourceLine != null && lineStored != null
				&& fCurrentSourceLine.equals(lineStored) && getRegExp() != null && getMatchText() != null);

	}
	void calculateRegExp() {

		String lineCurrent = getCurrentSourceLine();
		if (!isStoredDataValid(lineCurrent)) {
			setSourceLine(lineCurrent);
			if (extractRegExp(lineCurrent))
				return;

			Shell shell = PerlDebugPlugin.getActiveWorkbenchShell();
			dialog = new RegExpBPSettingsDialog(shell, this,
					"Could not extract Regular Expression...");
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
					+ "(.*)" + delim + "(.*)", 0, RESyntax.RE_SYNTAX_PERL5);
			match = findRegExp.getMatch(temp);
			if (match == null)
				return false;

			String var = line.substring(match.getStartIndex(1), match
					.getEndIndex(1));
			String text = line.substring(match.getStartIndex(2), match
					.getEndIndex(2));
			String mod = line.substring(match.getStartIndex(3), match
					.getEndIndex(3));
			if (var == null || text == null)
				return false;

			if (mod != null && mod.indexOf("i") >= 0)
				setIgnoreCase(true);
			else
				setIgnoreCase(false);

			if (mod != null && mod.indexOf("m") >= 0)
				setMultiLine(true);
			else
				setMultiLine(false);

			//System.out.println("\n" + var + ":" + text + "\n");
			setRegExp(text);
			setMatchText(var);
		} catch (REException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
			return false;
		}
		return (true);
	}

	private String getCurrentSourceLine() {

		IDocument doc = getDocument();

	

		String line = null;

		try {
			int length = doc.getLineLength(getLineNumber() - 1);
			int offset = doc.getLineOffset(getLineNumber() - 1);
			line = doc.get(offset, length);
		} catch (BadLocationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return null;
		}

		return line.trim();
	}

	public void updateIfRecognized() {

		String lineCurrent = getCurrentSourceLine();
		if (!isStoredDataValid(lineCurrent)) {
			setSourceLine(lineCurrent);
			extractRegExp(lineCurrent);
		}
	}
}