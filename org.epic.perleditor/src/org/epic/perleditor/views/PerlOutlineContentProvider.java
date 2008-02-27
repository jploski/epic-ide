package org.epic.perleditor.views;

import java.util.*;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.viewers.*;
import org.epic.core.model.*;
import org.epic.core.model.Package;

public class PerlOutlineContentProvider implements ITreeContentProvider
{
    static final String MODULES = " Modules";
    static final String SUBROUTINES = " Subroutines";
    
    private static final Object[] EMPTY_ARRAY = new Object[0];

    private final List prevSubsContent;
    private final List prevUsesContent;
    private final ISourceFileListener listener = new ISourceFileListener() {
        public void sourceFileChanged(SourceFile source)
        {
            PerlOutlineContentProvider.this.modelChanged();
        } };

    private SourceFile model;
    private TreeViewer viewer;
    
    public PerlOutlineContentProvider()
    {
        this.prevSubsContent = new ArrayList();
        this.prevUsesContent = new ArrayList();
    }

    public void dispose()
    {
        if (model != null) model.removeListener(listener);
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
    {
        if (oldInput instanceof SourceFile)
            ((SourceFile) oldInput).removeListener(listener);

        this.model = (SourceFile) newInput;
        this.viewer = (TreeViewer) viewer;

        rememberContent();
        
        if (model != null) model.addListener(listener);
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
    
    /**
     * @return true if the outline page's  differs from its previous
     *         content; false otherwise
     */
    private boolean contentChanged()
    {
        return
            packageContentChanged(model.getSubs(), prevSubsContent.iterator()) ||
            packageContentChanged(model.getUses(), prevUsesContent.iterator());
    }
    
    private boolean packageContentChanged(Iterator curContent, Iterator prevContent)
    {
        while(curContent.hasNext() && prevContent.hasNext())
        {
            IPackageElement curElem = (IPackageElement) curContent.next();
            IPackageElement prevElem = (IPackageElement) prevContent.next();
            
            if (packageElementsDiffer(curElem, prevElem))                
            {
                return true;
            }
        }
        return curContent.hasNext() != prevContent.hasNext();
    }
    
    private boolean packageElementsDiffer(IPackageElement curElem, IPackageElement prevElem)
    {
        return
            !curElem.getName().equals(prevElem.getName()) ||
            curElem.getOffset() != prevElem.getOffset() ||
            !curElem.getParent().getName().equals(prevElem.getParent().getName());
    }
    
    private void modelChanged()
    {
        if (contentChanged())
        {
            updateViewer();
            rememberContent();
        }
    }
    
    /**
     * Caches the content of the outline page derived from the model.
     * This is necessary to avoid calling {@link #updateViewer} every
     * time the model changes insignificantly.
     */
    private void rememberContent()
    {
        prevSubsContent.clear();
        prevUsesContent.clear();
        
        if (model != null)
        {
            for (Iterator i = model.getSubs(); i.hasNext();)
                prevSubsContent.add(i.next());        
            
            for (Iterator i = model.getUses(); i.hasNext();)
                prevUsesContent.add(i.next());
        }
    }
    
    /**
     * Loads the current contents of the outline page into the tree viewer
     * and expands its nodes. This is an expensive operation, especially
     * under Windows where it results in a visible and annoying redrawing.
     */
    private void updateViewer()
    {
        viewer.refresh();
        viewer.expandToLevel(3);
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
        	if (obj == this) return true;
        	if ((obj instanceof PackageElem)) obj = ((PackageElem) obj).pkg;
        	else if (!(obj instanceof Package)) return false;
            
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
