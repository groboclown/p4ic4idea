package com.perforce.p4java.tests.dev.unit.feature.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.client.IClientSummary;
import com.perforce.p4java.common.base.OSUtils;
import com.perforce.p4java.core.ChangelistStatus;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.generic.client.ClientLineEnding;
import com.perforce.p4java.impl.generic.core.Changelist;
import com.perforce.p4java.impl.mapbased.rpc.sys.RpcLineEndFilterOutputStream;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.server.IServer;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;


/*
 * This test ensures that the line ending for text files match the system that they
 * are on.
 */

@TestId("ClientLineEndingTest01")
public class ClientLineEndingTest extends P4JavaTestCase {

    private static String clientDir = defaultTestClientName + "_Dir" + File.separator + testId;

    //private static final String localWSPath = File.separator + "tmp"
    //	+ File.separator + "p4javatest";

    private static final int P4JTEST_CLIENTLINEENDINGS_COUNT = 13;

    private static final int CLETEST_FTYPE_CRCRLF = 0;
    private static final int CLETEST_FTYPE_ALLENDINGS = 1;
    private static final int CLETEST_FTYPE_LFCRLF = 2;
    private static final int CLETEST_FTYPE_CR = 3;
    private static final int CLETEST_FTYPE_LF = 4;
    private static final int CLETEST_FTYPE_CRLF = 5;

    private static final String CLETEST_STR_CR = "\r";
    private static final String CLETEST_STR_LF = "\n";
    private static final String CLETEST_STR_CRLF = "\r\n";
    private static final String CLETEST_STR_SHARE = CLETEST_STR_CRLF + "|" + CLETEST_STR_LF;
    private static final String CLETEST_STR_CRCRLF = "\r\r\n";
    private static final String CLETEST_STR_LFCRLF = "\n\r\n";

    private static IClient client;
    private static String clientRoot;
    private static String clientFilePath;

    @BeforeClass
    public static void beforeAll() throws Exception {
        server = getServer(serverUrlString, null, userName, "");
        client = server.getClient(getPlatformClientName(defaultTestClientName));
        clientRoot = client.getRoot();
        clientFilePath = clientRoot + File.separator + clientDir;
    }

    @Test
    public void testClientLineEndShareSpecForLFCRLF() throws IOException {

        IClientSummary.ClientLineEnd lineEnd = IClientSummary.ClientLineEnd.SHARE;
        List<IFileSpec> syncFiles = null;
        String currFileVer = null;
        boolean fileCreated = false;

        try {
            currFileVer = getTestFileVer();
            String[] fNameList = {
                    new File(clientFilePath, "LineEndingFile_LFCRLF"
                            + currFileVer).toString()
            };

            fileCreated = createFileWithSpecifiedLineEnding(fNameList[0], CLETEST_FTYPE_LFCRLF);

            syncFiles = taskAddSubmitSyncTestFiles(fNameList, lineEnd);
            assertFalse("Syncfiles unexpectedly returned Empty.", syncFiles.isEmpty());

            String lineEnding = fileLineEndingType(syncFiles.get(0).getClientPathString());
            assertEquals("Line endings should match.", "CLETEST_STR_LF", lineEnding);

        } catch (Exception exc) {
            fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
        } finally {
        }

    }

    @Test
    public void testClientLineEndShareSpecForCR() throws IOException {

        IClientSummary.ClientLineEnd lineEnd = IClientSummary.ClientLineEnd.SHARE;
        List<IFileSpec> syncFiles = null;
        String currFileVer = null;
        boolean fileCreated = false;

        try {

            currFileVer = getTestFileVer();
            String[] fNameList = {
                    new File(clientFilePath, "LineEndingFile_CR"
                            + currFileVer).toString()
            };

            fileCreated = createFileWithSpecifiedLineEnding(fNameList[0], CLETEST_FTYPE_CR);

            syncFiles = taskAddSubmitSyncTestFiles(fNameList, lineEnd);
            assertFalse("Syncfiles unexpectedly returned Empty.", syncFiles.isEmpty());

            String lineEnding = fileLineEndingType(syncFiles.get(0).getClientPathString());
            assertEquals("Line endings should match.", "CLETEST_STR_LF", lineEnding);

        } catch (Exception exc) {
            fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
        } finally {
        }


    }

    @Test
    public void testClientLineEndShareSpecForLF() throws IOException {

        IClientSummary.ClientLineEnd lineEnd = IClientSummary.ClientLineEnd.SHARE;
        List<IFileSpec> syncFiles = null;
        String currFileVer = null;
        boolean fileCreated = false;

        try {

            currFileVer = getTestFileVer();
            String[] fNameList = {
                    new File(clientFilePath, "LineEndingFile_LF" + currFileVer).toString()
            };

            fileCreated = createFileWithSpecifiedLineEnding(fNameList[0], CLETEST_FTYPE_LF);

            syncFiles = taskAddSubmitSyncTestFiles(fNameList, lineEnd);
            assertFalse("Syncfiles unexpectedly returned Empty.", syncFiles.isEmpty());

            String lineEnding = fileLineEndingType(syncFiles.get(0).getClientPathString());
            assertEquals("Line endings should match.", "CLETEST_STR_LF", lineEnding);

        } catch (Exception exc) {
            fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
        } finally {
        }

    }


    @Test
    public void testClientLineEndShareSpecForCRCRLF() throws IOException {

        IClientSummary.ClientLineEnd lineEnd = IClientSummary.ClientLineEnd.SHARE;
        List<IFileSpec> syncFiles = null;
        String currFileVer = null;
        boolean fileCreated = false;

        try {

            IServer server = getServer(serverUrlString, null, userName, "");
            IClient client = server.getClient(getPlatformClientName(defaultTestClientName));
            server.setCurrentClient(client);
            String clientRoot = client.getRoot();
            String clientFilePath = clientRoot + File.separator + clientDir;

            currFileVer = getTestFileVer();
            String[] fNameList = {
                    new File(clientFilePath, "LineEndingFile_CRCRLF" + currFileVer).toString()
            };

            fileCreated = createFileWithSpecifiedLineEnding(fNameList[0], CLETEST_FTYPE_CRCRLF);

            syncFiles = taskAddSubmitSyncTestFiles(fNameList, lineEnd);
            assertFalse("Syncfiles unexpectedly returned Empty.", syncFiles.isEmpty());

            String lineEnding = fileLineEndingType(syncFiles.get(0).getClientPathString());
            assertEquals("Line endings should match.", "CLETEST_STR_LF", lineEnding);

        } catch (Exception exc) {
            fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
        }
    }

    @Test
    public void testClientLineEndShareSpecForCRLF() throws IOException {

        IClientSummary.ClientLineEnd lineEnd = IClientSummary.ClientLineEnd.SHARE;
        List<IFileSpec> syncFiles = null;
        String currFileVer = null;
        boolean fileCreated = false;

        try {
            currFileVer = getTestFileVer();
            String[] fNameList = {
                    new File(clientFilePath, "LineEndingFile_CRLF" + currFileVer).toString()
            };

            fileCreated = createFileWithSpecifiedLineEnding(fNameList[0], CLETEST_FTYPE_CRLF);

            syncFiles = taskAddSubmitSyncTestFiles(fNameList, lineEnd);
            assertFalse("Syncfiles unexpectedly returned Empty.", syncFiles.isEmpty());

            String lineEnding = fileLineEndingType(syncFiles.get(0).getClientPathString());
            assertEquals("Line endings should match.", "CLETEST_STR_LF", lineEnding);

        } catch (Exception exc) {
            fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
        }

    }


