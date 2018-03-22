/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.dev101.options;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Test;

import com.perforce.p4java.option.client.SyncOptions;
import com.perforce.p4java.server.IServer;
import com.perforce.p4java.tests.dev.annotations.Standalone;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Simple standalone test for SyncOptions processing.
 */
@TestId("Dev101_SyncOptionsTest")
@Standalone
public class SyncOptionsTest extends P4JavaTestCase {

	public SyncOptionsTest() {
	}

	@Test
	public void testMethodTemplate() {
		IServer server = null;

		try {
			SyncOptions syncOpts = null;
			List<String> optsStrs = null;
			syncOpts = new SyncOptions();
			assertFalse(syncOpts.isClientBypass());
			assertFalse(syncOpts.isForceUpdate());
			assertFalse(syncOpts.isNoUpdate());
			assertFalse(syncOpts.isServerBypass());
			optsStrs = syncOpts.processOptions(null);
			assertNotNull(optsStrs);
			assertEquals(0, optsStrs.size());
			
			syncOpts = new SyncOptions().setForceUpdate(true)
								.setClientBypass(true)
								.setNoUpdate(true)
								.setServerBypass(true);
			assertTrue(syncOpts.isClientBypass());
			assertTrue(syncOpts.isForceUpdate());
			assertTrue(syncOpts.isNoUpdate());
			assertTrue(syncOpts.isServerBypass());
			optsStrs = syncOpts.processOptions(null);
			assertNotNull(optsStrs);
			assertEquals(4, optsStrs.size());
			assertNotNull(optsStrs.get(0));
			assertTrue(optsStrs.get(0).equals("-f"));
			assertNotNull(optsStrs.get(1));
			assertTrue(optsStrs.get(1).equals("-n"));
			assertNotNull(optsStrs.get(0));
			assertTrue(optsStrs.get(2).equals("-k"));
			assertNotNull(optsStrs.get(1));
			assertTrue(optsStrs.get(3).equals("-p"));
			
			syncOpts = new SyncOptions("-f", "-k"); // forceUpdate, clientBypass
			optsStrs = syncOpts.getOptions();
			assertNotNull(optsStrs);
			assertEquals(2, optsStrs.size());
			assertNotNull(optsStrs.get(0));
			assertTrue(optsStrs.get(0).equals("-f"));
			assertNotNull(optsStrs.get(1));
			assertTrue(optsStrs.get(1).equals("-k"));
			
			syncOpts = new SyncOptions(false, true, false, true); // noUpdate, serverBypass
			assertFalse(syncOpts.isClientBypass());
			assertFalse(syncOpts.isForceUpdate());
			assertTrue(syncOpts.isNoUpdate());
			assertTrue(syncOpts.isServerBypass());
			optsStrs = syncOpts.processOptions(null);
			assertNotNull(optsStrs);
			assertEquals(2, optsStrs.size());
			assertNotNull(optsStrs.get(0));
			assertTrue(optsStrs.get(0).equals("-n"));
			assertNotNull(optsStrs.get(1));
			assertTrue(optsStrs.get(1).equals("-p"));
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		} finally {
			if (server != null) {
				this.endServerSession(server);
			}
		}
	}
}
