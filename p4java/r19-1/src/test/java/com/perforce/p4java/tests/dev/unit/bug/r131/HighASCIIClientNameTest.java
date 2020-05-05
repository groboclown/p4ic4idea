package com.perforce.p4java.tests.dev.unit.bug.r131;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

/**
 * Test high-ascii client name with non-unicode server.
 *
 * <pre>
 * Create a client with a high-ascii char (i.e umlaut).
 * setCurrentClient() to this client
 * client.where(...) which fails with:
 * Client 'xxx_<wrongchar>' unknown - use 'client' command to create it.
 * </pre>
 */

@Jobs({"job060527"})
@TestId("Dev131_HighASCIIClientNameTest")
public class HighASCIIClientNameTest extends P4JavaRshTestCase {

	private static final String utf8_clientName = "Test_job060527_\u00F9_abcd";

	@ClassRule
	public static SimpleServerRule p4d = new SimpleServerRule("r18.1", HighASCIIClientNameTest.class.getSimpleName());
	private static IClient client;

	@BeforeClass
	public static void before() throws Throwable {
		setupServer(p4d.getRSHURL(), userName, password, true, null);

		client = createClient(server, utf8_clientName);
		server.setCurrentClient(client);
	}

	/**
	 * Test high ascii client name.
	 *
	 * @throws Exception the exception
	 */
	@Test
	public void testHighASCIIClientName() throws Exception {
		List<IFileSpec> fileSpecs = FileSpecBuilder.makeFileSpecList("//depot/basic/...");

		fileSpecs = client.where(fileSpecs);
		assertThat(fileSpecs, notNullValue());
		assertThat("incorrect size", fileSpecs.size() == 1);

		IFileSpec element = fileSpecs.get(0);
		String clientPath = "//" + utf8_clientName + "/basic/...";
		assertThat(element.getOpStatus(), is(FileSpecOpStatus.VALID));
		assertThat("file spec: " + element.getClientPathString() + " does not contain path: " + clientPath,
				element.getClientPathString().contains(clientPath));
	}
}
