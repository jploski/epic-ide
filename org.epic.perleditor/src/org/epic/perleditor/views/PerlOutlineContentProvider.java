package org.epic.perleditor.views;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.epic.core.model.*;
import org.epic.core.model.Package;

public class PerlOutlineContentProvider implements ITreeContentProvider
{
    static final String MODULES = " Modules";
    static final String SUBROUTINES = " Subroutines";
    
    private static final Object[] EMPTY_ARRAY = new Object[0];
    private SourceFile model;
    
    public PerlOutlineContentProvider()
    {        
    }

    public void dispose()
    {
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
    {
        model = (SourceFile) newInput;
    }

    public Object[] getChildren(Object parentElement)
    {
        if (parentElement instanceof SourceFile)
        {
            return model.getPackages().toArray();
        }
        else if (parentElement instanceof Package)
        {
            Package pkg = (Package) parentElement;
            PackageElem[] ret = new PackageElem[2];
            ret[0] = new PackageElem(pkg, MODULES);
            ret[1] = new PackageElem(pkg, SUBROUTINES);
            return ret;
        }
        else if (parentElement instanceof PackageElem)
        {
            PackageElem elem = (PackageElem) parentElement;
            
            if (elem.name.equals(MODULES))
            {
                return elem.pkg.getUses().toArray();
            }
            else if (elem.name.equals(SUBROUTINES))
            {
                return elem.pkg.getSubs().toArray();
            }
            else assert false;
        }
        return EMPTY_ARRAY;
    }

    public Object getParent(Object element)
    {
        if (element instanceof Subroutine)
            return ((Subroutine) element).getParent();
        else if (element instanceof ModuleUse)
            return ((ModuleUse) element).getParent();
        else if (element instanceof PackageElem)
            return ((PackageElem) element).pkg;
        else if (element instanceof Package)
            return model;

        return null;
    }

    public boolean hasChildren(Object element)
    {
        return getChildren(element).length > 0;
    }

    public Object[] getElements(Object inputElement)
    {
        return getChildren(inputElement);
    }
    
    public static class PackageElem
    {
        public final Package pkg;
        public final String name;
        
        public PackageElem(Package pkg, String name)
        {
            this.pkg = pkg;
            this.name = name;
        }
        
        public boolean equals(Object obj)
        {
            if (!(obj instanceof Package)) return false;
            
            Package pkg = (Package) obj;
            return pkg.equals(this.pkg) && name.equals(this.name);
        }
        
        public int hashCode()
        {
            return pkg.hashCode() * 37 + name.hashCode();
        }
        
        public String toString()
        {
            return name;
        }
    }
}
