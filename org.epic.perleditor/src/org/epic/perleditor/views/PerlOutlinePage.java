package org.epic.perleditor.views;

import java.util.*;

import org.eclipse.jface.viewers.*;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;
import org.epic.core.model.*;

public class PerlOutlinePage extends ContentOutlinePage
{
    private SourceFile source;
    private List prevSubsContent;
    private List prevUsesContent;
    
    /**
     * Subroutine in which the caret was during last call to updateSelection
     * We keep track of it to speed up outline synchronisations in the common
     * case (caret movements within a sub).
     */
    private Subroutine lastCaretSub;
    
    public PerlOutlinePage(SourceFile source)
    {
        this.source = source;
        this.prevSubsContent = new ArrayList();
        this.prevUsesContent = new ArrayList();
    }

    public void createControl(Composite parent)
    {
        super.createControl(parent);

        TreeViewer viewer = getTreeViewer();
        viewer.setContentProvider(new PerlOutlineContentProvider());
        viewer.setLabelProvider(new PerlOutlineLabelProvider());
        viewer.setInput(source);
        viewer.setSorter(new ViewerSorter());
        getTreeViewer().expandAll();
        rememberContent(source);
    }

    public void updateContent(SourceFile source)
    {
        lastCaretSub = null;
        if (!source.equals(this.source))
        {
            this.source = source;
            getTreeViewer().setInput(source);
        }
        if (contentChanged(source))
        {
            updateViewer();
            rememberContent(source);
        }
    }
    
    public void updateSelection(int caretLine)
    {
        // check lastCaretSub first to speed up things in the most common case
        if (lastCaretSub == null ||
            caretLine < lastCaretSub.getStartLine() ||
            caretLine > lastCaretSub.getEndLine())
        {
            lastCaretSub = null;
            for (Iterator i = source.getSubs(); i.hasNext();)
            {
                Subroutine sub = (Subroutine) i.next();
                if (caretLine >= sub.getStartLine() &&
                    caretLine <= sub.getEndLine())
                {
                    lastCaretSub = sub;
                    break;
                }
            }
        }
        if (lastCaretSub != null)
            setSelection(new StructuredSelection(lastCaretSub));
        else
            setSelection(StructuredSelection.EMPTY);
    }
    
    /**
     * @param source  SourceFile to be presented in the outline page
     * @return true if the outline page's content for <code>source</code>
     *         differs from the current content; false otherwise
     */
    private boolean contentChanged(SourceFile source)
    {
        return
            packageContentChanged(source.getSubs(), prevSubsContent.iterator()) ||
            packageContentChanged(source.getUses(), prevUsesContent.iterator());
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
    
    /**
     * Caches the content of the outline page derived from <code>source</code>.
     * This is necessary to avoid calling {@link #updateViewer} every time the
     * source file changes (yet the outline should stay unaffected).
     * 
     * @param source  SourceFile currently presented in the outline page
     */
    private void rememberContent(SourceFile source)
    {
        prevSubsContent.clear();
        for (Iterator i = source.getSubs(); i.hasNext();)
            prevSubsContent.add(i.next());
        
        prevUsesContent.clear();
        for (Iterator i = source.getUses(); i.hasNext();)
            prevUsesContent.add(i.next());
    }
    
    /**
     * Loads the current contents of the outline page into the tree viewer
     * and expands its nodes. This is an expensive operation, especially
     * under Windows where it results in a visible and annoying redrawing.
     */
    private void updateViewer()
    {
        TreeViewer viewer = getTreeViewer();
        if (viewer == null) return;

        viewer.getControl().getDisplay().asyncExec(new Runnable() {
            public void run() {
                TreeViewer viewer = getTreeViewer();
                if (viewer == null) return;
                viewer.refresh();
                viewer.expandToLevel(3);
            } });
    }
}