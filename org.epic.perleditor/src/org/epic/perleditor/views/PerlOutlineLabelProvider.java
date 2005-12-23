package  org.epic.perleditor.views;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.epic.core.model.*;
import org.epic.core.model.Package;
import org.epic.perleditor.editors.PerlImages;

public class PerlOutlineLabelProvider extends LabelProvider
{	
	private static final Image imageSubNode = PerlImages.ICON_SUBROUTINE_NODE.createImage();
    private static final Image imagePackageNode = PerlImages.ICON_PACKAGE_NODE.createImage();
    private static final Image imageUse = PerlImages.ICON_USE.createImage();
    private static final Image imageUseNode = PerlImages.ICON_USE_NODE.createImage();
    private static final Image imageSub = PerlImages.ICON_SUBROUTINE.createImage();
    private static final Image imageConstructor = PerlImages.ICON_CONSTRUCTOR.createImage();
	
	public Image getImage(Object element)
    {
        if (element instanceof Subroutine)
        {
            Subroutine sub = (Subroutine) element;
            if ("new".equals(sub.getName())) return imageConstructor;
            else return imageSub;
        }
        else if (element instanceof ModuleUse)
        {
            return imageUse;
        }
        else if (element instanceof PerlOutlineContentProvider.PackageElem)
        {
            PerlOutlineContentProvider.PackageElem elem =
                (PerlOutlineContentProvider.PackageElem) element;
            
            if (elem.name.equals(PerlOutlineContentProvider.SUBROUTINES))
                return imageSubNode;
            else if (elem.name.equals(PerlOutlineContentProvider.MODULES))
                return imageUseNode;
        }
        else if (element instanceof Package)
        {
            return imagePackageNode;
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
