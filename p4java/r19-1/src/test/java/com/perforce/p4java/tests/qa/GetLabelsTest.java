package com.perforce.p4java.tests.qa;

import static com.perforce.p4java.core.file.FileSpecBuilder.makeFileSpecList;
import static com.perforce.p4java.tests.qa.Helper.FILE_SEP;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.ILabel;
import com.perforce.p4java.core.ILabelSummary;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.option.server.GetLabelsOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.test.CaseSensitiveTestServer;
import com.perforce.test.TestServer;


public class GetLabelsTest {

    private static TestServer ts = null;
    private static Helper h = null;
    private static IOptionsServer server = null;
    private static IUser user = null;
    private static IClient client = null;
    private static File testFile = null;
    private static ILabel label = null;

    // a file
    @BeforeClass
    public static void beforeClass() throws Throwable {
        h = new Helper();
        ts = new CaseSensitiveTestServer();
        ts.getServerExecutableSpecification().setCodeline(h.getServerVersion());
        ts.start();

        server = h.getServer(ts);
        server.setUserName(ts.getUser());
        server.connect();

        user = server.getUser(ts.getUser());

        client = h.createClient(server, "client1");
        server.setCurrentClient(client);

        testFile = new File(client.getRoot() + FILE_SEP + "foo.txt");
        h.addFile(server, user, client, testFile.getAbsolutePath(), "ProxytTest", "text");

        label = h.addLabel(server, user, "label1", "//depot/...");
        client.labelSync(makeFileSpecList(testFile.getAbsolutePath()), label.getName(), null);

        label = h.addLabel(server, user, "LABEL2", "//depot/...");
        client.labelSync(makeFileSpecList(testFile.getAbsolutePath()), label.getName(), null);
    }


    // verify job046825: case-insensitive name matching
    @Test
    public void caseInsensitiveListing() throws Throwable {
        GetLabelsOptions opts = new GetLabelsOptions();
        opts.setCaseInsensitiveNameFilter("label*");
        List<ILabelSummary> labels = server.getLabels(null, opts);

        // we should get two labels here
        assertEquals("wrong number of labels", 2, labels.size());
    }


    // verify case-sensitive name matching
    @Test
    public void caseSensitiveListing() throws Throwable {
        GetLabelsOptions opts = new GetLabelsOptions();
        opts.setNameFilter("label*");
        List<ILabelSummary> labels = server.getLabels(null, opts);

        // we should get one label here
        assertEquals("wrong number of labels", 1, labels.size());
    }


    // verify the -t flag doesn't break anything
    @Test
    public void showTime() throws Throwable {
        GetLabelsOptions opts = new GetLabelsOptions();
        opts.setShowTime(true);
        List<ILabelSummary> labels = server.getLabels(null, opts);

        // we should get two labels
        assertEquals("wrong number of labels", 2, labels.size());
    }


    @AfterClass
    public static void afterClass() {
        h.after(ts);
    }
}
	
