package com.perforce.p4java.tests.dev.unit.features172;

import com.perforce.p4java.PropertyDefs;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.ListData;
import com.perforce.p4java.option.server.GetDepotFilesOptions;
import com.perforce.p4java.option.server.ListOptions;
import com.perforce.p4java.server.IServerAddress.Protocol;
import com.perforce.p4java.tests.GraphServerRule;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class ListCommandTest extends P4JavaRshTestCase {

	private static final String userName = "p4jtestsuper";

	@ClassRule
	public static GraphServerRule p4d = new GraphServerRule("r17.1", ListCommandTest.class.getSimpleName());

	@BeforeClass
	public static void beforeAll() throws Exception {
		Properties properties = new Properties();
		properties.put(PropertyDefs.ENABLE_GRAPH_SHORT_FORM, "true");

		String url = p4d.getRSHURL();
		url = url.replace(Protocol.P4JRSH.toString(), Protocol.P4JRSHNTS.toString());
		setupServer(url, userName, userName, true, properties);
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
	 * Basic test for p4 list functionality
	 *
	 * @throws P4JavaException
	 */
	@Test
	public void testList() throws P4JavaException {

		ListOptions listOptions = new ListOptions();
		listOptions.setLabel("test-label");

		String[] filePathToTest = {"//p4-perl/main/main/P4/..."};
		List<IFileSpec> testFileSpec = FileSpecBuilder.makeFileSpecList(filePathToTest);
		ListData listData = server.getListData(testFileSpec, listOptions);

		Assert.assertNotNull(listData);
		Assert.assertNotNull(listData.getLable());
		Assert.assertEquals("test-label", listData.getLable());
		Assert.assertTrue(listData.getTotalFileCount() > 0);

		List<IFileSpec> fileSpec = FileSpecBuilder.makeFileSpecList(filePathToTest[0] + "@test-label");
		GetDepotFilesOptions opts = new GetDepotFilesOptions();
		List<IFileSpec> result = server.getDepotFiles(fileSpec, opts);

		assertNotNull(result);
		Assert.assertEquals(listData.getTotalFileCount(), result.size());
	}

	/**
	 * Test listing files limited to only that can be mapped via client view
	 *
	 * @throws P4JavaException
	 */
	@Test
	public void testLimitingList() throws P4JavaException {

		ListOptions listOptions = new ListOptions();
		listOptions.setLabel("test-label-limiting");

		String[] filePathToTest = {"//p4-perl/main/main/P4/..."};
		List<IFileSpec> testFileSpec = FileSpecBuilder.makeFileSpecList(filePathToTest);
		ListData listData = client.getListData(testFileSpec, listOptions);

		Assert.assertNotNull(listData);
		Assert.assertNotNull(listData.getLable());
		Assert.assertEquals("test-label-limiting", listData.getLable());
		Assert.assertEquals(0, listData.getTotalFileCount());

		List<IFileSpec> fileSpec = FileSpecBuilder.makeFileSpecList(filePathToTest[0] + "@test-label-limiting");
		GetDepotFilesOptions opts = new GetDepotFilesOptions();
		List<IFileSpec> result = server.getDepotFiles(fileSpec, opts);

		assertNotNull(result);
		Assert.assertEquals(1, result.size());
		Assert.assertEquals("//p4-perl/main/main/P4/...@test-label-limiting - no such file(s).", result.get(0).getStatusMessage());
	}

	@Test
	public void testDeleteList() throws P4JavaException {

		ListOptions listOptions = new ListOptions();
		listOptions.setLabel("test-label-limiting");

		String[] filePathToTest = {"//p4-perl/main/main/P4/..."};
		List<IFileSpec> testFileSpec = FileSpecBuilder.makeFileSpecList(filePathToTest);
		ListData listData = server.getListData(testFileSpec, listOptions);

		Assert.assertNotNull(listData);
		Assert.assertNotNull(listData.getLable());
		Assert.assertEquals("test-label-limiting", listData.getLable());
		Assert.assertEquals(11, listData.getTotalFileCount());

		listOptions.setDelete(true);
		listData = server.getListData(null, listOptions);
		Assert.assertNotNull(listData);
		Assert.assertEquals(null, listData.getLable());

		List<IFileSpec> fileSpec = FileSpecBuilder.makeFileSpecList(filePathToTest[0] + "@test-label-limiting");
		GetDepotFilesOptions opts = new GetDepotFilesOptions();
		List<IFileSpec> result = server.getDepotFiles(fileSpec, opts);

		assertNotNull(result);
		Assert.assertEquals(1, result.size());
		Assert.assertEquals("Invalid changelist/client/label/date '@test-label-limiting'.", result.get(0).getStatusMessage());
	}
}
