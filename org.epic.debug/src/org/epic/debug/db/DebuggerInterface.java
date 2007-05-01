package org.epic.debug.db;

import gnu.regexp.REMatch;

import java.io.*;

import org.eclipse.core.runtime.*;
import org.eclipse.swt.widgets.Display;

/**
 * A low-level interface to the "perl -d" debugger process.
 * This class supports the execution of commands which would normally
 * be entered manually through the debugger's console. The actual
 * communication with the debugger happens on a dedicated thread.
 * Asynchronous (non-blocking) methods are provided for the long-running
 * commands (such as "resume"). When these commands finish, they notify
 * the registered listener using the event dispatch (Main) thread.
 * 
 * @author jploski
 */
public class DebuggerInterface
{
    private final CommandSlot slot = new CommandSlot();
    private final RE re = new RE();
    private final char[] buf = new char[1024];

    private final BufferedReader in;
    private final PrintWriter out;
    private final IListener listener;
    
    private final Thread thread;
    private final String perlVersion;
    private Boolean hasPadWalker;

    private boolean disposed;

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
    
    public DebuggerInterface(
        BufferedReader in,
        PrintWriter out,
        IListener listener) throws IOException
    {        
        this.in = in;
        this.out = out;
        this.listener = listener;
        
        thread = new Thread(new Runnable() {
            public void run() { commandLoop(); } },
            "EPIC:DebuggerInterface");
        thread.start();
        
        runSyncCommand(CMD_CLEAR_OUTPUT, null);
        perlVersion = getPerlVersion();
    }
    
    public void dispose()
    {
        if (!disposed)
        {                        
            try { runSyncCommand(CMD_EXEC, "q"); }
            catch (IOException e) { }
        
            synchronized (slot)
            {
                disposed = true;
                slot.notifyAll();
            }
        }
    }
    
    public Command asyncEval(String code)
    {
        return runAsyncCommand(CMD_EXEC, code, true);
    }
    
    public String eval(String code) throws IOException
    {
        return runSyncCommand(CMD_EXEC, code);
    }
    
    /**
     * @return true if the given path corresponds to a file in
     *         the debugger's file system; false otherwise
     */
    public boolean fileExists(IPath path) throws IOException
    {
        // Get an OS-specific path with escaped backslashes
        String osPath = getOSPath(path).replaceAll("\\\\", "\\\\\\\\");
        return "1".equals(eval("print $DB::OUT -f '" + osPath + "'"));
    }
    
    public boolean isDisposed()
    {
        return disposed;
    }
    
    public boolean isSuspended()
    {
        return slot.isEmpty();
    }
    
