package com.perforce.p4java.tests.dev.unit.features171;

import com.perforce.p4java.PropertyDefs;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.graph.IRevListCommit;
import com.perforce.p4java.option.server.GraphRevListOptions;
import com.perforce.p4java.tests.GraphServerRule;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.*;
import org.junit.rules.ExpectedException;

import java.util.List;
import java.util.Properties;

import static org.junit.Assert.*;

@TestId("Dev171_GraphRevListTest")
public class GraphRevListTest extends P4JavaRshTestCase {

    @Rule
    public ExpectedException exception = ExpectedException.none();


    @ClassRule
    public static GraphServerRule p4d = new GraphServerRule("r17.1", GraphRevListTest.class.getSimpleName());

    @BeforeClass
    public static void beforeAll() throws Exception {
        Properties properties = new Properties();
        properties.put(PropertyDefs.IGNORE_FILE_NAME_KEY_SHORT_FORM, ".p4ignore");
        properties.put(PropertyDefs.ENABLE_GRAPH_SHORT_FORM, "true");
        setupServer(p4d.getRSHURL(), "p4jtestsuper", "p4jtestsuper", true, properties);
    }

    @Before
    public void setUp() {
        // initialization code (before each test).
        try {
            client = server.getClient(getPlatformClientName("GraphCatFile.ws"));
            assertNotNull(client);
            server.setCurrentClient(client);
        } catch (P4JavaException e) {
            fail("Unexpected exception: " + e.getLocalizedMessage());
        }
    }

    /**
     * Tests the retrieval of rev list from given depot
     */
    @Test
    public void allRevListWithExistingDepot() {
        String depot = "//graph/p4-plugin";

        try {
            GraphRevListOptions graphRevListOptions = new GraphRevListOptions()
                    .withDepot(depot);

            List<IRevListCommit> revListResult = server.getGraphRevList(graphRevListOptions);
            assertNotNull(revListResult);

            assertNotNull(revListResult);
            assertTrue(revListResult.size() > 0);

        } catch (Exception e) {
            fail("Unexpected exception: " + e.getLocalizedMessage());
        }
    }

    /**
     * Tests the retrieval of rev list from given depot
     */
    @Test()
    public void revListWithNonExistingDepot() throws Exception {
        String depot = "//graph/p4-plugin-invalid";

        GraphRevListOptions graphRevListOptions = new GraphRevListOptions()
                .withDepot(depot);
        exception.expect(P4JavaException.class);
        List<IRevListCommit> revListResult = server.getGraphRevList(graphRevListOptions);
    }

    /**
     * Tests the retrieval of rev list from given depot
     */
    @Test
    public void revListWithMaxValue() {
        String depot = "//graph/p4-plugin";

        try {
            GraphRevListOptions graphRevListOptions = new GraphRevListOptions()
                    .withDepot(depot)
                    .withMaxValue(5);

            List<IRevListCommit> revListResult = server.getGraphRevList(graphRevListOptions);
            assertNotNull(revListResult);

            assertNotNull(revListResult);
            assertEquals(5, revListResult.size());

            graphRevListOptions.withMaxValue(0);
            revListResult = server.getGraphRevList(graphRevListOptions);
            assertNotNull(revListResult);

            assertNotNull(revListResult);
            assertEquals(649, revListResult.size());

            graphRevListOptions.withMaxValue(-1);
            revListResult = server.getGraphRevList(graphRevListOptions);
            assertNotNull(revListResult);

            assertNotNull(revListResult);
            assertEquals(649, revListResult.size());

        } catch (P4JavaException e) {
            fail("Unexpected exception: " + e.getLocalizedMessage());
        }
    }

    /**
     * Tests the retrieval of rev list from given depot
     */
    @Test
    public void revListWithCommit() throws P4JavaException {
        String depot = "//graph/p4-plugin";


        String[] commitValueTest1 = {"07c6c96621eafe23ae5ec411a9811ad6d3bf49da"};

        GraphRevListOptions graphRevListOptions = new GraphRevListOptions()
                .withDepot(depot)
                .withCommitValue(commitValueTest1);

        List<IRevListCommit> revListResult = server.getGraphRevList(graphRevListOptions);
        assertNotNull(revListResult);
        assertEquals(2, revListResult.size());


        graphRevListOptions = graphRevListOptions
                .withDepot(depot)
                .withCommitValue(null);
        revListResult = server.getGraphRevList(graphRevListOptions);
        assertNotNull(revListResult);
        assertEquals(649, revListResult.size());


        String[] commitValueTest2 = {"40b2770a413267e79b8b3d3adf299dda44b8161f", "d82a0624a2f64ee867b66a575f924d6147d0695c"};

        graphRevListOptions.withCommitValue(commitValueTest2);
        revListResult = server.getGraphRevList(graphRevListOptions);
        assertNotNull(revListResult);

        assertEquals(6, revListResult.size());

        String[] commitValueTest3 = {"40b2770a413267e79b8b3d3adf299dda44b81"};

        graphRevListOptions.withCommitValue(commitValueTest3);
        revListResult = server.getGraphRevList(graphRevListOptions);
        assertNotNull(revListResult);

        assertEquals(0, revListResult.size());


        exception.expect(P4JavaException.class);
        String[] commitValueTest4 = {"40b2770a413267e79b8b3d3adf299dda44b8161fd82a0624a2f64ee867b66a575f924d6147d0695c"};
        graphRevListOptions.withCommitValue(commitValueTest4);
        revListResult = server.getGraphRevList(graphRevListOptions);
    }
}
