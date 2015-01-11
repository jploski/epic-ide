/*
 * CgiHandler.java
 *
 * Brazil project web application toolkit,
 * export version: 2.0
 * Copyright (c) 1998-2002 Sun Microsystems, Inc.
 *
 * Sun Public License Notice
 *
 * The contents of this file are subject to the Sun Public License Version
 * 1.0 (the "License"). You may not use this file except in compliance with
 * the License. A copy of the License is included as the file "license.terms",
 * and also available at http://www.sun.com/
 *
 * The Original Code is from:
 *    Brazil project web application toolkit release 2.0.
 * The Initial Developer of the Original Code is: suhler.
 * Portions created by suhler are Copyright (C) Sun Microsystems, Inc.
 * All Rights Reserved.
 *
 * Contributor(s): cstevens, drach, suhler.
 *
 * Version:  1.22
 * Created by suhler on 98/09/14
 * Last modified by suhler on 02/05/02 11:15:33
 */

package org.epic.debug.cgi.server;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;

import org.epic.debug.util.ExecutionArguments;

import sunlabs.brazil.server.*;

/**
 * Handler for implementing cgi/1.1 interface. This implementation allows either
 * suffix matching (e.g. .cgi) to identify cgi scripts, or prefix matching (e.g.
 * /cgi-bin). Defaults to "/". All output from the cgi script is buffered (e.g.
 * chunked encoding is not supported). <br>
 * NOTE: in versions of Java prior to release 1.3, the ability to set a working
 * directory when running an external process is missing. This handler
 * automatically checks for this ability and sets the proper working directory,
 * but only if the underlying VM supports it.
 * <p>
 * The following request properties are used:
 * <dl class=props>
 * <dt>root
 * <dd>The document root for cgi files
 * <dt>suffix
 * <dd>The suffix for cgi files (defaults to .cgi)
 * <dt>prefix
 * <dd>The prefix for all cgi files (e.g. /cgi-bin)
 * <dt>custom
 * <dd>set to "true" to enable custom environment variables. If set, all server
 * properties starting with this handler's prefix are placed into the
 * environment with the name: <code>CONFIG_<i>name</i></code>, where
 * <i>name </i> is the property key, in upper case, with the prefix removed.
 * This allows cgi scripts to be customized in the server's configuration file.
 * </dl>
 * 
 * @author Stephen Uhler
 * @version 1.22, 02/05/02
 */
public class EpicCgiHandler implements Handler
{
    private static final Object LOCK = new Object();
    
    private static final String ROOT = "root"; // property for document root
    private static final String SUFFIX = "suffix"; // property for suffix string    
    private static final String PREFIX = "prefix"; // all cgi scripts must start with this
    private static final String CUSTOM = "custom"; // add custom query variables

    private static final String ENV = "ENV";

    private static String software = "Mini Java CgiHandler 0.2";
    private static Hashtable<String, String> envMap; // environ maps
    
    private CGIConfig config;

	private Socket diagSocket;
	private Socket outSocket;
	private Socket errorSocket;
    
    private PrintWriter mDiag; // diagnostic info to CGI proxy
    private OutputStream mOut; // forwards CGI stdout to CGI proxy
    private OutputStream mError; // forwards CGI stderr to CGI proxy
    private Exception defaultEnvError;

	/**
	 * construct table of CGI environment variables that need special handling
	 */
	static
    {
		envMap = new Hashtable<String, String>(2);
		envMap.put("content-length", "CONTENT_LENGTH");
		envMap.put("content-type", "CONTENT_TYPE");
	}

	public EpicCgiHandler()
    {
	}

	/**
	 * One time initialization. The handler configuration properties are
	 * extracted and set in {@link #respond(Request)}to allow upstream handlers
	 * to modify the parameters.
	 */
	public boolean init(Server server, String prefix)
    {
        config = new CGIConfig(server, prefix);

        return connectToCGIProxy();
	}