    @Test
    public void testClientLineEndWinSpecForLF() throws IOException {

        IClientSummary.ClientLineEnd lineEnd = IClientSummary.ClientLineEnd.WIN;
        List<IFileSpec> syncFiles = null;
        String currFileVer = null;
        boolean fileCreated = false;

        try {
            currFileVer = getTestFileVer();
            String[] fNameList = {
                    new File(clientFilePath, "LineEndingFile_LF"
                            + currFileVer).toString()
            };

            fileCreated = createFileWithSpecifiedLineEnding(fNameList[0], CLETEST_FTYPE_LF);

            syncFiles = taskAddSubmitSyncTestFiles(fNameList, lineEnd);
            assertFalse("Syncfiles unexpectedly returned Empty.", syncFiles.isEmpty());

            String syncFileEnd = fileLineEndingType(syncFiles.get(0).getClientPathString());
            assertEquals("Files end should be the same.", "CLETEST_STR_CRLF", syncFileEnd);

        } catch (Exception exc) {
            fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
        }

    }

    @Test
    public void testClientLineEndWinSpecForCRLF() throws IOException {

        IClientSummary.ClientLineEnd lineEnd = IClientSummary.ClientLineEnd.WIN;
        List<IFileSpec> syncFiles = null;
        String currFileVer = null;
        boolean fileCreated = false;

        try {
            currFileVer = getTestFileVer();
            String[] fNameList = {
                    new File(clientFilePath, "LineEndingFile_CRLF"
                            + currFileVer).toString()
            };

            fileCreated = createFileWithSpecifiedLineEnding(fNameList[0], CLETEST_FTYPE_CRLF);

            syncFiles = taskAddSubmitSyncTestFiles(fNameList, lineEnd);
            assertFalse("Syncfiles unexpectedly returned Empty.", syncFiles.isEmpty());

            String syncFileEnd = fileLineEndingType(syncFiles.get(0).getClientPathString());
            assertEquals("Files end should be the same.", "CLETEST_STR_CRLF", syncFileEnd);

        } catch (Exception exc) {
            fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
        }

    }

    @Test
    public void testClientLineEndWinSpecForCR() throws IOException {

        IClientSummary.ClientLineEnd lineEnd = IClientSummary.ClientLineEnd.WIN;
        List<IFileSpec> syncFiles = null;
        String currFileVer = null;
        boolean fileCreated = false;

        try {
            currFileVer = getTestFileVer();
            String[] fNameList = {
                    new File(clientFilePath, "LineEndingFile_CR"
                            + currFileVer).toString()
            };

            fileCreated = createFileWithSpecifiedLineEnding(fNameList[0], CLETEST_FTYPE_CR);

            syncFiles = taskAddSubmitSyncTestFiles(fNameList, lineEnd);
            assertFalse("Syncfiles unexpectedly returned Empty.", syncFiles.isEmpty());

            String syncFileEnd = fileLineEndingType(syncFiles.get(0).getClientPathString());
            assertEquals("Files end should be the same.", "CLETEST_STR_CR", syncFileEnd);

        } catch (Exception exc) {
            fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
        }

    }

    @Test
    public void testClientLineEndWinSpecForCRCRLF() throws IOException {

        IClientSummary.ClientLineEnd lineEnd = IClientSummary.ClientLineEnd.WIN;
        List<IFileSpec> syncFiles = null;
        String currFileVer = null;
        boolean fileCreated = false;

        try {
            currFileVer = getTestFileVer();
            String[] fNameList = {
                    new File(clientFilePath, "LineEndingFile_CRCRLF"
                            + currFileVer).toString()
            };

            fileCreated = createFileWithSpecifiedLineEnding(fNameList[0], CLETEST_FTYPE_CRCRLF);

            syncFiles = taskAddSubmitSyncTestFiles(fNameList, lineEnd);
            assertFalse("Syncfiles unexpectedly returned Empty.", syncFiles.isEmpty());

            String syncFileEnd = fileLineEndingType(syncFiles.get(0).getClientPathString());
            assertEquals("Files end should be the same.", "CLETEST_STR_CRCRLF", syncFileEnd);

        } catch (Exception exc) {
            fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
        }

    }


    @Test
    public void testClientLineEndWinSpecForLFCRLF() throws IOException {

        IClientSummary.ClientLineEnd lineEnd = IClientSummary.ClientLineEnd.WIN;
        List<IFileSpec> syncFiles = null;
        String currFileVer = null;
        boolean fileCreated = false;

        try {
            currFileVer = getTestFileVer();
            String[] fNameList = {
                    new File(clientFilePath, "LineEndingFile_LFCRLF"
                            + currFileVer).toString()
            };

            fileCreated = createFileWithSpecifiedLineEnding(fNameList[0], CLETEST_FTYPE_LFCRLF);

            syncFiles = taskAddSubmitSyncTestFiles(fNameList, lineEnd);
            assertFalse("Syncfiles unexpectedly returned Empty.", syncFiles.isEmpty());

            String syncFileEnd = fileLineEndingType(syncFiles.get(0).getClientPathString());
            assertEquals("Files end should be the same.", "CLETEST_STR_CRLF", syncFileEnd);

        } catch (Exception exc) {
            fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
        } finally {
        }

    }

    @Test
    public void testClientLineEndLocalSpecForLFCRLF() throws IOException {

        IClientSummary.ClientLineEnd lineEnd = IClientSummary.ClientLineEnd.LOCAL;
        List<IFileSpec> syncFiles = null;
        String currFileVer = null;
        boolean fileCreated = false;


        try {
            currFileVer = getTestFileVer();
            String[] fNameList = {
                    new File(clientFilePath, "LineEndingFile_LFCRLF"
                            + currFileVer).toString()
            };

            fileCreated = createFileWithSpecifiedLineEnding(fNameList[0], CLETEST_FTYPE_LFCRLF);

            syncFiles = taskAddSubmitSyncTestFiles(fNameList, lineEnd);
            assertFalse("Syncfiles unexpectedly returned Empty.", syncFiles.isEmpty());

            String syncFileEnd = fileLineEndingType(syncFiles.get(0).getClientPathString());

            if (OSUtils.isUnix() || OSUtils.isOSX()) {
                // nothing need transfer
                assertEquals("Files end should be the same.", "CLETEST_STR_LFCRLF", syncFileEnd);
            } else if (OSUtils.isWindows()) {
                // server store file with 'LFLF' as the last 'CRLF' will transfer to 'LF'
                assertEquals("Files end should be the same.", "CLETEST_STR_CRLF", syncFileEnd);
            } else if (OSUtils.isClassicMac()) {
                // server store file with 'LFLFLF' as the middle 'CR' will transfer to 'LF'
                assertEquals("Files end should be the same.", "CLETEST_STR_CR", syncFileEnd);
            }
        } catch (Exception exc) {
            fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
        }

    }


    @Test
    public void testClientLineEndLocalSpecForLF() throws IOException {

        IClientSummary.ClientLineEnd lineEnd = IClientSummary.ClientLineEnd.LOCAL;
        List<IFileSpec> syncFiles = null;
        String currFileVer = null;
        boolean fileCreated = false;

        try {
            currFileVer = getTestFileVer();
            String[] fNameList = {
                    new File(clientFilePath, "LineEndingFile_LF"
                            + currFileVer).toString()
            };

            fileCreated = createFileWithSpecifiedLineEnding(fNameList[0], CLETEST_FTYPE_LF);

            syncFiles = taskAddSubmitSyncTestFiles(fNameList, lineEnd);
            assertFalse("Syncfiles unexpectedly returned Empty.", syncFiles.isEmpty());

            String syncFileEnd = fileLineEndingType(syncFiles.get(0).getClientPathString());
            assertEquals("Files end should be the same.", osPlatformLineEnding(getCurrentOsName()), syncFileEnd);

        } catch (Exception exc) {
            fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
        }

    }

