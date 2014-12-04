package org.epic.perleditor.editors.perl;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.contentassist.*;
import org.eclipse.ui.editors.text.TextEditor;
import org.epic.core.model.ISourceElement;
import org.epic.core.util.PerlExecutor;
import org.epic.perleditor.PerlEditorPlugin;
import org.epic.perleditor.editors.PartitionTypes;
import org.epic.perleditor.editors.PerlPartitioner;
import org.epic.perleditor.preferences.CodeAssistPreferences;
import org.epic.perleditor.templates.*;
import org.epic.perleditor.templates.perl.*;

/**
 * Perl completion processor.
 */
public class PerlCompletionProcessor implements IContentAssistProcessor
{        
    private static final IPerlCompletionProposal[] NO_PROPOSALS =
        new IPerlCompletionProposal[0];
    
    // public to enable unit testing
    public static Pattern MODULE_PREFIX_PATTERN = Pattern.compile("([A-Za-z0-9_]+(::|->))+");
    public static Pattern VAR_PREFIX_PATTERN = Pattern.compile("\\$[A-Za-z0-9_]+(::|->)$");
    
	private final IContextInformationValidator fValidator = new Validator();
    private final PerlCompletionProposalComparator fComparator = new PerlCompletionProposalComparator();    
    private final TextEditor fTextEditor;
	private final TemplateEngine fTemplateEngine;

	public PerlCompletionProcessor(TextEditor textEditor) 
    {
		fTextEditor = textEditor;

		ContextType contextType = getContextType();
		if (contextType != null) fTemplateEngine = new TemplateEngine(contextType);
        else fTemplateEngine = null;
	}

	public ICompletionProposal[] computeCompletionProposals(
		ITextViewer viewer, int documentOffset)
    {
		// get the current document's text, up to caret position
		IDocument document = viewer.getDocument();
        ITextSelection selection = (ITextSelection) viewer.getSelectionProvider().getSelection();        
        if (selection.getLength() > 0) return NO_PROPOSALS;

		String lastTextLine = null;
		try
        {
            int lastLine = document.getLineOfOffset(documentOffset);
            int lineStartOffset = document.getLineOffset(lastLine);
            lastTextLine = document.get(
                lineStartOffset, documentOffset - lineStartOffset);
        }
        catch (BadLocationException ble)
        {
			// TODO what to do here?
		}

        String moduleNamePrefix = getModuleNamePrefix(lastTextLine); 
		if (moduleNamePrefix != null)
        {
            return computeModuleNameProposals(
                viewer, documentOffset, moduleNamePrefix);
        }
		else
        {
			String className = getClassName(documentOffset, document);
            return className != null
                ? computeMethodProposals(viewer, documentOffset, className)
                : sort(concatenate(
                    computeVariableProposals(viewer, documentOffset),
                    computeTemplateProposals(viewer, documentOffset)
                    ));
        }
    }
    
    public IContextInformation[] computeContextInformation(
        ITextViewer viewer,
        int documentOffset)
    {
        return null;
    }

    public char[] getCompletionProposalAutoActivationCharacters()
    {
        String activationChars  = PerlEditorPlugin.getDefault().getPreferenceStore().getString(CodeAssistPreferences.AUTO_ACTIVATION_CHARS);
        return activationChars.toCharArray();
    }

    public char[] getContextInformationAutoActivationCharacters()
    {
        return null;
    }

    public IContextInformationValidator getContextInformationValidator()
    {
        return fValidator;
    }

    public String getErrorMessage()
    {
        return null;
    }

    private ICompletionProposal[] computeMethodProposals(
        ITextViewer viewer, int documentOffset, String className)
    {
        List<String> proposals = getProposalsForClassname(className);
        ContextType contextType = getContextType();
        if (contextType != null)
        {
            SubroutineEngine subroutineEngine = new SubroutineEngine(contextType);
            subroutineEngine.complete(
                viewer,
                documentOffset,
                proposals.toArray(new String[proposals.size()]));
            return subroutineEngine.getResults();
        }
        else return new IPerlCompletionProposal[0];
    }
    
    private ICompletionProposal[] computeModuleNameProposals(
        ITextViewer viewer, int documentOffset, String moduleNamePrefix)
    {   
        ModuleCompletionHelper completionHelper = ModuleCompletionHelper.getInstance();
        return completionHelper.getProposals(moduleNamePrefix, documentOffset, viewer);
    }
    
    private IPerlCompletionProposal[] computeTemplateProposals(
        ITextViewer viewer, int documentOffset)
    {
        fTemplateEngine.reset();
        fTemplateEngine.complete(viewer, documentOffset);
        return fTemplateEngine.getResults();
    }

