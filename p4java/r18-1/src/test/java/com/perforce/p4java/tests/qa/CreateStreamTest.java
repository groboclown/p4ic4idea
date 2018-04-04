package com.perforce.p4java.tests.qa;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IDepot.DepotType;
import com.perforce.p4java.core.IStream;
import com.perforce.p4java.core.IStreamIgnoredMapping;
import com.perforce.p4java.core.IStreamRemappedMapping;
import com.perforce.p4java.core.IStreamSummary.IOptions;
import com.perforce.p4java.core.IStreamSummary.Type;
import com.perforce.p4java.core.IStreamViewMapping;
import com.perforce.p4java.core.IStreamViewMapping.PathType;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.core.ViewMap;
import com.perforce.p4java.impl.generic.core.Stream;
import com.perforce.p4java.impl.generic.core.Stream.StreamIgnoredMapping;
import com.perforce.p4java.impl.generic.core.Stream.StreamRemappedMapping;
import com.perforce.p4java.impl.generic.core.Stream.StreamViewMapping;
import com.perforce.p4java.impl.generic.core.StreamSummary;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.test.TestServer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import java.io.File;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;

@RunWith(JUnitPlatform.class)
public class CreateStreamTest {

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

        IUser user = server.getUser(ts.getUser());

        IClient client = helper.createClient(server, "client1");
        server.setCurrentClient(client);

        helper.createDepot(server, "Ace", DepotType.STREAM, null, "ace/...");
        helper.createStream(server, "//Ace/main", Type.MAINLINE, null);

        client.setStream("//Ace/main");
        client.update();

        File testFile = new File(client.getRoot(), "foo.txt");
        helper.addFile(server, user, client, testFile.getAbsolutePath(), "GetStreamIntegrationStatusTest", "text");
    }

    @DisplayName("just make sure the darn thing works")
    @Test
    public void simple() throws Exception {
        ViewMap<IStreamRemappedMapping> remappedView = new ViewMap<>();
        StreamRemappedMapping entry = new StreamRemappedMapping();
        entry.setLeftRemapPath("y/*");
        entry.setRightRemapPath("y/z/*");
        entry.setOrder(0);
        remappedView.addEntry(entry);

        ViewMap<IStreamIgnoredMapping> ignoredView = new ViewMap<>();
        StreamIgnoredMapping iEntry = new StreamIgnoredMapping();
        iEntry.setIgnorePath(".p4config");
        iEntry.setOrder(0);
        ignoredView.addEntry(iEntry);

        ViewMap<IStreamViewMapping> view = new ViewMap<>();
        StreamViewMapping sEntry = new StreamViewMapping();
        sEntry.setPathType(PathType.SHARE);
        sEntry.setViewPath("...");
        sEntry.setOrder(0);
        view.addEntry(sEntry);

        IOptions opts = new StreamSummary.Options(false, false, false, false);

        IStream stream = new Stream();
        stream.setDescription("A simple stream");
        stream.setName("Simple dev stream");
        stream.setParent("//Ace/main");
        stream.setStream("//Ace/simple");
        stream.setOwnerName(ts.getUser());
        stream.setType(Type.DEVELOPMENT);
        stream.setRemappedView(remappedView);
        stream.setIgnoredView(ignoredView);
        stream.setStreamView(view);
        stream.setOptions(opts);
        server.createStream(stream);

        stream = server.getStream("//Ace/simple");
        assertThat("wrong name", stream.getDescription(), containsString("A simple stream"));

        opts = stream.getOptions();
        assertThat("improper flow config", opts.isNoFromParent(), is(false));
        assertThat("improper flow config", opts.isNoToParent(), is(false));
    }

    @AfterAll
    public static void afterClass() {
        helper.after(ts);
    }
}
