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

package org.epic.debug.cgi;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

import sunlabs.brazil.server.Handler;
import sunlabs.brazil.server.Request;
import sunlabs.brazil.server.Server;

/**
 * Handler for implementing cgi/1.1 interface. This implementation allows either
 * suffix matching (e.g. .cgi) to identify cgi scripts, or prefix matching (e.g.
 * /bgi-bin). Defaults to "/". All output from the cgi script is buffered (e.g.
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
/* STR */
public class EpicCgiHandler implements Handler {
	private boolean mDebug;
	private String mDebugInclude;
	private ArrayList mRunInclude;
	private PrintWriter mInWriter;
	private PrintWriter mOutWriter;
	private PrintWriter mErrorWriter;
	private String propsPrefix; // string prefix in properties table
	private int port; // The listening port
	private String protocol; // the access protocol http/https
	private String hostname; // My hostname
	private static final String ROOT = "root"; // property for document root
	private static final String SUFFIX = "suffix";
	// property for suffix string
	private static final String PREFIX = "prefix";
	// All cgi scripts must start with this
	private static final String CUSTOM = "custom";
	// add custom query variables

	private static final String EXECUTABLE = "executable";
	private static final String ENV = "ENV";

	private static String software = "Mini Java CgiHandler 0.2";
	private static Hashtable envMap; // environ maps

	private Socket mInSocket;
	private Socket mOutSocket;
	private Socket mErrorSocket;
	private DataInputStream mErrorProcess;

	/**
	 * construct table of CGI environment variables that need special handling
	 */

	static {
		envMap = new Hashtable(2);
		envMap.put("content-length", "CONTENT_LENGTH");
		envMap.put("content-type", "CONTENT_TYPE");
	}

	public EpicCgiHandler() {
	}

	/**
	 * One time initialization. The handler configuration properties are
	 * extracted and set in {@link #respond(Request)}to allow upstream handlers
	 * to modify the parameters.
	 */

