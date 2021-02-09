package org.epic.debug.remote;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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
import org.epic.debug.util.CygwinPathMapper;
import org.epic.debug.util.IPathMapper;
import org.epic.debug.util.NullPathMapper;
import org.epic.debug.util.RemotePathMapper;
import org.epic.perleditor.PerlEditorPlugin;
import org.epic.perleditor.preferences.PreferenceConstants;

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
    
    private void addStartScriptToArchive() throws CoreException
    {
        try
        {
        	String remoteProjectDir=launchDelegate.getRemoteProjectDir(launch).replaceAll("\\\\", "\\\\\\\\");
        	if(remoteProjectDir.trim().isEmpty()) remoteProjectDir=".";
            zipOut.putNextEntry(new ZipEntry("start_epicDB.pl"));
            String startDB = "use Cwd 'abs_path';\n\n"
            	+ "$ENV{PERLDB_OPTS}=\"RemotePort="
                + launchDelegate.getEpicDebuggerIP(launch) + ":" 
                + launchDelegate.getEpicDebuggerPort(launch)
                + " DumpReused ReadLine=0 PrintRet=0\";\n" + "if( ! -d \""
                + remoteProjectDir
                + "\" ) {die(\"Target directory does not exist!\")};\n"
                + "chdir(\"" + remoteProjectDir + "/"
                + launchDelegate.getScriptPath(launch).removeLastSegments(1) + "\");"
                + "\npatchPerl5db() unless (-e perl5db.pl);"
                + "\nsystem(\"perl -d " + createIncPath() + " "
                + remoteProjectDir + "/"
                + launchDelegate.getScriptPath(launch) + "\");\n\n"
                + perl5dbPatcherSub();
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

    private String perl5dbPatcherSub(){
    	return 
    	  "sub patchPerl5db {\n"
    	+ "	return if(-e \"perl5db.pl\");\n"
    	+ "	my $marker = 'return unless $postponed_file{$filename};';\n"
    	+ "	my $patch  = '    { use epic_breakpoints; my $osingle = $single; $single = 0; $single = epic_breakpoints::_postponed($filename, $line) || $osingle; }';\n"
    	+ "	my $found  = 0;\n"
    	+ "	for my $path (@INC) {\n"
    	+ "		if (-e \"$path/perl5db.pl\") {\n"
    	+ "\n"
    	+ "			# Note: we do not use a replace all because of bug 1734045\n"
    	+ "			open(SFH, \"<$path/perl5db.pl\");\n"
    	+ "			open(OFH, \">perl5db.pl\");\n"
    	+ "			while (<SFH>) {\n"
    	+ "				my $line = $_;\n"
    	+ "				if ($line =~ /\\Q$marker\\E/) {\n"
    	+ "					$found=1;\n"
    	+ "					print OFH \"$patch\\n\";\n"
    	+ "				}\n"
    	+ "				print OFH $line;\n"
    	+ "			}\n"
    	+ "			close OFH;\n"
    	+ "			close SFH;\n"
    	+ "			if(!$found){\n"
    	+ "				unlink \"perl5db.pl\";\n"
    	+ "			}else{\n"
    	+ "				return;\n"
    	+ "			}\n"
    	+ "		}\n"
    	+ "	}\n"
    	+ "	if(!$found){\n"
    	+ "		die \"could not find a patchable perl5db\";\n"
    	+ "	}\n"
    	+ "}";
    }
    
    private String createIncPath() throws CoreException
    {
        StringBuilder buf = new StringBuilder();
        String localProjectDir = getPerlProject().getProjectDir().toString();
        String remoteProjectDir=launchDelegate.getRemoteProjectDir(launch).replaceAll("\\\\", "\\\\\\\\");
    	if(remoteProjectDir.trim().isEmpty()) remoteProjectDir=".";
        
        
        buf.append(" -I \\\"");
        buf.append(remoteProjectDir);
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
