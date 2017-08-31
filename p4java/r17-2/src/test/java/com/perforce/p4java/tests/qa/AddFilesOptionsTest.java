package com.perforce.p4java.tests.qa;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.option.client.AddFilesOptions;
import com.perforce.p4java.option.client.RevertFilesOptions;
import com.perforce.p4java.option.server.OpenedFilesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.test.ExecutableSpecification;
import com.perforce.test.TestServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

@RunWith(JUnitPlatform.class)
public class AddFilesOptionsTest {

    private static TestServer ts = null;
    private static Helper h = null;
    private static IClient client = null;
    private static IChangelist pendingChangelist = null;
    private static List<IFileSpec> normalFileSpecs = null;
    private static List<IFileSpec> wildcardFileSpecs = null;
    private static AddFilesOptions addFilesOptions = null;
    private static Valids valids = null;

    @BeforeAll
    public static void beforeClass() throws Throwable {
        h = new Helper();
        ts = new TestServer();
        ExecutableSpecification serverExecutableSpecification = ts.getServerExecutableSpecification();
        serverExecutableSpecification.setCodeline(h.getServerVersion());
        ts.start();

        IOptionsServer server = h.getServer(ts);
        server.setUserName(ts.getUser());
        server.connect();

        IUser user = server.getUser(ts.getUser());

        client = h.createClient(server, "client1");
        server.setCurrentClient(client);

        pendingChangelist = h.createChangelist(server, user, client);
        normalFileSpecs = h.createFile(client.getRoot() + Helper.FILE_SEP + "file1.txt", "Testing AddFilesOptions");
        wildcardFileSpecs = h.createFile(client.getRoot() + Helper.FILE_SEP + "foo@bar.txt", "Testing AddFilesOptions with wildcard");
    }

    @Test
    public void optionsSpecs() throws Exception {
        assertThat(AddFilesOptions.OPTIONS_SPECS, is("b:n i:c:gtz s:t b:f b:I s:Q"));
    }

    @Test
    public void defaultConstructor() throws Throwable {
        addFilesOptions = new AddFilesOptions();
        addFilesOptions.setFileType("text");
        valids = new Valids();
        valids.fileType = "text";
        testMethod(normalFileSpecs, true);
        testMethod(wildcardFileSpecs, true);
        testMethod(normalFileSpecs, false);
        testMethod(wildcardFileSpecs, false);
    }

    @Test
    public void explicitConstructorDefaults() throws Throwable {
        addFilesOptions = new AddFilesOptions(false, IChangelist.DEFAULT, null, false);
        addFilesOptions.setFileType("text");
        valids = new Valids();
        valids.fileType = "text";
        testMethod(normalFileSpecs, true);
        testMethod(wildcardFileSpecs, true);
        testMethod(normalFileSpecs, false);
        testMethod(wildcardFileSpecs, false);
    }

    @Test
    public void explicitConstructorNoUpdate() throws Throwable {
        addFilesOptions = new AddFilesOptions(true, IChangelist.DEFAULT, null, false);
        addFilesOptions.setFileType("text");
        valids = new Valids();
        valids.fileType = "text";
        valids.noUpdateGet = true;
        valids.noUpdate = true;
        testMethod(normalFileSpecs, true);
        testMethod(wildcardFileSpecs, true);
        testMethod(normalFileSpecs, false);
        testMethod(wildcardFileSpecs, false);
    }

    @Test
    public void stringConstructorNoUpdate() throws Throwable {
        addFilesOptions = new AddFilesOptions("-n");
        addFilesOptions.setFileType("text");
        valids = new Valids();
        valids.fileType = "text";
        valids.immutable = true;
        valids.noUpdate = true;
        testMethod(normalFileSpecs, false);
        testMethod(wildcardFileSpecs, false);

    }

    @Test
    public void explicitConstructorChangelistPending() throws Throwable {
        addFilesOptions = new AddFilesOptions(false, pendingChangelist.getId(), null, false);
        addFilesOptions.setFileType("text");
        valids = new Valids();
        valids.fileType = "text";
        valids.changelistIdGet = pendingChangelist.getId();
        valids.changelistId = pendingChangelist.getId();
        testMethod(normalFileSpecs, true);
        testMethod(wildcardFileSpecs, true);
        testMethod(normalFileSpecs, false);
        testMethod(wildcardFileSpecs, false);

    }

    @Test
    public void stringConstructorChangelistPending() throws Throwable {
        addFilesOptions = new AddFilesOptions("-c" + pendingChangelist.getId(), "-ttext");
        addFilesOptions.setFileType("text");
        valids = new Valids();
        valids.fileType = "text";
        valids.immutable = true;
        valids.changelistId = pendingChangelist.getId();
        testMethod(normalFileSpecs, false);
        testMethod(wildcardFileSpecs, false);

    }

