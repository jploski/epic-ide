package org.epic.perleditor.editors.perl;

import java.util.*;
import java.io.*;

import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationPresenter;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.editors.text.TextEditor;

import org.eclipse.jface.text.IDocument;

import gnu.regexp.RE;
import gnu.regexp.REMatch;

import org.epic.perleditor.editors.PerlImages;
import org.epic.perleditor.editors.util.PerlExecutableUtilities;

/**
 * Perl completion processor.
 */
public class PerlCompletionProcessor implements IContentAssistProcessor {

	/**
	 * Simple content assist tip closer. The tip is valid in a range
	 * of 5 characters around its popup location.
	 */
	protected static class Validator
		implements IContextInformationValidator, IContextInformationPresenter {

		protected int fInstallOffset;

		/*
		 * @see IContextInformationValidator#isContextInformationValid(int)
		 */
		public boolean isContextInformationValid(int offset) {
			return Math.abs(fInstallOffset - offset) < 5;
		}

		/*
		 * @see IContextInformationValidator#install(IContextInformation, ITextViewer, int)
		 */
		public void install(
			IContextInformation info,
			ITextViewer viewer,
			int offset) {
			fInstallOffset = offset;

		}

		/*
		 * @see org.eclipse.jface.text.contentassist.IContextInformationPresenter#updatePresentation(int, TextPresentation)
		 */
		public boolean updatePresentation(
			int documentPosition,
			TextPresentation presentation) {
			return false;
		}
	};

	protected IContextInformationValidator fValidator = new Validator();
	private TextEditor fTextEditor = null;

	/*
	 * Constructor
	 */
	public PerlCompletionProcessor(TextEditor textEditor) {
		fTextEditor = textEditor;
	}
	/* 
	 * Method declared on IContentAssistProcessor
	 */
	public ICompletionProposal[] computeCompletionProposals(
		ITextViewer viewer,
		int documentOffset) {

		String className = getClassName(viewer, documentOffset);

		if (className == null) {
			return null;
		}

		List proposals = getProposalsForClassname(viewer, className);

		ICompletionProposal[] result =
			new ICompletionProposal[proposals.size()];

		for (int i = 0; i < proposals.size(); i++) {
			String proposal = (String) proposals.get(i);

			Display display = ((TextViewer) viewer).getControl().getDisplay();

			if (proposal.endsWith("()")) {
				Image image =
					new Image(
						display,
						PerlImages.ICON_SUBROUTINE.getImageData());
				result[i] =
					new CompletionProposal(
						proposal,
						documentOffset,
						0,
						proposal.length() - 1,
						image,
						null,
						null,
						null);
			} else {
				result[i] =
					new CompletionProposal(
						proposal,
						documentOffset,
						0,
						proposal.length());
			}
		}
		return result;
	}

	/* 
	 * Method declared on IContentAssistProcessor
	 */
	public IContextInformation[] computeContextInformation(
		ITextViewer viewer,
		int documentOffset) {
		IContextInformation[] result = new IContextInformation[5];
		/*
		for (int i= 0; i < result.length; i++)
			result[i]= new ContextInformation(
				MessageFormat.format(PerlEditorMessages.getString("CompletionProcessor.ContextInfo.display.pattern"), new Object[] { new Integer(i), new Integer(documentOffset) }),  //$NON-NLS-1$
				MessageFormat.format(PerlEditorMessages.getString("CompletionProcessor.ContextInfo.value.pattern"), new Object[] { new Integer(i), new Integer(documentOffset - 5), new Integer(documentOffset + 5)})); //$NON-NLS-1$
		*/
		return result;
	}

	/* 
	 * Method declared on IContentAssistProcessor
	 */
	public char[] getCompletionProposalAutoActivationCharacters() {
		return new char[] { '>', ':' };
	}

	/* 
	 * Method declared on IContentAssistProcessor
	 */
	public char[] getContextInformationAutoActivationCharacters() {
		//return new char[] { '#' };
		return null;
	}

	/* 
	 * Method declared on IContentAssistProcessor
	 */
	public IContextInformationValidator getContextInformationValidator() {
		return fValidator;
	}

