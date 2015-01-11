package org.epic.debug.remote;

import java.io.IOException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.*;
import org.epic.debug.*;
import org.epic.debug.db.DebuggerInterface;
import org.epic.debug.util.*;

class RemoteDebugTarget extends DebugTarget
{
    private final IDebugEventSetListener listener = new IDebugEventSetListener() {
        public void handleDebugEvents(DebugEvent[] events)
        {
            for (int i = 0; i < events.length; i++)
            {
                try
                {
                    if (events[i].getKind() == DebugEvent.TERMINATE &&
                        getThreads()[0].equals(events[i].getSource()))
                    {
                        DebugPlugin.getDefault().removeDebugEventListener(this);
                        shutdown(); // we're done when our thread is
                        return;
                    }
                }
                catch (DebugException e)
                {
                    PerlDebugPlugin.log(e);
                }
            }
        } };
    
    public RemoteDebugTarget(
        ILaunch launch,
        DebuggerProxy process,
        RemotePort debugPort,
        IPathMapper pathMapper)
        throws CoreException
    {
        super(launch, process, debugPort, pathMapper);
        DebugPlugin.getDefault().addDebugEventListener(listener);
    }

    protected DebuggerInterface initDebuggerInterface(DebuggerInterface db)
        throws DebugException
    {
        boolean shouldRedirectIO = true;
        try
        {
            shouldRedirectIO = getLaunch().getLaunchConfiguration().getAttribute(
                PerlLaunchConfigurationConstants.ATTR_REMOTE_CAPTURE_OUTPUT,
                true);
        }
        catch (CoreException e) { /* should never occur */ } 

        if (getProxy().getIOHost() != null && shouldRedirectIO)
        {
            try
            {
                db.redirectIO(getProxy().getIOHost(), getProxy().getIOPort());
                db.redirectError(getProxy().getIOHost(), getProxy().getErrorPort());
            }
            catch (IOException e) { throwDebugException(e); }
        }
        
        // hackModPerlRegistryCooker(db); // not yet implemented, see method description 

        return super.initDebuggerInterface(db);
    }
    
    private DebuggerProxy getProxy()
    {
        return (DebuggerProxy) getProcess();
    }
    
    /**
     * Modifies ModPerl::Registry::get_mark_line (called by ModPerl::RegistryCooker)
     * in order to force the back-end to suspend when the execution of a user script
     * (via eval) begins.
     * 
     * TODO: the corresponding handling in the front-end is not implemented yet. The idea is
     * that we can get an opportunity to install script-local breakpoints when the artificial
     * eval-breakpoint created by this method is encountered, in analogy to the installation
     * of breakpoints upon loading normal Perl modules in epic_breakpoints.pm). 
     */
    private void hackModPerlRegistryCooker(DebuggerInterface db) throws DebugException
    {
        try
        {
            String output = db.eval(
                ";{if(defined($ENV{MOD_PERL})) { *ModPerl::Registry::get_mark_line = sub { " +
                "my $self = shift; " +
                "my $break = \"\\n\\$DB::single = 1;\"; " +
                "$ModPerl::Registry::MarkLine ? $break . \"\\n#line 1 $self->{FILENAME}\\n\" : $break;" +
                "}; print $DB::OUT \"mod_perl\\n\"; }}");
        }
        catch (IOException e) { throwDebugException(e); }
    }
}
