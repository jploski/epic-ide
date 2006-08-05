package org.epic.debug.ui.action;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.source.IVerticalRulerInfo;

import org.eclipse.ui.texteditor.AbstractRulerActionDelegate;
import org.eclipse.ui.texteditor.ITextEditor;


/**
 * Presents the standard properties dialog to configure the attibutes of a Perl Breakpoint.
 */
public class PerlBreakpointPropertiesRulerActionDelegate extends AbstractRulerActionDelegate
{
    //~ Methods

    /*
     * @see
     * org.eclipse.ui.texteditor.AbstractRulerActionDelegate#createAction(org.eclipse.ui.texteditor.ITextEditor,
     * org.eclipse.jface.text.source.IVerticalRulerInfo)
     */
    protected IAction createAction(ITextEditor editor, IVerticalRulerInfo rulerInfo)
    {
        return new PerlBreakpointPropertiesRulerAction(editor, rulerInfo);
    }

}
