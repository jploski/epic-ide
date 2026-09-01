package org.epic.perleditor.actions;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.epic.core.model.SourceFile;

public interface IOpenDeclarationHandler
{
    public IRegion findDeclaration(SourceFile sourceFile, String searchString);

    public String getLocalSearchString(String searchString);

    public String getSearchString(SourceFile sourceFile, ITextSelection selection);

    public String getTargetModule(String moduleName);
}
