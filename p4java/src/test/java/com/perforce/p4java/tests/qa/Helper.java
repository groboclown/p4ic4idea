package com.perforce.p4java.tests.qa;

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
import com.perforce.p4java.exception.P4JavaException;
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
import com.perforce.test.P4ExtFileUtils;
import com.perforce.test.TestServer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.fail;

public class Helper {
    public static final String FILE_SEP = System.getProperty("file.separator");
    private static final String SERVER_HOST = "p4java://localhost:";

    public String getServerVersion() {
        Properties p = new Properties();
        String version = null;
        // p4ic4idea: use proper resource loading
        try (InputStream is = P4ExtFileUtils.getStream(this, "/qa/test.properties")) {
            p.load(is);
            version = p.getProperty("serverVersion");
        // p4ic4idea: never catch Throwable unless you're really careful
        //} catch (Throwable t) {
        } catch (IOException t) {
            error(t);
        }
        return version;
    }

    public IOptionsServer getServer(TestServer ts) throws Throwable {
        // return ServerFactory.getOptionsServer(SERVER_HOST + ts.getPort(), null);
        return ServerFactory.getOptionsServer(ts.getRSHURL(), null);
    }

    public IOptionsServer getServerWithLocalUrl(TestServer ts) throws Throwable {
        // return ServerFactory.getOptionsServer(SERVER_HOST + ts.getPort(), null);
        return ServerFactory.getOptionsServer(ts.getLocalUrl(), null);
    }

    public IOptionsServer getServer(TestServer ts, Properties props) throws Throwable {
        //return ServerFactory.getOptionsServer(SERVER_HOST + ts.getPort(), props);
        return ServerFactory.getOptionsServer(ts.getRSHURL(), props);
    }

    public IOptionsServer getProxy(TestServer ts) throws Throwable {
        // TODO resolve this so it can use a proxy
        //return ServerFactory.getOptionsServer(SERVER_HOST + ts.getProxyPort(), null);
        return ServerFactory.getOptionsServer(ts.getRSHURL(), null);
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

        if (password != null) {
            user.setPassword(password);
        }

        server.setUserName(loginName);
        server.connect();
        server.createUser(user, false);

        if (password != null) {
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
        if (user != null) {
            client.setOwnerName(user.getLoginName());
        }
        if (isBlank(path)) {
            mapping = "//depot/... //" + name + "/...";
        } else {
            mapping = path;
        }

        File clientRoot;
        if (clientRootPath == null) {
            // No - this is wrong.
            clientRoot = new File("tmp/" + name + "-root");
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
        if (changelist != null) {
            editFilesOptions.setChangelistId(changelist.getId());
        }

        List<IFileSpec> editedFiles = client.editFiles(fileSpec, editFilesOptions);
        validateFileSpecs(editedFiles);
        if (changelist != null) {
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

    public IJob addJob(IOptionsServer server, IUser user, String desc) throws P4JavaException {
        Map<String, Object> fieldMap = new HashMap<String, Object>();
        fieldMap.put("Job", "new");
        fieldMap.put("Status", "open");
        fieldMap.put("User", server.getUserName());
        fieldMap.put("Description", desc);

        IJob job = server.createJob(fieldMap);

        assertThat(job, notNullValue());

        return job;

    }

    public void validateFileSpecs(List<IFileSpec> fileSpecs) {
        assertThat(fileSpecs, notNullValue());
        for (IFileSpec fileSpec : fileSpecs) {
            if (fileSpec.getOpStatus() != FileSpecOpStatus.INFO) {
                assertThat(fileSpec.getStatusString(), fileSpec.getOpStatus(),
                        is(FileSpecOpStatus.VALID));
            }
        }
    }

    public void validateFileSpecs(List<IFileSpec> fileSpecs, FileSpecOpStatus expectedOpStatus)
            throws Throwable {
        assertThat(fileSpecs, notNullValue());
        for (IFileSpec fileSpec : fileSpecs) {
            if (fileSpec.getOpStatus() != FileSpecOpStatus.INFO) {
                assertThat(fileSpec.getStatusString(), fileSpec.getOpStatus(),
                        is(expectedOpStatus));
            }
        }
    }

    public void after(TestServer ts) {
        if (ts != null) {
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
        List<IExtendedFileSpec> extendedFileSpecs = new ArrayList<IExtendedFileSpec>();
        for (IFileSpec sourceFile : sourceFiles) {
            List<IFileSpec> sourceFileList = new ArrayList<IFileSpec>();
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
            return new String(Files.readAllBytes(Paths.get(file)), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("IO problem in fileToString", e);
        }
    }
}
