package org.epic.debug.cgi;

import java.io.*;
import java.net.URL;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.*;
import org.eclipse.debug.core.model.IProcess;
import org.epic.core.PerlCore;
import org.epic.core.PerlProject;
import org.epic.core.util.PerlExecutableUtilities;
import org.epic.debug.*;
import org.epic.debug.util.RemotePort;
import org.osgi.framework.Bundle;

/**
 * Executes launch configurations of type "Perl CGI".
 */
public class CGILaunchConfigurationDelegate extends LaunchConfigurationDelegate
{
    protected void doLaunch(
        ILaunchConfiguration configuration,
        String mode,
        ILaunch launch,
        IProgressMonitor monitor) throws CoreException
    {
        RemotePort debugPort = createDebugPort(launch);
        
        try
        {            
            CGIProxy cgiProxy = new CGIProxy(launch, "CGI Process");
            int brazilPort = RemotePort.findFreePort();

            IProcess process = startBrazil(
                launch, cgiProxy, brazilPort, debugPort);
            
            cgiProxy.waitForConnect();
            if (!cgiProxy.isConnected())
            {
                PerlDebugPlugin.getDefault().logError(
                    "(CGI-Target) Could not connect to CGI-Proxy");
                launch.terminate();
                return;
            }
            launch.addProcess(cgiProxy);
            
            openBrowser(launch, brazilPort);
            
            if (debugPort != null)
                createCGIDebugTarget(launch, process, debugPort);
        }
        catch (CoreException e)
        {
            if (debugPort != null) debugPort.shutdown();
            launch.terminate();
            throw e;
        }
    }
    
    private BrazilProps createBrazilProps(
        ILaunch launch,
        CGIProxy cgiProxy,
        int brazilPort,
        int debugPort) throws CoreException
    {
        String htmlRootDir = getLaunchAttribute(launch,
            PerlLaunchConfigurationConstants.ATTR_HTML_ROOT_DIR, true);

        String cgiRootDir = getLaunchAttribute(launch,
            PerlLaunchConfigurationConstants.ATTR_CGI_ROOT_DIR, true);
                    
        String cgiFileExtension = getLaunchAttribute(launch,
            PerlLaunchConfigurationConstants.ATTR_CGI_FILE_EXTENSION, false);
        
        String perlParams = getLaunchAttribute(launch,
            PerlLaunchConfigurationConstants.ATTR_PERL_PARAMETERS, false);
        if (perlParams == null) perlParams = "";
        perlParams = perlParams.replaceAll("[\\n\\r]", " ");
        
        PerlProject project = PerlCore.create(getProject(launch));

        String perlPath = PerlExecutableUtilities.getPerlInterpreterPath();
        if (perlPath == null) perlPath = ""; // TODO report an error?

        BrazilProps props = new BrazilProps();

        props.add("cgi.InPort", cgiProxy.getInPort());
        props.add("cgi.OutPort", cgiProxy.getOutPort());
        props.add("cgi.ErrorPort", cgiProxy.getErrorPort());
        props.add("cgi.Debug", isDebugMode(launch));
        props.add("root", htmlRootDir);
        props.add("port", brazilPort);
        props.add("cgi.root", cgiRootDir);
        props.add("cgi.executable", perlPath);
        props.add("cgi.suffix", cgiFileExtension);
        props.add("cgi.PerlParams", perlParams);
        props.add("cgi.DebugInclude", "-I" + PerlDebugPlugin.getDefault().getInternalDebugInc());
        props.add("cgi.RunInclude", PerlExecutableUtilities.getPerlIncArgs(project));        

        String[] env = PerlDebugPlugin.getDebugEnv(launch, debugPort);
        for (int i = 0; i < env.length; i++)
        {
            int j = env[i].indexOf('=');
            if (j > 0) props.add(
                "cgi.ENV_" + env[i].substring(0,j),
                env[i].substring(j+1));
        }
        
        return props;    
    }
    
