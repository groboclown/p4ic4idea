package com.perforce.p4java.tests.qa;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.impl.generic.client.ClientView;
import com.perforce.p4java.impl.generic.client.ClientView.ClientViewMapping;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.test.TestServer;


public class ClientTest {
    private static TestServer ts = null;
    private static Helper helper = null;
    private static IClient client = null;

    @Before
    public void before() throws Throwable {
        helper = new Helper();
        ts = new TestServer();
        ts.getServerExecutableSpecification().setCodeline("main");
        ts.startAsync();

        IOptionsServer server = helper.getServer(ts);
        server.setUserName(ts.getUser());
        server.connect();

        client = helper.createClient(server, "client1");
        server.setCurrentClient(client);
    }

    @Test
    public void nullRoot() throws Exception {
        String oldRoot = client.getRoot();
        client.setRoot("null");
        String depotViewMap = "//depot/...";
        String clientViewMap = "//" + client.getName();
        if (!oldRoot.startsWith("/")) {
            clientViewMap += "/";
        }
        clientViewMap += oldRoot + "/...";
        if (clientViewMap.contains(" ")) {
            clientViewMap = "\"" + clientViewMap + "\"";
        }
        String mapping = depotViewMap + " " + clientViewMap;
        ClientView clientView = new ClientView();
        ClientViewMapping clientViewMapping = new ClientViewMapping(0, mapping);
        clientView.addEntry(clientViewMapping);
        client.setClientView(clientView);
        client.update();
        client.refresh();
        assertThat("Client root is not null.", client.getRoot(), is("null"));
        assertThat(
                "Client view does not contain local path.",
                client.getClientView().getEntry(0).getRight(),
                containsString(oldRoot));
    }

    @Test
    public void wildcardsInName() throws Exception {
        String clientName = "`1234567890-=\\][poiuytrewqasdfghjkl;.mnbvcxz~!$%^&()_+|}{POIUYTREWQASDFGHJKL:?><MNBVCXZ";
        client.setName(clientName);
        String mapping = "//depot/... //" + clientName + "/...";
        ClientView clientView = new ClientView();
        ClientViewMapping clientViewMapping = new ClientViewMapping(0, mapping);
        clientView.addEntry(clientViewMapping);
        client.setClientView(clientView);
        client.update();
        client.refresh();
        assertThat(
                "Client view does not contain client name.",
                client.getClientView().getEntry(0).getRight(),
                containsString(clientName));

    }

    @After
    public void after() {
        helper.after(ts);
    }
}
