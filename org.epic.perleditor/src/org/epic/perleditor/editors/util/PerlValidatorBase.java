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
	private static int maxErrorsShown = 10;
    private static final int BUF_SIZE = 1024;
    
    private final ILog log;
	private final PerlValidatorErrors errors;
    private final StringReaderThread srt = new StringReaderThread(":PerlValidator");
	
    protected PerlValidatorBase(ILog log)
    {
        this.log = log;
        errors = new PerlValidatorErrors();
    }
    
    /**
     * Validates the provided source code, creates markers on the given
     * IResource.
     */
	public synchronized void validate(IResource resource, String sourceCode)
        throws PerlValidatorException
    {
        String perlOutput = runPerl(resource, sourceCode);

        if (DEBUG) printPerlOutput(perlOutput);

		//TODO check if content is empty (indicates error)           

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
                        // trouble reading the other file's source code
                        throw new PerlValidatorException(e);
                    }
                }
            }
            
            addMarker(errorResource, attributes);
		}
        
        removeUnusedMarkers(resource);
	}
    
    protected abstract void addMarker(IResource resource, Map attributes);
    
    /**
     * This method is to be overridden only in testing environment.
     * Invoked whenever an IOException (hopefully just a "broken pipe")
     * occurs during communication with Perl.
     */
    protected void brokenPipe(IOException e) { }
    
    protected abstract void clearAllUsedMarkers(IResource resource);

    protected IResource getErrorResource(ParsedErrorLine line, IResource resource)
    {
        return line.isLocalError() ? resource : null;
    }
    
    protected abstract List getPerlCommandLine(IResource resource);
    
    protected abstract File getPerlWorkingDir(IResource resource);
    
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
        throws PerlValidatorException
    {
        if (sourceCode.length() < 1) return "";
        
        // Construct command line parameters
        List cmdList = getPerlCommandLine(resource);
        cmdList.add("-c");
    
        String[] cmdParams =
            (String[]) cmdList.toArray(new String[cmdList.size()]);
    
        // Get working directory -- Fixes Bug: 736631
        File workingDir = getPerlWorkingDir(resource);
                
        Process proc = null;
        
        try
        {
            proc = Runtime.getRuntime().exec(cmdParams, null, workingDir);

            /*
             * Due to Java Bug #4763384 sleep for a very small amount of time
             * immediately after starting the subprocess
             */
            try { Thread.sleep(1); }
            catch (InterruptedException e)
            {
                // believe it or not, InterruptedException sometimes occurs here
                // we can't help it ... ignore
            }

            proc.getInputStream().close();
            InputStream in = proc.getErrorStream();
            OutputStream out = proc.getOutputStream();
            //TODO which charset?
            Reader inr = new InputStreamReader(in);
            Writer outw = new OutputStreamWriter(out);
        
            srt.read(inr);
        
            // The first character is written to check that the output stream
            // is ready and not throwing exceptions...
            outw.write(sourceCode.charAt(0));
            outw.flush();
            
            // The remaining write operation will often result in
            // a "broken pipe" IOException because Perl does not wait
            // until WE close the stream (and Java unfortunately treats
            // this condition as an exception). To make things worse, there is
            // no way to detect that the thrown exception is indeed a "broken
            // pipe": there is no error code (not even platform-specific one),
            // and the error message carried by the exception is localized.
            try 
            {
                outw.write(sourceCode.substring(1));
                outw.write(0x1a); //this should avoid problem with Win98
                outw.flush();
            }
            catch (IOException e)
            {
                /* let's hope it's just a broken pipe */
                brokenPipe(e); // call it to support testing for this condition
            }

            out.close();
                
            String content = srt.getResult();
            inr.close();
            in.close();
            return content;
        }
        catch (InterruptedException e)
        {
            if (proc != null) proc.destroy();
            throw new PerlValidatorException(e);
        }
        catch (IOException e)
        {
            try { System.err.println(srt.getResult()); }
            catch (Exception _e) { _e.printStackTrace(); } 
            if (proc != null) proc.destroy();
            throw new PerlValidatorException(e);
        }
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
                    10001, // TODO: use some sort of constant
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
            Pattern.compile("^(.*) at (\\S+) line (\\d+)\\.$");

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
        
        private int parseInt(String str)
        {
            try { return Integer.parseInt(str); }
            catch (NumberFormatException e)
            {
                // this one should never occur
                log.log(
                    new Status(Status.ERROR,
                        PerlEditorPlugin.getPluginId(),
                        10003, // TODO: use some sort of constant
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