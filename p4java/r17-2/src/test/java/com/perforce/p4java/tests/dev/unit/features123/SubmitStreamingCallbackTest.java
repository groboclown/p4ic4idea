/**
 * Copyright (c) 2012 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features123;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.ChangelistStatus;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.impl.mapbased.server.cmd.ResultListBuilder;
import com.perforce.p4java.option.changelist.SubmitOptions;
import com.perforce.p4java.option.client.AddFilesOptions;
import com.perforce.p4java.option.client.DeleteFilesOptions;
import com.perforce.p4java.option.client.RevertFilesOptions;
import com.perforce.p4java.option.client.SyncOptions;
import com.perforce.p4java.option.server.LoginOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.ServerFactory;
import com.perforce.p4java.server.callback.ICommandCallback;
import com.perforce.p4java.server.callback.IStreamingCallback;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test submit of a changelist using IStreamingCallback.
 */
@Jobs({"job057603"})
@TestId("Dev123_SubmitStreamingCallbackTest")
public class SubmitStreamingCallbackTest extends P4JavaTestCase {

    IOptionsServer server = null;
    IClient client = null;
    IChangelist changelist = null;
    List<IFileSpec> files = null;
    String serverMessage = null;
    long completedTime = 0;

    public static class SimpleCallbackHandler implements IStreamingCallback {
        int expectedKey = 0;
        SubmitStreamingCallbackTest testCase = null;

        public SimpleCallbackHandler(SubmitStreamingCallbackTest testCase,
                                     int key) {
            if (testCase == null) {
                throw new NullPointerException(
                        "null testCase passed to CallbackHandler constructor");
            }
            this.expectedKey = key;
            this.testCase = testCase;
        }

        public boolean startResults(int key) throws P4JavaException {
            if (key != this.expectedKey) {
                fail("key mismatch; expected: " + this.expectedKey
                        + "; observed: " + key);
            }
            return true;
        }

        public boolean endResults(int key) throws P4JavaException {
            if (key != this.expectedKey) {
                fail("key mismatch; expected: " + this.expectedKey
                        + "; observed: " + key);
            }
            return true;
        }

        public boolean handleResult(Map<String, Object> resultMap, int key)
                throws P4JavaException {
            if (key != this.expectedKey) {
                fail("key mismatch; expected: " + this.expectedKey
                        + "; observed: " + key);
            }
            if (resultMap == null) {
                fail("null result map in handleResult");
            }
            return true;
        }
    }

    ;

    public static class ListCallbackHandler implements IStreamingCallback {

        int expectedKey = 0;
        SubmitStreamingCallbackTest testCase = null;
        List<Map<String, Object>> resultsList = null;

        public ListCallbackHandler(SubmitStreamingCallbackTest testCase,
                                   int key, List<Map<String, Object>> resultsList) {
            this.expectedKey = key;
            this.testCase = testCase;
            this.resultsList = resultsList;
        }

        public boolean startResults(int key) throws P4JavaException {
            if (key != this.expectedKey) {
                fail("key mismatch; expected: " + this.expectedKey
                        + "; observed: " + key);
            }
            return true;
        }

        public boolean endResults(int key) throws P4JavaException {
            if (key != this.expectedKey) {
                fail("key mismatch; expected: " + this.expectedKey
                        + "; observed: " + key);
            }
            return true;
        }

        public boolean handleResult(Map<String, Object> resultMap, int key)
                throws P4JavaException {
            if (key != this.expectedKey) {
                fail("key mismatch; expected: " + this.expectedKey
                        + "; observed: " + key);
            }
            if (resultMap == null) {
                fail("null resultMap passed to handleResult callback");
            }
            this.resultsList.add(resultMap);
            return true;
        }

        public List<Map<String, Object>> getResultsList() {
            return this.resultsList;
        }
    }

    ;

    /**
     * @BeforeClass annotation to a method to be run before all the tests in a
     * class.
     */
    @BeforeClass
    public static void oneTimeSetUp() {
        // one-time initialization code (before all the tests).
    }

    /**
     * @AfterClass annotation to a method to be run after all the tests in a
     * class.
     */
    @AfterClass
    public static void oneTimeTearDown() {
        // one-time cleanup code (after all the tests).
    }

