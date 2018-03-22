package com.perforce.p4java.tests.dev.unit.features171;

import com.perforce.p4java.PropertyDefs;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.graph.IGraphListTree;
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

public class GraphLsTree extends P4JavaRshTestCase {

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
	public void graphLsTree() {
		String sha = "b92b234a6c05049fdc8fe123449386756c7a08fd";

		try {
			List<IGraphListTree> list = server.getGraphListTree(sha);
			assertNotNull(list);
			assertEquals(100644, list.get(0).getMode());
			assertEquals("blob", list.get(0).getType());
			assertEquals("76d898a3abd298f47bd7bb0c67e4f29eca324997", list.get(0).getSha());
			assertEquals(".gitignore", list.get(0).getName());
		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		}
	}
}
