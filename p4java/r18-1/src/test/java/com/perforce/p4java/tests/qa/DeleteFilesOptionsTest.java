/*
NOT TESTING:
.applyRule 
.getOptions
.processFields
.processOptions
.setOptions
Old method with deleteNonSynced (old method doesn't support this)
*/

package com.perforce.p4java.tests.qa;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.option.client.DeleteFilesOptions;
import com.perforce.p4java.option.client.RevertFilesOptions;
import com.perforce.p4java.option.client.SyncOptions;
import com.perforce.p4java.option.server.OpenedFilesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.test.TestServer;

import org.junit.Ignore;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

@RunWith(JUnitPlatform.class)
public class DeleteFilesOptionsTest {

    private static TestServer ts = null;
    private static Helper helper = null;
    private static IClient client = null;
    private static IChangelist pendingChangelist = null;
    private static List<IFileSpec> syncedFileSpecs = null;
    private static List<IFileSpec> nonSyncedFileSpecs = null;
    private static DeleteFilesOptions deleteFilesOptions = null;
    private static Valids valids = null;

    @BeforeAll
    public static void beforeClass() throws Throwable {
        helper = new Helper();
        ts = new TestServer();
        ts.getServerExecutableSpecification().setCodeline(helper.getServerVersion());
        ts.start();

        IOptionsServer server = helper.getServer(ts);
        server.setUserName(ts.getUser());
        server.connect();

        IUser user = server.getUser(ts.getUser());

        client = helper.createClient(server, "client1");
        server.setCurrentClient(client);

        pendingChangelist = helper.createChangelist(server, user, client);
        syncedFileSpecs = helper.addFile(server, user, client, client.getRoot() + Helper.FILE_SEP + "synced.txt", "Testing DeleteFilesOptions", "text");
        nonSyncedFileSpecs = helper.addFile(server, user, client, client.getRoot() + Helper.FILE_SEP + "non-synced.txt", "Testing DeleteFilesOptions with non-synced", "text");
        List<IFileSpec> removedFileSpecs = client.sync(FileSpecBuilder.makeFileSpecList(nonSyncedFileSpecs.get(0).getOriginalPathString() + "#none"), new SyncOptions());
        helper.validateFileSpecs(removedFileSpecs);
    }

    @Test
    public void optionsSpecs() {
        assertThat(DeleteFilesOptions.OPTIONS_SPECS, is("i:c:clz b:n b:v b:k"));
    }

    @Test
    public void defaultConstructor() throws Throwable {
        deleteFilesOptions = new DeleteFilesOptions();
        valids = new Valids();
        testMethod(syncedFileSpecs, true);
        testMethod(nonSyncedFileSpecs, true);
        testMethod(syncedFileSpecs, false);
        testMethod(nonSyncedFileSpecs, false);
    }

    @Test
    public void explicitConstructorDefaults() throws Throwable {
        deleteFilesOptions = new DeleteFilesOptions(IChangelist.DEFAULT, false, false);
        valids = new Valids();
        testMethod(syncedFileSpecs, true);
        testMethod(nonSyncedFileSpecs, true);
        testMethod(syncedFileSpecs, false);
        testMethod(nonSyncedFileSpecs, false);

    }

    @Test
    public void explicitConstructorNoUpdate() throws Throwable {
        deleteFilesOptions = new DeleteFilesOptions(IChangelist.DEFAULT, true, false);
        valids = new Valids();
        valids.noUpdateGet = true;
        valids.noUpdate = true;
        testMethod(syncedFileSpecs, true);
        testMethod(nonSyncedFileSpecs, true);
        testMethod(syncedFileSpecs, false);
        testMethod(nonSyncedFileSpecs, false);
    }

    @Test
    public void stringConstructorNoUpdate() throws Throwable {
        deleteFilesOptions = new DeleteFilesOptions("-n");
        valids = new Valids();
        valids.immutable = true;
        valids.noUpdate = true;
        testMethod(syncedFileSpecs, false);
        testMethod(nonSyncedFileSpecs, false);
    }

    @Test
    public void explicitConstructorChangelistPending() throws Throwable {
        deleteFilesOptions = new DeleteFilesOptions(pendingChangelist.getId(), false, false);
        valids = new Valids();
        valids.changelistIdGet = pendingChangelist.getId();
        valids.changelistId = pendingChangelist.getId();
        testMethod(syncedFileSpecs, true);
        testMethod(nonSyncedFileSpecs, true);
        testMethod(syncedFileSpecs, false);
        testMethod(nonSyncedFileSpecs, false);
    }