    @Test
    public void explicitConstructorChangelistNegative() throws Throwable {
        addFilesOptions = new AddFilesOptions(false, IChangelist.UNKNOWN, null, false);
        addFilesOptions.setFileType("text");
        valids = new Valids();
        valids.fileType = "text";
        valids.changelistIdGet = IChangelist.UNKNOWN;
        testMethod(normalFileSpecs, true);
        testMethod(wildcardFileSpecs, true);
        testMethod(normalFileSpecs, false);
        testMethod(wildcardFileSpecs, false);

    }

    @Test
    public void explicitConstructorFileType() throws Throwable {
        addFilesOptions = new AddFilesOptions(false, IChangelist.DEFAULT, "binary", false);
        valids = new Valids();
        valids.fileType = "binary";
        valids.fileTypeGet = "binary";
        testMethod(normalFileSpecs, true);
        testMethod(wildcardFileSpecs, true);
        testMethod(normalFileSpecs, false);
        testMethod(wildcardFileSpecs, false);
    }

    @Test
    public void stringConstructorFileType() throws Throwable {
        addFilesOptions = new AddFilesOptions("-tbinary");
        valids = new Valids();
        valids.immutable = true;
        valids.fileTypeGet = null;
        valids.fileType = "binary";
        testMethod(normalFileSpecs, false);
        testMethod(wildcardFileSpecs, false);
    }

    @Test
    public void explicitConstructorWildcards() throws Throwable {
        addFilesOptions = new AddFilesOptions(false, IChangelist.DEFAULT, null, true);
        addFilesOptions.setFileType("text");
        valids = new Valids();
        valids.fileType = "text";
        valids.useWildcards = true;
        valids.useWildcardsGet = true;
        testMethod(normalFileSpecs, true);
        testMethod(wildcardFileSpecs, true);
        testMethod(normalFileSpecs, false);
        testMethod(wildcardFileSpecs, false);
    }

    @Test
    public void stringConstructorWildcards() throws Throwable {
        addFilesOptions = new AddFilesOptions("-f", "-ttext");
        valids = new Valids();
        valids.fileType = "text";
        valids.fileTypeGet = null;
        valids.immutable = true;
        valids.useWildcards = true;
        testMethod(normalFileSpecs, false);
        testMethod(wildcardFileSpecs, false);

    }

    @Test
    public void explicitConstructorAll() throws Throwable {
        addFilesOptions = new AddFilesOptions(true, pendingChangelist.getId(), "binary", true);
        valids = new Valids();
        valids.noUpdateGet = true;
        valids.noUpdate = true;
        valids.changelistIdGet = pendingChangelist.getId();
        valids.changelistId = pendingChangelist.getId();
        valids.fileTypeGet = "binary";
        valids.fileType = "binary";
        valids.useWildcardsGet = true;
        valids.useWildcards = true;
        testMethod(normalFileSpecs, true);
        testMethod(wildcardFileSpecs, true);
        testMethod(normalFileSpecs, false);
        testMethod(wildcardFileSpecs, false);
    }

    @Test
    public void stringConstructorAll() throws Throwable {
        addFilesOptions = new AddFilesOptions("-n", "-c" + pendingChangelist.getId(), "-tbinary", "-f");
        valids = new Valids();
        valids.immutable = true;
        valids.noUpdate = true;
        valids.changelistId = pendingChangelist.getId();
        valids.fileType = "binary";
        valids.fileTypeGet = null;
        valids.useWildcards = true;
        testMethod(normalFileSpecs, false);
        testMethod(wildcardFileSpecs, false);
    }

    @Test
    public void explicitConstructorAllExceptNoUpdate() throws Throwable {
        addFilesOptions = new AddFilesOptions(false, pendingChangelist.getId(), "binary", true);
        valids = new Valids();
        valids.changelistIdGet = pendingChangelist.getId();
        valids.changelistId = pendingChangelist.getId();
        valids.fileTypeGet = "binary";
        valids.fileType = "binary";
        valids.useWildcardsGet = true;
        valids.useWildcards = true;
        testMethod(normalFileSpecs, true);
        testMethod(wildcardFileSpecs, true);
        testMethod(normalFileSpecs, false);
        testMethod(wildcardFileSpecs, false);
    }

