/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.dev101.options;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Test;

import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.option.server.GetDepotFilesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Simple comparative getDepotFiles test. Not intended for
 * real-world depot file listing, just for testing the new
 * Options-based way of doing things.
 */

@TestId("Dev101_GetDepotFilesTest")
public class GetDepotFilesTest extends P4JavaTestCase {

	public static final String LISTTEST_ROOT = "//depot/basic/readonly/list/...";
	
	public GetDepotFilesTest() {
	}

	@Test
	public void testDepotFilesList() {
		try {
			IOptionsServer server = getServer();
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
