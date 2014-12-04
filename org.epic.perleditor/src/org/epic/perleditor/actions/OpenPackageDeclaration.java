package org.epic.perleditor.actions;

import java.util.Iterator;

import org.eclipse.jface.text.*;
import org.epic.core.model.SourceFile;
import org.epic.core.model.Package;
import org.epic.perleditor.editors.PartitionTypes;

/**
 * Attempts to find and open declaration of a selected package or,
 * if no selection exists, of the package over whose invocation
 * the caret is located. This action assumes that the package name
 * translates to a module file somewhere on the \@INC path, which
 * is common, but not reliable (a Perl package may be declared in
 * any file, and possibly spread over multiple files).
 *  
 * @author jploski
 */
class OpenPackageDeclaration extends AbstractOpenDeclaration
{
    //~ Constructors

    public OpenPackageDeclaration(OpenDeclarationAction action)
    {
        super(action);
    }
    
    //~ Methods
    
    protected IRegion findDeclaration(SourceFile sourceFile, String moduleName)
    {
        for (Iterator<Package> i = sourceFile.getPackages().iterator(); i.hasNext();)
        {
            Package pkg = i.next();           
            if (pkg.getName().equals(moduleName))
                return new Region(pkg.getOffset(), pkg.getLength());
        }
        return null;
    }
    
    protected String getLocalSearchString(String searchString)
    {
        return searchString;
    }
    
    protected String getSearchString(ITextSelection selection)
    {
        return getSelectedModuleName(selection);
    }
    
    protected String getTargetModule(String moduleName)
    {
        return moduleName;
    }
    
    /**
     * Returns the currently selected module name.
     * <p>
     * If the supplied selection's length is 0, the offset is treated as the
     * caret position and the enclosing partition is returned as the module
     * name.
     * 
     * @return selected module name or null if none is selected
     */
    private String getSelectedModuleName(ITextSelection selection)
    {
        // Note that we rely heavily on the correct partitioning delivered
        // by PerlPartitioner. When in doubt, fix PerlPartitioner instead of
        // adding workarounds here.

        IDocument doc = getSourceDocument();

        try
        {
            ITypedRegion partition = PartitionTypes.getPerlPartition(doc, selection.getOffset());
            if (!partition.getType().equals(PartitionTypes.DEFAULT)) return null;
            else
            {
                String moduleName =
                    doc.get(partition.getOffset(), partition.getLength());
                return moduleName;
            }
        }
        catch (BadLocationException e)
        {
            return null; // should never happen
        }
    }
}