    @Test
    public void stringConstructorAllExceptNoUpdate() throws Throwable {
        addFilesOptions = new AddFilesOptions("-c" + pendingChangelist.getId(), "-tbinary", "-f");
        valids = new Valids();
        valids.immutable = true;
        valids.changelistId = pendingChangelist.getId();
        valids.fileType = "binary";
        valids.fileTypeGet = null;
        valids.useWildcards = true;
        testMethod(normalFileSpecs, false);
        testMethod(wildcardFileSpecs, false);

    }

    @Test
    public void setNoUpdateFalse() throws Throwable {
        addFilesOptions = new AddFilesOptions();
        addFilesOptions.setNoUpdate(false);
        addFilesOptions.setFileType("text");
        valids = new Valids();
        valids.fileType = "text";
        testMethod(normalFileSpecs, true);
        testMethod(wildcardFileSpecs, true);
        testMethod(normalFileSpecs, false);
        testMethod(wildcardFileSpecs, false);
    }

    @Test
    public void setNoUpdateTrue() throws Throwable {
        addFilesOptions = new AddFilesOptions();
        addFilesOptions.setNoUpdate(true);
        addFilesOptions.setFileType("text");
        valids = new Valids();
        valids.fileType = "text";
        valids.noUpdate = true;
        valids.noUpdateGet = true;
        testMethod(normalFileSpecs, true);
        testMethod(wildcardFileSpecs, true);
        testMethod(normalFileSpecs, false);
        testMethod(wildcardFileSpecs, false);
    }

    @Test
    public void setChangelistIdDefault() throws Throwable {
        addFilesOptions = new AddFilesOptions();
        addFilesOptions.setChangelistId(IChangelist.DEFAULT);
        addFilesOptions.setFileType("text");
        valids = new Valids();
        valids.fileType = "text";
        testMethod(normalFileSpecs, true);
        testMethod(wildcardFileSpecs, true);
        testMethod(normalFileSpecs, false);
        testMethod(wildcardFileSpecs, false);

    }

    @Test
    public void setChangelistIdPending() throws Throwable {
        addFilesOptions = new AddFilesOptions();
        addFilesOptions.setChangelistId(pendingChangelist.getId());
        addFilesOptions.setFileType("text");
        valids = new Valids();
        valids.fileType = "text";
        valids.changelistIdGet = pendingChangelist.getId();
        valids.changelistId = pendingChangelist.getId();
        testMethod(normalFileSpecs, true);
        testMethod(wildcardFileSpecs, true);
        testMethod(normalFileSpecs, false);
        testMethod(wildcardFileSpecs, false);
    }

    @Test
    public void setChangelistIdNegative() throws Throwable {
        addFilesOptions = new AddFilesOptions();
        addFilesOptions.setChangelistId(IChangelist.UNKNOWN);
        addFilesOptions.setFileType("text");
        valids = new Valids();
        valids.fileType = "text";
        valids.changelistIdGet = IChangelist.UNKNOWN;
        testMethod(normalFileSpecs, true);
        testMethod(wildcardFileSpecs, true);
        testMethod(normalFileSpecs, false);
        testMethod(wildcardFileSpecs, false);
    }

    @Test
    public void setFileTypeNull() throws Throwable {
        addFilesOptions = new AddFilesOptions();
        addFilesOptions.setFileType(null);
        addFilesOptions.setFileType("text");
        valids = new Valids();
        valids.fileType = "text";
        testMethod(normalFileSpecs, true);
        testMethod(wildcardFileSpecs, true);
        testMethod(normalFileSpecs, false);
        testMethod(wildcardFileSpecs, false);
    }

    @Test
    public void setFileTypeBinary() throws Throwable {
        addFilesOptions = new AddFilesOptions();
        addFilesOptions.setFileType("binary");
        valids = new Valids();
        valids.fileTypeGet = "binary";
        valids.fileType = "binary";
        testMethod(normalFileSpecs, true);
        testMethod(wildcardFileSpecs, true);
        testMethod(normalFileSpecs, false);
        testMethod(wildcardFileSpecs, false);
    }

    @Test
    public void setUseWildcardsFalse() throws Throwable {
        addFilesOptions = new AddFilesOptions();
        addFilesOptions.setUseWildcards(false);
        addFilesOptions.setFileType("text");
        valids = new Valids();
        valids.fileType = "text";
        testMethod(normalFileSpecs, true);
        testMethod(wildcardFileSpecs, true);
        testMethod(normalFileSpecs, false);
        testMethod(wildcardFileSpecs, false);
    }

    @Test
    public void setUseWildcardsTrue() throws Throwable {
        addFilesOptions = new AddFilesOptions();
        addFilesOptions.setUseWildcards(true);
        addFilesOptions.setFileType("text");
        valids = new Valids();
        valids.fileType = "text";
        valids.useWildcardsGet = true;
        valids.useWildcards = true;
        testMethod(normalFileSpecs, true);
        testMethod(wildcardFileSpecs, true);
        testMethod(normalFileSpecs, false);
        testMethod(wildcardFileSpecs, false);

    }

