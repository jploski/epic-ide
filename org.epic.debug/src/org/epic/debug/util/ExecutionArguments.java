/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.epic.debug.util;


import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Platform;

/**
 * The execution arguments for running a Java VM. The execution arguments are
 * separated into two parts: arguments to the VM itself, and arguments to the Java
 * main program. This class provides convenience methods for parsing a string
 * of arguments into seperate components.
 * <p>
 * Clients may instantiate this class; it is not intended to be subclassed.
 * </p>
 */
public class ExecutionArguments {
	private String fVMArgs;
	private String fProgramArgs;

	/**
	 * Creates a new execution arguments object.
	 *
	 * @param vmArgs command line argument string passed to the VM
	 * @param programArgs command line argument string passed to the program
	 */
	public ExecutionArguments(String programArgs) {
		if ( programArgs == null)
			throw new IllegalArgumentException();
		fProgramArgs= programArgs;
	}

	

	/**
	 * Returns the program arguments as one string.
	 *
	 * @return the program arguments as one string
	 */
	public String getProgramArguments() {
		return fProgramArgs;
	}

	
	/**
	 * Returns the program arguments as an array of individual arguments.
	 *
	 * @return the program arguments as an array of individual arguments
	 */
	public String[] getProgramArgumentsArray() {
		return parseArguments(fProgramArgs);
	}

	public List getProgramArgumentsL() {
			return parseArgumentsL(fProgramArgs);
		}
	private static class ArgumentParser {
		private String fArgs;
		private int fIndex= 0;
		private int ch= -1;

		public ArgumentParser(String args) {
			fArgs= args;
		}

		public String[] parseArguments()
		{
			List  v;
			v = parseArgumentsL();
			
			String[] result= new String[v.size()];
		    v.toArray(result);
		    return result;
		}
		
		public List parseArgumentsL() {
			List v= new ArrayList();

			ch= getNext();
			while (ch > 0) {
				while (Character.isWhitespace((char)ch))
					ch= getNext();

				if (ch == '"') {
					v.add(parseString());
				} else {
					v.add(parseToken());
				}
			}

			//String[] result= new String[v.size()];
			//v.toArray(result);
			return v;
		}

		private int getNext() {
			if (fIndex < fArgs.length())
				return fArgs.charAt(fIndex++);
			return -1;
		}

		private String parseString() {
			StringBuffer buf= new StringBuffer();
			ch= getNext();
			while (ch > 0 && ch != '"') {
				if (ch == '\\') {
					ch= getNext();
					if (ch != '"') {           // Only escape double quotes
						buf.append('\\');
					} else {
						if (Platform.getOS().equals(Platform.OS_WIN32)) {
							// @see Bug 26870. Windows requires an extra escape for embedded strings
							buf.append('\\');
						}
					}
				}
				if (ch > 0) {
					buf.append((char)ch);
					ch= getNext();
				}
			}

			ch= getNext();

			return buf.toString();
		}

		private String parseToken() {
			StringBuffer buf= new StringBuffer();

			while (ch > 0 && !Character.isWhitespace((char)ch)) {
				if (ch == '\\') {
					ch= getNext();
					if (Character.isWhitespace((char)ch)) {
						// end of token, don't lose trailing backslash
						buf.append('\\');
						return buf.toString();
					}
					if (ch > 0) {
						if (ch != '"') {        // Only escape double quotes
							buf.append('\\');
						}
						buf.append((char)ch);
						ch= getNext();
					} else if (ch == -1) {     // Don't lose a trailing backslash
						buf.append('\\');
					}
				} else if (ch == '"') {
					buf.append(parseString());
				} else {
					buf.append((char)ch);
					ch= getNext();
				}
			}
			return buf.toString();
		}
	}

	private static String[] parseArguments(String args) {
		if (args == null)
			return new String[0];
		ArgumentParser parser= new ArgumentParser(args);
		String[] res= parser.parseArguments();

		return res;
	}
	
	private static List parseArgumentsL(String args) {
			if (args == null)
				return new ArrayList();
			ArgumentParser parser= new ArgumentParser(args);
			List res= parser.parseArgumentsL();

			return res;
		}
}