    @Test
    public void testClientLineEndLocalSpecForCRLF() throws IOException {

        IClientSummary.ClientLineEnd lineEnd = IClientSummary.ClientLineEnd.LOCAL;
        List<IFileSpec> syncFiles = null;
        String currFileVer = null;
        boolean fileCreated = false;

        try {
            currFileVer = getTestFileVer();
            String[] fNameList = {
                    new File(clientFilePath, "LineEndingFile_CRLF"
                            + currFileVer).toString()
            };

            fileCreated = createFileWithSpecifiedLineEnding(fNameList[0], CLETEST_FTYPE_CRLF);

            syncFiles = taskAddSubmitSyncTestFiles(fNameList, lineEnd);
            assertFalse("Syncfiles unexpectedly returned Empty.", syncFiles.isEmpty());

            String syncFileEnd = fileLineEndingType(syncFiles.get(0).getClientPathString());
            if (OSUtils.isOSX() || OSUtils.isUnix()) {
                // not transfer line-end as 'local' equals 'unix' client line-end
                assertEquals("Files end should be the same.", "CLETEST_STR_CRLF", syncFileEnd);
            } else if (OSUtils.isWindows()) {
                // file stored server should with 'LF', but after sync with 'win' line-end is back to 'CRLF'
                assertEquals("Files end should be the same.", "CLETEST_STR_CRLF", syncFileEnd);
            } else if (OSUtils.isClassicMac()) {
                // file stored server should with 'LFLF' as 'CR' is transfer to 'LF'.
                // So after sync with 'mac' line-end is back to 'CRCR'
                assertEquals("Files end should be the same.", "CLETEST_STR_CR", syncFileEnd);
            }

        } catch (Exception exc) {
            fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
        }

    }

    @Test
    public void testClientLineEndLocalSpecForCR() throws IOException {

        IClientSummary.ClientLineEnd lineEnd = IClientSummary.ClientLineEnd.LOCAL;
        List<IFileSpec> syncFiles = null;
        String currFileVer = null;
        boolean fileCreated = false;

        try {
            currFileVer = getTestFileVer();
            String[] fNameList = {
                    new File(clientFilePath, "LineEndingFile_CR"
                            + currFileVer).toString()
            };

            fileCreated = createFileWithSpecifiedLineEnding(fNameList[0], CLETEST_FTYPE_CR);

            syncFiles = taskAddSubmitSyncTestFiles(fNameList, lineEnd);
            assertFalse("Syncfiles unexpectedly returned Empty.", syncFiles.isEmpty());

            String syncFileEnd = fileLineEndingType(syncFiles.get(0).getClientPathString());
            assertEquals("Files end should be the same.", "CLETEST_STR_CR", syncFileEnd);

        } catch (Exception exc) {
            fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
        }

    }

    @Test
    public void testClientLineEndLocalSpecForCRCRLF() throws IOException {

        IClientSummary.ClientLineEnd lineEnd = IClientSummary.ClientLineEnd.LOCAL;
        List<IFileSpec> syncFiles = null;
        String currFileVer = null;
        boolean fileCreated = false;

        try {
            currFileVer = getTestFileVer();
            String[] fNameList = {
                    new File(clientFilePath, "LineEndingFile_CRCRLF"
                            + currFileVer).toString()
            };

            fileCreated = createFileWithSpecifiedLineEnding(fNameList[0], CLETEST_FTYPE_CRCRLF);

            syncFiles = taskAddSubmitSyncTestFiles(fNameList, lineEnd);
            assertFalse("Syncfiles unexpectedly returned Empty.", syncFiles.isEmpty());

            String syncFileEnd = fileLineEndingType(syncFiles.get(0).getClientPathString());
            if (OSUtils.isUnix() || OSUtils.isOSX()) {
                // server side store file - same as orignal raw file
                assertEquals("Files end should be the same", "CLETEST_STR_CRCRLF", syncFileEnd);
            } else if (OSUtils.isWindows()) {
                // server side store file with 'CRLF' as latest 'CRLF' is transfer to 'LF'
                assertEquals("Files end should be the same.", "CLETEST_STR_CRCRLF", syncFileEnd);
            } else if (OSUtils.isClassicMac()) {
                // server side store file with 'LFLFLF' as 'CR' is transfer to 'LF'
                assertEquals("Files end should be the same.", "CLETEST_STR_CR", syncFileEnd);
            }
        } catch (Exception exc) {
            fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
        }

    }


    @Test
    public void testClientLineEndUnixSpecForLF() throws IOException {

        IClientSummary.ClientLineEnd lineEnd = IClientSummary.ClientLineEnd.UNIX;
        List<IFileSpec> syncFiles = null;
        String currFileVer = null;
        boolean fileCreated = false;

        try {
            currFileVer = getTestFileVer();
            String[] fNameList = {
                    new File(clientFilePath, "LineEndingFile_LF"
                            + currFileVer).toString()
            };

            fileCreated = createFileWithSpecifiedLineEnding(fNameList[0], CLETEST_FTYPE_LF);

            syncFiles = taskAddSubmitSyncTestFiles(fNameList, lineEnd);
            assertFalse("Syncfiles unexpectedly returned Empty.", syncFiles.isEmpty());

            String syncFileEnd = fileLineEndingType(syncFiles.get(0).getClientPathString());
            assertEquals("Files end should be the same.", "CLETEST_STR_LF", syncFileEnd);

        } catch (Exception exc) {
            fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
        }

    }


    @Test
    public void testClientLineEndUnixSpecForCRLF() throws IOException {

        IClientSummary.ClientLineEnd lineEnd = IClientSummary.ClientLineEnd.UNIX;
        List<IFileSpec> syncFiles = null;
        String currFileVer = null;
        boolean fileCreated = false;

        try {
            currFileVer = getTestFileVer();
            String[] fNameList = {
                    new File(clientFilePath, "LineEndingFile_CRLF"
                            + currFileVer).toString()
            };

            fileCreated = createFileWithSpecifiedLineEnding(fNameList[0], CLETEST_FTYPE_CRLF);

            syncFiles = taskAddSubmitSyncTestFiles(fNameList, lineEnd);
            assertFalse("Syncfiles unexpectedly returned Empty.", syncFiles.isEmpty());

            String syncFileEnd = fileLineEndingType(syncFiles.get(0).getClientPathString());
            assertEquals("Files end should be the same.", "CLETEST_STR_CRLF", syncFileEnd);

        } catch (Exception exc) {
            fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
        }

    }

    @Test
    public void testClientLineEndUnixSpecForCR() throws IOException {

        IClientSummary.ClientLineEnd lineEnd = IClientSummary.ClientLineEnd.UNIX;
        List<IFileSpec> syncFiles = null;
        String currFileVer = null;
        boolean fileCreated = false;

        try {
            currFileVer = getTestFileVer();
            String[] fNameList = {
                    new File(clientFilePath, "LineEndingFile_CR"
                            + currFileVer).toString()
            };

            fileCreated = createFileWithSpecifiedLineEnding(fNameList[0], CLETEST_FTYPE_CR);

            syncFiles = taskAddSubmitSyncTestFiles(fNameList, lineEnd);
            assertFalse("Syncfiles unexpectedly returned Empty.", syncFiles.isEmpty());

            String syncFileEnd = fileLineEndingType(syncFiles.get(0).getClientPathString());
            assertEquals("Files end should be the same.", "CLETEST_STR_CR", syncFileEnd);

        } catch (Exception exc) {
            fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
        }

    }

