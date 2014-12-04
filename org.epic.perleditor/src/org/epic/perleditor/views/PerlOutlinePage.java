package org.epic.perleditor.views;

import java.util.Iterator;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;
import org.epic.core.ResourceMessages;
import org.epic.core.model.SourceFile;
import org.epic.core.model.Subroutine;
import org.epic.perleditor.PerlEditorPlugin;
import org.epic.perleditor.PerlPluginImages;
import org.epic.perleditor.preferences.PreferenceConstants;

public class PerlOutlinePage extends ContentOutlinePage
{
    private SourceFile source;
    private PerlOutlineContentProvider contentProvider;

    /**
     * Subroutine in which the caret was during last call to updateSelection We
     * keep track of it to speed up outline synchronisations in the common case
     * (caret movements within a sub).
     */
    private Subroutine lastCaretSub;

    public PerlOutlinePage(SourceFile source)
    {
        this.source = source;
        this.contentProvider = new PerlOutlineContentProvider();
    }

    public void createControl(Composite parent)
    {
        super.createControl(parent);

        TreeViewer viewer = getTreeViewer();
        viewer.setContentProvider(contentProvider);
        viewer.setLabelProvider(new PerlOutlineLabelProvider());
        if (PerlEditorPlugin.getDefault().getPreferenceStore()
            .getBoolean(PreferenceConstants.OUTLINE_SORT))
        {
            viewer.setSorter(new ViewerSorter());
        }
        viewer.setInput(source);
        contentProvider.correctViewerExpansion();

        IMenuManager menuMan = getSite().getActionBars().getMenuManager();
        menuMan.add(new RefreshAction());

        registerToolbarActions(this.getSite().getActionBars());
    }

