package org.epic.debug;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.debug.core.*;
import org.eclipse.debug.core.model.*;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.action.*;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.*;
import org.epic.perleditor.PerlEditorPlugin;
import org.epic.perleditor.editors.BasePDETestCase;
import org.epic.perleditor.preferences.PreferenceConstants;

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
    private Map<String, Integer> varAddrs;
    private int nextVarID;
    
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
        }
        
        protected String getScriptName() { return "test_Debugger.pl"; }
        };

    private TestDriver testBreakpointsListener = new TestDriver() {
        public void handleDebugEvents(DebugEvent[] events)
        {
            super.handleDebugEvents(events);
            for (int i = 0; i < events.length; i++)
            {
                if (events[i].getSource() instanceof IStackFrame)
                {
                    IStackFrame frame = (IStackFrame) events[i].getSource();
                    
                    // Note: we only get the event if the current stack frame is
                    // selected in the Debug view on suspend. Unfortunately, this
                    // sometimes doesn't happen and the test gets stuck, becomes
                    // unstuck when you clicking on the frame manually. :-(
                    
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
        } 

        protected String getScriptName() { return "test_Debugger.pl"; }
        };

    private TestDriver testVariablesListener = new TestDriver() {
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
                            StringBuffer buf = new StringBuffer();
                            varsToString(frame.getVariables(), buf);
                            appendData(buf.toString());
                        }
                        catch (Exception e) {e.printStackTrace();}
                        resume(frame.getThread());
                    }
                }
            }
        }

        protected String getScriptName() { return "test_Variables.pl"; }
        };
        
    protected void setUp() throws Exception
    {
        super.setUp();
        
        varAddrs = new HashMap<String, Integer>();
        nextVarID = 0;
        mainThread = Thread.currentThread();
        tStart = tEnd = 0L;
    }

    public void testBreakpoints() throws Exception
    {
        launchDebuggerAndWait(testBreakpointsListener, false);

        // check that (only) the expected breakpoints were hit
        assertEquals(
            "test_Debugger2.pl[line: 2]" +
            "TestDebugger3.pm[line: 7]" +
            "test_Debugger.pl[line: 16]" +
            "test_Debugger.pl[line: 25]" +
            "test_Debugger2.pl[line: 2]",
            testBreakpointsListener.getData());
    }

    public void testStepPerformance() throws Exception
    {
        launchDebuggerAndWait(testStepPerformanceListener, true);
        assertTrue(
            "why is this test running slower than expected? " +
            "(actual duration: " + (tEnd-tStart) + " ms)",
            tEnd-tStart < 13100);
    }
    
    public void testVariables() throws Exception
    {
        boolean savedShowInternalVariables = getShowVariablesPreference(
            "org.epic.debug.showPerlInternalVariablesAction");
        boolean savedShowGlobalVariables = getShowVariablesPreference(
            "org.epic.debug.showGlobalVariablesAction");
        
        try
        {
            setShowVariablesPreference(
                "org.epic.debug.showPerlInternalVariablesAction", false);
            setShowVariablesPreference(
                "org.epic.debug.showGlobalVariablesAction", true);
            
            launchDebuggerAndWait(testVariablesListener, false);
    
            String expected = readFile("test.in/TestDebugger-expected1.txt");
            String actual = testVariablesListener.getData();
            assertEquals(expected, actual);
        }
        finally
        {
            setShowVariablesPreference(
                "org.epic.debug.showPerlInternalVariablesAction",
                savedShowInternalVariables);
            setShowVariablesPreference(
                "org.epic.debug.showGlobalVariablesAction",
                savedShowGlobalVariables);
        }
    }

    private ILaunchConfiguration getLaunchConfig(String scriptName)
        throws Exception
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
            
            if (scriptName.equals(config.getName()) &&
                "EPICTest".equals(projectName) &&
                scriptName.equals(scriptFile)) return config;
        }
        return null;
    }
    
    private void launchDebuggerAndWait(
        TestDriver listener,
        boolean suspendAtFirst)
        throws Exception
    {
        IPreferenceStore prefs = PerlEditorPlugin.getDefault().getPreferenceStore();
        
        boolean p1 = PerlEditorPlugin.getDefault().getBooleanPreference(
            PreferenceConstants.DEBUG_DEBUG_CONSOLE);
        boolean p2 = PerlEditorPlugin.getDefault().getBooleanPreference(
            PreferenceConstants.DEBUG_SUSPEND_AT_FIRST);
        
        try
        {
            prefs.setValue(PreferenceConstants.DEBUG_DEBUG_CONSOLE, false);
            prefs.setValue(PreferenceConstants.DEBUG_SUSPEND_AT_FIRST, suspendAtFirst);

            ILaunchConfiguration config = getLaunchConfig(listener.getScriptName());
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
            prefs.setValue(PreferenceConstants.DEBUG_DEBUG_CONSOLE, p1);
            prefs.setValue(PreferenceConstants.DEBUG_SUSPEND_AT_FIRST, p2);
            
            IEditorPart editor = null;
            editor = findEditor("EPICTest/test_Debugger.pl");
            if (editor != null) closeEditor(editor);
            editor = findEditor("EPICTest/test_Debugger2.pl");
            if (editor != null) closeEditor(editor);
            editor = findEditor("EPICTest/test_Variables.pl");
            if (editor != null) closeEditor(editor);
            
            // not sure why the delay at this point is necessary,
            // but without it testVariables fails with EOF from
            // debugger :(
            try { spinEventLoop(3000); } catch (InterruptedException e) { }
        }
    }

    private void resume(final IThread thread)
    {
        interruptSpinEventLoop();
        Display.getDefault().asyncExec(new Runnable() {
            public void run() {
                try { spinEventLoop(0); } catch (Exception e) { }
                try { thread.resume(); }
                catch (DebugException e) { e.printStackTrace(); }
            } });
    }
    
    private IAction getShowVariablesAction(String id)
    {
        IViewPart variablesView = findView("org.eclipse.debug.ui.VariableView");
        IMenuManager man =
            variablesView.getViewSite().getActionBars().getMenuManager();
        
        ActionContributionItem item = 
            (ActionContributionItem) man.find(id);
        
        return item.getAction();
    }
    
    private boolean getShowVariablesPreference(String id)
    {
        return getShowVariablesAction(id).isChecked();
    }
    
    private void setShowVariablesPreference(String id, boolean value)
    {
        IAction action = getShowVariablesAction(id);
        if (action.isChecked() != value)
        {
            action.setChecked(value);
            action.run();
        }
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
    
    private void varsToString(IVariable[] vars, StringBuffer buf)
        throws Exception
    {
        final String HEX_ADDR = "0x[0-9a-z]+";
        final Pattern p = Pattern.compile(HEX_ADDR, Pattern.DOTALL);
        
        for (int i = 0; i < vars.length; i++)
        {
            String val = vars[i].getValue().getValueString();
            Matcher m = p.matcher(val);
            
            if (m.find())
            {
                Integer id = varAddrs.get(m.group());
                if (id == null)
                {
                    id = new Integer(nextVarID);
                    nextVarID++;
                    varAddrs.put(m.group(), id);
                }
                val = val.replaceAll(HEX_ADDR, id.toString());
            }
            
            buf.append(vars[i].getName());
            buf.append('=');
            buf.append(val);
            
            if (vars[i].getValue().hasVariables())
            {
                buf.append(", with {");
                varsToString(vars[i].getValue().getVariables(), buf);
                buf.append('}');
            }
            buf.append("\n");
        }
    }
    
    /**
     * Base class for debugger test drivers. An instance drives the
     * test by responding to DebugEvents and collects some data along
     * the way. The data can be evaluated upon test's completion.
     */
    private abstract class TestDriver implements IDebugEventSetListener
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
                        interruptSpinEventLoop();
                    }
                }
                else if (events[i].getSource() instanceof IDebugTarget)
                {
                    if (events[i].getKind() == DebugEvent.TERMINATE)
                    {
                        synchronized (this) { if (tEnd == 0L) tEnd = System.currentTimeMillis(); }
                        mainThread.interrupt();
                        interruptSpinEventLoop();
                    }
                }
            }
        }
        
        protected void appendData(String str)
        {
            data.append(str);
        }
        
        protected abstract String getScriptName();
    }
}
