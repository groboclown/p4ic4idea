package com.perforce.p4java.tests.qa;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.client.IClientSummary;
import com.perforce.p4java.option.server.GetClientsOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.test.TestServer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import java.util.List;

import static com.perforce.p4java.core.IDepot.DepotType.STREAM;
import static com.perforce.p4java.core.IStreamSummary.Type.MAINLINE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
@RunWith(JUnitPlatform.class)
public class GetClientsTest {

    private static TestServer ts = null;
    private static Helper helper = null;
    private static IOptionsServer server = null;

    @BeforeAll
    public static void beforeClass() throws Throwable {
        helper = new Helper();
        ts = new TestServer();
        ts.getServerExecutableSpecification().setCodeline(helper.getServerVersion());
        ts.start();

        server = helper.getServer(ts);
        server.setUserName(ts.getUser());
        server.connect();

        server.getUser(ts.getUser());

        IClient client = helper.createClient(server, "client1");
        server.setCurrentClient(client);

        client = helper.createClient(server, "CLIENT2");

        helper.createDepot(server, "Ace", STREAM, null, "ace/...");
        helper.createStream(server, "//Ace/main", MAINLINE, null);

        client = helper.createClient(server, "streamclient");
        client.setStream("//Ace/main");
        client.update();
    }


    @DisplayName("verify job046825: case-insensitive name matching")
    @Test
    public void caseInsensitiveListing() throws Throwable {
        GetClientsOptions opts = new GetClientsOptions();
        opts.setCaseInsensitiveNameFilter("client*");
        List<IClientSummary> clients = server.getClients(opts);

        // we should get two clients here
        assertThat("wrong number of clients",  clients.size(), is(2));
    }


    @DisplayName("verify case-sensitive name matching")
    @Test
    public void caseSensitiveListing() throws Throwable {
        GetClientsOptions opts = new GetClientsOptions();
        opts.setNameFilter("client*");
        List<IClientSummary> clients = server.getClients(opts);

        // we should get one label here
        if (server.isCaseSensitive()) {
            assertThat("wrong number of clients", clients.size(), is(1));
        } else {
            assertThat("wrong number of clients", clients.size(), is(2));
        }
    }


    @DisplayName("verify we still get clients; the -t flag is a no-op for p4java")
    @Test
    public void getTime() throws Throwable {
        GetClientsOptions opts = new GetClientsOptions();
        opts.setShowTime(false);
        List<IClientSummary> clients = server.getClients(opts);

        // we should get two clients
        assertThat("wrong number of clients",  clients.size(), is(3));
    }


    @DisplayName("verify we can get stream bound clients")
    @Test
    public void streamBoundClients() throws Throwable {
        GetClientsOptions opts = new GetClientsOptions();
        opts.setStream("//Ace/main");
        List<IClientSummary> clients = server.getClients(opts);

        // we should get one client
        assertThat("wrong number of clients",  clients.size(), is(1));
        assertThat("wrong client", clients.get(0).getName(), containsString("streamclient"));
    }

    @AfterAll
    public static void afterClass() {
        helper.after(ts);
    }
}
	
