package cbg.editor.test;
import junit.framework.TestCase;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.widgets.Shell;
import cbg.editor.ColoringSourceViewerConfiguration;
import cbg.editor.prefs.ColorsPreferencePage;
import cbg.editor.rules.ColorManager;
public class DocumentPartitionerTest extends TestCase {
	public void testDocumentPartitioner() {
		RuleBasedScanner scanner = config.getCodeScanner();
		assertNotNull(scanner);
		String code = "public static void main(";
		scanner.setRange(new Document(code), 0, code.length());
		checkPublicKeyword(scanner);
		checkWhitespaceWord(scanner);
		checkStaticKeyword(scanner);
		checkWhitespaceWord(scanner);
		checkVoidKeyword(scanner);
		checkWhitespaceWord(scanner);
	}

	public void testCase() {
		RuleBasedScanner scanner = config.getCodeScanner();
		assertNotNull(scanner);
		String code = "implements";
		scanner.setRange(new Document(code), 0, code.length());
		IToken keywordToken = scanner.nextToken();
		TextAttribute keywordAttribute = (TextAttribute) keywordToken.getData();
		assertEquals(colorManager.getColor(ColorsPreferencePage.KEYWORD1_COLOR), keywordAttribute.getForeground());
		assertEquals("implements".length(), scanner.getTokenLength());
		assertEquals(0, scanner.getTokenOffset());
		
		code = "Implements";
		scanner.setRange(new Document(code), 0, code.length());
		keywordToken = scanner.nextToken();
		keywordAttribute = (TextAttribute) keywordToken.getData();
		assertEquals(colorManager.getColor(ColorsPreferencePage.NULL_COLOR), keywordAttribute.getForeground());
		assertEquals("implements".length(), scanner.getTokenLength());
		assertEquals(0, scanner.getTokenOffset());
	}
	private void checkVoidKeyword(RuleBasedScanner scanner) {
		IToken keywordToken = scanner.nextToken();
		TextAttribute keywordAttribute = (TextAttribute) keywordToken.getData();
		assertEquals(colorManager.getColor(ColorsPreferencePage.KEYWORD3_COLOR), keywordAttribute.getForeground());
		assertEquals("void".length(), scanner.getTokenLength());
		assertEquals(14, scanner.getTokenOffset());
	}
	private void checkStaticKeyword(RuleBasedScanner scanner) {
		IToken keywordToken = scanner.nextToken();
		TextAttribute keywordAttribute = (TextAttribute) keywordToken.getData();
		assertEquals(colorManager.getColor(ColorsPreferencePage.KEYWORD1_COLOR), keywordAttribute.getForeground());
		assertEquals("static".length(), scanner.getTokenLength());
		assertEquals(7, scanner.getTokenOffset());
	}
	private void checkWhitespaceWord(RuleBasedScanner scanner) {
		IToken whiteToken = scanner.nextToken();
		assertTrue(whiteToken.isWhitespace());
		assertEquals(" ".length(), scanner.getTokenLength());
	}
	private void checkPublicKeyword(RuleBasedScanner scanner) {
		IToken keywordToken = scanner.nextToken();
		TextAttribute keywordAttribute = (TextAttribute) keywordToken.getData();
		assertEquals(colorManager.getColor(ColorsPreferencePage.KEYWORD1_COLOR), keywordAttribute.getForeground());
		assertEquals("public".length(), scanner.getTokenLength());
		assertEquals(0, scanner.getTokenOffset());
	}
	private ColorManager colorManager;
	private ColoringSourceViewerConfiguration config;
	public DocumentPartitionerTest(String name) {
		super(name);
	}
	public static void main(String[] args) {
		junit.awtui.TestRunner.run(DocumentPartitionerTest.class);
	}
	protected void setUp() throws Exception {
		super.setUp();
		IPreferenceStore store = new PreferenceStore();
		ColorManager.initDefaultColors(store);
		colorManager = new ColorManager(store);
		config = new ColoringSourceViewerConfiguration(colorManager);
		config.setFilename("foo.java");
		new SourceViewer(new Shell(), null, 0);
	}
	protected void tearDown() throws Exception {
		super.tearDown();
	}
}
