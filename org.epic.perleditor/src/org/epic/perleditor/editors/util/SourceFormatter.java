package org.epic.perleditor.editors.util;

import java.io.*;
import java.net.URL;
import java.util.List;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.IPreferenceStore;
import org.epic.perleditor.PerlEditorPlugin;
import org.epic.perleditor.preferences.PreferenceConstants;
import org.epic.perleditor.preferences.SourceFormatterPreferences;

import gnu.regexp.RE;
import gnu.regexp.REMatch;
import gnu.regexp.REException;

public class SourceFormatter {
	public String doConversion(String text) {

       IPreferenceStore store = PerlEditorPlugin.getDefault().getPreferenceStore();
       
	   int tabWidth = store.getInt(PreferenceConstants.EDITOR_TAB_WIDTH);
	   int pageSize = store.getInt(PreferenceConstants.EDITOR_PRINT_MARGIN_COLUMN);
	   boolean useTabs = store.getBoolean(PreferenceConstants.SPACES_INSTEAD_OF_TABS) ? false:true;
	   
	   boolean cuddleElse = store.getBoolean(SourceFormatterPreferences.CUDDLED_ELSE);
	   boolean bracesLeft = store.getBoolean(SourceFormatterPreferences.BRACES_LEFT);
	   boolean lineUpParentheses = store.getBoolean(SourceFormatterPreferences.LINE_UP_WITH_PARENTHESES);
	   
	   int containerTightnessBraces = store.getInt(SourceFormatterPreferences.CONTAINER_TIGHTNESS_BRACES);
	   int containerTightnessParentheses = store.getInt(SourceFormatterPreferences.CONTAINER_TIGHTNESS_PARENTHESES);
	   int containerTightnessSquareBrackets = store.getInt(SourceFormatterPreferences.CONTAINER_TIGHTNESS_SQUARE_BRACKETS);
	   

		String formattedText = null;
		try {		
			URL installURL = PerlEditorPlugin.getDefault().getDescriptor().getInstallURL();
			URL perlTidyURL = Platform.resolve(new URL(installURL,"perlutils/perltidy"));
		
		    String perlBin = PerlEditorPlugin.getDefault().getExecutablePreference().trim();
  
            List  cmdList =PerlExecutableUtilities.getPerlExecutableCommandLine();
            cmdList.add("perltidy");
            
            /* Add additional parameters */
            cmdList.add("-i=" + tabWidth);
			cmdList.add("-l=" + pageSize);
			cmdList.add("-bt=" + containerTightnessBraces);
			cmdList.add("-pt=" + containerTightnessParentheses);
			cmdList.add("-sbt=" + containerTightnessSquareBrackets);
			
			if(useTabs) {
				cmdList.add("-et=" + tabWidth);
			}
			
			if(cuddleElse) {
				cmdList.add("-ce");
			}
			
			if(bracesLeft) {
				cmdList.add("-bl");
			}
			
			if(lineUpParentheses) {
				 cmdList.add("-lp");
			}
			
            
			String[] cmdParams = (String[]) cmdList.toArray(new String[cmdList.size()]);

			Process proc =
				Runtime.getRuntime().exec(
					cmdParams,
					null,
					new File(perlTidyURL.getPath()));
					
			Thread.sleep(1);
			proc.getErrorStream().close();
			InputStream in = proc.getInputStream();
			OutputStream out = proc.getOutputStream();
			Writer outw = new OutputStreamWriter(out);
			outw.write(text);
			outw.flush();
			outw.close();
			
			formattedText = PerlExecutableUtilities.readStringFromStream(in);
			in.close();

		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		return formattedText;

		//		String result = "";
		//		try {
		//			String lineIn;
		//			int indent = 0;
		//			//int tabs = 4;
		//			BufferedReader br = new BufferedReader(new StringReader(text));
		//			while ((lineIn = br.readLine()) != null) {
		//				//Handle POD comments
		//				if (lineIn.startsWith("=")) {
		//					String pod = handlePodComments(lineIn, br);
		//					if(pod.length() > 0) {
		//						result += pod;
		//						continue;
		//					}
		//				}
		//				lineIn = lineIn.trim();
		//
		//				// Escape "(" and ")" for regularExpr with "\\"
		//				if (compareOccurrences(lineIn, "}", "{") > 0
		//					|| compareOccurrences(lineIn, "\\)", "\\(") > 0) {
		//					indent--;
		//				}
		//
		//                
		//				String prefix = "";
		//				for (int i = 0; i < indent; i++) {
		//					prefix += PreferenceUtil.getIndent();
		//				}
		//				
		//
		//				result += prefix + lineIn + "\n";
		//
		//				// Escape "(" and ")" for regularExpr with "\\"
		//				if (compareOccurrences(lineIn, "{", "}") > 0
		//					|| compareOccurrences(lineIn, "\\(", "\\)") > 0) {
		//					indent++;
		//				}
		//				// Handle HERE script
		//				if (lineIn.indexOf("<") != -1) {
		//					result += handleHereScript(lineIn, br);
		//				}
		//			}
		//		} catch (Exception ex) {
		//			ex.printStackTrace();
		//		}
		//		return result;
	}

	private int compareOccurrences(
		String line,
		String string1,
		String string2) {
		RE re;
		String[] doNotInspectBetween = { "\"", "'", "/", "~", "|" };
		try {
			// Delete comments
			re = new RE("#.*$");
			line = re.substitute(line, "");
			// Remove backslash characters
			re = new RE("\\\\.");
			line = re.substitute(line, "");
			for (int i = 0; i < doNotInspectBetween.length; i++) {
				re =
					new RE(
						doNotInspectBetween[i]
							+ ".*?"
							+ doNotInspectBetween[i]);
				line = re.substitute(line, "");
			}
			re = new RE(string1);
			REMatch[] matches1 = re.getAllMatches(line);
			re = new RE(string2);
			REMatch[] matches2 = re.getAllMatches(line);
			return matches1.length - matches2.length;
		} catch (REException ex) {
			ex.printStackTrace();
			return 0;
		}
	}
	private String handleHereScript(String line, BufferedReader br) {
		String hereBlock = "";
		try {
			RE re = new RE("<<[^a-zA-Z]*?([a-zA-Z]+)");
			REMatch[] matches = re.getAllMatches(line);
			if (matches.length == 0) {
				return hereBlock;
			}
			int found = 0;
			String content = "";
			while (found < matches.length
				&& (content = br.readLine()) != null) {
				hereBlock += content + "\n";

				if (matches.length == found
					&& matches[found].toString(1).equals("x")) {
					break;
				}
				if (content.equals(matches[found].toString(1))) {
					found++;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return hereBlock;
	}
	private String handlePodComments(String line, BufferedReader br) {
		String podBlock = "";
		try {
			RE re = new RE("^=[a-zA-Z]+");
			REMatch[] matches = re.getAllMatches(line);
			if (matches.length == 0) {
				return podBlock;
			}
			podBlock = line + "\n";
			String content = "";
			while ((content = br.readLine()) != null) {
				podBlock += content + "\n";
				if (content.equals("=cut")) {
					break;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return podBlock;
	}
}
