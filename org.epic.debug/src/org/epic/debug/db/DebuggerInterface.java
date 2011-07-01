package org.epic.debug.db;

import gnu.regexp.REMatch;

import java.io.*;

import org.eclipse.core.runtime.*;

/**
 * A low-level interface to the "perl -d" debugger process.
 * This class supports the (synchronous) execution of commands
 * which would normally be entered manually through the debugger's
 * console. DebuggerInterface does not support concurrent use by
 * multiple threads.
 * 
 * Note that every debugger command can throw SessionTerminatedException.
 * If you wish to distinguish between IOExceptions and the event of
 * debugger termination, you should catch this exception type explicitly.
 * 
 * @author jploski
 */
public class DebuggerInterface
{
    private final RE re = new RE();
    private final char[] buf = new char[1024];

    private BufferedReader in;
    private PrintWriter out;

    private final String perlVersion;
    private Boolean hasPadWalker;

    public static final int CMD_NONE = 0;
    public static final int CMD_STEP_INTO = 1;
    public static final int CMD_STEP_OVER = 2;
    public static final int CMD_STEP_RETURN = 4;
    public static final int CMD_RESUME = 8;
    public static final int CMD_SUSPEND = 16;
    public static final int CMD_TERMINATE = 32;
    public static final int CMD_CLEAR_OUTPUT = 64;
    public static final int CMD_EXEC = 128;
    public static final int CMD_EVAL = 256; // TODO: not needed?
    public static final int CMD_MODIFIER_RANGE_START = 1024;
    public static final int CMD_MODIFIER_SKIP_EVAL_CMD_RESULT = CMD_MODIFIER_RANGE_START;
    
    public DebuggerInterface(BufferedReader in, PrintWriter out)
        throws IOException
    {
        assert in != null;
        assert out != null;
        
        this.in = in;
        this.out = out;
        
        runSyncCommand(CMD_CLEAR_OUTPUT, null);
        perlVersion = getPerlVersion();
    }
    
    public synchronized void dispose()
    {
        if (in != null)
        {
            try { quit(); } catch (IOException e) { }
            try { in.close(); } catch (IOException e) { }
            out.close();
            
            in = null;
            out = null;
        }
    }
    
    public synchronized String eval(String code) throws IOException
    {
        return runSyncCommand(CMD_EXEC, code);
    }
    
    /**
     * @return true if the given path corresponds to a file in
     *         the debugger's file system; false otherwise
     */
    public synchronized boolean fileExists(IPath path)
        throws IOException
    {
        // Get an OS-specific path with escaped backslashes
        String osPath = getOSPath(path).replaceAll("\\\\", "\\\\\\\\");
        return "1".equals(eval("print $DB::OUT -f '" + osPath + "'"));
    }
    
    public synchronized IPPosition getCurrentIP() throws IOException
    {
        String output = runSyncCommand(CMD_EXEC, ".");

        // mRe_IP_Pos_CODE handles locations like
        //     main::CODE(0x814f960)(/some/path/trycatch.pl:7):
        // mRe_IP_Pos handles locations like
        //     main::(/some/path/foobar.pl:7):
        // mRe_IP_Pos_Eval handles locations like
        //

        REMatch result = re.IP_POS_CODE.getMatch(output);
        if (result == null)
        {
            result = re.IP_POS.getMatch(output);
            if (result == null)
                throw new IOException("could not match re.IP_POS in {" + output + "}");
            String filename = result.toString(1);
            REMatch temp = re.IP_POS_EVAL.getMatch(filename);
            if (temp != null) result = temp;
        }

        return new IPPosition(
            new Path(result.toString(1)),
            Integer.parseInt(result.toString(2)));
    }

    public synchronized String getOS() throws IOException
    {
        return runSyncCommand(CMD_EXEC, "print $DB::OUT $^O;").trim();
    }
    
    public synchronized String getPerlVersion() throws IOException
    {        
        return runSyncCommand(CMD_EXEC, "printf $DB::OUT \"%vd\", $^V;").trim();
    }
    
    public synchronized String getStackTrace() throws IOException
    {
        return runSyncCommand(CMD_EXEC, "T");
    }
    
