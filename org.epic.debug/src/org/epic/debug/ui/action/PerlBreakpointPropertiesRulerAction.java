package org.epic.debug.ui.action;

import org.eclipse.debug.core.model.IBreakpoint;

import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.ui.dialogs.PropertyDialogAction;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.IUpdate;

import org.epic.debug.PerlLineBreakpoint;


/**
 * Presents the standard properties dialog to configure the attibutes of a Perl Breakpoint from the
 * ruler popup menu of a text editor.
 */
public class PerlBreakpointPropertiesRulerAction extends RulerBreakpointAction implements IUpdate
{
    //~ Constructors

    public PerlBreakpointPropertiesRulerAction(ITextEditor editor, IVerticalRulerInfo rulerInfo)
    {
        super(editor, rulerInfo);
    }

    //~ Methods

    /*
     * @see org.eclipse.jface.action.Action#run()
     */
    public void run()
    {
        if (getBreakpoint() != null)
        {
            PropertyDialogAction action =
                new PropertyDialogAction(getEditor().getEditorSite(), new ISelectionProvider()
                    {
                        public void addSelectionChangedListener(ISelectionChangedListener listener)
                        {
                            // empty impl
                        }

                        public ISelection getSelection()
                        {
                            return new StructuredSelection(getBreakpoint());
                        }

                        public void removeSelectionChangedListener(
                            ISelectionChangedListener listener)
                        {
                            // empty impl
                        }

                        public void setSelection(ISelection selection)
                        {
                            // empty impl
                        }
                    });
            action.run();
        }
    }

    /*
     * @see org.eclipse.ui.texteditor.IUpdate#update()
     */
    public void update()
    {
        IBreakpoint breakpoint = getBreakpoint();
        if ((breakpoint != null) && (breakpoint instanceof PerlLineBreakpoint))
        {
            setEnabled(true);
        }
        else
        {
            setEnabled(false);
        }
    }

}