    @Test
    public void stringConstructorChangelistPending() throws Throwable {
        deleteFilesOptions = new DeleteFilesOptions("-c" + pendingChangelist.getId());
        valids = new Valids();
        valids.immutable = true;
        valids.changelistId = pendingChangelist.getId();
        testMethod(syncedFileSpecs, false);
        testMethod(nonSyncedFileSpecs, false);
    }

    @Test
    public void explicitConstructorChangelistNegative() throws Throwable {
        deleteFilesOptions = new DeleteFilesOptions(IChangelist.UNKNOWN, false, false);
        valids = new Valids();
        valids.changelistIdGet = IChangelist.UNKNOWN;
        testMethod(syncedFileSpecs, true);
        testMethod(nonSyncedFileSpecs, true);
        testMethod(syncedFileSpecs, false);
        testMethod(nonSyncedFileSpecs, false);
    }

    @Test
    public void explicitConstructorDeleteNonSynced() throws Throwable {
        deleteFilesOptions = new DeleteFilesOptions(IChangelist.DEFAULT, false, true);
        valids = new Valids();
        valids.deleteNonSynced = true;
        valids.deleteNonSyncedGet = true;
        testMethod(syncedFileSpecs, false);
        testMethod(nonSyncedFileSpecs, false);
    }

    @Test
    public void stringConstructorDeleteNonSynced() throws Throwable {
        deleteFilesOptions = new DeleteFilesOptions("-v");
        valids = new Valids();
        valids.immutable = true;
        valids.deleteNonSynced = true;
        testMethod(syncedFileSpecs, false);
        testMethod(nonSyncedFileSpecs, false);
    }

    @Test
    public void explicitConstructorAll() throws Throwable {
        deleteFilesOptions = new DeleteFilesOptions(pendingChangelist.getId(), true, true);
        valids = new Valids();
        valids.noUpdateGet = true;
        valids.noUpdate = true;
        valids.changelistIdGet = pendingChangelist.getId();
        valids.changelistId = pendingChangelist.getId();
        valids.deleteNonSyncedGet = true;
        valids.deleteNonSynced = true;
        testMethod(syncedFileSpecs, true);
        testMethod(nonSyncedFileSpecs, true);
        testMethod(syncedFileSpecs, false);
        testMethod(nonSyncedFileSpecs, false);
    }

    @Test
    public void stringConstructorAll() throws Throwable {
        deleteFilesOptions = new DeleteFilesOptions("-c" + pendingChangelist.getId(), "-n", "-v");
        valids = new Valids();
        valids.immutable = true;
        valids.noUpdate = true;
        valids.changelistId = pendingChangelist.getId();
        valids.deleteNonSynced = true;
        testMethod(syncedFileSpecs, false);
        testMethod(nonSyncedFileSpecs, false);
    }

    @Test
    public void explicitConstructorAllExceptNoUpdate() throws Throwable {
        deleteFilesOptions = new DeleteFilesOptions(pendingChangelist.getId(), false, true);
        valids = new Valids();
        valids.changelistIdGet = pendingChangelist.getId();
        valids.changelistId = pendingChangelist.getId();
        valids.deleteNonSyncedGet = true;
        valids.deleteNonSynced = true;
        testMethod(syncedFileSpecs, false);
        testMethod(nonSyncedFileSpecs, false);
    }

    @Test
    public void stringConstructorAllExceptNoUpdate() throws Throwable {
        deleteFilesOptions = new DeleteFilesOptions("-c" + pendingChangelist.getId(), "-v");
        valids = new Valids();
        valids.immutable = true;
        valids.changelistId = pendingChangelist.getId();
        valids.deleteNonSynced = true;
        testMethod(syncedFileSpecs, false);
        testMethod(nonSyncedFileSpecs, false);
    }

    @Test
    public void setNoUpdateFalse() throws Throwable {
        deleteFilesOptions = new DeleteFilesOptions();
        deleteFilesOptions.setNoUpdate(false);
        valids = new Valids();
        testMethod(syncedFileSpecs, true);
        testMethod(nonSyncedFileSpecs, true);
        testMethod(syncedFileSpecs, false);
        testMethod(nonSyncedFileSpecs, false);
    }

    @Test
    public void setNoUpdateTrue() throws Throwable {
        deleteFilesOptions = new DeleteFilesOptions();
        deleteFilesOptions.setNoUpdate(true);
        valids = new Valids();
        valids.noUpdate = true;
        valids.noUpdateGet = true;
        testMethod(syncedFileSpecs, true);
        testMethod(nonSyncedFileSpecs, true);
        testMethod(syncedFileSpecs, false);
        testMethod(nonSyncedFileSpecs, false);
    }

