package cbg.editor.test;

import junit.framework.TestCase;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.ITypedRegion;
import cbg.editor.ColoringPartitionScanner;
import cbg.editor.ColoringPartitioner;
import cbg.editor.ColoringSourceViewerConfiguration;
import cbg.editor.Modes;
import cbg.editor.jedit.Type;
import cbg.editor.rules.ColorManager;

public class PartitionScannerTestTest extends TestCase {
	private ColorManager colorManager;
	private ColoringSourceViewerConfiguration config;
	private ColoringPartitionScanner scanner;
	private IDocumentPartitioner partitioner;
	public PartitionScannerTestTest(String name) {
		super(name);
	}
	public static void main(String[] args) {
		junit.awtui.TestRunner.run(PartitionScannerTestTest.class);
	}
	public void testDocumentPartitionScanner() {
        assertNotNull(scanner);
        checkClassDef();
        checkMethodDef();
        checkLiteral();
        checkColon();
        checkDotStar();
        checkGreaterThanEqual();
        checkOpenParen();
	}

	private void checkMethodDef() {
		String code = "/**/";
		ITypedRegion[] regions = partitionCode(code);
		assertEquals(1, regions.length);
		assertEquals(code, getRegionSubstring(code, regions[0]));
		assertEquals(Type.SEQ + "." + ColoringPartitionScanner.COMMENT1, regions[0].getType());
	}
	private void checkLiteral() {
		String code = "'bob'";
		ITypedRegion[] regions = partitionCode(code);
		assertEquals(1, regions.length);
		assertEquals(code, getRegionSubstring(code, regions[0]));
		assertEquals(Type.SINGLE_S+ "." + ColoringPartitionScanner.LITERAL1, regions[0].getType());
	}

	private void checkDotStar() {
		String code = "import java.util.*";
		ITypedRegion[] regions = partitionCode(code);
		assertEquals(4, regions.length);
		assertEquals("import java", getRegionSubstring(code, regions[0]));
		assertEquals(IDocument.DEFAULT_CONTENT_TYPE, regions[0].getType());
		assertEquals(Type.SEQ + "." + ColoringPartitionScanner.NULL, regions[1].getType());
		assertEquals(Type.SEQ + "." + ColoringPartitionScanner.NULL, regions[3].getType());
		assertEquals(2, regions[3].getLength());
		assertEquals(16, regions[3].getOffset());		
	}

	private void checkColon() {
		String code = " label: ";
		ITypedRegion[] regions = partitionCode(code);
		assertEquals(2, regions.length);
		assertEquals(" label:", getRegionSubstring(code, regions[0]));
		// The @1 represents the portion of the label to not highlight
		assertEquals(Type.MARK_PREVIOUS + "@1" + "." + ColoringPartitionScanner.LABEL, regions[0].getType());
	}

	private void checkOpenParen() {
		String code = " a.foo(";
		ITypedRegion[] regions = partitionCode(code);
		assertEquals(1, regions.length);
		// Ideally this would be just foo( not a.foo(
		assertEquals(code, getRegionSubstring(code, regions[0]));
		assertEquals(Type.MARK_PREVIOUS + "@1" + "." + ColoringPartitionScanner.FUNCTION, regions[0].getType());
	}
	
	private void checkGreaterThanEqual() {
		String code = ">=";
		ITypedRegion[] regions = partitionCode(code);
		assertEquals(1, regions.length);
		assertEquals(code, getRegionSubstring(code, regions[0]));
		assertEquals(Type.SEQ + "." + ColoringPartitionScanner.OPERATOR, regions[0].getType());
	}

	/*
	 * public class Bob {
	 * 		public static void main(String[] args) {
	 * 			System.out.println("hello world");
	 * 		}
	 * }
	 * 
	 */
	private void checkClassDef() {
		String code = "public";
		ITypedRegion[] regions = partitionCode(code);
		assertEquals(1, regions.length);
		assertEquals("public", getRegionSubstring(code, regions[0]));
		assertEquals(IDocument.DEFAULT_CONTENT_TYPE, regions[0].getType());
	}
    
    private String getRegionSubstring(String code, ITypedRegion region) {
        return code.substring(region.getOffset(), region.getOffset() + region.getLength());
    }

	private ITypedRegion[] partitionCode(String code) {
		IDocument document = new Document(code);
        partitioner.connect(document);
		ITypedRegion[] regions = partitioner.computePartitioning(0, code.length());
        return regions;
	}
	
	protected void setUp() throws Exception {
		super.setUp();
		IPreferenceStore store = new PreferenceStore();
        ColorManager.initDefaultColors(store);
        colorManager = new ColorManager(store);
        config = new ColoringSourceViewerConfiguration(colorManager);
        config.setFilename("foo.java");
        scanner = new ColoringPartitionScanner(Modes.getMode("java.xml"));
		partitioner = new ColoringPartitioner(scanner, scanner.getContentTypes());
	}

}