    private void createCGIDebugTarget(
        ILaunch launch, IProcess process, RemotePort debugPort)
        throws CoreException
    {
        if (debugPort.waitForConnect(false) != RemotePort.WAIT_OK)
        {
            PerlDebugPlugin.errorDialog("Could not connect to debug port!");
            debugPort.shutdown();
            launch.terminate();
            return;
        }
        else
        {
            CGIDebugTarget target = new CGIDebugTarget(
                launch, process, debugPort, getPathMapper(launch));

            launch.addDebugTarget(target);
        }
    }
    
    private RemotePort createDebugPort(ILaunch launch)
    {
        if (!isDebugMode(launch)) return null;
        
        RemotePort debugPort = new RemotePort("DebugTarget.mDebugPort");
        debugPort.startConnect();
        return debugPort;
    }
    
    /**
     * @return a List of Files representing entries of the classpath
     *         passed to the Brazil (web server) JVM
     */
    private List<File> getBrazilJVMClasspath() throws CoreException
    {
        try
        {
            List<File> cp = new ArrayList<File>();
            
            URL brazilUrl = Platform.resolve(Platform.getBundle("org.epic.lib")
                .getEntry("/lib/brazil_mini.jar"));
            
            assert "file".equalsIgnoreCase(brazilUrl.getProtocol()) :
                "brazil_mini.jar must reside in the file system";
            
            cp.add(urlToFile(brazilUrl));
            
            Bundle bundle = PerlDebugPlugin.getDefault().getBundle();
            URL binUrl = bundle.getEntry("/bin");
            
            if (binUrl != null)
            {
                binUrl = Platform.resolve(binUrl);
                assert binUrl.getProtocol().equalsIgnoreCase("file");
    
                // 'bin' folder exists = we're running inside of
                // a hosted workbench 
    
                cp.add(urlToFile(binUrl));
            }
            else
            {
                URL dirUrl = Platform.resolve(bundle.getEntry("/"));
                
                if (dirUrl.getProtocol().equalsIgnoreCase("jar"))
                {
                    // org.epic.debug was deployed as a jar; add this jar
                    // to the classpath
                    
                    String path = dirUrl.getPath();
                    assert path.startsWith("file:");
                    assert path.endsWith(".jar!/");
                    
                    URL jarUrl = new URL(path.substring(0, path.length()-2));
                    cp.add(urlToFile(jarUrl));                
                }
                else
                {   
                    assert dirUrl.getProtocol().equalsIgnoreCase("file");
                    
                    // org.epic.debug was deployed as a directory:
                    // add this directory to the classpath
                    
                    cp.add(urlToFile(dirUrl));
                }
            }
            return cp;
        }
        catch (Exception e)
        {
            throw new CoreException(new Status(
                IStatus.ERROR,
                PerlDebugPlugin.getUniqueIdentifier(),
                IStatus.OK,
                "getBrazilJVMClasspath failed",
                e));
        }
    }
    
    private String getLaunchAttribute(
        ILaunch launch, String attrName, boolean isPath)
        throws CoreException
    {
        String attrValue = launch.getLaunchConfiguration().getAttribute(
            attrName, (String) null);
        
        if (attrValue == null) return null;        
        else return new Path(attrValue).toString();
    }
    
    private String getRelativeURL(ILaunch launch) throws CoreException
    {
        String htmlRootFile = getLaunchAttribute(launch,
            PerlLaunchConfigurationConstants.ATTR_HTML_ROOT_FILE, true);
        
        String htmlRootDir = getLaunchAttribute(launch,
            PerlLaunchConfigurationConstants.ATTR_HTML_ROOT_DIR, true);
        
        return
            new Path(htmlRootFile)
                .setDevice(null)
                .removeFirstSegments(new Path(htmlRootDir).segments().length)
                .toString();
    }

    /**
     * @param path a list of File objects representing paths
     * @return a string with absolute paths separated by
     *         the platform-specific path separator
     */
    private static String makePathString(List<File> path)
    {
        StringBuffer buf = new StringBuffer();
        for (Iterator<File> i = path.iterator(); i.hasNext();)
        {
            File entry = i.next();
            if (buf.length() > 0) buf.append(File.pathSeparator);
            buf.append(entry.getAbsolutePath());
        }
        return buf.toString();
    }
    