    public IPPosition getCurrentIP() throws IOException
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
            if (result == null) return null;
            String filename = result.toString(1);
            REMatch temp = re.IP_POS_EVAL.getMatch(filename);
            if (temp != null) result = temp;
        }

        return new IPPosition(
            new Path(result.toString(1)),
            Integer.parseInt(result.toString(2)));
    }

    public String getOS() throws IOException
    {
        return runSyncCommand(CMD_EXEC, "print $DB::OUT $^O;").trim();
    }
    
    public String getPerlVersion() throws IOException
    {
        return runSyncCommand(CMD_EXEC, "printf $DB::OUT \"%vd\", $^V;").trim();
    }
    
    public String getStackTrace() throws IOException
    {
        return runSyncCommand(CMD_EXEC, "T");
    }
    
    /**
     * @return true if we have an installed version of PadWalker required
     *         for dumping lexical variables; false otherwise
     */
    public boolean hasPadWalker() throws IOException
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
    
    public void redirectError(String host, int port) throws IOException
    {
        String command = "require IO::Socket; {my $OUT;"
            + "$OUT = new IO::Socket::INET("
            + "Timeout  => \'10\',"
            + "PeerAddr => \'" + host + ":" + port + "\',"
            + "Proto    => 'tcp',);" + "STDERR->fdopen($OUT,\"w\");}";

        runSyncCommand(CMD_EXEC, command);
    }
    
    public void redirectIO(String host, int port) throws IOException
    {
        String command = "require IO::Socket; {my $OUT;"
            + "$OUT = new IO::Socket::INET("
            + "Timeout  => \'10\',"
            + "PeerAddr => \'" + host + ":" + port + "\',"
            + "Proto    => 'tcp',);" + "STDOUT->fdopen($OUT,\"w\");"
            + "STDIN->fdopen($OUT,\"r\");}";

        runSyncCommand(CMD_EXEC, command);
    }
    
    public Command asyncResume()
    {
        return runAsyncCommand(CMD_RESUME, null, true);
    }
    
    public void resume() throws IOException
    {
        runSyncCommand(CMD_RESUME, null);
    }
    
    public void setFrame(int count) throws IOException
    {
        String cmd = perlVersion.startsWith("5.6")
            ? "O frame=" + count
            : "o frame=" + count;
        
        runSyncCommand(CMD_EXEC, cmd);
    }
    
    public void removeLineBreakpoint(int line) throws IOException
    {
        String command = perlVersion.startsWith("5.6")
            ? "d " + line
            : "B " + line;

        runSyncCommand(CMD_EXEC, command);
    }
    
    public boolean setLineBreakpoint(int line, String condition)
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
    public void setLoadBreakpoint(IPath path) throws IOException
    {
        runSyncCommand(CMD_EXEC, "b load " + getOSPath(path));
    }
    
    public Command asyncStepInto()
    {
        return runAsyncCommand(CMD_STEP_INTO, null, true);
    }
    
    public void stepInto() throws IOException
    {
        runSyncCommand(CMD_STEP_INTO, null);
    }

    public Command asyncStepOver()
    {
        return runAsyncCommand(CMD_STEP_OVER, null, true);
    }
    
    public void stepOver() throws IOException
    {
        runSyncCommand(CMD_STEP_OVER, null);
    }

    public Command asyncStepReturn()
    {
        return runAsyncCommand(CMD_STEP_RETURN, null, true);
    }
    
    public void stepReturn() throws IOException
    {
        runSyncCommand(CMD_STEP_RETURN, null);
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
    public boolean switchToFile(IPath path) throws IOException
    {
        String output = runSyncCommand(CMD_EXEC, "f " + getOSPath(path));

        return re.SWITCH_FILE_FAIL.getAllMatches(output).length == 0;
    }
    
    private void commandLoop()
    {
        while (!disposed)
        {   
            final Command cmd = slot.get();
            if (disposed) break;

            cmd.run();
            slot.remove();

            if (cmd.isNotifyOnFinish() && listener != null)
            {
                Display.getDefault().asyncExec(new Runnable() {
                    public void run() { 
                        listener.commandFinished(cmd);
                    } });
            }
        }
    }

    private String getOSPath(IPath path) throws IOException
    {
        return path.toString();
    }
    
    private Command runAsyncCommand(int command, String code, boolean notifyOnFinish)
    {
        Command cmd = new Command(command, code, notifyOnFinish);
        slot.put(cmd);
        return cmd;
    }
    
    private String runSyncCommand(int command, String code)
        throws IOException
    {
        Command cmd = new Command(command, code, false);
        slot.putAndWait(cmd);
        return cmd.getResult();
    }
    
    private String runCommand(int command, String code) throws IOException
    {
        assert Thread.currentThread() == thread;
        
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
        case CMD_SUSPEND:
        case CMD_TERMINATE:
        case CMD_CLEAR_OUTPUT:
            break;
        case CMD_EXEC:
        case CMD_EVAL:
            outputLine(code);
            break;
        default:
            throw new RuntimeException("unrecognized command " + command);
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
        // is not fool-proof. The debugge output may contain a fake
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
    
    private void outputLine(String line)
    {
        //System.err.println("->D: {" + line + "}");
        out.println(line);
    }
    
    private String readCommandOutput() throws IOException
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
                try { if (in.ready()) continue; } catch (IOException e) { }
            }
            catch (IOException e)
            {
                fireSessionTerminated();
                throw e;
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

            if (count < 0 || hasSessionTerminated(currentOutput))
            {
                fireSessionTerminated();
                return null;
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
    
    private void fireSessionTerminated()
    {
        if (!disposed && listener != null)
        {
            Display.getDefault().asyncExec(new Runnable() {
                public void run() { listener.sessionTerminated(); } });
        }
    }
    
    public static interface IListener
    {
        public void commandFinished(Command cmd);
        
        public void sessionTerminated();
    }
    
    public class Command
    {
        private final int type;
        private final String code;
        private final boolean notifyOnFinish;

        private String result;
        private IOException error;
        
        public Command(int type, String code, boolean notifyOnFinish)
        {
            this.type = type;
            this.code = code;
            this.notifyOnFinish = notifyOnFinish;
        }

        public String getResult() throws IOException
        {
            if (error != null) throw error;
            return result;
        }
        
        public String getCode()
        {
            return code;
        }
        
        public boolean isNotifyOnFinish()
        {
            return notifyOnFinish;
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
                
        void run()
        {
            try { result = runCommand(type, code); }
            catch (IOException e) { error = e; }
        }
        
        public String toString()
        {
            return "Command #" + type + " {" + code + "}" + ", notify=" + notifyOnFinish;
        }  
    }
    
    private class CommandSlot
    {
        private Command cmd;
        
        public synchronized Command get()
        {
            waitUntilFull();
            return cmd;
        }
        
        public synchronized boolean isEmpty()
        {
            return cmd == null;
        }
        
        public synchronized void put(Command cmd)
        {
            waitUntilEmpty();
            this.cmd = cmd;
            notifyAll();
        }
        
        public synchronized void putAndWait(Command cmd)
        {
            put(cmd);
            waitUntilEmpty();
        }
        
        public synchronized void remove()
        {
            this.cmd = null;
            notifyAll();
        }
        
        public synchronized void waitUntilEmpty()
        {
            while (cmd != null && !disposed)
                try { wait(); } catch (InterruptedException e) { }
        }
        
        public synchronized void waitUntilFull()
        {
            while (cmd == null && !disposed)
                try { wait(); } catch (InterruptedException e) { }
        }
    }
}
