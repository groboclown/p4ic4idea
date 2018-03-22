package com.perforce.p4java.tests.qa;

import com.google.common.io.Files;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IBranchMapping;
import com.perforce.p4java.core.IBranchSpec;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.IDepot;
import com.perforce.p4java.core.IDepot.DepotType;
import com.perforce.p4java.core.IJob;
import com.perforce.p4java.core.ILabel;
import com.perforce.p4java.core.ILabelMapping;
import com.perforce.p4java.core.IStream;
import com.perforce.p4java.core.IStreamSummary.IOptions;
import com.perforce.p4java.core.IStreamSummary.Type;
import com.perforce.p4java.core.IStreamViewMapping;
import com.perforce.p4java.core.IStreamViewMapping.PathType;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.core.ViewMap;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.FileStatOutputOptions;
import com.perforce.p4java.core.file.IExtendedFileSpec;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.impl.generic.client.ClientView;
import com.perforce.p4java.impl.generic.client.ClientView.ClientViewMapping;
import com.perforce.p4java.impl.generic.core.BranchSpec;
import com.perforce.p4java.impl.generic.core.Changelist;
import com.perforce.p4java.impl.generic.core.Depot;
import com.perforce.p4java.impl.generic.core.Label;
import com.perforce.p4java.impl.generic.core.Stream;
import com.perforce.p4java.impl.generic.core.Stream.StreamViewMapping;
import com.perforce.p4java.impl.generic.core.User;
import com.perforce.p4java.impl.mapbased.client.Client;
import com.perforce.p4java.option.client.AddFilesOptions;
import com.perforce.p4java.option.client.DeleteFilesOptions;
import com.perforce.p4java.option.client.EditFilesOptions;
import com.perforce.p4java.option.server.GetExtendedFilesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.ServerFactory;
import com.perforce.test.TestServer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class Helper {
    public static final String FILE_SEP = System.getProperty("file.separator");
    private static final String SERVER_HOST = "p4java://localhost:";

    public String getServerVersion() {
        Properties p = new Properties();
        String version = null;
        try (FileInputStream is = new FileInputStream("src/test/resources/qa/test.properties")) {
            p.load(is);
            version = p.getProperty("serverVersion");
        } catch (Throwable t) {
            error(t);
        }
        return version;
    }

    public IOptionsServer getServer(TestServer ts) throws Throwable {
        return ServerFactory.getOptionsServer(SERVER_HOST + ts.getPort(), null);
    }

    public IOptionsServer getServer(TestServer ts, Properties props) throws Throwable {
        return ServerFactory.getOptionsServer(SERVER_HOST + ts.getPort(), props);
    }

    public IOptionsServer getProxy(TestServer ts) throws Throwable {
        return ServerFactory.getOptionsServer(SERVER_HOST + ts.getProxyPort(), null);
    }

    public IUser createUser(IOptionsServer server, String loginName) throws Throwable {
        return createUser(server, loginName, null);
    }

    public IUser createUser(IOptionsServer server, String loginName, String password)
            throws Throwable {
        IUser user = new User();
        user.setLoginName(loginName);
        user.setFullName(loginName);
        user.setEmail(loginName + "@email.com");

        if (nonNull(password)) {
            user.setPassword(password);
        }

        server.setUserName(loginName);
        server.connect();
        server.createUser(user, false);

        if (nonNull(password)) {
            server.login(password);
        }
        assertThat(server.getUser(loginName), notNullValue());
        return user;
    }

    public IClient createClient(IOptionsServer server, String name) throws Throwable {
        return createClient(server, name, null, null, null);
    }

    public IClient createClient(IOptionsServer server, String name, String clientRootPath)
            throws Throwable {
        return createClient(server, name, null, null, clientRootPath);
    }

    public IClient createClient(IOptionsServer server, String name, IUser user, String path,
            String clientRootPath) throws Throwable {
        IClient client = new Client();
        client.setName(name);
        String mapping;
        if (nonNull(user)) {
            client.setOwnerName(user.getLoginName());
        }
        if (isBlank(path)) {
            mapping = "//depot/... //" + name + "/...";
        } else {
            mapping = path;
        }

        File clientRoot;
        if (clientRootPath == null) {
            clientRoot = new File(server.getServerInfo().getServerRoot(), name + "-root");
        } else {
            clientRoot = new File(clientRootPath);
        }
        assertThat(clientRoot.mkdir(), is(true));
        client.setRoot(clientRoot.getAbsolutePath());

        ClientView clientView = new ClientView();
        ClientViewMapping clientViewMapping = new ClientViewMapping(0, mapping);
        clientView.addEntry(clientViewMapping);
        client.setClientView(clientView);

        client.setServer(server);

        server.setCurrentClient(client);
        server.connect();
        server.createClient(client);
        assertThat(server.getClient(name), notNullValue());
        return client;
    }

    public IChangelist createChangelist(IOptionsServer server, IUser user, IClient client)
            throws Throwable {
        IChangelist changelist = new Changelist();
        changelist.setUsername(user.getLoginName());
        changelist.setClientId(client.getName());
        changelist.setDescription("Changelist for user " + user.getLoginName() + " and client "
                + client.getName() + ".");

        server.connect();
        changelist = client.createChangelist(changelist);
        assertThat(changelist, notNullValue());
        return changelist;
    }

    public List<IFileSpec> createFile(String path, String content) throws Throwable {
        List<IFileSpec> fileSpecs;
        try (FileWriter writer = new FileWriter(path)) {
            writer.write(content);
            writer.close();
            fileSpecs = FileSpecBuilder.makeFileSpecList(path);
            assertThat(fileSpecs.size() > 0, is(true));
            validateFileSpecs(fileSpecs);
        }
        return fileSpecs;
    }

    public List<IFileSpec> editFile(String path, String content, IChangelist changelist,
            IClient client) throws Throwable {
        List<IFileSpec> fileSpec = FileSpecBuilder.makeFileSpecList(expandASCII(path));
        EditFilesOptions editFilesOptions = new EditFilesOptions();
        if (nonNull(changelist)) {
            editFilesOptions.setChangelistId(changelist.getId());
        }

        List<IFileSpec> editedFiles = client.editFiles(fileSpec, editFilesOptions);
        validateFileSpecs(editedFiles);
        if (nonNull(changelist)) {
            changelist.update();
        }

        createFile(path, content);
        return editedFiles;

    }

    // create a basic stream in the server
    public IStream createStream(IOptionsServer server, String name, Type type, String parent)
            throws Throwable {
        IStream stream = new Stream();
        stream.setStream(name);
        stream.setType(type);
        stream.setParent(parent);
        stream.setName(name);
        stream.setOwnerName(server.getUserName());

        ViewMap<IStreamViewMapping> view = new ViewMap<>();
        StreamViewMapping entry = new StreamViewMapping();
        entry.setPathType(PathType.SHARE);
        entry.setViewPath("...");
        entry.setOrder(0);
        view.addEntry(entry);
        stream.setStreamView(view);

        server.createStream(stream);
        stream = server.getStream(name);
        assertThat(stream, notNullValue());

        if (type != Type.MAINLINE) {
            IOptions sOpts = stream.getOptions();
            sOpts.setNoFromParent(false);
            sOpts.setNoToParent(false);
            stream.setOptions(sOpts);
            stream.update();
        }

        return stream;
    }

    // create a depot
    public String createDepot(IOptionsServer server, String name, DepotType type, String mapping,
            String path) throws Throwable {
        IDepot nd = new Depot(name, server.getUserName(), null, "A depot of great importance", type,
                mapping, ".p4s", path);

        return server.createDepot(nd);
    }

    public List<IFileSpec> addFile(IOptionsServer server, IUser user, IClient client, String path,
            String content) throws Throwable {
        return addFile(server, user, client, path, content, null);
    }

    public List<IFileSpec> addFile(IOptionsServer server, IUser user, IClient client, String path,
            String content, String fileType) throws Throwable {
        List<IFileSpec> createdFileSpecs = createFile(path, content);
        IChangelist changelist = createChangelist(server, user, client);
        AddFilesOptions addFilesOptions = new AddFilesOptions();
        addFilesOptions.setUseWildcards(true);
        addFilesOptions.setChangelistId(changelist.getId());
        if (isNotBlank(fileType)) {
            addFilesOptions.setFileType(fileType);
        }

        List<IFileSpec> addedFileSpecs = client.addFiles(createdFileSpecs, addFilesOptions);
        validateFileSpecs(addedFileSpecs);

        changelist.update();
        List<IFileSpec> submittedFileSpecs = changelist.submit(false);
        validateFileSpecs(submittedFileSpecs);

        List<IFileSpec> depotFileSpecs = FileSpecBuilder
                .makeFileSpecList(submittedFileSpecs.get(0).getDepotPathString());
        validateFileSpecs(depotFileSpecs);

        return depotFileSpecs;

    }

    public List<IFileSpec> deleteFile(String path, IChangelist changelist, IClient client)
            throws Throwable {
        List<IFileSpec> fileSpec = FileSpecBuilder.makeFileSpecList(expandASCII(path));
        List<IFileSpec> deletedFiles = client.deleteFiles(fileSpec,
                new DeleteFilesOptions().setChangelistId(changelist.getId()));
        validateFileSpecs(deletedFiles);

        changelist.update();
        changelist.submit(false);

        return deletedFiles;

    }

    public ILabel addLabel(IOptionsServer server, IUser user, String name, String path)
            throws Throwable {
        ILabel label = new Label();
        label.setName(name);
        label.setOwnerName(user.getLoginName());

        ViewMap<ILabelMapping> viewMapping = new ViewMap<>();
        Label.LabelMapping entry = new Label.LabelMapping();
        entry.setLeft(path);
        viewMapping.addEntry(entry);
        label.setViewMapping(viewMapping);

        server.createLabel(label);

        label = server.getLabel(name);

        assertThat(label, notNullValue());

        return label;
    }

    public IBranchSpec addBranchspec(IOptionsServer server, IUser user, String name, String path1,
            String path2) throws Throwable {
        IBranchSpec branch = new BranchSpec();
        branch.setName(name);
        branch.setOwnerName(user.getLoginName());

        ViewMap<IBranchMapping> view = new ViewMap<>();
        view.addEntry(new BranchSpec.BranchViewMapping(0, path1 + SPACE + path2));
        branch.setBranchView(view);

        server.createBranchSpec(branch);
        branch = server.getBranchSpec(name);

        assertThat(branch, notNullValue());
        return branch;

    }

    public IJob addJob(IOptionsServer server, IUser user, String desc) throws Throwable {
        Map<String, Object> fieldMap = newHashMap();
        fieldMap.put("Job", "new");
        fieldMap.put("Status", "open");
        fieldMap.put("User", server.getUserName());
        fieldMap.put("Description", desc);

        IJob job = server.createJob(fieldMap);

        assertThat(job, notNullValue());

        return job;

    }

    public void validateFileSpecs(List<IFileSpec> fileSpecs) throws Throwable {
        assertThat(fileSpecs, notNullValue());
        for (IFileSpec fileSpec : fileSpecs) {
            if (fileSpec.getOpStatus() != FileSpecOpStatus.INFO) {
                assertThat(fileSpec.getStatusMessage(), fileSpec.getOpStatus(),
                        is(FileSpecOpStatus.VALID));
            }
        }
    }

    public void assertFileSpecError(List<IFileSpec> fileSpecs) {
        assertThat(fileSpecs, notNullValue());
        for (IFileSpec fileSpec : fileSpecs) {
            if (fileSpec.getOpStatus() == FileSpecOpStatus.ERROR) {
                assertEquals(fileSpec.getStatusMessage(), FileSpecOpStatus.ERROR, fileSpec.getOpStatus());
            }
        }
    }

    public void validateFileSpecs(List<IFileSpec> fileSpecs, FileSpecOpStatus expectedOpStatus)
            throws Throwable {
        assertThat(fileSpecs, notNullValue());
        for (IFileSpec fileSpec : fileSpecs) {
            if (fileSpec.getOpStatus() != FileSpecOpStatus.INFO) {
                assertThat(fileSpec.getStatusMessage(), fileSpec.getOpStatus(),
                        is(expectedOpStatus));
            }
        }
    }

    public void after(TestServer ts) {
        if (nonNull(ts)) {
            try {
                ts.delete();
            } catch (Exception e) {
                System.err.println(e.getMessage());
                e.printStackTrace();
                fail();
            }
        }
    }

    public void error(Throwable t) {
        System.err.println(t.getMessage());
        t.printStackTrace();
        fail();
    }

    public List<IExtendedFileSpec> getExtendedFileSpecs(IOptionsServer server,
            List<IFileSpec> sourceFiles, int changelistId) throws Throwable {
        FileStatOutputOptions fileStatOutputOptions = new FileStatOutputOptions();
        GetExtendedFilesOptions getExtendedFilesOptions = new GetExtendedFilesOptions();
        getExtendedFilesOptions.setOutputOptions(fileStatOutputOptions);
        List<IExtendedFileSpec> extendedFileSpecs = newArrayList();
        for (IFileSpec sourceFile : sourceFiles) {
            List<IFileSpec> sourceFileList = newArrayList();
            sourceFileList.add(sourceFile);
            fileStatOutputOptions.setShelvedFiles(true);
            getExtendedFilesOptions.setAffectedByChangelist(changelistId);

            List<IExtendedFileSpec> tempExtendedFileSpecs = server.getExtendedFiles(sourceFileList,
                    getExtendedFilesOptions);
            if (tempExtendedFileSpecs.get(0).getOpStatus() == FileSpecOpStatus.VALID) {
                extendedFileSpecs.addAll(tempExtendedFileSpecs);
            } else {
                fileStatOutputOptions.setShelvedFiles(false);
                getExtendedFilesOptions.setAffectedByChangelist(IChangelist.UNKNOWN);
                tempExtendedFileSpecs = server.getExtendedFiles(sourceFileList,
                        getExtendedFilesOptions);
                assertThat(tempExtendedFileSpecs.get(0).getOpStatus(), is(FileSpecOpStatus.VALID));
                extendedFileSpecs.addAll(tempExtendedFileSpecs);
            }
        }

        return extendedFileSpecs;
    }

    public String expandASCII(String input) throws Throwable {
        String output = input;
        output = output.replaceAll("%", "%25"); // this must be first
        output = output.replaceAll("@", "%40");
        output = output.replaceAll("#", "%23");
        output = output.replaceAll("\\*", "%2A");
        return output;
    }

    public String fileToString(String file) {
        try {
            return Files.toString(new File(file), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("IO problem in fileToString", e);
        }
    }
}
