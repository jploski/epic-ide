package cbg.editor.test;
import junit.framework.TestCase;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import cbg.editor.ColoringSourceViewerConfiguration;
import cbg.editor.prefs.ColorsPreferencePage;
import cbg.editor.rules.ColorManager;
public class DocumentPartitionerTest2 extends TestCase {
	public void testCase() {
		RuleBasedScanner scanner = config.getCodeScanner();
		assertNotNull(scanner);
		String code = "if";
		scanner.setRange(new Document(code), 0, code.length());
		IToken keywordToken = scanner.nextToken();
		TextAttribute keywordAttribute = (TextAttribute) keywordToken.getData();
		assertEquals(colorManager.getColor(ColorsPreferencePage.KEYWORD1_COLOR), keywordAttribute.getForeground());
		assertEquals(code.length(), scanner.getTokenLength());
		assertEquals(0, scanner.getTokenOffset());
		
		code = "IF";
		scanner.setRange(new Document(code), 0, code.length());
		keywordToken = scanner.nextToken();
		keywordAttribute = (TextAttribute) keywordToken.getData();
		assertEquals(colorManager.getColor(ColorsPreferencePage.KEYWORD1_COLOR), keywordAttribute.getForeground());
		assertEquals(code.length(), scanner.getTokenLength());
		assertEquals(0, scanner.getTokenOffset());
	}
	private ColorManager colorManager;
	private ColoringSourceViewerConfiguration config;
	public DocumentPartitionerTest2(String name) {
		super(name);
	}
	public static void main(String[] args) {
		junit.awtui.TestRunner.run(DocumentPartitionerTest2.class);
	}
	protected void setUp() throws Exception {
		super.setUp();
		IPreferenceStore store = new PreferenceStore();
		ColorManager.initDefaultColors(store);
		colorManager = new ColorManager(store);
		config = new ColoringSourceViewerConfiguration(colorManager);
		config.setFilename("foo.bat");
	}
	protected void tearDown() throws Exception {
		super.tearDown();
	}
}
