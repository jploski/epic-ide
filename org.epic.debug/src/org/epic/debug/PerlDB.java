/*
 * Created on 26.04.2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.epic.debug;

import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.core.resources.IProject;
import java.net.*;
import java.io.*;
import gnu.regexp.*;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.IPath;
import java.util.*;
import org.eclipse.debug.core.model.IVariable;
import PerlVarParser;
import org.epic.perleditor.editors.util.PerlExecutableUtilities;
import org.epic.perleditor.PerlEditorPlugin;
import cbg.editor.*;





/**
 * @author ruehl
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class PerlDB	implements IDebugElement {

	private DebugTarget mTarget;
	private CommandThread mCommandThread;
	private ServerSocket mServerSocket;
	private Socket mClientSocket;
	private Process mProcess;
	private int mCurrentCommand;
	/* NO debugging meassages are created for sub-commands*/
	private int mCurrentSubCommand;
	private Object mCurrentCommandDest;
	
	private final static String EMPTY_STRING="";
	private static final String mDBinitPerl =	"{$| = 1;  my $old = select STDERR; $|=1;select $old;}\n";
	
	private PerlDebugThread[] mThreads;
	
	final static int mCanResume = 1;
	final static int mCanStepInto = 2;
	final static int mCanStepOver = 4;
	final static int mCanStepReturn = 8;
	final static int mCanSuspend = 16;
	final static int mCanTerminate= 32;
	
	final static int mCommandNone= 0;
	final static int mCommandStepInto = 1;
	final static int mCommandStepOver = 2;
	final static int mCommandStepReturn = 4;
	final static int mCommandResume = 8;
	final static int mCommandSuspend = 16;
	final static int mCommandTerminate= 32;
	final static int mCommandClearOutput= 64;
	final static int mCommandExecuteCode = 128;
	final static int mCommandEvaluateCode = 256;
	final static int mCommandModifierRangeStart = 1024;
	final static int mCommandModifierSkipEvaluateCommandResult = mCommandModifierRangeStart;
	
	final static int mCommandFinished =1;
	final static int mSessionTerminated =2;
	
	private PrintWriter mDebugIn;
	private BufferedReader mDebugOut;
	private String mDebugOutput;
	private String mDebugSubCommandOutput;
	private RE mReCommandFinished1; 
	private RE mReCommandFinished2;
	private RE mReSessionFinished1,mReSessionFinished2;
	private RE mRe_IP_Pos;
	private RE mRe_IP_Pos_Eval;
	private RE mReSwitchFileFail;
	private RE mReSetLineBreakpoint;

	private IP_Position mStartIP;
	private PerlVarParser mVarParser = new PerlVarParser(this);
		
	private final static int mIsStepCommand= mCommandStepInto | mCommandStepOver | mCommandStepReturn;
	private final static int mIsRunCommand = mIsStepCommand | mCommandResume;
	
	
	private boolean mIsCommandFinished;
	private boolean mIsCommandRunning;
	
	private IPath mWorkingDir;
	
	private BreakpointMap mPendingBreakpoints;
	private BreakpointMap mActiveBreakpoints;
	
	private class CommandThread extends Thread
	{
		
		public CommandThread(){}
		
		public void run()
		{
			waitForCommandToFinish();
		}
		
				
	}
	private class IP_Position
	{
		int IP_Line;
		IPath IP_Path;
		
		public boolean equals(IP_Position fPos)
		{
			if(! IP_Path.equals(fPos.get_IP_Path()) )
				return false;
			
			if( IP_Line != fPos.get_IP_Line() )
				return false;
						
			return(true);
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
		 		
	public PerlDB(DebugTarget fTarget) throws InstantiationException
	{
		
		IPath path;
		
		mTarget = fTarget;
		mProcess = null;
		mCurrentCommand = mCommandNone;
		mCurrentSubCommand = mCommandNone;
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
		String	startfile = null;
		String prjName = null;
		mPendingBreakpoints = new BreakpointMap();
		mActiveBreakpoints  = new BreakpointMap();
		
		try {
				startfile = mTarget.getLaunch().getLaunchConfiguration().getAttribute(PerlLaunchConfigurationConstants.ATTR_STARTUP_FILE
					, EMPTY_STRING);
				prjName =  mTarget.getLaunch().getLaunchConfiguration().getAttribute(PerlLaunchConfigurationConstants.ATTR_PROJECT_NAME
								, EMPTY_STRING);
				} catch (Exception ce) {PerlDebugPlugin.log(ce);}
		IProject prj = PerlDebugPlugin.getWorkspace().getRoot().getProject(prjName);
		
		path = prj.getLocation().append(startfile);
		mWorkingDir =  path.removeLastSegments(1);
		
		/************************************/
		
		// Construct command line parameters
		List fCmdList = null;
		try{
		
		//fCmdList = new ArrayList();
		//fCmdList.add("c:\\perl\\bin\\perl.exe");
		fCmdList=PerlExecutableUtilities.getPerlExecutableCommandLine(prj);
		} catch ( Exception e){ System.out.println(e);}
		
		fCmdList.add("-d");
		
		if (PerlEditorPlugin.getDefault().getWarningsPreference()) {
			fCmdList.add("-w");
		}
		
		
		if (PerlEditorPlugin.getDefault().getTaintPreference()) {
			fCmdList.add("-T");
		}
		
		fCmdList.add(startfile);
		String[] cmdParams =
			(String[]) fCmdList.toArray(new String[fCmdList.size()]);

		
		
		
		/************************************/
		mThreads = new PerlDebugThread[1];
		mThreads[0]= new PerlDebugThread("Main-Thread",fTarget.getLaunch(),fTarget,this);
		
		try{
		mReCommandFinished1 = new RE("\n\\s+DB<\\d+>",0, RESyntax.RE_SYNTAX_PERL5); 
		mReCommandFinished2 = new RE("^\\s+DB<\\d+>",0, RESyntax.RE_SYNTAX_PERL5);
		mReSessionFinished1 = new RE("Use `q' to quit or `R' to restart",0, RESyntax.RE_SYNTAX_PERL5);
		mReSessionFinished2 = new RE("Debugged program terminated.",0, RESyntax.RE_SYNTAX_PERL5);
		mRe_IP_Pos =new RE("^[^\\(]*\\((.*):(\\d+)\\):[\\n\\t]",0, RESyntax.RE_SYNTAX_PERL5);
		mRe_IP_Pos_Eval =new RE("^[^\\(]*\\(eval\\s+\\d+\\)\\[(.*):(\\d+)\\]$",0, RESyntax.RE_SYNTAX_PERL5);
		mReSwitchFileFail = new RE("^No file",0, RESyntax.RE_SYNTAX_PERL5);
		mReSetLineBreakpoint = new RE("^\\s+DB<\\d+>",0, RESyntax.RE_SYNTAX_PERL5);
		} catch (REException e){ new InstantiationException("Couldn't RegEX");};
		String env[] = new String[1];
		
								
		mServerSocket = null ;
				try {
					mServerSocket = new ServerSocket(4444);
				} catch (IOException e) {
					throw new InstantiationException("Couldn't listen to Debug-Port");
				}
		
				mClientSocket = null;
				
				Thread connect = 
				new Thread() 
				{ 
					public void run()
					{
						try {
							System.out.println("Trying to Accept");
						mClientSocket = mServerSocket.accept();
						System.out.println("Accept !!!!!!!\n");
							} catch (IOException e) {
							System.out.println("Accept failed: 4444");
							}
					}
				};
				
				connect.start();
		String nix[] = PerlDebugPlugin.getDebugEnv();
		
		try{
				//Runtime.getRuntime().
			//	exec("perl -d "+startfile,PerlDebugPlugin.getDebugEnv(), new File(mWorkingDir.toString()));

			mProcess= Runtime.getRuntime().exec(
			cmdParams,
			PerlDebugPlugin.getDebugEnv(),
			new File(mWorkingDir.toString()));
			
			
			}catch (Exception e)
				{ System.out.println(e);
				  throw new InstantiationException("Failing to create Process !!!");
				}
				
	//DebugPlugin.newProcess(getLaunch(),mProcess,"Echo-Process");
	
	try{			
		synchronized(this)
		{
		for( int x=0; (x < 100) && (mClientSocket == null) ; ++x)
		{
			System.out.println("Waiting for connect (Try "+x+" of 100)\n");
				 wait(100);
		}
		}
	
		if( mClientSocket == null)
		{ 
			shutdown();
			throw new InstantiationException("Couldn't connect to Debugger");		
		}
		
		mDebugIn = new PrintWriter(mClientSocket.getOutputStream(), true);
		mDebugOut = new BufferedReader(
				new InputStreamReader( mClientSocket.getInputStream())
				);
		}catch(IOException e){throw new InstantiationException("Failing establish Communication with Debug Process  !!!");}
		 catch(InterruptedException e){throw new InstantiationException("Failing establish Communication with Debug Process  !!!");}
		 
		startCommand(mCommandClearOutput,null,false, this);
		PerlDebugPlugin.getPerlBreakPointmanager().addDebugger(this);
		updateStackFrames();
		generateDebugInitEvent();
	}
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IDebugElement#getModelIdentifier()
	 */
	public String getModelIdentifier() {
		return mTarget.getModelIdentifier();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IDebugElement#getDebugTarget()
	 */
	public IDebugTarget getDebugTarget() {
		return mTarget;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IDebugElement#getLaunch()
	 */
	public ILaunch getLaunch() {
		return mTarget.getLaunch();
	}

		public boolean canResume(Object fDest) {
		return ( !mIsCommandRunning && (mProcess != null));
	}

	
	public boolean canSuspend(Object fDest) {
		return (false);
	}

	public boolean isSuspended(Object fDest) {
		return ( (!mIsCommandRunning) && (mProcess != null));
	}

		public void resume(Object fDest) {
		startCommand(mCommandResume, fDest);
	}

	public void suspend(Object fDest) 
	{
		startCommand(mCommandSuspend, fDest);
	}

	
	public boolean canStepInto(Object fDest) {
		return( isSuspended(fDest) );
	}

		public boolean canStepOver(Object fDest) {
		return( isSuspended(fDest) );
	}

	public boolean canStepReturn(Object fDest) {
		return( isSuspended(fDest) );
	}

	public boolean isStepping(Object fDest) {
		return( ( (mCurrentCommand & mIsStepCommand) != 0) && mIsCommandRunning) ;
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

		public boolean canTerminate(Object fDest) {
		return true;
	}

	public boolean isTerminated(Object fDest) {
		return (mProcess == null);
	}

	public void terminate(Object fDest) {
		abortSession();

	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	public Object getAdapter(Class adapter) {
		if( adapter == this.getClass())
			return this;
		else
			return null;
	}
	
	public boolean startCommand(int fCommand,Object fThread)
	{
		return( startCommand(fCommand,null,true,fThread));
	}
	
	public String evaluateStatement(Object fThread, String fText)
	{
		startCommand(mCommandEvaluateCode,fText,true, fThread);
		String result = mDebugOutput.substring(0, mDebugOutput.lastIndexOf("\n"));
		return(result);
	}
	public boolean startCommand(int fCommand,String fCode,boolean fSpawn,Object fThread)
			{
				if( mIsCommandRunning )
					return(false);
				mCurrentCommandDest = fThread;
				mDebugOutput = null;
				mDebugSubCommandOutput = null;
				mCurrentCommand = fCommand;
				mCurrentSubCommand = mCommandNone;
				mIsCommandRunning = true;
				mIsCommandFinished = false;
				return ( startPerlDebugCommand(fCode,fSpawn) );
			}
	
	public boolean startSubCommand(int fCommand)
		{
			return( startSubCommand(fCommand,null,true));
		}
	
	private boolean startSubCommand(int fCommand,String fCode,boolean fSpawn)
		{
			mDebugSubCommandOutput = null;
			mCurrentSubCommand = fCommand;
			return ( startPerlDebugCommand(fCode,fSpawn) );
		}
		
	private boolean startPerlDebugCommand(String fCode, boolean fSpawn)
	{
		int command;
		boolean isSubCommand;
		
		
		if( ! isSubCommand())
		{
			command = mCurrentCommand;
			isSubCommand = false;
		}
		else
		{
			command = mCurrentSubCommand;
			isSubCommand = true;
		}
		
		command = maskCommandModifiers(command);
		if( isStepCommand(command) && ! isSubCommand() )
			mStartIP = getCurrent_IP_Position();
			
		switch( command )
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
				mDebugIn.println(fCode+"\n");
			break;			
			default: 
				return(false);
		}
		
		generateDebugEvent(command,true,mTarget);
			
			if( fSpawn )
			{
				mCommandThread = new CommandThread();
				mCommandThread.start();
				return(true);
			}
			
		
			return( waitForCommandToFinish() );
	}
	
	private void shutdown()
	{
		try{
			if( mDebugIn != null) mDebugIn.close();
			if( mDebugOut != null) mDebugOut.close();
			if( mProcess != null) mProcess.destroy();
			if( mServerSocket != null) mServerSocket.close();
		}catch( Exception e){};
		mProcess.destroy();
		mProcess = null;
	}
	
	void generateDebugEvent(int fCommand, boolean fStart, Object fCommandDest)
	{
		DebugEvent event = null;
		int stepEventKind;
		int stepEventEndDetail;
		
		if( isSubCommand() )
			return;
			
		if( ( (fCommand & mIsStepCommand) != 0 ) && !fStart )
		{
			if( isBreakPointReached() )
				stepEventEndDetail = DebugEvent.BREAKPOINT;
			else
				stepEventEndDetail = DebugEvent.STEP_END; 
				
			event = new DebugEvent(fCommandDest, DebugEvent.SUSPEND, stepEventEndDetail);
		}
		else
		{	
			switch( fCommand )
			{	
				case mCommandStepInto:
					event = new DebugEvent(fCommandDest, DebugEvent.RESUME, DebugEvent.STEP_INTO);
				break;
				
				case mCommandStepOver:
					event = new DebugEvent(fCommandDest, DebugEvent.RESUME, DebugEvent.STEP_INTO);
				break;
				
				case mCommandStepReturn:
					event = new DebugEvent(fCommandDest, DebugEvent.RESUME, DebugEvent.STEP_RETURN);
				break;
				
				case mCommandResume:
					if( fStart )
						event = new DebugEvent(fCommandDest, DebugEvent.RESUME, DebugEvent.CLIENT_REQUEST);
					else
						event = new DebugEvent(fCommandDest, DebugEvent.SUSPEND, DebugEvent.BREAKPOINT);
				break;
				
				case mCommandSuspend:
					if( !fStart )
						event = new DebugEvent(fCommandDest, DebugEvent.SUSPEND, DebugEvent.CLIENT_REQUEST);
				break;
				
				case mCommandTerminate:
					if( !fStart )
						event = new DebugEvent(fCommandDest, DebugEvent.TERMINATE);
				break;
				
				case mCommandEvaluateCode:
					if( fStart )
						event = new DebugEvent(fCommandDest, DebugEvent.RESUME, DebugEvent.CLIENT_REQUEST);
					else
						event = new DebugEvent(fCommandDest, DebugEvent.SUSPEND, DebugEvent.BREAKPOINT);
					break;
			}
		}
		if( event != null )
		{
			DebugEvent debugEvents[] = new DebugEvent[1];
			debugEvents[0]=event;
			DebugPlugin.getDefault().fireDebugEventSet(debugEvents);
		} 
	}


	public void generateDebugInitEvent()
	{
		DebugEvent event = null;
		
		event = new DebugEvent(mThreads[0], DebugEvent.SUSPEND, DebugEvent.STEP_END);
		DebugEvent debugEvents[] = new DebugEvent[1];
		debugEvents[0]=event;
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

		if( isTerminated(mCurrentCommandDest) )
			return(false);
			
		System.out.println("---Waiting for Command ("+mCurrentCommand+"--"+mCurrentSubCommand+") to finish----------------------------");
		
		while(true)
		{
			count = -1;		
			try{			
				count = mDebugOut.read(buf);
			}catch (IOException e)
				{ abortSession(); throw new RuntimeException("Terminating Debug Session due to IO-Error !");}
	
			if(count > 0) debugOutput.append(buf,0,count);
	
			currentOutput = debugOutput.toString();
	
			System.out.println("\nCurrent DEBUGOUTPUT:\n"+currentOutput+"\n");
		
			if( hasSessionTerminated(currentOutput) )
				{ finished = mSessionTerminated; break;}
			if(hasCommandTerminated(currentOutput))
				{ finished = mCommandFinished; break;}  
		}
		
		if( finished == mSessionTerminated)
		 {
		 	abortSession(); return(false);
		 }
		System.out.println("!!!!!!!!!!!!!!!!!!Command ("+mCurrentCommand+"--"+mCurrentSubCommand+") finished!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		if ( isStepCommand(mCurrentCommand) && ! isSubCommand() )
		{
			IP_Position endIP = getCurrent_IP_Position();
			while( (finished!=mSessionTerminated) && mStartIP.equals(endIP))
			{
				startSubCommand(mCurrentCommand | mCommandModifierSkipEvaluateCommandResult,null,false);
				endIP = getCurrent_IP_Position();
			}
			currentOutput = debugOutput.toString();
			
		}
		
		if ( isRunCommand(mCurrentCommand) && (mCurrentCommand != mCommandStepInto) && ! isSubCommand() )
		{
			
			while( (finished!=mSessionTerminated) && ! isBreakPointReached() && !isRunCommand(mCurrentCommand))
			{
				insertPendingBreakpoints();
				startSubCommand(mCurrentCommand | mCommandModifierSkipEvaluateCommandResult,null,false);
			}
			currentOutput = debugOutput.toString();
	
		}		
			ok = evaluateCommandResult(finished,currentOutput);
			commandPostExec(finished,currentOutput);
			mCurrentSubCommand = mCommandNone;
			return(ok);
	}
	
	private boolean hasCommandTerminated(String fOutput)
			{
				boolean erg;
				int count;
			
				erg =  mReCommandFinished1.isMatch(fOutput);
				count = mReCommandFinished1.getAllMatches(fOutput).length;
				if( erg || (count > 0) )	
				return( true );
				
				erg =  mReCommandFinished2.isMatch(fOutput);
				count = mReCommandFinished2.getAllMatches(fOutput).length;
				return( erg || (count > 0) );		
			}
		
			private boolean hasSessionTerminated(String fOutput)
			{
				boolean erg;
				int count;
			
				erg =  mReSessionFinished1.isMatch(fOutput);
				count = mReSessionFinished1.getAllMatches(fOutput).length;
				if( erg || (count > 0) )
				 return(true);

				erg =  mReSessionFinished2.isMatch(fOutput);
				count = mReSessionFinished2.getAllMatches(fOutput).length;
				if( erg || (count > 0) )
					 return(true);
					 
				return(false);
				 	
			}
		
			private void finishCommand(String fOutput)
			{
				System.out.println("############Cleanup Command ("+mCurrentCommand+"--"+mCurrentSubCommand+")");
				if(mCurrentSubCommand == mCommandNone)
				{
					mIsCommandRunning = false;
					mIsCommandFinished = true;
					generateDebugEvent(PerlDB.this.mCurrentCommand, false, mCurrentCommandDest);
					mDebugOutput =  fOutput;
				}
				else
					mDebugSubCommandOutput =  fOutput;
				System.out.println("############State isrunning "+mIsCommandRunning+" isfinished "+mIsCommandFinished+"\n");
			}
		
			private void abortCommandThread()
			{
				abortSession();
				PerlDB.this.generateDebugEvent(PerlDB.mCommandTerminate,false,mTarget);
					
			}
		
			private void abortSession()
			{ 
				mCurrentSubCommand = mCommandNone;
				mCurrentCommand = mCommandNone;
				generateDebugEvent(mCommandTerminate, false, mCurrentCommandDest);
				shutdown();
				mIsCommandRunning = false;
				mIsCommandFinished = false;
				PerlDebugPlugin.getPerlBreakPointmanager().removeDebugger(this);
			}
			
	private void commandPostExec(int fExitValue,String fOutput)
	{
		
		if( fExitValue == mCommandFinished)
			{finishCommand(fOutput);} 
		
		if( fExitValue == mSessionTerminated )
			{abortCommandThread();}
	}
	
	private boolean isBreakPointReached()
	{
		IP_Position pos = getCurrent_IP_Position();
		
		return(mActiveBreakpoints.getBreakpointForLocation( pos.get_IP_Path(), pos.get_IP_Line())!= null );
	}
	
	public IThread[] getThreads()
	{ 
		return mThreads; 
	}
	
	public Process getProcess()
	{ 
		return mProcess; 
	}
	
	private boolean evaluateCommandResult(int fResult,String fOutputString)
	{
		
		int command;
		boolean isSubCommand;
		
		if ( isSkipEvaluateCommandResult() )
			return(true);
			
		if( mCurrentSubCommand == mCommandNone)
		{
			command = mCurrentCommand;
			isSubCommand = false;
		}
		else
		{
			command = mCurrentSubCommand;
			isSubCommand = true;
		}
				
		switch( command )
		{
			case mCommandStepInto:
			case mCommandStepOver:
			case mCommandStepReturn:
			case mCommandResume:
			case mCommandSuspend:
			case mCommandTerminate:
			case mCommandEvaluateCode:
				updateStackFrames();
			break;
			case mCommandClearOutput:
			break;
			case mCommandExecuteCode:
			break;			
			default: 
				return(false);
		}	
		return(true);	
	}
	
	
	private void updateStackFrames()
	{
		StackFrame frame = new StackFrame(mThreads[0]);
		setCurrent_IP_Position(frame);
		setVarList(frame);
		mThreads[0].setStrackFrame(frame);
	}
	
	
	private IP_Position getCurrent_IP_Position()
		{
			int line;
			IPath file;
			IP_Position pos;
			String file_name;
			startSubCommand(mCommandExecuteCode,".",false);
			REMatch temp;
			
			REMatch result = mRe_IP_Pos.getMatch(mDebugSubCommandOutput);
			System.out.println( result.toString(1)+"--:--"+result.toString(2));
			file_name = result.toString(1);
			temp = mRe_IP_Pos_Eval.getMatch(file_name);
			if( temp != null)
				result = temp;
			System.out.println( result.toString(1)+"!--:--!"+result.toString(2));	
			line = Integer.parseInt(result.toString(2));
			file = new Path(result.toString(1));
			if( ! file.isAbsolute())
			{
				file = mWorkingDir.append(file);
			}
			
			pos = new IP_Position();
			pos.set_IP_Line(line);
			pos.set_IP_Path(file);
			return(pos);
		 
		}

	private void setCurrent_IP_Position(StackFrame fFrame)
	{
		IP_Position pos;
		pos = getCurrent_IP_Position();
		fFrame.set_IP_Line(pos.get_IP_Line());
		fFrame.set_IP_Path(pos.get_IP_Path());
		 
	}

	private  void setVarList(StackFrame fFrame)
			{
				IVariable[] lVars;
				startSubCommand(mCommandExecuteCode,"X ",false);
				
				lVars = mVarParser.parseVars(mDebugSubCommandOutput);
				try{
			    fFrame.setVariables(lVars);
				}catch (Exception e){};
		 
			}

	private boolean isStepCommand(int fCommand)
	{
		return( (fCommand & mIsStepCommand) > 0);	
	}

	private boolean isRunCommand(int fCommand)
	{
		return(  (fCommand & mIsRunCommand) > 0 );	
	}
	
	private boolean isSubCommand()
	{
		return( mCurrentSubCommand != mCommandNone);
	}
	private boolean isSkipEvaluateCommandResult()
	{
		return( isSubCommand() 
				&& ( (mCurrentSubCommand & mCommandModifierSkipEvaluateCommandResult) > 0));	
	}
	

private int maskCommandModifiers(int fCommand)
{
	return( fCommand & (mCommandModifierRangeStart-1));
}

public boolean addBreakpoint(PerlBreakpoint fBp)
{
	return( addBreakpoint(fBp,false) );
}

public boolean  addBreakpoint(PerlBreakpoint fBp, boolean fIsPending)
{
	boolean isValid;
	isValid = setBreakpoint(fBp,fIsPending);
	if( !isValid )
		fBp.setIsNoValidBreakpointPosition(true);
	return(isValid);
}


public String getPerlDbPath(IPath fPath)
{
		int match;
		IPath path;
		
 		if( ! mWorkingDir.isPrefixOf(fPath) )
 			return( fPath.toString() );
 			
 		match = mWorkingDir.matchingFirstSegments(fPath);
 		path = fPath.removeFirstSegments(match).makeRelative().setDevice(null);
 		return(path.toString());
}

boolean switchToFile(PerlBreakpoint fBp)
{
		String path,command;
		
		path = getPerlDbPath(fBp.getResourcePath());
		//path = path.replaceAll("\\","/");
		command = "f "+path+"\n";
		startSubCommand(mCommandExecuteCode,command, false);
		if( mReSwitchFileFail.getAllMatches(mDebugSubCommandOutput).length > 0 )
			return false;
		else
			return true;
}


boolean startSetLoadBreakpointCommand(PerlBreakpoint fBp)
{
	String path,command;
		
	path = getPerlDbPath(fBp.getResourcePath());
	//path = path.replaceAll("\\","/");
	command = "b load "+path;
		
		startSubCommand(mCommandExecuteCode,command, false);
		return true;
		
}

boolean startSetLineBreakpointCommand(PerlLineBreakpoint fBp)
{
		String line,command;
		
		line = Integer.toString(fBp.getLineNumber());
		command = "b "+line;
		
		startSubCommand(mCommandExecuteCode,command, false);
		if( mReSetLineBreakpoint.getAllMatches(mDebugSubCommandOutput).length > 0 )
			return true;
		else
			return false;
}

private boolean setBreakpoint(PerlBreakpoint fBp, boolean fIsPending)
{
	boolean erg;
		
	if( ! fIsPending )
	{
		erg = switchToFile(fBp);
		if( ! erg )
		{
			mPendingBreakpoints.add(fBp);
			startSetLoadBreakpointCommand(fBp);
			return(true);		
		}	
	}
	
	if( ! (fBp instanceof PerlLineBreakpoint) )
			return(false);
				
	erg = startSetLineBreakpointCommand( ((PerlLineBreakpoint) fBp) );
	
	if( erg )
	{
		mActiveBreakpoints.add(fBp);
		fBp.addInstallation(this);
	}
	
	
	
	return(erg);
			
}
public void removeBreakpoint(PerlBreakpoint fBp)
{
	String line,command;
	if( mPendingBreakpoints.remove(fBp) )
		return;
	
	if( ! (fBp instanceof PerlLineBreakpoint) )	
	 return;
		
	switchToFile(fBp);
		
	line = Integer.toString( ((PerlLineBreakpoint)fBp).getLineNumber());
	command = "B "+line;
	startSubCommand(mCommandExecuteCode,command, false);
}

private boolean insertPendingBreakpoints()
{
	IP_Position pos;
	Set bps;
	boolean erg;
	PerlBreakpoint bp;
	
	pos = getCurrent_IP_Position();
	bps = mPendingBreakpoints.getBreakpointsForFile(pos.get_IP_Path());
	if( bps == null || bps.size() == 0)
		return false;
		
	for(Iterator i = bps.iterator(); i.hasNext();)
	{
		bp = ((PerlBreakpoint) i.next());
		erg = addBreakpoint( bp,true);
		if ( ! erg )
			bp.setIsNoValidBreakpointPosition(true);
	}
		 
	bps.clear();
	
	
	return(true);
}
protected void finalize()throws Throwable
	{
		shutdown();
		super.finalize();
	}

}