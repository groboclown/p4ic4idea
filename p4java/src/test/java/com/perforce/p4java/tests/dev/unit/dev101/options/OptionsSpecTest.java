/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.dev101.options;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Test;

import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.exception.OptionsException;
import com.perforce.p4java.option.Options;
import com.perforce.p4java.server.IServer;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.Standalone;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Simple standalone test for Options.processFields and associated
 * gubbins.
 */

@Standalone
@Jobs({"job039408"})
@TestId("Dev101_OptionsSpecTest")
public class OptionsSpecTest extends P4JavaTestCase {
	
	static public class TestOptions extends Options {

		// Not intended for actual use...
		
		@Override
		public List<String> processOptions(IServer server)
				throws OptionsException {
			return null;
		}
		
	};

	public OptionsSpecTest() {
	}

	/**
	 * Do some very simple tests for error detection.
	 */
	@Test
	public void testSpecErrorDetection() {
		try {
			@SuppressWarnings("unused")
			List<String> optsList = null;
			TestOptions testOpts = new TestOptions();
			try {
				optsList = testOpts.processFields(null, (Object) null);
				fail("expected options exception for null spec string");
			} catch (OptionsException exc) {
			}
			try {
				optsList = testOpts.processFields("i:m b:d", (Object) null);
				fail("expected options exception for spec / args size mismatch");
			} catch (OptionsException exc) {
			}
			try {
				optsList = testOpts.processFields("b:d", 89);
				fail("expected options exception for boolean opt / arg type mismatch");
			} catch (OptionsException exc) {	
			}
			try {
				optsList = testOpts.processFields("i:d", "hello");
				fail("expected options exception for integer opt / arg type mismatch");
			} catch (OptionsException exc) {	
			}
			try {
				optsList = testOpts.processFields("s:d", 56);
				fail("expected options exception for string opt / arg type mismatch");
			} catch (OptionsException exc) {	
			}
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		}
	}
	
	/**
	 * Simple tests for cases with no rules applied.
	 */
	@Test
	public void testNonRulesIndividualExamples() {
		try {
			List<String> optsList = null;
			TestOptions testOpts = new TestOptions();
			optsList = testOpts.processFields("i:d", 120);
			assertNotNull(optsList);
			assertEquals(1, optsList.size());
			assertEquals("-d120", optsList.get(0));
			
			optsList = testOpts.processFields("b:b", true);
			assertNotNull(optsList);
			assertEquals(1, optsList.size());
			assertEquals("-b", optsList.get(0));
			
			optsList = testOpts.processFields("b:b", false);
			assertNotNull(optsList);
			assertEquals(0, optsList.size());
			
			optsList = testOpts.processFields("s:g", "testname");
			assertNotNull(optsList);
			assertEquals(1, optsList.size());
			assertEquals("-gtestname", optsList.get(0));
			
			optsList = testOpts.processFields("s:g", (String) null);
			assertNotNull(optsList);
			assertEquals(0, optsList.size());
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		}
	}
	
	@Test
	public void testMultipleNonRulesString() {
		try {
			// Note: order of strings in returned list is not specified,
			// so we have to dig around...
			
			List<String> optsList = null;
			TestOptions testOpts = new TestOptions();
			optsList = testOpts.processFields("s:r s:t i:m b:g i:l",
												"username",
												"test pattern",
												120,
												true,
												0);
			assertNotNull(optsList);
			assertEquals(5, optsList.size());
			boolean foundUsername = false;
			boolean foundTestPattern = false;
			boolean found120 = false;
			boolean foundTrue = false;
			boolean foundZero = false;
			
			for (String optStr : optsList) {
				if (optStr.equals("-rusername")) foundUsername = true;
				if (optStr.equals("-ttest pattern")) foundTestPattern = true;
				if (optStr.equals("-m120")) found120  = true;
				if (optStr.equals("-g")) foundTrue  = true;
				if (optStr.equals("-l0")) foundZero  = true;
			}
			
			assertTrue(foundUsername);
			assertTrue(foundTestPattern);
			assertTrue(found120);
			assertTrue(foundTrue);
			assertTrue(foundZero);
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		}
	}
	
	/**
	 * Test default integer rules application.
	 */
	@Test
	public void testIntegerRules() {
		try {
			List<String> optsList = null;
			TestOptions testOpts = new TestOptions();
			
			optsList = testOpts.processFields("i:m:gtz", 0);
			assertNotNull(optsList);
			assertEquals(0, optsList.size());
			optsList = testOpts.processFields("i:m:gtz", -1);
			assertNotNull(optsList);
			assertEquals(0, optsList.size());
			optsList = testOpts.processFields("i:m:gtz", 120);
			assertNotNull(optsList);
			assertEquals(1, optsList.size());
			assertEquals("-m120", optsList.get(0));
			
			optsList = testOpts.processFields("i:m:cl", 0);
			assertNotNull(optsList);
			assertEquals(1, optsList.size());
			assertEquals("-mdefault", optsList.get(0));
			optsList = testOpts.processFields("i:m:cl", -1);
			assertNotNull(optsList);
			assertEquals(0, optsList.size());
			optsList = testOpts.processFields("i:m:cl", 120);
			assertNotNull(optsList);
			assertEquals(1, optsList.size());
			assertEquals("-m120", optsList.get(0));
			optsList = testOpts.processFields("i:c:clz", 120);
			assertNotNull(optsList);
			assertEquals(1, optsList.size());
			assertEquals("-c120", optsList.get(0));
			optsList = testOpts.processFields("i:c:clz", IChangelist.DEFAULT);
			assertNotNull(optsList);
			assertEquals(0, optsList.size());
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		}
	}
}
