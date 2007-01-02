package  org.epic.perleditor.views;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.epic.core.model.*;
import org.epic.core.model.Package;
import org.epic.perleditor.PerlPluginImages;

public class PerlOutlineLabelProvider extends LabelProvider
{	
	public Image getImage(Object element)
    {
        if (element instanceof Subroutine)
        {
            Subroutine sub = (Subroutine) element;
            if ("new".equals(sub.getName())) return PerlPluginImages.get(PerlPluginImages.IMG_ICON_CONSTRUCTOR);
            else return PerlPluginImages.get(PerlPluginImages.IMG_ICON_SUBROUTINE);
        }
        else if (element instanceof ModuleUse)
        {
            return PerlPluginImages.get(PerlPluginImages.IMG_ICON_USE);
        }
        else if (element instanceof PerlOutlineContentProvider.PackageElem)
        {
            PerlOutlineContentProvider.PackageElem elem =
                (PerlOutlineContentProvider.PackageElem) element;
            
            if (elem.name.equals(PerlOutlineContentProvider.SUBROUTINES))
                return PerlPluginImages.get(PerlPluginImages.IMG_ICON_SUBROUTINE_NODE);
            else if (elem.name.equals(PerlOutlineContentProvider.MODULES))
                return PerlPluginImages.get(PerlPluginImages.IMG_ICON_USE_NODE);
        }
        else if (element instanceof Package)
        {
            return PerlPluginImages.get(PerlPluginImages.IMG_ICON_PACKAGE_NODE);
        }

        return null;
	}

	public String getText(Object element)
    {
		if (element instanceof ISourceElement)
            return ((ISourceElement) element).getName();
        else
            return element.toString();
	}

	public void dispose()
    {	
	}
}
