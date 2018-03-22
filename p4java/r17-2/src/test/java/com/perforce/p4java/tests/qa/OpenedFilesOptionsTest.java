/*
NOT TESTING:
.applyRule 
.getOptions
.processFields
.processOptions
.setOptions
Old method only supports maxFiles and changelistId
allClients and clientName don't make any sense
*/

package com.perforce.p4java.tests.qa;


import static com.perforce.p4java.core.IChangelist.DEFAULT;
import static com.perforce.p4java.core.IChangelist.UNKNOWN;
import static com.perforce.p4java.option.server.OpenedFilesOptions.OPTIONS_SPECS;
import static com.perforce.p4java.tests.qa.Helper.FILE_SEP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.option.client.AddFilesOptions;
import com.perforce.p4java.option.server.OpenedFilesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.test.TestServer;


public class OpenedFilesOptionsTest {

    private static TestServer ts = null;
    private static Helper h = null;
    private static IOptionsServer server = null;
    private static IUser user = null;
    private static IUser user2 = null;
    private static IClient client = null;
    private static IClient client2 = null;
    private static IChangelist pendingChangelist = null;
    private static OpenedFilesOptions openedFilesOptions = null;
    private static Valids valids = null;
    private static List<IFileSpec> notOpenedUser1Client1FileSpecs = null;
    private static List<IFileSpec> openedDefault1User1Client1FileSpecs = null;
    private static List<IFileSpec> openedDefault2User1Client1FileSpecs = null;
    private static List<IFileSpec> openedPendingUser1Client1FileSpecs = null;
    private static List<IFileSpec> openedDefaultUser2Client1FileSpecs = null;
    private static List<IFileSpec> openedDefaultUser1Client2FileSpecs = null;

    @BeforeClass
    public static void beforeClass() throws Throwable {
        h = new Helper();
        ts = new TestServer();
        ts.getServerExecutableSpecification().setCodeline(h.getServerVersion());
        ts.start();

        server = h.getServer(ts);
        server.setUserName(ts.getUser());
        server.connect();

        user = server.getUser(ts.getUser());

        client = h.createClient(server, "client1");
        server.setCurrentClient(client);

        AddFilesOptions afo = new AddFilesOptions();
        afo.setFileType("text");

        notOpenedUser1Client1FileSpecs = h.addFile(server, user, client, client.getRoot() + FILE_SEP + "not-opened.txt", "OpenedFilesOptions", "text");

        openedDefault1User1Client1FileSpecs = h.createFile(client.getRoot() + FILE_SEP + "opened-default1-user1-client1.txt", "OpenedFilesOptions");
        h.validateFileSpecs(client.addFiles(openedDefault1User1Client1FileSpecs, afo));

        openedDefault2User1Client1FileSpecs = h.createFile(client.getRoot() + FILE_SEP + "opened-default2-user1-client1.txt", "OpenedFilesOptions");
        h.validateFileSpecs(client.addFiles(openedDefault2User1Client1FileSpecs, afo));

        pendingChangelist = h.createChangelist(server, user, client);

        openedPendingUser1Client1FileSpecs = h.createFile(client.getRoot() + FILE_SEP + "opened-pending-user1-client1.txt", "OpenedFilesOptions");
        h.validateFileSpecs(client.addFiles(openedPendingUser1Client1FileSpecs, new AddFilesOptions().setChangelistId(pendingChangelist.getId()).setFileType("text")));

        client2 = h.createClient(server, "client2", user, null, null);
        server.setCurrentClient(client2);

        openedDefaultUser1Client2FileSpecs = h.createFile(client2.getRoot() + FILE_SEP + "opened-default-user1-client2.txt", "OpenedFilesOptions");
        h.validateFileSpecs(client.addFiles(openedDefaultUser1Client2FileSpecs, afo));

        user2 = h.createUser(server, "user2");
        server.setUserName(user2.getLoginName());
        server.setCurrentClient(client);

        openedDefaultUser2Client1FileSpecs = h.createFile(client.getRoot() + FILE_SEP + "opened-default-user2-client1.txt", "OpenedFilesOptions");
        h.validateFileSpecs(client.addFiles(openedDefaultUser2Client1FileSpecs, afo));
    }


