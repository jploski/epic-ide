package cbg.editor;

import java.util.Map;
import java.util.TreeMap;

import cbg.editor.jedit.CatalogReader;
import cbg.editor.jedit.Mode;
import cbg.editor.jedit.Rule;

public class Modes {
	protected static Modes soleInstace = new Modes();
	protected boolean hasBeenLoaded = false;
	protected Map modes;
	protected Mode[] modeList;
	
	public static Modes getSoleInstance() {
		return soleInstace;
	}
	protected Modes() {
		super();
		modes = new TreeMap();
	}

	public static Mode getMode(String name) {
		return getSoleInstance().getModeNamed(name);
	}

	public static Mode getModeFor(String filename) {
		return getSoleInstance().getModeForFilename(filename);
	}

	private Mode getModeForFilename(String filename) {
		if(filename == null) return getModeNamed("text.xml");
		// check to see if it's already loaded
		String modeName = filenameToModeName(filename);
		if(modeName == null) return getModeNamed("text.xml");
		return getModeNamed(modeName);
	}

	private String filenameToModeName(String filename) {
		Mode[] modes = getModeList();
		if(modes == null) return null;
		for (int i = 0; i < modes.length; i++) {
			Mode mode = modes[i];
			if(mode.matches(filename)) return mode.getFilename();
		}
		return null;
	}

	/**
	 * Answer a sorted array containing all of the modes defined by the catalog.
	 * @return Mode[]
	 */
	public Mode[] getModeList() {
		if(modeList == null) {
			loadCatalog();
		}
		return modeList;
	}

	protected void loadCatalog() {
		CatalogReader reader = new CatalogReader();
		modeList = reader.read("modes/catalog");
		for (int i = 0; i < modeList.length; i++) {
			Mode mode = modeList[i];
			modes.put(mode.getFilename(), mode);
		}
	}





	protected Mode getModeNamed(String name) {
		loadIfNecessary(name);
		return (Mode) modes.get(name);
	}

	private void loadIfNecessary(String name) {
		Mode hull = (Mode) modes.get(name);
		if(hull == null) {
			loadCatalog();
			/* this will happen when there was a problem loading the
			 * catalog */
			if(modes.size() == 0) return;
			hull = (Mode) modes.get(name);
		}
		if(hull.notLoaded()) hull.load();
	}

	/*
	 * Answer the Rule set this delegate/rule resolves to. This
	 * may require loading more modes.	 */
	public static Rule resolveDelegate(Mode mode, String delegateName) {
		int index = delegateName.indexOf("::");
		if(index == -1) {
			// Local delegate/rule set
			return mode.getRule(delegateName);
		}
		Mode loadedMode = getMode(delegateName.substring(0, index) + ".xml");
		return loadedMode.getRule(delegateName.substring(index + 2));
	}

}
