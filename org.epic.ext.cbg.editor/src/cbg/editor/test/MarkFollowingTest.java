package cbg.editor.test;

import junit.framework.TestCase;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.rules.IToken;
import cbg.editor.ColoringPartitionScanner;
import cbg.editor.ColoringSourceViewerConfiguration;
import cbg.editor.Modes;
import cbg.editor.jedit.Type;
import cbg.editor.rules.ColorManager;

public class MarkFollowingTest extends TestCase {
	private ColorManager colorManager;
	private ColoringSourceViewerConfiguration config;
	private ColoringPartitionScanner scanner;
	public MarkFollowingTest(String name) {
		super(name);
	}
	public static void main(String[] args) {
		junit.awtui.TestRunner.run(MarkFollowingTest.class);
	}
	public void testMarkFollowing() {
        assertNotNull(scanner);
        String code = "#topbar {}";
        scanner.setRange(new Document(code), 0, code.length());

        checkMarkFollowing(scanner);
	}

	public void testMarkFollowingFalse() {
	    assertNotNull(scanner);
	    String code = "topbar {}";
	    scanner.setRange(new Document(code), 0, code.length());
	
	    checkMarkFollowingFalse(scanner);
	}

	private void checkMarkFollowingFalse(ColoringPartitionScanner theScanner) {
		IToken keywordToken = theScanner.nextToken();
		assertTrue(keywordToken.isOther());
		assertEquals(1, theScanner.getTokenLength());
		assertEquals(0, theScanner.getTokenOffset());
	}

	private void checkMarkFollowing(ColoringPartitionScanner theScanner) {
		IToken keywordToken = theScanner.nextToken();
		String data = (String) keywordToken.getData();
		assertEquals(Type.MARK_FOLLOWING + "@1" + "." + ColoringPartitionScanner.LITERAL2, data);
		assertEquals("#topbar".length(), theScanner.getTokenLength());
		assertEquals(0, theScanner.getTokenOffset());
	}
    	
	protected void setUp() throws Exception {
		super.setUp();
		IPreferenceStore store = new PreferenceStore();
        ColorManager.initDefaultColors(store);
        colorManager = new ColorManager(store);
        config = new ColoringSourceViewerConfiguration(colorManager);
        config.setFilename("foo.css");
        scanner = new ColoringPartitionScanner(Modes.getMode("css.xml"));
	}

}