	/**
	 * Dispatch and handle the CGI request. Gets called on ALL requests. Set up
	 * the environment, exec the process, and deal appropriately with the input
	 * and output.
	 * 
	 * In this implementation, all cgi script files must end with a standard
	 * suffix, although the suffix may omitted from the url. The url
	 * /main/do/me/too?a=b will look, starting in DocRoot, for main.cgi,
	 * main/do.cgi, etc until a matching file is found.
	 * <p>
	 * Input parameters examined in the request properties:
	 * <dl>
	 * <dt>Suffix
	 * <dd>The suffix for all cgi scripts (defaults to .cgi)
	 * <dt>DocRoot
	 * <dd>The document root, for locating the script.
	 * </dl>
	 */
	public boolean respond(Request request)
    {
        // The current implementation of EPIC debugger cannot reliably
        // process concurrent debug connections. Therefore, we serialise
        // processing of CGI requests at the web server level (LOCK).

        synchronized (LOCK)
        {
            return respondImpl(request);
        }
    }
    
    private boolean respondImpl(Request request)
    {
		String url = request.props.getProperty("url.orig", request.url);
		String prefix = config.getRequestProperty(request, PREFIX, "/");

		if (!url.startsWith(prefix)) return false;
        if (url.endsWith("favicon.ico")) return false;

		String suffixes = config.getRequestProperty(request, SUFFIX, ".cgi");
		String root = config.getRequestProperty(
            request, ROOT, request.props.getProperty(ROOT, "."));

		request.log(
            Server.LOG_DIAGNOSTIC,
            "suffix=" + suffixes + 
            " root=" + root +
            " url: " + url);
        
        File cgiFile;
        int pathInfoStartI;        
        {
            Object[] ret = findCGIFile(request, url, suffixes, root);
            if (ret == null) return false;
            cgiFile = (File) ret[0];
            pathInfoStartI = ((Integer) ret[1]).intValue();
        }

		String[] command = createCommandLine(request, root, cgiFile);
        String[] env = createEnvironment(
            request, cgiFile, root, url, pathInfoStartI);

        execCGI(request, cgiFile, command, env);
		return true;
	}
    
    /**
     * Opens communication channels to the EPIC CGI proxy running
     * inside of the Eclipse JVM. The script output and diagnostic
     * information are forwarded to this proxy.
     */
    private boolean connectToCGIProxy()
    {
        try
        {
            diagSocket = new Socket("localhost", config.getDiagPort());
            outSocket = new Socket("localhost", config.getOutPort());
            errorSocket = new Socket("localhost", config.getErrorPort());

            mError = errorSocket.getOutputStream();
            mOut = outSocket.getOutputStream();
            mDiag = new PrintWriter(diagSocket.getOutputStream(), true);
            
            if (defaultEnvError != null)
            {
                mDiag.println("Failed to retrieve global environment variables:");
                defaultEnvError.printStackTrace(mDiag);
                mDiag.println("CGI scripts might not be executed properly.");
            }
        }
        catch (UnknownHostException e)
        {
            // TODO: can this ever happen? will anyone see the error message?
            e.printStackTrace();
            return false;
        }
        catch (IOException e)
        {
            // TODO: can this ever happen? will anyone see the error message?
            e.printStackTrace();
            return false;
        }
        return true;
    }
    
    /**
     * @return the command line used to execute the CGI script
     */
    private String[] createCommandLine(Request request, String root, File cgiFile)
    {
        //Get Perl executable and generate comand array
        ArrayList<String> commandList = new ArrayList<String>();
        commandList.add(config.getPerlExecutable());
        
        // Add absolute path to local working directory to make
        // perl -d refer to modules in the same directory by their
        // absolute rather than relative paths (relevant when setting
        // breakpoints).
        //
        // TODO: Cygwin path translation is missing here, this might
        // cause problems; compare to LocalLaunchConfigurationDelegate
        commandList.add("-I" +
            cgiFile.getParentFile().getAbsolutePath().replace('\\', '/'));
        
        commandList.addAll(config.getRunInclude());

        if (config.getDebugMode())
        {
            commandList.add(config.getDebugInclude());
            commandList.add("-d"); // Add debug switch
        }
        
        String perlParams = config.getPerlParams(); 
        if (perlParams != null && perlParams.length() > 0)
        {
            ExecutionArguments exArgs = new ExecutionArguments(perlParams);
            commandList.addAll(exArgs.getProgramArgumentsL());
        }

        String cgiFilePath;
        try { cgiFilePath = cgiFile.getCanonicalPath(); }
        catch (IOException e) { cgiFilePath = cgiFile.getAbsolutePath(); }
        
        // If the user-entered root directory contains slashes, but the canonicalized
        // CGI file path contains backslashes, then normalize backslashes into slashes.
        // This is supposed to avoid skipped breakpoints on Windows due to perl -d source
        // paths being reported with backslashes and expected with slashes by EPIC:
        if (root.indexOf('/') != -1 && cgiFilePath.indexOf("\\") != -1) cgiFilePath = cgiFilePath.replace('\\', '/');
        
        commandList.add(cgiFilePath);

        // Look at the query and check for an =
        // If no '=', then use '+' as an argument delimiter

        if (request.query.indexOf("=") == -1)
            commandList.add(request.query);

        String[] command =
            commandList.toArray(new String[commandList.size()]);

        /*
        for (int i = 0; i < command.length; i++)
            request.log(
                Server.LOG_DIAGNOSTIC,
                "command[" + i + "]= " + command[i]);
        */
        return command;
    }
    
