/*
 * Created on 15.08.2004
 *
 * Version: NewTest
 */

package org.epic.perleditor.views;

import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamMonitor;
import org.eclipse.debug.core.model.IStreamsProxy;
import org.eclipse.debug.ui.console.IConsole;
import org.eclipse.debug.ui.console.IConsoleHyperlink;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

/**
 * @author LeO Welsch
 *
 * TODO Adapt the Comment of the functionallity
 */
public class ConsoleView 
	implements IConsole
{

  /* (non-Javadoc)
   * @see org.eclipse.debug.ui.console.IConsole#connect(org.eclipse.debug.core.model.IStreamsProxy)
   */
  public void connect(IStreamsProxy streamsProxy) {
    // TODO Auto-generated method stub
    System.out.println("LeO: " + this.getClass());
    
  }

  /* (non-Javadoc)
   * @see org.eclipse.debug.ui.console.IConsole#connect(org.eclipse.debug.core.model.IStreamMonitor, java.lang.String)
   */
  public void connect(IStreamMonitor streamMonitor, String streamIdentifer) {
    // TODO Auto-generated method stub
    System.out.println("LeO: " + this.getClass());
    
  }

  /* (non-Javadoc)
   * @see org.eclipse.debug.ui.console.IConsole#addLink(org.eclipse.debug.ui.console.IConsoleHyperlink, int, int)
   */
  public void addLink(IConsoleHyperlink link, int offset, int length) {
    // TODO Auto-generated method stub
    System.out.println("LeO: " + this.getClass());
    
  }

  /* (non-Javadoc)
   * @see org.eclipse.debug.ui.console.IConsole#getRegion(org.eclipse.debug.ui.console.IConsoleHyperlink)
   */
  public IRegion getRegion(IConsoleHyperlink link) {
    // TODO Auto-generated method stub
    System.out.println("LeO: " + this.getClass());
    return null;
  }

  /* (non-Javadoc)
   * @see org.eclipse.debug.ui.console.IConsole#getDocument()
   */
  public IDocument getDocument() {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see org.eclipse.debug.ui.console.IConsole#getProcess()
   */
  public IProcess getProcess() {
    // TODO Auto-generated method stub
    System.out.println("LeO: " + this.getClass());
    return null;
  }

}
