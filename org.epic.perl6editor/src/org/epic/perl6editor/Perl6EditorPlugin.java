package org.epic.perl6editor;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Perl6EditorPlugin extends AbstractUIPlugin
{

	// The plug-in ID
	public static final String PLUGIN_ID = "org.epic.perl6editor"; //$NON-NLS-1$

	// The shared instance
	private static Perl6EditorPlugin plugin;
	
	/**
	 * The constructor
	 */
	public Perl6EditorPlugin()
	{
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception
	{
		super.start(context);
		plugin = this;
		System.out.println( "start c->b->name: " + context.getBundle().getSymbolicName());

	/*
		String[] cmdLine =
			{
				PerlEditorPlugin.getDefault().getPerlExecutable()
			  , "-e"
			  , "for(1..20){ printf STDERR \"# Hello World!\\n\"; printf STDOUT \"# Hello World!\n\"; sleep 1;}"
			};

		ConsoleColorProvider ccp = new ConsoleColorProvider();
		ccp.connect(
			DebugPlugin.newProcess(
				  new Launch(null, null, null)
				, DebugPlugin.exec(cmdLine, new File("."))
				, "My Console Tester" )
		  , new Console(
		);
	 */
//		System.out.println();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception
	{
		plugin = null;
		super.stop(context);
		System.out.println( "stop  c->b->name: " + context.getBundle().getSymbolicName());
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Perl6EditorPlugin getDefault()
	{
		return plugin;
	}

	/**
	 * Returns an image descriptor for the image file at the given
	 * plug-in relative path
	 *
	 * @param path the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor( String path )
	{
		return imageDescriptorFromPlugin( PLUGIN_ID, path );
	}
}