    public void refresh()
    {
        if (source != null)
        {
            SourceFile sameSource = source;
            source = null;
            updateContent(sameSource);
            contentProvider.correctViewerExpansion();
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
        if (lastCaretSub == null || caretLine < lastCaretSub.getStartLine()
            || caretLine > lastCaretSub.getEndLine())
        {
            lastCaretSub = null;
            for (Iterator<Subroutine> i = source.getSubs(); i.hasNext();)
            {
                Subroutine sub = i.next();
                if (caretLine >= sub.getStartLine()
                    && caretLine <= sub.getEndLine())
                {
                    lastCaretSub = sub;
                    break;
                }
            }
        }
        if (lastCaretSub != null) setSelection(new StructuredSelection(
            lastCaretSub));
        else setSelection(StructuredSelection.EMPTY);
    }

    /**
     * This action is here as a fault tolerance measure - the outline view may,
     * unfortunately, become garbled due to some bug in EPIC. To alleviate this
     * problem somewhat, we give the user a way for explicit recovery.
     */
    private class RefreshAction extends Action
    {
        public RefreshAction()
        {
            super();
            PlatformUI
                .getWorkbench()
                .getHelpSystem()
                .setHelp(
                    this,
                    ResourceMessages
                        .getString("PerlOutlinePage.RefreshAction.label"));
            setText(ResourceMessages
                .getString("PerlOutlinePage.RefreshAction.label")); //$NON-NLS-1$
            setImageDescriptor(PerlPluginImages
                .getDescriptor(PerlPluginImages.IMG_ICON_OUTLINE_REFRESH));
            setToolTipText(ResourceMessages
                .getString("PerlOutlinePage.RefreshAction.tooltip")); //$NON-NLS-1$
            setDescription(ResourceMessages
                .getString("PerlOutlinePage.RefreshAction.descr")); //$NON-NLS-1$
        }

        public void run()
        {
            refresh();
        }
    }

    class LexicalSortingAction extends Action
    {
        public LexicalSortingAction()
        {
            super();
            PlatformUI
                .getWorkbench()
                .getHelpSystem()
                .setHelp(
                    this,
                    ResourceMessages
                        .getString("PerlOutlinePage.RefreshAction.tooltip")); //$NON-NLS-1$
            setText(ResourceMessages
                .getString("PerlOutlinePage.LexicalSortAction.tooltip")); //$NON-NLS-1$
            setImageDescriptor(PerlPluginImages
                .getDescriptor(PerlPluginImages.IMG_ICON_OUTLINE_SORT));
            setToolTipText(ResourceMessages
                .getString("PerlOutlinePage.LexicalSortAction.tooltip")); //$NON-NLS-1$
            setDescription(ResourceMessages
                .getString("PerlOutlinePage.LexicalSortAction.descr")); //$NON-NLS-1$
            setChecked(PerlEditorPlugin.getDefault().getPreferenceStore()
                .getBoolean(PreferenceConstants.OUTLINE_SORT));
        }

        public void run()
        {
            if (PerlEditorPlugin.getDefault().getPreferenceStore()
                .getBoolean(PreferenceConstants.OUTLINE_SORT))
            {
                getTreeViewer().setSorter(null);
                PerlEditorPlugin.getDefault().getPreferenceStore()
                    .setValue(PreferenceConstants.OUTLINE_SORT, false);
                setChecked(false);
            }
            else
            {
                getTreeViewer().setSorter(new ViewerSorter());
                PerlEditorPlugin.getDefault().getPreferenceStore()
                    .setValue(PreferenceConstants.OUTLINE_SORT, true);
                setChecked(true);
            }
        }
    }

    class CollapseAllAction extends Action
    {
        public CollapseAllAction()
        {
            super();
            PlatformUI
                .getWorkbench()
                .getHelpSystem()
                .setHelp(
                    this,
                    ResourceMessages
                        .getString("PerlOutlinePage.CollapseAllAction.tooltip"));
            setText(ResourceMessages
                .getString("PerlOutlinePage.CollapseAllAction.tooltip"));
            setImageDescriptor(PerlPluginImages
                .getDescriptor(PerlPluginImages.IMG_ICON_OUTLINE_COLLAPSE));
            setToolTipText(ResourceMessages
                .getString("PerlOutlinePage.CollapseAllAction.tooltip")); //$NON-NLS-1$
            setDescription(ResourceMessages
                .getString("PerlOutlinePage.CollapseAllAction.descr")); //$NON-NLS-1$
        }

        public void run()
        {
            if (PerlEditorPlugin.getDefault().getPreferenceStore()
                .getBoolean(PreferenceConstants.OUTLINE_COLLAPSE_ALL))
            {
                PerlEditorPlugin.getDefault().getPreferenceStore()
                    .setValue(PreferenceConstants.OUTLINE_COLLAPSE_ALL, false);
                setChecked(false);
            }
            else
            {
                PerlEditorPlugin.getDefault().getPreferenceStore()
                    .setValue(PreferenceConstants.OUTLINE_COLLAPSE_ALL, true);
                setChecked(true);
            }
            contentProvider.correctViewerExpansion();

        }
    }

    class HideModulesAction extends Action
    {
        public HideModulesAction()
        {
            super();
            PlatformUI
                .getWorkbench()
                .getHelpSystem()
                .setHelp(
                    this,
                    ResourceMessages
                        .getString("PerlOutlinePage.HideModules.tooltip"));
            setText(ResourceMessages
                .getString("PerlOutlinePage.HideModules.tooltip"));
            setImageDescriptor(PerlPluginImages
                .getDescriptor(PerlPluginImages.IMG_ICON_USE_NODE));
            setToolTipText(ResourceMessages
                .getString("PerlOutlinePage.HideModules.tooltip")); //$NON-NLS-1$
            setDescription(ResourceMessages
                .getString("PerlOutlinePage.HideModules.descr")); //$NON-NLS-1$
            setChecked(PerlEditorPlugin.getDefault().getPreferenceStore()
                .getBoolean(PreferenceConstants.OUTLINE_MODULE_FOLDING));
        }

        public void run()
        {
            if (PerlEditorPlugin.getDefault().getPreferenceStore()
                .getBoolean(PreferenceConstants.OUTLINE_MODULE_FOLDING))
            {
                PerlEditorPlugin
                    .getDefault()
                    .getPreferenceStore()
                    .setValue(PreferenceConstants.OUTLINE_MODULE_FOLDING, false);
                setChecked(false);
            }
            else
            {
                PerlEditorPlugin.getDefault().getPreferenceStore()
                    .setValue(PreferenceConstants.OUTLINE_MODULE_FOLDING, true);
                setChecked(true);
            }
            contentProvider.correctViewerExpansion();
        }
    }

    class ShowSubroutineAction extends Action
    {
        public ShowSubroutineAction()
        {
            super();
            PlatformUI
                .getWorkbench()
                .getHelpSystem()
                .setHelp(
                    this,
                    ResourceMessages
                        .getString("PerlOutlinePage.HideMethods.tooltip"));
            setText(ResourceMessages
                .getString("PerlOutlinePage.HideMethods.tooltip"));
            setImageDescriptor(PerlPluginImages
                .getDescriptor(PerlPluginImages.IMG_ICON_SUBROUTINE_NODE));
            setToolTipText(ResourceMessages
                .getString("PerlOutlinePage.HideMethods.tooltip")); // $$NON-NLS-1$
            setDescription(ResourceMessages
                .getString("PerlOutlinePage.HideMethods.descr")); // $$NON-NLS-1$
            setChecked(PerlEditorPlugin.getDefault().getPreferenceStore()
                .getBoolean(PreferenceConstants.OUTLINE_SUBROUTINE_FOLDING));
        }

        public void run()
        {
            if (PerlEditorPlugin.getDefault().getPreferenceStore()
                .getBoolean(PreferenceConstants.OUTLINE_SUBROUTINE_FOLDING))
            {
                PerlEditorPlugin
                    .getDefault()
                    .getPreferenceStore()
                    .setValue(PreferenceConstants.OUTLINE_SUBROUTINE_FOLDING,
                        false);
                setChecked(false);
            }
            else
            {
                PerlEditorPlugin
                    .getDefault()
                    .getPreferenceStore()
                    .setValue(PreferenceConstants.OUTLINE_SUBROUTINE_FOLDING,
                        true);
                setChecked(true);
            }
            contentProvider.correctViewerExpansion();
        }
    }

    private void registerToolbarActions(IActionBars actionBars)
    {
        IToolBarManager toolBarManager = actionBars.getToolBarManager();
        toolBarManager.add(new CollapseAllAction());
        toolBarManager.add(new LexicalSortingAction());
        toolBarManager.add(new HideModulesAction());
        toolBarManager.add(new ShowSubroutineAction());
    }
}
