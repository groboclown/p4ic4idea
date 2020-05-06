// NOT FULLY IMPLEMENTED

package com.perforce.p4java.tests.qa;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.option.client.IntegrateFilesOptions;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import com.perforce.p4java.tests.dev.unit.features112.IntegrateSkipActionTest;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.List;

import static com.perforce.p4java.core.file.FileSpecBuilder.makeFileSpecList;
import static com.perforce.p4java.tests.qa.Helper.FILE_SEP;
import static org.junit.Assert.assertEquals;

public class ResolveFileTest extends P4JavaRshTestCase {

	@ClassRule
	public static SimpleServerRule p4d = new SimpleServerRule("r16.1", IntegrateSkipActionTest.class.getSimpleName());

	private static Helper h = new Helper();
	private static IClient client = null;
	private static File sourceBinFile = null;
	private static File targetBinFile = null;

	@BeforeClass
	public static void beforeClass() throws Throwable {
		setupServer(p4d.getRSHURL(), userName, password, true, null);

		IUser user = server.getUser(userName);
		client = h.createClient(server, "client1");
		server.setCurrentClient(client);

		File sourceFile = new File(client.getRoot() + FILE_SEP + "source.txt");
		File targetFile = new File(client.getRoot() + FILE_SEP + "target.txt");
		h.addFile(server, user, client, sourceFile.getAbsolutePath(), "1111", "text");
		h.addFile(server, user, client, targetFile.getAbsolutePath(), "2222", "text");

		sourceBinFile = new File(client.getRoot() + FILE_SEP + "source.bin");
		targetBinFile = new File(client.getRoot() + FILE_SEP + "target.bin");
		h.addFile(server, user, client, sourceBinFile.getAbsolutePath(), "1111", "binary");
		h.addFile(server, user, client, targetBinFile.getAbsolutePath(), "2222", "binary");
	}

	// reset the files
	@Before
	public void before() {

		try {
			client.revertFiles(makeFileSpecList("//..."), null);
		} catch (Throwable t) {

			h.error(t);
		}
	}

	// BINARY
	@Test
	public void binary() throws Throwable {
		List<IFileSpec> source = makeFileSpecList(sourceBinFile.getAbsolutePath());
		List<IFileSpec> target = makeFileSpecList(targetBinFile.getAbsolutePath());
		client.integrateFiles(source.get(0), target.get(0), null, new IntegrateFilesOptions().setDoBaselessMerge(true));

		// resolve the file
		String resolvedText = "12121212";
		InputStream is = new ByteArrayInputStream(resolvedText.getBytes());
		IFileSpec resolvedFile = client.resolveFile(target.get(0), is);
		is.close();

		assertEquals("edit from", resolvedFile.getHowResolved());
	}

}