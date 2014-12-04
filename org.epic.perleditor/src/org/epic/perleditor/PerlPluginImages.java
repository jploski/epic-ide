package org.epic.perleditor;

import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.osgi.framework.Bundle;

/**
 * Bundle of most images used by the Perl plug-in.
 * 
 * This class is adapted from org.eclipse.jdt.internal.ui.JavaPluginImages.
 */
public class PerlPluginImages
{
    public static final IPath ICONS_PATH = new Path("$nl$/icons"); //$NON-NLS-1$

    private static final String NAME_PREFIX = "org.epic.perleditor."; //$NON-NLS-1$
    private static final int NAME_PREFIX_LENGTH = NAME_PREFIX.length();

    private static ImageRegistry fgImageRegistry = null;
    private static HashMap<String, ImageDescriptor> fgAvoidSWTErrorMap = null;

    /*
     * Available cached Images in the Perl plug-in image registry.
     */
    public static final String IMG_ICON_EDITOR = NAME_PREFIX + "epic.gif"; //$NON-NLS-1$
    public static final String IMG_ICON_SUBROUTINE = NAME_PREFIX
        + "subroutine.gif"; //$NON-NLS-1$
    public static final String IMG_ICON_SUBROUTINE_NODE = NAME_PREFIX
        + "subroutine_node.gif"; //$NON-NLS-1$
    public static final String IMG_ICON_PACKAGE_NODE = NAME_PREFIX
        + "package_node.gif"; //$NON-NLS-1$
    public static final String IMG_ICON_USE = NAME_PREFIX + "use.gif"; //$NON-NLS-1$
    public static final String IMG_ICON_USE_NODE = NAME_PREFIX + "use_node.gif"; //$NON-NLS-1$
    public static final String IMG_ICON_VARIABLE = NAME_PREFIX + "variable.gif"; //$NON-NLS-1$
    public static final String IMG_ICON_CONSTRUCTOR = NAME_PREFIX
        + "constructor.gif"; //$NON-NLS-1$
    public static final String IMG_ICON_SEARCH = NAME_PREFIX + "search.gif"; //$NON-NLS-1$
    public static final String IMG_ICON_MARK_OCCURRENCES = NAME_PREFIX
        + "mark_occurrences.gif"; //$NON-NLS-1$
    public static final String IMG_NEW_PROJECT_WIZARD = NAME_PREFIX
        + "new_wizard.gif"; //$NON-NLS-1$
    public static final String IMG_OBJS_ERROR = NAME_PREFIX + "error_obj.gif"; //$NON-NLS-1$
    public static final String IMG_OBJS_WARNING = NAME_PREFIX
        + "warning_obj.gif"; //$NON-NLS-1$
    public static final String IMG_OBJS_INFO = NAME_PREFIX + "info_obj.gif"; //$NON-NLS-1$
    public static final String IMG_OBJS_TEMPLATE = NAME_PREFIX
        + "template_obj.gif"; //$NON-NLS-1$
    public static final String IMG_ICON_OUTLINE_SORT = NAME_PREFIX
        + "alphab_sort_co.gif"; //$NON-NLS-1$
    public static final String IMG_ICON_OUTLINE_COLLAPSE = NAME_PREFIX
        + "collapseall.gif"; //$NON-NLS-1$
    public static final String IMG_ICON_OUTLINE_REFRESH = NAME_PREFIX
        + "refresh.gif"; //$NON-NLS-1$

    static
    {
        createManaged("", IMG_ICON_EDITOR);
        createManaged("", IMG_ICON_SUBROUTINE);
        createManaged("", IMG_ICON_SUBROUTINE_NODE);
        createManaged("", IMG_ICON_PACKAGE_NODE);
        createManaged("", IMG_ICON_USE);
        createManaged("", IMG_ICON_USE_NODE);
        createManaged("", IMG_ICON_VARIABLE);
        createManaged("", IMG_ICON_CONSTRUCTOR);
        createManaged("", IMG_ICON_SEARCH);
        createManaged("", IMG_ICON_MARK_OCCURRENCES);
        createManaged("", IMG_NEW_PROJECT_WIZARD);
        createManaged("", IMG_OBJS_ERROR);
        createManaged("", IMG_OBJS_WARNING);
        createManaged("", IMG_OBJS_INFO);
        createManaged("", IMG_OBJS_TEMPLATE);
        createManaged("", IMG_ICON_OUTLINE_SORT);
        createManaged("", IMG_ICON_OUTLINE_COLLAPSE);
        createManaged("", IMG_ICON_OUTLINE_REFRESH);
    }

    /**
     * Returns the image managed under the given key in this registry.
     * 
     * @param key
     *            the image's key
     * @return the image managed under the given key
     */
    public static Image get(String key)
    {
        return getImageRegistry().get(key);
    }

    /**
     * Returns the image descriptor for the given key in this registry. Might be
     * called in a non-UI thread.
     * 
     * @param key
     *            the image's key
     * @return the image descriptor for the given key
     */
    public static ImageDescriptor getDescriptor(String key)
    {
        if (fgImageRegistry == null)
            return fgAvoidSWTErrorMap.get(key);

        return getImageRegistry().getDescriptor(key);
    }

    /**
     * Helper method to access the image registry from the PerlEditorPlugin
     * class.
     */
    static ImageRegistry getImageRegistry()
    {
        if (fgImageRegistry == null)
        {
            fgImageRegistry = new ImageRegistry();
            for (Iterator<String> iter = fgAvoidSWTErrorMap.keySet().iterator(); iter
                .hasNext();)
            {
                String key = iter.next();
                fgImageRegistry.put(key,
                    fgAvoidSWTErrorMap.get(key));
            }
            fgAvoidSWTErrorMap = null;
        }
        return fgImageRegistry;
    }

    private static ImageDescriptor createManaged(String prefix, String name)
    {
        return createManaged(prefix, name, name);
    }

    private static ImageDescriptor createManaged(String prefix, String name,
        String key)
    {
        ImageDescriptor result = create(prefix,
            name.substring(NAME_PREFIX_LENGTH), true);

        if (fgAvoidSWTErrorMap == null) fgAvoidSWTErrorMap = new HashMap<String, ImageDescriptor>();

        fgAvoidSWTErrorMap.put(key, result);
        return result;
    }

    /**
     * Creates an image descriptor for the given prefix and name in the Perl
     * plug-in bundle. The path can contain variables like $NL$. If no image
     * could be found, <code>useMissingImageDescriptor</code> decides if either
     * the 'missing image descriptor' is returned or <code>null</code>.
     */
    private static ImageDescriptor create(String prefix, String name,
        boolean useMissingImageDescriptor)
    {
        IPath path = ICONS_PATH.append(prefix).append(name);
        return createImageDescriptor(PerlEditorPlugin.getDefault().getBundle(),
            path, useMissingImageDescriptor);
    }

    /**
     * Creates an image descriptor for the given path in a bundle. The path can
     * contain variables like $NL$. If no image could be found,
     * <code>useMissingImageDescriptor</code> decides if either the 'missing
     * image descriptor' is returned or <code>null</code>.
     */
    private static ImageDescriptor createImageDescriptor(Bundle bundle,
        IPath path, boolean useMissingImageDescriptor)
    {
        URL url = Platform.find(bundle, path);
        if (url != null) return ImageDescriptor.createFromURL(url);
        else return useMissingImageDescriptor ? ImageDescriptor
            .getMissingImageDescriptor() : null;
    }
}