    @Test
    public void testClientLineEndUnixSpecForCRCRLF() throws IOException {

        IClientSummary.ClientLineEnd lineEnd = IClientSummary.ClientLineEnd.UNIX;
        List<IFileSpec> syncFiles = null;
        String currFileVer = null;
        boolean fileCreated = false;

        try {
            currFileVer = getTestFileVer();
            String[] fNameList = {
                    new File(clientFilePath, "LineEndingFile_CRCRLF"
                            + currFileVer).toString()
            };

            fileCreated = createFileWithSpecifiedLineEnding(fNameList[0], CLETEST_FTYPE_CRCRLF);

            syncFiles = taskAddSubmitSyncTestFiles(fNameList, lineEnd);
            assertFalse("Syncfiles unexpectedly returned Empty.", syncFiles.isEmpty());

            String syncFileEnd = fileLineEndingType(syncFiles.get(0).getClientPathString());
            assertEquals("Files end should be the same.", "CLETEST_STR_CRCRLF", syncFileEnd);

        } catch (Exception exc) {
            fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
        }

    }


    @Test
    public void testClientLineEndUnixSpecForLFCRLF() throws IOException {

        IClientSummary.ClientLineEnd lineEnd = IClientSummary.ClientLineEnd.UNIX;
        List<IFileSpec> syncFiles = null;
        String currFileVer = null;
        boolean fileCreated = false;

        try {
            currFileVer = getTestFileVer();
            String[] fNameList = {
                    new File(clientFilePath, "LineEndingFile_LFCRLF"
                            + currFileVer).toString()
            };

            fileCreated = createFileWithSpecifiedLineEnding(fNameList[0], CLETEST_FTYPE_LFCRLF);

            syncFiles = taskAddSubmitSyncTestFiles(fNameList, lineEnd);
            assertFalse("Syncfiles unexpectedly returned Empty.", syncFiles.isEmpty());

            String syncFileEnd = fileLineEndingType(syncFiles.get(0).getClientPathString());
            assertEquals("Files end should be the same.", "CLETEST_STR_LFCRLF", syncFileEnd);

        } catch (Exception exc) {
            fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
        }

    }


    @Test
    public void testMyLineEndings() {

        debugPrintTestName();

        assertEquals("Ending string for IClientSummary.ClientLineEnd.UNIX: ", CLETEST_STR_LF,
                expLineEndingStr(IClientSummary.ClientLineEnd.UNIX));
        assertEquals("Ending string for IClientSummary.ClientLineEnd.WIN: ", CLETEST_STR_CRLF,
                expLineEndingStr(IClientSummary.ClientLineEnd.WIN));
        assertEquals("Ending string for IClientSummary.ClientLineEnd.SHARE: ", CLETEST_STR_SHARE,
                expLineEndingStr(IClientSummary.ClientLineEnd.SHARE));
        if (getCurrentOsName().toLowerCase().contains("windows")) {
            assertEquals("Ending string for IClientSummary.ClientLineEnd.LOCAL: ", CLETEST_STR_CRLF,
                    expLineEndingStr(IClientSummary.ClientLineEnd.LOCAL));
        } else {
            assertEquals("Ending string for IClientSummary.ClientLineEnd.LOCAL: ", CLETEST_STR_LF,
                    expLineEndingStr(IClientSummary.ClientLineEnd.LOCAL));
        }
    }


    /**
     * Checks that the declared constant values in ClientLineEnding.java are what we expect:
     * a CARRIAGE_RETURN char should be '\r';
     * a LINE_FEED char should be '\n'
     */
    @Test
    public void testCLELineEndingBytes() {

        debugPrintTestName();

        char expCR = '\r';
        char expLF = '\n';
        byte[] expCRBytes = new byte[]{'\r'};
        byte[] expLFBytes = new byte[]{'\n'};
        byte[] expCRLFBytes = new byte[]{'\r', '\n'};
        byte[] expLFCRLFBytes = new byte[]{'\n', '\r', '\n'};

        assertEquals("Line ending char should match expected value.", expLF, ClientLineEnding.FST_L_LF_CHAR);
        assertEquals("Line ending char should match expected value.", expCR, ClientLineEnding.FST_L_CR_CHAR);
        assertTrue("Line ending byte should match expected value.", Arrays.equals(expLFBytes, ClientLineEnding.FST_L_LF_BYTES));
        assertTrue("Line ending byte should match expected value.", Arrays.equals(expCRBytes, ClientLineEnding.FST_L_CR_BYTES));
        assertTrue("Line ending byte should match expected value.", Arrays.equals(expCRLFBytes, ClientLineEnding.FST_L_CRLF_BYTES));
        assertTrue("Line ending byte should match expected value.", Arrays.equals(expLFCRLFBytes, ClientLineEnding.FST_L_LFCRLF_BYTES));

    }

    /**
     * Returns an array containing the constants of this enum type, in the order they are declared.
     */
    @Test
    public void testCLEValues() {

        debugPrintTestName();

        ClientLineEnding[] cLE = ClientLineEnding.values();
        assertNotNull("CLE array must not be Null.", cLE);
        assertEquals("CLE must have " + P4JTEST_CLIENTLINEENDINGS_COUNT + "members.",
                cLE.length, P4JTEST_CLIENTLINEENDINGS_COUNT);

        for (ClientLineEnding newCLE : cLE) {
            debugPrint("CLE: " + newCLE);
        }

    }

    /**
     * This test checks that the valuesOf() method returns the enum
     * constant that corresponds with the specified name
     */
    @Test
    public void testCLEValuesOf() {

        debugPrintTestName();

        ClientLineEnding cLE_Val;

        // positive tests
        cLE_Val = ClientLineEnding.valueOf("FST_L_LOCAL");
        assertNotNull("CLE value must not be Null.", cLE_Val);
        assertEquals("Returned CLE value should match enum type.", ClientLineEnding.FST_L_LOCAL, cLE_Val);
        debugPrint("FST_L_LOCAL: " + cLE_Val);

        cLE_Val = ClientLineEnding.valueOf("FST_L_LF");
        assertNotNull("CLE value must not be Null.", cLE_Val);
        assertEquals("Returned CLE value should match enum type.", ClientLineEnding.FST_L_LF, cLE_Val);
        debugPrint("FST_L_LF: " + cLE_Val);

        cLE_Val = ClientLineEnding.valueOf("FST_L_CR");
        assertNotNull("CLE value must not be Null.", cLE_Val);
        assertEquals("Returned CLE value should match enum type.", ClientLineEnding.FST_L_CR, cLE_Val);
        debugPrint("FST_L_CR: " + cLE_Val);

        cLE_Val = ClientLineEnding.valueOf("FST_L_CRLF");
        assertNotNull("CLE value must not be Null.", cLE_Val);
        assertEquals("Returned CLE value should match enum type.", ClientLineEnding.FST_L_CRLF, cLE_Val);
        debugPrint("FST_L_CRLF: " + cLE_Val);

        cLE_Val = ClientLineEnding.valueOf("FST_L_LFCRLF");
        assertNotNull("CLE value must not be Null.", cLE_Val);
        assertEquals("Returned CLE value should match enum type.", ClientLineEnding.FST_L_LFCRLF, cLE_Val);
        debugPrint("FST_L_LFCRLF: " + cLE_Val);
    }

    /**
     * Each of these should throw an IllegalArgumentsException
     * Throws:
     * java.lang.IllegalArgumentException - if this enum type has no constant
     * with the specified name
     */

