package org.epic.debug;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.ILineBreakpoint;

/**
 * @author ruehl
 */
public class PerlLineBreakpoint extends PerlBreakpoint implements ILineBreakpoint
{
    //~ Static fields/initializers

    private static final String PERL_LINE_BREAKPOINT = "org.epic.debug.perlLineBreakpointMarker"; // $NON-NLS-1$

    private static final String CONDITION_ENABLED = "org.epic.debug.conditionEnabled";

    private static final String CONDITION = "org.epic.debug.condition";

    private int hitCount = 0;
    
    private int removedLineNumber = -1;

    private String regExp = "";

    //~ Constructors

    public PerlLineBreakpoint()
    {
        super();
    }

    public PerlLineBreakpoint(IResource resource, int lineNumber) throws DebugException
    {
        this(resource, lineNumber, -1, -1, true, new HashMap<String, Serializable>(), PERL_LINE_BREAKPOINT);
    }

    protected PerlLineBreakpoint(IResource resource, int lineNumber, int charStart, int charEnd,
        boolean add, Map<String, Serializable> attributes, String markerType) throws DebugException
    {
        assert attributes != null;

        createPerlLineBreakpoint(resource, lineNumber, charStart, charEnd, add, attributes,
            markerType);
    }

    //~ Methods

    private void addLineBreakpointAttributes(Map<String, Serializable> attributes, String modelIdentifier, boolean enabled,
        int lineNumber, int charStart, int charEnd)
    {
        attributes.put(IBreakpoint.ID, modelIdentifier);
        attributes.put(IMarker.LINE_NUMBER, new Integer(lineNumber));
        attributes.put(IMarker.CHAR_START, new Integer(charStart));
        attributes.put(IMarker.CHAR_END, new Integer(charEnd));
        attributes.put(PerlBreakpoint.INVALID_POS, new Boolean(false));
        attributes.put(IBreakpoint.PERSISTED, Boolean.TRUE);
        attributes.put(IBreakpoint.ENABLED, Boolean.TRUE);
        attributes.put(IBreakpoint.REGISTERED, Boolean.FALSE);
    }

    private void createPerlLineBreakpoint(final IResource resource, final int lineNumber,
        final int charStart, final int charEnd, final boolean add, final Map<String, Serializable> attributes,
        final String markerType) throws DebugException
    {

        IWorkspaceRunnable wr =
            new IWorkspaceRunnable()
        {
            public void run(IProgressMonitor monitor) throws CoreException
            {
                setMarker(resource.createMarker(markerType));
                addLineBreakpointAttributes(attributes, getModelIdentifier(), true, lineNumber,
                    charStart, charEnd);

                ensureMarker().setAttributes(attributes);
                register(add);
            }
        };

        run(wr);
    }

    public int getCharEnd() throws CoreException
    {
        return ensureMarker().getAttribute(IMarker.CHAR_END, -1);
    }

    public int getCharStart() throws CoreException
    {
        return ensureMarker().getAttribute(IMarker.CHAR_START, -1);
    }

    public int getLineNumber() throws CoreException
    {
        return removedLineNumber != -1
        		? removedLineNumber
        		: ensureMarker().getAttribute(IMarker.LINE_NUMBER, -1);
    }
    
    public void pendingRemove() throws CoreException
    {
    	super.pendingRemove();
    	
    	// Remember the last line number the breakpoint marker had
    	// before it was removed; we need it to remove the breakpoint
    	// from the Perl debugger when it suspends next time:
    	this.removedLineNumber = getLineNumber(); 
    }

    /**
     * Add this breakpoint to the breakpoint manager, or sets it as unregistered.
     */
    protected void register(boolean register) throws CoreException
    {
        if (register)
        {
            DebugPlugin.getDefault().getBreakpointManager().addBreakpoint(this);
        }
        else
        {
            setRegistered(false);
        }
    }

    /**
     * Execute the given workspace runnable
     */
    protected void run(IWorkspaceRunnable wr) throws DebugException
    {
        try
        {
            ResourcesPlugin.getWorkspace().run(wr, null);
        }
        catch (CoreException e)
        {
            throw new DebugException(e.getStatus());
        }
    }

    public int getHitCount()
    {
        return this.hitCount;
    }

    public void setHitCount(int hitCount)
    {
        this.hitCount = hitCount;
    }

    public void setConditionEnabled(boolean conditionEnabled) throws CoreException {
        setAttributes(new String[]{CONDITION_ENABLED}, new Object[]{Boolean.valueOf(conditionEnabled)});
        recreate();
    }


    public void setCondition(String condition) throws CoreException {
        setAttributes(new String []{CONDITION}, new Object[]{condition});
        recreate();
    }

    public String getRegExp()
    {
        return this.regExp;
    }

    public boolean isConditionEnabled() throws CoreException
    {
        return ensureMarker().getAttribute(CONDITION_ENABLED, false);
    }

    public String getCondition() throws CoreException
    {
        return ensureMarker().getAttribute(CONDITION, null);
    }

    private void recreate()
    {
        // TODO: re-register the breakpoint now that the attributs have changed (see JavaBreakpoint)
    }

    public void setRegExp(String regExp)
    {
        this.regExp = regExp;
    }
    
    public String toString()
    {
        try { return getResourcePath() + ":" + getLineNumber(); }
        catch (Exception e) { return super.toString(); }
    }
}
