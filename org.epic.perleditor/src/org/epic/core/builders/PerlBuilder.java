/*
 * Created on Dec 26, 2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.epic.core.builders;

import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.epic.core.decorators.PerlDecorator;
import org.epic.core.decorators.PerlDecoratorManager;
import org.epic.perleditor.editors.util.PerlValidator;

/**
 * @author luelljoc
 *
 */
public class PerlBuilder extends IncrementalProjectBuilder {

	/**
	 * 
	 */
	public PerlBuilder() {
		super();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.internal.events.InternalBuilder#build(int, java.util.Map, org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected IProject[] build(int kind, Map args, IProgressMonitor monitor)
		throws CoreException {
			
		PerlDecorator decorator = PerlDecorator.getPerlDecorator();
			
		if (kind == IncrementalProjectBuilder.FULL_BUILD) {
			//System.out.println("Full Build(1)");
			getProject().accept(new BuildFullVisitor());
			decorator.fireLabelEvent(new LabelProviderChangedEvent(
									  decorator, PerlDecoratorManager.getSuccessResources().toArray()));
			//fullBuild(monitor);
		} else {
			IResourceDelta delta = getDelta(getProject());
			if (delta == null) {
				//System.out.println("Full Build(2)");
				getProject().accept(new BuildFullVisitor());
				decorator.fireLabelEvent(new LabelProviderChangedEvent(
						  decorator, PerlDecoratorManager.getSuccessResources().toArray()));
				//fullBuild(monitor);
			} else {
				//System.out.println("Incremental Build");
				try {
					delta.accept(new BuildDeltaVisitor());
					decorator.fireLabelEvent(new LabelProviderChangedEvent(
											  decorator, PerlDecoratorManager.getSuccessResources().toArray()));
				} catch (CoreException ex) {
					ex.printStackTrace();
				}
				//incrementalBuild(delta, monitor);
			}
		}
		return null;
	}

	protected void startupOnInitialize() {
		// add builder init logic here
		try {
			//TODO provide ProgressMonitor
			getProject().build(IncrementalProjectBuilder.FULL_BUILD, null);
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

}

class BuildDeltaVisitor implements IResourceDeltaVisitor {

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.IResourceDeltaVisitor#visit(org.eclipse.core.resources.IResourceDelta)
	 */
	public boolean visit(IResourceDelta delta) throws CoreException {
		switch (delta.getKind()) {
			case IResourceDelta.CHANGED :
			    if(PerlValidator.validate(delta.getResource()) || delta.getResource().getType() == IResource.PROJECT) {
					PerlDecoratorManager.addSuccessResources(delta.getResource());
			    }
				break;
		}
		return true;
	}
}

class BuildFullVisitor implements IResourceVisitor {

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.IResourceVisitor#visit(org.eclipse.core.resources.IResource)
	 */
	public boolean visit(IResource resource) throws CoreException {	
		if(PerlValidator.validate(resource) || resource.getType() == IResource.PROJECT)  {
			PerlDecoratorManager.addSuccessResources(resource);
		}
			
		return true;
	}
}