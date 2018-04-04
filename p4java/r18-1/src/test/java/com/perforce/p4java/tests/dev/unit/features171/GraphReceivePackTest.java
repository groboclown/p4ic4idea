package com.perforce.p4java.tests.dev.unit.features171;

import com.perforce.p4java.PropertyDefs;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.option.server.GraphReceivePackOptions;
import com.perforce.p4java.tests.GraphServerRule;
import com.perforce.p4java.tests.SimpleTestServer;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static com.perforce.p4java.server.CmdSpec.FILES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test class for graph receive-pack functionality
 * Usage: receive-pack -n repo [-u user -v] -i files... [ -r refs... | -F refs... | -p packed-refs ]
 */
@TestId("Dev171_GraphReceivePackTest")
public class GraphReceivePackTest extends P4JavaRshTestCase {

	private static final String PACK_FILE_PATH = SimpleTestServer.RESOURCES + "com/perforce/p4java/scm-api-plugin.git/";
	@Rule
	public ExpectedException exception = ExpectedException.none();

	@ClassRule
	public static GraphServerRule p4d = new GraphServerRule("r17.1", GraphReceivePackTest.class.getSimpleName());

	@BeforeClass
	public static void beforeAll() throws Exception {
		Properties properties = new Properties();
		properties.put(PropertyDefs.IGNORE_FILE_NAME_KEY_SHORT_FORM, ".p4ignore");
		properties.put(PropertyDefs.ENABLE_GRAPH_SHORT_FORM, "true");
		setupServer(p4d.getRSHURL(), "p4jtestsuper", "p4jtestsuper", true, properties);
		extract(new File(PACK_FILE_PATH + "scm-api-plugin.git.tar.gz"), PACK_FILE_PATH + "unpacked");
	}

	@Before
	public void setUp() {
		// initialization code (before each test).
		try {
			client = server.getClient(getPlatformClientName("GraphCatFile.ws"));
			assertNotNull(client);
			server.setCurrentClient(client);
		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}

	/**
	 * Cleans up the expanded git repo at the end of the tests
	 *
	 * @throws IOException
	 */
	@AfterClass
	public static void afterClass() throws IOException {
		FileUtils.cleanDirectory(new File(PACK_FILE_PATH + "unpacked/scm-api-plugin.git"));
	}

	/**
	 * Tests that command fails on incomplete options supplied to the command
	 *
	 * @throws P4JavaException
	 */
	@Test
	public void receivePackIncompleteOptions() throws P4JavaException {
		String repo = "//graph/pack-receive";
		GraphReceivePackOptions graphRecvPackOptions = new GraphReceivePackOptions();
		graphRecvPackOptions.setRepo(repo);

		exception.expect(RequestException.class);
		server.doGraphReceivePack(graphRecvPackOptions);
	}

	/**
	 * Tests that command fails when invoked on an invalid pack
	 *
	 * @throws P4JavaException
	 */
	@Test
	public void receivePackNonExitentPack() throws P4JavaException {
		String repo = "//graph/pack-receive";
		String packFile = "unpack.scm-api-plugin.git/objects/pack/pack-156ed2f0871e9c.pack";
		String masterCommit = "master=5631932f5cdf6c3b829911b6fe5ab42d436d74da";
		GraphReceivePackOptions graphRecvPackOptions = new GraphReceivePackOptions();
		graphRecvPackOptions.setRepo(repo);
		graphRecvPackOptions.setFile(packFile);
		graphRecvPackOptions.setRef(masterCommit);

		exception.expect(RequestException.class);
		server.doGraphReceivePack(graphRecvPackOptions);
	}

	/**
	 * Tests a valid case where the pack file exists and the command is formed correctly
	 *
	 * @throws P4JavaException
	 */
	@Test
	public void receivePackOnExistingPack() throws P4JavaException {
		String repo = "//graph/pack-receive";
		String packFile = PACK_FILE_PATH + "unpacked/scm-api-plugin.git/objects/pack/pack-956185b99350673698b9dbbeb6fd45b906e47436.pack";
		String packedRefs = PACK_FILE_PATH + "unpacked/scm-api-plugin.git/packed-refs";
		String masterCommit = "master=5631932f5cdf6c3b829911b6fe5ab42d436d74da";

		GraphReceivePackOptions graphRecvPackOptions = new GraphReceivePackOptions();
		graphRecvPackOptions.setRepo(repo);
		graphRecvPackOptions.setFile(packFile);
		graphRecvPackOptions.setRef(masterCommit);
		server.doGraphReceivePack(graphRecvPackOptions);

		List<Map<String, Object>> resultMaps = server.execMapCmdList(
				FILES, new String[]{repo + "/..."}, null);
		assertNotNull(resultMaps);
		assertTrue(resultMaps.size() > 10);

		//Tests without -r
		repo = "//graph/pack-receive-noR";
		graphRecvPackOptions = new GraphReceivePackOptions();
		graphRecvPackOptions.setRepo(repo);
		graphRecvPackOptions.setFile(packFile);
		server.doGraphReceivePack(graphRecvPackOptions);

		resultMaps = server.execMapCmdList(
				FILES, new String[]{repo + "/..."}, null);
		assertNotNull(resultMaps);
		assertEquals(1, resultMaps.size());

		//Tests -F
		repo = "//graph/pack-receive";
		graphRecvPackOptions = new GraphReceivePackOptions();
		graphRecvPackOptions.setRepo(repo);
		graphRecvPackOptions.setForceRef(masterCommit);
		server.doGraphReceivePack(graphRecvPackOptions);

		resultMaps = server.execMapCmdList(
				FILES, new String[]{repo + "/..."}, null);
		assertNotNull(resultMaps);
		assertTrue(resultMaps.size() > 10);

		//Tests -p
		repo = "//graph/pack-receive";
		graphRecvPackOptions = new GraphReceivePackOptions();
		graphRecvPackOptions.setRepo(repo);
		graphRecvPackOptions.setFile(packFile);
		graphRecvPackOptions.setPackedRef(packedRefs);
		server.doGraphReceivePack(graphRecvPackOptions);

		resultMaps = server.execMapCmdList(
				FILES, new String[]{repo + "/..."}, null);
		assertNotNull(resultMaps);
		assertTrue(resultMaps.size() > 10);
	}

	/**
	 * @param archive
	 * @param destination
	 * @throws Exception
	 */
	private static void extract(File archive, String destination) throws Exception {
		TarArchiveInputStream tarIn = null;
		tarIn = new TarArchiveInputStream(
				new GzipCompressorInputStream(new BufferedInputStream(new FileInputStream(archive))));

		TarArchiveEntry tarEntry = tarIn.getNextTarEntry();
		while (tarEntry != null) {
			File node = new File(destination, tarEntry.getName());

			if (tarEntry.isDirectory()) {
				node.mkdirs();
			} else {
				try {
					node.createNewFile();
				} catch (IOException e) {
					tarEntry = tarIn.getNextTarEntry();
					continue;
				}
				byte[] buf = new byte[1024];
				BufferedOutputStream bout = new BufferedOutputStream(new FileOutputStream(node));

				int len = 0;
				while ((len = tarIn.read(buf)) != -1) {
					bout.write(buf, 0, len);
				}
				bout.close();
			}
			tarEntry = tarIn.getNextTarEntry();
		}
		tarIn.close();
	}
}
