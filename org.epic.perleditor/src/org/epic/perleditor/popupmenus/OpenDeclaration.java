package org.epic.perleditor.popupmenus;

/**
 * This Class opens the Declaration of a Perl Subroutine the class is derived from the cursor postion resp. 
 * what is selected the search order is first in the current file, next in the require statments, 
 * and finally in the Packages
 *  
 * @author LeO
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;


import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.epic.perleditor.PerlEditorPlugin;
import org.epic.perleditor.editors.PerlEditor;
import org.epic.perleditor.editors.perl.PerlCompletionProcessor;
import org.epic.perleditor.views.model.Model;
import org.epic.perleditor.views.util.*;
//import org.epic.perleditor.PerlEditorPlugin;

public class OpenDeclaration extends Action
       implements org.eclipse.ui.IEditorActionDelegate,
                  org.eclipse.ui.IWorkbenchWindowActionDelegate
       {
  private TextEditor fTextEditor;
  private IResource resource;


  public OpenDeclaration() {
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
   */
  public void run(IAction action) {
  	run();
  }
  
  public void run() {
System.out.println("LeO was here");
    String selection;
    List currentFileList = null;
    int[] searchResults = new int[3];
    Vector fileSet = new Vector();
    String fileElement = "";
    IFile checkFile = null;
    
    // If fTextEditor is not set, use the current editor
    fTextEditor = (TextEditor)PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
     
    if (fTextEditor instanceof PerlEditor) {
      //it makes only a sense to do something here, otherwise we do not get any
      // Input
      PerlEditor editor = (PerlEditor) fTextEditor;
      String text = editor.getDocumentProvider().getDocument(editor.getEditorInput()).get();
      selection = getCurrentSelection(editor, text);

      if (selection.length() == 0) {
        messageBox("No valid selection found" , "Within the selection scope no valid SUB-name could be located!");
        return;
      }
      //get the file-list from the require-statement
      String requireRegExpr = "^[\\s]*require\\s+[\"']([^\\s]*.*?)[\"']";
      IWorkbenchPage myPage = PerlEditorPlugin.getWorkbenchWindow().getActivePage();
      IEditorReference[] myEditors = myPage.getEditorReferences();
      IEditorInput input = fTextEditor.getEditorInput();
      resource = (IResource) ((IAdaptable) input).getAdapter(IResource.class);
      String editorID = PerlEditorPlugin.getDefault().getWorkbench().getEditorRegistry()
                        .getDefaultEditor(resource.getFullPath().toString()).getId();

      //we don't wanna search the current editor again.
      fileSet.add(myPage.getActiveEditor().getTitle());

      int maxLen=fileSet.size();
      
      for (int currentPosition = 0; currentPosition < maxLen; currentPosition++) {
        //since we have the text from the current active Editor already, we don't retrieve it again!
        if (currentPosition > 0) {
          fileElement = (String) fileSet.get(currentPosition);
          text = "";
          checkFile = null;
          for (int i = 0; i < myEditors.length; i++) {
            if (myEditors[i].getTitle().equalsIgnoreCase(fileElement)) {
              editor = (PerlEditor) myEditors[i].getEditor(true);
              text = editor.getDocumentProvider().getDocument(
                  editor.getEditorInput()).get();
              break;
            }
          }
        }
        if (text.length() == 0) {
          //no text found in the Editors => let's search the file
          checkFile = resource.getProject().getFile(fileElement);
          text = textFromFile(checkFile);
        }
        if (text.length() > 0) {
          searchResults = searchSelection(text, selection);
          if (searchResults[0] == 1) {
            if (checkFile != null) {
              //neuen Editor aufmachen mit dem File in String 'test'
              IEditorInput inputNew = new FileEditorInput(checkFile);
              try {
                editor = (PerlEditor) myPage.openEditor(inputNew, editorID);
              } catch (PartInitException e1) {
                // TODO Auto-generated catch block
                //should not happen, because we have already read the input from the file
                e1.printStackTrace();
              }
            }
            editor.selectAndReveal(searchResults[1], searchResults[2]);
            myPage.activate(editor);
            return; //we have found the selection in current Editor!
          }
          //there is only something to add, if some text is there
          currentFileList = SourceParser.getElements(text, requireRegExpr, "", "", true);
          addFileName(currentFileList, fileSet);
        }

        if (fileSet.size()==0)
          break;
        
        maxLen=fileSet.size();  //get the new size
      }
      
      //nothing found, otherwise we would have returned already!
      String strRequire="";
      if (fileSet.size() > 1 || currentFileList.size() > 0) {
        strRequire ="\n\n"+"NOTE: The 'require' file-list is case-sensitive!";
      }
      messageBox( "No definition found", "Cannot locate definiton '" + selection + "'"+strRequire);
    }
  }

  /**
   * @param Title
   */
  private void messageBox(String Title, String ErrorMessage) {
    Shell shell;
    shell = PerlEditorPlugin.getWorkbenchWindow().getShell();
    MessageDialog.openInformation(shell, Title, ErrorMessage);
  }

  /**
   * Adds all the Filenames - unique - no doubles!
   * @param currentFileList
   * @param fileSet
   */
  private void addFileName(List currentFileList, Vector fileSet) {
    String fileElement;
    if (currentFileList.size() > 0) {
      //something to add
        for (int j = 0; j < currentFileList.size(); j++) {
          fileElement = ((Model) currentFileList.get(j)).getName();
          int i = 0;
          while (i < fileSet.size() && fileElement.length() > 0) {
            if (fileSet.elementAt(i).equals(fileElement)) {
              fileElement = "";
            }
            i++;
          }
          if (fileElement.length() > 0) {
            fileSet.add(fileElement);
          }
        }
    }
    System.out.println("fileset = " + fileSet);
  }

  /**
   * @param fileName = Name of the File
   * @return text    = Content of the File
   */
  private String textFromFile(IFile myFile) {
    String fileText = "";
    String NextLine = "";
    try {
      BufferedReader textIn = new BufferedReader(
                             new InputStreamReader (myFile.getContents()));
      while (true) {
        try {
          NextLine = textIn.readLine();
        } catch (IOException e1) {
          // TODO Auto-generated catch block
          e1.printStackTrace();
        }
        if (NextLine == null)
          break;
        fileText += NextLine + "\n";
      }
      try {
        textIn.close();
      } catch (IOException e1) {
        // TODO Auto-generated catch block
        e1.printStackTrace();
      }
      
    } catch (CoreException e) {
      // most likely the file does not exists!
      System.out.println("Exception: " + e.getMessage());
    }
    return fileText;
  }

  /**
   * @param text
   * @return [found (no=0, yes=1), startOffset, endOffset]
   */
  private int[] searchSelection(String text, String selection) {
    List subList;
    
    //retrieve all Subs
    subList = SourceParser.getElements(text, "^[\\s]*sub\\s+([^\\n\\r{]+)", "", "", false);

    if (subList != null) {
      for (int i = 0; i < subList.size(); i++) {
        Model model = (Model) subList.get(i);
        if (model.getName().equals(selection)) {
          //we found the place we are searching for
          return new int[] { 1, model.getStart(), model.getLength() };
        }
      }
    }
    
    //retrieve all Packages
    subList = SourceParser.getElements(text, 
				"^[\\s]*use\\s+([^\\s]*[A-Z]+[^;\\s\\n\\r]*)",
        "", "", false);

    if (subList != null) {
      selection += "()";
      List proposals;
      PerlCompletionProcessor myComplete=new PerlCompletionProcessor();
      for (int i = 0; i < subList.size(); i++) {
        Model model = (Model) subList.get(i);
        //get the subs from the Packages
        proposals  = myComplete.getProposalsForClassname(fTextEditor, model.getName());
        System.out.println("For class: " + model.getName() + " we have subs: " + proposals);
        for (Iterator iter = proposals.iterator(); iter.hasNext();) {
          String subClass = (String) iter.next();
          if (subClass.equals(selection)){
	          //we found the place we are searching for
            return new int[] {1,model.getStart(),model.getLength()};
          }
        }
      }
    }
    
    return new int[] { 0, 0, 0 };
  }

  /**
   * Retrieves the current Selection and returns the value
   * 
   * @param editor
   * @param text
   */
  private String getCurrentSelection(PerlEditor editor, String text) {
    String selection = ((TextSelection) fTextEditor.getSelectionProvider().getSelection()).getText();
    if (selection.length() == 0) {
      ISourceViewer viewer = editor.getViewer();
      int cursorPosition = viewer.getTextWidget().getCaretOffset();
      int maxLength = viewer.getTextWidget().getCharCount() - 1;
      int selectionStart = cursorPosition - 1;
      int selectionEnd = cursorPosition;
      //search in front the first letter
      boolean endSearch=false;
      String allowedSubChar="_";
      while (selectionStart >= 0 && !endSearch) { 
        if (!Character.isLetterOrDigit(text.charAt(selectionStart))) {
        //include also allowed special characters for sub-names  
            if (allowedSubChar.indexOf(text.charAt(selectionStart)) <0 ) {
              	endSearch = true;
            }
        }
        if (!endSearch)  
          --selectionStart;
      }
      
      endSearch=false;
      //search at the end for the last letter
      while (selectionEnd <= maxLength && !endSearch) {
        if (!Character.isLetterOrDigit(text.charAt(selectionEnd))) {
        //include also allowed special characters for sub-names  
            if (allowedSubChar.indexOf(text.charAt(selectionEnd)) <0 ) {
              	endSearch = true;
            }
        }
        if (!endSearch)  
          ++selectionEnd;
      }

      selection = text.substring(++selectionStart, selectionEnd);

      System.out.println("selection=" + selection);
    }
    
    try {
    	int myTest = Integer.parseInt(selection);
	    //Selection is only numbers => should Not be selected
  	  	selection ="";
    }
  	catch(NumberFormatException nfe) 
    {
	    //Selection is only special character => should Not be selected
	    if (selection.length() == 1 && !Character.isLetter(selection.charAt(0))){
	      selection = "";
	    }
    }

    return selection;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.IEditorActionDelegate#setActiveEditor(org.eclipse.jface.action.IAction, org.eclipse.ui.IEditorPart)
   */
  public void setActiveEditor(IAction action, IEditorPart targetEditor) {
    // TODO Auto-generated method stub

    fTextEditor = (TextEditor) targetEditor;

  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction, org.eclipse.jface.viewers.ISelection)
   */
  public void selectionChanged(IAction action, ISelection selection) {

  }

  /* (non-Javadoc)
   * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#dispose()
   */
  public void dispose() {
    // TODO Auto-generated method stub
    System.out.println("dispose()");

  }

  /* (non-Javadoc)
   * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#init(org.eclipse.ui.IWorkbenchWindow)
   */
  public void init(IWorkbenchWindow window) {
    // TODO Auto-generated method stub
    fTextEditor = (TextEditor) window.getActivePage().getActiveEditor();
    System.out.println("init()");
    
  }
}
