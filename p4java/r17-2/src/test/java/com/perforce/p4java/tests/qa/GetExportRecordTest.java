package com.perforce.p4java.tests.qa;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.server.ExportRecordsOptions;
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
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.expectThrows;
@RunWith(JUnitPlatform.class)
public class GetExportRecordTest {

    private static TestServer ts = null;
    private static Helper helper = null;
    private static IOptionsServer server = null;
    private ExportRecordsOptions opts;

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
        helper.addFile(server, user, client, testFile.getAbsolutePath(), "GetExportRecordTest", "text");
    }

    @BeforeEach
    public void beforeEach() {
        opts = new ExportRecordsOptions();
        opts.setUseJournal(true);
        opts.setFilter("table=db.have");
    }

    @DisplayName("test for assertion when no checkpoint is found")
    @Test
    public void missingCheckpoint() throws Throwable {
        ExportRecordsOptions opts = new ExportRecordsOptions();
        expectThrows(P4JavaException.class, () -> server.getExportRecords(opts));
    }

    @DisplayName("test for assertion when no checkpoint is found")
    @Test
    public void unspecifiedCheckpoint() throws Throwable {
        expectThrows(P4JavaException.class, () -> server.getExportRecords(null));
    }

    @DisplayName("verify job037798: skip data conversion")
    @Test
    public void rawRecords() throws Throwable {
        List<Map<String, Object>> exportList = server.getExportRecords(opts);
        assertThat(exportList, notNullValue());
        assertThat("incorrect file", (String) exportList.get(0).get("HAdfile"), containsString("//depot/foo.txt"));

        opts.setSkipDataConversion(true);
        exportList = server.getExportRecords(opts);
        assertThat(exportList, notNullValue());
        Object data = exportList.get(0).get("HAdfile");
        assertThat(data, instanceOf(byte[].class));
    }

    @DisplayName("verify job037798: skip data conversion")
    @Test
    public void rawRecordsRegex() throws Throwable {
        opts.setSkipFieldPattern("^HAdfile");
        opts.setSkipDataConversion(true);

        List<Map<String, Object>> exportList = server.getExportRecords(opts);
        assertThat(exportList, notNullValue());
        Object data = exportList.get(0).get("HAdfile");
        assertThat(data, instanceOf(byte[].class));
        //assertThat("incorrect file", (String)exportList.get(0).get("HAdfile"), containsString("//depot/foo.txt"));
    }

    @DisplayName("verify job037798: skip data conversion")
    @Test
    public void rawRecordsWithStartStop() throws Throwable {
        opts.setSkipStartField("HAcfile");
        opts.setSkipStopField("HArev");
        opts.setSkipDataConversion(true);

        List<Map<String, Object>> exportList = server.getExportRecords(opts);
        assertThat(exportList, notNullValue());
        Object data = exportList.get(0).get("HAdfile");
        assertThat(data, instanceOf(byte[].class));
        data = exportList.get(0).get("HAcfile");
        assertThat(data, instanceOf(byte[].class));
    }

    @AfterAll
    public static void afterClass() {
        helper.after(ts);
    }
}