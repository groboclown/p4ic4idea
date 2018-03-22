package com.perforce.p4java.tests.dev.unit.features171;

import com.perforce.p4java.PropertyDefs;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.graph.CommitAction;
import com.perforce.p4java.graph.ICommit;
import com.perforce.p4java.graph.IGraphObject;
import com.perforce.p4java.tests.GraphServerRule;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@TestId("Dev171_GraphCatFileTest")
public class GraphCatFileTest extends P4JavaRshTestCase {

	@ClassRule
	public static GraphServerRule p4d = new GraphServerRule("r17.1", GraphCatFileTest.class.getSimpleName());

	@BeforeClass
	public static void beforeAll() throws Exception {
		Properties properties = new Properties();
		properties.put(PropertyDefs.IGNORE_FILE_NAME_KEY_SHORT_FORM, ".p4ignore");
		properties.put(PropertyDefs.ENABLE_GRAPH_SHORT_FORM, "true");
		setupServer(p4d.getRSHURL(), "p4jtestsuper", "p4jtestsuper", true, properties);
	}

	@Test
	public void graphCatFileCommit() {
		String sha = "15b25972edfc52d0a0b1e80249bd8efdc914af92";

		try {
			ICommit commit = server.getCommitObject(sha);
			assertNotNull(commit);

			assertEquals(sha, commit.getCommit());
			assertEquals("b92b234a6c05049fdc8fe123449386756c7a08fd", commit.getTree());
			assertEquals(CommitAction.MERGE, commit.getAction());
			assertEquals("Paul Allen", commit.getAuthor());
			assertEquals("pallen@perforce.com", commit.getAuthorEmail());
			assertTrue(1400000000000L < commit.getDate().getTime());
			assertEquals("GitHub", commit.getCommitter());
			assertTrue(commit.getCommitter().startsWith("GitHub"));
			assertEquals("noreply@github.com", commit.getCommitterEmail());
			assertTrue(1400000000000L < commit.getCommitterDate().getTime());
			assertTrue(commit.getDescription().startsWith("\nMerge pull request #40 from"));

			assertEquals(2, commit.getParents().size());
			assertEquals("406a5a06e0b73426a6e67000c0285f5be617306b", commit.getParents().get(0));
			assertEquals("aa2a8bd219b8675e0279b09494a740edf646fb95", commit.getParents().get(1));
		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}

	@Test
	public void graphCatFileInvalidCommit() {
		String sha = "0000000000000000000000000000000000000000";

		try {
			ICommit commit = server.getCommitObject(sha);
			assertEquals(null, commit);
		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}

	@Test
	public void graphCatFileBlob() {
		String sha = "15b25972edfc52d0a0b1e80249bd8efdc914af92";
		String repo = "//graph/p4-plugin";

		try {
			InputStream inputStream = server.getBlobObject(repo, sha);
			assertNotNull(inputStream);

			String blob = IOUtils.toString(inputStream, "UTF-8");
			assertTrue(blob.contains("tree b92b234a6c05049fdc8fe123449386756c7a08fd"));
			assertTrue(blob.contains("Merge pull request #40 from s-sutherland"));
		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} catch (IOException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}

	@Test
	public void graphCatFileBlobInvalidRepo() {
		String sha = "15b25972edfc52d0a0b1e80249bd8efdc914af92";
		String repo = "//graph/foo";

		try {
			InputStream inputStream = server.getBlobObject(repo, sha);
			fail("Expected an P4JavaException to be thrown");
		} catch (P4JavaException e) {
			assertThat(e.getMessage(), is("Repo '//graph/foo' doesn't exist.\n"));
		}
	}

	@Test
	public void graphCatFileObject() {
		String sha = "b92b234a6c05049fdc8fe123449386756c7a08fd";

		try {
			IGraphObject obj = server.getGraphObject(sha);
			assertNotNull(obj);
			assertEquals("tree", obj.getType());
			assertEquals(sha, obj.getSha());
		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}

	@Test
	public void graphCatFileObject2() {
		String sha = "15b25972edfc52d0a0b1e80249bd8efdc914af92";

		try {
			IGraphObject obj = server.getGraphObject(sha);
			assertNotNull(obj);
			assertEquals("commit", obj.getType());
			assertEquals(sha, obj.getSha());
		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}
}
