package org.epic.perleditor.actions;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.epic.core.model.SourceFile;
import org.epic.perleditor.editors.PerlEditor;

public class ContributedOpenDeclarationHandler extends AbstractOpenDeclaration
{

    private final PerlEditor editor;
    private final IOpenDeclarationHandler handler;

    public ContributedOpenDeclarationHandler(OpenDeclarationAction action, IOpenDeclarationHandler handler)
    {
        super(action);
        this.editor = action.getEditor();
        this.handler = handler;
    }

    protected IRegion findDeclaration(SourceFile sourceFile, String searchString) throws CoreException
    {
        return handler.findDeclaration(sourceFile, searchString);
    }

    @Override
    protected String getLocalSearchString(String searchString)
    {
        return handler.getLocalSearchString(searchString);
    }

    @Override
    protected String getSearchString(ITextSelection selection)
    {
        return handler.getSearchString(editor.getSourceFile(), selection);
    }

    @Override
    protected String getTargetModule(String moduleName)
    {
        return handler.getTargetModule(moduleName);
    }

}
