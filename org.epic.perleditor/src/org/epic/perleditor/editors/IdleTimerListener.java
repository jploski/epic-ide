/*
 * Created on 05.12.2003
 *
 */
package org.epic.perleditor.editors;

import org.eclipse.jface.text.source.ISourceViewer;

/**
 * @author luelljoc
 *
 */
public interface IdleTimerListener {
	public void onEditorIdle(ISourceViewer viewer);
}