    /**
     * @return true if we have an installed version of PadWalker required
     *         for dumping lexical variables; false otherwise
     */
    public synchronized boolean hasPadWalker() throws IOException
    {
        if (hasPadWalker == null)
        {
            String result = runSyncCommand(
                CMD_EXEC,
                "print $DB::OUT eval { require PadWalker; PadWalker->VERSION(0.08) }");
            hasPadWalker = new Boolean(result.length() > 0);
        }

        return hasPadWalker.booleanValue();
    }
    
    public synchronized String quit() throws IOException
    {
        return runSyncCommand(CMD_TERMINATE, null);
    }
    
    public synchronized String redirectError(String host, int port) throws IOException
    {
        String command = "require IO::Socket; {my $OUT;"
            + "$OUT = new IO::Socket::INET("
            + "Timeout  => \'10\',"
            + "PeerAddr => \'" + host + ":" + port + "\',"
            + "Proto    => 'tcp',);" + "STDERR->fdopen($OUT,\"w\");}";

        return runSyncCommand(CMD_EXEC, command);
    }
    
    public synchronized String redirectIO(String host, int port) throws IOException
    {
        String command = "require IO::Socket; {my $OUT;"
            + "$OUT = new IO::Socket::INET("
            + "Timeout  => \'10\',"
            + "PeerAddr => \'" + host + ":" + port + "\',"
            + "Proto    => 'tcp',);" + "STDOUT->fdopen($OUT,\"w\");"
            + "STDIN->fdopen($OUT,\"r\");}";

        return runSyncCommand(CMD_EXEC, command);
    }
    
    public synchronized String resume() throws IOException
    {
        return runSyncCommand(CMD_RESUME, null);
    }
    
    public synchronized String setFrame(int count) throws IOException
    {
        String cmd = perlVersion.startsWith("5.6")
            ? "O frame=" + count
            : "o frame=" + count;
        
        return runSyncCommand(CMD_EXEC, cmd);
    }
    
    public synchronized String removeLineBreakpoint(int line) throws IOException
    {
        String command = perlVersion.startsWith("5.6")
            ? "d " + line
            : "B " + line;

        return runSyncCommand(CMD_EXEC, command);
    }
    
    public synchronized boolean setLineBreakpoint(int line, String condition)
        throws IOException
    {
        String output = runSyncCommand(
            CMD_EXEC, "b " + line +
            (condition != null ? (" " + condition) : ""));
        return re.SET_LINE_BREAKPOINT.getAllMatches(output).length == 0;
    }
    
    /**
     * Sets a breakpoint that will be triggered on loading of a given
     * source file.
     * 
     * @param path
     *        path to the source file, specific to the debugger's
     *        environment
     */
    public synchronized String setLoadBreakpoint(IPath path) throws IOException
    {
        return runSyncCommand(CMD_EXEC, "b load " + getOSPath(path));
    }
    
    public synchronized String stepInto() throws IOException
    {
        return runSyncCommand(CMD_STEP_INTO, null);
    }
    
    public synchronized String stepOver() throws IOException
    {
        return runSyncCommand(CMD_STEP_OVER, null);
    }
    
    public synchronized String stepReturn() throws IOException
    {
        return runSyncCommand(CMD_STEP_RETURN, null);
    }
    
    /**
     * Switches the debugging context to the given source file.
     * This affects commands such as {@link #setLineBreakpoint(int)}.
     * 
     * @param path
     *        path to the source file, specific to the debugger's
     *        environment
     * @return true if the operation succeeded,
     *         false if the source file has not been loaded yet
     */
    public synchronized boolean switchToFile(IPath path) throws IOException
    {
        String output = runSyncCommand(CMD_EXEC, "f " + getOSPath(path));

        return re.SWITCH_FILE_FAIL.getAllMatches(output).length == 0;
    }

    private String getOSPath(IPath path) throws IOException
    {
        return path.toString();
    }

    private String runSyncCommand(int command, String code) throws IOException
    {
        assert Thread.currentThread().getName().indexOf("Main") == -1
            : "forbidden DebuggerInterface use from display thread";

        Command cmd = new Command(command, code);
        return cmd.run();
    }
    
