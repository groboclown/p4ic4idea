package com.perforce.p4java.tests.qa;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.core.file.FileStatAncilliaryOptions;
import com.perforce.p4java.core.file.FileStatOutputOptions;
import com.perforce.p4java.core.file.IExtendedFileSpec;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.option.client.EditFilesOptions;
import com.perforce.p4java.option.server.GetExtendedFilesOptions;
import com.perforce.p4java.option.server.SetFileAttributesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.test.TestServer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.HashMap;
import java.util.List;

import static com.google.common.collect.Maps.newHashMap;
import static com.perforce.p4java.core.file.FileSpecBuilder.makeFileSpecList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;

@RunWith(JUnitPlatform.class)
public class GetExtendedFilesOptionsTest {

    private static TestServer ts = null;
    private static Helper helper = null;
    private static IOptionsServer server = null;
    private static File smallFile = null;

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

        File mediumFile = new File(client.getRoot(), "foobar.txt");
        helper.addFile(server, user, client, mediumFile.getAbsolutePath(), "GetExtendedFilesOptionsTestPlus", "text");

        smallFile = new File(client.getRoot(), "foo.txt");
        helper.addFile(server, user, client, smallFile.getAbsolutePath(), "GetExtendedFilesOptionsTest", "text+w");

        File bigFile = new File(client.getRoot(), "foobarbaz.txt");
        helper.addFile(server, user, client, bigFile.getAbsolutePath(), "GetExtendedFilesOptionsTestPlusPlus", "text+k");

        File mediumBinFile = new File(client.getRoot(), "bin_foobar.txt");
        helper.addFile(server, user, client, mediumBinFile.getAbsolutePath(), "GetExtendedFilesOptionsTestPlus", "binary");

        File smallBinFile = new File(client.getRoot(), "bin_foo.txt");
        helper.addFile(server, user, client, smallBinFile.getAbsolutePath(), "GetExtendedFilesOptionsTest", "binary");

        File bigBinFile = new File(client.getRoot(), "bin_foobarbaz.txt");
        helper.addFile(server, user, client, bigBinFile.getAbsolutePath(), "GetExtendedFilesOptionsTestPlusPlus", "binary");

        // add revisions to make difference head revs possible, then sync to an interesting
        // set of have revs
        List<IFileSpec> fileSpec = makeFileSpecList("//depot/...baz...");
        EditFilesOptions opts = new EditFilesOptions();
        IChangelist pendingChange = helper.createChangelist(server, user, client);
        opts.setChangelistId(pendingChange.getId());

        client.editFiles(fileSpec, opts);
        HashMap<String, String> hm = newHashMap();
        hm.put("bazFiles", "1");
        server.setFileAttributes(fileSpec, hm, null);

        pendingChange.refresh();
        pendingChange.submit(false);

        fileSpec = makeFileSpecList("//depot/...foo...");
        opts = new EditFilesOptions();
        pendingChange = helper.createChangelist(server, user, client);
        opts.setChangelistId(pendingChange.getId());

        client.editFiles(fileSpec, opts);
        hm = newHashMap();
        hm.put("fooFiles", "1");
        server.setFileAttributes(fileSpec, hm, null);

        pendingChange.refresh();
        pendingChange.submit(false);

        fileSpec = makeFileSpecList("//depot/...foo...");
        opts = new EditFilesOptions();
        pendingChange = helper.createChangelist(server, user, client);
        opts.setChangelistId(pendingChange.getId());

        client.editFiles(fileSpec, opts);
        hm = newHashMap();
        hm.put("fooFiles", "2");
        server.setFileAttributes(fileSpec, hm, null);

        pendingChange.refresh();
        pendingChange.submit(false);

        fileSpec = makeFileSpecList("//depot/...bar...");
        opts = new EditFilesOptions();
        pendingChange = helper.createChangelist(server, user, client);
        opts.setChangelistId(pendingChange.getId());

        client.editFiles(fileSpec, opts);
        hm = newHashMap();
        hm.put("barFiles", "1");
        server.setFileAttributes(fileSpec, hm, null);

