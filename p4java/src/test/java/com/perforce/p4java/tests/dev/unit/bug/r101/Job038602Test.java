/*
 * 
 */
package com.perforce.p4java.tests.dev.unit.bug.r101;

import com.perforce.p4java.P4JavaUtil;
import com.perforce.p4java.StandardPerforceServers;
import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.ChangelistStatus;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IExtendedFileSpec;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.impl.generic.core.Changelist;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.option.client.AddFilesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.test.ServerRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 *
 */
@TestId("Bugs101_Job038602Test")
public class Job038602Test {
	@Rule
	public ServerRule serverRule = StandardPerforceServers.createP4Java20132();

	@Rule
	public TemporaryFolder clientRoot = new TemporaryFolder();

	@Test
	public void testExtendedAscii() throws Exception {
		final String sourceDir = "//depot/101Bugs/Bugs101_Job038602Test";
		final String sourceFile = sourceDir + "/" + "test01.txt";
		final String targetFile = sourceDir + "/" + "test01Cpy.txt";
		IOptionsServer server;
		IClient client;
		File target;
		IChangelist changelist;

        server = P4JavaUtil.getServer(
                StandardPerforceServers.PORT_P4JAVA_20132, StandardPerforceServers.getStandardUserProperties());
        //server = P4JavaUtil.getServer(serverRule.getRshUrl(), null);
        client = P4JavaUtil.getDefaultClient(server, clientRoot.getRoot());
        assertNotNull(client);
        server.setCurrentClient(client);
        client.revertFiles(FileSpecBuilder.makeFileSpecList(targetFile), null);
        P4JavaUtil.checkedForceSyncFiles(client, sourceDir + "/...");
        target = new File(P4JavaUtil.getSystemPath(client, targetFile));
        if (!target.exists()) {
            P4JavaUtil.copyFile(P4JavaUtil.getSystemPath(client, sourceFile),
                    P4JavaUtil.getSystemPath(client, targetFile));
        }

        changelist = client.createChangelist(new Changelist(
                                                        IChangelist.UNKNOWN,
                                                        client.getName(),
                                                        P4JavaUtil.DEFAULT_USER,
                                                        ChangelistStatus.NEW,
                                                        null,
                                                        "Bugs101_Job038602Test Changelist",
                                                        false,
                                                        (Server) server
                                                    ));
        assertNotNull(changelist);
        AddFilesOptions afo = new AddFilesOptions();
        afo.setChangelistId(changelist.getId());
        afo.setFileType("text");
        List<IFileSpec> addFiles = client.addFiles(
                                        FileSpecBuilder.makeFileSpecList(targetFile),
                                        afo);
        assertNotNull(addFiles);
        assertEquals(1, FileSpecBuilder.getValidFileSpecs(addFiles).size());
        List<IExtendedFileSpec> extSpecs = server.getExtendedFiles(
                                    FileSpecBuilder.makeFileSpecList(targetFile), null);
        assertNotNull(extSpecs);
        assertEquals(1, extSpecs.size());
        assertNotNull(extSpecs.get(0).getFileType());
        assertEquals("text", extSpecs.get(0).getFileType());
	}
}
