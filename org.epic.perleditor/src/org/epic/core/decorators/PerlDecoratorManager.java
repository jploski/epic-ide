/*
 * Created on Jan 4, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.epic.core.decorators;


import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.eclipse.core.resources.IResource;

/**
 * @author balajik
 *
 * This class is used as a manager to organize the resources that need to 
 * be decorated. 
 * 
 */
public class PerlDecoratorManager
{
  /**
   * The resources that are to be decorated
   */ 
  private static List resourcesToBeUpdated = new Vector();
  
  /**
   * Constructor for DemoDecoratorManager.
   */
  public PerlDecoratorManager() 
  {
  }
  
  public static List getSuccessResources ()
  {
	return resourcesToBeUpdated;
  }

  public static void addSuccessResources (List successResourceList)
  {
	resourcesToBeUpdated = new Vector();
	resourcesToBeUpdated.addAll(successResourceList);
  }

  public static void appendSuccessResources (List successResourceList)
  {
	resourcesToBeUpdated.addAll(successResourceList);
  }

  public static void addSuccessResources (IResource resource)
  {
	resourcesToBeUpdated.add(resource);
  }
  

  public static boolean contains (IResource resource)
  {
	return resourcesToBeUpdated.contains (resource);
  }
  
  public static void removeResource (IResource resource)
  {
	if (resourcesToBeUpdated.contains (resource))
	{
	  resourcesToBeUpdated.remove (resource);
	}
  }
  
  private static void printSuccessResources()
  {
	Iterator i = resourcesToBeUpdated.iterator();
	System.out.println("The resources that are updated ");
	while (i.hasNext())
	{
	  IResource resource = (IResource) i.next();
	  System.out.println(resource.getName());
	}
  }
  
}