    @Test(expected = java.lang.IllegalArgumentException.class)
    public void testCLEValueOfIllegalArgException() {

        @SuppressWarnings("unused")
        ClientLineEnding cLE_BadVal;

        debugPrintTestName();

        cLE_BadVal = ClientLineEnding.valueOf(" FST_L_LOCAL ");
        debugPrint("Expecting java.lang.IllegalArgumentException");

        cLE_BadVal = ClientLineEnding.valueOf("FST_L_LOCAL ");
        debugPrint("Expecting java.lang.IllegalArgumentException");

        cLE_BadVal = ClientLineEnding.valueOf(" FST_L_LOCAL");
        debugPrint("Expecting java.lang.IllegalArgumentException");

        cLE_BadVal = ClientLineEnding.valueOf("_LOCAL");
        debugPrint("Expecting java.lang.IllegalArgumentException");

        cLE_BadVal = ClientLineEnding.valueOf("FST_L_LOCALY");
        debugPrint("Expecting java.lang.IllegalArgumentException");

        cLE_BadVal = ClientLineEnding.valueOf("FST_L_LOCAL\n");
        debugPrint("Expecting java.lang.IllegalArgumentException");

        cLE_BadVal = ClientLineEnding.valueOf("\nFST_L_LOCAL");
        debugPrint("Expecting java.lang.IllegalArgumentException");

        cLE_BadVal = ClientLineEnding.valueOf("FST__L_LOCAL");
        debugPrint("Expecting java.lang.IllegalArgumentException");

        cLE_BadVal = ClientLineEnding.valueOf("FST_L_LOCAL'");
        debugPrint("Expecting java.lang.IllegalArgumentException");

        cLE_BadVal = ClientLineEnding.valueOf("FST_ L_LOCAL");
        debugPrint("Expecting java.lang.IllegalArgumentException");

        cLE_BadVal = ClientLineEnding.valueOf("fst_l_local");
        debugPrint("Expecting java.lang.IllegalArgumentException");

        cLE_BadVal = ClientLineEnding.valueOf("FST_l_LOCAL");
        debugPrint("Expecting java.lang.IllegalArgumentException");

        cLE_BadVal = ClientLineEnding.valueOf("THIS_IS_A_TEST");
        debugPrint("Expecting java.lang.IllegalArgumentException");

        //here, LOCAL is spelled with a zero
        cLE_BadVal = ClientLineEnding.valueOf("FST_L_L0CAL");
        debugPrint("Expecting java.lang.IllegalArgumentException");

        cLE_BadVal = ClientLineEnding.valueOf(".FST_L_LOCAL.");
        debugPrint("Expecting java.lang.IllegalArgumentException");

        cLE_BadVal = ClientLineEnding.valueOf(" ");
        debugPrint("Expecting java.lang.IllegalArgumentException");

        cLE_BadVal = ClientLineEnding.valueOf("");
        debugPrint("Expecting java.lang.IllegalArgumentException");

    }

    /**
     * This test should throw an NullPointerException
     */
    @Test(expected = java.lang.NullPointerException.class)
    public void testCLEValueOfNullPtrException() {

        @SuppressWarnings("unused")
        ClientLineEnding cLE_BadVal;

        debugPrintTestName();

        cLE_BadVal = ClientLineEnding.valueOf(null);
        debugPrint("Expecting java.lang.NullPointerException");
    }

    @Test
    public void testUnixSpecLOCALNeedsLineEndFiltering() {

        debugPrintTestName();
        debugPrint("testUnixSpecLOCALNeedsLineEndFiltering");

        boolean lineEndsMatch = checkSpecNeedsLineEndFiltering(ClientLineEnding.FST_L_LOCAL,
                IClientSummary.ClientLineEnd.UNIX, !ClientLineEnding.localLineEndStr.equals(
                        RpcLineEndFilterOutputStream.P4SERVER_LINSEP_STR));
        assertTrue("Only when our local line ending does not match the standard server line ending we will require line end filtering", lineEndsMatch);

    }

    @Test
    public void testUnixSpecLFNeedsLineEndFiltering() {

        debugPrintTestName();
        debugPrint("testUnixSpecLFNeedsLineEndFiltering");

        boolean lineEndsMatch = checkSpecNeedsLineEndFiltering(ClientLineEnding.FST_L_LF,
                IClientSummary.ClientLineEnd.UNIX);
        assertTrue("LF on Unix should not need line end filtering", lineEndsMatch);

    }

    @Test
    public void testUnixSpecCRNeedsLineEndFiltering() {

        debugPrintTestName();
        debugPrint("testUnixSpecCRNeedsLineEndFiltering");

        boolean lineEndsMatch = checkSpecNeedsLineEndFiltering(ClientLineEnding.FST_L_CR,
                IClientSummary.ClientLineEnd.UNIX);
        assertTrue("CR on unix requres line end filtering", lineEndsMatch);

    }


    @Test
    public void testUnixSpecLFCRLFNeedsLineEndFiltering() {

        debugPrintTestName();
        debugPrint("testUnixSpecLFCRLFNeedsLineEndFiltering");

        boolean lineEndsMatch = checkSpecNeedsLineEndFiltering(ClientLineEnding.FST_L_LFCRLF,
                IClientSummary.ClientLineEnd.UNIX);
        assertTrue("LFCRLF on unix reqires line end filtering", lineEndsMatch);

    }

    @Test
    public void testUnixSpecCRLFNeedsLineEndFiltering() {

        debugPrintTestName();
        debugPrint("testUnixSpecCRLFNeedsLineEndFiltering");

        boolean lineEndsMatch = checkSpecNeedsLineEndFiltering(ClientLineEnding.FST_L_CRLF,
                IClientSummary.ClientLineEnd.UNIX);
        assertTrue("CRLF on unix reqires line end filtering", lineEndsMatch);

    }


    @Test
    public void testWinSpecLOCALNeedsLineEndFiltering() {

        debugPrintTestName();
        debugPrint("testWinSpecLOCALNeedsLineEndFiltering");

        boolean lineEndsMatch = checkSpecNeedsLineEndFiltering(ClientLineEnding.FST_L_LOCAL,
                IClientSummary.ClientLineEnd.WIN, !ClientLineEnding.localLineEndStr.equals(
                        RpcLineEndFilterOutputStream.P4SERVER_LINSEP_STR));
        assertTrue("Local line ending on windows will require line end filtering if our local line end does not match the server line ending", lineEndsMatch);

    }

    @Test
    public void testWinSpecLFNeedsLineEndFiltering() {

        debugPrintTestName();
        debugPrint("testWinSpecLFNeedsLineEndFiltering");

        boolean lineEndsMatch = checkSpecNeedsLineEndFiltering(ClientLineEnding.FST_L_LF,
                IClientSummary.ClientLineEnd.WIN, false);

        assertTrue("LF doesn't need line filtering, apart from windows.", lineEndsMatch);

    }

    @Test
    public void testWinSpecCRNeedsLineEndFiltering() {

        debugPrintTestName();
        debugPrint("testWinSpecCRNeedsLineEndFiltering");

        boolean lineEndsMatch = checkSpecNeedsLineEndFiltering(ClientLineEnding.FST_L_CR,
                IClientSummary.ClientLineEnd.WIN);
        assertTrue("CR on windows will need line end filtering", lineEndsMatch);

    }


