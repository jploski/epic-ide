package org.epic.perleditor.views;

import java.util.List;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;
import org.eclipse.jface.text.source.ISourceViewer;

import org.epic.perleditor.views.model.*;
import org.epic.perleditor.views.util.*;
import org.epic.perleditor.editors.IdleTimerListener;
//import org.epic.perleditor.editors.perl.PerlPartitionScanner;

public class PerlOutlinePage
	extends ContentOutlinePage
	implements IdleTimerListener {

	protected ISourceViewer input;
	protected int lastHashCode = 0;
	private	SourceElement subroutines;
	private SourceElement modules;
	private UpdateThread updateThread;
	
	private int waitForTermination = 1000; // millis

	public PerlOutlinePage(ISourceViewer input) {
		super();
		this.input = input;
		updateThread = new UpdateThread(Display.getCurrent());
		// Set thread priority to minimal
		updateThread.setPriority(Thread.MIN_PRIORITY);
		updateThread.start();
	}

	public void createControl(Composite parent) {
		super.createControl(parent);

		TreeViewer viewer = getTreeViewer();
		viewer.setContentProvider(new SourceElementContentProvider());
		viewer.setLabelProvider(new SourceElementLabelProvider());
		viewer.setInput(getInitalInput());
		viewer.setSorter(new NameSorter());

		// Tree is expanded by default
		viewer.expandAll();

	}
	

	public SourceElement getInitalInput() {
		SourceElement root = new SourceElement();
		modules = new SourceElement("Modules", SourceElement.MODULE_TYPE);
		subroutines =
			new SourceElement("Subroutines", SourceElement.SUBROUTINE_TYPE);
		//SourceParser parser = new SourceParser();

//		subroutines.addSubroutines(
//			parser.getElements(
//				input.getTextWidget().getText(),
//				PerlPartitionScanner.TOKEN_SUBROUTINE,
//				"{",
//				"=;"));

		subroutines.addSubroutines(getSubList());
				
//		modules.addModules(
//			parser.getElements(
//				input.getTextWidget().getText(),
//				PerlPartitionScanner.TOKEN_MODULE,
//				";",
//				"=>$"));

		modules.addModules(getModList());
		root.add(modules);
		root.add(subroutines);

		return root;
	}
	
	public List getSubList() {
		return getSubList(input.getTextWidget().getText());
	}
	
	public List getSubList(String text) {
		return SourceParser.getElements(
				text,
				"^[\\s]*sub\\s+([^\\n\\r{]+)",
				"", "",
				true);
	}
	
	public List getModList() {
		return getModList(input.getTextWidget().getText());
	}
	
	public List getModList(String text) {
//		return SourceParser.getElements(
//					text,
//					"^[\\s]*use\\s+([^a-z][^\\n;]+)",
//					"", "",
//					true);
		return SourceParser.getElements(
				text,
				"^[\\s]*use\\s+([^\\s]*[A-Z]+[^;\\s\\n\\r]*)",
				"", "",
				true);
	}
	
	public void dispose()  {
			updateThread.interrupt();
			try {
				updateThread.join(this.waitForTermination);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			super.dispose();
		}

	public void update(List subList, List modList) {
		// Update only if input has changed
		int hashCode = input.getTextWidget().getText().hashCode();

		if (hashCode == lastHashCode) {
			return;
		}

		lastHashCode = hashCode;

		getControl().setRedraw(false);
		subroutines.removeChildren();
		modules.removeChildren();
		//SourceParser parser = new SourceParser();
//		subroutines.addSubroutines(
//			parser.getElements(
//				input.getTextWidget().getText(),
//				PerlPartitionScanner.TOKEN_SUBROUTINE,
//				"{",
//				"=;"));

		subroutines.addSubroutines(subList);

//		modules.addModules(
//			parser.getElements(
//				input.getTextWidget().getText(),
//				PerlPartitionScanner.TOKEN_MODULE,
//				";",
//				"=>$"));

		modules.addModules(modList);

		getTreeViewer().refresh(subroutines, false);
		getTreeViewer().refresh(modules, false);

		// Tree is expanded by default
		//getTreeViewer().expandAll();

		getControl().setRedraw(true);
	}

	/* (non-Javadoc)
	 * @see org.epic.perleditor.editors.IdleTimerListener#onEditorIdle(org.eclipse.jface.text.source.ISourceViewer)
	 */
	public synchronized void onEditorIdle(ISourceViewer viewer) {
		updateThread.setSourceCode(input.getTextWidget().getText());
		updateThread.releaseLock();
	}

	class UpdateThread extends Thread {
		private Object lock = new Object();
		private String sourceCode;
		Display display;

		public UpdateThread(Display display) {
			this.display = display;
		}
		
		public void setSourceCode(String source) {
			sourceCode = source;
		}

		public void run() {
			try {
				while (!Thread.interrupted()) {
					synchronized (this.lock) {
						this.lock.wait();
					}

					display.syncExec(new Invoker(getSubList(sourceCode), getModList(sourceCode)));

				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		public synchronized void releaseLock() {
			synchronized (this.lock) {
				lock.notify();
			}
		}
	}

	class Invoker implements Runnable {
		List subList;
		List modList;
		
		public Invoker(List subList, List modList) {
			this.subList = subList;
			this.modList = modList;
		}

		public void run() {
			update(subList, modList);
		}

	}

}

class NameSorter extends ViewerSorter {

}