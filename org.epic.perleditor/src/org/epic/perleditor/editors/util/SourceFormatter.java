package org.epic.perleditor.editors.util;

import java.io.*;
import java.net.URL;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.IPreferenceStore;
import org.epic.perleditor.PerlEditorPlugin;
import org.epic.perleditor.preferences.PreferenceConstants;
import org.epic.perleditor.preferences.SourceFormatterPreferences;



public class SourceFormatter {
	
	public String doConversion(String text) {
		return doConversion(text, null);
	}
	
	public String doConversion(String text, List additionalOptions) {

	   StringReaderThread srt = new StringReaderThread();
		
       IPreferenceStore store = PerlEditorPlugin.getDefault().getPreferenceStore();
       
	   int numSpaces = store.getInt(PreferenceConstants.INSERT_TABS_ON_INDENT);
	   boolean useTabs = store.getBoolean(PreferenceConstants.SPACES_INSTEAD_OF_TABS) ? false:true;
	   int tabWidth = useTabs?store.getInt(PreferenceConstants.EDITOR_TAB_WIDTH):numSpaces ;
	   int pageSize = store.getInt(PreferenceConstants.EDITOR_PRINT_MARGIN_COLUMN);
	   
	   
	   boolean cuddleElse = store.getBoolean(SourceFormatterPreferences.CUDDLED_ELSE);
	   boolean bracesLeft = store.getBoolean(SourceFormatterPreferences.BRACES_LEFT);
	   boolean lineUpParentheses = store.getBoolean(SourceFormatterPreferences.LINE_UP_WITH_PARENTHESES);
	   boolean swallowOptionalBlankLines = store.getBoolean(SourceFormatterPreferences.SWALLOW_OPTIONAL_BLANK_LINES);
	   
//	   int containerTightnessBraces = store.getInt(SourceFormatterPreferences.CONTAINER_TIGHTNESS_BRACES);
//	   int containerTightnessParentheses = store.getInt(SourceFormatterPreferences.CONTAINER_TIGHTNESS_PARENTHESES);
//	   int containerTightnessSquareBrackets = store.getInt(SourceFormatterPreferences.CONTAINER_TIGHTNESS_SQUARE_BRACKETS);
	   

		String formattedText = null;
		try {		
			URL installURL = PerlEditorPlugin.getDefault().getDescriptor().getInstallURL();
			URL perlTidyURL = Platform.resolve(new URL(installURL,"perlutils/perltidy"));
  
            List  perlParamList =PerlExecutableUtilities.getPerlExecutableCommandLine();
            
            // Get perl executable plus arguments
            List cmdList = PerlExecutableUtilities.getPerlExecutableCommandLine();
            
            cmdList.add("perltidy");
            
            /* Add additional parameters */
            cmdList.add("--indent-columns=" + tabWidth);
			cmdList.add("--maximum-line-length=" + pageSize);
//			cmdList.add("--brace-tightness=" + containerTightnessBraces);
//			cmdList.add("--paren-tightness=" + containerTightnessParentheses);
//			cmdList.add("--square-bracket-tightness=" + containerTightnessSquareBrackets);
			
			if(useTabs) {
				cmdList.add("--entab-leading-whitespace=" + tabWidth);
			}
			
			if(cuddleElse) {
				cmdList.add("--cuddled-else");
			}
			
			if(bracesLeft) {
				cmdList.add("--opening-brace-on-new-line");
			}
			
			if(lineUpParentheses) {
				 cmdList.add("--line-up-parentheses");
			}

			if(swallowOptionalBlankLines) {
				 cmdList.add("--swallow-optional-blank-lines");
			}
			
			
			// Read additional options
			StringTokenizer st = new StringTokenizer(store.getString(SourceFormatterPreferences.PERLTIDY_OPTIONS));
			 while (st.hasMoreTokens()) {
				cmdList.add(st.nextToken());
			 }
			 
			 // Add additionally passed options
			 if(additionalOptions != null) {
			 	for(int i=0; i<additionalOptions.size(); i++) {
					cmdList.add((String) additionalOptions.get(i));
			 	}			
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
			Reader inr = new InputStreamReader(in);
			Writer outw = new OutputStreamWriter(out);
			srt.read(inr);
			
			if(text != null) {
				outw.write(text);
				outw.flush();
			}
				
			outw.close();
			
			formattedText = srt.getResult();
			inr.close();
			in.close();
			srt.dispose();
			

		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		return formattedText;
	}

	
}