    @Test
    public void setChainAll() throws Throwable {
        addFilesOptions = new AddFilesOptions();
        addFilesOptions.setNoUpdate(true).setChangelistId(pendingChangelist.getId()).setFileType("binary").setUseWildcards(true);
        valids = new Valids();
        valids.noUpdateGet = true;
        valids.noUpdate = true;
        valids.changelistIdGet = pendingChangelist.getId();
        valids.changelistId = pendingChangelist.getId();
        valids.fileTypeGet = "binary";
        valids.fileType = "binary";
        valids.useWildcardsGet = true;
        valids.useWildcards = true;
        testMethod(normalFileSpecs, true);
        testMethod(wildcardFileSpecs, true);
        testMethod(normalFileSpecs, false);
        testMethod(wildcardFileSpecs, false);
    }

    @Test
    public void overrideStringConstructor() throws Throwable {

        addFilesOptions = new AddFilesOptions("-n", "-c" + pendingChangelist.getId(), "-tbinary", "-f");
        addFilesOptions.setNoUpdate(false);
        addFilesOptions.setChangelistId(IChangelist.DEFAULT);
        addFilesOptions.setFileType("text");
        addFilesOptions.setUseWildcards(false);
        valids = new Valids();
        valids.immutable = true;
        valids.noUpdate = true;
        valids.changelistId = pendingChangelist.getId();
        valids.fileTypeGet = "text";
        valids.fileType = "binary";
        valids.useWildcards = true;
        testMethod(normalFileSpecs, false);
        testMethod(wildcardFileSpecs, false);

    }

    @Test
    public void setImmutableFalseNoUpdate() throws Throwable {
        addFilesOptions = new AddFilesOptions();
        addFilesOptions.setImmutable(false);
        addFilesOptions.setFileType("text");
        valids = new Valids();
        valids.fileType = "text";
        testMethod(normalFileSpecs, false);
        testMethod(wildcardFileSpecs, false);
        addFilesOptions.setNoUpdate(true);
        addFilesOptions.setFileType("text");
        valids = new Valids();
        valids.fileType = "text";
        valids.noUpdateGet = true;
        valids.noUpdate = true;
        testMethod(normalFileSpecs, false);
        testMethod(wildcardFileSpecs, false);

    }

    @Test
    public void setImmutableTrueNoUpdate() throws Throwable {
        addFilesOptions = new AddFilesOptions();
        addFilesOptions.setImmutable(true);
        addFilesOptions.setFileType("text");
        valids = new Valids();
        valids.fileType = "text";
        valids.immutable = true;
        testMethod(normalFileSpecs, false);
        testMethod(wildcardFileSpecs, false);
        addFilesOptions.setNoUpdate(true);
        valids = new Valids();
        valids.immutable = true;
        valids.noUpdateGet = true;
        testMethod(normalFileSpecs, false);
        testMethod(wildcardFileSpecs, false);

    }

    @Test
    public void setImmutableFalseChangelist() throws Throwable {
        addFilesOptions = new AddFilesOptions();
        addFilesOptions.setImmutable(false);
        addFilesOptions.setFileType("text");
        valids = new Valids();
        valids.fileType = "text";
        testMethod(normalFileSpecs, false);
        testMethod(wildcardFileSpecs, false);
        addFilesOptions.setChangelistId(pendingChangelist.getId());
        valids = new Valids();
        valids.changelistIdGet = pendingChangelist.getId();
        valids.changelistId = pendingChangelist.getId();
        testMethod(normalFileSpecs, false);
        testMethod(wildcardFileSpecs, false);
    }


    @Test
    public void setImmutableTrueChangelist() throws Throwable {
        addFilesOptions = new AddFilesOptions();
        addFilesOptions.setImmutable(true);
        addFilesOptions.setFileType("text");
        valids = new Valids();
        valids.fileType = "text";
        valids.immutable = true;
        testMethod(normalFileSpecs, false);
        testMethod(wildcardFileSpecs, false);
        addFilesOptions.setChangelistId(pendingChangelist.getId());
        valids = new Valids();
        valids.immutable = true;
        valids.changelistIdGet = pendingChangelist.getId();
        testMethod(normalFileSpecs, false);
        testMethod(wildcardFileSpecs, false);
    }

