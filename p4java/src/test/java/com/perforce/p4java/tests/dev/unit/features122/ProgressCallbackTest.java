/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.features122;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.server.callback.IProgressCallback;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * Tests IProgressCallback - desire more info passed to method tick().
 * The 'p4 sync' command more than just the clientFile as currently available.
 * Report the sync size at the start - which is the totalFileSize.
 */
@TestId("Bugs101_Job040241Test")
public class ProgressCallbackTest extends P4JavaRshTestCase {
	
	private IProgressCallback progressCallback = new IProgressCallback() {
		public int counter = 0;
		
		public void start(int key) {
			System.out.println("start: key = " + key);
			counter=0;
		}

		public boolean tick(int key, String tickMarker) {
			counter++;
			System.out.println("tick: key = " + key);
			System.out.println("tick: tickMarker = " + tickMarker);
			if (counter == 3) {
				System.out.println("Counter: " + counter + "... Cancel callback...");
				return false;
			}
			return true;
		}

		public void stop(int key) {
			System.out.println("stop: key = " + key);
		}
		
	};

	IClient client = null;

    @ClassRule
    public static SimpleServerRule p4d = new SimpleServerRule("r16.1", ProgressCallbackTest.class.getSimpleName());

    /**
	 * @Before annotation to a method to be run before each test in a class.
	 */
	@Before
	public void setUp() {
		// initialization code (before each test).
		try {
		    Properties properties = new Properties();
	        setupServer(p4d.getRSHURL(), "p4jtestuser", "p4jtestuser", true, properties);
			assertNotNull(server);
			client = server.getClient("p4TestUserWS");
			assertNotNull(client);
			server.setCurrentClient(client);
		} catch (Exception e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}

	/**
	 * @After annotation to a method to be run after each test in a class.
	 */
	@After
	public void tearDown() {
		// cleanup code (after each test).
		if (server != null) {
			this.endServerSession(server);
		}
	}

	/**
	 * Test Progresscallback
	 */
	@Test
	public void testProgresscallback() {

		try {
			IProgressCallback prevCallback = server.registerProgressCallback(progressCallback);
			assertNull(prevCallback);
			
			System.out.println("=========== fstat ===========");
			// fstat
			Map<String, Object>[] retMap = server.execInputStringMapCmd("fstat", new String[]{"//depot/112Dev/CopyFilesTest/..."}, null);
			assertNotNull(retMap);
			// fstat - no such file(s)
			retMap = server.execInputStringMapCmd("fstat", new String[]{"//depot/112Dev/CopyFilesTest/a/..."}, null);
			assertNotNull(retMap);
			System.out.println("==============================");

			System.out.println("=========== files ===========");
			// files
			retMap = server.execInputStringMapCmd("files", new String[]{"//depot/112Dev/CopyFilesTest/..."}, null);
			assertNotNull(retMap);
			// files - no such file(s)
			retMap = server.execInputStringMapCmd("files", new String[]{"//depot/112Dev/CopyFilesTest/a/..."}, null);
			assertNotNull(retMap);
			System.out.println("==============================");
		
			System.out.println("============ sync ============");
			// sync
			retMap = server.execInputStringMapCmd("sync", new String[]{"-f", "-n", "//depot/112Dev/CopyFilesTest/..."}, null);
			assertNotNull(retMap);
			// sync - no such file(s)
			retMap = server.execInputStringMapCmd("sync", new String[]{"-f", "-n", "//depot/112Dev/CopyFilesTest/a/..."}, null);
			assertNotNull(retMap);
			System.out.println("==============================");

			System.out.println("============ jobs ============");
			// jobs
			retMap = server.execInputStringMapCmd("jobs", null, null);
			assertNotNull(retMap);
			System.out.println("==============================");

			System.out.println("============ changes ============");
			// changes
			retMap = server.execInputStringMapCmd("changes", null, null);
			assertNotNull(retMap);
			System.out.println("==============================");

			System.out.println("============ users ============");
			// users
			retMap = server.execInputStringMapCmd("users", null, null);
			assertNotNull(retMap);
			System.out.println("==============================");

		} catch (Exception exc) {
			fail("Unexpected exception: " + exc.getLocalizedMessage());
		}
	}
}
