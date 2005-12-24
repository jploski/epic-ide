package org.epic.perleditor.editors;

import org.eclipse.jface.action.*;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.widgets.*;

/**
 * Helper class to execute actions contributed to popup menus.
 * Usage: specify an action (contribution) ID at construction time
 * and register an instance with the IMenuManager responsible for
 * populating the popup menu. The action will be run after the menu
 * appears.
 * 
 * @author jploski
 */
public class PopupActionRunner extends MenuAdapter implements IMenuListener
{
    private final String actionID;
    
    public PopupActionRunner(String actionID)
    {
        this.actionID = actionID;
    }
    
    public void menuAboutToShow(IMenuManager manager)
    {
        Menu menu = ((MenuManager) manager).getMenu();
        menu.addMenuListener(this);
    }
    
    public void menuShown(MenuEvent e)
    {
        Menu menu = (Menu) e.getSource();
        menu.removeMenuListener(this);
        
        MenuItem[] items = menu.getItems();
        
        for (int i = 0; i < items.length; i++)
        {
            Object item = items[i].getData();
            if (!(item instanceof ActionContributionItem)) continue;
            
            final ActionContributionItem cItem = (ActionContributionItem) item;
            if (!actionID.equals(cItem.getId())) continue;
            
            Display.getDefault().asyncExec(new Runnable() {
                public void run()
                {
                    cItem.getAction().run();
                } });
        }
    }
}