/*
 * Created on 11.04.2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.epic.debug;

/**
 * @author ruehl
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */


import org.eclipse.jface.resource.ImageDescriptor;
import java.net.MalformedURLException;
import java.net.URL;


public class PerlDebugImages {





   static final URL BASE_URL = PerlDebugPlugin.getDefault().getDescriptor().getInstallURL();
 
   static final String iconPath = "icons/";
   
   	   
	public static final ImageDescriptor DESC_OBJS_BREAKPOINT_INSTALLED= createImageDescriptor(iconPath + "installed_ovr.gif");
	public static final ImageDescriptor DESC_OBJS_BREAKPOINT_INSTALLED_DISABLED= createImageDescriptor(iconPath + "installed_ovr_disabled.gif");	
	

	public static final ImageDescriptor DESC_OBJS_METHOD_BREAKPOINT_ENTRY= createImageDescriptor(iconPath + "entry_ovr.gif");
	public static final ImageDescriptor DESC_OBJS_METHOD_BREAKPOINT_ENTRY_DISABLED= createImageDescriptor(iconPath + "entry_ovr_disabled.gif");
	public static final ImageDescriptor DESC_OBJS_METHOD_BREAKPOINT_EXIT= createImageDescriptor(iconPath + "exit_ovr.gif");
	public static final ImageDescriptor DESC_OBJS_METHOD_BREAKPOINT_EXIT_DISABLED= createImageDescriptor(iconPath + "exit_ovr_disabled.gif");

	public static final ImageDescriptor DESC_OBJS_CONDITIONAL_BREAKPOINT= createImageDescriptor(iconPath + "conditional_ovr.gif");
	public static final ImageDescriptor DESC_OBJS_CONDITIONAL_BREAKPOINT_DISABLED= createImageDescriptor(iconPath + "conditional_ovr_disabled.gif");

	public static final ImageDescriptor DESC_OBJS_SCOPED_BREAKPOINT= createImageDescriptor(iconPath + "scoped_ovr.gif");
	public static final ImageDescriptor DESC_OBJS_SCOPED_BREAKPOINT_DISABLED= createImageDescriptor(iconPath + "scoped_ovr_disabled.gif");

	public static final ImageDescriptor DESC_OBJS_UNCAUGHT_BREAKPOINT= createImageDescriptor(iconPath + "uncaught_ovr.gif");
	public static final ImageDescriptor DESC_OBJS_UNCAUGHT_BREAKPOINT_DISABLED= createImageDescriptor(iconPath + "uncaught_ovr_disabled.gif");

	public static final ImageDescriptor DESC_OBJS_CAUGHT_BREAKPOINT= createImageDescriptor(iconPath + "caught_ovr.gif");
	public static final ImageDescriptor DESC_OBJS_CAUGHT_BREAKPOINT_DISABLED= createImageDescriptor(iconPath + "caught_ovr_disabled.gif");
	
	public static final ImageDescriptor DESC_OBJS_CHANGED_DEBUG_VAR= createImageDescriptor(iconPath + "changedvariablevalue.gif");
	public static final ImageDescriptor DESC_OBJS_CHANGED_DEBUG_VAR_LOCAL = createImageDescriptor(iconPath + "changedvariablevalue_local.gif");
	public static final ImageDescriptor DESC_OBJS_DEBUG_VAR_LOCAL = createImageDescriptor(iconPath + "variable_local.gif");		
	
	/* Launchconfig*/
	public static final ImageDescriptor DESC_OBJS_LaunchTabMain= createImageDescriptor(iconPath + "epic.gif");
	
	/* Expression View */
		public static final ImageDescriptor ACTION_EXPRESSION_EVALUATE = 
	createImageDescriptor(iconPath + "run.gif");

	   private static ImageDescriptor createImageDescriptor(String path)
	   {
		  try
		  {
			 URL url = new URL(BASE_URL, path);

			 return ImageDescriptor.createFromURL(url);
		  }
		  catch(MalformedURLException e)
		  {
		  }

		  return ImageDescriptor.getMissingImageDescriptor();
	   }
	}