    // OPTIONS SPECS
    @Test
    public void optionsSpecs() throws Throwable {
        assertEquals("b:a i:c:cl s:C s:u i:m:gtz b:s", OPTIONS_SPECS);
    }


    // DEFAULTS
    @Test
    public void defaultConstructor() throws Throwable {
        openedFilesOptions = new OpenedFilesOptions();
        valids = new Valids();
        testMethod(true);
        testMethod(false);
    }

    @Test
    public void explicitConstructorDefaults() throws Throwable {
        openedFilesOptions = new OpenedFilesOptions(false, null, 0, null, UNKNOWN);
        valids = new Valids();
        testMethod(true);
        testMethod(false);
    }


    // ALL CLIENTS
    @Test
    public void explicitConstructorAllClients() throws Throwable {
        openedFilesOptions = new OpenedFilesOptions(true, null, 0, null, UNKNOWN);
        valids = new Valids();
        valids.allClientsGet = true;
        valids.allClients = true;
        testMethod(false);
    }

    @Test
    public void setAllClients() throws Throwable {
        openedFilesOptions = new OpenedFilesOptions();
        openedFilesOptions.setAllClients(true);
        valids = new Valids();
        valids.allClientsGet = true;
        valids.allClients = true;
        testMethod(false);
    }

    @Test
    public void stringConstructorAllClients() throws Throwable {
        openedFilesOptions = new OpenedFilesOptions("-a");
        valids = new Valids();
        valids.immutable = true;
        valids.allClients = true;
        testMethod(false);
    }

    @Test
    public void setImmutableFalseAllClients() throws Throwable {
        openedFilesOptions = new OpenedFilesOptions();
        openedFilesOptions.setImmutable(false);
        valids = new Valids();
        testMethod(false);
        openedFilesOptions.setAllClients(true);
        valids = new Valids();
        valids.allClientsGet = true;
        valids.allClients = true;
        valids.clientName = "client1";
        valids.clientNameGet = "client1";
        testMethod(false);
    }

    @Test
    public void setImmutableTrueAllClients() throws Throwable {
        openedFilesOptions = new OpenedFilesOptions();
        openedFilesOptions.setImmutable(true);
        valids = new Valids();
        valids.immutable = true;
        testMethod(false);
        openedFilesOptions.setAllClients(true);
        valids = new Valids();
        valids.immutable = true;
        valids.allClientsGet = true;
        valids.clientName = "client1";
        valids.clientNameGet = "client1";
        testMethod(false);
    }


    // CHANGELIST
    @Test
    public void explicitConstructorChangelistIdDefault() throws Throwable {
        openedFilesOptions = new OpenedFilesOptions(false, null, 0, null, DEFAULT);
        valids = new Valids();
        valids.changelistIdGet = DEFAULT;
        valids.changelistId = DEFAULT;
        testMethod(true);
        testMethod(false);
    }

    @Test
    public void explicitConstructorChangelistIdPending() throws Throwable {
        openedFilesOptions = new OpenedFilesOptions(false, null, 0, null, pendingChangelist.getId());
        valids = new Valids();
        valids.changelistIdGet = pendingChangelist.getId();
        valids.changelistId = pendingChangelist.getId();
        testMethod(true);
        testMethod(false);
    }


    @Test
    public void setChangelistId() throws Throwable {
        openedFilesOptions = new OpenedFilesOptions();
        openedFilesOptions.setChangelistId(pendingChangelist.getId());
        valids = new Valids();
        valids.changelistIdGet = pendingChangelist.getId();
        valids.changelistId = pendingChangelist.getId();
        testMethod(true);
        testMethod(false);
    }

    @Test
    public void stringConstructorChangelistId() throws Throwable {
        openedFilesOptions = new OpenedFilesOptions("-c" + pendingChangelist.getId());
        valids = new Valids();
        valids.immutable = true;
        valids.changelistId = pendingChangelist.getId();
        testMethod(false);
    }

