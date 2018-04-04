package com.perforce.p4java.tests.qa;


import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.option.server.DescribeOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.test.TestServer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.List;

import static com.perforce.p4java.core.file.DiffType.SUMMARY_DIFF;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

@RunWith(JUnitPlatform.class)
public class GetChangelistDiffsStreamTest {

    private static TestServer ts = null;
    private static Helper helper = null;
    private static IOptionsServer server = null;
    private static IUser user = null;
    private static IClient client = null;
    private static IChangelist pendingChangelist = null;

    @BeforeAll
    public static void beforeClass() throws Throwable {
        helper = new Helper();
        ts = new TestServer();
        ts.getServerExecutableSpecification().setCodeline(helper.getServerVersion());
        ts.start();

        server = helper.getServer(ts);
        server.setUserName(ts.getUser());
        server.connect();

        user = server.getUser(ts.getUser());

        client = helper.createClient(server, "client1");
        server.setCurrentClient(client);

        File testFile = new File(client.getRoot(), "foo.txt");

        helper.addFile(server, user, client, testFile.getAbsolutePath(), "GetChangelistDiffs", "text");

        pendingChangelist = helper.createChangelist(server, user, client);

        helper.editFile(testFile.getAbsolutePath(), "GetChangelistDiffs\nLine2", pendingChangelist, client);

        List<IFileSpec> shelvedFiles = client.shelveChangelist(pendingChangelist);
        helper.validateFileSpecs(shelvedFiles);
    }


    @Test
    public void newTypeSummaryOutputShelvedDiffsTrue() throws Throwable {
        DescribeOptions describeOptions = new DescribeOptions();
        describeOptions.setType(SUMMARY_DIFF);
        describeOptions.setOutputShelvedDiffs(true);

        InputStream diffs = server.getChangelistDiffsStream(pendingChangelist.getId(), describeOptions);
        BufferedReader reader = new BufferedReader(new InputStreamReader(diffs));
        StringBuilder output = new StringBuilder("");
        String line;
        while ((line = reader.readLine()) != null) {

            if (line.equals("")) {
                System.out.println("---------------------" + output.toString());
            }
            output.append(line).append("\n");
        }

        String pendingChangelistDate = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(pendingChangelist.getDate());
        String validOutput = "Change 2 by " + user.getLoginName() + "@" + client.getName() + " on " + pendingChangelistDate + " *pending*\n" +
                "\n" +
                "\tChangelist for user " + user.getLoginName() + " and client " + client.getName() + ".\n" +
                "\n" +
                "Shelved files ...\n" +
                "\n" +
                "... //depot/foo.txt#1 edit\n" +
                "\n\n" +
                "Differences ...\n" +
                "\n" +
                "==== //depot/foo.txt#1 (text) ====\n" +
                "\n" +
                "add 0 chunks 0 lines\n" +
                "deleted 0 chunks 0 lines\n" +
                "changed 1 chunks 1 / 2 lines\n";
        assertThat(output.toString(), is(validOutput));
    }

    @AfterAll
    public static void afterClass() {
        helper.after(ts);
    }

}