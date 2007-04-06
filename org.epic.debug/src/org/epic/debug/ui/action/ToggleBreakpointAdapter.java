package org.epic.debug.ui.action;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.ui.actions.IToggleBreakpointsTargetExtension;

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;

import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.texteditor.ITextEditor;

import org.epic.debug.PerlDebugModel;
import org.epic.debug.PerlLineBreakpoint;


/**
 */
public class ToggleBreakpointAdapter implements IToggleBreakpointsTargetExtension
{
    //~ Methods

    /*
     * @see
     * org.eclipse.debug.ui.actions.IToggleBreakpointsTargetExtension#canToggleBreakpoints(org.eclipse.ui.IWorkbenchPart,
     * org.eclipse.jface.viewers.ISelection)
     */
    public boolean canToggleBreakpoints(IWorkbenchPart part, ISelection selection)
    {
        return canToggleLineBreakpoints(part, selection);
    }

    /*
     * @see
     * org.eclipse.debug.ui.actions.IToggleBreakpointsTarget#canToggleLineBreakpoints(org.eclipse.ui.IWorkbenchPart,
     * org.eclipse.jface.viewers.ISelection)
     */
    public boolean canToggleLineBreakpoints(IWorkbenchPart part, ISelection selection)
    {
        // TODO: check if "remote" file
        return selection instanceof ITextSelection;
    }

    /*
     * @see
     * org.eclipse.debug.ui.actions.IToggleBreakpointsTarget#canToggleMethodBreakpoints(org.eclipse.ui.IWorkbenchPart,
     * org.eclipse.jface.viewers.ISelection)
     */
    public boolean canToggleMethodBreakpoints(IWorkbenchPart part, ISelection selection)
    {
        return false;
    }

    /*
     * @see
     * org.eclipse.debug.ui.actions.IToggleBreakpointsTarget#canToggleWatchpoints(org.eclipse.ui.IWorkbenchPart,
     * org.eclipse.jface.viewers.ISelection)
     */
    public boolean canToggleWatchpoints(IWorkbenchPart part, ISelection selection)
    {
        return false;
    }

    /*
     * @see
     * org.eclipse.debug.ui.actions.IToggleBreakpointsTargetExtension#toggleBreakpoints(org.eclipse.ui.IWorkbenchPart,
     * org.eclipse.jface.viewers.ISelection)
     */
    public void toggleBreakpoints(IWorkbenchPart part, ISelection selection) throws CoreException
    {
        toggleLineBreakpoints(part, selection);
    }

    /*
     * @see
     * org.eclipse.debug.ui.actions.IToggleBreakpointsTarget#toggleLineBreakpoints(org.eclipse.ui.IWorkbenchPart,
     * org.eclipse.jface.viewers.ISelection)
     */
    public void toggleLineBreakpoints(final IWorkbenchPart part, final ISelection selection)
        throws CoreException
    {
        // TODO: status bar messages
        Job job =
            new Job("Toggle Line Breakpoint (epic)")
        {
            protected IStatus run(IProgressMonitor monitor)
            {
                ITextEditor editor = getTextEditor(part);
                if ((editor == null) || ! (selection instanceof ITextSelection))
                {
                    return Status.OK_STATUS;
                }

                int lineNumber = ((TextSelection) selection).getStartLine() + 1;
                IResource resource = getResource(editor);

                try
                {
                    IBreakpoint breakpoint =
                        PerlDebugModel.lineBreakpointExists(resource, lineNumber);

                    if (breakpoint != null)
                    {
                        removeBreakpoint(breakpoint, true);
                    }
                    else
                    {
                        createLineBreakpoint(resource, lineNumber);
                    }

                    return Status.OK_STATUS;
                }
                catch (CoreException e)
                {
                    return e.getStatus();
                }
            }
        };

        job.setSystem(true);
        job.schedule();
    }

    /*
     * @see
     * org.eclipse.debug.ui.actions.IToggleBreakpointsTarget#toggleMethodBreakpoints(org.eclipse.ui.IWorkbenchPart,
     * org.eclipse.jface.viewers.ISelection)
     */
    public void toggleMethodBreakpoints(IWorkbenchPart part, ISelection selection)
        throws CoreException
    {
        // not yet supported
    }

    /*
     * @see
     * org.eclipse.debug.ui.actions.IToggleBreakpointsTarget#toggleWatchpoints(org.eclipse.ui.IWorkbenchPart,
     * org.eclipse.jface.viewers.ISelection)
     */
    public void toggleWatchpoints(IWorkbenchPart part, ISelection selection) throws CoreException
    {
        // not yet supported
    }

    protected IResource getResource(ITextEditor editor)
    {
        IResource resource = (IResource) editor.getEditorInput().getAdapter(IFile.class);

        if (resource == null)
        {
            resource = (IResource) editor.getEditorInput().getAdapter(IResource.class);
        }

        return resource;
    }

    protected ITextEditor getTextEditor(IWorkbenchPart part)
    {
        if (part instanceof ITextEditor) { return (ITextEditor) part; }

        return (ITextEditor) part.getAdapter(ITextEditor.class);
    }

    private void createLineBreakpoint(IResource resource, int lineNumber)
    {
        /*
         * TODO: investigate extracting regexps from line's that contain them
         *
         * the previous breakpoint implementation allowed for RegularExpression breakpoints - the idea
         * being a user would create them on a line containing a regexp, it would be auto-extracted
         * and when the breakpoint hit - the regexp view would automatically be openned w/ the
         * regexp and text to match already pre-populated. an excellent idea, but the current
         * implemenation had issues.
         *
         * 1) a regexp breakpoint could be created on any line w/o a regexp statement in it. while
         * functioning like a "normal" breakpoint, this was confusing b/c it spawned a pop-up window
         * asking the user for regexp and match input - which really made no sense.
         *
         * 2) a bug exists where the variable containing the match text, instead of the match text
         * itself is auto-populated into the regexp view. somehow the value of the match text
         * variable needs to be retrieved, but it requires a deeper understanding of how the
         * debugger interface works b/c the value would most likely need to be retreived from the
         * perl debugger instance itself (ie: p $variable).
         *
         * for a future implementation, it should be possible to automatically check the source line
         * for a regular expression, and if it exists, perform the extraction so the regexp view can
         * be auto-openned. the breakpoint properties page should be updated to reflect that the
         * breakpoint is for a regexp, along w/ the annotation icon.
         */

        try
        {
            new PerlLineBreakpoint(resource, lineNumber);
        }
        catch (DebugException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    private void removeBreakpoint(IBreakpoint breakpoint, boolean delete) throws CoreException
    {
        DebugPlugin.getDefault().getBreakpointManager().removeBreakpoint(breakpoint, delete);
    }

}
