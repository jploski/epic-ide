/*
 * Created on 26.04.2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */

package org.epic.debug;

import gnu.regexp.RE;
import gnu.regexp.REException;
import gnu.regexp.REMatch;
import gnu.regexp.RESyntax;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.ITerminate;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.epic.debug.util.PathMapperCygwin;
import org.epic.debug.varparser.PerlDebugValue;
import org.epic.debug.varparser.PerlDebugVar;
import org.epic.debug.varparser.PerlVarParser;
import org.epic.perleditor.PerlEditorPlugin;
import org.epic.regexp.views.RegExpView;

/**
 * @author ruehl
 * 
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class PerlDB implements IDebugElement, ITerminate {

	private static boolean mLocalVarsAvailable = true;
	private boolean mIsSessionTerminated;
	private DebugTarget mTarget;
	private CommandThread mCommandThread;
	private int mCurrentCommand;
	/* NO debugging meassages are created for sub-commands */
	private int mCurrentSubCommand;
	private Object mCurrentCommandDest;

	private final static String EMPTY_STRING = "";
	//private static final String mDBinitPerl = "{$| = 1; my $old = select
	// STDERR; $|=1;select $old;}\n";
	private static final String mDBinitPerl_5_8 = "o frame=2";
	private static final String mDBinitPerl_5_6 = "O frame=2";
	private final static String mPadwalkerError = "PadWalker module not found - please install";
	private static final String mLovalVarCommand_5_6 = ";{eval { require PadWalker; PadWalker->VERSION(0.08) }or print $DB::OUT (\""
			+ mPadwalkerError
			+ "\\n\");do 'dumpvar_epic.pl' unless defined &main::dumpvar_epic;defined &main::dumpvar_epic or print $DB::OUT \"dumpvar_epic.pl not available.\\n\";my $h = eval { PadWalker::peek_my(2) };my @vars = split (' ','');$@ and $@ =~ s/ at .*//, print $DB::OUT ($@);my $savout = select($DB::OUT);dumpvar_epic::dumplex($_,$h->{$_},defined $option{dumpDepth} ? $option{dumpDepth} : -1,@vars) for sort keys %$h;select($savout);};\n";

	private PerlDebugThread[] mThreads;

	final static int mCanResume = 1;
	final static int mCanStepInto = 2;
	final static int mCanStepOver = 4;
	final static int mCanStepReturn = 8;
	final static int mCanSuspend = 16;
	final static int mCanTerminate = 32;

	final static int mCommandNone = 0;
	final static int mCommandStepInto = 1;
	final static int mCommandStepOver = 2;
	final static int mCommandStepReturn = 4;
	final static int mCommandResume = 8;
	final static int mCommandSuspend = 16;
	final static int mCommandTerminate = 32;
	final static int mCommandClearOutput = 64;
	final static int mCommandExecuteCode = 128;
	final static int mCommandEvaluateCode = 256;
	final static int mCommandModifierRangeStart = 1024;
	final static int mCommandModifierSkipEvaluateCommandResult = mCommandModifierRangeStart;

	final static int COMMAND_FINISHED = 1;
	final static int SESSION_TERMINATED = 2;

	private PrintWriter mDebugIn;
	private BufferedReader mDebugOut;
	private String mDebugOutput;
	private String mDebugSubCommandOutput;
	private RE mReCommandFinished1;
	private RE mReCommandFinished2;
	private RE mReSessionFinished1, mReSessionFinished2;
	private RE mRe_IP_Pos;
	private RE mRe_IP_Pos_Eval;
	private RE mReSwitchFileFail;
	private RE mReSetLineBreakpoint;
	private RE mReStackTrace;
	private RE mReEnterFrame;
	private RE mReExitFrame;

	private IP_Position mStartIP;
	private PerlVarParser mVarParser = new PerlVarParser(this);

	private final static int mIsStepCommand = mCommandStepInto
			| mCommandStepOver | mCommandStepReturn;
	private final static int mIsRunCommand = mIsStepCommand | mCommandResume;

	private boolean mIsCommandFinished;
	private boolean mIsCommandRunning;

	private IPath mWorkingDir;

	private BreakpointMap mPendingBreakpoints;
	private BreakpointMap mActiveBreakpoints;

	private final static String mLineSeparator = System
			.getProperty("line.separator");
	private org.epic.debug.util.PathMapper mPathMapper;
	StringBuffer mRegExp = new StringBuffer();
	StringBuffer mText = new StringBuffer();
	private String mPerlVersion;
	private class CommandThread extends Thread {

		public CommandThread() {
		}

		public void run() {
			waitForCommandToFinish();
		}

	}
	public boolean mStopVarUpdate;
	private String mVarLocalString;
	private String mVarGlobalString;
	private VarUpdateThread mVarUpdateThread;
	private StackFrame mStackFrameOrg;
	private class IP_Position {
		int IP_Line;
		IPath IP_Path;

		public boolean equals(IP_Position fPos) {
			if (!IP_Path.equals(fPos.get_IP_Path()))
				return false;

			if (IP_Line != fPos.get_IP_Line())
				return false;

			return (true);
		}

		/**
		 * @return
		 */
		public int get_IP_Line() {
			return IP_Line;
		}

		/**
		 * @return
		 */
		public IPath get_IP_Path() {
			return IP_Path;
		}

		/**
		 * @param i
		 */
		public void set_IP_Line(int i) {
			IP_Line = i;
		}

		/**
		 * @param path
		 */
		public void set_IP_Path(IPath path) {
			IP_Path = path;
		}

	}

	public PerlDB(DebugTarget fTarget) throws InstantiationException {

		IPath path;
		String command;
		mTarget = fTarget;
		mCurrentCommand = mCommandNone;
		mCurrentSubCommand = mCommandNone;

		mIsSessionTerminated = false;
		mStopVarUpdate = false;
		mDebugIn = null;
		mDebugOut = null;
		mDebugOutput = null;

		mDebugSubCommandOutput = null;
		mCurrentCommandDest = null;
		mReCommandFinished1 = null;
		mReSessionFinished1 = null;
		mReSessionFinished2 = null;
		mIsCommandFinished = false;
		mIsCommandRunning = false;

		mPendingBreakpoints = new BreakpointMap();
		mActiveBreakpoints = new BreakpointMap();

		mThreads = new PerlDebugThread[1];
		mThreads[0] = new PerlDebugThread("Main-Thread", fTarget.getLaunch(),
				fTarget, this);

		try {
			mReCommandFinished1 = new RE("\n\\s+DB<\\d+>", 0,
					RESyntax.RE_SYNTAX_PERL5);
			mReCommandFinished2 = new RE("^\\s+DB<\\d+>", 0,
					RESyntax.RE_SYNTAX_PERL5);
			mReSessionFinished1 = new RE("Use `q' to quit or `R' to restart",
					0, RESyntax.RE_SYNTAX_PERL5);
			mReSessionFinished2 = new RE("Debugged program terminated.", 0,
					RESyntax.RE_SYNTAX_PERL5);
			mRe_IP_Pos = new RE("^[^\\(]*\\((.*):(\\d+)\\):[\\n\\t]", 0,
					RESyntax.RE_SYNTAX_PERL5);
			mRe_IP_Pos_Eval = new RE(
					"^[^\\(]*\\(eval\\s+\\d+\\)\\[(.*):(\\d+)\\]$", 0,
					RESyntax.RE_SYNTAX_PERL5);
			mReSwitchFileFail = new RE("^No file", 0, RESyntax.RE_SYNTAX_PERL5);
			mReSetLineBreakpoint = new RE("^\\s+DB<\\d+>", 0,
					RESyntax.RE_SYNTAX_PERL5);
			mReStackTrace = new RE(
					"^(.)\\s+=\\s+(.*)called from .* \\`([^\\']+)\\'\\s*line (\\d+)\\s*$",
					RE.REG_MULTILINE, RESyntax.RE_SYNTAX_PERL5);
			mReEnterFrame = new RE("^\\s*entering", 0, RESyntax.RE_SYNTAX_PERL5);
			mReExitFrame = new RE("^\\s*exited", 0, RESyntax.RE_SYNTAX_PERL5);
		} catch (REException e) {
			new InstantiationException("Couldn't RegEX");
		};

		mWorkingDir = mTarget.getWorkingDir();

		mDebugIn = mTarget.getDebugWriteStream();
		mDebugOut = mTarget.getDebugReadSrream();

		mPathMapper = null;
		String interpreterType = PerlEditorPlugin.getDefault()
				.getPreferenceStore().getString(
						PerlEditorPlugin.INTERPRETER_TYPE_PREFERENCE);

		//		   Check if cygwin is used
		if (interpreterType.equals(PerlEditorPlugin.INTERPRETER_TYPE_CYGWIN)) {
			mPathMapper = new PathMapperCygwin();
		}

		startCommand(mCommandClearOutput, null, false, this);

		if (!isTerminated(this)) {
			String version = getPerlVersion();

			mPerlVersion = "5.8";
			command = mDBinitPerl_5_8;
			if (version.startsWith("5.6.")) {
				mPerlVersion = "5.6";
				command = mDBinitPerl_5_6;
			}
			startCommand(mCommandExecuteCode, command, false, this);
			//		/****************test only*****/
			//		getLaunch().setAttribute(PerlLaunchConfigurationConstants.ATTR_DEBUG_IO_PORT,"4041");
			//		getLaunch().setAttribute(PerlLaunchConfigurationConstants.ATTR_DEBUG_ERROR_PORT,"4042");
			//		DebuggerProxy p = new DebuggerProxy(this, "Proxy");
			//getLaunch().addProcess(p);
			//		mTarget.setProcess(p);
			/** ******************************** */
			PerlDebugPlugin.getPerlBreakPointmanager().addDebugger(this);

			updateStackFramesInit(null);
			generateDebugInitEvent();
		} else
			generateDebugTermEvent();

	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.debug.core.model.IDebugElement#getModelIdentifier()
	 */
	public String getModelIdentifier() {
		return mTarget.getModelIdentifier();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.debug.core.model.IDebugElement#getDebugTarget()
	 */
	public IDebugTarget getDebugTarget() {
		return mTarget;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.debug.core.model.IDebugElement#getLaunch()
	 */
	public ILaunch getLaunch() {
		return mTarget.getLaunch();
	}

	public boolean canResume(Object fDest) {
		return (!mIsCommandRunning && (!mIsSessionTerminated));
	}

	public boolean canSuspend(Object fDest) {
		return (false);
	}

	public boolean isSuspended(Object fDest) {
		return ((!mIsCommandRunning) && (!mIsSessionTerminated));
	}

	public void resume(Object fDest) {
		startCommand(mCommandResume, fDest);
	}

	public void suspend(Object fDest) {
		startCommand(mCommandSuspend, fDest);
	}

	public boolean canStepInto(Object fDest) {
		return (isSuspended(fDest));
	}

	public boolean canStepOver(Object fDest) {
		return (isSuspended(fDest));
	}

	public boolean canStepReturn(Object fDest) {
		return (isSuspended(fDest));
	}

	public boolean isStepping(Object fDest) {
		return (((mCurrentCommand & mIsStepCommand) != 0) && mIsCommandRunning);
	}

	public void stepInto(Object fDest) {
		startCommand(mCommandStepInto, fDest);

	}

	public void stepOver(Object fDest) {
		startCommand(mCommandStepOver, fDest);
	}

	public void stepReturn(Object fDest) {
		startCommand(mCommandStepReturn, fDest);
	}

	public boolean canTerminate() {
		return canTerminate(null);
	}

	public boolean canTerminate(Object fDest) {
		return !isTerminated();
	}

	public boolean isTerminated() {
		return isTerminated(null);
	}
	public boolean isTerminated(Object fDest) {
		return (mIsSessionTerminated);
	}

	public void terminate() {
		terminate(null);
	}

	public void terminate(Object fDest) {
		abortSession();

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	public Object getAdapter(Class adapter) {
		if (adapter == this.getClass())
			return this;
		else
			return null;
	}

	public boolean startCommand(int fCommand, Object fThread) {
		return (startCommand(fCommand, null, true, fThread));
	}

	public String getPerlVersion() {
		return (evaluateStatement(mThreads[0],
				"printf $DB::OUT \"%vd\", $^V;\n", false));

	}

	public String evaluateStatement(Object fThread, String fText) {
		return evaluateStatement(fThread, fText, true);
	}

	synchronized public String evaluateStatement(Object fThread, String fText,
			boolean fUpdateVars) {

		String res;
		int command = mCommandEvaluateCode;

		if (!fUpdateVars)
			command = mCommandExecuteCode;

		//		if (mIsCommandRunning) {
		//			startSubCommand(command, fText, false);
		//			res = mDebugSubCommandOutput;
		//		} else {
		boolean erg = startCommand(command, fText, false, fThread);
		if (!erg)
			return (null);
		res = mDebugOutput;
		//	}

		if (res == null)
			return (null);
		int index_n = res.lastIndexOf("\n");
		int index_r = res.lastIndexOf("\r");

		if (res == null || ((index_n <= 0) && (index_r <= 0))) {
			return null;
		}

		int index;

		if (index_n > 0 && !(index_r > 0 && index_r < index_n))
			index = index_n;
		else
			index = index_r;

		String result = res.substring(0, index);
		return (result);
	}
	synchronized public boolean startCommand(int fCommand, String fCode,
			boolean fSpawn, Object fThread) {
		if (mIsCommandRunning)
			return (false);
		mCurrentCommandDest = fThread;
		mDebugOutput = null;
		mDebugSubCommandOutput = null;
		mCurrentCommand = fCommand;
		mCurrentSubCommand = mCommandNone;
		mIsCommandRunning = true;
		mIsCommandFinished = false;

		if (isStepCommand(fCommand)) {
			mStopVarUpdate = true;
			System.err.println("!!!!Stop");
		}
		return (startPerlDebugCommand(fCode, fSpawn));
	}

	public boolean startSubCommand(int fCommand) {
		return (startSubCommand(fCommand, null, true));
	}

	private boolean startSubCommand(int fCommand, String fCode, boolean fSpawn) {
		mDebugSubCommandOutput = null;
		mCurrentSubCommand = fCommand;
		return (startPerlDebugCommand(fCode, fSpawn));
	}

	private boolean startPerlDebugCommand(String fCode, boolean fSpawn) {
		int command;
		boolean isSubCommand;

		if (!isSubCommand()) {
			command = mCurrentCommand;
			isSubCommand = false;
		} else {
			command = mCurrentSubCommand;
			isSubCommand = true;
		}

		command = maskCommandModifiers(command);
		if (isStepCommand(command) && !isSubCommand())
			mStartIP = getCurrent_IP_Position();

		switch (command) {
			case mCommandStepInto :
				mDebugIn.println("s\n");
				break;

			case mCommandStepOver :
				mDebugIn.println("n\n");
				break;
			case mCommandStepReturn :
				mDebugIn.println("r\n");
				break;
			case mCommandResume :
				mDebugIn.println("c\n");
				break;
			case mCommandSuspend :
				break;
			case mCommandTerminate :
				break;
			case mCommandClearOutput :
				break;
			case mCommandExecuteCode :
			case mCommandEvaluateCode :
				mDebugIn.println(fCode + "\n");
				break;
			default :
				return (false);
		}

		generateDebugEvent(command, true, mTarget);

		if (fSpawn) {
			mCommandThread = new CommandThread();
			mCommandThread.start();
			return (true);
		}

		return (waitForCommandToFinish());
	}

	void generateDebugEvent(int fCommand, boolean fStart, Object fCommandDest) {
		DebugEvent event = null;
		int stepEventKind;
		int stepEventEndDetail;

		if (isSubCommand())
			return;

		if (((fCommand & mIsStepCommand) != 0) && !fStart) {
			if (isBreakPointReached())
				stepEventEndDetail = DebugEvent.BREAKPOINT;
			else
				stepEventEndDetail = DebugEvent.STEP_END;

			event = new DebugEvent(fCommandDest, DebugEvent.SUSPEND,
					stepEventEndDetail);
		} else {
			switch (fCommand) {
				case mCommandStepInto :
					event = new DebugEvent(fCommandDest, DebugEvent.RESUME,
							DebugEvent.STEP_INTO);
					break;

				case mCommandStepOver :
					event = new DebugEvent(fCommandDest, DebugEvent.RESUME,
							DebugEvent.STEP_INTO);
					break;

				case mCommandStepReturn :
					event = new DebugEvent(fCommandDest, DebugEvent.RESUME,
							DebugEvent.STEP_RETURN);
					break;

				case mCommandResume :
					if (fStart)
						event = new DebugEvent(fCommandDest, DebugEvent.RESUME,
								DebugEvent.CLIENT_REQUEST);
					else
						event = new DebugEvent(fCommandDest,
								DebugEvent.SUSPEND, DebugEvent.BREAKPOINT);
					break;

				case mCommandSuspend :
					if (!fStart)
						event = new DebugEvent(fCommandDest,
								DebugEvent.SUSPEND, DebugEvent.CLIENT_REQUEST);
					break;

				case mCommandTerminate :
					if (!fStart)
						event = new DebugEvent(fCommandDest,
								DebugEvent.TERMINATE);
					break;

				case mCommandEvaluateCode :
					if (fStart)
						event = new DebugEvent(fCommandDest, DebugEvent.RESUME,
								DebugEvent.CLIENT_REQUEST);
					else
						event = new DebugEvent(fCommandDest,
								DebugEvent.SUSPEND, DebugEvent.BREAKPOINT);
					break;
			}
		}
		if (event != null) {
			DebugEvent debugEvents[] = new DebugEvent[1];
			debugEvents[0] = event;
			DebugPlugin.getDefault().fireDebugEventSet(debugEvents);
		}
	}

	public void generateDebugInitEvent() {
		DebugEvent event = null;

		event = new DebugEvent(mThreads[0], DebugEvent.SUSPEND,
				DebugEvent.STEP_END);
		DebugEvent debugEvents[] = new DebugEvent[1];
		debugEvents[0] = event;
		DebugPlugin.getDefault().fireDebugEventSet(debugEvents);
	}

	public void generateDebugEvalEvent() {
		DebugEvent event = null;

		event = new DebugEvent(mThreads[0], DebugEvent.SUSPEND,
				DebugEvent.EVALUATION);
		DebugEvent debugEvents[] = new DebugEvent[1];
		debugEvents[0] = event;
		DebugPlugin.getDefault().fireDebugEventSet(debugEvents);
	}
	public void generateDebugTermEvent() {
		DebugEvent event = null;

		event = new DebugEvent(mThreads[0], DebugEvent.TERMINATE,
				DebugEvent.STEP_END);
		DebugEvent debugEvents[] = new DebugEvent[1];
		debugEvents[0] = event;
		DebugPlugin.getDefault().fireDebugEventSet(debugEvents);
	}

	public void generateTargetTermEvent() {
		DebugEvent event = null;

		event = new DebugEvent(mTarget, DebugEvent.TERMINATE,
				DebugEvent.STEP_END);
		DebugEvent debugEvents[] = new DebugEvent[1];
		debugEvents[0] = event;
		DebugPlugin.getDefault().fireDebugEventSet(debugEvents);
	}

	private boolean waitForCommandToFinish() {
		char[] buf = new char[1024];
		int count;
		int finished;
		StringBuffer debugOutput = new StringBuffer();
		String currentOutput;
		boolean ok;

		if (isTerminated(mCurrentCommandDest))
			return (false);

		System.out.println("---Waiting for Command (" + mCurrentCommand + "--"
				+ mCurrentSubCommand
				+ ") to finish----------------------------");

		while (true) {
			count = -1;
			try {
				count = mDebugOut.read(buf);
			} catch (IOException e) {
				abortSession();
				throw new RuntimeException(
						"Terminating Debug Session due to IO-Error !");
			}

			if (count > 0)
				debugOutput.append(buf, 0, count);
			currentOutput = debugOutput.toString();

			System.out.println("\nCurrent DEBUGOUTPUT:\n" + currentOutput
					+ "\n");
			if (count == -1 || hasSessionTerminated(currentOutput)) {
				finished = SESSION_TERMINATED;
				break;
			} else if (hasCommandTerminated(currentOutput)) {
				finished = COMMAND_FINISHED;
				break;
			} else if (count < 0) {
				finished = SESSION_TERMINATED;
				break;
			}

		}

		//	System.out.println(currentOutput);

		if (finished == SESSION_TERMINATED) {
			abortSession();
			return (false);
		}
		System.out.println("!!!!!!!!!!!!!!!!!!Command (" + mCurrentCommand
				+ "--" + mCurrentSubCommand
				+ ") finished!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		if (isStepCommand(mCurrentCommand) && !isSubCommand()) {
			IP_Position endIP = getCurrent_IP_Position();
			while ((finished != SESSION_TERMINATED) && mStartIP.equals(endIP)) {
				startSubCommand(mCurrentCommand
						| mCommandModifierSkipEvaluateCommandResult, null,
						false);
				endIP = getCurrent_IP_Position();
			}
			currentOutput = debugOutput.toString();

		}

		if (isRunCommand(mCurrentCommand)
				&& (mCurrentCommand != mCommandStepInto) && !isSubCommand()) {

			while ((finished != SESSION_TERMINATED) && !isBreakPointReached()
					&& !isRunCommand(mCurrentCommand)) {
				insertPendingBreakpoints();
				startSubCommand(mCurrentCommand
						| mCommandModifierSkipEvaluateCommandResult, null,
						false);
			}
			currentOutput = debugOutput.toString();

		}
		ok = evaluateCommandResult(finished, currentOutput);
		commandPostExec(finished, currentOutput);
		mCurrentSubCommand = mCommandNone;
		return (ok);
	}

	private boolean hasCommandTerminated(String fOutput) {
		boolean erg;
		int count;

		erg = mReCommandFinished1.isMatch(fOutput);
		count = mReCommandFinished1.getAllMatches(fOutput).length;
		if (erg || (count > 0))
			return (true);

		erg = mReCommandFinished2.isMatch(fOutput);
		count = mReCommandFinished2.getAllMatches(fOutput).length;
		return (erg || (count > 0));
	}

	private boolean hasSessionTerminated(String fOutput) {
		boolean erg;
		int count;

		erg = mReSessionFinished1.isMatch(fOutput);
		count = mReSessionFinished1.getAllMatches(fOutput).length;
		if (erg || (count > 0))
			return (true);

		erg = mReSessionFinished2.isMatch(fOutput);
		count = mReSessionFinished2.getAllMatches(fOutput).length;
		if (erg || (count > 0))
			return (true);

		return (false);

	}

	private void finishCommand(String fOutput) {
		System.out.println("############Cleanup Command (" + mCurrentCommand
				+ "--" + mCurrentSubCommand + ")");
		if (mCurrentSubCommand == mCommandNone) {

			switch (mCurrentCommand) {
				case mCommandStepInto :
				case mCommandStepOver :
				case mCommandStepReturn :
				case mCommandResume :
				case mCommandSuspend :
				case mCommandTerminate :
				case mCommandEvaluateCode :
					updateStackFramesInit(fOutput);
					break;
				default :
					mIsCommandRunning = false;
					mIsCommandFinished = true;
					break;
			}
			generateDebugEvent(PerlDB.this.mCurrentCommand, false,
					mCurrentCommandDest);
			mDebugOutput = fOutput;
		} else
			mDebugSubCommandOutput = fOutput;
		System.out.println("############State isrunning " + mIsCommandRunning
				+ " isfinished " + mIsCommandFinished + "\n");
	}

	private void abortCommandThread() {
		abortSession();
		PerlDB.this
				.generateDebugEvent(PerlDB.mCommandTerminate, false, mTarget);

	}

	private void abortSession() {
		mStopVarUpdate = true;
		mIsSessionTerminated = true;
		mCurrentSubCommand = mCommandNone;
		mCurrentCommand = mCommandNone;
		mIsCommandRunning = false;
		mIsCommandFinished = false;

		boolean finished = false;
		boolean skip = false;

		try {
			mCurrentCommandDest = mThreads[0];
			mDebugIn.println("q\n");
			mDebugIn.flush();
		} catch (RuntimeException e) {
			skip = true;
			PerlDebugPlugin.getDefault().logError(
					"Could not terminate Perl Process", e);
		}

		int count = 0;
		//	StringBuffer debugOutput=new StringBuffer();
		char buf[] = new char[1000];

		if (!skip)
			do {
				try {
					count = mDebugOut.read(buf);
					//		  System.out.println("Count: "+count+"\n");
				} catch (IOException e) {
					skip = true;
					break;
					//				PerlDebugPlugin.getDefault().logError(
					//								"Test: Could not terminate Perl Process",
					//								e);
				}

				//	if (count > 0)
				//		debugOutput.append(buf, 0, count);

			} while (count != -1);
		//		System.out.println("\n***************EXIT
		// DB-****************\n"+debugOutput.toString());
		//					try
		//					{
		//						this.mDebugOut.read();
		//					} catch (IOException e1)
		//					{
		//						finished = true
		//					}
		//				}

		//	}
		//	startCommand(mCommandClearOutput, null, false, this);
		//	startCommand(mCommandExecuteCode, "q\n", false, this);

		mCurrentSubCommand = mCommandNone;
		mCurrentCommand = mCommandNone;
		mIsCommandRunning = false;
		mIsCommandFinished = false;

		//	generateDebugTermEvent();
		PerlDebugPlugin.getPerlBreakPointmanager().removeDebugger(this);
		mTarget.debugSessionTerminated();
	}

	public void shutdown() {
		abortSession();
	}

	private void commandPostExec(int fExitValue, String fOutput) {

		if (fExitValue == COMMAND_FINISHED) {
			finishCommand(fOutput);
		}

		if (fExitValue == SESSION_TERMINATED) {
			abortCommandThread();
		}
	}

	private boolean isBreakPointReached() {
		IP_Position pos = getCurrent_IP_Position();

		PerlBreakpoint bp = mActiveBreakpoints.getBreakpointForLocation(pos
				.get_IP_Path(), pos.get_IP_Line());

		if (bp != null) {
			if (bp instanceof PerlRegExpBreakpoint) {
				mRegExp = new StringBuffer();
				mText = new StringBuffer();

				this.getRegExp(pos, mRegExp, mText);
				// show view
				Shell shell = PerlDebugPlugin.getActiveWorkbenchShell();
				if (shell != null) {
					shell.getDisplay().syncExec(new Runnable() {
						public void run() {
							RegExpView view = null;
							IWorkbenchPage activePage = PerlDebugPlugin
									.getWorkbenchWindow().getActivePage();
							try {
								view = (RegExpView) activePage
										.showView("org.epic.regexp.views.RegExpView");
							} catch (PartInitException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							view.setRegExpText(mRegExp.toString());
							view.setMatchText(mText.toString());

						}

					});

				}
			}
			return true;
		}
		return false;
	}

	public IThread[] getThreads() {
		return mThreads;
	}

	private boolean evaluateCommandResult(int fResult, String fOutputString) {

		int command;
		boolean isSubCommand;

		if (isSkipEvaluateCommandResult())
			return (true);

		if (mCurrentSubCommand == mCommandNone) {
			command = mCurrentCommand;
			isSubCommand = false;
		} else {
			command = mCurrentSubCommand;
			isSubCommand = true;
		}

		switch (command) {
			case mCommandStepInto :
			case mCommandStepOver :
			case mCommandStepReturn :
			case mCommandResume :
			case mCommandSuspend :
			case mCommandTerminate :
			case mCommandEvaluateCode :
				//	updateStackFramesInit(fOutputString);
				break;
			case mCommandClearOutput :
				break;
			case mCommandExecuteCode :
				break;
			default :
				return (false);
		}
		return (true);
	}

	private void updateStackFramesFinish(String fOutputString) {
		PerlDebugValue val;

		PerlDebugVar var_new, var_org;
		PerlDebugVar[] orgStackFrameVars, newStackFrameVars;
		try {
			if (mThreads[0].getStackFrames() == null)
				return;
		} catch (DebugException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		StackFrame frame = this.mStackFrameOrg;
		try {
			if (frame != null
					&& ((StackFrame) mThreads[0].getStackFrames()[0])
							.get_IP_Path().equals(frame.get_IP_Path())) {
				orgStackFrameVars = null;
				newStackFrameVars = null;

				orgStackFrameVars = (PerlDebugVar[]) frame.getVariables();
				newStackFrameVars = (PerlDebugVar[]) mThreads[0]
						.getStackFrames()[0].getVariables();

				boolean found;
				boolean checkLocals = isRequireCompareLocals(fOutputString);
				for (int new_pos = 0; new_pos < newStackFrameVars.length; ++new_pos) {
					found = false;
					var_new = newStackFrameVars[new_pos];
					if (orgStackFrameVars != null)
						for (int org_pos = 0; (org_pos < orgStackFrameVars.length)
								&& !found; ++org_pos) {
							var_org = orgStackFrameVars[org_pos];
							if (var_new.matches(var_org)) {
								found = true;

								if (!(var_new.isLocalScope() && !checkLocals))
									var_new.calculateChangeFlags(var_org);
							}
						}
					if (!found) {
						if (!(var_new.isLocalScope() && !checkLocals))
							var_new.setChangeFlags(
									PerlDebugValue.mValueHasChanged, true);
					}
				}
			}

		} catch (DebugException e1) {

			e1.printStackTrace();
		}

	}

	private void updateStackFramesInit(String fOutputString) {
		PerlDebugValue val;
		String erg;

		//setVarStrings();
		boolean ret = startSubCommand(mCommandExecuteCode, "T", false);
		if (mDebugSubCommandOutput == null)
			return;
		erg = mDebugSubCommandOutput.replaceAll("\n", "\r\n");
		if (!ret)
			return;

		try {
			if (mThreads[0].getStackFrames() != null)
				mStackFrameOrg = (StackFrame) mThreads[0].getStackFrames()[0];
			else
				mStackFrameOrg = null;
		} catch (DebugException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		REMatch[] matches = mReStackTrace.getAllMatches(erg);
		StackFrame[] frames = new StackFrame[matches.length + 1];
		frames[0] = new StackFrame(mThreads[0]);
		setCurrent_IP_Position(frames[0]);
		for (int pos = 0; pos < matches.length; ++pos) {
			PerlDebugVar[] vars = new PerlDebugVar[2];

			vars[0] = new PerlDebugVar(mThreads[0],
					PerlDebugVar.IS_GLOBAL_SCOPE, true);
			vars[1] = new PerlDebugVar(mThreads[0],
					PerlDebugVar.IS_GLOBAL_SCOPE, true);
			vars[0].setName("Called Function");
			val = new PerlDebugValue(mThreads[0]);
			val.setValue(matches[pos].toString(2));
			try {

				vars[0].setValue(val);
				vars[1].setName("Return Type");
				val = new PerlDebugValue(mThreads[0]);
				String retType;
				retType = matches[pos].toString(1);
				if (retType.equals("."))
					retType = "void";
				if (retType.equals("@"))
					retType = "list";
				if (retType.equals("$"))
					retType = "scalar";
				val.setValue(retType);
				vars[1].setValue(val);

				frames[pos + 1] = new StackFrame(mThreads[0]);
				frames[pos + 1].set_IP_Line(Integer.parseInt(matches[pos]
						.toString(4)));
				frames[pos + 1]
						.set_IP_Path(getPathFor(matches[pos].toString(3)));
				frames[pos + 1].setVariables(vars);
			} catch (Exception e) {
				System.out.println(e);
			}
		}
		mThreads[0].setStackFrames(frames);

		mIsCommandFinished = true;
		if (mVarUpdateThread != null)
			try {
				mVarUpdateThread.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			mIsCommandRunning = false;
			mIsCommandFinished = true;

		mVarUpdateThread = new VarUpdateThread(fOutputString);
		mVarUpdateThread.setPriority(Thread.MIN_PRIORITY);
		mVarUpdateThread.start();

	}
	private IP_Position getCurrent_IP_Position() {
		int line;
		IPath file;
		IP_Position pos;
		String file_name;
		startSubCommand(mCommandExecuteCode, ".", false);
		REMatch temp;

		REMatch result = mRe_IP_Pos.getMatch(mDebugSubCommandOutput);
		file_name = result.toString(1);
		temp = mRe_IP_Pos_Eval.getMatch(file_name);
		if (temp != null)
			result = temp;
		line = Integer.parseInt(result.toString(2));
		file = getPathFor(result.toString(1));

		pos = new IP_Position();
		pos.set_IP_Line(line);
		pos.set_IP_Path(file);
		return (pos);

	}

	IPath getPathFor(String fFilename) {

		IPath file = new Path(fFilename);
		if (!file.isAbsolute()) {
			file = mWorkingDir.append(file);
		} else if (mPathMapper != null) {
			file = mPathMapper.mapPath(file);
		}
		return (file);
	}
	private void setCurrent_IP_Position(StackFrame fFrame) {
		IP_Position pos;
		pos = getCurrent_IP_Position();
		fFrame.set_IP_Line(pos.get_IP_Line());
		fFrame.set_IP_Path(pos.get_IP_Path());

	}

	private void setVarStrings() {
		String command;
		String command_local = "y ";
		String result;
		boolean ret;

		command = "o frame=0\n";
		if (mPerlVersion.startsWith("5.6")) {
			command = "O frame=0\n";
			command_local = mLovalVarCommand_5_6;
		}
		ret = startCommand(mCommandExecuteCode, command, false, mThreads[0]);
		if (!ret || this.mStopVarUpdate)
			return;
		if (ShowLocalVariableActionDelegate.getPreferenceValue()) {
			result = evaluateStatement(mThreads[0], command_local, false);
			//startSubCommand(mCommandExecuteCode, command_local, false);
			if (result != null) {
				if (result.startsWith(mPadwalkerError)) {
					PerlDebugPlugin
							.errorDialog("***Error displaying Local Variables****\nInstall Padawalker on your Perl system or disable displaying of local variables");
					mLocalVarsAvailable = false;
				} else {
					mVarLocalString = result;
				}
			}
		}
		command = "o frame=2\n";
		if (mPerlVersion.startsWith("5.6"))
			command = "O frame=2\n";

		ret = startCommand(mCommandExecuteCode, command, false, mThreads);
		if (!ret || this.mStopVarUpdate)
			return;
		result = evaluateStatement(mThreads[0], "X ", false);

		mVarGlobalString = result;

	}

	private void setVarList(StackFrame fFrame) {
		IVariable[] lVars;
		ArrayList lVarList = null;

		if (fFrame == null)
			return;
		if (mStopVarUpdate == true)
			return;

		if (mVarLocalString != null)
			lVarList = mVarParser.parseVars(mVarLocalString,
					PerlDebugVar.IS_LOCAL_SCOPE);
		if (mStopVarUpdate == true)

			if (mStopVarUpdate == true) {
				System.err.println("Exit Local+++++++++++++++++");
				return;
			}

		if (lVarList != null)
			mVarParser.parseVars(mVarGlobalString,
					PerlDebugVar.IS_GLOBAL_SCOPE, lVarList);
		else
			lVarList = mVarParser.parseVars(mVarGlobalString,
					PerlDebugVar.IS_GLOBAL_SCOPE);

		if (mStopVarUpdate == true)
			if (mStopVarUpdate == true) {
				System.err.println("Exit Global+++++++++++++++++");
				return;
			}

		try {
			//	removeUnwantedVars(lVarList);
			fFrame.setVariables(lVarList);
		} catch (DebugException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private boolean isStepCommand(int fCommand) {
		return ((fCommand & mIsStepCommand) > 0);
	}

	private boolean isRunCommand(int fCommand) {
		return ((fCommand & mIsRunCommand) > 0);
	}

	private boolean isSubCommand() {
		return (mCurrentSubCommand != mCommandNone);
	}
	private boolean isSkipEvaluateCommandResult() {
		return (isSubCommand() && ((mCurrentSubCommand & mCommandModifierSkipEvaluateCommandResult) > 0));
	}

	private int maskCommandModifiers(int fCommand) {
		return (fCommand & (mCommandModifierRangeStart - 1));
	}

	public boolean addBreakpoint(PerlBreakpoint fBp) {
		return (addBreakpoint(fBp, false));
	}

	public boolean addBreakpoint(PerlBreakpoint fBp, boolean fIsPending) {
		boolean isValid;
		isValid = setBreakpoint(fBp, fIsPending);
		if (!isValid)
			fBp.setIsNoValidBreakpointPosition(true);
		return (isValid);
	}

	public String getPerlDbPath(IPath fPath) {
		int match;
		IPath path;

		if (!mWorkingDir.isPrefixOf(fPath))
			return (fPath.toString());

		match = mWorkingDir.matchingFirstSegments(fPath);
		path = fPath.removeFirstSegments(match).makeRelative().setDevice(null);
		return (path.toString());
	}

	boolean switchToFile(PerlBreakpoint fBp) {
		String path, command;

		path = getPerlDbPath(fBp.getResourcePath());
		//path = path.replaceAll("\\","/");
		command = "f " + path + "\n";
		startSubCommand(mCommandExecuteCode, command, false);
		if (mReSwitchFileFail.getAllMatches(mDebugSubCommandOutput).length > 0)
			return false;
		else
			return true;
	}

	boolean startSetLoadBreakpointCommand(PerlBreakpoint fBp) {
		String path, command;

		path = getPerlDbPath(fBp.getResourcePath());
		//path = path.replaceAll("\\","/");
		command = "b load " + path;

		startSubCommand(mCommandExecuteCode, command, false);
		return true;

	}

	boolean startSetLineBreakpointCommand(PerlLineBreakpoint fBp) {
		String line, command;

		line = Integer.toString(fBp.getLineNumber());
		command = "b " + line;

		startSubCommand(mCommandExecuteCode, command, false);
		if (mReSetLineBreakpoint.getAllMatches(mDebugSubCommandOutput).length > 0)
			return true;
		else
			return false;
	}

	private boolean setBreakpoint(PerlBreakpoint fBp, boolean fIsPending) {
		boolean erg;

		if (!fIsPending) {
			erg = switchToFile(fBp);
			if (!erg) {
				mPendingBreakpoints.add(fBp);
				startSetLoadBreakpointCommand(fBp);
				return (true);
			}
		}

		if (!(fBp instanceof PerlLineBreakpoint))
			return (false);

		erg = startSetLineBreakpointCommand(((PerlLineBreakpoint) fBp));

		if (erg) {
			mActiveBreakpoints.add(fBp);
			fBp.addInstallation(this);
		}

		return (erg);

	}
	public void removeBreakpoint(PerlBreakpoint fBp) {
		String line, command;
		if (mPendingBreakpoints.remove(fBp))
			return;

		if (!(fBp instanceof PerlLineBreakpoint))
			return;

		switchToFile(fBp);

		line = Integer.toString(((PerlLineBreakpoint) fBp).getLineNumber());
		if (this.mPerlVersion.startsWith("5.6"))
			command = "d ";
		else
			command = "B ";

		command = command + line;
		startSubCommand(mCommandExecuteCode, command, false);
	}

	private boolean insertPendingBreakpoints() {
		IP_Position pos;
		Set bps;
		boolean erg;
		PerlBreakpoint bp;

		pos = getCurrent_IP_Position();
		bps = mPendingBreakpoints.getBreakpointsForFile(pos.get_IP_Path());
		if (bps == null || bps.size() == 0)
			return false;

		for (Iterator i = bps.iterator(); i.hasNext();) {
			bp = ((PerlBreakpoint) i.next());
			erg = addBreakpoint(bp, true);
			if (!erg)
				bp.setIsNoValidBreakpointPosition(true);
		}

		bps.clear();

		return (true);
	}

	private boolean isRequireCompareLocals(String fOutputString) {
		if (fOutputString == null)
			return (false);
		StringTokenizer lines = new StringTokenizer(fOutputString, "\r\n");
		boolean exited = false;
		int level = 0;
		String line;

		while (lines.hasMoreTokens()) {
			line = (String) lines.nextToken();

			if (mReExitFrame.getAllMatches(line).length > 0)
				level--;
			else if (mReEnterFrame.getAllMatches(line).length > 0)
				level++;

			if (level < 0)
				return (false);
		}

		if (level != 0)
			return (false);

		return (true);

	}

	public void redirectIO(int fPort) {
		String ip = null;

		try {
			ip = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
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

	public void redirectError(int fPort) {
		String ip = null;

		try {
			ip = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		String command = "require IO::Socket; {my $OUT;"
				+ "$OUT = new IO::Socket::INET(" + "Timeout  => \'10\',"
				+ "PeerAddr => \'" + ip + ":" + fPort + "\',"
				+ "Proto    => 'tcp',);" + "STDERR->fdopen($OUT,\"w\");}";

		startCommand(mCommandExecuteCode, command, false, this);

	}

	public boolean containtsThread(PerlDebugThread fThread) {
		return (mThreads[0] == fThread);
	}
	private void getRegExp(IP_Position fPos, StringBuffer fRegexp,
			StringBuffer fArg) {
		StringBuffer sourceCode = new StringBuffer();

		int BUF_SIZE = 1024;

		//	Get the file content
		char[] buf = new char[BUF_SIZE];
		File inputFile = new File(fPos.get_IP_Path().makeAbsolute().toString());
		BufferedReader in;
		try {
			in = new BufferedReader(new FileReader(inputFile));

			int read = 0;
			while ((read = in.read(buf)) > 0) {
				sourceCode.append(buf, 0, read);
			}
			in.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		String line = null;
		Document doc = new Document(sourceCode.toString());
		try {
			int length = doc.getLineLength(fPos.get_IP_Line() - 1);
			int offset = doc.getLineOffset(fPos.get_IP_Line() - 1);
			line = doc.get(offset, length);
		} catch (BadLocationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return;
		}

		try {
			String delim;

			RE findDelim = new RE("[$%@].+[\\s]*=~[\\s]*[m]?(.)", 0,
					RESyntax.RE_SYNTAX_PERL5);

			REMatch match = findDelim.getMatch(line);
			if (match == null)
				return;
			delim = match.toString(1);
			if (delim == null)
				return;
			String temp = line;
			temp.replaceAll("\\" + delim, "xx");
			RE findRegExp = new RE("([$%@][^\\s]+)[\\s]*=~[\\s]*[m]?" + delim
					+ "(.*)" + delim, 0, RESyntax.RE_SYNTAX_PERL5);
			match = findRegExp.getMatch(temp);
			String var = line.substring(match.getStartIndex(1), match
					.getEndIndex(1));
			String text = line.substring(match.getStartIndex(2), match
					.getEndIndex(2));
			var = evaluateStatement(mThreads[0], "p \"" + var + "\"");
			text = evaluateStatement(mThreads[0], "p \"" + text + "\"");
			System.out.println("\n" + var + ":" + text + "\n");
			fRegexp.append(text);
			fArg.append(var);
		} catch (REException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}

	}

	public static void updateVariableView() {
		if (ShowLocalVariableActionDelegate.getPreferenceValue()
				&& (!mLocalVarsAvailable)) {

			PerlDebugPlugin
					.errorDialog("***Error displaying Local Variables****\nInstall Padawalker on your Perl system or disable displaying of local variables");
		}

		Set debuggers = PerlDebugPlugin.getPerlBreakPointmanager()
				.getDebugger();
		Iterator iterator = debuggers.iterator();
		PerlDB db;
		while (iterator.hasNext()) {
			db = (PerlDB) iterator.next();
			try {
				((StackFrame) db.mThreads[0].getStackFrames()[0]).updateVars();
			} catch (DebugException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			db.generateDebugEvalEvent();
		}
	}

	public class VarUpdateThread extends Thread {
		private String mString;

		public VarUpdateThread(String fString) {
			mString = fString;
		}

		public void run() {

			StackFrame frame = null;
			System.err.println("Start+++++++++++++++++" + mIsCommandFinished);
			try {
				for (int x = 0; (x < 5) && !mStopVarUpdate; x++)
					sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			System.err.println("Start 0+++++++++++++++++" + mIsCommandFinished);

			if (mStopVarUpdate == true) {
				System.err.println("Exit 0+++++++++++++++++"
						+ mIsCommandFinished);
				return;
			}

			setVarStrings();
			try {
				if (mThreads[0].getStackFrames() == null)
					return;
				frame = (StackFrame) mThreads[0].getStackFrames()[0];
			} catch (DebugException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			System.err.println("Start 1+++++++++++++++++" + mIsCommandFinished);

			if (mStopVarUpdate == true) {
				System.err.println("Exit 1+++++++++++++++++"
						+ mIsCommandFinished);
				return;
			}
			setVarList(frame);
			System.err.println("Start 2+++++++++++++++++" + mIsCommandFinished);
			if (mStopVarUpdate == true) {
				System.err.println("Exit 2+++++++++++++++++"
						+ mIsCommandFinished);
				return;
			}
			updateStackFramesFinish(mString);
			System.err.println("Start 3+++++++++++++++++" + mIsCommandFinished);
			if (mStopVarUpdate == true)
				return;
			generateDebugEvalEvent();
		}

	}
};