    /**
     * @return the environment passed to the executed CGI script
     */
    private String[] createEnvironment(
        Request request,
        File cgiFile,
        String root,
        String url,
        int pathInfoStartI)
    {
        List<String> env = new ArrayList<String>();

        /*
         * Build the environment array. First, get all the http headers most
         * are transferred directly to the environment, some are handled
         * specially. Multiple headers with the same name are not handled
         * properly.
         */
        Enumeration keys = request.headers.keys();
        while (keys.hasMoreElements())
        {
            String key = (String) keys.nextElement();
            String special = envMap.get(key.toLowerCase());
            if (special != null)
                env.add(special + "=" + request.headers.get(key));
            else env.add(
                "HTTP_" + key.toUpperCase().replace('-', '_') + "=" +
                request.headers.get(key));
        }

        // Add in the rest of them

        env.add("GATEWAY_INTERFACE=CGI/1.1");
        env.add("SERVER_SOFTWARE=" + software);
        env.add("SERVER_NAME=" + config.getHostname());
        env.add("PATH_INFO=" + url.substring(pathInfoStartI));
        
        request.log(Server.LOG_DIAGNOSTIC, "PATH_INFO=" + url.substring(pathInfoStartI));
        String suffix = cgiFile.getName();
        if (suffix.lastIndexOf('.') != -1)
            suffix = suffix.substring(suffix.lastIndexOf('.'));
        else suffix = "";
        
        String pre = url.substring(0, pathInfoStartI);
        if (pre.endsWith(suffix)) { // exact match, likely no PATH_INFO
            env.add("SCRIPT_NAME=" + pre);
            request.log(Server.LOG_DIAGNOSTIC, "SCRIPT_NAME=" + pre + " (ends with "+suffix+")");
        } else if (cgiFile.getName().equals("index"+suffix)) { // directory index match
            env.add("SCRIPT_NAME=" + pre + "index" + suffix);
            request.log(Server.LOG_DIAGNOSTIC, "SCRIPT_NAME=" + pre + "index" + suffix+ " (is index"+suffix+")");
        } else { // shortest prefix matched, append suffix
            env.add("SCRIPT_NAME=" + pre + suffix);
            request.log(Server.LOG_DIAGNOSTIC, "SCRIPT_NAME=" + pre + suffix);
        }
        env.add("SERVER_PORT=" + config.getServerPort());
        env.add("REMOTE_ADDR="
                + request.getSocket().getInetAddress().getHostAddress());
        env.add("PATH_TRANSLATED=" + root + url.substring(pathInfoStartI));
        env.add("REQUEST_METHOD=" + request.method);
        env.add("SERVER_PROTOCOL=" + request.protocol);
        env.add("QUERY_STRING=" + request.query);

        if (config.getProtocol().equals("https")) env.add("HTTPS=on");

        env.add("SERVER_URL=" + request.serverUrl());

        // Append the "custom" environment variables (if requested)

        if (!config.getRequestProperty(request, CUSTOM, "").equals(""))
        {
            Map<String, String> props = config.getProperties("");            
            for (Iterator<String> i = props.keySet().iterator(); i.hasNext();)
            {
                String key = i.next();
                env.add("CONFIG_" + key + "=" + props.get(key)); 
            }            
            env.add("CONFIG_PREFIX=" + config.getPropsPrefix());
        }

        // Append environment variables provided by EPIC
        // (configurable with the CGI Environment tab; if nothing
        // is configured, the environment of the workbench is used)
        
        Map<String, String> userEnv = config.getProperties(ENV + "_");
        for (Iterator<String> i = userEnv.keySet().iterator(); i.hasNext();)
        {
            String key = i.next();
            env.add(key + "=" + userEnv.get(key)); 
        }

        String[] environ = env.toArray(new String[env.size()]);

        return environ;
    }
    