    /**
     * @Before annotation to a method to be run before each test in a class.
     */
    @Before
    public void setUp() {
        // initialization code (before each test).
        try {
            Properties props = new Properties();

            props.put("enableProgress", "true");

            server = ServerFactory
                    .getOptionsServer(this.serverUrlString, props);
            assertNotNull(server);

            // Register callback
            server.registerCallback(new ICommandCallback() {
                public void receivedServerMessage(int key, int genericCode,
                                                  int severityCode, String message) {
                    serverMessage = message;
                }

                public void receivedServerInfoLine(int key, String infoLine) {
                    serverMessage = infoLine;
                }

                public void receivedServerErrorLine(int key, String errorLine) {
                    serverMessage = errorLine;
                }

                public void issuingServerCommand(int key, String command) {
                    serverMessage = command;
                }

                public void completedServerCommand(int key, long millisecsTaken) {
                    completedTime = millisecsTaken;
                }
            });
            // Connect to the server.
            server.connect();
            if (server.isConnected()) {
                if (server.supportsUnicode()) {
                    server.setCharsetName("utf8");
                }
            }

            // Set the server user
            server.setUserName(this.userName);

            // Login using the normal method
            server.login(this.password, new LoginOptions());

            client = getDefaultClient(server);
            assertNotNull(client);
            server.setCurrentClient(client);
        } catch (P4JavaException e) {
            fail("Unexpected exception: " + e.getLocalizedMessage());
        } catch (URISyntaxException e) {
            fail("Unexpected exception: " + e.getLocalizedMessage());
        }
    }

    /**
     * @After annotation to a method to be run after each test in a class.
     */
    @After
    public void tearDown() {
        // cleanup code (after each test).
        if (server != null) {
            this.endServerSession(server);
        }
    }

    /**
     * Test submit changelist using IStreamingCallback
     */
    @Test
    public void testSubmitChangelist() {
        int randNum = getRandomInt();
        String depotFile = null;

        try {

            String path = "/112Dev/GetOpenedFilesTest/src/com/perforce/p4cmd/";
            String name = "P4JCommandCallbackImpl";
            String ext = ".java";
            String file = client.getRoot() + path + name + ext;
            String file2 = client.getRoot() + path + name + "-" + randNum + ext;
            depotFile = "//depot" + path + name + "-" + randNum + ext;

            List<IFileSpec> files = client.sync(
                    FileSpecBuilder.makeFileSpecList(file),
                    new SyncOptions().setForceUpdate(true));
            assertNotNull(files);

            // Copy a file to be used for add
            copyFile(file, file2);

            changelist = getNewChangelist(server, client,
                    "SubmitStreamingCallbackTest add files");
            assertNotNull(changelist);
            changelist = client.createChangelist(changelist);
            assertNotNull(changelist);

            // Add a file specified as "binary" even though it is "text"
            files = client.addFiles(FileSpecBuilder.makeFileSpecList(file2),
                    new AddFilesOptions().setChangelistId(changelist.getId())
                            .setFileType("binary"));

            assertNotNull(files);
            changelist.refresh();

            List<Map<String, Object>> resultsList = new ArrayList<Map<String, Object>>();
            int key = this.getRandomInt();
            ListCallbackHandler handler = new ListCallbackHandler(this, key,
                    resultsList);

            changelist.submit(new SubmitOptions(), handler, key);

            assertNotNull(resultsList);
            assertTrue(resultsList.size() > 0);

            List<IFileSpec> fileList = new ArrayList<IFileSpec>();

            for (Map<String, Object> resultmap : resultsList) {
                if (resultmap != null) {
                    if (resultmap.get("submittedChange") != null) {
                        int id = new Integer((String) resultmap.get("submittedChange"));
                        ChangelistStatus status = ChangelistStatus.SUBMITTED;
                        fileList.add(new FileSpec(FileSpecOpStatus.INFO,
                                "Submitted as change " + id));
                    } else if (resultmap.get("locked") != null) {
                        // disregard this message for now
                    } else {
                        fileList.add(ResultListBuilder.handleFileReturn(resultmap, server));
                    }
                }
            }

            assertNotNull(fileList);
            assertTrue(fileList.size() > 0);

        } catch (P4JavaException e) {
            fail("Unexpected exception: " + e.getLocalizedMessage());
        } finally {
            if (client != null) {
                if (changelist != null) {
                    if (changelist.getStatus() == ChangelistStatus.PENDING) {
                        try {
                            // Revert files in pending changelist
                            client.revertFiles(
                                    changelist.getFiles(true),
                                    new RevertFilesOptions()
                                            .setChangelistId(changelist.getId()));
                        } catch (P4JavaException e) {
                            // Can't do much here...
                        }
                    }
                }
            }
            if (client != null && server != null) {
                if (depotFile != null) {
                    try {
                        // Delete submitted test files
                        IChangelist deleteChangelist = getNewChangelist(server,
                                client,
                                "SubmitStreamingCallbackTest delete submitted files");
                        deleteChangelist = client
                                .createChangelist(deleteChangelist);
                        client.deleteFiles(FileSpecBuilder
                                        .makeFileSpecList(new String[]{depotFile}),
                                new DeleteFilesOptions()
                                        .setChangelistId(deleteChangelist
                                                .getId()));
                        deleteChangelist.refresh();
                        deleteChangelist.submit(null);
                    } catch (P4JavaException e) {
                        // Can't do much here...
                    }
                }
            }
        }
    }

}
