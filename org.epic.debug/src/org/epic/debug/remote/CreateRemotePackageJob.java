package org.epic.debug.remote;

import java.io.*;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorRegistry;
import org.epic.core.PerlCore;
import org.epic.core.PerlProject;
import org.epic.debug.PerlDebugPlugin;
import org.epic.debug.util.RemotePathMapper;

/**
 * Creates a ZIP archive with project files and helper scripts.
 * This archive can be manually extracted and executed on a remote
 * machine by the user to establish a remote debugging session.
 */
public class CreateRemotePackageJob extends Job
{
    private final byte[] buffer;
    private final File archiveFile;
    private final ZipOutputStream zipOut;
    private final ILaunch launch;
    private final RemoteLaunchConfigurationDelegate launchDelegate;
    private final RemotePathMapper mapper;
    
    public CreateRemotePackageJob(        
        RemoteLaunchConfigurationDelegate launchDelegate,
        ILaunch launch,
        RemotePathMapper mapper) throws CoreException
    {
        super("Create Remote Debug Package");
        
        this.launch = launch;
        this.launchDelegate = launchDelegate;
        this.mapper = mapper;
        this.buffer = new byte[1024];
        this.archiveFile = launchDelegate.getDebugPackageFile(launch);
        this.zipOut = createOutputStream();
    }

    protected IStatus run(IProgressMonitor monitor)
    {
        monitor.beginTask("Create Remote Debug Package", countProjectFiles());
        try
        {
            addProjectFilesToArchive(monitor);
            addHelperScriptToArchive("dumpvar_epic.pm");
            addHelperScriptToArchive("epic_breakpoints.pm");
            addHelperScriptToArchive("autoflush_epic.pm");
            addPerl5DbToArchive();
            addStartScriptToArchive();

            return Status.OK_STATUS;
        }
        catch (CoreException e)
        {
            archiveFile.delete();
            PerlDebugPlugin.log(e);
            return e.getStatus();
        }
        finally
        {
            monitor.done();
            try { zipOut.close(); }
            catch (IOException e) { PerlDebugPlugin.log(e); }
        }        
    }
    
    private void addProjectFilesToArchive(IProgressMonitor monitor)
        throws CoreException
    {
        IResourceVisitor visitor = new ProjectFileArchiver(monitor);
        getProject().accept(visitor);       
    }
    
    private void addHelperScriptToArchive(String scriptName) throws CoreException
    {
        try
        {            
            addFileToArchive(
                PerlDebugPlugin.getDefault().getBundle().getEntry(scriptName).openStream(),
                getWorkingDirPrefix() + scriptName);
        }
        catch (IOException e)
        {
            throw new CoreException(new Status(
                IStatus.ERROR,
                PerlDebugPlugin.getUniqueIdentifier(),
                IStatus.OK,
                "Could not locate helper script " + scriptName,
                e));
        }
    }
    
    private void addFileToArchive(InputStream in, String pathInArchive)
        throws CoreException
    {
        try
        {
            zipOut.putNextEntry(new ZipEntry(pathInArchive));

            int len;
            while ((len = in.read(buffer)) > 0)
                zipOut.write(buffer, 0, len);
        }
        catch (IOException e)
        {
            throw new CoreException(new Status(
                IStatus.ERROR,
                PerlDebugPlugin.getUniqueIdentifier(),
                IStatus.OK,
                "Could not write zip entry " + pathInArchive,
                e));
        }
        finally
        {
            try { in.close(); } catch (IOException e) { }
        }
    }
    
    private void addPerl5DbToArchive() throws CoreException
    {
        try
        {
            File perl5Db = PerlDebugPlugin.getDefault().patchPerl5Db();
            addFileToArchive(
                new BufferedInputStream(new FileInputStream(perl5Db)),
                getWorkingDirPrefix() + "perl5db.pl");
        }
        catch (IOException e)
        {
            throw new CoreException(new Status(
                IStatus.ERROR,
                PerlDebugPlugin.getUniqueIdentifier(),
                IStatus.OK,
                "Could not add patched perl5db.pl to archive",
                e));
        }
    }
    
    private void addStartScriptToArchive() throws CoreException
    {
        try
        {
            zipOut.putNextEntry(new ZipEntry("start_epicDB.pl"));
            String startDB = "$ENV{PERLDB_OPTS}=\"RemotePort="
                + launchDelegate.getEpicDebuggerIP(launch) + ":" 
                + launchDelegate.getEpicDebuggerPort(launch)
                + " DumpReused ReadLine=0 PrintRet=0\";\n" + "if( ! -d \""
                + launchDelegate.getRemoteProjectDir(launch)
                + "\" ) {die(\"Target directory does not exist!\")};\n"
                + "chdir(\"" + launchDelegate.getRemoteProjectDir(launch) + "/"
                + launchDelegate.getScriptPath(launch).removeLastSegments(1) + "\");"
                + "\nsystem(\"perl -d " + createIncPath() + " "
                + launchDelegate.getRemoteProjectDir(launch) + "/"
                + launchDelegate.getScriptPath(launch) + "\");";
            zipOut.write(startDB.getBytes());
        }
        catch (IOException e)
        {
            throw new CoreException(new Status(
                IStatus.ERROR,
                PerlDebugPlugin.getUniqueIdentifier(),
                IStatus.OK,
                "Could not write zip entry for start_epicDB.pl",
                e));
        }
    }