    @Test
    public void testWinSpecLFCRLFNeedsLineEndFiltering() {

        debugPrintTestName();
        debugPrint("testWinSpecLFCRLFNeedsLineEndFiltering");

        boolean lineEndsMatch = checkSpecNeedsLineEndFiltering(ClientLineEnding.FST_L_LFCRLF,
                IClientSummary.ClientLineEnd.WIN);
        assertTrue("LFCRLF on windows needs line filtering", lineEndsMatch);

    }

    @Test
    public void testWinSpecCRLFNeedsLineEndFiltering() {

        debugPrintTestName();
        debugPrint("testWinSpecCRLFNeedsLineEndFiltering");

        boolean lineEndsMatch = checkSpecNeedsLineEndFiltering(ClientLineEnding.FST_L_CRLF,
                IClientSummary.ClientLineEnd.WIN, false);
        assertTrue("CRLF on windows does not need line filtering", lineEndsMatch);

    }

    @Test
    public void testShareSpecLOCALNeedsLineEndFiltering() {

        debugPrintTestName();
        debugPrint("testShareSpecLOCALNeedsLineEndFiltering");

        boolean lineEndsMatch = checkSpecNeedsLineEndFiltering(ClientLineEnding.FST_L_LOCAL,
                IClientSummary.ClientLineEnd.SHARE, !ClientLineEnding.localLineEndStr.equals(
                        RpcLineEndFilterOutputStream.P4SERVER_LINSEP_STR));
        assertTrue("Share should only need filtering if your local line endings do not match the server standard line ending", lineEndsMatch);

    }

    @Test
    public void testShareSpecLFNeedsLineEndFiltering() {

        debugPrintTestName();
        debugPrint("testShareSpecLFNeedsLineEndFiltering");

        boolean lineEndsMatch = checkSpecNeedsLineEndFiltering(ClientLineEnding.FST_L_LF,
                IClientSummary.ClientLineEnd.SHARE);
        assertTrue("needsLineEndFiltering results should match.", lineEndsMatch);

    }

    @Test
    public void testShareSpecCRNeedsLineEndFiltering() {

        debugPrintTestName();
        debugPrint("testShareSpecCRNeedsLineEndFiltering");

        boolean lineEndsMatch = checkSpecNeedsLineEndFiltering(ClientLineEnding.FST_L_CR,
                IClientSummary.ClientLineEnd.SHARE);
        assertTrue("needsLineEndFiltering results should match.", lineEndsMatch);

    }


    @Test
    public void testShareSpecLFCRLFNeedsLineEndFiltering() {

        debugPrintTestName();
        debugPrint("testShareSpecLFCRLFNeedsLineEndFiltering");

        boolean lineEndsMatch = checkSpecNeedsLineEndFiltering(ClientLineEnding.FST_L_LFCRLF,
                IClientSummary.ClientLineEnd.SHARE);
        assertTrue("needsLineEndFiltering results should match.", lineEndsMatch);

    }

    @Test
    public void testShareSpecCRLFNeedsLineEndFiltering() {

        debugPrintTestName();
        debugPrint("testShareSpecCRLFNeedsLineEndFiltering");

        boolean lineEndsMatch = checkSpecNeedsLineEndFiltering(ClientLineEnding.FST_L_CRLF,
                IClientSummary.ClientLineEnd.SHARE, false);
        assertTrue("Share spec with CRLF should not need line end filtering, although CRLF normally does", lineEndsMatch);

    }

    @Test
    public void testLocalSpecLOCALNeedsLineEndFiltering() {

        debugPrintTestName();
        debugPrint("testLocalSpecLOCALNeedsLineEndFiltering");

        boolean lineEndsMatch = checkSpecNeedsLineEndFiltering(ClientLineEnding.FST_L_LOCAL,
                IClientSummary.ClientLineEnd.LOCAL, !ClientLineEnding.localLineEndStr.equals(
                        RpcLineEndFilterOutputStream.P4SERVER_LINSEP_STR));
        assertTrue("Local line endings will only need filtering if the local line ending is different than the standard server line ending", lineEndsMatch);

    }

    @Test
    public void testLocalSpecLFNeedsLineEndFiltering() {

        debugPrintTestName();
        debugPrint("testLocalSpecLFNeedsLineEndFiltering");

        boolean lineEndsMatch = checkSpecNeedsLineEndFiltering(ClientLineEnding.FST_L_LF,
                IClientSummary.ClientLineEnd.LOCAL);
        assertTrue("Local line ending will need filtering only if is is not lf", lineEndsMatch);

    }

    @Test
    public void testLocalSpecCRNeedsLineEndFiltering() {

        debugPrintTestName();
        debugPrint("testLocalSpecCRNeedsLineEndFiltering");

        boolean lineEndsMatch = checkSpecNeedsLineEndFiltering(ClientLineEnding.FST_L_CR,
                IClientSummary.ClientLineEnd.LOCAL);
        assertTrue("CR line ending needs need line filtering", lineEndsMatch);

    }


    @Test
    public void testLocalSpecLFCRLFNeedsLineEndFiltering() {

        debugPrintTestName();
        debugPrint("testLocalSpecLFCRLFNeedsLineEndFiltering");

        boolean lineEndsMatch = checkSpecNeedsLineEndFiltering(ClientLineEnding.FST_L_LFCRLF,
                IClientSummary.ClientLineEnd.LOCAL);
        assertTrue("LFCRLF will need line end filtering", lineEndsMatch);

    }

    @Test
    public void testLocalSpecCRLFNeedsLineEndFiltering() {

        debugPrintTestName();
        debugPrint("testLocalSpecCRLFNeedsLineEndFiltering");

        boolean lineEndsMatch = checkSpecNeedsLineEndFiltering(ClientLineEnding.FST_L_CRLF,
                IClientSummary.ClientLineEnd.LOCAL);
        assertTrue("CRLF will need line end filtering if the local line end is anything but CRLF", lineEndsMatch);

    }

    @Test
    public void testMacSpecMACNeedsLineEndFiltering() {

        debugPrintTestName();
        debugPrint("testMacSpecMACNeedsLineEndFiltering");

        boolean lineEndsMatch = checkSpecNeedsLineEndFiltering(ClientLineEnding.FST_L_LOCAL,
                IClientSummary.ClientLineEnd.MAC, !ClientLineEnding.localLineEndStr.equals(
                        RpcLineEndFilterOutputStream.P4SERVER_LINSEP_STR));
        assertTrue("Local line endings on mac will need filtering only if they do not match the server line ending.", lineEndsMatch);

    }

    @Test
    public void testMacSpecLFNeedsLineEndFiltering() {

        debugPrintTestName();
        debugPrint("testMacSpecLFNeedsLineEndFiltering");

        boolean lineEndsMatch = checkSpecNeedsLineEndFiltering(ClientLineEnding.FST_L_LF,
                IClientSummary.ClientLineEnd.MAC);
        assertTrue("LF on Mac should not need line end filtering", lineEndsMatch);

    }

    @Test
    public void testMacSpecCRNeedsLineEndFiltering() {

        debugPrintTestName();
        debugPrint("testMacSpecCRNeedsLineEndFiltering");

        boolean lineEndsMatch = checkSpecNeedsLineEndFiltering(ClientLineEnding.FST_L_CR,
                IClientSummary.ClientLineEnd.MAC);
        assertTrue("CR on MAC will require line ending filtering", lineEndsMatch);

    }


    @Test
    public void testMacSpecLFCRLFNeedsLineEndFiltering() {

        debugPrintTestName();
        debugPrint("testMacSpecLFCRLFNeedsLineEndFiltering");

        boolean lineEndsMatch = checkSpecNeedsLineEndFiltering(ClientLineEnding.FST_L_LFCRLF,
                IClientSummary.ClientLineEnd.MAC);
        assertTrue("LFCRLF will need line end filtering on MAC", lineEndsMatch);

    }