    private void openBrowser(ILaunch launch, int httpPort)
        throws CoreException
    {
        try
        {
            CGIBrowser browser = new CGIBrowser(
                launch, getRelativeURL(launch), httpPort);
            browser.open();
        }
        catch (CoreException e)
        {
            PerlDebugPlugin.getDefault().logError(
                "Could not start web browser for CGI debugging.",
                e);
            throw e;
        }
    }
    
    private IProcess startBrazil(
        ILaunch launch,
        CGIProxy cgiProxy,
        int brazilPort,
        RemotePort debugPort) throws CoreException
    {
        try
        {
            createBrazilProps(
                launch,
                cgiProxy,
                brazilPort,
                debugPort != null ? debugPort.getServerPort() : -1
                ).save();
        }
        catch (CoreException e)
        {
            PerlDebugPlugin.getDefault().logError(
                "Could not read launch configuration attributes.",
                e);
            throw e;
        }
        catch (IOException e)
        {
            throw new CoreException(new Status(
                IStatus.ERROR,
                PerlDebugPlugin.getUniqueIdentifier(),
                IStatus.OK,
                "Could not create configuration file for web server.",
                e));
        }
        
        Process brazilProcess;
        try { brazilProcess = startBrazilProcess(); }
        catch (CoreException e)
        {
            PerlDebugPlugin.getDefault().logError(
                "Could not start web server", e);
            throw e;
        }

        return DebugPlugin.newProcess(launch, brazilProcess, "Web Server");
    }
    
    private Process startBrazilProcess() throws CoreException
    {
        String javaExec =
            System.getProperty("java.home") +
            File.separator +
            "bin" +
            File.separator +
            "java";
        File workingDir =
            PerlDebugPlugin.getDefault().getStateLocation().toFile();

        String[] cmdParams = {
            javaExec,
            "-classpath",
            makePathString(getBrazilJVMClasspath()),
            "sunlabs.brazil.server.Main",
            "-c",
            "brazil.cfg" };
        
        try
        {
            return
                Runtime.getRuntime().exec(cmdParams, null, workingDir);
        }
        catch (IOException e)
        {
            throw new CoreException(new Status(
                IStatus.ERROR,
                PerlDebugPlugin.getUniqueIdentifier(),
                IStatus.OK,
                "Could not start embedded web server: Runtime.exec failed",
                e));
        }
    }
    
    private File urlToFile(URL url)
    {
        String urlString = url.toExternalForm();
        
        if (urlString.matches("^file:/[A-Za-z]:/.*$"))
        {
            // Windows URL with volume letter: file:/C:/foo/bar/blah.txt
            return new File(urlString.substring(6));
        }
        else
        {
            // Unix URLs look like this: file:/foo/bar/blah.txt
            assert urlString.matches("^file:/[^/].*$");
            return new File(urlString.substring(5));
        }
    }

    private static class BrazilProps
    {
        private final Properties props;
        
        public BrazilProps()
        {
            props = new Properties();                        
        }
        
        public void add(String name, boolean value)
        {
            add(name, String.valueOf(value));
        }
        
        public void add(String name, int value)
        {
            add(name, String.valueOf(value));
        }
        
        public void add(String name, List<String> values)
        {
            int j = 0;
            for (Iterator<?> i = values.iterator(); i.hasNext(); j++)
                add(name + "[" + j + "]", i.next().toString());
        }
        
        public void add(String name, String value)
        {
            props.put(name, value);
        }
        
        public void save() throws IOException
        {
            File propsFile = PerlDebugPlugin.getDefault().extractTempFile(
                "brazil_cgi_templ.cfg",
                "brazil.cfg");
            
            // Append custom properties:

            OutputStream out = null;            
            try
            {
                out = new FileOutputStream(propsFile, true);
                props.store(out, null);
            }
            finally
            {
                if (out != null) try { out.close(); } catch (Exception e) { }
            }
        }
        
        public String toString()
        {
            return props.toString();
        }
    }
}
