package com.perforce.p4java.tests.qa;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IDepot;
import com.perforce.p4java.core.IDepot.DepotType;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.impl.generic.core.Depot;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.test.TestServer;









import java.io.File;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.fail;



public class DeleteDepotTest {
    private static TestServer ts = null;
    private static Helper h = null;
    private static IOptionsServer server = null;
    private static String depotName = "newDepot";

    // server setup, nothing fancy
    @BeforeClass
    public static void beforeClass() throws Throwable {
        h = new Helper();
        ts = new TestServer();
        ts.getServerExecutableSpecification().setCodeline(h.getServerVersion());
        ts.start();

        server = h.getServer(ts);
        server.setUserName(ts.getUser());
        server.connect();

        IUser user = server.getUser(ts.getUser());

        IClient client = h.createClient(server, "client1");
        server.setCurrentClient(client);

        File testFile = new File(client.getRoot(), "foo.txt");
        h.addFile(server, user, client, testFile.getAbsolutePath(), "EditFilesTest", "text");
    }

    @Before
    public void reset() {
        try {
            List<IDepot> depots = server.getDepots();
            if (depots.size() < 2) {
                IDepot nd = new Depot(
                        depotName,
                        server.getUserName(),
                        null,
                        "A depot of great importance",
                        DepotType.LOCAL,
                        null,
                        null,
                        depotName + "/..."
                );

                server.createDepot(nd);
            }
        } catch (Throwable ignore) {
        }
    }

    /**
     * Given depot should be deleted
     * @throws Exception
     */
    @Test
    public void basicUsage() throws Exception {
        String result = server.deleteDepot(depotName);
        assertThat(isNotBlank(result), is(true));
        assertThat(result, containsString("Depot " + depotName + " deleted."));
        List<IDepot> depots = server.getDepots();
        assertThat(depots.size(), is(1));
    }

    /**
     * failed delete depot as deopt isn't empty
     * @throws Exception
     */
    @Test
    public void depotWithFiles() throws Exception {
        try {
            server.deleteDepot("depot");
            fail("we should never get here");
        } catch (Throwable t) {
            assertThat(t.getMessage(), containsString("Depot depot isn't empty. To delete a depot, all file revisions must be removed and all lazy copy references"
                    + " from other depots must be severed. Use 'p4 obliterate' or 'p4 snap' to break file linkages from other depots,"
                    + " then clear this depot with 'p4 obliterate', then retry the deletion."));
        }
    }

    /**
     * failed delete depot as depot doesn't exist
     */
    @Test
    public void phantomDepot() {
        try {
            server.deleteDepot("phantom");
            fail("we should never get here");
        } catch (Throwable t) {
            assertThat(t.getMessage(), containsString("Depot 'phantom' doesn't exist."));
        }
    }

    @AfterClass
    public static void afterClass() {
        h.after(ts);
    }
}
