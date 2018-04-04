/*
NOT TESTING:
.applyRule 
.getOptions
.processFields
.processOptions
.setOptions
*/

package com.perforce.p4java.tests.qa;


import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.option.client.EditFilesOptions;
import com.perforce.p4java.option.client.RevertFilesOptions;
import com.perforce.p4java.option.server.OpenedFilesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.test.TestServer;

import org.junit.Ignore;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.List;

import static com.perforce.p4java.core.IChangelist.DEFAULT;
import static com.perforce.p4java.core.IChangelist.UNKNOWN;
import static com.perforce.p4java.core.file.FileSpecBuilder.makeFileSpecList;
import static com.perforce.p4java.option.client.EditFilesOptions.OPTIONS_SPECS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

@RunWith(JUnitPlatform.class)
public class EditFilesOptionsTest {

    private static TestServer ts = null;
    private static Helper helper = null;
    private static IClient client = null;
    private static IChangelist pendingChangelist = null;
    private static File testFile = null;
    private static List<IFileSpec> testFileSpecs = null;
    private static EditFilesOptions editFilesOptions = null;
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
        testFile = new File(client.getRoot(), "file.txt");
        testFileSpecs = helper.addFile(server, user, client, testFile.getAbsolutePath(), "Testing EditFilesOptions", "text");
    }

    @Test
    public void optionsSpecs() throws Throwable {
        assertThat(OPTIONS_SPECS, is("b:n b:k i:c:gtz s:t s:Q"));
    }

    @Test
    public void defaultConstructor() throws Throwable {
        editFilesOptions = new EditFilesOptions();
        valids = new Valids();
        testMethod(testFileSpecs, true);
        testMethod(testFileSpecs, false);
    }

    @Test
    public void explicitConstructorDefaults() throws Throwable {
        editFilesOptions = new EditFilesOptions(false, false, DEFAULT, null);
        valids = new Valids();
        testMethod(testFileSpecs, true);
        testMethod(testFileSpecs, false);
    }

    @Test
    public void explicitConstructorNoUpdate() throws Throwable {
        editFilesOptions = new EditFilesOptions(true, false, DEFAULT, null);
        valids = new Valids();
        valids.noUpdateGet = true;
        valids.noUpdate = true;
        testMethod(testFileSpecs, true);
        testMethod(testFileSpecs, false);
    }

    @Test
    public void stringConstructorNoUpdate() throws Throwable {
        editFilesOptions = new EditFilesOptions("-n");
        valids = new Valids();
        valids.immutable = true;
        valids.noUpdate = true;
        testMethod(testFileSpecs, false);
    }

    @Test
    public void explicitConstructorChangelistPending() throws Throwable {
        editFilesOptions = new EditFilesOptions(false, false, pendingChangelist.getId(), null);
        valids = new Valids();
        valids.changelistIdGet = pendingChangelist.getId();
        valids.changelistId = pendingChangelist.getId();
        testMethod(testFileSpecs, true);
        testMethod(testFileSpecs, false);
    }

    @Test
    public void stringConstructorChangelistPending() throws Throwable {
        editFilesOptions = new EditFilesOptions("-c" + pendingChangelist.getId());
        valids = new Valids();
        valids.immutable = true;
        valids.changelistId = pendingChangelist.getId();
        testMethod(testFileSpecs, false);
    }

    @Test
    public void explicitConstructorChangelistNegative() throws Throwable {
        editFilesOptions = new EditFilesOptions(false, false, UNKNOWN, null);
        valids = new Valids();
        valids.changelistIdGet = UNKNOWN;
        testMethod(testFileSpecs, true);
        testMethod(testFileSpecs, false);
    }

    @Test
    public void explicitConstructorFileType() throws Throwable {
        editFilesOptions = new EditFilesOptions(false, false, DEFAULT, "binary");
        valids = new Valids();
        valids.fileType = "binary";
        valids.fileTypeGet = "binary";
        testMethod(testFileSpecs, true);
        testMethod(testFileSpecs, false);
    }

    @Test
    public void stringConstructorFileType() throws Throwable {
        editFilesOptions = new EditFilesOptions("-tbinary");
        valids = new Valids();
        valids.immutable = true;
        valids.fileType = "binary";
        testMethod(testFileSpecs, false);
    }

    @Test
    public void explicitConstructorBypassClientUpdate() throws Throwable {
        editFilesOptions = new EditFilesOptions(false, true, DEFAULT, null);
        valids = new Valids();
        valids.bypassClientUpdate = true;
        valids.bypassClientUpdateGet = true;
        testMethod(testFileSpecs, true);
        testMethod(testFileSpecs, false);
    }

    @Test
    public void stringConstructorBypassClientUpdate() throws Throwable {
        editFilesOptions = new EditFilesOptions("-k");
        valids = new Valids();
        valids.immutable = true;
        valids.bypassClientUpdate = true;
        testMethod(testFileSpecs, false);
    }

    @Test
    public void explicitConstructorAll() throws Throwable {
        editFilesOptions = new EditFilesOptions(true, true, pendingChangelist.getId(), "binary");
        valids = new Valids();
        valids.noUpdateGet = true;
        valids.noUpdate = true;
        valids.changelistIdGet = pendingChangelist.getId();
        valids.changelistId = pendingChangelist.getId();
        valids.fileTypeGet = "binary";
        valids.fileType = "binary";
        valids.bypassClientUpdateGet = true;
        valids.bypassClientUpdate = true;
        testMethod(testFileSpecs, true);
        testMethod(testFileSpecs, false);
    }

    @Test
    public void stringConstructorAll() throws Throwable {
        editFilesOptions = new EditFilesOptions("-n", "-k", "-c" + pendingChangelist.getId(), "-tbinary");
        valids = new Valids();
        valids.immutable = true;
        valids.noUpdate = true;
        valids.changelistId = pendingChangelist.getId();
        valids.fileType = "binary";
        valids.bypassClientUpdate = true;
        testMethod(testFileSpecs, false);
    }

    @Test
    public void explicitConstructorAllExceptNoUpdate() throws Throwable {
        editFilesOptions = new EditFilesOptions(false, true, pendingChangelist.getId(), "binary");
        valids = new Valids();
        valids.changelistIdGet = pendingChangelist.getId();
        valids.changelistId = pendingChangelist.getId();
        valids.fileTypeGet = "binary";
        valids.fileType = "binary";
        valids.bypassClientUpdateGet = true;
        valids.bypassClientUpdate = true;
        testMethod(testFileSpecs, true);
        testMethod(testFileSpecs, false);
    }

    @Test
    public void stringConstructorAllExceptNoUpdate() throws Throwable {
        editFilesOptions = new EditFilesOptions("-k", "-c" + pendingChangelist.getId(), "-tbinary");
        valids = new Valids();
        valids.immutable = true;
        valids.changelistId = pendingChangelist.getId();
        valids.fileType = "binary";
        valids.bypassClientUpdate = true;
        testMethod(testFileSpecs, false);
    }

    @Test
    public void setNoUpdateFalse() throws Throwable {
        editFilesOptions = new EditFilesOptions();
        editFilesOptions.setNoUpdate(false);
        valids = new Valids();
        testMethod(testFileSpecs, true);
        testMethod(testFileSpecs, false);
    }

    @Test
    public void setNoUpdateTrue() throws Throwable {
        editFilesOptions = new EditFilesOptions();
        editFilesOptions.setNoUpdate(true);
        valids = new Valids();
        valids.noUpdate = true;
        valids.noUpdateGet = true;
        testMethod(testFileSpecs, true);
        testMethod(testFileSpecs, false);
    }

    @Test
    public void setChangelistIdDefault() throws Throwable {
        editFilesOptions = new EditFilesOptions();
        editFilesOptions.setChangelistId(DEFAULT);
        valids = new Valids();
        testMethod(testFileSpecs, true);
        testMethod(testFileSpecs, false);
    }

    @Test
    public void setChangelistIdPending() throws Throwable {
        editFilesOptions = new EditFilesOptions();
        editFilesOptions.setChangelistId(pendingChangelist.getId());
        valids = new Valids();
        valids.changelistIdGet = pendingChangelist.getId();
        valids.changelistId = pendingChangelist.getId();
        testMethod(testFileSpecs, true);
        testMethod(testFileSpecs, false);
    }

    @Test
    public void setChangelistIdNegative() throws Throwable {
        editFilesOptions = new EditFilesOptions();
        editFilesOptions.setChangelistId(UNKNOWN);
        valids = new Valids();
        valids.changelistIdGet = UNKNOWN;
        testMethod(testFileSpecs, true);
        testMethod(testFileSpecs, false);
    }

    @Test
    public void setFileTypeNull() throws Throwable {
        editFilesOptions = new EditFilesOptions();
        editFilesOptions.setFileType(null);
        valids = new Valids();
        testMethod(testFileSpecs, true);
        testMethod(testFileSpecs, false);
    }

    @Test
    public void setFileTypeBinary() throws Throwable {
        editFilesOptions = new EditFilesOptions();
        editFilesOptions.setFileType("binary");
        valids = new Valids();
        valids.fileTypeGet = "binary";
        valids.fileType = "binary";
        testMethod(testFileSpecs, true);
        testMethod(testFileSpecs, false);
    }

    @Test
    public void setBypassClientUpdateFalse() throws Throwable {
        editFilesOptions = new EditFilesOptions();
        editFilesOptions.setBypassClientUpdate(false);
        valids = new Valids();
        testMethod(testFileSpecs, true);
        testMethod(testFileSpecs, false);
    }

    @Test
    public void setBypassClientUpdateTrue() throws Throwable {
        editFilesOptions = new EditFilesOptions();
        editFilesOptions.setBypassClientUpdate(true);
        valids = new Valids();
        valids.bypassClientUpdateGet = true;
        valids.bypassClientUpdate = true;
        testMethod(testFileSpecs, true);
        testMethod(testFileSpecs, false);
    }

    @Test
    public void setChainAll() throws Throwable {
        editFilesOptions = new EditFilesOptions();
        editFilesOptions.setNoUpdate(true).setChangelistId(pendingChangelist.getId()).setFileType("binary").setBypassClientUpdate(true);
        valids = new Valids();
        valids.noUpdateGet = true;
        valids.noUpdate = true;
        valids.changelistIdGet = pendingChangelist.getId();
        valids.changelistId = pendingChangelist.getId();
        valids.fileTypeGet = "binary";
        valids.fileType = "binary";
        valids.bypassClientUpdateGet = true;
        valids.bypassClientUpdate = true;
        testMethod(testFileSpecs, true);
        testMethod(testFileSpecs, false);
    }

    @Test
    public void overrideStringConstructor() throws Throwable {
        editFilesOptions = new EditFilesOptions("-n", "-k", "-c" + pendingChangelist.getId(), "-tbinary");
        editFilesOptions.setNoUpdate(false);
        editFilesOptions.setChangelistId(DEFAULT);
        editFilesOptions.setFileType("text");
        editFilesOptions.setBypassClientUpdate(false);
        valids = new Valids();
        valids.immutable = true;
        valids.noUpdate = true;
        valids.changelistId = pendingChangelist.getId();
        valids.fileTypeGet = "text";
        valids.fileType = "binary";
        valids.bypassClientUpdate = true;
        testMethod(testFileSpecs, false);
    }

    @Test
    public void setImmutableFalseNoUpdate() throws Throwable {
        editFilesOptions = new EditFilesOptions();
        editFilesOptions.setImmutable(false);
        valids = new Valids();
        testMethod(testFileSpecs, false);
        editFilesOptions.setNoUpdate(true);
        valids = new Valids();
        valids.noUpdateGet = true;
        valids.noUpdate = true;
        testMethod(testFileSpecs, false);
    }

    @Test
    public void setImmutableTrueNoUpdate() throws Throwable {
        editFilesOptions = new EditFilesOptions();
        editFilesOptions.setImmutable(true);
        valids = new Valids();
        valids.immutable = true;
        testMethod(testFileSpecs, false);
        editFilesOptions.setNoUpdate(true);
        valids = new Valids();
        valids.immutable = true;
        valids.noUpdateGet = true;
        testMethod(testFileSpecs, false);
    }

    @Test
    public void setImmutableFalseChangelist() throws Throwable {
        editFilesOptions = new EditFilesOptions();
        editFilesOptions.setImmutable(false);
        valids = new Valids();
        testMethod(testFileSpecs, false);
        editFilesOptions.setChangelistId(pendingChangelist.getId());
        valids = new Valids();
        valids.changelistIdGet = pendingChangelist.getId();
        valids.changelistId = pendingChangelist.getId();
        testMethod(testFileSpecs, false);
    }


    @Test
    public void setImmutableTrueChangelist() throws Throwable {
        editFilesOptions = new EditFilesOptions();
        editFilesOptions.setImmutable(true);
        valids = new Valids();
        valids.immutable = true;
        testMethod(testFileSpecs, false);
        editFilesOptions.setChangelistId(pendingChangelist.getId());
        valids = new Valids();
        valids.immutable = true;
        valids.changelistIdGet = pendingChangelist.getId();
        testMethod(testFileSpecs, false);
    }

    @Test
    public void setImmutableFalseFileType() throws Throwable {
        editFilesOptions = new EditFilesOptions();
        editFilesOptions.setImmutable(false);
        valids = new Valids();
        testMethod(testFileSpecs, false);
        editFilesOptions.setFileType("binary");
        valids = new Valids();
        valids.fileTypeGet = "binary";
        valids.fileType = "binary";
        testMethod(testFileSpecs, false);
    }


    @Test
    public void setImmutableTrueFileType() throws Throwable {
        editFilesOptions = new EditFilesOptions();
        editFilesOptions.setImmutable(true);
        valids = new Valids();
        valids.immutable = true;
        testMethod(testFileSpecs, false);
        editFilesOptions.setFileType("binary");
        valids = new Valids();
        valids.immutable = true;
        valids.fileTypeGet = "binary";
        testMethod(testFileSpecs, false);
    }

    @Test
    public void setImmutableFalseBypassClientUpdate() throws Throwable {
        editFilesOptions = new EditFilesOptions();
        editFilesOptions.setImmutable(false);
        valids = new Valids();
        testMethod(testFileSpecs, false);
        editFilesOptions.setBypassClientUpdate(true);
        valids = new Valids();
        valids.bypassClientUpdateGet = true;
        valids.bypassClientUpdate = true;
        testMethod(testFileSpecs, false);
    }


    @Test
    public void setImmutableTrueBypassClientUpdate() throws Throwable {
        editFilesOptions = new EditFilesOptions();
        editFilesOptions.setImmutable(true);
        valids = new Valids();
        valids.immutable = true;
        testMethod(testFileSpecs, false);
        editFilesOptions.setBypassClientUpdate(true);
        valids = new Valids();
        valids.immutable = true;
        valids.bypassClientUpdateGet = true;
        testMethod(testFileSpecs, false);
    }


    @AfterAll
    public static void afterClass() {
        helper.after(ts);
    }

    private static void testMethod(List<IFileSpec> fileSpecs, boolean useOldMethod) throws Throwable {
        assertThat(editFilesOptions.isImmutable(), is(valids.immutable));
        assertThat(editFilesOptions.isNoUpdate(), is(valids.noUpdateGet));
        assertThat(editFilesOptions.getChangelistId(), is(valids.changelistIdGet));
        assertThat(editFilesOptions.getFileType(), is(valids.fileTypeGet));
        assertThat(editFilesOptions.isBypassClientUpdate(), is(valids.bypassClientUpdateGet));

        client.revertFiles(makeFileSpecList("//depot/..."), new RevertFilesOptions());

        List<IFileSpec> addedFileSpecs;

        if (useOldMethod) {
            addedFileSpecs = client.editFiles(fileSpecs, editFilesOptions.isNoUpdate(), editFilesOptions.isBypassClientUpdate(), editFilesOptions.getChangelistId(), editFilesOptions.getFileType());
        } else {
            addedFileSpecs = client.editFiles(fileSpecs, editFilesOptions);
        }

        List<IFileSpec> openedFileSpecs = client.openedFiles(null, new OpenedFilesOptions());
        helper.validateFileSpecs(openedFileSpecs);
        if (valids.noUpdate) {
            assertThat(openedFileSpecs.size(), is(0));
        } else {
            helper.validateFileSpecs(addedFileSpecs);
            assertThat(openedFileSpecs.size() > 0, is(true));
            for (IFileSpec openedFileSpec : openedFileSpecs) {
                assertThat(!testFile.canWrite(), is(valids.bypassClientUpdate));
                assertThat(openedFileSpec.getChangelistId(), is(valids.changelistId));
                assertThat(openedFileSpec.getFileType(), is(valids.fileType));
            }
        }
    }

    @Ignore
    private static class Valids {
        private boolean immutable = false;
        private boolean noUpdateGet = false;
        private boolean noUpdate = false;
        private int changelistIdGet = DEFAULT;
        private int changelistId = DEFAULT;
        private String fileTypeGet = null;
        private String fileType = "text";
        private boolean bypassClientUpdateGet = false;
        private boolean bypassClientUpdate = false;

    }
}