        pendingChange.refresh();
        pendingChange.submit(false);

        fileSpec = makeFileSpecList("//depot/bin_...");
        opts = new EditFilesOptions();
        pendingChange = helper.createChangelist(server, user, client);
        opts.setChangelistId(pendingChange.getId());

        client.editFiles(fileSpec, opts);
        hm = newHashMap();
        hm.put("binFiles", "57656c636f6d6520746f2050344a61766120696e2068657821");
        SetFileAttributesOptions aOpts = new SetFileAttributesOptions();
        aOpts.setHexValue(true);
        server.setFileAttributes(fileSpec, hm, aOpts);

        pendingChange.refresh();
        pendingChange.submit(false);

        // sync stuff to a knowable state
        fileSpec = makeFileSpecList("//depot/*foo.txt#3");
        client.sync(fileSpec, null);

        fileSpec = makeFileSpecList("//depot/*foobar.txt#2");
        client.sync(fileSpec, null);

        fileSpec = makeFileSpecList("//depot/*foobarbaz.txt#1");
        client.sync(fileSpec, null);
    }

    @DisplayName("make sure something is fetched even with a null value")
    @Test
    public void nullGetExtendedFilesOptions() throws Throwable {
        List<IFileSpec> fileSpec = makeFileSpecList(smallFile.getAbsolutePath());

        List<IExtendedFileSpec> results = server.getExtendedFiles(fileSpec, null);

        assertThat("Wrong number of file specs", results.size(), is(1));
        IExtendedFileSpec extendedFileSpec = results.get(0);
        assertThat(extendedFileSpec.getHeadRev(), is(3));
        assertThat(extendedFileSpec.getDepotPathString(), is("//depot/foo.txt"));
        assertThat(extendedFileSpec.getHeadModTime(), notNullValue());
        assertThat(extendedFileSpec.getHeadTime(), notNullValue());
        assertThat(extendedFileSpec.getHeadChange(), is(9));
        assertThat(extendedFileSpec.getHeadType(), is("text+w"));
        assertThat(extendedFileSpec.getHeadAction().toString(), is("edit"));
        assertThat(extendedFileSpec.getHaveRev(), is(3));
    }

    @DisplayName("make sure something is fetched even with the default options object and that default objects have correct values")
    @Test
    public void defaultGetExtendedFilesOptions() throws Throwable {
        List<IFileSpec> fileSpec = makeFileSpecList(smallFile.getAbsolutePath());
        GetExtendedFilesOptions opts = new GetExtendedFilesOptions();

        // assert that default values are all what we expect
        assertThat(opts.isReverseSort(), is(false));
        assertThat(opts.isSortByDate(), is(false));
        assertThat(opts.isSortByFileSize(), is(false));
        assertThat(opts.isSortByFiletype(), is(false));
        assertThat(opts.isSortByHaveRev(), is(false));
        assertThat(opts.isSortByHeadRev(), is(false));
        assertThat(opts.getAttributePattern(), nullValue());
        assertThat(opts.getMaxResults(), is(0));
        assertThat(opts.getSinceChangelist(), is(-1));

        List<IExtendedFileSpec> results = server.getExtendedFiles(fileSpec, opts);

        assertThat("Wrong number of file specs", results.size(), is(1));
        IExtendedFileSpec extendedFileSpec = results.get(0);
        assertThat(extendedFileSpec.getHeadRev(), is(3));
        assertThat(extendedFileSpec.getDepotPathString(), is("//depot/foo.txt"));
        assertThat(extendedFileSpec.getHeadModTime(), notNullValue());
        assertThat(extendedFileSpec.getHeadTime(), notNullValue());
        assertThat(extendedFileSpec.getHeadChange(), is(9));
        assertThat(extendedFileSpec.getHeadType(), is("text+w"));
        assertThat(extendedFileSpec.getHeadAction().toString(), is("edit"));
        assertThat(extendedFileSpec.getHaveRev(), is(3));
    }

    @DisplayName("make sure something is fetched even with all default objects")
    @Test
    public void defaultHelpers() throws Throwable {
        List<IFileSpec> fileSpec = makeFileSpecList(smallFile.getAbsolutePath());
        GetExtendedFilesOptions opts = new GetExtendedFilesOptions();
        FileStatOutputOptions oOpts = new FileStatOutputOptions();
        FileStatAncilliaryOptions aOpts = new FileStatAncilliaryOptions();

        // assert that default values are all what we expect
        assertThat(opts.isReverseSort(), is(false));
        assertThat(opts.isSortByDate(), is(false));
        assertThat(opts.isSortByFileSize(), is(false));
        assertThat(opts.isSortByFiletype(), is(false));
        assertThat(opts.isSortByHaveRev(), is(false));
        assertThat(opts.isSortByHeadRev(), is(false));
        assertThat(opts.getAttributePattern(), nullValue());
        assertThat(opts.getMaxResults(), is(0));
        assertThat(opts.getSinceChangelist(), is(-1));

        assertThat(oOpts.isMappedFiles(), is(false));
        assertThat(oOpts.isOpenedFiles(), is(false));
        assertThat(oOpts.isOpenedNeedsResolvingFiles(), is(false));
        assertThat(oOpts.isOpenedNotHeadRevFiles(), is(false));
        assertThat(oOpts.isOpenedResolvedFiles(), is(false));
        assertThat(oOpts.isShelvedFiles(), is(false));
        assertThat(oOpts.isSyncedFiles(), is(false));

        assertThat(aOpts.isAllRevs(), is(false));
        assertThat(aOpts.isBothPathTypes(), is(false));
        assertThat(aOpts.isExcludeLocalPath(), is(false));
        assertThat(aOpts.isFileSizeDigest(), is(false));
        assertThat(aOpts.isPendingIntegrationRecs(), is(false));

        opts.setAncilliaryOptions(aOpts);
        opts.setOutputOptions(oOpts);

        List<IExtendedFileSpec> results = server.getExtendedFiles(fileSpec, opts);

        assertThat("Wrong number of file specs", results.size(), is(1));
        IExtendedFileSpec extendedFileSpec = results.get(0);
        assertThat(extendedFileSpec.getHeadRev(), is(3));
        assertThat(extendedFileSpec.getDepotPathString(), is("//depot/foo.txt"));
        assertThat(extendedFileSpec.getHeadModTime(), notNullValue());
        assertThat(extendedFileSpec.getHeadTime(), notNullValue());
        assertThat(extendedFileSpec.getHeadChange(), is(9));
        assertThat(extendedFileSpec.getHeadType(), is("text+w"));
        assertThat(extendedFileSpec.getHeadAction().toString(), is("edit"));
        assertThat(extendedFileSpec.getHaveRev(), is(3));
    }

    @DisplayName("verify mutually exclusive options behave/override correctly ( mostly sort )")
    @Test
    public void sortBySize() throws Throwable {
        List<IFileSpec> fileSpec = makeFileSpecList("//depot/...");
        GetExtendedFilesOptions opts = new GetExtendedFilesOptions();

        opts.setSortByFileSize(true);

        List<IExtendedFileSpec> results = server.getExtendedFiles(fileSpec, opts);

        assertThat("Wrong number of file specs", results.size(), is(6));

        assertThat(results.get(0).getDepotPathString(), is("//depot/bin_foo.txt"));
        assertThat(results.get(1).getDepotPathString(), is("//depot/foo.txt"));
        assertThat(results.get(2).getDepotPathString(), is("//depot/bin_foobar.txt"));
        assertThat(results.get(3).getDepotPathString(), is("//depot/foobar.txt"));
        assertThat(results.get(4).getDepotPathString(), is("//depot/bin_foobarbaz.txt"));
        assertThat(results.get(5).getDepotPathString(), is("//depot/foobarbaz.txt"));


        opts.setReverseSort(true);
        results = server.getExtendedFiles(fileSpec, opts);

        assertThat("Wrong number of file specs", results.size(), is(6));

        assertThat(results.get(0).getDepotPathString(), is("//depot/bin_foobarbaz.txt"));
        assertThat(results.get(1).getDepotPathString(), is("//depot/foobarbaz.txt"));
        assertThat(results.get(2).getDepotPathString(), is("//depot/bin_foobar.txt"));
        assertThat(results.get(3).getDepotPathString(), is("//depot/foobar.txt"));
        assertThat(results.get(4).getDepotPathString(), is("//depot/bin_foo.txt"));
        assertThat(results.get(5).getDepotPathString(), is("//depot/foo.txt"));
    }

    @Test
    public void sortByFiletype() throws Throwable {
        List<IFileSpec> fileSpec = makeFileSpecList("//depot/...");
        GetExtendedFilesOptions opts = new GetExtendedFilesOptions();

        opts.setSortByFiletype(true);

        List<IExtendedFileSpec> results = server.getExtendedFiles(fileSpec, opts);

        assertThat("Wrong number of file specs", results.size(), is(6));

        assertThat(results.get(0).getDepotPathString(), is("//depot/bin_foo.txt"));
        assertThat(results.get(1).getDepotPathString(), is("//depot/bin_foobar.txt"));
        assertThat(results.get(2).getDepotPathString(), is("//depot/bin_foobarbaz.txt"));
        assertThat(results.get(3).getDepotPathString(), is("//depot/foobar.txt"));
        assertThat(results.get(4).getDepotPathString(), is("//depot/foobarbaz.txt"));
        assertThat(results.get(5).getDepotPathString(), is("//depot/foo.txt"));

        opts.setReverseSort(true);
        results = server.getExtendedFiles(fileSpec, opts);

        assertThat("Wrong number of file specs", results.size(), is(6));
        assertThat(results.get(0).getDepotPathString(), is("//depot/foo.txt"));
        assertThat(results.get(1).getDepotPathString(), is("//depot/foobarbaz.txt"));
        assertThat(results.get(2).getDepotPathString(), is("//depot/foobar.txt"));
        assertThat(results.get(3).getDepotPathString(), is("//depot/bin_foo.txt"));
        assertThat(results.get(4).getDepotPathString(), is("//depot/bin_foobar.txt"));
        assertThat(results.get(5).getDepotPathString(), is("//depot/bin_foobarbaz.txt"));

    }

    @Test
    public void sortByHeadRev() throws Throwable {
        List<IFileSpec> fileSpec = makeFileSpecList("//depot/...");
        GetExtendedFilesOptions opts = new GetExtendedFilesOptions();

        opts.setSortByHeadRev(true);

        List<IExtendedFileSpec> results = server.getExtendedFiles(fileSpec, opts);

        assertThat("Wrong number of file specs", results.size(), is(6));

        assertThat(results.get(0).getDepotPathString(), is("//depot/bin_foobarbaz.txt"));
        assertThat(results.get(1).getDepotPathString(), is("//depot/bin_foobar.txt"));
        assertThat(results.get(2).getDepotPathString(), is("//depot/foobarbaz.txt"));
        assertThat(results.get(3).getDepotPathString(), is("//depot/bin_foo.txt"));
        assertThat(results.get(4).getDepotPathString(), is("//depot/foobar.txt"));
        assertThat(results.get(5).getDepotPathString(), is("//depot/foo.txt"));

        opts.setReverseSort(true);
        results = server.getExtendedFiles(fileSpec, opts);

        assertThat("Wrong number of file specs", results.size(), is(6));

        assertThat(results.get(0).getDepotPathString(), is("//depot/foo.txt"));
        assertThat(results.get(1).getDepotPathString(), is("//depot/bin_foo.txt"));
        assertThat(results.get(2).getDepotPathString(), is("//depot/foobar.txt"));
        assertThat(results.get(3).getDepotPathString(), is("//depot/bin_foobar.txt"));
        assertThat(results.get(4).getDepotPathString(), is("//depot/foobarbaz.txt"));
        assertThat(results.get(5).getDepotPathString(), is("//depot/bin_foobarbaz.txt"));
    }

    @Test
    public void sortByHaveRev() throws Throwable {
        List<IFileSpec> fileSpec = makeFileSpecList("//depot/...");
        GetExtendedFilesOptions opts = new GetExtendedFilesOptions();

        opts.setSortByHaveRev(true);

        List<IExtendedFileSpec> results = server.getExtendedFiles(fileSpec, opts);

        assertThat("Wrong number of file specs", results.size(), is(6));

        assertThat(results.get(0).getDepotPathString(), is("//depot/bin_foobarbaz.txt"));
        assertThat(results.get(1).getDepotPathString(), is("//depot/foobarbaz.txt"));
        assertThat(results.get(2).getDepotPathString(), is("//depot/bin_foobar.txt"));
        assertThat(results.get(3).getDepotPathString(), is("//depot/foobar.txt"));
        assertThat(results.get(4).getDepotPathString(), is("//depot/bin_foo.txt"));
        assertThat(results.get(5).getDepotPathString(), is("//depot/foo.txt"));


        opts.setReverseSort(true);
        results = server.getExtendedFiles(fileSpec, opts);

        assertThat("Wrong number of file specs", results.size(), is(6));

        assertThat(results.get(0).getDepotPathString(), is("//depot/bin_foo.txt"));
        assertThat(results.get(1).getDepotPathString(), is("//depot/foo.txt"));
        assertThat(results.get(2).getDepotPathString(), is("//depot/bin_foobar.txt"));
        assertThat(results.get(3).getDepotPathString(), is("//depot/foobar.txt"));
        assertThat(results.get(4).getDepotPathString(), is("//depot/bin_foobarbaz.txt"));
        assertThat(results.get(5).getDepotPathString(), is("//depot/foobarbaz.txt"));
    }

    // all set sort flags get sent to the server, the first one listed is what the server will use
    // the order is set by GetExtendedFilesOptions in the OPTIONS_SPECS member
    // "s:F i:m:gtz b:r i:c:cl i:e:cl b:St b:Sd b:Sr b:Sh b:Ss s:A"
    @Test
    public void sortOverrides() throws Throwable {
        List<IFileSpec> fileSpec = makeFileSpecList("//depot/...");
        GetExtendedFilesOptions opts = new GetExtendedFilesOptions();
        // OPTIONS_SPECS: file type -> by date -> by head rev -> by have rev -> by filesize
        opts.setSortByFiletype(true);
        opts.setSortByDate(true);
        opts.setSortByHeadRev(true);
        opts.setSortByHaveRev(true);
        opts.setSortByFileSize(true);

        List<IExtendedFileSpec> results = server.getExtendedFiles(fileSpec, opts);

        assertThat("Wrong number of file specs", results.size(), is(6));

        assertThat(results.get(0).getDepotPathString(), is("//depot/bin_foo.txt"));
        assertThat(results.get(1).getDepotPathString(), is("//depot/bin_foobar.txt"));
        assertThat(results.get(2).getDepotPathString(), is("//depot/bin_foobarbaz.txt"));
        assertThat(results.get(3).getDepotPathString(), is("//depot/foobar.txt"));
        assertThat(results.get(4).getDepotPathString(), is("//depot/foobarbaz.txt"));
        assertThat(results.get(5).getDepotPathString(), is("//depot/foo.txt"));

        opts.setReverseSort(true);
        results = server.getExtendedFiles(fileSpec, opts);

        assertThat("Wrong number of file specs", results.size(), is(6));

        assertThat(results.get(0).getDepotPathString(), is("//depot/foo.txt"));
        assertThat(results.get(1).getDepotPathString(), is("//depot/foobarbaz.txt"));
        assertThat(results.get(2).getDepotPathString(), is("//depot/foobar.txt"));
        assertThat(results.get(3).getDepotPathString(), is("//depot/bin_foo.txt"));
        assertThat(results.get(4).getDepotPathString(), is("//depot/bin_foobar.txt"));
        assertThat(results.get(5).getDepotPathString(), is("//depot/bin_foobarbaz.txt"));
    }

    // verify all options work
    @Test
    public void fetchPlainAttribute() throws Throwable {
        List<IFileSpec> fileSpec = makeFileSpecList("//depot/foo.txt");
        GetExtendedFilesOptions opts = new GetExtendedFilesOptions();
        FileStatOutputOptions oOpts = new FileStatOutputOptions();
        FileStatAncilliaryOptions aOpts = new FileStatAncilliaryOptions();

        aOpts.setShowAttributes(true);
        aOpts.setShowHexAttributes(false);

        opts.setAncilliaryOptions(aOpts);
        opts.setOutputOptions(oOpts);

        List<IExtendedFileSpec> results = server.getExtendedFiles(fileSpec, opts);

        IExtendedFileSpec extendedFileSpec = results.get(0);
        assertThat(extendedFileSpec.getAttributes().size(), is(1));
        assertThat(extendedFileSpec.getAttributes().containsKey("fooFiles"), is(true));
        assertThat(new String(extendedFileSpec.getAttributes().get("fooFiles")), is("2"));
    }

    @Test
    public void fetchHexAttribute() throws Throwable {
        List<IFileSpec> fileSpec = makeFileSpecList("//depot/bin_foo.txt");
        GetExtendedFilesOptions opts = new GetExtendedFilesOptions();
        FileStatOutputOptions oOpts = new FileStatOutputOptions();
        FileStatAncilliaryOptions aOpts = new FileStatAncilliaryOptions();

        aOpts.setShowAttributes(true);

        opts.setAncilliaryOptions(aOpts);
        opts.setOutputOptions(oOpts);

        List<IExtendedFileSpec> results = server.getExtendedFiles(fileSpec, opts);

        IExtendedFileSpec extendedFileSpec = results.get(0);
        assertThat(extendedFileSpec.getAttributes().size(), is(1));
        assertThat(extendedFileSpec.getAttributes().containsKey("binFiles"), is(true));
        assertThat(new String(extendedFileSpec.getAttributes().get("binFiles")), is("Welcome to P4Java in hex!"));

        aOpts.setShowHexAttributes(true);

        opts.setAncilliaryOptions(aOpts);
        opts.setOutputOptions(oOpts);

        results = server.getExtendedFiles(fileSpec, opts);
        extendedFileSpec = results.get(0);
        assertThat(extendedFileSpec.getAttributes().size(), is(1));
        assertThat(extendedFileSpec.getAttributes().containsKey("binFiles"), is(true));
        assertThat(new String(extendedFileSpec.getAttributes().get("binFiles")), is("57656C636F6D6520746F2050344A61766120696E2068657821"));
    }

    @Test
    public void fetchWithAttributePattern() throws Throwable {
        List<IFileSpec> fileSpec = makeFileSpecList("//depot/foobarbaz.txt");
        GetExtendedFilesOptions opts = new GetExtendedFilesOptions();
        FileStatOutputOptions oOpts = new FileStatOutputOptions();
        FileStatAncilliaryOptions aOpts = new FileStatAncilliaryOptions();

        aOpts.setShowAttributes(true);

        // this should get us the barFiles attribute
        opts.setAncilliaryOptions(aOpts);
        opts.setOutputOptions(oOpts);
        opts.setAttributePattern("*bar*");

        List<IExtendedFileSpec> results = server.getExtendedFiles(fileSpec, opts);

        IExtendedFileSpec extendedFileSpec = results.get(0);
        assertThat(extendedFileSpec.getAttributes().size(), is(1));
        assertThat(extendedFileSpec.getAttributes().containsKey("barFiles"), is(true));
        assertThat(new String(extendedFileSpec.getAttributes().get("barFiles")), is("1"));

        // this should not get us any attributes
        opts.setAttributePattern("*baz*");

        results = server.getExtendedFiles(fileSpec, opts);
        extendedFileSpec = results.get(0);
        assertThat(extendedFileSpec.getAttributes().size(), is(0));
    }

    // verify all fstat functionality is present( manual test )

    // verify all fstat functionality is present( manual test )
    @AfterAll
    public static void afterClass() {
        helper.after(ts);
    }
}

