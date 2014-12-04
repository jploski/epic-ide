package org.epic.perleditor.templates.perl;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.editors.text.TextEditor;
import org.epic.core.util.PerlExecutor;
import org.epic.perleditor.PerlEditorPlugin;
import org.epic.perleditor.templates.ui.LinkedPositionManager;
import org.epic.perleditor.templates.ui.LinkedPositionUI;

/**
 * @author philipp
 */
public class ModuleCompletionHelper  {
	
	private static ModuleCompletionHelper gInstance;
	
	private final String perlCode;
	
	private String[] moduleNames = new String[0];
	
	private boolean initializing = false;
	
	public static ModuleCompletionHelper getInstance() {
		if (gInstance == null) {
			synchronized(ModuleCompletionHelper.class) {
				if (gInstance == null) {
					gInstance = new ModuleCompletionHelper();
				}
			}
		}
		return gInstance;
	}
	
	private ModuleCompletionHelper() {
		super();
		
		StringBuffer sb = new StringBuffer();
		sb.append("use strict;\n");
		sb.append("use File::Find;\n");
		sb.append("\n");
		sb.append("my %foundmods;\n");
		sb.append("\n");
		sb.append("find ( \n");
		sb.append("  sub {\n");
		sb.append("    return if ((/^\\./) || (! -f $File::Find::name) || (! /\\.pm$/));\n");
		sb.append("\n");
		sb.append("    open(MODFILE,$File::Find::name) || return;\n");
		sb.append("    while (<MODFILE>) { \n");
		sb.append("        if (/^\\s*package\\s+(\\S+);/){\n");
		sb.append("         $foundmods{$1} = 1;\n");
		sb.append("         last; \n");
		sb.append("     } \n");
		sb.append("    }  \n");
		sb.append("    close(MODFILE);\n");
		sb.append("  }, @INC\n");
		sb.append(");\n");
		sb.append("\n");
		sb.append("map { print $_ . \"\\n\"; } sort keys %foundmods;\n");
		
		perlCode = sb.toString();
	}
	
	public void scanForModules(TextEditor textEditor) throws CoreException {
        if (!PerlEditorPlugin.getDefault().requirePerlInterpreter(false)) return;

        synchronized (this) {
            if (initializing) {
                // avoid running multiple perls scanning for modules.
                return;
            }
            initializing = true;
        }

        PerlExecutor executor = new PerlExecutor();
        try
        {
            List<String> names =
                executor.execute(textEditor, null, perlCode).getStdoutLines();
            moduleNames = names.toArray(new String[names.size()]);
        }
        finally {
            executor.dispose(); 
            initializing = false;
        }
	}
	
	public ICompletionProposal[] getProposals(
			String moduleNameFragment, int documentOffset,
			ITextViewer viewer
	) {
		ArrayList<ModuleProposal> al = new ArrayList<ModuleProposal>();

		for (int loop = 0; loop < moduleNames.length; loop++) {
			String moduleName = moduleNames[loop];
			if ((moduleNameFragment == null) || 
					(moduleNameFragment.equals("")) ||
					(moduleName.startsWith(moduleNameFragment))
			) {
				al.add(
					new ModuleProposal(moduleName, moduleNameFragment, 
									   documentOffset, viewer));
			}
		}

		return al.toArray(new ICompletionProposal[0]);
	}
	
	class ModuleProposal implements ICompletionProposal {
		
		private String moduleName;
		private String moduleNameFragment;
		private int documentOffset;
		private ITextViewer fViewer;
		
		public ModuleProposal(String moduleName, String moduleNameFragment, int documentOffset, ITextViewer viewer) {
			this.moduleName = moduleName;
			this.moduleNameFragment = moduleNameFragment;
			this.documentOffset = documentOffset;
			this.fViewer = viewer;
		}
		
		public void apply(IDocument document) {
			String replacementText = moduleName + ";";
			String moriturus = moduleNameFragment;
			int len = moriturus.length();
			
			int start = documentOffset - len;
			
			try {
				document.replace(start, len, replacementText);
				LinkedPositionManager manager = new LinkedPositionManager(document);
				LinkedPositionUI editor = new LinkedPositionUI(fViewer, manager);
				editor.setFinalCaretOffset(replacementText.length() + start);
				editor.enter();
				
			} catch (BadLocationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		public Point getSelection(IDocument document) {
			return null;
		}
		
		public String getAdditionalProposalInfo() {		
			return null;
		}
		
		public String getDisplayString() {
			return moduleName;
		}
		
		public Image getImage() {
			return null;
		}
		
		public IContextInformation getContextInformation() {
			return null;
		}
		
		public String getModuleName() {
			return moduleName;
		}
	
	}
		
}