    @Test
    public void testMacSpecCRLFNeedsLineEndFiltering() {

        debugPrintTestName();
        debugPrint("testMacSpecCRLFNeedsLineEndFiltering");

        boolean lineEndsMatch = checkSpecNeedsLineEndFiltering(ClientLineEnding.FST_L_CRLF,
                IClientSummary.ClientLineEnd.MAC);
        assertTrue("CRLF on MAC will need line end filtering", lineEndsMatch);

    }


    //===============================================

    public List<IFileSpec> taskAddSubmitSyncTestFiles(String[] fNameList,
                                                      IClientSummary.ClientLineEnd newLineEnd) {

        IServer server = null;
        IClient client = null;
        IClientSummary.ClientLineEnd currLineEnd = null;
        List<IFileSpec> syncFiles = null;

        try {
            server = getServer(serverUrlString, null, userName, "");
            assertNotNull("Null Server Returned!!", server);

            server.setUserName(userName);
            server.login(password);

            client = server.getClient(getPlatformClientName(defaultTestClientName));
            server.setCurrentClient(client);
            assertNotNull("Null client returned", client);

            currLineEnd = client.getLineEnd();
            saveNewClientLineEndInSpec(client, newLineEnd);

            //create a changelist
            IChangelist changelist = createTestChangelist(server, client,
                    "Changelist to submit files with ClientLineEnd "
                            + newLineEnd + " under " + getName());

            //add the test files
            List<IFileSpec> testFiles = addTestFiles(client, fNameList,
                    changelist.getId(), true);
            assertNotNull("testFiles ahould not be Null.", testFiles);

            //submit files
            changelist.update();
            List<IFileSpec> submittedFiles = changelist.submit(false);
            assertNotNull("submittedFiles should not be Null.", submittedFiles);

            syncFiles = client.sync(
                    FileSpecBuilder.getValidFileSpecs(submittedFiles),
                    true, false, false, false);
            assertNotNull("syncFiles should not be Null.", syncFiles);


        } catch (Exception exc) {
            fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
        } finally {
            try {
                saveNewClientLineEndInSpec(client, currLineEnd);
            } catch (ConnectionException | RequestException | AccessException e) {
                fail("Unexpected Exception: " + e + " - " + e.getLocalizedMessage());
            }
        }

        return syncFiles;

    }


    public List<IFileSpec> addTestFiles(IClient client, String[] testFiles, boolean validOnly)
            throws ConnectionException, AccessException {

        int changelistId = 0;

        return (addTestFiles(client, testFiles, changelistId, validOnly));
    }

    public List<IFileSpec> addTestFiles(IClient client, String[] testFiles,
                                        int changelistID, boolean validOnly)
            throws ConnectionException, AccessException {

        List<IFileSpec> fList = FileSpecBuilder.makeFileSpecList(testFiles);
        assertNotNull("FileSpecBuilder unexpectedly returned Null SpecList.", fList);
        assertFalse("File List should not be empty.", fList.isEmpty());
        assertEquals("Number of FileList entries should equal original number of files.",
                fList.size(), testFiles.length);
        //add files to depot then submit
        List<IFileSpec> newAddedSpecList = client.addFiles(fList, false, changelistID, P4JTEST_FILETYPE_TEXT, true);

        if (validOnly) {
            return FileSpecBuilder.getValidFileSpecs(newAddedSpecList);
        } else {
            return newAddedSpecList;
        }


    }

    public void saveNewClientLineEndInSpec(IClient client, IClientSummary.ClientLineEnd newLineEnd) throws ConnectionException, RequestException, AccessException {

        //get the old line ending
        assertNotNull("Client object is null.", client);
        IClientSummary.ClientLineEnd oldLineEnd = client.getLineEnd();

        //set the new line ending
        client.setLineEnd(newLineEnd);
        client.update();
        //check that the changes stuck
        IClientSummary.ClientLineEnd cLineEnd = client.getLineEnd();
        debugPrint("setLineEnd to: " + newLineEnd, "OLD: " + oldLineEnd, "NEW: " + cLineEnd);

    }

    public String fileLineEndingType(String fName) {

        String lEnd = null;
        Scanner scanner = null;
        String fileText = "";
        String lEndStr = null;

        try {
            //read in the entire file
            scanner = new Scanner(new File(fName));
            Scanner scannerText = scanner.useDelimiter("\\z");
            while (scannerText.hasNext()) {
                fileText += scannerText.next();
            }

            //CLETEST_STR_LFCRLF
            if (fileText.contains(CLETEST_STR_LF)) {
                if (fileText.contains(CLETEST_STR_CRLF)) {
                    lEnd = CLETEST_STR_CRLF;
                    lEndStr = "CLETEST_STR_CRLF";
                    if (fileText.contains(CLETEST_STR_CRCRLF)) {
                        lEnd = CLETEST_STR_CRCRLF;
                        lEndStr = "CLETEST_STR_CRCRLF";
                    } else if (fileText.contains(CLETEST_STR_LFCRLF) && !fileText.contains(
                            CLETEST_STR_CRLF + CLETEST_STR_CRLF)) {
                        lEnd = CLETEST_STR_LFCRLF;
                        lEndStr = "CLETEST_STR_LFCRLF";
                    }
                } else {
                    lEnd = CLETEST_STR_LF;
                    lEndStr = "CLETEST_STR_LF";
                }
            } else {
                lEnd = CLETEST_STR_CR;
                lEndStr = "CLETEST_STR_CR";
            }
            debugPrint("FILE: " + fName, "Contained " + lEndStr + " line ending.");
            debugPrint("Ending Looks like: " + lEnd);

        } catch (Exception exc) {
            fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
        } finally {
            if (scanner != null) {
                scanner.close();
            }
        }

        return lEndStr;
    }

    /**
     * This function returns the expected ClientLineEnding given the IClientSummary.ClientLineEnd
     */
    public String expLineEndingStr(IClientSummary.ClientLineEnd iCLE) {

        String expLE = null;

        switch (iCLE) {
            case UNIX:
                expLE = CLETEST_STR_LF;
                break;
            case WIN:
                expLE = CLETEST_STR_CRLF;
                break;
            case LOCAL:
                if (getCurrentOsName().toLowerCase().contains("windows")) {
                    expLE = CLETEST_STR_CRLF;
                } else {
                    expLE = CLETEST_STR_LF;
                }
                break;
            case SHARE:
                expLE = CLETEST_STR_CRLF + "|" + CLETEST_STR_LF;
                break;
            default:
                expLE = CLETEST_STR_LF;
        }
        ;

        return expLE;
    }


    public boolean expectedCLFilterResult(IClientSummary.ClientLineEnd iCLE, ClientLineEnding cCLE) {

        boolean needsFilter = true;

        switch (iCLE) {
            case MAC:
            case UNIX:
                if (cCLE.equals(ClientLineEnding.FST_L_LF)) {
                    needsFilter = false;
                }
                break;
            case WIN:
                if (cCLE.equals(ClientLineEnding.FST_L_CRLF)) {
                    needsFilter = false;
                }
                break;
            case LOCAL:
                if (cCLE.equals(ClientLineEnding.FST_L_LF)) {
                    needsFilter = false;
                }
                break;
            case SHARE:
                if (cCLE.equals(ClientLineEnding.FST_L_CRLF) || cCLE.equals(ClientLineEnding.FST_L_LF)) {
                    needsFilter = false;
                }
                break;
        }
        ;

        return needsFilter;

    }