    @Test
    public void setChangelistIdDefault() throws Throwable {
        deleteFilesOptions = new DeleteFilesOptions();
        deleteFilesOptions.setChangelistId(IChangelist.DEFAULT);
        valids = new Valids();
        testMethod(syncedFileSpecs, true);
        testMethod(nonSyncedFileSpecs, true);
        testMethod(syncedFileSpecs, false);
        testMethod(nonSyncedFileSpecs, false);
    }

    @Test
    public void setChangelistIdPending() throws Throwable {
        deleteFilesOptions = new DeleteFilesOptions();
        deleteFilesOptions.setChangelistId(pendingChangelist.getId());
        valids = new Valids();
        valids.changelistIdGet = pendingChangelist.getId();
        valids.changelistId = pendingChangelist.getId();
        testMethod(syncedFileSpecs, true);
        testMethod(nonSyncedFileSpecs, true);
        testMethod(syncedFileSpecs, false);
        testMethod(nonSyncedFileSpecs, false);
    }

    @Test
    public void setChangelistIdNegative() throws Throwable {
        deleteFilesOptions = new DeleteFilesOptions();
        deleteFilesOptions.setChangelistId(IChangelist.UNKNOWN);
        valids = new Valids();
        valids.changelistIdGet = IChangelist.UNKNOWN;
        testMethod(syncedFileSpecs, true);
        testMethod(nonSyncedFileSpecs, true);
        testMethod(syncedFileSpecs, false);
        testMethod(nonSyncedFileSpecs, false);
    }

    @Test
    public void setDeleteNonSyncedFalse() throws Throwable {
        deleteFilesOptions = new DeleteFilesOptions();
        deleteFilesOptions.setDeleteNonSyncedFiles(false);
        valids = new Valids();
        testMethod(syncedFileSpecs, true);
        testMethod(nonSyncedFileSpecs, true);
        testMethod(syncedFileSpecs, false);
        testMethod(nonSyncedFileSpecs, false);
    }

    @Test
    public void setDeleteNonSyncedTrue() throws Throwable {
        deleteFilesOptions = new DeleteFilesOptions();
        deleteFilesOptions.setDeleteNonSyncedFiles(true);
        valids = new Valids();
        valids.deleteNonSyncedGet = true;
        valids.deleteNonSynced = true;
        testMethod(syncedFileSpecs, false);
        testMethod(nonSyncedFileSpecs, false);
    }

    @Test
    public void setChainAll() throws Throwable {
        deleteFilesOptions = new DeleteFilesOptions();
        deleteFilesOptions.setNoUpdate(true).setChangelistId(pendingChangelist.getId()).setDeleteNonSyncedFiles(true);
        valids = new Valids();
        valids.noUpdateGet = true;
        valids.noUpdate = true;
        valids.changelistIdGet = pendingChangelist.getId();
        valids.changelistId = pendingChangelist.getId();
        valids.deleteNonSyncedGet = true;
        valids.deleteNonSynced = true;
        testMethod(syncedFileSpecs, true);
        testMethod(nonSyncedFileSpecs, true);
        testMethod(syncedFileSpecs, false);
        testMethod(nonSyncedFileSpecs, false);
    }

    @Test
    public void overrideStringConstructor() throws Throwable {
        deleteFilesOptions = new DeleteFilesOptions("-c" + pendingChangelist.getId(), "-n", "-v");
        deleteFilesOptions.setNoUpdate(false);
        deleteFilesOptions.setChangelistId(IChangelist.DEFAULT);
        deleteFilesOptions.setDeleteNonSyncedFiles(false);
        valids = new Valids();
        valids.immutable = true;
        valids.noUpdate = true;
        valids.changelistId = pendingChangelist.getId();
        valids.deleteNonSynced = true;
        testMethod(syncedFileSpecs, false);
        testMethod(nonSyncedFileSpecs, false);
    }

    @Test
    public void setImmutableFalseNoUpdate() throws Throwable {
        deleteFilesOptions = new DeleteFilesOptions();
        deleteFilesOptions.setImmutable(false);
        valids = new Valids();
        testMethod(syncedFileSpecs, false);
        testMethod(nonSyncedFileSpecs, false);
        deleteFilesOptions.setNoUpdate(true);
        valids = new Valids();
        valids.noUpdateGet = true;
        valids.noUpdate = true;
        testMethod(syncedFileSpecs, false);
        testMethod(nonSyncedFileSpecs, false);
    }

    @Test
    public void setImmutableTrueNoUpdate() throws Throwable {
        deleteFilesOptions = new DeleteFilesOptions();
        deleteFilesOptions.setImmutable(true);
        valids = new Valids();
        valids.immutable = true;
        testMethod(syncedFileSpecs, false);
        testMethod(nonSyncedFileSpecs, false);
        deleteFilesOptions.setNoUpdate(true);
        valids = new Valids();
        valids.immutable = true;
        valids.noUpdateGet = true;
        testMethod(syncedFileSpecs, false);
        testMethod(nonSyncedFileSpecs, false);
    }

