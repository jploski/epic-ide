/*
 * Created on 08.05.2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.epic.core;

import org.epic.perleditor.PerlEditorPlugin;

/**
 * @author ST
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class Constants
{
	public static final String PERL_EDITOR_ID =
		"org.epic.perleditor.editors.PerlEditor";
	public static final String EMB_PERL_FILE_EXTENSION = "epl";
	public static final String PERL_NATURE_ID =
		"org.epic.perleditor.perlnature";
	
	public static final String MARKER_ATTR_PERL_ERROR_EXPLANATION =
		"org.epic.marker.printfPerlErrorMessage";
	
	public static final String DEBUG_PERSPECTIVE_ID =
		"org.eclipse.debug.ui.DebugPerspective";
    
    public static final String PROBLEM_MARKER =
        PerlEditorPlugin.getUniqueIdentifier() + ".perlProblemMarker"; //$NON-NLS-1$
}