    public void verifyFileLineEndings(IClientSummary.ClientLineEnd IClientSumLineEnd, String fileName) {

        Scanner scanner = null;
        String fileText = "";
        try {
            String expLineEnd = expLineEndingStr(IClientSumLineEnd);

            //read in the entire file
            scanner = new Scanner(new File(fileName));
            Scanner scannerText = scanner.useDelimiter("\\z");
            while (scannerText.hasNext()) {
                fileText += scannerText.next();
            }
            assertTrue("File should contain " + IClientSumLineEnd + " line ending.", fileText.contains(expLineEnd));
            debugPrint("FILE: " + fileName, "Contained " + IClientSumLineEnd + " line ending.");
        } catch (Exception exc) {
            fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
        } finally {
            if (scanner != null) {
                scanner.close();
            }
        }

    }

    public Changelist createNewChangelistImpl(IServer server, IClient client, String chgDescr) {

        Changelist changeListImpl = null;
        try {
            changeListImpl = new Changelist(
                    IChangelist.UNKNOWN,
                    client.getName(),
                    userName,
                    ChangelistStatus.NEW,
                    new Date(),
                    chgDescr,
                    false,
                    (Server) server
            );
        } catch (Exception exc) {
            fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
        }
        return changeListImpl;
    }

    public IChangelist createTestChangelist(IServer server, IClient client, String chgDescr) throws
            ConnectionException, RequestException, AccessException {

        Changelist changeListImpl = createNewChangelistImpl(server, client, chgDescr);
        IChangelist changelist = client.createChangelist(changeListImpl);

        return changelist;
    }


    public String getCurrentOsName() {

        String currOsName = getProperty("os.name");

        return (currOsName);

    }


    /**
     * This routine simply writes a file with different line endings
     */
    public boolean createFileWithSpecifiedLineEnding(String fName, int fType) throws IOException {

        String tLine = "This line ends in a ";
        BufferedWriter bw = null;
        String fText = "";

        try {
            //FIXME: move this to base level I/O
            //now make any dirs needed for the file

            File fileBaseSource = new File(fName);
            if (fileBaseSource.getParentFile().exists() == false) {
                boolean madeDir = fileBaseSource.getParentFile().mkdirs();
                debugPrint(fileBaseSource.getParentFile() + " does not exist: ", "MadeDir?: " + madeDir);
            } else {
                debugPrint("This is the fileBaseSource, and it does exist: " + fileBaseSource.getParentFile());
            }

            FileWriter fstream = new FileWriter(fName, false);
            bw = new BufferedWriter(fstream);

            switch (fType) {
                case CLETEST_FTYPE_CRCRLF:
                    fText = tLine + "CLETEST_STR_CRCRLF" + CLETEST_STR_CRCRLF
                            + tLine + "CLETEST_STR_CRCRLF" + CLETEST_STR_CRCRLF
                            + tLine + "CLETEST_STR_CRCRLF" + CLETEST_STR_CRCRLF
                            + tLine + "CLETEST_STR_CRCRLF" + CLETEST_STR_CRCRLF
                            + tLine + "CLETEST_STR_CRCRLF" + CLETEST_STR_CRCRLF;
                    break;

                case CLETEST_FTYPE_ALLENDINGS:
                    fText = tLine + "CLETEST_STR_CR" + CLETEST_STR_CR
                            + tLine + "CLETEST_STR_LF" + CLETEST_STR_LF
                            + tLine + "CLETEST_STR_CRCRLF" + CLETEST_STR_CRCRLF
                            + tLine + "CLETEST_STR_CRLF" + CLETEST_STR_CRLF
                            + tLine + "CLETEST_STR_CRCRLF" + CLETEST_STR_CRCRLF
                            + tLine + "CLETEST_STR_LF" + CLETEST_STR_LF
                            + tLine + "CLETEST_STR_CRCRLF" + CLETEST_STR_CRCRLF;
                    break;

                case CLETEST_FTYPE_LFCRLF:
                    fText = tLine + "CLETEST_STR_LFCRLF" + CLETEST_STR_LFCRLF
                            + tLine + "CLETEST_STR_LFCRLF" + CLETEST_STR_LFCRLF
                            + tLine + "CLETEST_STR_LFCRLF" + CLETEST_STR_LFCRLF
                            + tLine + "CLETEST_STR_LFCRLF" + CLETEST_STR_LFCRLF
                            + tLine + "CLETEST_STR_LFCRLF" + CLETEST_STR_LFCRLF;
                    break;

                case CLETEST_FTYPE_CR:
                    fText = tLine + "CLETEST_STR_CR" + CLETEST_STR_CR
                            + tLine + "CLETEST_STR_CR" + CLETEST_STR_CR
                            + tLine + "CLETEST_STR_CR" + CLETEST_STR_CR
                            + tLine + "CLETEST_STR_CR" + CLETEST_STR_CR
                            + tLine + "CLETEST_STR_CR" + CLETEST_STR_CR;
                    break;

                case CLETEST_FTYPE_CRLF:
                    fText = tLine + "CLETEST_STR_CRLF" + CLETEST_STR_CRLF
                            + tLine + "CLETEST_STR_CRLF" + CLETEST_STR_CRLF
                            + tLine + "CLETEST_STR_CRLF" + CLETEST_STR_CRLF
                            + tLine + "CLETEST_STR_CRLF" + CLETEST_STR_CRLF
                            + tLine + "CLETEST_STR_CRLF" + CLETEST_STR_CRLF;
                    break;

                case CLETEST_FTYPE_LF:
                    fText = tLine + "CLETEST_STR_LF" + CLETEST_STR_LF
                            + tLine + "CLETEST_STR_LF" + CLETEST_STR_LF
                            + tLine + "CLETEST_STR_LF" + CLETEST_STR_LF
                            + tLine + "CLETEST_STR_LF" + CLETEST_STR_LF
                            + tLine + "CLETEST_STR_LF" + CLETEST_STR_LF;
                    break;

                default:
                    fText = tLine + "CLETEST_STR_LF" + CLETEST_STR_LF
                            + tLine + "CLETEST_STR_LF" + CLETEST_STR_LF;
                    break;
            }
            bw.write(fText);
            debugPrint("File: " + fName + " created.");

        } catch (Exception exc) {
            fail("Unexpected Exception: " + exc + " - " + exc.getLocalizedMessage());
        } finally {
            if (bw != null) {
                bw.close();
            }
        }

        return true;
    }


    public boolean checkSpecNeedsLineEndFiltering(ClientLineEnding cle, IClientSummary.ClientLineEnd clientLineEnd, boolean match) {

        boolean filterExpected = true;
        boolean needsFilter;

        needsFilter = ClientLineEnding.needsLineEndFiltering(cle);
        assertNotNull("CLE value must not be Null.", needsFilter);

        filterExpected = expectedCLFilterResult(clientLineEnd, cle);
        debugPrint("For " + clientLineEnd + " ClientSpec - " + cle + " needs filter?: ", "" + (match ? needsFilter : !needsFilter), "" + filterExpected);

        return match ? (needsFilter == filterExpected) : (needsFilter != filterExpected);


    }

    public boolean checkSpecNeedsLineEndFiltering(ClientLineEnding cle, IClientSummary.ClientLineEnd clientLineEnd) {
        return checkSpecNeedsLineEndFiltering(cle, clientLineEnd, true);
    }

    private String osPlatformLineEnding(final String osName) {
        if (osName.toLowerCase().contains("windows")) {
            return "CLETEST_STR_CRLF";
        }
        return "CLETEST_STR_LF";
    }

    @AfterClass
    public static void afterAll() throws Exception {
        afterEach(server);
    }


}