    @Test
    public void setImmutableFalseFileType() throws Throwable {
        addFilesOptions = new AddFilesOptions();
        addFilesOptions.setImmutable(false);
        addFilesOptions.setFileType("text");
        valids = new Valids();
        valids.fileType = "text";
        testMethod(normalFileSpecs, false);
        testMethod(wildcardFileSpecs, false);
        addFilesOptions.setFileType("binary");
        valids = new Valids();
        valids.fileTypeGet = "binary";
        valids.fileType = "binary";
        testMethod(normalFileSpecs, false);
        testMethod(wildcardFileSpecs, false);
    }


    @Test
    public void setImmutableTrueFileType() throws Throwable {
        addFilesOptions = new AddFilesOptions();
        addFilesOptions.setImmutable(true);
        addFilesOptions.setFileType("text");
        valids = new Valids();
        valids.fileType = "text";
        valids.immutable = true;
        testMethod(normalFileSpecs, false);
        testMethod(wildcardFileSpecs, false);
        addFilesOptions.setFileType("binary");
        valids = new Valids();
        valids.immutable = true;
        valids.fileTypeGet = "binary";
        testMethod(normalFileSpecs, false);
        testMethod(wildcardFileSpecs, false);
    }

    @Test
    public void setImmutableFalseWildcards() throws Throwable {
        addFilesOptions = new AddFilesOptions();
        addFilesOptions.setImmutable(false);
        addFilesOptions.setFileType("text");
        valids = new Valids();
        valids.fileType = "text";
        testMethod(normalFileSpecs, false);
        testMethod(wildcardFileSpecs, false);
        addFilesOptions.setUseWildcards(true);
        valids = new Valids();
        valids.useWildcardsGet = true;
        valids.useWildcards = true;
        testMethod(normalFileSpecs, false);
        testMethod(wildcardFileSpecs, false);
    }


    @Test
    public void setImmutableTrueWildcards() throws Throwable {
        addFilesOptions = new AddFilesOptions();
        addFilesOptions.setImmutable(true);
        addFilesOptions.setFileType("text");
        valids = new Valids();
        valids.fileType = "text";
        valids.immutable = true;
        testMethod(normalFileSpecs, false);
        testMethod(wildcardFileSpecs, false);
        addFilesOptions.setUseWildcards(true);
        valids = new Valids();
        valids.immutable = true;
        valids.useWildcardsGet = true;
        testMethod(normalFileSpecs, false);
        testMethod(wildcardFileSpecs, false);
    }


    @AfterAll
    public static void afterClass() {
        h.after(ts);
    }

    private static void testMethod(List<IFileSpec> fileSpecs, boolean useOldMethod) throws Throwable {
        assertThat(addFilesOptions.isImmutable(), is(valids.immutable));
        assertThat(addFilesOptions.isNoUpdate(), is(valids.noUpdateGet));
        assertThat(addFilesOptions.getChangelistId(), is(valids.changelistIdGet));
        assertThat(addFilesOptions.getFileType(), is(valids.fileTypeGet));
        assertThat(addFilesOptions.isUseWildcards(), is(valids.useWildcardsGet));

        client.revertFiles(FileSpecBuilder.makeFileSpecList("//depot/..."), new RevertFilesOptions());

        List<IFileSpec> addedFileSpecs;
        if (useOldMethod) {
            addedFileSpecs = client.addFiles(fileSpecs, addFilesOptions.isNoUpdate(), addFilesOptions.getChangelistId(), addFilesOptions.getFileType(), addFilesOptions.isUseWildcards());
        } else {
            addedFileSpecs = client.addFiles(fileSpecs, addFilesOptions);
        }

        List<IFileSpec> openedFileSpecs = client.openedFiles(null, new OpenedFilesOptions());
        h.validateFileSpecs(openedFileSpecs);

        if (valids.noUpdate || (fileSpecs.equals(wildcardFileSpecs) && !valids.useWildcards)) {
            assertThat(openedFileSpecs.size(), is(0));
        } else {
            h.validateFileSpecs(addedFileSpecs);
            assertThat(openedFileSpecs.size() > 0, is(true));
            for (IFileSpec openedFileSpec : openedFileSpecs) {
                assertThat(openedFileSpec.getChangelistId(), is(valids.changelistId));
                assertThat(openedFileSpec.getFileType(), is(valids.fileType));
            }
        }
    }


    //@Ignore
    private static class Valids {
        private boolean immutable = false;
        private boolean noUpdateGet = false;
        private boolean noUpdate = false;
        private int changelistIdGet = IChangelist.DEFAULT;
        private int changelistId = IChangelist.DEFAULT;
        private String fileTypeGet = "text";
        private String fileType = "text";
        private boolean useWildcardsGet = false;
        private boolean useWildcards = false;
    }
}
