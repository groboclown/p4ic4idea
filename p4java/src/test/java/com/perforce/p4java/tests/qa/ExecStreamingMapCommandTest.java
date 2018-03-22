package com.perforce.p4java.tests.qa;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.ILabelSummary;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.callback.IStreamingCallback;
import com.perforce.test.TestServer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.perforce.p4java.core.file.FileSpecBuilder.makeFileSpecList;
import static com.perforce.p4java.impl.mapbased.rpc.RpcPropertyDefs.RPC_RELAX_CMD_NAME_CHECKS_NICK;
import static com.perforce.p4java.server.CmdSpec.EXPORT;
import static com.perforce.p4java.tests.qa.Helper.FILE_SEP;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.StringContains.containsString;
import static org.hamcrest.core.StringStartsWith.startsWith;

@RunWith(JUnitPlatform.class)
public class ExecStreamingMapCommandTest {
    private class StreamingCallback implements IStreamingCallback {

        private ArrayList<Map<String, Object>> results = newArrayList();

        public boolean startResults(int key) throws P4JavaException {
            // TODO Auto-generated method stub
            return false;
        }

        public boolean endResults(int key) throws P4JavaException {
            synchronized (this) {
                notifyAll();
            }
            return false;
        }

        public boolean handleResult(Map<String, Object> resultMap, int key) throws P4JavaException {
            results.add(resultMap);
            return false;
        }

        public ArrayList<Map<String, Object>> getResults() {
            return results;
        }

        public Map<String, Object> getResult() {
            if (results.size() > 0) {
                return results.get(0);
            } else {
                return null;
            }
        }
    }

    private static TestServer ts = null;
    private static Helper helper = null;
    private static IOptionsServer server = null;
    private static IOptionsServer server2 = null;
    private static IClient client = null;
    private static File testFile = null;

    @BeforeAll
    public static void beforeClass() throws Throwable {
        helper = new Helper();
        ts = new TestServer();
        ts.getServerExecutableSpecification().setCodeline(helper.getServerVersion());
        ts.start();

        server = helper.getServer(ts);
        server.setUserName(ts.getUser());
        server.connect();

        Properties props = new Properties();
        props.put(RPC_RELAX_CMD_NAME_CHECKS_NICK, "true");
        server2 = helper.getServer(ts, props);
        server2.setUserName(ts.getUser());
        server2.connect();

        IUser user = server.getUser(ts.getUser());

        client = helper.createClient(server, "client1");
        server.setCurrentClient(client);
        server2.setCurrentClient(client);

        testFile = new File(client.getRoot(), "foo.txt");
        helper.addFile(server, user, client, testFile.getAbsolutePath(), "ExeMapCmdTest", "text");

        File dir = new File(client.getRoot(), "dir");
        dir.mkdirs();
        testFile = new File(client.getRoot(), "dir" + FILE_SEP + "foo.txt");
        helper.addFile(server, user, client, testFile.getAbsolutePath(), "ExeMapCmdTest", "text");
    }

    @BeforeEach
    public void reset() {
        try {
            List<IFileSpec> fileSpec = makeFileSpecList(testFile.getAbsolutePath());
            client.revertFiles(fileSpec, null);

            List<ILabelSummary> labels = server.getLabels(null, null);
            for (ILabelSummary l : labels) {
                server.deleteLabel(l.getName(), false);
            }
        } catch (Throwable ignore) {
            ignore.printStackTrace();
        }
    }


    @DisplayName("try something that would normally accept")
    @Test
    public void acceptableCommand() throws Throwable {
        StreamingCallback cb = new StreamingCallback();
        server2.execStreamingMapCommand("edit", new String[]{"//depot/foo.txt"}, null, cb, 0);

        // wait for results
        synchronized (cb) {
            cb.wait(2000);
        }

        Map<String, Object> results = cb.getResult();
        assertThat(results, notNullValue());
        String action = (String) results.get("action");
        String depotFile = (String) results.get("depotFile");

        assertThat("did not see expected action", action, startsWith("edit"));
        assertThat("did not see expected depot path", depotFile, startsWith("//depot/foo.txt"));
    }


    @DisplayName("verify that FileSpec can parse dirs output")
    @Test
    public void parseDirs() throws Throwable {
        StreamingCallback cb = new StreamingCallback();
        server2.execStreamingMapCommand("dirs", new String[]{"//depot/*"}, null, cb, 0);

        // wait for results
        synchronized (cb) {
            cb.wait(2000);
        }

        Map<String, Object> results = cb.getResult();
        assertThat(results, notNullValue());
        IFileSpec file = new FileSpec(results, server, -1);
        assertThat("bad path", file.getDepotPathString(), containsString("dir"));
    }

    @DisplayName("verify that FileSpec can parse errors with \"dirs\" output")
    @Test
    public void badArgumentParseDirs() throws Throwable {
        StreamingCallback cb = new StreamingCallback();
        server2.execStreamingMapCommand("dirs", new String[]{"//depot/baz/*"}, null, cb, 0);

        // wait for results
        synchronized (cb) {
            cb.wait(2000);
        }

        Map<String, Object> results = cb.getResult();
        assertThat(results, nullValue());
    }

    @DisplayName("verify that FileSpec can parse errors with \"files\" output")
    @Test
    public void badArgumentParseFiles() throws Throwable {
        StreamingCallback cb = new StreamingCallback();
        server2.execStreamingMapCommand("files", new String[]{"//depot/baz/..."}, null, cb, 0);

        // wait for results
        synchronized (cb) {
            cb.wait(2000);
        }

        Map<String, Object> results = cb.getResult();
        assertThat(results, notNullValue());
    }

    @DisplayName("verify we cna turn off string translation")
    @Test
    public void exportWithoutTranslation() throws Throwable {
        HashMap<String, Object> inMap = newHashMap();
        Map<String, Object> skipParams = newHashMap();
        skipParams.put("fieldPattern", "^HAdfile");
        inMap.put(EXPORT.toString(), skipParams);

        StreamingCallback cb = new StreamingCallback();
        server2.execStreamingMapCommand("export", new String[]{"-j0", "-Ftable=db.have"}, inMap, cb, 0);

        // wait for results
        synchronized (cb) {
            cb.wait(2000);
        }

        Map<String, Object> results = cb.getResult();
        assertThat(results, notNullValue());

        Object data = results.get("HAdfile");
        assertThat(data, instanceOf(byte[].class));
    }


    @DisplayName("verify we get translation without an inmap")
    @Test
    public void exportWithTranslation() throws Throwable {
        StreamingCallback cb = new StreamingCallback();
        server2.execStreamingMapCommand("export", new String[]{"-j0", "-Ftable=db.have"}, null, cb, 0);

        // wait for results
        synchronized (cb) {
            cb.wait(2000);
        }

        Map<String, Object> results = cb.getResult();
        assertThat(results, notNullValue());

        Object data = results.get("HAdfile");
        assertThat(data, instanceOf(String.class));
    }

    @AfterAll
    public static void afterClass() {
        helper.after(ts);
    }
}