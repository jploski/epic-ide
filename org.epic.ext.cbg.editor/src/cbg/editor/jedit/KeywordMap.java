package cbg.editor.jedit;

import java.util.HashMap;
import java.util.Map;

public class KeywordMap {
	protected Map keywords;
	protected boolean ignoreCase;
	
	public KeywordMap(boolean ignoreCase) {
		super();
		keywords = new HashMap();
		this.ignoreCase = ignoreCase;
	}
	
	public void put(Object key, Object value) {
		keywords.put(key, value);
	}
	
	public boolean ignoreCase() {
		return ignoreCase;
	}
	
	public String[] get(Object key) {
		return (String[]) keywords.get(key);
	}
	
	
}
