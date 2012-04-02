package org.epic.core.popupmenus;

import java.io.IOException;

import org.eclipse.core.resources.IFolder;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.epic.core.util.XMLUtilities;

public class ToggleLibPathActionDelegate implements IObjectActionDelegate
{

    private IWorkbenchPart part;
    private IFolder folder = null;

    public void setActivePart(IAction action, IWorkbenchPart targetPart)
    {
        this.part = targetPart;
        ISelection selection = part.getSite().getSelectionProvider()
            .getSelection();
        if (selection instanceof IStructuredSelection)
        {
            IStructuredSelection sel = (IStructuredSelection) selection;
            Object firstElement = sel.getFirstElement();
            if (firstElement instanceof IFolder)
            {
                folder = (IFolder) firstElement;
                if (folder.isAccessible())
                {
                    if (isInIncPath(folder))
                    {
                        action.setText("Remove from Perl INC Path");
                    }
                    else
                    {
                        action.setText("Add to Perl INC Path");
                    }
                }
                action.setEnabled(folder.isAccessible());
            }
        }
    }

    public void run(IAction action)
    {
        if (folder != null)
        {
            if (folder.isAccessible())
            {
                if (isInIncPath(folder))
                {
                    removeFromIncPath(folder);
                }
                else
                {
                    addToIncPath(folder);
                }
            }
        }

    }

    private boolean addToIncPath(IFolder folder)
    {
        XMLUtilities xmlUtil = new XMLUtilities();
        int currentIncPathsCount = xmlUtil.getIncludeEntries(folder
            .getProject()).length;
        String[] incPaths = new String[currentIncPathsCount + 1];
        System.arraycopy(xmlUtil.getIncludeEntries(folder.getProject()), 0,
            incPaths, 0, currentIncPathsCount);
        incPaths[currentIncPathsCount] = folder.getLocation().toString();
        try
        {
            xmlUtil.writeIncludeEntries(folder.getProject(), incPaths);
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private boolean removeFromIncPath(IFolder folder)
    {
        XMLUtilities xmlUtil = new XMLUtilities();
        int currentIncPathsCount = xmlUtil.getIncludeEntries(folder
            .getProject()).length;
        String[] incPaths = new String[currentIncPathsCount - 1];
        int incPathIndex = 0;
        for (int index = 0; index < currentIncPathsCount; index++)
        {
            if (xmlUtil.getIncludeEntries(folder.getProject())[index]
                .matches(folder.getLocation().toString()))
            {
                // don't add this entry
            }
            else
            {
                incPaths[incPathIndex++] = xmlUtil.getIncludeEntries(folder
                    .getProject())[index];
            }
        }
        try
        {
            xmlUtil.writeIncludeEntries(folder.getProject(), incPaths);
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private boolean isInIncPath(IFolder folder)
    {
        XMLUtilities xmlUtil = new XMLUtilities();
        String[] incPaths = xmlUtil.getIncludeEntries(folder.getProject());
        for (int index = 0; index < incPaths.length; index++)
        {
            if (incPaths[index].matches(folder.getLocation().toString()))
            {
                return true;
            }
        }
        return false;
    }

    public void selectionChanged(IAction action, ISelection selection)
    {
        // TODO Auto-generated method stub

    }

}
