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
		// Changed for version 3.0 of Colorer Plugin
		//super(new ColorManager(store), EditorPlugin.getDefault().getEditorTools());
		super(new ColorManager(store));
		setMode(Modes.getMode("perl.xml"));
		fStore = store;

	}
	

}
