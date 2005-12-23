package org.epic.perleditor.editors.util;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.epic.core.Constants;
import org.epic.core.util.PerlExecutor;
import org.epic.perleditor.PerlEditorPlugin;

/**
 * Abstract base class for PerlValidator.
 * 
 * This class contains those parts of PerlValidator implementation
 * which are independent of the Eclipse runtime environment to aid
 * testing.
 *
 * @author jploski
 */
abstract class PerlValidatorBase
{
    private static final boolean DEBUG = true;
	private static int maxErrorsShown = 500;
    private static final int BUF_SIZE = 1024;
    
    private final ILog log;
	private final PerlValidatorErrors errors;
    private final PerlExecutor executor;
	
    protected PerlValidatorBase(ILog log, PerlExecutor executor)
    {
        this.log = log;
        this.executor = executor;
        
        errors = new PerlValidatorErrors();
    }
    
    /**
     * Validates the provided source code, creates markers on the given
     * IResource.
     */
	public synchronized void validate(IResource resource, String sourceCode)
        throws CoreException
    {
        String perlOutput = runPerl(resource, sourceCode);

        if (DEBUG) printPerlOutput(perlOutput);

		//TODO check if perlOutput is empty (indicates error)           

		// Mark problem markers as unused
        //
        // TODO: Scheduling all markers for removal at this point is actually
        // wrong because some markers might have been added by validation of
        // another resource (trigger). They should only disappear if validation
        // of all triggers no longer signals a problem. However, for the time
        // being we can live with disappearing markers - they typically do not
        // vanish completely because a related marker is still left in
        // the trigger as a reminder (at least this is the case for the most
        // common 'Can't locate .. in @INC' error message, which always leaves
        // "Compilation failed in require" in the trigger).
        //
        clearAllUsedMarkers(resource);

        List lines = makeLinesList(perlOutput);
        boolean continued = false;

		// Markers have to be added in reverse order
		// Otherwise lower line number will appear at the end of the list
		for (int i = lines.size() - 1; i >= 0; i--)
        {                
            String line = (String) lines.get(i);
            
            // Is this a continuation of the line i-1?
            if (line.startsWith(" "))
            {
                continued = true;
                continue;
            }
            else
            {
                if (continued) line += lines.get(i + 1);
                continued = false;                
            }
            
            ParsedErrorLine pline = new ParsedErrorLine(line, log);            
            IResource errorResource = getErrorResource(pline, resource);

            if (shouldIgnore(pline, errorResource)) continue;

            PerlValidatorErrors.ErrorMessage errorMsg =
                errors.getErrorMessage(pline.getMessage());
            
            Integer lineNr = new Integer(pline.getLineNumber());
            Map attributes = new HashMap(11);

            attributes.put(IMarker.SEVERITY, errorMsg.getSeverity());
			attributes.put(
                Constants.MARKER_ATTR_PERL_ERROR_EXPLANATION,
                errorMsg.getExplanation());            

            if (!pline.isLocalError() && errorResource == resource)
            {
                // last resort: we have a non-local error, but the resource
                // referred to in the error message could not be found
                attributes.put(
                    IMarker.MESSAGE,
                    pline.getMessage() + " in " + 
                    pline.getPath() + " line " + lineNr);
            }
            else
            {
                attributes.put(IMarker.MESSAGE, pline.getMessage());
                attributes.put(IMarker.LINE_NUMBER, lineNr);
                
                if (shouldUnderlineError(errorResource, pline.getLineNumber()))
                {
                    String errorSourceCode;
                    
                    try
                    {                    
                        if (errorResource == resource) errorSourceCode = sourceCode;
                        else errorSourceCode = readSourceFile(errorResource);
                    
                        underlineError(
                            errorResource,
                            errorSourceCode,
                            pline.getLineNumber(),
                            attributes);
                    }
                    catch (IOException e)
                    {
                        Status status = new Status(
                            Status.ERROR,
                            PerlEditorPlugin.getPluginId(),
                            IStatus.OK,
                            "Could not read source file of resource: " +
                            errorResource.getLocation() + ". Error markers will " +
                            "be incorrect for this resource.",
                            e);
                        // trouble reading the other file's source code
                        throw new CoreException(status);
                    }
                }
            }
            
            addMarker(errorResource, attributes);
		}
        
        removeUnusedMarkers(resource);
	}
    
    protected abstract void addMarker(IResource resource, Map attributes);

    protected abstract void clearAllUsedMarkers(IResource resource);

    protected IResource getErrorResource(ParsedErrorLine line, IResource resource)
    {
        return line.isLocalError() ? resource : null;
    }
    
    protected List getPerlArgs()
    {
        List args = new ArrayList();
        args.add("-c");
        return args;
    }
    
    protected abstract boolean isProblemMarkerPresent(
        ParsedErrorLine line, IResource resource);

    /**
     * Reads contents of the given text file.
     * 
     * @param path  file that should be read
     * @return text contents
     */
    protected String readSourceFile(String path) throws IOException
    {
        BufferedReader in = null;
        
        try
        {        
            StringWriter sourceCode = new StringWriter();

            char[] buf = new char[BUF_SIZE];
            in = new BufferedReader(new FileReader(path));

            int read = 0;
            while ((read = in.read(buf)) > 0) {
                sourceCode.write(buf, 0, read);
            }
            return sourceCode.toString();
        }
        finally
        {
            if (in != null) try { in.close(); } catch (IOException e) { }
        }
    }
    
    
    /**
     * Reads contents of the given text file.
     * 
     * @param resource file that should be read
     * @return text contents
     */
    protected String readSourceFile(IResource resource) throws IOException
    {
        return readSourceFile(resource.getLocation().makeAbsolute().toString());
    }
    
