package  org.epic.perleditor.views;

import gnu.regexp.RE;
import gnu.regexp.REException;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import  org.epic.perleditor.views.model.Module;
import  org.epic.perleditor.views.model.Subroutine;
import  org.epic.perleditor.views.model.SourceElement;

import org.epic.perleditor.editors.PerlImages;

public class SourceElementLabelProvider extends LabelProvider {	
	
	final static Image imageSubNode = PerlImages.ICON_SUBROUTINE_NODE.createImage();
	final static Image imageModuleNode = PerlImages.ICON_MODULE_NODE.createImage();
	final static Image imageSub = PerlImages.ICON_SUBROUTINE.createImage();
	final static Image imageModule = PerlImages.ICON_MODULE.createImage();
	final static Image imageConstructor = PerlImages.ICON_CONSTRUCTOR.createImage();
	
	/*
	 * @see ILabelProvider#getImage(Object)
	 */
	public Image getImage(Object element) {
		ImageDescriptor descriptor = null;
		Image image = null;
		
		RE matchNew = null;

				try {
					matchNew = new RE("^new[\\s\\(]");
				} catch (REException e) {
					e.printStackTrace();
					return (null);
				}
		
			
		
		if(element instanceof SourceElement) {
			if(((SourceElement)element).getType() == SourceElement.SUBROUTINE_TYPE) {
			   //descriptor = PerlImages.ICON_SUBROUTINE_NODE;
			   return imageSubNode;
			}
			else if(((SourceElement)element).getType() == SourceElement.MODULE_TYPE) {
			  //descriptor = PerlImages.ICON_MODULE_NODE;
			  return imageModuleNode;
			}
		}
		else if(element instanceof Subroutine) {
			//descriptor = PerlImages.ICON_SUBROUTINE;
			String name = ((Subroutine)element).getName();
		
			if (name != null
						&& ((matchNew.getAllMatches(name).length > 0)
							|| name.equals("new"))) {
						return imageConstructor;
					}
			return imageSub;
		}
		else if(element instanceof Module) {
			//descriptor = PerlImages.ICON_MODULE;
			return imageModule;
		}
	/*	
		if(descriptor != null) {
			image = descriptor.createImage();
		}
		
		return image;
    */
       return null;
	}

	/*
	 * @see ILabelProvider#getText(Object)
	 */
	public String getText(Object element) {
		if (element instanceof SourceElement) {
			if(((SourceElement)element).getName() == null) {
				return "NO LABEL";
			} else {
				return ((SourceElement)element).getName();
			}
		} else if (element instanceof Module) {
			return ((Module)element).getName();
		} else if (element instanceof Subroutine) {
			return ((Subroutine)element).getName();
		} else {
			throw unknownElement(element);
		}
	}

	public void dispose() {
		
	}

	protected RuntimeException unknownElement(Object element) {
		return new RuntimeException("Unknown type of element in tree of type " + element.getClass().getName());
	}

}
