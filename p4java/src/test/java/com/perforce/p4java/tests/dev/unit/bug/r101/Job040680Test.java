/**
 * 
 */
package com.perforce.p4java.tests.dev.unit.bug.r101;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import com.perforce.p4java.P4JavaUtil;
import com.perforce.p4java.StandardPerforceServers;
import com.perforce.test.ServerRule;
import org.junit.Rule;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.ChangelistStatus;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.FileStatOutputOptions;
import com.perforce.p4java.core.file.IExtendedFileSpec;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.Changelist;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.option.client.EditFilesOptions;
import com.perforce.p4java.option.client.RevertFilesOptions;
import com.perforce.p4java.option.server.GetExtendedFilesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.TestId;
import org.junit.rules.TemporaryFolder;

/**
 *
 */
@TestId("Bugs101_Job040680Test")
public class Job040680Test {
	@Rule
	public ServerRule serverRule = StandardPerforceServers.createP4Java20101();

	@Rule
	public TemporaryFolder clientRoot = new TemporaryFolder();

	public Job040680Test() {
	}

	@Test
	public void testExtendedFilesResults() throws Exception {
		final String testRoot = "//depot/101Bugs/Bugs101_Job040680Test";
		final String testFile = testRoot + "/" + "test.txt";
		IOptionsServer server = null;
		IClient client = null;
		IChangelist changelist = null;

		server = P4JavaUtil.getServer(serverRule.getRshUrl(), StandardPerforceServers.getStandardUserProperties());
		client = P4JavaUtil.getDefaultClient(server, clientRoot.getRoot());
		assertNotNull(client);
		server.setCurrentClient(client);
		P4JavaUtil.checkedForceSyncFiles(client, testRoot + "/...");
		changelist = client.createChangelist(new Changelist(
										IChangelist.UNKNOWN,
										client.getName(),
										server.getUserName(),
										ChangelistStatus.NEW,
										null,
										"Bugs101_Job040680Test test changelist",
										false,
										(Server) server
									));
		assertNotNull(changelist);
		List<IFileSpec> editList = client.editFiles(FileSpecBuilder.makeFileSpecList(testFile),
												new EditFilesOptions().setChangelistId(changelist.getId()));

		assertNotNull(editList);
		assertEquals(1, FileSpecBuilder.getValidFileSpecs(editList).size());
		changelist.refresh();

		List<IFileSpec> shelveList = client.shelveChangelist(changelist);
		assertNotNull(shelveList);
		assertEquals(1, FileSpecBuilder.getValidFileSpecs(editList).size());
		FileStatOutputOptions outputOptions = new FileStatOutputOptions();
		outputOptions.setShelvedFiles(true);
		List<IFileSpec> files = changelist.getFiles(false);
		List<IExtendedFileSpec> fstatFiles = server.getExtendedFiles(files, new GetExtendedFilesOptions()
														.setOutputOptions(outputOptions)
														.setAffectedByChangelist(changelist.getId()));
		assertNotNull(fstatFiles);
		for (IExtendedFileSpec file : fstatFiles) {
			assertNotNull("File was null", file.getDepotPath() );
			assertTrue("File was not shelved", file.isShelved() );
		}
	}
}
