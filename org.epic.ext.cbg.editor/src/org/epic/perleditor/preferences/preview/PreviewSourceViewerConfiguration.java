package org.epic.perleditor.preferences.preview;

import org.eclipse.jface.preference.IPreferenceStore;

import cbg.editor.*;
import cbg.editor.rules.*;

/**
 * @author luelljoc
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class PreviewSourceViewerConfiguration 
extends ColoringSourceViewerConfiguration {
		IPreferenceStore fStore;

	/**
	 * 
	 */

	public PreviewSourceViewerConfiguration(IPreferenceStore store) {
		super(new ColorManager(store), EditorPlugin.getDefault().getEditorTools());
		setMode(Modes.getMode("perl.xml"));
		fStore = store;

	}
	

}
