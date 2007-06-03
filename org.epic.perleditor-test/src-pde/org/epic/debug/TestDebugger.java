package org.epic.debug;

import org.eclipse.debug.core.*;
import org.eclipse.debug.core.model.*;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.*;
import org.epic.perleditor.PerlEditorPlugin;
import org.epic.perleditor.editors.BasePDETestCase;

/**
 * Assorted tests of the org.epic.debug plug-in. Note that these tests
 * are quite sensitive to the test workspace's content and state, while
 * they SHOULD not be sensitive to the state of the tested workbench.
 * When in doubt, check out a pristine test workspace from CVS. 
 */
public class TestDebugger extends BasePDETestCase
{
    private Thread mainThread;
    private long tStart, tEnd;
    
    private TestDriver testStepPerformanceListener = new TestDriver() {
        public void handleDebugEvents(DebugEvent[] events)
        {
            super.handleDebugEvents(events);
            for (int i = 0; i < events.length; i++)
            {
                if (events[i].getSource() instanceof IStackFrame)
                {
                    IStackFrame frame = (IStackFrame) events[i].getSource();
                    if (events[i].getKind() == DebugEvent.CHANGE &&
                        events[i].getDetail() == DebugEvent.CONTENT)
                    {
                        stepInto(frame.getThread());
                    }
                }
            }
        } };

    private TestDriver testBreakpointsListener = new TestDriver() {
        public void handleDebugEvents(DebugEvent[] events)
        {
            super.handleDebugEvents(events);
            for (int i = 0; i < events.length; i++)
            {
                if (events[i].getSource() instanceof IStackFrame)
                {
                    IStackFrame frame = (IStackFrame) events[i].getSource();
                    if (events[i].getKind() == DebugEvent.CHANGE &&
                        events[i].getDetail() == DebugEvent.CONTENT)
                    {
                        try
                        {
                            appendData(frame.getName());
                        }
                        catch (Exception e) {e.printStackTrace();}
                        resume(frame.getThread());
                    }
                }
            }
        } };
        
    protected void setUp() throws Exception
    {
        super.setUp();
        
        mainThread = Thread.currentThread();
        tStart = tEnd = 0L;
    }
    
    public void testBreakpoints() throws Exception
    {
        launchDebuggerAndWait(testBreakpointsListener, false);

        // check that (only) the expected breakpoints were hit
        assertEquals(
            "test_Debugger2.pl[line: 2]" +
            "test_Debugger.pl[line: 12]" +
            "test_Debugger.pl[line: 21]" +
            "test_Debugger2.pl[line: 2]",
            testBreakpointsListener.getData());
    }

    public void testStepPerformance() throws Exception
    {
        launchDebuggerAndWait(testStepPerformanceListener, true);
        assertTrue(
            "why is this test running slower than expected? " +
            "(actual duration: " + (tEnd-tStart) + " ms)",
            tEnd-tStart < 13000);
    }

    private ILaunchConfiguration getLaunchConfig() throws Exception
    {
        ILaunchConfiguration[] configs = DebugPlugin.getDefault()
            .getLaunchManager().getLaunchConfigurations();
    
        for (int i = 0; i < configs.length; i++)
        {
            ILaunchConfiguration config = configs[i];
            String projectName = config.getAttribute(
                    PerlLaunchConfigurationConstants.ATTR_PROJECT_NAME,
                    (String) null);
            String scriptFile = config.getAttribute(
                    PerlLaunchConfigurationConstants.ATTR_STARTUP_FILE,
                    (String) null);
            
            if ("test_Debugger.pl".equals(config.getName()) &&
                "EPICTest".equals(projectName) &&
                "test_Debugger.pl".equals(scriptFile)) return config;
        }
        return null;
    }
    
    private void launchDebuggerAndWait(
        IDebugEventSetListener listener,
        boolean suspendAtFirst)
        throws Exception
    {
        boolean p1 = PerlEditorPlugin.getDefault().getDebugConsolePreference();
        boolean p2 = PerlEditorPlugin.getDefault().getSuspendAtFirstPreference();
        
        try
        {
            PerlEditorPlugin.getDefault().setDebugConsolePreference(false);
            PerlEditorPlugin.getDefault().setSuspendAtFirstPreference(suspendAtFirst);

            ILaunchConfiguration config = getLaunchConfig();
            assert config != null;
            
            DebugPlugin.getDefault().addDebugEventListener(listener);
            DebugUITools.launch(config, ILaunchManager.DEBUG_MODE);
            spinEventLoop(0);
            
            IWorkbenchPage page =
                PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
            page.showView("org.eclipse.debug.ui.VariableView");
            spinEventLoop(0);
            
            for (;;)
            {
                try { spinEventLoop(10000); }
                catch (InterruptedException e)
                {
                    synchronized (this) { if (tEnd > 0L) break; }
                }
            }
            DebugPlugin.getDefault().removeDebugEventListener(listener);
        }
        finally
        {
            PerlEditorPlugin.getDefault().setDebugConsolePreference(p1);
            PerlEditorPlugin.getDefault().setSuspendAtFirstPreference(p2);
            
            IEditorPart editor = null;
            editor = findEditor("EPICTest/test_Debugger.pl");
            if (editor != null) closeEditor(editor);
            editor = findEditor("EPICTest/test_Debugger2.pl");
            if (editor != null) closeEditor(editor);            
        }
    }

    private void resume(final IThread thread)
    {
        Display.getDefault().asyncExec(new Runnable() {
            public void run() {
                try { spinEventLoop(0); } catch (Exception e) { }
                try { thread.resume(); }
                catch (DebugException e) { e.printStackTrace(); }
            } });
    }
    
    private void stepInto(final IThread thread)
    {
        Display.getDefault().asyncExec(new Runnable() {
            public void run() {
                try { spinEventLoop(0); } catch (Exception e) { }
                try { thread.stepInto(); }
                catch (DebugException e) { e.printStackTrace(); }
            } });
    }
    
    /**
     * Base class for debugger test drivers. An instance drives the
     * test by responding to DebugEvents and collects some data along
     * the way. The data can be evaluated upon test's completion.
     */
    private class TestDriver implements IDebugEventSetListener
    {
        private StringBuffer data;
        
        public TestDriver()
        {
            data = new StringBuffer();
        }
        
        public String getData()
        {
            return data.toString();
        }
        
        public void handleDebugEvents(DebugEvent[] events)
        {
            for (int i = 0; i < events.length; i++)
            {
                if (events[i].getSource() instanceof IThread)
                {
                    if (events[i].getKind() == DebugEvent.CREATE)
                    {
                        tStart = System.currentTimeMillis();
                    }
                    else if (events[i].getKind() == DebugEvent.TERMINATE)
                    {
                        synchronized (this) { tEnd = System.currentTimeMillis(); }
                        mainThread.interrupt();
                    }
                }
                else if (events[i].getSource() instanceof IDebugTarget)
                {
                    if (events[i].getKind() == DebugEvent.TERMINATE)
                    {
                        synchronized (this) { if (tEnd == 0L) tEnd = System.currentTimeMillis(); }
                        mainThread.interrupt();
                    }
                }
            }
        }
        
        protected void appendData(String str)
        {
            data.append(str);
        }
    }
}
