package cbg.editor.jedit;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import cbg.editor.ColoringEditorTools;
import cbg.editor.EditorPlugin;

public class CatalogReader {

	public CatalogReader() {
		super();
	}
	
	public Mode[] read(String filename) {
		URL mode = null;
		try {
			return readFile(ColoringEditorTools.getFile(filename));
		} catch (Exception e) {
			EditorPlugin.logError("Error reading catalog file " + mode.getFile(), e);
			e.printStackTrace();
			return new Mode[0];
		}
	}
	
	public Mode[] readFile(File file) throws DocumentException, IOException {
		SAXReader reader = new SAXReader();
		Document doc = null;
		doc = reader.read(file);		
		Element root = doc.getRootElement();
		List modeE = root.elements("MODE");
		List modes = new ArrayList(50);
		for (Iterator iter = modeE.iterator(); iter.hasNext();) {
			Element modeElement = (Element) iter.next();
			modes.add(newMode(modeElement));
		}
		return (Mode[]) modes.toArray(new Mode[modes.size()]);
	}

	private Mode newMode(Element modeElement) {
		return Mode.newMode(modeElement.attributeValue("NAME"),
			modeElement.attributeValue("FILE"),
			modeElement.attributeValue("FILE_NAME_GLOB"),
			modeElement.attributeValue("FIRST_LINE_GLOB"));
	}

	/** 
	 * This method is used to create the "extensions" attribute
	 * for the editor XML.
	 */
	public static void main(String[] args) {
		Set list = getListOfExtensions();
		StringBuffer sb = new StringBuffer("extensions=\"");
		for (Iterator iter = list.iterator(); iter.hasNext();) {
			String ext = (String) iter.next();
			sb.append(ext);
			sb.append(",");
		}
		sb.setLength(sb.length() - 1);
		sb.append("\"");
		System.out.println(sb.toString());
	}

	public static Set getListOfExtensions() {
		CatalogReader c = new CatalogReader();
		Mode[] modes = null;
		try {
			modes = c.readFile(new File("C:/workspaces/big/cbg.editor/modes/catalog"));
		} catch (DocumentException e) {
		} catch (IOException e) {
		}
		Set list = new TreeSet();
		for (int i = 0; i < modes.length; i++) {
			Mode mode = modes[i];
			mode.appendExtensionsOnto(list);
		}
		return list;
	}

}
