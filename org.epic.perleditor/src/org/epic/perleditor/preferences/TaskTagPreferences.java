package org.epic.perleditor.preferences;

import java.util.ArrayList;

import org.eclipse.jface.preference.IPreferenceStore;

/**
 * @author ptraeder
 */
public class TaskTagPreferences implements ITaskTagConstants {
  
	private static final String SEPARATOR = "#";  
	
	public TaskTagPreferences() {
		super();
	}
	
	public static void initializeDefaults(IPreferenceStore store) {
		store.setDefault(ID_TASK_TAGS, "TODO#TASK#");
		store.setDefault(ID_IGNORE_CASE, true);
		store.setDefault(ID_WHITESPACE, true);
	}  
	
	
	public static String[] parseStringList(String stringList) {
		int currentPos = stringList.indexOf(SEPARATOR);
		ArrayList resultList = new ArrayList();
		while (currentPos > 0) {
			resultList.add(stringList.substring(0, currentPos));
			stringList = stringList.substring(currentPos+1);
			currentPos = stringList.indexOf(SEPARATOR);
		}
		
		return (String[])resultList.toArray(new String[0]);    
		
	}

  public static String createList(String[] items) {
    String result = new String();
    for (int loop = 0; loop < items.length; loop++) {
      String string = items[loop];
      result += string + SEPARATOR;
    }    
    return result;
  }
  
}
