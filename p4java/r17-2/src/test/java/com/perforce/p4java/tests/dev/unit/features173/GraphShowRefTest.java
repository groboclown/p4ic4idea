package com.perforce.p4java.tests.dev.unit.features173;

import com.perforce.p4java.PropertyDefs;
import com.perforce.p4java.graph.IGraphRef;
import com.perforce.p4java.option.server.GraphShowRefOptions;
import com.perforce.p4java.tests.GraphServerRule;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@TestId("Dev173_GraphShowRefTest")
public class GraphShowRefTest extends P4JavaRshTestCase {

	@Rule
	public ExpectedException exception = ExpectedException.none();

	@ClassRule
	public static GraphServerRule p4d = new GraphServerRule("r17.1", GraphShowRefTest.class.getSimpleName());

	@BeforeClass
	public static void beforeAll() throws Exception {
		Properties properties = new Properties();
		properties.put(PropertyDefs.ENABLE_GRAPH_SHORT_FORM, "true");
		setupServer(p4d.getRSHURL(), "p4jtestsuper", "p4jtestsuper", true, properties);
	}

	@Test
	public void getAllRefs() {
		try {
			GraphShowRefOptions opts = new GraphShowRefOptions();
			List<IGraphRef> refs = server.getGraphShowRefs(opts);
			assertNotNull(refs);
			assertTrue(refs.size() > 70);
		} catch (Exception e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}

	@Test
	public void getAllBranches() {
		try {
			GraphShowRefOptions opts = new GraphShowRefOptions();
			opts.setType("branch");
			List<IGraphRef> refs = server.getGraphShowRefs(opts);
			assertNotNull(refs);

			// TODO activate test with P4D 17.2 (job091982)
			// assertEquals(4,refs.size());

		} catch (Exception e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}

	@Test
	public void getAllBranchesInRepo() {
		try {
			GraphShowRefOptions opts = new GraphShowRefOptions();
			opts.setType("branch");
			opts.setRepo("//graph/scm-api-plugin");
			List<IGraphRef> refs = server.getGraphShowRefs(opts);
			assertNotNull(refs);
			assertEquals(3, refs.size());

			IGraphRef ref = refs.get(0);
			assertNotNull(ref);
			assertEquals("//graph/scm-api-plugin", ref.getRepo());
			assertEquals("refs/heads/dev-A", ref.getName());
			assertEquals("branch", ref.getType());
			assertEquals("8c06badeb1142c5e026af456f665723103dd381e", ref.getSha());
		} catch (Exception e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}

	@Test
	public void getAllBranchesInNonRepo() {
		try {
			GraphShowRefOptions opts = new GraphShowRefOptions();
			opts.setType("branch");
			opts.setRepo("//graph/non");
			server.getGraphShowRefs(opts);
			fail("Exception Expected");
		} catch (Exception e) {
			assertTrue(e.getLocalizedMessage().contains("Repo '//graph/non' doesn't exist."));
		}
	}

	@Test
	public void getRepoRefsByUser() {
		try {
			GraphShowRefOptions opts = new GraphShowRefOptions();
			opts.setRepo("//graph/scm-api-plugin");
			opts.setUser("p4jtestsuper");
			List<IGraphRef> refs = server.getGraphShowRefs(opts);
			assertNotNull(refs);
			assertEquals(30, refs.size());
		} catch (Exception e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}

	@Test
	public void getRepoRefsByLimitedUser() {
		try {
			GraphShowRefOptions opts = new GraphShowRefOptions();
			opts.setRepo("//graph/scm-api-plugin");
			opts.setUser("jenkins");
			server.getGraphShowRefs(opts);
			fail("Exception Expected");
		} catch (Exception e) {
			assertTrue(e.getLocalizedMessage().contains("//graph/scm-api-plugin.git - access denied."));
		}
	}
}
