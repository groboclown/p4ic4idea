package com.perforce.p4java.tests.qa;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IDepot;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.impl.generic.core.Depot;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.test.TestServer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.List;

import static com.perforce.p4java.core.IDepot.DepotType.LOCAL;
import static com.perforce.p4java.core.IDepot.DepotType.REMOTE;
import static com.perforce.p4java.core.IDepot.DepotType.SPEC;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
@RunWith(JUnitPlatform.class)
public class GetDepotsTest {

    private static TestServer ts = null;
    private static Helper helper = null;
    private static IOptionsServer server = null;
    private static String depotName = "newDepot";

    // server setup, nothing fancy
    @BeforeAll
    public static void beforeClass() throws Throwable {
        helper = new Helper();
        ts = new TestServer();
        ts.getServerExecutableSpecification().setCodeline(helper.getServerVersion());
        ts.start();

        server = helper.getServer(ts);
        server.setUserName(ts.getUser());
        server.connect();

        IUser user = server.getUser(ts.getUser());

        IClient client = helper.createClient(server, "client1");
        server.setCurrentClient(client);

        File testFile = new File(client.getRoot(), "foo.txt");
        helper.addFile(server, user, client, testFile.getAbsolutePath(), "EditFilesTest", "text");

        IDepot nd = new Depot(
                depotName,
                server.getUserName(),
                null,
                "A depot of great importance",
                REMOTE,
                "1666",
                null,
                "//depot/..."
        );

        server.createDepot(nd);

        nd = new Depot(
                "spec",
                server.getUserName(),
                null,
                "A depot of great importance",
                SPEC,
                null,
                ".p4s",
                "spec/..."
        );

        server.createDepot(nd);
    }

    @DisplayName("attempt to delete a depot")
    @Test
    public void basicUsage() throws Throwable {
        List<IDepot> depots = server.getDepots();
        assertThat(depots, notNullValue());
        assertThat( depots.size(), is(3));
        for (IDepot d : depots) {
            if (d.getName().contentEquals("spec")) {
                assertThat( d.getDepotType(), is(SPEC));
                assertThat( d.getMap(), is("spec" + "/..."));
                assertThat( d.getDescription(), is("A depot of great importance"));
                assertThat(d.getAddress(), nullValue());
                assertThat( d.getOwnerName(), nullValue());
            }

            if (d.getName().contentEquals(depotName)) {
                assertThat( d.getDepotType(), is(REMOTE));
                assertThat( d.getMap(), is("//depot/..."));
                assertThat( d.getDescription(), is("A depot of great importance"));
                assertThat( d.getAddress(), is("1666"));
                assertThat( d.getOwnerName(), nullValue());
            }

            if (d.getName().contentEquals("depot")) {
                assertThat( d.getDepotType(), is(LOCAL));
                assertThat( d.getMap(), is("depot/..."));
                assertThat( d.getDescription(), is("Default depot"));
                assertThat(d.getAddress(), nullValue());
                assertThat( d.getOwnerName(), nullValue());
            }
        }
    }

    @AfterAll
    public static void afterClass() {
        helper.after(ts);
    }
}
	