/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.epic.core;

import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.*;
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.eclipse.ui.internal.registry.PerspectiveDescriptor;
import org.epic.perleditor.PerlEditorPlugin;

/**
 */
public class Perspective implements IPerspectiveFactory {
	/**
	 * Constructs a new Default layout engine.
	 */
	public Perspective() {
		super();
	}

	/**
	 * Defines the initial layout for a perspective.
	 * 
	 * Implementors of this method may add additional views to a perspective.
	 * The perspective already contains an editor folder with
	 * <code>ID = ILayoutFactory.ID_EDITORS</code>. Add additional views to
	 * the perspective in reference to the editor folder.
	 * 
	 * This method is only called when a new perspective is created. If an old
	 * perspective is restored from a persistence file then this method is not
	 * called.
	 * 
	 * @param factory
	 *            the factory used to add views to the perspective
	 */
	public void createInitialLayout(IPageLayout layout) {
		defineActions(layout);
		defineLayout(layout);
	}

	/**
	 * Defines the initial actions for a page.
	 */
	public void defineActions(IPageLayout layout) {
		// Add "new wizards".
		layout.addNewWizardShortcut("org.epic.newproject.wizard");//$NON-NLS-1$
		layout.addNewWizardShortcut("org.epic.newfile.wizard");//$NON-NLS-1$
		layout.addNewWizardShortcut("org.eclipse.ui.wizards.new.folder");//$NON-NLS-1$
		layout.addNewWizardShortcut("org.eclipse.ui.wizards.new.file");//$NON-NLS-1$

		// Add "show views".
		layout.addShowViewShortcut(IPageLayout.ID_RES_NAV);
		layout.addShowViewShortcut(IPageLayout.ID_BOOKMARKS);
		layout.addShowViewShortcut(IPageLayout.ID_OUTLINE);
		layout.addShowViewShortcut(IPageLayout.ID_PROP_SHEET);
		layout.addShowViewShortcut(IPageLayout.ID_TASK_LIST);

		layout.addShowViewShortcut("org.epic.perleditor.views.ExplainErrorsView");//$NON-NLS-1$
		layout.addShowViewShortcut("org.epic.perleditor.views.PerlDocView");//$NON-NLS-1$
		layout.addShowViewShortcut("org.epic.core.views.browser.BrowserView");//$NON-NLS-1$
		
		// add perspective shortcuts
		layout.addPerspectiveShortcut("org.eclipse.ui.resourcePerspective");//$NON-NLS-1$
		layout.addPerspectiveShortcut("org.eclipse.debug.ui.DebugPerspective");//$NON-NLS-1$

		layout.addActionSet(IPageLayout.ID_NAVIGATE_ACTION_SET);
		layout.addActionSet(IDebugUIConstants.LAUNCH_ACTION_SET);
	}

	/**
	 * Defines the initial layout for a page.
	 */
	public void defineLayout(IPageLayout layout) {
		// Editors are placed for free.
		String editorArea = layout.getEditorArea();

		// Top left.
		IFolderLayout topLeft = layout.createFolder(
				"topLeft", IPageLayout.LEFT, (float) 0.26, editorArea);//$NON-NLS-1$
		topLeft.addView(IPageLayout.ID_RES_NAV);
		topLeft.addPlaceholder(IPageLayout.ID_BOOKMARKS);

		// Bottom left.
		IFolderLayout bottomLeft = layout.createFolder(
				"bottomLeft", IPageLayout.BOTTOM, (float) 0.50,//$NON-NLS-1$
				"topLeft");//$NON-NLS-1$
		bottomLeft.addView(IPageLayout.ID_OUTLINE);

		// Bottom right.
		layout.addView(IPageLayout.ID_TASK_LIST, IPageLayout.BOTTOM,
				(float) 0.66, editorArea);
	}

	/**
	 * Switches to specified perspective
	 * 
	 * @param perspectiveId
	 *            ID of perspective
	 * @return <code>true</code> on succss otherwise <code>false</code>
	 */
	public static void switchPerspective(final String perspectiveId) {

		final IWorkbench workBench = PerlEditorPlugin.getDefault()
				.getWorkbench();

		Display display = workBench.getDisplay();

		display.asyncExec(new Runnable() {
			public void run() {

				IWorkbenchWindow window = workBench.getActiveWorkbenchWindow();

				// Make sure to get a window
				if (window == null) {
					window = workBench.getWorkbenchWindows()[0];
				}

				IPerspectiveRegistry reg = WorkbenchPlugin.getDefault()
						.getPerspectiveRegistry();
				PerspectiveDescriptor rtPerspectiveDesc = (PerspectiveDescriptor) reg
						.findPerspectiveWithId(perspectiveId);

				if (window != null && rtPerspectiveDesc != null) {
					IWorkbenchPage page = window.getActivePage();
					page.setPerspective(rtPerspectiveDesc);
				}
			}
		});

	}

}