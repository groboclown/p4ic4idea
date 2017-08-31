/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.features111;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.option.server.GetDepotFilesOptions;
import com.perforce.p4java.option.server.LoginOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.ServerFactory;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Simple tests of the new 10.2 login / logout / ticket scheme. The basics
 * are generally tested all over the place elsewhere (most tests will fail
 * if we get this wrong), but this particular test setup was used as
 * scaffolding to drive the underlying development, so it stays here.<p>
 * 
 * NOTE: race conditions may occasionally cause non-repeatable test failures
 * here; we may want to tighten this up a little over time -- HR.
 */
@TestId("Features102_NewAuthTicketTest")
public class NewAuthTicketTest extends P4JavaTestCase {

	public NewAuthTicketTest() {
	}
	
	/**
	 * See what happens when we do a simple log in followed
	 * by a couple of boring commands, then log out, then do it again...
	 */
	@Test
	public void testNewAuthTicketBasic() {
		IOptionsServer server = null;
		final String depotPath = "//depot/dev/...";
		
		try {
			server = ServerFactory.getOptionsServer(this.serverUrlString, null);
			assertNotNull("null server returned from server factory", server);
			server.connect();
			server.setUserName(this.getUserName());
			server.login(this.getPassword(), null);
			Map<String, String> counters = server.getCounters();
			assertNotNull(counters);
			List<IFileSpec> files = server.getDepotFiles(
											FileSpecBuilder.makeFileSpecList(depotPath),
											new GetDepotFilesOptions());
			assertNotNull(files);	// otherwise not really interested in
									// the result details -- we pass if we didn't
									// get an access exception, which is all that
									// matters here.
			server.logout();
			server.login(this.getPassword(), null);
			counters = server.getCounters();
			assertNotNull(counters);
			files = server.getDepotFiles(
								FileSpecBuilder.makeFileSpecList(depotPath),
								new GetDepotFilesOptions());
			assertNotNull(files);
			server.logout();
		} catch (Exception exc) {
			fail("unexpected exception: " + exc.getLocalizedMessage());
		}
	}
	
	/**
	 * See what happens when we log in as super user.
	 */
	@Test
	public void testNewAuthTicketSuperUser() {
		IOptionsServer server = null;
		final String depotPath = "//depot/dev/...";

		try {
			server = ServerFactory.getOptionsServer(this.serverUrlString, null);
			assertNotNull("null server returned from server factory", server);
			server.connect();
			server.setUserName(this.getSuperUserName());
			server.login(this.getSuperUserPassword(), null);
			Map<String, String> counters = server.getCounters();
			assertNotNull(counters);
			List<IFileSpec> files = server.getDepotFiles(
											FileSpecBuilder.makeFileSpecList(depotPath),
											new GetDepotFilesOptions());
			assertNotNull(files);	// otherwise not really interested in
									// the result details -- we pass if we didn't
									// get an access exception, which is all that
									// matters here.
			server.logout();
			server.login(this.getSuperUserPassword(), null);
			counters = server.getCounters();
			assertNotNull(counters);
			files = server.getDepotFiles(
								FileSpecBuilder.makeFileSpecList(depotPath),
								new GetDepotFilesOptions());
			assertNotNull(files);
			server.logout();
		} catch (Exception exc) {
			fail("unexpected exception: " + exc.getLocalizedMessage());
		}
	}
	
	/**
	 * See what happens when we try the simple version on a 10.1 server.
	 */
	@Test
	public void testNewAuthTicketCompatibility() {
		IOptionsServer server = null;
		final String depotPath = "//depot/dev/...";
		
		try {
			server = ServerFactory.getOptionsServer(this.serverUrlString, null);
			assertNotNull("null server returned from server factory", server);
			server.connect();
			server.setUserName(this.getUserName());
			server.login(this.getPassword(), new LoginOptions());
			Map<String, String> counters = server.getCounters();
			assertNotNull(counters);
			List<IFileSpec> files = server.getDepotFiles(
											FileSpecBuilder.makeFileSpecList(depotPath),
											new GetDepotFilesOptions());
			assertNotNull(files);	// otherwise not really interested in
									// the result details -- we pass if we didn't
									// get an access exception, which is all that
									// matters here.
			server.logout();
			server.login(this.getPassword(), new LoginOptions());
			counters = server.getCounters();
			assertNotNull(counters);
			files = server.getDepotFiles(
								FileSpecBuilder.makeFileSpecList(depotPath),
								new GetDepotFilesOptions());
			assertNotNull(files);
			server.logout();
		} catch (Exception exc) {
			fail("unexpected exception: " + exc.getLocalizedMessage());
		}
	}
	
	/**
	 * See what happens when we do a -a login...
	 */
	@Test
	public void testNewAuthTicketAllHostsLogin() {
		IOptionsServer server = null;
		final String depotPath = "//depot/dev/...";
		
		try {
			server = ServerFactory.getOptionsServer(this.serverUrlString, null);
			assertNotNull("null server returned from server factory", server);
			server.connect();
			server.setUserName(this.getUserName());
			server.login(this.getPassword(), new LoginOptions().setAllHosts(true));
			Map<String, String> counters = server.getCounters();
			assertNotNull(counters);
			List<IFileSpec> files = server.getDepotFiles(
											FileSpecBuilder.makeFileSpecList(depotPath),
											new GetDepotFilesOptions());
			assertNotNull(files);	// otherwise not really interested in
									// the result details -- we pass if we didn't
									// get an access exception, which is all that
									// matters here.
			server.logout();
			server.login(this.getPassword(), new LoginOptions().setAllHosts(true));
			counters = server.getCounters();
			assertNotNull(counters);
			files = server.getDepotFiles(
								FileSpecBuilder.makeFileSpecList(depotPath),
								new GetDepotFilesOptions());
			assertNotNull(files);
			server.logout();
		} catch (Exception exc) {
			fail("unexpected exception: " + exc.getLocalizedMessage());
		}
	}
	
	/**
	 * See what happens when we first log in as super user
	 * then try logging in as someone else...
	 */
	@Test
	public void testNewAuthTicketSuperUserAltLogin() {
		IOptionsServer server = null;
		final String depotPath = "//depot/dev/...";
		
		try {
			server = ServerFactory.getOptionsServer(this.serverUrlString, null);
			assertNotNull("null server returned from server factory", server);
			server.connect();
			server.setUserName(this.getSuperUserName());
			server.login(this.getSuperUserPassword(), new LoginOptions());
			Map<String, String> counters = server.getCounters();
			assertNotNull(counters);
			List<IFileSpec> files = server.getDepotFiles(
											FileSpecBuilder.makeFileSpecList(depotPath),
											new GetDepotFilesOptions());
			assertNotNull(files);	// otherwise not really interested in
									// the result details -- we pass if we didn't
									// get an access exception, which is all that
									// matters here.
			server.setUserName(this.getUserName());
			server.login(this.getPassword(), new LoginOptions());
			counters = server.getCounters();
			assertNotNull(counters);
			files = server.getDepotFiles(
											FileSpecBuilder.makeFileSpecList(depotPath),
											new GetDepotFilesOptions());
			assertNotNull(files);	// otherwise not really interested in
									// the result details -- we pass if we didn't
									// get an access exception, which is all that
									// matters here.
			server.logout();
		} catch (Exception exc) {
			fail("unexpected exception: " + exc.getLocalizedMessage());
		}
	}
}
