/*
 * Created on 05.12.2003
 *
 */
package org.epic.perleditor.editors;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.widgets.Display;
import org.epic.perleditor.PerlEditorPlugin;

/**
 * @author luelljoc
 *
 */
public class IdleTimer extends Thread {

	private ISourceViewer sourceViewer;
	private boolean changedSinceLastRun;
	private List listeners = new ArrayList();
	private int waitForTermination = 1000; // millis
	private Display display;
	private int previousHashCode = 0;

	public IdleTimer(ISourceViewer sourceViewer, Display display) {
		this.sourceViewer = sourceViewer;
		this.display = display;
	}

	public boolean addListener(Object listener) {
		// To make sure the new listener is update immediately
		changedSinceLastRun = true;
		return listeners.add(listener);
	}

	public boolean removeListener(Object listener) {
		return listeners.remove(listener);
	}

	public void run() {
		(
			(
				SourceViewer) this
					.sourceViewer)
					.appendVerifyKeyListener(new VerifyKeyListener() {
			public void verifyKey(VerifyEvent event) {
				changedSinceLastRun = true;
			}
		});

		while (!Thread.interrupted()) {
			long sleep =
				PerlEditorPlugin.getDefault().getPreferenceStore().getLong(
					PerlEditorPlugin.SYNTAX_VALIDATION_INTERVAL_PREFERENCE);

			if (!changedSinceLastRun) {

				Invoker invoker;
				invoker = null;
				try {
					for (int i = 0; i < listeners.size(); i++) {
						invoker =
							new Invoker(
								listeners.get(i),
								sourceViewer,
								this.previousHashCode);
						display.syncExec(invoker);
					}
				} catch (Exception ex) {
					// This might happen if display is no longer available
				}

				// Get hash from last invoker
				if (invoker != null)
					this.previousHashCode = invoker.getHashCode();
			}
			changedSinceLastRun = false;

			try {
				Thread.sleep(sleep);
			} catch (InterruptedException e) {
			  //The only reason why an exception is caught here, is that the sleep timer terminated before end
			  //makes no special reason to print this out! (LeO)

			  //			  e.printStackTrace();
			}
		}
	}

	public void dispose() throws InterruptedException {
		this.interrupt();
		this.join(this.waitForTermination);
	}

	class Invoker implements Runnable {

		private Object object;
		private ISourceViewer viewer;
		private int previousHashCode;
		private int hashCode = 0;

		public Invoker(
			Object obj,
			ISourceViewer viewer,
			int previousHashCode) {
			this.object = obj;
			this.viewer = viewer;
			this.previousHashCode = previousHashCode;
		}

		public int getHashCode() {
			return hashCode;
		}

		public void run() {

			try {
				// Get checksum
				//hashCode = sourceViewer.getTextWidget().getText().hashCode();
				hashCode = sourceViewer.getDocument().get().hashCode();

				if (((SourceViewer) viewer).getTextWidget().isVisible()
					&& hashCode != previousHashCode) {
					Class c = object.getClass();
					try {
						Class[] parameterTypes =
							new Class[] { ISourceViewer.class };
						Method m = c.getMethod("onEditorIdle", parameterTypes);
						Object[] arguments = new Object[] { sourceViewer };
						m.invoke(object, arguments);
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}

			} catch (Exception e) {
				// This exception might occur if sourceViewer is no longer valid
				// or a TargetInvocation exception occurs
				//e.printStackTrace();
			}
		}

	}

}
