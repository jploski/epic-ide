package org.epic.perleditor.views;

import java.util.Iterator;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;
import org.epic.core.ResourceMessages;
import org.epic.core.model.SourceFile;
import org.epic.core.model.Subroutine;

public class PerlOutlinePage extends ContentOutlinePage
{
    private SourceFile source;
    
    /**
     * Subroutine in which the caret was during last call to updateSelection
     * We keep track of it to speed up outline synchronisations in the common
     * case (caret movements within a sub).
     */
    private Subroutine lastCaretSub;
    
    public PerlOutlinePage(SourceFile source)
    {
        this.source = source;
    }

    public void createControl(Composite parent)
    {
        super.createControl(parent);

        TreeViewer viewer = getTreeViewer();
        viewer.setContentProvider(new PerlOutlineContentProvider());
        viewer.setLabelProvider(new PerlOutlineLabelProvider());
        viewer.setSorter(new ViewerSorter());
        viewer.setInput(source);
        viewer.expandAll();
        
        IMenuManager menuMan = getSite().getActionBars().getMenuManager();
        menuMan.add(new RefreshAction());
    }
    
    public void refresh()
    {
        if (source != null)
        {
            SourceFile sameSource = source;
            source = null;
            updateContent(sameSource);
        }
    }

    public void updateContent(SourceFile source)
    {
        lastCaretSub = null;
        if (!source.equals(this.source))
        {
            this.source = source;
            getTreeViewer().setInput(source);
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
     * This action is here as a fault tolerance measure - the outline
     * view may, unfortunately, become garbled due to some bug in EPIC.
     * To alleviate this problem somewhat, we give the user a way for
     * explicit recovery.
     */
    private class RefreshAction extends Action
    {
        public RefreshAction()
        {
            setText(ResourceMessages.getString("PerlOutlinePage.RefreshAction.label")); //$NON-NLS-1$
            setToolTipText(ResourceMessages.getString("PerlOutlinePage.RefreshAction.tooltip")); //$NON-NLS-1$
            setDescription(ResourceMessages.getString("PerlOutlinePage.RefreshAction.descr")); //$NON-NLS-1$
        }
        
        public void run()
        {
            refresh();
        }
    }
}