    private IPerlCompletionProposal[] computeVariableProposals(ITextViewer viewer, int documentOffset)
    {
        if (!PerlEditorPlugin.getDefault().getPreferenceStore().getBoolean(
            CodeAssistPreferences.INSPECT_VARIABLES)) return NO_PROPOSALS;
        
        ContextType contextType = getContextType();
        if (contextType == null) return NO_PROPOSALS;

    	Set<String> variables = getCompletionVariables(viewer, documentOffset);

    	VariableEngine varsEngine = new VariableEngine(contextType);

		varsEngine.complete(
			viewer,
			documentOffset,
			variables.toArray(new String[variables.size()]));

        return varsEngine.getResults();
    }

	/**
	 * Gets variable/filedescriptor info from source file
	 */
	private Set<String> getCompletionVariables(
		ITextViewer viewer, int documentOffset)
    {
		List<SourceElement> variablesModel = new ArrayList<SourceElement>();
		Set<String> variables = new HashSet<String>();

		String variableChars = "%$@";
		String filehandleChars = "<";

		try
        {
			documentOffset = getCompletionStartOffset(
                viewer.getDocument(),
                documentOffset,
                variableChars + filehandleChars);

            if (documentOffset < viewer.getDocument().getLength())
            {
    			String key = viewer.getDocument().get(documentOffset, 1);
    			if (variableChars.indexOf(key) != -1)
                {
                    variablesModel = getVariableElements(viewer, documentOffset);
    			}
                else if (filehandleChars.indexOf(key) != -1)
                {
    				variablesModel =
    					SourceParser.getElements(
    						viewer.getDocument(),
                            "open[a-z]*\\s*?\\s*?[(]\\s*?([A-Z_0-9]+)\\s*?[,]",
    						"<",
    						">",
    						true);
    			}
            }

			for (Iterator<SourceElement> i = variablesModel.iterator(); i.hasNext();)
            {
				ISourceElement elem = i.next();
				String name = elem.getName();

				// Only insert variables once
				if (!variables.contains(name))
                {
					variables.add(name);

					if (name.startsWith("@"))
                        variables.add("$" + name.substring(1) + "[]");
                    else if (name.startsWith("%"))
						variables.add("$" + name.substring(1) + "{}");
				}
			}
		}
        catch (BadLocationException ex)
        {
			ex.printStackTrace(); // TODO log it
		}
		return variables;
	}
    
    private List<SourceElement> getVariableElements(ITextViewer viewer, int documentOffset)
        throws BadLocationException
    {
        IDocument doc = viewer.getDocument();
        IDocumentPartitioner p = 
            ((IDocumentExtension3) doc).getDocumentPartitioner(
                PartitionTypes.PERL_PARTITIONING);
        
        if (!(p instanceof PerlPartitioner)) return Collections.emptyList();

        ITypedRegion[] regions =
            ((PerlPartitioner) p)
            .computePartitioning(0, documentOffset);
        
        List<SourceElement> elements = new ArrayList<SourceElement>();

        for (int i = 0; i < regions.length; i++)
        {
            if (regions[i].getType().equals(PartitionTypes.VARIABLE))
            {
                String name = doc.get(regions[i].getOffset(), regions[i].getLength());
                if (name.length() > 1 && name.charAt(1) == '{') continue; // ignore ${@foo} and the like
                elements.add(new SourceElement(
                    name, regions[i].getOffset(), regions[i].getLength()));
            }
        }
        return elements;
    }

	private String getClassName(int documentOffset, IDocument document)
    {
		try
        {
            // Find offset just after the -> or :: for which completion is requested 
			documentOffset =
                getCompletionStartOffset(document, documentOffset, "_");

			String text = document.get(0, documentOffset);
			if (!text.endsWith("->") && !text.endsWith("::")) return null;

			// Get the object name
			int lineNo = document.getLineOfOffset(documentOffset);
			String line = text.substring(document.getLineOffset(lineNo));

			// Lines can end with "->" of "::"
            if (VAR_PREFIX_PATTERN.matcher(line).find())
            {
				String objName = line.substring(line.lastIndexOf('$'));

                objName = objName.indexOf("->") != -1
                    ? objName.substring(0, objName.indexOf("->"))
                    : objName.substring(0, objName.indexOf("::"));
                
                String className = findTypedLexical(objName, text);
                if (className == null) className = findConstructorCall(objName, text);

                return className;
			}
            else
            {
                Matcher m = MODULE_PREFIX_PATTERN.matcher(line);
                if (m.find())
                {
                    // strip -> or :: from the end
                    String str = m.group(0);
                    return str.substring(0, str.length()-2); 
                }
                else return null;
			}
		}
        catch (Exception ex)
        {
			return null; // TODO error handling
		}
	}

    private String findConstructorCall(String objName, String text)
    {
        // Try to find something like "my $foo = Some::Class->..."
        Pattern p = Pattern.compile("\\" + objName + "\\s*=\\s*([a-zA-Z:->0-9_]+)(->|::|;)");
        Matcher m = p.matcher(text);
        String className = null;
        while (m.find()) className = m.group(1);
        return className;
    }
    