    protected abstract void removeUnusedMarkers(IResource resource);
    
    protected abstract boolean shouldUnderlineError(IResource resource, int lineNr);
    
    /**
     * Splits up the given text content into a list of up to maxErrorsShown lines.
     * If there are more lines in content, remaining lines are ignored.
     * 
     * @return a list of Strings, one per line (without line terminators)
     */
    private static List makeLinesList(String perlOutput)
    {
        List lines = new ArrayList();
        StringTokenizer st = new StringTokenizer(perlOutput, "\r\n");
        int lineCount = 0;

        while (st.hasMoreTokens() && lineCount < maxErrorsShown)
        {
            lines.add(st.nextToken());
            lineCount++;
        }
        return lines;
    }

    private void printPerlOutput(String perlOutput)
    {
        if (perlOutput.indexOf("syntax OK") == -1)
        {
			System.out.println("-----------------------------------------");
			System.out.println("           OUTPUT");
			System.out.println("-----------------------------------------");
			System.out.println(perlOutput);
			System.out.println("-----------------------------------------");
        }
    }
    
    /**
     * @return true if the error message contained in the line should
     *         be ignored, false otherwise
     */
    private boolean shouldIgnore(ParsedErrorLine line, IResource resource)
    {       
        // No line number?
        if (line.getLineNumber() < 0) return true;
        
        // Marker already present?        
        if (isProblemMarkerPresent(line, resource))
        {
            return true;
        }
        
        // Useless error message, with better ones around in the same output?
        if (line.getMessage().indexOf("BEGIN failed--compilation aborted") == 0)
            return true;
        else
            return false;
    }

    /**
     * Executes the Perl interpreter to validate the given script.
     * 
     * @param resource    source file
     * @param sourceCode  text of the source file 
     * @return stderr output of the Perl interpreter
     */
    private String runPerl(IResource resource, String sourceCode)
        throws CoreException
    {
        return executor.execute(resource, getPerlArgs(), sourceCode).stderr;
    }       
    
    private void underlineError(
        IResource resource, String sourceCode, int lineNo, Map attributes)
    {
        // Get start and end offset
        int lineOffset = 0;
        try
        {
            Document document = new Document(sourceCode);
            lineOffset = document.getLineOffset(lineNo - 1);
        }
        catch (BadLocationException e)
        {
            // this one should never occur
            log.log(
                new Status(Status.ERROR,
                    PerlEditorPlugin.getPluginId(),
                    IStatus.OK,
                    "Unexpected exception in PerlValidator.underlineError: " +
                    resource.getFullPath() + ", lineNo: " + lineNo +
                    "; report it as bug in plug-in " +
                    PerlEditorPlugin.getPluginId(),
                    e));
        }

        int endOfLine = sourceCode.indexOf("\n", lineOffset);
        String markerLine;

        if (endOfLine != -1) {
            markerLine =
                sourceCode.substring(lineOffset, endOfLine);
        } else {
            markerLine = sourceCode.substring(lineOffset);
        }

        char[] bytes = markerLine.toCharArray();

        int start = 0;
        while (start < bytes.length) {
            if (bytes[start] != '\t' && bytes[start] != ' ') {
                break;
            }
            start++;
        }

        start += lineOffset;

        int end = start + markerLine.trim().length();

        attributes.put(IMarker.CHAR_START, new Integer(start));
        attributes.put(IMarker.CHAR_END, new Integer(end));
    }
    
    protected static class ParsedErrorLine
    {
        private static final Pattern errorLineNoPattern =
            Pattern.compile("^(.*) at (\\S+) line (\\d+)[\\.,]");

        private static final Pattern cgiCarpPattern =
            Pattern.compile("^\\[.*?\\] \\S+: (.*)");
        
        private final ILog log;
        private final String line;
        private final String msg;
        private final String path;
        private final int lineNo;
        
        public ParsedErrorLine(String line, ILog log)
        {
            this.line = line;
            this.log =log;
            
            Matcher m = errorLineNoPattern.matcher(line);
            if (m.find())
            {
                msg = normalizeMsg(m.group(1));
                path = m.group(2);
                lineNo = parseInt(m.group(3));              
            }
            else
            {
                msg = normalizeMsg(line);
                path = "-";
                lineNo = -1;
            }
        }        
        
        public int getLineNumber()
        {
            return lineNo;
        }
        
        public String getMessage()
        {
            return msg;
        }
        
        public String getPath()
        {
            return path;
        }
        
        public boolean isLocalError()
        {
            return "-".equals(path);
        }
        
        public String toString()
        {
            return msg + ", " + path + ":" + lineNo;
        }
        
        private int parseInt(String str)
        {
            try { return Integer.parseInt(str); }
            catch (NumberFormatException e)
            {
                // this one should never occur
                log.log(
                    new Status(Status.ERROR,
                        PerlEditorPlugin.getPluginId(),
                        IStatus.OK,
                        "Could not parse line number contained in Perl " +
                        "error message {" + line + "}; report it as a bug " +
                        "in plug-in " + PerlEditorPlugin.getPluginId(),
                        e));
                return -1;
            }
        }
        
        private String normalizeMsg(String msg)
        {
            return stripCGICarpOutput(msg);
        }
        
        /**
         * @return msg with CGI::Carp's timestamp stripped from the beginning
         *         (if it was present)
         */
        private String stripCGICarpOutput(String msg)
        {
            if (msg.startsWith("["))
            {
                Matcher m = cgiCarpPattern.matcher(msg);
                if (m.find()) return m.group(1);
            }
            return msg;
        }
    }
}