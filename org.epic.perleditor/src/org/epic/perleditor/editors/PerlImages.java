package org.epic.perleditor.editors;

import org.eclipse.jface.resource.ImageDescriptor;
import org.epic.perleditor.PerlEditorPlugin;

import java.net.MalformedURLException;
import java.net.URL;



public class PerlImages
{
   static final URL BASE_URL = PerlEditorPlugin.getDefault().getDescriptor().getInstallURL();
   public static final ImageDescriptor ICON_EDITOR;
   public static final ImageDescriptor ICON_SUBROUTINE;
   public static final ImageDescriptor ICON_SUBROUTINE_NODE;
   public static final ImageDescriptor ICON_MODULE;
   public static final ImageDescriptor ICON_MODULE_NODE;
   public static final ImageDescriptor NEW_PROJECT_WIZARD;

   static
   {
      String iconPath = "icons/";

      ICON_EDITOR = createImageDescriptor(iconPath + "epic.gif"); 
      ICON_SUBROUTINE = createImageDescriptor(iconPath + "subroutine.gif");
      ICON_SUBROUTINE_NODE = createImageDescriptor(iconPath + "subroutine_node.gif");
      ICON_MODULE = createImageDescriptor(iconPath + "module.gif");
      ICON_MODULE_NODE = createImageDescriptor(iconPath + "module_node.gif");
	  NEW_PROJECT_WIZARD = createImageDescriptor(iconPath + "new_wizard.gif");
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