    @Test
    public void setImmutableFalseChangelistId() throws Throwable {
        openedFilesOptions = new OpenedFilesOptions();
        openedFilesOptions.setImmutable(false);
        valids = new Valids();
        testMethod(false);
        openedFilesOptions.setChangelistId(pendingChangelist.getId());
        valids = new Valids();
        valids.changelistIdGet = pendingChangelist.getId();
        valids.clientName = "client1";
        valids.clientNameGet = "client1";
        testMethod(true);
        testMethod(false);
    }

    @Test
    public void setImmutableTrueChangelistId() throws Throwable {
        openedFilesOptions = new OpenedFilesOptions();
        openedFilesOptions.setImmutable(true);
        valids = new Valids();
        valids.immutable = true;
        testMethod(false);
        openedFilesOptions.setChangelistId(pendingChangelist.getId());
        valids = new Valids();
        valids.immutable = true;
        valids.changelistIdGet = pendingChangelist.getId();
        valids.clientName = "client1";
        valids.clientNameGet = "client1";
        testMethod(true);
        testMethod(false);
    }


    // MAX FILES
    @Test
    public void explicitConstructorMaxFiles() throws Throwable {
        openedFilesOptions = new OpenedFilesOptions(false, null, 1, null, UNKNOWN);
        valids = new Valids();
        valids.maxFilesGet = 1;
        valids.maxFiles = 1;
        testMethod(true);
        testMethod(false);
    }

    @Test
    public void explicitConstructorMaxFilesNegative() throws Throwable {
        openedFilesOptions = new OpenedFilesOptions(false, null, -1, null, UNKNOWN);
        valids = new Valids();
        valids.maxFilesGet = -1;
        testMethod(true);
        testMethod(false);
    }


    @Test
    public void setMaxFiles() throws Throwable {
        openedFilesOptions = new OpenedFilesOptions();
        openedFilesOptions.setMaxFiles(1);
        valids = new Valids();
        valids.maxFilesGet = 1;
        valids.maxFiles = 1;
        testMethod(true);
        testMethod(false);
    }

    @Test
    public void stringConstructorMaxFiles() throws Throwable {
        openedFilesOptions = new OpenedFilesOptions("-m 1");
        valids = new Valids();
        valids.immutable = true;
        valids.maxFiles = 1;
        testMethod(false);
    }

    @Test
    public void setImmutableFalseMaxFiles() throws Throwable {
        openedFilesOptions = new OpenedFilesOptions();
        openedFilesOptions.setImmutable(false);
        valids = new Valids();
        testMethod(false);
        openedFilesOptions.setMaxFiles(1);
        valids = new Valids();
        valids.maxFilesGet = 1;
        valids.clientName = "client1";
        valids.clientNameGet = "client1";
        testMethod(true);
        testMethod(false);
    }

    @Test
    public void setImmutableTrueMaxFiles() throws Throwable {
        openedFilesOptions = new OpenedFilesOptions();
        openedFilesOptions.setImmutable(true);
        valids = new Valids();
        valids.immutable = true;
        testMethod(false);
        openedFilesOptions.setMaxFiles(1);
        valids = new Valids();
        valids.immutable = true;
        valids.maxFilesGet = 1;
        valids.clientName = "client1";
        valids.clientNameGet = "client1";
        testMethod(true);
        testMethod(false);
    }


    // USER NAME
    @Test
    public void explicitConstructorUserName() throws Throwable {
        openedFilesOptions = new OpenedFilesOptions(false, null, 0, user2.getLoginName(), UNKNOWN);
        valids = new Valids();
        valids.userNameGet = user2.getLoginName();
        valids.userName = user2.getLoginName();
        testMethod(false);
    }

    @Test
    public void explicitConstructorUserNameNonExistent() throws Throwable {
        openedFilesOptions = new OpenedFilesOptions(false, null, 0, "dne", UNKNOWN);
        valids = new Valids();
        valids.userNameGet = "dne";
        testMethod(false);
    }


