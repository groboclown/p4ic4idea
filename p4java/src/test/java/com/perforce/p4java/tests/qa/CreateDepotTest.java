package com.perforce.p4java.tests.qa;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IDepot;
import com.perforce.p4java.core.IDepot.DepotType;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.impl.generic.core.Depot;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.test.TestServer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.fail;

@RunWith(JUnitPlatform.class)
public class CreateDepotTest {
    private static TestServer ts = null;
    private static Helper helper = null;
    private static IOptionsServer server = null;
    private static IUser user = null;
    private static IClient client = null;
    private static final String NEW_DEPOT_NAME = "newDepot";

    @BeforeAll
    public static void beforeClass() throws Throwable {
        helper = new Helper();
        ts = new TestServer();
        ts.getServerExecutableSpecification().setCodeline(helper.getServerVersion());

        ts.initialize();
        // just use RSH
        //ts.start();

        server = helper.getServer(ts);
        server.setUserName(ts.getUser());
        server.connect();

        user = server.getUser(ts.getUser());

        client = helper.createClient(server, "client1");
        server.setCurrentClient(client);

        File testFile = new File(client.getRoot(), "foo.txt");
        helper.addFile(server, user, client, testFile.getAbsolutePath(), "EditFilesTest", "text");
    }

    @BeforeEach
    public void reset() {
        try {
            server.deleteDepot(NEW_DEPOT_NAME);
        } catch (Throwable ignore) {
        }
    }

    /**
     * attempt to create a local depot
     * @throws Exception
     */
    @Test
    public void basicUsage() throws Exception {
        int depotSize = getDepotSize();

        String depotName1 = "newDepot1";
        IDepot nd = new Depot(
                depotName1,
                server.getUserName(),
                null,
                "A depot of great importance",
                DepotType.LOCAL,
                null,
                null,
                depotName1 + "/..."
        );

        String result = server.createDepot(nd);
        assertThat(isNotBlank(result), is(true));
        assertThat(result, containsString("Depot " + depotName1 + " saved."));
        List<IDepot> depots = server.getDepots();
        assertThat(depots.size(), is(depotSize + 1));
    }

    /**
     * create remote depot
     * @throws Exception
     */
    @Test
    public void remoteDepot() throws Exception {
        int depotSize = getDepotSize();
        String depotName2 = "newDepot2";
        IDepot nd = new Depot(
                depotName2,
                server.getUserName(),
                null,
                "A depot of great importance",
                DepotType.REMOTE,
                "1666",
                null,
                "//depot/..."
        );

        String result = server.createDepot(nd);
        assertThat(isNotBlank(result), is(true));
        assertThat(result, containsString("Depot " + depotName2 + " saved."));
        List<IDepot> depots = server.getDepots();
        assertThat(depots.size(), is(depotSize + 1));

        boolean depotSeen = false;
        for (IDepot d : depots) {
            if (d.getName().equals(depotName2)) {
                d = server.getDepot(depotName2);
                assertThat(d.getDepotType(), is(DepotType.REMOTE));
                assertThat(d.getMap(), is("//depot/..."));
                assertThat(d.getDescription(), is("A depot of great importance"));
                assertThat(d.getAddress(), is("1666"));
                assertThat(d.getOwnerName(), is(user.getLoginName()));

                depotSeen = true;
            }
        }

        assertThat(depotSeen, is(true));
    }

    private int getDepotSize() throws Exception {
        return server.getDepots().size();
    }

    /**
     * create spec depot
     * @throws Exception
     */
    @Test
    public void specDepot() throws Exception {
        int depotSize = getDepotSize();
        String depotName3 = "newDepot3";
        IDepot nd = new Depot(
                depotName3,
                server.getUserName(),
                null,
                "A depot of great importance",
                DepotType.SPEC,
                null,
                null,
                depotName3 + "/..."
        );

        String result = server.createDepot(nd);
        assertThat(result, notNullValue());
        assertThat(result, containsString("Depot " + depotName3 + " saved."));
        List<IDepot> depots = server.getDepots();
        assertThat(depots.size(), is(depotSize + 1));
        boolean depotSeen = false;

        for (IDepot d : depots) {
            if (d.getName().contentEquals(depotName3)) {
                d = server.getDepot(depotName3);
                assertThat(d.getDepotType(), is(DepotType.SPEC));
                assertThat(d.getMap(), is(depotName3 + "/..."));
                assertThat(d.getDescription(), is("A depot of great importance"));
                assertThat(isBlank(d.getAddress()), is(true));
                assertThat(d.getOwnerName(), is(user.getLoginName()));
                depotSeen = true;
            }
        }

        assertThat(depotSeen, is(true));
    }

    /**
     * create depot with illegal name - overlapping name
     * @throws Exception
     */
    @Test
    public void overlappingName() throws Exception {
        try {
            IDepot nd = new Depot(
                    client.getName(),
                    server.getUserName(),
                    null,
                    "A depot of great importance",
                    DepotType.LOCAL,
                    null,
                    null,
                    NEW_DEPOT_NAME + "/..."
            );

            server.createDepot(nd);
            fail("We should never reach this");
        } catch (Throwable e) {
            assertThat(e.getMessage(), containsString("client1 is a client, not a depot."));
        }
    }

    /**
     * create depot with illegal name - purely numeric name
     * @throws Exception
     */
    @Test
    public void pureNumericName() throws Exception {
        try {

            IDepot nd = new Depot(
                    "12345",
                    server.getUserName(),
                    null,
                    "A depot of great importance",
                    DepotType.LOCAL,
                    null,
                    null,
                    NEW_DEPOT_NAME + "/..."
            );

            server.createDepot(nd);
            assertThat("We should never reach this", false);
        } catch (Throwable t) {
            assertThat("wrong message seen:" + t.getMessage(), t.getMessage(), containsString("Purely numeric name not allowed - '12345'."));
        }
    }

    /**
     * create depot with illegal name - Revision chars (@, #)
     * @throws Exception
     */
    @Test
    public void symbolName() throws Exception {
        try {
            IDepot nd = new Depot(
                    "@@@@",
                    server.getUserName(),
                    null,
                    "A depot of great importance",
                    DepotType.LOCAL,
                    null,
                    null,
                    NEW_DEPOT_NAME + "/..."
            );

            server.createDepot(nd);
            assertThat("We should never reach this", false);
        } catch (Throwable t) {
            assertThat("wrong message seen:" + t.getMessage(), t.getMessage(), containsString("Revision chars (@, #) not allowed in '@@@@'."));
        }
    }

    @AfterAll
    public static void afterClass() {
        helper.after(ts);
    }
}