    /**
     * Executes the given CGI script file using the provided
     * command line and environment. Script stdout is returned
     * both to the browser and to the CGI proxy. Script stderr
     * is returned only to the CGI proxy.
     */
    private void execCGI(
        Request request,
        File cgiFile,
        String[] command,
        String[] env)
    {        
        mDiag.println("***********************************************************");
        mDiag.println("Requested URI: " +
            request.props.getProperty("url.orig", request.url));
        mDiag.println("---------------------CGI Command Line----------------------");
        for (int i = 0; i < command.length; i++) mDiag.println(command[i]);
        mDiag.println("-------------------Environment Variables-------------------");
        for (int i = 0; i < env.length; i++) mDiag.println(env[i]);

        Process cgi = null;
        StreamForwarder stderrFwd = null;
        
        try
        {
            cgi = Runtime.getRuntime().exec(
                command, env, new File(cgiFile.getParentFile().getCanonicalPath()));

            DataInputStream in = new DataInputStream(
                new BufferedInputStream(cgi.getInputStream()));

            // If we have data, send it to the process

            if (request.postData != null)
            {
                OutputStream toCgi = cgi.getOutputStream();
                toCgi.write(request.postData, 0, request.postData.length);
                toCgi.close();
                mDiag.println("------------------------POST data--------------------------");
                mDiag.println(new String(request.postData, "ISO-8859-1"));
                mDiag.flush();
            }
            
            stderrFwd = new StreamForwarder(
                "EpicCgiHandler.readError",
                new BufferedInputStream(cgi.getErrorStream()),
                mError);
            
            stderrFwd.start();

            mDiag.println("-----------------------Script Output-----------------------");
            
            // Now get the output of the cgi script. Start by reading the
            // "mini header", then just copy the rest

            String head;
            String type = "text/html";
            int status = 200;
            while (true)
            {
                head = in.readLine();
                if (head == null || head.length() == 0)
                {
                    mOut.write("\r\n".getBytes());
                    mOut.flush();
                    break;
                }
                mOut.write(head.getBytes("ISO-8859-1"));
                mOut.write("\r\n".getBytes());
                mOut.flush();
                
                int colonIndex = head.indexOf(':');
                if (colonIndex < 0)
                {
                    request.sendError(500, "Missing header from cgi output");
                    mError.write(
                        "Error 500: Missing header from cgi output"
                        .getBytes("ASCII"));
                    return;
                }

                String lower = head.toLowerCase();
                if (lower.startsWith("status:"))
                {
                    try
                    {
                        status = Integer.parseInt(
                            head.substring(colonIndex + 1).trim());
                    }
                    catch (NumberFormatException e) { }
                }
                else if (lower.startsWith("content-type:"))
                {
                    type = head.substring(colonIndex + 1).trim();
                }
                else if (lower.startsWith("location:"))
                {
                    status = 302;
                    request.addHeader(head);
                }
                else
                {
                    request.addHeader(head);
                }
            }

            /*
             * Now copy the rest of the data into a buffer, so we can count it.
             * we should be doing chunked encoding for 1.1 capable clients XXX
             */

            ByteArrayOutputStream buff = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int bread;
            while ((bread = in.read(buf, 0, buf.length)) > 0)
            {
                buff.write(buf, 0, bread);
                mOut.write(buf, 0, bread);
                mOut.flush();
            }

            request.sendHeaders(status, type, buff.size());
            buff.writeTo(request.out);
            request.log(Server.LOG_DIAGNOSTIC, "CGI output " + buff.size() + " bytes.");
            cgi.waitFor();            
        }
        catch (Exception e)
        {
            if (cgi != null) cgi.destroy();
            
            StringWriter trace = new StringWriter();
            e.printStackTrace(new PrintWriter(trace, true));
            request.sendError(500, "CGI failure", trace.toString());
            try
            {
                mError.write(
                    ("Error 500: " + "CGI failure: " + e.getMessage()).getBytes("ASCII")
                    );
                e.printStackTrace(new PrintStream(mError));
            }
            catch (IOException _e) { /* not much we can do really */}
        }
        finally
        {
            try
            {
                if (stderrFwd != null) stderrFwd.join();
            }
            catch (Exception e) { }
        }
    }
    