    @Test
    public void setUserName() throws Throwable {
        openedFilesOptions = new OpenedFilesOptions();
        openedFilesOptions.setUserName(user2.getLoginName());
        valids = new Valids();
        valids.userNameGet = user2.getLoginName();
        valids.userName = user2.getLoginName();
        testMethod(false);
    }

    @Test
    public void stringConstructorUserName() throws Throwable {
        openedFilesOptions = new OpenedFilesOptions("-u " + user2.getLoginName());
        valids = new Valids();
        valids.immutable = true;
        valids.userName = user2.getLoginName();
        testMethod(false);
    }

    @Test
    public void setImmutableFalseUserName() throws Throwable {
        openedFilesOptions = new OpenedFilesOptions();
        openedFilesOptions.setImmutable(false);
        valids = new Valids();
        testMethod(false);
        openedFilesOptions.setUserName(user2.getLoginName());
        valids = new Valids();
        valids.userNameGet = user2.getLoginName();
        valids.clientName = "client1";
        valids.clientNameGet = "client1";
        testMethod(false);
    }

    @Test
    public void setImmutableTrueUserName() throws Throwable {
        openedFilesOptions = new OpenedFilesOptions();
        openedFilesOptions.setImmutable(true);
        valids = new Valids();
        valids.immutable = true;
        testMethod(false);
        openedFilesOptions.setUserName(user2.getLoginName());
        valids = new Valids();
        valids.immutable = true;
        valids.userNameGet = user2.getLoginName();
        valids.clientName = "client1";
        valids.clientNameGet = "client1";
        testMethod(false);
    }


    // CLIENT NAME
    @Test
    public void explicitConstructorClientName() throws Throwable {
        openedFilesOptions = new OpenedFilesOptions(false, client2.getName(), 0, null, UNKNOWN);
        valids = new Valids();
        valids.clientNameGet = client2.getName();
        valids.clientName = client2.getName();
        testMethod(false);
    }

    @Test
    public void explicitConstructorClientNameNonExistent() throws Throwable {
        openedFilesOptions = new OpenedFilesOptions(false, "dne", 0, null, UNKNOWN);
        valids = new Valids();
        valids.clientNameGet = "dne";
        testMethod(false);
    }


    @Test
    public void setClientName() throws Throwable {
        openedFilesOptions = new OpenedFilesOptions();
        openedFilesOptions.setClientName(client2.getName());
        valids = new Valids();
        valids.clientNameGet = client2.getName();
        valids.clientName = client2.getName();
        testMethod(false);
    }

    @Test
    public void stringConstructorClientName() throws Throwable {
        openedFilesOptions = new OpenedFilesOptions("-C " + client2.getName());
        valids = new Valids();
        valids.immutable = true;
        valids.clientName = client2.getName();
        testMethod(false);
    }

    @Test
    public void setImmutableFalseClientName() throws Throwable {
        openedFilesOptions = new OpenedFilesOptions();
        openedFilesOptions.setImmutable(false);
        valids = new Valids();
        testMethod(false);
        openedFilesOptions.setClientName(client2.getName());
        valids = new Valids();
        valids.clientNameGet = client2.getName();
        valids.clientName = client2.getName();
        testMethod(false);
    }

    @Test
    public void setImmutableTrueClientName() throws Throwable {
        openedFilesOptions = new OpenedFilesOptions();
        openedFilesOptions.setImmutable(true);
        valids = new Valids();
        valids.immutable = true;
        testMethod(false);
        openedFilesOptions.setClientName(client2.getName());
        valids = new Valids();
        valids.immutable = true;
        valids.clientNameGet = client2.getName();
        testMethod(false);
    }


    // ALL
    @Test
    public void explicitConstructorAll() throws Throwable {
        openedFilesOptions = new OpenedFilesOptions(true, client2.getName(), 1, user.getLoginName(), DEFAULT);
        valids = new Valids();
        valids.allClientsGet = true;
        valids.allClients = true;
        valids.maxFilesGet = 1;
        valids.maxFiles = 1;
        valids.userNameGet = user.getLoginName();
        valids.userName = user.getLoginName();
        valids.changelistIdGet = DEFAULT;
        valids.changelistId = DEFAULT;
        valids.clientNameGet = client2.getName();
        valids.clientName = client2.getName();
        testMethod(false);
    }


