/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.bug.r101;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Test;

import com.perforce.p4java.Log;
import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.option.client.SyncOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.callback.ILogCallback;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Tests sync issues with job040241. Uses the log callback to detect
 * the problem, which may not always be reliable if the message changes.
 */
@TestId("Bugs101_Job040241Test")
public class Job040241Test extends P4JavaTestCase {
	
	private ILogCallback logCallback = new ILogCallback() {
		// Note: we don't care about anything except
		// the target log message, which is:
		
		static final String TARGET_MSG =
				"file close error in closeFile(): Corrupt GZIP trailer (bad CRC value)";

		public LogTraceLevel getTraceLevel() {
			return LogTraceLevel.ALL;
		}

		public void internalError(String errorString) {
			
		}

		public void internalException(Throwable thr) {
			
		}

		public void internalInfo(String infoString) {
			
		}

		public void internalStats(String statsString) {
		}

		public void internalTrace(LogTraceLevel traceLevel, String traceMessage) {
			
		}

		public void internalWarn(String warnString) {
			if ((warnString != null) && warnString.equals(TARGET_MSG)) {
				fail("Got CRC warning message: " + warnString);
			}
		}
		
	};

	public Job040241Test() {
	}

	@Test
	public void testJob040241SyncError() {
		final String TEST_FILE = "//depot/101Bugs/Bugs101_Job040241Test/readonly/example.ar";
		
		IOptionsServer server = null;
		IClient client = null;

		try {
			server = getServer();
			client = getDefaultClient(server);
			assertNotNull(client);
			server.setCurrentClient(client);
			Log.setLogCallback(this.logCallback);
			List<IFileSpec> syncFiles = client.sync(FileSpecBuilder.makeFileSpecList(TEST_FILE),
														new SyncOptions().setForceUpdate(true));
			assertNotNull(syncFiles);
		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		} finally {
			if (server != null) {
				this.endServerSession(server);
			}
		}
	}
}