    private String findTypedLexical(String objName, String text)
    {        
        // Try to find typed lexical like "my Some::Class $foo = ...;"
        // or "my __PACKAGE__ $foo = ...;"
        Pattern p = Pattern.compile("my\\s+(\\S+)\\s+\\" + objName + "\\s*=");
        Matcher m = p.matcher(text);
        int foundAt = -1;
        String className = null;
        while (m.find())
        {
            className = m.group(1);
            foundAt = m.start(1);
        }                
        if (className != null)
        {
            if ("__PACKAGE__".equals(className)) 
            {
                // Find most recent "package ...;" line before
                // the typed lexical was introduced
                p = Pattern.compile("\\s*package\\s+(\\S+)\\s*;");
                m = p.matcher(text);
                
                while (m.find() && m.start(1) < foundAt)
                    className = m.group(1);
                
                // not found? bad luck
                if ("__PACKAGE__".equals(className)) return null;
            }
        }
        return className;
    }
	
    private List<String> getProposalsForClassname(String className)
    {
		String perlCode =
			"use "
				+ className
				+ ";\n\n"
                + "@classes = qw(" + className + ");\n"
                + "push(@classes, @" + className + "::ISA);\n"
                + "foreach $class (@classes) {\n"
                + "next if ($class eq 'Exporter');\n"
                + "%symtab = eval '%'.$class.'::';\n"
				+ "foreach $name (keys %symtab) {\n"
				+ " next if($name !~ /[a-z]/ || $name =~ /^_/);\n"
				+ "   if(defined &{$class.'::'.$name}) {\n"
				+ "       $proposals{$name.'()'} = 1;\n"
				+ "   }\n"
				+ "}}\n"
                + "print join(\"\\n\", sort keys %proposals);";

        PerlExecutor executor = new PerlExecutor();
		try
        {
            return executor.execute(fTextEditor, null, perlCode).getStdoutLines();
        }
        catch (CoreException e)
        {
            PerlEditorPlugin.getDefault().getLog().log(e.getStatus());
            return new ArrayList<String>();
        }
        finally
        {
            executor.dispose();
        }
	}

	/**
	 * Orders the given proposals.
	 */
	private IPerlCompletionProposal[] sort(IPerlCompletionProposal[] proposals)
    {
		Arrays.sort(proposals, fComparator);
		return proposals;
	}

	/**
	 * Finds the content assist start offset: the offset just after
     * the sequence of characters triggering the completion. For example,
     * if the completion was requested after typing "$foo->ab", the method
     * would return the offset just after "$foo->".
     *
     * @param document      document in which completion was requested
     * @param caretOffset   offset at which completion was requested
	 * @return offset
	 */
	private int getCompletionStartOffset(
		IDocument document,
		int caretOffset,
        String specialChars)
		throws BadLocationException
    {
		while (stepLeft(document, caretOffset, specialChars)) caretOffset--;
		if (stepLeft(document, caretOffset, specialChars)) caretOffset--;
		return caretOffset;
	}
    
    private ContextType getContextType()
    {
        return ContextTypeRegistry.getInstance().getContextType("perl"); //$NON-NLS-1$
    }
    
    private String getModuleNamePrefix(String line)
    {
        Pattern pattern = Pattern.compile(
            "\\s*use[ \\t]+(\\S*)\\s*$", Pattern.MULTILINE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(line);
        return matcher.matches() ? matcher.group(1) : null;
    }
    
    private boolean stepLeft(IDocument doc, int offset, String specialChars)
        throws BadLocationException
    {
        return
            offset != 0 &&
            (Character.isUnicodeIdentifierPart(doc.getChar(offset - 1)) ||
            specialChars.indexOf(doc.getChar(offset - 1)) != -1);
    }
    
    private static IPerlCompletionProposal[] concatenate(
        IPerlCompletionProposal[] a,
        IPerlCompletionProposal[] b)
    {   
        IPerlCompletionProposal[] result =
            new IPerlCompletionProposal[b.length + a.length];
        System.arraycopy(b, 0, result, 0, b.length);
        System.arraycopy(a, 0, result, b.length, a.length);
        return result;
    }

    /**
     * Simple content assist tip closer. The tip is valid in a range
     * of 5 characters around its popup location.
     */
    private static class Validator
        implements IContextInformationValidator, IContextInformationPresenter
    {
        protected int fInstallOffset;

        public boolean isContextInformationValid(int offset)
        {
            return Math.abs(fInstallOffset - offset) < 5;
        }

        public void install(
            IContextInformation info,
            ITextViewer viewer,
            int offset)
        {
            fInstallOffset = offset;
        }

        public boolean updatePresentation(
            int documentPosition,
            TextPresentation presentation)
        {
            return false;
        }
    }
}