    private String runCommand(int command, String code) throws IOException
    {
        command = maskCommandModifiers(command);

        switch (command)
        {
        case CMD_STEP_INTO:
            outputLine("s");
            break;
        case CMD_STEP_OVER:
            outputLine("n");
            break;
        case CMD_STEP_RETURN:
            outputLine("r");
            break;
        case CMD_RESUME:
            outputLine("c");
            break;
        case CMD_TERMINATE:
            outputLine("q");
            break;
        case CMD_SUSPEND:
        case CMD_CLEAR_OUTPUT:
            break;
        case CMD_EXEC:
        case CMD_EVAL:
            outputLine(code);
            break;
        default:
            throw new RuntimeException("unrecognized command " + command);
        }
        
        synchronized (this)
        {
            if (out == null) throw new SessionTerminatedException("disposed");
        }
        return readCommandOutput();
    }
    
    private int maskCommandModifiers(int command)
    {
        return command & (CMD_MODIFIER_RANGE_START - 1);
    }

    private int hasCommandTerminated(String output)
    {
        // TODO: this way of finding out whether a command has terminated
        // is not fool-proof. The debugger output may contain a fake
        // command termination token, so there's a bug waiting to be
        // uncovered here...
        
        REMatch[] matches;

        matches = re.CMD_FINISHED1.getAllMatches(output);
        if (matches != null && matches.length > 0)
            return matches[matches.length-1].getStartIndex();

        matches = re.CMD_FINISHED2.getAllMatches(output);
        if (matches != null && matches.length > 0)
            return matches[matches.length-1].getStartIndex();

        return -1;
    }

    private boolean hasSessionTerminated(String fOutput)
    {
        boolean erg;
        int count;

        erg = re.SESSION_FINISHED1.isMatch(fOutput);
        count = re.SESSION_FINISHED1.getAllMatches(fOutput).length;
        if (erg || (count > 0)) return true;

        erg = re.SESSION_FINISHED2.isMatch(fOutput);
        count = re.SESSION_FINISHED2.getAllMatches(fOutput).length;
        if (erg || (count > 0)) return true;

        return false;
    }
    
    private synchronized void outputLine(String line)
    {
        //System.err.println("->D: {" + line + "}");
        if (out != null)
        {
            out.println(line);
            out.flush();
        }
    }
    
    private String readCommandOutput()
        throws IOException, SessionTerminatedException
    {
        StringBuffer output = new StringBuffer();
        while (true)
        {
            int count;
            try
            {
                count = in.read(buf);
                //System.err.println("<-D: {" + String.valueOf(buf, 0, count) + "}");
                if (count > 0) output.append(buf, 0, count);
                try { if (in.ready() && count != -1) continue; } catch (IOException e) { }
            }
            catch (IOException e)
            {
                throw new SessionTerminatedException(
                    "IOException while reading debugger's response");
            }
            
            // Note that we apply the regular expressions used to find out
            // whether the command/session has terminated only to the last few
            // characters of the output; applying them to the whole output
            // (which can become *very* long) causes major performance problems

            int inspectLen = Math.min(output.length(), 350);            
            String currentOutput = output.substring(
                output.length() - inspectLen,
                output.length());
            
            int endIndex = hasCommandTerminated(currentOutput);

            if (count < 0)
            {
                throw new SessionTerminatedException("EOF from debugger");
            }
            else if (hasSessionTerminated(currentOutput))
            {
                throw new SessionTerminatedException(
                    "Debugger session terminated normally");
            }
            else if (endIndex != -1)
            {
                // Return command output without the trailing
                // DB<nn> prompt
                String ret = output.substring(
                    0, output.length() - inspectLen + endIndex);
                return ret;
            }
        }
    }
    
    public class Command
    {
        private final int type;
        private final String code;
        
        public Command(int type, String code)
        {
            this.type = type;
            this.code = code;
        }

        public String getCode()
        {
            return code;
        }

        public boolean isStepCommand()
        {
            return
                type == CMD_STEP_INTO ||
                type == CMD_STEP_OVER ||
                type == CMD_STEP_RETURN;
        }
        
        public int getType()
        {
            return type;
        }
                
        public String run() throws IOException, SessionTerminatedException
        {
            return runCommand(type, code);           
        }
        
        public String toString()
        {
            return "Command #" + type + " {" + code + "}";
        }  
    }
    
    /**
     * Thrown when an executed debugger command does not terminate
     * normally because the debugging session has finished.
     * For convenience of clients who consider abnormal command
     * termination as bad regardless of its reason, this is a subtype
     * of IOException.
     */
    public static class SessionTerminatedException extends IOException
    {
        private static final long serialVersionUID = 1L;

        public SessionTerminatedException(final String msg)
        {
            super(msg);
        }
    }
}
