package com.perforce.p4java.tests.qa;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.option.client.EditFilesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.test.TestServer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.List;

import static com.perforce.p4java.core.file.FileAction.EDIT;
import static com.perforce.p4java.core.file.FileSpecBuilder.makeFileSpecList;
import static com.perforce.p4java.core.file.FileSpecOpStatus.INFO;
import static com.perforce.p4java.core.file.FileSpecOpStatus.VALID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringStartsWith.startsWith;
@RunWith(JUnitPlatform.class)
public class EditFilesTest {

	private static TestServer ts = null;
	private static Helper helper = null;
    private static IClient client = null;
    private static File missingFile = null;

	// simple setup with one file with multiple revs
	@BeforeAll
	public static void beforeClass() throws Throwable {
		helper = new Helper();
		ts = new TestServer();
		ts.getServerExecutableSpecification().setCodeline(helper.getServerVersion());
		ts.start();

        IOptionsServer server = helper.getServer(ts);
		server.setUserName(ts.getUser());
		server.connect();

        IUser user = server.getUser(ts.getUser());

		client = helper.createClient(server, "client1");
		server.setCurrentClient(client);

        File testFile = new File(client.getRoot(), "foo.txt");
		helper.addFile(server, user, client, testFile.getAbsolutePath(), "EditFilesTest", "text");

		missingFile = new File(client.getRoot(), "missing.txt");
		helper.addFile(server, user, client, missingFile.getAbsolutePath(), "EditFilesTest", "text");
		missingFile.delete();
	}

	// verify that we get a warning about the missing file
	@Test
	public void editMissingFile() throws Throwable {
		List<IFileSpec> fileSpec = makeFileSpecList(missingFile.getAbsolutePath());
		List<IFileSpec> editList = client.editFiles(fileSpec, new EditFilesOptions());

		for (IFileSpec file : editList) {
			if (file.getOpStatus() == VALID) {
				assertThat(file.getAction(), is(EDIT));
			} else if (file.getOpStatus() == INFO) {
				assertThat(file.getStatusMessage(), startsWith("can't change mode of file"));
			}
		}
	}

	@AfterAll
	public static void afterClass() {
		helper.after(ts);
	}
}
	