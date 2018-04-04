package com.perforce.p4java.tests.dev.unit.features171;

import com.perforce.p4java.PropertyDefs;
import com.perforce.p4java.core.file.FileAction;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.RequestException;
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

public class GraphDescribeCommitTest extends P4JavaRshTestCase {

	private static final String userName = "p4jtestsuper";

	@ClassRule
	public static GraphServerRule p4d = new GraphServerRule("r17.1", GraphHaveTest.class.getSimpleName());

	@BeforeClass
	public static void beforeAll() throws Exception {
		Properties properties = new Properties();
		properties.put(PropertyDefs.ENABLE_GRAPH_SHORT_FORM, "true");
		setupServer(p4d.getRSHURL(), userName, userName, true, properties);
	}

	@Test
	public void describeCommit() {
		String repo = "//graph/p4-plugin.git";
		String sha = "cd6ffee3a1d7fd8f9dfd00b5bbe519af58372a6f";

		try {
			List<IFileSpec> commit = server.getCommitFiles(repo, sha);
			assertNotNull(commit);
			assertEquals(2, commit.size());

			IFileSpec item = commit.get(0);
			assertNotNull(item);
			assertEquals("//graph/p4-plugin/.gitignore", item.getDepotPathString());
			assertEquals(FileAction.EDIT, item.getAction());
			assertEquals("cd6ffee3a1d7fd8f9dfd00b5bbe519af58372a6f", item.getCommitSha());
			assertEquals("7fb6378711932a372698247ecbdb5ad84d760667", item.getTreeSha());

		} catch (ConnectionException e) {
			fail("Unexpected ConnectionException: " + e.getLocalizedMessage());
		} catch (AccessException e) {
			fail("Unexpected AccessException: " + e.getLocalizedMessage());
		} catch (RequestException e) {
			fail("Unexpected AccessException: " + e.getLocalizedMessage());
		}
	}
}