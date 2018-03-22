/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.dev101.options;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.impl.mapbased.server.Parameters;
import com.perforce.p4java.option.client.IntegrateFilesOptions;
import com.perforce.p4java.tests.dev.annotations.Standalone;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Simple test for one of the more irritating Parameter methods.
 */
@Standalone
@TestId("Dev101_Parameters")
public class ParametersTest extends P4JavaTestCase {

	public ParametersTest() {
	}

	@Test
	public void testIntegrationParametersProcessing() {
		try {
			String[] params = Parameters.processParameters(
									null,
									(IFileSpec) null,
									(IFileSpec) null,
									null,
									null);
			assertNotNull(params);
			assertEquals(0, params.length);
			
			 params = Parameters.processParameters(
						new IntegrateFilesOptions(),
						new FileSpec("//depot/dev/jteam/..."),
						new FileSpec("//depot/main/jteam/..."),
						"test1234",
						null);
			 assertNotNull(params);
			 assertEquals(4, params.length);
			 assertEquals("-b", params[0]);
			 assertEquals("test1234", params[1]);
			 assertEquals("//depot/dev/jteam/...", params[2]);
			 assertEquals("//depot/main/jteam/...", params[3]);
			 
			 params = Parameters.processParameters(
						new IntegrateFilesOptions("-s", "-v", "-o"),
						new FileSpec("//depot/dev/jteam/...#2"),
						new FileSpec("//depot/main/jteam/..."),
						"test1234",
						null);
			 assertNotNull(params);
			 assertEquals(7, params.length);
			 assertEquals("-v", params[0]);
			 assertEquals("-o", params[1]);
			 assertEquals("-b", params[2]);
			 assertEquals("test1234", params[3]);
			 assertEquals("-s", params[4]);
			 assertEquals("//depot/dev/jteam/...#2", params[5]);
			 assertEquals("//depot/main/jteam/...", params[6]);
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		}
	}
}
