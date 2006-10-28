package org.epic.debug;

import gnu.regexp.RE;
import gnu.regexp.REException;
import gnu.regexp.REMatch;
import gnu.regexp.RESyntax;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.ITerminate;
import org.eclipse.debug.core.model.IThread;
import org.epic.debug.ui.action.ShowLocalVariableActionDelegate;
import org.epic.debug.util.DebuggerProxy2;
import org.epic.debug.varparser.PerlDebugValue;
import org.epic.debug.varparser.PerlDebugVar;
import org.epic.debug.varparser.TokenVarParser;
import org.epic.perleditor.PerlEditorPlugin;
import org.epic.regexp.views.RegExpView;

/**
 * @author ruehl
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class PerlDB implements IDebugElement, ITerminate
{
    private static final String mDBinitPerl_5_8 = "o frame=2";
    private static final String mDBinitPerl_5_6 = "O frame=2";
    private static final String mPadwalkerError = "PadWalker module not found - please install";
    private static final String mLovalVarCommand = ";{eval { require PadWalker; PadWalker->VERSION(0.08) }or print $DB::OUT (\""
        + mPadwalkerError
        + "\\n\");do 'dumpvar_epic.pm' unless defined &dumpvar_epic::dumpvar_epic;defined &dumpvar_epic::dumpvar_epic or print $DB::OUT \"dumpvar_epic.pm not available.\\n\";my $h = eval { PadWalker::peek_my(2) };my @vars = split (' ','');$@ and $@ =~ s/ at .*//, print $DB::OUT ($@);my $savout = select($DB::OUT);dumpvar_epic::dumplex($_,$h->{$_},defined $option{dumpDepth} ? $option{dumpDepth} : -1,@vars) for sort keys %$h;print \"E\";select($savout);};\n";
    private static final String mGlobalVarCommand = ";{do 'dumpvar_epic.pm' unless defined &dumpvar_epic::dumpvar_epic;defined &dumpvar_epic::dumpvar_epic or print $DB::OUT \"dumpvar_epic.pm not available.\\n\";my $savout = select($DB::OUT);dumpvar_epic::dumpvar_epic();select($savout);};\n";

    // Command codes
    private static final int mCommandNone = 0;
    private static final int mCommandStepInto = 1;
    private static final int mCommandStepOver = 2;
    private static final int mCommandStepReturn = 4;
    private static final int mCommandResume = 8;
    private static final int mCommandSuspend = 16;
    private static final int mCommandTerminate = 32;
    private static final int mCommandClearOutput = 64;
    private static final int mCommandExecuteCode = 128;
    private static final int mCommandEvaluateCode = 256;
    private static final int mCommandModifierRangeStart = 1024;
    private static final int mCommandModifierSkipEvaluateCommandResult = mCommandModifierRangeStart;

    private static final int mIsStepCommand =
        mCommandStepInto | mCommandStepOver | mCommandStepReturn;
    private static final int mIsRunCommand =
        mIsStepCommand | mCommandResume;

    // Result codes after executing a command
    private static final int COMMAND_FINISHED = 1;
    private static final int SESSION_TERMINATED = 2;

    private static boolean mLocalVarsAvailable = true;

    private final PerlDebugThread[] mThreads;

    // Regular expressions used for parsing "perl -d" output
    private final RE mReCommandFinished1;
    private final RE mReCommandFinished2;
    private final RE mReSessionFinished1, mReSessionFinished2;
    private final RE mRe_IP_Pos;
    private final RE mRe_IP_Pos_Eval;
    private final RE mRe_IP_Pos_CODE;
    private final RE mReSwitchFileFail;
    private final RE mReSetLineBreakpoint;
    private final RE mReStackTrace;
    private final RE mReEnterFrame;
    private final RE mReExitFrame;

    // Input/output streams and working directory of the "perl -d" process
    private /*final*/ PrintWriter mDebugIn;
    private /*final*/ BufferedReader mDebugOut;
    private final IPath mWorkingDir;

    private final TokenVarParser mVarParser = new TokenVarParser(this);

    private final BreakpointMap mPendingBreakpoints;
    private final BreakpointMap mActiveBreakpoints;

    private final org.epic.debug.util.PathMapper mPathMapper;

    private final String mPerlVersion;

    private String mDebugOutput;
    private String mDebugSubCommandOutput;
    private IPPosition mStartIP;
    private boolean mIsCommandRunning;
    private boolean mIsSessionTerminated;
    private DebugTarget mTarget;
    private CommandThread mCommandThread;
    private int mCurrentCommand;
    /* NO debugging meassages are created for sub-commands */
    private int mCurrentSubCommand;
    private Object mCurrentCommandDest;

    public boolean mStopVarUpdate;
    private String mVarLocalString;
    private String mVarGlobalString;
    private VarUpdateJob mVarUpdateJob;
    private StackFrame mStackFrameOrg;

    public PerlDB(DebugTarget fTarget) throws CoreException
    {
        mTarget = fTarget;
        mWorkingDir = mTarget.getLocalWorkingDir();
        mCurrentCommand = mCommandNone;
        mCurrentSubCommand = mCommandNone;

        mPendingBreakpoints = new BreakpointMap();
        mActiveBreakpoints = new BreakpointMap();

        mThreads = new PerlDebugThread[1];
        mThreads[0] = new PerlDebugThread("Main-Thread", fTarget.getLaunch(),
            fTarget, this);

        mReCommandFinished1 = newRE("\n\\s+DB<+\\d+>+", false);
        mReCommandFinished2 = newRE("^\\s+DB<\\d+>", false);
        mReSessionFinished1 = newRE("Use `q' to quit or `R' to restart", false);
        mReSessionFinished2 = newRE("Debugged program terminated.", false);
        mRe_IP_Pos = newRE("^[^\\(]*\\((.*):(\\d+)\\):[\\n\\t]", false);
        mRe_IP_Pos_Eval = newRE("^[^\\(]*\\(eval\\s+\\d+\\)\\[(.*):(\\d+)\\]$", false);
        mRe_IP_Pos_CODE = newRE("^.*CODE\\(0x[0-9a-fA-F]+\\)\\(([^:]*):(\\d+)\\):[\\n\\t]", false);
        mReSwitchFileFail = newRE("^No file", false);
        mReSetLineBreakpoint = newRE("^\\s+DB<\\d+>", false);
        mReEnterFrame = newRE("^\\s*entering", false);
        mReExitFrame = newRE("^\\s*exited", false);
        mReStackTrace = newRE(
            "^(.)\\s+=\\s+(.*)called from .* \\`([^\\']+)\\'\\s*line (\\d+)\\s*$",
            true);

        mDebugIn = mTarget.getDebugWriteStream();
        mDebugOut = mTarget.getDebugReadStream();

        if (PerlEditorPlugin.getDefault().getDebugConsolePreference())
        {
            DebuggerProxy2 p = new DebuggerProxy2(mDebugIn, mDebugOut, getLaunch());
            getLaunch().addProcess(p);
            mDebugIn = p.getDebugIn();
            mDebugOut = p.getDebugOut();
        }

        mPathMapper = mTarget.getPathMapper();

        startCommand(mCommandClearOutput, null, false, this);

        if (!isTerminated(this))
        {
            String command;
            if (getPerlVersion().startsWith("5.6."))
            {
                mPerlVersion = "5.6";
                command = mDBinitPerl_5_6;
            }
            else
            {
                mPerlVersion = "5.8";
                command = mDBinitPerl_5_8;
            }
            startCommand(mCommandExecuteCode, command, false, this);

            PerlDebugPlugin.getPerlBreakPointmanager().addDebugger(this);
            mTarget.perlDBstarted(this);
            updateStackFramesInit(null);
            generateDebugInitEvent();
            if (isBreakPointReached())
            {

                DebugEvent event = new DebugEvent(mThreads[0],
                    DebugEvent.BREAKPOINT, DebugEvent.BREAKPOINT);
                DebugEvent debugEvents[] = new DebugEvent[1];
                debugEvents[0] = event;
                DebugPlugin.getDefault().fireDebugEventSet(debugEvents);
            }
        }
        else
        {
            mPerlVersion = "5.8";
            generateDebugTermEvent();
        }
    }

    public String getModelIdentifier()
    {
        return mTarget.getModelIdentifier();
    }

    public IDebugTarget getDebugTarget()
    {
        return mTarget;
    }

    public ILaunch getLaunch()
    {
        return mTarget.getLaunch();
    }

    public boolean canResume(Object fDest)
    {
        return !mIsCommandRunning && !mIsSessionTerminated;
    }

    public boolean canSuspend(Object fDest)
    {
        return false;
    }

    public boolean isSuspended(Object fDest)
    {
        return !mIsCommandRunning && !mIsSessionTerminated;
    }

    public void resume(Object fDest)
    {
        startCommand(mCommandResume, fDest);
    }

    public void suspend(Object fDest)
    {
        startCommand(mCommandSuspend, fDest);
    }

    public boolean canStepInto(Object fDest)
    {
        return isSuspended(fDest);
    }

    public boolean canStepOver(Object fDest)
    {
        return isSuspended(fDest);
    }

    public boolean canStepReturn(Object fDest)
    {
        return isSuspended(fDest);
    }

    public boolean isStepping(Object fDest)
    {
        return (mCurrentCommand & mIsStepCommand) != 0 && mIsCommandRunning;
    }

    public void stepInto(Object fDest)
    {
        startCommand(mCommandStepInto, fDest);
    }

    public void stepOver(Object fDest)
    {
        startCommand(mCommandStepOver, fDest);
    }

    public void stepReturn(Object fDest)
    {
        startCommand(mCommandStepReturn, fDest);
    }

    public boolean canTerminate()
    {
        return canTerminate(null);
    }

    public boolean canTerminate(Object fDest)
    {
        return !isTerminated();
    }

    public boolean isTerminated()
    {
        return isTerminated(null);
    }

    public boolean isTerminated(Object fDest)
    {
        return mIsSessionTerminated;
    }

    public void terminate()
    {
        abortSession();
    }

    public Object getAdapter(Class adapter)
    {
        if (adapter == this.getClass()) return this;
        else return null;
    }

    public boolean startCommand(int fCommand, Object fThread)
    {
        return (startCommand(fCommand, null, true, fThread));
    }

    public String getPerlVersion()
    {
        return (evaluateStatement(mThreads[0],
            "printf $DB::OUT \"%vd\", $^V;\n", false));

    }

    public String evaluateStatement(Object fThread, String fText)
    {
        return evaluateStatement(fThread, fText, true);
    }

    synchronized public String evaluateStatement(Object fThread, String fText,
        boolean fUpdateVars)
    {

        String res;
        int command = mCommandEvaluateCode;

        if (!fUpdateVars) command = mCommandExecuteCode;

        boolean erg = startCommand(command, fText, false, fThread);
        if (!erg) return (null);
        res = mDebugOutput;

        if (res == null) return (null);
        int index_n = res.lastIndexOf("\n");
        int index_r = res.lastIndexOf("\r");

        if (res == null || ((index_n <= 0) && (index_r <= 0)))
        {
            return null;
        }

        int index;

        if (index_n > 0 && !(index_r > 0 && index_r < index_n)) index = index_n;
        else index = index_r;

        String result = res.substring(0, index);
        return (result);
    }

    public synchronized boolean startCommand(
        int fCommand,
        String fCode,
        boolean fSpawn,
        Object fThread)
    {
        if (mIsCommandRunning) return false;

        mCurrentCommandDest = fThread;
        mDebugOutput = null;
        mDebugSubCommandOutput = null;
        mCurrentCommand = fCommand;
        mCurrentSubCommand = mCommandNone;
        mIsCommandRunning = true;
        if (isStepCommand(fCommand))
        {
            mStopVarUpdate = true;
        }
        return startPerlDebugCommand(fCode, fSpawn);
    }

    public boolean startSubCommand(int fCommand)
    {
        return (startSubCommand(fCommand, null, true));
    }

    private boolean startSubCommand(int fCommand, String fCode, boolean fSpawn)
    {
        mDebugSubCommandOutput = null;
        mCurrentSubCommand = fCommand;
        return (startPerlDebugCommand(fCode, fSpawn));
    }

    private boolean startPerlDebugCommand(String fCode, boolean fSpawn)
    {
        int command;
        if (!isSubCommand())
        {
            command = mCurrentCommand;
        }
        else
        {
            command = mCurrentSubCommand;
        }

        command = maskCommandModifiers(command);
        if (isStepCommand(command) && !isSubCommand())
            mStartIP = getCurrent_IP_Position();

        switch (command)
        {
        case mCommandStepInto:
            mDebugIn.println("s\n");
            break;

        case mCommandStepOver:
            mDebugIn.println("n\n");
            break;
        case mCommandStepReturn:
            mDebugIn.println("r\n");
            break;
        case mCommandResume:
            mDebugIn.println("c\n");
            break;
        case mCommandSuspend:
            break;
        case mCommandTerminate:
            break;
        case mCommandClearOutput:
            break;
        case mCommandExecuteCode:
        case mCommandEvaluateCode:
            mDebugIn.println(fCode + "\n");
            break;
        default:
            return (false);
        }

        generateDebugEvent(command, true, mTarget);

        if (fSpawn)
        {
            mCommandThread = new CommandThread();
            mCommandThread.start();
            return (true);
        }

        return (waitForCommandToFinish());
    }

    void generateDebugEvent(int fCommand, boolean fStart, Object fCommandDest)
    {
        DebugEvent event = null;
        int stepEventEndDetail;

        if (isSubCommand()) return;

        if (((fCommand & mIsStepCommand) != 0) && !fStart)
        {
            if (isBreakPointReached()) stepEventEndDetail = DebugEvent.BREAKPOINT;
            else stepEventEndDetail = DebugEvent.STEP_END;

            event = new DebugEvent(fCommandDest, DebugEvent.SUSPEND,
                stepEventEndDetail);
        }
        else
        {
            switch (fCommand)
            {
            case mCommandStepInto:
                event = new DebugEvent(fCommandDest, DebugEvent.RESUME,
                    DebugEvent.STEP_INTO);
                break;

            case mCommandStepOver:
                event = new DebugEvent(fCommandDest, DebugEvent.RESUME,
                    DebugEvent.STEP_INTO);
                break;

            case mCommandStepReturn:
                event = new DebugEvent(fCommandDest, DebugEvent.RESUME,
                    DebugEvent.STEP_RETURN);
                break;

            case mCommandResume:
                if (fStart) event = new DebugEvent(fCommandDest,
                    DebugEvent.RESUME, DebugEvent.CLIENT_REQUEST);
                else event = new DebugEvent(fCommandDest, DebugEvent.SUSPEND,
                    DebugEvent.BREAKPOINT);
                break;

            case mCommandSuspend:
                if (!fStart)
                    event = new DebugEvent(fCommandDest, DebugEvent.SUSPEND,
                        DebugEvent.CLIENT_REQUEST);
                break;

            case mCommandTerminate:
                if (!fStart)
                    event = new DebugEvent(fCommandDest, DebugEvent.TERMINATE);
                break;

            case mCommandEvaluateCode:
                if (fStart) event = new DebugEvent(fCommandDest,
                    DebugEvent.RESUME, DebugEvent.CLIENT_REQUEST);
                else event = new DebugEvent(fCommandDest, DebugEvent.SUSPEND,
                    DebugEvent.BREAKPOINT);
                break;
            }
        }
        if (event != null)
        {
            DebugEvent debugEvents[] = new DebugEvent[1];
            debugEvents[0] = event;
            DebugPlugin.getDefault().fireDebugEventSet(debugEvents);
        }
    }

    public void generateDebugInitEvent()
    {
        DebugEvent event = null;

        event = new DebugEvent(mThreads[0], DebugEvent.SUSPEND,
            DebugEvent.STEP_END);
        DebugEvent debugEvents[] = new DebugEvent[1];
        debugEvents[0] = event;
        DebugPlugin.getDefault().fireDebugEventSet(debugEvents);
    }

    public void generateDebugEvalEvent()
    {
        DebugEvent event = null;

        event = new DebugEvent(mThreads[0], DebugEvent.SUSPEND,
            DebugEvent.EVALUATION);
        DebugEvent debugEvents[] = new DebugEvent[1];
        debugEvents[0] = event;
        DebugPlugin.getDefault().fireDebugEventSet(debugEvents);
    }

    public void generateDebugTermEvent()
    {
        DebugEvent event = null;

        event = new DebugEvent(mThreads[0], DebugEvent.TERMINATE,
            DebugEvent.STEP_END);
        DebugEvent debugEvents[] = new DebugEvent[1];
        debugEvents[0] = event;
        DebugPlugin.getDefault().fireDebugEventSet(debugEvents);
    }

    public void generateTargetTermEvent()
    {
        DebugEvent event = null;

        event = new DebugEvent(mTarget, DebugEvent.TERMINATE,
            DebugEvent.STEP_END);
        DebugEvent debugEvents[] = new DebugEvent[1];
        debugEvents[0] = event;
        DebugPlugin.getDefault().fireDebugEventSet(debugEvents);
    }

    private boolean waitForCommandToFinish()
    {
        char[] buf = new char[1024];
        int count;
        int finished;
        StringBuffer debugOutput = new StringBuffer();
        String currentOutput;
        boolean ok;

        if (isTerminated(mCurrentCommandDest)) return (false);

        while (true)
        {
            count = -1;
            try
            {
                count = mDebugOut.read(buf);
            }
            catch (IOException e)
            {
                abortSession();
                return (false);
            }            

            if (count > 0) debugOutput.append(buf, 0, count);
            
            try { if (mDebugOut.ready()) continue; } catch (IOException e) { }
            
            // Note that we apply the regular expressions used to find out
            // whether the command/session has terminated only to the last few
            // characters of the output; applying them to the whole output
            // (which can become *very* long) causes major performance problems
            int inspectLen = Math.min(debugOutput.length(), 350);
            currentOutput = debugOutput.substring(
                debugOutput.length() - inspectLen,
                debugOutput.length());

            if (count == -1 || hasSessionTerminated(currentOutput))
            {
                finished = SESSION_TERMINATED;
                break;
            }
            else if (hasCommandTerminated(currentOutput))
            {
                finished = COMMAND_FINISHED;
                break;
            }
            else if (count < 0)
            {
                finished = SESSION_TERMINATED;
                break;
            }
        }
        currentOutput = debugOutput.toString();

        if (finished == SESSION_TERMINATED)
        {
            abortSession();
            return (false);
        }
        if (isStepCommand(mCurrentCommand) && !isSubCommand())
        {
            IPPosition endIP = getCurrent_IP_Position();
            while ((finished != SESSION_TERMINATED) && mStartIP.equals(endIP))
            {
                startSubCommand(mCurrentCommand
                    | mCommandModifierSkipEvaluateCommandResult, null, false);
                endIP = getCurrent_IP_Position();
            }
            currentOutput = debugOutput.toString();

        }

        if (isRunCommand(mCurrentCommand)
            && (mCurrentCommand != mCommandStepInto) && !isSubCommand())
        {

            while ((finished != SESSION_TERMINATED) && !isBreakPointReached()
                && !isRunCommand(mCurrentCommand))
            {
                insertPendingBreakpoints();
                startSubCommand(mCurrentCommand
                    | mCommandModifierSkipEvaluateCommandResult, null, false);
            }
            currentOutput = debugOutput.toString();

        }
        ok = evaluateCommandResult(finished, currentOutput);
        commandPostExec(finished, currentOutput);
        mCurrentSubCommand = mCommandNone;
        return (ok);
    }

    private boolean hasCommandTerminated(String fOutput)
    {
        boolean erg;
        int count;

        erg = mReCommandFinished1.isMatch(fOutput);
        count = mReCommandFinished1.getAllMatches(fOutput).length;
        if (erg || (count > 0)) return (true);

        erg = mReCommandFinished2.isMatch(fOutput);
        count = mReCommandFinished2.getAllMatches(fOutput).length;
        return (erg || (count > 0));
    }

    private boolean hasSessionTerminated(String fOutput)
    {
        boolean erg;
        int count;

        erg = mReSessionFinished1.isMatch(fOutput);
        count = mReSessionFinished1.getAllMatches(fOutput).length;
        if (erg || (count > 0)) return (true);

        erg = mReSessionFinished2.isMatch(fOutput);
        count = mReSessionFinished2.getAllMatches(fOutput).length;
        if (erg || (count > 0)) return (true);

        return (false);

    }

    private void finishCommand(String fOutput)
    {
        if (mCurrentSubCommand == mCommandNone)
        {

            switch (mCurrentCommand)
            {
            case mCommandStepInto:
            case mCommandStepOver:
            case mCommandStepReturn:
            case mCommandResume:
            case mCommandSuspend:
            case mCommandTerminate:
            case mCommandEvaluateCode:
                updateStackFramesInit(fOutput);
                break;
            default:
                mIsCommandRunning = false;
                break;
            }
            generateDebugEvent(PerlDB.this.mCurrentCommand, false,
                mCurrentCommandDest);
            mDebugOutput = fOutput;
        }
        else mDebugSubCommandOutput = fOutput;
    }

    private void abortCommandThread()
    {
        abortSession();
        PerlDB.this
            .generateDebugEvent(PerlDB.mCommandTerminate, false, mTarget);

    }

    private void abortSession()
    {
        mStopVarUpdate = true;
        mIsSessionTerminated = true;
        mCurrentSubCommand = mCommandNone;
        mCurrentCommand = mCommandNone;
        mIsCommandRunning = false;
        boolean skip = false;

        if (!isSuspended(null) && !(mTarget instanceof CGITarget)) mTarget
            .killDebugProcess();
        else
        {
            try
            {
                mCurrentCommandDest = mThreads[0];
                mDebugIn.println("q\n");
                mDebugIn.flush();
            }
            catch (RuntimeException e)
            {
                skip = true;
                PerlDebugPlugin.getDefault().logError(
                    "Could not terminate Perl Process", e);
            }

            int count = 0;
            char buf[] = new char[1000];

            if (!skip) do
            {
                try
                {
                    count = mDebugOut.read(buf);
                }
                catch (IOException e)
                {
                    skip = true;
                    break;
                }
            }
            while (count != -1);
        }
        mCurrentSubCommand = mCommandNone;
        mCurrentCommand = mCommandNone;
        mIsCommandRunning = false;
        PerlDebugPlugin.getPerlBreakPointmanager().removeDebugger(this);
        mTarget.debugSessionTerminated();
    }

    public void shutdown()
    {
        abortSession();
    }

    private void commandPostExec(int fExitValue, String fOutput)
    {

        if (fExitValue == COMMAND_FINISHED)
        {
            finishCommand(fOutput);
        }

        if (fExitValue == SESSION_TERMINATED)
        {
            abortCommandThread();
        }
    }

    private boolean isBreakPointReached()
    {
        IPPosition pos = getCurrent_IP_Position();

        PerlBreakpoint bp = mActiveBreakpoints.getBreakpointForLocation(pos
            .getPath(), pos.getLine());

        if (bp != null)
        {
            if (bp instanceof PerlRegExpBreakpoint)
            {
                ((PerlRegExpBreakpoint) (bp)).calculateRegExp();
                final String mRegExp = ((PerlRegExpBreakpoint) (bp)).getRegExp();
                final String mText = ((PerlRegExpBreakpoint) (bp)).getMatchText();
                final boolean mMultiLine = ((PerlRegExpBreakpoint) (bp)).getMultiLine();
                final boolean mIgnoreCase = ((PerlRegExpBreakpoint) (bp)).getIgnoreCase();

                // show view
                Shell shell = PerlDebugPlugin.getActiveWorkbenchShell();
                if (shell != null)
                {
                    shell.getDisplay().syncExec(new Runnable()
                    {
                        public void run()
                        {
                            RegExpView view = null;
                            IWorkbenchPage activePage = PerlDebugPlugin
                                .getWorkbenchWindow().getActivePage();
                            try
                            {
                                view = (RegExpView) activePage
                                    .showView("org.epic.regexp.views.RegExpView");
                            }
                            catch (PartInitException e)
                            {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                            view.setRegExpText(mRegExp);
                            view.setMatchText(mText);
                            view.setMultilineCheckbox(mMultiLine);
                            view.setIgnoreCaseCheckbox(mIgnoreCase);

                        }

                    });

                }
            }
            return true;
        }
        return false;
    }

    public IThread[] getThreads()
    {
        return mThreads;
    }

    private boolean evaluateCommandResult(int fResult, String fOutputString)
    {

        int command;
        if (isSkipEvaluateCommandResult()) return (true);

        if (mCurrentSubCommand == mCommandNone)
        {
            command = mCurrentCommand;
        }
        else
        {
            command = mCurrentSubCommand;
        }

        switch (command)
        {
        case mCommandStepInto:
        case mCommandStepOver:
        case mCommandStepReturn:
        case mCommandResume:
        case mCommandSuspend:
        case mCommandTerminate:
        case mCommandEvaluateCode:
            // updateStackFramesInit(fOutputString);
            break;
        case mCommandClearOutput:
            break;
        case mCommandExecuteCode:
            break;
        default:
            return (false);
        }
        return (true);
    }

    private void updateStackFramesFinish(String fOutputString)
    {
        PerlDebugVar var_new, var_org;
        PerlDebugVar[] orgStackFrameVars, newStackFrameVars;
        try
        {
            if (mThreads[0].getStackFrames() == null) return;
        }
        catch (DebugException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        StackFrame frame = this.mStackFrameOrg;
        try
        {
            if (frame != null
                && ((StackFrame) mThreads[0].getStackFrames()[0]).get_IP_Path()
                    .equals(frame.get_IP_Path()))
            {
                orgStackFrameVars = null;
                newStackFrameVars = null;

                orgStackFrameVars = (PerlDebugVar[]) frame.getVariables();
                newStackFrameVars = (PerlDebugVar[]) mThreads[0]
                    .getStackFrames()[0].getVariables();

                boolean found;
                boolean checkLocals = isRequireCompareLocals(fOutputString);
                for (int new_pos = 0; new_pos < newStackFrameVars.length; ++new_pos)
                {
                    found = false;
                    var_new = newStackFrameVars[new_pos];
                    if (orgStackFrameVars != null)
                    {
                        for (int org_pos = 0; (org_pos < orgStackFrameVars.length)
                            && !found; ++org_pos)
                        {
                            var_org = orgStackFrameVars[org_pos];
                            if (var_new.matches(var_org))
                            {
                                found = true;

                                if (!(var_new.isLocalScope() && !checkLocals))
                                    var_new.calculateChangeFlags(var_org);
                            }
                        }
                        if (!found)
                        {
                            if (!(var_new.isLocalScope() && !checkLocals))
                                var_new.setChangeFlags(
                                    PerlDebugValue.mValueHasChanged, true);
                        }
                    }
                }
            }

        }
        catch (DebugException e1)
        {

            e1.printStackTrace();
        }

    }

    private void updateStackFramesInit(String fOutputString)
    {
        PerlDebugValue val;
        String erg;

        // setVarStrings();
        boolean ret = startSubCommand(mCommandExecuteCode, "T", false);
        if (mDebugSubCommandOutput == null) return;
        erg = mDebugSubCommandOutput.replaceAll("\n", "\r\n");
        if (!ret) return;

        try
        {
            if (mThreads[0].getStackFrames() != null) mStackFrameOrg = (StackFrame) mThreads[0]
                .getStackFrames()[0];
            else mStackFrameOrg = null;
        }
        catch (DebugException e1)
        {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        REMatch[] matches = mReStackTrace.getAllMatches(erg);
        StackFrame[] frames = new StackFrame[matches.length + 1];
        frames[0] = new StackFrame(mThreads[0]);
        setCurrent_IP_Position(frames[0]);
        for (int pos = 0; pos < matches.length; ++pos)
        {
            PerlDebugVar[] vars = new PerlDebugVar[2];

            vars[0] = new PerlDebugVar(mThreads[0],
                PerlDebugVar.IS_GLOBAL_SCOPE, true);
            vars[1] = new PerlDebugVar(mThreads[0],
                PerlDebugVar.IS_GLOBAL_SCOPE, true);
            vars[0].setName("Called Function");
            val = new PerlDebugValue(mThreads[0]);
            val.setValue(matches[pos].toString(2));
            try
            {

                vars[0].setValue(val);
                vars[1].setName("Return Type");
                val = new PerlDebugValue(mThreads[0]);
                String retType;
                retType = matches[pos].toString(1);
                if (retType.equals(".")) retType = "void";
                if (retType.equals("@")) retType = "list";
                if (retType.equals("$")) retType = "scalar";
                val.setValue(retType);
                vars[1].setValue(val);

                frames[pos + 1] = new StackFrame(mThreads[0]);
                frames[pos + 1].set_IP_Line(Integer.parseInt(matches[pos]
                    .toString(4)));
                frames[pos + 1]
                    .set_IP_Path(getPathFor(matches[pos].toString(3)));
                frames[pos + 1].setVariables(vars);
            }
            catch (Exception e)
            {
                System.out.println(e);
            }
        }
        mThreads[0].setStackFrames(frames);

        if (mVarUpdateJob != null) try
        {
            mVarUpdateJob.join();
        }
        catch (InterruptedException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        mStopVarUpdate = false;
        mIsCommandRunning = false;
        mVarUpdateJob = new VarUpdateJob("Retrieving Variables", fOutputString);
        mVarUpdateJob.setPriority(Job.SHORT);
        mVarUpdateJob.schedule();
        ;

    }

    private IPPosition getCurrent_IP_Position()
    {
        int line;
        IPath file;
        IPPosition pos;
        String file_name;
        startSubCommand(mCommandExecuteCode, ".", false);
        REMatch temp;

        // mRe_IP_Pos_CODE handles locations like
        //     main::CODE(0x814f960)(/some/path/trycatch.pl:7):
        // mRe_IP_Pos handles locations like
        //     main::(/some/path/foobar.pl:7):
        // mRe_IP_Pos_Eval handles locations like
        //
        
        REMatch result = mRe_IP_Pos_CODE.getMatch(mDebugSubCommandOutput);
        if (result == null)
        {
            result = mRe_IP_Pos.getMatch(mDebugSubCommandOutput);
            file_name = result.toString(1);
            temp = mRe_IP_Pos_Eval.getMatch(file_name);
            if (temp != null) result = temp;
        }
        line = Integer.parseInt(result.toString(2));
        file = getPathFor(result.toString(1));

        pos = new IPPosition();
        pos.setLine(line);
        pos.setPath(file);
        return (pos);

    }

    IPath getPathFor(String fFilename)
    {

        IPath file = new Path(fFilename);
        if (!file.isAbsolute())
        {
            file = mWorkingDir.append(file);
        }
        else if (mPathMapper != null)
        {
            file = mPathMapper.mapPath(file);
        }
        return (file);
    }

    private void setCurrent_IP_Position(StackFrame fFrame)
    {
        IPPosition pos;
        pos = getCurrent_IP_Position();
        fFrame.set_IP_Line(pos.getLine());
        fFrame.set_IP_Path(pos.getPath());

    }

    private void setVarStrings(IProgressMonitor fMon)
    {
        String command;
        String result;
        boolean ret;

        command = "o frame=0\n";
        if (mPerlVersion.startsWith("5.6"))
        {
            command = "O frame=0\n";
        }
        ret = startCommand(mCommandExecuteCode, command, false, mThreads[0]);
        if (!ret || this.mStopVarUpdate) return;
        if (ShowLocalVariableActionDelegate.getPreferenceValue())
        {
            result = evaluateStatement(mThreads[0], mLovalVarCommand, false);
            // startSubCommand(mCommandExecuteCode, command_local, false);
            if (result != null)
            {
                if (result.startsWith(mPadwalkerError))
                {
                    PerlDebugPlugin
                        .errorDialog("***Error displaying Local Variables****\nInstall PadWalker on your Perl system or disable displaying of local variables");
                    mLocalVarsAvailable = false;
                }
                else
                {
                    mVarLocalString = result;
                }
            }
        }
        fMon.worked(40);
        Thread.yield();

        ret = startCommand(mCommandExecuteCode, command, false, mThreads);
        fMon.worked(50);
        Thread.yield();
        if (!ret || this.mStopVarUpdate) return;
        result = evaluateStatement(mThreads[0], mGlobalVarCommand, false);
        fMon.worked(70);
        Thread.yield();

        command = "o frame=2\n";
        if (mPerlVersion.startsWith("5.6")) command = "O frame=2\n";

        mVarGlobalString = result;
    }

    private void setVarList(StackFrame fFrame)
    {
        ArrayList lVarList = null;

        if (fFrame == null) return;
        if (mStopVarUpdate == true) return;

        if (mVarLocalString != null)
            lVarList = mVarParser.parseVars(mVarLocalString,
                PerlDebugVar.IS_LOCAL_SCOPE);
        if (mStopVarUpdate == true)

        if (mStopVarUpdate == true)
        {
            return;
        }

        if (lVarList != null) mVarParser.parseVars(mVarGlobalString,
            PerlDebugVar.IS_GLOBAL_SCOPE, lVarList);
        else lVarList = mVarParser.parseVars(mVarGlobalString,
            PerlDebugVar.IS_GLOBAL_SCOPE);

        if (mStopVarUpdate == true) if (mStopVarUpdate == true)
        {
            return;
        }

        try
        {
            // removeUnwantedVars(lVarList);
            fFrame.setVariables(lVarList);
        }
        catch (DebugException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    private boolean isStepCommand(int fCommand)
    {
        return ((fCommand & mIsStepCommand) > 0);
    }

    private boolean isRunCommand(int fCommand)
    {
        return ((fCommand & mIsRunCommand) > 0);
    }

    private boolean isSubCommand()
    {
        return (mCurrentSubCommand != mCommandNone);
    }

    private boolean isSkipEvaluateCommandResult()
    {
        return (isSubCommand() && ((mCurrentSubCommand & mCommandModifierSkipEvaluateCommandResult) > 0));
    }

    private int maskCommandModifiers(int fCommand)
    {
        return (fCommand & (mCommandModifierRangeStart - 1));
    }

    public boolean addBreakpoint(PerlBreakpoint fBp)
    {
        return (addBreakpoint(fBp, false));
    }

    public boolean addBreakpoint(PerlBreakpoint fBp, boolean fIsPending)
    {
        boolean isValid;
        isValid = setBreakpoint(fBp, fIsPending);
        if (!isValid) fBp.setIsNoValidBreakpointPosition(true);
        return (isValid);
    }

    public String getPerlDbPath(IPath fPath)
    {
        int match;
        IPath path;

        if (!mWorkingDir.isPrefixOf(fPath)) return (fPath.toString());

        match = mWorkingDir.matchingFirstSegments(fPath);
        path = fPath.removeFirstSegments(match).makeRelative().setDevice(null);
        return (path.toString());
    }

    boolean switchToFile(PerlBreakpoint fBp)
    {
        String path, command;

        path = getPerlDbPath(fBp.getResourcePath());
        // path = path.replaceAll("\\","/");
        command = "f " + path + "\n";
        startSubCommand(mCommandExecuteCode, command, false);
        if (mReSwitchFileFail.getAllMatches(mDebugSubCommandOutput).length > 0) return false;
        else return true;
    }

    boolean startSetLoadBreakpointCommand(PerlBreakpoint fBp)
    {
        String path, command;

        path = getPerlDbPath(fBp.getResourcePath());
        // path = path.replaceAll("\\","/");
        command = "b load " + path;

        startSubCommand(mCommandExecuteCode, command, false);
        return true;

    }

    boolean startSetLineBreakpointCommand(PerlLineBreakpoint fBp)
    {
        String line, command;

        line = Integer.toString(fBp.getLineNumber());
        command = "b " + line;

        startSubCommand(mCommandExecuteCode, command, false);
        if (mReSetLineBreakpoint.getAllMatches(mDebugSubCommandOutput).length > 0) return true;
        else return false;
    }

    private boolean setBreakpoint(PerlBreakpoint fBp, boolean fIsPending)
    {
        boolean erg;

        if (!fIsPending)
        {
            erg = switchToFile(fBp);
            if (!erg)
            {
                mPendingBreakpoints.add(fBp);
                startSetLoadBreakpointCommand(fBp);
                return (true);
            }
        }

        if (!(fBp instanceof PerlLineBreakpoint)) return (false);

        erg = startSetLineBreakpointCommand(((PerlLineBreakpoint) fBp));

        if (erg)
        {
            mActiveBreakpoints.add(fBp);
            fBp.addInstallation(this);
        }

        return (erg);

    }

    public void removeBreakpoint(PerlBreakpoint fBp)
    {
        String line, command;
        if (mPendingBreakpoints.remove(fBp)) return;

        if (!(fBp instanceof PerlLineBreakpoint)) return;

        switchToFile(fBp);

        line = Integer.toString(((PerlLineBreakpoint) fBp).getLineNumber());
        if (this.mPerlVersion.startsWith("5.6")) command = "d ";
        else command = "B ";

        command = command + line;
        startSubCommand(mCommandExecuteCode, command, false);
    }

    private boolean insertPendingBreakpoints()
    {
        IPPosition pos;
        Set bps;
        boolean erg;
        PerlBreakpoint bp;

        pos = getCurrent_IP_Position();
        bps = mPendingBreakpoints.getBreakpointsForFile(pos.getPath());
        if (bps == null || bps.size() == 0) return false;

        for (Iterator i = bps.iterator(); i.hasNext();)
        {
            bp = ((PerlBreakpoint) i.next());
            erg = addBreakpoint(bp, true);
            if (!erg) bp.setIsNoValidBreakpointPosition(true);
        }

        bps.clear();

        return (true);
    }

    private boolean isRequireCompareLocals(String fOutputString)
    {
        if (fOutputString == null) return (false);
        StringTokenizer lines = new StringTokenizer(fOutputString, "\r\n");
        int level = 0;
        String line;

        while (lines.hasMoreTokens())
        {
            line = (String) lines.nextToken();

            if (mReExitFrame.getAllMatches(line).length > 0) level--;
            else if (mReEnterFrame.getAllMatches(line).length > 0) level++;

            if (level < 0) return (false);
        }

        if (level != 0) return (false);

        return (true);

    }

    public void redirectIO(int fPort)
    {
        String ip = null;

        try
        {
            ip = InetAddress.getLocalHost().getHostAddress();
        }
        catch (UnknownHostException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        String command = "require IO::Socket; {my $OUT;"
            + "$OUT = new IO::Socket::INET(" + "Timeout  => \'10\',"
            + "PeerAddr => \'" + ip + ":" + fPort + "\',"
            + "Proto    => 'tcp',);" + "STDOUT->fdopen($OUT,\"w\");"
            + "STDIN->fdopen($OUT,\"r\");}";

        startCommand(mCommandExecuteCode, command, false, this);

    }

    public void getRemoteInc(List fErg)
    {

        String erg;
        erg = evaluateStatement(mThreads[0],
            ";{foreach $t(@INC) {print $DB::OUT $t.\"\\n\";}}", false);

        StringTokenizer s = new StringTokenizer(erg, "\r\n");

        String token;
        int count = s.countTokens();

        for (int x = 0; x < count; ++x)
        {
            token = s.nextToken();
            fErg.add(token);
        }
    }

    public void redirectError(int fPort)
    {
        String ip = null;

        try
        {
            ip = InetAddress.getLocalHost().getHostAddress();
        }
        catch (UnknownHostException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        String command = "require IO::Socket; {my $OUT;"
            + "$OUT = new IO::Socket::INET(" + "Timeout  => \'10\',"
            + "PeerAddr => \'" + ip + ":" + fPort + "\',"
            + "Proto    => 'tcp',);" + "STDERR->fdopen($OUT,\"w\");}";

        startCommand(mCommandExecuteCode, command, false, this);

    }

    public boolean containtsThread(PerlDebugThread fThread)
    {
        return (mThreads[0] == fThread);
    }

    public static void updateVariableView()
    {
        if (ShowLocalVariableActionDelegate.getPreferenceValue()
            && (!mLocalVarsAvailable))
        {

            PerlDebugPlugin
                .errorDialog("***Error displaying Local Variables****\nInstall Padawalker on your Perl system or disable displaying of local variables");
        }

        Set debuggers = PerlDebugPlugin.getPerlBreakPointmanager()
            .getDebugger();
        Iterator iterator = debuggers.iterator();
        PerlDB db;
        while (iterator.hasNext())
        {
            db = (PerlDB) iterator.next();
            try
            {
                ((StackFrame) db.mThreads[0].getStackFrames()[0]).updateVars();
            }
            catch (DebugException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            db.generateDebugEvalEvent();
        }
    }

    public class VarUpdateJob extends UIJob
    {
        private String mString;

        public VarUpdateJob(String fName, String fString)
        {
            super(fName);
            mString = fString;
        }

        public IStatus runInUIThread(IProgressMonitor fMon)
        {
            this.getThread().setPriority(Thread.MIN_PRIORITY);
            fMon.beginTask("Vars", 100);
            Thread.yield();
            StackFrame frame = null;

            try
            {
                for (int x = 0; (x < 2) && !mStopVarUpdate; x++)
                    Thread.sleep(100);
            }
            catch (InterruptedException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
                fMon.worked(10);
                Thread.yield();
            }

            if (mStopVarUpdate == true)
            {
                return (Status.OK_STATUS );
            }

            setVarStrings(fMon);
            try
            {
                if (mThreads[0].getStackFrames() == null)
                    return (Status.OK_STATUS );
                frame = (StackFrame) mThreads[0].getStackFrames()[0];
            }
            catch (DebugException e1)
            {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }

            if (mStopVarUpdate == true)
            {
                return (Status.OK_STATUS );
            }
            setVarList(frame);
            fMon.worked(80);
            Thread.yield();

            if (mStopVarUpdate == true)
            {
                return (Status.OK_STATUS );
            }
            updateStackFramesFinish(mString);
            fMon.worked(90);
            Thread.yield();

            if (mStopVarUpdate == true) return (Status.OK_STATUS );
            generateDebugEvalEvent();
            fMon.done();
            return (Status.OK_STATUS );
        }
    }

    private RE newRE(String re, boolean multiline)
    {
        try
        {
            return new RE(
                re,
                multiline ? RE.REG_MULTILINE : 0,
                RESyntax.RE_SYNTAX_PERL5);
        }
        catch (REException e)
        {
            // we have a bug in PerlDB's constructor
            throw new RuntimeException(e);
        }
    }

    private class CommandThread extends Thread
    {
        public CommandThread()
        {
            super("EPIC-PerlDB.CommandThread");
        }

        public void run()
        {
            waitForCommandToFinish();
        }
    }

    /**
     * Stores position of the instruction pointer.
     */
    private static class IPPosition
    {
        private int line;
        private IPath path;

        public boolean equals(IPPosition fPos)
        {
            return
                path.equals(fPos.getPath()) &&
                line == fPos.getLine();
        }

        public int getLine()
        {
            return line;
        }

        public IPath getPath()
        {
            return path;
        }

        public int hashCode()
        {
            return path.hashCode() * 37 + line;
        }

        public void setLine(int i)
        {
            line = i;
        }

        public void setPath(IPath path)
        {
            this.path = path;
        }
        
        public String toString()
        {
            return path + ":" + line;
        }
    }
}