    private String createIncPath() throws CoreException
    {
        StringBuffer buf = new StringBuffer();
        String localProjectDir = getPerlProject().getProjectDir().toString();
        String remoteProjectDir = launchDelegate.getRemoteProjectDir(launch);
        
        buf.append(" -I \\\"");
        buf.append(launchDelegate.getRemoteProjectDir(launch));
        buf.append("\\\"");
        
        for (Iterator<String> i = getPerlProject().getRawIncPath().iterator(); i.hasNext();)
        {
            String path = new Path((String) i.next()).toString();

            if (path.startsWith(localProjectDir))
                path = remoteProjectDir + path.substring(localProjectDir.length()); 
                
            buf.append(" -I \\\"");
            buf.append(path);
            buf.append("\\\"");
        }
        return buf.toString();
    }

    private int countProjectFiles()
    {
        PerlFileCounter visitor = new PerlFileCounter();
        try
        {
            getProject().accept(visitor);
            return visitor.getCount();
        }
        catch (CoreException e)
        {
            PerlDebugPlugin.log(e);
            return 0;
        }
    }
    
    private ZipOutputStream createOutputStream() throws CoreException
    {
        try
        {
            return new ZipOutputStream(new FileOutputStream(archiveFile));
        }
        catch (FileNotFoundException e)
        {
            throw new CoreException(new Status(
                IStatus.ERROR,
                PerlDebugPlugin.getUniqueIdentifier(),
                IStatus.OK,
                "Could not write remote debug package file " +
                archiveFile.getAbsolutePath(),
                e));
        }
    }
    
    private PerlProject getPerlProject() throws CoreException
    {
        return PerlCore.create(getProject());
    }
    
    private IProject getProject() throws CoreException
    {
        return launchDelegate.getProject(launch);
    }
    
    private String getWorkingDirPrefix() throws CoreException
    {
        String prefix = launchDelegate.getScriptPath(launch).removeLastSegments(1).toString();
        if (prefix.length() > 0) prefix += "/";
        return prefix;
    }

    private class ProjectFileArchiver implements IResourceVisitor
    {
        private final IProgressMonitor monitor;

        public ProjectFileArchiver(IProgressMonitor monitor)
        {
            this.monitor = monitor;
        }

        public boolean visit(IResource resource) throws CoreException
        {
            if (resource.isLinked() && resource instanceof IFolder)
                mapper.addLinkedFolderMapping((IFolder) resource);

            if (resource instanceof IFile)
            {
                addFileToArchive((IFile) resource);
                monitor.worked(1);
            }

            return true;
        }
        
        private void addFileToArchive(IFile resource) throws CoreException
        {
            try
            {
                CreateRemotePackageJob.this.addFileToArchive(
                    new BufferedInputStream(
                        new FileInputStream(resource.getLocation().toString())),
                    resource.getFullPath().removeFirstSegments(1).toString());
            }
            catch (IOException e)
            {
                throw new CoreException(new Status(
                    IStatus.ERROR,
                    PerlDebugPlugin.getUniqueIdentifier(),
                    IStatus.OK,
                    "Could not read file " + resource.getLocation(),
                    e));
            }
        }
    }

    private static class PerlFileCounter implements IResourceVisitor
    {
        private static final String PERL_EDITOR_ID = "org.epic.perleditor.editors.PerlEditor";
        private static final String EMB_PERL_FILE_EXTENSION = "epl";
        private final IEditorRegistry registry;
        private int count;

        public PerlFileCounter()
        {
            count = 0;
            registry = PerlDebugPlugin.getDefault().getWorkbench().getEditorRegistry();
        }

        public int getCount()
        {
            return count;
        }

        public boolean visit(IResource resource) throws CoreException
        {
            IEditorDescriptor defaultEditorDescriptor =
                registry.getDefaultEditor(resource.getFullPath().toString());

            if (defaultEditorDescriptor != null &&
                defaultEditorDescriptor.getId().equals(PERL_EDITOR_ID) &&
                !resource.getFileExtension().equals(EMB_PERL_FILE_EXTENSION))
            {
                count++;
            }
            return true;
        }
    }
}
