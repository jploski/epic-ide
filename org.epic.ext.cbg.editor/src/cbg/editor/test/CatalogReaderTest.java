package cbg.editor.test;

import java.util.Set;

import cbg.editor.jedit.CatalogReader;

import junit.framework.TestCase;

public class CatalogReaderTest extends TestCase {

	public CatalogReaderTest(String name) {
		super(name);
	}

	public static void main(String[] args) {
		junit.awtui.TestRunner.run(CatalogReaderTest.class);
	}

	public void testCatalogReader() {
		Set exts = CatalogReader.getListOfExtensions();
		assertEquals(127, exts.size());
	}
}
