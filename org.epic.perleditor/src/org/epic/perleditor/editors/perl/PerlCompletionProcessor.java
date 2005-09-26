package org.epic.perleditor.editors.perl;

import gnu.regexp.RE;
import gnu.regexp.REMatch;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationPresenter;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.editors.text.TextEditor;
import org.epic.perleditor.PerlEditorPlugin;
import org.epic.perleditor.editors.util.PerlExecutableUtilities;
import org.epic.perleditor.preferences.CodeAssistPreferences;
import org.epic.perleditor.templates.ContextType;
import org.epic.perleditor.templates.ContextTypeRegistry;
import org.epic.perleditor.templates.TemplateEngine;
import org.epic.perleditor.templates.perl.IPerlCompletionProposal;
import org.epic.perleditor.templates.perl.ModuleCompletionHelper;
import org.epic.perleditor.templates.perl.PerlCompletionProposalComparator;
import org.epic.perleditor.templates.perl.SubroutineEngine;
import org.epic.perleditor.templates.perl.VariableEngine;
import org.epic.perleditor.views.model.Model;
import org.epic.perleditor.views.util.SourceParser;

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
	}

	protected IContextInformationValidator fValidator = new Validator();
	private TextEditor fTextEditor = null;

	private TemplateEngine fTemplateEngine;
	private PerlCompletionProposalComparator fComparator;


	/*
	 * Constructor
	 */
	public PerlCompletionProcessor(TextEditor textEditor) {
		fTextEditor = textEditor;

		ContextType contextType = ContextTypeRegistry.getInstance().getContextType("perl"); //$NON-NLS-1$
		if (contextType != null)
			fTemplateEngine = new TemplateEngine(contextType);

		fComparator = new PerlCompletionProposalComparator();
	}
	/**
   * 
   */
  public PerlCompletionProcessor() {
    
    // nothing to do cause, it's only required for OpenDeclaration
  }
  /* 
	 * Method declared on IContentAssistProcessor
	 */
	public ICompletionProposal[] computeCompletionProposals(
		ITextViewer viewer,
		int documentOffset) {

		// get the current document's text
		IDocument document = viewer.getDocument();
		String text = null;
		try {
			text = document.get(0, documentOffset);
		} catch (BadLocationException ble) {
			// TODO what to do here?
		}
		
		// try to find out what we should present as completion options
		
		// do we need module completion?
		Pattern pattern = Pattern.compile(".*use\\s*(.*)$", Pattern.MULTILINE | Pattern.DOTALL);
		Matcher matcher = pattern.matcher(text);
		if (matcher.matches()) {
			// --> show modules
			String moduleNameFragment = matcher.group(1);
			
			ModuleCompletionHelper completionHelper = ModuleCompletionHelper.getInstance();
			return completionHelper.getProposals(moduleNameFragment, documentOffset, viewer);
		}
		else {
			String className = getClassName(documentOffset, null);
			if (className != null) {
	
				List proposals = getProposalsForClassname(className);
	
				IPerlCompletionProposal[] subroutineResults =
					new IPerlCompletionProposal[0];
				SubroutineEngine subroutineEngine;
				ContextType contextType = ContextTypeRegistry.getInstance().getContextType("perl"); //$NON-NLS-1$
				if (contextType != null) {
					subroutineEngine = new SubroutineEngine(contextType);
					subroutineEngine.complete(
						viewer,
						documentOffset,
						(String[]) proposals.toArray(new String[0]));
					subroutineResults = subroutineEngine.getResults();
				}
				return subroutineResults;
			} else {
				// we're not inside a class
				
				// variables
				IPerlCompletionProposal[] varsResults =	new IPerlCompletionProposal[0];
				boolean inspectVars = PerlEditorPlugin.getDefault().getPreferenceStore().getBoolean(CodeAssistPreferences.INSPECT_VARIABLES);
				if(inspectVars) {
					//Get variables
					List variables;
					variables = getCompletionVariables(viewer, documentOffset);
					VariableEngine varsEngine;
					ContextType contextType = ContextTypeRegistry.getInstance().getContextType("perl"); //$NON-NLS-1$
					if (contextType != null) {
						varsEngine = new VariableEngine(contextType);
						varsEngine.complete(
							viewer,
							documentOffset,
							(String[]) variables.toArray(new String[0]));
						varsResults = varsEngine.getResults();
					}
				}
	
				// templates
				fTemplateEngine.reset();
				fTemplateEngine.complete(viewer, documentOffset);
				IPerlCompletionProposal[] templateResults =
					fTemplateEngine.getResults();
	
				// concatenate arrays
				IPerlCompletionProposal[] result;
				result =
					new IPerlCompletionProposal[templateResults.length
						+ varsResults.length];
				System.arraycopy(
					templateResults,
					0,
					result,
					0,
					templateResults.length);
				System.arraycopy(
					varsResults,
					0,
					result,
					templateResults.length,
					varsResults.length);
	
				return sort(result);
			}
		}

		//return result;
		//return subroutineResults;
	}

	/**
	 * Gets variable/filedescriptor info from source file
	 * @param viewer
	 * @param documentOffset
	 * @return
	 */
	private List getCompletionVariables(
		ITextViewer viewer,
		int documentOffset) {

		String VARIABLE_REGEXP = "([$@%][a-z0-9A-Z_]+)\\s*[=;]";
		String FILEHANDLE_REGEXP =
			"open[a-z]*\\s*?\\s*?[(]\\s*?([A-Z_0-9]+)\\s*?[,]";

		List variablesModel = new ArrayList();
		List variables = new ArrayList();

		String variableChars = "%$@";
		String filehandleChars = "<";

		try {

			documentOffset =
				getCompletionStartOffset(
					viewer.getDocument(),
					documentOffset,
					variableChars + filehandleChars);

			String key = viewer.getDocument().get(documentOffset, 1);
			if (variableChars.indexOf(key) != -1) {
				String regexp = VARIABLE_REGEXP;
				variablesModel =
					SourceParser.getElements(
						viewer.getDocument(),
						regexp,
						"",
						"",
						true);
			} else if (filehandleChars.indexOf(key) != -1) {
				String regexp = FILEHANDLE_REGEXP;
				variablesModel =
					SourceParser.getElements(
						viewer.getDocument(),
						regexp,
						"<",
						">",
						true);
			}

			Map alreadyInserted = new HashMap();
			for (Iterator iterate = variablesModel.iterator();
				iterate.hasNext();
				) {
				Model model = (Model) iterate.next();
				String element = model.getName();

				// Only insert variables once
				if (alreadyInserted.get(element) == null) {
					variables.add(element);
					alreadyInserted.put(element, "");
					
					String elementAdditional = null;

					if (element.startsWith("@")) {
						elementAdditional = "$" + element.substring(1) + "[]";
					} else if (element.startsWith("%")) {
						elementAdditional = "$" + element.substring(1) + "{}";
					}
					if (elementAdditional != null && alreadyInserted.get(elementAdditional) == null) {
						variables.add(elementAdditional);
						alreadyInserted.put(elementAdditional, "");
					}
					
				}

			}

		} catch (BadLocationException ex) {
			ex.printStackTrace();
		}
		return variables;
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
		String activationCharas  = PerlEditorPlugin.getDefault().getPreferenceStore().getString(CodeAssistPreferences.AUTO_ACTIVATION_CHARS);
		return activationCharas.toCharArray();
		//return ">:<$@%".toCharArray();
		//return new char[] { '>', ':', '<', '$', '@', '%' };
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

	private String getClassName(int documentOffset, IDocument document) {
		String objName = null;
		String className = null;

		try {
			// Calculate documentOffset
			String specialChars = "_"; // "_" can be contained in classname

			documentOffset =
				getCompletionStartOffset(
					document,
					documentOffset,
					specialChars);

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

	public final List getProposalsForClassname(TextEditor fTextEditor, String className){
	  this.fTextEditor = fTextEditor ;
	  return getProposalsForClassname(className);
	}
	
	private List getProposalsForClassname(
		String className) {
		List result = new ArrayList();
	//	int READ_BUFFER_SIZE = 128;

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

		try {

			//			Construct command line parameters
			List cmdList =
				PerlExecutableUtilities.getPerlExecutableCommandLine(
					fTextEditor);

			String[] cmdParams =
				(String[]) cmdList.toArray(new String[cmdList.size()]);

			//Get working directory -- Fixes Bug: 736631
			String workingDir =
				((IFileEditorInput) fTextEditor.getEditorInput())
					.getFile()
					.getLocation()
					.makeAbsolute()
					.removeLastSegments(1)
					.toString();

			/*
			 * Due to Java Bug #4763384 sleep for a very small amount of time
			 * immediately after starting the subprocess
			 */
			Process proc =
				Runtime.getRuntime().exec(
					cmdParams,
					null,
					new File(workingDir));
			Thread.sleep(1);

			proc.getErrorStream().close();
			InputStream in = proc.getInputStream();
			OutputStream out = proc.getOutputStream();
			//TODO which charset?
			Writer outw = new OutputStreamWriter(out);

			try {
				outw.write(perlCode);
				outw.write(0x1a); //this should avoid problem with Win98
				outw.flush();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
			out.close();

			String content = PerlExecutableUtilities.readStringFromStream(in);
			in.close();

			String line;
			StringTokenizer st = new StringTokenizer(content, "\n");
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

			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		return result;

	}

	/**
	* Order the given proposals.
	*/
	private IPerlCompletionProposal[] sort(IPerlCompletionProposal[] proposals) {
		Arrays.sort(proposals, fComparator);
		return proposals;
	}

	/**
	 * Returns the content assist start offset
	 * 
	 * @param document
	 * @param documentOffset
	 * @param specialChars
	 * @return offset
	 * @throws BadLocationException
	 */
	private int getCompletionStartOffset(
		IDocument document,
		int documentOffset,
		String specialChars)
		throws BadLocationException {
		while (((documentOffset != 0)
			&& Character.isUnicodeIdentifierPart(
				document.getChar(documentOffset - 1)))
			|| ((documentOffset != 0)
				&& specialChars.indexOf(document.getChar(documentOffset - 1))
					!= (-1))) {
			documentOffset--;
		}

		if (((documentOffset != 0)
			&& Character.isUnicodeIdentifierStart(
				document.getChar(documentOffset - 1)))
			|| ((documentOffset != 0)
				&& specialChars.indexOf(document.getChar(documentOffset - 1))
					!= (-1))) {
			documentOffset--;
		}

		return documentOffset;
	}
}