	public boolean init(Server server, String prefix) {
		propsPrefix = prefix;
		port = server.listen.getLocalPort();
		hostname = server.hostName;
		protocol = server.protocol;
		int portIn = Integer.parseInt(server.props.getProperty(propsPrefix
				+ "InPort", null));
		int portOut = Integer.parseInt(server.props.getProperty(propsPrefix
				+ "OutPort", null));
		int portError = Integer.parseInt(server.props.getProperty(propsPrefix
				+ "ErrorPort", null));
		mDebug = server.props.getProperty(propsPrefix + "Debug", null)
				.equalsIgnoreCase("true");
		mDebugInclude = server.props.getProperty(propsPrefix + "DebugInclude",
				null);
		mRunInclude = new ArrayList();
		int x = 0;
		String inc = null;
		do {
			inc = server.props.getProperty(propsPrefix + "RunInclude[" + x
					+ "]", null);
			++x;
			if (inc != null)
				mRunInclude.add(inc);
		} while (inc != null);

		//System.out.println("**************Ports: "+portIn+" "+portOut+"
		// "+portError+"\n");
		try {
			mInSocket = new Socket("localhost", portIn);
			mOutSocket = new Socket("localhost", portOut);
			mErrorSocket = new Socket("localhost", portError);

			mErrorWriter = new PrintWriter(mErrorSocket.getOutputStream(), true);
			mOutWriter = new PrintWriter(mOutSocket.getOutputStream(), true);
			mInWriter = new PrintWriter(mInSocket.getOutputStream(), true);
		} catch (UnknownHostException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		//		mInWriter.println("IN*****************");
		//		mOutWriter.println("Out*****************");
		//		mErrorWriter.println("Debug*****************"+mDebug+"\n");
		return true;
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

	public boolean respond(Request request) {
		String[] command; // The command to run
		ArrayList commandList = new ArrayList();
		Process cgi; // The result of the cgi process

		// Find the cgi script associated with this request.
		// + turn the url into a file name
		// + search path until a script is found

		String url = request.props.getProperty("url.orig", request.url);
		String prefix = request.props.getProperty(propsPrefix + PREFIX, "/");

		if (!url.startsWith(prefix)) {
			return false;
		}

		boolean useCustom = !request.props
				.getProperty(propsPrefix + CUSTOM, "").equals("");
		String suffixes = request.props.getProperty(propsPrefix + SUFFIX,
				".cgi");
		String root = request.props.getProperty(propsPrefix + ROOT,
				request.props.getProperty(ROOT, "."));
		request.log(Server.LOG_DIAGNOSTIC, propsPrefix + " suffix=" + suffixes
				+ " root=" + root + " url: " + url);
		String suffix = null;
		StringTokenizer tok = new StringTokenizer(suffixes, ",");
		File name = null;
		int start = 1;
		int end = 0;
		while (tok.hasMoreTokens()) {
			suffix = tok.nextToken();
			request
					.log(Server.LOG_DIAGNOSTIC, "Checking for suffix: "
							+ suffix);
			start = 1;
			end = 0;
			name = null;
			while (end < url.length()) {
				end = url.indexOf(File.separatorChar, start);
				if (end < 0) {
					end = url.length();
				}
				String s = url.substring(1, end);
				if (!s.endsWith(suffix)) {
					s += suffix;
				}
				name = new File(root, s);
				request.log(Server.LOG_DIAGNOSTIC, propsPrefix
						+ " looking for: " + name);
				if (name.isFile()) {
					break;
				}
				name = null;
				start = end + 1;
			}
			if (name != null)
				break;
		}
		if (name == null) {
			return false;
		}

		request.log(Server.LOG_DIAGNOSTIC, "Suffix: " + suffix);
		// Formulate the command. Look at the query and check for an =
		// If no '=', then use '+' as an argument delimeter

		String query = request.query;

		//Get Perl executable and generate comand array
		commandList.add(request.props.getProperty(propsPrefix + EXECUTABLE,
				"perl"));
		if (mRunInclude.size() > 0)
			commandList.addAll(mRunInclude);

		if (mDebug) {
			commandList.add(mDebugInclude);
			commandList.add("-d"); // Add debug switch
		}

		commandList.add(name.getAbsolutePath());

		if (query.indexOf("=") == -1) { // need args
			commandList.add(query);
		}

		command = (String[]) commandList
				.toArray(new String[commandList.size()]);
		for (int x = 0; x < command.length; ++x)
			request.log(Server.LOG_DIAGNOSTIC, propsPrefix + " command[" + x
					+ "]= " + command[x]);

		/*
		 * Build the environment string. First, get all the http headers most
		 * are transferred directly to the environment, some are handled
		 * specially. Multiple headers with the same name are not handled
		 * properly.
		 */

		Vector env = new Vector();
		createEnvArray(env);
		Enumeration keys = request.headers.keys();
		while (keys.hasMoreElements()) {
			String key = (String) keys.nextElement();
			String special = (String) envMap.get(key.toLowerCase());
			if (special != null) {
				env.addElement(special + "=" + request.headers.get(key));
			} else {
				env.addElement("HTTP_" + key.toUpperCase().replace('-', '_')
						+ "=" + request.headers.get(key));
			}
		}

		// Add in the rest of them

		env.addElement("GATEWAY_INTERFACE=CGI/1.1");
		env.addElement("SERVER_SOFTWARE=" + software);
		env.addElement("SERVER_NAME=" + hostname);
		env.addElement("PATH_INFO=" + url.substring(end));

		String pre = url.substring(0, end);
		if (pre.endsWith(suffix)) {
			env.addElement("SCRIPT_NAME=" + pre);
		} else {
			env.addElement("SCRIPT_NAME=" + pre + suffix);
		}
		env.addElement("SERVER_PORT=" + port);
		env.addElement("REMOTE_ADDR="
				+ request.getSocket().getInetAddress().getHostAddress());
		env.addElement("PATH_TRANSLATED=" + root + url.substring(end));
		env.addElement("REQUEST_METHOD=" + request.method);
		env.addElement("SERVER_PROTOCOL=" + request.protocol);
		env.addElement("QUERY_STRING=" + request.query);

		if (protocol.equals("https")) {
			env.addElement("HTTPS=on");
		}
		env.addElement("SERVER_URL=" + request.serverUrl());

		/*
		 * add in the "custom" environment variables, if requested
		 */

		if (useCustom) {
			int len = propsPrefix.length();
			keys = request.props.propertyNames();
			while (keys.hasMoreElements()) {
				String key = (String) keys.nextElement();
				if (key.startsWith(propsPrefix)) {
					env.addElement("CONFIG_" + key.substring(len).toUpperCase()
							+ "=" + request.props.getProperty(key, null));
				}
			}
			env.addElement("CONFIG_PREFIX=" + propsPrefix);

		}

		// Set custom environment variables
		keys = request.props.propertyNames();
		while (keys.hasMoreElements()) {
			String key = (String) keys.nextElement();

			if (key.startsWith(propsPrefix + ENV + "_")) {
				request.log(Server.LOG_DIAGNOSTIC, key);
				env.addElement(key
						.substring((propsPrefix + ENV + "_").length())
						+ "=" + request.props.getProperty(key, null));
			}
		}

		String environ[] = new String[env.size()];
		env.copyInto(environ);
		request.log(Server.LOG_DIAGNOSTIC, propsPrefix + " ENV= " + env);

		// Run the script
		mInWriter
				.println("***********************************************************");
		mInWriter
				.println("-------------------Environmemt Variables-------------------");
		for (int i = 0; i < environ.length; i++) {
			mInWriter.println(environ[i]);
		}
		mInWriter
				.println("------------------------------------------------------------");
		try {
			cgi = exec(command, environ, new File(name.getParent()));

			DataInputStream in = new DataInputStream(new BufferedInputStream(
					cgi.getInputStream()));
			mErrorProcess = new DataInputStream(
					new BufferedInputStream(cgi.getErrorStream()));
			Thread readError = new Thread() {
				public void run() {
					int c;
					try {
						while ((c = mErrorProcess.read()) >= 0) {
							mErrorWriter.print((char) c);
							mErrorWriter.flush();
						}
					} catch (IOException e) {
						// TODO Auto-generated catch block
						
					}
				}
			};
			readError.start();
			// If we have data, send it to the process

			if (request.postData != null) {
				OutputStream toGci = cgi.getOutputStream();
				toGci.write(request.postData, 0, request.postData.length);
				toGci.close();
				mInWriter.print(request.postData);
				mInWriter.flush();
			}

			// Now get the output of the cgi script. Start by reading the
			// "mini header", then just copy the rest

			String head;
			String type = "text/html";
			int status = 200;
			while (true) {
				head = in.readLine();
				if (head == null || head.length() == 0) {
					break;
				}
				mOutWriter.println(head);
				int colonIndex = head.indexOf(':');
				if (colonIndex < 0) {
					request.sendError(500, "Missing header from cgi output");
					mErrorWriter
							.println("Error 500: Missing header from cgi output");
					return true;
				}
				String lower = head.toLowerCase();
				if (lower.startsWith("status:")) {
					try {
						status = Integer.parseInt(head
								.substring(colonIndex + 1).trim());
					} catch (NumberFormatException e) {
					}
				} else if (lower.startsWith("content-type:")) {
					type = head.substring(colonIndex + 1).trim();
				} else if (lower.startsWith("location:")) {
					status = 302;
					request.addHeader(head);
				} else {
					request.addHeader(head);
				}
			}

			/*
			 * Now copy the rest of the data into a buffer, so we can count it.
			 * we should be doing chunked encoding for 1.1 capable clients XXX
			 */

			ByteArrayOutputStream buff = new ByteArrayOutputStream();
			int c;
			int y = 0;
			while ((c = in.read()) >= 0) {
				buff.write(c);
				mOutWriter.print((char) c);
				mOutWriter.flush();
			}

			request.sendHeaders(status, type, buff.size());
			buff.writeTo(request.out);
			request.log(Server.LOG_DIAGNOSTIC, propsPrefix, "Cgi output "
					+ buff.size());
			cgi.waitFor();
			readError.join();
		} catch (Exception e) {
			// System.out.println("oops: " + e);
			//e.printStackTrace();
			request.sendError(500, "CGI failure", e.getMessage());
			mErrorWriter.println("Error 500: " + "CGI failure: "
					+ e.getMessage());
			e.printStackTrace(mErrorWriter);
		}
		return true;
	}
	private int params;
	private Method execMethod;

	private void search() throws Exception {
		Method[] m = Runtime.class.getDeclaredMethods();
		int n = -1;
		for (int i = 0; i < m.length; i++) {
			if (m[i].getName().equals("exec")) {
				Class[] c = m[i].getParameterTypes();
				if (c.length == 3 && c[0] == String[].class) {
					// we have 3 arg exec for sure
					params = 3;
					n = i;
					break;
				}
				if (c.length == 2 && c[0] == String[].class) {
					// let's save it in case we need it, but keep looking
					params = 2;
					n = i;
				}
			}
		}

		if (n == -1) {
			throw new Exception(
					"No method exec(String[], ...) found in Runtime");
		}

		execMethod = m[n];
	}

	private Object[] args;

	private Process exec(String[] cmd, String[] envp, File dir)
			throws Exception {
		if (execMethod == null) {
			search();
			if (params == 2) {
				args = new Object[2];
			} else {
				args = new Object[3];
			}
		}

		args[0] = cmd;
		args[1] = envp;
		if (params == 3) {
			args[2] = dir;
		}

		return (Process) execMethod.invoke(Runtime.getRuntime(), args);
	}

	/** ******************************************** */
	//	private final static String mDebugOptions =
	//"PERLDB_OPTS=RemotePort=localhost:4444 DumpReused ReadLine=0";
	//	 frame=2";
	void createEnvArray(Vector fEnv) {
		String mDebugEnv[];
		Process proc = null;
		String env = null;
		int count;
		try {
			proc = Runtime
					.getRuntime()
					.exec(
							"perl  -e\"while(($k,$v)= each %ENV){ print\\\"$k=$v\\n\\\";}\"");
		} catch (Exception e) {
			System.out.println("Failing to create Process !!!");
		}

		InputStream in = proc.getInputStream();
		StringBuffer content = new StringBuffer();

		byte[] buffer = new byte[1];

		try {
			while ((count = in.read(buffer)) > 0) {
				content.append(new String(buffer));
			}

			env = content.toString();
			in.close();
		} catch (Exception e) {
		};

		StringTokenizer s = new StringTokenizer(env, "\r\n");
		count = s.countTokens();

		String token;

		for (int x = 0; x < count; ++x) {
			token = s.nextToken();
			fEnv.addElement(token);
		}

		//	fEnv.addElement(mDebugOptions);

		//mDebugEnv[count+1] = "PERL5DB=BEGIN {require'perl5db.pl'}";
	}

}