    // SETTER RETURNS
    @Test
    public void setterReturns() throws Throwable {
        openedFilesOptions = new OpenedFilesOptions();
        assertEquals(OpenedFilesOptions.class, openedFilesOptions.setAllClients(true).getClass());
        assertEquals(OpenedFilesOptions.class, openedFilesOptions.setChangelistId(DEFAULT).getClass());
        assertEquals(OpenedFilesOptions.class, openedFilesOptions.setClientName("asdf").getClass());
        assertEquals(OpenedFilesOptions.class, openedFilesOptions.setMaxFiles(0).getClass());
        assertEquals(OpenedFilesOptions.class, openedFilesOptions.setUserName("asdf").getClass());
    }


    // OVERRIDE STRING CONSTRUCTOR
    @Test
    public void overrideStringConstructor() throws Throwable {
        openedFilesOptions = new OpenedFilesOptions("-c" + pendingChangelist.getId(), "-m1", "-u" + user.getLoginName(), "-a", "-C" + client.getName());
        openedFilesOptions.setChangelistId(DEFAULT);
        openedFilesOptions.setMaxFiles(0);
        openedFilesOptions.setAllClients(false);
        openedFilesOptions.setClientName(client2.getName());
        openedFilesOptions.setUserName(user2.getLoginName());
        valids = new Valids();
        valids.immutable = true;
        valids.changelistIdGet = DEFAULT;
        valids.changelistId = pendingChangelist.getId();
        valids.maxFilesGet = 0;
        valids.maxFiles = 1;
        valids.userNameGet = user2.getLoginName();
        valids.userName = user.getLoginName();
        valids.allClientsGet = false;
        valids.allClients = true;
        valids.clientNameGet = client2.getName();
        valids.clientName = client.getName();
        testMethod(false);
    }


    @AfterClass
    public static void afterClass() {
        h.after(ts);
    }

    private static void testMethod(boolean useOldMethod) throws Throwable {

        assertEquals(valids.immutable, openedFilesOptions.isImmutable());
        assertEquals(valids.allClientsGet, openedFilesOptions.isAllClients());
        assertEquals(valids.changelistIdGet, openedFilesOptions.getChangelistId());
        assertEquals(valids.clientNameGet, openedFilesOptions.getClientName());
        assertEquals(valids.maxFilesGet, openedFilesOptions.getMaxFiles());
        assertEquals(valids.userNameGet, openedFilesOptions.getUserName());

        List<IFileSpec> openedFiles = null;

        if (useOldMethod) {

            openedFiles = client.openedFiles(null, openedFilesOptions.getMaxFiles(), openedFilesOptions.getChangelistId());

        } else {

            openedFiles = client.openedFiles(null, openedFilesOptions);

        }

        h.validateFileSpecs(openedFiles);

        for (IFileSpec openedFile : openedFiles) {

            assertTrue(openedFile.getDepotPathString() != notOpenedUser1Client1FileSpecs.get(0).getOriginalPathString());
            assertTrue(openedFile.getClientName() != client2.getName());

        }

        if (valids.userName != null) {

            for (IFileSpec openedFile : openedFiles) {

                assertEquals(valids.userName, openedFile.getUserName());

            }

        }

        if (valids.maxFiles != 0) {

            assertEquals(valids.maxFiles, openedFiles.size());

        }

        if (valids.changelistId != UNKNOWN) {

            for (IFileSpec openedFile : openedFiles) {

                assertEquals(valids.changelistId, openedFile.getChangelistId());

            }

        }

    }

    @Ignore
    private static class Valids {

        private boolean immutable = false;
        private boolean allClientsGet = false;
        @SuppressWarnings("unused")
        private boolean allClients = false;
        private int changelistIdGet = UNKNOWN;
        private int changelistId = UNKNOWN;
        private String clientNameGet = null;
        @SuppressWarnings("unused")
        private String clientName = null;
        private int maxFilesGet = 0;
        private int maxFiles = 0;
        private String userNameGet = null;
        private String userName = null;

    }

}