/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.epic.debug.ui.action;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.ui.texteditor.AbstractRulerActionDelegate;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * @deprecated direct cut and paste of
 *             org.eclipse.debug.ui.actions.RulerEnableDisableBreakpointActionDelegate
 *             to enable support for 3.1 users. if/when 3.1 support is no longer
 *             required, this class can be removed.
 *
 * when removing this class, update plugin.xml to this entry instead:
 *
 * <pre>
 *  &lt;action&gt;
 *      label=&quot;Enable/Disable Breakpoint&quot;
 *      helpContextId=&quot;enable_disable_breakpoint_action_context&quot;
 *      class=&quot;org.eclipse.debug.ui.actions.RulerEnableDisableBreakpointActionDelegate&quot;
 *      menubarPath=&quot;debug&quot;
 *      id=&quot;org.epic.debug.ui.action.EnableDisableBreakpointRulerActionDelegate&quot;&gt;
 *  &lt;/action&gt;
 * </pre>
 */
public class RulerEnableDisableBreakpointActionDelegate extends AbstractRulerActionDelegate {

    /* (non-Javadoc)
     * @see org.eclipse.ui.texteditor.AbstractRulerActionDelegate#createAction(org.eclipse.ui.texteditor.ITextEditor, org.eclipse.jface.text.source.IVerticalRulerInfo)
     */
    protected IAction createAction(ITextEditor editor, IVerticalRulerInfo rulerInfo) {
        return new RulerEnableDisableBreakpointAction(editor, rulerInfo);
    }

}