package com.perforce.p4java.tests.qa;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.client.IClientSummary;
import com.perforce.p4java.client.IClientViewMapping;
import com.perforce.p4java.core.CoreFactory;
import com.perforce.p4java.core.IBranchMapping;
import com.perforce.p4java.core.IBranchSpec;
import com.perforce.p4java.core.IBranchSpecSummary;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.IChangelistSummary;
import com.perforce.p4java.core.IJob;
import com.perforce.p4java.core.ILabel;
import com.perforce.p4java.core.ILabelMapping;
import com.perforce.p4java.core.ILabelSummary;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.core.IUserGroup;
import com.perforce.p4java.core.IUserSummary;
import com.perforce.p4java.core.ViewMap;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.generic.client.ClientView;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.test.TestServer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.HashMap;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.jupiter.api.Assertions.fail;

@RunWith(JUnitPlatform.class)
public class CoreFactoryTest {
    private static TestServer ts = null;
    private static Helper helper = null;
    private static IOptionsServer server = null;
    private static IUser user = null;
    private static IClient client = null;

    // simple setup with one file
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
        helper.addFile(server, user, client, testFile.getAbsolutePath(), "CommonFactoryTest", "text");
    }

    @BeforeEach
    public void reset() throws Exception {
        server.setUserName(user.getLoginName());
    }

    // we should get something here
    @Test
    public void createJobOnServer() throws Exception {
        List<IJob> jobs = server.getJobs(null, null);
        int startingCount = jobs.size();

        HashMap<String, Object> map = newHashMap();

        map.put("Job", "new");
        map.put("Description", "Adding a job with CoreFactory");
        map.put("Status", "open");
        map.put("User", user.getLoginName());

        IJob job = CoreFactory.createJob(server, map, true);

        // test the server object
        jobs = server.getJobs(null, null);
        assertThat("wrong number of jobs", jobs.size(), is(startingCount + 1));
        assertThat(jobs.get(startingCount).getDescription(), is("Adding a job with CoreFactory\n"));

        // test the local object
        assertThat(job.getDescription(), is("Adding a job with CoreFactory\n"));
        assertThat(job.getRawFields().get("User"), is(server.getUserName()));
    }

    @Test
    public void createJob() throws Exception {
        List<IJob> jobs = server.getJobs(null, null);
        int startingCount = jobs.size();
        HashMap<String, Object> map = newHashMap();
        map.put("Job", "new");
        map.put("Description", "Adding a job with CoreFactory");
        map.put("Status", "open");
        map.put("User", user.getLoginName());

        IJob job = CoreFactory.createJob(server, map, false);

        jobs = server.getJobs(null, null);
        assertThat("wrong number of jobs", jobs.size(), is(startingCount));

        assertThat(job.getDescription(), is("Adding a job with CoreFactory"));
        assertThat(job.getRawFields().get("User"), is(server.getUserName()));
        assertThat(job.getRawFields().get("Job"), is("new"));
        assertThat(job.getRawFields().get("Status"), is("open"));
    }

    // we should get something here
    @Test
    public void createFailJobOnServer() throws Exception {
        try {
            HashMap<String, Object> map = newHashMap();

            map.put("Job", "new");
            map.put("Description", "Adding a job with CoreFactory");
            map.put("Status", "trololololololo");
            map.put("User", user.getLoginName());

            CoreFactory.createJob(server, map, true);

            // we shouldn't get here
            fail("we shouldn't be here");
        } catch (RequestException e) {
            assertThat(e.getLocalizedMessage(), startsWith("Error in job specification"));
        }

    }

    // we should get something here
    @Test
    public void createChangeOnServer() throws Exception {
        List<IChangelistSummary> changes = server.getChangelists(null, null);
        int startingCount = changes.size();

        CoreFactory.createChangelist(client, "A new changelist from CoreFactory", true);

        // test the server object
        changes = server.getChangelists(null, null);
        assertThat("wrong number of changes", changes.size(), is(startingCount + 1));
        assertThat(changes.get(0).getDescription(), is("A new changelist from CoreFacto"));
    }

    @Test
    public void createChange() throws Exception {
        List<IChangelistSummary> changes = server.getChangelists(null, null);
        int startingCount = changes.size();

        IChangelist change = CoreFactory.createChangelist(client, "A new changelist from CoreFactory", false);

        // test the server object
        changes = server.getChangelists(null, null);
        assertThat("wrong number of changes", changes.size(), is(startingCount));

        // test the local object
        assertThat("wrong description", change.getDescription(), is("A new changelist from CoreFactory"));
    }

    // we should get something here
    @Test
    public void createChangeOnServerWithNullDesc() throws Exception {
        List<IChangelistSummary> changes = server.getChangelists(null, null);
        int startingCount = changes.size();

        CoreFactory.createChangelist(client, null, true);

        // test the server object
        changes = server.getChangelists(null, null);
        assertThat("wrong number of changes", changes.size(), is(startingCount + 1));
        assertThat(changes.get(0).getDescription(), is("New changelist created by P4Jav"));
    }

    // we should get something here
    @Test
    public void createClientOnServer() throws Exception {
        List<IClientSummary> clients = server.getClients(null);
        int startingCount = clients.size();

        HashMap<String, Object> map = newHashMap();
        map.put("//depot/foo*", "//client2/foo*");

        CoreFactory.createClient(server, "client2", "new client from CoreFactory", client.getRoot(), new String[]{"//depot/foo* //client2/foo*"}, true);

        // test the server object
        clients = server.getClients(null);
        assertThat("wrong number of clients", clients.size(), is(startingCount + 1));
        assertThat(clients.get(1).getDescription(), is("new client from CoreFactory\n"));

        IClient cli = server.getClient(clients.get(1));
        verifyClientViewMapping(cli);
    }

    private void verifyClientViewMapping(IClient cli) {
        ClientView view = cli.getClientView();
        List<IClientViewMapping> list = view.getEntryList();

        assertThat("wrong number of view lines", list.size(), is(1));
        IClientViewMapping firstClientViewMapping = list.get(0);
        assertThat(firstClientViewMapping.getLeft(false), is("//depot/foo*"));
        assertThat(firstClientViewMapping.getRight(false), is("//client2/foo*"));
    }

    // we should get something here
    @Test
    public void createClient() throws Exception {
        List<IClientSummary> clients = server.getClients(null);
        int startingCount = clients.size();

        IClient cli = CoreFactory.createClient(server, "client2", "new client from CoreFactory", client.getRoot(), new String[]{"//depot/foo* //client2/foo*"}, false);

        // test the server object
        clients = server.getClients(null);
        assertThat("wrong number of clients", clients.size(), is(startingCount));

        verifyClientViewMapping(cli);
    }

    @Test
    public void createLabelOnServer() throws Exception {
        List<ILabelSummary> labels = server.getLabels(null, null);
        int startingCount = labels.size();

        ILabel label = CoreFactory.createLabel(server, "label1", "Label created with CoreFactory", new String[]{"//depot/foo*", "//depot/bar*"}, true);

        // test the server object
        labels = server.getLabels(null, null);
        assertThat("wrong number of labels", labels.size(), is(startingCount + 1));

        verifyLabelMapping(label);
    }

    private void verifyLabelMapping(ILabel label) {
        ViewMap<ILabelMapping> view = label.getViewMapping();
        List<ILabelMapping> list = view.getEntryList();

        assertThat("wrong number of view lines", list.size(), is(2));
        assertThat(list.get(0).getLeft(), is("//depot/foo*"));
        assertThat(list.get(1).getLeft(), is("//depot/bar*"));
    }

    @Test
    public void createLabel() throws Exception {
        List<ILabelSummary> labels = server.getLabels(null, null);
        int startingCount = labels.size();

        ILabel label = CoreFactory.createLabel(server, "label2", "Label created with CoreFactory", new String[]{"//depot/foo*", "//depot/bar*"}, false);

        // test the server object
        labels = server.getLabels(null, null);
        assertThat("wrong number of labels", labels.size(), is(startingCount));

        verifyLabelMapping(label);
    }

    @Test
    public void createUserOnServer() throws Exception {
        List<IUserSummary> users = server.getUsers(null, null);
        int startingCount = users.size();

        server.setUserName("user2");
        IUser user = CoreFactory.createUser(server, "user2", "user2@perforce.com", "User two", null, true);

        // test the server object
        users = server.getUsers(null, null);
        assertThat("wrong number of labels", users.size(), is(startingCount + 1));

        verifyCreatedUser(user);
    }

    private void verifyCreatedUser(IUser user) {
        assertThat("wrong name", user.getFullName(), is("User two"));
        assertThat("wrong login name", user.getLoginName(), is("user2"));
        assertThat("wrong email", user.getEmail(), is("user2@perforce.com"));
    }

    @Test
    public void createUser() throws Exception {
        List<IUserSummary> users = server.getUsers(null, null);
        int startingCount = users.size();

        IUser user = CoreFactory.createUser(server, "user2", "user2@perforce.com", "User two", null, false);

        // test the server object
        users = server.getUsers(null, null);
        assertThat("wrong number of labels", users.size(), is(startingCount));

        verifyCreatedUser(user);
    }

    @Test
    public void createGroupOnServer() throws Exception {
        List<IUserGroup> groups = server.getUserGroups(null, null);
        int startingCount = groups.size();

        List<String> users = newArrayList();
        users.add(user.getLoginName());

        IUserGroup group = CoreFactory.createUserGroup(server, "group1", users, true);

        // test the server object
        groups = server.getUserGroups(null, null);
        assertThat("wrong number of groups", groups.size(), is(startingCount + 1));
        assertThat("wrong name", group.getName(), is("group1"));
        assertThat("wrong login name", group.getUsers().get(0), is(user.getLoginName()));
    }

    @Test
    public void createGroup() throws Exception {
        List<IUserGroup> groups = server.getUserGroups(null, null);
        int startingCount = groups.size();

        List<String> users = newArrayList();
        users.add(user.getLoginName());

        IUserGroup group = CoreFactory.createUserGroup(server, "group1", users, false);

        // test the server object
        groups = server.getUserGroups(null, null);
        assertThat("wrong number of groups", groups.size(), is(startingCount));

        assertThat("wrong name", group.getName(), is("group1"));
        assertThat("wrong login name", group.getUsers().get(0), is(user.getLoginName()));
    }

    @Test
    public void createBranchSpecOnServer() throws Exception {
        List<IBranchSpecSummary> branches = server.getBranchSpecs(null);
        int startingCount = branches.size();

        IBranchSpec branch = CoreFactory.newBranchSpec(server, "branch1", "A branch created from CoreFactory", new String[]{"//depot/foo... //depot/bar..."}, true);

        // test the server object
        branches = server.getBranchSpecs(null);
        assertThat("wrong number of groups", branches.size(), is(startingCount + 1));

        assertThat("wrong name", branch.getName(), is("branch1"));
        assertThat("wrong description", branch.getDescription(), is("A branch created from CoreFactory\n"));

        verifyBranchMapping(branch);
    }

    private void verifyBranchMapping(IBranchSpec branch) {
        ViewMap<IBranchMapping> view = branch.getBranchView();
        List<IBranchMapping> list = view.getEntryList();

        assertThat("wrong number of view lines", list.size(), is(1));
        assertThat(list.get(0).getLeft(false), is("//depot/foo..."));
        assertThat(list.get(0).getRight(false), is("//depot/bar..."));
    }

    @Test
    public void createBranchSpec() throws Exception {
        List<IBranchSpecSummary> branches = server.getBranchSpecs(null);
        int startingCount = branches.size();

        IBranchSpec branch = CoreFactory.newBranchSpec(server, "branch2", "A branch created from CoreFactory", new String[]{"//depot/foo... //depot/bar..."}, false);

        // test the server object
        branches = server.getBranchSpecs(null);
        assertThat("wrong number of groups", branches.size(), is(startingCount));

        assertThat("wrong name", branch.getName(), is("branch2"));
        assertThat("wrong description", branch.getDescription(), is("A branch created from CoreFactory"));

        verifyBranchMapping(branch);
    }


    @AfterAll
    public static void afterClass() {
        helper.after(ts);
    }
}