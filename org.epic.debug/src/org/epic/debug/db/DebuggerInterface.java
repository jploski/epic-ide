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
class DebuggerInterface
{
    private final Object LOCK = new Object();
    private final RE re = new RE();
    private final char[] buf = new char[1024];

    private final BufferedReader in;
    private final PrintWriter out;
    private final IListener listener;
    
    private final Thread thread;
    private final String perlVersion;    
    
    private boolean disposed;
    private Command asyncCommand;

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
        
            synchronized (LOCK)
            {
                disposed = true;
                thread.interrupt();
            }
        }
    }
    
    private void commandLoop()
    {
        synchronized (LOCK)
        {
            while (!disposed)
            {   
                while (!disposed && asyncCommand == null)
                    try { LOCK.wait(); } catch (InterruptedException e) { }
                    
                if (disposed) break;

                asyncCommand.run();                
                LOCK.notifyAll();
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
    
    public boolean isDisposed()
    {
        return disposed;
    }
    
    public boolean isSuspended()
    {
        return asyncCommand == null;
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
    
    public String getPerlVersion() throws IOException
    {
        return runSyncCommand(CMD_EXEC, "printf $DB::OUT \"%vd\", $^V;").trim();
    }
    
    public String getStackTrace() throws IOException
    {
        return runSyncCommand(CMD_EXEC, "T");
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
    
    public boolean setLineBreakpoint(int line) throws IOException
    {
        String output = runSyncCommand(CMD_EXEC, "b " + line);
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
        runSyncCommand(CMD_EXEC, "b load " + path);
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
        String output = runSyncCommand(CMD_EXEC, "f " + path);

        return re.SWITCH_FILE_FAIL.getAllMatches(output).length == 0;
    }
    
    private Command runAsyncCommand(int command, String code, boolean notifyOnFinish)
    {
        synchronized (LOCK)
        {
            asyncCommand = new Command(command, code, notifyOnFinish);
            LOCK.notifyAll();
            return asyncCommand;
        }
    }
    
    private String runSyncCommand(int command, String code)
        throws IOException
    {
        synchronized (LOCK)        
        {
            Command cmd = runAsyncCommand(command, code, false);
            while (asyncCommand != null && !disposed)
                try { LOCK.wait(); } catch(InterruptedException e) {}

            return cmd.getResult();
        }
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

    private boolean hasCommandTerminated(String fOutput)
    {
        boolean erg;
        int count;

        erg = re.CMD_FINISHED1.isMatch(fOutput);
        count = re.CMD_FINISHED1.getAllMatches(fOutput).length;
        if (erg || (count > 0)) return (true);

        erg = re.CMD_FINISHED2.isMatch(fOutput);
        count = re.CMD_FINISHED2.getAllMatches(fOutput).length;
        return erg || (count > 0);
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

            if (count < 0 || hasSessionTerminated(currentOutput))
            {
                fireSessionTerminated();
                return null;
            }
            else if (hasCommandTerminated(currentOutput))
            {
                // Return command output without the trailing
                // DB<nn> prompt

                int lfIndex = output.lastIndexOf("\n");
                int crIndex = output.lastIndexOf("\r");
                
                if (lfIndex < 0) lfIndex = Integer.MAX_VALUE;
                if (crIndex < 0) crIndex = Integer.MAX_VALUE;
                
                int nlIndex = Math.min(crIndex, lfIndex);
                if (nlIndex != Integer.MAX_VALUE)
                {
                    return output.substring(0, nlIndex);
                }
                else return output.toString();
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
            finally
            {
                asyncCommand = null;
                if (notifyOnFinish && listener != null)
                {
                    Display.getDefault().asyncExec(new Runnable() {
                        public void run() { 
                            listener.commandFinished(Command.this);
                        } });
                }
            }
        }
        
        public String toString()
        {
            return "Command #" + type + " {" + code + "}" + ", notify=" + notifyOnFinish;
        }  
    }
}
