package com.perforce.p4java.tests.qa;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.ILabel;
import com.perforce.p4java.core.ILabelSummary;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.test.TestServer;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static com.perforce.p4java.common.base.StringHelper.format;
import static com.perforce.p4java.core.file.FileSpecBuilder.makeFileSpecList;
import static com.perforce.p4java.impl.generic.core.InputMapper.map;
import static com.perforce.p4java.impl.generic.core.Label.newLabel;
import static com.perforce.p4java.impl.mapbased.rpc.RpcPropertyDefs.RPC_RELAX_CMD_NAME_CHECKS_NICK;
import static com.perforce.p4java.impl.mapbased.rpc.func.helper.MapUnmapper.unmapLabelMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.StringContains.containsString;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.Assert.fail;



public class ExecMapCmdTest {

    private static TestServer ts = null;
    private static Helper helper = null;
    private static IOptionsServer server = null;
    private static IOptionsServer server2 = null;
    private static IUser user = null;
    private static IClient client = null;
    private static File testFile = null;

    /**
     * simple setup with one file
     * @throws Throwable
     */
    @BeforeClass
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

        user = server.getUser(ts.getUser());

        client = helper.createClient(server, "client1");
        server.setCurrentClient(client);
        server2.setCurrentClient(client);

        testFile = new File(client.getRoot(), "foo.txt");
        helper.addFile(server, user, client, testFile.getAbsolutePath(), "ExeMapCmdTest", "text");
    }

    @Before
    public void reset() {
        try {
            List<IFileSpec> fileSpec = makeFileSpecList(testFile.getAbsolutePath());
            client.revertFiles(fileSpec, null);

            List<ILabelSummary> labels = server.getLabels(null, null);
            for (ILabelSummary l : labels) {
                server.deleteLabel(l.getName(), false);
            }

            server2.setUserName(user.getLoginName());
        } catch (Throwable ignore) {
        }
    }

    /**
     * try something that would normally accept
     * @throws Throwable
     */
    @SuppressWarnings("deprecation")
    @Test
    public void acceptableCommand() throws Throwable {
        Map<String, Object>[] results = server2.execMapCmd("edit", new String[]{"//depot/foo.txt"}, null);
        assertThat(results, notNullValue());
        assertThat("wrong size in results array", results.length, is(1));
        String action = (String) results[0].get("action");
        String depotFile = (String) results[0].get("depotFile");

        String msg = server2.getErrorOrInfoStr(results[0]);
        assertThat("bad message", msg, nullValue());

        msg = server2.getInfoStr(results[0]);
        assertThat("bad message", msg, nullValue());

        msg = server2.getErrorStr(results[0]);
        assertThat("bad message", msg, nullValue());


        assertThat("did not see expected action", action, startsWith("edit"));
        assertThat("did not see expected depot path", depotFile, startsWith("//depot/foo.txt"));
    }


    /**
     * try something that would normally accept
     * @throws Throwable
     */
    @SuppressWarnings("deprecation")
    @Test
    public void badCommand() throws Throwable {
        Map<String, Object>[] results = server2.execMapCmd("edit", new String[]{"//depot/bar.txt"}, null);
        assertThat(results, notNullValue());
        assertThat("wrong size in results array", results.length, is(1));

        String msg = server2.getErrorOrInfoStr(results[0]);
        assertThat("bad message", msg, containsString("//depot/bar.txt - file(s) not on client."));

        msg = server2.getInfoStr(results[0]);
        assertThat("bad message", msg, nullValue());

        msg = server2.getErrorStr(results[0]);
        assertThat("bad message", msg, nullValue());
    }


    /**
     * try to trigger an error
     * @throws Throwable
     */
    @Test
    public void nonexistentUser() throws Throwable {
        server2.setUserName("otherUser");
        badCommand();
    }

    /**
     * try something not accepted by P4Java normally; this should fail
     * @throws Throwable
     */
    @Test
    public void unacceptableCommandControl() throws Throwable {
        String cmdName = "get";
        try {
            server.execMapCmd(cmdName, new String[]{"//depot/foo.txt"}, null);
            fail("did not receive expected exception");
        } catch (P4JavaException e) {
            assertThat(e.getLocalizedMessage(), containsString(format("command name '%s' unimplemented or unrecognized by p4java", cmdName)));
        }
    }

    /**
     * try something not accepted by P4Java normally; this should work
     * @throws Throwable
     */
    @SuppressWarnings("deprecation")
    @Test
    public void unacceptableCommand() throws Throwable {
        Map<String, Object>[] results = server2.execMapCmd("get", new String[]{"-f", "//depot/foo.txt"}, null);
        assertThat(results, notNullValue());
        assertThat("wrong size in results array", results.length, is(1));
        String action = (String) results[0].get("action");
        String depotFile = (String) results[0].get("depotFile");

        assertThat("expected an action string", action, notNullValue());
        assertThat("expected a depotFile string", depotFile, notNullValue());

        String msg = server2.getErrorOrInfoStr(results[0]);
        assertThat("bad message", msg, nullValue());

        msg = server2.getInfoStr(results[0]);
        assertThat("bad message", msg, nullValue());

        msg = server2.getErrorStr(results[0]);
        assertThat("bad message", msg, nullValue());


        assertThat("did not see expected action", action, startsWith("refreshed"));
        assertThat("did not see expected depot path", depotFile, startsWith("//depot/foo.txt"));
    }

    @Test
    public void inputString() throws Throwable {
        ILabel label = newLabel(server, "label1", "inputString", new String[]{"//depot/foo...", "//depot/bar..."});
        StringBuffer strBuf = new StringBuffer();
        Map<String, Object> inMap = map(label);
        unmapLabelMap(inMap, strBuf);

        Map<String, Object>[] retMaps = server.execInputStringMapCmd("label", new String[]{"-i"}, strBuf.toString());
        assertThat("null returned from execInputStringMapCmd", retMaps, notNullValue());

        List<ILabelSummary> labels = server.getLabels(null, null);

        assertThat("wrong number of labels", labels.size(), is(1));
        assertThat("incorrect label", labels.get(0).getName(), is("label1"));
    }

    @Test
    public void inputStringWithBadCommand() throws Throwable {
        ILabel label = newLabel(server, "label1", "inputString", new String[]{"//depot/foo...", "//depot/bar..."});
        StringBuffer strBuf = new StringBuffer();
        Map<String, Object> inMap = map(label);
        unmapLabelMap(inMap, strBuf);

        String cmdName = "get";
        try {
            server.execInputStringMapCmd(cmdName, new String[]{"-i"}, strBuf.toString());
            fail("should not get here");
        } catch (P4JavaException e) {
            assertThat(e.getLocalizedMessage(), containsString(format("command name '%s' unimplemented or unrecognized by p4java", cmdName)));
        }
    }

    // ping does not work as of 11.1 because it can't handle some of the
    // RPC messages sent. This will probably never work; I'm just documenting it for
    // future generations
    @Ignore
    public void ping() {
    }

    @AfterClass
    public static void afterClass() {
        helper.after(ts);
    }
}