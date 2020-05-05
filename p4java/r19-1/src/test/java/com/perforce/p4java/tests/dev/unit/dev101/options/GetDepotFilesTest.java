/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.dev101.options;

import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.option.server.GetDepotFilesOptions;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Simple comparative getDepotFiles test. Not intended for
 * real-world depot file listing, just for testing the new
 * Options-based way of doing things.
 */

@TestId("Dev101_GetDepotFilesTest")
public class GetDepotFilesTest extends P4JavaRshTestCase {

	public static final String LISTTEST_ROOT = "//depot/basic/readonly/list/...";
	
	public GetDepotFilesTest() {
	}
	
	@ClassRule
    public static SimpleServerRule p4d = new SimpleServerRule("r16.1", GetDepotFilesTest.class.getSimpleName());

	@Before
	public void setUp() throws Exception {
		setupServer(p4d.getRSHURL(), userName, password, true, props);
	}

	@Test
	public void testDepotFilesList() {
		try {
			List<IFileSpec> files = server.getDepotFiles(
							FileSpecBuilder.makeFileSpecList(LISTTEST_ROOT), false);
			assertNotNull(files);
			List<IFileSpec> optsFiles = server.getDepotFiles(
							FileSpecBuilder.makeFileSpecList(LISTTEST_ROOT), null);
			assertNotNull(optsFiles);
			assertEquals(files.size(), optsFiles.size());
			
			files = server.getDepotFiles(
					FileSpecBuilder.makeFileSpecList(LISTTEST_ROOT), false);
			assertNotNull(files);
			optsFiles = server.getDepotFiles(
							FileSpecBuilder.makeFileSpecList(LISTTEST_ROOT),
							new GetDepotFilesOptions(false));
			assertNotNull(optsFiles);
			assertEquals(files.size(), optsFiles.size());
			
			files = server.getDepotFiles(
					FileSpecBuilder.makeFileSpecList(LISTTEST_ROOT), true);
			assertNotNull(files);
			optsFiles = server.getDepotFiles(
							FileSpecBuilder.makeFileSpecList(LISTTEST_ROOT),
							new GetDepotFilesOptions(true));
			assertNotNull(optsFiles);
			assertEquals(files.size(), optsFiles.size());
			optsFiles = server.getDepotFiles(
					FileSpecBuilder.makeFileSpecList(LISTTEST_ROOT),
					new GetDepotFilesOptions().setAllRevs(true));
			assertNotNull(optsFiles);
			assertEquals(files.size(), optsFiles.size());
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		}
	}
}
