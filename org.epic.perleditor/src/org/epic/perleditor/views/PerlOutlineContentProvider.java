package org.epic.perleditor.views;

import java.util.*;

import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.widgets.TreeItem;
import org.epic.core.model.*;
import org.epic.core.model.Package;
import org.epic.perleditor.PerlEditorPlugin;
import org.epic.perleditor.preferences.PreferenceConstants;

public class PerlOutlineContentProvider implements ITreeContentProvider
{
    static final String MODULES = " Modules";
    static final String SUBROUTINES = " Subroutines";

    private static final Object[] EMPTY_ARRAY = new Object[0];

    private final List<Subroutine> prevSubsContent;
    private final List<ModuleUse> prevUsesContent;
    private final ISourceFileListener listener = new ISourceFileListener()
    {
        public void sourceFileChanged(SourceFile source)
        {
            PerlOutlineContentProvider.this.modelChanged();
        }
    };

    private SourceFile model;
    private TreeViewer viewer;

    public PerlOutlineContentProvider()
    {
        this.prevSubsContent = new ArrayList<Subroutine>();
        this.prevUsesContent = new ArrayList<ModuleUse>();
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
        if (element instanceof Subroutine) return ((Subroutine) element)
            .getParent();
        else if (element instanceof ModuleUse) return ((ModuleUse) element)
            .getParent();
        else if (element instanceof PackageElem) return ((PackageElem) element).pkg;
        else if (element instanceof Package) return model;

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
     * @return true if the outline page's differs from its previous content;
     *         false otherwise
     */
    private boolean contentChanged()
    {
        return packageContentChanged(model.getSubs(),
            prevSubsContent.iterator())
            || packageContentChanged(model.getUses(),
                prevUsesContent.iterator());
    }

    private boolean packageContentChanged(Iterator<? extends IPackageElement> curContent,
        Iterator<? extends IPackageElement> prevContent)
    {
        while (curContent.hasNext() && prevContent.hasNext())
        {
            IPackageElement curElem = curContent.next();
            IPackageElement prevElem = prevContent.next();

            if (packageElementsDiffer(curElem, prevElem))
            {
                return true;
            }
        }
        return curContent.hasNext() != prevContent.hasNext();
    }

    private boolean packageElementsDiffer(IPackageElement curElem,
        IPackageElement prevElem)
    {
        return !curElem.getName().equals(prevElem.getName())
            || curElem.getOffset() != prevElem.getOffset()
            || !curElem.getParent().getName()
                .equals(prevElem.getParent().getName());
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
     * Caches the content of the outline page derived from the model. This is
     * necessary to avoid calling {@link #updateViewer} every time the model
     * changes insignificantly.
     */
    private void rememberContent()
    {
        prevSubsContent.clear();
        prevUsesContent.clear();

        if (model != null)
        {
            for (Iterator<Subroutine> i = model.getSubs(); i.hasNext();)
                prevSubsContent.add(i.next());

            for (Iterator<ModuleUse> i = model.getUses(); i.hasNext();)
                prevUsesContent.add(i.next());
        }
    }

    /**
     * Loads the current contents of the outline page into the tree viewer and
     * expands its nodes. This is an expensive operation, especially under
     * Windows where it results in a visible and annoying redrawing.
     */
    private void updateViewer()
    {
        try
        {
            viewer.refresh();
        }
        catch (SWTException e)
        {
            // Fault tolerance/workaround for bug 1874581:
            viewer.getTree().removeAll();
            viewer.refresh();
        }
        correctViewerExpansion();
    }

    public void correctViewerExpansion()
    {
        if (PerlEditorPlugin.getDefault().getPreferenceStore()
            .getBoolean(PreferenceConstants.OUTLINE_COLLAPSE_ALL))
        {
            viewer.collapseAll();
        }
        else
        {
            viewer.expandAll();
            try
            {
                TreeItem[] topLevelItems = viewer.getTree().getItems();
                for (int topIndex = 0; topIndex < topLevelItems.length; topIndex++)
                {
                    TreeItem[] items = topLevelItems[topIndex].getItems();
                    for (int itemsIndex = 0; itemsIndex < items.length; itemsIndex++)
                    {
                        if (items[itemsIndex].getText().equals(
                            PerlOutlineContentProvider.MODULES)
                            && PerlEditorPlugin
                                .getDefault()
                                .getPreferenceStore()
                                .getBoolean(
                                    PreferenceConstants.OUTLINE_MODULE_FOLDING))
                        {
                            items[itemsIndex].setExpanded(false);
                        }
                        else if (items[itemsIndex].getText().equals(
                            PerlOutlineContentProvider.SUBROUTINES)
                            && PerlEditorPlugin
                                .getDefault()
                                .getPreferenceStore()
                                .getBoolean(
                                    PreferenceConstants.OUTLINE_SUBROUTINE_FOLDING))
                        {
                            items[itemsIndex].setExpanded(false);
                        }
                        else
                        {
                            items[itemsIndex].setExpanded(true);
                        }
                    }
                }
            }
            catch (IllegalArgumentException e)
            {
                // Tree View is not available yet
            }
        }
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
