package org.epic.perl6editor.editors;

import org.eclipse.ui.editors.text.TextEditor;

public class PerlEditor extends TextEditor
{

    private ColorManager colorManager;

    public PerlEditor()
    {
        super();
        colorManager = new ColorManager();
        setSourceViewerConfiguration( new PerlConfiguration( colorManager ));
        setDocumentProvider( new PerlDocumentProvider());
	}

    public void dispose()
    {
        colorManager.dispose();
        super.dispose();
	}

}

// END