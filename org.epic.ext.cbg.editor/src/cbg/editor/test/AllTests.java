package cbg.editor.test;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {
	public static void main(String[] args) {
		junit.textui.TestRunner.run(AllTests.suite());
	}
	public static Test suite() {
		TestSuite suite = new TestSuite("Test for cbg.editor.test");
		//$JUnit-BEGIN$
		//suite.addTest(new TestSuite(CatalogReaderTest.class));
		suite.addTest(new TestSuite(PartitionScannerTestTest.class));
		suite.addTest(new TestSuite(DocumentPartitionerTest.class));
		suite.addTest(new TestSuite(DocumentPartitionerTest2.class));
		suite.addTest(new TestSuite(MarkFollowingTest.class));
		//$JUnit-END$
		return suite;
	}
}
