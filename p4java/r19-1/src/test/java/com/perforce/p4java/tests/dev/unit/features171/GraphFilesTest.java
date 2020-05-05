package com.perforce.p4java.tests.dev.unit.features171;

import com.perforce.p4java.PropertyDefs;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.server.GetDepotFilesOptions;
import com.perforce.p4java.tests.GraphServerRule;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class GraphFilesTest extends P4JavaRshTestCase {

	@ClassRule
	public static GraphServerRule p4d = new GraphServerRule("r17.1", GraphFilesTest.class.getSimpleName());

	@BeforeClass
	public static void beforeAll() throws Exception {
		Properties properties = new Properties();
		properties.put(PropertyDefs.IGNORE_FILE_NAME_KEY_SHORT_FORM, ".p4ignore");
		properties.put(PropertyDefs.ENABLE_GRAPH_SHORT_FORM, "true");
		setupServer(p4d.getRSHURL(), "p4jtestsuper", "p4jtestsuper", true, properties);
	}

	@Test
	public void graphCatFileCommit() {
		String path = "//graph/p4-plugin/...";
		String sha = "ca25f3eb7074ce44654a6bbaaa574cae1d214289";

		try {
			GetDepotFilesOptions filesOpts = new GetDepotFilesOptions();
			List<IFileSpec> spec = FileSpecBuilder.makeFileSpecList(path + "@" + sha);
			List<IFileSpec> files = server.getDepotFiles(spec, filesOpts);
			assertNotNull(files);

			IFileSpec file = null;
			for(IFileSpec f : files) {
				if(f.getDepotPathString().equalsIgnoreCase("//graph/p4-plugin/src/main/webapp/script.js")) {
					file = f;
				}
			}

			assertNotNull(file);
			assertEquals("1cd34a0367cb4a6a54bf0e8735f7e7af376b9d0d", file.getBlobSha());
			assertEquals("ca25f3eb7074ce44654a6bbaaa574cae1d214289", file.getCommitSha());
			assertEquals("//graph/p4-plugin.git", file.getRepoName());

		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}
}
