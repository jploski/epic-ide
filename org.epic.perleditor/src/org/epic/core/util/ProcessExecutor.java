package org.epic.core.util;

import java.io.*;
import java.util.List;

/**
 * Responsible for execution of external, non-interactive processes which
 * expect input in form of command-line parameters and stdin and provide
 * output through stdout and/or stderr.
 * <p>
 * The Perl interpreter is one such process (when run non-interactively).
 * A specialized client class is available for this case and should be used
 * instead of the generic ProcessExecutor.
 * </p>
 * 
 * @see org.epic.core.util.PerlExecutor2
 * @author jploski
 */
public class ProcessExecutor
{
    private boolean disposed;
    private boolean ignoreBrokenPipe;
    private final String charsetName;
    private final StringReaderThread stdout;
    private final StringReaderThread stderr;
    
    /**
     * Creates a ProcessExecutor which will use the platform's default charset.
     */
    public ProcessExecutor()
    {
        this(null);
    }

    /**
     * Creates a ProcessExecutor which will use the given charset.
     *
     * @param charsetName
     *        The name of a supported
     *        {@link java.nio.charset.Charset </code>charset<code>}
     */
    public ProcessExecutor(String charsetName)
    {
        this.charsetName = charsetName;
        this.stdout = new StringReaderThread(":ProcessExecutor:stdout");
        this.stderr = new StringReaderThread(":ProcessExecutor:stderr");        
    }
    
    /**
     * Releases resources held by this ProcessExecutor.
     * Any active calls to {@link #execute} will throw an InterruptedException.
     * This ProcessExecutor must no longer be used after dispose.
     */
    public void dispose()
    {
        disposed = true;

        try { stdout.dispose(); }
        catch (InterruptedException e) { /* should never happen */ }
        try { stderr.dispose(); }
        catch (InterruptedException e) { /* should never happen */ }
    }
    
    /**
     * Same as {@link #execute(String[], String, File, String)}, except the command-line
     * is provided as a List of Strings rather than an array. 
     */
    public ProcessOutput execute(List<String> commandLine, String input, File workingDir, String charset)
        throws InterruptedException, IOException
    {
        return execute(
            commandLine.toArray(new String[commandLine.size()]),
            input,
            workingDir,
            charset);
    }
    
    /**
     * Executes a process specified by the (platform-specific) command line,
     * collects output and blocks until the process terminates. The process
     * is executed in the given working directory, with the provided input,
     * which is converted to bytes using this ProcessExecutor's charset.
     * 
     * @param commandLine  path to the process and command line parameters
     * @param input        input to be passed to the process via stdin
     * @param workingDir   working directory in which to execute the process
     * @param inputCharset name of the charset in which input should be encoded,
     *                     overrides the charset configured through constructor
     * @return output provided by the process through stdour or stderr,
     *         depending on this ProcessExecutor's configuration
     * @exception java.lang.InterruptedException
     *            if {@link #dispose} was called during this operation
     * @execption java.io.IOException
     *            if the process could not be started or communication problems
     *            were encountered 
     */
    public ProcessOutput execute(String[] commandLine, String input, File workingDir, String inputCharset)
        throws InterruptedException, IOException
    {
        if (disposed) throw new IllegalStateException("ProcessExecutor disposed");
        
        Process proc = null;
        
        try
        {
            proc = Runtime.getRuntime().exec(commandLine, null, workingDir);
    
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

            InputStream procStderr = proc.getErrorStream();
            InputStream procStdout = proc.getInputStream();
            OutputStream procStdin = proc.getOutputStream();
            
            Reader stderrReader;
            Reader stdoutReader;
            Writer inputWriter;
            
            if (inputCharset != null)
            {
                stderrReader = new InputStreamReader(procStderr, inputCharset);
                stdoutReader = new InputStreamReader(procStdout, inputCharset);
                inputWriter = new OutputStreamWriter(procStdin, inputCharset);
            }
            else if (charsetName != null)
            {
                stderrReader = new InputStreamReader(procStderr, charsetName);
                stdoutReader = new InputStreamReader(procStdout, charsetName);
                inputWriter = new OutputStreamWriter(procStdin, charsetName);
            }
            else
            {
                stderrReader = new InputStreamReader(procStderr);
                stdoutReader = new InputStreamReader(procStdout);
                inputWriter = new OutputStreamWriter(procStdin);
            }
        
            stderr.read(stderrReader);
            stdout.read(stdoutReader);
            
            if (input.length() > 0)
            {

                // because pushing a character stream into perl throws an error
                // on files with BOM, we detect BOM and skip this char. 
                int bomOffset = 0;
                try {
                    bomOffset = getBOMOffset(input.substring(0,1));
                } catch (UnsupportedEncodingException e) {
                    // let's ignore it for the time being...
                }

                // We split delivery of the process input into two steps
                // to support our client PerlValidator:
                // The first character is written to check that the output stream
                // is ready and not throwing exceptions...
                inputWriter.write(input.substring(bomOffset,bomOffset+1));
                inputWriter.flush();
                
                // The remaining write operation will often result in
                // a "broken pipe" IOException because Perl does not wait
                // until WE close the stream (and Java unfortunately treats
                // this condition as an exception). To make things worse, there is
                // no way to detect that the thrown exception is indeed a "broken
                // pipe": there is no error code (not even platform-specific one),
                // and the error message carried by the exception is localized.
                //
                // Additionally, if "broken pipe" happened, data might be left
                // unflushed, gets flushed on additional calls to "close" and
                // leads to another exception again. So only "close" here!
                try
                {
                    inputWriter.write(input.substring(bomOffset+1));
                    inputWriter.close();
                }
                catch (IOException e)
                {
                    /* let's hope it's just a broken pipe */
                    brokenPipe(e); // call it to support testing for this condition
                }
            }
    
            ProcessOutput ret = new ProcessOutput(
                stdout.getResult(),
                stderr.getResult());
            
            stderrReader.close();
            stdoutReader.close();

            return ret;
        }
        catch (InterruptedException e)
        {
            // it must have been dispose()
            if (proc != null) proc.destroy();
            throw e;
        }
        catch (IOException e)
        { 
            if (proc != null) proc.destroy();
            throw e;
        }
    }
    

