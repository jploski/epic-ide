package org.epic.core.builders;

import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.epic.perleditor.PerlEditorPlugin;

/**
 * Manages incremental and full builds of Perl projects.
 * The "builds" currently only consist of validation using PerlValidator.
 * 
 * @author jploski
 * @author luelljoc
 */
public class PerlBuilder extends IncrementalProjectBuilder
{
    /**
     * IResource instances representing Perl files that need
     * to be validated during the current build.
     */
    private Set dirtyResources;

    /**
     * IResource instances representing Perl files that need
     * to be validated during the current build, excluding those
     * Perl files that still need to be validated because the
     * previous build was cancelled.
     */
    private Set newDirtyResources;
    
    /**
     * IResource instances whose labels have to be updated after
     * the current build finishes (normally or by being cancelled).
     */
    private Set validatedResources;
    
	/**
	 * No-arg default constructor per IncrementalProjectBuilder specification.
	 */
	public PerlBuilder()
	{
	}
    
	protected IProject[] build(int kind, Map args, IProgressMonitor monitor)
		throws CoreException
	{
        // To maintain a responsive GUI, the actual build is split into
        // two phases:
        // phase 1 (this method, blocking, fast) determines which Perl files
        //         have to be validated
        // phase 2 (PerlBuilderJob, non-blocking, slow) validates them
        //         asynchronously
        
        try
        {
            dirtyResources = new HashSet();
            newDirtyResources = new HashSet();
            validatedResources = new HashSet();
        
            return buildImpl(kind, args, monitor);
        }
        finally
        {
            // don't keep unnecessary references
            dirtyResources = null;
            newDirtyResources = null;
            validatedResources = null;
        }
    }
    
    private IProject[] buildImpl(int kind, Map args, IProgressMonitor monitor)
        throws CoreException
    {
        if (PerlEditorPlugin.getDefault().requirePerlInterpreter(false))
        {
            cancelPreviousPerlBuilderJob();
            findDirtyResources(kind);
            startPerlBuilderJob();
        }
        else { /* it makes no sense to even attempt a build without perl */ }
        return null;
	}
    
    /**
     * Adds a resource to validatedResources if it is an IProject (label
     * always updated after build). Otherwise adds it to dirtyResources
     * and newDirtyResources to schedule it for validation.
     */
    private void visitResource(IResource resource)
    {
        switch (resource.getType())
        {
        case IResource.PROJECT:
            validatedResources.add(resource);
            break;
        case IResource.FILE:
            validatedResources.remove(resource);
            dirtyResources.add(resource);
            newDirtyResources.add(resource);
            break;
        }
    }
    
    /**
     * Cancels the PerlBuilderJob which might still be running
     * asynchronously, started by the previous build. Schedules
     * any remaining dirtyResources of that job to be processed
     * during the current build.
     */
    private void cancelPreviousPerlBuilderJob()
    {
        IJobManager jobMan = Platform.getJobManager();
        Job[] jobs = jobMan.find(PerlBuilderJob.JOB_FAMILY);

        if (jobs.length == 0) return; // no previous build found
        
        jobMan.cancel(PerlBuilderJob.JOB_FAMILY);
        try { jobMan.join(PerlBuilderJob.JOB_FAMILY, null); }
        catch (InterruptedException e)
        {
            // nobody should interrupt our thread in this state;
            // if they do anyway, we treat is as a build cancellation
            throw new OperationCanceledException();
        }
        
        PerlBuilderJob cancelled = (PerlBuilderJob) jobs[0];        
        dirtyResources.addAll(cancelled.getDirtyResources());
    }
    
    /**
     * Visits all of project resources or just changed resources (depending
     * on the build kind) and schedules them for validation by adding them
     * to the dirtyResources set.
     */
    private void findDirtyResources(int buildKind)
    {
        try
        {
            IResourceDelta delta = getDelta(getProject());
            
            if (buildKind == IncrementalProjectBuilder.FULL_BUILD || delta == null)
                getProject().accept(new BuildFullVisitor());
            else
                delta.accept(new BuildDeltaVisitor());
        }
        catch (CoreException e)
        {
            // this exception should never occur because our visitors
            // are not supposed to throw

            PerlEditorPlugin.getDefault().getLog().log(
                new Status(Status.ERROR,
                    PerlEditorPlugin.getPluginId(),
                    IStatus.OK,
                    "Unexpected exception while building project " +
                    getProject().getName() +
                    "; report it as bug in plug-in " +
                    PerlEditorPlugin.getPluginId(),
                    e));
        }
    }
    
    /**
     * Returns the elements from dirtyResources sorted so that members
     * of newDirtyResources precede non-members. Validation will occur
     * in that order, improving responsiveness (under assumption that
     * incremental builds are "interactive" and thus have higher priority
     * than "background" full builds).
     * 
     * Note: the dirtyResources set is modified as a side effect.
     */
    private List getSortedDirtyResources()
    {
        List sorted = new ArrayList(dirtyResources.size());
        
        sorted.addAll(newDirtyResources);
        dirtyResources.removeAll(newDirtyResources);
        sorted.addAll(dirtyResources);
        
        return sorted;
    }
    
    /**
     * Starts an asynchronous, low-priority PerlBuilderJob which does
     * the actual validation work.
     */
    private void startPerlBuilderJob()
    {
        Job job = new PerlBuilderJob(
            getSortedDirtyResources(),
            validatedResources);
        
        job.setPriority(Job.DECORATE);
        job.schedule();
    }
    
    private class BuildDeltaVisitor implements IResourceDeltaVisitor
    {
        public boolean visit(IResourceDelta delta) // does NOT throw CoreException
        {       
            if (delta.getKind() == IResourceDelta.CHANGED)
                visitResource(delta.getResource());

            return true;
        }
    }

    private class BuildFullVisitor implements IResourceVisitor
    {
        public boolean visit(IResource resource) // does NOT throw CoreException
        {
            visitResource(resource);
            return true;
        }
    }
}