    /**
     * Resolves the CGI script file to be executed based on the
     * requested URI, configured CGI suffixes and the root directory.  
     * 
     * @return null if the resolution algorithm fails;
     *         otherwise a 2-element array with:
     *         File cgiFile (the resolved CGI script file) and
     *         Integer pathInfoStartIndex (index in the uri at
     *         which the CGI path ends and PATH_INFO to be passed
     *         into the script begins)
     */
    private Object[] findCGIFile(
        Request request,
        String uri,
        String suffixes,
        String root)
    {
        // Check if the requested URI refers directly to a file or directory.
        // If it is a file, we got our match - exit early.
        // If it is a directory, try to locate an index.(suffix) file below it.
        // Otherwise, proceed with the shortest prefix resolution algorithm.
        
        File fileAtURI = new File(root, uri);
        if (fileAtURI.isFile())
        {
            StringTokenizer tok = new StringTokenizer(suffixes, ",");
            String suffix = null;
            while (tok.hasMoreTokens())
            {
                suffix = tok.nextToken();
                if (uri.endsWith(suffix))
                {
                    request.log(Server.LOG_DIAGNOSTIC, "Found CGI at uri: " + fileAtURI);
                    return new Object[] { fileAtURI, new Integer(uri.length()) };
                }
            }
            request.log(Server.LOG_DIAGNOSTIC, "Found non-CGI at uri: " + fileAtURI);
            return null;
        }
        else if (fileAtURI.isDirectory())
        {
            request.log(Server.LOG_DIAGNOSTIC, "Found directory at uri: " + fileAtURI);
            
            StringTokenizer tok = new StringTokenizer(suffixes, ",");
            String suffix = null;
            while (tok.hasMoreTokens()) 
            {
                suffix = tok.nextToken();
                File indexFile = new File(root, uri + "index" + suffix);
                request.log(Server.LOG_DIAGNOSTIC, "Trying " + indexFile);
                if (indexFile.isFile()) 
                {
                    request.log(Server.LOG_DIAGNOSTIC, "Found CGI index at uri: " + indexFile);
                    return new Object[] { indexFile, new Integer(uri.length()) };
                }
            }
            request.log(Server.LOG_DIAGNOSTIC, "No CGI index found at uri: " + fileAtURI);
        }
        
        // Try to find the shortest prefix in uri which can be mapped
        // to an existing CGI script. This is to correctly extract
        // PATH_INFO from URLs
        // like http://localhost/cgi-bin/foo.cgi/some/path/info
        // or even http://localhost/cgi-bin/foo/some/path/info
        // (PATH_INFO=/some/path/info)
        
        String suffix = null;
        StringTokenizer tok = new StringTokenizer(suffixes, ",");        
        int start = 1;
        int end = 0;

        while (tok.hasMoreTokens())
        {
            suffix = tok.nextToken();
            request.log(Server.LOG_DIAGNOSTIC, "Checking for suffix: " + suffix);
            start = 1;
            end = 0;
            
            // Search for the shortest matching prefix
            while (end < uri.length())
            {
                end = uri.indexOf('/', start);
                if (end < 0) end = uri.length();

                String s = uri.substring(1, end);
                if (!s.endsWith(suffix)) s += suffix;

                File cgiFile = new File(root, s);
                request.log(Server.LOG_DIAGNOSTIC, "Trying " + cgiFile);
                if (cgiFile.isFile())
                {
                    request.log(Server.LOG_DIAGNOSTIC, "Found CGI at uri: " + cgiFile);
                    return new Object[] { cgiFile, new Integer(end) };
                }
                start = end + 1;
            }
        }        
        return null;
    }
}