	/* 
	 * Method declared on IContentAssistProcessor
	 */
	public String getErrorMessage() {
		return null;
	}

	private String getClassName(ITextViewer viewer, int documentOffset) {
		String objName = null;
		String className = null;

		try {
			IDocument document = viewer.getDocument();

			String text = document.get(0, documentOffset);
			if (!text.endsWith("->") && !text.endsWith("::")) {
				return null;
			}

			// Get the object name
			int line = document.getLineOfOffset(documentOffset);
			String lineContent = text.substring(document.getLineOffset(line));

			// Lines can end with "->" of "::"
			RE refType = new RE("\\$[a-zA-Z]+(->|::)$");

			if (refType.getAllMatches(lineContent).length > 0) {
				RE re;

				re = new RE("\\s*=\\s*(.*::.*)->$");
				REMatch[] found = re.getAllMatches(text);

				if (found.length > 0) {
					className = found[0].toString(1);
				} else {
					objName =
						lineContent.substring(lineContent.lastIndexOf("$"));

					if (objName.indexOf("->") != -1) {
						objName = objName.substring(0, objName.indexOf("->"));
					} else {
						objName = objName.substring(0, objName.indexOf("::"));
					}

					// **** Get the classname ***
					//re = new RE("\\" + objName + "\\s*=\\s*(.*?)(->|::)[a-zA-Z]+\\(");
					re =
						new RE(
							"\\"
								+ objName
								+ "\\s*=\\s*([a-zA-Z:->]+)(->|::|;)");
					REMatch[] matches = re.getAllMatches(text);

					if (matches.length == 0) {
						return null;
					}

					// Get only last match
					className = matches[matches.length - 1].toString(1);
				}

			} else {

				RE re = new RE("([a-z|A-Z]+::.+?)(::|->)$");
				REMatch[] matches = re.getAllMatches(lineContent);
				if (matches.length > 0) {
					className = matches[0].toString(1);
				} else {
					return null;
				}

			}
		} catch (Exception ex) {
			return null;
		}

		return className;
	}

	private List getProposalsForClassname(
		ITextViewer viewer,
		String className) {
		List result = new ArrayList();

		String perlCode =
			"use "
				+ className
				+ ";\n\n"
				+ "foreach $name (sort keys %"
				+ className
				+ "::) {\n"
				+ " next if($name !~ /[a-z]/ || $name =~ /^_/);\n"
				+ "   if(defined &{\""
				+ className
				+ "::$name\"}) {\n"
				+ "       print \"$name()\\n\";\n"
				+ "   }\n"
				+ "   else {\n"
				+ "       #print \"$name\\n\";\n"
				+ "   }\n"
				+ "}\n";

		String tmpFileName = null;

		try {

			tmpFileName =
				System.getProperty("java.io.tmpdir")
					+ File.separator
					+ "epic-proposal"
					+ new Date().getTime();

			// Write output file
			FileOutputStream fos = new FileOutputStream(new File(tmpFileName));
			fos.write(perlCode.getBytes());
			fos.flush();
			fos.close();

			//			Construct command line parameters
			List cmdList =
				PerlExecutableUtilities.getPerlExecutableCommandLine(
					fTextEditor);
			cmdList.add(tmpFileName);

			String[] cmdParams =
				(String[]) cmdList.toArray(new String[cmdList.size()]);

			Process proc = Runtime.getRuntime().exec(cmdParams);

			InputStream in = proc.getInputStream();

			byte[] buffer = new byte[1];
			StringBuffer content = new StringBuffer();

			int read;
			while ((read = in.read(buffer)) > 0) {
				content.append(new String(buffer));
			}
			in.close();

			String line;
			StringTokenizer st = new StringTokenizer(content.toString(), "\n");
			while (st.hasMoreTokens()) {
				line = st.nextToken();
				if (line.indexOf("\r") != -1) {
					line = line.substring(0, line.indexOf("\r"));
				}

				result.add(line);
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			try {
				//				Delete tmp file
				new File(tmpFileName).delete();
				//TODO
				// Just because of Linux problems
				new File(tmpFileName + "_call").delete();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		return result;

	}
}
