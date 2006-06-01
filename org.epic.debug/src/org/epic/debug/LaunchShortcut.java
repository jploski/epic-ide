package org.epic.debug;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.part.FileEditorInput;

/**
 * A launch shortcut is capable of launching a selection or active editor in the
 * workbench. A new launch configuration with default values is created, if an
 * existing launch configuration can't be re-used.
 * <p>
 * A launch shortcut is defined as an extension of type
 * <code>org.eclipse.debug.ui.launchShortcuts</code>. A shortcut specifies
 * the perspectives in which is should be available from the "Run/Debug" cascade
 * menus.
 * </p>
 * <p>
 * A launch shortcut extension is defined in <code>plugin.xml</code>.
 * Following is an example definition of a launch shortcut extension:
 * 
 * <pre>
 *   &lt;extension
 *          point=&quot;org.eclipse.debug.ui.launchShortcuts&quot;&gt;
 *       &lt;shortcut
 *             category=&quot;org.eclipse.jdt.ui.JavaPerspective&quot;
 *             class=&quot;org.epic.debug.LaunchShortcut&quot;
 *             icon=&quot;icons/epic.gif&quot;
 *             id=&quot;org.epic.debug.LaunchShortcut&quot;
 *             label=&quot;Perl Local&quot;
 *             modes=&quot;run, debug&quot;&gt;
 *           
 *             &lt;contextualLaunch&gt;
 *              &lt;enablement&gt;
 *                &lt;with variable=&quot;selection&quot;&gt;
 *                  &lt;count value=&quot;1&quot;/&gt;
 *                  &lt;iterate&gt;
 *                    &lt;or&gt;
 *                      &lt;test property=&quot;org.eclipse.debug.ui.matchesPattern&quot; value=&quot;*.pl&quot;/&gt;
 *                      &lt;test property=&quot;org.eclipse.debug.ui.matchesPattern&quot; value=&quot;*.pm&quot;/&gt;
 *                    &lt;/or&gt;
 *                  &lt;/iterate&gt;
 *                &lt;/with&gt;
 *              &lt;/enablement&gt;
 *   		   &lt;/contextualLaunch&gt;
 *    
 *  	    &lt;/shortcut&gt;
 *  	&lt;/extension&gt;
 *  
 * </pre>
 * 
 * @author Katrin Dust
 * 
 */
public class LaunchShortcut implements ILaunchShortcut {

	/*
	 * Identifier for local Perl configuration
	 */
	private static final String DEBUG_LAUNCH_CONFIGURATION_PERL = "org.epic.debug.launchConfigurationPerl";

	/**
	 * Locates a launchable entity in the given selection and launches an
	 * application in the specified mode.
	 * 
	 * @see org.eclipse.debug.ui.ILaunchShortcut#launch(org.eclipse.jface.viewers.ISelection,
	 *      java.lang.String)
	 * 
	 * @param selection
	 *            workbench selection
	 * @param mode
	 *            one of the launch modes defined by the launch manager
	 * @see org.eclipse.debug.core.ILaunchManager
	 */
	public void launch(ISelection selection, String mode) {
		if (selection instanceof IStructuredSelection) {
			if (((IStructuredSelection) selection).getFirstElement() instanceof IFile) {
				launch((IFile) ((IStructuredSelection) selection)
						.getFirstElement(), mode);
			}
		}
	}

	/**
	 * Locates a launchable entity in the given active editor, and launches an
	 * application in the specified mode.
	 * 
	 * @see org.eclipse.debug.ui.ILaunchShortcut#launch(org.eclipse.ui.IEditorPart,
	 *      java.lang.String)
	 * 
	 * @param editor
	 *            the active editor in the workbench
	 * @param mode
	 *            one of the launch modes defined by the launch manager
	 * @see org.eclipse.debug.core.ILaunchManager
	 */
	public void launch(IEditorPart editor, String mode) {
		FileEditorInput editorInput = (FileEditorInput) editor.getEditorInput();
		launch(editorInput.getFile(), mode);
	}

	/**
	 * Launches an application in the specified mode.
	 * 
	 * @param bin
	 * @param mode
	 */
	private void launch(IFile bin, String mode) {
		ILaunchConfiguration config = findLaunchConfiguration(bin, mode);
		if (config != null) {
			DebugUITools.launch(config, mode);
		}
	}

	/**
	 * If re-usable configuration associated with the File and the project
	 * exist, this configuration is returned. Otherwise a new
	 * configuration is created.
	 * 
	 * @param bin
	 * @param mode
	 * @return a re-useable or new config or <code>null</code> if none
	 */
	private ILaunchConfiguration findLaunchConfiguration(IFile bin,
			String mode) {
		ILaunchConfiguration configuration = null;
		List candidateConfigs = Collections.EMPTY_LIST;
		try {
			ILaunchConfiguration[] configs = DebugPlugin.getDefault()
					.getLaunchManager().getLaunchConfigurations();
			candidateConfigs = new ArrayList(configs.length);
			for (int i = 0; i < configs.length; i++) {
				ILaunchConfiguration config = configs[i];
				String projectName = config.getAttribute(
						PerlLaunchConfigurationConstants.ATTR_PROJECT_NAME,
						(String) null);
				String programFile = config.getAttribute(
						PerlLaunchConfigurationConstants.ATTR_STARTUP_FILE,
						(String) null);
				String name = bin.getName();
				if (programFile != null && programFile.equals(name)) {
					if (projectName != null
							&& projectName.equals(bin.getProject().getName())) {
						candidateConfigs.add(config);
					}
				}
			}
		} catch (CoreException e) {
			PerlDebugPlugin.log(e);
		}

		// If there are no existing configs associated with the File and the
		// project, create one.
		// If there is more then one config associated with the File, return
		// the first one.
		int candidateCount = candidateConfigs.size();
		if (candidateCount < 1) {
			configuration = createConfiguration(bin);
		} else {
			configuration = (ILaunchConfiguration) candidateConfigs.get(0);
		}
		return configuration;
	}

	/**
	 * Creates a new configuration associated with the given file.
	 * 
	 * @param file
	 * @return ILaunchConfiguration
	 */
	private ILaunchConfiguration createConfiguration(IFile file) {
		ILaunchConfiguration config = null;
		String projectName = file.getProjectRelativePath().toString();
		ILaunchConfigurationType[] configType = getLaunchManager()
				.getLaunchConfigurationTypes();
		ILaunchConfigurationType type = null;
		for (int i = 0; i < configType.length; i++) {
			if (configType[i].getIdentifier().equals(
					DEBUG_LAUNCH_CONFIGURATION_PERL)) {
				type = configType[i];
			}
		}
		try {
			if (type != null) {
				ILaunchConfigurationWorkingCopy wc = type.newInstance(null,
						getLaunchManager()
								.generateUniqueLaunchConfigurationNameFrom(
										file.getName()));
				wc.setAttribute(
						PerlLaunchConfigurationConstants.ATTR_STARTUP_FILE,
						projectName);
				wc.setAttribute(
						PerlLaunchConfigurationConstants.ATTR_PROJECT_NAME,
						file.getProject().getName());
				wc.setAttribute(
						PerlLaunchConfigurationConstants.ATTR_WORKING_DIRECTORY,
						(String) null);
				config = wc.doSave();
			}
		} catch (CoreException e) {
			PerlDebugPlugin.log(e);
		}
		return config;
	}

	/**
	 * Method to get the LaunchManager
	 * 
	 * @return ILaunchManager
	 */
	protected ILaunchManager getLaunchManager() {
		return DebugPlugin.getDefault().getLaunchManager();
	}
	
}