    /**
     * This method is for the benefit of PerlValidator and should not concern
     * other clients.
     * <p>
     * Configures this ProcessExecutor to ignore suspected broken pipe
     * exceptions while delivering input to the executed process. 
     * It is intended as a workaround for a situation in which the Perl
     * interpreter (invoked with a -c switch) closes its output stream
     * before it has received the entire input; this particular behavior
     * causes an exception on the Java side, but can be safely ignored.
     */
    public void ignoreBrokenPipe()
    {
        this.ignoreBrokenPipe = true;
    }
    
    /**
     * Invoked when a suspected broken pipe exception was thrown
     * while delivering input to the executed process. This method
     * is here to enable testing for this condition.
     */
    protected void brokenPipe(IOException e) throws IOException
    {
        if (!ignoreBrokenPipe) throw e; // just rethrow by default
    }
    
    private int getBOMOffset(String bomChar) throws UnsupportedEncodingException {
        byte[] bom = new byte[4];
        //inputCharset = "UTF-32BE";
        bom = bomChar.getBytes("UTF-32BE");
        if ( (bom[0] == (byte)0x00) && (bom[1] == (byte)0x00) &&
                    (bom[2] == (byte)0xFE) && (bom[3] == (byte)0xFF) ) {
           return 1;
        }
        //inputCharset = "UTF-32LE";
        bom = bomChar.getBytes("UTF-32LE");
        if ( (bom[0] == (byte)0xFF) && (bom[1] == (byte)0xFE) &&
                    (bom[2] == (byte)0x00) && (bom[3] == (byte)0x00) ) {
           return 1;
        }
        //encoding = "UTF-8";
        bom = bomChar.getBytes("UTF-8");
        if (  (bom[0] == (byte)0xEF) && (bom[1] == (byte)0xBB) &&
              (bom[2] == (byte)0xBF) ) {
           return 1;
        }
        //encoding = "UTF-16BE";
        bom = bomChar.getBytes("UTF-16BE");
        if ( (bom[0] == (byte)0xFE) && (bom[1] == (byte)0xFF) ) {
           return 1;
        }
        //encoding = "UTF-16LE";
        bom = bomChar.getBytes("UTF-16LE");
        if ( (bom[0] == (byte)0xFF) && (bom[1] == (byte)0xFE) ) {
           return 1;
        } else {
           // Unicode BOM mark not found
           return 0;
        }    
    }
}
