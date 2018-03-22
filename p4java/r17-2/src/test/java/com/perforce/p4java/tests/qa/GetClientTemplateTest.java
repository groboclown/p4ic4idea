package com.perforce.p4java.tests.qa;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.client.IClientViewMapping;
import com.perforce.p4java.option.server.GetClientTemplateOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.test.TestServer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import static com.perforce.p4java.core.IDepot.DepotType.STREAM;
import static com.perforce.p4java.core.IStreamSummary.Type.MAINLINE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.StringContains.containsString;

@RunWith(JUnitPlatform.class)
public class GetClientTemplateTest {

    private static TestServer ts = null;
    private static Helper helper = null;
    private static IOptionsServer server = null;

    // a file
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


    @DisplayName("verify the stream option")
    @Test
    public void streamBasedTemplate() throws Throwable {
        GetClientTemplateOptions opts = new GetClientTemplateOptions();
        opts.setStream("//Ace/main");
        IClient client = server.getClientTemplate("tmp", opts);

        // we should get one label here
        IClientViewMapping clientViewMapping = client.getClientView().getEntry(0);
        assertThat("incorrect depot map", clientViewMapping.getLeft(), containsString("//Ace/main/..."));
        assertThat("incorrect client map", clientViewMapping.getRight(), containsString("//tmp/..."));
    }


    @DisplayName("just make sure the darn thing works")
    @Test
    public void simple() throws Throwable {
        IClient client2 = server.getClientTemplate("client3");
        assertThat(client2.getName(), is("client3"));
    }

    @DisplayName("just make sure the darn thing works")
    @Test
    public void existingClient() throws Throwable {
        IClient client2 = server.getClientTemplate("client1");
        assertThat(client2, nullValue());
    }

    @AfterAll
    public static void afterClass() {
        helper.after(ts);
    }
}

