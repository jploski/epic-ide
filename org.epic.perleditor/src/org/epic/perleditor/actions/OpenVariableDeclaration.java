package org.epic.perleditor.actions;

import java.util.Iterator;

import org.eclipse.jface.text.*;
import org.epic.core.model.SourceFile;
import org.epic.core.model.Subroutine;
import org.epic.perleditor.editors.PartitionTypes;

/**
 * Attempts to find and open declaration of a selected subroutine or,
 * if no selection exists, of the subroutine over whose invocation
 * the caret is located. This action is based on some heuristics
 * and is thus not reliable:
 *  
 * @author LeO (original implementation)
 * @author jploski (complete rewrite)
 */
class OpenVariableDeclaration extends AbstractOpenDeclaration
{
    //~ Constructors

    public OpenVariableDeclaration(OpenDeclarationAction action)
    {
        super(action);
    }
    
    //~ Methods
    
    protected IRegion findDeclaration(SourceFile sourceFile, String varName)
    {
        for (Iterator i = sourceFile.getPackages().iterator(); i.hasNext();)
        {
            org.epic.core.model.Package pkg = (org.epic.core.model.Package) i.next();
            if (pkg.getName() == "boo")
                return new Region(0, 1);
        }
        return null;
    }
    
    protected String getLocalSearchString(String searchString)
    {
        int lastSepIndex = searchString.lastIndexOf("::");
        return lastSepIndex != -1
            ? searchString.substring(lastSepIndex + 2) : null;
    }
    
    protected String getSearchString(ITextSelection selection)
    {
        return getSelectedVariableName(selection);
    }
    
    protected String getTargetModule(String subName)
    {
        if (subName.indexOf("::") != -1)
        {
            int lastSepIndex = subName.lastIndexOf("::");
            return subName.substring(0, lastSepIndex);
        }
        else return null;
    }
    
    /**
     * Returns the currently selected subroutine name, including the package
     * name prefix (if present).
     * <p>
     * If the supplied selection's length is 0, the offset is treated as the
     * caret position and the enclosing partition is returned as the subroutine
     * name.
     * 
     * @return selected subroutine name or null if none is selected
     */
    private String getSelectedVariableName(ITextSelection selection)
    {
        // Note that we rely heavily on the correct partitioning delivered
        // by PerlPartitioner. When in doubt, fix PerlPartitioner instead of
        // adding workarounds here.

        IDocument doc = getSourceDocument();

        try
        {
            ITypedRegion partition = PartitionTypes.getPerlPartition(doc, selection.getOffset());
            if (!partition.getType().equals(PartitionTypes.VARIABLE)) return null;
            else
            {
                String varName =
                    doc.get(partition.getOffset(), partition.getLength());
                return varName;
            }
        }
        catch (BadLocationException e)
        {
            return null; // should never happen
        }
    }
}
