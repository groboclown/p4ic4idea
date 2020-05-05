package com.perforce.p4java.tests.dev.unit.feature.client;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.client.IClientViewMapping;
import com.perforce.p4java.core.IStream;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.generic.client.ClientView;
import com.perforce.p4java.impl.generic.core.Stream;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import com.perforce.p4java.tests.dev.unit.features172.FilesysUTF8bomTest;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.assertTrue;

public class ClientWhereTest extends P4JavaRshTestCase {

	@ClassRule
	public static SimpleServerRule p4d = new SimpleServerRule("r16.1", FilesysUTF8bomTest.class.getSimpleName());

	static IClient client = null;
	static IClient streamsClient = null;
	static String filename1 = "clientWhereTest/test.txt";
	static String filename2 = "clientWhereTestExclusion/test.txt";
	static String filename3 = "overlay1/test.txt";
	static String filename4 = "overlay2/test.txt";
	static String streamsFilename1 = "streamsWhereTest/test.txt";
	static String streamsFilename2 = "streamsWhereTestExclusion/test.txt";
	static String streamsFilename3 = "streamsWhereTestInclusion/test.txt";
	static String streamsFilename4 = "clientWhereTestInclusion/test.txt";


	@BeforeClass
	public static void beforeAll() throws Exception {
		setupServer(p4d.getRSHURL(), userName, password, true, null);
		client = getClient(server);
		createTextFileOnServer(client, filename1, "clientWhereTest");
		createTextFileOnServer(client, filename2, "clientWhereTest");
		createTextFileOnServer(client, filename3, "clientWhereTest");
		createTextFileOnServer(client, filename4, "clientWhereTest");
		ClientView clientView = client.getClientView();
		IClientViewMapping clientViewExcludeMapping = new ClientView.ClientViewMapping(2, "-//depot/clientWhereTestExclusion/...", "//" + client.getName() + "/clientWhereTestExclusion/...");
		IClientViewMapping clientViewOverlayMapping1 = new ClientView.ClientViewMapping(3, "//depot/overlay1/...", "//" + client.getName() + "/overlay/...");
		IClientViewMapping clientViewOverlayMapping2 = new ClientView.ClientViewMapping(4, "+//depot/overlay2/...", "//" + client.getName() + "/overlay/...");
		clientView.addEntry(clientViewExcludeMapping);
		clientView.addEntry(clientViewOverlayMapping1);
		clientView.addEntry(clientViewOverlayMapping2);
		client.setClientView(clientView);
	}

	@Test
	public void testLocalWhere() throws ConnectionException, AccessException, RequestException {
		server.setCurrentClient(client);
		IFileSpec fileSpec1 = new FileSpec("//depot/" + filename1);
		IFileSpec fileSpec2 = new FileSpec("//depot/" + filename2);
		IFileSpec fileSpec3 = new FileSpec("//depot/" + filename3);
		IFileSpec fileSpec4 = new FileSpec("//depot/" + filename4);
		List fileSpecList = new ArrayList<IFileSpec>();
		fileSpecList.add(fileSpec1);
		fileSpecList.add(fileSpec2);
		fileSpecList.add(fileSpec3);
		fileSpecList.add(fileSpec4);
		client.localWhere(fileSpecList);
		for (Object filespecObject : fileSpecList) {
			IFileSpec fileSpec = (IFileSpec) filespecObject;
			assertTrue(fileSpec.getClientPathString().contains("//" + client.getName()));
			assertTrue(fileSpec.getLocalPathString().contains(client.getRoot()));
			assertTrue(fileSpec.getDepotPathString().contains("//depot/"));
		}
	}

	@Test
	public void testLocalWhereWithStreamsClient() throws Exception {
		String streamsDepotName = "testStreamsDepot";
		String streamName = "testStream";
		String testStream = "//" + streamsDepotName + "/" + streamName;
		String[] viewPaths = new String[]{"share ..."};

		createStreamsDepot(streamsDepotName, server, "1");
		IStream streamObject = Stream.newStream(server, testStream,"mainline", null, null, null, null, viewPaths, null, null);
		server.createStream(streamObject);
		streamsClient = createStreamsClient(server, "streamsClient", testStream);
		createTextFileOnServer(streamsClient, streamsFilename1, "clientWhereTest");
		createTextFileOnServer(streamsClient, streamsFilename2, "clientWhereTest");
		createTextFileOnServer(streamsClient, streamsFilename3, "clientWhereTest");

		IFileSpec fileSpec1 = new FileSpec(testStream + "/" + streamsFilename1);
		IFileSpec fileSpec2 = new FileSpec(testStream + "/" + streamsFilename2);
		IFileSpec fileSpec3 = new FileSpec(testStream + "/" + streamsFilename3);
		List fileSpecList = new ArrayList<IFileSpec>();
		fileSpecList.add(fileSpec1);
		fileSpecList.add(fileSpec2);
		fileSpecList.add(fileSpec3);
		streamsClient.localWhere(fileSpecList);
		for (Object filespecObject : fileSpecList) {
			IFileSpec fileSpec = (IFileSpec) filespecObject;
			assertTrue(fileSpec.getClientPathString().contains("//" + streamsClient.getName()));
			assertTrue(fileSpec.getLocalPathString().contains(streamsClient.getRoot()));
			assertTrue(fileSpec.getDepotPathString().contains("//" + streamsDepotName + "/"));
		}
	}

	@Test
	public void testRemoteWhere() throws ConnectionException, AccessException, RequestException {
		server.setCurrentClient(client);
		IFileSpec fileSpec11 = new FileSpec("//depot/" + filename1);
		IFileSpec fileSpec12 = new FileSpec("//depot/" + filename2);
		IFileSpec fileSpec13 = new FileSpec("//depot/" + filename3);
		IFileSpec fileSpec14 = new FileSpec("//depot/" + filename4);
		List fileSpecList = new ArrayList<IFileSpec>();
		fileSpecList.add(fileSpec11);
		fileSpecList.add(fileSpec12);
		fileSpecList.add(fileSpec13);
		fileSpecList.add(fileSpec14);
		fileSpecList = client.where(fileSpecList);
		for (Object filespecObject : fileSpecList) {
			IFileSpec fileSpec = (IFileSpec) filespecObject;
			assertTrue(fileSpec.getClientPathString().contains("//" + client.getName()));
			assertTrue(fileSpec.getLocalPathString().contains(client.getRoot()));
			assertTrue(fileSpec.getDepotPathString().contains("//depot/"));
		}
	}

}
