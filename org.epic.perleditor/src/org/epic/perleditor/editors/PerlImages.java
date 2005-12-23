package org.epic.perleditor.editors;

import org.eclipse.jface.resource.ImageDescriptor;
import org.epic.perleditor.PerlEditorPlugin;

import java.net.MalformedURLException;
import java.net.URL;



public class PerlImages
{
   static final URL BASE_URL = PerlEditorPlugin.getDefault().getBundle().getEntry("/");
   public static final ImageDescriptor ICON_EDITOR;
   public static final ImageDescriptor ICON_SUBROUTINE;
   public static final ImageDescriptor ICON_SUBROUTINE_NODE;
   public static final ImageDescriptor ICON_PACKAGE_NODE;
   public static final ImageDescriptor ICON_USE;
   public static final ImageDescriptor ICON_USE_NODE;
   public static final ImageDescriptor ICON_VARIABLE;
   public static final ImageDescriptor ICON_CONSTRUCTOR;
   public static final ImageDescriptor ICON_SEARCH;
   public static final ImageDescriptor ICON_MARK_OCCURRENCES;
   public static final ImageDescriptor NEW_PROJECT_WIZARD;
   public static final ImageDescriptor IMG_OBJS_ERROR;
   public static final ImageDescriptor IMG_OBJS_WARNING;
   public static final ImageDescriptor IMG_OBJS_INFO;
   public static final ImageDescriptor IMG_OBJS_TEMPLATE;

   static
   {
	  String iconPath = "icons/";

	  ICON_EDITOR = createImageDescriptor(iconPath + "epic.gif"); 
	  ICON_SUBROUTINE = createImageDescriptor(iconPath + "subroutine.gif");
	  ICON_SUBROUTINE_NODE = createImageDescriptor(iconPath + "subroutine_node.gif");
	  ICON_PACKAGE_NODE = createImageDescriptor(iconPath + "package_node.gif");
      ICON_USE = createImageDescriptor(iconPath + "use.gif");
      ICON_USE_NODE = createImageDescriptor(iconPath + "use_node.gif");
	  ICON_VARIABLE = createImageDescriptor(iconPath + "variable.gif");
	  ICON_CONSTRUCTOR = createImageDescriptor(iconPath + "constructor.gif");
	  ICON_SEARCH = createImageDescriptor(iconPath + "search.gif");
	  ICON_MARK_OCCURRENCES = createImageDescriptor(iconPath + "mark_occurrences.gif");
	  NEW_PROJECT_WIZARD = createImageDescriptor(iconPath + "new_wizard.gif");
	  IMG_OBJS_ERROR = createImageDescriptor(iconPath + "error_obj.gif");
	  IMG_OBJS_WARNING = createImageDescriptor(iconPath + "warning_obj.gif");
	  IMG_OBJS_INFO = createImageDescriptor(iconPath + "info_obj.gif");
	  IMG_OBJS_TEMPLATE = createImageDescriptor(iconPath + "template_obj.gif");
   }

  
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