    @Test
    public void setImmutableFalseChangelist() throws Throwable {
        deleteFilesOptions = new DeleteFilesOptions();
        deleteFilesOptions.setImmutable(false);
        valids = new Valids();
        testMethod(syncedFileSpecs, false);
        testMethod(nonSyncedFileSpecs, false);
        deleteFilesOptions.setChangelistId(pendingChangelist.getId());
        valids = new Valids();
        valids.changelistIdGet = pendingChangelist.getId();
        valids.changelistId = pendingChangelist.getId();
        testMethod(syncedFileSpecs, false);
        testMethod(nonSyncedFileSpecs, false);
    }


    @Test
    public void setImmutableTrueChangelist() throws Throwable {
        deleteFilesOptions = new DeleteFilesOptions();
        deleteFilesOptions.setImmutable(true);
        valids = new Valids();
        valids.immutable = true;
        testMethod(syncedFileSpecs, false);
        testMethod(nonSyncedFileSpecs, false);
        deleteFilesOptions.setChangelistId(pendingChangelist.getId());
        valids = new Valids();
        valids.immutable = true;
        valids.changelistIdGet = pendingChangelist.getId();
        testMethod(syncedFileSpecs, false);
        testMethod(nonSyncedFileSpecs, false);
    }

    @Test
    public void setImmutableFalseDeleteNonSynced() throws Throwable {
        deleteFilesOptions = new DeleteFilesOptions();
        deleteFilesOptions.setImmutable(false);
        valids = new Valids();
        testMethod(syncedFileSpecs, false);
        testMethod(nonSyncedFileSpecs, false);
        deleteFilesOptions.setDeleteNonSyncedFiles(true);
        valids = new Valids();
        valids.deleteNonSyncedGet = true;
        valids.deleteNonSynced = true;
        testMethod(syncedFileSpecs, false);
        testMethod(nonSyncedFileSpecs, false);
    }


    @Test
    public void setImmutableTrueDeleteNonSynced() throws Throwable {
        deleteFilesOptions = new DeleteFilesOptions();
        deleteFilesOptions.setImmutable(true);
        valids = new Valids();
        valids.immutable = true;
        testMethod(syncedFileSpecs, false);
        testMethod(nonSyncedFileSpecs, false);
        deleteFilesOptions.setDeleteNonSyncedFiles(true);
        valids = new Valids();
        valids.immutable = true;
        valids.deleteNonSyncedGet = true;
        testMethod(syncedFileSpecs, false);
        testMethod(nonSyncedFileSpecs, false);
    }

    @AfterAll
    public static void afterClass() {
        helper.after(ts);
    }

    private static void testMethod(List<IFileSpec> fileSpecs, boolean useOldMethod) throws Throwable {
        assertThat( deleteFilesOptions.isImmutable(), is(valids.immutable));
        assertThat( deleteFilesOptions.isNoUpdate(), is(valids.noUpdateGet));
        assertThat( deleteFilesOptions.getChangelistId(), is(valids.changelistIdGet));
        assertThat( deleteFilesOptions.isDeleteNonSyncedFiles(), is(valids.deleteNonSyncedGet));

        client.revertFiles(FileSpecBuilder.makeFileSpecList("//depot/..."), new RevertFilesOptions());

        List<IFileSpec> deletedFileSpecs;

        if (useOldMethod) {
            deletedFileSpecs = client.deleteFiles(fileSpecs, deleteFilesOptions.getChangelistId(), deleteFilesOptions.isNoUpdate());
        } else {
            deletedFileSpecs = client.deleteFiles(fileSpecs, deleteFilesOptions);
        }

        List<IFileSpec> openedFileSpecs = client.openedFiles(null, new OpenedFilesOptions());
        helper.validateFileSpecs(openedFileSpecs);
        if ((valids.noUpdate) || (fileSpecs.equals(nonSyncedFileSpecs) && !valids.deleteNonSynced)) {
            assertThat( openedFileSpecs.size(), is(0));
        } else {
            helper.validateFileSpecs(deletedFileSpecs);
            assertThat(openedFileSpecs.size() > 0, is(true));
            for (IFileSpec openedFileSpec : openedFileSpecs) {
                assertThat( openedFileSpec.getChangelistId(), is(valids.changelistId));
            }
        }
    }
    @Ignore
    private static class Valids {
        private boolean immutable = false;
        private boolean noUpdateGet = false;
        private boolean noUpdate = false;
        private int changelistIdGet = IChangelist.DEFAULT;
        private int changelistId = IChangelist.DEFAULT;
        private boolean deleteNonSyncedGet = false;
        private boolean deleteNonSynced = false;
    }

}